package com.dmd.foodbank.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dmd.foodbank.R
import com.dmd.foodbank.data.model.Donation
import com.dmd.foodbank.data.model.Milestone
import com.dmd.foodbank.databinding.FragmentProfileBinding
import com.dmd.foodbank.ui.AuthViewModel
import com.dmd.foodbank.ui.ProfileViewModel
import com.dmd.foodbank.ui.auth.LoginActivity
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val vm: ProfileViewModel by viewModels()
    private val authVm: AuthViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentProfileBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!vm.session.isLoggedIn()) {
            showNotLoggedIn(); return
        }

        vm.load()
        setupTabs()
        setupSpinners()
        setupObservers()

        binding.btnSaveProfile.setOnClickListener {
            vm.updateProfile(
                binding.etName.text.toString(),
                binding.etPhone.text.toString(),
                binding.spinnerDonorType.selectedItem.toString()
            )
        }

        binding.btnChangePassword.setOnClickListener {
            vm.changePassword(
                binding.etCurrentPw.text.toString(),
                binding.etNewPw.text.toString(),
                binding.etConfirmPw.text.toString()
            )
        }

        binding.btnLogout.setOnClickListener {
            authVm.logout()
            startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    private fun showNotLoggedIn() {
        binding.layoutNotLoggedIn.visibility = View.VISIBLE
        binding.layoutProfile.visibility = View.GONE
        binding.btnLoginPrompt.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun setupTabs() {
        listOf("📊 Overview", "📋 History", "🏆 Milestones", "⚙️ Settings", "🔐 Security")
            .forEach { binding.tabLayout.addTab(binding.tabLayout.newTab().setText(it)) }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { showTab(tab.position) }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        showTab(0)
    }

    private fun showTab(pos: Int) {
        val panels = listOf(binding.panelOverview, binding.panelHistory, binding.panelMilestones, binding.panelSettings, binding.panelSecurity)
        panels.forEachIndexed { i, v -> v.visibility = if (i == pos) View.VISIBLE else View.GONE }
    }

    private fun setupSpinners() {
        val types = arrayOf("Individual", "Corporate / Business", "Government / Parastatal", "Religious / Community Org")
        binding.spinnerDonorType.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
    }

    private fun setupObservers() {
        binding.layoutNotLoggedIn.visibility = View.GONE
        binding.layoutProfile.visibility = View.VISIBLE

        vm.user.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe
            binding.tvInitials.text = user.initials()
            binding.tvUserName.text = user.fullName
            binding.tvUserEmail.text = user.email
            binding.tvBadge.text = user.donorType
            binding.tvTotalDonated.text = user.formattedTotal()
            binding.tvDonationCount.text = "${user.donationCount} donation${if (user.donationCount != 1) "s" else ""}"
            binding.tvMealsEstimate.text = "${(user.totalDonated / 10).toInt()}+ meals funded"
            // Pre-fill settings
            binding.etName.setText(user.fullName)
            binding.etPhone.setText(user.phone)

            // Milestones
            val ms = vm.getMilestones()
            binding.rvMilestones.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMilestones.adapter = MilestoneAdapter(ms)
        }

        vm.donations.observe(viewLifecycleOwner) { donations ->
            binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
            binding.rvHistory.adapter = DonationHistoryAdapter(donations)

            if (donations.isEmpty()) {
                binding.tvNoDonations.visibility = View.VISIBLE
            } else {
                binding.tvNoDonations.visibility = View.GONE
                // Recent 3 for overview
                binding.rvRecentOverview.layoutManager = LinearLayoutManager(requireContext())
                binding.rvRecentOverview.adapter = DonationHistoryAdapter(donations.take(3))
            }
        }

        vm.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) { Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show(); vm.clearMessages() }
        }

        vm.success.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
                    .setBackgroundTint(requireContext().getColor(android.R.color.holo_green_dark)).show()
                vm.clearMessages()
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── Adapters ─────────────────────────────────────────────────────────────────

class DonationHistoryAdapter(private val items: List<Donation>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<DonationHistoryAdapter.VH>() {
    inner class VH(val b: com.dmd.foodbank.databinding.ItemDonationBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(com.dmd.foodbank.databinding.ItemDonationBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val d = items[pos]
        h.b.tvDonationType.text = if (d.donationType == "money") "💰 Monetary Donation" else "🥕 Food Donation"
        h.b.tvDonationDate.text = d.formattedDate()
        h.b.tvDonationAmount.text = d.formattedAmount()
        h.b.tvDonationFreq.text = d.frequency.replaceFirstChar { it.uppercase() }
    }
}

class MilestoneAdapter(private val items: List<Pair<Milestone, Boolean>>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<MilestoneAdapter.VH>() {
    inner class VH(val b: com.dmd.foodbank.databinding.ItemMilestoneBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(com.dmd.foodbank.databinding.ItemMilestoneBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun getItemCount() = items.size
    override fun onBindViewHolder(h: VH, pos: Int) {
        val (ms, reached) = items[pos]
        h.b.tvMilestoneIcon.text = ms.icon
        h.b.tvMilestoneLabel.text = "${ms.label} ${if (reached) "✅" else "🔒"}"
        h.b.tvMilestoneDesc.text = ms.description
        h.b.cardMilestone.alpha = if (reached) 1f else 0.55f
    }
}
