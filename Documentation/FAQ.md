# Frequently Asked Questions about SerenityOS

## Will SerenityOS support `$THING`?

Maybe. Maybe not. There is no plan.

## When will you implement `$THING`?

Maybe someday. Maybe never. If you want to see something happen, you can do it yourself!

## Where are the ISO images?

There are no ISO images. This project does not cater to non-technical users.

## Well, how do I run this thing then?

Simple, my friend! Just refer to the [build instructions](BuildInstructions.md).

## I did a `git pull` and now the build is broken! What do I do?

If it builds on CI, it should build for you too. You may need to rebuild the toolchain. If that doesn't help, try it with a clean repo.

If you can't figure out what to do, ask in the `#build-problems` channel on Discord.

## Why don't you use `$LIBRARY` instead of implementing `$FEATURE` yourself?

The SerenityOS project tries to maximize hackability, accountability, and fun(!) by implementing everything ourselves.

## Does SerenityOS have a package manager?

In short, no, SerenityOS does not have a "Linux-style" package manager with pre-built software.

More specifically, SerenityOS does not have a package manager that allows you to download pre-built binary images software. The project uses a monorepo approach, meaning that all software is built in the same style and using the same tools. Additionally, the SerenityOS ABI, such as library symbols or syscall interfaces, have absolutely no stability guarantee and should be expected to change at any moment. (You can expect the POSIX C library API to remain relatively stable.) There is no reason to have a binary package manager because of this.

The supported method to use third-party software is by compiling ports that can be found in the [Ports directory](../Ports). A port is a piece of software that can optionally be installed, might not have been built by us, but supports running on SerenityOS. They act quite similarly to packages, each coming with their own install script. The significant difference is that ports are always built from source.

Currently when running the system in a virtual machine, ports should be cross compiled on the host, and added to the file system image before booting. It is possible to compile ports on SerenityOS itself, which requires quite a bit of manual work. At the moment, this is not a recommended or actively supported workflow. In the future, this process _may_ be supported more easily with the `pkg` tool. If you are interested in contributing in this regard, take a look at the #package-manager channel on Discord.

In regards to SerenityOS components themselves, it is possible to [exclude some of them](./AdvancedBuildInstructions.md#component-configuration) from the build at compile time.

## Why is there an MP3 implementation in SerenityOS if MP3 is protected by patents?

_This section is informational; we are not lawyers and this is not legal advice. Consult a qualified lawyer if you have legal questions regarding your use of SerenityOS software._

MP3 was indeed originally protected by patents, preventing certain uses of the format. All MP3 patents, however, have expired since at least 2017, depending on where a specific patent was registered. Therefore, we believe it to be completely legal to implement MP3 as 2-clause BSD licensed software without acquiring patent licenses.

_However_, this does not apply to many other multimedia formats, such as the popular H.264 (AVC) and H.265 (HEVC) video codecs or the JPEG 2000 image format. As long as there is any reason to believe that a format is covered by patents, there will not be an implementation in the SerenityOS monorepo, as we believe this to be incompatible with the BSD 2-clause license in general. _However however_, third-party ports with differing licenses can provide implementations for these formats, such as ffmpeg. Depending on your situation and/or use case, using this third-party software might not be legal (for example, see the [ffmpeg information on the same topic](https://ffmpeg.org/legal.html)). Everything regarding the legal situation of the SerenityOS code is handled by [our license](../LICENSE), everything regarding the legal situation of third-party code is handled by the license of the particular software.
