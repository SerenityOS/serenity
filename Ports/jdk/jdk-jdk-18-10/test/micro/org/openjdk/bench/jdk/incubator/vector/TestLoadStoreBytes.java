/*
 *  Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */
package org.openjdk.bench.jdk.incubator.vector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.TimeUnit;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {
    "--add-modules=jdk.incubator.foreign,jdk.incubator.vector",
    "-Dforeign.restricted=permit",
    "--enable-native-access", "ALL-UNNAMED",
    "-Djdk.incubator.vector.VECTOR_ACCESS_OOB_CHECK=1"})
public class TestLoadStoreBytes {
  private static final VectorSpecies<Byte> SPECIES = VectorSpecies.ofLargestShape(byte.class);

  @Param("1024")
  private int size;

  private byte[] srcArray;

  private byte[] dstArray;


  private ByteBuffer srcBufferHeap;

  private ByteBuffer dstBufferHeap;

  private ByteBuffer srcBufferNative;

  private ByteBuffer dstBufferNative;


  private ResourceScope implicitScope;

  private MemorySegment srcSegmentImplicit;

  private MemorySegment dstSegmentImplicit;

  private ByteBuffer srcBufferSegmentImplicit;

  private ByteBuffer dstBufferSegmentImplicit;


  private MemoryAddress srcAddress;

  private MemoryAddress dstAddress;

  byte[] a, b, c;

  @Setup
  public void setup() {
    srcArray = new byte[size];
    dstArray = srcArray.clone();
    for (int i = 0; i < srcArray.length; i++) {
      srcArray[i] = (byte) i;
    }


    srcBufferHeap = ByteBuffer.allocate(size);
    dstBufferHeap = ByteBuffer.allocate(size);

    srcBufferNative = ByteBuffer.allocateDirect(size);
    dstBufferNative = ByteBuffer.allocateDirect(size);


    implicitScope = ResourceScope.newImplicitScope();
    srcSegmentImplicit = MemorySegment.allocateNative(size, SPECIES.vectorByteSize(), implicitScope);
    srcBufferSegmentImplicit = srcSegmentImplicit.asByteBuffer();
    dstSegmentImplicit = MemorySegment.allocateNative(size, SPECIES.vectorByteSize(), implicitScope);
    dstBufferSegmentImplicit = dstSegmentImplicit.asByteBuffer();


    srcAddress = CLinker.allocateMemory(size);
    dstAddress = CLinker.allocateMemory(size);

    a = new byte[size];
    b = new byte[size];
    c = new byte[size];
  }


  @Benchmark
  public void array() {
//    final var srcArray = this.srcArray;
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
      var v = ByteVector.fromArray(SPECIES, srcArray, i);
      v.intoArray(dstArray, i);
    }
  }

  @Benchmark
  public void array2() {
//    final var srcArray = this.srcArray;
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
      var v = ByteVector.fromByteArray(SPECIES, srcArray, i, ByteOrder.nativeOrder());
      v.intoByteArray(dstArray, i, ByteOrder.nativeOrder());
    }
  }

  @Benchmark
  public void arrayScalar() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i ++) {
      var v = srcArray[i];
      dstArray[i] = v;
    }
  }

  @Benchmark
  public void vectAdd1() {
    var a = this.a;
    var b = this.b;
    var c = this.c;

    for (int i = 0; i < a.length; i += SPECIES.length()) {
      ByteVector av = ByteVector.fromArray(SPECIES, a, i);
      ByteVector bv = ByteVector.fromArray(SPECIES, b, i);
      av.lanewise(VectorOperators.ADD, bv).intoArray(c, i);
    }
  }

  @Benchmark
  public void vectAdd2() {
    var a = this.a;
    var b = this.b;
    var c = this.c;

    for (int i = 0; i < a.length/SPECIES.length(); i++) {
      ByteVector av = ByteVector.fromArray(SPECIES, a, (i*SPECIES.length()));
      ByteVector bv = ByteVector.fromArray(SPECIES, b, (i*SPECIES.length()));
      av.lanewise(VectorOperators.ADD, bv).intoArray(c, (i*SPECIES.length()));
    }
  }

  @Benchmark
  public void arrayAdd() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
      var v = ByteVector.fromArray(SPECIES, srcArray, i);
      v = v.add(v);
      v.intoArray(dstArray, i);
    }
  }

  @Benchmark
  public void bufferHeap() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
      var v = ByteVector.fromByteBuffer(SPECIES, srcBufferHeap, i, ByteOrder.nativeOrder());
      v.intoByteBuffer(dstBufferHeap, i, ByteOrder.nativeOrder());
    }
  }

  @Benchmark
  public void bufferHeapScalar() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i++) {
      var v = srcBufferHeap.get(i);
      dstBufferHeap.put(i, v);
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.PRINT)
  public void bufferNative() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
      var v = ByteVector.fromByteBuffer(SPECIES, srcBufferNative, i, ByteOrder.nativeOrder());
      v.intoByteBuffer(dstBufferNative, i, ByteOrder.nativeOrder());
    }
  }

  @Benchmark
  public void bufferNativeScalar() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i++) {
      var v = srcBufferNative.get(i);
      dstBufferNative.put(i, v);
    }
  }


  @Benchmark
  public void bufferSegmentImplicit() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
      var v = ByteVector.fromByteBuffer(SPECIES, srcBufferSegmentImplicit, i, ByteOrder.nativeOrder());
      v.intoByteBuffer(dstBufferSegmentImplicit, i, ByteOrder.nativeOrder());
    }
  }

  @Benchmark
  @CompilerControl(CompilerControl.Mode.PRINT)
  public void segmentImplicitScalar() {
    for (int i = 0; i < SPECIES.loopBound(srcArray.length); i++) {
      var v = MemoryAccess.getByteAtOffset(srcSegmentImplicit, i);
      MemoryAccess.setByteAtOffset(dstSegmentImplicit, i, v);
    }
  }

  @Benchmark
  public void bufferSegmentConfined() {
    try (final var scope = ResourceScope.newConfinedScope()) {
      final var srcBufferSegmentConfined = srcAddress.asSegment(size, scope).asByteBuffer();
      final var dstBufferSegmentConfined = dstAddress.asSegment(size, scope).asByteBuffer();

      for (int i = 0; i < SPECIES.loopBound(srcArray.length); i += SPECIES.length()) {
        var v = ByteVector.fromByteBuffer(SPECIES, srcBufferSegmentConfined, i, ByteOrder.nativeOrder());
        v.intoByteBuffer(dstBufferSegmentConfined, i, ByteOrder.nativeOrder());
      }
    }
  }
}
