# Frequently Asked Questions about SerenityOS

## Will SerenityOS support `$THING`?

Maybe. Maybe not. There is no plan.

## When will you implement `$THING`?

Maybe someday. Maybe never. If you want to see something happen, you can do it yourself!

## Where are the ISO images?

There are no ISO images. This project does not cater to non-technical users.

## Well, how do I run this thing then?

Simple, my friend! Just refer to the [build instructions](BuildInstructions.md).

## Why is the system 32-bit?

The system was originally 32-bit only, since that's what Andreas was most familiar with when starting out. Nowadays, there is a 64-bit port as well (Intel/AMD x86\_64)

## I did a `git pull` and now the build is broken! What do I do?

If it builds on CI, it should build for you too. You may need to rebuild the toolchain. If that doesn't help, try it with a clean repo.

If you can't figure out what to do, ask in the `#build-problems` channel on Discord.

## Why don't you use `$LIBRARY` instead of implementing `$FEATURE` yourself?

The SerenityOS project tries to maximize hackability, accountability, and fun(!) by implementing everything ourselves.

## Does SerenityOS have a package manager?

No, SerenityOS does not have a package manager. The project uses a monorepo approach, meaning that all software is built in the same style and using the same tools. There is no reason to have something like a package manager because of this.

*However* there are ports which can be found in the [Ports directory](../Ports). A port is a piece of software that can optionally be installed, might not have been built by us, but supports running on SerenityOS. They act quite similarly to packages, each coming with their own install script.

Currently when running the system in a virtual machine, ports need to be cross compiled on the host, and added to the file system image before booting. Then it's also possible to configure the build system to [in- or exclude components](./AdvancedBuildInstructions.md#component-configuration) from a build.

## Why is there an MP3 implementation in SerenityOS if MP3 is protected by patents?

*This section is informational; we are not lawyers and this is not legal advice. Consult a qualified lawyer if you have legal questions regarding your use of SerenityOS software.*

MP3 was indeed originally protected by patents, preventing certain uses of the format. All MP3 patents, however, have expired since at least 2017, depending on where a specific patent was registered. Therefore, we believe it to be completely legal to implement MP3 as 2-clause BSD licensed software without acquiring patent licenses.

*However*, this does not apply to many other multimedia formats, such as the popular H.264 (AVC) and H.265 (HEVC) video codecs or the JPEG 2000 image format. As long as there is any reason to believe that a format is covered by patents, there will not be an implementation in the SerenityOS monorepo, as we believe this to be incompatible with the BSD 2-clause license in general. *However however*, third-party ports with differing licenses can provide implementations for these formats, such as ffmpeg. Depending on your situation and/or use case, using this third-party software might not be legal (for example, see the [ffmpeg information on the same topic](https://ffmpeg.org/legal.html)). Everything regarding the legal situation of the SerenityOS code is handled by [our license](../LICENSE), everything regarding the legal situation of third-party code is handled by the license of the particular software.

