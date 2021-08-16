#
# Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @run shell jstatGcCauseOutput1.sh
# @summary Test that output of 'jstat -gccause 0' has expected line counts

. ${TESTSRC-.}/../../jvmstat/testlibrary/utils.sh

setup
verify_os

JSTAT="${TESTJAVA}/bin/jstat"

# Explicitly use serial gc because if this tests runs on a server
# class machine, ergonomics will automatically use UseParallelGC.
# The UseParallelGC collector does not currently update the gc cause counters.

${JSTAT} ${COMMON_JSTAT_FLAGS} -gccause 0 2>&1 | awk -f ${TESTSRC}/gcCauseOutput1.awk
${JSTAT} ${COMMON_JSTAT_FLAGS} -J-XX:+UseSerialGC -gccause 0 2>&1 | awk -f ${TESTSRC}/gcCauseOutput1.awk
