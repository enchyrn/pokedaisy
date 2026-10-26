package com.pokedaisy.app

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Properties

/**
 * Remappable mapping of physical keys/buttons to GBA buttons, from
 * `files/controls.properties`, per controller ([Pad]). One or more key names per GBA button
 * (comma-separated; any of them triggers it). The D-pad and analog stick stay
 * fixed — only the face/shoulder/menu buttons are here.
 */
object GbaControls {

    /** X / Y: the DS games' X (menu) and Y (registered item); in the other games a second START / SELECT (they
     * were START 2 / SELECT 2, still their names in the file: [prop]). On the field of a two-screen device X takes
     * the buttons to the companion instead (PokeDaisyActivity's bottom focus). TURBO A / B (issue #32, unbound by
     * default) press A / B on and off while held - [TURBO] marks them in [load]'s map, [GbaInput] pulses them. */
    enum class Btn(val bit: Int, val label: String) {
        A(MgbaCore.Key.A, "A"), B(MgbaCore.Key.B, "B"),
        L(MgbaCore.Key.L, "L"), R(MgbaCore.Key.R, "R"),
        START(MgbaCore.Key.START, "START"), SELECT(MgbaCore.Key.SELECT, "SELECT"),
        // On a DS game they're its own X / Y ([DS_X] / [DS_Y]: GbaInput swaps them in).
        X(MgbaCore.Key.START or DS_X, "X"), Y(MgbaCore.Key.SELECT or DS_Y, "Y"),
        TURBO_A(MgbaCore.Key.A or TURBO, "TURBO A"), TURBO_B(MgbaCore.Key.B or TURBO, "TURBO B");

        val prop get() = when (this) { X -> "start2"; Y -> "select2"; else -> name.lowercase() }
        /** Its row's name in GAME BUTTONS: the GBA's own buttons say so, X / Y are the DS's. */
        val title get() = if (this == X || this == Y) label else "GBA $label"
        /** A line under its row: what X / Y press in the games that have no X / Y (English; translated where drawn). */
        /** X's NEW tag: on the field of a two-screen device it takes the buttons to the companion. */
        val badge: String? get() = if (this == X) com.pokedaisy.app.companion.NewFeatures.badge(com.pokedaisy.app.companion.NewFeatures.X_COMPANION) else null
        val subtitle: String? get() = when (this) {
            X -> "DS games' X. Others: START, or the companion on the field"
            Y -> "DS games' Y. Others: SELECT"
            else -> null
        }
        /** TURBO A / B start unbound and can go back to it; every other button always keeps a key. */
        val canClear get() = this == TURBO_A || this == TURBO_B
    }

    private val DEFAULTS = linkedMapOf(
        "a" to "BUTTON_A, Z",
        "b" to "BUTTON_B, X",
        "l" to "BUTTON_L1, A",
        "r" to "BUTTON_R1, S",
        "start" to "BUTTON_START, ENTER",
        "select" to "BUTTON_SELECT, BACKSLASH, SHIFT_RIGHT",
        // Face buttons go by Android's names (Xbox positions) on every device, even where
        // the printed labels differ (AYN, Retroid: BUTTON_A is the bottom one); GAME BUTTONS remaps.
        "start2" to "BUTTON_X",
        "select2" to "BUTTON_Y",
        "turbo_a" to "",
        "turbo_b" to "",
    )

    /** Above every GBA key bit: a [load] value with it is a turbo button for the bits below. */
    const val TURBO = 1 shl 16

    /** X / Y's mark: on a DS game ([GbaInput.ds]) the key presses the DS's X / Y instead. */
    const val DS_X = 1 shl 17
    const val DS_Y = 1 shl 18

    private fun file(dir: File) = File(dir, "controls.properties")

    /**
     * A controller (or keyboard) with buttons of its own: Android's [android.view.InputDevice.getDescriptor] as
     * [key], stable across reconnects, and its name for GAME BUTTONS. A controller nobody rebound on uses the
     * shared map (the file's plain keys, all there was before maps per controller); its first rebind copies that
     * map under `pad.<key>.` and changes only the copy, so the keyboard and a gamepad never overwrite each other.
     */
    data class Pad(val key: String, val name: String)

    /** The controller that pressed a key last (GAME BUTTONS shows and rebinds its map), null = the shared map. */
    var currentPad by mutableStateOf<Pad?>(null)
        private set

    /** Called with every key event's device: a real controller becomes [currentPad]; the touch pad and injected
     *  keys (virtual, no device) leave it as it is. */
    fun notePad(device: InputDevice?) {
        val pad = padOf(device) ?: return
        if (pad != currentPad) currentPad = pad
    }

    fun padOf(device: InputDevice?): Pad? =
        device?.takeUnless { it.isVirtual }?.let { Pad(it.descriptor, it.name) }

    private fun padPrefix(pad: String) = "pad.$pad."

    /** Every button's keys, from the shared map or [pad]'s own. */
    class Controls(private val shared: Map<Int, Int>, private val pads: Map<String, Map<Int, Int>>) {
        fun forPad(pad: String?): Map<Int, Int> = pad?.let(pads::get) ?: shared
    }

    private fun readProps(dir: File): Properties {
        val p = Properties()
        val f = file(dir)
        if (f.isFile) runCatching { f.inputStream().use(p::load) }
        var changed = false
        for ((k, v) in DEFAULTS) if (!p.containsKey(k)) { p.setProperty(k, v); changed = true }
        if (changed) store(dir, p, "PokeDaisy GBA button map — Android key names, comma-separated")
        return p
    }

    private fun store(dir: File, props: Properties, comment: String = "PokeDaisy GBA button map") {
        runCatching { file(dir).outputStream().use { props.store(it, comment) } }
    }

    private fun keysOf(props: Properties, prop: String): List<String> =
        props.getProperty(prop).orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private fun keyMap(props: Properties, prefix: String): Map<Int, Int> {
        val out = HashMap<Int, Int>()
        for (btn in Btn.entries) keysOf(props, prefix + btn.prop).forEach { n -> resolveKey(n)?.let { out[it] = btn.bit } }
        return out
    }

    private fun ownsMap(props: Properties, pad: String?) =
        pad != null && Btn.entries.any { props.containsKey(padPrefix(pad) + it.prop) }

    /** The prefix [pad]'s keys are read from: its own, else the shared map's. */
    private fun readPrefix(props: Properties, pad: String?) = if (ownsMap(props, pad)) padPrefix(pad!!) else ""

    /** keycode -> GBA key bit ([TURBO] set for TURBO A / B) of the shared map. A key bound twice takes the later button. */
    fun load(dir: File): Map<Int, Int> = keyMap(readProps(dir), "")

    /** The shared map and every controller's own. */
    fun loadAll(dir: File): Controls {
        val props = readProps(dir)
        val pads = props.stringPropertyNames().filter { it.startsWith("pad.") }
            .map { it.removePrefix("pad.").substringBeforeLast('.') }.toSet()
            .filter { ownsMap(props, it) }
        return Controls(keyMap(props, ""), pads.associateWith { keyMap(props, padPrefix(it)) })
    }

    fun hasOwnMap(dir: File, pad: String?): Boolean = ownsMap(readProps(dir), pad)

    fun padName(dir: File, pad: String): String? = readProps(dir).getProperty(padPrefix(pad) + "name")

    fun rawBindings(dir: File, pad: String? = currentPad?.key): Map<Btn, List<String>> {
        val props = readProps(dir)
        val prefix = readPrefix(props, pad)
        return Btn.entries.associateWith { keysOf(props, prefix + it.prop) }
    }

    /** The prefix [pad]'s changes go to: its own map, copied from the shared one on its first change. */
    private fun writePrefix(props: Properties, pad: Pad?): String {
        if (pad == null) return ""
        val prefix = padPrefix(pad.key)
        if (!ownsMap(props, pad.key)) for (btn in Btn.entries) props.setProperty(prefix + btn.prop, props.getProperty(btn.prop).orEmpty())
        props.setProperty(prefix + "name", pad.name)
        return prefix
    }

    /** [btn] is [keyCode] now on [pad] - and that key leaves any other button it was on (else the later button kept it). */
    fun setBinding(dir: File, btn: Btn, keyCode: Int, pad: Pad? = currentPad) =
        setBindingName(dir, btn, Hotkeys.keyName(keyCode), pad)

    fun setBindingName(dir: File, btn: Btn, name: String, pad: Pad? = currentPad) {
        val props = readProps(dir)
        val prefix = writePrefix(props, pad)
        for (other in Btn.entries) if (other != btn) {
            val keys = keysOf(props, prefix + other.prop)
            if (name in keys) props.setProperty(prefix + other.prop, (keys - name).joinToString(", "))
        }
        props.setProperty(prefix + btn.prop, name)
        store(dir, props)
    }

    /** Leaves [btn] unbound on [pad] - TURBO A / B only ([Btn.canClear]). */
    fun clearBinding(dir: File, btn: Btn, pad: Pad? = currentPad) {
        if (!btn.canClear) return
        val props = readProps(dir)
        props.setProperty(writePrefix(props, pad) + btn.prop, "")
        store(dir, props)
    }

    /** RESET TO DEFAULT: [pad]'s buttons (the shared map's with none) back to the app's defaults; other
     *  controllers keep theirs. */
    fun resetToDefault(dir: File, pad: Pad? = currentPad) {
        val props = readProps(dir)
        val prefix = writePrefix(props, pad)
        for ((k, v) in DEFAULTS) props.setProperty(prefix + k, v)
        store(dir, props)
    }

    private fun resolveKey(name: String): Int? {
        if (name.isEmpty()) return null
        return runCatching { KeyEvent::class.java.getField("KEYCODE_$name").getInt(null) }
            .onFailure { Log.w("pokedaisy", "unknown control key: $name") }.getOrNull()
    }
}
