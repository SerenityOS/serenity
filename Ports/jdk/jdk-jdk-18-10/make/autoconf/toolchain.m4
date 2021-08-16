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

########################################################################
# This file is responsible for detecting, verifying and setting up the
# toolchain, i.e. the compiler, linker and related utilities. It will setup
# proper paths to the binaries, but it will not setup any flags.
#
# The binaries used is determined by the toolchain type, which is the family of
# compilers and related tools that are used.
########################################################################

m4_include([toolchain_microsoft.m4])

# All valid toolchains, regardless of platform (used by help.m4)
VALID_TOOLCHAINS_all="gcc clang xlc microsoft"

# These toolchains are valid on different platforms
VALID_TOOLCHAINS_linux="gcc clang"
VALID_TOOLCHAINS_macosx="gcc clang"
VALID_TOOLCHAINS_aix="xlc"
VALID_TOOLCHAINS_windows="microsoft"

# Toolchain descriptions
TOOLCHAIN_DESCRIPTION_clang="clang/LLVM"
TOOLCHAIN_DESCRIPTION_gcc="GNU Compiler Collection"
TOOLCHAIN_DESCRIPTION_microsoft="Microsoft Visual Studio"
TOOLCHAIN_DESCRIPTION_xlc="IBM XL C/C++"

# Minimum supported versions, empty means unspecified
TOOLCHAIN_MINIMUM_VERSION_clang="3.5"
TOOLCHAIN_MINIMUM_VERSION_gcc="5.0"
TOOLCHAIN_MINIMUM_VERSION_microsoft="19.10.0.0" # VS2017
TOOLCHAIN_MINIMUM_VERSION_xlc=""

# Minimum supported linker versions, empty means unspecified
TOOLCHAIN_MINIMUM_LD_VERSION_gcc="2.18"

# Prepare the system so that TOOLCHAIN_CHECK_COMPILER_VERSION can be called.
# Must have CC_VERSION_NUMBER and CXX_VERSION_NUMBER.
# $1 - optional variable prefix for compiler and version variables (BUILD_)
# $2 - optional variable prefix for comparable variable (OPENJDK_BUILD_)
# $3 - optional human readable description for the type of compilers ("build " or "")
AC_DEFUN([TOOLCHAIN_PREPARE_FOR_VERSION_COMPARISONS],
[
  if test "x[$]$1CC_VERSION_NUMBER" != "x[$]$1CXX_VERSION_NUMBER"; then
    AC_MSG_WARN([The $3C and C++ compilers have different version numbers, [$]$1CC_VERSION_NUMBER vs [$]$1CXX_VERSION_NUMBER.])
    AC_MSG_WARN([This typically indicates a broken setup, and is not supported])
  fi

  # We only check CC_VERSION_NUMBER since we assume CXX_VERSION_NUMBER is equal.
  if [ [[ "[$]$1CC_VERSION_NUMBER" =~ (.*\.){4} ]] ]; then
    AC_MSG_WARN([C compiler version number has more than four parts (W.X.Y.Z): [$]$1CC_VERSION_NUMBER. Comparisons might be wrong.])
  fi

  if [ [[  "[$]$1CC_VERSION_NUMBER" =~ [0-9]{6} ]] ]; then
    AC_MSG_WARN([C compiler version number has a part larger than 99999: [$]$1CC_VERSION_NUMBER. Comparisons might be wrong.])
  fi

  $2COMPARABLE_ACTUAL_VERSION=`$AWK -F. '{ printf("%05d%05d%05d%05d\n", [$]1, [$]2, [$]3, [$]4) }' <<< "[$]$1CC_VERSION_NUMBER"`
])

# Check if the configured compiler (C and C++) is of a specific version or
# newer. TOOLCHAIN_PREPARE_FOR_VERSION_COMPARISONS must have been called before.
#
# Arguments:
#   VERSION:   The version string to check against the found version
#   IF_AT_LEAST:   block to run if the compiler is at least this version (>=)
#   IF_OLDER_THAN:   block to run if the compiler is older than this version (<)
#   PREFIX:   Optional variable prefix for compiler to compare version for (OPENJDK_BUILD_)
UTIL_DEFUN_NAMED([TOOLCHAIN_CHECK_COMPILER_VERSION],
    [*VERSION PREFIX IF_AT_LEAST IF_OLDER_THAN], [$@],
[
  # Need to assign to a variable since m4 is blocked from modifying parts in [].
  REFERENCE_VERSION=ARG_VERSION

  if [ [[ "$REFERENCE_VERSION" =~ (.*\.){4} ]] ]; then
    AC_MSG_ERROR([Internal error: Cannot compare to ARG_VERSION, only four parts (W.X.Y.Z) is supported])
  fi

  if [ [[ "$REFERENCE_VERSION" =~ [0-9]{6} ]] ]; then
    AC_MSG_ERROR([Internal error: Cannot compare to ARG_VERSION, only parts < 99999 is supported])
  fi

  # Version comparison method inspired by http://stackoverflow.com/a/24067243
  COMPARABLE_REFERENCE_VERSION=`$AWK -F. '{ printf("%05d%05d%05d%05d\n", [$]1, [$]2, [$]3, [$]4) }' <<< "$REFERENCE_VERSION"`

  if test [$]ARG_PREFIX[COMPARABLE_ACTUAL_VERSION] -ge $COMPARABLE_REFERENCE_VERSION ; then
    :
    ARG_IF_AT_LEAST
  else
    :
    ARG_IF_OLDER_THAN
  fi
])

# Prepare the system so that TOOLCHAIN_CHECK_COMPILER_VERSION can be called.
# Must have LD_VERSION_NUMBER.
# $1 - optional variable prefix for compiler and version variables (BUILD_)
# $2 - optional variable prefix for comparable variable (OPENJDK_BUILD_)
AC_DEFUN([TOOLCHAIN_PREPARE_FOR_LD_VERSION_COMPARISONS],
[
  if [ [[ "[$]$1LD_VERSION_NUMBER" =~ (.*\.){4} ]] ]; then
    AC_MSG_WARN([Linker version number has more than four parts (W.X.Y.Z): [$]$1LD_VERSION_NUMBER. Comparisons might be wrong.])
  fi

  if [ [[  "[$]$1LD_VERSION_NUMBER" =~ [0-9]{6} ]] ]; then
    AC_MSG_WARN([Linker version number has a part larger than 99999: [$]$1LD_VERSION_NUMBER. Comparisons might be wrong.])
  fi

  $2COMPARABLE_ACTUAL_LD_VERSION=`$AWK -F. '{ printf("%05d%05d%05d%05d\n", [$]1, [$]2, [$]3, [$]4) }' <<< "[$]$1LD_VERSION_NUMBER"`
])

# Check if the configured linker is of a specific version or
# newer. TOOLCHAIN_PREPARE_FOR_LD_VERSION_COMPARISONS must have been called before.
#
# Arguments:
#   VERSION:   The version string to check against the found version
#   IF_AT_LEAST:   block to run if the compiler is at least this version (>=)
#   IF_OLDER_THAN:   block to run if the compiler is older than this version (<)
#   PREFIX:   Optional variable prefix for compiler to compare version for (OPENJDK_BUILD_)
UTIL_DEFUN_NAMED([TOOLCHAIN_CHECK_LINKER_VERSION],
    [*VERSION PREFIX IF_AT_LEAST IF_OLDER_THAN], [$@],
[
  # Need to assign to a variable since m4 is blocked from modifying parts in [].
  REFERENCE_VERSION=ARG_VERSION

  if [ [[ "$REFERENCE_VERSION" =~ (.*\.){4} ]] ]; then
    AC_MSG_ERROR([Internal error: Cannot compare to ARG_VERSION, only four parts (W.X.Y.Z) is supported])
  fi

  if [ [[ "$REFERENCE_VERSION" =~ [0-9]{6} ]] ]; then
    AC_MSG_ERROR([Internal error: Cannot compare to ARG_VERSION, only parts < 99999 is supported])
  fi

  # Version comparison method inspired by http://stackoverflow.com/a/24067243
  COMPARABLE_REFERENCE_VERSION=`$AWK -F. '{ printf("%05d%05d%05d%05d\n", [$]1, [$]2, [$]3, [$]4) }' <<< "$REFERENCE_VERSION"`

  if test [$]ARG_PREFIX[COMPARABLE_ACTUAL_LD_VERSION] -ge $COMPARABLE_REFERENCE_VERSION ; then
    :
    ARG_IF_AT_LEAST
  else
    :
    ARG_IF_OLDER_THAN
  fi
])

# Setup a number of variables describing how native output files are
# named on this platform/toolchain.
AC_DEFUN([TOOLCHAIN_SETUP_FILENAME_PATTERNS],
[
  # Define filename patterns
  if test "x$OPENJDK_TARGET_OS" = xwindows; then
    LIBRARY_PREFIX=
    SHARED_LIBRARY_SUFFIX='.dll'
    STATIC_LIBRARY_SUFFIX='.lib'
    SHARED_LIBRARY='[$]1.dll'
    STATIC_LIBRARY='[$]1.lib'
    OBJ_SUFFIX='.obj'
    EXECUTABLE_SUFFIX='.exe'
  else
    LIBRARY_PREFIX=lib
    SHARED_LIBRARY_SUFFIX='.so'
    STATIC_LIBRARY_SUFFIX='.a'
    SHARED_LIBRARY='lib[$]1.so'
    STATIC_LIBRARY='lib[$]1.a'
    OBJ_SUFFIX='.o'
    EXECUTABLE_SUFFIX=''
    if test "x$OPENJDK_TARGET_OS" = xmacosx; then
      # For full static builds, we're overloading the SHARED_LIBRARY
      # variables in order to limit the amount of changes required.
      # It would be better to remove SHARED and just use LIBRARY and
      # LIBRARY_SUFFIX for libraries that can be built either
      # shared or static and use STATIC_* for libraries that are
      # always built statically.
      if test "x$STATIC_BUILD" = xtrue; then
        SHARED_LIBRARY='lib[$]1.a'
        SHARED_LIBRARY_SUFFIX='.a'
      else
        SHARED_LIBRARY='lib[$]1.dylib'
        SHARED_LIBRARY_SUFFIX='.dylib'
      fi
    fi
  fi

  AC_SUBST(LIBRARY_PREFIX)
  AC_SUBST(SHARED_LIBRARY_SUFFIX)
  AC_SUBST(STATIC_LIBRARY_SUFFIX)
  AC_SUBST(SHARED_LIBRARY)
  AC_SUBST(STATIC_LIBRARY)
  AC_SUBST(OBJ_SUFFIX)
  AC_SUBST(EXECUTABLE_SUFFIX)
])

# Determine which toolchain type to use, and make sure it is valid for this
# platform. Setup various information about the selected toolchain.
AC_DEFUN_ONCE([TOOLCHAIN_DETERMINE_TOOLCHAIN_TYPE],
[
  AC_ARG_WITH(toolchain-type, [AS_HELP_STRING([--with-toolchain-type],
      [the toolchain type (or family) to use, use '--help' to show possible values @<:@platform dependent@:>@])])

  # Linux x86_64 needs higher binutils after 8265783
  # (this really is a dependency on as version, but we take ld as a check for a general binutils version)
  if test "x$OPENJDK_TARGET_CPU" = "xx86_64"; then
    TOOLCHAIN_MINIMUM_LD_VERSION_gcc="2.25"
  fi

  # Use indirect variable referencing
  toolchain_var_name=VALID_TOOLCHAINS_$OPENJDK_BUILD_OS
  VALID_TOOLCHAINS=${!toolchain_var_name}

  if test "x$OPENJDK_TARGET_OS" = xmacosx; then
    if test -n "$XCODEBUILD"; then
      # On Mac OS X, default toolchain to clang after Xcode 5
      XCODE_VERSION_OUTPUT=`"$XCODEBUILD" -version 2>&1 | $HEAD -n 1`
      $ECHO "$XCODE_VERSION_OUTPUT" | $GREP "Xcode " > /dev/null
      if test $? -ne 0; then
        AC_MSG_NOTICE([xcodebuild output: $XCODE_VERSION_OUTPUT])
        AC_MSG_ERROR([Failed to determine Xcode version.])
      fi
      XCODE_MAJOR_VERSION=`$ECHO $XCODE_VERSION_OUTPUT | \
          $SED -e 's/^Xcode \(@<:@1-9@:>@@<:@0-9.@:>@*\)/\1/' | \
          $CUT -f 1 -d .`
      AC_MSG_NOTICE([Xcode major version: $XCODE_MAJOR_VERSION])
      if test $XCODE_MAJOR_VERSION -ge 5; then
          DEFAULT_TOOLCHAIN="clang"
      else
          DEFAULT_TOOLCHAIN="gcc"
      fi
    else
      # If Xcode is not installed, but the command line tools are
      # then we can't run xcodebuild. On these systems we should
      # default to clang
      DEFAULT_TOOLCHAIN="clang"
    fi
  else
    # First toolchain type in the list is the default
    DEFAULT_TOOLCHAIN=${VALID_TOOLCHAINS%% *}
  fi

  if test "x$with_toolchain_type" = xlist; then
    # List all toolchains
    AC_MSG_NOTICE([The following toolchains are valid on this platform:])
    for toolchain in $VALID_TOOLCHAINS; do
      toolchain_var_name=TOOLCHAIN_DESCRIPTION_$toolchain
      TOOLCHAIN_DESCRIPTION=${!toolchain_var_name}
      $PRINTF "  %-10s  %s\n" $toolchain "$TOOLCHAIN_DESCRIPTION"
    done

    exit 0
  elif test "x$with_toolchain_type" != x; then
    # User override; check that it is valid
    if test "x${VALID_TOOLCHAINS/$with_toolchain_type/}" = "x${VALID_TOOLCHAINS}"; then
      AC_MSG_NOTICE([Toolchain type $with_toolchain_type is not valid on this platform.])
      AC_MSG_NOTICE([Valid toolchains: $VALID_TOOLCHAINS.])
      AC_MSG_ERROR([Cannot continue.])
    fi
    TOOLCHAIN_TYPE=$with_toolchain_type
  else
    # No flag given, use default
    TOOLCHAIN_TYPE=$DEFAULT_TOOLCHAIN
  fi
  AC_SUBST(TOOLCHAIN_TYPE)

  # on AIX, check for xlclang++ on the PATH and TOOLCHAIN_PATH and use it if it is available
  if test "x$OPENJDK_TARGET_OS" = xaix; then
    if test "x$TOOLCHAIN_PATH" != x; then
      XLC_TEST_PATH=${TOOLCHAIN_PATH}/
    fi

    XLCLANG_VERSION_OUTPUT=`${XLC_TEST_PATH}xlclang++ -qversion 2>&1 | $HEAD -n 1`
    $ECHO "$XLCLANG_VERSION_OUTPUT" | $GREP "IBM XL C/C++ for AIX" > /dev/null
    if test $? -eq 0; then
      AC_MSG_NOTICE([xlclang++ output: $XLCLANG_VERSION_OUTPUT])
    else
      AC_MSG_ERROR([xlclang++ version output check failed, output: $XLCLANG_VERSION_OUTPUT])
    fi
  fi

  TOOLCHAIN_CC_BINARY_clang="clang"
  TOOLCHAIN_CC_BINARY_gcc="gcc"
  TOOLCHAIN_CC_BINARY_microsoft="cl"
  TOOLCHAIN_CC_BINARY_xlc="xlclang"

  TOOLCHAIN_CXX_BINARY_clang="clang++"
  TOOLCHAIN_CXX_BINARY_gcc="g++"
  TOOLCHAIN_CXX_BINARY_microsoft="cl"
  TOOLCHAIN_CXX_BINARY_xlc="xlclang++"

  # Use indirect variable referencing
  toolchain_var_name=TOOLCHAIN_DESCRIPTION_$TOOLCHAIN_TYPE
  TOOLCHAIN_DESCRIPTION=${!toolchain_var_name}
  toolchain_var_name=TOOLCHAIN_MINIMUM_VERSION_$TOOLCHAIN_TYPE
  TOOLCHAIN_MINIMUM_VERSION=${!toolchain_var_name}
  toolchain_var_name=TOOLCHAIN_MINIMUM_LD_VERSION_$TOOLCHAIN_TYPE
  TOOLCHAIN_MINIMUM_LD_VERSION=${!toolchain_var_name}
  toolchain_var_name=TOOLCHAIN_CC_BINARY_$TOOLCHAIN_TYPE
  TOOLCHAIN_CC_BINARY=${!toolchain_var_name}
  toolchain_var_name=TOOLCHAIN_CXX_BINARY_$TOOLCHAIN_TYPE
  TOOLCHAIN_CXX_BINARY=${!toolchain_var_name}

  TOOLCHAIN_SETUP_FILENAME_PATTERNS

  if test "x$TOOLCHAIN_TYPE" = "x$DEFAULT_TOOLCHAIN"; then
    AC_MSG_NOTICE([Using default toolchain $TOOLCHAIN_TYPE ($TOOLCHAIN_DESCRIPTION)])
  else
    AC_MSG_NOTICE([Using user selected toolchain $TOOLCHAIN_TYPE ($TOOLCHAIN_DESCRIPTION). Default toolchain is $DEFAULT_TOOLCHAIN.])
  fi
])

# Before we start detecting the toolchain executables, we might need some
# special setup, e.g. additional paths etc.
AC_DEFUN_ONCE([TOOLCHAIN_PRE_DETECTION],
[
  # Store the CFLAGS etc passed to the configure script.
  ORG_CFLAGS="$CFLAGS"
  ORG_CXXFLAGS="$CXXFLAGS"

  # autoconf magic only relies on PATH, so update it if tools dir is specified
  OLD_PATH="$PATH"

  if test "x$XCODE_VERSION_OUTPUT" != x; then
    # For Xcode, we set the Xcode version as TOOLCHAIN_VERSION
    TOOLCHAIN_VERSION=`$ECHO $XCODE_VERSION_OUTPUT | $CUT -f 2 -d ' '`
    TOOLCHAIN_DESCRIPTION="$TOOLCHAIN_DESCRIPTION from Xcode $TOOLCHAIN_VERSION"
  fi
  AC_SUBST(TOOLCHAIN_VERSION)

  # Finally prepend TOOLCHAIN_PATH to the PATH, to allow --with-tools-dir to
  # override all other locations.
  if test "x$TOOLCHAIN_PATH" != x; then
    export PATH=$TOOLCHAIN_PATH:$PATH
  fi
])

# Restore path, etc
AC_DEFUN_ONCE([TOOLCHAIN_POST_DETECTION],
[
  # Restore old path, except for the microsoft toolchain, which requires the
  # toolchain path to remain in place. Otherwise the compiler will not work in
  # some siutations in later configure checks.
  if test "x$TOOLCHAIN_TYPE" != "xmicrosoft"; then
    PATH="$OLD_PATH"
  fi

  # Restore the flags to the user specified values.
  # This is necessary since AC_PROG_CC defaults CFLAGS to "-g -O2"
  CFLAGS="$ORG_CFLAGS"
  CXXFLAGS="$ORG_CXXFLAGS"
])

# Check if a compiler is of the toolchain type we expect, and save the version
# information from it. If the compiler does not match the expected type,
# this function will abort using AC_MSG_ERROR. If it matches, the version will
# be stored in CC_VERSION_NUMBER/CXX_VERSION_NUMBER (as a dotted number), and
# the full version string in CC_VERSION_STRING/CXX_VERSION_STRING.
#
# $1 = compiler to test (CC or CXX)
# $2 = human readable name of compiler (C or C++)
AC_DEFUN([TOOLCHAIN_EXTRACT_COMPILER_VERSION],
[
  COMPILER=[$]$1
  COMPILER_NAME=$2

  if test  "x$TOOLCHAIN_TYPE" = xxlc; then
    # xlc -qversion output typically looks like
    #     IBM XL C/C++ for AIX, V11.1 (5724-X13)
    #     Version: 11.01.0000.0015
    COMPILER_VERSION_OUTPUT=`$COMPILER -qversion 2>&1`
    # Check that this is likely to be the IBM XL C compiler.
    $ECHO "$COMPILER_VERSION_OUTPUT" | $GREP "IBM XL C" > /dev/null
    if test $? -ne 0; then
      ALT_VERSION_OUTPUT=`$COMPILER --version 2>&1`
      AC_MSG_NOTICE([The $COMPILER_NAME compiler (located as $COMPILER) does not seem to be the required $TOOLCHAIN_TYPE compiler.])
      AC_MSG_NOTICE([The result from running with -qversion was: "$COMPILER_VERSION_OUTPUT"])
      AC_MSG_NOTICE([The result from running with --version was: "$ALT_VERSION_OUTPUT"])
      AC_MSG_ERROR([A $TOOLCHAIN_TYPE compiler is required. Try setting --with-tools-dir.])
    fi
    # Collapse compiler output into a single line
    COMPILER_VERSION_STRING=`$ECHO $COMPILER_VERSION_OUTPUT`
    COMPILER_VERSION_NUMBER=`$ECHO $COMPILER_VERSION_OUTPUT | \
        $SED -e 's/^.*, V\(@<:@1-9@:>@@<:@0-9.@:>@*\).*$/\1/'`
  elif test  "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # There is no specific version flag, but all output starts with a version string.
    # First line typically looks something like:
    # Microsoft (R) 32-bit C/C++ Optimizing Compiler Version 16.00.40219.01 for 80x86
    # but the compiler name may vary depending on locale.
    COMPILER_VERSION_OUTPUT=`$COMPILER 2>&1 1>/dev/null | $HEAD -n 1 | $TR -d '\r'`
    # Check that this is likely to be Microsoft CL.EXE.
    $ECHO "$COMPILER_VERSION_OUTPUT" | $GREP "Microsoft" > /dev/null
    if test $? -ne 0; then
      AC_MSG_NOTICE([The $COMPILER_NAME compiler (located as $COMPILER) does not seem to be the required $TOOLCHAIN_TYPE compiler.])
      AC_MSG_NOTICE([The result from running it was: "$COMPILER_VERSION_OUTPUT"])
      AC_MSG_ERROR([A $TOOLCHAIN_TYPE compiler is required. Try setting --with-tools-dir.])
    fi
    # Collapse compiler output into a single line
    COMPILER_VERSION_STRING=`$ECHO $COMPILER_VERSION_OUTPUT`
    COMPILER_VERSION_NUMBER=`$ECHO $COMPILER_VERSION_OUTPUT | \
        $SED -e 's/^.*ersion.\(@<:@1-9@:>@@<:@0-9.@:>@*\) .*$/\1/'`
  elif test  "x$TOOLCHAIN_TYPE" = xgcc; then
    # gcc --version output typically looks like
    #     gcc (Ubuntu/Linaro 4.8.1-10ubuntu9) 4.8.1
    #     Copyright (C) 2013 Free Software Foundation, Inc.
    #     This is free software; see the source for copying conditions.  There is NO
    #     warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    COMPILER_VERSION_OUTPUT=`$COMPILER --version 2>&1`
    # Check that this is likely to be GCC.
    $ECHO "$COMPILER_VERSION_OUTPUT" | $GREP "Free Software Foundation" > /dev/null
    if test $? -ne 0; then
      AC_MSG_NOTICE([The $COMPILER_NAME compiler (located as $COMPILER) does not seem to be the required $TOOLCHAIN_TYPE compiler.])
      AC_MSG_NOTICE([The result from running with --version was: "$COMPILER_VERSION"])
      AC_MSG_ERROR([A $TOOLCHAIN_TYPE compiler is required. Try setting --with-tools-dir.])
    fi
    # Remove Copyright and legalese from version string, and
    # collapse into a single line
    COMPILER_VERSION_STRING=`$ECHO $COMPILER_VERSION_OUTPUT | \
        $SED -e 's/ *Copyright .*//'`
    COMPILER_VERSION_NUMBER=`$ECHO $COMPILER_VERSION_OUTPUT | \
        $SED -e 's/^.* \(@<:@1-9@:>@<:@0-9@:>@*\.@<:@0-9.@:>@*\)@<:@^0-9.@:>@.*$/\1/'`
  elif test  "x$TOOLCHAIN_TYPE" = xclang; then
    # clang --version output typically looks like
    #    Apple LLVM version 5.0 (clang-500.2.79) (based on LLVM 3.3svn)
    #    clang version 3.3 (tags/RELEASE_33/final)
    # or
    #    Debian clang version 3.2-7ubuntu1 (tags/RELEASE_32/final) (based on LLVM 3.2)
    #    Target: x86_64-pc-linux-gnu
    #    Thread model: posix
    COMPILER_VERSION_OUTPUT=`$COMPILER --version 2>&1`
    # Check that this is likely to be clang
    $ECHO "$COMPILER_VERSION_OUTPUT" | $GREP "clang" > /dev/null
    if test $? -ne 0; then
      AC_MSG_NOTICE([The $COMPILER_NAME compiler (located as $COMPILER) does not seem to be the required $TOOLCHAIN_TYPE compiler.])
      AC_MSG_NOTICE([The result from running with --version was: "$COMPILER_VERSION_OUTPUT"])
      AC_MSG_ERROR([A $TOOLCHAIN_TYPE compiler is required. Try setting --with-tools-dir.])
    fi
    # Collapse compiler output into a single line
    COMPILER_VERSION_STRING=`$ECHO $COMPILER_VERSION_OUTPUT`
    COMPILER_VERSION_NUMBER=`$ECHO $COMPILER_VERSION_OUTPUT | \
        $SED -e 's/^.* version \(@<:@1-9@:>@@<:@0-9.@:>@*\).*$/\1/'`
  else
      AC_MSG_ERROR([Unknown toolchain type $TOOLCHAIN_TYPE.])
  fi
  # This sets CC_VERSION_NUMBER or CXX_VERSION_NUMBER. (This comment is a grep marker)
  $1_VERSION_NUMBER="$COMPILER_VERSION_NUMBER"
  # This sets CC_VERSION_STRING or CXX_VERSION_STRING. (This comment is a grep marker)
  $1_VERSION_STRING="$COMPILER_VERSION_STRING"

  AC_MSG_NOTICE([Using $TOOLCHAIN_TYPE $COMPILER_NAME compiler version $COMPILER_VERSION_NUMBER @<:@$COMPILER_VERSION_STRING@:>@])
])

# Try to locate the given C or C++ compiler in the path, or otherwise.
#
# $1 = compiler to test (CC or CXX)
# $2 = human readable name of compiler (C or C++)
# $3 = compiler name to search for
AC_DEFUN([TOOLCHAIN_FIND_COMPILER],
[
  COMPILER_NAME=$2
  SEARCH_LIST="$3"

  if test "x[$]$1" != x; then
    # User has supplied compiler name already, always let that override.
    AC_MSG_NOTICE([Will use user supplied compiler $1=[$]$1])
    if test "x`basename [$]$1`" = "x[$]$1"; then
      # A command without a complete path is provided, search $PATH.

      UTIL_LOOKUP_PROGS(POTENTIAL_$1, [$]$1)
      if test "x$POTENTIAL_$1" != x; then
        $1=$POTENTIAL_$1
      else
        AC_MSG_ERROR([User supplied compiler $1=[$]$1 could not be found])
      fi
    else
      # Otherwise it might already be a complete path
      if test ! -x "[$]$1"; then
        AC_MSG_ERROR([User supplied compiler $1=[$]$1 does not exist])
      fi
    fi
  else
    # No user supplied value. Locate compiler ourselves.

    # If we are cross compiling, assume cross compilation tools follows the
    # cross compilation standard where they are prefixed with the autoconf
    # standard name for the target. For example the binary
    # i686-sun-solaris2.10-gcc will cross compile for i686-sun-solaris2.10.
    # If we are not cross compiling, then the default compiler name will be
    # used.

    UTIL_LOOKUP_TOOLCHAIN_PROGS(POTENTIAL_$1, $SEARCH_LIST)
    if test "x$POTENTIAL_$1" != x; then
      $1=$POTENTIAL_$1
    else
      HELP_MSG_MISSING_DEPENDENCY([devkit])
      AC_MSG_ERROR([Could not find a $COMPILER_NAME compiler. $HELP_MSG])
    fi
  fi

  # Now we have a compiler binary in $1. Make sure it's okay.
  TEST_COMPILER="[$]$1"

  AC_MSG_CHECKING([resolved symbolic links for $1])
  SYMLINK_ORIGINAL="$TEST_COMPILER"
  UTIL_REMOVE_SYMBOLIC_LINKS(SYMLINK_ORIGINAL)
  if test "x$TEST_COMPILER" = "x$SYMLINK_ORIGINAL"; then
    AC_MSG_RESULT([no symlink])
  else
    AC_MSG_RESULT([$SYMLINK_ORIGINAL])

    # We can't handle ccache by gcc wrappers, since we need to know if we're
    # using ccache. Instead ccache usage must be controlled by a configure option.
    COMPILER_BASENAME=`$BASENAME "$SYMLINK_ORIGINAL"`
    if test "x$COMPILER_BASENAME" = "xccache"; then
      AC_MSG_NOTICE([Please use --enable-ccache instead of providing a wrapped compiler.])
      AC_MSG_ERROR([$TEST_COMPILER is a symbolic link to ccache. This is not supported.])
    fi
  fi

  TOOLCHAIN_EXTRACT_COMPILER_VERSION([$1], [$COMPILER_NAME])
])

# Retrieve the linker version number and store it in LD_VERSION_NUMBER
# (as a dotted number), and
# the full version string in LD_VERSION_STRING.
#
# $1 = linker to test (LD or BUILD_LD)
# $2 = human readable name of linker (Linker or BuildLinker)
AC_DEFUN([TOOLCHAIN_EXTRACT_LD_VERSION],
[
  LINKER=[$]$1
  LINKER_NAME="$2"

  if test  "x$TOOLCHAIN_TYPE" = xxlc; then
    LINKER_VERSION_STRING="Unknown"
    LINKER_VERSION_NUMBER="0.0"
  elif test  "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # There is no specific version flag, but all output starts with a version string.
    # First line typically looks something like:
    #   Microsoft (R) Incremental Linker Version 12.00.31101.0
    LINKER_VERSION_STRING=`$LINKER 2>&1 | $HEAD -n 1 | $TR -d '\r'`
    # Extract version number
    [ LINKER_VERSION_NUMBER=`$ECHO $LINKER_VERSION_STRING | \
        $SED -e 's/.* \([0-9][0-9]*\(\.[0-9][0-9]*\)*\).*/\1/'` ]
  elif test  "x$TOOLCHAIN_TYPE" = xgcc; then
    # gcc -Wl,-version output typically looks like:
    #   GNU ld (GNU Binutils for Ubuntu) 2.26.1
    #   Copyright (C) 2015 Free Software Foundation, Inc.
    #   This program is free software; [...]
    # If using gold it will look like:
    #   GNU gold (GNU Binutils 2.30) 1.15
    LINKER_VERSION_STRING=`$LINKER -Wl,--version 2> /dev/null | $HEAD -n 1`
    # Extract version number
    if [ [[ "$LINKER_VERSION_STRING" == *gold* ]] ]; then
      [ LINKER_VERSION_NUMBER=`$ECHO $LINKER_VERSION_STRING | \
          $SED -e 's/.* \([0-9][0-9]*\(\.[0-9][0-9]*\)*\).*) .*/\1/'` ]
    else
      [ LINKER_VERSION_NUMBER=`$ECHO $LINKER_VERSION_STRING | \
          $SED -e 's/.* \([0-9][0-9]*\(\.[0-9][0-9]*\)*\).*/\1/'` ]
    fi
  elif test  "x$TOOLCHAIN_TYPE" = xclang; then
    # clang -Wl,-v output typically looks like
    #   @(#)PROGRAM:ld  PROJECT:ld64-305
    #   configured to support archs: armv6 armv7 armv7s arm64 i386 x86_64 x86_64h armv6m armv7k armv7m armv7em (tvOS)
    #   Library search paths: [...]
    # or
    #   GNU ld (GNU Binutils for Ubuntu) 2.26.1

    LINKER_VERSION_STRING=`$LINKER -Wl,-v 2>&1 | $HEAD -n 1`
    # Check if we're using the GNU ld
    $ECHO "$LINKER_VERSION_STRING" | $GREP "GNU" > /dev/null
    if test $? -eq 0; then
      # Extract version number
      [ LINKER_VERSION_NUMBER=`$ECHO $LINKER_VERSION_STRING | \
          $SED -e 's/.* \([0-9][0-9]*\(\.[0-9][0-9]*\)*\).*/\1/'` ]
    else
      # Extract version number
      [ LINKER_VERSION_NUMBER=`$ECHO $LINKER_VERSION_STRING | \
          $SED -e 's/.*-\([0-9][0-9]*\)/\1/'` ]
    fi
  fi

  $1_VERSION_NUMBER="$LINKER_VERSION_NUMBER"
  $1_VERSION_STRING="$LINKER_VERSION_STRING"

  AC_MSG_NOTICE([Using $TOOLCHAIN_TYPE $LINKER_NAME version $LINKER_VERSION_NUMBER @<:@$LINKER_VERSION_STRING@:>@])
])

# Make sure we did not pick up /usr/bin/link, which is the unix-style link
# executable.
#
# $1 = linker to test (LD or BUILD_LD)
AC_DEFUN(TOOLCHAIN_VERIFY_LINK_BINARY,
[
  LINKER=[$]$1

  AC_MSG_CHECKING([if the found link.exe is actually the Visual Studio linker])
  $LINKER --version > /dev/null
  if test $? -eq 0 ; then
    AC_MSG_RESULT([no])
    AC_MSG_ERROR([$LINKER is the winenv link tool. Please check your PATH and rerun configure.])
  else
    AC_MSG_RESULT([yes])
  fi
])
# Detect the core components of the toolchain, i.e. the compilers (CC and CXX),
# preprocessor (CPP and CXXCPP), the linker (LD), the assembler (AS) and the
# archiver (AR). Verify that the compilers are correct according to the
# toolchain type.
AC_DEFUN_ONCE([TOOLCHAIN_DETECT_TOOLCHAIN_CORE],
[
  #
  # Setup the compilers (CC and CXX)
  #
  TOOLCHAIN_FIND_COMPILER([CC], [C], $TOOLCHAIN_CC_BINARY)
  # Now that we have resolved CC ourself, let autoconf have its go at it
  AC_PROG_CC([$CC])

  TOOLCHAIN_FIND_COMPILER([CXX], [C++], $TOOLCHAIN_CXX_BINARY)
  # Now that we have resolved CXX ourself, let autoconf have its go at it
  AC_PROG_CXX([$CXX])

  # This is the compiler version number on the form X.Y[.Z]
  AC_SUBST(CC_VERSION_NUMBER)
  AC_SUBST(CXX_VERSION_NUMBER)

  TOOLCHAIN_PREPARE_FOR_VERSION_COMPARISONS

  if test "x$TOOLCHAIN_MINIMUM_VERSION" != x; then
    TOOLCHAIN_CHECK_COMPILER_VERSION(VERSION: $TOOLCHAIN_MINIMUM_VERSION,
        IF_OLDER_THAN: [
          AC_MSG_WARN([You are using $TOOLCHAIN_TYPE older than $TOOLCHAIN_MINIMUM_VERSION. This is not a supported configuration.])
        ]
    )
  fi

  #
  # Setup the preprocessor (CPP and CXXCPP)
  #
  AC_PROG_CPP
  UTIL_FIXUP_EXECUTABLE(CPP)
  AC_PROG_CXXCPP
  UTIL_FIXUP_EXECUTABLE(CXXCPP)

  #
  # Setup the linker (LD)
  #
  if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # In the Microsoft toolchain we have a separate LD command "link".
    UTIL_LOOKUP_TOOLCHAIN_PROGS(LD, link)
    TOOLCHAIN_VERIFY_LINK_BINARY(LD)
    LDCXX="$LD"
  else
    # All other toolchains use the compiler to link.
    LD="$CC"
    LDCXX="$CXX"
  fi
  AC_SUBST(LD)
  # FIXME: it should be CXXLD, according to standard (cf CXXCPP)
  AC_SUBST(LDCXX)

  TOOLCHAIN_EXTRACT_LD_VERSION([LD], [linker])
  TOOLCHAIN_PREPARE_FOR_LD_VERSION_COMPARISONS

  if test "x$TOOLCHAIN_MINIMUM_LD_VERSION" != x; then
    AC_MSG_NOTICE([comparing linker version to minimum version $TOOLCHAIN_MINIMUM_LD_VERSION])
    TOOLCHAIN_CHECK_LINKER_VERSION(VERSION: $TOOLCHAIN_MINIMUM_LD_VERSION,
        IF_OLDER_THAN: [
          AC_MSG_ERROR([You are using a linker older than $TOOLCHAIN_MINIMUM_LD_VERSION. This is not a supported configuration.])
        ]
    )
  fi

  #
  # Setup the assembler (AS)
  #
  if test "x$TOOLCHAIN_TYPE" != xmicrosoft; then
    AS="$CC -c"
  else
    if test "x$OPENJDK_TARGET_CPU_BITS" = "x64"; then
      # On 64 bit windows, the assember is "ml64.exe"
      UTIL_LOOKUP_TOOLCHAIN_PROGS(AS, ml64)
    else
      # otherwise, the assember is "ml.exe"
      UTIL_LOOKUP_TOOLCHAIN_PROGS(AS, ml)
    fi
  fi
  AC_SUBST(AS)

  #
  # Setup the archiver (AR)
  #
  if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # The corresponding ar tool is lib.exe (used to create static libraries)
    UTIL_LOOKUP_TOOLCHAIN_PROGS(AR, lib)
  elif test "x$TOOLCHAIN_TYPE" = xgcc; then
    UTIL_LOOKUP_TOOLCHAIN_PROGS(AR, ar gcc-ar)
  else
    UTIL_LOOKUP_TOOLCHAIN_PROGS(AR, ar)
  fi
])

# Setup additional tools that is considered a part of the toolchain, but not the
# core part. Many of these are highly platform-specific and do not exist,
# and/or are not needed on all platforms.
AC_DEFUN_ONCE([TOOLCHAIN_DETECT_TOOLCHAIN_EXTRA],
[
  if test "x$OPENJDK_TARGET_OS" = "xmacosx"; then
    UTIL_LOOKUP_PROGS(LIPO, lipo)
    UTIL_REQUIRE_PROGS(OTOOL, otool)
    UTIL_REQUIRE_PROGS(INSTALL_NAME_TOOL, install_name_tool)

    UTIL_LOOKUP_TOOLCHAIN_PROGS(METAL, metal)
    if test "x$METAL" = x; then
      AC_MSG_CHECKING([if metal can be run using xcrun])
      METAL="xcrun -sdk macosx metal"
      test_metal=`$METAL --version 2>&1`
      if test $? -ne 0; then
        AC_MSG_RESULT([no])
        AC_MSG_ERROR([XCode tool 'metal' neither found in path nor with xcrun])
      else
        AC_MSG_RESULT([yes, will be using '$METAL'])
      fi
    fi

    UTIL_LOOKUP_TOOLCHAIN_PROGS(METALLIB, metallib)
    if test "x$METALLIB" = x; then
      AC_MSG_CHECKING([if metallib can be run using xcrun])
      METALLIB="xcrun -sdk macosx metallib"
      test_metallib=`$METALLIB --version 2>&1`
      if test $? -ne 0; then
        AC_MSG_RESULT([no])
        AC_MSG_ERROR([XCode tool 'metallib' neither found in path nor with xcrun])
      else
        AC_MSG_RESULT([yes, will be using '$METALLIB'])
      fi
    fi
  fi

  if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # Setup the manifest tool (MT)
    UTIL_LOOKUP_TOOLCHAIN_PROGS(MT, mt)
    # Setup the resource compiler (RC)
    UTIL_LOOKUP_TOOLCHAIN_PROGS(RC, rc)
    UTIL_LOOKUP_TOOLCHAIN_PROGS(DUMPBIN, dumpbin)
  fi

  if test "x$OPENJDK_TARGET_OS" != xwindows; then
    UTIL_LOOKUP_TOOLCHAIN_PROGS(STRIP, strip)
    if test "x$TOOLCHAIN_TYPE" = xgcc; then
      UTIL_LOOKUP_TOOLCHAIN_PROGS(NM, nm gcc-nm)
    else
      UTIL_LOOKUP_TOOLCHAIN_PROGS(NM, nm)
    fi
    GNM="$NM"
    AC_SUBST(GNM)
  fi

  # objcopy is used for moving debug symbols to separate files when
  # full debug symbols are enabled.
  if test "x$OPENJDK_TARGET_OS" = xlinux; then
    UTIL_LOOKUP_TOOLCHAIN_PROGS(OBJCOPY, gobjcopy objcopy)
  fi

  UTIL_LOOKUP_TOOLCHAIN_PROGS(OBJDUMP, gobjdump objdump)

  case $TOOLCHAIN_TYPE in
    gcc|clang)
      UTIL_REQUIRE_TOOLCHAIN_PROGS(CXXFILT, c++filt)
      ;;
  esac
])

# Setup the build tools (i.e, the compiler and linker used to build programs
# that should be run on the build platform, not the target platform, as a build
# helper). Since the non-cross-compile case uses the normal, target compilers
# for this, we can only do this after these have been setup.
AC_DEFUN_ONCE([TOOLCHAIN_SETUP_BUILD_COMPILERS],
[
  if test "x$COMPILE_TYPE" = "xcross"; then
    # Now we need to find a C/C++ compiler that can build executables for the
    # build platform. We can't use the AC_PROG_CC macro, since it can only be
    # used once. Also, we need to do this without adding a tools dir to the
    # path, otherwise we might pick up cross-compilers which don't use standard
    # naming.

    OLDPATH="$PATH"

    AC_ARG_WITH(build-devkit, [AS_HELP_STRING([--with-build-devkit],
        [Devkit to use for the build platform toolchain])])
    if test "x$with_build_devkit" = "xyes"; then
      AC_MSG_ERROR([--with-build-devkit must have a value])
    elif test -n "$with_build_devkit"; then
      if test ! -d "$with_build_devkit"; then
        AC_MSG_ERROR([--with-build-devkit points to non existing dir: $with_build_devkit])
      else
        UTIL_FIXUP_PATH([with_build_devkit])
        BUILD_DEVKIT_ROOT="$with_build_devkit"
        # Check for a meta data info file in the root of the devkit
        if test -f "$BUILD_DEVKIT_ROOT/devkit.info"; then
          # Process devkit.info so that existing devkit variables are not
          # modified by this
          $SED -e "s/^DEVKIT_/BUILD_DEVKIT_/g" \
              -e "s/\$DEVKIT_ROOT/\$BUILD_DEVKIT_ROOT/g" \
              -e "s/\$host/\$build/g" \
              $BUILD_DEVKIT_ROOT/devkit.info \
              > $CONFIGURESUPPORT_OUTPUTDIR/build-devkit.info
          . $CONFIGURESUPPORT_OUTPUTDIR/build-devkit.info
          # This potentially sets the following:
          # A descriptive name of the devkit
          BASIC_EVAL_BUILD_DEVKIT_VARIABLE([BUILD_DEVKIT_NAME])
          # Corresponds to --with-extra-path
          BASIC_EVAL_BUILD_DEVKIT_VARIABLE([BUILD_DEVKIT_EXTRA_PATH])
          # Corresponds to --with-toolchain-path
          BASIC_EVAL_BUILD_DEVKIT_VARIABLE([BUILD_DEVKIT_TOOLCHAIN_PATH])
          # Corresponds to --with-sysroot
          BASIC_EVAL_BUILD_DEVKIT_VARIABLE([BUILD_DEVKIT_SYSROOT])

          if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
            BASIC_EVAL_BUILD_DEVKIT_VARIABLE([BUILD_DEVKIT_VS_INCLUDE])
            BASIC_EVAL_BUILD_DEVKIT_VARIABLE([BUILD_DEVKIT_VS_LIB])
          fi
        fi

        AC_MSG_CHECKING([for build platform devkit])
        if test "x$BUILD_DEVKIT_NAME" != x; then
          AC_MSG_RESULT([$BUILD_DEVKIT_NAME in $BUILD_DEVKIT_ROOT])
        else
          AC_MSG_RESULT([$BUILD_DEVKIT_ROOT])
        fi

        # Fallback default of just /bin if DEVKIT_PATH is not defined
        if test "x$BUILD_DEVKIT_TOOLCHAIN_PATH" = x; then
          BUILD_DEVKIT_TOOLCHAIN_PATH="$BUILD_DEVKIT_ROOT/bin"
        fi
        PATH="$BUILD_DEVKIT_TOOLCHAIN_PATH:$BUILD_DEVKIT_EXTRA_PATH"

        BUILD_SYSROOT="$BUILD_DEVKIT_SYSROOT"

        if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
          # For historical reasons, paths are separated by ; in devkit.info
          BUILD_VS_INCLUDE="${BUILD_DEVKIT_VS_INCLUDE//;/:}"
          BUILD_VS_LIB="${BUILD_DEVKIT_VS_LIB//;/:}"

          TOOLCHAIN_SETUP_VISUAL_STUDIO_SYSROOT_FLAGS(BUILD_, BUILD_)
        fi
      fi
    else
      if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
        # If we got no devkit, we need to go hunting for the proper env
        TOOLCHAIN_FIND_VISUAL_STUDIO_BAT_FILE($OPENJDK_BUILD_CPU, [$TOOLCHAIN_VERSION])
        TOOLCHAIN_EXTRACT_VISUAL_STUDIO_ENV($OPENJDK_BUILD_CPU, BUILD_)

        # We cannot currently export the VS_PATH to spec.gmk. This is probably
        # strictly not correct, but seems to work anyway.

        # Convert VS_INCLUDE and VS_LIB into sysroot flags
        TOOLCHAIN_SETUP_VISUAL_STUDIO_SYSROOT_FLAGS(BUILD_)
      fi
    fi

    if test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
      UTIL_REQUIRE_PROGS(BUILD_CC, cl, [$VS_PATH])
      UTIL_REQUIRE_PROGS(BUILD_CXX, cl, [$VS_PATH])

      # On windows, the assember is "ml.exe". We currently don't need this so
      # do not require.
      if test "x$OPENJDK_BUILD_CPU_BITS" = "x64"; then
        # On 64 bit windows, the assember is "ml64.exe"
        UTIL_LOOKUP_PROGS(BUILD_AS, ml64, [$VS_PATH])
      else
        # otherwise the assember is "ml.exe"
        UTIL_LOOKUP_PROGS(BUILD_AS, ml, [$VS_PATH])
      fi

      # On windows, the ar tool is lib.exe (used to create static libraries).
      # We currently don't need this so do not require.
      UTIL_LOOKUP_PROGS(BUILD_AR, lib, [$VS_PATH])

      # In the Microsoft toolchain we have a separate LD command "link".
      UTIL_REQUIRE_PROGS(BUILD_LD, link, [$VS_PATH])
      TOOLCHAIN_VERIFY_LINK_BINARY(BUILD_LD)
      BUILD_LDCXX="$BUILD_LD"
    else
      if test "x$OPENJDK_BUILD_OS" = xmacosx; then
        UTIL_REQUIRE_PROGS(BUILD_CC, clang cc gcc)
        UTIL_REQUIRE_PROGS(BUILD_CXX, clang++ CC g++)
      else
        UTIL_REQUIRE_PROGS(BUILD_CC, cc gcc)
        UTIL_REQUIRE_PROGS(BUILD_CXX, CC g++)
      fi
      UTIL_LOOKUP_PROGS(BUILD_NM, nm gcc-nm)
      UTIL_LOOKUP_PROGS(BUILD_AR, ar gcc-ar lib)
      UTIL_LOOKUP_PROGS(BUILD_OBJCOPY, objcopy)
      UTIL_LOOKUP_PROGS(BUILD_STRIP, strip)
      # Assume the C compiler is the assembler
      BUILD_AS="$BUILD_CC -c"
      # Just like for the target compiler, use the compiler as linker
      BUILD_LD="$BUILD_CC"
      BUILD_LDCXX="$BUILD_CXX"
    fi

    PATH="$OLDPATH"

    TOOLCHAIN_EXTRACT_COMPILER_VERSION(BUILD_CC, [BuildC])
    TOOLCHAIN_EXTRACT_COMPILER_VERSION(BUILD_CXX, [BuildC++])
    TOOLCHAIN_PREPARE_FOR_VERSION_COMPARISONS([BUILD_], [OPENJDK_BUILD_], [build ])
    TOOLCHAIN_EXTRACT_LD_VERSION(BUILD_LD, [build linker])
    TOOLCHAIN_PREPARE_FOR_LD_VERSION_COMPARISONS([BUILD_], [OPENJDK_BUILD_])
  else
    # If we are not cross compiling, use the normal target compilers for
    # building the build platform executables.
    BUILD_CC="$CC"
    BUILD_CXX="$CXX"
    BUILD_LD="$LD"
    BUILD_LDCXX="$LDCXX"
    BUILD_NM="$NM"
    BUILD_AS="$AS"
    BUILD_OBJCOPY="$OBJCOPY"
    BUILD_STRIP="$STRIP"
    BUILD_AR="$AR"

    TOOLCHAIN_PREPARE_FOR_VERSION_COMPARISONS([], [OPENJDK_BUILD_], [build ])
    TOOLCHAIN_PREPARE_FOR_LD_VERSION_COMPARISONS([BUILD_], [OPENJDK_BUILD_])
  fi

  AC_SUBST(BUILD_CC)
  AC_SUBST(BUILD_CXX)
  AC_SUBST(BUILD_LD)
  AC_SUBST(BUILD_LDCXX)
  AC_SUBST(BUILD_NM)
  AC_SUBST(BUILD_AS)
  AC_SUBST(BUILD_AR)
])

# Do some additional checks on the detected tools.
AC_DEFUN_ONCE([TOOLCHAIN_MISC_CHECKS],
[
  # Check for extra potential brokenness.
  if test  "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    # On Windows, double-check that we got the right compiler.
    CC_VERSION_OUTPUT=`$CC 2>&1 1>/dev/null | $HEAD -n 1 | $TR -d '\r'`
    COMPILER_CPU_TEST=`$ECHO $CC_VERSION_OUTPUT | $SED -n "s/^.* \(.*\)$/\1/p"`
    if test "x$OPENJDK_TARGET_CPU" = "xx86"; then
      if test "x$COMPILER_CPU_TEST" != "x80x86" -a "x$COMPILER_CPU_TEST" != "xx86"; then
        AC_MSG_ERROR([Target CPU mismatch. We are building for $OPENJDK_TARGET_CPU but CL is for "$COMPILER_CPU_TEST"; expected "80x86" or "x86".])
      fi
    elif test "x$OPENJDK_TARGET_CPU" = "xx86_64"; then
      if test "x$COMPILER_CPU_TEST" != "xx64"; then
        AC_MSG_ERROR([Target CPU mismatch. We are building for $OPENJDK_TARGET_CPU but CL is for "$COMPILER_CPU_TEST"; expected "x64".])
      fi
    elif test "x$OPENJDK_TARGET_CPU" = "xaarch64"; then
      if test "x$COMPILER_CPU_TEST" != "xARM64"; then
        AC_MSG_ERROR([Target CPU mismatch. We are building for $OPENJDK_TARGET_CPU but CL is for "$COMPILER_CPU_TEST"; expected "arm64".])
      fi
    fi
  fi

  if test "x$TOOLCHAIN_TYPE" = xgcc || test "x$TOOLCHAIN_TYPE" = xclang; then
    # Check if linker has -z noexecstack.
    HAS_NOEXECSTACK=`$CC -Wl,--help 2>/dev/null | $GREP 'z noexecstack'`
    # This is later checked when setting flags.
  fi

  # Setup hotspot lecagy names for toolchains
  HOTSPOT_TOOLCHAIN_TYPE=$TOOLCHAIN_TYPE
  if test "x$TOOLCHAIN_TYPE" = xclang; then
    HOTSPOT_TOOLCHAIN_TYPE=gcc
  elif test "x$TOOLCHAIN_TYPE" = xmicrosoft; then
    HOTSPOT_TOOLCHAIN_TYPE=visCPP
  fi
  AC_SUBST(HOTSPOT_TOOLCHAIN_TYPE)
])

# Setup the JTReg Regression Test Harness.
AC_DEFUN_ONCE([TOOLCHAIN_SETUP_JTREG],
[
  AC_ARG_WITH(jtreg, [AS_HELP_STRING([--with-jtreg],
      [Regression Test Harness @<:@probed@:>@])])

  if test "x$with_jtreg" = xno; then
    # jtreg disabled
    AC_MSG_CHECKING([for jtreg test harness])
    AC_MSG_RESULT([no, disabled])
  elif test "x$with_jtreg" != xyes && test "x$with_jtreg" != x; then
    if test -d "$with_jtreg"; then
      # An explicit path is specified, use it.
      JT_HOME="$with_jtreg"
    else
      case "$with_jtreg" in
        *.zip )
          JTREG_SUPPORT_DIR=$CONFIGURESUPPORT_OUTPUTDIR/jtreg
          $RM -rf $JTREG_SUPPORT_DIR
          $MKDIR -p $JTREG_SUPPORT_DIR
          $UNZIP -qq -d $JTREG_SUPPORT_DIR $with_jtreg

          # Try to find jtreg to determine JT_HOME path
          JTREG_PATH=`$FIND $JTREG_SUPPORT_DIR | $GREP "/bin/jtreg"`
          if test "x$JTREG_PATH" != x; then
            JT_HOME=$($DIRNAME $($DIRNAME $JTREG_PATH))
          fi
          ;;
        * )
          ;;
      esac
    fi
    UTIL_FIXUP_PATH([JT_HOME])
    if test ! -d "$JT_HOME"; then
      AC_MSG_ERROR([jtreg home directory from --with-jtreg=$with_jtreg does not exist])
    fi

    if test ! -e "$JT_HOME/lib/jtreg.jar"; then
      AC_MSG_ERROR([jtreg home directory from --with-jtreg=$with_jtreg is not a valid jtreg home])
    fi

    AC_MSG_CHECKING([for jtreg test harness])
    AC_MSG_RESULT([$JT_HOME])
  else
    # Try to locate jtreg using the JT_HOME environment variable
    if test "x$JT_HOME" != x; then
      # JT_HOME set in environment, use it
      if test ! -d "$JT_HOME"; then
        AC_MSG_WARN([Ignoring JT_HOME pointing to invalid directory: $JT_HOME])
        JT_HOME=
      else
        if test ! -e "$JT_HOME/lib/jtreg.jar"; then
          AC_MSG_WARN([Ignoring JT_HOME which is not a valid jtreg home: $JT_HOME])
          JT_HOME=
        else
          AC_MSG_NOTICE([Located jtreg using JT_HOME from environment])
        fi
      fi
    fi

    if test "x$JT_HOME" = x; then
      # JT_HOME is not set in environment, or was deemed invalid.
      # Try to find jtreg on path
      UTIL_LOOKUP_PROGS(JTREGEXE, jtreg)
      if test "x$JTREGEXE" != x; then
        # That's good, now try to derive JT_HOME
        JT_HOME=`(cd $($DIRNAME $JTREGEXE)/.. && pwd)`
        if test ! -e "$JT_HOME/lib/jtreg.jar"; then
          AC_MSG_WARN([Ignoring jtreg from path since a valid jtreg home cannot be found])
          JT_HOME=
        else
          AC_MSG_NOTICE([Located jtreg using jtreg executable in path])
        fi
      fi
    fi

    AC_MSG_CHECKING([for jtreg test harness])
    if test "x$JT_HOME" != x; then
      AC_MSG_RESULT([$JT_HOME])
    else
      AC_MSG_RESULT([no, not found])

      if test "x$with_jtreg" = xyes; then
        AC_MSG_ERROR([--with-jtreg was specified, but no jtreg found.])
      fi
    fi
  fi

  UTIL_FIXUP_PATH(JT_HOME)
  AC_SUBST(JT_HOME)
])

# Setup the JIB dependency resolver
AC_DEFUN_ONCE([TOOLCHAIN_SETUP_JIB],
[
  AC_ARG_WITH(jib, [AS_HELP_STRING([--with-jib],
      [Jib dependency management tool @<:@not used@:>@])])

  if test "x$with_jib" = xno || test "x$with_jib" = x; then
    # jib disabled
    AC_MSG_CHECKING([for jib])
    AC_MSG_RESULT(no)
  elif test "x$with_jib" = xyes; then
    AC_MSG_ERROR([Must supply a value to --with-jib])
  else
    JIB_HOME="${with_jib}"
    AC_MSG_CHECKING([for jib])
    AC_MSG_RESULT(${JIB_HOME})
    if test ! -d "${JIB_HOME}"; then
      AC_MSG_ERROR([--with-jib must be a directory])
    fi
    JIB_JAR=$(ls ${JIB_HOME}/lib/jib-*.jar)
    if test ! -f "${JIB_JAR}"; then
      AC_MSG_ERROR([Could not find jib jar file in ${JIB_HOME}])
    fi
  fi

  AC_SUBST(JIB_HOME)
])
