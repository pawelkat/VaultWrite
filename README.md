# VaultWrite

Tiny Android **library** (no APK) for writing to the local Obsidian vault mirror on the phone
(`/sdcard/DriveSyncFiles/Vault`, synced by DriveSync). Extracted from
[ToDaily](https://github.com/pawelkat/ToDaily)'s `DailyNoteAppender` / `VaultPaths` / `StorageAccess`.

Used by [GymMonth](https://github.com/pawelkat/GymMonth) (visits → Daily `## Workouts`, settings note).
SwimWeek will follow. Each app stays a separate APK.

**No INTERNET, no Drive OAuth, no secrets.** Only plain file IO under the vault root, gated on
All files access (`MANAGE_EXTERNAL_STORAGE` on Android 11+, `WRITE_EXTERNAL_STORAGE` legacy).

## API (`com.vaultwrite`)

| Type | What |
| --- | --- |
| `DailyNoteAppender` | Pure markdown: append a bullet under any heading (`Workouts` → `## Workouts`), before the next heading of same/higher level; create the heading if missing; create a Daily stub (Templates/Daily shape: Private / UBS / Notes / Links / Photos + target heading) if the file is missing; skip duplicates by `dedupeKey` (any line containing it) or identical bullet. |
| `VaultPaths` | `DEFAULT_VAULT_ROOT`, `dailyRelativePath(date)` → `Daily/YYYY-MM-DD.md`, `noteRelativePath(title)` → `Notes/<title>.md`, `resolve()` (rejects `..` / absolute paths). |
| `VaultFileWriter(root)` | JVM-only IO: `appendToDaily(date, heading, bullet, dedupeKey)`, `writeNote(relativePath, markdown)` (create/overwrite, skips identical), `readNote()`. Never throws. |
| `StorageAccess(context)` | `hasWriteAccess()`, `manageAllFilesIntent()` (ToDaily UX). |
| `VaultWriter(context) { root }` | Façade = storage check + `VaultFileWriter`. Returns `VaultWriteResult.NeedStoragePermission` when access is missing. |
| `VaultCatalog` | User-editable JSON catalogs: `loadOrSeed(root, relPath, vaultAvailable, bundled, accept)` — vault file missing → seed once from the APK copy; exists → always use it (never overwritten); no access / unreadable / invalid → bundled fallback. |
| `KeyValueNote` | Render/parse a settings note: YAML frontmatter + `\| Setting \| Value \|` table. |
| `VaultWriteResult` | `Saved(relativePath, absolutePath, createdFile, duplicate, bullet)` / `NeedStoragePermission` / `Failed`. |

All IO is blocking — call from `Dispatchers.IO`.

## Consuming it (Termux-friendly sibling include)

Check out VaultWrite **next to** the app (`/root/VaultWrite` beside `/root/gymmonth` on the S24,
`/workspace/VaultWrite` beside `/workspace/GymMonth` on the box):

```bash
git clone https://github.com/pawelkat/VaultWrite.git ../VaultWrite
```

App `settings.gradle.kts`:

```kotlin
val vaultWriteDir = providers.gradleProperty("vaultWriteDir").orNull?.let(::File)
    ?: File(rootDir, "../VaultWrite")
include(":vaultwrite")
project(":vaultwrite").projectDir = File(vaultWriteDir, "vaultwrite")
```

App root `build.gradle.kts` must declare the Android **library** plugin (same AGP as the app):

```kotlin
plugins {
    id("com.android.library") version "8.9.1" apply false   // or alias(libs.plugins.android.library)
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
}
```

App module: `implementation(project(":vaultwrite"))`.

Why not `includeBuild`: the module has no plugin versions of its own, so it reuses the host's
AGP/Kotlin and the host's repositories — one Gradle daemon, no dependency substitution, works the
same on Termux and the box. Override the location with `-PvaultWriteDir=/path/to/VaultWrite`.

The host manifest must declare:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" tools:ignore="ScopedStorage" />
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" tools:ignore="ScopedStorage" />
<application android:requestLegacyExternalStorage="true" ...>
```

## Build / test standalone

```bash
./gradlew :vaultwrite:testDebugUnitTest
```

aarch64 hosts (Termux / box): pass `-Pandroid.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/36.1.0/aapt2`.

## License

Proprietary / TBD (same as GymMonth).
