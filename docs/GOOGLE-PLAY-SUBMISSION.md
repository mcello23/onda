# Google Play Submission Guide (Internal Testing & Android Auto)

This runbook covers uploading **Onda** (`io.github.mcello23.onda`) to **Google Play Console — Internal Testing**, enabling native **Android Auto** without developer-mode *Unknown Sources*.

---

## 1. Build the Release App Bundle (`.aab`)

Locally (with `keystore.properties` present):

```bash
export JAVA_HOME=/home/mcosta/.local/jdk/jdk-17.0.21.1+2
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew testDebugUnitTest bundleRelease
```

Outputs:
- **Play Upload Bundle:** `app/build/outputs/bundle/release/app-release.aab`
- **R8 Deobfuscation Mapping:** `app/build/outputs/mapping/release/mapping.txt`

CI (`.github/workflows/build.yml`) also builds and uploads `onda-aab` and `onda-r8-mapping` on every push to `main` and `v*` tag.

---

## 2. Play Console Initial Setup

1. **Create App**
   - App name: `Onda`
   - Default language: Portuguese (Brazil) or English
   - App type: **App** · Pricing: **Free**
   - Confirm Developer Program Policies & US export laws.
2. **Developer Identity & Package Registration**
   - Ensure Play Console identity verification is complete and register `io.github.mcello23.onda` under Android Developer Verification.
   - Complete the **EEA/UK Trader vs. Non-Trader declaration** (*Account details → About you*) — required by **1 Feb 2027** (or at creation for new accounts).
3. **Play App Signing**
   - Enroll in **Play App Signing** when creating the first release.
   - *Important if replacing your sideloaded APK:* If Google generates a new app signing key, uninstall your sideloaded Onda APK once (export a JSON backup in **Settings → Local Backup** first!), then install from Play Store Internal Testing. Alternatively, upload your existing release keystore during Play App Signing setup to keep upgrade compatibility.

---

## 3. Required Play Console App Content Declarations

Fill out these items under **Monitor and improve → Policy and programs → App content** (even for Internal Testing, completing them upfront avoids blocks if you opt into Android Auto or promote later):

- **Privacy Policy URL:**
  `https://github.com/mcello23/onda/blob/main/docs/privacy-policy.md`
  *(Or enable GitHub Pages on `/docs` and use `https://mcello23.github.io/onda/privacy-policy.html`.)*
- **App Access:** All functionality is available without special access (no login or demo account needed).
- **Ads:** No, the app does not contain ads.
- **Content Rating (IARC):** Complete honestly — answer **Yes** to third-party/live unmoderated broadcast audio where asked, and keep target audience **13+ / General** (do **not** target children under 13 / Families policy).
- **Target Audience & Content:** 13+ (not designed for children).
- **Data Safety Form:**
  - *Does your app collect or share any of the required user data types?* → **No** (no developer collection, no analytics, no ads, no accounts; ephemeral direct user-initiated media/catalog requests only).
  - *Data encrypted in transit?* Note that API calls use HTTPS while some third-party public radio streams use HTTP (disclosed in the privacy policy).
- **Foreground Service Permissions (`mediaPlayback`):**
  - Core use case: Continuous live internet radio and podcast playback while the app is backgrounded, screen is locked, or connected to Android Auto.
  - Attach a 15–30s screen recording showing: opening Onda → tapping play → locking/backgrounding → media playback notification & controls continuing smoothly.

---

## 4. Enable Android Auto & Upload to Internal Testing

1. **Opt into Android Auto:**
   - Go to **Release → Setup → Advanced settings → Form factors** → add **Android Auto**.
   - Note: Enabling Android Auto triggers Android Auto quality review when publishing to Closed/Open/Production tracks; for **Internal Testing**, invited testers can install immediately via the Play Store opt-in link and test directly in Android Auto.
2. **Upload Release:**
   - Go to **Testing → Internal testing → Create new release**.
   - Upload `app-release.aab` and (optionally) `mapping.txt` for crash symbolication.
   - Add your Google email(s) under **Testers**, copy the opt-in URL, open it on your phone, and install Onda from the Google Play Store.
   - Disable *Unknown sources* in Android Auto Developer Settings and verify Onda appears natively in your car!

---

## 5. Before Promoting to Closed Testing or Public Production (Future)

If you ever decide to publish publicly beyond Internal Testing:
- **Bundled stations (`stations.json`):** Review IP/trademark risk for the 64 hardcoded broadcaster streams/logos; consider switching to dynamic Radio Browser discovery only and ensuring store screenshots do not prominently feature third-party trademarked logos.
- **12-tester closed test:** New personal accounts require 12 opted-in testers for 14 consecutive days before Production access.
- **Store listing assets:** 512×512 PNG icon, 1024×500 feature graphic, phone screenshots (plus 3 foldable screenshots + Adaptive Tier 3 verification from **1 April 2027**).
- **Manual readiness attestation:** Update `.app-review-readiness.json` and run:
  ```bash
  bash ~/repositories/app-store-compliance/agent-os/hooks/app-store-compliance-guard.sh "/home/mcosta/radio-app"
  ```
