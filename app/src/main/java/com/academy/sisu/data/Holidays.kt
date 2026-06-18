package com.academy.sisu.data

import java.time.LocalDate

/**
 * 대한민국 공휴일 (학원 등원일 자동 제외용).
 * - 2026: 인사혁신처 공휴일 안내 기준 + 근로자의 날(5/1)·제헌절(7/17) 반영.
 * - 2027: 대통령령 기준 + 대체공휴일 규칙(요일 검증).
 * 모든 항목은 앱의 '공휴일 관리'에서 켜고 끌 수 있습니다.
 */
object Holidays {
    val MAP: Map<LocalDate, String> = linkedMapOf(
        LocalDate.of(2026, 1, 1) to "신정",
        LocalDate.of(2026, 2, 16) to "설날 연휴",
        LocalDate.of(2026, 2, 17) to "설날",
        LocalDate.of(2026, 2, 18) to "설날 연휴",
        LocalDate.of(2026, 3, 1) to "삼일절",
        LocalDate.of(2026, 3, 2) to "삼일절 대체공휴일",
        LocalDate.of(2026, 5, 1) to "근로자의 날",
        LocalDate.of(2026, 5, 5) to "어린이날",
        LocalDate.of(2026, 5, 24) to "부처님오신날",
        LocalDate.of(2026, 5, 25) to "부처님오신날 대체공휴일",
        LocalDate.of(2026, 6, 6) to "현충일",
        LocalDate.of(2026, 7, 17) to "제헌절",
        LocalDate.of(2026, 8, 15) to "광복절",
        LocalDate.of(2026, 8, 17) to "광복절 대체공휴일",
        LocalDate.of(2026, 9, 24) to "추석 연휴",
        LocalDate.of(2026, 9, 25) to "추석",
        LocalDate.of(2026, 9, 26) to "추석 연휴",
        LocalDate.of(2026, 10, 3) to "개천절",
        LocalDate.of(2026, 10, 5) to "개천절 대체공휴일",
        LocalDate.of(2026, 10, 9) to "한글날",
        LocalDate.of(2026, 12, 25) to "크리스마스",

        LocalDate.of(2027, 1, 1) to "신정",
        LocalDate.of(2027, 2, 6) to "설날 연휴",
        LocalDate.of(2027, 2, 7) to "설날",
        LocalDate.of(2027, 2, 8) to "설날 연휴",
        LocalDate.of(2027, 2, 9) to "설날 대체공휴일",
        LocalDate.of(2027, 3, 1) to "삼일절",
        LocalDate.of(2027, 5, 1) to "근로자의 날",
        LocalDate.of(2027, 5, 5) to "어린이날",
        LocalDate.of(2027, 5, 13) to "부처님오신날",
        LocalDate.of(2027, 6, 6) to "현충일",
        LocalDate.of(2027, 7, 17) to "제헌절",
        LocalDate.of(2027, 7, 19) to "제헌절 대체공휴일",
        LocalDate.of(2027, 8, 15) to "광복절",
        LocalDate.of(2027, 8, 16) to "광복절 대체공휴일",
        LocalDate.of(2027, 9, 14) to "추석 연휴",
        LocalDate.of(2027, 9, 15) to "추석",
        LocalDate.of(2027, 9, 16) to "추석 연휴",
        LocalDate.of(2027, 10, 3) to "개천절",
        LocalDate.of(2027, 10, 4) to "개천절 대체공휴일",
        LocalDate.of(2027, 10, 9) to "한글날",
        LocalDate.of(2027, 10, 11) to "한글날 대체공휴일",
        LocalDate.of(2027, 12, 25) to "크리스마스",
        LocalDate.of(2027, 12, 27) to "크리스마스 대체공휴일"
    )

    /** 캘린더 표시용 짧은 이름 */
    fun shortName(name: String): String =
        name.replace(" 연휴", "")
            .replace(" 대체공휴일", " 대체")
            .replace("부처님오신날", "석탄일")
}
