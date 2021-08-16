/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
public class StrictMathBench {

    public int int1 = 1, int2 = 2, int42 = 42, int5 = 5;
    public long long1 = 1L, long2 = 2L, long747 = 747L, long13 = 13L;
    public float float1 = 1.0f, float2 = 2.0f, floatNegative99 = -99.0f, float7 = 7.0f, eFloat = 2.718f;
    public double double1 = 1.0d, double2 = 2.0d, double81 = 81.0d, doubleNegative12 = -12.0d, double4Dot1 = 4.1d;

    @Benchmark
    public double  absDouble() {
        return  StrictMath.abs(doubleNegative12);
    }

    @Benchmark
    public int  absExactInt() {
        return  StrictMath.absExact(int2);
    }

    @Benchmark
    public long  absExactLong() {
        return  StrictMath.absExact(long2);
    }

    @Benchmark
    public float  absFloat() {
        return  StrictMath.abs(floatNegative99);
    }

    @Benchmark
    public int  absInt() {
        return  StrictMath.abs(int42);
    }

    @Benchmark
    public long  absLong() {
        return  StrictMath.abs(long13);
    }

    @Benchmark
    public double  acosDouble() {
        return  StrictMath.acos(double1);
    }

    @Benchmark
    public int  addExactInt() {
        return  StrictMath.addExact(int42, int5);
    }

    @Benchmark
    public long  addExactLong() {
        return  StrictMath.addExact(long2, long13);
    }

    @Benchmark
    public double  asinDouble() {
        return  StrictMath.asin(double1);
    }

    @Benchmark
    public double  atanDouble() {
        return  StrictMath.atan(double1);
    }

    @Benchmark
    public double  atan2Double() {
        return  StrictMath.atan2(double1, double2);
    }

    @Benchmark
    public double  cbrt() {
        return  StrictMath.cbrt(double81);
    }

    @Benchmark
    public double  ceilDouble() {
        return  StrictMath.ceil(double4Dot1);
    }

    @Benchmark
    public double  copySignDouble() {
        return  StrictMath.copySign(double81, doubleNegative12);
    }

    @Benchmark
    public float  copySignFloat() {
        return  StrictMath.copySign(floatNegative99, float1);
    }

    @Benchmark
    public double  cosDouble() {
        return  StrictMath.cos(double1);
    }

    @Benchmark
    public double  coshDouble() {
        return  StrictMath.cosh(double2);
    }

    @Benchmark
    public int  decrementExactInt() {
        return  StrictMath.decrementExact(int42);
    }

    @Benchmark
    public long  decrementExactLong() {
        return  StrictMath.decrementExact(long747);
    }

    @Benchmark
    public double  expDouble() {
        return  StrictMath.exp(double4Dot1);
    }

    @Benchmark
    public double  expm1() {
        return  StrictMath.expm1(doubleNegative12);
    }

    @Benchmark
    public double  floorDouble() {
        return  StrictMath.floor(doubleNegative12);
    }

    @Benchmark
    public int  floorDivIntInt() {
        return  StrictMath.floorDiv(int42, int5);
    }

    @Benchmark
    public long  floorDivLongInt() {
        return  StrictMath.floorDiv(long747, int42);
    }

    @Benchmark
    public long  floorDivLongLong() {
        return  StrictMath.floorDiv(long747, long13);
    }

    @Benchmark
    public int  floorModIntInt() {
        return  StrictMath.floorMod(int42, int5);
    }

    @Benchmark
    public int  floorModLongInt() {
        return  StrictMath.floorMod(long747, int5);
    }

    @Benchmark
    public long  floorModLongLong() {
        return  StrictMath.floorMod(long747, long13);
    }

    @Benchmark
    public double  fmaDouble() {
        return  StrictMath.fma(double2, double81, double4Dot1);
    }

    @Benchmark
    public float  fmaFloat() {
        return  StrictMath.fma(float2, floatNegative99, float7);
    }

    @Benchmark
    public int  getExponentDouble() {
        return  StrictMath.getExponent(double81);
    }

    @Benchmark
    public int  getExponentFloat() {
        return  StrictMath.getExponent(float7);
    }

    @Benchmark
    public double  hypotDouble() {
        return  StrictMath.hypot(double2, double4Dot1);
    }

    @Benchmark
    public double  IEEERemainderDouble() {
        return  StrictMath.IEEEremainder(double81, double4Dot1);
    }

    @Benchmark
    public int  IncrementExactInt() {
        return  StrictMath.incrementExact(int42);
    }

    @Benchmark
    public long  IncrementExactLong() {
        return  StrictMath.incrementExact(long747);
    }

    @Benchmark
    public double  logDouble() {
        return  StrictMath.log(double81);
    }

    @Benchmark
    public double  log10Double() {
        return  StrictMath.log10(double81);
    }

    @Benchmark
    public double  log1pDouble() {
        return  StrictMath.log1p(double81);
    }

    @Benchmark
    public int  maxInt() {
        return  StrictMath.max(int1, int2);
    }

    @Benchmark
    public long  maxLong() {
        return  StrictMath.max(long1, long2);
    }

    @Benchmark
    public float  maxFloat() {
        return  StrictMath.max(float1, float2);
    }

    @Benchmark
    public double  maxDouble() {
        return  StrictMath.max(double1, doubleNegative12);
    }

    @Benchmark
    public int  minInt() {
        return  StrictMath.min(int1, int2);
    }

    @Benchmark
    public long  minLong() {
        return  StrictMath.min(long1, long2);
    }

    @Benchmark
    public float  minFloat() {
        return  StrictMath.min(float1, floatNegative99);
    }

    @Benchmark
    public double  minDouble() {
        return  StrictMath.min(double4Dot1, double2);
    }

    @Benchmark
    public int  multiplyExactInt() {
        return  StrictMath.multiplyExact(int42, int5);
    }

    @Benchmark
    public long  multiplyExactLongInt() {
        return  StrictMath.multiplyExact(long747, int42);
    }

    @Benchmark
    public long  multiplyExactLongLong() {
        return  StrictMath.multiplyExact(long747, long13);
    }

    @Benchmark
    public long  multiplyFullIntInt() {
        return  StrictMath.multiplyFull(int42, int5);
    }

    @Benchmark
    public long  multiplyHighLongLog() {
        return  StrictMath.multiplyHigh(long747, long13);
    }

    @Benchmark
    public int  negateExactInt() {
        return  StrictMath.negateExact(int42);
    }

    @Benchmark
    public long  negateExactLong() {
        return  StrictMath.negateExact(long747);
    }

    @Benchmark
    public double  nextAfterDoubleDouble() {
        return  StrictMath.nextAfter(double81, double4Dot1);
    }

    @Benchmark
    public float  nextAfterFloatDouble() {
        return  StrictMath.nextAfter(float7, doubleNegative12);
    }

    @Benchmark
    public double  nextDownDouble() {
        return  StrictMath.nextDown(float7);
    }

    @Benchmark
    public float  nextDownFloat() {
        return  StrictMath.nextDown(floatNegative99);
    }

    @Benchmark
    public double  nextUpDouble() {
        return  StrictMath.nextUp(double81);
    }

    @Benchmark
    public float  nextUpFloat() {
        return  StrictMath.nextUp(float7);
    }

    @Benchmark
    public double  powDouble() {
        return  StrictMath.pow(double4Dot1, double2);
    }

    @Benchmark
    public double  random() {
        return  StrictMath.random();
    }

    @Benchmark
    public double  rintDouble() {
        return  StrictMath.rint(double4Dot1);
    }

    @Benchmark
    public long  roundDouble() {
        return  StrictMath.round( StrictMath.PI);
    }

    @Benchmark
    public int  roundFloat() {
        return  StrictMath.round(eFloat);
    }

    @Benchmark
    public double  scalbDoubleInt() {
        return  StrictMath.scalb(double81, int2);
    }

    @Benchmark
    public float  scalbFloatInt() {
        return  StrictMath.scalb(float7, int2);
    }

    @Benchmark
    public double  sigNumDouble() {
        return  StrictMath.signum(double4Dot1);
    }

    @Benchmark
    public double  signumFloat() {
        return  StrictMath.signum(floatNegative99);
    }

    @Benchmark
    public double  sinDouble() {
        return  StrictMath.sin(double1);
    }

    @Benchmark
    public double  sinhDouble() {
        return  StrictMath.sinh(double4Dot1);
    }

    @Benchmark
    public double  sqrtDouble() {
        return  StrictMath.sqrt(double4Dot1);
    }

    @Benchmark
    public double  subtractExactIntInt() {
        return  StrictMath.subtractExact(int42,int5);
    }

    @Benchmark
    public double  subtractExactLongLong() {
        return  StrictMath.subtractExact(long747,long13);
    }

    @Benchmark
    public double  tanDouble() {
        return  StrictMath.tan(double1);
    }

    @Benchmark
    public double  tanhDouble() {
        return  StrictMath.tanh(double1);
    }

    @Benchmark
    public double  toDegreesDouble() {
        return  StrictMath.toDegrees(double81);
    }

    @Benchmark
    public double  toIntExactLong() {
        return  StrictMath.toIntExact(long747);
    }

    @Benchmark
    public double  toRadiansDouble() {
        return  StrictMath.toRadians(double81);
    }

    @Benchmark
    public double  ulpDouble() {
        return  StrictMath.ulp(double4Dot1);
    }

    @Benchmark
    public double  ulpFloat() {
        return  StrictMath.ulp(float7);
    }

}
