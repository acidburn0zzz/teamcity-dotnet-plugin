package jetbrains.buildServer.dotnet

class PackageVersionParserImpl : PackageVersionParser {
    override fun tryParse(version: String): PackageVersion? {
        val matcher = _packageVersionPattern.matcher(version)
        if(!matcher.find() || matcher.groupCount() < 5) {
            return null
        }

        return PackageVersion(
                matcher.group("major").toInt(),
                matcher.group("minor").toInt(),
                matcher.group("build").toInt(),
                matcher.group("buildName") ?: "");
    }

    companion object {
        val _packageVersionPattern= Regex("(^[a-z.]+\\.|^)(?<major>[\\d]+)\\.(?<minor>[\\d]+)\\.(?<build>[\\d]+)(-(?<buildName>\\w+)|)(.nupkg|)$", RegexOption.IGNORE_CASE).toPattern()
    }
}