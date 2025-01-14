# Text Transformer Accessibility Service

The **Text Transformer Accessibility Service** is an Android application designed to enhance text
manipulation across all apps. By leveraging Android's Accessibility Service, it enables users to
apply various text transformations seamlessly, including:

- **Random Case**: Alternates the casing of letters randomly.
- **UPPERCASE**: Converts all letters to uppercase.
- **lowercase**: Converts all letters to lowercase.
- **Emojify**: Replaces certain words with corresponding emojis.
- **Leet**: Transforms text into leetspeak by substituting letters with numbers or symbols.

## Features

- **Global Text Transformation**: Apply transformations to text across all applications.
- **Customizable Transformations**: Select which transformations to apply based on user preference.
- **Accessibility Integration**: Utilizes Android's Accessibility Service to monitor and modify text
  input.

## Installation

1. **Clone the Repository**:

   ```bash
   git clone https://github.com/variablevar/probable-adventure.git
   ```

2. **Open in Android Studio**:

    - Launch Android Studio.
    - Select **Open an Existing Project**.
    - Navigate to the cloned repository folder and open it.

3. **Build the Project**:

    - Ensure all dependencies are installed.
    - Click on **Build** > **Make Project**.

4. **Run on Device**:

    - Connect your Android device or start an emulator.
    - Click on **Run** > **Run 'app'**.

## Usage

1. **Enable Accessibility Service**:

    - Go to **Settings** > **Accessibility**.
    - Find and select **Text Transformer Accessibility Service**.
    - Toggle the switch to enable the service named **Random Case**.

2. **Configure Transformations**:

    - Open the **Random Case** app.
    - First select the texts , and then click the floating button
    - Select the transformations you wish to apply.

3. **Apply Transformations**:

    - Navigate to any app with text input.
    - Enter your text.
    - The selected transformations will be applied automatically.

## Permissions

The app requires the following permissions:

- **Accessibility Service**: To monitor and modify text input across applications.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your
enhancements.

## License

This project is licensed under the MIT [LICENSE](LICENSE).

## Acknowledgements

- [Android Accessibility Service Documentation](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
- [Developing an Accessibility Service Codelab](https://codelabs.developers.google.com/codelabs/developing-android-a11y-service)

---

*Note: This application is intended to assist users by providing additional text manipulation
capabilities. It is not a replacement for Android's built-in accessibility features.* 