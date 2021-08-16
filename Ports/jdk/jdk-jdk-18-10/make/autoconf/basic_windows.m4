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

# Setup basic configuration paths, and platform-specific stuff related to PATHs.
AC_DEFUN([BASIC_SETUP_PATHS_WINDOWS],
[
  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl"; then
    # Clarify if it is wsl1 or wsl2, and use that as OS_ENV from this point forward
    $PATHTOOL -w / > /dev/null 2>&1
    if test $? -ne 0; then
      # Without Windows access to our root, it's definitely WSL1
      OPENJDK_BUILD_OS_ENV=windows.wsl1
    else
      # This test is not guaranteed, but there is no documented way of
      # distinguishing between WSL1 and WSL2. Assume only WSL2 has WSL_INTEROP
      # in /run/WSL
      if test -d "/run/WSL" ; then
        OPENJDK_BUILD_OS_ENV=windows.wsl2
      else
        OPENJDK_BUILD_OS_ENV=windows.wsl1
      fi
    fi
    # This is a bit silly since we really don't have a target env as such,
    # but do it to keep consistency.
    OPENJDK_TARGET_OS_ENV=$OPENJDK_BUILD_OS_ENV
  fi

  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.msys2"; then
    # Must be done prior to calling any commands to avoid mangling of command line
    export MSYS2_ARG_CONV_EXCL="*"
  fi

  AC_MSG_CHECKING([Windows environment type])
  WINENV_VENDOR=${OPENJDK_BUILD_OS_ENV#windows.}
  AC_MSG_RESULT([$WINENV_VENDOR])

  if test "x$WINENV_VENDOR" = x; then
    AC_MSG_ERROR([Unknown Windows environment. Neither cygwin, msys2, wsl1 nor wsl2 was detected.])
  fi

  if test "x$PATHTOOL" = x; then
    AC_MSG_ERROR([Incorrect $WINENV_VENDOR installation. Neither cygpath nor wslpath was found])
  fi

  if test "x$CMD" = x; then
    AC_MSG_ERROR([Incorrect Windows/$WINENV_VENDOR setup. Could not locate cmd.exe])
  fi

  AC_MSG_CHECKING([$WINENV_VENDOR drive prefix])
  WINENV_PREFIX=`$PATHTOOL -u c:/ | $SED -e 's!/c/!!'`
  AC_MSG_RESULT(['$WINENV_PREFIX'])
  AC_SUBST(WINENV_PREFIX)

  AC_MSG_CHECKING([$WINENV_VENDOR root directory as Windows path])
  if test "x$OPENJDK_BUILD_OS_ENV" != "xwindows.wsl1"; then
    WINENV_ROOT=`$PATHTOOL -w / 2> /dev/null`
    # msys2 has a trailing backslash; strip it
    WINENV_ROOT=${WINENV_ROOT%\\}
  else
    WINENV_ROOT='[[unavailable]]'
  fi
  AC_MSG_RESULT(['$WINENV_ROOT'])
  AC_SUBST(WINENV_ROOT)

  AC_MSG_CHECKING([$WINENV_VENDOR temp directory])
  WINENV_TEMP_DIR=$($PATHTOOL -u $($CMD /q /c echo %TEMP% 2> /dev/null) | $TR -d '\r\n')
  AC_MSG_RESULT([$WINENV_TEMP_DIR])

  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl2"; then
    # Don't trust the current directory for WSL2, but change to an OK temp dir
    cd "$WINENV_TEMP_DIR"
    # Bring along confdefs.h or autoconf gets all confused
    cp "$CONFIGURE_START_DIR/confdefs.h" "$WINENV_TEMP_DIR"
  fi

  AC_MSG_CHECKING([$WINENV_VENDOR release])
  WINENV_UNAME_RELEASE=`$UNAME -r`
  AC_MSG_RESULT([$WINENV_UNAME_RELEASE])

  AC_MSG_CHECKING([$WINENV_VENDOR version])
  WINENV_UNAME_VERSION=`$UNAME -v`
  AC_MSG_RESULT([$WINENV_UNAME_VERSION])

  WINENV_VERSION="$WINENV_UNAME_RELEASE, $WINENV_UNAME_VERSION"

  AC_MSG_CHECKING([Windows version])

  # We must change directory to one guaranteed to work, otherwise WSL1
  # can complain (since it does not have a WINENV_ROOT so it can't access
  # unix-style paths from Windows.
  # Additional [] needed to keep m4 from mangling shell constructs.
  [ WINDOWS_VERSION=`cd $WINENV_TEMP_DIR && $CMD /c ver | $EGREP -o '([0-9]+\.)+[0-9]+'` ]
  AC_MSG_RESULT([$WINDOWS_VERSION])

  # Additional handling per specific env
  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.cygwin"; then
    # Additional [] needed to keep m4 from mangling shell constructs.
    [ CYGWIN_VERSION_OLD=`$ECHO $WINENV_UNAME_RELEASE | $GREP -e '^1\.[0-6]'` ]
    if test "x$CYGWIN_VERSION_OLD" != x; then
      AC_MSG_NOTICE([Your cygwin is too old. You are running $CYGWIN_RELEASE, but at least cygwin 1.7 is required. Please upgrade.])
      AC_MSG_ERROR([Cannot continue])
    fi
    if test "x$LDD" = x; then
      AC_MSG_ERROR([ldd is missing, which is needed on cygwin])
    fi
    WINENV_MARKER_DLL=cygwin1.dll
  elif test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.msys2"; then
    if test "x$LDD" = x; then
      AC_MSG_ERROR([ldd is missing, which is needed on msys2])
    fi
    WINENV_MARKER_DLL=msys-2.0.dll
  elif test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl1" || test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl2"; then
    AC_MSG_CHECKING([wsl distribution])
    WSL_DISTRIBUTION=`$LSB_RELEASE -d | sed 's/Description:\t//'`
    AC_MSG_RESULT([$WSL_DISTRIBUTION])

    WINENV_VERSION="$WINENV_VERSION ($WSL_DISTRIBUTION)"

    # Tell WSL to automatically translate the PATH variable
    export WSLENV=PATH/l
  fi

  # Chicken and egg: FIXPATH is needed for UTIL_FIXUP_PATH to work. So for the
  # first run we use the auto-detect abilities of fixpath.sh.
  FIXPATH_DIR="$TOPDIR/make/scripts"
  FIXPATH="$BASH $FIXPATH_DIR/fixpath.sh exec"
  FIXPATH_BASE="$BASH $FIXPATH_DIR/fixpath.sh"
  FIXPATH_SAVED_PATH="$PATH"
  UTIL_FIXUP_PATH(FIXPATH_DIR)

  # Now we can use FIXPATH_DIR to rewrite path to fixpath.sh properly.
  if test "x$WINENV_PREFIX" = x; then
    # On msys the prefix is empty, but we need to pass something to have the
    # fixpath.sh options parser happy.
    WINENV_PREFIX_ARG="NONE"
  else
    WINENV_PREFIX_ARG="$WINENV_PREFIX"
  fi
  FIXPATH_ARGS="-e $PATHTOOL -p $WINENV_PREFIX_ARG -r ${WINENV_ROOT//\\/\\\\}  -t $WINENV_TEMP_DIR -c $CMD -q"
  FIXPATH_BASE="$BASH $FIXPATH_DIR/fixpath.sh $FIXPATH_ARGS"
  FIXPATH="$FIXPATH_BASE exec"

  AC_SUBST(FIXPATH_BASE)
  AC_SUBST(FIXPATH)

  SRC_ROOT_LENGTH=`$ECHO "$TOPDIR" | $WC -m`
  if test $SRC_ROOT_LENGTH -gt 100; then
    AC_MSG_ERROR([Your base path is too long. It is $SRC_ROOT_LENGTH characters long, but only 100 is supported])
  fi

  # Test if windows or unix "find" is first in path.
  AC_MSG_CHECKING([what kind of 'find' is first on the PATH])
  FIND_BINARY_OUTPUT=`find --version 2>&1`
  if test "x`echo $FIND_BINARY_OUTPUT | $GREP GNU`" != x; then
    AC_MSG_RESULT([unix style])
  elif test "x`echo $FIND_BINARY_OUTPUT | $GREP FIND`" != x; then
    AC_MSG_RESULT([Windows])
    AC_MSG_NOTICE([Your path contains Windows tools (C:\Windows\system32) before your unix tools.])
    AC_MSG_NOTICE([This will not work. Please correct and make sure /usr/bin (or similar) is first in path.])
    AC_MSG_ERROR([Cannot continue])
  else
    AC_MSG_RESULT([unknown])
    AC_MSG_WARN([It seems that your find utility is non-standard.])
  fi
])

# Verify that the directory is usable on Windows
AC_DEFUN([BASIC_WINDOWS_VERIFY_DIR],
[
  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl1"; then
    OUTPUTDIR_WIN=`$FIXPATH_BASE print $1`
    if test "x$OUTPUTDIR_WIN" = x; then
      AC_MSG_NOTICE([For wsl1, the $2 dir must be located on a Windows drive. Please see doc/building.md for details.])
      AC_MSG_ERROR([Cannot continue])
    fi
  fi
])

# Create fixpath wrapper
AC_DEFUN([BASIC_WINDOWS_FINALIZE_FIXPATH],
[
  if test "x$OPENJDK_BUILD_OS" = xwindows; then
    FIXPATH_CMDLINE=". $TOPDIR/make/scripts/fixpath.sh -e $PATHTOOL \
        -p $WINENV_PREFIX_ARG -r ${WINENV_ROOT//\\/\\\\}  -t $WINENV_TEMP_DIR \
        -c $CMD -q"
    $ECHO >  $OUTPUTDIR/fixpath '#!/bin/bash'
    $ECHO >> $OUTPUTDIR/fixpath export PATH='"[$]PATH:'$PATH'"'
    $ECHO >> $OUTPUTDIR/fixpath $FIXPATH_CMDLINE '"[$]@"'
    $CHMOD +x $OUTPUTDIR/fixpath
    FIXPATH_BASE="$OUTPUTDIR/fixpath"
    FIXPATH="$FIXPATH_BASE exec"
  fi
])

# Platform-specific finalization
AC_DEFUN([BASIC_WINDOWS_FINALIZE],
[
  if test "x$OPENJDK_BUILD_OS_ENV" = "xwindows.wsl2"; then
    # Change back from temp dir
    cd $CONFIGURE_START_DIR
  fi
])
