# Spline

This code implements cubic spline interpolation and a few related methods.

# Typescript

Typescript code is in `ts/src` folder. To compile to JavaScript, run `tsc` command 
in `ts` folder, JavaScript files will appear in `ts/dist`.

Demo (source in `ts/demo` folder) can be seen at at:
- [Simple](https://sorotokin.com/spline/demo/simple.html)
- [Parametric](https://sorotokin.com/spline/demo/parametric.html)

# Java

Java code is in `java/src` folder. To compile and run the demo use:

```
mkdir -p java/dist
cd java/src
javac -d ../dist --source-path ../src com/sorotokin/spline/Main.java
java -cp ../dist com.sorotokin.spline.Main
```

To run tests

```
java -cp ../dist:/path/to/junit.jar junit.textui.TestRunner com.sorotokin.spline.SplineTest
```