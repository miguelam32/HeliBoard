// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable

/**
 * Aproximación de "vidrio esmerilado" (glassmorphism): panel translúcido con
 * borde suave y un brillo diagonal, para que las filas se vean como cristal
 * flotando sobre el video de fondo. No es un blur en tiempo real del fondo
 * (eso requiere Android 12+ / RenderEffect); es un efecto visual aproximado
 * compatible con todas las versiones.
 */
class GlassCardDrawable(private val cornerRadiusPx: Float) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26FFFFFF // blanco translúcido ~15%
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x4DFFFFFF // blanco translúcido ~30%
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val shinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    override fun onBoundsChange(bounds: android.graphics.Rect) {
        super.onBoundsChange(bounds)
        rect.set(1f, 1f, bounds.width() - 1f, bounds.height() - 1f)
        if (rect.width() > 0 && rect.height() > 0) {
            shinePaint.shader = LinearGradient(
                0f, 0f, rect.width() * 0.6f, rect.height(),
                intArrayOf(0x33FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, fillPaint)
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, shinePaint)
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, strokePaint)
    }

    override fun setAlpha(alpha: Int) { fillPaint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { fillPaint.colorFilter = colorFilter }
    @Suppress("DEPRECATION")
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}
