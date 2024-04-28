# Library stubs

This directory contains stubs for SerenityOS's LibC that are referenced from the LLVM runtime
libraries. These are needed by the linker in order to add the required `DT_NEEDED` entries and to
not emit errors regarding undefined libc symbols. Additionally, it provides fake empty libunwind.so
and libc++.so for CMake configuration checks to succeed when bootstrapping the OS.

## Do these need to be updated?

Most likely no but it depends. Either way, if you are reading this, you are probably qualified
enough to figure out if a failing LLVM toolchain configuration or build is caused by an out-of-date
LibC stub.

## How to generate LibC stub?

First, you need to compile the LLVM toolchain and the SerenityOS's LibC. This will be a bit awkward
(see discussion at https://github.com/SerenityOS/serenity/pull/23960) until (unless) we solve the
dependency cycle between LibC and libunwind. Then, using the `llvm-ifs` tool,
`Userland/Libraries/LibC/libc.so` can be converted into a stripped-down stub form. To do that, run
the following command:

```sh
Toolchain/Local/clang/bin/llvm-ifs --output-elf=<path-to-stub> <path-to-original>
```

## How to generate `empty.so`?

Simple, my friend:

```sh
touch empty.cpp
Toolchain/Local/clang/bin/clang++ --target={arch}-pc-serenity -nostdlib -shared empty.cpp -o empty.so
# And optionally,
Toolchain/Local/clang/bin/llvm-strip empty.so
```
