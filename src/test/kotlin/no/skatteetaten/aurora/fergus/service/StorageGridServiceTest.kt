package no.skatteetaten.aurora.fergus.service

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.skatteetaten.aurora.fergus.controllers.Access
import no.skatteetaten.aurora.fergus.controllers.AuthorizationPayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.openapitools.client.ApiClient
import org.openapitools.client.api.AuthApi
import org.openapitools.client.api.ContainersApi
import org.openapitools.client.api.GroupsApi
import org.openapitools.client.api.S3Api
import org.openapitools.client.api.UsersApi
import org.openapitools.client.model.AuthorizeResponse
import org.openapitools.client.model.Container
import org.openapitools.client.model.ContainerCreate
import org.openapitools.client.model.ContainerCreateResponse
import org.openapitools.client.model.ContainerListResponse
import org.openapitools.client.model.GetPatchPostPutGroupResponse
import org.openapitools.client.model.GetPatchPostPutUserResponse
import org.openapitools.client.model.Group
import org.openapitools.client.model.ListGroupsResponse
import org.openapitools.client.model.ListUsersResponse
import org.openapitools.client.model.PostAccessKeyResponse
import org.openapitools.client.model.S3AccessKeyWithSecrets
import org.openapitools.client.model.User
import reactor.core.publisher.Mono
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageGridServiceTest {

    private val storageGridAuthApi: AuthApi = mockk()
    private val storageGridContainersApi: ContainersApi = mockk()
    private val storageGridGroupsApi: GroupsApi = mockk()
    private val storageGridUsersApi: UsersApi = mockk()
    private val storageGridS3Api: S3Api = mockk()
    private val apiClient: ApiClient = mockk(null, true)

    private val storageGridService = StorageGridServiceReactive(
        "false",
        "defaultpassword",
        storageGridAuthApi,
        storageGridContainersApi,
        storageGridGroupsApi,
        storageGridUsersApi,
        storageGridS3Api
    )

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    @Test
    fun authorizeHappyTest() {
        val mockToken = "testtoken"
        val authorizeResponse = AuthorizeResponse().status(AuthorizeResponse.StatusEnum.SUCCESS).data(mockToken)

        coEvery {
            storageGridAuthApi.authorizePost(any())
        } returns Mono.just(authorizeResponse)

        val body = AuthorizationPayload(
            accountId = "testAccount",
            username = "testUser",
            password = "testPass",
        )
        runBlocking {
            val token = storageGridService.authorize(body)

            assertThat(token).isEqualTo(mockToken)
        }
    }

    @Test
    fun provideBucketHappyTest() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"

        every {
            storageGridContainersApi.getApiClient()
        } returns apiClient

        val containerListResponse = ContainerListResponse()
            .status(ContainerListResponse.StatusEnum.SUCCESS)
            .data(listOf<Container>())
        coEvery {
            storageGridContainersApi.orgContainersGet(any())
        } returns Mono.just(containerListResponse)

        val containerCreateResponse = ContainerCreateResponse()
            .status(ContainerCreateResponse.StatusEnum.SUCCESS)
            .data(
                ContainerCreate().name(bucketName)
            )
        coEvery {
            storageGridContainersApi.orgContainersPost(any())
        } returns Mono.just(containerCreateResponse)

        runBlocking {
            val bucketNameResponse = storageGridService.provideBucket(bucketName, mockToken)

            assertThat(bucketNameResponse).isEqualTo(bucketName)
        }
    }

    @Test
    fun provideGroupHappyTest() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"
        val path = "testpath"
        val access = listOf<Access>(Access.READ, Access.WRITE)
        val groupId = UUID.randomUUID()

        every {
            storageGridGroupsApi.getApiClient()
        } returns apiClient

        val listGroupsResponse = ListGroupsResponse()
            .status(ListGroupsResponse.StatusEnum.SUCCESS)
            .data(listOf<Group>())
        coEvery {
            storageGridGroupsApi.orgGroupsGet(any(), any(), any(), any(), any())
        } returns Mono.just(listGroupsResponse)

        val groupCreateResponse = GetPatchPostPutGroupResponse()
            .status(GetPatchPostPutGroupResponse.StatusEnum.SUCCESS)
            .data(
                Group()
                    .displayName("$bucketName-$path-RW")
                    .uniqueName("group/$bucketName-$path-RW")
                    .id(groupId.toString())
            )
        coEvery {
            storageGridGroupsApi.orgGroupsPost(any())
        } returns Mono.just(groupCreateResponse)

        runBlocking {
            val groupIdResponse = storageGridService.provideGroup(bucketName, path, access, mockToken)

            assertThat(groupIdResponse).isEqualTo(groupId)
        }
    }

    @Test
    fun provideUserHappyTest() {
        val mockToken = "testtoken"
        val userName = "testUser"
        val groupId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        every {
            storageGridUsersApi.getApiClient()
        } returns apiClient

        val listUsersResponse = ListUsersResponse()
            .status(ListUsersResponse.StatusEnum.SUCCESS)
            .data(listOf<User>())
        coEvery {
            storageGridUsersApi.orgUsersGet(any(), any(), any(), any(), any())
        } returns Mono.just(listUsersResponse)

        val userCreateResponse = GetPatchPostPutUserResponse()
            .status(GetPatchPostPutUserResponse.StatusEnum.SUCCESS)
            .data(
                User()
                    .fullName(userName)
                    .uniqueName("user/$userName")
                    .id(userId)
            )
        coEvery {
            storageGridUsersApi.orgUsersPost(any())
        } returns Mono.just(userCreateResponse)

        runBlocking {
            val userIdResponse = storageGridService.provideUser(userName, groupId, mockToken)

            assertThat(userIdResponse).isEqualTo(userId)
        }
    }

    @Test
    fun assignPasswordToUserHappyTest() {
        val mockToken = "testtoken"
        val userId = UUID.randomUUID()
        val password = "password"

        every {
            storageGridUsersApi.getApiClient()
        } returns apiClient

        coEvery {
            storageGridUsersApi.orgUsersIdChangePasswordPost(userId.toString(), any())
        } returns Mono.empty()

        runBlocking {
            val passwordResponse = storageGridService.assignPasswordToUser(userId, password, mockToken)

            assertThat(passwordResponse).isEqualTo(password)
        }
    }

    @Test
    fun assignDefaultPasswordToUserHappyTest() {
        val mockToken = "testtoken"
        val userId = UUID.randomUUID()

        every {
            storageGridUsersApi.getApiClient()
        } returns apiClient

        coEvery {
            storageGridUsersApi.orgUsersIdChangePasswordPost(userId.toString(), any())
        } returns Mono.empty()

        runBlocking {
            val passwordResponse = storageGridService.assignPasswordToUser(userId, null, mockToken)

            assertThat(passwordResponse).isEqualTo(storageGridService.defaultpass)
        }
    }

    @Test
    fun provideS3AccessKeysHappyTest() {
        val mockToken = "testtoken"
        val userId = UUID.randomUUID()
        val accessKey = "access1234567"
        val secretAccessKey = "secretOhSoSecret"

        every {
            storageGridS3Api.getApiClient()
        } returns apiClient

        val s3AccessKeyResponse = PostAccessKeyResponse()
            .status(PostAccessKeyResponse.StatusEnum.SUCCESS)
            .data(
                S3AccessKeyWithSecrets()
                    .id("abcABC_01234-0123456789abcABCabc0123456789==")
                    .userUUID(userId.toString())
                    .accessKey(accessKey)
                    .secretAccessKey(secretAccessKey)
            )
        coEvery {
            storageGridS3Api.orgUsersUserIdS3AccessKeysPost(userId.toString(), any())
        } returns Mono.just(s3AccessKeyResponse)

        runBlocking {
            val s3AccessKeys = storageGridService.provideS3AccessKeys(userId, mockToken)

            assertThat(s3AccessKeys.s3accesskey).isEqualTo(accessKey)
            assertThat(s3AccessKeys.s3secretaccesskey).isEqualTo(secretAccessKey)
        }
    }
}
