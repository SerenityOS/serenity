% Building the JDK

## TL;DR (Instructions for the Impatient)

If you are eager to try out building the JDK, these simple steps works most of
the time. They assume that you have installed Git (and Cygwin if running
on Windows) and cloned the top-level JDK repository that you want to build.

 1. [Get the complete source code](#getting-the-source-code): \
    `git clone https://git.openjdk.java.net/jdk/`

 2. [Run configure](#running-configure): \
    `bash configure`

    If `configure` fails due to missing dependencies (to either the
    [toolchain](#native-compiler-toolchain-requirements), [build tools](
    #build-tools-requirements), [external libraries](
    #external-library-requirements) or the [boot JDK](#boot-jdk-requirements)),
    most of the time it prints a suggestion on how to resolve the situation on
    your platform. Follow the instructions, and try running `bash configure`
    again.

 3. [Run make](#running-make): \
    `make images`

 4. Verify your newly built JDK: \
    `./build/*/images/jdk/bin/java -version`

 5. [Run basic tests](##running-tests): \
    `make run-test-tier1`

If any of these steps failed, or if you want to know more about build
requirements or build functionality, please continue reading this document.

## Introduction

The JDK is a complex software project. Building it requires a certain amount of
technical expertise, a fair number of dependencies on external software, and
reasonably powerful hardware.

If you just want to use the JDK and not build it yourself, this document is not
for you. See for instance [OpenJDK installation](
http://openjdk.java.net/install) for some methods of installing a prebuilt
JDK.

## Getting the Source Code

Make sure you are getting the correct version. As of JDK 10, the source is no
longer split into separate repositories so you only need to clone one single
repository. At the [OpenJDK Git site](https://git.openjdk.java.net/) you
can see a list of all available repositories. If you want to build an older version,
e.g. JDK 11, it is recommended that you get the `jdk11u` repo, which contains
incremental updates, instead of the `jdk11` repo, which was frozen at JDK 11 GA.

If you are new to Git, a good place to start is the book [Pro
Git](https://git-scm.com/book/en/v2). The rest of this document
assumes a working knowledge of Git.

### Special Considerations

For a smooth building experience, it is recommended that you follow these rules
on where and how to check out the source code.

  * Do not check out the source code in a path which contains spaces. Chances
    are the build will not work. This is most likely to be an issue on Windows
    systems.

  * Do not check out the source code in a path which has a very long name or is
    nested many levels deep. Chances are you will hit an OS limitation during
    the build.

  * Put the source code on a local disk, not a network share. If possible, use
    an SSD. The build process is very disk intensive, and having slow disk
    access will significantly increase build times. If you need to use a
    network share for the source code, see below for suggestions on how to keep
    the build artifacts on a local disk.

  * On Windows, if using [Cygwin](#cygwin), extra care must be taken to make sure
    the environment is consistent. It is recommended that you follow this
    procedure:

      * Create the directory that is going to contain the top directory of the
        JDK clone by using the `mkdir` command in the Cygwin bash shell.
        That is, do *not* create it using Windows Explorer. This will ensure
        that it will have proper Cygwin attributes, and that it's children will
        inherit those attributes.

      * Do not put the JDK clone in a path under your Cygwin home
        directory. This is especially important if your user name contains
        spaces and/or mixed upper and lower case letters.

      * You need to install a git client. You have two choices, Cygwin git or
        Git for Windows. Unfortunately there are pros and cons with each choice.

        * The Cygwin `git` client has no line ending issues and understands
          Cygwin paths (which are used throughout the JDK build system).
          However, it does not currently work well with the Skara CLI tooling.
          Please see the [Skara wiki on Git clients](
          https://wiki.openjdk.java.net/display/SKARA/Skara#Skara-Git) for
          up-to-date information about the Skara git client support.

        * The [Git for Windows](https://gitforwindows.org) client has issues
          with line endings, and do not understand Cygwin paths. It does work
          well with the Skara CLI tooling, however. To alleviate the line ending
          problems, make sure you set `core.autocrlf` to `false` (this is asked
          during installation).

    Failure to follow this procedure might result in hard-to-debug build
    problems.

## Build Hardware Requirements

The JDK is a massive project, and require machines ranging from decent to
powerful to be able to build in a reasonable amount of time, or to be able to
complete a build at all.

We *strongly* recommend usage of an SSD disk for the build, since disk speed is
one of the limiting factors for build performance.

### Building on x86

At a minimum, a machine with 2-4 cores is advisable, as well as 2-4 GB of RAM.
(The more cores to use, the more memory you need.) At least 6 GB of free disk
space is required.

Even for 32-bit builds, it is recommended to use a 64-bit build machine, and
instead create a 32-bit target using `--with-target-bits=32`.

### Building on aarch64

At a minimum, a machine with 8 cores is advisable, as well as 8 GB of RAM.
(The more cores to use, the more memory you need.) At least 6 GB of free disk
space is required.

If you do not have access to sufficiently powerful hardware, it is also
possible to use [cross-compiling](#cross-compiling).

### Building on 32-bit arm

This is not recommended. Instead, see the section on [Cross-compiling](
#cross-compiling).

## Operating System Requirements

The mainline JDK project supports Linux, macOS, AIX and Windows.
Support for other operating system, e.g. BSD, exists in separate "port"
projects.

In general, the JDK can be built on a wide range of versions of these operating
systems, but the further you deviate from what is tested on a daily basis, the
more likely you are to run into problems.

This table lists the OS versions used by Oracle when building the JDK. Such
information is always subject to change, but this table is up to date at the
time of writing.

 Operating system   Vendor/version used
 -----------------  -------------------------------------------------------
 Linux              Oracle Enterprise Linux 6.4 / 7.6
 macOS              Mac OS X 10.13 (High Sierra)
 Windows            Windows Server 2012 R2

The double version numbers for Linux are due to the hybrid model
used at Oracle, where header files and external libraries from an older version
are used when building on a more modern version of the OS.

The Build Group has a wiki page with [Supported Build Platforms](
https://wiki.openjdk.java.net/display/Build/Supported+Build+Platforms). From
time to time, this is updated by contributors to list successes or failures of
building on different platforms.

### Windows

Windows XP is not a supported platform, but all newer Windows should be able to
build the JDK.

On Windows, it is important that you pay attention to the instructions in the
[Special Considerations](#special-considerations).

Windows is the only non-POSIX OS supported by the JDK, and as such, requires
some extra care. A POSIX support layer is required to build on Windows.
Currently, the only supported such layers are Cygwin and Windows Subsystem for
Linux (WSL). (Msys is no longer supported due to a too old bash; msys2 would
likely be possible to support in a future version but that would require effort
to implement.)

Internally in the build system, all paths are represented as Unix-style paths,
e.g. `/cygdrive/c/git/jdk/Makefile` rather than `C:\git\jdk\Makefile`. This
rule also applies to input to the build system, e.g. in arguments to
`configure`. So, use `--with-msvcr-dll=/cygdrive/c/msvcr100.dll` rather than
`--with-msvcr-dll=c:\msvcr100.dll`. For details on this conversion, see the section
on [Fixpath](#fixpath).

#### Cygwin

A functioning [Cygwin](http://www.cygwin.com/) environment is required for
building the JDK on Windows. If you have a 64-bit OS, we strongly recommend
using the 64-bit version of Cygwin.

**Note:** Cygwin has a model of continuously updating all packages without any
easy way to install or revert to a specific version of a package. This means
that whenever you add or update a package in Cygwin, you might (inadvertently)
update tools that are used by the JDK build process, and that can cause
unexpected build problems.

The JDK requires GNU Make 4.0 or greater in Cygwin. This is usually not a
problem, since Cygwin currently only distributes GNU Make at a version above
4.0.

Apart from the basic Cygwin installation, the following packages must also be
installed:

  * `autoconf`
  * `make`
  * `zip`
  * `unzip`

Often, you can install these packages using the following command line:
```
<path to Cygwin setup>/setup-x86_64 -q -P autoconf -P make -P unzip -P zip
```

Unfortunately, Cygwin can be unreliable in certain circumstances. If you
experience build tool crashes or strange issues when building on Windows,
please check the Cygwin FAQ on the ["BLODA" list](
https://cygwin.com/faq/faq.html#faq.using.bloda) and the section on [fork()
failures](https://cygwin.com/faq/faq.html#faq.using.fixing-fork-failures).

#### Windows Subsystem for Linux (WSL)

Windows 10 1809 or newer is supported due to a dependency on the wslpath utility
and support for environment variable sharing through WSLENV. Version 1803 can
work but intermittent build failures have been observed.

It's possible to build both Windows and Linux binaries from WSL. To build
Windows binaries, you must use a Windows boot JDK (located in a
Windows-accessible directory). To build Linux binaries, you must use a Linux
boot JDK. The default behavior is to build for Windows. To build for Linux, pass
`--build=x86_64-unknown-linux-gnu --host=x86_64-unknown-linux-gnu` to
`configure`.

If building Windows binaries, the source code must be located in a Windows-
accessible directory. This is because Windows executables (such as Visual Studio
and the boot JDK) must be able to access the source code. Also, the drive where
the source is stored must be mounted as case-insensitive by changing either
/etc/fstab or /etc/wsl.conf in WSL. Individual directories may be corrected
using the fsutil tool in case the source was cloned before changing the mount
options.

Note that while it's possible to build on WSL, testing is still not fully
supported.

### macOS

Apple is using a quite aggressive scheme of pushing OS updates, and coupling
these updates with required updates of Xcode. Unfortunately, this makes it
difficult for a project such as the JDK to keep pace with a continuously updated
machine running macOS. See the section on [Apple Xcode](#apple-xcode) on some
strategies to deal with this.

It is recommended that you use at least Mac OS X 10.13 (High Sierra). At the time
of writing, the JDK has been successfully compiled on macOS 10.12 (Sierra).

The standard macOS environment contains the basic tooling needed to build, but
for external libraries a package manager is recommended. The JDK uses
[homebrew](https://brew.sh/) in the examples, but feel free to use whatever
manager you want (or none).

### Linux

It is often not much problem to build the JDK on Linux. The only general advice
is to try to use the compilers, external libraries and header files as provided
by your distribution.

The basic tooling is provided as part of the core operating system, but you
will most likely need to install developer packages.

For apt-based distributions (Debian, Ubuntu, etc), try this:
```
sudo apt-get install build-essential
```

For rpm-based distributions (Fedora, Red Hat, etc), try this:
```
sudo yum groupinstall "Development Tools"
```

For Alpine Linux, aside from basic tooling, install the GNU versions of some
programs:

```
sudo apk add build-base bash grep zip
```

### AIX

Please consult the AIX section of the [Supported Build Platforms](
https://wiki.openjdk.java.net/display/Build/Supported+Build+Platforms) OpenJDK
Build Wiki page for details about which versions of AIX are supported.

## Native Compiler (Toolchain) Requirements

Large portions of the JDK consists of native code, that needs to be compiled to
be able to run on the target platform. In theory, toolchain and operating
system should be independent factors, but in practice there's more or less a
one-to-one correlation between target operating system and toolchain.

 Operating system   Supported toolchain
 ------------------ -------------------------
 Linux              gcc, clang
 macOS              Apple Xcode (using clang)
 AIX                IBM XL C/C++
 Windows            Microsoft Visual Studio

Please see the individual sections on the toolchains for version
recommendations. As a reference, these versions of the toolchains are used, at
the time of writing, by Oracle for the daily builds of the JDK. It should be
possible to compile the JDK with both older and newer versions, but the closer
you stay to this list, the more likely you are to compile successfully without
issues.

 Operating system   Toolchain version
 ------------------ -------------------------------------------------------
 Linux              gcc 10.2.0
 macOS              Apple Xcode 10.1 (using clang 10.0.0)
 Windows            Microsoft Visual Studio 2019 update 16.7.2

All compilers are expected to be able to compile to the C99 language standard,
as some C99 features are used in the source code. Microsoft Visual Studio
doesn't fully support C99 so in practice shared code is limited to using C99
features that it does support.

### gcc

The minimum accepted version of gcc is 5.0. Older versions will generate a warning
by `configure` and are unlikely to work.

The JDK is currently known to be able to compile with at least version 10.2 of
gcc.

In general, any version between these two should be usable.

### clang

The minimum accepted version of clang is 3.5. Older versions will not be
accepted by `configure`.

To use clang instead of gcc on Linux, use `--with-toolchain-type=clang`.

### Apple Xcode

The oldest supported version of Xcode is 8.

You will need the Xcode command lines developers tools to be able to build
the JDK. (Actually, *only* the command lines tools are needed, not the IDE.)
The simplest way to install these is to run:
```
xcode-select --install
```

It is advisable to keep an older version of Xcode for building the JDK when
updating Xcode. This [blog page](
http://iosdevelopertips.com/xcode/install-multiple-versions-of-xcode.html) has
good suggestions on managing multiple Xcode versions. To use a specific version
of Xcode, use `xcode-select -s` before running `configure`, or use
`--with-toolchain-path` to point to the version of Xcode to use, e.g.
`configure --with-toolchain-path=/Applications/Xcode8.app/Contents/Developer/usr/bin`

If you have recently (inadvertently) updated your OS and/or Xcode version, and
the JDK can no longer be built, please see the section on [Problems with the
Build Environment](#problems-with-the-build-environment), and [Getting
Help](#getting-help) to find out if there are any recent, non-merged patches
available for this update.

### Microsoft Visual Studio

The minimum accepted version of Visual Studio is 2017. Older versions will not
be accepted by `configure` and will not work. The maximum accepted
version of Visual Studio is 2019.

If you have multiple versions of Visual Studio installed, `configure` will by
default pick the latest. You can request a specific version to be used by
setting `--with-toolchain-version`, e.g. `--with-toolchain-version=2017`.

If you have Visual Studio installed but `configure` fails to detect it, it may
be because of [spaces in path](#spaces-in-path).

### IBM XL C/C++

Please consult the AIX section of the [Supported Build Platforms](
https://wiki.openjdk.java.net/display/Build/Supported+Build+Platforms) OpenJDK
Build Wiki page for details about which versions of XLC are supported.


## Boot JDK Requirements

Paradoxically, building the JDK requires a pre-existing JDK. This is called the
"boot JDK". The boot JDK does not, however, have to be a JDK built directly from
the source code available in the OpenJDK Community.  If you are porting the JDK
to a new platform, chances are that there already exists another JDK for that
platform that is usable as boot JDK.

The rule of thumb is that the boot JDK for building JDK major version *N*
should be a JDK of major version *N-1*, so for building JDK 9 a JDK 8 would be
suitable as boot JDK. However, the JDK should be able to "build itself", so an
up-to-date build of the current JDK source is an acceptable alternative. If
you are following the *N-1* rule, make sure you've got the latest update
version, since JDK 8 GA might not be able to build JDK 9 on all platforms.

Early in the release cycle, version *N-1* may not yet have been released. In
that case, the preferred boot JDK will be version *N-2* until version *N-1*
is available.

If the boot JDK is not automatically detected, or the wrong JDK is picked, use
`--with-boot-jdk` to point to the JDK to use.

### Getting JDK binaries

JDK binaries for Linux, Windows and macOS can be downloaded from
[jdk.java.net](http://jdk.java.net). An alternative is to download the
[Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads). Another
is the [Adopt OpenJDK Project](https://adoptopenjdk.net/), which publishes
experimental prebuilt binaries for various platforms.

On Linux you can also get a JDK from the Linux distribution. On apt-based
distros (like Debian and Ubuntu), `sudo apt-get install openjdk-<VERSION>-jdk`
is typically enough to install a JDK \<VERSION\>. On rpm-based distros (like
Fedora and Red Hat), try `sudo yum install java-<VERSION>-openjdk-devel`.

## External Library Requirements

Different platforms require different external libraries. In general, libraries
are not optional - that is, they are either required or not used.

If a required library is not detected by `configure`, you need to provide the
path to it. There are two forms of the `configure` arguments to point to an
external library: `--with-<LIB>=<path>` or `--with-<LIB>-include=<path to
include> --with-<LIB>-lib=<path to lib>`. The first variant is more concise,
but require the include files and library files to reside in a default
hierarchy under this directory. In most cases, it works fine.

As a fallback, the second version allows you to point to the include directory
and the lib directory separately.

### FreeType

FreeType2 from [The FreeType Project](http://www.freetype.org/) is not required
on any platform. The exception is on Unix-based platforms when configuring such
that the build artifacts will reference a system installed library,
rather than bundling the JDK's own copy.

  * To install on an apt-based Linux, try running `sudo apt-get install
    libfreetype6-dev`.
  * To install on an rpm-based Linux, try running `sudo yum install
    freetype-devel`.
  * To install on Alpine Linux, try running `sudo apk add freetype-dev`.
  * To install on macOS, try running `brew install freetype`.

Use `--with-freetype-include=<path>` and `--with-freetype-lib=<path>`
if `configure` does not automatically locate the platform FreeType files.

### CUPS

CUPS, [Common UNIX Printing System](http://www.cups.org) header files are
required on all platforms, except Windows. Often these files are provided by
your operating system.

  * To install on an apt-based Linux, try running `sudo apt-get install
    libcups2-dev`.
  * To install on an rpm-based Linux, try running `sudo yum install
    cups-devel`.
  * To install on Alpine Linux, try running `sudo apk add cups-dev`.

Use `--with-cups=<path>` if `configure` does not properly locate your CUPS
files.

### X11

Certain [X11](http://www.x.org/) libraries and include files are required on
Linux.

  * To install on an apt-based Linux, try running `sudo apt-get install
    libx11-dev libxext-dev libxrender-dev libxrandr-dev libxtst-dev libxt-dev`.
  * To install on an rpm-based Linux, try running `sudo yum install
    libXtst-devel libXt-devel libXrender-devel libXrandr-devel libXi-devel`.
  * To install on Alpine Linux, try running `sudo apk add libx11-dev
    libxext-dev libxrender-dev libxrandr-dev libxtst-dev libxt-dev`.

Use `--with-x=<path>` if `configure` does not properly locate your X11 files.

### ALSA

ALSA, [Advanced Linux Sound Architecture](https://www.alsa-project.org/) is
required on Linux. At least version 0.9.1 of ALSA is required.

  * To install on an apt-based Linux, try running `sudo apt-get install
    libasound2-dev`.
  * To install on an rpm-based Linux, try running `sudo yum install
    alsa-lib-devel`.
  * To install on Alpine Linux, try running `sudo apk add alsa-lib-dev`.

Use `--with-alsa=<path>` if `configure` does not properly locate your ALSA
files.

### libffi

libffi, the [Portable Foreign Function Interface Library](
http://sourceware.org/libffi) is required when building the Zero version of
Hotspot.

  * To install on an apt-based Linux, try running `sudo apt-get install
    libffi-dev`.
  * To install on an rpm-based Linux, try running `sudo yum install
    libffi-devel`.
  * To install on Alpine Linux, try running `sudo apk add libffi-dev`.

Use `--with-libffi=<path>` if `configure` does not properly locate your libffi
files.

## Build Tools Requirements

### Autoconf

The JDK requires [Autoconf](http://www.gnu.org/software/autoconf) on all
platforms. At least version 2.69 is required.

  * To install on an apt-based Linux, try running `sudo apt-get install
    autoconf`.
  * To install on an rpm-based Linux, try running `sudo yum install
    autoconf`.
  * To install on Alpine Linux, try running `sudo apk add autoconf`.
  * To install on macOS, try running `brew install autoconf`.
  * To install on Windows, try running `<path to Cygwin setup>/setup-x86_64 -q
    -P autoconf`.

If `configure` has problems locating your installation of autoconf, you can
specify it using the `AUTOCONF` environment variable, like this:

```
AUTOCONF=<path to autoconf> configure ...
```

### GNU Make

The JDK requires [GNU Make](http://www.gnu.org/software/make). No other flavors
of make are supported.

At least version 3.81 of GNU Make must be used. For distributions supporting
GNU Make 4.0 or above, we strongly recommend it. GNU Make 4.0 contains useful
functionality to handle parallel building (supported by `--with-output-sync`)
and speed and stability improvements.

Note that `configure` locates and verifies a properly functioning version of
`make` and stores the path to this `make` binary in the configuration. If you
start a build using `make` on the command line, you will be using the version
of make found first in your `PATH`, and not necessarily the one stored in the
configuration. This initial make will be used as "bootstrap make", and in a
second stage, the make located by `configure` will be called. Normally, this
will present no issues, but if you have a very old `make`, or a non-GNU Make
`make` in your path, this might cause issues.

If you want to override the default make found by `configure`, use the `MAKE`
configure variable, e.g. `configure MAKE=/opt/gnu/make`.

### GNU Bash

The JDK requires [GNU Bash](http://www.gnu.org/software/bash). No other shells
are supported.

At least version 3.2 of GNU Bash must be used.

## Running Configure

To build the JDK, you need a "configuration", which consists of a directory
where to store the build output, coupled with information about the platform,
the specific build machine, and choices that affect how the JDK is built.

The configuration is created by the `configure` script. The basic invocation of
the `configure` script looks like this:

```
bash configure [options]
```

This will create an output directory containing the configuration and setup an
area for the build result. This directory typically looks like
`build/linux-x64-server-release`, but the actual name depends on your specific
configuration. (It can also be set directly, see [Using Multiple
Configurations](#using-multiple-configurations)). This directory is referred to
as `$BUILD` in this documentation.

`configure` will try to figure out what system you are running on and where all
necessary build components are. If you have all prerequisites for building
installed, it should find everything. If it fails to detect any component
automatically, it will exit and inform you about the problem.

Some command line examples:

  * Create a 32-bit build for Windows with FreeType2 in `C:\freetype-i586`:
    ```
    bash configure --with-freetype=/cygdrive/c/freetype-i586 --with-target-bits=32
    ```

  * Create a debug build with the `server` JVM and DTrace enabled:
    ```
    bash configure --enable-debug --with-jvm-variants=server --enable-dtrace
    ```

### Common Configure Arguments

Here follows some of the most common and important `configure` argument.

To get up-to-date information on *all* available `configure` argument, please
run:
```
bash configure --help
```

(Note that this help text also include general autoconf options, like
`--dvidir`, that is not relevant to the JDK. To list only JDK-specific
features, use `bash configure --help=short` instead.)

#### Configure Arguments for Tailoring the Build

  * `--enable-debug` - Set the debug level to `fastdebug` (this is a shorthand
    for `--with-debug-level=fastdebug`)
  * `--with-debug-level=<level>` - Set the debug level, which can be `release`,
    `fastdebug`, `slowdebug` or `optimized`. Default is `release`. `optimized`
    is variant of `release` with additional Hotspot debug code.
  * `--with-native-debug-symbols=<method>` - Specify if and how native debug
    symbols should be built. Available methods are `none`, `internal`,
    `external`, `zipped`. Default behavior depends on platform. See [Native
    Debug Symbols](#native-debug-symbols) for more details.
  * `--with-version-string=<string>` - Specify the version string this build
    will be identified with.
  * `--with-version-<part>=<value>` - A group of options, where `<part>` can be
    any of `pre`, `opt`, `build`, `major`, `minor`, `security` or `patch`. Use
    these options to modify just the corresponding part of the version string
    from the default, or the value provided by `--with-version-string`.
  * `--with-jvm-variants=<variant>[,<variant>...]` - Build the specified variant
    (or variants) of Hotspot. Valid variants are: `server`, `client`,
    `minimal`, `core`, `zero`, `custom`. Note that not all
    variants are possible to combine in a single build.
  * `--enable-jvm-feature-<feature>` or `--disable-jvm-feature-<feature>` -
    Include (or exclude) `<feature>` as a JVM feature in Hotspot. You can also
    specify a list of features to be enabled, separated by space or comma, as
    `--with-jvm-features=<feature>[,<feature>...]`. If you prefix `<feature>`
    with a `-`, it will be disabled. These options will modify the default list
    of features for the JVM variant(s) you are building. For the `custom` JVM
    variant, the default list is empty. A complete list of valid JVM features
    can be found using `bash configure --help`.
  * `--with-target-bits=<bits>` - Create a target binary suitable for running
    on a `<bits>` platform. Use this to create 32-bit output on a 64-bit build
    platform, instead of doing a full cross-compile. (This is known as a
    *reduced* build.)

On Linux, BSD and AIX, it is possible to override where Java by default
searches for runtime/JNI libraries. This can be useful in situations where
there is a special shared directory for system JNI libraries. This setting
can in turn be overridden at runtime by setting the `java.library.path` property.

  * `--with-jni-libpath=<path>` - Use the specified path as a default
  when searching for runtime libraries.

#### Configure Arguments for Native Compilation

  * `--with-devkit=<path>` - Use this devkit for compilers, tools and resources
  * `--with-sysroot=<path>` - Use this directory as sysroot
  * `--with-extra-path=<path>[;<path>]` - Prepend these directories to the
    default path when searching for all kinds of binaries
  * `--with-toolchain-path=<path>[;<path>]` - Prepend these directories when
    searching for toolchain binaries (compilers etc)
  * `--with-extra-cflags=<flags>` - Append these flags when compiling JDK C
    files
  * `--with-extra-cxxflags=<flags>` - Append these flags when compiling JDK C++
    files
  * `--with-extra-ldflags=<flags>` - Append these flags when linking JDK
    libraries

#### Configure Arguments for External Dependencies

  * `--with-boot-jdk=<path>` - Set the path to the [Boot JDK](
    #boot-jdk-requirements)
  * `--with-freetype=<path>` - Set the path to [FreeType](#freetype)
  * `--with-cups=<path>` - Set the path to [CUPS](#cups)
  * `--with-x=<path>` - Set the path to [X11](#x11)
  * `--with-alsa=<path>` - Set the path to [ALSA](#alsa)
  * `--with-libffi=<path>` - Set the path to [libffi](#libffi)
  * `--with-jtreg=<path>` - Set the path to JTReg. See [Running Tests](
    #running-tests)

Certain third-party libraries used by the JDK (libjpeg, giflib, libpng, lcms
and zlib) are included in the JDK repository. The default behavior of the
JDK build is to use the included ("bundled") versions of libjpeg, giflib,
libpng and lcms.
For zlib, the system lib (if present) is used except on Windows and AIX.
However the bundled libraries may be replaced by an external version.
To do so, specify `system` as the `<source>` option in these arguments.
(The default is `bundled`).

  * `--with-libjpeg=<source>` - Use the specified source for libjpeg
  * `--with-giflib=<source>` - Use the specified source for giflib
  * `--with-libpng=<source>` - Use the specified source for libpng
  * `--with-lcms=<source>` - Use the specified source for lcms
  * `--with-zlib=<source>` - Use the specified source for zlib

On Linux, it is possible to select either static or dynamic linking of the C++
runtime. The default is static linking, with dynamic linking as fallback if the
static library is not found.

  * `--with-stdc++lib=<method>` - Use the specified method (`static`, `dynamic`
    or `default`) for linking the C++ runtime.

### Configure Control Variables

It is possible to control certain aspects of `configure` by overriding the
value of `configure` variables, either on the command line or in the
environment.

Normally, this is **not recommended**. If used improperly, it can lead to a
broken configuration. Unless you're well versed in the build system, this is
hard to use properly. Therefore, `configure` will print a warning if this is
detected.

However, there are a few `configure` variables, known as *control variables*
that are supposed to be overridden on the command line. These are variables that
describe the location of tools needed by the build, like `MAKE` or `GREP`. If
any such variable is specified, `configure` will use that value instead of
trying to autodetect the tool. For instance, `bash configure
MAKE=/opt/gnumake4.0/bin/make`.

If a configure argument exists, use that instead, e.g. use `--with-jtreg`
instead of setting `JTREGEXE`.

Also note that, despite what autoconf claims, setting `CFLAGS` will not
accomplish anything. Instead use `--with-extra-cflags` (and similar for
`cxxflags` and `ldflags`).

## Running Make

When you have a proper configuration, all you need to do to build the JDK is to
run `make`. (But see the warning at [GNU Make](#gnu-make) about running the
correct version of make.)

When running `make` without any arguments, the default target is used, which is
the same as running `make default` or `make jdk`. This will build a minimal (or
roughly minimal) set of compiled output (known as an "exploded image") needed
for a developer to actually execute the newly built JDK. The idea is that in an
incremental development fashion, when doing a normal make, you should only
spend time recompiling what's changed (making it purely incremental) and only
do the work that's needed to actually run and test your code.

The output of the exploded image resides in `$BUILD/jdk`. You can test the
newly built JDK like this: `$BUILD/jdk/bin/java -version`.

### Common Make Targets

Apart from the default target, here are some common make targets:

  * `hotspot` - Build all of hotspot (but only hotspot)
  * `hotspot-<variant>` - Build just the specified jvm variant
  * `images` or `product-images` - Build the JDK image
  * `docs` or `docs-image` - Build the documentation image
  * `test-image` - Build the test image
  * `all` or `all-images` - Build all images (product, docs and test)
  * `bootcycle-images` - Build images twice, second time with newly built JDK
    (good for testing)
  * `clean` - Remove all files generated by make, but not those generated by
    configure
  * `dist-clean` - Remove all files, including configuration

Run `make help` to get an up-to-date list of important make targets and make
control variables.

It is possible to build just a single module, a single phase, or a single phase
of a single module, by creating make targets according to these followin
patterns. A phase can be either of `gensrc`, `gendata`, `copy`, `java`,
`launchers`, or `libs`. See [Using Fine-Grained Make Targets](
#using-fine-grained-make-targets) for more details about this functionality.

  * `<phase>` - Build the specified phase and everything it depends on
  * `<module>` - Build the specified module and everything it depends on
  * `<module>-<phase>` - Compile the specified phase for the specified module
    and everything it depends on

Similarly, it is possible to clean just a part of the build by creating make
targets according to these patterns:

  * `clean-<outputdir>` - Remove the subdir in the output dir with the name
  * `clean-<phase>` - Remove all build results related to a certain build
    phase
  * `clean-<module>` - Remove all build results related to a certain module
  * `clean-<module>-<phase>` - Remove all build results related to a certain
    module and phase

### Make Control Variables

It is possible to control `make` behavior by overriding the value of `make`
variables, either on the command line or in the environment.

Normally, this is **not recommended**. If used improperly, it can lead to a
broken build. Unless you're well versed in the build system, this is hard to
use properly. Therefore, `make` will print a warning if this is detected.

However, there are a few `make` variables, known as *control variables* that
are supposed to be overridden on the command line. These make up the "make time"
configuration, as opposed to the "configure time" configuration.

#### General Make Control Variables

  * `JOBS` - Specify the number of jobs to build with. See [Build
    Performance](#build-performance).
  * `LOG` - Specify the logging level and functionality. See [Checking the
    Build Log File](#checking-the-build-log-file)
  * `CONF` and `CONF_NAME` - Selecting the configuration(s) to use. See [Using
    Multiple Configurations](#using-multiple-configurations)

#### Test Make Control Variables

These make control variables only make sense when running tests. Please see
[Testing the JDK](testing.html) for details.

  * `TEST`
  * `TEST_JOBS`
  * `JTREG`
  * `GTEST`

#### Advanced Make Control Variables

These advanced make control variables can be potentially unsafe. See [Hints and
Suggestions for Advanced Users](#hints-and-suggestions-for-advanced-users) and
[Understanding the Build System](#understanding-the-build-system) for details.

  * `SPEC`
  * `CONF_CHECK`
  * `COMPARE_BUILD`
  * `JDK_FILTER`
  * `SPEC_FILTER`

## Running Tests

Most of the JDK tests are using the [JTReg](http://openjdk.java.net/jtreg)
test framework. Make sure that your configuration knows where to find your
installation of JTReg. If this is not picked up automatically, use the
`--with-jtreg=<path to jtreg home>` option to point to the JTReg framework.
Note that this option should point to the JTReg home, i.e. the top directory,
containing `lib/jtreg.jar` etc.

The [Adoption Group](https://wiki.openjdk.java.net/display/Adoption) provides
recent builds of jtreg [here](
https://ci.adoptopenjdk.net/view/Dependencies/job/dependency_pipeline/lastSuccessfulBuild/artifact/jtreg/).
Download the latest `.tar.gz` file, unpack it, and point `--with-jtreg` to the
`jtreg` directory that you just unpacked.

Building of Hotspot Gtest suite requires the source code of Google Test framework.
The top directory, which contains both `googletest` and `googlemock`
directories, should be specified via `--with-gtest`.
The supported version of Google Test is 1.8.1, whose source code can be obtained:

 * by downloading and unpacking the source bundle from [here](https://github.com/google/googletest/releases/tag/release-1.8.1)
 * or by checking out `release-1.8.1` tag of `googletest` project: `git clone -b release-1.8.1 https://github.com/google/googletest`

To execute the most basic tests (tier 1), use:
```
make run-test-tier1
```

For more details on how to run tests, please see the [Testing
the JDK](testing.html) document.

## Cross-compiling

Cross-compiling means using one platform (the *build* platform) to generate
output that can ran on another platform (the *target* platform).

The typical reason for cross-compiling is that the build is performed on a more
powerful desktop computer, but the resulting binaries will be able to run on a
different, typically low-performing system. Most of the complications that
arise when building for embedded is due to this separation of *build* and
*target* systems.

This requires a more complex setup and build procedure. This section assumes
you are familiar with cross-compiling in general, and will only deal with the
particularities of cross-compiling the JDK. If you are new to cross-compiling,
please see the [external links at Wikipedia](
https://en.wikipedia.org/wiki/Cross_compiler#External_links) for a good start
on reading materials.

Cross-compiling the JDK requires you to be able to build both for the build
platform and for the target platform. The reason for the former is that we need
to build and execute tools during the build process, both native tools and Java
tools.

If all you want to do is to compile a 32-bit version, for the same OS, on a
64-bit machine, consider using `--with-target-bits=32` instead of doing a
full-blown cross-compilation. (While this surely is possible, it's a lot more
work and will take much longer to build.)

### Cross compiling the easy way with OpenJDK devkits

The OpenJDK build system provides out-of-the box support for creating and using
so called devkits. A `devkit` is basically a collection of a cross-compiling
toolchain and a sysroot environment which can easily be used together with the
`--with-devkit` configure option to cross compile the OpenJDK. On Linux/x86_64,
the following command:
```
bash configure --with-devkit=<devkit-path> --openjdk-target=ppc64-linux-gnu && make
```

will configure and build OpenJDK for Linux/ppc64 assuming that `<devkit-path>`
points to a Linux/x86_64 to Linux/ppc64 devkit.

Devkits can be created from the `make/devkit` directory by executing:
```
make [ TARGETS="<TARGET_TRIPLET>+" ] [ BASE_OS=<OS> ] [ BASE_OS_VERSION=<VER> ]
```

where `TARGETS` contains one or more `TARGET_TRIPLET`s of the form
described in [section 3.4 of the GNU Autobook](
https://sourceware.org/autobook/autobook/autobook_17.html). If no
targets are given, a native toolchain for the current platform will be
created. Currently, at least the following targets are known to work:

 Supported devkit targets
 -------------------------
 x86_64-linux-gnu
 aarch64-linux-gnu
 arm-linux-gnueabihf
 ppc64-linux-gnu
 ppc64le-linux-gnu
 s390x-linux-gnu

`BASE_OS` must be one of "OEL6" for Oracle Enterprise Linux 6 or
"Fedora" (if not specified "OEL6" will be the default). If the base OS
is "Fedora" the corresponding Fedora release can be specified with the
help of the `BASE_OS_VERSION` option (with "27" as default version).
If the build is successful, the new devkits can be found in the
`build/devkit/result` subdirectory:
```
cd make/devkit
make TARGETS="ppc64le-linux-gnu aarch64-linux-gnu" BASE_OS=Fedora BASE_OS_VERSION=21
ls -1 ../../build/devkit/result/
x86_64-linux-gnu-to-aarch64-linux-gnu
x86_64-linux-gnu-to-ppc64le-linux-gnu
```

Notice that devkits are not only useful for targeting different build
platforms. Because they contain the full build dependencies for a
system (i.e. compiler and root file system), they can easily be used
to build well-known, reliable and reproducible build environments. You
can for example create and use a devkit with GCC 7.3 and a Fedora 12
sysroot environment (with glibc 2.11) on Ubuntu 14.04 (which doesn't
have GCC 7.3 by default) to produce OpenJDK binaries which will run on
all Linux systems with runtime libraries newer than the ones from
Fedora 12 (e.g. Ubuntu 16.04, SLES 11 or RHEL 6).

### Boot JDK and Build JDK

When cross-compiling, make sure you use a boot JDK that runs on the *build*
system, and not on the *target* system.

To be able to build, we need a "Build JDK", which is a JDK built from the
current sources (that is, the same as the end result of the entire build
process), but able to run on the *build* system, and not the *target* system.
(In contrast, the Boot JDK should be from an older release, e.g. JDK 8 when
building JDK 9.)

The build process will create a minimal Build JDK for you, as part of building.
To speed up the build, you can use `--with-build-jdk` to `configure` to point
to a pre-built Build JDK. Please note that the build result is unpredictable,
and can possibly break in subtle ways, if the Build JDK does not **exactly**
match the current sources.

### Specifying the Target Platform

You *must* specify the target platform when cross-compiling. Doing so will also
automatically turn the build into a cross-compiling mode. The simplest way to
do this is to use the `--openjdk-target` argument, e.g.
`--openjdk-target=arm-linux-gnueabihf`. or `--openjdk-target=aarch64-oe-linux`.
This will automatically set the `--build`, `--host` and `--target` options for
autoconf, which can otherwise be confusing. (In autoconf terminology, the
"target" is known as "host", and "target" is used for building a Canadian
cross-compiler.)

### Toolchain Considerations

You will need two copies of your toolchain, one which generates output that can
run on the target system (the normal, or *target*, toolchain), and one that
generates output that can run on the build system (the *build* toolchain). Note
that cross-compiling is only supported for gcc at the time being. The gcc
standard is to prefix cross-compiling toolchains with the target denominator.
If you follow this standard, `configure` is likely to pick up the toolchain
correctly.

The *build* toolchain will be autodetected just the same way the normal
*build*/*target* toolchain will be autodetected when not cross-compiling. If
this is not what you want, or if the autodetection fails, you can specify a
devkit containing the *build* toolchain using `--with-build-devkit` to
`configure`, or by giving `BUILD_CC` and `BUILD_CXX` arguments.

It is often helpful to locate the cross-compilation tools, headers and
libraries in a separate directory, outside the normal path, and point out that
directory to `configure`. Do this by setting the sysroot (`--with-sysroot`) and
appending the directory when searching for cross-compilations tools
(`--with-toolchain-path`). As a compact form, you can also use `--with-devkit`
to point to a single directory, if it is correctly setup. (See `basics.m4` for
details.)

### Native Libraries

You will need copies of external native libraries for the *target* system,
present on the *build* machine while building.

Take care not to replace the *build* system's version of these libraries by
mistake, since that can render the *build* machine unusable.

Make sure that the libraries you point to (ALSA, X11, etc) are for the
*target*, not the *build*, platform.

#### ALSA

You will need alsa libraries suitable for your *target* system. For most cases,
using Debian's pre-built libraries work fine.

Note that alsa is needed even if you only want to build a headless JDK.

  * Go to [Debian Package Search](https://www.debian.org/distrib/packages) and
    search for the `libasound2` and `libasound2-dev` packages for your *target*
    system. Download them to /tmp.

  * Install the libraries into the cross-compilation toolchain. For instance:
```
cd /tools/gcc-linaro-arm-linux-gnueabihf-raspbian-2012.09-20120921_linux/arm-linux-gnueabihf/libc
dpkg-deb -x /tmp/libasound2_1.0.25-4_armhf.deb .
dpkg-deb -x /tmp/libasound2-dev_1.0.25-4_armhf.deb .
```

  * If alsa is not properly detected by `configure`, you can point it out by
    `--with-alsa`.

#### X11

You will need X11 libraries suitable for your *target* system. For most cases,
using Debian's pre-built libraries work fine.

Note that X11 is needed even if you only want to build a headless JDK.

  * Go to [Debian Package Search](https://www.debian.org/distrib/packages),
    search for the following packages for your *target* system, and download them
    to /tmp/target-x11:
      * libxi
      * libxi-dev
      * x11proto-core-dev
      * x11proto-input-dev
      * x11proto-kb-dev
      * x11proto-render-dev
      * x11proto-xext-dev
      * libice-dev
      * libxrender
      * libxrender-dev
      * libxrandr-dev
      * libsm-dev
      * libxt-dev
      * libx11
      * libx11-dev
      * libxtst
      * libxtst-dev
      * libxext
      * libxext-dev

  * Install the libraries into the cross-compilation toolchain. For instance:
    ```
    cd /tools/gcc-linaro-arm-linux-gnueabihf-raspbian-2012.09-20120921_linux/arm-linux-gnueabihf/libc/usr
    mkdir X11R6
    cd X11R6
    for deb in /tmp/target-x11/*.deb ; do dpkg-deb -x $deb . ; done
    mv usr/* .
    cd lib
    cp arm-linux-gnueabihf/* .
    ```

    You can ignore the following messages. These libraries are not needed to
    successfully complete a full JDK build.
    ```
    cp: cannot stat `arm-linux-gnueabihf/libICE.so': No such file or directory
    cp: cannot stat `arm-linux-gnueabihf/libSM.so': No such file or directory
    cp: cannot stat `arm-linux-gnueabihf/libXt.so': No such file or directory
    ```

  * If the X11 libraries are not properly detected by `configure`, you can
    point them out by `--with-x`.

### Cross compiling with Debian sysroots

Fortunately, you can create sysroots for foreign architectures with tools
provided by your OS. On Debian/Ubuntu systems, one could use `qemu-deboostrap` to
create the *target* system chroot, which would have the native libraries and headers
specific to that *target* system. After that, we can use the cross-compiler on the *build*
system, pointing into chroot to get the build dependencies right. This allows building
for foreign architectures with native compilation speed.

For example, cross-compiling to AArch64 from x86_64 could be done like this:

  * Install cross-compiler on the *build* system:
    ```
    apt install g++-aarch64-linux-gnu gcc-aarch64-linux-gnu
    ```

  * Create chroot on the *build* system, configuring it for *target* system:
    ```
    sudo qemu-debootstrap \
      --arch=arm64 \
      --verbose \
      --include=fakeroot,symlinks,build-essential,libx11-dev,libxext-dev,libxrender-dev,libxrandr-dev,libxtst-dev,libxt-dev,libcups2-dev,libfontconfig1-dev,libasound2-dev,libfreetype6-dev,libpng-dev,libffi-dev \
      --resolve-deps \
      buster \
      ~/sysroot-arm64 \
      http://httpredir.debian.org/debian/
    ```

  * Make sure the symlinks inside the newly created chroot point to proper locations:
    ```
    sudo chroot ~/sysroot-arm64 symlinks -cr .
    ```

  * Configure and build with newly created chroot as sysroot/toolchain-path:
    ```
    sh ./configure \
      --openjdk-target=aarch64-linux-gnu \
      --with-sysroot=~/sysroot-arm64
    make images
    ls build/linux-aarch64-server-release/
    ```

The build does not create new files in that chroot, so it can be reused for multiple builds
without additional cleanup.

The build system should automatically detect the toolchain paths and dependencies, but sometimes
it might require a little nudge with:

  * Native compilers: override `CC` or `CXX` for `./configure`

  * Freetype lib location: override `--with-freetype-lib`, for example `${sysroot}/usr/lib/${target}/`

  * Freetype includes location: override `--with-freetype-include` for example `${sysroot}/usr/include/freetype2/`

  * X11 libraries location: override `--x-libraries`, for example `${sysroot}/usr/lib/${target}/`

Architectures that are known to successfully cross-compile like this are:

  Target        Debian tree  Debian arch   `--openjdk-target=...`   `--with-jvm-variants=...`
  ------------  ------------ ------------- ------------------------ --------------
  x86           buster       i386          i386-linux-gnu           (all)
  arm           buster       armhf         arm-linux-gnueabihf      (all)
  aarch64       buster       arm64         aarch64-linux-gnu        (all)
  ppc64le       buster       ppc64el       powerpc64le-linux-gnu    (all)
  s390x         buster       s390x         s390x-linux-gnu          (all)
  mipsle        buster       mipsel        mipsel-linux-gnu         zero
  mips64le      buster       mips64el      mips64el-linux-gnueabi64 zero
  armel         buster       arm           arm-linux-gnueabi        zero
  ppc           sid          powerpc       powerpc-linux-gnu        zero
  ppc64be       sid          ppc64         powerpc64-linux-gnu      (all)
  m68k          sid          m68k          m68k-linux-gnu           zero
  alpha         sid          alpha         alpha-linux-gnu          zero
  sh4           sid          sh4           sh4-linux-gnu            zero

### Building for ARM/aarch64

A common cross-compilation target is the ARM CPU. When building for ARM, it is
useful to set the ABI profile. A number of pre-defined ABI profiles are
available using `--with-abi-profile`: arm-vfp-sflt, arm-vfp-hflt, arm-sflt,
armv5-vfp-sflt, armv6-vfp-hflt. Note that soft-float ABIs are no longer
properly supported by the JDK.

### Building for musl

Just like it's possible to cross-compile for a different CPU, it's possible to
cross-compile for musl libc on a glibc-based *build* system.
A devkit suitable for most target CPU architectures can be obtained from
[musl.cc](https://musl.cc). After installing the required packages in the
sysroot, configure the build with `--openjdk-target`:

```
sh ./configure --with-jvm-variants=server \
--with-boot-jdk=$BOOT_JDK \
--with-build-jdk=$BUILD_JDK \
--openjdk-target=x86_64-unknown-linux-musl \
--with-devkit=$DEVKIT \
--with-sysroot=$SYSROOT
```

and run `make` normally.

### Verifying the Build

The build will end up in a directory named like
`build/linux-arm-normal-server-release`.

Inside this build output directory, the `images/jdk` will contain the newly
built JDK, for your *target* system.

Copy these folders to your *target* system. Then you can run e.g.
`images/jdk/bin/java -version`.

## Build Performance

Building the JDK requires a lot of horsepower. Some of the build tools can be
adjusted to utilize more or less of resources such as parallel threads and
memory. The `configure` script analyzes your system and selects reasonable
values for such options based on your hardware. If you encounter resource
problems, such as out of memory conditions, you can modify the detected values
with:

  * `--with-num-cores` -- number of cores in the build system, e.g.
    `--with-num-cores=8`.

  * `--with-memory-size` -- memory (in MB) available in the build system, e.g.
    `--with-memory-size=1024`

You can also specify directly the number of build jobs to use with
`--with-jobs=N` to `configure`, or `JOBS=N` to `make`. Do not use the `-j` flag
to `make`. In most cases it will be ignored by the makefiles, but it can cause
problems for some make targets.

It might also be necessary to specify the JVM arguments passed to the Boot JDK,
using e.g. `--with-boot-jdk-jvmargs="-Xmx8G"`. Doing so will override the
default JVM arguments passed to the Boot JDK.

At the end of a successful execution of `configure`, you will get a performance
summary, indicating how well the build will perform. Here you will also get
performance hints. If you want to build fast, pay attention to those!

If you want to tweak build performance, run with `make LOG=info` to get a build
time summary at the end of the build process.

### Disk Speed

If you are using network shares, e.g. via NFS, for your source code, make sure
the build directory is situated on local disk (e.g. by `ln -s
/localdisk/jdk-build $JDK-SHARE/build`). The performance penalty is extremely
high for building on a network share; close to unusable.

Also, make sure that your build tools (including Boot JDK and toolchain) is
located on a local disk and not a network share.

As has been stressed elsewhere, do use SSD for source code and build directory,
as well as (if possible) the build tools.

### Virus Checking

The use of virus checking software, especially on Windows, can *significantly*
slow down building of the JDK. If possible, turn off such software, or exclude
the directory containing the JDK source code from on-the-fly checking.

### Ccache

The JDK build supports building with ccache when using gcc or clang. Using
ccache can radically speed up compilation of native code if you often rebuild
the same sources. Your milage may vary however, so we recommend evaluating it
for yourself. To enable it, make sure it's on the path and configure with
`--enable-ccache`.

### Precompiled Headers

By default, the Hotspot build uses preccompiled headers (PCH) on the toolchains
were it is properly supported (clang, gcc, and Visual Studio). Normally, this
speeds up the build process, but in some circumstances, it can actually slow
things down.

You can experiment by disabling precompiled headers using
`--disable-precompiled-headers`.

### Icecc / icecream

[icecc/icecream](http://github.com/icecc/icecream) is a simple way to setup a
distributed compiler network. If you have multiple machines available for
building the JDK, you can drastically cut individual build times by utilizing
it.

To use, setup an icecc network, and install icecc on the build machine. Then
run `configure` using `--enable-icecc`.

### Using sjavac

To speed up Java compilation, especially incremental compilations, you can try
the experimental sjavac compiler by using `--enable-sjavac`.

### Building the Right Target

Selecting the proper target to build can have dramatic impact on build time.
For normal usage, `jdk` or the default target is just fine. You only need to
build `images` for shipping, or if your tests require it.

See also [Using Fine-Grained Make Targets](#using-fine-grained-make-targets) on
how to build an even smaller subset of the product.

## Troubleshooting

If your build fails, it can sometimes be difficult to pinpoint the problem or
find a proper solution.

### Locating the Source of the Error

When a build fails, it can be hard to pinpoint the actual cause of the error.
In a typical build process, different parts of the product build in parallel,
with the output interlaced.

#### Build Failure Summary

To help you, the build system will print a failure summary at the end. It looks
like this:

```
ERROR: Build failed for target 'hotspot' in configuration 'linux-x64' (exit code 2)

=== Output from failing command(s) repeated here ===
* For target hotspot_variant-server_libjvm_objs_psMemoryPool.o:
/localhome/git/jdk-sandbox/hotspot/src/share/vm/services/psMemoryPool.cpp:1:1: error: 'failhere' does not name a type
   ... (rest of output omitted)

* All command lines available in /localhome/git/jdk-sandbox/build/linux-x64/make-support/failure-logs.
=== End of repeated output ===

=== Make failed targets repeated here ===
lib/CompileJvm.gmk:207: recipe for target '/localhome/git/jdk-sandbox/build/linux-x64/hotspot/variant-server/libjvm/objs/psMemoryPool.o' failed
make/Main.gmk:263: recipe for target 'hotspot-server-libs' failed
=== End of repeated output ===

Hint: Try searching the build log for the name of the first failed target.
Hint: If caused by a warning, try configure --disable-warnings-as-errors.
```

Let's break it down! First, the selected configuration, and the top-level
target you entered on the command line that caused the failure is printed.

Then, between the `Output from failing command(s) repeated here` and `End of
repeated output` the first lines of output (stdout and stderr) from the actual
failing command is repeated. In most cases, this is the error message that
caused the build to fail. If multiple commands were failing (this can happen in
a parallel build), output from all failed commands will be printed here.

The path to the `failure-logs` directory is printed. In this file you will find
a `<target>.log` file that contains the output from this command in its
entirety, and also a `<target>.cmd`, which contain the complete command line
used for running this command. You can re-run the failing command by executing
`. <path to failure-logs>/<target>.cmd` in your shell.

Another way to trace the failure is to follow the chain of make targets, from
top-level targets to individual file targets. Between `Make failed targets
repeated here` and `End of repeated output` the output from make showing this
chain is repeated. The first failed recipe will typically contain the full path
to the file in question that failed to compile. Following lines will show a
trace of make targets why we ended up trying to compile that file.

Finally, some hints are given on how to locate the error in the complete log.
In this example, we would try searching the log file for "`psMemoryPool.o`".
Another way to quickly locate make errors in the log is to search for "`]
Error`" or "`***`".

Note that the build failure summary will only help you if the issue was a
compilation failure or similar. If the problem is more esoteric, or is due to
errors in the build machinery, you will likely get empty output logs, and `No
indication of failed target found` instead of the make target chain.

#### Checking the Build Log File

The output (stdout and stderr) from the latest build is always stored in
`$BUILD/build.log`. The previous build log is stored as `build.log.old`. This
means that it is not necessary to redirect the build output yourself if you
want to process it.

You can increase the verbosity of the log file, by the `LOG` control variable
to `make`. If you want to see the command lines used in compilations, use
`LOG=cmdlines`. To increase the general verbosity, use `LOG=info`, `LOG=debug`
or `LOG=trace`. Both of these can be combined with `cmdlines`, e.g.
`LOG=info,cmdlines`. The `debug` log level will show most shell commands
executed by make, and `trace` will show all. Beware that both these log levels
will produce a massive build log!

### Fixing Unexpected Build Failures

Most of the time, the build will fail due to incorrect changes in the source
code.

Sometimes the build can fail with no apparent changes that have caused the
failure. If this is the first time you are building the JDK on this particular
computer, and the build fails, the problem is likely with your build
environment. But even if you have previously built the JDK with success, and it
now fails, your build environment might have changed (perhaps due to OS
upgrades or similar). But most likely, such failures are due to problems with
the incremental rebuild.

#### Problems with the Build Environment

Make sure your configuration is correct. Re-run `configure`, and look for any
warnings. Warnings that appear in the middle of the `configure` output is also
repeated at the end, after the summary. The entire log is stored in
`$BUILD/configure.log`.

Verify that the summary at the end looks correct. Are you indeed using the Boot
JDK and native toolchain that you expect?

By default, the JDK has a strict approach where warnings from the compiler is
considered errors which fail the build. For very new or very old compiler
versions, this can trigger new classes of warnings, which thus fails the build.
Run `configure` with `--disable-warnings-as-errors` to turn of this behavior.
(The warnings will still show, but not make the build fail.)

#### Problems with Incremental Rebuilds

Incremental rebuilds mean that when you modify part of the product, only the
affected parts get rebuilt. While this works great in most cases, and
significantly speed up the development process, from time to time complex
interdependencies will result in an incorrect build result. This is the most
common cause for unexpected build problems.

Here are a suggested list of things to try if you are having unexpected build
problems. Each step requires more time than the one before, so try them in
order. Most issues will be solved at step 1 or 2.

 1. Make sure your repository is up-to-date

    Run `git pull origin master` to make sure you have the latest changes.

 2. Clean build results

    The simplest way to fix incremental rebuild issues is to run `make clean`.
    This will remove all build results, but not the configuration or any build
    system support artifacts. In most cases, this will solve build errors
    resulting from incremental build mismatches.

 3. Completely clean the build directory.

    If this does not work, the next step is to run `make dist-clean`, or
    removing the build output directory (`$BUILD`). This will clean all
    generated output, including your configuration. You will need to re-run
    `configure` after this step. A good idea is to run `make
    print-configuration` before running `make dist-clean`, as this will print
    your current `configure` command line. Here's a way to do this:

    ```
    make print-configuration > current-configuration
    make dist-clean
    bash configure $(cat current-configuration)
    make
    ```

 4. Re-clone the Git repository

    Sometimes the Git repository gets in a state that causes the product
    to be un-buildable. In such a case, the simplest solution is often the
    "sledgehammer approach": delete the entire repository, and re-clone it.
    If you have local changes, save them first to a different location using
    `git format-patch`.

### Specific Build Issues

#### Clock Skew

If you get an error message like this:
```
File 'xxx' has modification time in the future.
Clock skew detected. Your build may be incomplete.
```
then the clock on your build machine is out of sync with the timestamps on the
source files. Other errors, apparently unrelated but in fact caused by the
clock skew, can occur along with the clock skew warnings. These secondary
errors may tend to obscure the fact that the true root cause of the problem is
an out-of-sync clock.

If you see these warnings, reset the clock on the build machine, run `make
clean` and restart the build.

#### Out of Memory Errors

On Windows, you might get error messages like this:
```
fatal error - couldn't allocate heap
cannot create ... Permission denied
spawn failed
```
This can be a sign of a Cygwin problem. See the information about solving
problems in the [Cygwin](#cygwin) section. Rebooting the computer might help
temporarily.

#### Spaces in Path

On Windows, when configuring, `fixpath.sh` may report that some directory
names have spaces. Usually, it assumes those directories have
[short paths](https://docs.microsoft.com/en-us/windows-server/administration/windows-commands/fsutil-8dot3name).
You can run `fsutil file setshortname` in `cmd` on certain directories, such as
`Microsoft Visual Studio` or `Windows Kits`, to assign arbitrary short paths so
`configure` can access them.

### Getting Help

If none of the suggestions in this document helps you, or if you find what you
believe is a bug in the build system, please contact the Build Group by sending
a mail to [build-dev@openjdk.java.net](mailto:build-dev@openjdk.java.net).
Please include the relevant parts of the configure and/or build log.

If you need general help or advice about developing for the JDK, you can also
contact the Adoption Group. See the section on [Contributing to OpenJDK](
#contributing-to-openjdk) for more information.

## Hints and Suggestions for Advanced Users

### Bash Completion

The `configure` and `make` commands tries to play nice with bash command-line
completion (using `<tab>` or `<tab><tab>`). To use this functionality, make
sure you enable completion in your `~/.bashrc` (see instructions for bash in
your operating system).

Make completion will work out of the box, and will complete valid make targets.
For instance, typing `make jdk-i<tab>` will complete to `make jdk-image`.

The `configure` script can get completion for options, but for this to work you
need to help `bash` on the way. The standard way of running the script, `bash
configure`, will not be understood by bash completion. You need `configure` to
be the command to run. One way to achieve this is to add a simple helper script
to your path:

```
cat << EOT > /tmp/configure
#!/bin/bash
if [ \$(pwd) = \$(cd \$(dirname \$0); pwd) ] ; then
  echo >&2 "Abort: Trying to call configure helper recursively"
  exit 1
fi

bash \$PWD/configure "\$@"
EOT
chmod +x /tmp/configure
sudo mv /tmp/configure /usr/local/bin
```

Now `configure --en<tab>-dt<tab>` will result in `configure --enable-dtrace`.

### Using Multiple Configurations

You can have multiple configurations for a single source repository. When you
create a new configuration, run `configure --with-conf-name=<name>` to create a
configuration with the name `<name>`. Alternatively, you can create a directory
under `build` and run `configure` from there, e.g. `mkdir build/<name> && cd
build/<name> && bash ../../configure`.

Then you can build that configuration using `make CONF_NAME=<name>` or `make
CONF=<pattern>`, where `<pattern>` is a substring matching one or several
configurations, e.g. `CONF=debug`. The special empty pattern (`CONF=`) will
match *all* available configuration, so `make CONF= hotspot` will build the
`hotspot` target for all configurations. Alternatively, you can execute `make`
in the configuration directory, e.g. `cd build/<name> && make`.

### Handling Reconfigurations

If you update the repository and part of the configure script has changed, the
build system will force you to re-run `configure`.

Most of the time, you will be fine by running `configure` again with the same
arguments as the last time, which can easily be performed by `make
reconfigure`. To simplify this, you can use the `CONF_CHECK` make control
variable, either as `make CONF_CHECK=auto`, or by setting an environment
variable. For instance, if you add `export CONF_CHECK=auto` to your `.bashrc`
file, `make` will always run `reconfigure` automatically whenever the configure
script has changed.

You can also use `CONF_CHECK=ignore` to skip the check for a needed configure
update. This might speed up the build, but comes at the risk of an incorrect
build result. This is only recommended if you know what you're doing.

From time to time, you will also need to modify the command line to `configure`
due to changes. Use `make print-configuration` to show the command line used
for your current configuration.

### Using Fine-Grained Make Targets

The default behavior for make is to create consistent and correct output, at
the expense of build speed, if necessary.

If you are prepared to take some risk of an incorrect build, and know enough of
the system to understand how things build and interact, you can speed up the
build process considerably by instructing make to only build a portion of the
product.

#### Building Individual Modules

The safe way to use fine-grained make targets is to use the module specific
make targets. All source code in the JDK is organized so it belongs to a
module, e.g. `java.base` or `jdk.jdwp.agent`. You can build only a specific
module, by giving it as make target: `make jdk.jdwp.agent`. If the specified
module depends on other modules (e.g. `java.base`), those modules will be built
first.

You can also specify a set of modules, just as you can always specify a set of
make targets: `make jdk.crypto.cryptoki jdk.crypto.ec jdk.crypto.mscapi`

#### Building Individual Module Phases

The build process for each module is divided into separate phases. Not all
modules need all phases. Which are needed depends on what kind of source code
and other artifact the module consists of. The phases are:

  * `gensrc` (Generate source code to compile)
  * `gendata` (Generate non-source code artifacts)
  * `copy` (Copy resource artifacts)
  * `java` (Compile Java code)
  * `launchers` (Compile native executables)
  * `libs` (Compile native libraries)

You can build only a single phase for a module by using the notation
`$MODULE-$PHASE`. For instance, to build the `gensrc` phase for `java.base`,
use `make java.base-gensrc`.

Note that some phases may depend on others, e.g. `java` depends on `gensrc` (if
present). Make will build all needed prerequisites before building the
requested phase.

#### Skipping the Dependency Check

When using an iterative development style with frequent quick rebuilds, the
dependency check made by make can take up a significant portion of the time
spent on the rebuild. In such cases, it can be useful to bypass the dependency
check in make.

> **Note that if used incorrectly, this can lead to a broken build!**

To achieve this, append `-only` to the build target. For instance, `make
jdk.jdwp.agent-java-only` will *only* build the `java` phase of the
`jdk.jdwp.agent` module. If the required dependencies are not present, the
build can fail. On the other hand, the execution time measures in milliseconds.

A useful pattern is to build the first time normally (e.g. `make
jdk.jdwp.agent`) and then on subsequent builds, use the `-only` make target.

#### Rebuilding Part of java.base (JDK\_FILTER)

If you are modifying files in `java.base`, which is the by far largest module
in the JDK, then you need to rebuild all those files whenever a single file has
changed. (This inefficiency will hopefully be addressed in JDK 10.)

As a hack, you can use the make control variable `JDK_FILTER` to specify a
pattern that will be used to limit the set of files being recompiled. For
instance, `make java.base JDK_FILTER=javax/crypto` (or, to combine methods,
`make java.base-java-only JDK_FILTER=javax/crypto`) will limit the compilation
to files in the `javax.crypto` package.

## Understanding the Build System

This section will give you a more technical description on the details of the
build system.

### Configurations

The build system expects to find one or more configuration. These are
technically defined by the `spec.gmk` in a subdirectory to the `build`
subdirectory. The `spec.gmk` file is generated by `configure`, and contains in
principle the configuration (directly or by files included by `spec.gmk`).

You can, in fact, select a configuration to build by pointing to the `spec.gmk`
file with the `SPEC` make control variable, e.g. `make SPEC=$BUILD/spec.gmk`.
While this is not the recommended way to call `make` as a user, it is what is
used under the hood by the build system.

### Build Output Structure

The build output for a configuration will end up in `build/<configuration
name>`, which we refer to as `$BUILD` in this document. The `$BUILD` directory
contains the following important directories:

```
buildtools/
configure-support/
hotspot/
images/
jdk/
make-support/
support/
test-results/
test-support/
```

This is what they are used for:

  * `images`: This is the directory were the output of the `*-image` make
    targets end up. For instance, `make jdk-image` ends up in `images/jdk`.

  * `jdk`: This is the "exploded image". After `make jdk`, you will be able to
    launch the newly built JDK by running `$BUILD/jdk/bin/java`.

  * `test-results`: This directory contains the results from running tests.

  * `support`: This is an area for intermediate files needed during the build,
    e.g. generated source code, object files and class files. Some noteworthy
    directories in `support` is `gensrc`, which contains the generated source
    code, and the `modules_*` directories, which contains the files in a
    per-module hierarchy that will later be collapsed into the `jdk` directory
    of the exploded image.

  * `buildtools`: This is an area for tools compiled for the build platform
    that are used during the rest of the build.

  * `hotspot`: This is an area for intermediate files needed when building
    hotspot.

  * `configure-support`, `make-support` and `test-support`: These directories
    contain files that are needed by the build system for `configure`, `make`
    and for running tests.

### Fixpath

Windows path typically look like `C:\User\foo`, while Unix paths look like
`/home/foo`. Tools with roots from Unix often experience issues related to this
mismatch when running on Windows.

In the JDK build, we always use Unix paths internally, and only just before
calling a tool that does not understand Unix paths do we convert them to
Windows paths.

This conversion is done by the `fixpath` tool, which is a small wrapper that
modifies unix-style paths to Windows-style paths in command lines. Fixpath is
compiled automatically by `configure`.

### Native Debug Symbols

Native libraries and executables can have debug symbol (and other debug
information) associated with them. How this works is very much platform
dependent, but a common problem is that debug symbol information takes a lot of
disk space, but is rarely needed by the end user.

The JDK supports different methods on how to handle debug symbols. The
method used is selected by `--with-native-debug-symbols`, and available methods
are `none`, `internal`, `external`, `zipped`.

  * `none` means that no debug symbols will be generated during the build.

  * `internal` means that debug symbols will be generated during the build, and
    they will be stored in the generated binary.

  * `external` means that debug symbols will be generated during the build, and
    after the compilation, they will be moved into a separate `.debuginfo` file.
    (This was previously known as FDS, Full Debug Symbols).

  * `zipped` is like `external`, but the .debuginfo file will also be zipped
    into a `.diz` file.

When building for distribution, `zipped` is a good solution. Binaries built
with `internal` is suitable for use by developers, since they facilitate
debugging, but should be stripped before distributed to end users.

### Autoconf Details

The `configure` script is based on the autoconf framework, but in some details
deviate from a normal autoconf `configure` script.

The `configure` script in the top level directory of the JDK is just a thin
wrapper that calls `make/autoconf/configure`. This in turn will run `autoconf`
to create the runnable (generated) configure script, as
`.build/generated-configure.sh`. Apart from being responsible for the
generation of the runnable script, the `configure` script also provides
functionality that is not easily expressed in the normal Autoconf framework. As
part of this functionality, the generated script is called.

The build system will detect if the Autoconf source files have changed, and
will trigger a regeneration of the generated script if needed. You can also
manually request such an update by `bash configure autogen`.

In previous versions of the JDK, the generated script was checked in at
`make/autoconf/generated-configure.sh`. This is no longer the case.

### Developing the Build System Itself

This section contains a few remarks about how to develop for the build system
itself. It is not relevant if you are only making changes in the product source
code.

While technically using `make`, the make source files of the JDK does not
resemble most other Makefiles. Instead of listing specific targets and actions
(perhaps using patterns), the basic modus operandi is to call a high-level
function (or properly, macro) from the API in `make/common`. For instance, to
compile all classes in the `jdk.internal.foo` package in the `jdk.foo` module,
a call like this would be made:

```
$(eval $(call SetupJavaCompilation, BUILD_FOO_CLASSES, \
    SETUP := GENERATE_OLDBYTECODE, \
    SRC := $(TOPDIR)/src/jkd.foo/share/classes, \
    INCLUDES := jdk/internal/foo, \
    BIN := $(SUPPORT_OUTPUTDIR)/foo_classes, \
))
```

By encapsulating and expressing the high-level knowledge of *what* should be
done, rather than *how* it should be done (as is normal in Makefiles), we can
build a much more powerful and flexible build system.

Correct dependency tracking is paramount. Sloppy dependency tracking will lead
to improper parallelization, or worse, race conditions.

To test for/debug race conditions, try running `make JOBS=1` and `make
JOBS=100` and see if it makes any difference. (It shouldn't).

To compare the output of two different builds and see if, and how, they differ,
run `$BUILD1/compare.sh -o $BUILD2`, where `$BUILD1` and `$BUILD2` are the two
builds you want to compare.

To automatically build two consecutive versions and compare them, use
`COMPARE_BUILD`. The value of `COMPARE_BUILD` is a set of variable=value
assignments, like this:
```
make COMPARE_BUILD=CONF=--enable-new-hotspot-feature:MAKE=hotspot
```
See `make/InitSupport.gmk` for details on how to use `COMPARE_BUILD`.

To analyze build performance, run with `LOG=trace` and check `$BUILD/build-trace-time.log`.
Use `JOBS=1` to avoid parallelism.

Please check that you adhere to the [Code Conventions for the Build System](
http://openjdk.java.net/groups/build/doc/code-conventions.html) before
submitting patches.

## Contributing to the JDK

So, now you've built your JDK, and made your first patch, and want to
contribute it back to the OpenJDK Community.

First of all: Thank you! We gladly welcome your contribution.
However, please bear in mind that the JDK is a massive project, and we must ask
you to follow our rules and guidelines to be able to accept your contribution.

The official place to start is the ['How to contribute' page](
http://openjdk.java.net/contribute/). There is also an official (but somewhat
outdated and skimpy on details) [Developer's Guide](
http://openjdk.java.net/guide/).

If this seems overwhelming to you, the Adoption Group is there to help you! A
good place to start is their ['New Contributor' page](
https://wiki.openjdk.java.net/display/Adoption/New+Contributor), or start
reading the comprehensive [Getting Started Kit](
https://adoptopenjdk.gitbooks.io/adoptopenjdk-getting-started-kit/en/). The
Adoption Group will also happily answer any questions you have about
contributing. Contact them by [mail](
http://mail.openjdk.java.net/mailman/listinfo/adoption-discuss) or [IRC](
http://openjdk.java.net/irc/).

---
# Override styles from the base CSS file that are not ideal for this document.
header-includes:
 - '<style type="text/css">pre, code, tt { color: #1d6ae5; }</style>'
---
