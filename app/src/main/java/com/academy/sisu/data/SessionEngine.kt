package com.academy.sisu.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 핵심 계산.
 * 시작일부터 하루씩 진행하며 '등원 요일'이고 공휴일·방학이 아니면 1회로 카운트.
 *  - count 1..N = 1차(파랑), N+1..2N = 2차(초록), ... (색 교대)
 *  - 각 사이클의 N번째 등원일 = 정산일(end)
 *  - 등원 요일이지만 공휴일/방학이라 빠진 날 = skip (뒤로 밀림)
 */
object SessionEngine {

    fun compute(
        student: Student,
        until: LocalDate,
        isHolidayActive: (LocalDate) -> Boolean,
        isVacation: (LocalDate) -> Boolean
    ): Computed {
        val n = student.sessions.coerceAtLeast(1)
        val classMap = LinkedHashMap<LocalDate, DayInfo>()
        val skipSet = LinkedHashSet<LocalDate>()
        val ends = ArrayList<LocalDate>()
        var count = 0
        var d = student.start
        var guard = 0
        while (!d.isAfter(until) && guard < 4000) {
            guard++
            val wd = d.dayOfWeek.value // 1=월 .. 7=일
            if (student.weekdays.contains(wd)) {
                if (isHolidayActive(d) || isVacation(d)) {
                    skipSet.add(d)
                } else {
                    count++
                    val cycle = (count - 1) / n
                    val idx = (count - 1) % n + 1
                    val end = idx == n
                    classMap[d] = DayInfo(cycle, idx, end)
                    if (end) ends.add(d)
                }
            }
            d = d.plusDays(1)
        }
        return Computed(classMap, skipSet, ends, n)
    }

    fun summary(student: Student, c: Computed, today: LocalDate): Summary {
        val n = student.sessions.coerceAtLeast(1)
        var attendedBefore = 0
        for ((ds, _) in c.classMap) {
            if (ds.isBefore(today)) attendedBefore++
        }
        val cycleNo = attendedBefore / n
        val doneInCycle = attendedBefore % n
        val targetGlobal = cycleNo * n + n
        var g = 0
        var endDate: LocalDate? = null
        for ((ds, _) in c.classMap) {
            g++
            if (g == targetGlobal) { endDate = ds; break }
        }
        val dday = endDate?.let { ChronoUnit.DAYS.between(today, it) }
        return Summary(
            cycleNo = cycleNo,
            doneInCycle = doneInCycle,
            n = n,
            endDate = endDate,
            dday = dday,
            startFuture = student.start.isAfter(today)
        )
    }
}
