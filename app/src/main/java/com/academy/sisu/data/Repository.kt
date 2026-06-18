package com.academy.sisu.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate

/** SharedPreferences + JSON 기반 로컬 저장소. (기기에만 저장됩니다) */
class Repository(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("sisu_store", Context.MODE_PRIVATE)

    // ---------- students ----------
    fun loadStudents(): List<Student> = try {
        val raw = sp.getString(KEY_STUDENTS, null) ?: return emptyList()
        parseStudents(JSONArray(raw))
    } catch (e: Exception) { emptyList() }

    fun saveStudents(list: List<Student>) {
        sp.edit().putString(KEY_STUDENTS, studentsToJson(list).toString()).apply()
    }

    // ---------- vacations ----------
    fun loadVacations(): Set<LocalDate> = parseDateSet(sp.getString(KEY_VAC, null))
    fun saveVacations(set: Set<LocalDate>) {
        sp.edit().putString(KEY_VAC, dateSetToJson(set).toString()).apply()
    }

    // ---------- holiday off (수업일로 처리할 공휴일) ----------
    fun loadHolOff(): Set<LocalDate> = parseDateSet(sp.getString(KEY_HOLOFF, null))
    fun saveHolOff(set: Set<LocalDate>) {
        sp.edit().putString(KEY_HOLOFF, dateSetToJson(set).toString()).apply()
    }

    // ---------- selected ----------
    fun loadSelected(): String = sp.getString(KEY_SEL, "all") ?: "all"
    fun saveSelected(s: String) { sp.edit().putString(KEY_SEL, s).apply() }

    // ---------- export / import ----------
    fun exportJson(students: List<Student>, vac: Set<LocalDate>, holOff: Set<LocalDate>): String {
        val o = JSONObject()
        o.put("app", "sisu-calendar")
        o.put("version", 2)
        o.put("exportedAt", Instant.now().toString())
        o.put("students", studentsToJson(students))
        o.put("vacations", dateSetToJson(vac))
        o.put("holOff", dateSetToJson(holOff))
        return o.toString(2)
    }

    data class ImportData(
        val students: List<Student>,
        val vacations: Set<LocalDate>,
        val holOff: Set<LocalDate>
    )

    fun importJson(text: String): ImportData {
        val o = JSONObject(text)
        val students = parseStudents(o.optJSONArray("students") ?: JSONArray())
        val vac = parseDateArray(o.optJSONArray("vacations"))
        val holOff = parseDateArray(o.optJSONArray("holOff"))
        return ImportData(students, vac, holOff)
    }

    // ---------- helpers ----------
    private fun studentsToJson(list: List<Student>): JSONArray {
        val arr = JSONArray()
        for (s in list) {
            val o = JSONObject()
            o.put("id", s.id)
            o.put("name", s.name)
            val wd = JSONArray()
            s.weekdays.forEach { wd.put(it) }
            o.put("weekdays", wd)
            o.put("sessions", s.sessions)
            o.put("start", s.start.toString())
            o.put("color", s.color)
            o.put("createdAt", s.createdAt)
            arr.put(o)
        }
        return arr
    }

    private fun parseStudents(arr: JSONArray): List<Student> {
        val out = ArrayList<Student>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val wdArr = o.optJSONArray("weekdays") ?: JSONArray()
            val wd = ArrayList<Int>()
            for (j in 0 until wdArr.length()) wd.add(wdArr.optInt(j))
            val start = try { LocalDate.parse(o.optString("start")) } catch (e: Exception) { LocalDate.now() }
            out.add(
                Student(
                    id = o.optString("id").ifBlank { "s" + System.nanoTime().toString(36) },
                    name = o.optString("name"),
                    weekdays = wd.filter { it in 1..7 }.distinct().sorted(),
                    sessions = o.optInt("sessions", 12),
                    start = start,
                    color = o.optLong("color", PALETTE[0]),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return out
    }

    private fun dateSetToJson(set: Set<LocalDate>): JSONArray {
        val arr = JSONArray()
        set.forEach { arr.put(it.toString()) }
        return arr
    }

    private fun parseDateSet(raw: String?): Set<LocalDate> {
        if (raw.isNullOrBlank()) return emptySet()
        return try { parseDateArray(JSONArray(raw)) } catch (e: Exception) { emptySet() }
    }

    private fun parseDateArray(arr: JSONArray?): Set<LocalDate> {
        if (arr == null) return emptySet()
        val out = LinkedHashSet<LocalDate>()
        for (i in 0 until arr.length()) {
            try { out.add(LocalDate.parse(arr.optString(i))) } catch (e: Exception) { /* skip */ }
        }
        return out
    }

    companion object {
        private const val KEY_STUDENTS = "students"
        private const val KEY_VAC = "vacations"
        private const val KEY_HOLOFF = "holOff"
        private const val KEY_SEL = "selected"
    }
}
