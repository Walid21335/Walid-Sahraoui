package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.pdf.PdfGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class PortalRole {
    ADMIN, TEACHER, STUDENT
}

class ExamPortalViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = AppRepository(db.appDao())

    // UI perspective & Active States
    private val _currentRole = MutableStateFlow(PortalRole.STUDENT)
    val currentRole: StateFlow<PortalRole> = _currentRole.asStateFlow()

    private val _selectedStudent = MutableStateFlow<Student?>(null)
    val selectedStudent: StateFlow<Student?> = _selectedStudent.asStateFlow()

    private val _selectedTeacher = MutableStateFlow<Teacher?>(null)
    val selectedTeacher: StateFlow<Teacher?> = _selectedTeacher.asStateFlow()

    // Database Streams
    val students: StateFlow<List<Student>> = repository.allStudents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teachers: StateFlow<List<Teacher>> = repository.allTeachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<Subject>> = repository.allSubjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exams: StateFlow<List<Exam>> = repository.allExams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val submissions: StateFlow<List<Submission>> = repository.allSubmissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<Notification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val studentCount: StateFlow<Int> = repository.studentsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val teacherCount: StateFlow<Int> = repository.teachersCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val examCount: StateFlow<Int> = repository.examsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Temporary Visual Feedback States
    private val _exportedFile = MutableStateFlow<File?>(null)
    val exportedFile: StateFlow<File?> = _exportedFile.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // --- Student Exam Session States ---
    private val _activeExamQuestions = MutableStateFlow<List<Question>>(emptyList())
    val activeExamQuestions: StateFlow<List<Question>> = _activeExamQuestions.asStateFlow()

    private val _studentAnswers = MutableStateFlow<Map<Int, String>>(emptyMap()) // Maps QuestionId to answered option or typed text
    val studentAnswers: StateFlow<Map<Int, String>> = _studentAnswers.asStateFlow()

    // --- Teacher LaTeX Question Draft States ---
    val latexDraftText = MutableStateFlow("احسب المشتقة الكلية \\[ f'(x) = \\frac{d}{dx}(\\sin(x^2)) \\]")
    val latexDraftPoints = MutableStateFlow("5")
    val latexDraftType = MutableStateFlow("MATH_PROBLEM") // QCM, OPEN, MATH_PROBLEM, SCIENTIFIC
    val latexDraftChoicesList = MutableStateFlow(listOf("\$2x \\cos(x^2)\$", "\$2 \\cos(x^2)\$", "\$-\\cos(x^2)\$", "\$\\sin(2x)\$"))
    val latexDraftAnswerHint = MutableStateFlow("الحل يعتمد على استخدام قاعدة السلسلة (Chain Rule).")
    val latexDraftRubricCriteria = MutableStateFlow("تطبيق قاعدة السلسلة (2.5) | دقة مشتق الدالة الخارجية (1.5) | تصفيف الإشارة الحسابية (1.0)")

    init {
        // Prepopulate data securely inside a coroutine dispatcher context
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
            // Default to first student and teacher to bypass manual typing immediately for review
            val stdList = repository.allStudents.first()
            if (stdList.isNotEmpty()) {
                _selectedStudent.value = stdList[0] // Set Walid Al-Hawasi
            }
            val tchList = repository.allTeachers.first()
            if (tchList.isNotEmpty()) {
                _selectedTeacher.value = tchList[0] // Set Ahmed Al-Mahmoud
            }
        }
    }

    fun switchRole(role: PortalRole) {
        _currentRole.value = role
        viewModelScope.launch {
            val stds = repository.allStudents.first()
            val tchs = repository.allTeachers.first()
            if (role == PortalRole.STUDENT && stds.isNotEmpty()) {
                _selectedStudent.value = stds[0]
            } else if (role == PortalRole.TEACHER && tchs.isNotEmpty()) {
                _selectedTeacher.value = tchs[0]
            }
        }
    }

    fun selectStudent(student: Student) {
        _selectedStudent.value = student
    }

    fun selectTeacher(teacher: Teacher) {
        _selectedTeacher.value = teacher
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun clearExportedFile() {
        _exportedFile.value = null
    }

    // --- PDF / Exam Permit Management ---
    fun downloadOfficialConvocation(context: Context, student: Student) {
        viewModelScope.launch {
            val examList = exams.value
            val pdfFile = PdfGenerator.generateConvocationPdf(context, student, examList)
            if (pdfFile != null) {
                _exportedFile.value = pdfFile
                _toastMessage.value = "تم إنشاء البوابة والاستدعاء الرقمي الموثق بنجاح وصيغة PDF جاهزة للحفظ والتسجيل!"
                
                // Post internal alert notification
                repository.insertNotification(Notification(
                    title = "لقد تم تصدير وتحميل استدعائك الرسمي",
                    message = "تم حفظ ملف PDF الموثق برمز QR Code بنجاح لاسمه: ${student.name} بالقاعة: ${student.hallNumber}",
                    type = "SYSTEM"
                ))
            } else {
                _toastMessage.value = "فشل تصدير مستند الاستحفاظ الإلكتروني. يرجى مراجعة إعدادات الذاكرة."
            }
        }
    }

    fun downloadExamResultsCsv(context: Context, exam: Exam) {
        viewModelScope.launch {
            val filteredSubs = submissions.value.filter { it.examId == exam.id }
            val stdsMap = students.value.associate { it.id to it.name }
            val csvFile = PdfGenerator.exportScoresExcel(context, exam.title, filteredSubs, stdsMap)
            if (csvFile != null) {
                _exportedFile.value = csvFile
                _toastMessage.value = "تم استخراج ملف الإحصائيات الشامل للدرجات كصيغة Excel (CSV) بنجاح للتنزيل!"
            } else {
                _toastMessage.value = "فشل تصدير ملف النتائج."
            }
        }
    }

    // --- Admin Operations ---
    fun createStudent(name: String, email: String, nationalId: String, classRating: String, hallNumber: String) {
        viewModelScope.launch {
            val code = "CONV-2026-${(1000..9999).random()}"
            val newStudent = Student(
                name = name,
                email = email,
                nationalId = nationalId,
                seatNumber = "SEC-${('A'..'D').random()}-${(100..499).random()}",
                classRating = classRating,
                invitationCode = code,
                hallNumber = hallNumber,
                barcodeHash = "STU:$nationalId:$code:${(1000..9999).random()}"
            )
            repository.insertStudent(newStudent)
            _toastMessage.value = "تم تسجيل وإدراج المترشح $name بنظام التصريح بنجاح!"
            
            repository.insertNotification(Notification(
                title = "تسجيل مترشح جديد",
                message = "تم تسجيل المترشح $name وقسم الاستدعاء الإلكتروني بنجاح برقم جلوسه تلقائياً.",
                type = "CONVOCATION"
            ))
        }
    }

    fun createTeacher(name: String, email: String, department: String) {
        viewModelScope.launch {
            val code = "TCH-${(100..999).random()}"
            val newTeacher = Teacher(
                name = name,
                email = email,
                department = department,
                teacherCode = code
            )
            repository.insertTeacher(newTeacher)
            _toastMessage.value = "تم إدراج بيانات الأستاذ $name بنجاح."
        }
    }

    fun addNotification(title: String, message: String, type: String = "ALERT") {
        viewModelScope.launch {
            repository.insertNotification(Notification(title = title, message = message, type = type))
        }
    }

    // --- Exam Setup (Teacher View) ---
    fun createExamWithQuestions(title: String, subjectId: String, date: String, time: String, duration: Int, points: Double) {
        viewModelScope.launch {
            val creator = _selectedTeacher.value?.id ?: 1
            val newExam = Exam(
                title = title,
                subjectId = subjectId,
                date = date,
                time = time,
                durationMinutes = duration,
                totalPoints = points,
                creatorId = creator,
                isPublished = true
            )
            val newExamId = repository.insertExam(newExam).toInt()

            // Automatically inject a default LaTeX mathematical question into the newly created exam
            repository.insertQuestion(Question(
                examId = newExamId,
                type = "MATH_PROBLEM",
                text = "باستخدام التعريف المناسب للمشتقة الأولى: \\[ f'(x) = \\lim_{h \\to 0} \\frac{f(x + h) - f(x)}{h} \\] أوجد مشتقة الدالة \$f(x) = x^2\$ مبينًا جميع خطوات النهاية الرياضية.",
                answerHint = "f'(x) = 2x. الخطوات تشمل فك المربع وطرح f(x) ثم الاختصار بالـ h.",
                rubricCriteriaJson = "صحة صياغة المبرهنة (2) | إتقان فك وتصفييد الأقواس الجبرية (3) | الدقة الرياضية (2)",
                points = 7.0
            ))

            _toastMessage.value = "تم نشر وحفظ قالب الاختبار النهائي $title وجاهز للاجتياز!"
            repository.insertNotification(Notification(
                title = "اختبار جديد متاح للاجتياز",
                message = "تم نشر اختبار مادة $title مضافاً إليه مشتقات تفاضلية. يرجى من المترشحين مراجعة الاستدعاء لمواعيد الجلوس.",
                type = "ALERT"
            ))
        }
    }

    // Substantially appends a custom drafted quiz question containing LaTeX to an existing exam
    fun addDraftQuestionToExam(examId: Int) {
        viewModelScope.launch {
            val pts = latexDraftPoints.value.toDoubleOrNull() ?: 5.0
            val newQuestion = Question(
                examId = examId,
                type = latexDraftType.value,
                text = latexDraftText.value,
                choicesJson = if (latexDraftType.value == "QCM") latexDraftChoicesList.value.joinToString("|") else "",
                answerHint = latexDraftAnswerHint.value,
                rubricCriteriaJson = latexDraftRubricCriteria.value,
                points = pts
            )
            repository.insertQuestion(newQuestion)
            _toastMessage.value = "تم حفظ المسألة العلمية بنجاح وإلحاقها بأسئلة الامتحان!"
        }
    }

    // --- Student Taking Exam Flow ---
    fun startExamSession(examId: Int) {
        viewModelScope.launch {
            _studentAnswers.value = emptyMap()
            // Load questions reactively
            repository.getQuestionsByExam(examId).collect { list ->
                _activeExamQuestions.value = list
            }
        }
    }

    fun updateStudentAnswer(questionId: Int, answer: String) {
        val updated = _studentAnswers.value.toMutableMap()
        updated[questionId] = answer
        _studentAnswers.value = updated
    }

    // Submits and triggers the custom automatic/semi-automatic assessment engines with Rubrics
    fun submitExamAnswers(examId: Int, studentId: Int) {
        viewModelScope.launch {
            val currentQuestions = _activeExamQuestions.value
            val answers = _studentAnswers.value
            
            var calculatedScore = 0.0
            val rubricGradesBuilder = StringBuilder()
            val commentsBuilder = StringBuilder()

            // 1. Interactive quiz auto-evaluation for standard QCM options
            currentQuestions.forEach { q ->
                val stdAns = answers[q.id] ?: ""
                if (q.type == "QCM") {
                    val isCorrect = q.answerHint.contains(stdAns) || (stdAns.isNotEmpty() && q.choicesJson.split("|").firstOrNull() == stdAns)
                    if (isCorrect) {
                        calculatedScore += q.points
                        rubricGradesBuilder.append("السؤال: ${q.id} (QCM) -> صحيح (مستحق: ${q.points}/${q.points})\n")
                    } else {
                        rubricGradesBuilder.append("السؤال: ${q.id} (QCM) -> غير دقيق (مستحق: 0.0/${q.points})\n")
                    }
                } else {
                    // Semi-auto evaluation: initially allocate 50% score for standard formulas matches, teachers modify later
                    var scoreAwarded = 0.0
                    val formulaKeywords = listOf("sin", "\\frac", "cos", "\\int", "\\sqrt", "9*10^13", "e^", "x", "y")
                    var matches = 0
                    formulaKeywords.forEach { word ->
                        if (stdAns.contains(word)) matches++
                    }
                    if (matches > 0) {
                        scoreAwarded = q.points * (0.4 + (0.1 * matches).coerceAtMost(0.6))
                        commentsBuilder.append("توجيه تلقائي للسؤال رقم ${q.id}: تم الكشف عن صياغة رياضية صحيحة.\n")
                    } else {
                        scoreAwarded = q.points * 0.1 // minimum participation
                    }
                    calculatedScore += scoreAwarded
                    rubricGradesBuilder.append("السؤال: ${q.id} -> تم تقييم الكود وتوجيه الدرجة تلقائياً: $scoreAwarded/${q.points}\n")
                }
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val newSubmission = Submission(
                examId = examId,
                studentId = studentId,
                answersJson = answers.entries.joinToString("\n") { "س ${it.key}: ${it.value}" },
                score = calculatedScore,
                commentsJson = if (commentsBuilder.isNotEmpty()) commentsBuilder.toString() else "تم تسليم الإجابات بنجاح وسيتم المراجعة النهائية من قبل الأستاذ.",
                status = "SENT", // Marked as SENT (Submitted, ready for further Rubric detail editing by professional teacher)
                submissionDate = dateFormat.format(Date()),
                rubricGradesJson = rubricGradesBuilder.toString()
            )

            repository.insertSubmission(newSubmission)
            _toastMessage.value = "تم رفع وتسجيل ورقة الامتحان إلكترونياً بنجاح! نتيجتك التلقائية الأولية: $calculatedScore"
            
            repository.insertNotification(Notification(
                title = "تم تسليم ورقة إجابة جديدة",
                message = "قام المترشح ID: $studentId بتسليم إجاباته في الامتحان بنجاح للفرز الماسي.",
                type = "EXAM_GRADE"
            ))
        }
    }

    // --- Teacher Rubric-based Grading / Re-Assessment ---
    fun submitManualGrading(submissionId: Int, finalScore: Double, rubricsFeedback: String, comments: String) {
        viewModelScope.launch {
            // Find submission, update, save
            val list = submissions.value
            val sub = list.find { it.id == submissionId }
            if (sub != null) {
                val updatedSub = sub.copy(
                    score = finalScore,
                    rubricGradesJson = rubricsFeedback,
                    commentsJson = comments,
                    status = "GRADED" // Marked as permanently evaluated
                )
                repository.insertSubmission(updatedSub)
                _toastMessage.value = "تم اعتماد التقييم الرقمي بالـ Rubrics ونشر النتيجة النهائية للمترشح!"
                
                repository.insertNotification(Notification(
                    title = "لقد تم تصحيح ورقة اختبارك",
                    message = "قام الأستاذ المشرف باعتماد الدرجة النهائية لورقتك: $finalScore درجات مع تلميحات الـ Rubric.",
                    type = "EXAM_GRADE"
                ))
            }
        }
    }

    fun markNotificationsAsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }
}
