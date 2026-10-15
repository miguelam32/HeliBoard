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

/**
 * Tine el icono completo de un boton del toolbar con el mismo barrido RGB del teclado
 * principal (letras/fondo), sincronizado en velocidad para que se vea como una sola ola.
 * keyIndex/totalKeys ubican este boton dentro de la fila (0..totalKeys-1).
 */
fun applyRgbSweepTint(view: android.widget.ImageView, keyIndex: Int, totalKeys: Int) {
    var running = true
    val startTime = System.currentTimeMillis()
    val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            val elapsed = System.currentTimeMillis() - startTime
            val speed = 0.00003f
            val wavelength = 0.8f
            val positionFraction = if (totalKeys > 1) keyIndex.toFloat() / totalKeys else 0f
            var hue = ((elapsed * speed) + (positionFraction * wavelength)) % 1.0f
            if (hue < 0) hue += 1.0f
            val hsv = floatArrayOf(hue * 360f, 0.85f, 1.0f)
            view.setColorFilter(android.graphics.Color.HSVToColor(hsv))
            view.postDelayed(this, 50)
        }
    }
    view.post(ticker)
    view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            running = true
            view.post(ticker)
        }
        override fun onViewDetachedFromWindow(v: View) {
            running = false
            view.removeCallbacks(ticker)
        }
    })
}

/** Brillo blanco suave (sin animación) para indicar "activo", p.ej. el pin ya fijado. */
class SoftGlowDrawable : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var cx = 0f
    private var cy = 0f
    private var radius = 0f

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        cx = bounds.width() / 2f
        cy = bounds.height() / 2f
        radius = maxOf(cx, cy)
        if (radius > 0f) {
            paint.shader = android.graphics.RadialGradient(
                cx, cy, radius,
                intArrayOf(0x40FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun draw(canvas: Canvas) {
        if (radius > 0f) canvas.drawCircle(cx, cy, radius, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    @Suppress("DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
