# Lagom

The Serenity C++ library, for other Operating Systems.

## About

If you want to bring the comfortable Serenity classes with you to another system, look no further. This is basically a "port" of the `AK` and `LibCore` libraries to generic \*nix systems.

*Lagom* is a Swedish word that means "just the right amount." ([Wikipedia](https://en.wikipedia.org/wiki/Lagom))

## Fuzzing

Lagom can be used to fuzz parts of SerenityOS's code base. Fuzzers can be run locally, and they also run continuously on OSS-Fuzz.

### Fuzzing locally

Lagom can be used to fuzz parts of SerenityOS's code base. This requires buildling with `clang`, so it's convenient to use a different build directory for that. Fuzzers work best with Address Sanitizer enabled. Run CMake like this:

    # From the root of the SerenityOS checkout:
    cmake -GNinja -S Meta/Lagom -B Build/lagom-fuzzers \
      -DBUILD_LAGOM=ON \
      -DENABLE_FUZZER_SANITIZER=ON \
      -DENABLE_ADDRESS_SANITIZER=ON \
      -DENABLE_UNDEFINED_SANITIZER=ON \
      -DCMAKE_CXX_COMPILER=clang++ \
      -DCMAKE_C_COMPILER=clang
    cd Build/lagom-fuzzers
    ninja
    # Or as a handy rebuild-rerun line:
    ninja FuzzJs && ./Fuzzers/FuzzJs

(Note that we require clang >= 12, so depending on your package manager you may need to specify `clang++-12` and `clang-12` instead.)

Any fuzzing results (particularly slow inputs, crashes, etc.) will be dropped in the current directory.

clang emits different warnings than gcc, so you may have to remove `-Werror` in CMakeLists.txt and Meta/Lagom/CMakeLists.txt.

Fuzzers work better if you give them a fuzz corpus, e.g. `./Fuzzers/FuzzBMPLoader ../Base/res/html/misc/bmpsuite_files/rgba32-61754.bmp` Pay attention that LLVM also likes creating new files, don't blindly commit them (yet)!

To run several fuzz jobs in parallel, pass `-jobs=24 -workers=24`.

To get less log output, pass `-close_fd_mask=3` -- but that but hides assertion messages. Just `1` only closes stdout.
It's good to move overzealous log output behind `FOO_DEBUG` macros.

### Keeping track of interesting testcases

There are many quirky files that exercise a lot of interesting edge cases.
We should probably keep track of them, somewhere.

We have a [bmp suite and a jpg suite and several others](https://github.com/SerenityOS/serenity/tree/master/Base/res/html/misc).
They are GPL'ed, and therefore not quite as compatible with the rest of Serenity.
That's probably not a problem, but keeping "our" testcases separate from those GPL'ed suits sounds like a good idea.

We could keep those testcases somewhere else in the repository, like a `fuzz` directory.
But fuzzing tends to generate more and more and more files, and they will blow up in size.
Especially if we keep all interesting testcases, which is exactly what I intend to do.

So we should keep the actual testcases out of the main serenity repo,
that's why we created https://github.com/SerenityOS/serenity-fuzz-corpora

Feel free to upload lots and lots files there, or use them for great good!

### Fuzzing on OSS-Fuzz

https://oss-fuzz.com/ automatically runs all fuzzers in the Fuzzers/ subdirectory whose name starts with "Fuzz" and which are added to the build in `Fuzzers/CMakeLists.txt` if `ENABLE_OSS_FUZZ` is set. Looking for "serenity" on oss-fuzz.com finds interesting links, in particular:

* [known open bugs found by fuzzers](https://oss-fuzz.com/testcases?project=serenity&open=yes)
  * [oss-fuzz bug tracker for these](https://bugs.chromium.org/p/oss-fuzz/issues/list?sort=-opened&can=1&q=proj:serenity)
* [coverage report](https://oss-fuzz.com/coverage-report/job/libfuzzer_asan_serenity/latest)
* [build logs](https://oss-fuzz-build-logs.storage.googleapis.com/index.html#serenity)

Here's [Serenity's OSS-Fuzz Config](https://github.com/google/oss-fuzz/tree/master/projects/serenity).

To run the oss-fuzz build locally:

```
git clone https://github.com/google/oss-fuzz/
cd oss-fuzz
python3 infra/helper.py build_image serenity
python3 infra/helper.py build_fuzzers serenity
```

These commands will put the fuzzers in `build/out/serenity` in the oss-fuzz repo. You can run the binaries in there individually, or simply type:

```
python3 infra/helper.py run_fuzzer serenity FUZZER_NAME
```

To build the fuzzers using the oss-fuzz build process, but against a local serenity checkout:

```
python3 infra/helper.py build_fuzzers serenity $HOME/src/serenity/
```

To run a shell in oss-fuzz's serenity docker image:

```
docker run -it gcr.io/oss-fuzz/serenity bash
```

### Analyzing a crash

LLVM fuzzers have a weird interface. In particular, to see the help, you need to call it with `-help=1`, and it will ignore `--help` and `-help`.

To reproduce a crash, run it like this: `MyFuzzer crash-27480a219572aa5a11b285968a3632a4cf25388e`

To reproduce a crash in gdb, you want to disable various signal handlers, so that gdb sees the actual location of the crash:

```
$ gdb ./Fuzzers/FuzzBMP
<... SNIP some output ...>
(gdb) run -handle_abrt=0 -handle_segv=0 crash-27480a219572aa5a11b285968a3632a4cf25388e
<... SNIP some output ...>
FuzzBMP: ../../Userland/Libraries/LibGfx/Bitmap.cpp:84: Gfx::Bitmap::Bitmap(Gfx::BitmapFormat, const Gfx::IntSize &, Gfx::Bitmap::Purgeable): Assertion `m_data && m_data != (void*)-1' failed.

Thread 1 "FuzzBMP" received signal SIGABRT, Aborted.
__GI_raise (sig=sig@entry=6) at ../sysdeps/unix/sysv/linux/raise.c:50
50	../sysdeps/unix/sysv/linux/raise.c: File or directory not found.
(gdb)
```

UBSan doesn't always give useful information. use something like `export UBSAN_OPTIONS=print_stacktrace=1` to always print stacktraces.

You may run into annoying issues with the stacktrace:

```
==123456==WARNING: invalid path to external symbolizer!
==123456==WARNING: Failed to use and restart external symbolizer!
```

That means it couldn't find the executable `llvm-symbolizer`, which could be in your OS's package `llvm`.
`llvm-symbolizer-11` will [not be recognized](https://stackoverflow.com/a/42845444/).
