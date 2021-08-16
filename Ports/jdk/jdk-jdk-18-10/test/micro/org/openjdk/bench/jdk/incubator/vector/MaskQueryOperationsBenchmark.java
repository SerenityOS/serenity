//
// Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
//
// This code is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License version 2 only, as
// published by the Free Software Foundation.
//
// This code is distributed in the hope that it will be useful, but WITHOUT
// ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
// version 2 for more details (a copy is included in the LICENSE file that
// accompanied this code).
//
// You should have received a copy of the GNU General Public License version
// 2 along with this work; if not, write to the Free Software Foundation,
// Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
//
// Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
// or visit www.oracle.com if you need additional information or have any
// questions.
//
//
package org.openjdk.bench.jdk.incubator.vector;

import java.util.Random;
import jdk.incubator.vector.*;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class MaskQueryOperationsBenchmark {
    @Param({"128","256","512"})
    int bits;

    @Param({"1","2","3"})
    int inputs;

    VectorSpecies<Byte> bspecies;
    VectorSpecies<Short> sspecies;
    VectorSpecies<Integer> ispecies;
    VectorSpecies<Long> lspecies;
    VectorMask<Byte> bmask;
    VectorMask<Short> smask;
    VectorMask<Integer> imask;
    VectorMask<Long> lmask;
    boolean [] mask_arr;

    static final boolean [] mask_avg_case = {
       false, false, false, true, false, false, false, false,
       false, false, false, true, false, false, false, false,
       false, false, false, true, false, false, false, false,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       false, false, false, true, false, false, false, false,
       false, false, false, true, false, false, false, false,
       false, false, false, true, false, false, false, false
    };

    static final boolean [] mask_best_case  = {
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true,
       true, true, true, true, true, true, true, true
    };

    static final boolean [] mask_worst_case  = {
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false,
       false, false, false, false, false, false, false, false
    };

    @Setup(Level.Trial)
    public void BmSetup() {
        bspecies = VectorSpecies.of(byte.class, VectorShape.forBitSize(bits));
        sspecies = VectorSpecies.of(short.class, VectorShape.forBitSize(bits));
        ispecies = VectorSpecies.of(int.class, VectorShape.forBitSize(bits));
        lspecies = VectorSpecies.of(long.class, VectorShape.forBitSize(bits));

        if( 1 == inputs) {
          mask_arr = mask_best_case;
        } else if ( 2 == inputs ) {
          mask_arr = mask_worst_case;
        } else {
          mask_arr = mask_avg_case;
        }

        bmask   = VectorMask.fromArray(bspecies, mask_arr, 0);
        smask   = VectorMask.fromArray(sspecies, mask_arr, 0);
        imask   = VectorMask.fromArray(ispecies, mask_arr, 0);
        lmask   = VectorMask.fromArray(lspecies, mask_arr, 0);
    }

    @Benchmark
    public int testTrueCountByte(Blackhole bh) {
        return bmask.trueCount();
    }

    @Benchmark
    public int testTrueCountShort(Blackhole bh) {
        return smask.trueCount();
    }
    @Benchmark
    public int testTrueCountInt(Blackhole bh) {
        return imask.trueCount();
    }
    @Benchmark
    public int testTrueCountLong(Blackhole bh) {
        return lmask.trueCount();
    }

    @Benchmark
    public int testFirstTrueByte(Blackhole bh) {
        return bmask.firstTrue();
    }

    @Benchmark
    public int testFirstTrueShort(Blackhole bh) {
        return smask.firstTrue();
    }
    @Benchmark
    public int testFirstTrueInt(Blackhole bh) {
        return imask.firstTrue();
    }
    @Benchmark
    public int testFirstTrueLong(Blackhole bh) {
        return lmask.firstTrue();
    }

    @Benchmark
    public int testLastTrueByte(Blackhole bh) {
        return bmask.lastTrue();
    }

    @Benchmark
    public int testLastTrueShort(Blackhole bh) {
        return smask.lastTrue();
    }
    @Benchmark
    public int testLastTrueInt(Blackhole bh) {
        return imask.lastTrue();
    }
    @Benchmark
    public int testLastTrueLong(Blackhole bh) {
        return lmask.lastTrue();
    }
}
