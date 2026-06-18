package com.academy.sisu.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academy.sisu.AcademyViewModel
import com.academy.sisu.R
import com.academy.sisu.data.SessionEngine
import com.academy.sisu.data.Student
import com.academy.sisu.data.Summary
import com.academy.sisu.ui.theme.Accent
import com.academy.sisu.ui.theme.Accent2
import com.academy.sisu.ui.theme.Bg
import com.academy.sisu.ui.theme.Bg2
import com.academy.sisu.ui.theme.LineCol
import com.academy.sisu.ui.theme.MagC
import com.academy.sisu.ui.theme.MagText
import com.academy.sisu.ui.theme.MutedCol
import com.academy.sisu.ui.theme.RedText
import com.academy.sisu.ui.theme.SatC
import com.academy.sisu.ui.theme.Surface1
import com.academy.sisu.ui.theme.Surface2
import com.academy.sisu.ui.theme.Surface3
import com.academy.sisu.ui.theme.SunC
import com.academy.sisu.ui.theme.TextCol
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

private enum class SheetKind { Student, Menu, Holiday, DayDetail }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(vm: AcademyViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now() }

    var viewedMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var sheet by remember { mutableStateOf<SheetKind?>(null) }
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    var dayDetailDate by remember { mutableStateOf<LocalDate?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    var pendingImportText by remember { mutableStateOf<String?>(null) }
    var showClearVac by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            sheet = null; editingStudent = null; dayDetailDate = null
        }
    }
    fun openAdd() { editingStudent = null; sheet = SheetKind.Student }
    fun openEdit(s: Student) { editingStudent = s; sheet = SheetKind.Student }
    fun openDay(d: LocalDate) { dayDetailDate = d; sheet = SheetKind.DayDetail }

    // 학생별 계산 캐시 (드래그 중에는 calcVersion 이 안 올라가 재계산되지 않음)
    val horizon = remember(viewedMonth, today) {
        val a = today.plusMonths(18)
        val b = viewedMonth.atEndOfMonth().plusMonths(2)
        if (a.isAfter(b)) a else b
    }
    val studentsSnapshot = vm.students.toList()
    val caches = remember(studentsSnapshot, vm.calcVersion, horizon) {
        studentsSnapshot.associate { s ->
            s.id to SessionEngine.compute(s, horizon, vm::isHolidayActive, vm::isVacation)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            toast = try {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(vm.exportString().toByteArray(Charsets.UTF_8))
                }
                "백업 파일을 저장했습니다"
            } catch (e: Exception) {
                "내보내기에 실패했습니다"
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                if (text != null) pendingImportText = text else toast = "파일을 읽지 못했습니다"
            } catch (e: Exception) {
                toast = "파일을 읽지 못했습니다"
            }
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1900)
            toast = null
        }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ===== 헤더 (로고 + 액션) =====
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "로고",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(30.dp)
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { openAdd() }) {
                    Icon(Icons.Filled.Add, contentDescription = "학생 추가", tint = TextCol)
                }
                IconButton(onClick = { sheet = SheetKind.Menu }) {
                    Icon(Icons.Filled.Menu, contentDescription = "메뉴", tint = TextCol)
                }
            }

            // ===== 학생 칩 =====
            StudentStrip(vm = vm, onSelect = { vm.select(it) }, onAdd = { openAdd() })

            // ===== 요약 (특정 학생 선택 시) =====
            val selStu = if (vm.selected != "all") studentsSnapshot.firstOrNull { it.id == vm.selected } else null
            if (selStu != null) {
                val comp = caches[selStu.id]
                if (comp != null) {
                    SummaryCard(
                        student = selStu,
                        summary = SessionEngine.summary(selStu, comp, today),
                        onEdit = { openEdit(selStu) }
                    )
                }
            }

            // ===== 월 이동 =====
            MonthNav(
                month = viewedMonth,
                onPrev = { viewedMonth = viewedMonth.minusMonths(1) },
                onNext = { viewedMonth = viewedMonth.plusMonths(1) },
                onToday = { viewedMonth = YearMonth.from(today) }
            )

            // ===== 요일 헤더 =====
            WeekdayHeader()

            // ===== 달력 / 빈 화면 =====
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (studentsSnapshot.isEmpty()) {
                    EmptyState(onAdd = { openAdd() })
                } else {
                    MonthGrid(
                        vm = vm,
                        month = viewedMonth,
                        today = today,
                        caches = caches,
                        onDayTap = { openDay(it) },
                        onToast = { toast = it }
                    )
                }
            }
        }

        // ===== 토스트 =====
        toast?.let { msg ->
            Box(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 26.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Surface3)
                        .border(1.dp, LineCol, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text(msg, color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ===== 바텀시트 =====
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null; editingStudent = null; dayDetailDate = null },
            sheetState = sheetState,
            containerColor = Surface1,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (sheet) {
                SheetKind.Student -> StudentSheetContent(
                    vm = vm,
                    editing = editingStudent,
                    onClose = { closeSheet() },
                    onToast = { toast = it }
                )
                SheetKind.Menu -> MenuSheetContent(
                    vm = vm,
                    onOpenHolidays = { sheet = SheetKind.Holiday },
                    onClearVac = { showClearVac = true },
                    onExport = {
                        closeSheet()
                        exportLauncher.launch("시수캘린더_백업_${today}.json")
                    },
                    onImport = {
                        closeSheet()
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    }
                )
                SheetKind.Holiday -> HolidaySheetContent(vm = vm, onClose = { closeSheet() })
                SheetKind.DayDetail -> {
                    val d = dayDetailDate
                    if (d != null) {
                        DayDetailSheetContent(vm = vm, caches = caches, date = d, onClose = { closeSheet() })
                    }
                }
                null -> {}
            }
        }
    }

    // ===== 방학 전체 해제 확인 =====
    if (showClearVac) {
        AlertDialog(
            onDismissRequest = { showClearVac = false },
            confirmButton = {
                TextButton(onClick = {
                    val n = vm.vacations.size
                    vm.clearVacations()
                    showClearVac = false
                    closeSheet()
                    toast = if (n > 0) "방학이 모두 해제되었습니다" else "지정된 방학이 없습니다"
                }) { Text("전체 해제", color = RedText) }
            },
            dismissButton = {
                TextButton(onClick = { showClearVac = false }) { Text("취소") }
            },
            title = { Text("방학 전체 해제") },
            text = { Text("지정된 방학 ${vm.vacations.size}일을 모두 해제할까요?") }
        )
    }

    // ===== 데이터 불러오기 확인 =====
    if (pendingImportText != null) {
        AlertDialog(
            onDismissRequest = { pendingImportText = null },
            confirmButton = {
                TextButton(onClick = {
                    val ok = vm.importFrom(pendingImportText ?: "")
                    pendingImportText = null
                    toast = if (ok) "데이터를 불러왔습니다" else "올바른 백업 파일이 아닙니다"
                }) { Text("덮어쓰기", color = RedText) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportText = null }) { Text("취소") }
            },
            title = { Text("데이터 불러오기") },
            text = { Text("백업으로 현재 데이터를 덮어쓸까요? 되돌릴 수 없습니다.") }
        )
    }
}

/* ============================== 학생 칩 ============================== */

@Composable
private fun StudentStrip(vm: AcademyViewModel, onSelect: (String) -> Unit, onAdd: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StripChip(text = "전체", active = vm.selected == "all", swatch = null) { onSelect("all") }
        vm.students.forEach { s ->
            StripChip(text = s.name, active = vm.selected == s.id, swatch = Color(s.color)) { onSelect(s.id) }
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, Accent2, RoundedCornerShape(999.dp))
                .clickable { onAdd() }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+ 학생", color = Accent2, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StripChip(text: String, active: Boolean, swatch: Color?, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (active) Surface3 else Surface1)
            .border(1.dp, if (active) Accent else LineCol, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (swatch != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(swatch))
        }
        Text(
            text = text,
            color = if (active) TextCol else MutedCol,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/* ============================== 요약 카드 ============================== */

@Composable
private fun SummaryCard(student: Student, summary: Summary, onEdit: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, LineCol, RoundedCornerShape(16.dp))
            .padding(15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(11.dp).clip(CircleShape).background(Color(student.color)))
            Spacer(Modifier.width(10.dp))
            Text(student.name, color = TextCol, fontSize = 16.sp, fontWeight = FontWeight.Black)
            if (summary.startFuture) {
                Spacer(Modifier.width(8.dp))
                Text("시작 예정", color = MutedCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(Surface3)
                    .border(1.dp, LineCol, RoundedCornerShape(9.dp))
                    .clickable { onEdit() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("수정", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Pill("등원", weekdaysLabel(student.weekdays))
            Pill("시수", "${student.sessions}회")
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Pill("시작", fmtKDate(student.start))
            Pill("진행", "${summary.doneInCycle}/${summary.n}회")
        }

        val endTxt = summary.endDate?.let { fmtKDate(it) } ?: "—"
        val dd = summary.dday
        val ddTxt = when {
            dd == null -> ""
            dd == 0L -> "D-DAY"
            dd > 0 -> "D-$dd"
            else -> "종료 ${-dd}일 경과"
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MagC.copy(alpha = 0.10f))
                .border(1.dp, MagC.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${summary.cycleNo + 1}차 수업 종료(정산)일",
                    color = MagText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    endTxt,
                    color = Color(0xFFF5D0FE), fontSize = 14.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (ddTxt.isNotEmpty()) {
                Text(ddTxt, color = Color(0xFFF0ABFC), fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun Pill(label: String, value: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Bg2)
            .border(1.dp, LineCol, RoundedCornerShape(9.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = MutedCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

/* ============================== 월 이동 / 요일 / 빈 화면 ============================== */

@Composable
private fun MonthNav(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "이전 달", tint = TextCol)
        }
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToday() }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${month.year}년 ${month.monthValue}월",
                color = TextCol, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "다음 달", tint = TextCol)
        }
    }
}

@Composable
private fun WeekdayHeader() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        WD_KO.forEachIndexed { i, d ->
            Text(
                text = d,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = when (i) {
                    0 -> SunC
                    6 -> SatC
                    else -> MutedCol
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("아직 등록된 학생이 없어요", color = TextCol, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "학생을 등록하면 등원 요일과 시수에 맞춰 등원일과 정산일이 자동으로 표시됩니다.",
            color = MutedCol, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Accent)
                .clickable { onAdd() }
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Text("+ 첫 학생 등록하기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
