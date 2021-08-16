#
# Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
# @bug      6336608 6367473 6511738
# @summary  Tests OperatingSystemMXBean.getSystemLoadAverage() api.
# @author   Mandy Chung
#
# @run build GetSystemLoadAverage
# @run shell/timeout=300 TestSystemLoadAvg.sh
#

#
# This test tests the system load average on linux and solaris.
# On windows tests if it returns -1.0 The verification is done
# by the GetSystemLoadAverage class.  By default it takes no
# input argument which verifies the system load average with
# /usr/bin/uptime command. Or specify "-1.0" as the input argument
# indicatiing that the platform doesn't support the system load average.

#Set appropriate jdk
#

if [ ! -z "${TESTJAVA}" ] ; then
     jdk="$TESTJAVA"
else
     echo "--Error: TESTJAVA must be defined as the pathname of a jdk to test."
     exit 1
fi

runOne()
{
   echo "$TESTJAVA/bin/java -classpath $TESTCLASSES $@"
   $TESTJAVA/bin/java ${TESTVMOPTS} -classpath $TESTCLASSES $@
}

# Retry 5 times to be more resilent to system load fluctation.
MAX=5
i=1
while true; do
  echo "Run $i: TestSystemLoadAvg"
  case `uname -s` in
       Linux | Darwin | AIX )
         runOne GetSystemLoadAverage
         ;;
      * )
         # On Windows -1.0 should be returned
         runOne GetSystemLoadAverage "-1.0"
         ;;
  esac
  if [ $? -eq 0 ]; then
      # exit if the test passes
      echo "Run $i: TestSystemLoadAvg test passed"
      exit 0
  elif [ $i -eq $MAX ] ; then
      echo "TEST FAILED: TestSystemLoadAvg test failed $i runs"
      exit 1
  fi
  i=`expr $i + 1`
  # sleep for 5 seconds
  sleep 5
done
