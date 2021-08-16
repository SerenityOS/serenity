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

################################################################################
# Setup X11 Windows system
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_X11],
[
  if test "x$NEEDS_LIB_X11" = xfalse; then
    if (test "x${with_x}" != x && test "x${with_x}" != xno); then
      AC_MSG_WARN([X11 is not used, so --with-x is ignored])
    fi
    X_CFLAGS=
    X_LIBS=
  else

    if test "x${with_x}" = xno; then
      AC_MSG_ERROR([It is not possible to disable the use of X11. Remove the --without-x option.])
    fi

    if test "x${with_x}" != x &&  test "x${with_x}" != xyes; then
      # The user has specified a X11 base directory. Use it for includes and
      # libraries, unless explicitly overridden.
      if test "x$x_includes" = xNONE; then
        x_includes="${with_x}/include"
      fi
      if test "x$x_libraries" = xNONE; then
        x_libraries="${with_x}/lib"
      fi
    else
      # Check if the user has specified sysroot, but not --with-x, --x-includes or --x-libraries.
      # Make a simple check for the libraries at the sysroot, and setup --x-includes and
      # --x-libraries for the sysroot, if that seems to be correct.
      if test "x$SYSROOT" != "x"; then
        if test "x$x_includes" = xNONE; then
          if test -f "$SYSROOT/usr/X11R6/include/X11/Xlib.h"; then
            x_includes="$SYSROOT/usr/X11R6/include"
          elif test -f "$SYSROOT/usr/include/X11/Xlib.h"; then
            x_includes="$SYSROOT/usr/include"
          fi
        fi
        if test "x$x_libraries" = xNONE; then
          if test -f "$SYSROOT/usr/X11R6/lib/libX11.so"; then
            x_libraries="$SYSROOT/usr/X11R6/lib"
          elif test -f "$SYSROOT/usr/lib64/libX11.so" && test "x$OPENJDK_TARGET_CPU_BITS" = x64; then
            x_libraries="$SYSROOT/usr/lib64"
          elif test -f "$SYSROOT/usr/lib/libX11.so"; then
            x_libraries="$SYSROOT/usr/lib"
          elif test -f "$SYSROOT/usr/lib/$OPENJDK_TARGET_CPU-$OPENJDK_TARGET_OS-$OPENJDK_TARGET_ABI/libX11.so"; then
            x_libraries="$SYSROOT/usr/lib/$OPENJDK_TARGET_CPU-$OPENJDK_TARGET_OS-$OPENJDK_TARGET_ABI/libX11.so"
          elif test -f "$SYSROOT/usr/lib/$OPENJDK_TARGET_CPU_AUTOCONF-$OPENJDK_TARGET_OS-$OPENJDK_TARGET_ABI/libX11.so"; then
            x_libraries="$SYSROOT/usr/lib/$OPENJDK_TARGET_CPU_AUTOCONF-$OPENJDK_TARGET_OS-$OPENJDK_TARGET_ABI/libX11.so"
          fi
        fi
      fi
    fi

    # Now let autoconf do it's magic
    AC_PATH_X
    AC_PATH_XTRA

    # AC_PATH_XTRA creates X_LIBS and sometimes adds -R flags. When cross compiling
    # this doesn't make sense so we remove it.
    if test "x$COMPILE_TYPE" = xcross; then
      X_LIBS=`$ECHO $X_LIBS | $SED 's/-R \{0,1\}[[^ ]]*//g'`
    fi

    if test "x$no_x" = xyes; then
      HELP_MSG_MISSING_DEPENDENCY([x11])
      AC_MSG_ERROR([Could not find X11 libraries. $HELP_MSG])
    fi

    AC_LANG_PUSH(C)
    OLD_CFLAGS="$CFLAGS"
    CFLAGS="$CFLAGS $SYSROOT_CFLAGS $X_CFLAGS"

    HEADERS_TO_CHECK="X11/extensions/shape.h X11/extensions/Xrender.h X11/extensions/XTest.h X11/Intrinsic.h"
    # There is no Xrandr extension on AIX
    if test "x$OPENJDK_TARGET_OS" = xaix; then
      X_CFLAGS="$X_CFLAGS -DNO_XRANDR"
    else
      HEADERS_TO_CHECK="$HEADERS_TO_CHECK X11/extensions/Xrandr.h"
    fi

    # Need to include Xlib.h and Xutil.h to avoid "present but cannot be compiled" warnings on Solaris 10
    AC_CHECK_HEADERS([$HEADERS_TO_CHECK],
        [X11_HEADERS_OK=yes],
        [X11_HEADERS_OK=no; break],
        [
          # include <X11/Xlib.h>
          # include <X11/Xutil.h>
        ]
    )

    if test "x$X11_HEADERS_OK" = xno; then
      HELP_MSG_MISSING_DEPENDENCY([x11])
      AC_MSG_ERROR([Could not find all X11 headers (shape.h Xrender.h Xrandr.h XTest.h Intrinsic.h). $HELP_MSG])
    fi

    CFLAGS="$OLD_CFLAGS"
    AC_LANG_POP(C)
  fi # NEEDS_LIB_X11

  AC_SUBST(X_CFLAGS)
  AC_SUBST(X_LIBS)
])
