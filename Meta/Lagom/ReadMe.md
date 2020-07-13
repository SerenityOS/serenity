# Lagom

The Serenity C++ library, for other Operating Systems.

## About

If you want to bring the comfortable Serenity classes with you to another system, look no further. This is basically a "port" of the `AK` and `LibCore` libraries to generic \*nix systems.

*Lagom* is a Swedish word that means "just the right amount." ([Wikipedia](https://en.wikipedia.org/wiki/Lagom))

## Fuzzing

Lagom can be used to fuzz parts of SerenityOS's code base. This requires buildling with `clang`, so it's convenient to use a different build directory for that. Run CMake like so:

    # From the root of the SerenityOS checkout:
    mkdir BuildLagom && cd BuildLagom
    cmake -GNinja -DBUILD_LAGOM=ON -DENABLE_FUZZER_SANITIZER=ON -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++ ..
    ninja FuzzJs && Meta/Lagom/Fuzzers/FuzzJs

clang emits different warnings than gcc, so you'll likely have to remove `-Werror` in CMakeLists.txt and Meta/Lagom/CMakeLIsts.txt.
