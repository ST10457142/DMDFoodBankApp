package com.dmd.foodbank.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.dmd.foodbank.data.model.*
import kotlinx.coroutines.flow.Flow

// ─── Session Manager ──────────────────────────────────────────────────────────

class SessionManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("dmd_session", Context.MODE_PRIVATE)

    fun saveSession(user: User) {
        prefs.edit()
            .putInt("user_id", user.id)
            .putString("user_role", user.role)
            .putString("user_name", user.fullName)
            .apply()
    }

    fun getSession(): Session? {
        val id = prefs.getInt("user_id", -1)
        if (id == -1) return null
        return Session(
            userId = id,
            role = prefs.getString("user_role", "donor") ?: "donor",
            name = prefs.getString("user_name", "") ?: ""
        )
    }

    fun clearSession() = prefs.edit().clear().apply()

    fun isLoggedIn() = prefs.getInt("user_id", -1) != -1
    fun isAdmin() = prefs.getString("user_role", "") == "admin"
}

// ─── Main Repository ──────────────────────────────────────────────────────────

class AppRepository(private val db: AppDatabase) {

    // ── Auth ─────────────────────────────────────────────────────────────────

    suspend fun login(email: String, password: String): Result<User> {
        val user = db.userDao().findByEmail(email)
            ?: return Result.failure(Exception("No account found with that email."))
        if (user.password != password)
            return Result.failure(Exception("Incorrect password. Please try again."))
        db.userDao().updateLastLogin(user.id, System.currentTimeMillis())
        return Result.success(user)
    }

    suspend fun register(
        fullName: String, email: String, password: String,
        confirmPassword: String, donorType: String
    ): Result<User> {
        if (fullName.isBlank()) return Result.failure(Exception("Full name is required."))
        if (!email.contains("@")) return Result.failure(Exception("Valid email address required."))
        if (password.length < 8) return Result.failure(Exception("Password must be at least 8 characters."))
        if (!password.any { it.isUpperCase() }) return Result.failure(Exception("Password must contain an uppercase letter."))
        if (!password.any { it.isDigit() }) return Result.failure(Exception("Password must contain a number."))
        if (password != confirmPassword) return Result.failure(Exception("Passwords do not match."))
        if (db.userDao().emailExists(email) > 0)
            return Result.failure(Exception("An account with this email already exists."))

        val user = User(fullName = fullName, email = email, password = password, donorType = donorType)
        val id = db.userDao().insert(user)
        return Result.success(user.copy(id = id.toInt()))
    }

    suspend fun registerVolunteer(name: String, email: String, skills: String, availability: String): Result<Unit> {
        if (name.isBlank() || email.isBlank() || skills.isBlank() || availability.isBlank())
            return Result.failure(Exception("All fields are required."))
        db.volunteerDao().insert(Volunteer(fullName = name, email = email, skills = skills, availability = availability))
        return Result.success(Unit)
    }

    suspend fun registerBeneficiary(name: String, phone: String, household: String, hub: String): Result<Unit> {
        if (name.isBlank() || phone.isBlank() || household.isBlank() || hub.isBlank())
            return Result.failure(Exception("All fields are required."))
        db.beneficiaryDao().insert(Beneficiary(fullName = name, phone = phone, householdSize = household, nearestHub = hub))
        return Result.success(Unit)
    }

    // ── User ─────────────────────────────────────────────────────────────────

    suspend fun getUser(id: Int): User? = db.userDao().findById(id)

    suspend fun updateUser(user: User) = db.userDao().update(user)

    suspend fun changePassword(userId: Int, currentPw: String, newPw: String, confirmPw: String): Result<Unit> {
        val user = db.userDao().findById(userId) ?: return Result.failure(Exception("User not found."))
        if (user.password != currentPw) return Result.failure(Exception("Current password is incorrect."))
        if (newPw.length < 8) return Result.failure(Exception("New password must be at least 8 characters."))
        if (newPw != confirmPw) return Result.failure(Exception("New passwords do not match."))
        db.userDao().update(user.copy(password = newPw))
        return Result.success(Unit)
    }

    fun getAllUsers(): Flow<List<User>> = db.userDao().getAllUsers()
    fun getAllDonors(): Flow<List<User>> = db.userDao().getAllDonors()
    fun searchDonors(q: String): Flow<List<User>> = db.userDao().searchDonors("%$q%")
    suspend fun setUserActive(id: Int, active: Boolean) = db.userDao().setActive(id, active)
    suspend fun setUserRole(id: Int, role: String) = db.userDao().setRole(id, role)
    suspend fun getTopDonors(): List<User> = db.userDao().getTopDonors()

    // ── Donations ─────────────────────────────────────────────────────────────

    suspend fun donate(userId: Int?, amount: Double, frequency: String): Result<Donation> {
        if (amount < 10.0) return Result.failure(Exception("Minimum donation is R 10.00."))
        val donation = Donation(userId = userId, amount = amount, frequency = frequency)
        db.donationDao().insert(donation)
        if (userId != null) db.userDao().incrementDonation(userId, amount)
        return Result.success(donation)
    }

    suspend fun donateFoodItem(userId: Int?, description: String, hub: String): Result<Unit> {
        if (description.isBlank()) return Result.failure(Exception("Please describe the food items."))
        if (hub.isBlank()) return Result.failure(Exception("Please select a drop-off hub."))
        db.donationDao().insert(
            Donation(userId = userId, amount = 0.0, donationType = "food",
                foodDescription = "$description | Hub: $hub")
        )
        return Result.success(Unit)
    }

    fun getUserDonations(userId: Int): Flow<List<Donation>> = db.donationDao().getDonationsForUser(userId)
    fun getAllDonations(): Flow<List<Donation>> = db.donationDao().getAllDonations()
    fun getDonationsByType(type: String): Flow<List<Donation>> = db.donationDao().getDonationsByType(type)
    suspend fun getRecentDonations(): List<Donation> = db.donationDao().getRecent()
    suspend fun getTotalMoneyRaised(): Double = db.donationDao().getTotalMoneyRaised()
    suspend fun getThisMonthTotal(): Double = db.donationDao().getThisMonthTotal()
    suspend fun getTotalDonationCount(): Int = db.donationDao().getTotalCount()

    // ── Contact Messages ──────────────────────────────────────────────────────

    suspend fun sendMessage(name: String, email: String, subject: String, message: String): Result<Unit> {
        if (name.isBlank() || !email.contains("@") || message.isBlank())
            return Result.failure(Exception("Please fill in all required fields."))
        db.contactMessageDao().insert(ContactMessage(name = name, email = email, subject = subject, message = message))
        return Result.success(Unit)
    }

    fun getAllMessages(): Flow<List<ContactMessage>> = db.contactMessageDao().getAll()
    fun getUnreadCount(): Flow<Int> = db.contactMessageDao().getUnreadCount()
    suspend fun markMessageRead(id: Int) = db.contactMessageDao().markRead(id)
    suspend fun deleteMessage(msg: ContactMessage) = db.contactMessageDao().delete(msg)
    suspend fun getMessageById(id: Int): ContactMessage? = db.contactMessageDao().findById(id)

    // ── Beneficiaries ─────────────────────────────────────────────────────────

    fun getAllBeneficiaries(): Flow<List<Beneficiary>> = db.beneficiaryDao().getAll()
    fun getPendingBeneficiaryCount(): Flow<Int> = db.beneficiaryDao().getPendingCount()
    suspend fun updateBeneficiaryStatus(id: Int, status: String) = db.beneficiaryDao().updateStatus(id, status)
    suspend fun getTotalBeneficiaries(): Int = db.beneficiaryDao().getTotal()

    // ── Volunteers ────────────────────────────────────────────────────────────

    fun getAllVolunteers(): Flow<List<Volunteer>> = db.volunteerDao().getAll()
    fun getPendingVolunteerCount(): Flow<Int> = db.volunteerDao().getPendingCount()
    suspend fun updateVolunteerStatus(id: Int, status: String) = db.volunteerDao().updateStatus(id, status)

    // ── Hubs (static data matching PHP locations page) ────────────────────────

    fun getHubs(): List<Hub> = listOf(
        Hub("Soweto Community Hub", "123 Ubuntu St, Soweto, 1804", "Mon–Sat: 08:00–16:00", "011 111 2222", StockStatus.FULL, -26.2678, 27.8585),
        Hub("Alexandra Distribution Centre", "45 Pan Africa Ave, Alexandra, 2090", "Mon–Fri: 09:00–15:00", "011 222 3333", StockStatus.LOW, -26.1052, 28.0918),
        Hub("Tembisa Food Bank", "78 Ivory Park Rd, Tembisa, 1632", "Mon–Sat: 08:00–17:00", "011 333 4444", StockStatus.FULL, -25.9964, 28.2268),
        Hub("Roodepoort Centre", "12 West Rand Dr, Roodepoort, 1725", "Mon–Fri: 08:00–15:00", "011 444 5555", StockStatus.FULL, -26.1625, 27.8725),
        Hub("Diepsloot Hub", "99 Cluster A, Diepsloot, 2189", "Tue & Thu: 09:00–14:00", "011 555 6666", StockStatus.CRITICAL, -25.9345, 28.0114),
        Hub("Orange Farm Depot", "34 Main Rd, Orange Farm, 1805", "Mon, Wed, Fri: 09:00–13:00", "011 666 7777", StockStatus.LOW, -26.4650, 27.8650),
        Hub("Katlehong Centre", "56 Natalspruit Rd, Katlehong, 1431", "Mon–Sat: 08:00–16:00", "011 777 8888", StockStatus.FULL, -26.3350, 28.1500)
    )
}
