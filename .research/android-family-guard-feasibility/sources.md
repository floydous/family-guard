# Sources & References

Riset ini mengacu pada sumber primer (dokumentasi resmi, standar keamanan industri, kode sumber terbuka, dan diskusi teknis resmi para perancang sistem):

## 1. Dokumentasi Resmi Android & AOSP (Google)
- **DevicePolicyManager & Device Administration:**
  - `android.app.admin.DevicePolicyManager` - [Android Developers Reference](https://developer.android.com/reference/android/app/admin/DevicePolicyManager)
  - `android.os.UserManager` (`DISALLOW_INSTALL_APPS`, `DISALLOW_INSTALL_UNKNOWN_SOURCES`) - [Android Developers Reference](https://developer.android.com/reference/android/os/UserManager)
  - Building a Device Policy Controller (DPC) & Dedicated Devices - [Android Enterprise Documentation](https://developer.android.com/work/dpc/dedicated-devices/cookbook)
- **Package Management & Visibility:**
  - `android.content.pm.InstallSourceInfo` (API Level 30+) - [Android API Reference](https://developer.android.com/reference/android/content/pm/InstallSourceInfo)
  - Package Visibility Filtering on Android 11+ (`QUERY_ALL_PACKAGES` & `<queries>`) - [Android Developers Guide](https://medium.com/androiddevelopers/package-visibility-in-android-11-cc857f221cd9)
- **Network & VPN Service:**
  - `android.net.VpnService` - [Android Developers Reference](https://developer.android.com/reference/android/net/VpnService)
- **UI & Permissions:**
  - `android.permission.SYSTEM_ALERT_WINDOW` & Changes in Android 11/12 - [Android Security Guidance](https://developer.android.com/about/versions/11/privacy/permissions)
  - Tapjacking & `HIDE_OVERLAY_WINDOWS` - [Android Security Fraud Prevention](https://developer.android.com/security/fraud-prevention/activities)
  - `AccessibilityService` API & Google Play Policy Restrictions - [Google Play Policy Center](https://orangeoma.zendesk.com/hc/en-us/articles/4407888308242-Google-Play-policy-about-use-of-the-Accessibility-API)

## 2. Standar Keamanan & Analisis Ancaman (Threat Intelligence)
- **MITRE ATT&CK Mobile Matrix:**
  - Technique T1655.001: *Masquerading: Match Legitimate Name or Location* - [MITRE ATT&CK Mobile](https://attack.mitre.org/techniques/T1655/001/)
  - Technique T1417.002: *Input Capture / GUI Input Capture & Cloak and Dagger* - [MITRE ATT&CK Mobile](https://attack.mitre.org/versions/v12/techniques/T1417/002/)
- **OWASP Mobile Application Security (MASVS / MASTG):**
  - MASTG-DEMO-0104: *App Requesting SYSTEM_ALERT_WINDOW Permission* - [OWASP MAS](https://mas.owasp.org/MASTG/demos/android/MASVS-PLATFORM/MASVS-PLATFORM-0104/)

## 3. Investigasi Teknis DNS Filtering & HTTPS Block Page
- **Pi-hole Official Documentation & Community Core Team:**
  - "Want to have a working blocking page with a secure SSL HTTPS/HTTP" - Pi-hole Userspace Discourse. Konfirmasi resmi: Block page IP mode pada Pi-hole ditinggalkan karena HTTPS menyebabkan *broken certificate/TLS handshake errors*.
  - Pi-hole v5/v6 deprecation of HTML Block Pages for HTTPS domains - [Pi-hole Discourse](https://discourse.pi-hole.net/t/want-to-have-a-working-blocking-page-with-a-secure-ssl-https-http/53935)
- **AdGuard & NextDNS Documentation:**
  - AdGuard Home Issue #5640 & #7027: *Custom Block Page on DNS level produces TLS validation errors* - [AdGuard Home GitHub](https://github.com/AdguardTeam/AdGuardHome/issues/5640)
  - NextDNS Help Center: *Block page with Root CA requirement & quirks* - [NextDNS Discussions](https://help.nextdns.io/t/y4h7frw/nextdns-block-page-with-root-ca-a-strange-quirk)
  - AdGuard Android HTTPS Filtering Architecture (Local Root CA installation & Android 7+ restrictions) - [AdGuard Knowledge Base](https://adguard.com/kb/archive/adguard-for-windows/solving-problems/connection-not-trusted/)
- **RethinkDNS & Blokada Architecture:**
  - Local `VpnService` TUN routing, local DNS parser (Go/Rust), memory-efficient Bloom Filters & Radix Trees for blocklists.
