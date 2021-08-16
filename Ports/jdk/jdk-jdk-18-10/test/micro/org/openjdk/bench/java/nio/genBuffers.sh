#! /bin/sh

#
# Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

javac -d . ../../../../../../../make/jdk/src/classes/build/tools/spp/Spp.java

genBin() {
  for MS in "Heap" "Direct"
    do
      for SWAP in "Swap" ""
      do
        for RO in "RO" ""
        do
        extraArgs=""
        if [ "$RO" == "RO" ] ; then
          extraArgs="-KRO"
        fi
        java build.tools.spp.Spp -be -nel -K$1 -Dtype=$1 -DType=$2 -DFulltype=$3 \
              $extraArgs \
              -Kview \
              -DMs=$MS \
              -Dms=`echo "$MS" | awk '{print tolower($0)}'` \
              -DSWAP=$SWAP \
              -DRO=$RO \
              -DCarrierBW=$4 \
              -i$5 \
              -o$out
        done
      done
    done
}

gen() {
    out=$2Buffers.java
    rm -f $out
    java build.tools.spp.Spp -be -nel -K$1 -Dtype=$1 -DType=$2 -DFulltype=$3 \
          -DCarrierBW=$4 -iX-Buffers.java.template -o$out

    java build.tools.spp.Spp -be -nel -K$1 -Dtype=$1 -DType=$2 -DFulltype=$3 \
          -DMs=Heap -Dms=heap -DSWAP="" -DRO="" -iX-Buffers-bin.java.template -o$out

    if [ "$1" == "byte" ] ; then
      genBin $1 $2 $3 $4 X-ByteBuffers-bin.java.template
      genBin char Char Character 2 X-ByteBuffers-bin.java.template
      genBin short Short Short 2 X-ByteBuffers-bin.java.template
      genBin int Int Integer 4 X-ByteBuffers-bin.java.template
      genBin long Long Long 8 X-ByteBuffers-bin.java.template
      genBin float Float Float 4 X-ByteBuffers-bin.java.template
      genBin double Double Double 8 X-ByteBuffers-bin.java.template
    else
      genBin $1 $2 $3 $4 X-Buffers-bin.java.template
    fi

    printf "}\n" >> $out
}

gen byte Byte Byte 1
gen char Char Character 2
gen short Short Short 2
gen int Int Integer 4
gen long Long Long 8
gen float Float Float 4
gen double Double Double 8
