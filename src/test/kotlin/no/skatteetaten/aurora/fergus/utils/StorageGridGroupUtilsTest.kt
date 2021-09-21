package no.skatteetaten.aurora.fergus.utils

import org.junit.jupiter.api.Test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan

class StorageGridGroupUtilsTest {
    val path = "5e0a376d-b834-44b2-868f-40884d23ee17"

    @Test
    fun `should generate a groupName with $bucketName-$groupNamePostfix if length is one off max length`() {
        val bucketName = "aurora-utv-aurora-utvikling"
        val groupNamePostfix = "RWD"
        val groupName = StorageGridGroupUtils.ensureShortDisplayGroupName(path, bucketName, groupNamePostfix)

        val expected = "$bucketName-$groupNamePostfix"
        assertThat(groupName).isEqualTo(expected)
        assertThat(groupName.length).isEqualTo(31)
    }

    @Test
    fun `should generate a groupName with only bucketName since it has displayName max length`() {
        val bucketName = "aurora-utv-aurora-test-utvikling"
        val groupNamePostfix = "RWD"
        val groupName = StorageGridGroupUtils.ensureShortDisplayGroupName(path, bucketName, groupNamePostfix)

        assertThat(bucketName.length).isEqualTo(32)
        assertThat(groupName).isEqualTo(bucketName)
        assertThat(groupName.length).isEqualTo(StorageGridGroupUtils.GROUP_DISPLAYNAME_MAXLENGTH)
    }

    @Test
    fun `should generate a groupName with bucketName, groupNamePostfix and a part of path`() {
        val bucketName = "aurora-utv"
        val groupNamePostfix = "RWD"
        val groupName = StorageGridGroupUtils.ensureShortDisplayGroupName(path, bucketName, groupNamePostfix)

        assertThat(groupName).isEqualTo("aurora-utv-5e0a376d-b834-44b-RWD")
        assertThat(groupName.length).isEqualTo(StorageGridGroupUtils.GROUP_DISPLAYNAME_MAXLENGTH)
    }

    @Test
    fun `should generate a groupName including bucketName, path and groupNamePostfix`() {
        val newPath = "abc"
        val bucketName = "aurora-utv"
        val groupNamePostfix = "RWD"
        val groupName = StorageGridGroupUtils.ensureShortDisplayGroupName(newPath, bucketName, groupNamePostfix)

        assertThat(groupName).isEqualTo("aurora-utv-abc-RWD")
        assertThat(groupName.length).isLessThan(StorageGridGroupUtils.GROUP_DISPLAYNAME_MAXLENGTH)
    }
}
