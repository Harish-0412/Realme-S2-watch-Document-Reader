# Toolchain sources

Versions were checked against official vendor documentation during implementation on 2026-10-14. These intentionally pinned stable releases form the API-35 toolchain; source compilation could not be verified in this workspace because dependency downloads were blocked and no SDK was installed.

- [AGP 8.9 release notes](https://developer.android.com/build/releases/agp-8-9-0-release-notes): lists 8.9.2 fixes, maximum API 35, Gradle 8.11.1, default build-tools 35.0.0 and JDK 17 baseline.
- [Kotlin 2.1.20 release](https://kotlinlang.org/docs/whatsnew2120.html): Kotlin release and Gradle support.
- [Compose compiler configuration](https://developer.android.com/jetpack/androidx/releases/compose-kotlin): Kotlin 2.0+ uses the Compose compiler Gradle plugin; the project pins both plugins to 2.1.20.
- [Compose BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping): library version alignment. Project BOM is 2025.04.01; Material 3, UI and tooling use that BOM.
- [AndroidX Core releases](https://developer.android.com/jetpack/androidx/releases/core): 1.15.0.
- [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity): 1.10.1.
- [Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle): 2.9.0.
- [DataStore releases](https://developer.android.com/jetpack/androidx/releases/datastore): 1.1.4.
- [Coroutines 1.10.2 release](https://github.com/Kotlin/kotlinx.coroutines/releases/tag/1.10.2).
- [Serialization 1.8.1 release](https://github.com/Kotlin/kotlinx.serialization/releases/tag/v1.8.1).
- [Gradle checksums](https://gradle.org/release-checksums/): Gradle 8.11.1 distribution SHA-256 `f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6`; wrapper JAR SHA-256 `2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046`.

The actual wrapper JAR and shell/batch scripts come from the [Gradle v8.11.1 tag](https://github.com/gradle/gradle/tree/v8.11.1/gradle/wrapper). Wrapper distribution verification is configured in `gradle-wrapper.properties`; the included JAR checksum was independently compared with the official checksum above.
