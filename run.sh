#!/bin/sh
# Exit if any command fails
set -e

cd tunnel
cargo ndk -t arm64-v8a -t armeabi-v7a -t x86_64 build --release

mkdir -p ../app/src/main/jniLibs/arm64-v8a ../app/src/main/jniLibs/armeabi-v7a ../app/src/main/jniLibs/x86_64

cp target/aarch64-linux-android/release/libtunnel.so ../app/src/main/jniLibs/arm64-v8a/
cp target/armv7-linux-androideabi/release/libtunnel.so ../app/src/main/jniLibs/armeabi-v7a/
cp target/x86_64-linux-android/release/libtunnel.so ../app/src/main/jniLibs/x86_64/

cd ../
./gradlew installDebug && adb shell am start -n rahul.secretcodes.vvpn/.MainActivity


