package jetbrains.buildServer.dotnet

import jetbrains.buildServer.nuget.feedReader.NuGetFeedClient
import jetbrains.buildServer.nuget.feedReader.NuGetFeedReader
import jetbrains.buildServer.nuget.feedReader.NuGetPackage
import jetbrains.buildServer.nuget.feedReader.impl.NuGetFeedGetMethodFactory
import jetbrains.buildServer.nuget.feedReader.impl.NuGetFeedReaderImpl
import jetbrains.buildServer.nuget.feedReader.impl.NuGetFeedUrlResolver
import jetbrains.buildServer.nuget.feedReader.impl.NuGetPackagesFeedParser
import java.io.File
import java.io.IOException

class NuGetFeedReaderHolder : NuGetFeedReader {
    private val _feedReader: NuGetFeedReader

    init {
        val getMethodFactory = NuGetFeedGetMethodFactory()
        _feedReader = NuGetFeedReaderImpl(NuGetFeedUrlResolver(getMethodFactory), getMethodFactory, NuGetPackagesFeedParser())
    }

    @Throws(IOException::class)
    override fun queryPackageVersions(nuGetFeedClient: NuGetFeedClient, feedUrl: String, packageId: String): Collection<NuGetPackage> {
        return _feedReader.queryPackageVersions(nuGetFeedClient, feedUrl, packageId)
    }

    @Throws(IOException::class)
    override fun downloadPackage(nuGetFeedClient: NuGetFeedClient, downloadUrl: String, destination: File) {
        _feedReader.downloadPackage(nuGetFeedClient, downloadUrl, destination)
    }
}
