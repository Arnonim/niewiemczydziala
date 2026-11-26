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
    private var rotState = 0
    private var rotAt = 0L

    private val CB = Regex("verify you are human|potwierd[źz]|jeste[śs] cz[łl]owiekiem|nie jestem robotem|weryfikacja", RegexOption.IGNORE_CASE)
    private val TILE = Regex("obrazek|image|kafelek|tile|kliknij|zaznacz", RegexOption.IGNORE_CASE)
    private val AIR = Regex("tryb samolotowy|airplane mode|samolot", RegexOption.IGNORE_CASE)
    private val OK = Regex("^(ok|wy[łl][ąa]cz|w[łl][ąa]cz|tak)$", RegexOption.IGNORE_CASE)

    override fun onServiceConnected() {
        super.onServiceConnected()
        RunState.a11yConnected = true
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {
        if (RunState.rotateDone < RunState.rotateRequest) { handleRotate(); return }
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
            buzz()
        }
    }

    private fun handleRotate() {
        val now = System.currentTimeMillis()
        val root = rootInActiveWindow ?: return
        val nodes = ArrayList<AccessibilityNodeInfo>(256)
        walk(root, nodes, 0)

        val sw = nodes.firstOrNull { isAirplane(it) }
        when (rotState) {
            0 -> {
                if (sw == null) return
                if (!sw.isChecked) click(sw)
                rotState = 1; rotAt = now
            }
            1 -> {
                if (now - rotAt < 6000) return
                val ok = nodes.firstOrNull { it.isClickable && OK.containsMatchIn(desc(it)) }
                if (ok != null && sw != null && sw.isChecked) { click(ok); return }
                if (sw != null && sw.isChecked) click(sw)
                rotState = 2; rotAt = now
            }
            else -> {
                if (now - rotAt < 4000) return
                performGlobalAction(GLOBAL_ACTION_BACK)
                rotState = 0
                RunState.rotateDone++
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

    private fun isAirplane(n: AccessibilityNodeInfo): Boolean {
        if (!AIR.containsMatchIn(desc(n))) return false
        return n.isCheckable || n.className?.toString()?.contains("Switch", true) == true ||
            n.isClickable || n.parent?.isClickable == true
    }

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
        if (n.parent != null && n.parent.isClickable &&
            n.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun buzz() {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= 26)
            v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(120)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        RunState.a11yConnected = false
        super.onDestroy()
    }
}
