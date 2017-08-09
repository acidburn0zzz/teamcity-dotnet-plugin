package jetbrains.buildServer.dotnet

interface PackageVersionParser {
    fun tryParse(version: String): PackageVersion?
}