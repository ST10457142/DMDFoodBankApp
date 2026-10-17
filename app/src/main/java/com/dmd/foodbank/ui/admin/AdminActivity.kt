package com.dmd.foodbank.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dmd.foodbank.R
import com.dmd.foodbank.data.model.*
import com.dmd.foodbank.databinding.*
import com.dmd.foodbank.ui.AdminViewModel
import com.dmd.foodbank.ui.auth.LoginActivity
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private val vm: AdminViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!vm.session.isAdmin()) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        vm.loadStats()
        setupNavigation()
        showDashboard()

        intent.getStringExtra("success_msg")?.let {
            Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG)
                .setBackgroundTint(getColor(android.R.color.holo_green_dark)).show()
        }

        binding.btnLogout.setOnClickListener {
            vm.session.clearSession()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun setupNavigation() {
        binding.navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> showDashboard()
                R.id.nav_donors -> showDonors()
                R.id.nav_donations -> showDonationsAdmin()
                R.id.nav_beneficiaries -> showBeneficiaries()
                R.id.nav_volunteers -> showVolunteers()
                R.id.nav_messages -> showMessages()
                R.id.nav_users -> showUsers()
            }
            binding.drawerLayout.closeDrawers()
            true
        }

        // Unread badge
        vm.unreadMessages.observe(this) { count ->
            updateBadge(R.id.nav_messages, count)
        }

        // Pending badges
        vm.pendingBeneficiaries.observe(this) { count ->
            updateBadge(R.id.nav_beneficiaries, count)
        }
    }

    private fun updateBadge(itemId: Int, count: Int) {
        val menuItem = binding.navView.menu.findItem(itemId)
        val actionView = menuItem.actionView
        val tvBadge = actionView?.findViewById<TextView>(R.id.tvBadge)
        if (tvBadge != null) {
            if (count > 0) {
                tvBadge.text = if (count > 99) "99+" else count.toString()
                tvBadge.visibility = View.VISIBLE
            } else {
                tvBadge.visibility = View.GONE
            }
        }
    }

    private fun loadFragment(view: View) {
        binding.contentContainer.removeAllViews()
        binding.contentContainer.addView(view)
    }

    private fun showDashboard() {
        supportActionBar?.title = "Dashboard"
        val v = layoutInflater.inflate(R.layout.fragment_admin_dashboard, binding.contentContainer, false)
        loadFragment(v)

        vm.totalRaised.observe(this) { v.findViewById<TextView>(R.id.tvTotalRaised).text = "R %.2f".format(it) }
        vm.monthRaised.observe(this) { v.findViewById<TextView>(R.id.tvMonthRaised).text = "R %.2f".format(it) }
        vm.totalDonations.observe(this) { v.findViewById<TextView>(R.id.tvTotalDonations).text = "$it" }
        vm.unreadMessages.observe(this) { v.findViewById<TextView>(R.id.tvUnreadMsgs).text = "$it" }
        vm.topDonors.observe(this) { donors ->
            val rv = v.findViewById<RecyclerView>(R.id.rvTopDonors)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = TopDonorsAdapter(donors)
        }
        vm.recentDonations.observe(this) { donations ->
            val rv = v.findViewById<RecyclerView>(R.id.rvRecentDonations)
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = AdminDonationsAdapter(donations)
        }
    }

    private fun showDonors() {
        supportActionBar?.title = "Donors"
        val b = AdminListBinding.inflate(layoutInflater)
        loadFragment(b.root)

        val adapter = AdminUsersAdapter(emptyList()) { user, action ->
            when (action) {
                "toggle" -> { vm.toggleUserActive(user); vm.loadStats() }
            }
        }
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter

        b.etSearch.addTextChangedListener { text ->
            val q = text.toString()
            val liveData = if (q.isBlank()) vm.allDonors else vm.searchDonors(q)
            liveData.observe(this) { adapter.update(it) }
        }

        vm.allDonors.observe(this) { adapter.update(it) }
        vm.success.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Snackbar.make(b.root, msg, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(android.R.color.holo_green_dark)).show()
                vm.clearMessages()
            }
        }
    }

    private fun showDonationsAdmin() {
        supportActionBar?.title = "Donations"
        val b = AdminListBinding.inflate(layoutInflater)
        loadFragment(b.root)
        b.etSearch.visibility = View.GONE

        val adapter = AdminDonationsAdapter(emptyList())
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter
        vm.allDonations.observe(this) { adapter.update(it) }
    }

    private fun showBeneficiaries() {
        supportActionBar?.title = "Beneficiaries"
        val b = AdminListBinding.inflate(layoutInflater)
        loadFragment(b.root)
        b.etSearch.visibility = View.GONE

        val adapter = BeneficiaryAdapter { id, status -> vm.updateBeneficiaryStatus(id, status) }
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter
        vm.allBeneficiaries.observe(this) { adapter.update(it) }
    }

    private fun showVolunteers() {
        supportActionBar?.title = "Volunteers"
        val b = AdminListBinding.inflate(layoutInflater)
        loadFragment(b.root)
        b.etSearch.visibility = View.GONE

        val adapter = VolunteerAdapter { id, status -> vm.updateVolunteerStatus(id, status) }
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter
        vm.allVolunteers.observe(this) { adapter.update(it) }
    }

    private fun showMessages() {
        supportActionBar?.title = "Messages"
        val b = AdminListBinding.inflate(layoutInflater)
        loadFragment(b.root)
        b.etSearch.visibility = View.GONE

        val adapter = MessagesAdapter(
            onRead = { vm.markMessageRead(it.id) },
            onDelete = { msg ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Delete message")
                    .setMessage("Delete message from ${msg.name}?")
                    .setPositiveButton("Delete") { _, _ -> vm.deleteMessage(msg) }
                    .setNegativeButton("Cancel", null).show()
            },
            onView = { msg ->
                vm.markMessageRead(msg.id)
                MaterialAlertDialogBuilder(this)
                    .setTitle("From: ${msg.name}")
                    .setMessage("Subject: ${msg.subject}\n\n${msg.message}")
                    .setPositiveButton("Close", null).show()
            }
        )
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter
        vm.allMessages.observe(this) { adapter.update(it) }
    }

    private fun showUsers() {
        supportActionBar?.title = "All Users"
        val b = AdminListBinding.inflate(layoutInflater)
        loadFragment(b.root)
        b.etSearch.visibility = View.GONE

        val myId = vm.session.getSession()?.userId
        val adapter = AdminUsersAdapter(emptyList()) { user, action ->
            when (action) {
                "toggle" -> vm.toggleUserActive(user)
                "make_admin" -> vm.setUserRole(user, "admin")
                "make_donor" -> vm.setUserRole(user, "donor")
            }
        }
        b.recyclerView.layoutManager = LinearLayoutManager(this)
        b.recyclerView.adapter = adapter
        vm.allUsers.observe(this) { adapter.update(it) }
        vm.success.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Snackbar.make(b.root, msg, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(getColor(android.R.color.holo_green_dark)).show()
                vm.clearMessages()
            }
        }
    }
}

// ─── Admin Adapters ───────────────────────────────────────────────────────────

class TopDonorsAdapter(private var items: List<User>) : RecyclerView.Adapter<TopDonorsAdapter.VH>() {
    inner class VH(val b: ItemAdminDonorRowBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemAdminDonorRowBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = items[pos]
        h.b.tvRank.text = "#${pos + 1}"
        h.b.tvName.text = u.fullName
        h.b.tvAmount.text = u.formattedTotal()
        h.b.tvExtra.text = "${u.donationCount} donations"
    }
}

class AdminDonationsAdapter(private var items: List<Donation>) : RecyclerView.Adapter<AdminDonationsAdapter.VH>() {
    inner class VH(val b: ItemAdminDonationRowBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemAdminDonationRowBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val d = items[pos]
        h.b.tvType.text = if (d.donationType == "money") "💰 Money" else "🥕 Food"
        h.b.tvAmount.text = d.formattedAmount()
        h.b.tvDate.text = d.formattedDate()
        h.b.tvFreq.text = d.frequency
    }
    fun update(newItems: List<Donation>) { items = newItems; notifyDataSetChanged() }
}

class AdminUsersAdapter(private var items: List<User>, private val onAction: (User, String) -> Unit) :
    RecyclerView.Adapter<AdminUsersAdapter.VH>() {
    inner class VH(val b: ItemAdminUserRowBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemAdminUserRowBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = items[pos]
        h.b.tvName.text = u.fullName
        h.b.tvEmail.text = u.email
        h.b.tvRole.text = u.role
        h.b.tvTotal.text = u.formattedTotal()
        h.b.tvStatus.text = if (u.isActive) "Active" else "Inactive"
        h.b.btnToggle.text = if (u.isActive) "Deactivate" else "Activate"
        h.b.btnToggle.setOnClickListener { onAction(u, "toggle") }
        h.b.btnRole.text = if (u.role == "admin") "-Admin" else "+Admin"
        h.b.btnRole.setOnClickListener { onAction(u, if (u.role == "admin") "make_donor" else "make_admin") }
    }
    fun update(newItems: List<User>) { items = newItems; notifyDataSetChanged() }
}

class BeneficiaryAdapter(private val onStatusChange: (Int, String) -> Unit) :
    RecyclerView.Adapter<BeneficiaryAdapter.VH>() {
    private var items = listOf<Beneficiary>()
    inner class VH(val b: ItemBeneficiaryRowBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemBeneficiaryRowBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val bene = items[pos]
        h.b.tvName.text = bene.fullName
        h.b.tvPhone.text = bene.phone
        h.b.tvHousehold.text = bene.householdSize
        h.b.tvHub.text = bene.nearestHub
        h.b.tvStatus.text = bene.status.replaceFirstChar { it.uppercase() }
        h.b.btnApprove.visibility = if (bene.status == "pending") View.VISIBLE else View.GONE
        h.b.btnReject.visibility = if (bene.status == "pending") View.VISIBLE else View.GONE
        h.b.btnApprove.setOnClickListener { onStatusChange(bene.id, "approved") }
        h.b.btnReject.setOnClickListener { onStatusChange(bene.id, "rejected") }
    }
    fun update(newItems: List<Beneficiary>) { items = newItems; notifyDataSetChanged() }
}

class VolunteerAdapter(private val onStatusChange: (Int, String) -> Unit) :
    RecyclerView.Adapter<VolunteerAdapter.VH>() {
    private var items = listOf<Volunteer>()
    inner class VH(val b: ItemVolunteerRowBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemVolunteerRowBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val vol = items[pos]
        h.b.tvName.text = vol.fullName
        h.b.tvEmail.text = vol.email
        h.b.tvSkills.text = vol.skills
        h.b.tvAvailability.text = vol.availability
        h.b.tvStatus.text = vol.status.replaceFirstChar { it.uppercase() }
        h.b.btnApprove.visibility = if (vol.status == "pending") View.VISIBLE else View.GONE
        h.b.btnReject.visibility = if (vol.status == "pending") View.VISIBLE else View.GONE
        h.b.btnApprove.setOnClickListener { onStatusChange(vol.id, "approved") }
        h.b.btnReject.setOnClickListener { onStatusChange(vol.id, "rejected") }
    }
    fun update(newItems: List<Volunteer>) { items = newItems; notifyDataSetChanged() }
}

class MessagesAdapter(
    private val onRead: (ContactMessage) -> Unit,
    private val onDelete: (ContactMessage) -> Unit,
    private val onView: (ContactMessage) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.VH>() {
    private var items = listOf<ContactMessage>()
    inner class VH(val b: ItemMessageRowBinding) : RecyclerView.ViewHolder(b.root)
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemMessageRowBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val msg = items[pos]
        h.b.tvSender.text = msg.name
        h.b.tvSubject.text = msg.subject.ifBlank { "(No subject)" }
        h.b.tvPreview.text = msg.message.take(60)
        h.b.tvUnreadDot.visibility = if (!msg.isRead) View.VISIBLE else View.INVISIBLE
        h.b.btnView.setOnClickListener { onView(msg) }
        h.b.btnDelete.setOnClickListener { onDelete(msg) }
        h.b.root.alpha = if (msg.isRead) 0.7f else 1f
    }
    fun update(newItems: List<ContactMessage>) { items = newItems; notifyDataSetChanged() }
}
