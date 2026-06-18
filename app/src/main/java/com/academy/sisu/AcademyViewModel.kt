package com.academy.sisu

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.academy.sisu.data.Holidays
import com.academy.sisu.data.PALETTE
import com.academy.sisu.data.Repository
import com.academy.sisu.data.Student
import java.time.LocalDate
import java.util.UUID

class AcademyViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    val students = mutableStateListOf<Student>()

    var vacations by mutableStateOf<Set<LocalDate>>(emptySet())
        private set
    var holOff by mutableStateOf<Set<LocalDate>>(emptySet())
        private set
    var selected by mutableStateOf("all")
        private set

    /** 방학/공휴일 등 '계산에 영향을 주는' 확정 변경이 일어날 때마다 +1.
     *  드래그 중(미확정)에는 올리지 않아 화면 계산이 깜빡이지 않게 한다. */
    var calcVersion by mutableStateOf(0)
        private set

    private var paintAdd = true

    init {
        students.addAll(repo.loadStudents())
        vacations = repo.loadVacations()
        holOff = repo.loadHolOff()
        selected = repo.loadSelected()
        if (selected != "all" && students.none { it.id == selected }) selected = "all"
    }

    fun isHolidayActive(d: LocalDate) = Holidays.MAP.containsKey(d) && !holOff.contains(d)
    fun holidayName(d: LocalDate): String? = Holidays.MAP[d]
    fun isVacation(d: LocalDate) = vacations.contains(d)

    fun select(id: String) {
        selected = id
        repo.saveSelected(id)
    }

    fun nextColor(): Long {
        val used = students.map { it.color }.toSet()
        return PALETTE.firstOrNull { it !in used } ?: PALETTE[students.size % PALETTE.size]
    }

    fun addStudent(name: String, weekdays: List<Int>, sessions: Int, start: LocalDate, color: Long): String {
        val id = "s" + System.currentTimeMillis().toString(36) + UUID.randomUUID().toString().take(4)
        students.add(Student(id, name.trim(), weekdays.distinct().sorted(), sessions, start, color))
        repo.saveStudents(students)
        select(id)
        return id
    }

    fun updateStudent(id: String, name: String, weekdays: List<Int>, sessions: Int, start: LocalDate, color: Long) {
        val i = students.indexOfFirst { it.id == id }
        if (i >= 0) {
            students[i] = students[i].copy(
                name = name.trim(),
                weekdays = weekdays.distinct().sorted(),
                sessions = sessions,
                start = start,
                color = color
            )
            repo.saveStudents(students)
            calcVersion++
        }
    }

    fun deleteStudent(id: String) {
        students.removeAll { it.id == id }
        if (selected == id) select("all")
        repo.saveStudents(students)
        calcVersion++
    }

    // ---------- 방학: 단일 토글 ----------
    fun toggleVacation(d: LocalDate) {
        vacations = if (vacations.contains(d)) vacations - d else vacations + d
        repo.saveVacations(vacations)
        calcVersion++
    }

    // ---------- 방학: 길게 눌러 + 끌어서 칠하기 ----------
    /** @return true 면 '추가' 모드, false 면 '해제' 모드 */
    fun startPaint(d: LocalDate): Boolean {
        paintAdd = !vacations.contains(d)
        vacations = if (paintAdd) vacations + d else vacations - d
        return paintAdd
    }

    fun paint(d: LocalDate) {
        vacations = if (paintAdd) vacations + d else vacations - d
    }

    fun commitPaint() {
        repo.saveVacations(vacations)
        calcVersion++
    }

    fun clearVacations() {
        vacations = emptySet()
        repo.saveVacations(vacations)
        calcVersion++
    }

    // ---------- 공휴일 토글 (off = 수업일로 처리) ----------
    fun toggleHoliday(d: LocalDate) {
        holOff = if (holOff.contains(d)) holOff - d else holOff + d
        repo.saveHolOff(holOff)
        calcVersion++
    }

    // ---------- 백업 ----------
    fun exportString(): String = repo.exportJson(students.toList(), vacations, holOff)

    fun importFrom(text: String): Boolean = try {
        val data = repo.importJson(text)
        students.clear()
        students.addAll(data.students)
        vacations = data.vacations
        holOff = data.holOff
        select("all")
        repo.saveStudents(students)
        repo.saveVacations(vacations)
        repo.saveHolOff(holOff)
        calcVersion++
        true
    } catch (e: Exception) {
        false
    }
}
