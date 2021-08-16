#
# Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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
# @bug     4982128
# @summary Test low memory detection of non-heap memory pool
#
# @requires vm.gc=="null"
#
# @run build LowMemoryTest2 MemoryUtil
# @run shell/timeout=600 LowMemoryTest2.sh
#

if [ ! -z "${TESTJAVA}" ] ; then
     JAVA=${TESTJAVA}/bin/java
     CLASSPATH=${TESTCLASSES}
     export CLASSPATH
else
     echo "--Error: TESTJAVA must be defined as the pathname of a jdk to test."
     exit 1
fi

# Test execution

failures=0

go() {
    echo ''
    sh -xc "$JAVA $TESTVMOPTS $*" 2>&1
    if [ $? != 0 ]; then failures=`expr $failures + 1`; fi
}

# Run test with each GC configuration
#
# Notes: To ensure that metaspace fills up we disable class unloading.
# Also we set the max metaspace to 16MB/32MB - otherwise the test takes too
# long to run. The 32MB setting is required for running with CDS archive.

go -noclassgc -XX:MaxMetaspaceSize=32m -XX:+UseSerialGC LowMemoryTest2
go -noclassgc -XX:MaxMetaspaceSize=32m -XX:+UseParallelGC LowMemoryTest2

# Test class metaspace - might hit MaxMetaspaceSize instead if
# UseCompressedClassPointers is off or if 32 bit.
#
# (note: This is very shaky and that shakiness exposes a problem with MemoryMXBean:
#
#  MemoryMXBean defines "used" "committed" and "max" (see java/lang/management/MemoryUsage.java)
#  This abstraction misses a definition for "address space exhausted" which with the new Metaspace (jep387)
#  can happen before committed/used hits any trigger. We now commit only on demand and therefore class loaders
#  can sit atop of uncommitted address space, denying new loaders address space. In the old Metaspace,
#  we would have committed the space right away and therefore the MemoryMXBean "committed" trigger
#  would have fired. In the new Metaspace, we don't commit, so the MemoryMXBean does not fire.
go -noclassgc -XX:MaxMetaspaceSize=16m -XX:CompressedClassSpaceSize=4m LowMemoryTest2

echo ''
if [ $failures -gt 0 ];
  then echo "$failures test(s) failed";
  else echo "All test(s) passed"; fi
exit $failures
