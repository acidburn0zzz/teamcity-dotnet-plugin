package jetbrains.buildServer.dotnet

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.nuget.feedReader.NuGetFeedClient
import jetbrains.buildServer.nuget.feedReader.NuGetFeedReader
import jetbrains.buildServer.nuget.feedReader.NuGetPackage
import jetbrains.buildServer.tools.ToolType
import jetbrains.buildServer.tools.available.AvailableToolsFetcher
import jetbrains.buildServer.tools.available.DownloadableToolVersion
import jetbrains.buildServer.tools.available.FetchAvailableToolsResult
import java.io.IOException

class DotnetAvailableToolsFetcher(
        private val _feed: NuGetFeedClient,
        private val _reader: NuGetFeedReader,
        private val _packageVersionParser: PackageVersionParser
) : AvailableToolsFetcher {

    override fun fetchAvailable(): FetchAvailableToolsResult {
        try {
            return FetchAvailableToolsResult.createSuccessful(
                    _reader.queryPackageVersions(_feed, DotnetConstants.PACKAGE_FEED_URL, DotnetConstants.PACKAGE_TYPE)
                    .filter { _packageVersionParser.tryParse(it.packageVersion) != null }
                    .map { DotnetDownloadableToolVersion(it) })
        }
        catch (e: IOException) {
            LOG.debug(e)
            return FetchAvailableToolsResult.createError("Failed to fetch versions from: " + DotnetConstants.PACKAGE_FEED_URL, e)
        }
    }

    companion object {
        private val LOG: Logger = Logger.getInstance(DotnetAvailableToolsFetcher::class.java.name);
    }

    private class DotnetDownloadableToolVersion(
            private val _package: NuGetPackage): DownloadableToolVersion {
        override fun getDownloadUrl(): String {
            return _package.downloadUrl
        }

        override fun getDestinationFileName(): String {
            return _package.packageId + "." + _package.packageVersion + DotnetConstants.PACKAGE_NUGET_EXTENSION
        }

        override fun getType(): ToolType {
            return DotnetToolProviderAdapter.ToolType
        }

        override fun getVersion(): String {
            return _package.packageVersion
        }

        override fun getId(): String {
            return DotnetToolProviderAdapter.ToolType.getType() + "." + _package.packageVersion
        }

        override fun getDisplayName(): String {
            return DotnetToolProviderAdapter.ToolType.getType() + version
        }
    }
}