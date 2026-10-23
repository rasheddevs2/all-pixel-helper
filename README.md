# All Pixel Helper (Android prototype)

This project was assembled from the source-code snippets in the uploaded Google AI Studio conversation export. It is a prototype and has not been compiled or tested on a device in this session.

## Build on GitHub Actions
1. Upload the **contents** of this project folder to the root of your `all-pixel-helper` repository (do not upload only the ZIP).
2. Ensure `.github/workflows/build.yml` is present.
3. Open the repository's Actions tab and run **Build Android APK**.
4. If the build succeeds, download the `All-Pixel-Helper-Debug-APK` artifact.

## Notes
- The workflow installs Gradle in the cloud and does not require a checked-in `gradlew` wrapper.
- This is browser/WebView tracking diagnostics, not a system-wide Android extension.
- Network interception and JavaScript bridge behavior may vary by website. Do not assume 100% event capture.
- Inspect and test the app before using it for real client work.
