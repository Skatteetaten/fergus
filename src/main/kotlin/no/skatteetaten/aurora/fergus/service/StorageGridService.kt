package no.skatteetaten.aurora.fergus.service

import java.util.UUID
import org.openapitools.client.api.GroupsApi
import org.openapitools.client.api.UsersApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.ContainerCreate
import org.openapitools.client.model.ContainerCreateResponse
import org.openapitools.client.model.ContainerListResponse
import org.openapitools.client.model.Credentials
import org.openapitools.client.model.GetPatchPostPutGroupResponse
import org.openapitools.client.model.GetPatchPostPutUserResponse
import org.openapitools.client.model.InlineObject1
import org.openapitools.client.model.PasswordChangeRequest
import org.openapitools.client.model.PatchUserRequest
import org.openapitools.client.model.Policies
import org.openapitools.client.model.PolicyS3
import org.openapitools.client.model.PolicyS3Statement
import org.openapitools.client.model.PostAccessKeyResponse
import org.openapitools.client.model.PostGroupRequest
import org.openapitools.client.model.PostUserRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import mu.KotlinLogging
import no.skatteetaten.aurora.fergus.controllers.Access
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import no.skatteetaten.aurora.fergus.error.FergusException
import no.skatteetaten.aurora.fergus.utils.StorageGridGroupUtils

private val logger = KotlinLogging.logger {}

@Service
class StorageGridServiceReactive(
    @Value("\${fergus.provision.user.randompass}") val randompass: String,
    @Value("\${fergus.provision.user.defaultpass}") val defaultpass: String,
    @Autowired private val storageGridApiFactory: StorageGridApiFactory,
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
                "The Storagegrid auth api returned an error on authorizePost"
            )
        }
        return response.data // Returns authorization token
    }

    override suspend fun provideBucket(
        bucketName: String,
        bucketRegion: String,
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
            val containerCreate = ContainerCreate().name(bucketName).region(bucketRegion)
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
        val storageGridGroupsApi = storageGridApiFactory.storageGridGroupsApi(token)
        val shortGroupName = createShortGroupName(bucketName, path, access)
        val uniqueGroupName = "group/$shortGroupName"
        val displayGroupName = createDisplayGroupName(bucketName, path, access)

        // Get group by shortname
        val getGroupResponse = try {
            storageGridGroupsApi
                .orgGroupsGroupShortNameGet(shortGroupName)
                .awaitSingle()
        } catch (wcre: WebClientResponseException) {
            if (wcre.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw FergusException("Error trying to get existing group", wcre)
            }
        }
        if (getGroupResponse != null && getGroupResponse.status == GetPatchPostPutGroupResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid groups api returned an error on orgGroupsGroupShortNameGet"
            )
        }

        // Check if uniqueGroupName already exists, if not, create with policy
        val groupId = if (getGroupResponse != null && getGroupResponse.data.uniqueName.equals(uniqueGroupName)) {
            // Find id for matching group
            getGroupResponse.data.id
        } else {
            createGroupWithPolicy(displayGroupName, uniqueGroupName, bucketName, path, access, storageGridGroupsApi)
        }

        return UUID.fromString(groupId)
    }

    private suspend fun createGroupWithPolicy(
        displayGroupName: String,
        uniqueGroupName: String,
        bucketName: String,
        path: String,
        access: List<Access>,
        storageGridGroupsApi: GroupsApi
    ): String? {
        val bucketStatement = PolicyS3Statement()
            .effect(PolicyS3Statement.EffectEnum.ALLOW)
            .addActionItem("s3:ListBucket")
            .addActionItem("s3:GetBucketLocation")
            .addResourceItem("arn:aws:s3:::$bucketName")
        val objectActionStatement = createS3ObjectActionStatement(bucketName, path, access)

        val postGroupRequest = PostGroupRequest()
            .displayName(displayGroupName)
            .policies(
                Policies().s3(
                    PolicyS3()
                        .addStatementItem(bucketStatement)
                        .addStatementItem(objectActionStatement)
                )
            )
            .uniqueName(uniqueGroupName)
        try {
            val groupCreateResponse = storageGridGroupsApi
                .orgGroupsPost(postGroupRequest)
                .awaitSingle()
            if (groupCreateResponse.status == GetPatchPostPutGroupResponse.StatusEnum.ERROR) {
                throw ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The Storagegrid groups api returned an error on orgGroupsPost"
                )
            }
            return groupCreateResponse.data.id
        } catch (wcre: WebClientResponseException) {
            throw FergusException("Error trying to get create group. Details: ${wcre.responseBodyAsString}", wcre)
        }
    }

    private fun createShortGroupName(
        bucketName: String,
        path: String,
        access: List<Access>
    ): String {
        val groupNamePostfix = createGroupAccessPostfix(access)

        return "$bucketName-$path-$groupNamePostfix"
    }

    private fun createDisplayGroupName(
        bucketName: String,
        path: String,
        access: List<Access>
    ): String {
        val groupNamePostfix = createGroupAccessPostfix(access)

        return StorageGridGroupUtils.ensureShortDisplayGroupName(path, bucketName, groupNamePostfix)
    }

    private fun createGroupAccessPostfix(access: List<Access>): String {
        var groupNamePostfix = ""
        if (access.isNotEmpty()) {
            access.forEach { groupNamePostfix += it.name.take(1) }
        } else groupNamePostfix = "RWD"
        return groupNamePostfix
    }

    private fun createS3ObjectActionStatement(
        bucketName: String,
        path: String,
        access: List<Access>
    ): PolicyS3Statement {
        val objectActionStatement = PolicyS3Statement()
            .effect(PolicyS3Statement.EffectEnum.ALLOW)
            .addResourceItem("arn:aws:s3:::$bucketName/$path/*")
        if (access.isEmpty()) {
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
        val storageGridUsersApi = storageGridApiFactory.storageGridUsersApi(token)
        // Get user by shortname
        val getUserResponse = try {
            storageGridUsersApi
                .orgUsersUserShortNameGet(userName)
                .awaitSingle()
        } catch (wcre: WebClientResponseException) {
            if (wcre.statusCode == HttpStatus.NOT_FOUND) {
                null
            } else {
                throw FergusException("Error trying to get existing user", wcre)
            }
        }
        if (getUserResponse != null && getUserResponse.status == GetPatchPostPutUserResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid users api returned an error on orgUsersUserShortNameGet"
            )
        }

        // Check if userName already exists, if not, create
        val userId = if (getUserResponse != null && getUserResponse.data.uniqueName == "user/$userName") {
            // Update group membership for user
            val uId = getUserResponse.data.id
            val existingGroupIds = getUserResponse.data.memberOf
            updateUserGroupMember(userName, groupId, uId, existingGroupIds, storageGridUsersApi)
            uId
        } else {
            createUser(userName, groupId, storageGridUsersApi)
        }

        if (userId == null) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Could not find or create requested user"
            )
        }

        return userId
    }

    private suspend fun createUser(userName: String, groupId: UUID, storageGridUsersApi: UsersApi): UUID? {
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

    private suspend fun updateUserGroupMember(
        userName: String,
        groupId: UUID,
        userId: UUID?,
        existingGroupIds: List<UUID>?,
        storageGridUsersApi: UsersApi
    ) {
        val patchUserRequest = PatchUserRequest().fullName(userName)
        aggregateGroupIdsIntoRequest(existingGroupIds, groupId, patchUserRequest)
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

    private fun aggregateGroupIdsIntoRequest(
        existingGroupIds: List<UUID>?,
        groupId: UUID,
        patchUserRequest: PatchUserRequest
    ) {
        val groupIdSet: MutableSet<UUID> = existingGroupIds?.toMutableSet() ?: mutableSetOf<UUID>()
        groupIdSet.add(groupId)
        for (gId in groupIdSet) {
            patchUserRequest.addMemberOfItem(gId)
        }
    }

    override suspend fun assignPasswordToUser(
        userId: UUID,
        password: String?,
        token: String
    ): String {
        val storageGridUsersApi = storageGridApiFactory.storageGridUsersApi(token)
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
        val storageGridS3Api = storageGridApiFactory.storageGridS3Api(token)
        val postAccessKeyResponse = storageGridS3Api
            .orgUsersUserIdS3AccessKeysPost(userId.toString(), InlineObject1())
            .awaitFirst()
        if (postAccessKeyResponse.status === PostAccessKeyResponse.StatusEnum.ERROR) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The Storagegrid S3 api returned an error on orgUsersUserIdS3AccessKeysPost"
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

    suspend fun provideBucket(bucketName: String, bucketRegion: String, token: String): String = integrationDisabled()

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
