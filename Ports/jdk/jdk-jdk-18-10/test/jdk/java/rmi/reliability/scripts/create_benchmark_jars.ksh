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
# Script to create rmibench.jar and serialbench files that are delivered to
# the Performance Group (http://perfwww.ireland/).

echo "*******************************************************"
echo "CREATING rmibench.jar"
echo "*******************************************************"

rm -rf /tmp/rmibench
rm -f `pwd`/rmibench.jar

mkdir -p /tmp/rmibench
cp -r ../benchmark/bench /tmp/rmibench/
rm -rf /tmp/rmibench/bench/SCCS
rm -rf /tmp/rmibench/bench/rmi/SCCS
rm -rf /tmp/rmibench/bench/serial
rm -rf /tmp/rmibench/bench/rmi/altroot/SCCS

javac \
    /tmp/rmibench/bench/rmi/altroot/*.java \
    /tmp/rmibench/bench/rmi/*.java \
    /tmp/rmibench/bench/*.java

jar cvfm `pwd`/rmibench.jar /tmp/rmibench/bench/rmi/manifest -C /tmp/rmibench .


echo "*******************************************************"
echo "CREATING serialbench.jar"
echo "*******************************************************"

rm -rf /tmp/serialbench
rm -f `pwd`/serialbench.jar

mkdir -p /tmp/serialbench
cp -r ../benchmark/bench /tmp/serialbench/
rm -rf /tmp/serialbench/bench/SCCS
rm -rf /tmp/serialbench/bench/serial/SCCS
rm -rf /tmp/serialbench/bench/rmi

javac \
    /tmp/serialbench/bench/serial/*.java \
    /tmp/serialbench/bench/*.java

jar cvfm `pwd`/serialbench.jar /tmp/serialbench/bench/serial/manifest -C /tmp/serialbench .
