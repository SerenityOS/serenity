#!/bin/sh

#
# Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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

# @test
# @key printer
# @bug 6662775
# @summary Tests queuePrintJob is sufficient permission for printing. This test 
#          prints a page to a printer. If a document printer is installed, a 
#          popup can appear (to select the file location).
# @run clean PrintSE
# @run build PrintSE
# @run compile PrintSE.java
# @run shell/timeout=300 PrintSE.sh

echo -------------------------------------------------------------
echo Launching test for `basename $0 .sh`
echo -------------------------------------------------------------

createJavaPolicyFile()
{
   cat << EOF > ${TESTCLASSES}/print.policy
grant {
 permission java.lang.RuntimePermission "queuePrintJob";
};
EOF
}

createJavaPolicyFile

${TESTJAVA}/bin/java ${TESTVMOPTS} \
    -Djava.security.manager \
    -Djava.security.policy=${TESTCLASSES}/print.policy \
    -cp ${TESTCLASSES} PrintSE

exit $?
