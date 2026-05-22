package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(private val appDao: AppDao) {

    // Exposure of Flows
    val allStudents: Flow<List<Student>> = appDao.getAllStudents()
    val studentsCount: Flow<Int> = appDao.getStudentsCount()

    val allTeachers: Flow<List<Teacher>> = appDao.getAllTeachers()
    val teachersCount: Flow<Int> = appDao.getTeachersCount()

    val allSubjects: Flow<List<Subject>> = appDao.getAllSubjects()
    val subjectsCount: Flow<Int> = appDao.getSubjectsCount()

    val allExams: Flow<List<Exam>> = appDao.getAllExams()
    val examsCount: Flow<Int> = appDao.getExamsCount()

    val allSubmissions: Flow<List<Submission>> = appDao.getAllSubmissions()

    val allNotifications: Flow<List<Notification>> = appDao.getAllNotifications()
    val unreadNotificationsCount: Flow<Int> = appDao.getUnreadNotificationsCount()

    // Database insertions and queries wrapping
    suspend fun getStudentById(id: Int): Student? = appDao.getStudentById(id)
    suspend fun getStudentByEmail(email: String): Student? = appDao.getStudentByEmail(email)
    suspend fun getStudentByNationalId(nid: String): Student? = appDao.getStudentByNationalId(nid)
    suspend fun insertStudent(student: Student): Long = appDao.insertStudent(student)
    suspend fun deleteStudent(student: Student) = appDao.deleteStudent(student)

    suspend fun getTeacherById(id: Int): Teacher? = appDao.getTeacherById(id)
    suspend fun getTeacherByEmail(email: String): Teacher? = appDao.getTeacherByEmail(email)
    suspend fun insertTeacher(teacher: Teacher): Long = appDao.insertTeacher(teacher)
    suspend fun deleteTeacher(teacher: Teacher) = appDao.deleteTeacher(teacher)

    suspend fun insertSubject(subject: Subject) = appDao.insertSubject(subject)

    suspend fun getExamById(id: Int): Exam? = appDao.getExamById(id)
    suspend fun insertExam(exam: Exam): Long = appDao.insertExam(exam)
    suspend fun deleteExam(examId: Int) {
        appDao.deleteExamById(examId)
        appDao.deleteQuestionsForExam(examId)
    }

    fun getQuestionsByExam(examId: Int): Flow<List<Question>> = appDao.getQuestionsByExam(examId)
    suspend fun getQuestionsByExamSync(examId: Int): List<Question> = appDao.getQuestionsByExamSync(examId)
    suspend fun insertQuestion(question: Question): Long = appDao.insertQuestion(question)

    fun getSubmissionsForExam(examId: Int): Flow<List<Submission>> = appDao.getSubmissionsForExam(examId)
    fun getSubmissionsForStudent(studentId: Int): Flow<List<Submission>> = appDao.getSubmissionsForStudent(studentId)
    suspend fun getSubmission(examId: Int, studentId: Int): Submission? = appDao.getSubmission(examId, studentId)
    suspend fun insertSubmission(submission: Submission): Long = appDao.insertSubmission(submission)

    suspend fun insertNotification(notification: Notification): Long = appDao.insertNotification(notification)
    suspend fun markNotificationAsRead(id: Int) = appDao.markNotificationAsRead(id)
    suspend fun markAllNotificationsAsRead() = appDao.markAllNotificationsAsRead()

    // Prepopulate some initial data if database is empty
    suspend fun prepopulateIfNeeded() {
        val currentStdCount = studentsCount.first()
        if (currentStdCount > 0) return // Already populated

        // 1. Add Subjects
        val math = Subject("MATH101", "الرياضيات العامة لطلبة العلوم", "الرياضيات")
        val phys = Subject("PHYS202", "الفيزياء الكهرومغناطيسية", "الفيزياء")
        val chem = Subject("CHEM301", "الكيمياء العضوية المتقدمة", "الكيمياء")
        
        insertSubject(math)
        insertSubject(phys)
        insertSubject(chem)

        // 2. Add Teachers
        val t1Id = insertTeacher(Teacher(name = "أ.د. أحمد المحمود", email = "ahmed@sanad.edu", department = "الرياضيات والعلوم الإحصائية", teacherCode = "TCH101"))
        val t2Id = insertTeacher(Teacher(name = "أ.د. ليلى السعدي", email = "layla@sanad.edu", department = "الفيزياء التطبيقية", teacherCode = "TCH202"))

        // 3. Add Students
        val s1Id = insertStudent(Student(
            name = "وليد الهواسي",
            email = "walidhawa389@gmail.com",
            nationalId = "9876543210",
            seatNumber = "SEC-A-412",
            classRating = "السنة الثانية - قسم الرياضيات",
            invitationCode = "CONV-2026-9041",
            hallNumber = "القاعة الكبرى (ج)",
            barcodeHash = "STU:9876543210:SEC-A-412:9041"
        ))

        val s2Id = insertStudent(Student(
            name = "سارة عبد الرحمن",
            email = "sara@student.edu",
            nationalId = "1122334455",
            seatNumber = "SEC-B-108",
            classRating = "السنة الثالثة - قسم الفيزياء",
            invitationCode = "CONV-2026-1025",
            hallNumber = "قاعة ابن الهيثم (3)",
            barcodeHash = "STU:1122334455:SEC-B-108:1025"
        ))

        val s3Id = insertStudent(Student(
            name = "محمد كمال الدين",
            email = "mohammed@student.edu",
            nationalId = "5566778899",
            seatNumber = "SEC-A-033",
            classRating = "السنة الأولى - علوم الحاسوب",
            invitationCode = "CONV-2026-5532",
            hallNumber = "مدرج الفارابي (ب)",
            barcodeHash = "STU:5566778899:SEC-A-033:5532"
        ))

        // 4. Create Exam 1 (Mathematics Exam)
        val mExamId = insertExam(Exam(
            title = "الاختبار النهائي في حساب التفاضل والتكامل",
            subjectId = "MATH101",
            date = "2026-06-15",
            time = "09:00",
            durationMinutes = 120,
            totalPoints = 40.0,
            creatorId = t1Id.toInt(),
            isPublished = true,
            examRequirements = "يسمح باستخدام الآلة الحاسبة غير القابلة للبرمجة وكتابة القوانين الملحقة."
        )).toInt()

        // Questions for Exam 1
        insertQuestion(Question(
            examId = mExamId,
            type = "QCM",
            text = "ما هي المشتقة الأولى للدالة الرياضية التالية بالنسبة للمتغير x: \\[ f(x) = \\sin(x^2) + e^{-3x} \\]",
            choicesJson = "\$2x \\cos(x^2) - 3e^{-3x}\$|\$2x \\sin(x^2) + e^{-3x}\$|\$\\cos(x^2) - 3e^{-3x}\$|\$2 \\cos(x^2) + 3e^{-3x}\$",
            answerHint = "الخيار الأول: \$2x \\cos(x^2) - 3e^{-3x}\$ باستخدام قاعدة السلسلة لمشتقة الزاوية.",
            points = 5.0
        ))

        insertQuestion(Question(
            examId = mExamId,
            type = "MATH_PROBLEM",
            text = "احسب قيمة النهاية اللانهائية التالية بالتفصيل: \\[ \\lim_{x \\to \\infty} \\frac{3x^2 - 5x + 2}{7x^2 + 11x - 4} \\]",
            answerHint = "المعدل الرياضي يقترب من معامل أعلى قوة في البسط والمقام وهو: \\frac{3}{7}",
            rubricCriteriaJson = "فهم النهاية (2) | تطبيق خوارزمية القسمة (2) | دقة الحساب النهائي (1)",
            points = 5.0
        ))

        insertQuestion(Question(
            examId = mExamId,
            type = "MATH_PROBLEM",
            text = "أوجد التكامل المحدود التالي مبينًا خطوات التجزئة: \\[ I = \\int_{0}^{\\pi} x \\sin(x) \\, dx \\]",
            answerHint = "باستخدام التكامل بالتجزئة (Integration by parts): u=x => du=dx, dv=sin(x)dx => v=-cos(x). ينتج: I = \\pi",
            rubricCriteriaJson = "اختيار الأجزاء المناسبة (3) | حساب الدالة الأصلية (3) | تطبيق حدود التكامل المحدود (4)",
            points = 10.0
        ))

        insertQuestion(Question(
            examId = mExamId,
            type = "QCM",
            text = "إذا كانت المصفوفة A من رتبة \$2 \\times 2\$ حيث \\[ A = \\begin{pmatrix} a & b \\\\ c & d \\end{pmatrix} \\] فما هو القانون الصحيح لحساب محدد هذه المصفوفة \$\\det(A)\$؟",
            choicesJson = "\$ad - bc\$|\$ab - cd\$|\$ac - bd\$|\$ad + bc\$",
            answerHint = "الخيار الأول: \$ad - bc\$ ضرب عناصر القطر الرئيسي مطروحاً منه القطر الثانوي.",
            points = 5.0
        ))

        insertQuestion(Question(
            examId = mExamId,
            type = "TABLE",
            text = "الجدول التالي يحتوي على قيم المترشحين ونسبة النجاح المقدرة حسب مستوى التحضير. حدد قيمة معامل التغير المستنتج.\n\n|| مستوى التحضير \$x\$ || العلامة المتوقعة \$y\$ ||\n|| \$1\$ || \$60\$ ||\n|| \$2\$ || \$75\$ ||\n|| \$3\$ || \$90\$ ||",
            answerHint = "العلاقة خطية طرية بمعدل زيادة ثابت قدره +15 درجة لكل مستوى تحضير إضافي.",
            rubricCriteriaJson = "تحليل قراءة الجدول (2.5) | استنتاج المعادلة الخطية (2.5)",
            points = 5.0
        ))

        insertQuestion(Question(
            examId = mExamId,
            type = "OPEN",
            text = "اشرح مبرهنة القيمة المتوسطة في التفاضل والتكامل، واذكر الشرطين الأساسيين لتطبيقها على مجال مغلق $[a, b]$.",
            answerHint = "الشرطان هما: الاتصال على الفترة المغلقة وقابلية الاشتقاق على الفترة المفتوحة. تنص على وجود نقطة c يكون عندها مماس المنحنى موازيًا للمستقيم الواصل بين نقطتي النهاية.",
            rubricCriteriaJson = "ذكر شروط المبرهنة (5) | التفسير الهندسي البياني المعتمد (5)",
            points = 10.0
        ))


        // 5. Create Exam 2 (Physics Exam)
        val pExamId = insertExam(Exam(
            title = "اختبار الفيزياء الكهرومغناطيسية والنسبية",
            subjectId = "PHYS202",
            date = "2026-06-18",
            time = "11:00",
            durationMinutes = 90,
            totalPoints = 20.0,
            creatorId = t2Id.toInt(),
            isPublished = true,
            examRequirements = "يوفر القسم ورقة الثوابت الكهرومغناطيسية وثابت بلانك."
        )).toInt()

        // Questions for Exam 2
        insertQuestion(Question(
            examId = pExamId,
            type = "SCIENTIFIC",
            text = "باستخدام علاقة أينشتاين الشهيرة للتكافؤ بين الكتلة والطاقة: \\[ E = m c^2 \\] حيث \$c \\approx 3 \\times 10^8 \\text{ m/s}\$， احسب الطاقة الكلية الناتجة عن فناء كتلة مقدارها \$m = 1 \\text{ g}\$ بالـ Joules.",
            answerHint = "m = 10^{-3} kg. E = 10^{-3} * (3*10^8)^2 = 9 * 10^{13} Joules.",
            rubricCriteriaJson = "التحويل للوحدات الدولية (2) | التطبيق العددي للقانون (2) | الصحة الحسابية والوحدات (1)",
            points = 5.0
        ))

        insertQuestion(Question(
            examId = pExamId,
            type = "QCM",
            text = "ما هي العلاقة التي تحدد شدة المجال المغناطيسي \$B\$ داخل ملف لولبي طوله \$L\$ وعدد لفاته \$N\$ ويمر به تيار \$I\$؟",
            choicesJson = "\$B = \\mu_0 \\frac{N I}{L}\$|\$B = \\mu_0 N I L\$|\$B = 2\\pi \\mu_0 \\frac{I}{L}\$|\$B = \\mu_0 \\frac{I}{2\\pi R}\$",
            answerHint = "الخيار الأول: \$B = \\mu_0 \\frac{N I}{L}\$ حسب قانون أمبير للملف اللولبي.",
            points = 5.0
        ))

        insertQuestion(Question(
            examId = pExamId,
            type = "OPEN",
            text = "اشرح المفهوم الفيزيائي لظاهرة ظاهرة مفعول كومتن (Compton Effect)، وكيف دعمت هذه الظاهرة الطبيعة الجسيمية للموجات الكهرومغناطيسية؟",
            answerHint = "تصادم فوتون بأشعة X مع إلكترون وتغير طول موجة الفوتون المشتت يؤكد انتقال كمية الحركة كجسيم مادي.",
            rubricCriteriaJson = "شرح الآلية الكهروميوية للتصادم (5) | مناقشة الطبيعة الجسيمية والنتائج (5)",
            points = 10.0
        ))

        // 6. Prepopulate some Notifications
        insertNotification(Notification(
            title = "تم إصدار الاستدعاء الإلكتروني للاختبارات",
            message = "أصبح الاستدعاء الإلكتروني الخاص بامتحانات الفصل الثاني متاحاً للتحميل بنسخة PDF مع كود QR. يرجى المراجعة والطباعة فوراً.",
            type = "CONVOCATION"
        ))

        insertNotification(Notification(
            title = "موعد اختبار الرياضيات المتقدمة",
            message = "تم تأكيد موعد اختبار مقرر الرياضيات MATH101 بتاريخ 2026-06-15 الساعة التاسعة صباحاً بقاعة الامتحان المحددة في استدعائك.",
            type = "ALERT"
        ))

        // 7. Add dummy submission for Sarah to show grading
        insertSubmission(Submission(
            examId = pExamId,
            studentId = s2Id.toInt(),
            answersJson = "إجابة السؤال الاول: E = (10^-3) * (3*10^8)^2 = 9*10^13 Joules.\nإجابة السؤال الثاني (QCM): الخيار الأول B = mu_0 * N * I / L.\nإجابة السؤال الثالث: مفعول كومتن يثبت تصادم الفوتونات بالألكترونات مما يسبب تشتت وزيادة طول موجة الفوتون.",
            score = 17.5,
            commentsJson = "إجابة ممتازة ومكتوبة بخطوات رياضية كاملة وتطبيق موفق للوحدات.",
            status = "GRADED",
            submissionDate = "2026-05-20 14:32",
            rubricGradesJson = "التحويل والدقة: 4.5/5 | الخيارات الصحيحة: 5/5 | التفسير الفيزيائي والعمق العلمي: 8/10"
        ))
    }
}
