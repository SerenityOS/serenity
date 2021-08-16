#
# Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
# Setup alsa (Advanced Linux Sound Architecture)
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_ALSA],
[
  AC_ARG_WITH(alsa, [AS_HELP_STRING([--with-alsa],
      [specify prefix directory for the alsa package
      (expecting the libraries under PATH/lib and the headers under PATH/include)])])
  AC_ARG_WITH(alsa-include, [AS_HELP_STRING([--with-alsa-include],
      [specify directory for the alsa include files])])
  AC_ARG_WITH(alsa-lib, [AS_HELP_STRING([--with-alsa-lib],
      [specify directory for the alsa library])])

  if test "x$NEEDS_LIB_ALSA" = xfalse; then
    if (test "x${with_alsa}" != x && test "x${with_alsa}" != xno) || \
        (test "x${with_alsa_include}" != x && test "x${with_alsa_include}" != xno) || \
        (test "x${with_alsa_lib}" != x && test "x${with_alsa_lib}" != xno); then
      AC_MSG_WARN([[alsa not used, so --with-alsa[-*] is ignored]])
    fi
    ALSA_CFLAGS=
    ALSA_LIBS=
  else
    ALSA_FOUND=no

    if test "x${with_alsa}" = xno || test "x${with_alsa_include}" = xno || test "x${with_alsa_lib}" = xno; then
      AC_MSG_ERROR([It is not possible to disable the use of alsa. Remove the --without-alsa option.])
    fi

    if test "x${with_alsa}" != x; then
      ALSA_LIBS="-L${with_alsa}/lib -lasound"
      ALSA_CFLAGS="-I${with_alsa}/include"
      ALSA_FOUND=yes
    fi
    if test "x${with_alsa_include}" != x; then
      ALSA_CFLAGS="-I${with_alsa_include}"
      ALSA_FOUND=yes
    fi
    if test "x${with_alsa_lib}" != x; then
      ALSA_LIBS="-L${with_alsa_lib} -lasound"
      ALSA_FOUND=yes
    fi
    # Do not try pkg-config if we have a sysroot set.
    if test "x$SYSROOT" = x; then
      if test "x$ALSA_FOUND" = xno; then
        PKG_CHECK_MODULES(ALSA, alsa, [ALSA_FOUND=yes], [ALSA_FOUND=no])
      fi
    fi
    if test "x$ALSA_FOUND" = xno; then
      AC_CHECK_HEADERS([alsa/asoundlib.h],
          [
            ALSA_FOUND=yes
            ALSA_CFLAGS=-Iignoreme
            ALSA_LIBS=-lasound
            DEFAULT_ALSA=yes
          ],
          [ALSA_FOUND=no]
      )
    fi
    if test "x$ALSA_FOUND" = xno; then
      HELP_MSG_MISSING_DEPENDENCY([alsa])
      AC_MSG_ERROR([Could not find alsa! $HELP_MSG])
    fi
  fi

  AC_SUBST(ALSA_CFLAGS)
  AC_SUBST(ALSA_LIBS)
])
