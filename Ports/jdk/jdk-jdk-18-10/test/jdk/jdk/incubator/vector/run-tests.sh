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

POSITIONAL=()
while [[ $# -gt 0 ]]
do
key="$1"

TESTS=""
DISABLE_VECTOR_INTRINSICS=false
case $key in
    -t|--tests)
    TESTS="$2"
    echo "Tests set to $TESTS"
    shift # past argument
    shift # past value
    ;;
    -d|--disable-vector-intrinsics)
    DISABLE_VECTOR_INTRINSICS=true
    echo "Warning: Disabling Vector intrinsics..."
    shift # past argument
    ;;
    *)    # unknown option
    POSITIONAL+=("$1") # save it in an array for later
    shift # past argument
    ;;
esac
done
set -- "${POSITIONAL[@]}" # restore positional parameters


if [ "$TESTS" == "" ]; then
  # Run all the tests by default.
  TESTS="Byte64VectorTests,Byte128VectorTests,Byte256VectorTests,Byte512VectorTests,"
  TESTS+="Int64VectorTests,Int128VectorTests,Int256VectorTests,Int512VectorTests,"
  TESTS+="Long64VectorTests,Long128VectorTests,Long256VectorTests,Long512VectorTests,"
  TESTS+="Short64VectorTests,Short128VectorTests,Short256VectorTests,Short512VectorTests,"
  TESTS+="Double64VectorTests,Double128VectorTests,Double256VectorTests,Double512VectorTests,"
  TESTS+="Float64VectorTests,Float128VectorTests,Float256VectorTests,Float512VectorTests"
fi

# Get Java flags.
JAVA_FLAGS="-XX:-TieredCompilation"
if [ "$DISABLE_VECTOR_INTRINSICS" == "true" ]; then
	JAVA_FLAGS+=" -XX:-UseVectorApiIntrinsics"
fi

LogRun false "Running tests $(date)\n"
LogRun true "Running the following tests:\n"
LogRun true "${TESTS}\n"
LogRun false "${JAVA} -cp \"${VECTORTESTS_HOME_CP}${SEPARATOR}${TESTNG_RUN_JAR}${SEPARATOR}${JCOMMANDER_JAR}\" ${JAVA_FLAGS} --add-modules jdk.incubator.vector --add-opens jdk.incubator.vector/jdk.incubator.vector=ALL-UNNAMED org.testng.TestNG -testclass $TESTS"

# Actual TestNG run.
time ${JAVA} -cp "${VECTORTESTS_HOME_CP}${SEPARATOR}${TESTNG_RUN_JAR}${SEPARATOR}${JCOMMANDER_JAR}" \
  ${JAVA_FLAGS} \
  --add-modules jdk.incubator.vector \
  --add-opens jdk.incubator.vector/jdk.incubator.vector=ALL-UNNAMED \
  -Djdk.incubator.vector.test.loop-iterations=${TEST_ITER_COUNT} \
  org.testng.TestNG -testclass $TESTS
LogRun true "Tests run complete. Please look at test-output/index.html to visualize the results.\n"

