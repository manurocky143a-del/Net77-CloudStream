package com.horis.net77

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent SharedPreferences cache for the t_hash_t session cookie.
 * Cookies are valid for ~15 hours; we re-fetch after that.
 */
object Net77Storage {
    private const val PREFS_NAME    = "Net77Prefs"
    private const val KEY_COOKIE    = "t_hash_t"
    private const val KEY_TIMESTAMP = "cookie_ts"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getCookie(): Pair<String?, Long?> {
        val p = prefs ?: return Pair(null, null)
        val cookie = p.getString(KEY_COOKIE, null)
        val ts     = if (p.contains(KEY_TIMESTAMP)) p.getLong(KEY_TIMESTAMP, 0L) else null
        return Pair(cookie, ts)
    }

    fun saveCookie(cookie: String) {
        prefs?.edit()
            ?.putString(KEY_COOKIE, cookie)
            ?.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            ?.apply()
    }

    fun clearCookie() {
        prefs?.edit()
            ?.remove(KEY_COOKIE)
            ?.remove(KEY_TIMESTAMP)
            ?.apply()
    }
}
