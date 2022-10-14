# Library stubs

This directory contains stubs for SerenityOS libraries (LibC, LibM, LibDl, LibPthread)
that are referenced from the LLVM runtime libraries. These are needed by the linker
in order to add the required `DT_NEEDED` entries.

## Do these need to be updated?

Generally, no. LLVM does not use the header files to decide which functionality it can
use. After adding a new function to a header, you don't have to worry about LLVM
toolchain builds failing because the symbol is not present in the stubs.

## How to generate these?

First, you need to have a working SerenityOS installation that's been built by the
Clang toolchain. Then, using the `llvm-ifs` tool, these libraries need to be converted
into a stripped-down stub form. To do that, run the following command:

```sh
Toolchain/Local/clang/bin/llvm-ifs --output-format=ELF --output=<path-to-stub> <path-to-original>
```
