package com.dmd.foodbank.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dmd.foodbank.R
import com.dmd.foodbank.databinding.ActivityRegisterBinding
import com.dmd.foodbank.ui.AuthViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val vm: AuthViewModel by viewModels()
    private var currentTab = 0  // 0=Donor, 1=Volunteer, 2=Beneficiary

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupSpinners()
        setupObservers()

        binding.btnRegister.setOnClickListener { submit() }
        binding.tvSignIn.setOnClickListener { finish() }
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("❤️ Donor"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🙋 Volunteer"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("🤲 Beneficiary"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                updateFormVisibility()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        updateFormVisibility()
    }

    private fun updateFormVisibility() {
        binding.donorFields.visibility = if (currentTab == 0) View.VISIBLE else View.GONE
        binding.volunteerFields.visibility = if (currentTab == 1) View.VISIBLE else View.GONE
        binding.beneficiaryFields.visibility = if (currentTab == 2) View.VISIBLE else View.GONE

        binding.btnRegister.text = when (currentTab) {
            0 -> "Create Account"
            1 -> "Register as Volunteer"
            else -> "Apply for Assistance"
        }
    }

    private fun setupSpinners() {
        val donorTypes = arrayOf("Individual", "Corporate / Business", "Government / Parastatal", "Religious / Community Org")
        binding.spinnerDonorType.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, donorTypes)

        val skills = arrayOf("Sorting & Packing", "Food Distribution", "Administration", "Driving / Logistics", "Fundraising", "Social Media")
        binding.spinnerSkills.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, skills)

        val avail = arrayOf("Weekdays only", "Weekends only", "Flexible")
        binding.spinnerAvailability.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, avail)

        val households = arrayOf("1–2 people", "3–4 people", "5–6 people", "7+ people")
        binding.spinnerHousehold.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, households)

        val hubs = arrayOf("Soweto Community Hub", "Alexandra Distribution Centre", "Tembisa Food Bank", "Roodepoort Centre")
        binding.spinnerHub.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, hubs)
    }

    private fun submit() {
        when (currentTab) {
            0 -> vm.registerDonor(
                binding.etFullName.text.toString(),
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString(),
                binding.etConfirmPassword.text.toString(),
                binding.spinnerDonorType.selectedItem.toString()
            )
            1 -> vm.registerVolunteer(
                binding.etVolName.text.toString(),
                binding.etVolEmail.text.toString(),
                binding.spinnerSkills.selectedItem.toString(),
                binding.spinnerAvailability.selectedItem.toString()
            )
            2 -> vm.registerBeneficiary(
                binding.etBeneName.text.toString(),
                binding.etBenePhone.text.toString(),
                binding.spinnerHousehold.selectedItem.toString(),
                binding.spinnerHub.selectedItem.toString()
            )
        }
    }

    private fun setupObservers() {
        vm.loading.observe(this) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }

        vm.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                vm.clearMessages()
            }
        }

        vm.registerSuccess.observe(this) { ok ->
            if (ok == true) {
                val msg = vm.success.value ?: "Registration complete!"
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    putExtra("success_msg", msg)
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                })
                finish()
            }
        }
    }
}
