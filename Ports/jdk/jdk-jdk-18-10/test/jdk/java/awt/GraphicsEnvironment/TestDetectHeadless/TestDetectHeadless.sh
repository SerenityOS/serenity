#!/bin/sh

#
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
#

#
# Portions Copyright (c) 2014 IBM Corporation
#

# @test
# @bug 8058930 7077826
# @summary java.awt.GraphicsEnvironment.getHeadlessProperty() does not work for AIX
#
# @build TestDetectHeadless
# @run shell TestDetectHeadless.sh

OS=`uname -s`
case "$OS" in
    Windows* | CYGWIN* | Darwin)
        echo "Passed"; exit 0 ;;
    * ) unset DISPLAY ;;
esac

${TESTJAVA}/bin/java ${TESTVMOPTS} \
    -cp ${TESTCLASSES} TestDetectHeadless

if [ $? -ne 0 ]; then
	exit 1;
fi

DISPLAY=
export DISPLAY

${TESTJAVA}/bin/java ${TESTVMOPTS} \
    -cp ${TESTCLASSES} TestDetectHeadless

exit $?
