package com.afertettu.app.di

import com.afertettu.app.core.link.RedirectResolver
import com.afertettu.app.core.debug.LogExporter
import com.afertettu.app.core.debug.RequestLog
import com.afertettu.app.core.media.AutoMediaDownloader
import com.afertettu.app.core.media.OfflineMedia
import com.afertettu.app.core.media.MediaDownloader
import com.afertettu.app.core.media.MediaSavingNotice
import com.afertettu.app.core.network.ConnectivityMonitor
import com.afertettu.app.core.network.HostThrottle
import com.afertettu.app.core.network.HttpClientFactory
import com.afertettu.app.data.bsky.BskyApi
import com.afertettu.app.data.bsky.BskyVideo
import com.afertettu.app.core.link.LinkRouter
import com.afertettu.app.data.accounts.AccountStore
import com.afertettu.app.data.cache.FeedCache
import com.afertettu.app.data.repository.FeedRepository
import com.afertettu.app.data.repository.TimelineRepository
import com.afertettu.app.data.read.ReadMarks
import com.afertettu.app.data.settings.SettingsStore
import com.afertettu.app.feature.accounts.AccountsViewModel
import com.afertettu.app.feature.post.PostDetailViewModel
import com.afertettu.app.feature.search.SearchViewModel
import com.afertettu.app.feature.settings.SettingsViewModel
import com.afertettu.app.feature.feed.FeedViewModel
import com.afertettu.app.feature.media.SavedMediaViewModel
import com.afertettu.app.feature.timeline.TimelineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Single composition root. Layers get added here as they land:
 * step 3 sources, step 4 database.
 */
val appModule = module {

    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { RequestLog() }
    single { RedirectResolver() }
    single { LogExporter(androidContext()) }
    single { HostThrottle() }
    single { HttpClientFactory.create() }
    single { BskyApi(get(), get(), get()) }
    single { BskyVideo(get(), get()) }
    single { ConnectivityMonitor(androidContext()) }
    single {
        val videos: BskyVideo = get()
        MediaDownloader(androidContext(), get(named("appScope"))) { playlist -> videos.fileFor(playlist) }
    }
    single { OfflineMedia(androidContext()) }
    single { ReadMarks(androidContext()) }
    single { MediaSavingNotice(androidContext(), get(named("appScope"))) }
    single { AutoMediaDownloader(get(), get(), get(), get(), get(), get()) }
    single { AccountStore(androidContext()) }
    single { LinkRouter() }
    single { FeedRepository(get()) }
    single {
        val settings: SettingsStore = get()
        FeedCache(androidContext(), get()) { settings.current.keepPostsDays }
    }
    single { SettingsStore(androidContext()) }
    single { TimelineRepository(get(), get(), get()) }

    viewModel { AccountsViewModel(get(), get(), get(), get()) }
    viewModel { PostDetailViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { FeedViewModel(get(), get(), get()) }
    viewModel { TimelineViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), androidContext()) }
    viewModel { SavedMediaViewModel(get()) }
}
