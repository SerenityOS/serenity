# Frequently Asked Questions about SerenityOS

## Will SerenityOS support `$THING`?

Maybe. Maybe not. There is no plan.

## When will you implement `$THING`?

Maybe someday. Maybe never. If you want to see something happen, you can do it yourself!

## Where are the ISO images?

There are no ISO images. This project does not cater to non-technical users.

## Why is the system 32-bit?

The system was originally 32-bit only, since that's what Andreas was most familiar with when starting out. Nowadays, there is a 64-bit port as well (Intel/AMD x86\_64)

## I did a `git pull` and now the build is broken! What do I do?

If it builds on CI, it should build for you too. You may need to rebuild the toolchain. If that doesn't help, try it with a clean repo.

If you can't figure out what to do, ask in the `#build-problems` channel on Discord.

## Why don't you use `$LIBRARY` instead of implementing `$FEATURE` yourself?

The SerenityOS project tries to maximize hackability, accountability, and fun(!) by implementing everything ourselves.

## Does SerenityOS have a package manager?

No, SerenityOS does not have a package manager. The project uses a monorepo approach, meaning that all software is built in the same style and using the same tools. There is no reason to have something like a package manager because of this.

*However* there are ports which can be found in the [Ports directory](../Ports). A port is a piece of software that can optionally be installed, might have not been built by us but supports running on SerenityOS. They act quite similar to packages, coming with an install script each.

Currently when running the system in a virtual machine, ports need to be cross compiled on the host and added to the file system image before booting. Then its also possible to configure the build system to [in- or exclude components](./AdvancedBuildInstructions.md#component-configuration) from a build.
