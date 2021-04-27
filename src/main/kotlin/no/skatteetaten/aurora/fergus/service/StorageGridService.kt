package no.skatteetaten.aurora.fergus.service

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import no.skatteetaten.aurora.fergus.FergusException
import no.skatteetaten.aurora.fergus.controllers.Access
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.GroupsApi
import org.openapitools.client.api.S3Api
import org.openapitools.client.api.UsersApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.ContainerCreate
import org.openapitools.client.model.ContainerCreateResponse
import org.openapitools.client.model.ContainerListResponse
import org.openapitools.client.model.Credentials
import org.openapitools.client.model.GetPatchPostPutGroupResponse
import org.openapitools.client.model.GetPatchPostPutUserResponse
import org.openapitools.client.model.InlineObject1
import org.openapitools.client.model.ListGroupsResponse
import org.openapitools.client.model.ListUsersResponse
import org.openapitools.client.model.PasswordChangeRequest
import org.openapitools.client.model.PatchUserRequest
import org.openapitools.client.model.Policies
import org.openapitools.client.model.PolicyS3
import org.openapitools.client.model.PolicyS3Statement
import org.openapitools.client.model.PostAccessKeyResponse
import org.openapitools.client.model.PostGroupRequest
import org.openapitools.client.model.PostUserRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class StorageGridServiceReactive(
    @Value("\${fergus.provision.user.randompass}") val randompass: String,
    @Value("\${fergus.provision.user.defaultpass}") val defaultpass: String,
    private val storageGridAuthApi: AuthApi,
    private val storageGridApiFactory: StorageGridApiFactory,
    private val storageGridGroupsApi: GroupsApi,
    private val storageGridUsersApi: UsersApi,
    private val storageGridS3Api: S3Api
) : StorageGridService {

    override suspend fun authorize(
        authorizationPayload: AuthorizationPayload
    ): String {
        val response: AuthorizeResponse = storageGridApiFactory.storageGridAuthApi()
            .authorizePost(authorizationPayload.toAuthorizeInput())
            .awaitSingle()
        if (response.status === AuthorizeResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid auth api returned an error"
            )
        }
        return response.data // Returns authorization token
    }

    override suspend fun provideBucket(
        bucketName: String,
        token: String
    ): String {
        val storageGridContainersApi = storageGridApiFactory.storageGridContainersApi(token)
        // Get list of buckets for tenant
        val bucketListResponse = storageGridContainersApi
            .orgContainersGet(listOf<String>())
            .awaitSingle()
        if (bucketListResponse.status === ContainerListResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid containers api returned an error on orgContainersGet"
            )
        }
        // Check if bucketName exists in bucketListResponse, if not, create
        val bucketNames: List<String> = bucketListResponse.data.map { it.name }
        if (!bucketNames.contains(bucketName)) {
            val containerCreate = ContainerCreate().name(bucketName)
            val containerCreateResponse = storageGridContainersApi
                .orgContainersPost(containerCreate)
                .awaitSingle()
            if (containerCreateResponse.status === ContainerCreateResponse.StatusEnum.ERROR) {
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Storagegrid containers api returned an error on orgContainersPost"
                )
            }
        }

        return bucketName
    }

    override suspend fun provideGroup(bucketName: String, path: String, access: List<Access>, token: String): UUID {
        storageGridGroupsApi.apiClient.setBearerToken(token)

        val groupName = createGroupName(bucketName, path, access)

        // Get list of buckets for tenant
        val listGroupsResponse = storageGridGroupsApi
            .orgGroupsGet(null, 100000, null, null, null)
            .awaitSingle()
        if (listGroupsResponse.status === ListGroupsResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid groups api returned an error on orgGroupsGet"
            )
        }
        // Check if groupName exists in listGroupsResponse, if not, create with policy
        val groupDisplayNames: List<String> = listGroupsResponse.data.mapNotNull { it.displayName }
        val groupId = if (!groupDisplayNames.contains(groupName)) {
            createGroupWithPolicy(groupName, bucketName, path, access)
        } else {
            // Find id for matching group
            (listGroupsResponse.data.filter { it -> it.displayName == groupName }).first().id
        }

        if (groupId == null) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find or create requested group"
            )
        }

        return UUID.fromString(groupId)
    }

    private suspend fun createGroupWithPolicy(
        groupName: String,
        bucketName: String,
        path: String,
        access: List<Access>
    ): String? {
        val bucketStatement = PolicyS3Statement()
            .effect(PolicyS3Statement.EffectEnum.ALLOW)
            .addActionItem("s3:ListBucket")
            .addActionItem("s3:GetBucketLocation")
            .addResourceItem("arn:aws:s3:::$bucketName/*")
        val objectActionStatement = createS3ObjectActionStatement(bucketName, path, access)

        val postGroupRequest = PostGroupRequest()
            .displayName(groupName)
            .policies(
                Policies().s3(
                    PolicyS3()
                        .id(groupName)
                        .addStatementItem(bucketStatement)
                        .addStatementItem(objectActionStatement)
                )
            )
        val groupCreateResponse = storageGridGroupsApi
            .orgGroupsPost(postGroupRequest)
            .awaitSingle()
        if (groupCreateResponse.status === GetPatchPostPutGroupResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid groups api returned an error on orgGroupsPost"
            )
        }

        return groupCreateResponse.data.id
    }

    private fun createGroupName(
        bucketName: String,
        path: String,
        access: List<Access>
    ): String {
        var groupNamePostfix = ""
        if (access.size > 0) {
            access.forEach { groupNamePostfix += it.name.take(1) }
        } else groupNamePostfix = "RWD"

        val groupName = "$bucketName-$path-$groupNamePostfix"
        return groupName
    }

    private fun createS3ObjectActionStatement(
        bucketName: String,
        path: String,
        access: List<Access>
    ): PolicyS3Statement {
        var objectActionStatement = PolicyS3Statement()
            .effect(PolicyS3Statement.EffectEnum.ALLOW)
            .addResourceItem("arn:aws:s3:::$bucketName/$path/*")
        if (access.size <= 0) {
            objectActionStatement
                .addActionItem("s3:PutObject")
                .addActionItem("s3:GetObject")
                .addActionItem("s3:DeleteObject")
            return objectActionStatement
        }

        access.forEach {
            when (it) {
                Access.READ -> objectActionStatement.addActionItem("s3:GetObject")
                Access.WRITE -> objectActionStatement.addActionItem("s3:PutObject")
                Access.DELETE -> objectActionStatement.addActionItem("s3:DeleteObject")
            }
        }
        return objectActionStatement
    }

    override suspend fun provideUser(
        userName: String,
        groupId: UUID,
        token: String
    ): UUID {
        storageGridUsersApi.apiClient.setBearerToken(token)
        // Get list of buckets for tenant
        val listUsersResponse = storageGridUsersApi
            .orgUsersGet(null, 100000, null, null, null)
            .awaitSingle()
        if (listUsersResponse.status === ListUsersResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid users api returned an error on orgUsersGet"
            )
        }
        // Check if userName exists in listUsersResponse, if not, create
        val userNames: List<String> = listUsersResponse.data.mapNotNull { it.fullName }
        val userId = if (!userNames.contains(userName)) {
            createUser(userName, groupId)
        } else {
            // Update group membership for user
            val uId = (listUsersResponse.data.filter { it -> it.fullName == userName }).first().id
            updateUserGroupMember(userName, groupId, uId)
            uId
        }

        if (userId == null) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find or create requested user"
            )
        }

        return userId
    }

    private suspend fun createUser(userName: String, groupId: UUID): UUID? {
        val postUserRequest = PostUserRequest()
            .fullName(userName)
            .uniqueName("user/$userName")
            .addMemberOfItem(groupId)
        val userCreateResponse = storageGridUsersApi
            .orgUsersPost(postUserRequest)
            .awaitSingle()
        if (userCreateResponse.status === GetPatchPostPutUserResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid users api returned an error on orgUsersPost"
            )
        }
        return userCreateResponse.data.id
    }

    private suspend fun updateUserGroupMember(userName: String, groupId: UUID, userId: UUID?) {
        val patchUserRequest = PatchUserRequest().fullName(userName).addMemberOfItem(groupId)
        val patchUserResponse = storageGridUsersApi
            .orgUsersIdPatch(userId.toString(), patchUserRequest)
            .awaitSingle()
        if (patchUserResponse.status === GetPatchPostPutUserResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid users api returned an error on orgUsersIdPatch"
            )
        }
    }

    override suspend fun assignPasswordToUser(
        userId: UUID,
        password: String?,
        token: String
    ): String {
        storageGridUsersApi.apiClient.setBearerToken(token)
        val newPassword: String
        if (password != null && password.isNotEmpty()) {
            newPassword = password
        } else {
            // Check for a default password from Aurora config
            if (randompass.toBoolean()) {
                newPassword = createRandomPassword()
            } else {
                newPassword = defaultpass
            }
        }
        val passwordChangeRequest = PasswordChangeRequest().password(newPassword)
        storageGridUsersApi
            .orgUsersIdChangePasswordPost(userId.toString(), passwordChangeRequest)
            .awaitFirstOrNull()

        return newPassword
    }

    private fun createRandomPassword(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        val length = 10 + kotlin.random.Random.nextInt(6)
        val randomString = (1..length)
            .map { i -> kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
        return randomString
    }

    override suspend fun provideS3AccessKeys(
        userId: UUID,
        token: String
    ): S3AccessKeys {
        storageGridS3Api.apiClient.setBearerToken(token)

        val postAccessKeyResponse = storageGridS3Api
            .orgUsersUserIdS3AccessKeysPost(userId.toString(), InlineObject1())
            .awaitFirst()
        if (postAccessKeyResponse.status === PostAccessKeyResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid users api returned an error on orgUsersUserIdS3AccessKeysPost"
            )
        }
        if (postAccessKeyResponse.data.accessKey == null || postAccessKeyResponse.data.secretAccessKey == null) {
            throw FergusException("Did not get S3 access keys")
        }

        return S3AccessKeys(postAccessKeyResponse.data.accessKey!!, postAccessKeyResponse.data.secretAccessKey!!)
    }
}

interface StorageGridService {
    suspend fun authorize(authorizationPayload: AuthorizationPayload): String = integrationDisabled()

    suspend fun provideBucket(bucketName: String, token: String): String = integrationDisabled()

    suspend fun provideGroup(bucketName: String, path: String, access: List<Access>, token: String): UUID =
        integrationDisabled()

    suspend fun provideUser(userName: String, groupId: UUID, token: String): UUID =
        integrationDisabled()

    suspend fun assignPasswordToUser(userId: UUID, password: String?, token: String): String =
        integrationDisabled()

    suspend fun provideS3AccessKeys(userId: UUID, token: String): S3AccessKeys =
        integrationDisabled()

    private fun integrationDisabled(): Nothing =
        throw FergusException("StorageGrid integration is disabled for this environment")
}

fun AuthorizationPayload.toAuthorizeInput(): Credentials = Credentials()
    .accountId(accountId)
    .username(username)
    .password(password)

data class S3AccessKeys(
    val s3accesskey: String,
    val s3secretaccesskey: String,
)
