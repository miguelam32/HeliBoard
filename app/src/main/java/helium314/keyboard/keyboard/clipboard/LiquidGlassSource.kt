// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import helium314.keyboard.latin.VideoBackgroundView
import kotlin.math.max

/**
 * Captura periódicamente el video de fondo (vía TextureView.getBitmap, que es
 * barato porque es justo para eso), lo reduce/difumina, y deja el resultado
 * disponible para que cada fila del portapapeles dibuje su "rebanada" exacta:
 * cristal líquido de verdad sobre el video, que sigue moviéndose detrás.
 */
class LiquidGlassSource(private val onFrame: () -> Unit) {
    companion object {
        private const val DOWNSCALE = 3
        private const val TICK_MS = 90L
    }

    var currentFrame: Bitmap? = null
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var videoView: VideoBackgroundView? = null
    private var running = false
    private var phase = 0f

    private val tick = object : Runnable {
        override fun run() {
            captureFrame()
            phase += 0.35f
            onFrame()
            if (running) handler.postDelayed(this, TICK_MS)
        }
    }

    fun start(videoView: VideoBackgroundView?) {
        this.videoView = videoView
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
        currentFrame?.recycle()
        currentFrame = null
    }

    fun wavePhase() = phase

    private fun captureFrame() {
        val video = videoView ?: return
        if (!video.isAvailable || video.width <= 0 || video.height <= 0) return
        val w = max(1, video.width / DOWNSCALE)
        val h = max(1, video.height / DOWNSCALE)
        val small = try { video.getBitmap(w, h) } catch (_: Exception) { null } ?: return
        val blurred = boxBlurBySubsampling(small)
        val old = currentFrame
        currentFrame = blurred
        old?.recycle()
    }

    // blur barato: reducir y volver a escalar un par de veces ya difumina bastante,
    // y es mucho más liviano que un blur de verdad por pixel en CPU
    private fun boxBlurBySubsampling(src: Bitmap): Bitmap {
        var bmp = src
        repeat(1) {
            val w = max(1, bmp.width / 2)
            val h = max(1, bmp.height / 2)
            val down = Bitmap.createScaledBitmap(bmp, w, h, true)
            if (bmp !== src) bmp.recycle()
            bmp = Bitmap.createScaledBitmap(down, src.width, src.height, true)
            down.recycle()
        }
        return bmp
    }
}
