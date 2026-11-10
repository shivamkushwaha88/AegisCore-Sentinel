# AegisCore Sentinel — Cross-Platform Product & Implementation Plan

A privacy-first security transparency and audit product for Android mobile (and Windows desktop companion), built on the principle of evidence-backed, user-controlled remediation without cloud dependencies or unsubstantiated security claims.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The implementation plan has been revised to adopt the user's detailed cross-platform product strategy and technical blueprint:

- **Truthful Assessment Over Arbitrary Scores**: Replaced misleading single scores (e.g. "94/100" or "100% safe") with transparent, evidence-backed metrics: **Finding Severity** (Critical, High, Moderate, Low, Informational), **Evidence Confidence** (Confirmed, Strong Indicator, Weak Indicator, Insufficient Data), and **Assessment Coverage** (applicable checks completed vs total).
- **Capability-Aware Android Collectors**: Audits visible installed packages (`PackageManager`), requested vs granted runtime permissions, exported component risk factors, verified signing certificate digests, and public device posture signals (Android API, security patch level, lock screen status).
- **Scoped User-Selected File & APK Preflight**: Safe, bounded parsing of user-selected APKs and files using Android Storage Access Framework (SAF) — inspecting package manifests, signature schemes, requested permissions, and hashes without executing untrusted code or scanning inaccessible `/data/data` private paths.
- **Developer Trust Profiles & Scoped Exceptions**: Rather than promising impossible OS-level process sandboxing or silent blocking, the Trust Manager provides transparent, scoped risk-acceptance annotations tied to exact package and signing certificate fingerprints.
- **Shared Data Contracts & Zero Outbound Telemetry**: Standardized `ScanRun`, `CheckResult`, `EvidenceRecord`, `Finding`, and `TrustException` models with local Room SQLite storage, sanitized report export, and zero outbound network traffic.

---

## 1. Overview & Core Concept

**Product Promise**: *Understand what is happening on your devices. See the evidence. Know what you can safely do about it.*

AegisCore Sentinel is not a replacement for Android's built-in security controls or enterprise EDR. It is a local, privacy-first security transparency companion that audits:
1. **Installed Applications & Permissions**: Clear distinction between *requested*, *granted*, and *observable* capabilities.
2. **Exported Components & Attack Surface**: Static analysis of unprotected or misconfigured activities, receivers, and services.
3. **App Signing Identity & Lineage**: Package certificates, installer attribution, and update integrity.
4. **Device Posture**: Public, non-heuristic security patch levels, OS version, and lock-screen status.
5. **APK Preflight**: Inspects uninstalled APKs selected via SAF to evaluate manifest flags, permissions, and certs before user installation.
6. **Scoped Risk Annotations**: Allows developers to document and review accepted risks for AI tools, terminals, and dev packages.

---

## 2. User Experience & Visual Design

### Visual Identity & Theme
- **Canvas & Elevation**: Void obsidian background (`#0B0F19`), elevated card surfaces (`#131B2E`), subtle structural borders (`#263247`).
- **Semantic Accents**:
  - **Healthy / Complete Check**: `#10B981` (Emerald)
  - **Review Recommended**: `#F59E0B` (Amber)
  - **High Severity / Immediate Review**: `#EF4444` (Crimson)
  - **Developer & Trust Exception**: `#6366F1` (Indigo)
- **Typography**: M3 typography hierarchy with monospace typography strictly reserved for package names, SHA-256 digests, permission constants, and technical evidence.

### Mobile Screen Structure (Jetpack Compose)

```
┌─────────────────────────────────────────────────────────────────┐
│                    AEGISCORE SENTINEL                           │
├─────────────────────────────────────────────────────────────────┤
│  Assessment: Review Recommended                                 │
│  [ 3 items need attention ]    [ 88% checks completed ]         │
│  Last audit: Today, 10:42 AM   Network monitor: Off             │
├─────────────────────────────────────────────────────────────────┤
│  [ ▶ Run Full Audit ]         [ ⚡ Quick Assessment ]           │
├─────────────────────────────────────────────────────────────────┤
│  High-Priority Findings (Evidence-Backed)                       │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │ HIGH [CONFIRMED] ● com.example.analytics                  │  │
│  │ Granted: ACCESS_BACKGROUND_LOCATION + READ_PHONE_STATE    │  │
│  │ Source: PackageManager · Target SDK: 28 (Outdated)        │  │
│  │ [ Open App Settings ]   [ Add Trust Note ]   [ Details ]  │  │
│  └───────────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│  Audit Modules:                                                 │
│  ┌─────────────────────────┐     ┌─────────────────────────┐    │
│  │ 📱 App Risk Explorer    │     │ 🔍 APK Preflight (SAF)  │    │
│  │ 42 apps · 12 sensitive  │     │ Inspect APK before inst │    │
│  └─────────────────────────┘     └─────────────────────────┘    │
│  ┌─────────────────────────┐     ┌─────────────────────────┐    │
│  │ 🛡 Device Posture       │     │ ⚙ Trust Profiles (2)    │    │
│  │ Patch: 2026-10-05 (Pass)│     │ Scoped risk exceptions  │    │
│  └─────────────────────────┘     └─────────────────────────┘    │
├─────────────────────────────────────────────────────────────────┤
│  Navigation: [ Overview ] [ Apps ] [ Inspect ] [ Trust ] [ Log ]│
└─────────────────────────────────────────────────────────────────┘
```

### Detailed Views
1. **Overview Dashboard**: Honest posture summary, coverage percentage, check counts, priority findings, and quick actions.
2. **App Risk Explorer**: Searchable list of visible installed apps, filterable by high-impact permissions (Camera, Mic, Location, Accessibility, SMS), target SDK, and installer source. Tap opens detailed package view with requested vs granted breakdown and direct system Settings links.
3. **Inspect / APK Preflight**: SAF file picker integration. Parses APK binary manifest, extracts package identity, permissions, exported components, and SHA-256 certificate digest without executing the APK.
4. **Trust Profiles & Exceptions**: Scoped exceptions bound to package ID + certificate digest + rule ID. Includes user rationale, created date, and expiry date. Clearly indicates that exceptions do not alter OS permissions.
5. **Security Timeline & Reports**: Chronological audit log with exportable, sanitized JSON and plain-text reports via Android share sheet.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale |
|---|---|---|
| **No Universal Score (94/100)** | Replaced with **Severity + Confidence + Coverage %** | A single score creates false reassurance and conceals blind spots. Users see what was checked and what was unavailable. |
| **No Fake Real-time Interception** | Snapshot-based audits with transparent status | Android sandboxing prevents third-party apps from snooping clipboard or mic usage of other apps; claiming to detect zero-days from generic heuristics is dishonest. |
| **User-Selected Files Only (SAF)** | Storage Access Framework with scoped URIs | Direct access to `/data/data` or full filesystem is prohibited by Android 11+ storage privacy rules. |
| **Remediation via Platform Intents** | `ACTION_APPLICATION_DETAILS_SETTINGS`, uninstall intents, file deletion | Android 14+ prohibits apps from killing third-party background processes or revoking permissions directly. Directing users to system settings is safe, reliable, and compliant. |
| **Trust Manager as Risk Annotation** | Scoped exceptions bound to package + cert hash + rule | Clarifies to users that trusting a developer tool documents risk acceptance rather than enforcing an OS-level sandbox. |
| **Local SQLite Persistence (Room)** | On-device Room Database with encrypted export options | Maximum privacy, zero telemetry, full offline operation. |

---

## 4. Technical Architecture & Data Strategy

### Android Client Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│   OverviewScreen    AppsScreen    InspectScreen    TrustScreen   Logs  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ StateFlow / UI Events
┌───────────────────────────────────▼────────────────────────────────────┐
│                    SentinelViewModel (MVVM)                            │
│   - scanState : StateFlow<ScanProgress>                                │
│   - coverage : StateFlow<AuditCoverage>                                │
│   - findings : StateFlow<List<Finding>>                                │
│   - apps : StateFlow<List<AppSnapshot>>                                │
│   - trustExceptions : StateFlow<List<TrustException>>                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Orchestrates
┌───────────────────────────────────▼────────────────────────────────────┐
│                 AegisCore Android Capability Registry                  │
│  ┌──────────────────────┐ ┌─────────────────────┐ ┌──────────────────┐ │
│  │ PackageCollector     │ │ PermissionCollector │ │ ComponentCollect │ │
│  └──────────────────────┘ └─────────────────────┘ └──────────────────┘ │
│  ┌──────────────────────┐ ┌─────────────────────┐ ┌──────────────────┐ │
│  │ DevicePostureCollect │ │ SafApkPreflight     │ │ EvidenceBuilder  │ │
│  └──────────────────────┘ └─────────────────────┘ └──────────────────┘ │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Rule Evaluation & Evidence Mapping
┌───────────────────────────────────▼────────────────────────────────────┐
│                   Versioned Rule Evaluator                             │
│   - RuleSet: ANDROID_BASELINE_V1                                       │
│   - Maps observations into EvidenceRecord & Finding entities           │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Persists / Queries
┌───────────────────────────────────▼────────────────────────────────────┐
│                 Local Room Database (SQLite Engine)                    │
│   - ScanRunDao           - CheckResultDao        - AppSnapshotDao      │
│   - EvidenceRecordDao    - FindingDao            - TrustExceptionDao   │
└────────────────────────────────────────────────────────────────────────┘
```

### Shared Domain Contracts & Room Entities

1. **`ScanRun`**: `id`, `startedAt`, `completedAt`, `scanType`, `platform` (ANDROID), `osVersion`, `securityPatch`, `ruleSetVersion`, `status`, `applicableChecks`, `completedChecks`, `unavailableChecks`.
2. **`CheckResult`**: `id`, `scanRunId`, `checkId`, `status` (`Pass`, `Review`, `NotObservable`, `NeedsUserAccess`, `Error`), `summary`, `limitationCode`.
3. **`AppSnapshot`**: `id`, `scanRunId`, `packageName`, `displayName`, `versionName`, `versionCode`, `targetSdk`, `installerPackage`, `signingCertificateSha256`, `firstInstallTime`, `lastUpdateTime`.
4. **`CapabilityFact`**: `id`, `appSnapshotId`, `factType` (`AndroidPermission`, `AndroidSpecialAccess`), `capabilityOrPermission`, `declaredOrRequested`, `effectiveState` (`Granted`, `Denied`, `Unknown`), `observationSource`.
5. **`EvidenceRecord`**: `id`, `scanRunId`, `subjectType`, `subjectId`, `sourceApi`, `observationCode`, `sanitizedValue`, `confidence`, `limitations`.
6. **`Finding`**: `id`, `ruleId`, `ruleVersion`, `scanRunId`, `targetType`, `targetId`, `title`, `summary`, `whyItMatters`, `severity` (`Critical`, `High`, `Moderate`, `Low`, `Informational`), `confidence` (`Confirmed`, `StrongIndicator`, `WeakIndicator`), `recommendedAction`, `status`.
7. **`TrustException`**: `id`, `platform`, `targetType`, `packageName`, `signingCertificateSha256`, `ruleId`, `reason`, `createdAt`, `expiresAt`, `enabled`.

---

## 5. Verification Plan

### Automated Verification
1. **Compilation**: Run `compile_applet` to ensure full Kotlin/Compose/Room compilation, KSP schema generation, and zero dependency errors.
2. **Unit & Robolectric Tests**:
   - `PermissionCollectorTest`: Verifies requested vs granted separation and sensitive permission flagging.
   - `RuleEvaluatorTest`: Verifies deterministic rule evaluation, severity/confidence assignment, and coverage calculations.
   - `TrustExceptionTest`: Verifies scoping by package + cert SHA-256 and finding suppression behavior.
   - `NegativeTests`: Asserts that unobservable APIs return `NotObservable`, never a false `Pass` or fake green checkmark.

### User Flow Walkthrough
1. **First Launch**: Honest onboarding explaining local processing, package visibility, and what AegisCore cannot inspect.
2. **Run Audit**: Live progress meter; calculates coverage %; returns categorized findings with confirmed evidence.
3. **App Inspection & Remediation**: View sensitive permissions for an app; tap "Open App Settings" to jump directly to Android's app settings.
4. **APK Preflight**: Select an APK via SAF; verify manifest extraction, signing certificate hash display, and permission breakdown.
5. **Add Trust Exception**: Annotate an app or developer tool; confirm finding status changes to `Dismissed/Exception` with expiration reminder.
6. **Export Sanitized Report**: Export a privacy-preserving text/JSON report via Android share sheet.
