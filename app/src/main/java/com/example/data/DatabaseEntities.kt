package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val nationalId: String,       // الرقم الوطني أو الجامعي
    val seatNumber: String,       // رقم المقعد/الجلوس
    val classRating: String,      // القسم أو المستوى الدراسي
    val invitationCode: String,   // رمز الاستدعاء الإلكتروني
    val hallNumber: String,       // رقم قاعة الامتحان
    val barcodeHash: String       // الرمز السري للـ QR Code
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val department: String,       // التخصص أو القسم
    val teacherCode: String       // رمز المعلم
)

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey val id: String,   // رمز المادة (e.g. "MATH101")
    val name: String,
    val department: String
)

@Entity(tableName = "exams")
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,            // عنوان الاختبار
    val subjectId: String,        // رمز المادة المرتبطة
    val date: String,             // تاريخ الاختبار (YYYY-MM-DD)
    val time: String,             // توقيت الاختبار (HH:MM)
    val durationMinutes: Int,     // مدة الامتحان بالدقائق
    val totalPoints: Double,      // النقاط الإجمالية
    val creatorId: Int,           // معرف الأستاذ المنشئ
    val isPublished: Boolean = false,
    val examRequirements: String = "" // متطلبات خاصة بالامتحان
)

@Entity(tableName = "questions")
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val type: String,             // QCM, OPEN, MATH_PROBLEM, SCIENTIFIC, TABLE
    val text: String,             // نص السؤال (يدعم LaTeX)
    val choicesJson: String = "", // قائمة الخيارات للـ QCM مفصولة بـ | أو JSON
    val answerHint: String = "",  // الإجابة النموذجية أو تلميح الحل
    val rubricCriteriaJson: String = "", // معايير التقييم كـ JSON أو نص تفصيلي
    val points: Double            // علامة السؤال
)

@Entity(tableName = "submissions")
data class Submission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: Int,
    val studentId: Int,
    val answersJson: String,      // إجابات الطالب
    val score: Double = 0.0,      // العلامة الحاصل عليها
    val commentsJson: String = "",// ملاحظات المصحح
    val status: String,           // SENT (تم التسليم), GRADED (تم التصحيح)
    val submissionDate: String,
    val rubricGradesJson: String = "" // درجات الـ Rubric بالتفصيل
)

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String,             // ALERT, CONVOCATION, EXAM_GRADE, SYSTEM
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
