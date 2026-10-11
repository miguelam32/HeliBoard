// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.LinearGradient
import android.graphics.SweepGradient
import android.graphics.drawable.Drawable
import android.view.View
import android.view.animation.LinearInterpolator

private val RAINBOW_COLORS = intArrayOf(
    0xFFFF0000.toInt(), 0xFFFF7F00.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(),
    0xFF00FFFF.toInt(), 0xFF0000FF.toInt(), 0xFF9400D3.toInt(), 0xFFFF0000.toInt()
)

/**
 * Borde animado para botones pequeños/circulares: UN solo color que muta
 * lentamente a través de todo el espectro (como un RGB de PC en modo
 * "respiración" lento), en vez de mostrar todos los colores a la vez.
 */
class RainbowBorderDrawable(
    private val strokeWidthPx: Float,
    private val cornerRadiusPx: Float
) : Drawable(), Animatable2 {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
    }
    private val rect = RectF()
    private var running = false
    private val hsv = floatArrayOf(0f, 0.85f, 1f)

    // una vuelta completa al espectro de color cada 14 segundos: lento y gradual
    private val animator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 14000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            hsv[0] = it.animatedValue as Float
            paint.color = android.graphics.Color.HSVToColor(hsv)
            invalidateSelf()
        }
    }

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        val half = strokeWidthPx / 2f
        rect.set(half, half, bounds.width() - half, bounds.height() - half)
    }

    override fun draw(canvas: Canvas) {
        if (rect.width() <= 0f || rect.height() <= 0f) return
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
    }

    override fun start() { if (!running) { running = true; animator.start() } }
    override fun stop() { running = false; animator.cancel() }
    override fun isRunning() = running
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Línea horizontal con un degradado arcoíris que "fluye" de forma continua. */
class RainbowLineDrawable : Drawable(), Animatable2 {
    private val paint = Paint()
    private var offset = 0f
    private var running = false

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3500
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            offset = it.animatedValue as Float
            invalidateSelf()
        }
    }

    override fun draw(canvas: Canvas) {
        val w = bounds.width().toFloat()
        val h = bounds.height().toFloat()
        if (w <= 0f || h <= 0f) return
        val shader = LinearGradient(
            0f, 0f, w, 0f,
            RAINBOW_COLORS, null, Shader.TileMode.REPEAT
        )
        val matrix = Matrix()
        matrix.postTranslate(offset * w, 0f)
        shader.setLocalMatrix(matrix)
        paint.shader = shader
        canvas.drawRect(0f, 0f, w, h, paint)
    }

    override fun start() { if (!running) { running = true; animator.start() } }
    override fun stop() { running = false; animator.cancel() }
    override fun isRunning() = running
    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/** Pequeña interfaz propia para no arrastrar android.graphics.drawable.Animatable en API baja. */
interface Animatable2 {
    fun start()
    fun stop()
    fun isRunning(): Boolean
}

/** Agrega un borde arcoíris animado como overlay (no reemplaza el fondo/ripple del botón). */
fun applyRainbowBorder(view: View, strokeDp: Float = 2.2f, cornerDp: Float = 100f) {
    val density = view.resources.displayMetrics.density
    val drawable = RainbowBorderDrawable(strokeDp * density, cornerDp * density)
    fun layoutBounds() {
        if (view.width > 0 && view.height > 0) {
            drawable.setBounds(0, 0, view.width, view.height)
        }
    }
    view.overlay.add(drawable)
    drawable.start()
    view.post { layoutBounds() }
    view.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
        if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) layoutBounds()
    }
}
