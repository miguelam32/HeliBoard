// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import java.io.File

/**
 * Reproduce un video en loop, sin audio, como fondo del teclado.
 * Se coloca como la vista de MÁS ABAJO (primer hijo) dentro de un FrameLayout
 * para que las teclas (transparentes) se dibujen encima.
 */
class VideoBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle), TextureView.SurfaceTextureListener {

    private var mediaPlayer: MediaPlayer? = null
    private var pendingFile: File? = null

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    /** Llamar con el archivo de video a reproducir, o null para detener y limpiar. */
    fun setVideoFile(file: File?) {
        pendingFile = file
        if (file == null) {
            releasePlayer()
            return
        }
        if (isAvailable) {
            startPlayback(file)
        }
    }

    private fun startPlayback(file: File) {
        releasePlayer()
        if (!file.exists()) return
        val surfaceTexture = surfaceTexture ?: return
        val surface = Surface(surfaceTexture)
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setSurface(surface)
                isLooping = true
                setVolume(0f, 0f)
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true }
                prepareAsync()
            }
        } catch (_: Exception) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (_: Exception) { }
            release()
        }
        mediaPlayer = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        pendingFile?.let { startPlayback(it) }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releasePlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}
