package com.academy.sisu.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 핵심 계산.
 * 시작일부터 하루씩 진행하며 '등원 요일'이고 공휴일·방학·개인휴일이 아니면 1회로 카운트.
 *  - 사이클 내 idx 를 누적하다가, 그 날짜에 적용되는 목표 시수(activeSessions)에 도달하면 정산일(end) → 다음 사이클로.
 *  - 시수를 중간에 바꾸면 '변경 적용일'부터 목표 시수가 달라지고, 진행 중 사이클은 새 목표에 맞춰 마감된다.
 *  - 등원 요일이지만 공휴일/방학이라 빠진 날 = skip (뒤로 밀림)
 */
object SessionEngine {

    /** date 시점에 적용되는 등원 요일. from<=date 중 가장 늦은 구간을 사용. */
    fun activeWeekdays(student: Student, date: LocalDate): List<Int> {
        var chosen: Schedule? = student.schedules.firstOrNull()
        for (s in student.schedules) {
            if (!s.from.isAfter(date)) {
                val c = chosen
                if (c == null || s.from.isAfter(c.from)) chosen = s
            }
        }
        return chosen?.weekdays ?: emptyList()
    }

    /** date 시점에 적용되는 수업 시수(사이클 목표). from<=date 중 가장 늦은 구간을 사용. */
    fun activeSessions(student: Student, date: LocalDate): Int {
        var chosen: SessionSeg? = student.sessionPlan.firstOrNull()
        for (s in student.sessionPlan) {
            if (!s.from.isAfter(date)) {
                val c = chosen
                if (c == null || s.from.isAfter(c.from)) chosen = s
            }
        }
        return (chosen?.sessions ?: 12).coerceAtLeast(1)
    }

    fun compute(
        student: Student,
        until: LocalDate,
        isHolidayActive: (LocalDate) -> Boolean,
        isVacation: (LocalDate) -> Boolean
    ): Computed {
        val classMap = LinkedHashMap<LocalDate, DayInfo>()
        val skipSet = LinkedHashSet<LocalDate>()
        val ends = ArrayList<LocalDate>()
        val offSet = student.offDays.toHashSet()
        var idx = 0          // 현재 사이클에서 센 횟수
        var cycleNo = 0
        var d = student.start
        var guard = 0
        while (!d.isAfter(until) && guard < 4000) {
            guard++
            val wd = d.dayOfWeek.value // 1=월 .. 7=일
            if (activeWeekdays(student, d).contains(wd)) {
                when {
                    offSet.contains(d) -> {
                        // 개인 휴일: 세지 않음 → 뒤로 밀림
                    }
                    isHolidayActive(d) || isVacation(d) -> skipSet.add(d)
                    else -> {
                        idx++
                        val target = activeSessions(student, d)
                        val end = idx >= target
                        classMap[d] = DayInfo(cycleNo, idx, target, end)
                        if (end) {
                            ends.add(d)
                            cycleNo++
                            idx = 0
                        }
                    }
                }
            }
            d = d.plusDays(1)
        }
        return Computed(classMap, skipSet, ends, student.sessions.coerceAtLeast(1))
    }

    /**
     * 오늘 기준 '다음 정산일'과 진행 상황 요약. (시수가 구간마다 달라도 classMap 을 그대로 이용해 정확히 계산)
     */
    fun summary(student: Student, c: Computed, today: LocalDate): Summary {
        val entries = c.classMap.entries.toList()   // 날짜 오름차순

        // 오늘 이후(당일 포함) 첫 정산일 = 현재/다음 사이클
        var cycleNo = 0
        var targetN = student.sessions.coerceAtLeast(1)
        var endDate: LocalDate? = null
        for (e in entries) {
            val info = e.value
            if (info.end && !e.key.isBefore(today)) {
                endDate = e.key
                cycleNo = info.cycle
                targetN = info.n
                break
            }
        }
        if (endDate == null) {
            val last = entries.lastOrNull()?.value
            if (last != null) {
                cycleNo = last.cycle
                targetN = last.n
            }
        }

        // 현재 사이클에서 오늘 전까지 마친 횟수
        var doneInCycle = 0
        for (e in entries) {
            if (e.value.cycle == cycleNo && e.key.isBefore(today)) doneInCycle++
        }

        val dday = endDate?.let { ChronoUnit.DAYS.between(today, it) }
        return Summary(
            cycleNo = cycleNo,
            doneInCycle = doneInCycle,
            n = targetN,
            endDate = endDate,
            dday = dday,
            startFuture = student.start.isAfter(today)
        )
    }
}
