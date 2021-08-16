#!/bin/ksh -p

#
# Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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


#   @test
#   @summary  Try to force GTK3. We must bail out to GTK2 if no 3 available.
#
#   @key headful
#   @bug 8156128 8212903
#   @compile ProvokeGTK.java
#   @requires os.family == "linux"
#   @run shell/timeout=400 DemandGTK3.sh

#
# Note that we depend on
# strace in the PATH
# /sbin/ldconfig (which may be is not in PATH)
# It is true for OEL 7 and Ubuntu 14, 16
# but may fail in future. Save tomorrow for tomorrow.
#

which strace
if [ $?  -ne 0 ]
then
    echo "Please provide strace: \"which strace\" failed."
    exit 1
fi

HAVE_3=`/sbin/ldconfig -v 2>/dev/null | grep libgtk-3.so | wc -l`


if [ "${HAVE_3}" = "0" ]
then
    
    echo "No GTK 3 library found: we should bail out to 2"
    strace -o strace.log -fe open,openat ${TESTJAVA}/bin/java  -cp ${TESTCLASSPATH}  -Djdk.gtk.version=3 ProvokeGTK
    EXECRES=$?
    grep  'libgtk-x11.*=\ *[0-9]*$' strace.log > logg
else
    echo "There is GTK 3 library: we should use it"
    strace -o strace.log -fe open,openat ${TESTJAVA}/bin/java  -cp ${TESTCLASSPATH}  -Djdk.gtk.version=3 ProvokeGTK
    EXECRES=$?
    grep  'libgtk-3.*=\ *[0-9]*$' strace.log > logg
fi

if [ ${EXECRES}  -ne 0 ]
then
    echo "java execution failed for unknown reason, see logs"
    exit 2
fi

cat logg
if [  -s logg ]
then
    echo "Success."
    exit 0
else
    echo "Failed. Examine logs."
    exit 3
fi

