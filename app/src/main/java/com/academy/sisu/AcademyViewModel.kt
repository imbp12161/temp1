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
import com.academy.sisu.data.Schedule
import com.academy.sisu.data.SessionSeg
import com.academy.sisu.data.Student
import java.time.LocalDate
import java.util.UUID

class AcademyViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    val students = mutableStateListOf<Student>()
    val groups = mutableStateListOf<String>()

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
    private var paintKind = 0                 // 0=방학(전체), 1=개인 휴일(학생)
    private var paintStudentId: String? = null
    private var paintSnapshotVac: Set<LocalDate> = emptySet()

    /** 개인 휴일 칠하기 라이브 상태 (드래그 중에만 사용; 커밋 시 학생 offDays 로 반영).
     *  방학과 동일하게 calcVersion 을 건드리지 않아 드래그 중 재계산/깜빡임이 없다. */
    var offLiveStudent by mutableStateOf<String?>(null)
        private set
    var offLive by mutableStateOf<Set<LocalDate>>(emptySet())
        private set

    init {
        students.addAll(repo.loadStudents())
        groups.addAll(repo.loadGroups())
        vacations = repo.loadVacations()
        holOff = repo.loadHolOff()
        selected = repo.loadSelected()
        if (selected != "all" && students.none { it.id == selected }) selected = "all"
    }

    fun isHolidayActive(d: LocalDate) = Holidays.MAP.containsKey(d) && !holOff.contains(d)
    fun holidayName(d: LocalDate): String? = Holidays.MAP[d]
    fun isVacation(d: LocalDate) = vacations.contains(d)

    /** 학생 개인 휴일 여부 (드래그 중 라이브 상태 반영) */
    fun isOff(studentId: String, d: LocalDate): Boolean {
        if (offLiveStudent == studentId) return offLive.contains(d)
        return students.firstOrNull { it.id == studentId }?.offDays?.contains(d) == true
    }

    fun select(id: String) {
        selected = id
        repo.saveSelected(id)
    }

    fun nextColor(): Long {
        val used = students.map { it.color }.toSet()
        return PALETTE.firstOrNull { it !in used } ?: PALETTE[students.size % PALETTE.size]
    }

    private fun ensureGroup(name: String) {
        val n = name.trim()
        if (n.isNotEmpty() && !groups.contains(n)) {
            groups.add(n)
            repo.saveGroups(groups.toList())
        }
    }

    fun addGroup(name: String) = ensureGroup(name)

    fun renameGroup(old: String, newName: String) {
        val n = newName.trim()
        val idx = groups.indexOf(old)
        if (n.isEmpty() || idx < 0 || (n != old && groups.contains(n))) return
        groups[idx] = n
        for (i in students.indices) {
            if (students[i].group == old) students[i] = students[i].copy(group = n)
        }
        repo.saveGroups(groups.toList())
        repo.saveStudents(students)
        calcVersion++
    }

    fun deleteGroup(name: String) {
        if (groups.remove(name)) {
            for (i in students.indices) {
                if (students[i].group == name) students[i] = students[i].copy(group = "")
            }
            repo.saveGroups(groups.toList())
            repo.saveStudents(students)
            calcVersion++
        }
    }

    fun addStudent(name: String, weekdays: List<Int>, sessions: Int, start: LocalDate, color: Long, group: String): String {
        val id = "s" + System.currentTimeMillis().toString(36) + UUID.randomUUID().toString().take(4)
        val wd = weekdays.distinct().sorted()
        students.add(Student(id, name.trim(), listOf(Schedule(start, wd)), listOf(SessionSeg(start, sessions.coerceAtLeast(1))), start, color, group.trim()))
        ensureGroup(group)
        repo.saveStudents(students)
        select(id)
        return id
    }

    /**
     * 학생 정보 수정.
     * 요일이 바뀐 경우에만 changeFrom(변경 적용일)부터의 새 구간을 추가한다.
     * 그 전 기록(이미 진행한 수업)은 그대로 유지되고, 변경일부터 새 요일로 이어서 계산된다.
     */
    fun updateStudent(
        id: String,
        name: String,
        weekdays: List<Int>,
        sessions: Int,
        start: LocalDate,
        color: Long,
        group: String,
        changeFrom: LocalDate,
        sessionChangeFrom: LocalDate
    ) {
        val i = students.indexOfFirst { it.id == id }
        if (i < 0) return
        val cur = students[i]
        val newWd = weekdays.distinct().sorted()
        var schedules = cur.schedules
        if (newWd != cur.weekdays) {
            val eff = if (changeFrom.isBefore(start)) start else changeFrom
            val list = cur.schedules.filter { it.from != eff }.toMutableList()
            list.add(Schedule(eff, newWd))
            list.sortBy { it.from }
            schedules = list
        }
        var sessionPlan = cur.sessionPlan
        if (sessions != cur.sessions) {
            val eff = if (sessionChangeFrom.isBefore(start)) start else sessionChangeFrom
            val list = cur.sessionPlan.filter { it.from != eff }.toMutableList()
            list.add(SessionSeg(eff, sessions.coerceAtLeast(1)))
            list.sortBy { it.from }
            sessionPlan = list
        }
        students[i] = cur.copy(
            name = name.trim(),
            schedules = schedules,
            sessionPlan = sessionPlan,
            start = start,
            color = color,
            group = group.trim()
        )
        ensureGroup(group)
        repo.saveStudents(students)
        calcVersion++
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

    // ---------- 길게 눌러 + 끌어서 칠하기 (전체=방학 / 학생 선택 시=그 학생 개인 휴일) ----------
    /** @return true 면 '추가' 모드, false 면 '해제' 모드 */
    fun startPaint(d: LocalDate): Boolean {
        val sel = selected
        if (sel == "all") {
            paintKind = 0
            paintSnapshotVac = vacations
            paintAdd = !vacations.contains(d)
            vacations = if (paintAdd) vacations + d else vacations - d
        } else {
            paintKind = 1
            paintStudentId = sel
            val base = students.firstOrNull { it.id == sel }?.offDays?.toSet() ?: emptySet()
            offLiveStudent = sel
            paintAdd = !base.contains(d)
            offLive = if (paintAdd) base + d else base - d
        }
        return paintAdd
    }

    fun paint(d: LocalDate) {
        if (paintKind == 0) {
            vacations = if (paintAdd) vacations + d else vacations - d
        } else {
            offLive = if (paintAdd) offLive + d else offLive - d
        }
    }

    /** 칠하기 도중 취소(좌우 스와이프 등) 시 시작 전 상태로 되돌림 (저장 안 함) */
    fun cancelPaint() {
        if (paintKind == 0) {
            vacations = paintSnapshotVac
        } else {
            offLiveStudent = null
            offLive = emptySet()
        }
    }

    fun commitPaint() {
        if (paintKind == 0) {
            repo.saveVacations(vacations)
        } else {
            val sid = paintStudentId
            val i = students.indexOfFirst { it.id == sid }
            if (i >= 0) {
                students[i] = students[i].copy(offDays = offLive.toList().sorted())
                repo.saveStudents(students)
            }
            offLiveStudent = null
            offLive = emptySet()
        }
        calcVersion++
    }

    /** 특정 학생의 개인 휴일 하루 토글 (날짜 상세에서 사용) */
    fun toggleOff(studentId: String, d: LocalDate) {
        val i = students.indexOfFirst { it.id == studentId }
        if (i < 0) return
        val cur = students[i].offDays.toMutableList()
        if (cur.contains(d)) cur.remove(d) else cur.add(d)
        students[i] = students[i].copy(offDays = cur.sorted())
        repo.saveStudents(students)
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
    fun exportString(): String = repo.exportJson(students.toList(), vacations, holOff, groups.toList())

    fun importFrom(text: String): Boolean = try {
        val data = repo.importJson(text)
        students.clear()
        students.addAll(data.students)
        groups.clear()
        groups.addAll(data.groups)
        vacations = data.vacations
        holOff = data.holOff
        select("all")
        repo.saveStudents(students)
        repo.saveGroups(groups.toList())
        repo.saveVacations(vacations)
        repo.saveHolOff(holOff)
        calcVersion++
        true
    } catch (e: Exception) {
        false
    }
}
