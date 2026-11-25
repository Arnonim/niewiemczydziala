package pl.ospzelazkow.glos.a11y

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class HarnasAccessibilityService : AccessibilityService() {

    private var lastClick = 0L
    private val CB = Regex("verify you are human|potwierd[Ĺşz]|jeste[Ĺ›s] cz[Ĺ‚l]owiekiem|nie jestem robotem|weryfikacja", RegexOption.IGNORE_CASE)
    private val TILE = Regex("obrazek|image|kafelek|tile|kliknij|zaznacz", RegexOption.IGNORE_CASE)

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (!RunState.running) return
        val now = System.currentTimeMillis()
        if (now - lastClick < 500) return
        val root = rootInActiveWindow ?: return

        val nodes = ArrayList<AccessibilityNodeInfo>(256)
        walk(root, nodes, 0)

        val box = nodes.firstOrNull { isBox(it) }
        if (box != null && !checked(box) && click(box)) { lastClick = now; return }

        if (nodes.count { it.isClickable && TILE.containsMatchIn(desc(it)) } >= 4 &&
            now - RunState.lastChallenge > 12_000) {
            RunState.lastChallenge = now
            (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.let {
                if (Build.VERSION.SDK_INT >= 26)
                    it.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                else @Suppress("DEPRECATION") it.vibrate(120)
            }
        }
    }

    private fun walk(n: AccessibilityNodeInfo?, out: MutableList<AccessibilityNodeInfo>, d: Int) {
        if (n == null || d > 25) return
        out += n
        for (i in 0 until n.childCount) walk(n.getChild(i), out, d + 1)
    }

    private fun desc(n: AccessibilityNodeInfo) =
        listOfNotNull(n.contentDescription, n.text, n.className).joinToString(" ")

    private fun isBox(n: AccessibilityNodeInfo): Boolean {
        val c = n.className?.toString().orEmpty()
        if (c.contains("CheckBox", true) || c.contains("Switch", true)) return true
        if (!CB.containsMatchIn(desc(n))) return false
        return n.isClickable || n.parent?.isClickable == true
    }

    private fun checked(n: AccessibilityNodeInfo): Boolean {
        if (n.isChecked) return true
        val s = (n.stateDescription?.toString().orEmpty() + " " + desc(n)).lowercase()
        return s.contains("checked") || (s.contains("zaznaczon") && !s.contains("niezaznaczon"))
    }

    private fun click(n: AccessibilityNodeInfo): Boolean {
        if (n.isClickable && n.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        var p = n.parent; var g = 0
        while (p != null && g++ < 4) {
            if (p.isClickable && p.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            p = p.parent
        }
        return false
    }

    override fun onInterrupt() = Unit
}