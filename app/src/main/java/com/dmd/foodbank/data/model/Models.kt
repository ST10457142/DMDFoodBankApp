package com.dmd.foodbank.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

// ─── User ───────────────────────────────────────────────────────────────────

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val password: String,           // BCrypt hash
    val role: String = "donor",     // donor | admin
    val donorType: String = "Individual",
    val phone: String = "",
    val totalDonated: Double = 0.0,
    val donationCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long? = null,
    val isActive: Boolean = true
) {
    fun initials(): String =
        fullName.trim().split(" ")
            .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

    fun formattedTotal(): String = "R %.2f".format(totalDonated)
}

// ─── Donation ────────────────────────────────────────────────────────────────

@Entity(tableName = "donations")
data class Donation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int?,
    val amount: Double,
    val donationType: String = "money",   // money | food
    val foodDescription: String? = null,
    val frequency: String = "once",        // once | monthly
    val status: String = "completed",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun formattedAmount(): String =
        if (donationType == "money") "R %.2f".format(amount) else "In-kind"

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(createdAt))
    }
}

// ─── Contact Message ─────────────────────────────────────────────────────────

@Entity(tableName = "contact_messages")
data class ContactMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val subject: String = "",
    val message: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

// ─── Beneficiary ─────────────────────────────────────────────────────────────

@Entity(tableName = "beneficiaries")
data class Beneficiary(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val householdSize: String,
    val nearestHub: String,
    val status: String = "pending",    // pending | approved | rejected
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Volunteer ───────────────────────────────────────────────────────────────

@Entity(tableName = "volunteers")
data class Volunteer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val email: String,
    val skills: String,
    val availability: String,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Hub ─────────────────────────────────────────────────────────────────────

data class Hub(
    val name: String,
    val address: String,
    val hours: String,
    val phone: String,
    val stockStatus: StockStatus,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

enum class StockStatus(val label: String, val emoji: String) {
    FULL("Fully stocked", "🟢"),
    LOW("Low stock", "🟡"),
    CRITICAL("Critical – needs help", "🔴")
}

// ─── Milestone ───────────────────────────────────────────────────────────────

data class Milestone(
    val icon: String,
    val label: String,
    val description: String,
    val threshold: Double,
    val countThreshold: Int = 0
) {
    fun isReached(totalDonated: Double, donationCount: Int): Boolean =
        totalDonated >= threshold && donationCount >= countThreshold
}

object Milestones {
    val all = listOf(
        Milestone("🌱", "First Donation", "Made your very first donation", 0.01, 1),
        Milestone("🌟", "R 500 Milestone", "Total giving exceeded R 500", 500.0),
        Milestone("🔥", "Regular Donor", "Made 5 or more donations", 0.0, 5),
        Milestone("💫", "R 1,000 Club", "Total giving exceeded R 1,000", 1000.0),
        Milestone("🏅", "R 5,000 Benefactor", "Total giving exceeded R 5,000", 5000.0),
        Milestone("🏆", "R 10,000 Champion", "Total giving exceeded R 10,000", 10000.0)
    )
}

// ─── Session ─────────────────────────────────────────────────────────────────

data class Session(
    val userId: Int,
    val role: String,
    val name: String
) {
    val isAdmin get() = role == "admin"
}
