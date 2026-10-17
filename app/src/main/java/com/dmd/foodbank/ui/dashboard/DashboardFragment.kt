package com.dmd.foodbank.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.dmd.foodbank.R
import com.dmd.foodbank.data.model.Donation
import com.dmd.foodbank.databinding.FragmentDashboardBinding
import com.dmd.foodbank.ui.HomeViewModel
import com.dmd.foodbank.ui.auth.LoginActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentDashboardBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!vm.session.isLoggedIn()) {
            showNotLoggedIn(); return
        }

        vm.load()
        setupObservers()

        binding.btnDonate.setOnClickListener { findNavController().navigate(R.id.action_dashboard_to_donate) }
        binding.btnProfile.setOnClickListener { findNavController().navigate(R.id.action_dashboard_to_profile) }
    }

    private fun showNotLoggedIn() {
        binding.layoutNotLoggedIn.visibility = View.VISIBLE
        binding.layoutDashboard.visibility = View.GONE
        binding.btnLoginPrompt.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun setupObservers() {
        binding.layoutNotLoggedIn.visibility = View.GONE
        binding.layoutDashboard.visibility = View.VISIBLE

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date())

        vm.user.observe(viewLifecycleOwner) { user ->
            if (user == null) return@observe
            binding.tvGreeting.text = "Welcome back, ${user.fullName} 👋"
            binding.tvTotalDonated.text = "R %.2f".format(user.totalDonated)
            binding.tvMyTotal.text = "R %.2f".format(user.totalDonated)
            binding.tvMyCount.text = "${user.donationCount}"
            binding.tvMealsEst.text = "${(user.totalDonated / 10).toInt()}"
            binding.tvInitials.text = user.initials()

            // Milestone progress
            val next = vm.nextMilestone(user.totalDonated)
            if (next != null) {
                binding.tvMilestone.text = "${next.icon} ${next.label}"
                val remaining = next.threshold - user.totalDonated
                binding.tvMilestoneRemaining.text = "R %.2f to go!".format(remaining)
                val pct = ((user.totalDonated / next.threshold) * 100).toInt().coerceIn(0, 100)
                binding.progressMilestone.progress = pct
            } else {
                binding.tvMilestone.text = "🏆 Champion Donor!"
                binding.tvMilestoneRemaining.text = "All milestones reached!"
                binding.progressMilestone.progress = 100
            }
        }

        vm.monthTotal.observe(viewLifecycleOwner) { total ->
            val pct = vm.getMonthProgress()
            binding.tvMonthTotal.text = "R %.2f / R 50,000".format(total)
            binding.progressMonth.progress = pct
            binding.tvMonthPct.text = "$pct% reached"
        }

        vm.recentDonations.observe(viewLifecycleOwner) { donations ->
            binding.rvRecentDonations.layoutManager = LinearLayoutManager(requireContext())
            binding.rvRecentDonations.adapter = RecentDonationsAdapter(donations)
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ─── Adapter ──────────────────────────────────────────────────────────────────

class RecentDonationsAdapter(private val items: List<Donation>) :
    androidx.recyclerview.widget.RecyclerView.Adapter<RecentDonationsAdapter.VH>() {

    inner class VH(val binding: com.dmd.foodbank.databinding.ItemDonationBinding) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(com.dmd.foodbank.databinding.ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        holder.binding.tvDonationType.text = if (d.donationType == "money") "💰 Monetary Donation" else "🥕 Food Donation"
        holder.binding.tvDonationDate.text = d.formattedDate()
        holder.binding.tvDonationAmount.text = d.formattedAmount()
        holder.binding.tvDonationFreq.text = d.frequency.replaceFirstChar { it.uppercase() }
    }
}
