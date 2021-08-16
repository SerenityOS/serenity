# Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
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

if [ -z "${TESTJAVA}" ]; then
  echo "TESTJAVA undefined: can't continue."
  exit 1
fi

OS=`uname`

case "$OS" in
    Linux* )
        GNOMESID=`pgrep gnome-session`
        DBUS_SESSION_BUS_ADDRESS=`grep -z DBUS_SESSION_BUS_ADDRESS /proc/$GNOMESID/environ | cut -d= -f2-`
        export DBUS_SESSION_BUS_ADDRESS
        DISPLAY=`grep -z DISPLAY /proc/$GNOMESID/environ | cut -d= -f2-`
        export DISPLAY
        ;;
    Sun* )
        GNOMESID=`pgrep gnome-session`
        DBUS_SESSION_BUS_ADDRESS=`pargs -e $GNOMESID | grep DBUS_SESSION_BUS_ADDRESS | cut -d= -f2-`
        export DBUS_SESSION_BUS_ADDRESS
        DISPLAY=`pargs -e $GNOMESID | grep DISPLAY | cut -d= -f2-`
        export DISPLAY
        ;;
    * )
        echo "This Feature is not to be tested on $OS"
        exit 0
        ;;
esac

if [ ${GNOME_DESKTOP_SESSION_ID:-nonset} = "nonset" ];
then
    if [ ${GNOME_SESSION_NAME:-nonset} = "nonset" ];
    then
        echo "This test should run under Gnome"
        exit 0
    fi
fi

SCHEMAS=`gsettings list-schemas | wc -l`

if [ $SCHEMAS -eq 0 ];
then
    TOOL=`which gconftool-2`
    USE_GSETTINGS="false"
else
    TOOL=`which gsettings`
    USE_GSETTINGS="true"
fi

cd ${TESTSRC}
echo $PWD
echo "${TESTJAVA}/bin/javac -d ${TESTCLASSES} rfe4758438.java"
${TESTJAVA}/bin/javac -d ${TESTCLASSES} rfe4758438.java

cd ${TESTCLASSES}
${TESTJAVA}/bin/java -DuseGsettings=${USE_GSETTINGS} -Dtool=${TOOL} ${TESTVMOPTS} rfe4758438

if [ $? -ne 0 ]
then
    echo "Test failed. See the error stream output"
    exit 1
fi
exit 0
