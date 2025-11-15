# Setting up a development environment on macOS

# Prerequisites

This installation guide assumes that you have [Homebrew](https://brew.sh) and either Xcode or the Xcode Command Line Tools installed.

**Note:** If you have the full Xcode application installed, you should set your command line tools to use Xcode's tools instead of Homebrew's:

```console
sudo xcode-select --switch /Applications/Xcode.app
```

If you only have the Command Line Tools installed (which is sufficient for building SerenityOS), you can skip this step.

Make sure you also have all the following dependencies installed:

```console
# core
brew install coreutils e2fsprogs qemu bash imagemagick ninja cmake ccache rsync zstd

# (option 1) fuse + ext2
brew install m4 autoconf automake libtool
brew install --cask macfuse
Toolchain/BuildFuseExt2.sh

# (option 2) genext2fs
brew install genext2fs

# for kernel debugging, on Apple Silicon
brew install x86_64-elf-gdb
```

If you have Xcode version 14.2 or older, also install a newer host compiler from homebrew. Xcode 14.3 is known to work.

```console
brew install llvm@18
# OR
brew install gcc@13
```

# Nix

If Nix is installed, entering the devshell will provide an environment with all required build dependencies already included.

```
nix --extra-experimental-features "nix-command flakes" develop
```

# Notes

You can use both Intel and Apple Silicon Macs to run a native architecture version of SerenityOS.
If Rosetta 2 is installed on an Apple Silicon Mac, it should be disabled in order to build the native aarch64 version.

Installing macfuse for the first time requires enabling its system extension in System Settings (previously System Preferences on older macOS versions) and then restarting your machine. The output from installing macfuse with brew says this, but it's easy to miss.

Xcode or Xcode Command Line Tools are required for building. The Command Line Tools alone are sufficient - you don't need the full Xcode application. However, if you do have the full Xcode installed, make sure it's updated to avoid CMake compatibility issues with GCC.

Homebrew is known to ship bleeding edge CMake versions, but building CMake from source with homebrew
gcc or llvm may not work. If homebrew does not offer cmake 3.25.x+ on your platform, it may be necessary
to manually run Toolchain/BuildCMake.sh with Apple clang from Xcode as the first compiler in your $PATH.

If you want to debug the x86-64 kernel on an Apple Silicon machine, you can install the `x86_64-elf-gdb`
package to get a native build of GDB that can cross-debug x86-64 code.

## Next Steps

Once you have all dependencies installed, return to the main [BuildInstructions.md](BuildInstructions.md#build) to build and run SerenityOS.
