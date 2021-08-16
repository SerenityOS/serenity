#
# Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

###############################################################################
# Setup the most fundamental tools that relies on not much else to set up,
# but is used by much of the early bootstrap code.
AC_DEFUN_ONCE([BASIC_SETUP_FUNDAMENTAL_TOOLS],
[
  # Bootstrapping: These tools are needed by UTIL_LOOKUP_PROGS
  AC_PATH_PROGS(BASENAME, basename)
  UTIL_CHECK_NONEMPTY(BASENAME)
  AC_PATH_PROGS(DIRNAME, dirname)
  UTIL_CHECK_NONEMPTY(DIRNAME)
  AC_PATH_PROGS(FILE, file)
  UTIL_CHECK_NONEMPTY(FILE)
  AC_PATH_PROGS(LDD, ldd)

  # First are all the fundamental required tools.
  UTIL_REQUIRE_PROGS(BASH, bash)
  UTIL_REQUIRE_PROGS(CAT, cat)
  UTIL_REQUIRE_PROGS(CHMOD, chmod)
  UTIL_REQUIRE_PROGS(CP, cp)
  UTIL_REQUIRE_PROGS(CUT, cut)
  UTIL_REQUIRE_PROGS(DATE, date)
  UTIL_REQUIRE_PROGS(DIFF, gdiff diff)
  UTIL_REQUIRE_PROGS(ECHO, echo)
  UTIL_REQUIRE_PROGS(EXPR, expr)
  UTIL_REQUIRE_PROGS(FIND, find)
  UTIL_REQUIRE_PROGS(GUNZIP, gunzip)
  UTIL_REQUIRE_PROGS(GZIP, pigz gzip)
  UTIL_REQUIRE_PROGS(HEAD, head)
  UTIL_REQUIRE_PROGS(LN, ln)
  UTIL_REQUIRE_PROGS(LS, ls)
  # gmkdir is known to be safe for concurrent invocations with -p flag.
  UTIL_REQUIRE_PROGS(MKDIR, gmkdir mkdir)
  UTIL_REQUIRE_PROGS(MKTEMP, mktemp)
  UTIL_REQUIRE_PROGS(MV, mv)
  UTIL_REQUIRE_PROGS(AWK, gawk nawk awk)
  UTIL_REQUIRE_PROGS(PRINTF, printf)
  UTIL_REQUIRE_PROGS(RM, rm)
  UTIL_REQUIRE_PROGS(RMDIR, rmdir)
  UTIL_REQUIRE_PROGS(SH, sh)
  UTIL_REQUIRE_PROGS(SORT, sort)
  UTIL_REQUIRE_PROGS(TAIL, tail)
  UTIL_REQUIRE_PROGS(TAR, gtar tar)
  UTIL_REQUIRE_PROGS(TEE, tee)
  UTIL_REQUIRE_PROGS(TOUCH, touch)
  UTIL_REQUIRE_PROGS(TR, tr)
  UTIL_REQUIRE_PROGS(UNAME, uname)
  UTIL_REQUIRE_PROGS(WC, wc)
  UTIL_REQUIRE_PROGS(XARGS, xargs)

  # Then required tools that require some special treatment.
  UTIL_REQUIRE_SPECIAL(GREP, [AC_PROG_GREP])
  UTIL_REQUIRE_SPECIAL(EGREP, [AC_PROG_EGREP])
  UTIL_REQUIRE_SPECIAL(FGREP, [AC_PROG_FGREP])
  UTIL_REQUIRE_SPECIAL(SED, [AC_PROG_SED])

  # Optional tools, we can do without them
  UTIL_LOOKUP_PROGS(DF, df)
  UTIL_LOOKUP_PROGS(NICE, nice)
  UTIL_LOOKUP_PROGS(READLINK, greadlink readlink)

  # These are only needed on some platforms
  UTIL_LOOKUP_PROGS(PATHTOOL, cygpath wslpath)
  UTIL_LOOKUP_PROGS(LSB_RELEASE, lsb_release)
  UTIL_LOOKUP_PROGS(CMD, cmd.exe, $PATH:/cygdrive/c/windows/system32:/mnt/c/windows/system32:/c/windows/system32)

  # For compare.sh only
  UTIL_LOOKUP_PROGS(CMP, cmp)
  UTIL_LOOKUP_PROGS(UNIQ, uniq)

  # Always force rm.
  RM="$RM -f"
])

###############################################################################
# Check if we have found a usable version of make
# $1: the path to a potential make binary (or empty)
# $2: the description on how we found this
AC_DEFUN([BASIC_CHECK_MAKE_VERSION],
[
  MAKE_CANDIDATE="$1"
  DESCRIPTION="$2"

  # On Cygwin, we require a newer version of make than on other platforms
  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.cygwin"; then
    MAKE_VERSION_EXPR="-e 4\."
    MAKE_REQUIRED_VERSION="4.0"
   else
    MAKE_VERSION_EXPR="-e 3\.8[[12]] -e 4\."
    MAKE_REQUIRED_VERSION="3.81"
  fi

  if test "x$MAKE_CANDIDATE" != x; then
    AC_MSG_NOTICE([Testing potential make at $MAKE_CANDIDATE, found using $DESCRIPTION])
    MAKE_VERSION_STRING=`$MAKE_CANDIDATE --version | $HEAD -n 1`
    IS_GNU_MAKE=`$ECHO $MAKE_VERSION_STRING | $GREP 'GNU Make'`
    if test "x$IS_GNU_MAKE" = x; then
      AC_MSG_NOTICE([Found potential make at $MAKE_CANDIDATE, however, this is not GNU Make. Ignoring.])
    else
      IS_MODERN_MAKE=`$ECHO $MAKE_VERSION_STRING | $GREP $MAKE_VERSION_EXPR`
      if test "x$IS_MODERN_MAKE" = x; then
        AC_MSG_NOTICE([Found GNU make at $MAKE_CANDIDATE, however this is not version $MAKE_REQUIRED_VERSION or later. (it is: $MAKE_VERSION_STRING). Ignoring.])
      else
        if test "x$OPENJDK_BUILD_OS" = "xwindows"; then
          if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.cygwin"; then
            MAKE_EXPECTED_ENV='cygwin'
          elif test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.msys2"; then
            MAKE_EXPECTED_ENV='msys'
          elif test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl1" || test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl2"; then
            if test "x$OPENJDK_BUILD_CPU" = "xaarch64"; then
              MAKE_EXPECTED_ENV='aarch64-.*-linux-gnu'
            else
              MAKE_EXPECTED_ENV='x86_64-.*-linux-gnu'
            fi
          else
            AC_MSG_ERROR([Unknown Windows environment])
          fi
          MAKE_BUILT_FOR=`$MAKE_CANDIDATE --version | $GREP -i 'built for'`
          IS_MAKE_CORRECT_ENV=`$ECHO $MAKE_BUILT_FOR | $GREP $MAKE_EXPECTED_ENV`
        else
          # Not relevant for non-Windows
          IS_MAKE_CORRECT_ENV=true
        fi
        if test "x$IS_MAKE_CORRECT_ENV" = x; then
          AC_MSG_NOTICE([Found GNU make version $MAKE_VERSION_STRING at $MAKE_CANDIDATE, but it is not for $MAKE_EXPECTED_ENV (it says: $MAKE_BUILT_FOR). Ignoring.])
        else
          FOUND_MAKE=$MAKE_CANDIDATE
          UTIL_FIXUP_EXECUTABLE(FOUND_MAKE)
        fi
      fi
    fi
  fi
])

###############################################################################
AC_DEFUN([BASIC_CHECK_MAKE_OUTPUT_SYNC],
[
  # Check if make supports the output sync option and if so, setup using it.
  AC_MSG_CHECKING([if make --output-sync is supported])
  if $MAKE --version -O > /dev/null 2>&1; then
    OUTPUT_SYNC_SUPPORTED=true
    AC_MSG_RESULT([yes])
    AC_MSG_CHECKING([for output-sync value])
    AC_ARG_WITH([output-sync], [AS_HELP_STRING([--with-output-sync],
      [set make output sync type if supported by make. @<:@recurse@:>@])],
      [OUTPUT_SYNC=$with_output_sync])
    if test "x$OUTPUT_SYNC" = "x"; then
      OUTPUT_SYNC=none
    fi
    AC_MSG_RESULT([$OUTPUT_SYNC])
    if ! $MAKE --version -O$OUTPUT_SYNC > /dev/null 2>&1; then
      AC_MSG_ERROR([Make did not the support the value $OUTPUT_SYNC as output sync type.])
    fi
  else
    OUTPUT_SYNC_SUPPORTED=false
    AC_MSG_RESULT([no])
  fi
  AC_SUBST(OUTPUT_SYNC_SUPPORTED)
  AC_SUBST(OUTPUT_SYNC)
])

###############################################################################
# Goes looking for a usable version of GNU make.
AC_DEFUN([BASIC_CHECK_GNU_MAKE],
[
  UTIL_SETUP_TOOL(MAKE,
  [
    # Try our hardest to locate a correct version of GNU make
    UTIL_LOOKUP_PROGS(CHECK_GMAKE, gmake)
    BASIC_CHECK_MAKE_VERSION("$CHECK_GMAKE", [gmake in PATH])

    if test "x$FOUND_MAKE" = x; then
      UTIL_LOOKUP_PROGS(CHECK_MAKE, make)
      BASIC_CHECK_MAKE_VERSION("$CHECK_MAKE", [make in PATH])
    fi

    if test "x$FOUND_MAKE" = x; then
      if test "x$TOOLCHAIN_PATH" != x; then
        # We have a toolchain path, check that as well before giving up.
        OLD_PATH=$PATH
        PATH=$TOOLCHAIN_PATH:$PATH
        UTIL_LOOKUP_PROGS(CHECK_TOOLSDIR_GMAKE, gmake)
        BASIC_CHECK_MAKE_VERSION("$CHECK_TOOLSDIR_GMAKE", [gmake in tools-dir])
        if test "x$FOUND_MAKE" = x; then
          UTIL_LOOKUP_PROGS(CHECK_TOOLSDIR_MAKE, make)
          BASIC_CHECK_MAKE_VERSION("$CHECK_TOOLSDIR_MAKE", [make in tools-dir])
        fi
        PATH=$OLD_PATH
      fi
    fi

    if test "x$FOUND_MAKE" = x; then
      AC_MSG_ERROR([Cannot find GNU make $MAKE_REQUIRED_VERSION or newer! Please put it in the path, or add e.g. MAKE=/opt/gmake3.81/make as argument to configure.])
    fi
  ],[
    # If MAKE was set by user, verify the version
    BASIC_CHECK_MAKE_VERSION("$MAKE", [user supplied MAKE=$MAKE])
    if test "x$FOUND_MAKE" = x; then
      AC_MSG_ERROR([The specified make (by MAKE=$MAKE) is not GNU make $MAKE_REQUIRED_VERSION or newer.])
    fi
  ])

  MAKE=$FOUND_MAKE
  AC_SUBST(MAKE)
  AC_MSG_NOTICE([Using GNU make at $FOUND_MAKE (version: $MAKE_VERSION_STRING)])

  BASIC_CHECK_MAKE_OUTPUT_SYNC
])

###############################################################################
AC_DEFUN([BASIC_CHECK_FIND_DELETE],
[
  # Test if find supports -delete
  AC_MSG_CHECKING([if find supports -delete])
  FIND_DELETE="-delete"

  DELETEDIR=`$MKTEMP -d tmp.XXXXXXXXXX` || (echo Could not create temporary directory!; exit $?)

  echo Hejsan > $DELETEDIR/TestIfFindSupportsDelete

  TEST_DELETE=`$FIND "$DELETEDIR" -name TestIfFindSupportsDelete $FIND_DELETE 2>&1`
  if test -f $DELETEDIR/TestIfFindSupportsDelete; then
    # No, it does not.
    $RM $DELETEDIR/TestIfFindSupportsDelete
    if test "x$OPENJDK_TARGET_OS" = "xaix"; then
      # AIX 'find' is buggy if called with '-exec {} \+' and an empty file list
      FIND_DELETE="-print | $XARGS $RM"
    else
      FIND_DELETE="-exec $RM \{\} \+"
    fi
    AC_MSG_RESULT([no])
  else
    AC_MSG_RESULT([yes])
  fi
  $RMDIR $DELETEDIR
  AC_SUBST(FIND_DELETE)
])

###############################################################################
AC_DEFUN([BASIC_CHECK_TAR],
[
  # Test which kind of tar was found
  if test "x$($TAR --version | $GREP "GNU tar")" != "x"; then
    TAR_TYPE="gnu"
  elif test "x$($TAR --version | $GREP "bsdtar")" != "x"; then
    TAR_TYPE="bsd"
  elif test "x$($TAR -v | $GREP "bsdtar")" != "x"; then
    TAR_TYPE="bsd"
  elif test "x$OPENJDK_BUILD_OS" = "xaix"; then
    TAR_TYPE="aix"
  fi
  AC_MSG_CHECKING([what type of tar was found])
  AC_MSG_RESULT([$TAR_TYPE])

  if test "x$TAR_TYPE" = "xgnu"; then
    TAR_INCLUDE_PARAM="T"
    TAR_SUPPORTS_TRANSFORM="true"
  elif test "x$TAR_TYPE" = "aix"; then
    # -L InputList of aix tar: name of file listing the files and directories
    # that need to be   archived or extracted
    TAR_INCLUDE_PARAM="L"
    TAR_SUPPORTS_TRANSFORM="false"
  else
    TAR_INCLUDE_PARAM="I"
    TAR_SUPPORTS_TRANSFORM="false"
  fi
  AC_SUBST(TAR_TYPE)
  AC_SUBST(TAR_INCLUDE_PARAM)
  AC_SUBST(TAR_SUPPORTS_TRANSFORM)
])

###############################################################################
AC_DEFUN([BASIC_CHECK_GREP],
[
  # Test that grep supports -Fx with a list of pattern which includes null pattern.
  # This is a problem for the grep resident on AIX.
  AC_MSG_CHECKING([that grep ($GREP) -Fx handles empty lines in the pattern list correctly])
  # Multiple subsequent spaces..
  STACK_SPACES='aaa   bbb   ccc'
  # ..converted to subsequent newlines, causes STACK_LIST to be a list with some empty
  # patterns in it.
  STACK_LIST=${STACK_SPACES// /$'\n'}
  NEEDLE_SPACES='ccc bbb aaa'
  NEEDLE_LIST=${NEEDLE_SPACES// /$'\n'}
  RESULT="$($GREP -Fvx "$STACK_LIST" <<< "$NEEDLE_LIST")"
  if test "x$RESULT" == "x"; then
    AC_MSG_RESULT([yes])
  else
    if test "x$OPENJDK_TARGET_OS" = "xaix"; then
      ADDINFO="Please make sure you use GNU grep, usually found at /opt/freeware/bin."
    fi
    AC_MSG_ERROR([grep does not handle -Fx correctly. ${ADDINFO}])
  fi
])

###############################################################################
AC_DEFUN_ONCE([BASIC_SETUP_COMPLEX_TOOLS],
[
  BASIC_CHECK_GNU_MAKE

  BASIC_CHECK_FIND_DELETE
  BASIC_CHECK_TAR
  BASIC_CHECK_GREP
  BASIC_SETUP_PANDOC

  # These tools might not be installed by default,
  # need hint on how to install them.
  UTIL_REQUIRE_PROGS(UNZIP, unzip)
  # Since zip uses "ZIP" as a environment variable for passing options, we need
  # to name our variable differently, hence ZIPEXE.
  UTIL_REQUIRE_PROGS(ZIPEXE, zip)

  # Non-required basic tools

  UTIL_LOOKUP_PROGS(READELF, greadelf readelf)
  UTIL_LOOKUP_PROGS(DOT, dot)
  UTIL_LOOKUP_PROGS(HG, hg)
  UTIL_LOOKUP_PROGS(GIT, git)
  UTIL_LOOKUP_PROGS(STAT, stat)
  UTIL_LOOKUP_PROGS(TIME, time)
  UTIL_LOOKUP_PROGS(FLOCK, flock)
  # Dtrace is usually found in /usr/sbin, but that directory may not
  # be in the user path.
  UTIL_LOOKUP_PROGS(DTRACE, dtrace, $PATH:/usr/sbin)
  UTIL_LOOKUP_PROGS(PATCH, gpatch patch)
  # Check if it's GNU time
  IS_GNU_TIME=`$TIME --version 2>&1 | $GREP 'GNU time'`
  if test "x$IS_GNU_TIME" != x; then
    IS_GNU_TIME=yes
  else
    IS_GNU_TIME=no
  fi
  AC_SUBST(IS_GNU_TIME)

  if test "x$OPENJDK_TARGET_OS" = "xmacosx"; then
    UTIL_REQUIRE_PROGS(DSYMUTIL, dsymutil)
    UTIL_REQUIRE_PROGS(MIG, mig)
    UTIL_REQUIRE_PROGS(XATTR, xattr)
    UTIL_LOOKUP_PROGS(CODESIGN, codesign)

    if test "x$CODESIGN" != "x"; then
      # Check for user provided code signing identity.
      # If no identity was provided, fall back to "openjdk_codesign".
      AC_ARG_WITH([macosx-codesign-identity], [AS_HELP_STRING([--with-macosx-codesign-identity],
        [specify the code signing identity])],
        [MACOSX_CODESIGN_IDENTITY=$with_macosx_codesign_identity],
        [MACOSX_CODESIGN_IDENTITY=openjdk_codesign]
      )

      AC_SUBST(MACOSX_CODESIGN_IDENTITY)

      # Verify that the codesign certificate is present
      AC_MSG_CHECKING([if codesign certificate is present])
      $RM codesign-testfile
      $TOUCH codesign-testfile
      $CODESIGN -s "$MACOSX_CODESIGN_IDENTITY" codesign-testfile 2>&AS_MESSAGE_LOG_FD \
          >&AS_MESSAGE_LOG_FD || CODESIGN=
      $RM codesign-testfile
      if test "x$CODESIGN" = x; then
        AC_MSG_RESULT([no])
      else
        AC_MSG_RESULT([yes])
        # Verify that the codesign has --option runtime
        AC_MSG_CHECKING([if codesign has --option runtime])
        $RM codesign-testfile
        $TOUCH codesign-testfile
        $CODESIGN --option runtime -s "$MACOSX_CODESIGN_IDENTITY" codesign-testfile \
            2>&AS_MESSAGE_LOG_FD >&AS_MESSAGE_LOG_FD || CODESIGN=
        $RM codesign-testfile
        if test "x$CODESIGN" = x; then
          AC_MSG_ERROR([codesign does not have --option runtime. macOS 10.13.6 and above is required.])
        else
          AC_MSG_RESULT([yes])
        fi
      fi
    fi
    UTIL_REQUIRE_PROGS(SETFILE, SetFile)
  fi
  if ! test "x$OPENJDK_TARGET_OS" = "xwindows"; then
    UTIL_REQUIRE_PROGS(ULIMIT, ulimit)
  fi
])

###############################################################################
# Check for support for specific options in bash
AC_DEFUN_ONCE([BASIC_CHECK_BASH_OPTIONS],
[
  # Check bash version
  # Extra [ ] to stop m4 mangling
  [ BASH_VER=`$BASH --version | $SED -n  -e 's/^.*bash.*ersion *\([0-9.]*\).*$/\1/ p'` ]
  AC_MSG_CHECKING([bash version])
  AC_MSG_RESULT([$BASH_VER])

  BASH_MAJOR=`$ECHO $BASH_VER | $CUT -d . -f 1`
  BASH_MINOR=`$ECHO $BASH_VER | $CUT -d . -f 2`
  if test $BASH_MAJOR -lt 3 || (test $BASH_MAJOR -eq 3 && test $BASH_MINOR -lt 2); then
    AC_MSG_ERROR([bash version 3.2 or better is required])
  fi

  # Test if bash supports pipefail.
  AC_MSG_CHECKING([if bash supports pipefail])
  if ${BASH} -c 'set -o pipefail'; then
    BASH_ARGS="$BASH_ARGS -o pipefail"
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
  fi

  AC_MSG_CHECKING([if bash supports errexit (-e)])
  if ${BASH} -e -c 'true'; then
    BASH_ARGS="$BASH_ARGS -e"
    AC_MSG_RESULT([yes])
  else
    AC_MSG_RESULT([no])
  fi

  AC_SUBST(BASH_ARGS)
])

################################################################################
#
# Setup Pandoc
#
AC_DEFUN_ONCE([BASIC_SETUP_PANDOC],
[
  UTIL_LOOKUP_PROGS(PANDOC, pandoc)

  PANDOC_MARKDOWN_FLAG="markdown"
  if test -n "$PANDOC"; then
    AC_MSG_CHECKING(if the pandoc smart extension needs to be disabled for markdown)
    if $PANDOC --list-extensions | $GREP -q '\+smart'; then
      AC_MSG_RESULT([yes])
      PANDOC_MARKDOWN_FLAG="markdown-smart"
    else
      AC_MSG_RESULT([no])
    fi
  fi

  if test -n "$PANDOC"; then
    ENABLE_PANDOC="true"
  else
    ENABLE_PANDOC="false"
  fi
  AC_SUBST(ENABLE_PANDOC)
  AC_SUBST(PANDOC_MARKDOWN_FLAG)
])
