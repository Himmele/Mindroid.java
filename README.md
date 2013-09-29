## Mindroid.java ##

Mindroid is an application framework that lets you create innovative applications using a set of reusable components - just like Android. 
The name Mindroid has two different meanings. On one hand Mindroid is a minimal set of core Android classes and on the other hand
these classes also form the mind of Android.
Mindroid is almost completely Android API-compliant with minor exceptions since Mindroid even runs on Java v1.4 (Java SE and Jave ME CDC) targets.
Mindroid also includes an application framework <a href="http://esrlabs.com/mindroid">documentation</a>.
The application framework is mainly intended for machine-to-machine (M2M) communication projects and the Internet of Things.
We also plan to make this application framework work on Java ME 8 and CLDC 8 (JSR 360).

#### [Mindroid Developer Guide](http://esrlabs.com/Mindroid) ####

### Build ###
* ant sdk (Compile the Mindroid.jar SDK)
* ant install-apps (Build and install apps to the apps directory)
* ant docs (Build the SDK docs)

### Run ###
* Linux: java -classpath Mindroid.jar:Main.jar main.Main rootDir=../../
* Microsoft Windows: java -classpath Mindroid.jar;Main.jar main.Main rootDir=../../

The rootDir must point to the parent directory of the apps directory to enable the package manager to find and load the apps.
When starting the Mindroid test e.g. from out/pc rootDir has to point to "../../".
