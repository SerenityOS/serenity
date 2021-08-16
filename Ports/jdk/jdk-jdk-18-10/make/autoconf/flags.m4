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

m4_include([flags-cflags.m4])
m4_include([flags-ldflags.m4])
m4_include([flags-other.m4])

################################################################################
#
# Setup ABI profile (for arm)
#
AC_DEFUN([FLAGS_SETUP_ABI_PROFILE],
[
  AC_ARG_WITH(abi-profile, [AS_HELP_STRING([--with-abi-profile],
      [specify ABI profile for ARM builds (arm-vfp-sflt,arm-vfp-hflt,arm-sflt, armv5-vfp-sflt,armv6-vfp-hflt,aarch64) @<:@toolchain dependent@:>@ ])])

  if test "x$with_abi_profile" != x; then
    if test "x$OPENJDK_TARGET_CPU" != xarm && \
        test "x$OPENJDK_TARGET_CPU" != xaarch64; then
      AC_MSG_ERROR([--with-abi-profile only available on arm/aarch64])
    fi

    OPENJDK_TARGET_ABI_PROFILE=$with_abi_profile
    AC_MSG_CHECKING([for ABI profle])
    AC_MSG_RESULT([$OPENJDK_TARGET_ABI_PROFILE])

    # --- Arm-sflt CFLAGS and ASFLAGS ---
    # Armv5te is required for assembler, because pld insn used in arm32 hotspot is only in v5E and above.
    # However, there is also a GCC bug which generates unaligned strd/ldrd instructions on armv5te:
    # https://gcc.gnu.org/bugzilla/show_bug.cgi?id=82445, and it was fixed in gcc 7.1.
    # The resulting compromise is to enable v5TE for assembler and let GCC generate code for v5T.
    if test "x$OPENJDK_TARGET_ABI_PROFILE" = xarm-vfp-sflt; then
      ARM_FLOAT_TYPE=vfp-sflt
      ARM_ARCH_TYPE_FLAGS='-march=armv7-a -mthumb'
      ARM_ARCH_TYPE_ASFLAGS='-march=armv7-a -mthumb'
    elif test "x$OPENJDK_TARGET_ABI_PROFILE" = xarm-vfp-hflt; then
      ARM_FLOAT_TYPE=vfp-hflt
      ARM_ARCH_TYPE_FLAGS='-march=armv7-a -mthumb'
      ARM_ARCH_TYPE_ASFLAGS='-march=armv7-a -mthumb'
    elif test "x$OPENJDK_TARGET_ABI_PROFILE" = xarm-sflt; then
      ARM_FLOAT_TYPE=sflt
      ARM_ARCH_TYPE_FLAGS='-march=armv5t -marm'
      ARM_ARCH_TYPE_ASFLAGS='-march=armv5te'
    elif test "x$OPENJDK_TARGET_ABI_PROFILE" = xarmv5-vfp-sflt; then
      ARM_FLOAT_TYPE=vfp-sflt
      ARM_ARCH_TYPE_FLAGS='-march=armv5t -marm'
      ARM_ARCH_TYPE_ASFLAGS='-march=armv5te'
    elif test "x$OPENJDK_TARGET_ABI_PROFILE" = xarmv6-vfp-hflt; then
      ARM_FLOAT_TYPE=vfp-hflt
      ARM_ARCH_TYPE_FLAGS='-march=armv6 -marm'
      ARM_ARCH_TYPE_ASFLAGS='-march=armv6'
    elif test "x$OPENJDK_TARGET_ABI_PROFILE" = xaarch64; then
      # No special flags, just need to trigger setting JDK_ARCH_ABI_PROP_NAME
      ARM_FLOAT_TYPE=
      ARM_ARCH_TYPE_FLAGS=
      ARM_ARCH_TYPE_ASFLAGS=
    else
      AC_MSG_ERROR([Invalid ABI profile: "$OPENJDK_TARGET_ABI_PROFILE"])
    fi

    if test "x$ARM_FLOAT_TYPE" = xvfp-sflt; then
      ARM_FLOAT_TYPE_FLAGS='-mfloat-abi=softfp -mfpu=vfp -DFLOAT_ARCH=-vfp-sflt'
      ARM_FLOAT_TYPE_ASFLAGS="-mfloat-abi=softfp -mfpu=vfp"
    elif test "x$ARM_FLOAT_TYPE" = xvfp-hflt; then
      ARM_FLOAT_TYPE_FLAGS='-mfloat-abi=hard -mfpu=vfp -DFLOAT_ARCH=-vfp-hflt'
      ARM_FLOAT_TYPE_ASFLAGS="-mfloat-abi=hard -mfpu=vfp"
    elif test "x$ARM_FLOAT_TYPE" = xsflt; then
      ARM_FLOAT_TYPE_FLAGS='-msoft-float -mfpu=vfp'
      ARM_FLOAT_TYPE_ASFLAGS="-mfloat-abi=soft -mfpu=vfp"
    fi
    AC_MSG_CHECKING([for $ARM_FLOAT_TYPE floating point flags])
    AC_MSG_RESULT([$ARM_FLOAT_TYPE_FLAGS])
    AC_MSG_CHECKING([for $ARM_FLOAT_TYPE floating point flags for assembler])
    AC_MSG_RESULT([$ARM_FLOAT_TYPE_ASFLAGS])

    AC_MSG_CHECKING([for arch type flags])
    AC_MSG_RESULT([$ARM_ARCH_TYPE_FLAGS])
    AC_MSG_CHECKING([for arch type flags for assembler])
    AC_MSG_RESULT([$ARM_ARCH_TYPE_ASFLAGS])

    # Now set JDK_ARCH_ABI_PROP_NAME. This is equivalent to the last part of the
    # autoconf target triplet.
    [ JDK_ARCH_ABI_PROP_NAME=`$ECHO $OPENJDK_TARGET_AUTOCONF_NAME | $SED -e 's/.*-\([^-]*\)$/\1/'` ]
    # Sanity check that it is a known ABI.
    if test "x$JDK_ARCH_ABI_PROP_NAME" != xgnu && \
        test "x$JDK_ARCH_ABI_PROP_NAME" != xgnueabi  && \
        test "x$JDK_ARCH_ABI_PROP_NAME" != xgnueabihf; then
          AC_MSG_WARN([Unknown autoconf target triplet ABI: "$JDK_ARCH_ABI_PROP_NAME"])
    fi
    AC_MSG_CHECKING([for ABI property name])
    AC_MSG_RESULT([$JDK_ARCH_ABI_PROP_NAME])
    AC_SUBST(JDK_ARCH_ABI_PROP_NAME)
  fi

  # When building with an abi profile, the name of that profile is appended on the
  # bundle platform, which is used in bundle names.
  if test "x$OPENJDK_TARGET_ABI_PROFILE" != x; then
    OPENJDK_TARGET_BUNDLE_PLATFORM="$OPENJDK_TARGET_OS_BUNDLE-$OPENJDK_TARGET_ABI_PROFILE"
  fi
])

AC_DEFUN([FLAGS_SETUP_MACOSX_VERSION],
[
  # Additional macosx handling
  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    # The expected format for <version> is either nn.n.n or nn.nn.nn. See
    # /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/AvailabilityVersions.h

    # MACOSX_VERSION_MIN specifies the lowest version of Macosx that the built
    # binaries should be compatible with, even if compiled on a newer version
    # of the OS. It currently has a hard coded value. Setting this also limits
    # exposure to API changes in header files. Bumping this is likely to
    # require code changes to build.
    if test "x$OPENJDK_TARGET_CPU_ARCH" = xaarch64; then
      MACOSX_VERSION_MIN=11.00.00
    else
      MACOSX_VERSION_MIN=10.12.0
    fi
    MACOSX_VERSION_MIN_NODOTS=${MACOSX_VERSION_MIN//\./}

    AC_SUBST(MACOSX_VERSION_MIN)

    # Setting --with-macosx-version-max=<version> makes it an error to build or
    # link to macosx APIs that are newer than the given OS version.
    AC_ARG_WITH([macosx-version-max], [AS_HELP_STRING([--with-macosx-version-max],
        [error on use of newer functionality. @<:@macosx@:>@])],
        [
          if echo "$with_macosx_version_max" | $GREP -q "^[[0-9]][[0-9]]\.[[0-9]]\.[[0-9]]\$"; then
              MACOSX_VERSION_MAX=$with_macosx_version_max
          elif echo "$with_macosx_version_max" | $GREP -q "^[[0-9]][[0-9]]\.[[0-9]][[0-9]]\.[[0-9]][[0-9]]\$"; then
              MACOSX_VERSION_MAX=$with_macosx_version_max
          elif test "x$with_macosx_version_max" = "xno"; then
              # Use build system default
              MACOSX_VERSION_MAX=
          else
              AC_MSG_ERROR([osx version format must be nn.n.n or nn.nn.nn])
          fi
        ],
        [MACOSX_VERSION_MAX=]
    )
    MACOSX_VERSION_MAX_NODOTS=${MACOSX_VERSION_MAX//\./}

    AC_SUBST(MACOSX_VERSION_MAX)
  fi
])

# Reset the global CFLAGS/LDFLAGS variables and initialize them with the
# corresponding configure arguments instead
AC_DEFUN_ONCE([FLAGS_SETUP_USER_SUPPLIED_FLAGS],
[
  if test "x$CFLAGS" != "x"; then
    AC_MSG_WARN([Ignoring CFLAGS($CFLAGS) found in environment. Use --with-extra-cflags])
  fi

  if test "x$CXXFLAGS" != "x"; then
    AC_MSG_WARN([Ignoring CXXFLAGS($CXXFLAGS) found in environment. Use --with-extra-cxxflags])
  fi

  if test "x$LDFLAGS" != "x"; then
    AC_MSG_WARN([Ignoring LDFLAGS($LDFLAGS) found in environment. Use --with-extra-ldflags])
  fi

  if test "x$ASFLAGS" != "x"; then
    AC_MSG_WARN([Ignoring ASFLAGS($ASFLAGS) found in environment. Use --with-extra-asflags])
  fi

  AC_ARG_WITH(extra-cflags, [AS_HELP_STRING([--with-extra-cflags],
      [extra flags to be used when compiling jdk c-files])])

  AC_ARG_WITH(extra-cxxflags, [AS_HELP_STRING([--with-extra-cxxflags],
      [extra flags to be used when compiling jdk c++-files])])

  AC_ARG_WITH(extra-ldflags, [AS_HELP_STRING([--with-extra-ldflags],
      [extra flags to be used when linking jdk])])

  AC_ARG_WITH(extra-asflags, [AS_HELP_STRING([--with-extra-asflags],
      [extra flags to be passed to the assembler])])

  USER_CFLAGS="$with_extra_cflags"
  USER_CXXFLAGS="$with_extra_cxxflags"
  USER_LDFLAGS="$with_extra_ldflags"
  USER_ASFLAGS="$with_extra_asflags"
])

# Setup the sysroot flags and add them to global CFLAGS and LDFLAGS so
# that configure can use them while detecting compilers.
# TOOLCHAIN_TYPE is available here.
# Param 1 - Optional prefix to all variables. (e.g BUILD_)
AC_DEFUN([FLAGS_SETUP_SYSROOT_FLAGS],
[
  if test "x[$]$1SYSROOT" != "x"; then
    if test "x$TOOLCHAIN_TYPE" = xgcc; then
      $1SYSROOT_CFLAGS="--sysroot=[$]$1SYSROOT"
      $1SYSROOT_LDFLAGS="--sysroot=[$]$1SYSROOT"
    elif test "x$TOOLCHAIN_TYPE" = xclang; then
      $1SYSROOT_CFLAGS="-isysroot [$]$1SYSROOT"
      $1SYSROOT_LDFLAGS="-isysroot [$]$1SYSROOT"
    fi
  fi

  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    # We also need -iframework<path>/System/Library/Frameworks
    $1SYSROOT_CFLAGS="[$]$1SYSROOT_CFLAGS -iframework [$]$1SYSROOT/System/Library/Frameworks"
    $1SYSROOT_LDFLAGS="[$]$1SYSROOT_LDFLAGS -iframework [$]$1SYSROOT/System/Library/Frameworks"
    if test -d "[$]$1SYSROOT/System/Library/Frameworks/JavaVM.framework/Frameworks" ; then
      # These always need to be set on macOS 10.X, or we can't find the frameworks embedded in JavaVM.framework
      # set this here so it doesn't have to be peppered throughout the forest
      $1SYSROOT_CFLAGS="[$]$1SYSROOT_CFLAGS -F [$]$1SYSROOT/System/Library/Frameworks/JavaVM.framework/Frameworks"
      $1SYSROOT_LDFLAGS="[$]$1SYSROOT_LDFLAGS -F [$]$1SYSROOT/System/Library/Frameworks/JavaVM.framework/Frameworks"
    fi
  fi

  # For the microsoft toolchain, we need to get the SYSROOT flags from the
  # Visual Studio environment. Currently we cannot handle this as a separate
  # build toolchain.
  if test "x$1" = x && test "x$OPENJDK_BUILD_OS" = "xwindows" \
      && test "x$TOOLCHAIN_TYPE" = "xmicrosoft"; then
    TOOLCHAIN_SETUP_VISUAL_STUDIO_ENV
  fi

  AC_SUBST($1SYSROOT_CFLAGS)
  AC_SUBST($1SYSROOT_LDFLAGS)
])

AC_DEFUN_ONCE([FLAGS_PRE_TOOLCHAIN],
[
  # We should always include user supplied flags
  FLAGS_SETUP_USER_SUPPLIED_FLAGS

  # The sysroot flags are needed for configure to be able to run the compilers
  FLAGS_SETUP_SYSROOT_FLAGS

  # For xlc, the word size flag is required for correct behavior.
  # For clang/gcc, the flag is only strictly required for reduced builds, but
  # set it always where possible (x86 and ppc).
  if test "x$TOOLCHAIN_TYPE" = xxlc; then
    MACHINE_FLAG="-q${OPENJDK_TARGET_CPU_BITS}"
  elif test "x$TOOLCHAIN_TYPE" = xgcc || test "x$TOOLCHAIN_TYPE" = xclang; then
    if test "x$OPENJDK_TARGET_CPU_ARCH" = xx86 &&
        test "x$OPENJDK_TARGET_CPU" != xx32 ||
        test "x$OPENJDK_TARGET_CPU_ARCH" = xppc; then
      MACHINE_FLAG="-m${OPENJDK_TARGET_CPU_BITS}"
    fi
  fi

  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    if test "x$OPENJDK_TARGET_CPU" = xaarch64; then
      MACHINE_FLAG="$MACHINE_FLAG -arch arm64"
    elif test "x$OPENJDK_TARGET_CPU" = xx86_64; then
      MACHINE_FLAG="$MACHINE_FLAG -arch x86_64"
    fi
  fi

  # FIXME: global flags are not used yet...
  # The "global" flags will *always* be set. Without them, it is not possible to
  # get a working compilation.
  GLOBAL_CFLAGS="$MACHINE_FLAG $SYSROOT_CFLAGS $USER_CFLAGS"
  GLOBAL_CXXFLAGS="$MACHINE_FLAG $SYSROOT_CFLAGS $USER_CXXFLAGS"
  GLOBAL_LDFLAGS="$MACHINE_FLAG $SYSROOT_LDFLAGS $USER_LDFLAGS"
  # FIXME: Don't really know how to do with this, but this was the old behavior
  GLOBAL_CPPFLAGS="$SYSROOT_CFLAGS"

  # FIXME: For compatilibity, export this as EXTRA_CFLAGS for now.
  EXTRA_CFLAGS="$MACHINE_FLAG $USER_CFLAGS"
  EXTRA_CXXFLAGS="$MACHINE_FLAG $USER_CXXFLAGS"
  EXTRA_LDFLAGS="$MACHINE_FLAG $USER_LDFLAGS"
  EXTRA_ASFLAGS="$USER_ASFLAGS"

  AC_SUBST(EXTRA_CFLAGS)
  AC_SUBST(EXTRA_CXXFLAGS)
  AC_SUBST(EXTRA_LDFLAGS)
  AC_SUBST(EXTRA_ASFLAGS)

  # For autoconf testing to work, the global flags must also be stored in the
  # "unnamed" CFLAGS etc.
  CFLAGS="$GLOBAL_CFLAGS"
  CXXFLAGS="$GLOBAL_CXXFLAGS"
  LDFLAGS="$GLOBAL_LDFLAGS"
  CPPFLAGS="$GLOBAL_CPPFLAGS"

  if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # When autoconf sends both compiler and linker flags to cl.exe at the same
    # time, linker flags must be last at the command line. Achieve this by
    # moving them to LIBS.
    LIBS="$LIBS -link $LDFLAGS"
    LDFLAGS=""
  fi
])

AC_DEFUN([FLAGS_SETUP_TOOLCHAIN_CONTROL],
[
  # COMPILER_TARGET_BITS_FLAG  : option for selecting 32- or 64-bit output
  # COMPILER_COMMAND_FILE_FLAG : option for passing a command file to the compiler
  # COMPILER_BINDCMD_FILE_FLAG : option for specifying a file which saves the binder
  #                              commands produced by the link step (currently AIX only)
  if test "x$TOOLCHAIN_TYPE" = xxlc; then
    COMPILER_TARGET_BITS_FLAG="-q"
    COMPILER_COMMAND_FILE_FLAG="-f"
    COMPILER_BINDCMD_FILE_FLAG="-bloadmap:"
  else
    COMPILER_TARGET_BITS_FLAG="-m"
    COMPILER_COMMAND_FILE_FLAG="@"
    COMPILER_BINDCMD_FILE_FLAG=""

    # Check if @file is supported by gcc
    if test "x$TOOLCHAIN_TYPE" = xgcc; then
      AC_MSG_CHECKING([if @file is supported by gcc])
      # Extra emtpy "" to prevent ECHO from interpreting '--version' as argument
      $ECHO "" "--version" > command.file
      if $CXX @command.file 2>&AS_MESSAGE_LOG_FD >&AS_MESSAGE_LOG_FD; then
        AC_MSG_RESULT(yes)
        COMPILER_COMMAND_FILE_FLAG="@"
      else
        AC_MSG_RESULT(no)
        COMPILER_COMMAND_FILE_FLAG=
      fi
      $RM command.file
    fi
  fi

  AC_SUBST(COMPILER_TARGET_BITS_FLAG)
  AC_SUBST(COMPILER_COMMAND_FILE_FLAG)
  AC_SUBST(COMPILER_BINDCMD_FILE_FLAG)

  # Check that the compiler supports -mX (or -qX on AIX) flags
  # Set COMPILER_SUPPORTS_TARGET_BITS_FLAG to 'true' if it does
  FLAGS_COMPILER_CHECK_ARGUMENTS(ARGUMENT: [${COMPILER_TARGET_BITS_FLAG}${OPENJDK_TARGET_CPU_BITS}],
      IF_TRUE: [COMPILER_SUPPORTS_TARGET_BITS_FLAG=true],
      IF_FALSE: [COMPILER_SUPPORTS_TARGET_BITS_FLAG=false])
  AC_SUBST(COMPILER_SUPPORTS_TARGET_BITS_FLAG)

  if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    CC_OUT_OPTION=-Fo
    LD_OUT_OPTION=-out:
    AR_OUT_OPTION=-out:
  else
    # The option used to specify the target .o,.a or .so file.
    # When compiling, how to specify the to be created object file.
    CC_OUT_OPTION='-o$(SPACE)'
    # When linking, how to specify the output
    LD_OUT_OPTION='-o$(SPACE)'
    # When archiving, how to specify the destination static archive.
    if test "x$OPENJDK_TARGET_OS" = xmacosx; then
      AR_OUT_OPTION='-r -cs$(SPACE)'
    else
      AR_OUT_OPTION='-rcs$(SPACE)'
    fi
  fi
  AC_SUBST(CC_OUT_OPTION)
  AC_SUBST(LD_OUT_OPTION)
  AC_SUBST(AR_OUT_OPTION)

  # Generate make dependency files
  if test "x$TOOLCHAIN_TYPE" = xgcc; then
    GENDEPS_FLAGS="-MMD -MF"
  elif test "x$TOOLCHAIN_TYPE" = xclang; then
    GENDEPS_FLAGS="-MMD -MF"
  elif test "x$TOOLCHAIN_TYPE" = xxlc; then
    GENDEPS_FLAGS="-qmakedep=gcc -MF"
  fi
  AC_SUBST(GENDEPS_FLAGS)
])

AC_DEFUN_ONCE([FLAGS_POST_TOOLCHAIN],
[
  FLAGS_SETUP_TOOLCHAIN_CONTROL

  if test "x$BUILD_SYSROOT" != x; then
    FLAGS_SETUP_SYSROOT_FLAGS([BUILD_])
  else
    if test "x$COMPILE_TYPE" != "xcross"; then
      BUILD_SYSROOT_CFLAGS="$SYSROOT_CFLAGS"
      BUILD_SYSROOT_LDFLAGS="$SYSROOT_LDFLAGS"
    fi
  fi
])

AC_DEFUN([FLAGS_SETUP_FLAGS],
[
  FLAGS_SETUP_MACOSX_VERSION
  FLAGS_SETUP_ABI_PROFILE

  FLAGS_SETUP_SHARED_LIBS
  FLAGS_SETUP_DEBUG_SYMBOLS
  FLAGS_SETUP_WARNINGS
  FLAGS_SETUP_QUALITY_CHECKS
  FLAGS_SETUP_OPTIMIZATION

  FLAGS_SETUP_CFLAGS
  FLAGS_SETUP_LDFLAGS

  FLAGS_SETUP_ARFLAGS
  FLAGS_SETUP_STRIPFLAGS
  FLAGS_SETUP_RCFLAGS

  FLAGS_SETUP_ASFLAGS
  FLAGS_SETUP_ASFLAGS_CPU_DEP([TARGET])
  FLAGS_SETUP_ASFLAGS_CPU_DEP([BUILD], [OPENJDK_BUILD_])
])

# FLAGS_C_COMPILER_CHECK_ARGUMENTS(ARGUMENT: [ARGUMENT], IF_TRUE: [RUN-IF-TRUE],
#                                  IF_FALSE: [RUN-IF-FALSE])
# ------------------------------------------------------------
# Check that the C compiler supports an argument
UTIL_DEFUN_NAMED([FLAGS_C_COMPILER_CHECK_ARGUMENTS],
    [*ARGUMENT IF_TRUE IF_FALSE PREFIX], [$@],
[
  AC_MSG_CHECKING([if ARG_PREFIX[CC] supports "ARG_ARGUMENT"])
  supports=yes

  saved_cflags="$CFLAGS"
  saved_cc="$CC"
  CFLAGS="$CFLAGS $CFLAGS_WARNINGS_ARE_ERRORS ARG_ARGUMENT"
  CC="$ARG_PREFIX[CC]"
  AC_LANG_PUSH([C])
  AC_COMPILE_IFELSE([AC_LANG_SOURCE([[int i;]])], [],
      [supports=no])
  AC_LANG_POP([C])
  CC="$saved_cc"
  CFLAGS="$saved_cflags"

  AC_MSG_RESULT([$supports])
  if test "x$supports" = "xyes" ; then
    :
    ARG_IF_TRUE
  else
    :
    ARG_IF_FALSE
  fi
])

# FLAGS_CXX_COMPILER_CHECK_ARGUMENTS(ARGUMENT: [ARGUMENT], IF_TRUE: [RUN-IF-TRUE],
#                                    IF_FALSE: [RUN-IF-FALSE])
# ------------------------------------------------------------
# Check that the C++ compiler supports an argument
UTIL_DEFUN_NAMED([FLAGS_CXX_COMPILER_CHECK_ARGUMENTS],
    [*ARGUMENT IF_TRUE IF_FALSE PREFIX], [$@],
[
  AC_MSG_CHECKING([if ARG_PREFIX[CXX] supports "ARG_ARGUMENT"])
  supports=yes

  saved_cxxflags="$CXXFLAGS"
  saved_cxx="$CXX"
  CXXFLAGS="$CXXFLAG $CFLAGS_WARNINGS_ARE_ERRORS ARG_ARGUMENT"
  CXX="$ARG_PREFIX[CXX]"
  AC_LANG_PUSH([C++])
  AC_COMPILE_IFELSE([AC_LANG_SOURCE([[int i;]])], [],
      [supports=no])
  AC_LANG_POP([C++])
  CXX="$saved_cxx"
  CXXFLAGS="$saved_cxxflags"

  AC_MSG_RESULT([$supports])
  if test "x$supports" = "xyes" ; then
    :
    ARG_IF_TRUE
  else
    :
    ARG_IF_FALSE
  fi
])

# FLAGS_COMPILER_CHECK_ARGUMENTS(ARGUMENT: [ARGUMENT], IF_TRUE: [RUN-IF-TRUE],
#                                IF_FALSE: [RUN-IF-FALSE])
# ------------------------------------------------------------
# Check that the C and C++ compilers support an argument
UTIL_DEFUN_NAMED([FLAGS_COMPILER_CHECK_ARGUMENTS],
    [*ARGUMENT IF_TRUE IF_FALSE PREFIX], [$@],
[
  FLAGS_C_COMPILER_CHECK_ARGUMENTS(ARGUMENT: [ARG_ARGUMENT],
      IF_TRUE: [C_COMP_SUPPORTS="yes"],
      IF_FALSE: [C_COMP_SUPPORTS="no"],
      PREFIX: [ARG_PREFIX])
  FLAGS_CXX_COMPILER_CHECK_ARGUMENTS(ARGUMENT: [ARG_ARGUMENT],
      IF_TRUE: [CXX_COMP_SUPPORTS="yes"],
      IF_FALSE: [CXX_COMP_SUPPORTS="no"],
      PREFIX: [ARG_PREFIX])

  AC_MSG_CHECKING([if both ARG_PREFIX[CC] and ARG_PREFIX[CXX] support "ARG_ARGUMENT"])
  supports=no
  if test "x$C_COMP_SUPPORTS" = "xyes" -a "x$CXX_COMP_SUPPORTS" = "xyes"; then
    supports=yes;
  fi

  AC_MSG_RESULT([$supports])
  if test "x$supports" = "xyes" ; then
    :
    ARG_IF_TRUE
  else
    :
    ARG_IF_FALSE
  fi
])

# FLAGS_LINKER_CHECK_ARGUMENTS(ARGUMENT: [ARGUMENT], IF_TRUE: [RUN-IF-TRUE],
#                                   IF_FALSE: [RUN-IF-FALSE])
# ------------------------------------------------------------
# Check that the linker support an argument
UTIL_DEFUN_NAMED([FLAGS_LINKER_CHECK_ARGUMENTS],
    [*ARGUMENT IF_TRUE IF_FALSE], [$@],
[
  AC_MSG_CHECKING([if linker supports "ARG_ARGUMENT"])
  supports=yes

  saved_ldflags="$LDFLAGS"
  LDFLAGS="$LDFLAGS ARG_ARGUMENT"
  AC_LANG_PUSH([C])
  AC_LINK_IFELSE([AC_LANG_PROGRAM([[]],[[]])],
      [], [supports=no])
  AC_LANG_POP([C])
  LDFLAGS="$saved_ldflags"

  AC_MSG_RESULT([$supports])
  if test "x$supports" = "xyes" ; then
    :
    ARG_IF_TRUE
  else
    :
    ARG_IF_FALSE
  fi
])
