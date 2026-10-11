// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import helium314.keyboard.latin.VideoBackgroundView
import kotlin.math.sin

/**
 * Dibuja, para UNA fila, el pedazo de video que le corresponde según su
 * posición real en pantalla, difuminado y con una leve onda, más un tinte
 * y borde de "vidrio" encima. El video sigue moviéndose detrás: cada ~90ms
 * LiquidGlassSource trae un frame nuevo y esto se redibuja solo.
 */
class LiquidGlassDrawable(
    private val ownerView: View,
    private val videoView: VideoBackgroundView,
    private val source: LiquidGlassSource,
    private val cornerRadiusPx: Float
) : Drawable() {

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x14FFFFFF }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4DFFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val rect = RectF()
    private val clipPath = Path()
    private val srcRect = Rect()
    private val dstRect = Rect()
    private val rowLoc = IntArray(2)
    private val videoLoc = IntArray(2)

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        rect.set(1f, 1f, bounds.width() - 1f, bounds.height() - 1f)
        clipPath.reset()
        clipPath.addRoundRect(rect, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }

    override fun draw(canvas: Canvas) {
        val frame = source.currentFrame
        val videoW = videoView.width
        val videoH = videoView.height
        if (frame != null && frame.width > 0 && frame.height > 0 && videoW > 0 && videoH > 0) {
            ownerView.getLocationInWindow(rowLoc)
            videoView.getLocationInWindow(videoLoc)
            val scaleX = frame.width.toFloat() / videoW
            val scaleY = frame.height.toFloat() / videoH
            val relLeft = ((rowLoc[0] - videoLoc[0]) * scaleX).toInt()
            val relTop = ((rowLoc[1] - videoLoc[1]) * scaleY).toInt()
            val w = (bounds.width() * scaleX).toInt().coerceAtLeast(1)
            val h = (bounds.height() * scaleY).toInt().coerceAtLeast(1)
            val clampedW = w.coerceAtMost(frame.width)
            val clampedH = h.coerceAtMost(frame.height)
            val clampedLeft = relLeft.coerceIn(0, (frame.width - clampedW).coerceAtLeast(0))
            val clampedTop = relTop.coerceIn(0, (frame.height - clampedH).coerceAtLeast(0))
            srcRect.set(clampedLeft, clampedTop, clampedLeft + clampedW, clampedTop + clampedH)
            if (srcRect.left >= 0 && srcRect.top >= 0 && srcRect.width() > 0 && srcRect.height() > 0) {
                val saveCount = canvas.save()
                canvas.clipPath(clipPath)
                // leve onda de "cristal líquido": desplaza el recorte verticalmente con el tiempo
                val wave = (sin(source.wavePhase() + rect.centerX() * 0.015f) * 4f).toInt()
                dstRect.set(0, wave, bounds.width(), bounds.height() + wave)
                canvas.drawBitmap(frame, srcRect, dstRect, bitmapPaint)
                canvas.restoreToCount(saveCount)
            }
        }
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, tintPaint)
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
