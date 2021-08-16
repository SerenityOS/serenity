#
# Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

# Support macro for PLATFORM_EXTRACT_TARGET_AND_BUILD.
# Converts autoconf style CPU name to OpenJDK style, into
# VAR_CPU, VAR_CPU_ARCH, VAR_CPU_BITS and VAR_CPU_ENDIAN.
AC_DEFUN([PLATFORM_EXTRACT_VARS_FROM_CPU],
[
  # First argument is the cpu name from the trip/quad
  case "$1" in
    x86_64*x32)
      VAR_CPU=x32
      VAR_CPU_ARCH=x86
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=little
      ;;
    x86_64)
      VAR_CPU=x86_64
      VAR_CPU_ARCH=x86
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    i?86)
      VAR_CPU=x86
      VAR_CPU_ARCH=x86
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=little
      ;;
    alpha*)
      VAR_CPU=alpha
      VAR_CPU_ARCH=alpha
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    arm*)
      VAR_CPU=arm
      VAR_CPU_ARCH=arm
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=little
      ;;
    aarch64)
      VAR_CPU=aarch64
      VAR_CPU_ARCH=aarch64
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    ia64)
      VAR_CPU=ia64
      VAR_CPU_ARCH=ia64
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    loongarch64)
      VAR_CPU=loongarch64
      VAR_CPU_ARCH=loongarch
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    m68k)
      VAR_CPU=m68k
      VAR_CPU_ARCH=m68k
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    mips)
      VAR_CPU=mips
      VAR_CPU_ARCH=mips
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    mipsel)
      VAR_CPU=mipsel
      VAR_CPU_ARCH=mipsel
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=little
      ;;
    mips64)
      VAR_CPU=mips64
      VAR_CPU_ARCH=mips64
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=big
      ;;
    mips64el)
      VAR_CPU=mips64el
      VAR_CPU_ARCH=mips64el
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    powerpc)
      VAR_CPU=ppc
      VAR_CPU_ARCH=ppc
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    powerpc64)
      VAR_CPU=ppc64
      VAR_CPU_ARCH=ppc
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=big
      ;;
    powerpc64le)
      VAR_CPU=ppc64le
      VAR_CPU_ARCH=ppc
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    riscv64)
      VAR_CPU=riscv64
      VAR_CPU_ARCH=riscv
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=little
      ;;
    s390)
      VAR_CPU=s390
      VAR_CPU_ARCH=s390
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    s390x)
      VAR_CPU=s390x
      VAR_CPU_ARCH=s390
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=big
      ;;
    sh*eb)
      VAR_CPU=sh
      VAR_CPU_ARCH=sh
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    sh*)
      VAR_CPU=sh
      VAR_CPU_ARCH=sh
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=little
      ;;
    sparc)
      VAR_CPU=sparc
      VAR_CPU_ARCH=sparc
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    sparcv9|sparc64)
      VAR_CPU=sparcv9
      VAR_CPU_ARCH=sparc
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=big
      ;;
    sparc)
      VAR_CPU=sparc
      VAR_CPU_ARCH=sparc
      VAR_CPU_BITS=32
      VAR_CPU_ENDIAN=big
      ;;
    sparcv9|sparc64)
      VAR_CPU=sparcv9
      VAR_CPU_ARCH=sparc
      VAR_CPU_BITS=64
      VAR_CPU_ENDIAN=big
      ;;
    *)
      AC_MSG_ERROR([unsupported cpu $1])
      ;;
  esac
])

# Support macro for PLATFORM_EXTRACT_TARGET_AND_BUILD.
# Converts autoconf style OS name to OpenJDK style, into
# VAR_OS, VAR_OS_TYPE and VAR_OS_ENV.
AC_DEFUN([PLATFORM_EXTRACT_VARS_FROM_OS],
[
  case "$1" in
    *linux*)
      VAR_OS=linux
      VAR_OS_TYPE=unix
      ;;
    *darwin*)
      VAR_OS=macosx
      VAR_OS_TYPE=unix
      ;;
    *bsd*)
      VAR_OS=bsd
      VAR_OS_TYPE=unix
      ;;
    *cygwin*)
      VAR_OS=windows
      VAR_OS_ENV=windows.cygwin
      ;;
    *wsl*)
      VAR_OS=windows
      VAR_OS_ENV=windows.wsl
      ;;
    *msys*)
      VAR_OS=windows
      VAR_OS_ENV=windows.msys2
      ;;
    *aix*)
      VAR_OS=aix
      VAR_OS_TYPE=unix
      ;;
    *serenity*)
      VAR_OS=serenity
      VAR_OS_TYPE=unix
      ;;
    *)
      AC_MSG_ERROR([unsupported operating system $1])
      ;;
  esac
])

# Support macro for PLATFORM_EXTRACT_TARGET_AND_BUILD.
# Converts autoconf style OS name to OpenJDK style, into
# VAR_LIBC.
AC_DEFUN([PLATFORM_EXTRACT_VARS_FROM_LIBC],
[
  case "$1" in
    *linux*-musl)
      VAR_LIBC=musl
      ;;
    *linux*-gnu)
      VAR_LIBC=gnu
      ;;
    *)
      VAR_LIBC=default
      ;;
  esac
])

# Support macro for PLATFORM_EXTRACT_TARGET_AND_BUILD.
# Converts autoconf style OS name to OpenJDK style, into
# VAR_ABI.
AC_DEFUN([PLATFORM_EXTRACT_VARS_FROM_ABI],
[
  case "$1" in
    *linux*-musl)
      VAR_ABI=musl
      ;;
    *linux*-gnu)
      VAR_ABI=gnu
      ;;
    *linux*-gnueabi)
      VAR_ABI=gnueabi
      ;;
    *linux*-gnueabihf)
      VAR_ABI=gnueabihf
      ;;
    *linux*-gnuabi64)
      VAR_ABI=gnuabi64
      ;;
    *)
      VAR_ABI=default
      ;;
  esac
])

# Expects $host_os $host_cpu $build_os and $build_cpu
# and $with_target_bits to have been setup!
#
# Translate the standard triplet(quadruplet) definition
# of the target/build system into OPENJDK_TARGET_OS, OPENJDK_TARGET_CPU,
# OPENJDK_BUILD_OS, etc.
AC_DEFUN([PLATFORM_EXTRACT_TARGET_AND_BUILD],
[
  # Copy the autoconf trip/quadruplet verbatim to OPENJDK_TARGET_AUTOCONF_NAME
  # (from the autoconf "host") and OPENJDK_BUILD_AUTOCONF_NAME
  # Note that we might later on rewrite e.g. OPENJDK_TARGET_CPU due to reduced build,
  # but this will not change the value of OPENJDK_TARGET_AUTOCONF_NAME.
  OPENJDK_TARGET_AUTOCONF_NAME="$host"
  OPENJDK_BUILD_AUTOCONF_NAME="$build"
  AC_SUBST(OPENJDK_TARGET_AUTOCONF_NAME)
  AC_SUBST(OPENJDK_BUILD_AUTOCONF_NAME)

  # Convert the autoconf OS/CPU value to our own data, into the VAR_OS/CPU/LIBC variables.
  PLATFORM_EXTRACT_VARS_FROM_OS($build_os)
  PLATFORM_EXTRACT_VARS_FROM_CPU($build_cpu)
  PLATFORM_EXTRACT_VARS_FROM_LIBC($build_os)
  PLATFORM_EXTRACT_VARS_FROM_ABI($build_os)
  # ..and setup our own variables. (Do this explicitly to facilitate searching)
  OPENJDK_BUILD_OS="$VAR_OS"
  if test "x$VAR_OS_TYPE" != x; then
    OPENJDK_BUILD_OS_TYPE="$VAR_OS_TYPE"
  else
    OPENJDK_BUILD_OS_TYPE="$VAR_OS"
  fi
  if test "x$VAR_OS_ENV" != x; then
    OPENJDK_BUILD_OS_ENV="$VAR_OS_ENV"
  else
    OPENJDK_BUILD_OS_ENV="$VAR_OS"
  fi
  OPENJDK_BUILD_CPU="$VAR_CPU"
  OPENJDK_BUILD_CPU_ARCH="$VAR_CPU_ARCH"
  OPENJDK_BUILD_CPU_BITS="$VAR_CPU_BITS"
  OPENJDK_BUILD_CPU_ENDIAN="$VAR_CPU_ENDIAN"
  OPENJDK_BUILD_CPU_AUTOCONF="$build_cpu"
  OPENJDK_BUILD_LIBC="$VAR_LIBC"
  OPENJDK_BUILD_ABI="$VAR_ABI"
  AC_SUBST(OPENJDK_BUILD_OS)
  AC_SUBST(OPENJDK_BUILD_OS_TYPE)
  AC_SUBST(OPENJDK_BUILD_OS_ENV)
  AC_SUBST(OPENJDK_BUILD_CPU)
  AC_SUBST(OPENJDK_BUILD_CPU_ARCH)
  AC_SUBST(OPENJDK_BUILD_CPU_BITS)
  AC_SUBST(OPENJDK_BUILD_CPU_ENDIAN)
  AC_SUBST(OPENJDK_BUILD_CPU_AUTOCONF)
  AC_SUBST(OPENJDK_BUILD_LIBC)
  AC_SUBST(OPENJDK_BUILD_ABI)

  AC_MSG_CHECKING([openjdk-build os-cpu])
  AC_MSG_RESULT([$OPENJDK_BUILD_OS-$OPENJDK_BUILD_CPU])

  if test "x$OPENJDK_BUILD_OS" = "xlinux"; then
    AC_MSG_CHECKING([openjdk-build C library])
    AC_MSG_RESULT([$OPENJDK_BUILD_LIBC])
  fi

  # Convert the autoconf OS/CPU value to our own data, into the VAR_OS/CPU/LIBC variables.
  PLATFORM_EXTRACT_VARS_FROM_OS($host_os)
  PLATFORM_EXTRACT_VARS_FROM_CPU($host_cpu)
  PLATFORM_EXTRACT_VARS_FROM_LIBC($host_os)
  PLATFORM_EXTRACT_VARS_FROM_ABI($host_os)
  # ... and setup our own variables. (Do this explicitly to facilitate searching)
  OPENJDK_TARGET_OS="$VAR_OS"
  if test "x$VAR_OS_TYPE" != x; then
    OPENJDK_TARGET_OS_TYPE="$VAR_OS_TYPE"
  else
    OPENJDK_TARGET_OS_TYPE="$VAR_OS"
  fi
  if test "x$VAR_OS_ENV" != x; then
    OPENJDK_TARGET_OS_ENV="$VAR_OS_ENV"
  else
    OPENJDK_TARGET_OS_ENV="$VAR_OS"
  fi
  OPENJDK_TARGET_CPU="$VAR_CPU"
  OPENJDK_TARGET_CPU_ARCH="$VAR_CPU_ARCH"
  OPENJDK_TARGET_CPU_BITS="$VAR_CPU_BITS"
  OPENJDK_TARGET_CPU_ENDIAN="$VAR_CPU_ENDIAN"
  OPENJDK_TARGET_CPU_AUTOCONF="$host_cpu"
  OPENJDK_TARGET_OS_UPPERCASE=`$ECHO $OPENJDK_TARGET_OS | $TR 'abcdefghijklmnopqrstuvwxyz' 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'`
  OPENJDK_TARGET_LIBC="$VAR_LIBC"
  OPENJDK_TARGET_ABI="$VAR_ABI"

  AC_SUBST(OPENJDK_TARGET_OS)
  AC_SUBST(OPENJDK_TARGET_OS_TYPE)
  AC_SUBST(OPENJDK_TARGET_OS_ENV)
  AC_SUBST(OPENJDK_TARGET_OS_UPPERCASE)
  AC_SUBST(OPENJDK_TARGET_CPU)
  AC_SUBST(OPENJDK_TARGET_CPU_ARCH)
  AC_SUBST(OPENJDK_TARGET_CPU_BITS)
  AC_SUBST(OPENJDK_TARGET_CPU_ENDIAN)
  AC_SUBST(OPENJDK_TARGET_CPU_AUTOCONF)
  AC_SUBST(OPENJDK_TARGET_LIBC)
  AC_SUBST(OPENJDK_TARGET_ABI)

  AC_MSG_CHECKING([openjdk-target os-cpu])
  AC_MSG_RESULT([$OPENJDK_TARGET_OS-$OPENJDK_TARGET_CPU])

  if test "x$OPENJDK_TARGET_OS" = "xlinux"; then
    AC_MSG_CHECKING([openjdk-target C library])
    AC_MSG_RESULT([$OPENJDK_TARGET_LIBC])
  fi
])

# Check if a reduced build (32-bit on 64-bit platforms) is requested, and modify behaviour
# accordingly. Must be done after setting up build and target system, but before
# doing anything else with these values.
AC_DEFUN([PLATFORM_SETUP_TARGET_CPU_BITS],
[
  AC_ARG_WITH(target-bits, [AS_HELP_STRING([--with-target-bits],
       [build 32-bit or 64-bit binaries (for platforms that support it), e.g. --with-target-bits=32 @<:@guessed@:>@])])

  # We have three types of compiles:
  # native  == normal compilation, target system == build system
  # cross   == traditional cross compilation, target system != build system; special toolchain needed
  # reduced == using native compilers, but with special flags (e.g. -m32) to produce 32-bit builds on 64-bit machines
  #
  if test "x$OPENJDK_BUILD_AUTOCONF_NAME" != "x$OPENJDK_TARGET_AUTOCONF_NAME"; then
    # We're doing a proper cross-compilation
    COMPILE_TYPE="cross"
  else
    COMPILE_TYPE="native"
  fi

  if test "x$with_target_bits" != x; then
    if test "x$COMPILE_TYPE" = "xcross"; then
      AC_MSG_ERROR([It is not possible to combine --with-target-bits=X and proper cross-compilation. Choose either.])
    fi

    if test "x$with_target_bits" = x32 && test "x$OPENJDK_TARGET_CPU_BITS" = x64; then
      # A reduced build is requested
      COMPILE_TYPE="reduced"
      OPENJDK_TARGET_CPU_BITS=32
      if test "x$OPENJDK_TARGET_CPU_ARCH" = "xx86"; then
        OPENJDK_TARGET_CPU=x86
      elif test "x$OPENJDK_TARGET_CPU_ARCH" = "xsparc"; then
        OPENJDK_TARGET_CPU=sparc
      else
        AC_MSG_ERROR([Reduced build (--with-target-bits=32) is only supported on x86_64 and sparcv9])
      fi
    elif test "x$with_target_bits" = x64 && test "x$OPENJDK_TARGET_CPU_BITS" = x32; then
      AC_MSG_ERROR([It is not possible to use --with-target-bits=64 on a 32 bit system. Use proper cross-compilation instead.])
    elif test "x$with_target_bits" = "x$OPENJDK_TARGET_CPU_BITS"; then
      AC_MSG_NOTICE([--with-target-bits are set to build platform address size; argument has no meaning])
    else
      AC_MSG_ERROR([--with-target-bits can only be 32 or 64, you specified $with_target_bits!])
    fi
  fi
  AC_SUBST(COMPILE_TYPE)

  AC_MSG_CHECKING([compilation type])
  AC_MSG_RESULT([$COMPILE_TYPE])
])

# Setup the legacy variables, for controlling the old makefiles.
#
AC_DEFUN([PLATFORM_SETUP_LEGACY_VARS],
[
  PLATFORM_SETUP_LEGACY_VARS_HELPER([TARGET])
  PLATFORM_SETUP_LEGACY_VARS_HELPER([BUILD])
])

# $1 - Either TARGET or BUILD to setup the variables for.
AC_DEFUN([PLATFORM_SETUP_LEGACY_VARS_HELPER],
[
  # Also store the legacy naming of the cpu.
  # Ie i586 and amd64 instead of x86 and x86_64
  OPENJDK_$1_CPU_LEGACY="$OPENJDK_$1_CPU"
  if test "x$OPENJDK_$1_CPU" = xx86; then
    OPENJDK_$1_CPU_LEGACY="i586"
  elif test "x$OPENJDK_$1_OS" != xmacosx && test "x$OPENJDK_$1_CPU" = xx86_64; then
    # On all platforms except MacOSX replace x86_64 with amd64.
    OPENJDK_$1_CPU_LEGACY="amd64"
  elif test "x$OPENJDK_$1_CPU" = xalpha; then
    # Avoid name collisions with variables named alpha
    OPENJDK_$1_CPU_LEGACY="_alpha_"
  elif test "x$OPENJDK_$1_CPU" = xsh; then
    # Avoid name collisions with variables named sh
    OPENJDK_$1_CPU_LEGACY="_sh_"
  fi
  AC_SUBST(OPENJDK_$1_CPU_LEGACY)

  # And the second legacy naming of the cpu.
  # Ie i386 and amd64 instead of x86 and x86_64.
  OPENJDK_$1_CPU_LEGACY_LIB="$OPENJDK_$1_CPU"
  if test "x$OPENJDK_$1_CPU" = xx86; then
    OPENJDK_$1_CPU_LEGACY_LIB="i386"
  elif test "x$OPENJDK_$1_CPU" = xx86_64; then
    OPENJDK_$1_CPU_LEGACY_LIB="amd64"
  fi
  AC_SUBST(OPENJDK_$1_CPU_LEGACY_LIB)

  # Setup OPENJDK_$1_CPU_OSARCH, which is used to set the os.arch Java system property
  OPENJDK_$1_CPU_OSARCH="$OPENJDK_$1_CPU"
  if test "x$OPENJDK_$1_OS" = xlinux && test "x$OPENJDK_$1_CPU" = xx86; then
    # On linux only, we replace x86 with i386.
    OPENJDK_$1_CPU_OSARCH="i386"
  elif test "x$OPENJDK_$1_OS" != xmacosx && test "x$OPENJDK_$1_CPU" = xx86_64; then
    # On all platforms except macosx, we replace x86_64 with amd64.
    OPENJDK_$1_CPU_OSARCH="amd64"
  fi
  AC_SUBST(OPENJDK_$1_CPU_OSARCH)

  OPENJDK_$1_CPU_JLI="$OPENJDK_$1_CPU"
  if test "x$OPENJDK_$1_CPU" = xx86; then
    OPENJDK_$1_CPU_JLI="i386"
  elif test "x$OPENJDK_$1_OS" != xmacosx && test "x$OPENJDK_$1_CPU" = xx86_64; then
    # On all platforms except macosx, we replace x86_64 with amd64.
    OPENJDK_$1_CPU_JLI="amd64"
  fi

  # The new version string in JDK 9 also defined new naming of OS and ARCH for bundles
  # The macOS bundle name was revised in JDK 17
  #
  # macosx is macos and x86_64 is x64
  if test "x$OPENJDK_$1_OS" = xmacosx; then
    OPENJDK_$1_OS_BUNDLE="macos"
  else
    OPENJDK_$1_OS_BUNDLE="$OPENJDK_TARGET_OS"
  fi
  if test "x$OPENJDK_$1_CPU" = xx86_64; then
    OPENJDK_$1_CPU_BUNDLE="x64"
  else
    OPENJDK_$1_CPU_BUNDLE="$OPENJDK_$1_CPU"
  fi

  OPENJDK_$1_LIBC_BUNDLE=""
  if test "x$OPENJDK_$1_LIBC" = "xmusl"; then
    OPENJDK_$1_LIBC_BUNDLE="-$OPENJDK_$1_LIBC"
  fi

  OPENJDK_$1_BUNDLE_PLATFORM="${OPENJDK_$1_OS_BUNDLE}-${OPENJDK_$1_CPU_BUNDLE}${OPENJDK_$1_LIBC_BUNDLE}"
  AC_SUBST(OPENJDK_$1_BUNDLE_PLATFORM)

  if test "x$COMPILE_TYPE" = "xcross"; then
    # FIXME: ... or should this include reduced builds..?
    DEFINE_CROSS_COMPILE_ARCH="CROSS_COMPILE_ARCH:=$OPENJDK_$1_CPU_LEGACY"
  else
    DEFINE_CROSS_COMPILE_ARCH=""
  fi
  AC_SUBST(DEFINE_CROSS_COMPILE_ARCH)

  # Convert openjdk platform names to hotspot names

  HOTSPOT_$1_OS=${OPENJDK_$1_OS}
  if test "x$OPENJDK_$1_OS" = xmacosx; then
    HOTSPOT_$1_OS=bsd
  fi
  AC_SUBST(HOTSPOT_$1_OS)

  HOTSPOT_$1_OS_TYPE=${OPENJDK_$1_OS_TYPE}
  if test "x$OPENJDK_$1_OS_TYPE" = xunix; then
    HOTSPOT_$1_OS_TYPE=posix
  fi
  AC_SUBST(HOTSPOT_$1_OS_TYPE)

  HOTSPOT_$1_CPU=${OPENJDK_$1_CPU}
  if test "x$OPENJDK_$1_CPU" = xx86; then
    HOTSPOT_$1_CPU=x86_32
  elif test "x$OPENJDK_$1_CPU" = xsparcv9; then
    HOTSPOT_$1_CPU=sparc
  elif test "x$OPENJDK_$1_CPU" = xppc64; then
    HOTSPOT_$1_CPU=ppc_64
  elif test "x$OPENJDK_$1_CPU" = xppc64le; then
    HOTSPOT_$1_CPU=ppc_64
  fi
  AC_SUBST(HOTSPOT_$1_CPU)

  # This is identical with OPENJDK_*, but define anyway for consistency.
  HOTSPOT_$1_CPU_ARCH=${OPENJDK_$1_CPU_ARCH}
  AC_SUBST(HOTSPOT_$1_CPU_ARCH)

  # Setup HOTSPOT_$1_CPU_DEFINE
  if test "x$OPENJDK_$1_CPU" = xx86; then
    HOTSPOT_$1_CPU_DEFINE=IA32
  elif test "x$OPENJDK_$1_CPU" = xx86_64; then
    HOTSPOT_$1_CPU_DEFINE=AMD64
  elif test "x$OPENJDK_$1_CPU" = xx32; then
    HOTSPOT_$1_CPU_DEFINE=X32
  elif test "x$OPENJDK_$1_CPU" = xsparcv9; then
    HOTSPOT_$1_CPU_DEFINE=SPARC
  elif test "x$OPENJDK_$1_CPU" = xaarch64; then
    HOTSPOT_$1_CPU_DEFINE=AARCH64
  elif test "x$OPENJDK_$1_CPU" = xppc64; then
    HOTSPOT_$1_CPU_DEFINE=PPC64
  elif test "x$OPENJDK_$1_CPU" = xppc64le; then
    HOTSPOT_$1_CPU_DEFINE=PPC64

  # The cpu defines below are for zero, we don't support them directly.
  elif test "x$OPENJDK_$1_CPU" = xsparc; then
    HOTSPOT_$1_CPU_DEFINE=SPARC
  elif test "x$OPENJDK_$1_CPU" = xppc; then
    HOTSPOT_$1_CPU_DEFINE=PPC32
  elif test "x$OPENJDK_$1_CPU" = xs390; then
    HOTSPOT_$1_CPU_DEFINE=S390
  elif test "x$OPENJDK_$1_CPU" = xs390x; then
    HOTSPOT_$1_CPU_DEFINE=S390
  elif test "x$OPENJDK_$1_CPU" = xriscv64; then
    HOTSPOT_$1_CPU_DEFINE=RISCV
  elif test "x$OPENJDK_$1_CPU" != x; then
    HOTSPOT_$1_CPU_DEFINE=$(echo $OPENJDK_$1_CPU | tr a-z A-Z)
  fi
  AC_SUBST(HOTSPOT_$1_CPU_DEFINE)

  HOTSPOT_$1_LIBC=$OPENJDK_$1_LIBC
  AC_SUBST(HOTSPOT_$1_LIBC)

  # For historical reasons, the OS include directories have odd names.
  OPENJDK_$1_OS_INCLUDE_SUBDIR="$OPENJDK_TARGET_OS"
  if test "x$OPENJDK_TARGET_OS" = "xwindows"; then
    OPENJDK_$1_OS_INCLUDE_SUBDIR="win32"
  elif test "x$OPENJDK_TARGET_OS" = "xmacosx"; then
    OPENJDK_$1_OS_INCLUDE_SUBDIR="darwin"
  fi
  AC_SUBST(OPENJDK_$1_OS_INCLUDE_SUBDIR)
])

AC_DEFUN([PLATFORM_SET_RELEASE_FILE_OS_VALUES],
[
  if test "x$OPENJDK_TARGET_OS" = "xlinux"; then
    RELEASE_FILE_OS_NAME=Linux
  fi
  if test "x$OPENJDK_TARGET_OS" = "xwindows"; then
    RELEASE_FILE_OS_NAME=Windows
  fi
  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    RELEASE_FILE_OS_NAME="Darwin"
  fi
  if test "x$OPENJDK_TARGET_OS" = "xaix"; then
    RELEASE_FILE_OS_NAME="AIX"
  fi
  RELEASE_FILE_OS_ARCH=${OPENJDK_TARGET_CPU}
  RELEASE_FILE_LIBC=${OPENJDK_TARGET_LIBC}

  AC_SUBST(RELEASE_FILE_OS_NAME)
  AC_SUBST(RELEASE_FILE_OS_ARCH)
  AC_SUBST(RELEASE_FILE_LIBC)
])

AC_DEFUN([PLATFORM_SET_MODULE_TARGET_OS_VALUES],
[
  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    OPENJDK_MODULE_TARGET_OS_NAME="macos"
  else
    OPENJDK_MODULE_TARGET_OS_NAME="$OPENJDK_TARGET_OS"
  fi

  if test "x$OPENJDK_TARGET_CPU" = xx86_64; then
    OPENJDK_MODULE_TARGET_OS_ARCH="amd64"
  else
    OPENJDK_MODULE_TARGET_OS_ARCH="$OPENJDK_TARGET_CPU"
  fi

  OPENJDK_MODULE_TARGET_PLATFORM="${OPENJDK_MODULE_TARGET_OS_NAME}-${OPENJDK_MODULE_TARGET_OS_ARCH}"
  AC_SUBST(OPENJDK_MODULE_TARGET_PLATFORM)
])

#%%% Build and target systems %%%
AC_DEFUN_ONCE([PLATFORM_SETUP_OPENJDK_BUILD_AND_TARGET],
[
  # Figure out the build and target systems. # Note that in autoconf terminology, "build" is obvious, but "target"
  # is confusing; it assumes you are cross-compiling a cross-compiler (!)  and "target" is thus the target of the
  # product you're building. The target of this build is called "host". Since this is confusing to most people, we
  # have not adopted that system, but use "target" as the platform we are building for. In some places though we need
  # to use the configure naming style.
  AC_CANONICAL_BUILD
  AC_CANONICAL_HOST
  AC_CANONICAL_TARGET

  PLATFORM_EXTRACT_TARGET_AND_BUILD
  PLATFORM_SETUP_TARGET_CPU_BITS
  PLATFORM_SET_MODULE_TARGET_OS_VALUES
  PLATFORM_SET_RELEASE_FILE_OS_VALUES
  PLATFORM_SETUP_LEGACY_VARS

  # Deprecated in JDK 15
  UTIL_DEPRECATED_ARG_ENABLE(deprecated-ports)
])

AC_DEFUN_ONCE([PLATFORM_SETUP_OPENJDK_BUILD_OS_VERSION],
[
  ###############################################################################

  # Note that this is the build platform OS version!

  OS_VERSION="`uname -r | ${SED} 's!\.! !g' | ${SED} 's!-! !g'`"
  OS_VERSION_MAJOR="`${ECHO} ${OS_VERSION} | ${CUT} -f 1 -d ' '`"
  OS_VERSION_MINOR="`${ECHO} ${OS_VERSION} | ${CUT} -f 2 -d ' '`"
  OS_VERSION_MICRO="`${ECHO} ${OS_VERSION} | ${CUT} -f 3 -d ' '`"
  AC_SUBST(OS_VERSION_MAJOR)
  AC_SUBST(OS_VERSION_MINOR)
  AC_SUBST(OS_VERSION_MICRO)
])

AC_DEFUN_ONCE([PLATFORM_SETUP_OPENJDK_TARGET_BITS],
[
  ###############################################################################
  #
  # Now we check if libjvm.so will use 32 or 64 bit pointers for the C/C++ code.
  # (The JVM can use 32 or 64 bit Java pointers but that decision
  # is made at runtime.)
  #

  # Make compilation sanity check
  AC_CHECK_HEADERS([stdio.h], , [
    AC_MSG_NOTICE([Failed to compile stdio.h. This likely implies missing compile dependencies.])
    if test "x$COMPILE_TYPE" = xreduced; then
      HELP_MSG_MISSING_DEPENDENCY([reduced])
      AC_MSG_NOTICE([You are doing a reduced build. Check that you have 32-bit libraries installed. $HELP_MSG])
    elif test "x$COMPILE_TYPE" = xcross; then
      AC_MSG_NOTICE([You are doing a cross-compilation. Check that you have all target platform libraries installed.])
    fi
    AC_MSG_ERROR([Cannot continue.])
  ])

  AC_CHECK_SIZEOF([int *], [1111])

  # AC_CHECK_SIZEOF defines 'ac_cv_sizeof_int_p' to hold the number of bytes used by an 'int*'
  if test "x$ac_cv_sizeof_int_p" = x; then
    # The test failed, lets stick to the assumed value.
    AC_MSG_WARN([The number of bits in the target could not be determined, using $OPENJDK_TARGET_CPU_BITS.])
  else
    TESTED_TARGET_CPU_BITS=`expr 8 \* $ac_cv_sizeof_int_p`

    if test "x$TESTED_TARGET_CPU_BITS" != "x$OPENJDK_TARGET_CPU_BITS"; then
      AC_MSG_NOTICE([The tested number of bits in the target ($TESTED_TARGET_CPU_BITS) differs from the number of bits expected to be found in the target ($OPENJDK_TARGET_CPU_BITS)])
      if test "x$COMPILE_TYPE" = xreduced; then
        HELP_MSG_MISSING_DEPENDENCY([reduced])
        AC_MSG_NOTICE([You are doing a reduced build. Check that you have 32-bit libraries installed. $HELP_MSG])
      elif test "x$COMPILE_TYPE" = xcross; then
        AC_MSG_NOTICE([You are doing a cross-compilation. Check that you have all target platform libraries installed.])
      fi
      AC_MSG_ERROR([Cannot continue.])
    fi
  fi

  AC_MSG_CHECKING([for target address size])
  AC_MSG_RESULT([$OPENJDK_TARGET_CPU_BITS bits])
])

AC_DEFUN_ONCE([PLATFORM_SETUP_OPENJDK_TARGET_ENDIANNESS],
[
  ###############################################################################
  #
  # Is the target little of big endian?
  #
  AC_C_BIGENDIAN([ENDIAN="big"],[ENDIAN="little"],[ENDIAN="unknown"],[ENDIAN="universal_endianness"])

  if test "x$ENDIAN" = xuniversal_endianness; then
    AC_MSG_ERROR([Building with both big and little endianness is not supported])
  fi
  if test "x$ENDIAN" != "x$OPENJDK_TARGET_CPU_ENDIAN"; then
    AC_MSG_ERROR([The tested endian in the target ($ENDIAN) differs from the endian expected to be found in the target ($OPENJDK_TARGET_CPU_ENDIAN)])
  fi
])
