package com.prajwalch.torrentsearch

import android.util.Log
import android.view.KeyEvent

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry

import com.prajwalch.torrentsearch.ui.main.MainActivity

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FocusAuditTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val instr = InstrumentationRegistry.getInstrumentation()
    private val args = InstrumentationRegistry.getArguments()
    private val full = args.getString("full") == "1"

    private val keyCodes = mapOf(
        'd' to KeyEvent.KEYCODE_DPAD_DOWN, 'u' to KeyEvent.KEYCODE_DPAD_UP,
        'l' to KeyEvent.KEYCODE_DPAD_LEFT, 'r' to KeyEvent.KEYCODE_DPAD_RIGHT,
        'c' to KeyEvent.KEYCODE_DPAD_CENTER, 'e' to KeyEvent.KEYCODE_ENTER,
        'b' to KeyEvent.KEYCODE_BACK, 'x' to KeyEvent.KEYCODE_DEL,
    )

    private fun log(msg: String) {
        Log.i("FocusAudit", msg)
        println("FocusAudit: $msg")
    }

    private fun nodeLine(n: SemanticsNode): String {
        val cfg = n.config
        val text = cfg.getOrNull(SemanticsProperties.Text)?.joinToString("/")
        val desc = cfg.getOrNull(SemanticsProperties.ContentDescription)?.joinToString("/")
        val role = cfg.getOrNull(SemanticsProperties.Role)?.toString()
        val focused = cfg.getOrNull(SemanticsProperties.Focused)
        val click = cfg.getOrNull(SemanticsActions.OnClick) != null
        val sel = cfg.getOrNull(SemanticsProperties.Selected) == true
        val b = n.boundsInRoot
        val bStr = "[%.0f,%.0f][%.0f,%.0f]".format(b.left, b.top, b.right, b.bottom)
        return "bounds=$bStr focused=$focused role=$role click=$click selected=$sel " +
            "text='$text' desc='$desc'"
    }

    private fun dump(tag: String) {
        rule.waitForIdle()
        log(">>> $tag")
        val roots = rule.onAllNodes(isRoot()).fetchSemanticsNodes()
        log("  (${roots.size} root(s))")
        roots.forEachIndexed { ri, root ->
            fun walk(n: SemanticsNode, depth: Int) {
                val b = n.boundsInRoot
                val focused = n.config.getOrNull(SemanticsProperties.Focused) == true
                if (b.width > 0f && b.height > 0f && (focused || full)) {
                    log("  R$ri ${" ".repeat(depth)}#$n.id ${nodeLine(n)}")
                }
                n.children.forEach { walk(it, depth + 1) }
            }
            walk(root, 0)
        }
        log("<<< $tag")
    }

    private fun dumpAll(tag: String) = dump(tag)

    private fun key(c: Char) {
        val code = keyCodes[c]
        if (code != null) instr.sendKeyDownUpSync(code)
        rule.waitForIdle()
    }

    private fun walk(walkSeq: String) {
        dump("start")
        walkSeq.forEachIndexed { i, c ->
            key(c)
            dump("step$i:$c")
        }
    }

    private fun clickLabel(label: String) {
        rule.onNodeWithText(label).performClick()
        rule.waitForIdle()
    }

    private fun setupNoDialog() {
        // If the first-run "Enable search providers" dialog is shown, enable recommended
        // providers so the app is in its normal post-setup state.
        runCatching {
            rule.onNodeWithText("Enable").performClick()
            rule.waitForIdle()
        }
        runCatching {
            rule.waitUntil(15000) {
                runCatching { rule.onNodeWithText("Enable").assertExists() }.isFailure
            }
        }
    }

    @Test
    fun auditHome() {
        setupNoDialog()
        val seq = args.getString("seq") ?: "ddddrruu"
        walk(seq)
    }

    @Test
    fun auditSearchResults() {
        setupNoDialog()
        rule.onAllNodes(hasSetTextAction()).onFirst().performTextInput("test")
        rule.waitForIdle()
        clickLabel("Search")
        walk(args.getString("seq") ?: "ddddddrr")
    }

    @Test
    fun auditBrowse() {
        setupNoDialog()
        clickLabel("Browse")
        walk(args.getString("seq") ?: "ddddrrrruu")
    }

    @Test
    fun auditBookmarks() {
        setupNoDialog()
        key('d')
        key('c')
        walk(args.getString("seq") ?: "ddddrrrruu")
    }

    @Test
    fun auditSearchHistory() {
        setupNoDialog()
        key('d'); key('r'); key('c')
        walk(args.getString("seq") ?: "ddddddrr")
    }

    @Test
    fun auditSettings() {
        setupNoDialog()
        key('d'); key('r'); key('r'); key('c')
        walk(args.getString("seq") ?: "ddddrrrrddd")
    }

    @Test
    fun auditSearchProviders() {
        setupNoDialog()
        key('d'); key('r'); key('r'); key('c')
        dump("settings-start")
        rule.onNodeWithText("Search Providers", substring = true).performClick()
        rule.waitForIdle()
        walk(args.getString("seq") ?: "dddrdrdd")
    }

    @Test
    fun auditBackBehavior() {
        setupNoDialog()
        key('d') // focus search bar -> expands popup
        dump("expanded")
        key('b') // BACK
        dump("after-back")
        key('d')
        dump("after-back-down")
    }
}