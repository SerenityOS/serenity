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
  
## Hardware acceleration on macOS Big Sur

If you are on macOS Big Sur, you will need to manually enable QEMU's hardware acceleration before running Serenity, by
creating a new file called `entitlements.xml` in the `Build/` folder, with the content below, and then running this
command:

`codesign -s - --entitlements entitlements.xml --force $(which qemu-system-x86_64)`

<details>
<summary>Content for 'entitlements.xml'.</summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
	"http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>com.apple.security.hypervisor</key>
    <true/>
</dict>
</plist>
```

</details>

