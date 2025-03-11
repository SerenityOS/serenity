# Configuring the clangd Language Server

clangd is recommended as a C++ language server in general, as it is most likely to work on all platforms.

CLion ships with a special build of clangd that usually works fine. Most other editors can be configured to use any build of clangd via the LSP or dedicated extensions/plugins. Refer to the editor-specific documentation on how to install and enable such extensions.

## `.clangd`

clangd uses `compile_commands.json` files to understand the project. CMake will generate these in directories such as Build/x86_64, Build/x86_64clang, and Build/lagom.
Depending on which configuration (architecture and toolchain) you use most, set the CompilationDatabase configuration item in the below `.clangd` file accordingly.

The recommended `.clangd` file (at the root of your Serenity checkout) is as follows; it should work out of the box and can be adjusted as needed:

```yaml
CompileFlags:
    # Add compilation flags to remove errors, or to make clangd behave like you’re compiling a specific system configuration.
    Add: []
    # You can remove unwanted flags such as those that aren't supported by the current version of clang.
    Remove: []
    # Build/x86_64 is also possible if you don’t have the Clang toolchain, but doesn’t work as well.
    CompilationDatabase: Build/x86_64clang

Style:
    # clangd 20+: Use correct include style.
    AngledHeaders: ["AK/.*", "Userland/.*", "Kernel/.*", "Applications/.*", "Lib.*/.*"]

Diagnostics:
    UnusedIncludes: Strict
    MissingIncludes: Loose
```

The UnusedIncludes and MissingIncludes flags are used to disable the [Include Cleaner](https://clangd.llvm.org/design/include-cleaner) feature of newer clangd releases.
It can be re-enabled if you don't mind the noisy inlay hints and problems.

Adding and removing compiler flags can fix many bugs, but also be useful for better code comprehension. Here are some tips:

-   Add `-D__serenity__` if you are not using the Serenity toolchain clangd, so that clangd treats all code as being compiled for Serenity and not for your host system.
-   Add the flags `-DKERNEL` or `-DPREKERNEL` if you want clangd to behave like the Kernel or Prekernel compilation, respectively.
-   When using a GCC compilation database (e.g. Build/x86_64), clangd will frequently complain about command-line arguments at the top of files. For instance: `clang: Unknown argument: '-mpreferred-stack-boundary=3'`. To fix this, simply add `-mno-<flag name>` to the `Add` list. In the example, that would be `-mno-preferred-stack-boundary`.
-   There are also known issues with the GCC compilation database for the Kernel, here `-mno-sse` and `-mno-8087` may help.

Run `./Meta/serenity.sh run` at least once to generate the `compile_commands.json` file. Every time a new source is added or the compilation commands get adjusted (through CMake) you need to rerun `./Meta/serenity.sh build` or any other build command in order to update the compile commands. Until you do so, clangd will not be aware of the new source or report incorrect compile errors.

### clangd Command-line Arguments

When using the system clangd, it has to be configured to find the cross-compiler’s built-in include paths. Otherwise, there will always be errors like “file \<new\> not found”. The clangd command-line flag for this is `--query-driver=SERENITY_PATH/Toolchain/Local/**/*` where you have to replace SERENITY_PATH with the Serenity source directory. Some editors have placeholders you can use here (e.g. VSCode’s `${workspaceFolder}`).

For clangd 19 and below, `--header-insertion=never` is strongly recommended, as this prevents clangd from inserting incorrectly-styled includes. See the [corresponding clangd issue](https://github.com/clangd/clangd/issues/1247). From clangd 20 onwards, this option is not needed anymore and replaced by the `AngledHeaders` directive that configures clangd’s include style correctly.

## Using Serenity Toolchain clangd

The LLVM/Clang toolchain for Serenity is capable of building a version of clangd that is aware of the Serenity targets and their configuration. In general, it is recommended to use this clangd, as it will additionally always be up-to-date. It does, however, require you to build the Clang toolchain. See [AdvancedBuildInstructions](AdvancedBuildInstructions.md#serenity-aware-clang-tools) for how to build this clangd. Then, configure your editor to use the clangd executable situated at `Toolchain/Local/clang/bin/clangd`.

## Known Issues

-   Some distribution clangd packages still have issues identifying paths to the serenity cross-compilers' builtin include paths after supplying the `--query-driver` option. This has been seen on at least Debian. If the inlay hints suggest that `<new>` cannot be found, first triple check your configuration matches the `.clangd` file from above, verify that you've run the OS via `Meta/serenity.sh run`, and quadruple check your clangd command-line arguments. If all of the above are correct, building `clangd` from the serenity clang toolchain is known to work.
-   clangd has a tendency to crash when stressing bleeding edge compiler features. Sometimes, just opening `AK/Variant.h` is enough. You can usually just restart clangd. If that doesn't help, close currently open C++ files and/or switch branches before restarting, which helps sometimes.
