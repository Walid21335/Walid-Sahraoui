package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.Student
import com.example.data.Teacher
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.data.Exam
import com.example.data.Question
import com.example.data.Submission
import com.example.data.Notification
import com.example.latex.LatexText
import com.example.qr.QrCodeImage
import com.example.ui.viewmodel.ExamPortalViewModel
import com.example.ui.viewmodel.PortalRole
import java.io.File

// App Navigation Destinations
enum class ScreenRoute {
    LOGIN,
    DASHBOARD,
    STUDENT_CONVOCATION,
    EXAM_WORK_SESSION,
    TEACHER_CREATOR,
    TEACHER_QUESTION_DRAFTER,
    TEACHER_GRADER,
    ROSTER_MANAGEMENT,
    NOTIFICATION_CENTER
}

@Composable
fun MainAppContent(
    viewModel: ExamPortalViewModel,
    userDarkTheme: MutableState<Boolean>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ScreenRoute.LOGIN) }
    
    // Core state observations
    val currentRole by viewModel.currentRole.collectAsState()
    val selectedStudent by viewModel.selectedStudent.collectAsState()
    val selectedTeacher by viewModel.selectedTeacher.collectAsState()
    
    val studentsList by viewModel.students.collectAsState()
    val teachersList by viewModel.teachers.collectAsState()
    val examsList by viewModel.exams.collectAsState()
    val submissionsList by viewModel.submissions.collectAsState()
    val notificationsList by viewModel.notifications.collectAsState()
    val unreadNotifsCount by viewModel.unreadNotificationsCount.collectAsState()
    
    val toastMessage by viewModel.toastMessage.collectAsState()
    val exportedFile by viewModel.exportedFile.collectAsState()

    // Active exam variables
    var activeExamForSession by remember { mutableStateOf<Exam?>(null) }
    val activeExamQuestions by viewModel.activeExamQuestions.collectAsState()
    val studentAnswers by viewModel.studentAnswers.collectAsState()
    
    // Grading states
    var activeSubmissionForGrading by remember { mutableStateOf<Submission?>(null) }

    // Display localized toasts or dialogs
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            // Display alert/toast mechanism
            // viewModel.clearToast()
        }
    }

    // Trigger Android System Share Sheet when PDF/Excel generation completes
    LaunchedEffect(exportedFile) {
        exportedFile?.let { file ->
            shareGeneratedFile(context, file)
            viewModel.clearExportedFile()
        }
    }

    Scaffold(
        topBar = {
            if (currentScreen != ScreenRoute.LOGIN) {
                AppTopBar(
                    title = when (currentScreen) {
                        ScreenRoute.DASHBOARD -> "بوابة سند التعليمية - لوحة التحكم"
                        ScreenRoute.STUDENT_CONVOCATION -> "قاعة الاستدعاء الإلكتروني"
                        ScreenRoute.EXAM_WORK_SESSION -> "جلسة الاختبار النهائي"
                        ScreenRoute.TEACHER_CREATOR -> "منشئ الاختبارات الموحدة"
                        ScreenRoute.TEACHER_QUESTION_DRAFTER -> "محرر ومصمم أسئلة LaTeX"
                        ScreenRoute.TEACHER_GRADER -> "مركز تقييم ورقات الإجابة"
                        ScreenRoute.ROSTER_MANAGEMENT -> "إيجاد وإدارة اللوائح الأكاديمية"
                        ScreenRoute.NOTIFICATION_CENTER -> "مركز التنبيهات والإشعارات"
                        else -> "سند التعليمي"
                    },
                    currentScreen = currentScreen,
                    unreadCount = unreadNotifsCount,
                    darkTheme = userDarkTheme.value,
                    onToggleDarkTheme = { userDarkTheme.value = !userDarkTheme.value },
                    onNavigateTo = { currentScreen = it },
                    onBack = {
                        currentScreen = when (currentScreen) {
                            ScreenRoute.STUDENT_CONVOCATION -> ScreenRoute.DASHBOARD
                            ScreenRoute.EXAM_WORK_SESSION -> ScreenRoute.STUDENT_CONVOCATION
                            ScreenRoute.TEACHER_CREATOR -> ScreenRoute.DASHBOARD
                            ScreenRoute.TEACHER_QUESTION_DRAFTER -> ScreenRoute.TEACHER_CREATOR
                            ScreenRoute.TEACHER_GRADER -> ScreenRoute.DASHBOARD
                            ScreenRoute.ROSTER_MANAGEMENT -> ScreenRoute.DASHBOARD
                            ScreenRoute.NOTIFICATION_CENTER -> ScreenRoute.DASHBOARD
                            else -> ScreenRoute.DASHBOARD
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (currentScreen != ScreenRoute.LOGIN && currentScreen != ScreenRoute.EXAM_WORK_SESSION) {
                AppBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Central Page Router
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                }, label = "ScreenTransition"
            ) { route ->
                when (route) {
                    ScreenRoute.LOGIN -> LoginScreen(
                        currentRole = currentRole,
                        viewModel = viewModel,
                        students = studentsList,
                        teachers = teachersList,
                        onLoginSuccess = { role ->
                            viewModel.switchRole(role)
                            currentScreen = ScreenRoute.DASHBOARD
                        }
                    )
                    ScreenRoute.DASHBOARD -> DashboardScreen(
                        viewModel = viewModel,
                        role = currentRole,
                        selectedStudent = selectedStudent,
                        selectedTeacher = selectedTeacher,
                        studentsCount = studentsList.size,
                        teachersCount = teachersList.size,
                        examsCount = examsList.size,
                        notifsCount = notificationsList.size,
                        onNavigate = { currentScreen = it }
                    )
                    ScreenRoute.STUDENT_CONVOCATION -> StudentConvocationScreen(
                        student = selectedStudent ?: Student(name = "مترشح مؤقت", email = "", nationalId = "", seatNumber = "", classRating = "", invitationCode = "", hallNumber = "", barcodeHash = ""),
                        exams = examsList,
                        submissions = submissionsList.filter { it.studentId == (selectedStudent?.id ?: 0) },
                        onDownloadPdf = { viewModel.downloadOfficialConvocation(context, it) },
                        onStartExam = { exam ->
                            activeExamForSession = exam
                            viewModel.startExamSession(exam.id)
                            currentScreen = ScreenRoute.EXAM_WORK_SESSION
                        }
                    )
                    ScreenRoute.EXAM_WORK_SESSION -> ActiveExamWorkspaceScreen(
                        exam = activeExamForSession ?: Exam(title = "اختبار", subjectId = "", date = "", time = "", durationMinutes = 60, totalPoints = 20.0, creatorId = 1),
                        questions = activeExamQuestions,
                        answers = studentAnswers,
                        onUpdateAnswer = { qId, choice -> viewModel.updateStudentAnswer(qId, choice) },
                        onSubmit = { examId ->
                            viewModel.submitExamAnswers(examId, selectedStudent?.id ?: 1)
                            currentScreen = ScreenRoute.STUDENT_CONVOCATION
                        }
                    )
                    ScreenRoute.TEACHER_CREATOR -> TeacherCreatorScreen(
                        exams = examsList,
                        subjects = listOf("MATH101", "PHYS202", "CHEM301"),
                        viewModel = viewModel,
                        onAddDraftQuestion = { exam ->
                            activeExamForSession = exam
                            currentScreen = ScreenRoute.TEACHER_QUESTION_DRAFTER
                        },
                        onReviewScores = { exam ->
                            activeExamForSession = exam
                            currentScreen = ScreenRoute.TEACHER_GRADER
                        },
                        onExportExcel = { viewModel.downloadExamResultsCsv(context, it) }
                    )
                    ScreenRoute.TEACHER_QUESTION_DRAFTER -> TeacherQuestionDrafterScreen(
                        exam = activeExamForSession ?: Exam(title = "اختبار", subjectId = "", date = "", time = "", durationMinutes = 60, totalPoints = 20.0, creatorId = 1),
                        viewModel = viewModel,
                        onSave = {
                            viewModel.addDraftQuestionToExam(activeExamForSession?.id ?: 1)
                            currentScreen = ScreenRoute.TEACHER_CREATOR
                        }
                    )
                    ScreenRoute.TEACHER_GRADER -> TeacherGradingCenterScreen(
                        exam = activeExamForSession ?: Exam(title = "اختبار", subjectId = "", date = "", time = "", durationMinutes = 60, totalPoints = 20.0, creatorId = 1),
                        submissions = submissionsList.filter { it.examId == (activeExamForSession?.id ?: 1) },
                        studentsMap = studentsList.associate { it.id to it.name },
                        viewModel = viewModel,
                        onBack = { currentScreen = ScreenRoute.TEACHER_CREATOR }
                    )
                    ScreenRoute.ROSTER_MANAGEMENT -> RosterManagementScreen(
                        students = studentsList,
                        teachers = teachersList,
                        viewModel = viewModel
                    )
                    ScreenRoute.NOTIFICATION_CENTER -> NotificationCenterScreen(
                        notifications = notificationsList,
                        viewModel = viewModel
                    )
                }
            }

            // Quick Floating Toast success notifier
            toastMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearToast() }) {
                            Icon(Icons.Default.Close, contentDescription = "قفل")
                        }
                    }
                }
            }
        }
    }
}

// === Sub-Composable Screen Views ===

// --- Application Navigation Header Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    currentScreen: ScreenRoute,
    unreadCount: Int,
    darkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onNavigateTo: (ScreenRoute) -> Unit,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            if (currentScreen != ScreenRoute.DASHBOARD) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
            } else {
                IconButton(onClick = { onNavigateTo(ScreenRoute.ROSTER_MANAGEMENT) }) {
                    Icon(Icons.Default.School, contentDescription = "اللوائح")
                }
            }
        },
        actions = {
            // Dark state toggle
            IconButton(onClick = onToggleDarkTheme) {
                Icon(
                    imageVector = if (darkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                    contentDescription = "الوضع المظلم/المضيء"
                )
            }
            // Real-time alert notifications icon with dynamic badge count
            Box(contentAlignment = Alignment.TopEnd) {
                IconButton(onClick = { onNavigateTo(ScreenRoute.NOTIFICATION_CENTER) }) {
                    Icon(Icons.Default.Notifications, contentDescription = "الإشعارات")
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = (-4).dp, y = (4).dp)
                            .size(16.dp)
                            .background(Color.Red, shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

// --- Application Bottom Navigation Bar ---
@Composable
fun AppBottomNavBar(
    currentScreen: ScreenRoute,
    onNavigate: (ScreenRoute) -> Unit
) {
    NavigationBar(
        windowInsets = WindowInsets.navigationBars,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.DASHBOARD,
            onClick = { onNavigate(ScreenRoute.DASHBOARD) },
            label = { Text("الرئيسية") },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.STUDENT_CONVOCATION,
            onClick = { onNavigate(ScreenRoute.STUDENT_CONVOCATION) },
            label = { Text("قاعة الامتحانات") },
            icon = { Icon(Icons.Default.Badge, contentDescription = "قاعة الامتحانات") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.TEACHER_CREATOR,
            onClick = { onNavigate(ScreenRoute.TEACHER_CREATOR) },
            label = { Text("الأسلاك والتشخيص") },
            icon = { Icon(Icons.Default.Description, contentDescription = "الامتحانات") }
        )
        NavigationBarItem(
            selected = currentScreen == ScreenRoute.NOTIFICATION_CENTER,
            onClick = { onNavigate(ScreenRoute.NOTIFICATION_CENTER) },
            label = { Text("الإشعارات") },
            icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "الإشعارات") }
        )
    }
}

// --- 1. Login Entrance Screen ---
@Composable
fun LoginScreen(
    currentRole: PortalRole,
    viewModel: ExamPortalViewModel,
    students: List<Student>,
    teachers: List<Teacher>,
    onLoginSuccess: (PortalRole) -> Unit
) {
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    
    // Background math symbols float decoration
    val floatingSymbols = listOf("∫", "π", "√", "Σ", "Δ", "≈", "x²", "E=mc²", "→")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // Drawing dynamic ambient gradient circles on the canvas background
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0D47A1).copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.1f, size.height * 0.2f),
                        radius = 400.dp.toPx()
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00796B).copy(alpha = 0.08f), Color.Transparent),
                        center = Offset(size.width * 0.8f, size.height * 0.8f),
                        radius = 450.dp.toPx()
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Symbol icons header
                Row(
                    modifier = Modifier.padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    floatingSymbols.take(4).forEach { sym ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = sym,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = "ســنـد التعليمي",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "بوابة إدارة وقبول الاستدعاء التلقائي وصياغة LaTeX",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                // Localization RTL: Roles Selector tabs
                Text(
                    text = "يرجى تحديد صفة الدخول:",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    textAlign = TextAlign.Right
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PortalRole.values().forEach { role ->
                        val isSelected = currentRole == role
                        Button(
                            onClick = { viewModel.switchRole(role) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(
                                    alpha = 0.1f
                                ),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (role) {
                                    PortalRole.ADMIN -> "مؤسسة / مشرف"
                                    PortalRole.TEACHER -> "أستاذ المشرف"
                                    PortalRole.STUDENT -> "طالب مترشح"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Inputs
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("البريد الإلكتروني الموحد") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("الرقم السري") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )

                Button(
                    onClick = { onLoginSuccess(currentRole) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تسجيل الدخول الآمن", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                // Immediate Bypass shortcuts cards which make testing extremely effortless
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تجاوز فوري وعرض لوحة التحكم مباشرة:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.switchRole(PortalRole.STUDENT)
                            if (students.isNotEmpty()) viewModel.selectStudent(students[0])
                            onLoginSuccess(PortalRole.STUDENT)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF0D47A1)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("طالب (وليد)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.switchRole(PortalRole.TEACHER)
                            if (teachers.isNotEmpty()) viewModel.selectTeacher(teachers[0])
                            onLoginSuccess(PortalRole.TEACHER)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0F2F1), contentColor = Color(0xFF00796B)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("أستاذ (أحمد)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.switchRole(PortalRole.ADMIN)
                            onLoginSuccess(PortalRole.ADMIN)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBE9E7), contentColor = Color(0xFFD84315)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("المدير الكلي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 2. Dashboard Interface ---
@Composable
fun DashboardScreen(
    viewModel: ExamPortalViewModel,
    role: PortalRole,
    selectedStudent: Student?,
    selectedTeacher: Teacher?,
    studentsCount: Int,
    teachersCount: Int,
    examsCount: Int,
    notifsCount: Int,
    onNavigate: (ScreenRoute) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        // Custom Greeting Banner Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = when (role) {
                                PortalRole.ADMIN -> "مدير الإدارة الرقابية"
                                PortalRole.TEACHER -> "أستاذ المادة"
                                PortalRole.STUDENT -> "مترشح مستقل"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "مرحباً بك مجدداً في نظام سند",
                        color = Color.White,
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = when (role) {
                        PortalRole.ADMIN -> "بوابة العمادة العامة لمركز الامتحانات"
                        PortalRole.TEACHER -> selectedTeacher?.name ?: "الأستاذ المحاضر"
                        PortalRole.STUDENT -> selectedStudent?.name ?: "المترشح التلقائي"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (role == PortalRole.STUDENT && selectedStudent != null) {
                    Text(
                        text = "رقم جلوسك الأكاديمي الموحد: ${selectedStudent.seatNumber} | القاعة: ${selectedStudent.hallNumber}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Statistical executive grid values
        Text(
            text = "المؤشرات الإحصائية العامة للعام الجامعي 2026",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "المترشحين",
                value = studentsCount.toString(),
                icon = Icons.Default.Groups,
                color = Color(0xFF1976D2),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "أساتذة المادة",
                value = teachersCount.toString(),
                icon = Icons.Default.SupervisorAccount,
                color = Color(0xFF00796B),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                title = "المسائل والاختبارات",
                value = examsCount.toString(),
                icon = Icons.Default.Quiz,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "بلاغات وتنبيهات",
                value = notifsCount.toString(),
                icon = Icons.Default.Campaign,
                color = Color(0xFF7B1FA2),
                modifier = Modifier.weight(1f)
            )
        }

        // System Shortcuts Buttons Depending on Role
        Text(
            text = "الوصول السريع للمهام التعليمية والأكاديمية",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 12.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Feature 1: Electronic Convocation PDF/QR Access (Available to all)
                ShortcutRow(
                    title = "فحص وقبول الاستدعاء التلقائي (الـ PDF والـ QR)",
                    desc = "مراجعة وترميز قاعة الامتحان والرمز القضيبى ومطابقة الهوية الرقمية.",
                    icon = Icons.Default.QrCodeScanner,
                    color = Color(0xFF0D47A1),
                    onClick = { onNavigate(ScreenRoute.STUDENT_CONVOCATION) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Feature 2: Exam editor with LaTeX (Teacher + Admin)
                ShortcutRow(
                    title = "مصنف منشئ الاختبارات ومحرر صيغ LaTeX",
                    desc = "إنشاء مسائل رياضية وتكاملات محددة واشتقاقات ورسم معايير الـ Rubric.",
                    icon = Icons.Default.AddBox,
                    color = Color(0xFF2E7D32),
                    onClick = { onNavigate(ScreenRoute.TEACHER_CREATOR) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Feature 3: Class Roster roster registry (Admin + Teacher)
                ShortcutRow(
                    title = "سجل الكوكبة واللوائح الموثقة",
                    desc = "قوائم الطلبة، أرقام الجلوس الذاتية، الفرز وصرف الأرقام الوطنية.",
                    icon = Icons.Default.FolderShared,
                    color = Color(0xFFD84315),
                    onClick = { onNavigate(ScreenRoute.ROSTER_MANAGEMENT) }
                )
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(color.copy(alpha = 0.15f), CircleShape)
                        .padding(6.dp)
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
                Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun ShortcutRow(
    title: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Right
            )
            Text(
                text = desc,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                textAlign = TextAlign.Right,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

// --- 3. Student Electronic Convocation and Exam Permit Card Room ---
@Composable
fun StudentConvocationScreen(
    student: Student,
    exams: List<Exam>,
    submissions: List<Submission>,
    onDownloadPdf: (Student) -> Unit,
    onStartExam: (Exam) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        Text(
            text = "بطاقة الاستدعاء والترخيص الإلكتروني الموحد للامتحانات",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Visual Layout mimicking real Printed convocation card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header decoration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("مقبول ومؤكد", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "الجمهورية - وزارة التعليم العالي",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Student and Seat details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(text = student.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(text = "رقم الهوية الجامعية: ${student.nationalId}", fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
                        Text(text = "المستوى الدراسي: ${student.classRating}", fontSize = 12.sp)
                        Text(text = "كود الملف: ${student.invitationCode}", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.primaryContainer)
                Spacer(modifier = Modifier.height(16.dp))

                // Dual Hall / Room Grid Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("رقم المقعد المحدد", fontSize = 11.sp, color = Color.Gray)
                        Text(student.seatNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("القاعة / المدرج", fontSize = 11.sp, color = Color.Gray)
                        Text(student.hallNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Render algorithmic QR Code
                Text(
                    text = "رمز QR الرقمي المعتمد في مكتب الدخول الرئاسي:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                QrCodeImage(content = student.barcodeHash)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onDownloadPdf(student) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مشاركة وتحميل بطاقة الاستدعاء PDF الرسمية", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Active Registered Exams to Take
        Text(
            text = "مواعيد وجدول الفتح للاختبارات النهائية المعتمدة لك:",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        exams.forEach { exam ->
            val sub = submissions.find { it.examId == exam.id }
            val isSubmitted = sub != null
            val isGraded = sub?.status == "GRADED"

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Badge Status representation
                        Box(
                            modifier = Modifier
                                .background(
                                    when {
                                        isGraded -> Color(0xFFE8F5E9)
                                        isSubmitted -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFECEFF1)
                                    },
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = when {
                                    isGraded -> "تم التصحيح والتقييم: ${sub.score}/${exam.totalPoints}"
                                    isSubmitted -> "تم تسليم ورقتك للأستاذ"
                                    else -> "جاهز للاجتياز الفوري"
                                },
                                color = when {
                                    isGraded -> Color(0xFF2E7D32)
                                    isSubmitted -> Color(0xFFE65100)
                                    else -> Color.Black
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (exam.subjectId == "MATH101") "قسم الرياضيات المتقدمة" else "قسم العلوم الفيزيائية",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = exam.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Right)
                    Text(text = "تاريخ الاختبار: ${exam.date} | توقيت الانطلاق: ${exam.time}", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 2.dp))
                    Text(text = "المدة الزمنية للاختبار: ${exam.durationMinutes} دقيقة | الدرجات الكلية: ${exam.totalPoints} درجات", fontSize = 11.sp, color = Color.Gray)

                    if (isGraded && sub != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF1F8E9), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFDCEDC8), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("ملاحظات الأستاذ وخلاصة الـ Rubrics المصنفة:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF33691E))
                                Text(text = sub.commentsJson, fontSize = 11.sp, textAlign = TextAlign.Right, color = Color(0xFF558B2F), modifier = Modifier.padding(top = 4.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = sub.rubricGradesJson, fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color.Gray, textAlign = TextAlign.Left, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    // Submission actions if exam is unsubmitted
                    if (!isSubmitted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onStartExam(exam) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دخول ورقة الاختبار الافتراضي واجتياز الامتحان", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- 4. Student Live Mathematical Exam Session Workspace ---
@Composable
fun ActiveExamWorkspaceScreen(
    exam: Exam,
    questions: List<Question>,
    answers: Map<Int, String>,
    onUpdateAnswer: (Int, String) -> Unit,
    onSubmit: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        // Active Exam Info Header
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Red, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("مراقب رقمياً", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "ورقة الإجابة الرقمية للمترشح", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "الامتحان: ${exam.title}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Text(text = "الحد الأقصى للعلامات: ${exam.totalPoints} درجات | المدة: ${exam.durationMinutes} دقيقة كحد أقصى للعد التنازلي.", fontSize = 11.sp, color = Color.Gray)
            }
        }

        // Loop dynamic Scientific LaTeX questions
        questions.forEachIndexed { index, q ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "[ الدرجة المستحقة: ${q.points}ن ]", color = Color(0xFF0D47A1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = "مسألة رقم ${index + 1} (${when(q.type) {
                                "QCM" -> "اختيار متعامد"
                                "MATH_PROBLEM" -> "مسألة استنتاجية تفاضلية"
                                "SCIENTIFIC" -> "قوانين تطبيقية عامة"
                                "TABLE" -> "مسألة جداول ومصفوفات علمية"
                                else -> "سؤال مفتوح"
                            }})",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // LATEX RENDERING INTERACTION
                    LatexText(text = q.text)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Different response components depending on question type
                    if (q.type == "QCM") {
                        val choices = q.choicesJson.split("|")
                        choices.forEach { choice ->
                            val isSelected = answers[q.id] == choice
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpdateAnswer(q.id, choice) }
                                    .padding(vertical = 6.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = choice,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Right,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onUpdateAnswer(q.id, choice) }
                                )
                            }
                        }
                    } else {
                        // Math problem, open response typing
                        OutlinedTextField(
                            value = answers[q.id] ?: "",
                            onValueChange = { onUpdateAnswer(q.id, it) },
                            placeholder = { Text("أدخل صياغة إجابتك الرياضية هنا مبسطة، للاختبار يدعم كتابة $...$ أو رموز LaTeX") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6
                        )
                        
                        // Live LaTeX typing preview specifically for student solutions so they see feedback!
                        if ((answers[q.id] ?: "").isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "معاينة إجابتك وصياغتك الرياضية الفورية:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                LatexText(text = answers[q.id] ?: "")
                            }
                        }
                    }
                }
            }
        }

        // Final Exam Submit Card
        Button(
            onClick = { onSubmit(exam.id) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.CloudUpload, contentDescription = null)
            Spacer(modifier = Modifier.width(10.dp))
            Text("تأكيد وتسليم ورقة الإجابة الكلية إلكترونياً", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

// --- 5. Teacher Exam Management Console ---
@Composable
fun TeacherCreatorScreen(
    exams: List<Exam>,
    subjects: List<String>,
    viewModel: ExamPortalViewModel,
    onAddDraftQuestion: (Exam) -> Unit,
    onReviewScores: (Exam) -> Unit,
    onExportExcel: (Exam) -> Unit
) {
    var examTitleInput by remember { mutableStateOf("") }
    var durationInput by remember { mutableStateOf("90") }
    var totalPointsInput by remember { mutableStateOf("20") }
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        Text(
            text = "منشئ قوالب الامتحانات وإدارة الفحوص الحيوية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Fast exam creation form card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                Text(
                    text = "نشر قالب اختبار جديد بالكامل:",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = examTitleInput,
                    onValueChange = { examTitleInput = it },
                    label = { Text("عنوان موضوع الامتحان الموحد") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = totalPointsInput,
                        onValueChange = { totalPointsInput = it },
                        label = { Text("علامة الاختبار") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it },
                        label = { Text("المدة الكلية d") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Simple subject choice tabs
                Text(text = "المسار الاختصاصي:", fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subjects.forEach { subj ->
                        val isSelected = selectedSubject == subj
                        Button(
                            onClick = { selectedSubject = subj },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.2f),
                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(subj, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (examTitleInput.isNotEmpty()) {
                            viewModel.createExamWithQuestions(
                                title = examTitleInput,
                                subjectId = selectedSubject,
                                date = dateFormat.format(Date()),
                                time = timeFormat.format(Date()),
                                duration = durationInput.toIntOrNull() ?: 90,
                                points = totalPointsInput.toDoubleOrNull() ?: 20.0
                            )
                            examTitleInput = ""
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إطلاق ونشر قالب الامتحان فوراً للرواق المالي", fontWeight = FontWeight.Bold)
                }
            }
        }

        // List of Active Exams with teacher options
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("المجموع: ${exams.size}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Text(
                text = "قوالب الاختبارات ومراقبة التصحيحات بالـ Rubrics:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        exams.forEach { exam ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE0F2F1), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(exam.subjectId, color = Color(0xFF004D40), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = exam.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Right)
                    }

                    Text(text = "تاريخ النشر: ${exam.date} | المدة: ${exam.durationMinutes} دقيقة", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 4.dp))
                    Text(text = "إجمالي النقاط: ${exam.totalPoints} درجات", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Teacher action tools
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { onAddDraftQuestion(exam) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ألحق سؤال LaTeX", fontSize = 10.sp)
                        }

                        Button(
                            onClick = { onReviewScores(exam) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Grade, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تقييم و رصد الدرجات", fontSize = 10.sp)
                        }

                        IconButton(
                            onClick = { onExportExcel(exam) },
                            modifier = Modifier
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .size(40.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "تصدير نتائج Excel", tint = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }
    }
}

// --- 6. Teacher Workbench for LaTeX Mathematical Question Generator ---
@Composable
fun TeacherQuestionDrafterScreen(
    exam: Exam,
    viewModel: ExamPortalViewModel,
    onSave: () -> Unit
) {
    val draftText by viewModel.latexDraftText.collectAsState()
    val draftPoints by viewModel.latexDraftPoints.collectAsState()
    val draftType by viewModel.latexDraftType.collectAsState()
    val choicesList by viewModel.latexDraftChoicesList.collectAsState()
    val answerHint by viewModel.latexDraftAnswerHint.collectAsState()
    val rubricsText by viewModel.latexDraftRubricCriteria.collectAsState()

    // Preloaded Math triggers shortcuts bar
    val shortcutKeys = listOf(
        "\\frac{a}{b}", "\\sqrt{x}", "\\int_{a}^{b}", "x^2", "u_0", "\\pi", "\\Delta", "\\sin(x)", "\\begin{pmatrix}"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        Text(
            text = "مصمم المسائل الرياضية وصياغة معادلات LaTeX مع معاينة فورية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                
                // Form setup parameters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = draftPoints,
                        onValueChange = { viewModel.latexDraftPoints.value = it },
                        label = { Text("علامة السؤال") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Question Type Selector
                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedTextField(
                            value = when(draftType) {
                                "QCM" -> "اختيار متعامد"
                                "MATH_PROBLEM" -> "مسألة رياضية استنتاجية"
                                "SCIENTIFIC" -> "فيزياء وكهرباء"
                                "TABLE" -> "مصفوفة/جدول علمي"
                                else -> "سؤال مقالي"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("نمط المسألة") },
                            trailingIcon = {
                                IconButton(onClick = {
                                    val nextType = when(draftType) {
                                        "QCM" -> "MATH_PROBLEM"
                                        "MATH_PROBLEM" -> "SCIENTIFIC"
                                        "SCIENTIFIC" -> "TABLE"
                                        else -> "QCM"
                                    }
                                    viewModel.latexDraftType.value = nextType
                                }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Copy Paste LaTeX Symbols Toolbar which is highly helpful!
                Text(
                    text = "شريط الرموز الرياضية الجاهزة للإدراج التلقائي:",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    shortcutKeys.forEach { key ->
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .clickable {
                                    viewModel.latexDraftText.value += " $key "
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = key, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Question LaTeX input field
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { viewModel.latexDraftText.value = it },
                    label = { Text("نص المسألة العلمية (كتابة تفصيلية تدعم LaTeX)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )

                Spacer(modifier = Modifier.height(16.dp))

                // CRITICAL REALTIME PREVIEW WITH NO LAG offline
                Text(
                    text = "المعاينة البصرية والرياضية الفورية للمعادلات (LaTeX Live Preview):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D47A1),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFECEFF1).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    // Typewriter
                    LatexText(text = draftText)
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (draftType == "QCM") {
                    Text(text = "الخيارات المتوفرة (مفصولة ومحررة):", fontSize = 11.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = choicesList.joinToString(" | "),
                        onValueChange = { 
                            viewModel.latexDraftChoicesList.value = it.split(" | ").map { s -> s.trim() }
                        },
                        label = { Text("الخيارات المقترحة مفصولة بـ | ") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                // Helper criteria Rubrics setup
                OutlinedTextField(
                    value = rubricsText,
                    onValueChange = { viewModel.latexDraftRubricCriteria.value = it },
                    label = { Text("معايير التقييم وتوزيع نقاط الـ Rubric التفصيلية") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                OutlinedTextField(
                    value = answerHint,
                    onValueChange = { viewModel.latexDraftAnswerHint.value = it },
                    label = { Text("توجيه الإجابة النموذجية ومفتاح الحل المصنف") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onSave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ السؤال بقسم الأسئلة المعتمدة", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- 7. Teacher Grading Panel with Slider criteria Rubrics ---
@Composable
fun TeacherGradingCenterScreen(
    exam: Exam,
    submissions: List<Submission>,
    studentsMap: Map<Int, String>,
    viewModel: ExamPortalViewModel,
    onBack: () -> Unit
) {
    var gradingSelectedSub by remember { mutableStateOf<Submission?>(null) }
    
    // Sliders for dynamic Criterion-based Rubrics evaluation
    var rubricFormulaSlider by remember { mutableStateOf(4.0f) }
    var rubricCalculationSlider by remember { mutableStateOf(3.5f) }
    var rubricLogicSlider by remember { mutableStateOf(7.0f) }
    
    var manualCommentInput by remember { mutableStateOf("إجابة قوية تظهر مهارة جيدة في استخدام القوانين الحسابية.") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        Text(
            text = "مركز توقيع وفرز درجات المترشحين بالـ Rubrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (gradingSelectedSub == null) {
            // Unexamined sheets lists
            Text(
                text = "قائمة الأوراق المسلمة لاختبار: ${exam.title}",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (submissions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Text("لم تسلم أي أوراق إجابة بعد لهذا الاختبار الموحد.", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }

            submissions.forEach { sub ->
                val stdName = studentsMap[sub.studentId] ?: "مترشح مستقل"
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clickable { gradingSelectedSub = sub }
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (sub.status == "GRADED") Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (sub.status == "GRADED") "تم الانتهاء: ${sub.score}" else "معلق وقيد التصحيح اللمسي",
                                    color = if (sub.status == "GRADED") Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(text = "الاسم: $stdName", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "تاريخ ووقت التسليم: ${sub.submissionDate}", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "النص المختصر للإجابة: ${sub.answersJson.take(100)}...", fontSize = 11.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                gradingSelectedSub = sub
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("البدء بالتقييم التفصيلي بالـ Rubrics", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else {
            // Detailed Rubric Scoring workbench sheet
            val sub = gradingSelectedSub!!
            val stdName = studentsMap[sub.studentId] ?: "مترشح مستقل"

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { gradingSelectedSub = null }) {
                            Icon(Icons.Default.Close, contentDescription = "قفل")
                        }
                        Text(text = "تصحيح ورقة الطالب: $stdName", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "إجابة المترشح المقروءة:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Gray)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = sub.answersJson, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "رصد الدرجات التفصيلي استناداً لمعايير الـ Rubric:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                    // Rubric Criterion 1: Formula Correctness
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "الدرجة المستحقة: ${String.format("%.1f", rubricFormulaSlider)} / 5.0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "1. صياغة القوانين واستخدام الرموز الرياضية:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Slider(
                        value = rubricFormulaSlider,
                        onValueChange = { rubricFormulaSlider = it },
                        valueRange = 0f..5f,
                        steps = 9
                    )

                    // Rubric Criterion 2: Calculation Correctness
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "الدرجة المستحقة: ${String.format("%.1f", rubricCalculationSlider)} / 5.0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "2. الدقة العددية وصحة التكامل/الاشتقاق:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Slider(
                        value = rubricCalculationSlider,
                        onValueChange = { rubricCalculationSlider = it },
                        valueRange = 0f..5f,
                        steps = 9
                    )

                    // Rubric Criterion 3: Logical Sequence of proof
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "الدرجة المستحقة: ${String.format("%.1f", rubricLogicSlider)} / 10.0", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(text = "3. التسلسل المنطقي وجودة الحل العلمي المعتمد:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Slider(
                        value = rubricLogicSlider,
                        onValueChange = { rubricLogicSlider = it },
                        valueRange = 0f..10f,
                        steps = 19
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    val computedSum = rubricFormulaSlider + rubricCalculationSlider + rubricLogicSlider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "العلامة التراكمية الإجمالية المحتسبة: ${String.format("%.1f", computedSum)} / 20.0 درجات إجمالاً",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualCommentInput,
                        onValueChange = { manualCommentInput = it },
                        label = { Text("الملاحظات والتوجيهات الأكاديمية للمصحح") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val rubricOutput = "معيار القوانين: ${String.format("%.1f", rubricFormulaSlider)}/5.0 | معيار الحسابات: ${String.format("%.1f", rubricCalculationSlider)}/5.0 | التسلسل المنطقي: ${String.format("%.1f", rubricLogicSlider)}/10.0"
                            viewModel.submitManualGrading(
                                submissionId = sub.id,
                                finalScore = computedSum.toDouble(),
                                rubricsFeedback = rubricOutput,
                                comments = manualCommentInput
                            )
                            gradingSelectedSub = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اعتماد رصد العلامة النهائية الكلية والمصادقة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- 8. Roster Registry Management Screen (Admin view) ---
@Composable
fun RosterManagementScreen(
    students: List<Student>,
    teachers: List<Teacher>,
    viewModel: ExamPortalViewModel
) {
    var isAddingStudent by remember { mutableStateOf(false) }
    
    // Inputs student form
    var sName by remember { mutableStateOf("") }
    var sEmail by remember { mutableStateOf("") }
    var sNationalId by remember { mutableStateOf("") }
    var sClass by remember { mutableStateOf("السنة الثانية - قسم الرياضيات") }
    var sHall by remember { mutableStateOf("قاعة الخوارزمي (2)") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { isAddingStudent = !isAddingStudent },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isAddingStudent) "قيد العرض اللائحي" else "سجل مترشحاً جديداً")
            }
            Text(
                text = "سجلات لوائح الترشح الأكاديمية المعتمدة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (isAddingStudent) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.End) {
                    Text("إدراج قيد مترشح جديد بنظام سند المالي والتشريعي:", fontWeight = FontWeight.Bold)

                    OutlinedTextField(value = sName, onValueChange = { sName = it }, label = { Text("الاسم واللقب الكامل") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = sEmail, onValueChange = { sEmail = it }, label = { Text("البريد الموحد الرسمي") }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    OutlinedTextField(value = sNationalId, onValueChange = { sNationalId = it }, label = { Text("رقم الهوية الوطنية/الجامعية الموحد") }, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
                    OutlinedTextField(value = sClass, onValueChange = { sClass = it }, label = { Text("الشعبة والقسم") }, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp))
                    OutlinedTextField(value = sHall, onValueChange = { sHall = it }, label = { Text("قاعة الامتحانات") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (sName.isNotEmpty()) {
                                viewModel.createStudent(sName, sEmail, sNationalId, sClass, sHall)
                                sName = ""
                                sEmail = ""
                                sNationalId = ""
                                isAddingStudent = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("اعتماد وإصدار بطاقة الاستدعاء والترخيص", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // List display candidates roster details
        Text("بيانات المترشحين المقيدين بنظام سند:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        
        students.forEach { std ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                        Text(text = std.name, fontWeight = FontWeight.Bold)
                        Text(text = "رقم جلوس: ${std.seatNumber} | قاعة الامتحان: ${std.hallNumber}", fontSize = 11.sp, color = Color.Gray)
                        Text(text = "الشعبة والجامعة: ${std.classRating}", fontSize = 11.sp, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)).padding(10.dp)) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// --- 9. Notification Center & Alerts Panel ---
@Composable
fun NotificationCenterScreen(
    notifications: List<Notification>,
    viewModel: ExamPortalViewModel
) {
    LaunchedEffect(Unit) {
        viewModel.markNotificationsAsRead()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.End
    ) {
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { viewModel.markNotificationsAsRead() }) {
                Text("تحديد الكل كمقروء")
            }
            Text(
                text = "صندوق البلاغات والإشعارات الوزارية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, modifier = Modifier.size(52.dp), tint = Color.LightGray)
                    Text("الصندوق فارغ من أي بلاغات حتى الساعة.", color = Color.Gray, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }

        notifications.forEach { notif ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (notif.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(text = notif.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = notif.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), textAlign = TextAlign.Right, modifier = Modifier.padding(top = 2.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                when (notif.type) {
                                    "CONVOCATION" -> Color(0xFFE3F2FD)
                                    "ALERT" -> Color(0xFFFFF3E0)
                                    "EXAM_GRADE" -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFECEFF1)
                                },
                                CircleShape
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = when (notif.type) {
                                "CONVOCATION" -> Icons.Default.QrCode
                                "ALERT" -> Icons.Default.Warning
                                "EXAM_GRADE" -> Icons.Default.AssignmentTurnedIn
                                else -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (notif.type) {
                                "CONVOCATION" -> Color(0xFF1976D2)
                                "ALERT" -> Color(0xFFE65100)
                                "EXAM_GRADE" -> Color(0xFF2E7D32)
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// === Native File Sharing Helper Utility ===
private fun shareGeneratedFile(context: Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val fileUri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (file.name.endsWith(".pdf")) "application/pdf" else "text/csv"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = Intent.createChooser(intent, "حفظ ومشاركة المستند التعليمي من سند")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
