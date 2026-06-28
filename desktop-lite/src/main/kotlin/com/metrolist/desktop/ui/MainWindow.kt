package com.metrolist.desktop.ui

import com.metrolist.desktop.download.DownloadListener
import com.metrolist.desktop.download.DownloadManager
import com.metrolist.desktop.download.DownloadStatus
import com.metrolist.desktop.download.DownloadStore
import com.metrolist.desktop.lyrics.LyricsState
import com.metrolist.desktop.model.Song
import com.metrolist.desktop.playback.PlaybackController
import com.metrolist.desktop.playback.PlaybackListener
import com.metrolist.desktop.playback.RepeatMode
import com.metrolist.desktop.ui.theme.Theme
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JPanel

/**
 * App shell: sidebar + card-switched screens (Home / Search / Downloads / Now Playing) + bottom bar.
 * Observes the [PlaybackController] and [DownloadManager] and fans updates out to the screens.
 */
class MainWindow(
    private val controller: PlaybackController,
    private val store: DownloadStore,
    private val downloads: DownloadManager,
) : JFrame("Metrolist Lite"), PlaybackListener, DownloadListener {

    private val controls = Controls(
        togglePlay = controller::togglePlay,
        next = controller::next,
        prev = controller::previous,
        toggleShuffle = controller::toggleShuffle,
        cycleRepeat = controller::cycleRepeat,
        seek = controller::seekTo,
        setVolume = controller::setVolume,
    )

    private val cards = CardLayout()
    private val content = JPanel(cards).apply { background = Theme.bg }

    private val home = HomePanel(
        onSearch = ::startSearch,
        onPlaySong = { controller.playQueue(listOf(it), 0) },
        onPlayDownloadedAt = ::playDownloadedAt,
    )
    private val searchPanel = SearchPanel(
        onSearch = ::startSearch,
        onPlayAt = ::playFromResults,
        onDownload = { downloads.download(it) },
        statusOf = { downloads.statusOf(it) },
    )
    private val downloadsPanel = DownloadsPanel(onPlayAt = ::playDownloadedAt, onRemove = { downloads.remove(it) })
    private val nowPlaying = NowPlayingPanel(controls, onToggleFullscreen = ::toggleImmersive)
    private val bottomBar = BottomBar(controls, onOpenNowPlaying = { navigate(Screen.NOW_PLAYING) })
    private val sidebar = Sidebar(::navigate)

    private var currentResults: List<Song> = emptyList()
    private val recents = mutableListOf<Song>()
    private val inProgress = HashMap<String, DownloadStatus>()
    private var immersive = false

    init {
        defaultCloseOperation = DISPOSE_ON_CLOSE
        minimumSize = Dimension(1000, 660)
        size = Dimension(1200, 780)
        setLocationRelativeTo(null)

        AppIcon.image?.let { iconImage = it }
        title = ""
        rootPane.putClientProperty("apple.awt.fullWindowContent", true)
        rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
        rootPane.putClientProperty("apple.awt.windowTitleVisible", false)

        content.add(home, Screen.HOME.name)
        content.add(searchPanel, Screen.SEARCH.name)
        content.add(downloadsPanel, Screen.DOWNLOADS.name)
        content.add(nowPlaying, Screen.NOW_PLAYING.name)

        val main = JPanel(BorderLayout()).apply { background = Theme.bg }
        main.add(sidebar, BorderLayout.WEST)
        main.add(content, BorderLayout.CENTER)

        contentPane.background = Theme.bg
        contentPane.layout = BorderLayout()
        contentPane.add(main, BorderLayout.CENTER)
        contentPane.add(bottomBar, BorderLayout.SOUTH)

        controller.addListener(this)
        downloads.addListener(this)
        navigate(Screen.HOME)
        refreshDownloads()
        downloads.backfillCovers() // fetch any missing artwork for older downloads (online)
        downloads.backfillLyrics() // fetch word-synced lyrics for older downloads (online)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) { controller.stop() }
        })
    }

    private fun toggleImmersive() {
        immersive = !immersive
        if (immersive) navigate(Screen.NOW_PLAYING)
        sidebar.isVisible = !immersive
        bottomBar.isVisible = !immersive
        setFullscreen(immersive)
        contentPane.revalidate(); contentPane.repaint()
    }

    private fun setFullscreen(on: Boolean) {
        val gd = graphicsConfiguration?.device
            ?: java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice
        runCatching {
            if (on) {
                if (gd.isFullScreenSupported) gd.fullScreenWindow = this else extendedState = java.awt.Frame.MAXIMIZED_BOTH
            } else {
                if (gd.fullScreenWindow === this) gd.fullScreenWindow = null else extendedState = java.awt.Frame.NORMAL
            }
        }
    }

    private fun navigate(screen: Screen) {
        cards.show(content, screen.name)
        sidebar.select(screen)
        if (screen == Screen.SEARCH) searchPanel.focusSearch()
    }

    private fun startSearch(query: String) {
        searchPanel.setQueryText(query)
        searchPanel.setStatus("Searching for \"${query.trim()}\"…")
        navigate(Screen.SEARCH)
        controller.search(query)
    }

    private fun playFromResults(index: Int) {
        if (currentResults.isEmpty()) return
        controller.playQueue(currentResults, index)
    }

    private fun playDownloadedAt(index: Int) {
        val songs = store.all().map { it.toSong() }
        if (songs.isEmpty()) return
        controller.playQueue(songs, index)
    }

    private fun refreshDownloads() {
        val songs = store.all().map { it.toSong() }
        home.setDownloads(songs)
        downloadsPanel.refresh(store.all(), inProgress)
    }

    // ---- PlaybackListener (EDT) ----

    override fun onSearchResults(query: String, songs: List<Song>) {
        currentResults = songs
        searchPanel.showResults(query, songs)
    }

    override fun onTrackChanged(song: Song?, index: Int) {
        nowPlaying.setTrack(song)
        bottomBar.setTrack(song)
        if (song != null) {
            recents.removeAll { it.videoId == song.videoId }
            recents.add(0, song)
            home.setRecents(recents)
            ImageLoader.palette(song.thumbnailUrl) { colors -> nowPlaying.setPalette(colors) }
        }
    }

    override fun onPlaybackState(playing: Boolean, paused: Boolean) {
        nowPlaying.setState(playing, paused)
        bottomBar.setState(playing, paused)
    }

    override fun onProgress(positionSec: Double, durationSec: Double) {
        nowPlaying.setProgress(positionSec, durationSec)
        bottomBar.setProgress(positionSec, durationSec)
    }

    override fun onModes(shuffle: Boolean, repeat: RepeatMode) {
        bottomBar.setModes(shuffle, repeat)
    }

    override fun onLyrics(state: LyricsState) {
        nowPlaying.setLyrics(state)
    }

    override fun onStatus(message: String) {
        searchPanel.setStatus(message)
    }

    // ---- DownloadListener (EDT) ----

    override fun onDownloadChanged(videoId: String, status: DownloadStatus) {
        when (status) {
            is DownloadStatus.Downloading, is DownloadStatus.Queued, is DownloadStatus.Failed -> inProgress[videoId] = status
            else -> inProgress.remove(videoId)
        }
        searchPanel.updateDownload(videoId, status)
        downloadsPanel.refresh(store.all(), inProgress)
    }

    override fun onLibraryChanged() {
        refreshDownloads()
    }
}
