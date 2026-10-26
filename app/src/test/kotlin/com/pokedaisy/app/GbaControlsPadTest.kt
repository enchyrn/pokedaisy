package com.pokedaisy.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** GAME BUTTONS keeps a map per controller: binding on one never changes another's, and each resets on its own. */
class GbaControlsPadTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val keyboard = GbaControls.Pad("kbd0001", "Keyboard")
    private val nova = GbaControls.Pad("nova0002", "Nova Lite")

    private val buttonA = 96   // KEYCODE_BUTTON_A
    private val z = 54         // KEYCODE_Z
    private val buttonY = 100  // KEYCODE_BUTTON_Y

    private fun dir(): File = tmp.newFolder()

    @Test fun bindingOnOneControllerLeavesTheOtherAlone() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "Z", keyboard)
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", nova)

        assertEquals(listOf("Z"), GbaControls.rawBindings(d, keyboard.key)[GbaControls.Btn.A])
        assertEquals(listOf("BUTTON_Y"), GbaControls.rawBindings(d, nova.key)[GbaControls.Btn.A])
    }

    @Test fun aControllerWithoutItsOwnMapUsesTheSharedOne() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", nova)

        assertFalse(GbaControls.hasOwnMap(d, keyboard.key))
        assertEquals(GbaControls.rawBindings(d, null), GbaControls.rawBindings(d, keyboard.key))
        assertEquals(listOf("BUTTON_A", "Z"), GbaControls.rawBindings(d, null)[GbaControls.Btn.A])
    }

    @Test fun aControllersFirstBindingStartsFromTheSharedMap() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", nova)

        assertTrue(GbaControls.hasOwnMap(d, nova.key))
        // B wasn't touched: it keeps what the shared map had.
        assertEquals(GbaControls.rawBindings(d, null)[GbaControls.Btn.B], GbaControls.rawBindings(d, nova.key)[GbaControls.Btn.B])
    }

    @Test fun theGameReadsEachControllersOwnMap() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", nova)
        val input = GbaInput().apply { setControls(GbaControls.loadAll(d)) }

        input.onKey(buttonY, true, nova.key)
        assertEquals(MgbaCore.Key.A, input.mask and MgbaCore.Key.A)
        input.onKey(buttonY, false, nova.key)

        // The keyboard still has the shared map: Z is A there, and the Nova's BUTTON_A no longer is.
        input.onKey(z, true, keyboard.key)
        assertEquals(MgbaCore.Key.A, input.mask and MgbaCore.Key.A)
        input.onKey(z, false, keyboard.key)
        input.onKey(buttonA, true, nova.key)
        assertEquals(0, input.mask and MgbaCore.Key.A)
    }

    @Test fun resetPutsOneControllerBackToTheDefaults() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", nova)
        GbaControls.setBindingName(d, GbaControls.Btn.A, "Z", keyboard)
        GbaControls.setBindingName(d, GbaControls.Btn.B, "Z", keyboard)

        GbaControls.resetToDefault(d, nova)

        assertEquals(listOf("BUTTON_A", "Z"), GbaControls.rawBindings(d, nova.key)[GbaControls.Btn.A])
        assertEquals(listOf("Z"), GbaControls.rawBindings(d, keyboard.key)[GbaControls.Btn.B])
    }

    @Test fun resetWithNoControllerResetsTheSharedMap() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", null)
        GbaControls.resetToDefault(d, null)
        assertEquals(listOf("BUTTON_A", "Z"), GbaControls.rawBindings(d, null)[GbaControls.Btn.A])
    }

    @Test fun aControllersNameIsKept() {
        val d = dir()
        GbaControls.setBindingName(d, GbaControls.Btn.A, "BUTTON_Y", nova)
        assertEquals("Nova Lite", GbaControls.padName(d, nova.key))
    }

    @Test fun mapsSavedBeforeControllersHadTheirOwnStillLoad() {
        val d = dir()
        File(d, "controls.properties").writeText("a=BUTTON_Y\n")
        assertEquals(listOf("BUTTON_Y"), GbaControls.rawBindings(d, nova.key)[GbaControls.Btn.A])
    }
}
