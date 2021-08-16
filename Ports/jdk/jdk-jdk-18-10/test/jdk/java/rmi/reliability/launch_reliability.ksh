#
# Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
# Script to build and launch the RMI reliability suite.
# This script is used to run the reliability test for a 
# certain number of hours.  This script is NOT used when 
# running the juicer and benchmark tests as part of the 
# jtreg regression suite.

JAVA_HOME=$1
WORK_DIR=$2
RES_DIR=$3
SHELLTOUSE=$4
SUITE_DIR=$5
NHOURS=$6
shift 6
VMOPTS=$*

###You need not export these variables if your reliability run is from this shell itself######
###If you are launching another shell then you need to export these variables.#######

if [ "${WORK_DIR}" = "" ] ; then
	WORK_DIR=`pwd`
fi
if [ "${RES_DIR}" = "" ] ; then
	RES_DIR=`pwd`/results
fi
if [ "${SHELLTOUSE}" = "" ] ; then
	SHELLTOUSE=ksh
fi
if [ "${JAVA_HOME}" = "" ] ; then
	WHENCEJAVA=`whence java`
	JAVABIN=`dirname ${WHENCEJAVA}`
	JAVA_HOME=`dirname ${JAVABIN}`
fi
if [ "${SUITE_DIR}" = "" ] ; then
	SUITE_DIR=`pwd`
fi
if [ "${VMOPTS}" = "" ] ; then
	VMOPTS=-server -showversion
fi
if [ "${NHOURS}" = "" ] ; then
	NHOURS=1
fi

export JAVA_HOME
export WORK_DIR
export RES_DIR
export SHELLTOUSE
export SUITE_DIR
export NHOURS
export VMOPTS

echo "######### launch_reliability script ##########"
echo "JAVA_HOME : $JAVA_HOME "
echo "WORK_DIR : $WORK_DIR "
echo "RES_DIR : $RES_DIR "
echo "SHELLTOUSE : $SHELLTOUSE "
echo "SUITE_DIR : $SUITE_DIR "
echo "NHOURS : $NHOURS "
echo "VMOPTS : $VMOPTS "


# set platform-dependent variables
if [ `uname` = "Linux" ] ; then
        PATH_SEP=":"
else
        PATH_SEP=";"
fi

export PATH_SEP
mainpid=$$

mkdir -p ${RES_DIR}

rm -rf ${WORK_DIR}/rmibench_scratch
rm -rf ${WORK_DIR}/serialbench_scratch
rm -rf ${WORK_DIR}/juicer_scratch
mkdir -p ${WORK_DIR}/rmibench_scratch
mkdir -p ${WORK_DIR}/serialbench_scratch
mkdir -p ${WORK_DIR}/juicer_scratch

echo ""
echo "     Starting RMI bench test "
$SHELLTOUSE ${SUITE_DIR}/scripts/run_rmibench.ksh ${WORK_DIR}/rmibench_scratch $RES_DIR $JAVA_HOME $SUITE_DIR $NHOURS $VMOPTS &
pid1=$!

sleep 30
echo ""
echo "     Starting Serialization bench test "
$SHELLTOUSE ${SUITE_DIR}/scripts/run_serialbench.ksh ${WORK_DIR}/serialbench_scratch $RES_DIR $JAVA_HOME $SUITE_DIR $NHOURS $VMOPTS &
pid2=$!

sleep 30
echo ""
echo "     Starting RMI juicer test "
$SHELLTOUSE ${SUITE_DIR}/scripts/run_juicer.ksh ${WORK_DIR}/juicer_scratch $RES_DIR $JAVA_HOME $SUITE_DIR $NHOURS $VMOPTS &
pid3=$!

sleep 30
echo ""
echo "     Waiting for jobs to complete"

wait $pid1 $pid2 $pid3

echo ""
echo "     Done RMI reliability testing "

rm -rf ${WORK_DIR}/rmibench_scratch
rm -rf ${WORK_DIR}/serialbench_scratch
rm -rf ${WORK_DIR}/juicer_scratch

kill -9 $mainpid 

