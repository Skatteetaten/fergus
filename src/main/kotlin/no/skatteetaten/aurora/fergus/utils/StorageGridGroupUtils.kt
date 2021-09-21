package no.skatteetaten.aurora.fergus.utils

object StorageGridGroupUtils {
    const val GROUP_DISPLAYNAME_MAXLENGTH = 32
    fun ensureShortDisplayGroupName(path: String, bucketName: String, groupNamePostfix: String): String {
        val groupFullPath = "$bucketName-$path-$groupNamePostfix"
        if (groupFullPath.length <= GROUP_DISPLAYNAME_MAXLENGTH) {
            return groupFullPath
        }

        val groupNoPath = "$bucketName-$groupNamePostfix"
        // Minus 1 because there will be an extra separator added when including shortPath.
        val pathPartLength = GROUP_DISPLAYNAME_MAXLENGTH - 1 - groupNoPath.length
        return if (pathPartLength > 0) {
            val shortPath = path.substring(0, pathPartLength)
            "$bucketName-$shortPath-$groupNamePostfix"
        } else if (groupNoPath.length > GROUP_DISPLAYNAME_MAXLENGTH) {
            groupNoPath.substring(0, GROUP_DISPLAYNAME_MAXLENGTH)
        } else {
            groupNoPath
        }
    }
}
