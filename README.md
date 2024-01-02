# Soul Essent

<img src="https://i.ibb.co/gPqCvrh/Background.png" align="right"
alt="Size Limit logo by Anton Lovchikov" width="80" height="80">

**SOUL Essent** is a _NLP toolkit_ with http API access.</br>
In terms of the SOUL infrastructure, SOUL Essent is at a fairly high level and lies outside the contract execution area.</br>
It can even be deployed on another machine and have IP forwarded to the network to optimize calculations that require a model for classification or
other calculations not of a static nature, but using neural network models.</br>
SOUL Essent is a multilingual environment that uses adapters to run the daemon in Python and work with it with high performance.</br>

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)
![Python](https://img.shields.io/badge/python-3670A0?style=for-the-badge&logo=python&logoColor=ffdd54)

<br/>

## Module principles ☝️
- Essent **does not store any information**, either anonymous or in original form.
- Essent is a **standalone product** and can be separated from the SOUL infrastructure without using emulation of missing models
- Essent and all forks are distributed exclusively **under the MIT license**. All products within SOUL Essent also use open licenses (not limited to MIT)


### Get started
```shell
java --version # openjdk 18.0.2-ea 2022-07-19
```

```shell
git clone https://github.com/soul-guru/soul-essent
cd cd soul-essent
sh gradlew buildShadow

docker build -f Dockerfile -t soul/essent:latest .
```