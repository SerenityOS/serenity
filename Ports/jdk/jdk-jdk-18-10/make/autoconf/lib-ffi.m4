#
# Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
# Setup libffi (Foreign Function Interface)
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_LIBFFI],
[
  UTIL_ARG_ENABLE(NAME: libffi-bundling, DEFAULT: false,
      RESULT: ENABLE_LIBFFI_BUNDLING,
      DESC: [enable bundling of libffi.so to make the built JDK runnable on more systems])

  AC_ARG_WITH(libffi, [AS_HELP_STRING([--with-libffi],
      [specify prefix directory for the libffi package
      (expecting the libraries under PATH/lib and the headers under PATH/include)])])
  AC_ARG_WITH(libffi-include, [AS_HELP_STRING([--with-libffi-include],
      [specify directory for the libffi include files])])
  AC_ARG_WITH(libffi-lib, [AS_HELP_STRING([--with-libffi-lib],
      [specify directory for the libffi library])])

  if test "x$NEEDS_LIB_FFI" = xfalse; then
    if (test "x${with_libffi}" != x && test "x${with_libffi}" != xno) || \
        (test "x${with_libffi_include}" != x && test "x${with_libffi_include}" != xno) || \
        (test "x${with_libffi_lib}" != x && test "x${with_libffi_lib}" != xno); then
      AC_MSG_WARN([[libffi not used, so --with-libffi[-*] is ignored]])
    fi
    LIBFFI_CFLAGS=
    LIBFFI_LIBS=
  else
    LIBFFI_FOUND=no

    if test "x${with_libffi}" = xno || test "x${with_libffi_include}" = xno || test "x${with_libffi_lib}" = xno; then
      AC_MSG_ERROR([It is not possible to disable the use of libffi. Remove the --without-libffi option.])
    fi

    if test "x${with_libffi}" != x; then
      LIBFFI_LIB_PATH="${with_libffi}/lib"
      LIBFFI_LIBS="-L${with_libffi}/lib -lffi"
      LIBFFI_CFLAGS="-I${with_libffi}/include"
      LIBFFI_FOUND=yes
    fi
    if test "x${with_libffi_include}" != x; then
      LIBFFI_CFLAGS="-I${with_libffi_include}"
      LIBFFI_FOUND=yes
    fi
    if test "x${with_libffi_lib}" != x; then
      LIBFFI_LIB_PATH="${with_libffi_lib}"
      LIBFFI_LIBS="-L${with_libffi_lib} -lffi"
      LIBFFI_FOUND=yes
    fi
    # Do not try pkg-config if we have a sysroot set.
    if test "x$SYSROOT" = x; then
      if test "x$LIBFFI_FOUND" = xno; then
        # Figure out LIBFFI_CFLAGS and LIBFFI_LIBS
        PKG_CHECK_MODULES([LIBFFI], [libffi], [LIBFFI_FOUND=yes], [LIBFFI_FOUND=no])
      fi
    fi
    if test "x$LIBFFI_FOUND" = xno; then
      AC_CHECK_HEADERS([ffi.h],
          [
            LIBFFI_FOUND=yes
            LIBFFI_CFLAGS=
            LIBFFI_LIBS=-lffi
          ],
          [LIBFFI_FOUND=no]
      )
    fi
    # on macos we need a special case for system's libffi as
    # headers are located only in sdk in $SYSROOT and in ffi subfolder
    if test "x$LIBFFI_FOUND" = xno; then
      if test "x$SYSROOT" != "x"; then
        AC_CHECK_HEADER([$SYSROOT/usr/include/ffi/ffi.h],
            [
              LIBFFI_FOUND=yes
              LIBFFI_CFLAGS="-I${SYSROOT}/usr/include/ffi"
              LIBFFI_LIBS=-lffi
            ],
            [LIBFFI_FOUND=no]
        )
      fi
    fi
    if test "x$LIBFFI_FOUND" = xno; then
      HELP_MSG_MISSING_DEPENDENCY([ffi])
      AC_MSG_ERROR([Could not find libffi! $HELP_MSG])
    fi

    AC_MSG_CHECKING([if libffi works])
    AC_LANG_PUSH(C)
    OLD_CFLAGS="$CFLAGS"
    CFLAGS="$CFLAGS $LIBFFI_CFLAGS"
    OLD_LIBS="$LIBS"
    LIBS="$LIBS $LIBFFI_LIBS"
    AC_LINK_IFELSE([AC_LANG_PROGRAM([#include <ffi.h>],
        [
          ffi_call(NULL, NULL, NULL, NULL);
          return 0;
        ])],
        [LIBFFI_WORKS=yes],
        [LIBFFI_WORKS=no]
    )
    CFLAGS="$OLD_CFLAGS"
    LIBS="$OLD_LIBS"
    AC_LANG_POP(C)
    AC_MSG_RESULT([$LIBFFI_WORKS])

    if test "x$LIBFFI_WORKS" = xno; then
      HELP_MSG_MISSING_DEPENDENCY([ffi])
      AC_MSG_ERROR([Found libffi but could not link and compile with it. $HELP_MSG])
    fi

    # Find the libffi.so.X to bundle
    if test "x${ENABLE_LIBFFI_BUNDLING}" = "xtrue"; then
      AC_MSG_CHECKING([for libffi lib file location])
      if test "x${LIBFFI_LIB_PATH}" != x; then
        if test -e ${LIBFFI_LIB_PATH}/libffi.so.?; then
          LIBFFI_LIB_FILE="${LIBFFI_LIB_PATH}/libffi.so.?"
        else
          AC_MSG_ERROR([Could not locate libffi.so.? for bundling in ${LIBFFI_LIB_PATH}])
        fi
      else
        # If we don't have an explicit path, look in a few obvious places
        if test "x${OPENJDK_TARGET_CPU}" = "xx86"; then
          if test -e ${SYSROOT}/usr/lib/libffi.so.? ; then
            LIBFFI_LIB_FILE="${SYSROOT}/usr/lib/libffi.so.?"
          elif test -e ${SYSROOT}/usr/lib/i386-linux-gnu/libffi.so.? ; then
            LIBFFI_LIB_FILE="${SYSROOT}/usr/lib/i386-linux-gnu/libffi.so.?"
          else
            AC_MSG_ERROR([Could not locate libffi.so.? for bundling])
          fi
        elif test "x${OPENJDK_TARGET_CPU}" = "xx86_64"; then
          if test -e ${SYSROOT}/usr/lib64/libffi.so.? ; then
            LIBFFI_LIB_FILE="${SYSROOT}/usr/lib64/libffi.so.?"
          elif test -e ${SYSROOT}/usr/lib/x86_64-linux-gnu/libffi.so.? ; then
            LIBFFI_LIB_FILE="${SYSROOT}/usr/lib/x86_64-linux-gnu/libffi.so.?"
          else
            AC_MSG_ERROR([Could not locate libffi.so.? for bundling])
          fi
        else
          # Fallback on the default /usr/lib dir
          if test -e ${SYSROOT}/usr/lib/libffi.so.? ; then
            LIBFFI_LIB_FILE="${SYSROOT}/usr/lib/libffi.so.?"
          else
            AC_MSG_ERROR([Could not locate libffi.so.? for bundling])
          fi
        fi
      fi
      # Make sure the wildcard is evaluated
      LIBFFI_LIB_FILE="$(ls ${LIBFFI_LIB_FILE})"
      AC_MSG_RESULT([${LIBFFI_LIB_FILE}])
    fi
  fi

  AC_SUBST(LIBFFI_CFLAGS)
  AC_SUBST(LIBFFI_LIBS)
  AC_SUBST(ENABLE_LIBFFI_BUNDLING)
  AC_SUBST(LIBFFI_LIB_FILE)
])
