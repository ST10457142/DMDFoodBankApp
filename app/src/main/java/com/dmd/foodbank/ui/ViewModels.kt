package com.dmd.foodbank.ui

import android.app.Application
import androidx.lifecycle.*
import com.dmd.foodbank.data.model.*
import com.dmd.foodbank.data.repository.AppDatabase
import com.dmd.foodbank.data.repository.AppRepository
import com.dmd.foodbank.data.repository.SessionManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ─── Base ─────────────────────────────────────────────────────────────────────

open class BaseViewModel(application: Application) : AndroidViewModel(application) {
    protected val db = AppDatabase.getInstance(application)
    protected val repo = AppRepository(db)
    val session = SessionManager(application)

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    protected fun setError(msg: String) { _error.value = msg }
    protected fun setSuccess(msg: String) { _success.value = msg }
    protected fun setLoading(b: Boolean) { _loading.value = b }
    fun clearMessages() { _error.value = null; _success.value = null }
}

// ─── Auth ─────────────────────────────────────────────────────────────────────

class AuthViewModel(application: Application) : BaseViewModel(application) {

    private val _loginSuccess = MutableLiveData<User?>()
    val loginSuccess: LiveData<User?> = _loginSuccess

    private val _registerSuccess = MutableLiveData<Boolean>()
    val registerSuccess: LiveData<Boolean> = _registerSuccess

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { setError("Email and password are required."); return }
        setLoading(true)
        viewModelScope.launch {
            val result = repo.login(email.trim(), password)
            setLoading(false)
            result.fold(
                onSuccess = { user ->
                    session.saveSession(user)
                    _loginSuccess.value = user
                },
                onFailure = { setError(it.message ?: "Login failed.") }
            )
        }
    }

    fun registerDonor(name: String, email: String, password: String, confirm: String, donorType: String) {
        setLoading(true)
        viewModelScope.launch {
            val result = repo.register(name.trim(), email.trim(), password, confirm, donorType)
            setLoading(false)
            result.fold(
                onSuccess = { setSuccess("Account created! Please sign in."); _registerSuccess.value = true },
                onFailure = { setError(it.message ?: "Registration failed.") }
            )
        }
    }

    fun registerVolunteer(name: String, email: String, skills: String, availability: String) {
        setLoading(true)
        viewModelScope.launch {
            val result = repo.registerVolunteer(name.trim(), email.trim(), skills, availability)
            setLoading(false)
            result.fold(
                onSuccess = { setSuccess("Volunteer application received!"); _registerSuccess.value = true },
                onFailure = { setError(it.message ?: "Failed.") }
            )
        }
    }

    fun registerBeneficiary(name: String, phone: String, household: String, hub: String) {
        setLoading(true)
        viewModelScope.launch {
            val result = repo.registerBeneficiary(name.trim(), phone.trim(), household, hub)
            setLoading(false)
            result.fold(
                onSuccess = { setSuccess("Application received! We'll contact you within 48 hours."); _registerSuccess.value = true },
                onFailure = { setError(it.message ?: "Failed.") }
            )
        }
    }

    fun logout() { session.clearSession() }
}

// ─── Home / Dashboard ─────────────────────────────────────────────────────────

class HomeViewModel(application: Application) : BaseViewModel(application) {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _recentDonations = MutableLiveData<List<Donation>>()
    val recentDonations: LiveData<List<Donation>> = _recentDonations

    private val _monthTotal = MutableLiveData(0.0)
    val monthTotal: LiveData<Double> = _monthTotal

    fun load() {
        val sess = session.getSession() ?: return
        viewModelScope.launch {
            _user.value = repo.getUser(sess.userId)
            _recentDonations.value = repo.getRecentDonations()
                .filter { it.userId == sess.userId }
                .take(5)
            _monthTotal.value = repo.getThisMonthTotal()
        }
    }

    fun getMonthProgress(): Int {
        val goal = 50_000.0
        return ((_monthTotal.value ?: 0.0) / goal * 100).toInt().coerceIn(0, 100)
    }

    fun nextMilestone(totalDonated: Double): Milestone? =
        Milestones.all.firstOrNull { totalDonated < it.threshold && it.threshold > 0 }
}

// ─── Donate ───────────────────────────────────────────────────────────────────

class DonateViewModel(application: Application) : BaseViewModel(application) {

    private val _donationComplete = MutableLiveData<Donation?>()
    val donationComplete: LiveData<Donation?> = _donationComplete

    private val _userTotal = MutableLiveData<Double>()
    val userTotal: LiveData<Double> = _userTotal

    private val _userCount = MutableLiveData<Int>()
    val userCount: LiveData<Int> = _userCount

    fun loadUserStats() {
        val sess = session.getSession() ?: return
        viewModelScope.launch {
            val u = repo.getUser(sess.userId)
            _userTotal.value = u?.totalDonated ?: 0.0
            _userCount.value = u?.donationCount ?: 0
        }
    }

    fun donateMoney(amount: Double, frequency: String) {
        setLoading(true)
        viewModelScope.launch {
            val userId = session.getSession()?.userId
            val result = repo.donate(userId, amount, frequency)
            setLoading(false)
            result.fold(
                onSuccess = { d ->
                    _donationComplete.value = d
                    loadUserStats()
                    setSuccess("Thank you for donating R %.2f! Your generosity makes a real difference.".format(amount))
                },
                onFailure = { setError(it.message ?: "Donation failed.") }
            )
        }
    }

    fun donateFood(description: String, hub: String) {
        setLoading(true)
        viewModelScope.launch {
            val userId = session.getSession()?.userId
            val result = repo.donateFoodItem(userId, description, hub)
            setLoading(false)
            result.fold(
                onSuccess = { setSuccess("Food donation registered! Please drop off at $hub.") },
                onFailure = { setError(it.message ?: "Failed.") }
            )
        }
    }

    fun hubs() = repo.getHubs()
}

// ─── Profile ──────────────────────────────────────────────────────────────────

class ProfileViewModel(application: Application) : BaseViewModel(application) {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    val donations = MutableLiveData<List<Donation>>()

    fun load() {
        val sess = session.getSession() ?: return
        viewModelScope.launch {
            _user.value = repo.getUser(sess.userId)
            repo.getUserDonations(sess.userId).collect { donations.value = it }
        }
    }

    fun updateProfile(name: String, phone: String, donorType: String) {
        val u = _user.value ?: return
        if (name.isBlank()) { setError("Full name is required."); return }
        viewModelScope.launch {
            repo.updateUser(u.copy(fullName = name, phone = phone, donorType = donorType))
            session.saveSession(u.copy(fullName = name))
            _user.value = repo.getUser(u.id)
            setSuccess("Profile updated successfully!")
        }
    }

    fun changePassword(current: String, new: String, confirm: String) {
        val u = _user.value ?: return
        viewModelScope.launch {
            val result = repo.changePassword(u.id, current, new, confirm)
            result.fold(
                onSuccess = { setSuccess("Password changed successfully!") },
                onFailure = { setError(it.message ?: "Failed.") }
            )
        }
    }

    fun getMilestones(): List<Pair<Milestone, Boolean>> {
        val u = _user.value ?: return emptyList()
        return Milestones.all.map { it to it.isReached(u.totalDonated, u.donationCount) }
    }
}

// ─── Contact ──────────────────────────────────────────────────────────────────

class ContactViewModel(application: Application) : BaseViewModel(application) {
    fun sendMessage(name: String, email: String, subject: String, message: String) {
        setLoading(true)
        viewModelScope.launch {
            val result = repo.sendMessage(name.trim(), email.trim(), subject.trim(), message.trim())
            setLoading(false)
            result.fold(
                onSuccess = { setSuccess("Message sent! We'll reply within 2 business days.") },
                onFailure = { setError(it.message ?: "Failed to send.") }
            )
        }
    }
}

// ─── Admin ────────────────────────────────────────────────────────────────────

class AdminViewModel(application: Application) : BaseViewModel(application) {

    // Summary stats
    private val _totalRaised = MutableLiveData(0.0)
    val totalRaised: LiveData<Double> = _totalRaised
    private val _monthRaised = MutableLiveData(0.0)
    val monthRaised: LiveData<Double> = _monthRaised
    private val _totalDonations = MutableLiveData(0)
    val totalDonations: LiveData<Int> = _totalDonations
    private val _topDonors = MutableLiveData<List<User>>()
    val topDonors: LiveData<List<User>> = _topDonors
    private val _recentDonations = MutableLiveData<List<Donation>>()
    val recentDonations: LiveData<List<Donation>> = _recentDonations
    private val _totalBeneficiaries = MutableLiveData(0)
    val totalBeneficiaries: LiveData<Int> = _totalBeneficiaries

    val allDonors = repo.getAllDonors().asLiveData()
    val allDonations = repo.getAllDonations().asLiveData()
    val allBeneficiaries = repo.getAllBeneficiaries().asLiveData()
    val allVolunteers = repo.getAllVolunteers().asLiveData()
    val allMessages = repo.getAllMessages().asLiveData()
    val allUsers = repo.getAllUsers().asLiveData()
    val unreadMessages = repo.getUnreadCount().asLiveData()
    val pendingBeneficiaries = repo.getPendingBeneficiaryCount().asLiveData()
    val pendingVolunteers = repo.getPendingVolunteerCount().asLiveData()

    fun loadStats() {
        viewModelScope.launch {
            _totalRaised.value = repo.getTotalMoneyRaised()
            _monthRaised.value = repo.getThisMonthTotal()
            _totalDonations.value = repo.getTotalDonationCount()
            _topDonors.value = repo.getTopDonors()
            _recentDonations.value = repo.getRecentDonations()
            _totalBeneficiaries.value = repo.getTotalBeneficiaries()
        }
    }

    fun toggleUserActive(user: User) {
        viewModelScope.launch {
            repo.setUserActive(user.id, !user.isActive)
            setSuccess("User status updated.")
        }
    }

    fun setUserRole(user: User, role: String) {
        viewModelScope.launch {
            repo.setUserRole(user.id, role)
            setSuccess("Role updated to $role.")
        }
    }

    fun updateBeneficiaryStatus(id: Int, status: String) {
        viewModelScope.launch { repo.updateBeneficiaryStatus(id, status) }
    }

    fun updateVolunteerStatus(id: Int, status: String) {
        viewModelScope.launch { repo.updateVolunteerStatus(id, status) }
    }

    fun markMessageRead(id: Int) {
        viewModelScope.launch { repo.markMessageRead(id) }
    }

    fun deleteMessage(msg: com.dmd.foodbank.data.model.ContactMessage) {
        viewModelScope.launch { repo.deleteMessage(msg) }
    }

    fun searchDonors(q: String) = repo.searchDonors(q).asLiveData()
}
