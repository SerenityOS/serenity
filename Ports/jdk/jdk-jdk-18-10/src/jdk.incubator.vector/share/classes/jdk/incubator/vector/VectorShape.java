/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
package jdk.incubator.vector;

import jdk.internal.vm.annotation.ForceInline;
import jdk.internal.vm.annotation.Stable;

import jdk.internal.vm.vector.VectorSupport;

/**
 * A {@code VectorShape} selects a particular implementation of
 * {@link Vector}s.
 *
 * A shape in combination with the element type determines
 * a particular
 * {@linkplain VectorSpecies#of(Class,VectorShape) vector species}
 * object.
 *
 * @apiNote
 * Because not all shapes are supported by all platforms,
 * shape-agnostic code is more portable.
 * User code that selects particular shapes may
 * fail to run, or run slowly, on some platforms.
 * Use
 * {@link VectorShape#preferredShape() VectorShape.preferredShape()}
 * and
 * {@link VectorSpecies#ofPreferred(Class) VectorSpecies.ofPreferred()}
 * to select the shape that is usually preferable for
 * most uses.
 */
public enum VectorShape {
    /** Shape of length 64 bits */
    S_64_BIT(64),
    /** Shape of length 128 bits */
    S_128_BIT(128),
    /** Shape of length 256 bits */
    S_256_BIT(256),
    /** Shape of length 512 bits */
    S_512_BIT(512),
    /** Shape of maximum length supported on the platform */
    S_Max_BIT(getMaxVectorBitSize(byte.class));

    final int vectorBitSize;
    final int vectorBitSizeLog2;
    final int switchKey;  // 1+ordinal(), which is non-zero

    VectorShape(int vectorBitSize) {
        this.switchKey = 1+ordinal();
        this.vectorBitSize = vectorBitSize;
        this.vectorBitSizeLog2 = Integer.numberOfTrailingZeros(vectorBitSize);
    }

    /**
     * Returns the size, in bits, of vectors of this shape.
     *
     * @return the size, in bits, of vectors of this shape.
     */
    @ForceInline
    public int vectorBitSize() {
        return vectorBitSize;
    }

    /**
     * Return the number of lanes of a vector of this shape and whose element
     * type is of the provided species
     *
     * @param species the species describing the element type
     * @return the number of lanes
     */
    /*package-private*/
    int laneCount(VectorSpecies<?> species) {
        return vectorBitSize() / species.elementSize();
    }

    /**
     * Finds a vector species with the given element type
     * and the current shape.
     * Returns the same value as
     * {@code VectorSpecies.of(elementType, this)}.

     * @param elementType the required element type
     * @param <E> the boxed element type
     * @return a species for the given element type and this shape
     * @see VectorSpecies#of(Class, VectorShape)
     */
    public <E>
    VectorSpecies<E> withLanes(Class<E> elementType) {
        return VectorSpecies.of(elementType, this);
    }

    // Max vector size and increments for local use.
    static final int
            MAX_VECTOR_SIZE   = 2048,
            INC_VECTOR_SIZE   = 128;

    /**
     * Finds an appropriate shape depending on the
     * proposed bit-size of a vector.
     *
     * @param bitSize the proposed vector size in bits
     * @return a shape corresponding to the vector bit-size
     * @throws IllegalArgumentException if no such vector shape exists
     * @see #vectorBitSize()
     */
    public static VectorShape forBitSize(int bitSize) {
        switch (bitSize) {
            case 64:
                return VectorShape.S_64_BIT;
            case 128:
                return VectorShape.S_128_BIT;
            case 256:
                return VectorShape.S_256_BIT;
            case 512:
                return VectorShape.S_512_BIT;
            default:
                if ((bitSize > 0) && (bitSize <= MAX_VECTOR_SIZE) && (bitSize % INC_VECTOR_SIZE == 0)) {
                    return VectorShape.S_Max_BIT;
                } else {
                    throw new IllegalArgumentException("Bad vector bit-size: " + bitSize);
                }
        }
    }

    /**
     * Finds an appropriate index shape depending on the
     * proposed index-bit-size and element-size of a vector.
     *
     * @param indexBitSize the proposed index vector size in bits
     * @param elementSize the proposed element size in bits
     * @return a shape corresponding to the index vector bit-size
     * and element size.
     * @throws IllegalArgumentException if no such vector shape exists
     * @see #vectorBitSize()
     */
    public static VectorShape forIndexBitSize(int indexBitSize, int elementSize) {
        switch (indexBitSize) {
            case 32:
            case 64:
                return VectorShape.S_64_BIT;
            case 128:
                return VectorShape.S_128_BIT;
            case 256:
                return VectorShape.S_256_BIT;
            case 512:
                return VectorShape.S_512_BIT;
            default:
                // Calculate the maximum of index bit size and step under given element size.
                // For AArch64 SVE, vector size is from 128 to 2048 bits, in increments of 128.
                // For X86, vector size is 64, 128, 256 or 512.
                int maxBitSize = (MAX_VECTOR_SIZE/elementSize) * Integer.SIZE;
                int step = (INC_VECTOR_SIZE/elementSize) * Integer.SIZE;
                if ((indexBitSize > 0) && (indexBitSize <= maxBitSize) && (indexBitSize % step == 0)) {
                    return VectorShape.S_Max_BIT;
                } else {
                    throw new IllegalArgumentException("Bad index vector bit-size: " + indexBitSize);
                }
        }
    }

    // Switch keys for local use.
    // We need these because switches keyed on enums
    // don't optimize properly; see JDK-8161245
    static final int
        SK_64_BIT   = 1,
        SK_128_BIT  = 2,
        SK_256_BIT  = 3,
        SK_512_BIT  = 4,
        SK_Max_BIT  = 5,
        SK_LIMIT    = 6;

    /*package-private*/
    static VectorShape ofSwitchKey(int sk) {
        switch (sk) {
            case SK_64_BIT:     return S_64_BIT;
            case SK_128_BIT:    return S_128_BIT;
            case SK_256_BIT:    return S_256_BIT;
            case SK_512_BIT:    return S_512_BIT;
            case SK_Max_BIT:    return S_Max_BIT;
        }
        throw new AssertionError();
    }
    static {
        for (VectorShape vs : values()) {
            assert(ofSwitchKey(vs.switchKey) == vs);
        }
    }

    // non-public support for computing preferred shapes

    /*package-private*/
    static VectorShape largestShapeFor(Class<?> etype) {
        return VectorShape.forBitSize(getMaxVectorBitSize(etype));
    }

    /**
     * Finds the vector shape preferred by the current platform
     * for all vector element types.
     * <p>
     * The preferred shape by the platform has the largest possible
     * bit-size, under the constraint that all lane sizes are
     * supported, from {@code byte} to {@code double}.  Thus, all the
     * {@linkplain VectorSpecies#ofPreferred(Class) preferred vector species}
     * for various lane types will have a common underlying shape.
     *
     * @return a preferred shape for all element types
     * @throws UnsupportedOperationException if no such shape exists
     * @see VectorSpecies#ofPreferred(Class)
     */
    @ForceInline
    public static VectorShape preferredShape() {
        VectorShape shape = PREFERRED_SHAPE;
        if (shape != null) {
            return shape;
        }
        return computePreferredShape();
    }

    private static VectorShape computePreferredShape() {
        int prefBitSize = Integer.MAX_VALUE;
        for (LaneType type : LaneType.values()) {
            Class<?> etype = type.elementType;
            prefBitSize = Math.min(prefBitSize, getMaxVectorBitSize(etype));
        }
        // If these assertions fail, we must reconsider our API portability assumptions.
        assert(prefBitSize >= Double.SIZE && prefBitSize < Integer.MAX_VALUE / Long.SIZE);
        assert(prefBitSize == getMaxVectorBitSize(byte.class));
        VectorShape shape = VectorShape.forBitSize(prefBitSize);
        PREFERRED_SHAPE = shape;
        return shape;
    }

    /**
     * Returns the maximum vector bit size for a given element type.
     *
     * @param etype the element type.
     * @return the maximum vector bit.
     */
     /*package-private*/
    static int getMaxVectorBitSize(Class<?> etype) {
        // VectorSupport.getMaxLaneCount may return -1 if C2 is not enabled,
        // or a value smaller than the S_64_BIT.vectorBitSize / elementSizeInBits if MaxVectorSize < 16
        // If so default to S_64_BIT
        int maxLaneCount = VectorSupport.getMaxLaneCount(etype);
        int elementSizeInBits = LaneType.of(etype).elementSize;
        return Math.max(maxLaneCount * elementSizeInBits, S_64_BIT.vectorBitSize);
    }

    private static @Stable VectorShape PREFERRED_SHAPE;

    // ==== JROSE NAME CHANGES ====

    // MOVED preferredShape() back into this file

}
