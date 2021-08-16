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

# For manual invocation.
# You can regenerate the source files,
# and you can clean them up.
# FIXME: Move this script under $REPO/make/gensrc/
list_mech_gen() {
    ( # List MG files physically present
      grep -il 'mechanically generated.*do not edit' $(find * -name \*.java -print)
      # List MG files currently deleted (via --clean)
      hg status -nd .
    ) | egrep '(^|/)(Byte|Short|Int|Long|Float|Double)(Scalar|([0-9Max]+Vector)).*\.java$'
}
case $* in
'')             CLASS_FILTER='*';;
--generate*)    CLASS_FILTER=${2-'*'};;
--clean)        MG=$(list_mech_gen); set -x; rm -f $MG; exit;;
--revert)       MG=$(list_mech_gen); set -x; hg revert $MG; exit;;
--list)         list_mech_gen; exit;;
--help|*)       echo "Usage: $0 [--generate [file] | --clean | --revert | --list]"; exit 1;;
esac

. config.sh

# Detect whether to generate the performance tests
generate_perf_tests=false
if [ -d "$PERF_DEST" ]; then
  generate_perf_tests=true
fi

# First, generate the template file.
bash ./gen-template.sh $generate_perf_tests

Log false "Generating Vector API tests, $(date)\n"

# Compile SPP
Log true "Compiling SPP... "
compilation=$(${JAVAC} -d . "${JDK_SRC_HOME}/make/jdk/src/classes/build/tools/spp/Spp.java")
Log false "$compilation\n"
Log true "done\n"

# For each type
for type in byte short int long float double
do
  Type="$(tr '[:lower:]' '[:upper:]' <<< ${type:0:1})${type:1}"
  TYPE="$(tr '[:lower:]' '[:upper:]' <<< ${type})"
  args="-K$type -Dtype=$type -DType=$Type -DTYPE=$TYPE"

  Boxtype=$Type
  Wideboxtype=$Boxtype
  MaxValue=MAX_VALUE
  MinValue=MIN_VALUE

  kind=BITWISE

  bitstype=$type
  Bitstype=$Type
  Boxbitstype=$Boxtype

  fptype=$type
  Fptype=$Type
  Boxfptype=$Boxtype

  case $type in
    byte)
      Wideboxtype=Byte
      args="$args -KbyteOrShort"
      ;;
    short)
      Wideboxtype=Short
      args="$args -KbyteOrShort"
      ;;
    int)
      Boxtype=Integer
      Wideboxtype=Integer
      fptype=float
      Fptype=Float
      Boxfptype=Float
      args="$args -KintOrLong"
      ;;
    long)
      Wideboxtype=Long
      fptype=double
      Fptype=Double
      Boxfptype=Double
      args="$args -KintOrLong"
      ;;
    float)
      kind=FP
      bitstype=int
      Bitstype=Int
      Boxbitstype=Integer
      Wideboxtype=Float
      MaxValue=POSITIVE_INFINITY
      MinValue=NEGATIVE_INFINITY
      ;;
    double)
      kind=FP
      bitstype=long
      Bitstype=Long
      Boxbitstype=Long
      Wideboxtype=Double
      MaxValue=POSITIVE_INFINITY
      MinValue=NEGATIVE_INFINITY
      ;;
  esac

  args="$args -K$kind -K$Type -DBoxtype=$Boxtype -DWideboxtype=$Wideboxtype -DMaxValue=$MaxValue -DMinValue=$MinValue"
  args="$args -Dbitstype=$bitstype -DBitstype=$Bitstype -DBoxbitstype=$Boxbitstype"
  args="$args -Dfptype=$fptype -DFptype=$Fptype -DBoxfptype=$Boxfptype"

  abstractvectortype=${typeprefix}${Type}Vector
  abstractvectorteststype=${typeprefix}${Type}VectorTests
  abstractbitsvectortype=${typeprefix}${Bitstype}Vector
  abstractfpvectortype=${typeprefix}${Fptype}Vector
  args="$args -Dabstractvectortype=$abstractvectortype -Dabstractvectorteststype=$abstractvectorteststype -Dabstractbitsvectortype=$abstractbitsvectortype -Dabstractfpvectortype=$abstractfpvectortype"

  # Generate tests for operations
  # For each size
  Log true "${Type}:"

  for bits in 64 128 256 512 Max
  do
    vectortype=${typeprefix}${Type}${bits}Vector
    vectorteststype=${typeprefix}${Type}${bits}VectorTests
    vectorbenchtype=${typeprefix}${Type}${bits}Vector
    masktype=${typeprefix}${Type}${bits}Mask
    bitsvectortype=${typeprefix}${Bitstype}${bits}Vector
    fpvectortype=${typeprefix}${Fptype}${bits}Vector
    shape=S${bits}Bit
    Shape=S_${bits}_BIT
    if [[ "${vectortype}" == "ByteMaxVector" ]]; then
      args="$args -KByteMax"
    fi
    bitargs="$args -Dbits=$bits -Dvectortype=$vectortype -Dvectorteststype=$vectorteststype -Dvectorbenchtype=$vectorbenchtype -Dmasktype=$masktype -Dbitsvectortype=$bitsvectortype -Dfpvectortype=$fpvectortype -Dshape=$shape -DShape=$Shape"
    if [ $bits == 'Max' ]; then
      bitargs="$bitargs -KMaxBit"
    fi

    # Generate jtreg tests
    case $vectorteststype in
    $CLASS_FILTER)
      Log true " ${bits}_jtreg $vectorteststype.java"
      Log false "${JAVA} -cp . ${SPP_CLASSNAME} -nel $bitargs -i${TEMPLATE_FILE} -o$vectorteststype.java "
      TEST_DEST_FILE="${vectorteststype}.java"
      rm -f ${TEST_DEST_FILE}
      ${JAVA} -cp . ${SPP_CLASSNAME} -nel $bitargs \
        -i${TEMPLATE_FILE} \
        -o${TEST_DEST_FILE}
      if [ VAR_OS_ENV==windows.cygwin ]; then
        tr -d  '\r' < ${TEST_DEST_FILE} > temp
        mv temp ${TEST_DEST_FILE}
      fi
      ;;
    esac

    if [ $generate_perf_tests == true ]; then
      # Generate jmh performance tests
      case $vectorbenchtype in
      $CLASS_FILTER)
        Log true " ${bits}_jmh $vectorbenchtype.java"
        Log false "${JAVA} -cp . ${SPP_CLASSNAME} -nel $bitargs -i${PERF_TEMPLATE_FILE} -o${vectorteststype}Perf.java "
        PERF_DEST_FILE="${PERF_DEST}/${vectorbenchtype}.java"
        rm -f ${PERF_DEST_FILE}
        ${JAVA} -cp . ${SPP_CLASSNAME} -nel $bitargs \
          -i${PERF_TEMPLATE_FILE} \
          -o${PERF_DEST_FILE}
        if [ VAR_OS_ENV==windows.cygwin ]; then
          tr -d  '\r' < ${PERF_DEST_FILE} > temp
          mv temp ${PERF_DEST_FILE}
        fi
        ;;
      esac
    fi
  done

  if [ $generate_perf_tests == true ]; then
    # Generate jmh performance tests
    case ${Type}Scalar in
    $CLASS_FILTER)
    Log true " scalar ${Type}Scalar.java"
    PERF_DEST_FILE="${PERF_DEST}/${Type}Scalar.java"
    rm -f ${PERF_DEST_FILE}
    ${JAVA} -cp . ${SPP_CLASSNAME} -nel $args \
      -i${PERF_SCALAR_TEMPLATE_FILE} \
      -o${PERF_DEST_FILE}
    if [ VAR_OS_ENV==windows.cygwin ]; then
      tr -d  '\r' < ${PERF_DEST_FILE} > temp
      mv temp ${PERF_DEST_FILE}
    fi
      ;;
    esac
  fi

  # Generate tests for loads and stores
  # For each size
  for bits in 64 128 256 512 Max
  do
    vectortype=${typeprefix}${Type}${bits}Vector
    vectorteststype=${typeprefix}${Type}${bits}VectorLoadStoreTests
    vectorbenchtype=${typeprefix}${Type}${bits}VectorLoadStore
    masktype=${typeprefix}${Type}${bits}Mask
    bitsvectortype=${typeprefix}${Bitstype}${bits}Vector
    fpvectortype=${typeprefix}${Fptype}${bits}Vector
    shape=S${bits}Bit
    Shape=S_${bits}_BIT
    if [[ "${vectortype}" == "ByteMaxVector" ]]; then
      args="$args -KByteMax"
    fi
    bitargs="$args -Dbits=$bits -Dvectortype=$vectortype -Dvectorteststype=$vectorteststype -Dvectorbenchtype=$vectorbenchtype -Dmasktype=$masktype -Dbitsvectortype=$bitsvectortype -Dfpvectortype=$fpvectortype -Dshape=$shape -DShape=$Shape"
    if [ $bits == 'Max' ]; then
      bitargs="$bitargs -KMaxBit"
    fi

    # Generate
    case $vectorteststype in
    $CLASS_FILTER)
      Log true " ${bits}_ls $vectorteststype.java"
      Log false "${JAVA} -cp . ${SPP_CLASSNAME} -nel $bitargs -itemplates/X-LoadStoreTest.java.template -o$vectorteststype.java "
      TEST_DEST_FILE="${vectorteststype}.java"
      rm -f ${TEST_DEST_FILE}
      ${JAVA} -cp . ${SPP_CLASSNAME} -nel $bitargs \
        -itemplates/X-LoadStoreTest.java.template \
        -o${TEST_DEST_FILE}
      if [ VAR_OS_ENV==windows.cygwin ]; then
        tr -d  '\r' < ${TEST_DEST_FILE} > temp
        mv temp ${TEST_DEST_FILE}
      fi
      ;;
    esac

    # TODO: Generate jmh performance tests for LoadStore variants
  done

  Log true " done\n"

done

rm -fr build

