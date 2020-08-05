
[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![LinkedIn][linkedin-shield]][linkedin-url]



<!-- PROJECT LOGO -->
<br />
<p align="center">
  <a href="https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app">
     <img src="images/logo.png" alt="Logo" width="80" height="80">
 </a>

  <h3 align="center">Respiratory rate calculator app</h3>

  <p align="center">
    Android applicaition that measures the respiratory rate of the user with the help of a mobile thermal camera
    <br />
    <a href="https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app"><strong>Explore the docs »</strong></a>
    <br />
    <br />
    <a href="https://youtu.be/LGrs2qq4VDg/">View Demo</a>
    ·
    <a href="https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/issues">Report Bug</a>
    ·
    <a href="https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/issues">Request Feature</a>
  </p>
</p>



<!-- TABLE OF CONTENTS -->
## Table of Contents

* [About the Project](#about-the-project)
  * [Built With](#built-with)
  * [Features](#features)
* [Getting Started](#getting-started)
  * [Prerequisites](#prerequisites)
  * [Installation](#installation)
* [Contributing](#contributing)
* [Contact](#contact)
* [Acknowledgements](#acknowledgements)



<!-- ABOUT THE PROJECT -->
## About The Project

[![Screen Shot 1][product-screenshot]](https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/)
[![Screen Shot 2][product-screenshot-2]](https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/)

Respiratory rate calculator is an android native application that measures the respiratory rate for users and detects breathing disorders. The application measures the breath temperature of the user for 30 seconds with the help of a flir one mobile thermal camera and flir one SDK. Before starting to record, the area under of the nose of the user is detected using OpenCV to monitor the breath temperature from that area. After 30 seconds of recording, the app will display the results of the test of the user including respiration signal, respiratory rate calculated, and if the respiratory rate calculated is normal or abnormal. The respiratory rate is the peak count of the respiration signal and an algorithim is implemented to detect the peaks of the signal after tbe signal has been filtered using a low pass butterworth filter.

A list of commonly used resources that I find helpful are listed in the acknowledgements.

### Features
* Thermal camera tunning
* Automatic detection for the area under the nose
* Signal plotting
* Signal filtering
* Peak detection


### Built With
* [Android studio](https://getbootstrap.com)
* [Flir One SDK](https://reactjs.org/)
* [OpenCV Android SDK](https://d3js.org/)
* [Android Graph View](http://www.android-graphview.org/)

## Getting Started


### Prerequisites

*Android studio / Install android studio from https://developer.android.com/studio

*Git / Check if you have Git using:
```sh
npm --version
```
If not install git from https://git-scm.com/downloads

*Google API 28 <br/>
Install Google API 28 from android studio. / visit this link to see how: https://abhiandroid.com/androidstudio/download-new-api-for-sdk-android-studio.html



### Installation

1. Clone the Repo 
```sh
git clone https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app.git
```
2. Import the project cloned on android studio

3. Build the APK

4. Send the APK to an android device and install the application


<!-- CONTRIBUTING -->
## Contributing

Contributions are what make the open source community such an amazing place to be learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request


<!-- CONTACT -->
## Contact

Haitham Chabayta - haithamchabayta@gmail.com

LinkedIn: https://www.linkedin.com/in/haitham-chabayta-0654681b1/



<!-- ACKNOWLEDGEMENTS -->
## Acknowledgements
* [GitHub Emoji Cheat Sheet](https://www.webpagefx.com/tools/emoji-cheat-sheet)
* [GitHub Pages](https://pages.github.com)
* [Graph View](http://www.android-graphview.org/)
* [Flir one SDK Documentation](https://developer.flir.com/mobile/flironesdk/)
* [Filtering algorithim java](http://www.dspguide.com/)
* [Peak detection algorithim](https://gist.github.com/tiraeth/1306602)



[contributors-shield]: https://img.shields.io/github/contributors/haitham-chabayta/contactless-respiratory-rate-measurement-app.svg?style=flat-square
[contributors-url]: https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/haitham-chabayta/contactless-respiratory-rate-measurement-app.svg?style=flat-square
[forks-url]: https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/network/members
[stars-shield]: https://img.shields.io/github/stars/haitham-chabayta/contactless-respiratory-rate-measurement-app.svg?style=flat-square
[stars-url]: https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/stargazers
[issues-shield]: https://img.shields.io/github/issues/haitham-chabayta/contactless-respiratory-rate-measurement-app.svg?style=flat-square
[issues-url]: https://github.com/haitham-chabayta/contactless-respiratory-rate-measurement-app/issues
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=flat-square&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/haitham-chabayta-0654681b1/
[product-screenshot]: images/screenshot.PNG
[product-screenshot-2]: images/screenshot-2.PNG
