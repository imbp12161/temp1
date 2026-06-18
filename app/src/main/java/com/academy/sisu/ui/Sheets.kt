package com.academy.sisu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academy.sisu.AcademyViewModel
import com.academy.sisu.data.Computed
import com.academy.sisu.data.Holidays
import com.academy.sisu.data.PALETTE
import com.academy.sisu.data.Student
import com.academy.sisu.ui.theme.Accent
import com.academy.sisu.ui.theme.Bg2
import com.academy.sisu.ui.theme.DimCol
import com.academy.sisu.ui.theme.LineCol
import com.academy.sisu.ui.theme.LineSoft
import com.academy.sisu.ui.theme.MagText
import com.academy.sisu.ui.theme.MutedCol
import com.academy.sisu.ui.theme.RedC
import com.academy.sisu.ui.theme.RedSkin
import com.academy.sisu.ui.theme.RedText
import com.academy.sisu.ui.theme.Surface2
import com.academy.sisu.ui.theme.Surface3
import com.academy.sisu.ui.theme.TextCol
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/* ============================== 공통 소품 ============================== */

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        color = MutedCol,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 2.dp, bottom = 8.dp)
    )
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Accent else Bg2)
            .border(1.dp, if (selected) Accent else LineCol, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else MutedCol,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun PrimaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White)
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

@Composable
private fun GhostButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Surface2, contentColor = TextCol)
    ) { Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}

/* ============================== 학생 등록/수정 ============================== */

private fun LocalDate.toUtcMillis(): Long =
    this.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSheetContent(
    vm: AcademyViewModel,
    editing: Student?,
    onClose: () -> Unit,
    onToast: (String) -> Unit
) {
    var name by remember(editing) { mutableStateOf(editing?.name ?: "") }
    var weekdays by remember(editing) { mutableStateOf(editing?.weekdays?.toSet() ?: emptySet<Int>()) }
    var sessions by remember(editing) { mutableStateOf(editing?.sessions ?: 12) }
    var start by remember(editing) { mutableStateOf(editing?.start ?: LocalDate.now()) }
    var color by remember(editing) { mutableStateOf(editing?.color ?: vm.nextColor()) }
    var group by remember(editing) { mutableStateOf(editing?.group ?: "") }
    var newGroupMode by remember(editing) { mutableStateOf(false) }
    var newGroupText by remember(editing) { mutableStateOf("") }
    var showDate by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(bottom = 18.dp)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Text(
            text = if (editing == null) "학생 등록" else "학생 정보 수정",
            color = TextCol, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
        )
        Text(
            text = "이름, 등원 요일(월~금), 수업 시수를 정합니다.",
            color = MutedCol, fontSize = 12.5.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 이름
        FieldLabel("학생 이름")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            placeholder = { Text("예: 김민준", color = DimCol) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextCol,
                unfocusedTextColor = TextCol,
                focusedContainerColor = Bg2,
                unfocusedContainerColor = Bg2,
                cursorColor = Accent,
                focusedBorderColor = Accent,
                unfocusedBorderColor = LineCol
            )
        )
        Spacer(Modifier.height(16.dp))

        // 등원 요일
        FieldLabel("등원 요일")
        val wdLabels = listOf(1 to "월", 2 to "화", 3 to "수", 4 to "목", 5 to "금")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            wdLabels.forEach { (v, lbl) ->
                ToggleChip(
                    label = lbl,
                    selected = weekdays.contains(v),
                    modifier = Modifier.weight(1f)
                ) {
                    weekdays = if (weekdays.contains(v)) weekdays - v else weekdays + v
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // 수업 시수
        FieldLabel("수업 시수")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(8, 12, 16, 20).forEach { v ->
                ToggleChip(
                    label = "${v}회",
                    selected = sessions == v,
                    modifier = Modifier.weight(1f)
                ) { sessions = v }
            }
        }
        Spacer(Modifier.height(16.dp))

        // 시작일
        FieldLabel("시작일 (등록일)")
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Bg2)
                .border(1.dp, LineCol, RoundedCornerShape(12.dp))
                .clickable { showDate = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(fmtKDate(start), color = TextCol, fontSize = 15.sp)
        }
        Spacer(Modifier.height(16.dp))

        // 색상
        FieldLabel("표시 색상")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PALETTE.forEach { c ->
                val sel = c == color
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (sel) 2.dp else 0.dp,
                            color = if (sel) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { color = c }
                )
            }
            val isCustom = color !in PALETTE
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759),
                                Color(0xFF00C7BE), Color(0xFF007AFF), Color(0xFFAF52DE),
                                Color(0xFFFF2D55), Color(0xFFFF3B30)
                            )
                        )
                    )
                    .border(
                        width = if (isCustom) 3.dp else 0.dp,
                        color = if (isCustom) Color.White else Color.Transparent,
                        shape = CircleShape
                    )
                    .clickable { showColorPicker = true }
            )
        }
        Spacer(Modifier.height(20.dp))

        // 그룹(반)
        FieldLabel("그룹 (반)")
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroupChip("미분류", selected = group.isBlank() && !newGroupMode) {
                group = ""; newGroupMode = false
            }
            vm.groups.forEach { g ->
                GroupChip(g, selected = !newGroupMode && group == g) {
                    group = g; newGroupMode = false
                }
            }
            GroupChip("+ 새 그룹", selected = newGroupMode) {
                newGroupMode = true
                group = newGroupText
            }
        }
        if (newGroupMode) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newGroupText,
                onValueChange = { newGroupText = it; group = it },
                placeholder = { Text("새 그룹 이름 (예: 월수금 7시반)", color = DimCol) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextCol,
                    unfocusedTextColor = TextCol,
                    focusedContainerColor = Bg2,
                    unfocusedContainerColor = Bg2,
                    cursorColor = Accent,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = LineCol
                )
            )
        }
        Spacer(Modifier.height(20.dp))

        // 액션
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            if (editing != null) {
                Button(
                    onClick = { showDelete = true },
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RedC.copy(alpha = 0.14f),
                        contentColor = RedSkin
                    )
                ) { Text("삭제", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
            GhostButton("취소", modifier = Modifier.weight(1f)) { onClose() }
            PrimaryButton("저장", modifier = Modifier.weight(1f)) {
                if (name.isBlank()) {
                    onToast("학생 이름을 입력하세요"); return@PrimaryButton
                }
                if (weekdays.isEmpty()) {
                    onToast("등원 요일을 1개 이상 선택하세요"); return@PrimaryButton
                }
                val wd = weekdays.toList().sorted()
                if (editing == null) {
                    vm.addStudent(name.trim(), wd, sessions, start, color, group)
                    onToast("학생이 등록되었습니다")
                } else {
                    vm.updateStudent(editing.id, name.trim(), wd, sessions, start, color, group)
                    onToast("수정되었습니다")
                }
                onClose()
            }
        }
    }

    if (showDate) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = start.toUtcMillis())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { start = millisToLocalDate(it) }
                    showDate = false
                }) { Text("확인") }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) { Text("취소") }
            }
        ) {
            DatePicker(state = dpState)
        }
    }

    if (showDelete && editing != null) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteStudent(editing.id)
                    showDelete = false
                    onToast("삭제되었습니다")
                    onClose()
                }) { Text("삭제", color = RedText) }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("취소") }
            },
            title = { Text("학생 삭제") },
            text = { Text("이 학생을 삭제할까요? 되돌릴 수 없습니다.") }
        )
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initial = color,
            onPick = { color = it; showColorPicker = false },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initial: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val hsv0 = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toInt(), it) }
    }
    var hue by remember { mutableStateOf(hsv0[0]) }
    var sat by remember { mutableStateOf(hsv0[1]) }
    var value by remember { mutableStateOf(hsv0[2].coerceAtLeast(0.06f)) }

    val current = Color.hsv(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface2,
        confirmButton = {
            TextButton(onClick = { onPick(current.toArgb().toLong() and 0xFFFFFFFFL) }) {
                Text("선택", color = TextCol, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = MutedCol) }
        },
        title = { Text("색상 선택", color = TextCol, fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(current)
                        .border(1.dp, LineCol, RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(16.dp))
                Text("색조(Hue)", color = MutedCol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Slider(value = hue, onValueChange = { hue = it }, valueRange = 0f..360f)
                Text("채도(Saturation)", color = MutedCol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Slider(value = sat, onValueChange = { sat = it }, valueRange = 0f..1f)
                Text("밝기(Value)", color = MutedCol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                Slider(value = value, onValueChange = { value = it }, valueRange = 0f..1f)
            }
        }
    )
}

/* ============================== 메뉴 ============================== */

@Composable
private fun MenuRow(title: String, sub: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 15.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = TextCol, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = MutedCol, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", color = DimCol, fontSize = 18.sp)
    }
}

@Composable
fun MenuSheetContent(
    vm: AcademyViewModel,
    onOpenHolidays: () -> Unit,
    onClearVac: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp)
            .navigationBarsPadding()
    ) {
        Text("메뉴", color = TextCol, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        Text("데이터는 이 기기에 저장됩니다. 정기적으로 백업해 주세요.",
            color = MutedCol, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 8.dp))

        MenuRow("공휴일 관리", "자동 제외할 공휴일을 켜고 끕니다 (2026·2027)", onOpenHolidays)
        MenuRow("방학 전체 해제", "지정된 방학 ${vm.vacations.size}일", onClearVac)
        MenuRow("데이터 내보내기 (백업)", "학생·방학·공휴일 설정을 파일로 저장", onExport)
        MenuRow("데이터 불러오기", "백업 파일에서 복원", onImport)
    }
}

/* ============================== 공휴일 관리 ============================== */

@Composable
fun HolidaySheetContent(
    vm: AcademyViewModel,
    onClose: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 14.dp)
            .navigationBarsPadding()
    ) {
        Text("공휴일 관리", color = TextCol, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        Text("켜진 공휴일은 등원일에서 자동 제외됩니다. 학원이 운영하는 날은 끄세요. (예: 근로자의 날)",
            color = MutedCol, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 12.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Holidays.MAP.entries.sortedBy { it.key }.forEach { (date, label) ->
                val on = !vm.holOff.contains(date)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${date.monthValue}/${date.dayOfMonth}(${weekdayKo(date)})",
                        color = MutedCol, fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(74.dp)
                    )
                    Text(
                        text = "$label · ${date.year}",
                        color = TextCol, fontSize = 14.sp,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                    Switch(
                        checked = on,
                        onCheckedChange = { vm.toggleHoliday(date) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Accent,
                            checkedBorderColor = Accent,
                            uncheckedThumbColor = Color(0xFFCBD1DE),
                            uncheckedTrackColor = Surface3,
                            uncheckedBorderColor = LineCol
                        )
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        GhostButton("닫기", modifier = Modifier.fillMaxWidth()) { onClose() }
    }
}

/* ============================== 날짜 상세 ============================== */

@Composable
fun DayDetailSheetContent(
    vm: AcademyViewModel,
    caches: Map<String, Computed>,
    date: LocalDate,
    onClose: () -> Unit
) {
    val holName = vm.holidayName(date)
    val isVac = vm.isVacation(date)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 18.dp)
            .navigationBarsPadding()
    ) {
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
            Text(
                "${date.monthValue}월 ${date.dayOfMonth}일",
                color = TextCol, fontSize = 22.sp, fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(8.dp))
            Text("${weekdayKo(date)}요일", color = MutedCol, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // 배지
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            if (holName != null) {
                val txt = "공휴일 · $holName" + if (vm.holOff.contains(date)) " (수업일로 처리됨)" else ""
                Badge(txt, RedText, RedC.copy(alpha = 0.16f))
            }
            if (isVac) Badge("방학", RedSkin, RedC.copy(alpha = 0.12f))
        }

        // 학생 목록
        var any = false
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            for (s in vm.students) {
                val c = caches[s.id] ?: continue
                val inf = c.classMap[date]
                val skip = c.skipSet.contains(date)
                if (inf != null || skip) {
                    any = true
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(11.dp).clip(CircleShape).background(Color(s.color)))
                        Spacer(Modifier.width(10.dp))
                        Text(s.name, color = TextCol, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (inf != null) {
                            val t = "${inf.cycle + 1}차 ${inf.idx}/${s.sessions}회" +
                                if (inf.end) " · 정산일" else ""
                            Text(t, color = if (inf.end) MagText else MutedCol, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Text("제외(밀림)", color = RedSkin, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (!any) {
                Text(
                    if (vm.students.isEmpty()) "등록된 학생이 없습니다." else "이 날 등원하는 학생이 없습니다.",
                    color = MutedCol, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            GhostButton("닫기", modifier = Modifier.weight(1f)) { onClose() }
            PrimaryButton(if (isVac) "방학 해제" else "방학으로 지정", modifier = Modifier.weight(1f)) {
                vm.toggleVacation(date)
                onClose()
            }
        }
    }
}

@Composable
private fun Badge(text: String, fg: Color, bg: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.35f), RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/* ============================== 그룹 칩 / 그룹 관리 ============================== */

@Composable
private fun GroupChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Surface3 else Surface2)
            .border(1.dp, if (selected) Accent else LineCol, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            color = if (selected) TextCol else MutedCol,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun GroupManageSheetContent(vm: AcademyViewModel, onClose: () -> Unit) {
    var newName by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("그룹 (반) 관리", color = TextCol, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(4.dp))
        Text(
            "학생을 반별로 묶어 관리할 수 있어요. 그룹을 삭제하면 소속 학생은 '미분류'로 이동합니다.",
            color = MutedCol, fontSize = 12.5.sp
        )
        Spacer(Modifier.height(16.dp))

        FieldLabel("새 그룹 추가")
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                placeholder = { Text("그룹 이름", color = DimCol) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextCol,
                    unfocusedTextColor = TextCol,
                    focusedContainerColor = Bg2,
                    unfocusedContainerColor = Bg2,
                    cursorColor = Accent,
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = LineCol
                )
            )
            PrimaryButton("추가") {
                if (newName.isNotBlank()) {
                    vm.addGroup(newName)
                    newName = ""
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel("그룹 목록")
        if (vm.groups.isEmpty()) {
            Text(
                "아직 그룹이 없습니다.",
                color = DimCol, fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            vm.groups.forEach { g ->
                val count = vm.students.count { it.group == g }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface2)
                        .border(1.dp, LineCol, RoundedCornerShape(12.dp))
                        .padding(start = 13.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(g, color = TextCol, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("학생 ${count}명", color = MutedCol, fontSize = 12.sp)
                    }
                    TextButton(onClick = { renameTarget = g; renameText = g }) {
                        Text("이름변경", color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { vm.deleteGroup(g) }) {
                        Text("삭제", color = RedSkin, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        PrimaryButton("닫기", modifier = Modifier.fillMaxWidth()) { onClose() }
    }

    val target = renameTarget
    if (target != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor = Surface2,
            confirmButton = {
                TextButton(onClick = {
                    vm.renameGroup(target, renameText)
                    renameTarget = null
                }) { Text("변경", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("취소", color = MutedCol) }
            },
            title = { Text("그룹 이름 변경", color = TextCol, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextCol,
                        unfocusedTextColor = TextCol,
                        focusedContainerColor = Bg2,
                        unfocusedContainerColor = Bg2,
                        cursorColor = Accent,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = LineCol
                    )
                )
            }
        )
    }
}
