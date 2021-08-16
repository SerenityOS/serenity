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

AC_DEFUN_ONCE([SRCDIRS_SETUP_DIRS],
[
  OUTPUTDIR="$OUTPUTDIR"
  AC_SUBST(OUTPUTDIR)
  JDK_OUTPUTDIR="$OUTPUTDIR/jdk"
])

################################################################################
# Define a mechanism for importing extra prebuilt modules
#

AC_DEFUN_ONCE([SRCDIRS_SETUP_IMPORT_MODULES],
[
  AC_ARG_WITH(import-modules, [AS_HELP_STRING([--with-import-modules],
      [import a set of prebuilt modules either as a zip file or an exploded directory])])

  if test "x$with_import_modules" != x \
      && test "x$with_import_modules" != "xno"; then
    if test -d "$with_import_modules"; then
      IMPORT_MODULES_TOPDIR="$with_import_modules"
      UTIL_FIXUP_PATH([IMPORT_MODULES_TOPDIR])
    elif test -e "$with_import_modules"; then
      IMPORT_MODULES_TOPDIR="$CONFIGURESUPPORT_OUTPUTDIR/import-modules"
      $RM -rf "$IMPORT_MODULES_TOPDIR"
      $MKDIR -p "$IMPORT_MODULES_TOPDIR"
      if ! $UNZIP -q "$with_import_modules" -d "$IMPORT_MODULES_TOPDIR"; then
        AC_MSG_ERROR([--with-import-modules="$with_import_modules" must point to a dir or a zip file])
      fi
    else
      AC_MSG_ERROR([--with-import-modules="$with_import_modules" must point to a dir or a zip file])
    fi
  fi

  if test -d "$IMPORT_MODULES_TOPDIR/modules"; then
    IMPORT_MODULES_CLASSES="$IMPORT_MODULES_TOPDIR/modules"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/modules_cmds"; then
    IMPORT_MODULES_CMDS="$IMPORT_MODULES_TOPDIR/modules_cmds"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/modules_libs"; then
    IMPORT_MODULES_LIBS="$IMPORT_MODULES_TOPDIR/modules_libs"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/modules_conf"; then
    IMPORT_MODULES_CONF="$IMPORT_MODULES_TOPDIR/modules_conf"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/modules_legal"; then
    IMPORT_MODULES_LEGAL="$IMPORT_MODULES_TOPDIR/modules_legal"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/modules_man"; then
    IMPORT_MODULES_MAN="$IMPORT_MODULES_TOPDIR/modules_man"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/modules_src"; then
    IMPORT_MODULES_SRC="$IMPORT_MODULES_TOPDIR/modules_src"
  fi
  if test -d "$IMPORT_MODULES_TOPDIR/make"; then
    IMPORT_MODULES_MAKE="$IMPORT_MODULES_TOPDIR/make"
  fi

  AC_SUBST(IMPORT_MODULES_CLASSES)
  AC_SUBST(IMPORT_MODULES_CMDS)
  AC_SUBST(IMPORT_MODULES_LIBS)
  AC_SUBST(IMPORT_MODULES_CONF)
  AC_SUBST(IMPORT_MODULES_LEGAL)
  AC_SUBST(IMPORT_MODULES_MAN)
  AC_SUBST(IMPORT_MODULES_SRC)
  AC_SUBST(IMPORT_MODULES_MAKE)
])
