# Troubleshooting

In case of an error, you might find an answer of how to deal it here.

## Building SerenityOS

### CMake fails to configure the build because it's outdated

Ensure your CMake version is >= 3.16 with `cmake --version`. If your system doesn't provide a suitable
version of CMake, you can download a binary release from the [CMake website](https://cmake.org/download).

### QEMU is missing or is outdated

Ensure your [QEMU](https://www.qemu.org/) version is >= 5 with `qemu-system-i386 -version`. Otherwise,
install it. You can also build it using the `Toolchain/BuildQemu.sh` script.

### GCC is missing or is outdated

Ensure your gcc version is >= 11 with `gcc --version`. Otherwise, install it. If your gcc binary is not
called `gcc` you have to specify the names of your C and C++ compiler when you run cmake, e.g.
`cmake ../.. -GNinja -DCMAKE_C_COMPILER=gcc-11 -DCMAKE_CXX_COMPILER=g++-11`.

## Running SerenityOS

### The VM is really slow

On Linux, QEMU is significantly faster if it's able to use KVM. The run script will automatically enable KVM
if `/dev/kvm` exists and is readable+writable by the current user. On Windows, ensure that you have
WHPX acceleration enabled.

### Slow boot on HiDPI systems

On some Windows systems running with >100% scaling, the booting phase of Serenity might slow to a crawl. Changing the
zoom settings of the QEMU window will speed up the emulation, but you'll have to squint harder to read the smaller display.

The default display backend (`SERENITY_QEMU_DISPLAY_BACKEND=sdl,gl=off`) does _not_ have this problem. If you're
running into this problem, make sure you haven't changed the QEMU display backend.

A quick workaround is opening the properties of the QEMU executable at `C:\Program Files\qemu\qemu-system-x86_64.exe`, and
in the Compatibility tab changing the DPI settings to force the scaling to be performed by the System, by changing the
setting at at the bottom of the window. The QEMU window will now render at normal size while retaining acceptable emulation speeds.

This is being tracked as issue [#7657](https://github.com/SerenityOS/serenity/issues/7657).

### Boot fails with "Error: Kernel Image too big for memory slot. Halting!"

This means the kernel is too large again. Contact us on the discord server or open a GitHub Issue about it.
You might want to revert latest changes in tree to see if that solves the problem temporarily.

### Boot fails with "Your computer does not support long mode (64-bit mode). Halting!"

Either your machine (if you try to boot on bare metal) is very old, thus it's not supporting x86_64
extensions, or you try to use VirtualBox without using a x64 virtualization mode or you try to use
`qemu-system-i386` which doesn't support x86_64 extensions too.

### Boot fails with "Your computer does not support PAE. Halting!"

- If booting on bare metal, your CPU is too old to boot Serenity.
- If you're using VirtualBox, you need to enable PAE/NX. Check the instructions [here.](VirtualBox.md)
- If you're using QEMU, the [CPU model configuration](https://qemu-project.gitlab.io/qemu/system/qemu-cpu-models.html) is not exposing PAE.
