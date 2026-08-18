package com.dinushlakmal.lakaboard.ime

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.dinushlakmal.lakaboard.R
import com.dinushlakmal.lakaboard.audio.SoundHapticHelper
import com.dinushlakmal.lakaboard.audio.SoundProfile
import com.dinushlakmal.lakaboard.viewmodel.ShiftState

/**
 * KeyDefinition
 * ---------------------------------------------------------------------
 * Model holding primary character, secondary hint, weight, and key type.
 */
data class KeyDefinition(
    val primary: String,
    val secondary: String? = null,
    val isFunctionKey: Boolean = false,
    val iconRes: Int? = null,
    val weight: Float = 1.0f
)

/**
 * KeyboardEventListener
 * ---------------------------------------------------------------------
 * Callback interface delivering raw user keypresses and mode switches.
 */
interface KeyboardEventListener {
    fun onTextEntered(text: String)
    fun onBackspacePressed()
    fun onEnterPressed()
    fun onSpacePressed()
    fun onShiftToggle(newState: ShiftState)
    fun onLanguageToggle()
    fun onEmojiPickerRequested()
    fun onThemeSelectorRequested()
    fun onClipboardRequested()
    fun onTextStylerRequested()
    fun onCursorToolRequested()
    fun onCollapseRequested()
    fun onSymbolsModeRequested()
}

/**
 * KeyboardUiBinder
 * ---------------------------------------------------------------------
 * High-performance UI binding controller for custom Android IME XML layouts.
 * Replicates the frosted-glass transparent keyboard with secondary hints,
 * quick emoji ribbon, shift state machine, and rapid touch dispatch.
 */
class KeyboardUiBinder(
    private val context: Context,
    private val listener: KeyboardEventListener,
    private val soundHapticHelper: SoundHapticHelper? = null
) {
    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val handler = Handler(Looper.getMainLooper())

    private var rootView: FrameLayout? = null
    private var ivCustomBg: ImageView? = null
    private var viewGlassOverlay: View? = null
    private var tvSpacebar: TextView? = null
    private var tvLangChip: TextView? = null
    private var ivShiftIcon: ImageView? = null
    private var shiftContainer: FrameLayout? = null

    // Track letter key TextViews to update casing instantly on Shift/Caps toggles
    private val letterKeyViews = ArrayList<Pair<KeyDefinition, TextView>>()

    private var shiftState: ShiftState = ShiftState.OFF
    private var lastShiftPressTime: Long = 0L

    // Default keyboard row definitions matching the visual specs
    private val rowQwertyTop = listOf(
        KeyDefinition("q", "1"),
        KeyDefinition("w", "2"),
        KeyDefinition("e", "3"),
        KeyDefinition("r", "4"),
        KeyDefinition("t", "5"),
        KeyDefinition("y", "6"),
        KeyDefinition("u", "7"),
        KeyDefinition("i", "8"),
        KeyDefinition("o", "9"),
        KeyDefinition("p", "0")
    )

    private val rowHome = listOf(
        KeyDefinition("a", "@"),
        KeyDefinition("s", "#"),
        KeyDefinition("d", "$"),
        KeyDefinition("f", "-"),
        KeyDefinition("g", "&"),
        KeyDefinition("h", "_"),
        KeyDefinition("j", "+"),
        KeyDefinition("k", "("),
        KeyDefinition("l", ")")
    )

    private val rowBottomLetters = listOf(
        KeyDefinition("z", "*"),
        KeyDefinition("x", "\""),
        KeyDefinition("c", "'"),
        KeyDefinition("v", ":"),
        KeyDefinition("b", ";"),
        KeyDefinition("n", "!"),
        KeyDefinition("m", "?")
    )

    private val quickEmojis = listOf("❤️", "😂", "😊", "😏", "😒", "😌", "🥺", "🥲", "😮‍💨", "😁", "🔥", "🙏")

    /**
     * Inflates and binds the complete keyboard view hierarchy.
     */
    fun bindKeyboard(parent: ViewGroup? = null): View {
        val root = inflater.inflate(R.layout.keyboard_root, parent, false) as FrameLayout
        rootView = root

        ivCustomBg = root.findViewById(R.id.iv_keyboard_custom_bg)
        viewGlassOverlay = root.findViewById(R.id.view_glass_overlay)

        bindUtilityBar(root)
        bindEmojiRibbon(root)
        bindLetterRows(root)
        bindActionBar(root)

        return root
    }

    /**
     * Sets a custom background photo with the semi-transparent frosted overlay.
     */
    fun setCustomBackground(drawable: Drawable?) {
        ivCustomBg?.apply {
            if (drawable != null) {
                setImageDrawable(drawable)
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    fun setCustomBackground(bitmap: Bitmap?) {
        if (bitmap != null) {
            setCustomBackground(BitmapDrawable(context.resources, bitmap))
        } else {
            setCustomBackground(null as Drawable?)
        }
    }

    fun setSpacebarLabel(text: String) {
        tvSpacebar?.text = text
    }

    fun setLanguageChipLabel(text: String) {
        tvLangChip?.text = text
    }

    private fun bindUtilityBar(root: View) {
        tvLangChip = root.findViewById(R.id.tv_chip_lang)

        root.findViewById<View>(R.id.chip_lang_toggle).setOnClickListener {
            press()
            listener.onLanguageToggle()
        }
        root.findViewById<View>(R.id.chip_emoji_picker).setOnClickListener {
            press()
            listener.onEmojiPickerRequested()
        }
        root.findViewById<View>(R.id.chip_theme).setOnClickListener {
            press()
            listener.onThemeSelectorRequested()
        }
        root.findViewById<View>(R.id.chip_clipboard).setOnClickListener {
            press()
            listener.onClipboardRequested()
        }
        root.findViewById<View>(R.id.chip_font_styler).setOnClickListener {
            press()
            listener.onTextStylerRequested()
        }
        root.findViewById<View>(R.id.chip_cursor_tool).setOnClickListener {
            press()
            listener.onCursorToolRequested()
        }
        root.findViewById<View>(R.id.chip_collapse).setOnClickListener {
            press()
            listener.onCollapseRequested()
        }
    }

    private fun bindEmojiRibbon(root: View) {
        val ribbon = root.findViewById<LinearLayout>(R.id.container_emoji_ribbon) ?: return
        ribbon.removeAllViews()

        for (emoji in quickEmojis) {
            val emojiView = TextView(context).apply {
                text = emoji
                textSize = 20f
                setPadding(dp(8), dp(4), dp(8), dp(4))
                isClickable = true
                isFocusable = true
                background = ContextCompat.getDrawable(context, R.drawable.sel_utility_circle)
                setOnClickListener {
                    press()
                    listener.onTextEntered(emoji)
                }
            }
            ribbon.addView(emojiView)
        }
    }

    private fun bindLetterRows(root: View) {
        letterKeyViews.clear()

        val layoutRowTop = root.findViewById<LinearLayout>(R.id.row_qwerty_top)
        val layoutRowHome = root.findViewById<LinearLayout>(R.id.row_home)
        val layoutRowBottom = root.findViewById<LinearLayout>(R.id.row_bottom)

        // Populate QWERTY Row 2
        layoutRowTop?.let { layout ->
            layout.removeAllViews()
            for (key in rowQwertyTop) {
                layout.addView(createKeyView(key, layout))
            }
        }

        // Populate Home Row 3
        layoutRowHome?.let { layout ->
            layout.removeAllViews()
            for (key in rowHome) {
                layout.addView(createKeyView(key, layout))
            }
        }

        // Populate Bottom Row 4 with Shift + Letters + Backspace
        layoutRowBottom?.let { layout ->
            layout.removeAllViews()

            // 1. Shift Key
            val shiftKeyView = createShiftKeyView(layout)
            layout.addView(shiftKeyView)

            // 2. Letters Z..M
            for (key in rowBottomLetters) {
                layout.addView(createKeyView(key, layout))
            }

            // 3. Backspace Key
            val backspaceKeyView = createBackspaceKeyView(layout)
            layout.addView(backspaceKeyView)
        }
    }

    private fun bindActionBar(root: View) {
        val actionBar = root.findViewById<View>(R.id.included_action_bar) ?: return
        tvSpacebar = actionBar.findViewById(R.id.tv_spacebar_label)

        actionBar.findViewById<View>(R.id.key_sym_mode).setOnClickListener {
            press()
            listener.onSymbolsModeRequested()
        }

        actionBar.findViewById<View>(R.id.key_globe).setOnClickListener {
            press()
            listener.onLanguageToggle()
        }

        actionBar.findViewById<View>(R.id.key_comma).setOnClickListener {
            press()
            listener.onTextEntered(",")
        }

        actionBar.findViewById<View>(R.id.key_spacebar).setOnClickListener {
            press()
            listener.onSpacePressed()
        }

        actionBar.findViewById<View>(R.id.key_period).setOnClickListener {
            press()
            listener.onTextEntered(".")
        }

        actionBar.findViewById<View>(R.id.key_enter).setOnClickListener {
            press()
            listener.onEnterPressed()
        }
    }

    private fun createKeyView(key: KeyDefinition, parent: ViewGroup): View {
        val view = inflater.inflate(R.layout.view_key, parent, false) as FrameLayout
        val tvMain = view.findViewById<TextView>(R.id.tv_main_label)
        val tvSub = view.findViewById<TextView>(R.id.tv_sub_label)

        val initialChar = if (shiftState != ShiftState.OFF) key.primary.uppercase() else key.primary.lowercase()
        tvMain.text = initialChar

        if (key.secondary != null) {
            tvSub.text = key.secondary
            tvSub.visibility = View.VISIBLE
        } else {
            tvSub.visibility = View.GONE
        }

        letterKeyViews.add(Pair(key, tvMain))

        // Tap for primary, Long-press for secondary sub-label
        view.setOnClickListener {
            press()
            val textToInsert = if (shiftState != ShiftState.OFF) key.primary.uppercase() else key.primary.lowercase()
            listener.onTextEntered(textToInsert)

            // Auto-revert single shift
            if (shiftState == ShiftState.SHIFT_ONCE) {
                setShiftState(ShiftState.OFF)
            }
        }

        if (key.secondary != null) {
            view.setOnLongClickListener {
                press()
                listener.onTextEntered(key.secondary)
                true
            }
        }

        val lp = LinearLayout.LayoutParams(0, dp(48), key.weight).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        view.layoutParams = lp
        return view
    }

    private fun createShiftKeyView(parent: ViewGroup): View {
        val view = inflater.inflate(R.layout.view_key, parent, false) as FrameLayout
        shiftContainer = view
        val ivIcon = view.findViewById<ImageView>(R.id.iv_key_icon)
        ivShiftIcon = ivIcon
        ivIcon.setImageResource(R.drawable.ic_ime_shift)
        ivIcon.visibility = View.VISIBLE
        view.findViewById<View>(R.id.tv_main_label).visibility = View.GONE
        view.findViewById<View>(R.id.tv_sub_label).visibility = View.GONE

        view.setOnClickListener {
            press()
            val now = System.currentTimeMillis()
            val isDoubleTap = (now - lastShiftPressTime) < 350L
            lastShiftPressTime = now

            val nextState = when {
                isDoubleTap && shiftState != ShiftState.CAPS_LOCK -> ShiftState.CAPS_LOCK
                shiftState == ShiftState.OFF -> ShiftState.SHIFT_ONCE
                shiftState == ShiftState.SHIFT_ONCE -> ShiftState.OFF
                else -> ShiftState.OFF
            }
            setShiftState(nextState)
        }

        val lp = LinearLayout.LayoutParams(0, dp(48), 1.4f).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        view.layoutParams = lp
        return view
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createBackspaceKeyView(parent: ViewGroup): View {
        val view = inflater.inflate(R.layout.view_key, parent, false) as FrameLayout
        val ivIcon = view.findViewById<ImageView>(R.id.iv_key_icon)
        ivIcon.setImageResource(R.drawable.ic_ime_backspace)
        ivIcon.visibility = View.VISIBLE
        view.findViewById<View>(R.id.tv_main_label).visibility = View.GONE
        view.findViewById<View>(R.id.tv_sub_label).visibility = View.GONE

        // Continuous repeat on hold
        val repeatRunnable = object : Runnable {
            override fun run() {
                press()
                listener.onBackspacePressed()
                handler.postDelayed(this, 60L)
            }
        }

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    press()
                    listener.onBackspacePressed()
                    handler.postDelayed(repeatRunnable, 400L)
                    view.isPressed = true
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(repeatRunnable)
                    view.isPressed = false
                    true
                }
                else -> false
            }
        }

        val lp = LinearLayout.LayoutParams(0, dp(48), 1.4f).apply {
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
        view.layoutParams = lp
        return view
    }

    fun setShiftState(state: ShiftState) {
        shiftState = state
        listener.onShiftToggle(state)

        ivShiftIcon?.apply {
            when (state) {
                ShiftState.OFF -> {
                    setImageResource(R.drawable.ic_ime_shift)
                    alpha = 0.7f
                }
                ShiftState.SHIFT_ONCE -> {
                    setImageResource(R.drawable.ic_ime_shift)
                    alpha = 1.0f
                }
                ShiftState.CAPS_LOCK -> {
                    setImageResource(R.drawable.ic_ime_shift_locked)
                    alpha = 1.0f
                }
            }
        }

        // Update all letter keys on screen instantly
        val isUpper = state != ShiftState.OFF
        for ((key, tv) in letterKeyViews) {
            tv.text = if (isUpper) key.primary.uppercase() else key.primary.lowercase()
        }
    }

    private fun press() {
        soundHapticHelper?.onKeyPress(SoundProfile.MECHANICAL_CLICK)
    }

    private fun dp(value: Int): Int {
        val density = context.resources.displayMetrics.density
        return (value * density + 0.5f).toInt()
    }
}
