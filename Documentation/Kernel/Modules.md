# Kernel Modules

## What are kernel modules

Kernel modules are "atomic" component units in the Kernel one can decide to
not compile into the final image.

## How can I benefit from this?

1. Faster boot times. Let's assume you have a old machine (2006 era), and you 
want to install Serenity on it. That machine doesn't include SATA AHCI HBA,
so you can easily instruct the build system to not compile AHCI support for your
Kernel image.
2. Diagnosing hardware problems. Maybe your hardware setup is not functioning
correctly with the SerenityOS Kernel. In such case, you might want to not 
compile a Storage component, or a suspected-to-fail network driver.

There's also a security perspective but that was not examined thoroughly yet.
Please note that the default configuration is built to include features that are
not extraordinary, so you should already see the benefits without doing anything
at all.

## How can I set what to compile and what not?

Simply edit the `config.ini` file in Kernel build directory of the target platform.
For example, for `i686`, you will find it in `Build/i686/Kernel/config.ini`.

Please note that the default Kernel image should suffice for most users out
there. If you still want to change a setting, please ensure that feature you
want to enable is actually required before attempting to do so.

## What about loadable kernel modules?

Currently there are no loadable kernel modules. While there are advantages to
utilizing loadable kernel modules, the overall disadvantages - both in terms of
usability (when you have tons of kernel drivers, but we don't) and security, are
currently winning the equation to not use them.
Therefore, if you want to test a new Kernel image you just built, you will need
to restart your machine.

## Is it going to be a kernel modules subsystem like Linux has?

Definitely not. Linux has a much broad amount of subsets targeted at multiple
purposes and contradicting goals within its community. Because we aim to create
a desktop oriented OS, it's a matter of making things customizable so it's 
useful for the desktop user. Therefore, it's definitely not a goal to make every
single piece of code to not be in the final image if one desires to.
