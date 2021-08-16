#
# Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
# Setup bundled libraries.
#
# For libjpeg, giflib, libpng, lcms2 and zlib the source is present in the
# OpenJDK repository (referred to as "bundled"). Default is to use libjpeg,
# giflib, libpng and lcms2 libraries as "bundled". The default for zlib is
# to use the bundled zlib on Windows and AIX, otherwise the external ("system")
# zlib, if present. However the libs may be replaced by an external ("system")
# version by the user.
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_BUNDLED_LIBS],
[
  LIB_SETUP_LIBJPEG
  LIB_SETUP_GIFLIB
  LIB_SETUP_LIBPNG
  LIB_SETUP_ZLIB
  LIB_SETUP_LCMS
  LIB_SETUP_HARFBUZZ
])

################################################################################
# Setup libjpeg
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_LIBJPEG],
[
  AC_ARG_WITH(libjpeg, [AS_HELP_STRING([--with-libjpeg],
      [use libjpeg from build system or OpenJDK source (system, bundled) @<:@bundled@:>@])])

  AC_MSG_CHECKING([for which libjpeg to use])
  # default is bundled
  DEFAULT_LIBJPEG=bundled
  # if user didn't specify, use DEFAULT_LIBJPEG
  if test "x${with_libjpeg}" = "x"; then
    with_libjpeg=${DEFAULT_LIBJPEG}
  fi
  AC_MSG_RESULT(${with_libjpeg})

  if test "x${with_libjpeg}" = "xbundled"; then
    USE_EXTERNAL_LIBJPEG=false
  elif test "x${with_libjpeg}" = "xsystem"; then
    AC_CHECK_HEADER(jpeglib.h, [],
        [ AC_MSG_ERROR([--with-libjpeg=system specified, but jpeglib.h not found!])])
    AC_CHECK_LIB(jpeg, jpeg_CreateDecompress, [],
        [ AC_MSG_ERROR([--with-libjpeg=system specified, but no libjpeg found])])

    USE_EXTERNAL_LIBJPEG=true
  else
    AC_MSG_ERROR([Invalid use of --with-libjpeg: ${with_libjpeg}, use 'system' or 'bundled'])
  fi

  AC_SUBST(USE_EXTERNAL_LIBJPEG)
])

################################################################################
# Setup giflib
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_GIFLIB],
[
  AC_ARG_WITH(giflib, [AS_HELP_STRING([--with-giflib],
      [use giflib from build system or OpenJDK source (system, bundled) @<:@bundled@:>@])])

  AC_MSG_CHECKING([for which giflib to use])
  # default is bundled
  DEFAULT_GIFLIB=bundled
  # if user didn't specify, use DEFAULT_GIFLIB
  if test "x${with_giflib}" = "x"; then
    with_giflib=${DEFAULT_GIFLIB}
  fi
  AC_MSG_RESULT(${with_giflib})

  if test "x${with_giflib}" = "xbundled"; then
    USE_EXTERNAL_LIBGIF=false
  elif test "x${with_giflib}" = "xsystem"; then
    AC_CHECK_HEADER(gif_lib.h, [],
        [ AC_MSG_ERROR([--with-giflib=system specified, but gif_lib.h not found!])])
    AC_CHECK_LIB(gif, DGifGetCode, [],
        [ AC_MSG_ERROR([--with-giflib=system specified, but no giflib found!])])

    USE_EXTERNAL_LIBGIF=true
  else
    AC_MSG_ERROR([Invalid value of --with-giflib: ${with_giflib}, use 'system' or 'bundled'])
  fi

  AC_SUBST(USE_EXTERNAL_LIBGIF)
])

################################################################################
# Setup libpng
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_LIBPNG],
[
  AC_ARG_WITH(libpng, [AS_HELP_STRING([--with-libpng],
     [use libpng from build system or OpenJDK source (system, bundled) @<:@bundled@:>@])])

  PKG_CHECK_MODULES(PNG, libpng, [LIBPNG_FOUND=yes], [LIBPNG_FOUND=no])
  AC_MSG_CHECKING([for which libpng to use])

  # default is bundled
  DEFAULT_LIBPNG=bundled
  # if user didn't specify, use DEFAULT_LIBPNG
  if test "x${with_libpng}" = "x"; then
    with_libpng=${DEFAULT_LIBPNG}
  fi

  if test "x${with_libpng}" = "xbundled"; then
    USE_EXTERNAL_LIBPNG=false
    PNG_CFLAGS=""
    PNG_LIBS=""
    AC_MSG_RESULT([bundled])
  elif test "x${with_libpng}" = "xsystem"; then
    if test "x${LIBPNG_FOUND}" = "xyes"; then
      # PKG_CHECK_MODULES will set PNG_CFLAGS and PNG_LIBS
      USE_EXTERNAL_LIBPNG=true
      AC_MSG_RESULT([system])
    else
      AC_MSG_RESULT([system not found])
      AC_MSG_ERROR([--with-libpng=system specified, but no libpng found!])
    fi
  else
    AC_MSG_ERROR([Invalid value of --with-libpng: ${with_libpng}, use 'system' or 'bundled'])
  fi

  AC_SUBST(USE_EXTERNAL_LIBPNG)
  AC_SUBST(PNG_CFLAGS)
  AC_SUBST(PNG_LIBS)
])

################################################################################
# Setup zlib
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_ZLIB],
[
  AC_ARG_WITH(zlib, [AS_HELP_STRING([--with-zlib],
      [use zlib from build system or OpenJDK source (system, bundled) @<:@bundled@:>@])])

  AC_CHECK_LIB(z, compress,
      [ ZLIB_FOUND=yes ],
      [ ZLIB_FOUND=no ])

  AC_MSG_CHECKING([for which zlib to use])

  DEFAULT_ZLIB=system
  if test "x$OPENJDK_TARGET_OS" = xwindows -o "x$OPENJDK_TARGET_OS" = xaix; then
    # On windows and aix default is bundled, on others default is system
    DEFAULT_ZLIB=bundled
  fi

  if test "x${ZLIB_FOUND}" != "xyes"; then
    # If we don't find any system...set default to bundled
    DEFAULT_ZLIB=bundled
  fi

  # If user didn't specify, use DEFAULT_ZLIB
  if test "x${with_zlib}" = "x"; then
    with_zlib=${DEFAULT_ZLIB}
  fi

  if test "x${with_zlib}" = "xbundled"; then
    USE_EXTERNAL_LIBZ=false
    AC_MSG_RESULT([bundled])
  elif test "x${with_zlib}" = "xsystem"; then
    if test "x${ZLIB_FOUND}" = "xyes"; then
      USE_EXTERNAL_LIBZ=true
      AC_MSG_RESULT([system])

      if test "x$USE_EXTERNAL_LIBPNG" != "xtrue"; then
        # If we use bundled libpng, we must verify that we have a proper zlib.
        # For instance zlib-ng has had issues with inflateValidate().
        AC_MSG_CHECKING([for system zlib functionality])
        AC_COMPILE_IFELSE(
            [AC_LANG_PROGRAM([#include "zlib.h"], [
                #if ZLIB_VERNUM >= 0x1281
                  inflateValidate(NULL, 0);
                #endif
            ])],
            [AC_MSG_RESULT([ok])],
            [
                AC_MSG_RESULT([not ok])
                AC_MSG_ERROR([System zlib not working correctly])
            ]
        )
      fi
    else
      AC_MSG_RESULT([system not found])
      AC_MSG_ERROR([--with-zlib=system specified, but no zlib found!])
    fi
  else
    AC_MSG_ERROR([Invalid value for --with-zlib: ${with_zlib}, use 'system' or 'bundled'])
  fi

  LIBZ_CFLAGS=""
  LIBZ_LIBS=""
  if test "x$USE_EXTERNAL_LIBZ" = "xfalse"; then
    LIBZ_CFLAGS="$LIBZ_CFLAGS -I$TOPDIR/src/java.base/share/native/libzip/zlib"
  else
    LIBZ_LIBS="-lz"
  fi

  AC_SUBST(USE_EXTERNAL_LIBZ)
  AC_SUBST(LIBZ_CFLAGS)
  AC_SUBST(LIBZ_LIBS)
])

################################################################################
# Setup lcms (Little CMS)
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_LCMS],
[
  AC_ARG_WITH(lcms, [AS_HELP_STRING([--with-lcms],
      [use lcms2 from build system or OpenJDK source (system, bundled) @<:@bundled@:>@])])

  AC_MSG_CHECKING([for which lcms to use])

  DEFAULT_LCMS=bundled
  # If user didn't specify, use DEFAULT_LCMS
  if test "x${with_lcms}" = "x"; then
    with_lcms=${DEFAULT_LCMS}
  fi

  if test "x${with_lcms}" = "xbundled"; then
    USE_EXTERNAL_LCMS=false
    LCMS_CFLAGS=""
    LCMS_LIBS=""
    AC_MSG_RESULT([bundled])
  elif test "x${with_lcms}" = "xsystem"; then
    AC_MSG_RESULT([system])
    PKG_CHECK_MODULES([LCMS], [lcms2], [LCMS_FOUND=yes], [LCMS_FOUND=no])
    if test "x${LCMS_FOUND}" = "xyes"; then
      # PKG_CHECK_MODULES will set LCMS_CFLAGS and LCMS_LIBS
      USE_EXTERNAL_LCMS=true
    else
      AC_MSG_ERROR([--with-lcms=system specified, but no lcms found!])
    fi
  else
    AC_MSG_ERROR([Invalid value for --with-lcms: ${with_lcms}, use 'system' or 'bundled'])
  fi

  AC_SUBST(USE_EXTERNAL_LCMS)
  AC_SUBST(LCMS_CFLAGS)
  AC_SUBST(LCMS_LIBS)
])

################################################################################
# Setup harfbuzz
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_HARFBUZZ],
[
  AC_ARG_WITH(harfbuzz, [AS_HELP_STRING([--with-harfbuzz],
      [use harfbuzz from build system or OpenJDK source (system, bundled) @<:@bundled@:>@])])

  AC_MSG_CHECKING([for which harfbuzz to use])

  DEFAULT_HARFBUZZ=bundled
  # If user didn't specify, use DEFAULT_HARFBUZZ
  if test "x${with_harfbuzz}" = "x"; then
    with_harfbuzz=${DEFAULT_HARFBUZZ}
  fi

  if test "x${with_harfbuzz}" = "xbundled"; then
    USE_EXTERNAL_HARFBUZZ=false
    HARFBUZZ_CFLAGS=""
    HARFBUZZ_LIBS=""
    AC_MSG_RESULT([bundled])
  elif test "x${with_harfbuzz}" = "xsystem"; then
    AC_MSG_RESULT([system])
    PKG_CHECK_MODULES([HARFBUZZ], [harfbuzz], [HARFBUZZ_FOUND=yes], [HARFBUZZ_FOUND=no])
    if test "x${HARFBUZZ_FOUND}" = "xyes"; then
      # PKG_CHECK_MODULES will set HARFBUZZ_CFLAGS and HARFBUZZ_LIBS
      USE_EXTERNAL_HARFBUZZ=true
    else
      HELP_MSG_MISSING_DEPENDENCY([harfbuzz])
      AC_MSG_ERROR([--with-harfbuzz=system specified, but no harfbuzz found! $HELP_MSG])
    fi
  else
    AC_MSG_ERROR([Invalid value for --with-harfbuzz: ${with_harfbuzz}, use 'system' or 'bundled'])
  fi

  AC_SUBST(USE_EXTERNAL_HARFBUZZ)
  AC_SUBST(HARFBUZZ_CFLAGS)
  AC_SUBST(HARFBUZZ_LIBS)
])
