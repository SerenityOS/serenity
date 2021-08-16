/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class MathBench {

    @Param("0")
    public long seed;

    public int dividend;
    public int divisor;

    public long longDividend;
    public long longDivisor;

    public int int1 = 1, int2 = 2, int42 = 42, int5 = 5;
    public long long1 = 1L, long2 = 2L, long747 = 747L, long13 = 13L;
    public float float1 = 1.0f, float2 = 2.0f, floatNegative99 = -99.0f, float7 = 7.0f, eFloat = 2.718f;
    public double double1 = 1.0d, double2 = 2.0d, double81 = 81.0d, doubleNegative12 = -12.0d, double4Dot1 = 4.1d, double0Dot5 = 0.5d;

    @Setup
    public void setupValues() {
        Random random = new Random(seed);
        dividend = Math.abs(random.nextInt() + 4711);
        divisor  = Math.abs(random.nextInt(dividend) + 17);
        longDividend = Math.abs(random.nextLong() + 4711L);
        longDivisor  = Math.abs(random.nextLong() + longDividend);
    }

    @Benchmark
    public double  absDouble() {
        return  Math.abs(doubleNegative12);
    }

    @Benchmark
    public float  absFloat() {
        return  Math.abs(floatNegative99);
    }

    @Benchmark
    public int  absExactInt() {
        return  Math.absExact(int2);
    }

    @Benchmark
    public long  absExactLong() {
        return  Math.absExact(long2);
    }

    @Benchmark
    public int  absInt() {
        return  Math.abs(int42);
    }

    @Benchmark
    public long  absLong() {
        return  Math.abs(long13);
    }

    @Benchmark
    public double  acosDouble() {
        return  Math.acos(double1);
    }

    @Benchmark
    public int  addExactInt() {
        return  Math.addExact(int42, int5);
    }

    @Benchmark
    public long  addExactLong() {
        return  Math.addExact(long2, long13);
    }

    @Benchmark
    public double  asinDouble() {
        return  Math.asin(double1);
    }

    @Benchmark
    public double  atanDouble() {
        return  Math.atan(double1);
    }

    @Benchmark
    public double  atan2Double() {
        return  Math.atan2(double1, double2);
    }

    @Benchmark
    public double  cbrt() {
        return  Math.cbrt(double81);
    }

    @Benchmark
    public double  ceilDouble() {
        return  Math.ceil(double4Dot1);
    }

    @Benchmark
    public double  copySignDouble() {
        return  Math.copySign(double81, doubleNegative12);
    }

    @Benchmark
    public float  copySignFloat() {
        return  Math.copySign(floatNegative99, float1);
    }

    @Benchmark
    public double  cosDouble() {
        return  Math.cos(double1);
    }

    @Benchmark
    public double  coshDouble() {
        return  Math.cosh(double2);
    }

    @Benchmark
    public int  decrementExactInt() {
        return  Math.decrementExact(int42);
    }

    @Benchmark
    public long  decrementExactLong() {
        return  Math.decrementExact(long747);
    }

    @Benchmark
    public double  expDouble() {
        return  Math.exp(double4Dot1);
    }

    @Benchmark
    public double  expm1() {
        return  Math.expm1(doubleNegative12);
    }

    @Benchmark
    public double  floorDouble() {
        return  Math.floor(doubleNegative12);
    }

    @Benchmark
    public int  floorDivIntInt() {
        return  Math.floorDiv(int42, int5);
    }

    @Benchmark
    public long  floorDivLongInt() {
        return  Math.floorDiv(long747, int42);
    }

    @Benchmark
    public long  floorDivLongLong() {
        return  Math.floorDiv(long747, long13);
    }

    @Benchmark
    public int  floorModIntInt() {
        return  Math.floorMod(int42, int5);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int floorModIntIntMultiple() {
        return Math.floorMod( dividend,  divisor) +
               Math.floorMod( dividend, -divisor) +
               Math.floorMod(-dividend,  divisor) +
               Math.floorMod(-dividend, -divisor);
    }

    @Benchmark
    public int  floorModLongInt() {
        return  Math.floorMod(long747, int5);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public int floorModLongIntMultiple() {
        return Math.floorMod( longDividend,  divisor) +
               Math.floorMod( longDividend, -divisor) +
               Math.floorMod(-longDividend,  divisor) +
               Math.floorMod(-longDividend, -divisor);
    }

    @Benchmark
    public long  floorModLongLong() {
        return  Math.floorMod(long747, long13);
    }

    @Benchmark
    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public long floorModLongLongMultiple() {
        return Math.floorMod( longDividend,  longDivisor) +
               Math.floorMod( longDividend, -longDivisor) +
               Math.floorMod(-longDividend,  longDivisor) +
               Math.floorMod(-longDividend, -longDivisor);
    }

    @Benchmark
    public double  fmaDouble() {
        return  Math.fma(double2, double81, double4Dot1);
    }

    @Benchmark
    public float  fmaFloat() {
        return  Math.fma(float2, floatNegative99, float7);
    }

    @Benchmark
    public int  getExponentDouble() {
        return  Math.getExponent(double81);
    }

    @Benchmark
    public int  getExponentFloat() {
        return  Math.getExponent(float7);
    }

    @Benchmark
    public double  hypotDouble() {
        return  Math.hypot(double2, double4Dot1);
    }

    @Benchmark
    public double  IEEERemainderDouble() {
        return  Math.IEEEremainder(double81, double4Dot1);
    }

    @Benchmark
    public int  incrementExactInt() {
        return  Math.incrementExact(int42);
    }

    @Benchmark
    public long  incrementExactLong() {
        return  Math.incrementExact(long747);
    }

    @Benchmark
    public double  logDouble() {
        return  Math.log(double81);
    }

    @Benchmark
    public double  log10Double() {
        return  Math.log10(double81);
    }

    @Benchmark
    public double  log1pDouble() {
        return  Math.log1p(double81);
    }

    @Benchmark
    public int  maxInt() {
        return  Math.max(int1, int2);
    }

    @Benchmark
    public long  maxLong() {
        return  Math.max(long1, long2);
    }

    @Benchmark
    public float  maxFloat() {
        return  Math.max(float1, float2);
    }

    @Benchmark
    public double  maxDouble() {
        return  Math.max(double1, doubleNegative12);
    }

    @Benchmark
    public int  minInt() {
        return  Math.min(int1, int2);
    }

    @Benchmark
    public long  minLong() {
        return  Math.min(long1, long2);
    }

    @Benchmark
    public float  minFloat() {
        return  Math.min(float1, floatNegative99);
    }

    @Benchmark
    public double  minDouble() {
        return  Math.min(double4Dot1, double2);
    }

    @Benchmark
    public int  multiplyExactInt() {
        return  Math.multiplyExact(int42, int5);
    }

    @Benchmark
    public long  multiplyExactLongInt() {
        return  Math.multiplyExact(long747, int42);
    }

    @Benchmark
    public long  multiplyExactLongLong() {
        return  Math.multiplyExact(long747, long13);
    }

    @Benchmark
    public long  multiplyFullIntInt() {
        return  Math.multiplyFull(int42, int5);
    }

    @Benchmark
    public long  multiplyHighLongLog() {
        return  Math.multiplyHigh(long747, long13);
    }

    @Benchmark
    public int  negateExactInt() {
        return  Math.negateExact(int42);
    }

    @Benchmark
    public long  negateExactLong() {
        return  Math.negateExact(long747);
    }

    @Benchmark
    public double  nextAfterDoubleDouble() {
        return  Math.nextAfter(double81, double4Dot1);
    }

    @Benchmark
    public float  nextAfterFloatDouble() {
        return  Math.nextAfter(float7, doubleNegative12);
    }

    @Benchmark
    public double  nextDownDouble() {
        return  Math.nextDown(float7);
    }

    @Benchmark
    public float  nextDownFloat() {
        return  Math.nextDown(floatNegative99);
    }

    @Benchmark
    public double  nextUpDouble() {
        return  Math.nextUp(double81);
    }

    @Benchmark
    public float  nextUpFloat() {
        return  Math.nextUp(float7);
    }

    @Benchmark
    public double  powDouble() {
        return  Math.pow(double4Dot1, double2);
    }

    @Benchmark
    public double  powDoubleLoop() {
        double sum = 0.0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                sum += i + Math.pow(j * 1.0, i * 1.0);
            }
        }
        return sum;
    }

    @Benchmark
    public double  powDouble0Dot5() {
        return  Math.pow(double4Dot1, double0Dot5);
    }

    @Benchmark
    public double  powDouble0Dot5Const() {
        return  Math.pow(double4Dot1, 0.5);
    }

    @Benchmark
    public double  powDouble0Dot5Loop() {
        double sum = 0.0;
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 1000; j++) {
                sum += i + Math.pow(j * 1.0, 0.5);
            }
        }
        return sum;
    }

    @Benchmark
    public double  random() {
        return  Math.random();
    }

    @Benchmark
    public double  rintDouble() {
        return  Math.rint(double4Dot1);
    }

    @Benchmark
    public long  roundDouble() {
        return  Math.round( Math.PI);
    }

    @Benchmark
    public int  roundFloat() {
        return  Math.round(eFloat);
    }

    @Benchmark
    public double  scalbDoubleInt() {
        return  Math.scalb(double81, int2);
    }

    @Benchmark
    public float  scalbFloatInt() {
        return  Math.scalb(float7, int2);
    }

    @Benchmark
    public double  sigNumDouble() {
        return  Math.signum(double4Dot1);
    }

    @Benchmark
    public double  signumFloat() {
        return  Math.signum(floatNegative99);
    }

    @Benchmark
    public double  sinDouble() {
        return  Math.sin(double1);
    }

    @Benchmark
    public double  sinhDouble() {
        return  Math.sinh(double4Dot1);
    }

    @Benchmark
    public double  sqrtDouble() {
        return  Math.sqrt(double4Dot1);
    }

    @Benchmark
    public double  subtractExactIntInt() {
        return  Math.subtractExact(int42,int5);
    }

    @Benchmark
    public double  subtractExactLongLong() {
        return  Math.subtractExact(long747,long13);
    }

    @Benchmark
    public double  tanDouble() {
        return  Math.tan(double1);
    }

    @Benchmark
    public double  tanhDouble() {
        return  Math.tanh(double1);
    }

    @Benchmark
    public double  toDegreesDouble() {
        return  Math.toDegrees(double81);
    }

    @Benchmark
    public double  toIntExactLong() {
        return  Math.toIntExact(long747);
    }

    @Benchmark
    public double  toRadiansDouble() {
        return  Math.toRadians(double81);
    }

    @Benchmark
    public double  ulpDouble() {
        return  Math.ulp(double4Dot1);
    }

    @Benchmark
    public double  ulpFloat() {
        return  Math.ulp(float7);
    }

}
