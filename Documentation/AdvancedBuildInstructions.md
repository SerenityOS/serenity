# Advanced Build Instructions

This file covers a few advanced scenarios that go beyond what the basic build guide provides.

## Customizing the disk image

To add, modify or remove files of the disk image's file system, e.g. to change the default keyboard layout, you can create a shell script with the name `sync-local.sh` in the project root, with content like this:

```sh
#!/bin/sh

set -e

cat << 'EOF' > mnt/etc/Keyboard.ini
[Mapping]
Keymaps=de
EOF

# Add a file in anon's home dir
cp /somewhere/on/your/system/file.txt mnt/home/anon
```


This will configure your keymap to German (`de`) instead of US English. See [`Base/res/keymaps/`](../Base/res/keymaps/) for a full list. Note that the `keymap` program itself will also modify the `/etc/Keyboard.ini` config file, but this way the change will persist across image rebuilds.

## Ninja build targets

The `Meta/serenity.sh` script provides an abstraction over the build targets which are made available by CMake. The
following build targets cannot be accessed through the script and have to be used directly by changing the current
directory to `Build/i686` and then running `ninja <target>`:

- `ninja grub-image`: Builds a disk image (`grub_disk_image`) with GRUB
- `ninja extlinux-image`: Builds a disk image (`extlinux_disk_image`) with extlinux
- `ninja check-style`: Runs the same linters the CI does to verify project style on changed files
- `ninja install-ports`: Copies the entire ports tree into the installed rootfs for building ports in Serenity
- `ninja lint-shell-scripts`: Checks style of shell scripts in the source tree with shellcheck
- `ninja all_generated`: Builds all generated code. Useful for running analysis tools that can use compile_commands.json without a full system build
- `ninja configure-components`: See the [Component Configuration](#component-configuration) section below.

## CMake build options

There are some optional features that can be enabled during compilation that are intended to help with specific types of development work or introduce experimental features. Currently, the following build options are available:
- `ENABLE_ADDRESS_SANITIZER` and `ENABLE_KERNEL_ADDRESS_SANITIZER`: builds in runtime checks for memory corruption bugs (like buffer overflows and memory leaks) in Lagom test cases and the kernel, respectively.
- `ENABLE_KERNEL_COVERAGE_COLLECTION`: enables the KCOV API and kernel coverage collection instrumentation. Only useful for coverage guided kernel fuzzing.
- `ENABLE_MEMORY_SANITIZER`: enables runtime checks for uninitialized memory accesses in Lagom test cases.
- `ENABLE_UNDEFINED_SANITIZER`: builds in runtime checks for [undefined behavior](https://en.wikipedia.org/wiki/Undefined_behavior) (like null pointer dereferences and signed integer overflows) in Lagom test cases.
- `ENABLE_COMPILER_EXPLORER_BUILD`: Skip building non-library entities in Lagom (this only applies to Lagom).
- `ENABLE_FUZZER_SANITIZER`: builds [fuzzers](https://en.wikipedia.org/wiki/Fuzzing) for various parts of the system.
- `ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS`: sets -Og and -ggdb3 compile options for building the Kernel. Allows for easier debugging of Kernel code. By default, the Kernel is built with -O2 instead.
- `ENABLE_ALL_THE_DEBUG_MACROS`: used for checking whether debug code compiles on CI. This should not be set normally, as it clutters the console output and makes the system run very slowly. Instead, enable only the needed debug macros, as described below.
- `ENABLE_ALL_DEBUG_FACILITIES`: used for checking whether debug code compiles on CI. Enables both `ENABLE_ALL_THE_DEBUG_MACROS` and `ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS`.
- `ENABLE_COMPILETIME_FORMAT_CHECK`: checks for the validity of `std::format`-style format string during compilation. Enabled by default.
- `ENABLE_PCI_IDS_DOWNLOAD`: downloads the [`pci.ids` database](https://pci-ids.ucw.cz/) that contains information about PCI devices at build time, if not already present. Enabled by default.
- `BUILD_LAGOM`: builds [Lagom](../Meta/Lagom/ReadMe.md), which makes various SerenityOS libraries and programs available on the host system.
- `ENABLE_KERNEL_LTO`: builds the kernel with link-time optimization.
- `ENABLE_MOLD_LINKER`: builds the userland with the [`mold` linker](https://github.com/rui314/mold). `mold` can be built by running `Toolchain/BuildMold.sh`.
- `INCLUDE_WASM_SPEC_TESTS`: downloads and includes the WebAssembly spec testsuite tests. In order to use this option, you will need to install `prettier` and `wabt`. wabt version 1.0.23 or higher is required to pre-process the WebAssembly spec testsuite.
- `SERENITY_TOOLCHAIN`: Specifies whether to use the established GNU toolchain, or the experimental Clang-based toolchain for building SerenityOS. See the [Clang-based toolchain](#clang-based-toolchain) section below.
- `SERENITY_ARCH`: Specifies which architecture to build for. Currently supported options are `i686` and `x86_64`. `x86_64` requires a separate toolchain build from `i686`.
- `BUILD_<component>`: builds the specified component, e.g. `BUILD_HEARTS` (note: must be all caps). Check the components.ini file in your build directory for a list of available components. Make sure to run `ninja clean` and `rm -rf Build/i686/Root` after disabling components. These options can be easily configured by using the `ConfigureComponents` utility. See the [Component Configuration](#component-configuration) section below.
- `BUILD_EVERYTHING`: builds all optional components, overrides other `BUILD_<component>` flags when enabled

Many parts of the SerenityOS codebase have debug functionality, mostly consisting of additional messages printed to the debug console. This is done via the `<component_name>_DEBUG` macros, which can be enabled individually at build time. They are listed in [this file](../Meta/CMake/all_the_debug_macros.cmake).

To toggle or change a build option, see the [CMake Cache Manipulation](#cmake-cache-manipulation) section below.

## CMake Cache Manipulation

CMake caches variables and options in the binary directory. This allows a developer to tailor variables that are `set()` within the persistent configuration cache.

There are three main ways to manipulate the cache:
- `cmake path/to/binary/dir -DVAR_NAME=Value`
- `ccmake` (TUI interface)
- `cmake-gui`

Options can be set via the initial `cmake` invocation that creates the binary directory to set the initial cache for the binary directory.
Once the binary directory exists, any of the three options above can be used to change the value of cache variables.

For example, boolean options such as `ENABLE_<setting>` or `<component_name>_DEBUG` can be enabled with the value `ON` and disabled with `OFF`:

```console
# Reconfigure an existing binary directory with process debug enabled
$ cmake -B Build/i686 -DPROCESS_DEBUG=ON
```

For more information on how the CMake cache works, see the CMake guide for [Running CMake](https://cmake.org/runningcmake/). Additional context is available in the CMake documentation for
[variables](https://cmake.org/cmake/help/latest/manual/cmake-language.7.html#variables) and [set()](https://cmake.org/cmake/help/latest/command/set.html#set-cache-entry).

## SuperBuild configuration

Serenity uses host tools written in idiomatic Serenity C++ to generate code and data for the main target build.
The "SuperBuild" pattern helps to separate the host build of core Serenity libraries from the target build of the
entire operating system environment. The SuperBuild allows clear separation of the host and target builds in the project's CMakeLists
and unifies the approach taken towards different compiler toolchains and architectures.

The recommended way to build and run the system, `./Meta/serenity.sh run`, invokes the SuperBuild equivalently to the commands below:

```console
$ cmake -GNinja -S Meta/CMake/Superbuild -B Build/superbuild-i686 -DSERENITY_ARCH=i686 -DSERENITY_TOOLCHAIN=GNU
$ cmake --build Build/superbuild-i686
$ ninja -C Build/i686 setup-and-run
```

The CMake configuration of the `superbuild-<arch>` directory configures two [ExternalProjects](https://cmake.org/cmake/help/latest/module/ExternalProject.html).
The first project is `lagom`, which is the host build of the project. For more information on Lagom, see the [Lagom ReadMe](../Meta/Lagom/ReadMe.md). It is used
to build all the code generators and other host tools needed for the main Serenity build. The second project is the main build, which compiles the system for the
target architecture using the selected toolchain.

The `superbuild-<arch>` configuration also generates the [CMake toolchain file](https://cmake.org/cmake/help/latest/manual/cmake-toolchains.7.html#cross-compiling)
for the selected compiler toolchain and architecture via the `-DSERENITY_ARCH` and `-DSERENITY_TOOLCHAIN` arguments to the SuperBuild configuration step.
The Serenity project depends on the install step of the Lagom build, as it uses [find_package](https://cmake.org/cmake/help/latest/command/find_package.html) to locate
the host tools for use in the code generation custom commands.

The SuperBuild build steps are roughly equivalent to the following commands:

```console
# Generate CMakeToolchain.txt
mkdir -p Build/i686
cp Toolchain/CMake/GNUToolchain.txt.in Build/i686/CMakeToolchain.txt
sed -i 's/@SERENITY_ARCH@/i686/g' Build/i686/CMakeToolchain.txt
sed -i 's/@SERENITY_SOURCE_DIR@/'"$PWD"'/g' Build/i686/CMakeToolchain.txt
sed -i 's/@SERENITY_BUILD_DIR@/'"$PWD"'\/Build\/i686/g' Build/i686/CMakeToolchain.txt

# Configure and install Lagom
cmake -GNinja -S Meta/Lagom -B Build/lagom -DCMAKE_INSTALL_PREFIX=${PWD}/Build/lagom-install
ninja -C Build/lagom install
# Configure and install Serenity, pointing it to Lagom's install prefix
cmake -GNinja -B Build/i686 -DCMAKE_PREFIX_PATH=${PWD}/Build/lagom-install -DSERENITY_ARCH=i686 -DCMAKE_TOOLCHAIN_FILE=${PWD}/Build/i686/CMakeToolchain.txt
ninja -C Build/i686 install
```

Directing future `ninja` or `cmake --build` invocations to the `superbuild-<arch>` directory ensures that any headers or cpp files shared between the
host and target builds will be rebuilt, and the new host tools and libraries will be staged to the lagom-install directory. This is where the superbuild
differs from manually entering the commands above, it establishes a dependency between the install stage of lagom and the configure/build stages of Serenity.

The main limitation of the SuperBuild is that any non-option CMake cache variables such as component configuration or debug flag settings must be done
after a build has started. That is, the CMakeCache.txt for the Serenity and Lagom builds is not created until the SuperBuild build starts and reaches the
proper stage for the build in question. For more information on the CMake cache see the [CMake Cache Manipulation](#cmake-cache-manipulation) section above.

The debug flags might be manipulated after a build per the following commands:

```console
# Initial build, generate binary directories for both child builds
$ cmake -GNinja -S Meta/CMake/Superbuild -B Build/superbuild-i686 -DSERENITY_ARCH=i686 -DSERENITY_TOOLCHAIN=GNU
$ cmake --build Build/superbuild-i686

# Turn on process debug and don't build the browser for the Serenity build
$ cmake -B Build/i686 -DPROCESS_DEBUG=ON -DBUILD_BROWSER=OFF
$ ninja -C Build/i686 install

# Build host tests in Lagom build
$ cmake -S Meta/Lagom -B Build/lagom -DBUILD_LAGOM=ON
$ ninja -C Build/lagom install
```

## Component Configuration

For selecting which components of the system to build and install, a helper program, `ConfigureComponents` is available.

It requires `whiptail` as a dependency, which is available on most systems in the `newt` or `libnewt` package. To build and run it, run the following commands from the `Build/i686` directory:
```console
$ ninja configure-components
```

This will prompt you which build type you want to use and allows you to customize it by manually adding or removing certain components. It will then run a CMake command based on the selection as well as `ninja clean` and `rm -rf Root` to remove old build artifacts.

## Tests

For information on running host and target tests, see [Running Tests](RunningTests.md). The documentation there explains the difference between host tests run with Lagom and
target tests run on SerenityOS. It also contains useful information for debugging CI test failures.

## Running SerenityOS with VirtualBox and VMware

Outside of QEMU, Serenity will run on VirtualBox and VMware. If you're curious, see how to [install Serenity on VirtualBox](VirtualBox.md) or [install Serenity on VMware](VMware.md).

## Running SerenityOS on bare metal

Bare curious users may even consider sourcing suitable hardware to [install Serenity on a physical PC.](BareMetalInstallation.md)

## Filesystem performance on Windows

If you're using the native Windows QEMU binary, QEMU is not able to access the ext4 root partition
of the WSL2 installation without going via the 9P network file share. The root of your WSL2 distro will begin at the
network path `\\wsl$\{distro-name}`.

Alternatively, you may prefer to copy `Build/_disk_image` and `Build/Kernel/Kernel` to a native Windows partition (e.g.
`/mnt/c`) before running `ninja run`, in which case `SERENITY_DISK_IMAGE` will be a regular Windows path (e.g.
`'D:\serenity\_disk_image'`).

## Clang-based toolchain

SerenityOS can also be built with the [Clang](https://en.wikipedia.org/wiki/Clang) compiler instead of GCC. This is
useful for stopping us from relying on compiler-specific behavior, and the built-in static analyzer helps us catch more
bugs.  Code compiled with both of these toolchains works identically in most cases, with the limitation that ports
can't be built with Clang yet.

To build the Clang-based toolchain, run `BuildClang.sh` from the `Toolchain` directory. The script defaults to building
the tooling needed for 32-bit SerenityOS, but the `ARCH=x86_64` environment variable can be set to build the 64-bit
toolchain.

**Warning:** While the build script is running, your computer may slow down extremely or even lock up for short
intervals.  This generally happens if you have more CPU cores than free RAM in gigabytes. To fix this, limit the number
of parallel compile tasks be setting the `MAKEJOBS` environment variable to a number less than your CPU core count.

Once the build script finishes, you can use it to compile SerenityOS. Either set the `SERENITY_TOOLCHAIN` build
option to `Clang` as shown [above](#cmake-build-options), or pass `Clang` as the TOOLCHAIN option to
`Meta/serenity.sh`, for example: `Meta/serenity.sh run i686 Clang`.
