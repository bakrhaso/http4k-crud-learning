# HelloWorld

## Package
```
./gradlew distZip
```

## Native compile
```
./gradlew clean test nativeCompile

./build/native/nativeCompile/helloworld
```
Above snippet was taken from https://github.com/http4k/examples/blob/master/graalvm/build_and_run.sh


## Notes
- Out of the box, OpenAPI only works with Jackson. OOTB, Jackson doesn't work with native.
-
