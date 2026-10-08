package au.bpi.inspect

import android.app.Application
import au.bpi.inspect.data.AppDb
import au.bpi.inspect.data.J
import au.bpi.inspect.data.LibComment
import au.bpi.inspect.data.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

class BpiApp : Application() {
    lateinit var db: AppDb
        private set
    lateinit var prefs: Prefs
        private set
    /** Trade names, e.g. "QBCC licensed builder", for the trade picker. */
    var trades: List<String> = emptyList()
        private set

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        db = AppDb.build(this)
        prefs = Prefs(this)
        val json = JSONObject(assets.open("library.json").bufferedReader().use { it.readText() })
        val tr = json.getJSONObject("trades")
        val tradeMap = tr.keys().asSequence().associateWith { tr.getString(it) }
        trades = tradeMap.values.toList()
        scope.launch { seedLibrary(json, tradeMap) }
    }

    /** First launch only: load the standard comment library. Your own additions and edits are never overwritten. */
    private suspend fun seedLibrary(json: JSONObject, tradeMap: Map<String, String>) {
        val dao = db.library()
        if (dao.count() > 0) return
        val out = mutableListOf<LibComment>()
        var sort = 0
        val lib = json.getJSONArray("library")
        for (i in 0 until lib.length()) {
            val a = lib.getJSONObject(i)
            val subs = a.getJSONArray("subs")
            for (j in 0 until subs.length()) {
                val s = subs.getJSONObject(j)
                val cs = s.getJSONArray("comments")
                for (k in 0 until cs.length()) {
                    val c = cs.getJSONObject(k)
                    val t = c.getJSONArray("trades")
                    val names = List(t.length()) { tradeMap[t.getString(it)] ?: t.getString(it) }
                    out += LibComment(
                        area = a.getString("area"), sub = s.getString("name"),
                        cls = c.getString("cls"), text = c.getString("text"),
                        tradesJson = J.list(names), sort = sort++
                    )
                }
            }
        }
        dao.insertAll(out)
    }

    companion object {
        lateinit var instance: BpiApp
            private set
    }
}
