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
# Check if a potential freeype library match is correct and usable
################################################################################
AC_DEFUN([LIB_CHECK_POTENTIAL_FREETYPE],
[
  POTENTIAL_FREETYPE_INCLUDE_PATH="$1"
  POTENTIAL_FREETYPE_LIB_PATH="$2"
  METHOD="$3"

  # Let's start with an optimistic view of the world :-)
  FOUND_FREETYPE=yes

  # First look for the canonical freetype main include file ft2build.h.
  if ! test -s "$POTENTIAL_FREETYPE_INCLUDE_PATH/ft2build.h"; then
    # Oh no! Let's try in the freetype2 directory.
    POTENTIAL_FREETYPE_INCLUDE_PATH="$POTENTIAL_FREETYPE_INCLUDE_PATH/freetype2"
    if ! test -s "$POTENTIAL_FREETYPE_INCLUDE_PATH/ft2build.h"; then
      # Fail.
      FOUND_FREETYPE=no
    fi
  fi

  if test "x$FOUND_FREETYPE" = "xyes"; then
    # Include file found, let's continue the sanity check.
    AC_MSG_NOTICE([Found freetype include files at $POTENTIAL_FREETYPE_INCLUDE_PATH using $METHOD])

    FREETYPE_LIB_NAME="${LIBRARY_PREFIX}${FREETYPE_BASE_NAME}${SHARED_LIBRARY_SUFFIX}"
    if ! test -s "$POTENTIAL_FREETYPE_LIB_PATH/$FREETYPE_LIB_NAME"; then
      AC_MSG_NOTICE([Could not find $POTENTIAL_FREETYPE_LIB_PATH/$FREETYPE_LIB_NAME. Ignoring location.])
      FOUND_FREETYPE=no
    fi
  fi

  if test "x$FOUND_FREETYPE" = "xyes"; then
    FREETYPE_INCLUDE_PATH="$POTENTIAL_FREETYPE_INCLUDE_PATH"
    AC_MSG_CHECKING([for freetype includes])
    AC_MSG_RESULT([$FREETYPE_INCLUDE_PATH])
    FREETYPE_LIB_PATH="$POTENTIAL_FREETYPE_LIB_PATH"
    AC_MSG_CHECKING([for freetype libraries])
    AC_MSG_RESULT([$FREETYPE_LIB_PATH])
  fi
])

################################################################################
# Setup freetype (The FreeType2 font rendering library)
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_FREETYPE],
[
  AC_ARG_WITH(freetype, [AS_HELP_STRING([--with-freetype],
      [specify whether to use 'system' or 'bundled' freetype.
       The selected option applies to both build time and run time.
       The default behaviour can be platform dependent.
       If using 'system' and either the include files or libraries cannot be
       located automatically, then additionally specify both using
       --with-freetype-include and --with-freetype-lib.])])
  AC_ARG_WITH(freetype-include, [AS_HELP_STRING([--with-freetype-include],
      [specify directory for the freetype include files])])
  AC_ARG_WITH(freetype-lib, [AS_HELP_STRING([--with-freetype-lib],
      [specify directory for the freetype library])])

  # This setup is to verify access to system installed freetype header and
  # libraries. On Windows and MacOS this does not apply and using these options
  # will report an error. On other platforms they will default to using the
  # system libraries. If they are found automatically, nothing need be done.
  # If they are not found, the configure "--with-freetype-*" options may be
  # used to fix that. If the preference is to bundle on these platforms then
  # use --with-freetype=bundled.

  FREETYPE_BASE_NAME=freetype
  FREETYPE_CFLAGS=
  FREETYPE_LIBS=

  if (test "x$with_freetype_include" = "x" && test "x$with_freetype_lib" != "x") || \
     (test "x$with_freetype_include" != "x" && test "x$with_freetype_lib" = "x"); then
    AC_MSG_ERROR([Must specify both or neither of --with-freetype-include and --with-freetype-lib])
  fi

  FREETYPE_TO_USE=bundled
  if test "x$OPENJDK_TARGET_OS" != "xwindows" && \
      test "x$OPENJDK_TARGET_OS" != "xmacosx" && \
      test "x$OPENJDK_TARGET_OS" != "xaix"; then
    FREETYPE_TO_USE=system
  fi
  if test "x$with_freetype" != "x" ; then
    if test "x$with_freetype" = "xsystem" ; then
      FREETYPE_TO_USE=system
    elif test "x$with_freetype" = "xbundled" ; then
      FREETYPE_TO_USE=bundled
      if test "x$with_freetype_include" != "x" || \
          test "x$with_freetype_lib" != "x" ; then
        AC_MSG_ERROR(['bundled' cannot be specified with --with-freetype-include and --with-freetype-lib])
      fi
    else
      AC_MSG_ERROR([Valid values for --with-freetype are 'system' and 'bundled'])
    fi
  fi

  if test "x$with_freetype_include" != "x" && \
      test "x$with_freetype_lib" != "x" ; then
    FREETYPE_TO_USE=system
  fi

  if test "x$FREETYPE_TO_USE" = "xsystem" && \
     (test "x$OPENJDK_TARGET_OS" = "xwindows" || \
     test "x$OPENJDK_TARGET_OS" = "xmacosx"); then
    AC_MSG_ERROR([Only bundled freetype can be specified on Mac and Windows])
  fi

  if test "x$with_freetype_include" != "x" ; then
    POTENTIAL_FREETYPE_INCLUDE_PATH="$with_freetype_include"
  fi
  if test "x$with_freetype_lib" != "x" ; then
    POTENTIAL_FREETYPE_LIB_PATH="$with_freetype_lib"
  fi

  if test "x$FREETYPE_TO_USE" = "xsystem" ; then
    if test "x$POTENTIAL_FREETYPE_INCLUDE_PATH" != "x" && \
        test "x$POTENTIAL_FREETYPE_LIB_PATH" != "x" ; then
      # Okay, we got it. Check that it works.
      LIB_CHECK_POTENTIAL_FREETYPE($POTENTIAL_FREETYPE_INCLUDE_PATH,
          $POTENTIAL_FREETYPE_LIB_PATH, [--with-freetype])
      if test "x$FOUND_FREETYPE" != "xyes" ; then
        AC_MSG_ERROR([Can not find or use freetype at location given by --with-freetype-lib|include])
      fi
    else
      # User did not specify a location, but asked for system freetype.
      # Try to locate it.

      # If we have a sysroot, assume that's where we are supposed to look and
      # skip pkg-config.
      if test "x$SYSROOT" = "x" ; then
        if test "x$FOUND_FREETYPE" != "xyes" ; then
          # Check modules using pkg-config, but only if we have it (ugly output
          # results otherwise)
          if test "x$PKG_CONFIG" != "x" ; then
            PKG_CHECK_MODULES(FREETYPE, freetype2, [FOUND_FREETYPE=yes], [FOUND_FREETYPE=no])
            if test "x$FOUND_FREETYPE" = "xyes" ; then
              AC_MSG_CHECKING([for freetype])
              AC_MSG_RESULT([yes (using pkg-config)])
            fi
          fi
        fi
      fi

      if test "x$FOUND_FREETYPE" != "xyes" ; then
        # Check in well-known locations
        FREETYPE_BASE_DIR="$SYSROOT/usr"

        if test "x$OPENJDK_TARGET_CPU_BITS" = "x64" ; then
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib/$OPENJDK_TARGET_CPU-linux-gnu], [well-known location])
          if test "x$FOUND_FREETYPE" != "xyes" ; then
            LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
                [$FREETYPE_BASE_DIR/lib64], [well-known location])
          fi
        else
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib/i386-linux-gnu], [well-known location])
          if test "x$FOUND_FREETYPE" != "xyes" ; then
            LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
                [$FREETYPE_BASE_DIR/lib32], [well-known location])
          fi
        fi

        if test "x$FOUND_FREETYPE" != "xyes" ; then
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib], [well-known location])
        fi

        if test "x$FOUND_FREETYPE" != "xyes" ; then
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib/$OPENJDK_TARGET_CPU-$OPENJDK_TARGET_OS-$OPENJDK_TARGET_ABI], [well-known location])
        fi

        if test "x$FOUND_FREETYPE" != "xyes" ; then
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib/$OPENJDK_TARGET_CPU_AUTOCONF-$OPENJDK_TARGET_OS-$OPENJDK_TARGET_ABI], [well-known location])
        fi

        if test "x$FOUND_FREETYPE" != "xyes" ; then
          FREETYPE_BASE_DIR="$SYSROOT/usr/X11"
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib], [well-known location])
        fi

        if test "x$FOUND_FREETYPE" != "xyes" ; then
          FREETYPE_BASE_DIR="$SYSROOT/usr/local"
          LIB_CHECK_POTENTIAL_FREETYPE([$FREETYPE_BASE_DIR/include],
              [$FREETYPE_BASE_DIR/lib], [well-known location])
        fi
      fi # end check in well-known locations

      if test "x$FOUND_FREETYPE" != "xyes" ; then
        HELP_MSG_MISSING_DEPENDENCY([freetype])
        AC_MSG_ERROR([Could not find freetype! $HELP_MSG ])
      fi
    fi # end user specified settings

    # Set FREETYPE_CFLAGS, _LIBS and _LIB_PATH from include and lib dir.
    if test "x$FREETYPE_CFLAGS" = "x" ; then
      if test -d $FREETYPE_INCLUDE_PATH/freetype2/freetype ; then
        FREETYPE_CFLAGS="-I$FREETYPE_INCLUDE_PATH/freetype2 -I$FREETYPE_INCLUDE_PATH"
      else
        FREETYPE_CFLAGS="-I$FREETYPE_INCLUDE_PATH"
      fi
    fi

    if test "x$FREETYPE_LIBS" = "x" ; then
      FREETYPE_LIBS="-L$FREETYPE_LIB_PATH -l$FREETYPE_BASE_NAME"
    fi
  fi

  AC_MSG_RESULT([Using freetype: $FREETYPE_TO_USE])

  AC_SUBST(FREETYPE_TO_USE)
  AC_SUBST(FREETYPE_CFLAGS)
  AC_SUBST(FREETYPE_LIBS)
])
