// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.keyboard.clipboard

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardHistoryManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.getPlatformDialogThemeContext

class ClipboardAdapter(
       val clipboardLayoutParams: ClipboardLayoutParams,
       val keyEventListener: OnKeyEventListener
) : RecyclerView.Adapter<ClipboardAdapter.ViewHolder>() {

    var clipboardHistoryManager: ClipboardHistoryManager? = null

    var pinnedIconResId = 0
    var itemBackgroundId = 0
    var itemTypeFace: Typeface? = null
    var itemTextColor = 0
    var itemTextSize = 0f

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.clipboard_entry_key, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setContent(getItem(position))
    }

    private fun getItem(position: Int) = clipboardHistoryManager?.getHistoryEntry(position)

    override fun getItemCount() = clipboardHistoryManager?.getHistorySize() ?: 0

    // un AlertDialog normal no se puede mostrar desde un IME sin fijar el window token y tipo de ventana
    private fun showImeDialog(builder: AlertDialog.Builder, anchor: View): AlertDialog {
        val dialog = builder.create()
        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.token = anchor.windowToken
        layoutParams?.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        window?.attributes = layoutParams
        window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.show()
        return dialog
    }

    // para fijados: pide confirmación dos veces antes de borrar
    private fun confirmAndDelete(position: Int, view: View) {
        val ctx = getPlatformDialogThemeContext(view.context)
        showImeDialog(
            AlertDialog.Builder(ctx)
                .setTitle(R.string.clipboard_delete_confirm_title)
                .setMessage(R.string.clipboard_delete_confirm_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete) { _, _ ->
                    showImeDialog(
                        AlertDialog.Builder(ctx)
                            .setMessage(R.string.clipboard_delete_confirm_final)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(R.string.delete) { _, _ ->
                                clipboardHistoryManager?.forceRemoveEntry(position)
                            },
                        view
                    )
                },
            view
        )
    }

    inner class ViewHolder(
            view: View
    ) : RecyclerView.ViewHolder(view), View.OnClickListener, View.OnTouchListener {

        private val pinnedIconView: ImageView
        private val deleteIconView: TextView
        private val contentTextView: TextView
        private val contentImageView: ImageView
        private var isPinned = false

        init {
            view.apply {
                setOnClickListener(this@ViewHolder)
                setOnTouchListener(this@ViewHolder)
                setBackgroundResource(itemBackgroundId)
                isHapticFeedbackEnabled = false
            }
            Settings.getValues().mColors.setBackground(view, ColorType.KEY_BACKGROUND)
            pinnedIconView = view.findViewById<ImageView>(R.id.clipboard_entry_pinned_icon).apply {
                setImageResource(pinnedIconResId)
                setOnClickListener {
                    val id = itemView.tag as? Long ?: return@setOnClickListener
                    clipboardHistoryManager?.toggleClipPinned(id)
                }
            }
            deleteIconView = view.findViewById<TextView>(R.id.clipboard_entry_delete_icon).apply {
                setOnClickListener {
                    val position = bindingAdapterPosition
                    if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                    if (isPinned) {
                        // fijado: no se borra de frente, se pide confirmación dos veces
                        confirmAndDelete(position, it)
                    } else {
                        // sin fijar: se borra directo
                        clipboardHistoryManager?.removeEntry(position)
                    }
                }
            }
            contentTextView = view.findViewById<TextView>(R.id.clipboard_entry_text_content).apply {
                typeface = itemTypeFace
                setTextColor(itemTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize)
                maxLines = 1
                isSingleLine = true
            }
            contentImageView = view.findViewById(R.id.clipboard_entry_image_content)
            clipboardLayoutParams.setItemProperties(view)
            val colors = Settings.getValues().mColors
            colors.setColor(pinnedIconView, ColorType.CLIPBOARD_PIN)
            deleteIconView.setTextColor(colors.get(ColorType.CLIPBOARD_PIN))
        }

        fun setContent(historyEntry: ClipboardHistoryEntry?) {
            if (historyEntry == null) return
            itemView.tag = historyEntry.id
            isPinned = historyEntry.isPinned
            if (historyEntry.filename != null) {
                historyEntry.setImageAndDescription(contentImageView, contentTextView)
            } else {
                contentTextView.text = historyEntry.text?.take(1000) // truncate displayed text for performance reasons
            }
            pinnedIconView.alpha = if (isPinned) 1f else 0.35f
            deleteIconView.visibility = View.VISIBLE
            contentImageView.visibility = if (historyEntry.filename != null) View.VISIBLE else View.GONE
            contentTextView.visibility = if (contentTextView.text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                keyEventListener.onKeyDown(view.tag as Long)
            }
            return false
        }

        override fun onClick(view: View) {
            keyEventListener.onKeyUp(view.tag as Long)
        }
    }
}
