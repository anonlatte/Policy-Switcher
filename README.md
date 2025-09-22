# Policy Switcher

This repository ships with a GitHub Actions based CI/CD pipeline tailored for an Android application. The workflow lives in [`android-ci.yml`](.github/workflows/android-ci.yml) and automates build validation for every change as well as release packaging when version tags are pushed.

## Workflow overview
- **Build & Test (CI)** runs on pull requests, pushes to `main`, manual dispatches, and tag events. The job checks out the code, validates the Gradle wrapper, provisions JDK 17 and the Android SDK (API level 34 / Build Tools 34.0.0), enables Gradle caching, and then executes `./gradlew lint test assembleDebug`. The resulting debug APK is published as an artifact for quick download.
- **Release build (CD)** is triggered only when a tag that starts with `v` is pushed. After reusing the same toolchain setup, it builds both `assembleRelease` and `bundleRelease`, uploads the generated APK/AAB artifacts, and publishes a GitHub release that includes those binaries.

## Usage notes
- Update the `api-level` and `build-tools` values in the workflow if your project targets a different Android SDK version.
- The workflow assumes the presence of a Gradle wrapper (`gradlew`) and a standard `app` module structure for artifact paths. Adjust the Gradle tasks or artifact globs if your module name differs.
- For signed releases, configure signing inside your Gradle scripts and expose the required credentials as encrypted repository secrets, then reference them in the workflow steps.
- Trigger a release by creating and pushing a version tag (for example, `v1.0.0`). The workflow will automatically create a GitHub release with the packaged binaries.
