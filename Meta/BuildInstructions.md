# Building the Serenity operating system

Let's start with a quick guide to building the i686-pc-serenity toolchain.

I keep my toolchain in /opt/cross (so /opt/cross/bin needs to be in $PATH) and my Serenity sources are in $HOME/src/serenity

You need to adjust these so they fit your system.

## Dependencies:

First off, GCC needs MPFR, MPC and GMP. On Ubuntu, this is as simple as:

    sudo apt install libmpfr-dev libmpc-dev libgmp-dev

For Serenity, we will need e2fsprogs and QEMU:

    sudo apt install e2fsprogs qemu-system-i386

## Binutils:

Download GNU binutils-2.32 and apply the patch serenity/Meta/binutils-2.32-serenity.patch

Make a build directory next to the binutils source directory.

In the build directory, run configure:

    ../binutils-2.32/configure \
        --prefix=/opt/cross \
        --target=i686-pc-serenity \
        --with-sysroot=$HOME/src/serenity/Root \
        --disable-nls
 

Then build and install:

    make
    sudo make install

## Serenity LibC and LibM headers:

Before we can build GCC, we need to put the Serenity LibC headers where GCC can find them. So go into serenity/LibC/ and install them:

    ./install.sh

Then do the same in serenity/LibM/:

    ./install.sh

Don't worry about any error messages from the above commands. We only care about copying the headers to the right place at this time.

## GCC (part 1):

Okay, then let's build GCC.

Download GNU GCC-8.3.0 and apply the patch serenity/Meta/gcc-8.3.0-serenity.patch

Make a build directory next to the GCC source directory.

In the build directory, run configure:

    ../gcc-8.3.0/configure \
        --prefix=/opt/cross \
        --target=i686-pc-serenity \
        --with-sysroot=$HOME/src/serenity/Root \
        --with-newlib \
        --enable-languages=c,c++

Then build and install:

    make all-gcc all-target-libgcc
    sudo make install-gcc install-target-libgcc

## Serenity LibC for GCC:

Now let's go into serenity/LibC/ and build the C library. This is required in order to complete the GCC build.

    make
    ./install.sh

The C library is now installed in serenity/Root/ and we can build GCC's libstdc++...

## GCC (part 2):

Go back to the GCC build directory and finish building libstdc++:

    make all-target-libstdc++-v3
    sudo make install-target-libstdc++-v3

## Serenity (Full build)

If everything worked out, you now have the i686-pc-serenity toolchain ready and we can build Serenity.

Go into serenity/Kernel and build it:

    ./makeall.sh

Then take it for a spin:

    ./run
