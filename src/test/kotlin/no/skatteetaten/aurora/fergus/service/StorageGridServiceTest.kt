package no.skatteetaten.aurora.fergus.service

import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
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
import org.openapitools.client.model.PostAccessKeyResponse
import org.openapitools.client.model.S3AccessKeyWithSecrets
import org.openapitools.client.model.User
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StorageGridServiceTest {

    private val storageGridAuthApi: AuthApi = mockk()
    private val storageGridContainersApi: ContainersApi = mockk()
    private val storageGridGroupsApi: GroupsApi = mockk()
    private val storageGridUsersApi: UsersApi = mockk()
    private val storageGridS3Api: S3Api = mockk()
    private val storageGridApiFactory: StorageGridApiFactory = mockk()
    private val apiClient: ApiClient = mockk(null, true)

    private val storageGridService = StorageGridServiceReactive(
        "false",
        "defaultpassword",
        storageGridApiFactory
    )

    @BeforeEach
    fun init() {
        clearAllMocks()
    }

    @Test
    fun authorizeHappyTest() {
        val mockToken = "testtoken"
        val authorizeResponse = AuthorizeResponse().status(AuthorizeResponse.StatusEnum.SUCCESS).data(mockToken)

        every {
            storageGridApiFactory.storageGridAuthApi()
        } returns storageGridAuthApi

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
    fun `Should throw error when authorize fails`() {
        val mockToken = "testtoken"
        val authorizeResponse = AuthorizeResponse()
            .status(AuthorizeResponse.StatusEnum.ERROR)

        every {
            storageGridApiFactory.storageGridAuthApi()
        } returns storageGridAuthApi

        coEvery {
            storageGridAuthApi.authorizePost(any())
        } returns Mono.just(authorizeResponse)

        val payload = AuthorizationPayload(
            accountId = "wrongAccount",
            username = "wrongUser",
            password = "wrongPass",
        )

        runBlocking {
            assertk.assertThat { storageGridService.authorize(payload) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid auth api returned an error on authorizePost")
        }
    }

    @Test
    fun provideBucketHappyTest() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"

        every {
            storageGridApiFactory.storageGridContainersApi(any())
        } returns storageGridContainersApi

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
    fun `Should throw error on provideBucket when storageGridContainersApi_orgContainersGet fails`() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"

        every {
            storageGridApiFactory.storageGridContainersApi(any())
        } returns storageGridContainersApi

        val containerListResponse = ContainerListResponse()
            .status(ContainerListResponse.StatusEnum.ERROR)
        coEvery {
            storageGridContainersApi.orgContainersGet(any())
        } returns Mono.just(containerListResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideBucket(bucketName, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid containers api returned an error on orgContainersGet")
        }
    }

    @Test
    fun `Should throw error on provideBucket when storageGridContainersApi_orgContainersPost fails`() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"

        every {
            storageGridApiFactory.storageGridContainersApi(any())
        } returns storageGridContainersApi

        val containerListResponse = ContainerListResponse()
            .status(ContainerListResponse.StatusEnum.SUCCESS)
            .data(listOf<Container>())
        coEvery {
            storageGridContainersApi.orgContainersGet(any())
        } returns Mono.just(containerListResponse)

        val containerCreateResponse = ContainerCreateResponse()
            .status(ContainerCreateResponse.StatusEnum.ERROR)
        coEvery {
            storageGridContainersApi.orgContainersPost(any())
        } returns Mono.just(containerCreateResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideBucket(bucketName, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid containers api returned an error on orgContainersPost")
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
            storageGridApiFactory.storageGridGroupsApi(any())
        } returns storageGridGroupsApi

        val getGroupResponse = GetPatchPostPutGroupResponse()
            .data(Group())
            .status(GetPatchPostPutGroupResponse.StatusEnum.SUCCESS)
        coEvery {
            storageGridGroupsApi.orgGroupsGroupShortNameGet(any())
        } returns Mono.just(getGroupResponse)

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
    fun `Should throw error on provideGroup when storageGridGroupsApi_orgGroupsGroupShortNameGet fails`() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"
        val path = "testpath"
        val access = listOf<Access>(Access.READ, Access.WRITE)

        every {
            storageGridApiFactory.storageGridGroupsApi(any())
        } returns storageGridGroupsApi

        val getGroupResponse = GetPatchPostPutGroupResponse()
            .status(GetPatchPostPutGroupResponse.StatusEnum.ERROR)
        coEvery {
            storageGridGroupsApi.orgGroupsGroupShortNameGet(any())
        } returns Mono.just(getGroupResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideGroup(bucketName, path, access, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid groups api returned an error on orgGroupsGroupShortNameGet")
        }
    }

    @Test
    fun `Should throw error on provideGroup when storageGridGroupsApi_orgGroupsPost fails`() {
        val mockToken = "testtoken"
        val bucketName = "bucket-1"
        val path = "testpath"
        val access = listOf<Access>(Access.READ, Access.WRITE)

        every {
            storageGridApiFactory.storageGridGroupsApi(any())
        } returns storageGridGroupsApi

        val getGroupResponse = GetPatchPostPutGroupResponse()
            .data(Group())
            .status(GetPatchPostPutGroupResponse.StatusEnum.SUCCESS)
        coEvery {
            storageGridGroupsApi.orgGroupsGroupShortNameGet(any())
        } returns Mono.just(getGroupResponse)

        val groupCreateResponse = GetPatchPostPutGroupResponse()
            .status(GetPatchPostPutGroupResponse.StatusEnum.ERROR)
        coEvery {
            storageGridGroupsApi.orgGroupsPost(any())
        } returns Mono.just(groupCreateResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideGroup(bucketName, path, access, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid groups api returned an error on orgGroupsPost")
        }
    }

    @Test
    fun provideUserHappyTest() {
        val mockToken = "testtoken"
        val userName = "testUser"
        val groupId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        every {
            storageGridApiFactory.storageGridUsersApi(any())
        } returns storageGridUsersApi

        val getUserResponse = GetPatchPostPutUserResponse()
            .data(User())
            .status(GetPatchPostPutUserResponse.StatusEnum.SUCCESS)
        coEvery {
            storageGridUsersApi.orgUsersUserShortNameGet(any())
        } returns Mono.just(getUserResponse)

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
    fun `Should throw error on provideUser when storageGridUsersApi_orgUsersUserShortNameGet fails`() {
        val mockToken = "testtoken"
        val userName = "testUser"
        val groupId = UUID.randomUUID()

        every {
            storageGridApiFactory.storageGridUsersApi(any())
        } returns storageGridUsersApi

        val getUserResponse = GetPatchPostPutUserResponse()
            .status(GetPatchPostPutUserResponse.StatusEnum.ERROR)
        coEvery {
            storageGridUsersApi.orgUsersUserShortNameGet(any())
        } returns Mono.just(getUserResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideUser(userName, groupId, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid users api returned an error on orgUsersUserShortNameGet")
        }
    }

    @Test
    fun `Should throw error on provideUser when storageGridUsersApi_orgUsersPost fails`() {
        val mockToken = "testtoken"
        val userName = "testUser"
        val groupId = UUID.randomUUID()

        every {
            storageGridApiFactory.storageGridUsersApi(any())
        } returns storageGridUsersApi

        val getUserResponse = GetPatchPostPutUserResponse()
            .data(User())
            .status(GetPatchPostPutUserResponse.StatusEnum.SUCCESS)
        coEvery {
            storageGridUsersApi.orgUsersUserShortNameGet(any())
        } returns Mono.just(getUserResponse)

        val userCreateResponse = GetPatchPostPutUserResponse()
            .status(GetPatchPostPutUserResponse.StatusEnum.ERROR)
        coEvery {
            storageGridUsersApi.orgUsersPost(any())
        } returns Mono.just(userCreateResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideUser(userName, groupId, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid users api returned an error on orgUsersPost")
        }
    }

    @Test
    fun assignPasswordToUserHappyTest() {
        val mockToken = "testtoken"
        val userId = UUID.randomUUID()
        val password = "password"

        every {
            storageGridApiFactory.storageGridUsersApi(any())
        } returns storageGridUsersApi

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
            storageGridApiFactory.storageGridUsersApi(any())
        } returns storageGridUsersApi

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
            storageGridApiFactory.storageGridS3Api(any())
        } returns storageGridS3Api

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

    @Test
    fun `Should throw error on provideS3AccessKeys when storageGridS3Api_orgUsersUserIdS3AccessKeysPost fails`() {
        val mockToken = "testtoken"
        val userId = UUID.randomUUID()

        every {
            storageGridApiFactory.storageGridS3Api(any())
        } returns storageGridS3Api

        val s3AccessKeyResponse = PostAccessKeyResponse()
            .status(PostAccessKeyResponse.StatusEnum.ERROR)
        coEvery {
            storageGridS3Api.orgUsersUserIdS3AccessKeysPost(userId.toString(), any())
        } returns Mono.just(s3AccessKeyResponse)

        runBlocking {
            assertk.assertThat { storageGridService.provideS3AccessKeys(userId, mockToken) }
                .isFailure()
                .isInstanceOf(ResponseStatusException::class)
                .messageContains("The Storagegrid S3 api returned an error on orgUsersUserIdS3AccessKeysPost")
        }
    }
}
