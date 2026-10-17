package com.dmd.foodbank.data.repository

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dmd.foodbank.data.model.*
import kotlinx.coroutines.flow.Flow

// ─── DAOs ────────────────────────────────────────────────────────────────────

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email AND isActive = 1 LIMIT 1")
    suspend fun findByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun findById(id: Int): User?

    @Query("SELECT * FROM users ORDER BY totalDonated DESC")
    fun getAllUsers(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role != 'admin' ORDER BY totalDonated DESC")
    fun getAllDonors(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE role != 'admin' AND (fullName LIKE :q OR email LIKE :q)")
    fun searchDonors(q: String): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Update
    suspend fun update(user: User)

    @Query("UPDATE users SET totalDonated = totalDonated + :amount, donationCount = donationCount + 1 WHERE id = :userId")
    suspend fun incrementDonation(userId: Int, amount: Double)

    @Query("UPDATE users SET lastLogin = :time WHERE id = :id")
    suspend fun updateLastLogin(id: Int, time: Long)

    @Query("UPDATE users SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean)

    @Query("UPDATE users SET role = :role WHERE id = :id")
    suspend fun setRole(id: Int, role: String)

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun emailExists(email: String): Int

    @Query("SELECT totalDonated FROM users WHERE id = :id")
    suspend fun getTotalDonated(id: Int): Double

    @Query("SELECT * FROM users ORDER BY totalDonated DESC LIMIT 5")
    suspend fun getTopDonors(): List<User>
}

@Dao
interface DonationDao {
    @Insert
    suspend fun insert(donation: Donation): Long

    @Query("SELECT * FROM donations WHERE userId = :userId ORDER BY createdAt DESC")
    fun getDonationsForUser(userId: Int): Flow<List<Donation>>

    @Query("SELECT * FROM donations ORDER BY createdAt DESC LIMIT 200")
    fun getAllDonations(): Flow<List<Donation>>

    @Query("SELECT * FROM donations WHERE donationType = :type ORDER BY createdAt DESC")
    fun getDonationsByType(type: String): Flow<List<Donation>>

    @Query("SELECT * FROM donations ORDER BY createdAt DESC LIMIT 8")
    suspend fun getRecent(): List<Donation>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM donations WHERE donationType = 'money'")
    suspend fun getTotalMoneyRaised(): Double

    @Query("""
        SELECT COALESCE(SUM(amount), 0) FROM donations 
        WHERE donationType = 'money'
        AND strftime('%Y-%m', datetime(createdAt/1000, 'unixepoch')) = strftime('%Y-%m', 'now')
    """)
    suspend fun getThisMonthTotal(): Double

    @Query("SELECT COUNT(*) FROM donations")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM donations WHERE donationType = 'food'")
    suspend fun getFoodDonationCount(): Int
}

@Dao
interface ContactMessageDao {
    @Insert
    suspend fun insert(message: ContactMessage): Long

    @Query("SELECT * FROM contact_messages ORDER BY createdAt DESC LIMIT 100")
    fun getAll(): Flow<List<ContactMessage>>

    @Query("SELECT COUNT(*) FROM contact_messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("UPDATE contact_messages SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Int)

    @Delete
    suspend fun delete(message: ContactMessage)

    @Query("SELECT * FROM contact_messages WHERE id = :id")
    suspend fun findById(id: Int): ContactMessage?
}

@Dao
interface BeneficiaryDao {
    @Insert
    suspend fun insert(beneficiary: Beneficiary): Long

    @Query("SELECT * FROM beneficiaries ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Beneficiary>>

    @Query("SELECT COUNT(*) FROM beneficiaries WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("UPDATE beneficiaries SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("SELECT COUNT(*) FROM beneficiaries")
    suspend fun getTotal(): Int
}

@Dao
interface VolunteerDao {
    @Insert
    suspend fun insert(volunteer: Volunteer): Long

    @Query("SELECT * FROM volunteers ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Volunteer>>

    @Query("SELECT COUNT(*) FROM volunteers WHERE status = 'pending'")
    fun getPendingCount(): Flow<Int>

    @Query("UPDATE volunteers SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)
}

// ─── Database ─────────────────────────────────────────────────────────────────

@Database(
    entities = [User::class, Donation::class, ContactMessage::class, Beneficiary::class, Volunteer::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun donationDao(): DonationDao
    abstract fun contactMessageDao(): ContactMessageDao
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun volunteerDao(): VolunteerDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "dmd_foodbank.db")
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed admin user (password: Admin@1234 – stored as plain for demo; use BCrypt in prod)
                            db.execSQL("""
                                INSERT INTO users (fullName, email, password, role, donorType, phone, totalDonated, donationCount, createdAt, isActive)
                                VALUES ('Admin User', 'admin@dmdfoodbank.org', 'Admin@1234', 'admin', 'Individual', '0000000000', 0.0, 0, ${System.currentTimeMillis()}, 1)
                            """.trimIndent())
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
