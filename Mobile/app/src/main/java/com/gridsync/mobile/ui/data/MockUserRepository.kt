package com.gridsync.mobile.ui.data

data class MockProsumerProfile(
    val id: String,
    val nic: String,
    val fullName: String,
    val email: String,
    val phone: String,
    val address: String,
    val status: String,
    val role: String = "Prosumer",
    val deactivationRequested: Boolean = false,
)

object MockUserRepository {
    private var profile = MockProsumerProfile(
        id = "u-prosumer-1",
        nic = "199512345678",
        fullName = "Ayesha Perera",
        email = "ayesha.perera@example.com",
        phone = "0771234567",
        address = "12 Flower Road, Colombo 07",
        status = "Active",
        deactivationRequested = false,
    )

    fun current(): MockProsumerProfile = profile

    fun updateContact(fullName: String, phone: String, address: String): MockProsumerProfile {
        profile = profile.copy(
            fullName = fullName.trim(),
            phone = phone.trim(),
            address = address.trim(),
        )
        return profile
    }

    fun requestDeactivation(): MockProsumerProfile {
        if (profile.status != "Active") {
            throw IllegalStateException("Only Active accounts can request deactivation.")
        }
        if (profile.deactivationRequested) {
            throw IllegalStateException("Deactivation already requested.")
        }
        profile = profile.copy(deactivationRequested = true)
        return profile
    }

    /** UI stub — always succeeds for demo. */
    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank()) {
            throw IllegalArgumentException("Current password is required.")
        }
        if (newPassword.length < 8 ||
            !newPassword.any { it.isLetter() } ||
            !newPassword.any { it.isDigit() }
        ) {
            throw IllegalArgumentException("New password needs 8+ chars with a letter and a number.")
        }
    }
}
