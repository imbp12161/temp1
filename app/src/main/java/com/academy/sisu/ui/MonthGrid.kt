package com.academy.sisu.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academy.sisu.AcademyViewModel
import com.academy.sisu.data.Computed
import com.academy.sisu.data.Holidays
import com.academy.sisu.ui.theme.BlueC
import com.academy.sisu.ui.theme.DimCol
import com.academy.sisu.ui.theme.MagC
import com.academy.sisu.ui.theme.MagText
import com.academy.sisu.ui.theme.RedC
import com.academy.sisu.ui.theme.RedText
import com.academy.sisu.ui.theme.SatC
import com.academy.sisu.ui.theme.SkipText
import com.academy.sisu.ui.theme.SunC
import com.academy.sisu.ui.theme.TextCol
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs

private data class DotModel(val color: Color, val end: Boolean)

private data class CellModel(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val vacation: Boolean,
    val vacLeft: Boolean,
    val vacRight: Boolean,
    val holName: String?,
    val holActive: Boolean,
    val classColor: Color?,
    val endDay: Boolean,
    val skip: Boolean,
    val dots: List<DotModel>
)

@Composable
fun MonthGrid(
    vm: AcademyViewModel,
    month: YearMonth,
    today: LocalDate,
    caches: Map<String, Computed>,
    onDayTap: (LocalDate) -> Unit,
    onToast: (String) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val firstCell = remember(month) {
        val first = month.atDay(1)
        first.minusDays((first.dayOfWeek.value % 7).toLong())
    }

    // 길게 눌러 칠하기가 진행 중인지 (렌더링용 상태 아님 → 좌우 스와이프 제스처와의 충돌 방지용)
    val paintActive = remember { BooleanArray(1) }
    // 길게 누름 직후의 탭 무시 플래그
    val suppressTap = remember { BooleanArray(1) }
    // 가로 스와이프(달 넘김)가 감지되면 켜짐 → 방학 칠하기보다 우선
    val horizontalLock = remember { BooleanArray(1) }

    val selId = vm.selected
    val isAll = selId == "all"

    fun buildCell(date: LocalDate, ctx: YearMonth): CellModel {
        val inMonth = date.monthValue == ctx.monthValue && date.year == ctx.year
        val holName = vm.holidayName(date)
        val holActive = holName != null && vm.isHolidayActive(date)
        val vac = vm.isVacation(date)
        val col = date.dayOfWeek.value % 7
        val vacLeft = vac && col > 0 && vm.isVacation(date.minusDays(1))
        val vacRight = vac && col < 6 && vm.isVacation(date.plusDays(1))
        var classColor: Color? = null
        var endDay = false
        var skip = false
        val dots = ArrayList<DotModel>()
        if (vac) {
            // 방학(드래그 중 라이브 포함): 수업/점 표시를 덮어써서 즉시 방학으로 보이게
        } else if (!isAll) {
            val comp = caches[selId]
            val info = comp?.classMap?.get(date)
            if (info != null) {
                classColor = BlueC               // 사이클 색 교대 없이 항상 파랑
                endDay = info.end
            } else if (comp?.skipSet?.contains(date) == true) {
                skip = true
            }
        } else {
            for (s in vm.students) {
                val info = caches[s.id]?.classMap?.get(date)
                if (info != null) dots.add(DotModel(Color(s.color), info.end))
            }
        }
        return CellModel(date, inMonth, date == today, vac, vacLeft, vacRight, holName, holActive, classColor, endDay, skip, dots)
    }

    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val weeksTarget = remember(month) { weeksOf(month) }

        fun cellAt(pos: Offset): LocalDate {
            val c = (pos.x / (wPx / 7f)).toInt().coerceIn(0, 6)
            val r = (pos.y / (hPx / weeksTarget)).toInt().coerceIn(0, weeksTarget - 1)
            return firstCell.plusDays((r * 7 + c).toLong())
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(month, selId, wPx, hPx) {
                    detectTapGestures(
                        onPress = { suppressTap[0] = false; horizontalLock[0] = false },
                        onTap = { off -> if (!suppressTap[0]) onDayTap(cellAt(off)) }
                    )
                }
                .pointerInput(month, selId, wPx, hPx) {
                    var add = true
                    var count = 0
                    var aborted = false
                    val painted = HashSet<LocalDate>()
                    detectDragGesturesAfterLongPress(
                        onDragStart = { off ->
                            if (horizontalLock[0]) {
                                aborted = true            // 좌우 스와이프 중 → 방학 칠하기 안 함
                            } else {
                                aborted = false
                                suppressTap[0] = true
                                paintActive[0] = true
                                painted.clear()
                                val d = cellAt(off)
                                add = vm.startPaint(d)
                                painted.add(d)
                                count = 1
                                triggerVibrate(context)
                            }
                        },
                        onDrag = { change, _ ->
                            if (!aborted) {
                                val d = cellAt(change.position)
                                if (painted.add(d)) {
                                    vm.paint(d)
                                    count = painted.size
                                }
                            }
                        },
                        onDragEnd = {
                            if (!aborted) {
                                vm.commitPaint()
                                paintActive[0] = false
                                onToast("${count}일 ${if (add) "방학 지정" else "방학 해제"}")
                            }
                        },
                        onDragCancel = {
                            if (!aborted) {
                                vm.commitPaint()
                                paintActive[0] = false
                                onToast("${count}일 ${if (add) "방학 지정" else "방학 해제"}")
                            }
                        }
                    )
                }
                .pointerInput(wPx) {
                    var dx = 0f
                    var allowed = false
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f; allowed = !paintActive[0] },
                        onHorizontalDrag = { _, d ->
                            dx += d
                            horizontalLock[0] = true
                        },
                        onDragEnd = {
                            if (allowed && abs(dx) > wPx * 0.16f) {
                                if (dx > 0f) onPrevMonth() else onNextMonth()
                            }
                        },
                        onDragCancel = { }
                    )
                }
        ) {
            AnimatedContent(
                targetState = month,
                transitionSpec = {
                    val forward = targetState.isAfter(initialState)
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(260)) { full -> dir * full } + fadeIn(tween(200)))
                        .togetherWith(slideOutHorizontally(tween(260)) { full -> -dir * full } + fadeOut(tween(200)))
                },
                label = "month"
            ) { m ->
                val fc = remember(m) {
                    val f = m.atDay(1)
                    f.minusDays((f.dayOfWeek.value % 7).toLong())
                }
                val weeks = remember(m) { weeksOf(m) }
                Column(Modifier.fillMaxSize()) {
                    for (r in 0 until weeks) {
                        Row(Modifier.fillMaxWidth().weight(1f)) {
                            for (c in 0 until 7) {
                                val date = fc.plusDays((r * 7 + c).toLong())
                                DayCell(
                                    model = buildCell(date, m),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(model: CellModel, modifier: Modifier) {
    Box(
        modifier.then(if (model.inMonth) Modifier else Modifier.alpha(0.4f)),
        contentAlignment = Alignment.Center
    ) {

        // 방학 배경: 연속된 방학은 좌우로 이어붙임(가운데는 각지게, 양 끝만 둥글게)
        if (model.vacation) {
            val r = 11.dp
            val shape = RoundedCornerShape(
                topStart = if (model.vacLeft) 0.dp else r,
                topEnd = if (model.vacRight) 0.dp else r,
                bottomEnd = if (model.vacRight) 0.dp else r,
                bottomStart = if (model.vacLeft) 0.dp else r
            )
            Box(
                Modifier
                    .matchParentSize()
                    .padding(
                        start = if (model.vacLeft) 0.dp else 4.dp,
                        end = if (model.vacRight) 0.dp else 4.dp,
                        top = 7.dp,
                        bottom = 7.dp
                    )
                    .clip(shape)
                    .background(RedC.copy(alpha = 0.17f))
            )
        }

        // 숫자(+원판): 항상 정중앙 고정
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            val discColor = when {
                model.classColor != null -> model.classColor
                model.skip -> RedC.copy(alpha = 0.20f)
                else -> Color.Transparent
            }
            val discBorder = when {
                model.endDay -> Modifier.border(2.4.dp, MagC, CircleShape)
                model.isToday -> Modifier.border(1.6.dp, TextCol.copy(alpha = 0.55f), CircleShape)
                model.skip -> Modifier.border(1.4.dp, RedC.copy(alpha = 0.55f), CircleShape)
                else -> Modifier
            }
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(discColor)
                    .then(discBorder)
            )
            val numColor = when {
                model.classColor != null || model.endDay -> Color.White
                model.skip -> SkipText
                model.holName != null && model.holActive -> RedText
                !model.inMonth -> DimCol
                model.date.dayOfWeek == DayOfWeek.SUNDAY -> SunC
                model.date.dayOfWeek == DayOfWeek.SATURDAY -> SatC
                else -> TextCol
            }
            val numWeight = when {
                model.isToday -> FontWeight.Black
                model.classColor != null || model.endDay -> FontWeight.Bold
                else -> FontWeight.Medium
            }
            Text(
                text = model.date.dayOfMonth.toString(),
                color = numColor,
                fontSize = 14.5.sp,
                fontWeight = numWeight
            )
        }

        // 전체 보기 점: 숫자 아래 고정 위치
        if (model.dots.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.Center).offset(y = 23.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                model.dots.take(5).forEach { dot ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dot.color)
                            .then(if (dot.end) Modifier.border(1.6.dp, MagC, CircleShape) else Modifier)
                    )
                }
            }
        }

        // 공휴일 이름: 숫자 아래 고정 위치 (숫자 위치는 흔들리지 않음)
        if (model.holName != null) {
            Text(
                text = Holidays.shortName(model.holName),
                color = if (model.holActive) RedText else DimCol,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 25.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 1.dp)
            )
        }
    }
}

/** 그 달을 표시하는 데 필요한 주(행) 수. 1일의 요일 위치 + 일수를 7로 올림. (4~6주) */
private fun weeksOf(ym: YearMonth): Int {
    val startCol = ym.atDay(1).dayOfWeek.value % 7   // 0=일 .. 6=토
    return (startCol + ym.lengthOfMonth() + 6) / 7
}

private fun triggerVibrate(context: Context) {
    try {
        val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (_: Exception) {
    }
}
