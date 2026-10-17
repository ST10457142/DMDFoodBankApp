package com.dmd.foodbank.ui.donate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dmd.foodbank.R
import com.dmd.foodbank.databinding.FragmentDonateBinding
import com.dmd.foodbank.ui.DonateViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class DonateFragment : Fragment() {

    private var _binding: FragmentDonateBinding? = null
    private val binding get() = _binding!!
    private val vm: DonateViewModel by viewModels()

    private var selectedAmount = 50.0
    private var currentTab = 0  // 0=Money, 1=Food

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentDonateBinding.inflate(inflater, container, false).also { _binding = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.loadUserStats()
        setupTabs()
        setupAmountChips()
        setupSpinners()
        setupObservers()

        binding.btnDonate.setOnClickListener { submitDonation() }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("💰 Donate Money"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🥕 Donate Food"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                binding.moneyFields.visibility = if (currentTab == 0) View.VISIBLE else View.GONE
                binding.foodFields.visibility = if (currentTab == 1) View.VISIBLE else View.GONE
                binding.btnDonate.text = if (currentTab == 0) "🔒 Donate Securely" else "Register Food Donation"
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAmountChips() {
        val amounts = mapOf(50.0 to "5 meals", 100.0 to "10 meals", 250.0 to "25 meals", 500.0 to "50 meals", 1000.0 to "100 meals")
        amounts.forEach { (amount, label) ->
            val chip = Chip(requireContext()).apply {
                text = "R ${amount.toInt()}\n$label"
                isCheckable = true
                isChecked = amount == 50.0
                setChipBackgroundColorResource(R.color.chip_state)
                setTextColor(ContextCompat.getColorStateList(requireContext(), R.color.chip_text_state))
                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedAmount = amount
                        binding.tilCustomAmount.visibility = View.GONE
                    }
                }
            }
            binding.chipGroupAmount.addView(chip)
        }
        // Custom chip
        val customChip = Chip(requireContext()).apply {
            text = "Custom"
            isCheckable = true
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    binding.tilCustomAmount.visibility = View.VISIBLE
                    binding.etCustomAmount.requestFocus()
                }
            }
        }
        binding.chipGroupAmount.addView(customChip)
    }

    private fun setupSpinners() {
        val freqs = arrayOf("One-time donation", "Monthly recurring")
        binding.spinnerFrequency.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, freqs)

        val hubs = vm.hubs().map { it.name }.toTypedArray()
        binding.spinnerFoodHub.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, hubs)
    }

    private fun submitDonation() {
        if (currentTab == 0) {
            val customText = binding.etCustomAmount.text.toString()
            val amount = if (binding.tilCustomAmount.visibility == View.VISIBLE && customText.isNotEmpty())
                customText.toDoubleOrNull() ?: 0.0 else selectedAmount
            val freq = if (binding.spinnerFrequency.selectedItemPosition == 0) "once" else "monthly"
            vm.donateMoney(amount, freq)
        } else {
            vm.donateFood(
                binding.etFoodDescription.text.toString(),
                binding.spinnerFoodHub.selectedItem.toString()
            )
        }
    }

    private fun setupObservers() {
        vm.loading.observe(viewLifecycleOwner) {
            binding.btnDonate.isEnabled = !it
            binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }

        vm.error.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) { Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show(); vm.clearMessages() }
        }

        vm.success.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrEmpty()) {
                showSuccessDialog(msg)
                vm.clearMessages()
            }
        }

        vm.userTotal.observe(viewLifecycleOwner) { total ->
            binding.tvUserTotal.text = "Your total: R %.2f".format(total)
            binding.cardUserStats.visibility = if (vm.session.isLoggedIn()) View.VISIBLE else View.GONE
        }

        vm.userCount.observe(viewLifecycleOwner) { count ->
            binding.tvUserCount.text = "$count donation${if (count != 1) "s" else ""} made"
        }
    }

    private fun showSuccessDialog(msg: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("🎉 Thank you!")
            .setMessage(msg)
            .setPositiveButton("View My Profile") { _, _ ->
                parentFragmentManager.setFragmentResult("navigate", Bundle().apply { putString("dest", "profile") })
            }
            .setNegativeButton("Done", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
