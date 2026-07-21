package com.academy.sisu.data

import java.time.LocalDate

/** 학생 표시 색상 팔레트 (ARGB Long). Compose 에서 Color(student.color) 로 사용. */
val PALETTE: List<Long> = listOf(
    0xFF3B82F6L, 0xFF22C55EL, 0xFFEAB308L, 0xFFA855F7L, 0xFFEC4899L, 0xFF14B8A6L
)

/** 선택 가능한 수업 시수 */
val SESSION_OPTIONS = listOf(8, 12, 16, 20)

/** 한 구간의 등원 요일. from 날짜부터 이 요일 패턴을 적용한다. (요일 변경 이력용) */
data class Schedule(
    val from: LocalDate,
    val weekdays: List<Int>
)

/**
 * 학생 1명.
 * schedules: 등원 요일 이력. from 오름차순. 가장 최근 항목이 현재 요일.
 *   요일을 바꾸면 '변경 적용일'을 from 으로 하는 새 구간이 추가되어, 그 전 기록은 그대로 유지된다.
 * offDays: 이 학생만의 비정기 휴일(개인 휴가 등). 이 날 등원 요일이어도 수업으로 세지 않고 뒤로 밀린다.
 * sessions: 한 사이클 등원 횟수 (8/12/16/20)
 * color: ARGB Long
 */
data class Student(
    val id: String,
    val name: String,
    val schedules: List<Schedule>,
    val sessions: Int,
    val start: LocalDate,
    val color: Long,
    val group: String = "",
    val offDays: List<LocalDate> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
) {
    /** 현재(가장 최근) 등원 요일 — 표시·입력 기본값용 */
    val weekdays: List<Int>
        get() = (schedules.maxByOrNull { it.from } ?: schedules.firstOrNull())?.weekdays ?: emptyList()
}

/** 어떤 날짜의 한 학생 등원 정보. cycle: 0-based 사이클, idx: 사이클 내 1..N, end: 정산일 여부 */
data class DayInfo(val cycle: Int, val idx: Int, val end: Boolean)

/** 한 학생의 전체 계산 결과 */
data class Computed(
    val classMap: Map<LocalDate, DayInfo>,
    val skipSet: Set<LocalDate>,
    val ends: List<LocalDate>,
    val n: Int
)

/** 다음 정산일 요약 */
data class Summary(
    val cycleNo: Int,       // 0-based 현재 사이클
    val doneInCycle: Int,   // 현재 사이클에서 오늘까지 마친 횟수
    val n: Int,
    val endDate: LocalDate?,
    val dday: Long?,
    val startFuture: Boolean
)
