// Standalone build for the VaultWrite library. Host apps (GymMonth, later SwimWeek)
// include the `:vaultwrite` module directly from a sibling checkout instead — see README.
plugins {
    id("com.android.library") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
}
