#
# Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
# @bug 4990825
# @run shell jstatOptions1.sh
# @summary Test that output of 'jstat -options matches the usage.out file

. ${TESTSRC-.}/../../jvmstat/testlibrary/utils.sh

setup

JSTAT="${TESTJAVA}/bin/jstat"

rm -f jstat.out1 jstat.out2 2>/dev/null
${JSTAT} ${COMMON_JSTAT_FLAGS} -options > jstat.out1 2>&1
${JSTAT} ${COMMON_JSTAT_FLAGS} -options -J-Djstat.showUnsupported=true > jstat.out2 2>&1

diff -w jstat.out1 ${TESTSRC}/options1.out
diff -w jstat.out2 ${TESTSRC}/options2.out
