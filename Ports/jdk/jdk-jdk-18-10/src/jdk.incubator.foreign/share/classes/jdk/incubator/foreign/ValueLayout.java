/*
 *  Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */
package jdk.incubator.foreign;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A value layout. A value layout is used to model the memory layout associated with values of basic data types, such as <em>integral</em> types
 * (either signed or unsigned) and <em>floating-point</em> types. Each value layout has a size and a byte order (see {@link ByteOrder}).
 *
 * <p>
 * This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 * The {@code equals} method should be used for comparisons.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 *
 * @implSpec
 * This class is immutable and thread-safe.
 */
public final class ValueLayout extends AbstractLayout implements MemoryLayout {

    private final ByteOrder order;

    ValueLayout(ByteOrder order, long size) {
        this(order, size, size, Map.of());
    }

    ValueLayout(ByteOrder order, long size, long alignment, Map<String, Constable> attributes) {
        super(OptionalLong.of(size), alignment, attributes);
        this.order = order;
    }

    /**
     * Returns the value's byte order.
     *
     * @return the value's  byte order.
     */
    public ByteOrder order() {
        return order;
    }

    /**
     * Returns a new value layout with given byte order.
     *
     * @param order the desired byte order.
     * @return a new value layout with given byte order.
     */
    public ValueLayout withOrder(ByteOrder order) {
        return new ValueLayout(Objects.requireNonNull(order), bitSize(), alignment, attributes);
    }

    @Override
    public String toString() {
        return decorateLayoutString(String.format("%s%d",
                order == ByteOrder.BIG_ENDIAN ? "B" : "b",
                bitSize()));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        if (!(other instanceof ValueLayout)) {
            return false;
        }
        ValueLayout v = (ValueLayout)other;
        return order.equals(v.order) &&
            bitSize() == v.bitSize() &&
            alignment == v.alignment;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), order, bitSize(), alignment);
    }

    @Override
    ValueLayout dup(long alignment, Map<String, Constable> attributes) {
        return new ValueLayout(order, bitSize(), alignment, attributes);
    }

    @Override
    public Optional<DynamicConstantDesc<ValueLayout>> describeConstable() {
        return Optional.of(decorateLayoutConstant(DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, "value",
                CD_VALUE_LAYOUT, MH_VALUE, bitSize(), order == ByteOrder.BIG_ENDIAN ? BIG_ENDIAN : LITTLE_ENDIAN)));
    }

    //hack: the declarations below are to make javadoc happy; we could have used generics in AbstractLayout
    //but that causes issues with javadoc, see JDK-8224052

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueLayout withName(String name) {
        return (ValueLayout)super.withName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueLayout withBitAlignment(long alignmentBits) {
        return (ValueLayout)super.withBitAlignment(alignmentBits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueLayout withAttribute(String name, Constable value) {
        return (ValueLayout)super.withAttribute(name, value);
    }
}
