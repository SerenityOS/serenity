# Lagom

The Serenity C++ library, for other Operating Systems.

## About

If you want to bring the comfortable Serenity classes with you to another system, look no further. This is basically a "port" of the `AK` and `LibCore` libraries to generic \*nix systems.

*Lagom* is a Swedish word that means "just the right amount." ([Wikipedia](https://en.wikipedia.org/wiki/Lagom))

## Fuzzing

Lagom can be used to fuzz parts of SerenityOS's code base. This requires buildling with `clang`, so it's convenient to use a different build directory for that. Run CMake like this:

    # From the root of the SerenityOS checkout:
    mkdir BuildLagom && cd BuildLagom
    cmake -GNinja -DBUILD_LAGOM=ON -DENABLE_FUZZER_SANITIZER=ON -DCMAKE_C_COMPILER=clang -DCMAKE_CXX_COMPILER=clang++ ..
    ninja Meta/Lagom/all
    # Or as a handy rebuild-rerun line:
    ninja FuzzJs && Meta/Lagom/Fuzzers/FuzzJs

Any fuzzing results (particularly slow inputs, crashes, etc.) will be dropped in the current directory.

clang emits different warnings than gcc, so you may have to remove `-Werror` in CMakeLists.txt and Meta/Lagom/CMakeLists.txt.

Fuzzers work better if you give them a fuzz corpus, e.g. `Meta/Lagom/Fuzzers/FuzzBMP ../Base/res/html/misc/bmpsuite_files/rgba32-61754.bmp` Pay attention that LLVM also likes creating new files, don't blindly commit them (yet)!

### Analyzing a crash

LLVM fuzzers have a weird interface. In particular, to see the help, you need to call it with `-help=1`, and it will ignore `--help` and `-help`.

To reproduce a crash, run it like this: `MyFuzzer crash-27480a219572aa5a11b285968a3632a4cf25388e`

To reproduce a crash in gdb, you want to disable various signal handlers, so that gdb sees the actual location of the crash:

```
$ gdb ./Meta/Lagom/Fuzzers/FuzzBMP
<... SNIP some output ...>
(gdb) run -handle_abrt=0 -handle_segv=0 crash-27480a219572aa5a11b285968a3632a4cf25388e
<... SNIP some output ...>
FuzzBMP: ../../Libraries/LibGfx/Bitmap.cpp:84: Gfx::Bitmap::Bitmap(Gfx::BitmapFormat, const Gfx::IntSize &, Gfx::Bitmap::Purgeable): Assertion `m_data && m_data != (void*)-1' failed.

Thread 1 "FuzzBMP" received signal SIGABRT, Aborted.
__GI_raise (sig=sig@entry=6) at ../sysdeps/unix/sysv/linux/raise.c:50
50	../sysdeps/unix/sysv/linux/raise.c: File or directory not found.
(gdb)
```
