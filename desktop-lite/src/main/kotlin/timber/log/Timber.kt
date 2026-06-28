package timber.log

/**
 * Minimal no-Android replacement for Jake Wharton's Timber.
 *
 * The reused `innertube` source logs via `timber.log.Timber`, whose real artifact depends on
 * `android.util.Log` and therefore cannot run on a plain JVM. This shim mirrors the subset of the
 * Timber API that innertube actually calls (d / v / i / w / e, with String-first and
 * Throwable-first overloads) and routes output to stdout/stderr.
 *
 * Debug/verbose/info logs are silent unless the env var METROLIST_DEBUG is set, keeping the
 * console clean for the MVP while still allowing diagnosis when needed.
 */
object Timber {

    private val debug: Boolean = System.getenv("METROLIST_DEBUG") != null

    /** Timber.tag(...).d(...) — we ignore the tag and return the same logger. */
    fun tag(tag: String?): Timber = this

    private fun fmt(message: String?, args: Array<out Any?>): String =
        if (args.isEmpty() || message == null) message ?: "" else runCatching { message.format(*args) }.getOrDefault(message)

    // ---- debug ----
    fun d(message: String?, vararg args: Any?) { if (debug) println("D: ${fmt(message, args)}") }
    fun d(t: Throwable?, message: String?, vararg args: Any?) { if (debug) println("D: ${fmt(message, args)} :: ${t?.message}") }
    fun d(t: Throwable?) { if (debug) println("D: ${t?.message}") }

    // ---- verbose ----
    fun v(message: String?, vararg args: Any?) { if (debug) println("V: ${fmt(message, args)}") }
    fun v(t: Throwable?, message: String?, vararg args: Any?) { if (debug) println("V: ${fmt(message, args)} :: ${t?.message}") }
    fun v(t: Throwable?) { if (debug) println("V: ${t?.message}") }

    // ---- info ----
    fun i(message: String?, vararg args: Any?) { if (debug) println("I: ${fmt(message, args)}") }
    fun i(t: Throwable?, message: String?, vararg args: Any?) { if (debug) println("I: ${fmt(message, args)} :: ${t?.message}") }
    fun i(t: Throwable?) { if (debug) println("I: ${t?.message}") }

    // ---- warn ----
    fun w(message: String?, vararg args: Any?) { System.err.println("W: ${fmt(message, args)}") }
    fun w(t: Throwable?, message: String?, vararg args: Any?) { System.err.println("W: ${fmt(message, args)} :: ${t?.message}") }
    fun w(t: Throwable?) { System.err.println("W: ${t?.message}") }

    // ---- error ----
    fun e(message: String?, vararg args: Any?) { System.err.println("E: ${fmt(message, args)}") }
    fun e(t: Throwable?, message: String?, vararg args: Any?) {
        System.err.println("E: ${fmt(message, args)} :: ${t?.message}")
        if (debug) t?.printStackTrace()
    }
    fun e(t: Throwable?) {
        System.err.println("E: ${t?.message}")
        if (debug) t?.printStackTrace()
    }
}
