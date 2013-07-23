## Mindroid.java ##

Mindroid's application framework lets you create extremely rich and innovative apps using a set of reusable components.
The Mindroid application framework currently complies to the Java specification v1.4 to make it work on Jave ME CDC targets.
It provides a subset of the Android application framework API.
The framework is mainly intended for machine-to-machine (M2M) projects in the Internet of Things.
We also plan to make this application framework work on Java ME 8 and CLDC 8 (JSR 360).

#### [Mindroid Developer Guide](http://esrlabs.com/Mindroid) ####

### Build ###
* ant sdk (Compiles the Mindroid.jar SDK)
* and install-apps (Build and installs apps to the apps directory)
* ant docs (Builds the SDK docs)

### Run ###
* Linux: java -classpath Mindroid.jar:Main.jar main.Main rootDir=../../
* Microsoft Windows: java -classpath Mindroid.jar;Main.jar main.Main rootDir=../../
The rootDir must point to the parent directory of the apps directory to that the package manager is able to find and load the apps.
When starting the Mindroid test e.g. from out/pc rootDir has to point to "../../".
