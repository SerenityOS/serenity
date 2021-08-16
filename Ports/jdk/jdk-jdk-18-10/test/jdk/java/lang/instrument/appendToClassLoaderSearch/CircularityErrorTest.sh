#
# Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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
# @bug 6173575
# @summary Unit tests for appendToBootstrapClassLoaderSearch and
#   appendToSystemClasLoaderSearch methods.
#
# @run shell/timeout=240 CircularityErrorTest.sh

if [ "${TESTSRC}" = "" ]
then
  echo "TESTSRC not set.  Test cannot execute.  Failed."
  exit 1
fi

if [ "${COMPILEJAVA}" = "" ]
then
  COMPILEJAVA="${TESTJAVA}"
fi

. ${TESTSRC}/CommonSetup.sh

# Setup to create circularity condition

# B extends A
# This yields us A.jar (containing A.class) and B.keep (class file)
rm -f "${TESTCLASSES}"/A.java "${TESTCLASSES}"/B.java
cp "${TESTSRC}"/A.1 "${TESTCLASSES}"/A.java
cp "${TESTSRC}"/B.1 "${TESTCLASSES}"/B.java
(cd "${TESTCLASSES}"; \
    $JAVAC ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} A.java B.java; \
    $JAVAC ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} -d . "${TESTSRC}"/CircularityErrorTest.java; \
    $JAR ${TESTTOOLVMOPTS} cf A.jar A.class; \
    rm -f A.class; mv B.class B.keep)

# A extends B
# This yields us A.class
rm -f "${TESTCLASSES}"/A.java "${TESTCLASSES}"/B.java
cp "${TESTSRC}"/A.2 "${TESTCLASSES}"/A.java
cp "${TESTSRC}"/B.2 "${TESTCLASSES}"/B.java
(cd "${TESTCLASSES}"; \
     $JAVAC ${TESTJAVACOPTS} ${TESTTOOLVMOPTS} A.java B.java; rm -f B.class A.java B.java)

# Move B.keep to B.class creates the A extends B and
# B extends A condition.
(cd "${TESTCLASSES}"; mv B.keep B.class)

# Create the manifest
MANIFEST="${TESTCLASSES}"/agent.mf
rm -f "${MANIFEST}"
echo "Premain-Class: CircularityErrorTest" > "${MANIFEST}"

# Setup test case as an agent
$JAR ${TESTTOOLVMOPTS} -cfm "${TESTCLASSES}"/CircularityErrorTest.jar "${MANIFEST}" \
  -C "${TESTCLASSES}" CircularityErrorTest.class

# Finally we run the test
(cd "${TESTCLASSES}";
  $JAVA ${TESTVMOPTS} -javaagent:CircularityErrorTest.jar CircularityErrorTest)
