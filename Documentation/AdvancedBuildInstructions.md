# Advanced Build Instructions

This file covers a few advanced scenarios that go beyond what the basic build guide provides.

## Customizing the disk image

To add, modify or remove files of the disk image's file system, e.g. to change the default keyboard layout, you can create a shell script with the name `sync-local.sh` in the project root, with content like this:

```sh
#!/bin/sh

set -e

cat << 'EOF' > mnt/etc/Keyboard.ini
[Mapping]
Keymap=de
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

## CMake build options

There are some optional features that can be enabled during compilation that are intended to help with specific types of development work or introduce experimental features. Currently, the following build options are available:
- `ENABLE_ADDRESS_SANITIZER` and `ENABLE_KERNEL_ADDRESS_SANITIZER`: builds in runtime checks for memory corruption bugs (like buffer overflows and memory leaks) in Lagom test cases and the kernel, respectively.
- `ENABLE_KERNEL_COVERAGE_COLLECTION`: enables the KCOV API and kernel coverage collection instrumentation. Only useful for coverage guided kernel fuzzing.
- `ENABLE_MEMORY_SANITIZER`: enables runtime checks for uninitialized memory accesses in Lagom test cases.
- `ENABLE_UNDEFINED_SANITIZER`: builds in runtime checks for [undefined behavior](https://en.wikipedia.org/wiki/Undefined_behavior) (like null pointer dereferences and signed integer overflows) in Lagom test cases.
- `ENABLE_FUZZER_SANITIZER`: builds [fuzzers](https://en.wikipedia.org/wiki/Fuzzing) for various parts of the system.
- `ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS`: sets -Og and -ggdb3 compile options for building the Kernel. Allows for easier debugging of Kernel code. By default, the Kernel is built with -Os instead.
- `ENABLE_ALL_THE_DEBUG_MACROS`: used for checking whether debug code compiles on CI. This should not be set normally, as it clutters the console output and makes the system run very slowly. Instead, enable only the needed debug macros, as described below.
- `ENABLE_ALL_DEBUG_FACILITIES`: used for checking whether debug code compiles on CI. Enables both `ENABLE_ALL_THE_DEBUG_MACROS` and `ENABLE_EXTRA_KERNEL_DEBUG_SYMBOLS`.
- `ENABLE_COMPILETIME_FORMAT_CHECK`: checks for the validity of `std::format`-style format string during compilation. Enabled by default.
- `ENABLE_PCI_IDS_DOWNLOAD`: downloads the [`pci.ids` database](https://pci-ids.ucw.cz/) that contains information about PCI devices at build time, if not already present. Enabled by default.
- `BUILD_LAGOM`: builds [Lagom](../Meta/Lagom/ReadMe.md), which makes various SerenityOS libraries and programs available on the host system.
- `ENABLE_KERNEL_LTO`: builds the kernel with link-time optimization.
- `INCLUDE_WASM_SPEC_TESTS`: downloads and includes the WebAssembly spec testsuite tests. In order to use this option, you will need to install `prettier` and `wabt`. wabt version 1.0.23 or higher is required to pre-process the WebAssembly spec testsuite.
- `USE_CLANG_TOOLCHAIN`: uses the alternative Clang-based toolchain for building SerenityOS instead of the established GCC-based one. See the [Clang-based toolchain](#clang-based-toolchain) section below.
- `BUILD_<component>`: builds the specified component, e.g. `BUILD_HEARTS` (note: must be all caps). Check the components.ini file in your build directory for a list of available components. Make sure to run `ninja clean` and `rm -rf Build/i686/Root` after disabling components. These options can be easily configured by using the `ConfigureComponents` utility. See the [Component Configuration](#component-configuration) section below.
- `BUILD_EVERYTHING`: builds all optional components, overrides other `BUILD_<component>` flags when enabled

Many parts of the SerenityOS codebase have debug functionality, mostly consisting of additional messages printed to the debug console. This is done via the `<component_name>_DEBUG` macros, which can be enabled individually at build time. They are listed in [this file](../Meta/CMake/all_the_debug_macros.cmake).

To toggle a build option, add it to the `cmake` command invocation with a `-D` prefix. To enable it, add `=ON` at the end, or add `=OFF` to disable it. The complete command should look similarly to this:

```console
$ cmake ../.. -G Ninja -DPROCESS_DEBUG=ON -DENABLE_PCI_IDS_DOWNLOAD=OFF
```

For the changes to take effect, SerenityOS needs to be recompiled and the disk image needs to be rebuilt.

## Component Configuration

For selecting which components of the system to build and install, a helper program, `ConfigureComponents` is available.

It requires `whiptail` as a dependency, which is available on most systems in the `newt` or `libnewt` package. To build and run it, run the following commands from the `Build/i686` directory:
```console
$ cmake ../.. -G Ninja        # Only required if CMake hasn't been run before.
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

Note that `Meta/serenity.sh` is not yet supported, so the appropriate `ninja` targets (`install`, `image` and `run`)
need to be invoked manually.

To build the Clang-based toolchain, run `BuildClang.sh` from the `Toolchain` directory. The script defaults to building
the tooling needed for 32-bit SerenityOS, but the `ARCH=x86_64` environment variable can be set to build the 64-bit
toolchain.

**Warning:** While the build script is running, your computer may slow down extremely or even lock up for short
intervals.  This generally happens if you have more CPU cores than free RAM in gigabytes. To fix this, limit the number
of parallel compile tasks be setting the `MAKEJOBS` environment variable to a number less than your CPU core count.

Once the build script finishes, you can use it to compile SerenityOS if you enable the `USE_CLANG_TOOLCHAIN` build
option as shown [above](#cmake-build-options).
