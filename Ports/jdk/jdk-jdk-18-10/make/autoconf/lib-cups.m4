#
# Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
# Setup cups (Common Unix Printing System)
################################################################################
AC_DEFUN_ONCE([LIB_SETUP_CUPS],
[
  AC_ARG_WITH(cups, [AS_HELP_STRING([--with-cups],
      [specify prefix directory for the cups package
      (expecting the headers under PATH/include)])])
  AC_ARG_WITH(cups-include, [AS_HELP_STRING([--with-cups-include],
      [specify directory for the cups include files])])

  if test "x$NEEDS_LIB_CUPS" = xfalse; then
    if (test "x${with_cups}" != x && test "x${with_cups}" != xno) || \
        (test "x${with_cups_include}" != x && test "x${with_cups_include}" != xno); then
      AC_MSG_WARN([[cups not used, so --with-cups[-*] is ignored]])
    fi
    CUPS_CFLAGS=
  else
    CUPS_FOUND=no

    if test "x${with_cups}" = xno || test "x${with_cups_include}" = xno; then
      AC_MSG_ERROR([It is not possible to disable the use of cups. Remove the --without-cups option.])
    fi

    if test "x${with_cups}" != x; then
      AC_MSG_CHECKING([for cups headers])
      if test -s "${with_cups}/include/cups/cups.h"; then
        CUPS_CFLAGS="-I${with_cups}/include"
        CUPS_FOUND=yes
        AC_MSG_RESULT([$CUPS_FOUND])
      else
        AC_MSG_ERROR([Can't find 'include/cups/cups.h' under ${with_cups} given with the --with-cups option.])
      fi
    fi
    if test "x${with_cups_include}" != x; then
      AC_MSG_CHECKING([for cups headers])
      if test -s "${with_cups_include}/cups/cups.h"; then
        CUPS_CFLAGS="-I${with_cups_include}"
        CUPS_FOUND=yes
        AC_MSG_RESULT([$CUPS_FOUND])
      else
        AC_MSG_ERROR([Can't find 'cups/cups.h' under ${with_cups_include} given with the --with-cups-include option.])
      fi
    fi
    if test "x$CUPS_FOUND" = xno; then
      # Are the cups headers installed in the default /usr/include location?
      AC_CHECK_HEADERS([cups/cups.h cups/ppd.h], [
          CUPS_FOUND=yes
          CUPS_CFLAGS=
          DEFAULT_CUPS=yes
      ])
    fi
    if test "x$CUPS_FOUND" = xno; then
      HELP_MSG_MISSING_DEPENDENCY([cups])
      AC_MSG_ERROR([Could not find cups! $HELP_MSG ])
    fi
  fi

  AC_SUBST(CUPS_CFLAGS)
])
