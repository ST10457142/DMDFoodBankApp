package com.dmd.foodbank.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.dmd.foodbank.databinding.ActivityLoginBinding
import com.dmd.foodbank.ui.AuthViewModel
import com.dmd.foodbank.ui.MainActivity
import com.dmd.foodbank.ui.admin.AdminActivity
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val vm: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Show flash message from register
        intent.getStringExtra("success_msg")?.let { showSnack(it, isError = false) }

        binding.btnLogin.setOnClickListener {
            vm.login(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            )
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.tvBackHome.setOnClickListener { finish() }

        // Observe
        vm.loading.observe(this) { binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE }

        vm.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) { showSnack(msg, isError = true); vm.clearMessages() }
        }

        vm.loginSuccess.observe(this) { user ->
            if (user != null) {
                val dest = if (user.role == "admin") AdminActivity::class.java else MainActivity::class.java
                startActivity(Intent(this, dest).apply {
                    putExtra("success_msg", "Welcome back, ${user.fullName}!")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
    }

    private fun showSnack(msg: String, isError: Boolean) {
        val snack = Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG)
        if (!isError) snack.setBackgroundTint(getColor(android.R.color.holo_green_dark))
        snack.show()
    }
}
