#
# Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
# @test
# @summary Test RMI Bootstrap with SSL and no keystore.
# @bug 4932854
#
# @build TestLogger RmiSslNoKeyStoreTest
# @run shell/timeout=300  RmiSslNoKeyStoreTest.sh

# Define the Java class test name
TESTCLASS="RmiSslNoKeyStoreTest"
export TESTCLASS

# Source in utility shell script to generate and remove .properties and .acl files
. ${TESTSRC}/GeneratePropertyPassword.sh

generatePropertyPasswordFiles `ls ${TESTSRC}/*_ssltest*.in`

rm -rf ${TESTCLASSES}/ssl
mkdir -p ${TESTCLASSES}/ssl
cp -rf ${TESTSRC}/ssl/*store ${TESTCLASSES}/ssl
chmod -R 777 ${TESTCLASSES}/ssl

DEBUGOPTIONS=""
export DEBUGOPTIONS

EXTRAOPTIONS="--add-exports jdk.management.agent/jdk.internal.agent=ALL-UNNAMED \
 --add-exports jdk.management.agent/sun.management.jmxremote=ALL-UNNAMED"
export EXTRAOPTIONS

# Call the common generic test
#
echo -------------------------------------------------------------
echo Launching test for `basename $0 .sh`
echo -------------------------------------------------------------
sh ${TESTSRC}/../RunTest.sh ${DEBUGOPTIONS} ${EXTRAOPTIONS} ${TESTCLASS} \
    ${TESTCLASSES}/management_ssltest*.properties
result=$?
restoreFilePermissions `ls ${TESTSRC}/*_ssltest*.in`
exit $result
