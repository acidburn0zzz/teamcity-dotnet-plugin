package jetbrains.buildServer.dotnet

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.tools.*
import jetbrains.buildServer.tools.available.AvailableToolsFetcher
import jetbrains.buildServer.tools.available.AvailableToolsStateImpl
import jetbrains.buildServer.tools.available.FetchToolsPolicy
import jetbrains.buildServer.tools.utils.URLDownloader
import jetbrains.buildServer.util.ArchiveUtil
import jetbrains.buildServer.util.StringUtil
import jetbrains.buildServer.util.TimeService
import java.io.File
import java.io.FileFilter
import java.util.*

class DotnetToolProviderAdapter(
        private val _timeService: TimeService,
        private val _availableToolsFetcher: AvailableToolsFetcher,
        private val _packageVersionParser: PackageVersionParser): ServerToolProviderAdapter() {

    val _availableTools: AvailableToolsStateImpl;

    init {
        _availableTools = AvailableToolsStateImpl(_timeService, Collections.singletonList(_availableToolsFetcher));
    }

    override fun getType(): ToolType {
        return ToolType;
    }

    override fun getAvailableToolVersions(): MutableCollection<out ToolVersion> {
        return super.getAvailableToolVersions()
                .plus(_availableTools.getAvailable(FetchToolsPolicy.FetchNew).getFetchedTools())
                .toMutableList();
    }

    override fun tryGetPackageVersion(toolPackage: File): GetPackageVersionResult {
        val packageVersion = _packageVersionParser.tryParse(toolPackage.name)
        if (packageVersion == null) {
            return GetPackageVersionResult.error("Failed to get version of " + toolPackage)
        }

        return GetPackageVersionResult.version(DotnetToolVersion(packageVersion.toString()))
    }

    override fun fetchToolPackage(toolVersion: ToolVersion, targetDirectory: File): File {
        LOG.info("Start installing package " + toolVersion.displayName)
        val downloadableNuGetTool = _availableTools.getAvailable(FetchToolsPolicy.ReturnCached).getFetchedTools()
                .filter { it.version == toolVersion.version }
                .firstOrNull()
                ?: throw ToolException("Failed to find package " + toolVersion)

        LOG.info("Downloading package from: " + downloadableNuGetTool.downloadUrl)
        val location = File(targetDirectory, downloadableNuGetTool.destinationFileName)
        try {
            URLDownloader.download(downloadableNuGetTool.downloadUrl, location)
        } catch (e: Throwable) {
            throw ToolException("Failed to download package " + toolVersion + " to " + location + e.message, e)
        }

        LOG.debug("Successfully downloaded package $toolVersion to $location")
        return location
    }

    override fun unpackToolPackage(toolPackage: File, targetDirectory: File) {
        var pathPrefix = ""
        if (NUGET_PACKAGE_FILE_FILTER.accept(toolPackage) && _packageVersionParser.tryParse(toolPackage.name) != null) {
            pathPrefix = DotnetConstants.PACKAGE_BINARY_NUPKG_PATH + "/"
        }

        if (!ArchiveUtil.unpackZip(toolPackage, pathPrefix, targetDirectory)) {
            throw ToolException("Failed to unpack package $toolPackage to $targetDirectory")
        }
    }

    private class DotnetToolTypeAdapter : ToolTypeAdapter() {
        override fun getType(): String {
            return DotnetConstants.PACKAGE_TYPE;
        }

        override fun getDisplayName(): String {
            return DotnetConstants.PACKAGE_TOOL_TYPE_NAME
        }

        override fun getDescription(): String? {
            return "Is used in the TeamCity NUnit build runner to run tests."
        }

        override fun getShortDisplayName(): String {
            return DotnetConstants.PACKAGE_SHORT_TOOL_TYPE_NAME
        }

        override fun getTargetFileDisplayName(): String {
            return DotnetConstants.PACKAGE_TARGET_FILE_DISPLAY_NAME
        }

        override fun isSupportDownload(): Boolean {
            return true
        }

        override fun getToolSiteUrl(): String {
            return "https://github.com/JetBrains/TeamCity.MSBuild.Logger/"
        }

        override fun getToolLicenseUrl(): String {
            return "https://github.com/JetBrains/TeamCity.MSBuild.Logger/blob/master/LICENSE"
        }

        override fun getTeamCityHelpFile(): String {
            return "DotnetIntegration"
        }

        override fun getValidPackageDescription(): String? {
            return "Specify the path to a " + displayName + " (.nupkg).\n" +
                    "<br/>Download <em>TeamCity.Dotnet.Integration.&lt;VERSION&gt;.nupkg</em> from\n" +
                    "<a href=\"https://www.nuget.org/packages/TeamCity.Dotnet.Integration/\" target=\"_blank\">www.nuget.org</a>"
        }
    }

    private inner class DotnetToolVersion internal constructor(version: String)
        : SimpleToolVersion(
            ToolType,
            version,
            ToolVersionIdHelper.getToolId(DotnetConstants.PACKAGE_TYPE, version))

    companion object {
        internal val ToolType: ToolTypeAdapter = DotnetToolTypeAdapter()
        private val LOG: Logger = Logger.getInstance(DotnetToolProviderAdapter::class.java.name)
        private val NUGET_PACKAGE_FILE_FILTER = FileFilter { pathname ->
            val name = pathname.name
            pathname.isFile && name.startsWith(DotnetConstants.PACKAGE_TYPE) && name.endsWith(DotnetConstants.PACKAGE_NUGET_EXTENSION)
        }
    }
}