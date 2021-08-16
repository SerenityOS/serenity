#
# Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
# Setup fontconfig
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_FONTCONFIG],
[
  AC_ARG_WITH(fontconfig, [AS_HELP_STRING([--with-fontconfig],
      [specify prefix directory for the fontconfig package
      (expecting the headers under PATH/include)])])
  AC_ARG_WITH(fontconfig-include, [AS_HELP_STRING([--with-fontconfig-include],
      [specify directory for the fontconfig include files])])

  if test "x$NEEDS_LIB_FONTCONFIG" = xfalse; then
    if (test "x${with_fontconfig}" != x && test "x${with_fontconfig}" != xno) || \
        (test "x${with_fontconfig_include}" != x && test "x${with_fontconfig_include}" != xno); then
      AC_MSG_WARN([[fontconfig not used, so --with-fontconfig[-*] is ignored]])
    fi
    FONTCONFIG_CFLAGS=
  else
    FONTCONFIG_FOUND=no

    if test "x${with_fontconfig}" = xno || test "x${with_fontconfig_include}" = xno; then
      AC_MSG_ERROR([It is not possible to disable the use of fontconfig. Remove the --without-fontconfig option.])
    fi

    if test "x${with_fontconfig}" != x; then
      AC_MSG_CHECKING([for fontconfig headers])
      if test -s "${with_fontconfig}/include/fontconfig/fontconfig.h"; then
        FONTCONFIG_CFLAGS="-I${with_fontconfig}/include"
        FONTCONFIG_FOUND=yes
        AC_MSG_RESULT([$FONTCONFIG_FOUND])
      else
        AC_MSG_ERROR([Can't find 'include/fontconfig/fontconfig.h' under ${with_fontconfig} given with the --with-fontconfig option.])
      fi
    fi
    if test "x${with_fontconfig_include}" != x; then
      AC_MSG_CHECKING([for fontconfig headers])
      if test -s "${with_fontconfig_include}/fontconfig/fontconfig.h"; then
        FONTCONFIG_CFLAGS="-I${with_fontconfig_include}"
        FONTCONFIG_FOUND=yes
        AC_MSG_RESULT([$FONTCONFIG_FOUND])
      else
        AC_MSG_ERROR([Can't find 'fontconfig/fontconfig.h' under ${with_fontconfig_include} given with the --with-fontconfig-include option.])
      fi
    fi
    if test "x$FONTCONFIG_FOUND" = xno; then
      # Are the fontconfig headers installed in the default /usr/include location?
      AC_CHECK_HEADERS([fontconfig/fontconfig.h], [
          FONTCONFIG_FOUND=yes
          FONTCONFIG_CFLAGS=
          DEFAULT_FONTCONFIG=yes
      ])
    fi
    if test "x$FONTCONFIG_FOUND" = xno; then
      HELP_MSG_MISSING_DEPENDENCY([fontconfig])
      AC_MSG_ERROR([Could not find fontconfig! $HELP_MSG ])
    fi
  fi

  AC_SUBST(FONTCONFIG_CFLAGS)
])
