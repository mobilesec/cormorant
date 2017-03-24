[![travis status](https://travis-ci.org/mobilesec/cormorant.svg?branch=master)](https://travis-ci.org/mobilesec/cormorant)
[![GitHub release](https://img.shields.io/github/release/mobilesec/cormorant.svg?maxAge=2592000)]()
[![GitHub tag](https://img.shields.io/github/tag/mobilesec/cormorant.svg?maxAge=2592000)]()

# Cormorant
CORMORANT is an extensible, risk-aware, multi-modal, crossdevice authentication framework that enables transparent
continuous authentication using different biometrics across multiple trusted devices.

# Please note
CORMORANT is currently under development and far from being mature. APIs are thus subject to change and not yet considered stable. In case you wan't to work with or contribute to this project, make sure to contact one of the contributors.  

# API

You can easily create authentication and risk plugins for cormorant using the API provided. The following gradle snippet illustrates how to add the cormorant dependency to your android project.

```gradle
repositories {  
   jcenter()  
}

dependencies {
  compile 'at.usmile.cormorant:cormorant-api:0.0.1'
}
```

# Architecture
![framework architecture](https://raw.githubusercontent.com/mobilesec/cormorant/develop/cormorant-documentation/framework_architecture.svg)

# Development

Make sure to run ```./gradlew licenseFormat``` before commiting to ensure licence headers are correct (or else they break the build).

# Disclaimer

You are using this application at your own risk. *We are not responsible for any damage caused by this application, incorrect usage or inaccuracies in this manual.*

# Presentations

[![Continuous risk-aware multi-modal authentication](https://img.youtube.com/vi/c9uYvoSfy38/0.jpg)](https://www.youtube.com/watch?v=c9uYvoSfy38)

# Literature
[1] D. Hintze, R. Findling, M. Muaaz, E.Koch, R. Mayrhofer: *[CORMORANT: Towards Continuous Risk-Aware Multi-Modal Cross-Device Authentication](https://dl.acm.org/authorize?N08572)*, UbiComp/ISWC'15 Adjunct, Adjunct Proceedings of the 2015 ACM International Joint Conference on Pervasive and Ubiquitous Computing and Proceedings of the 2015 ACM International Symposium on Wearable Computers, 2015, September 13-17, Osaka, Japan, Pages 169-172

[2] D. Hintze, M. Muaaz, R. Findling, S. Scholz, E.Koch, R. Mayrhofer: *[Confidence and Risk Estimation Plugins for Multi-Modal Authentication on Mobile Devices using CORMORANT](https://dl.acm.org/citation.cfm?id=2843845)*, Proceedings of the 13th International Conference on Advances in Mobile Computing & Multimedia (MoMM 2015), December 11-13, Brussels, Belgium, Pages 384-388

[3] D. Hintze, S. Scholz, E. Koch, R. Mayrhofer: *[Location-based Risk Assesment for Mobile Authentication](https://dl.acm.org/citation.cfm?id=2971448)*, Adjunct Proceedings of the 2016 ACM International Joint Conference on Pervasive and Ubiquitous Computing and Proceedings of the 2016 ACM International Symposium on Wearable Computers, 2016, September 14-17, Heidelberg, Germany, Pages 85-88
