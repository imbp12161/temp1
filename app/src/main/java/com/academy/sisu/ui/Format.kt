package com.academy.sisu.ui

import java.time.LocalDate

/** 일요일(0) ~ 토요일(6) 인덱스용 한글 요일 */
val WD_KO = listOf("일", "월", "화", "수", "목", "금", "토")

/** java.time DayOfWeek.value(월=1..일=7) -> 일0..토6 인덱스 */
fun dowIndex(date: LocalDate): Int = date.dayOfWeek.value % 7

fun weekdayKo(date: LocalDate): String = WD_KO[dowIndex(date)]

/** "6월 18일(목)" */
fun fmtKDate(date: LocalDate): String =
    "${date.monthValue}월 ${date.dayOfMonth}일(${weekdayKo(date)})"

/** 등원 요일 리스트(1=월..5=금) -> "월·수·금" */
fun weekdaysLabel(weekdays: List<Int>): String =
    weekdays.sorted().joinToString("·") { WD_KO[it % 7] }
