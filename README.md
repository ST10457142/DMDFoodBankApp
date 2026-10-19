DMD Food Bank App
A food management mobile application that stores and manages all donation information in
one central location, making donors, donations, beneficiaries, management, and reporting
more efficient and organized.

Module :
XISD6329 – Work Integrated Learning
Team Members
1.Ditebogo J. Nkwana
2.Thembelinhle M. Ngobeni
3.Mogomotsi Malepe
4.Dineo Nkuna

Features :
- User registration and login (passwords encrypted)
- Single Sign-On (SSO)
- Change user settings
- Input validation (app does not crash on bad input)
- Centralised database for donors, donations, and beneficiaries
- Automated testing and build via GitHub Actions

Tech Stack :
- Language:Kotlin
- Platform:Android
- Database:Firebase 
- Authentication:Firebase Auth
- CI/CD:GitHub Actions

How to Run :
1. Clone the repo: git clone 
2. Open in Android Studio
3. Let Gradle sync
4. Run on a real Android device (not emulator)
   
Links :
- Demo Video: https://youtube.com/shorts/aGZP98Xcnsk?si=XOcFGbX7BTTtLuAh 
  
Automated Testing :
This project uses GitHub Actions to automatically run tests and build the app on every push.
See `.github/workflows/android.yml’
