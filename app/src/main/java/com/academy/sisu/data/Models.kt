package com.academy.sisu.data

import java.time.LocalDate

/** 학생 표시 색상 팔레트 (ARGB Long). Compose 에서 Color(student.color) 로 사용. */
val PALETTE: List<Long> = listOf(
    0xFF3B82F6L, 0xFF22C55EL, 0xFFEAB308L, 0xFFA855F7L, 0xFFEC4899L,
    0xFF14B8A6L, 0xFFF97316L, 0xFF06B6D4L, 0xFF84CC16L, 0xFFF43F5EL
)

/** 선택 가능한 수업 시수 */
val SESSION_OPTIONS = listOf(8, 12, 16, 20)

/**
 * 학생 1명.
 * weekdays: 등원 요일. 1=월 ... 5=금 (java.time DayOfWeek.value 와 동일하게 1=월 시작)
 * sessions: 한 사이클 등원 횟수 (8/12/16/20)
 * color: ARGB Long
 */
data class Student(
    val id: String,
    val name: String,
    val weekdays: List<Int>,
    val sessions: Int,
    val start: LocalDate,
    val color: Long,
    val createdAt: Long = System.currentTimeMillis()
)

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
