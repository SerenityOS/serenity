# Setting up a development environment on macOS

# Prerequisites

This installation guide assumes that you have Homebrew, Xcode and `xcode-tools` installed.

Make sure you also have all the following dependencies installed:

```console
# core
brew install coreutils e2fsprogs qemu bash gcc@11 imagemagick ninja cmake ccache rsync

# (option 1) fuse + ext2
brew install m4 autoconf automake libtool
brew install --cask osxfuse
Toolchain/BuildFuseExt2.sh

# (option 2) genext2fs
brew install genext2fs
```

Notes:

- Installing osxfuse for the first time requires enabling its system extension in System Preferences and then restarting
  your machine. The output from installing osxfuse with brew says this, but it's easy to miss.
