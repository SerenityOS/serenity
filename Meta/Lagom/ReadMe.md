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
    mkdir BuildLagom && cd BuildLagom
    cmake -GNinja -DBUILD_LAGOM=ON -DENABLE_FUZZER_SANITIZER=ON -DENABLE_ADDRESS_SANITIZER=ON -DCMAKE_CXX_COMPILER=clang++ ..
    ninja Meta/Lagom/all
    # Or as a handy rebuild-rerun line:
    ninja FuzzJs && Meta/Lagom/Fuzzers/FuzzJs

Any fuzzing results (particularly slow inputs, crashes, etc.) will be dropped in the current directory.

clang emits different warnings than gcc, so you may have to remove `-Werror` in CMakeLists.txt and Meta/Lagom/CMakeLists.txt.

Fuzzers work better if you give them a fuzz corpus, e.g. `Meta/Lagom/Fuzzers/FuzzBMP ../Base/res/html/misc/bmpsuite_files/rgba32-61754.bmp` Pay attention that LLVM also likes creating new files, don't blindly commit them (yet)!

To run several fuzz jobs in parallel, pass `-jobs=24 -workers=24`.

To get less log output, pass `-close_fd_mask=3` -- but that but hides assertion messages. Just `1` only closes stdout.
It's good to move overzealous log output behind `FOO_DEBUG` macros.

### Keeping track of interesting testcases

There are many quirky files that exercise a lot of interesting edge cases.
We should probably keep track of them, somewhere.

We have a [bmp suite and a jpg suite and several others](https://github.com/SerenityOS/serenity/tree/master/Base/res/html/misc).
They are GPL'ed, and therefore not quite as compatible with the rest of Serenity.
That's probably not a problem, but keeping "our" testcases separate from those GPL'ed suits sounds like a good idea.

We could keep those testcases somewhere else in the repository, like [a `fuzz` directory](https://github.com/SerenityOS/serenity/tree/master/Base/res/html/misc/jpgsuite_files/fuzz).
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

### Analyzing a crash

LLVM fuzzers have a weird interface. In particular, to see the help, you need to call it with `-help=1`, and it will ignore `--help` and `-help`.

To reproduce a crash, run it like this: `MyFuzzer crash-27480a219572aa5a11b285968a3632a4cf25388e`

To reproduce a crash in gdb, you want to disable various signal handlers, so that gdb sees the actual location of the crash:

```
$ gdb ./Meta/Lagom/Fuzzers/FuzzBMP
<... SNIP some output ...>
(gdb) run -handle_abrt=0 -handle_segv=0 crash-27480a219572aa5a11b285968a3632a4cf25388e
<... SNIP some output ...>
FuzzBMP: ../../Userland/Libraries/LibGfx/Bitmap.cpp:84: Gfx::Bitmap::Bitmap(Gfx::BitmapFormat, const Gfx::IntSize &, Gfx::Bitmap::Purgeable): Assertion `m_data && m_data != (void*)-1' failed.

Thread 1 "FuzzBMP" received signal SIGABRT, Aborted.
__GI_raise (sig=sig@entry=6) at ../sysdeps/unix/sysv/linux/raise.c:50
50	../sysdeps/unix/sysv/linux/raise.c: File or directory not found.
(gdb)
```
