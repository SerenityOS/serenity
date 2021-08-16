#!/bin/bash
#
# Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.  Oracle designates this
# particular file as subject to the "Classpath" exception as provided
# by Oracle in the LICENSE file that accompanied this code.
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

. config.sh

Log false "Building Vector API tests, $(date)\n"

# For each type
for type in byte short int long float double
do
  Type="$(tr '[:lower:]' '[:upper:]' <<< ${type:0:1})${type:1}"

  # For each size
  for bits in 64 128 256 512
  do
    vectorteststype=${typeprefix}${Type}${bits}VectorTests
    # Compile
    Log true "Compiling ${vectorteststype}... "
    Log false "\n${JAVAC} -cp \"${VECTORTESTS_HOME_CP}$SEPARATOR${TESTNG_JAR}\" --add-modules=jdk.incubator.vector $vectorteststype.java 2>&1"
    compilation=$(${JAVAC} -cp "${VECTORTESTS_HOME_CP}$SEPARATOR${TESTNG_JAR}" \
                    --add-modules=jdk.incubator.vector $vectorteststype.java 2>&1)
    if [[ $compilation  == *"error"* ]]; then
      Log true "$compilation\n"
      exit -1
    else
      Log false "$compilation\n"
    fi
    Log true "done\n"
  done
done

rm -fr build


