// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import java.io.File
import kotlin.math.max

/**
 * Reproduce un video en loop, sin audio, como fondo del teclado.
 * Se coloca como la vista de MÁS ABAJO (primer hijo) dentro de un FrameLayout
 * para que las teclas (transparentes) se dibujen encima.
 * Aplica un escalado tipo "center-crop": el video siempre cubre todo el
 * área sin deformarse, sea cual sea su proporción (horizontal, vertical, cuadrado).
 */
class VideoBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : TextureView(context, attrs, defStyle), TextureView.SurfaceTextureListener {

    private var mediaPlayer: MediaPlayer? = null
    private var pendingFile: File? = null
    private var srcVideoWidth = 0
    private var srcVideoHeight = 0

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
                setOnVideoSizeChangedListener { _, w, h ->
                    srcVideoWidth = w
                    srcVideoHeight = h
                    applyCenterCropTransform()
                }
                setOnPreparedListener { it.start() }
                setOnErrorListener { _, _, _ -> true } // evitar crash si el archivo es inválido
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
        srcVideoWidth = 0
        srcVideoHeight = 0
    }

    /** Escala el contenido del video para que cubra toda la vista sin deformarse (equivalente a object-fit: cover). */
    private fun applyCenterCropTransform() {
        val vw = srcVideoWidth
        val vh = srcVideoHeight
        val w = width
        val h = height
        if (vw <= 0 || vh <= 0 || w <= 0 || h <= 0) return

        // Por defecto TextureView estira el buffer del video para llenar exactamente
        // width x height (deformando la proporción). Calculamos un factor extra de
        // escalado, por encima de ese estiramiento por defecto, para que el video
        // quede escalado de forma UNIFORME y cubra toda la vista sin deformarse.
        val defaultFactorX = w.toFloat() / vw
        val defaultFactorY = h.toFloat() / vh
        val uniformFactor = max(defaultFactorX, defaultFactorY)
        val extraScaleX = uniformFactor / defaultFactorX
        val extraScaleY = uniformFactor / defaultFactorY

        val matrix = Matrix()
        matrix.setScale(extraScaleX, extraScaleY, w / 2f, h / 2f)
        setTransform(matrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        applyCenterCropTransform()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        pendingFile?.let { startPlayback(it) }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        applyCenterCropTransform()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releasePlayer()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}
