/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class InterfaceCalls {

    interface AnInterface {
        public int getInt();
    }

    interface SecondInterface {
        public int get1();
    }

    interface OnlyHasOneImplInterface {
        public int getLong();
    }

    interface AloneInterface {
        public int getNumber();
    }

    class SingleImplementor implements OnlyHasOneImplInterface {
        public int getLong() {
            return 1;
        }
    }

    class Extender1 extends SingleImplementor {
    }

    class FirstClass implements AnInterface {
        public int getInt() {
            return 1;
        }
    }

    class SecondClass implements AnInterface {
        public int getInt() {
            return 2;
        }
    }

    class ThirdClass implements AnInterface {
        public int getInt() {
            return -3;
        }
    }

    class FourthClass implements AnInterface {
        public int getInt() {
            return -4;
        }
    }

    class FifthClass implements AnInterface {
        public int getInt() {
            return -5;
        }
    }

    class MultiClass1 implements AnInterface, SecondInterface {
        public int get1() {
            return 1;
        }

        public int getInt() {
            return 2;
        }
    }

    class MultiClass2 implements AnInterface, SecondInterface {
        public int get1() {
            return -1;
        }

        public int getInt() {
            return -2;
        }
    }

    class Aloner implements AloneInterface {
        public int getNumber() {
            return 7;
        }
    }

    public Object dummy1;

    public Object dummy2;

    public Object dummy3;

    public AnInterface multi1a, multi2a;

    public SecondInterface multi1b, multi2b;

    public Object multic, multic2;

    public AnInterface[] as = new AnInterface[5];

    public AnInterface multi;

    public OnlyHasOneImplInterface single1;

    public OnlyHasOneImplInterface single2;

    public AloneInterface alone;

    int count;

    @Setup
    public void setupSubclass() {
        dummy1 = new FirstClass();
        dummy2 = new SecondClass();
        dummy3 = new ThirdClass();
        as[0] = new FirstClass();
        as[1] = new SecondClass();
        as[2] = new ThirdClass();
        as[3] = new FourthClass();
        as[4] = new FifthClass();
        MultiClass1 mc1 = new MultiClass1();
        multi1a = mc1;
        multi1b = mc1;
        multic = mc1;
        MultiClass2 mc2 = new MultiClass2();
        multi2a = mc2;
        multi2b = mc2;
        multic2 = mc2;
        single1 = new SingleImplementor();
        single2 = new Extender1();
        alone = new Aloner();
    }

    private void swapMultiParts() {
        AnInterface tmpa = multi1a;
        SecondInterface tmpb = multi1b;
        multi1a = multi2a;
        multi2a = tmpa;
        multi1b = multi2b;
        multi2b = tmpb;
    }

    @SuppressWarnings("unused")
    private void swapMulti() {
        Object tmp = multic;
        multic = multic2;
        multic2 = tmp;
    }

    /**
     * Tests a call where there are multiple implementors but only one of the implementors is every used here so the
     * call-site is monomorphic
     */
    @Benchmark
    public int testMonomorphic() {
        return as[0].getInt();
    }

    /** Tests a interface call that only has one single implementation */
    @Benchmark
    public int testSingle() {
        return alone.getNumber();
    }

    /**
     * Tests a call where there is a single implementation but multiple classes that inherit that implementation and both
     * these implementors are used.
     */
    @Benchmark
    public int testSingle2() {
        OnlyHasOneImplInterface oi;
        if ((count & 1) == 0) {
            oi = single1;
        } else {
            oi = single2;
        }
        count++;
        return oi.getLong();
    }

    /**
     * Tests calling two different interface methods in two different interfaces on the same receiver. Make sure to switch
     * between two different types of receivers to achieve polymorhpism
     */
    @Benchmark
    public void testCall2Poly2(Blackhole bh) {
        bh.consume(multi1a.getInt());
        bh.consume(multi1b.get1());
        swapMultiParts();
    }

    @Benchmark
    public int testCallMulti1Poly2NoSwap() {
        return multi1a.getInt();
    }

    /**
     * This test goes together with Multi2 below It tests if a class implements multiple interfaces if the different
     * interfaces take different amounts of time (They do for hotspot)
     */
    @Benchmark
    public int testCallMulti1Poly2() {
        swapMultiParts();
        return multi1a.getInt();
    }

    /**
     * This test goes together with Multi2 below It tests if a class implements multiple interfaces if the different
     * interfaces take different amounts of time (They do for hotspot)
     */
    @Benchmark
    public int testCallMulti2Poly2() {
        swapMultiParts();
        return multi1b.get1();
    }

    /** Interface call with three different receivers */
    @Benchmark
    public void testCallPoly3(Blackhole bh) {
        for (int kk = 0; kk < 3; kk++) {
            bh.consume(as[kk].getInt());
        }
    }

    /** Interface call with five different receivers. */
    @Benchmark
    public void testCallPoly5(Blackhole bh) {
        for (int kk = 0; kk < 5; kk++) {
            bh.consume(as[kk].getInt());
        }
    }

    int l;

    /**
     * Interface call address computation within loop but the receiver preexists the loop and the ac can be moved outside
     * of the loop
     */
    @Benchmark
    public int testAC1() {
        AnInterface ai = as[l];
        l = 1 - l;
        return ai.getInt();
    }

    /** Tests an interface cast followed by an interface call. */
    @Benchmark
    public int testInterfaceCastAndCall() throws Exception {
        return ((AnInterface) dummy1).getInt() + ((AnInterface) dummy2).getInt()
                + ((AnInterface) dummy3).getInt();
    }
}
