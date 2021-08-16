#
# Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

#!/bin/ksh
#
# Script to build and launch the Serialization bench test as part of
# the RMI reliability test.  This script is used to run 
# the bench test for a certain number of hours.  This 
# script is NOT used when running the bench test as part 
# of the jtreg regression suite.

WORK_DIR=$1
RES_DIR=$2
JAVA_HOME=$3
SUITE_DIR=$4
NHOURS=$5
shift 5
VMOPTS=$*

echo "          ######### run_serialbench script ##########"
echo "          WORK_DIR : $WORK_DIR "
echo "          RES_DIR : $RES_DIR "
echo "          JAVA_HOME : $JAVA_HOME "
echo "          SUITE_DIR : $SUITE_DIR "
echo "          NHOURS : $NHOURS "
echo "          VMOPTS : $VMOPTS "

${JAVA_HOME}/bin/javac \
    -d $WORK_DIR \
    ${SUITE_DIR}/benchmark/bench/serial/*.java \
    ${SUITE_DIR}/benchmark/bench/*.java

echo "          Starting serialization benchmark "

${JAVA_HOME}/bin/java \
    $VMOPTS \
    -cp $WORK_DIR \
    bench.serial.Main \
    -t $NHOURS \
    -v \
    -c ${SUITE_DIR}/benchmark/bench/serial/config \
    > ${RES_DIR}/log.serialbench 2>&1 &

RETVAL=$?

echo "     Serialization benchmark test finished with exit value ${RETVAL}"

exit ${RETVAL}

