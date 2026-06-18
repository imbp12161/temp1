package com.academy.sisu.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academy.sisu.AcademyViewModel
import com.academy.sisu.data.Computed
import com.academy.sisu.data.Holidays
import com.academy.sisu.ui.theme.Accent2
import com.academy.sisu.ui.theme.BlueC
import com.academy.sisu.ui.theme.DimCol
import com.academy.sisu.ui.theme.GreenC
import com.academy.sisu.ui.theme.MagC
import com.academy.sisu.ui.theme.MagText
import com.academy.sisu.ui.theme.PaintBottom
import com.academy.sisu.ui.theme.PaintTop
import com.academy.sisu.ui.theme.RedC
import com.academy.sisu.ui.theme.RedText
import com.academy.sisu.ui.theme.SatC
import com.academy.sisu.ui.theme.SkipText
import com.academy.sisu.ui.theme.SunC
import com.academy.sisu.ui.theme.TextCol
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private data class DotModel(val color: Color, val end: Boolean)

private data class CellModel(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val vacation: Boolean,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val firstCell = remember(month) {
        val first = month.atDay(1)
        first.minusDays((first.dayOfWeek.value % 7).toLong())
    }

    var painting by remember { mutableStateOf(false) }
    var paintAdd by remember { mutableStateOf(true) }
    var paintCount by remember { mutableIntStateOf(0) }
    val painted = remember { mutableSetOf<LocalDate>() }
    // 길게 눌러 칠하기가 시작된 직후의 '탭' 이벤트를 무시하기 위한 플래그(상태 아님 → 불필요한 리컴포지션 방지)
    val suppressTap = remember { BooleanArray(1) }

    val selId = vm.selected
    val isAll = selId == "all"

    fun buildCell(date: LocalDate): CellModel {
        val inMonth = date.monthValue == month.monthValue && date.year == month.year
        val holName = vm.holidayName(date)
        val holActive = holName != null && vm.isHolidayActive(date)
        val vac = vm.isVacation(date)
        var classColor: Color? = null
        var endDay = false
        var skip = false
        val dots = ArrayList<DotModel>()
        if (!isAll) {
            val comp = caches[selId]
            val info = comp?.classMap?.get(date)
            if (info != null) {
                classColor = if (info.cycle % 2 == 0) BlueC else GreenC
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
        return CellModel(date, inMonth, date == today, vac, holName, holActive, classColor, endDay, skip, dots)
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val cellH = maxHeight / 6

        fun cellAt(pos: Offset): LocalDate {
            val col = (pos.x / (wPx / 7f)).toInt().coerceIn(0, 6)
            val row = (pos.y / (hPx / 6f)).toInt().coerceIn(0, 5)
            return firstCell.plusDays((row * 7 + col).toLong())
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(month, selId, wPx, hPx) {
                    detectTapGestures(
                        onPress = { suppressTap[0] = false },
                        onTap = { off -> if (!suppressTap[0]) onDayTap(cellAt(off)) }
                    )
                }
                .pointerInput(month, selId, wPx, hPx) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { off ->
                            suppressTap[0] = true
                            val d = cellAt(off)
                            paintAdd = vm.startPaint(d)
                            painted.clear()
                            painted.add(d)
                            paintCount = 1
                            painting = true
                            triggerVibrate(context)
                        },
                        onDrag = { change, _ ->
                            val d = cellAt(change.position)
                            if (painted.add(d)) {
                                vm.paint(d)
                                paintCount = painted.size
                            }
                        },
                        onDragEnd = {
                            vm.commitPaint()
                            painting = false
                            onToast("${paintCount}일 ${if (paintAdd) "방학 지정" else "방학 해제"}")
                        },
                        onDragCancel = {
                            vm.commitPaint()
                            painting = false
                            onToast("${paintCount}일 ${if (paintAdd) "방학 지정" else "방학 해제"}")
                        }
                    )
                }
        ) {
            Column(Modifier.fillMaxSize()) {
                for (r in 0 until 6) {
                    Row(Modifier.fillMaxWidth().height(cellH)) {
                        for (c in 0 until 7) {
                            val date = firstCell.plusDays((r * 7 + c).toLong())
                            DayCell(
                                model = buildCell(date),
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }

        if (painting) {
            PaintBanner(
                add = paintAdd,
                count = paintCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(14.dp)
            )
        }
    }
}

@Composable
private fun DayCell(model: CellModel, modifier: Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {

        if (model.vacation) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 3.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(RedC.copy(alpha = 0.16f))
            )
        }

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
                .size(38.dp)
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

        if (model.date.dayOfMonth == 1) {
            Text(
                text = "${model.date.monthValue}월",
                color = Accent2,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp)
            )
        }

        if (model.dots.isNotEmpty()) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                model.dots.take(4).forEach { dot ->
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(dot.color)
                            .then(if (dot.end) Modifier.border(1.6.dp, MagC, CircleShape) else Modifier)
                    )
                }
            }
            if (model.dots.size > 4) {
                Text(
                    text = "+${model.dots.size - 4}",
                    color = MagText,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 5.dp, bottom = 3.dp)
                )
            }
        }

        if (model.holName != null) {
            Text(
                text = Holidays.shortName(model.holName),
                color = if (model.holActive) RedText else DimCol,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun PaintBanner(add: Boolean, count: Int, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(PaintTop, PaintBottom)))
            .border(1.dp, MagC.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = if (add) "방학 지정 중" else "방학 해제 중",
                color = Color(0xFFFBE8FF),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "손가락을 끌어 범위를 칠하세요",
                color = Color(0xFFE9B8F5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = "${count}일",
            color = Color(0xFFF5D0FE),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black
        )
    }
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
