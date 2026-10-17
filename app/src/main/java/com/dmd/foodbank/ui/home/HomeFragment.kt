package com.dmd.foodbank.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.dmd.foodbank.R
import com.dmd.foodbank.databinding.FragmentHomeBinding
import com.dmd.foodbank.ui.HomeViewModel
import com.dmd.foodbank.ui.auth.LoginActivity

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentHomeBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.load()
        setupObservers()
        setupClicks()
    }

    private fun setupClicks() {
        binding.btnDonateHero.setOnClickListener { findNavController().navigate(R.id.action_home_to_donate) }
        binding.btnAboutHero.setOnClickListener { findNavController().navigate(R.id.action_home_to_about) }
        binding.btnJoin.setOnClickListener {
            if (vm.session.isLoggedIn()) findNavController().navigate(R.id.action_home_to_dashboard)
            else startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
        binding.cardDonateMoney.setOnClickListener { findNavController().navigate(R.id.action_home_to_donate) }
        binding.cardDonateFood.setOnClickListener { findNavController().navigate(R.id.action_home_to_donate) }
        binding.cardVolunteer.setOnClickListener {
            startActivity(Intent(requireContext(), com.dmd.foodbank.ui.auth.RegisterActivity::class.java))
        }
    }

    private fun setupObservers() {
        vm.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvWelcomeBanner.text = "Welcome back, ${user.fullName}!"
                binding.tvWelcomeBanner.visibility = View.VISIBLE
                binding.btnJoin.text = "My Dashboard"
            } else {
                binding.tvWelcomeBanner.visibility = View.GONE
                binding.btnJoin.text = "Join Us"
            }
        }

        vm.monthTotal.observe(viewLifecycleOwner) { total ->
            binding.tvMonthTotal.text = "R %.2f raised this month".format(total)
            val pct = vm.getMonthProgress()
            binding.progressMonth.progress = pct
            binding.tvMonthPct.text = "$pct% of target reached"
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
