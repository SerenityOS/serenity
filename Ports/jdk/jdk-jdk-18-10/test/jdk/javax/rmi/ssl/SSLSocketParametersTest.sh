#
# Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
# @bug 5016500
# @summary Test SslRmi[Client|Server]SocketFactory SSL socket parameters.
# @author Luis-Miguel Alventosa
# @run clean SSLSocketParametersTest
# @run build SSLSocketParametersTest
# @run shell/timeout=300 SSLSocketParametersTest.sh

echo -------------------------------------------------------------
echo Launching test for `basename $0 .sh`
echo -------------------------------------------------------------

# case 1: /* default constructor - default config */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 1 || exit $?

# case 2: /* non-default constructor - default config */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 2 || exit $?

# case 3: /* needClientAuth=true */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 3 || exit $?

# case 4: /* server side dummy_ciphersuite */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 4 || exit $?

# case 5: /* server side dummy_protocol */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 5 || exit $?

# case 6: /* client side dummy_ciphersuite */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 6 || exit $?

# case 7: /* client side dummy_protocol */
${TESTJAVA}/bin/java ${TESTVMOPTS} -classpath ${TESTCLASSES} -Dtest.src=${TESTSRC} SSLSocketParametersTest 7 || exit $?
