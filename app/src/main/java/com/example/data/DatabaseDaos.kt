package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // === Students ===
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT COUNT(*) FROM students")
    fun getStudentsCount(): Flow<Int>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Int): Student?

    @Query("SELECT * FROM students WHERE email = :email LIMIT 1")
    suspend fun getStudentByEmail(email: String): Student?

    @Query("SELECT * FROM students WHERE nationalId = :nationalId LIMIT 1")
    suspend fun getStudentByNationalId(nationalId: String): Student?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student): Long

    @Delete
    suspend fun deleteStudent(student: Student)


    // === Teachers ===
    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT COUNT(*) FROM teachers")
    fun getTeachersCount(): Flow<Int>

    @Query("SELECT * FROM teachers WHERE id = :id LIMIT 1")
    suspend fun getTeacherById(id: Int): Teacher?

    @Query("SELECT * FROM teachers WHERE email = :email LIMIT 1")
    suspend fun getTeacherByEmail(email: String): Teacher?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher): Long

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)


    // === Subjects ===
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<Subject>>

    @Query("SELECT COUNT(*) FROM subjects")
    fun getSubjectsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject)


    // === Exams ===
    @Query("SELECT * FROM exams ORDER BY date DESC, time DESC")
    fun getAllExams(): Flow<List<Exam>>

    @Query("SELECT COUNT(*) FROM exams")
    fun getExamsCount(): Flow<Int>

    @Query("SELECT * FROM exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: Int): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Query("DELETE FROM exams WHERE id = :examId")
    suspend fun deleteExamById(examId: Int)


    // === Questions ===
    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY id ASC")
    fun getQuestionsByExam(examId: Int): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE examId = :examId ORDER BY id ASC")
    suspend fun getQuestionsByExamSync(examId: Int): List<Question>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: Question): Long

    @Query("DELETE FROM questions WHERE examId = :examId")
    suspend fun deleteQuestionsForExam(examId: Int)


    // === Submissions ===
    @Query("SELECT * FROM submissions ORDER BY submissionDate DESC")
    fun getAllSubmissions(): Flow<List<Submission>>

    @Query("SELECT * FROM submissions WHERE examId = :examId")
    fun getSubmissionsForExam(examId: Int): Flow<List<Submission>>

    @Query("SELECT * FROM submissions WHERE studentId = :studentId")
    fun getSubmissionsForStudent(studentId: Int): Flow<List<Submission>>

    @Query("SELECT * FROM submissions WHERE examId = :examId AND studentId = :studentId LIMIT 1")
    suspend fun getSubmission(examId: Int, studentId: Int): Submission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubmission(submission: Submission): Long


    // === Notifications ===
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<Notification>>

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadNotificationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification): Long

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Int)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()
}
