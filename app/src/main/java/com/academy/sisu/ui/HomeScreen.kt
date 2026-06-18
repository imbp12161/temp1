package com.academy.sisu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.academy.sisu.AcademyViewModel
import com.academy.sisu.data.SessionEngine
import com.academy.sisu.data.Student
import com.academy.sisu.data.Summary
import com.academy.sisu.ui.theme.Accent
import com.academy.sisu.ui.theme.Accent2
import com.academy.sisu.ui.theme.Bg
import com.academy.sisu.ui.theme.LineCol
import com.academy.sisu.ui.theme.MagText
import com.academy.sisu.ui.theme.MutedCol
import com.academy.sisu.ui.theme.Surface1
import com.academy.sisu.ui.theme.Surface2
import com.academy.sisu.ui.theme.Surface3
import com.academy.sisu.ui.theme.TextCol
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class HomeSheet { Add, Edit, Groups }

private data class HomeSection(val label: String, val items: List<Student>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: AcademyViewModel, onOpenStudent: (String) -> Unit) {
    val today = remember { LocalDate.now() }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var editing by remember { mutableStateOf<Student?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    fun closeSheet() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { sheet = null; editing = null }
    }

    val horizon = remember(today) { today.plusMonths(18) }
    val studentsSnapshot = vm.students.toList()
    val caches = remember(studentsSnapshot, vm.calcVersion, horizon) {
        studentsSnapshot.associate { s ->
            s.id to SessionEngine.compute(s, horizon, vm::isHolidayActive, vm::isVacation)
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1900)
            toast = null
        }
    }

    // 그룹별 섹션 구성
    val groupsOrder = vm.groups.toList()
    val byGroup = LinkedHashMap<String, MutableList<Student>>()
    groupsOrder.forEach { byGroup[it] = mutableListOf() }
    val ungrouped = ArrayList<Student>()
    studentsSnapshot.forEach { s ->
        val g = s.group
        if (g.isNotBlank() && byGroup.containsKey(g)) byGroup[g]!!.add(s) else ungrouped.add(s)
    }
    val sections = ArrayList<HomeSection>()
    groupsOrder.forEach { g ->
        val list = byGroup[g]!!
        if (list.isNotEmpty()) sections.add(HomeSection(g, list))
    }
    if (ungrouped.isNotEmpty()) sections.add(HomeSection("미분류", ungrouped))

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // 헤더
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 6.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("학생", color = TextCol, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { sheet = HomeSheet.Groups }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text("그룹 관리", color = Accent2, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = { editing = null; sheet = HomeSheet.Add }) {
                    Icon(Icons.Filled.Add, contentDescription = "학생 추가", tint = TextCol)
                }
            }

            if (studentsSnapshot.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(30.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("아직 등록된 학생이 없어요", color = TextCol, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "오른쪽 위 + 버튼으로 학생을 등록하세요.",
                        color = MutedCol, fontSize = 13.sp, textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Accent)
                            .clickable { editing = null; sheet = HomeSheet.Add }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text("+ 첫 학생 등록하기", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 24.dp)
                ) {
                    sections.forEach { section ->
                        item(key = "header_${section.label}") {
                            Row(
                                Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(section.label, color = TextCol, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("${section.items.size}명", color = MutedCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        items(section.items, key = { it.id }) { s ->
                            val comp = caches[s.id]
                            val summary = if (comp != null) SessionEngine.summary(s, comp, today) else null
                            StudentRow(
                                student = s,
                                summary = summary,
                                onClick = { onOpenStudent(s.id) },
                                onEdit = { editing = s; sheet = HomeSheet.Edit }
                            )
                        }
                    }
                }
            }
        }

        toast?.let { msg ->
            Box(
                Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 26.dp),
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

    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null; editing = null },
            sheetState = sheetState,
            containerColor = Surface1,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            when (sheet) {
                HomeSheet.Add -> StudentSheetContent(vm = vm, editing = null, onClose = { closeSheet() }, onToast = { toast = it })
                HomeSheet.Edit -> StudentSheetContent(vm = vm, editing = editing, onClose = { closeSheet() }, onToast = { toast = it })
                HomeSheet.Groups -> GroupManageSheetContent(vm = vm, onClose = { closeSheet() })
                null -> {}
            }
        }
    }
}

@Composable
private fun StudentRow(
    student: Student,
    summary: Summary?,
    onClick: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface2)
            .border(1.dp, LineCol, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(student.color)))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(student.name, color = TextCol, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(8.dp))
                Text(
                    weekdaysLabel(student.weekdays) + " · " + "${student.sessions}회",
                    color = MutedCol, fontSize = 12.sp, fontWeight = FontWeight.Medium
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("시작", color = MutedCol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text(fmtKDate(student.start), color = TextCol, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("정산", color = MagText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text(
                    summary?.endDate?.let { fmtKDate(it) } ?: "—",
                    color = Color(0xFFF5D0FE), fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
                val dd = summary?.dday
                if (dd != null) {
                    Spacer(Modifier.width(8.dp))
                    val ddTxt = when {
                        dd == 0L -> "D-DAY"
                        dd > 0 -> "D-$dd"
                        else -> "지남"
                    }
                    Text(ddTxt, color = Color(0xFFF0ABFC), fontSize = 12.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(9.dp))
                .background(Surface3)
                .border(1.dp, LineCol, RoundedCornerShape(9.dp))
                .clickable { onEdit() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("수정", color = TextCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
