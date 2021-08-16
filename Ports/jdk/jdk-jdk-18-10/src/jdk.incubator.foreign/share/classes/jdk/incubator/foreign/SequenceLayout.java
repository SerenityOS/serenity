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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * A sequence layout. A sequence layout is used to denote a repetition of a given layout, also called the sequence layout's <em>element layout</em>.
 * The repetition count, where it exists (e.g. for <em>finite</em> sequence layouts) is said to be the the sequence layout's <em>element count</em>.
 * A finite sequence layout can be thought of as a group layout where the sequence layout's element layout is repeated a number of times
 * that is equal to the sequence layout's element count. In other words this layout:
 *
 * <pre>{@code
MemoryLayout.sequenceLayout(3, MemoryLayout.valueLayout(32, ByteOrder.BIG_ENDIAN));
 * }</pre>
 *
 * is equivalent to the following layout:
 *
 * <pre>{@code
MemoryLayout.structLayout(
    MemoryLayout.valueLayout(32, ByteOrder.BIG_ENDIAN),
    MemoryLayout.valueLayout(32, ByteOrder.BIG_ENDIAN),
    MemoryLayout.valueLayout(32, ByteOrder.BIG_ENDIAN));
 * }</pre>
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
public final class SequenceLayout extends AbstractLayout implements MemoryLayout {

    private final OptionalLong elemCount;
    private final MemoryLayout elementLayout;

    SequenceLayout(OptionalLong elemCount, MemoryLayout elementLayout) {
        this(elemCount, elementLayout, elementLayout.bitAlignment(), Map.of());
    }

    SequenceLayout(OptionalLong elemCount, MemoryLayout elementLayout, long alignment, Map<String, Constable> attributes) {
        super(elemCount.isPresent() && AbstractLayout.optSize(elementLayout).isPresent() ?
                OptionalLong.of(elemCount.getAsLong() * elementLayout.bitSize()) :
                OptionalLong.empty(), alignment, attributes);
        this.elemCount = elemCount;
        this.elementLayout = elementLayout;
    }

    /**
     * Returns the element layout associated with this sequence layout.
     *
     * @return The element layout associated with this sequence layout.
     */
    public MemoryLayout elementLayout() {
        return elementLayout;
    }

    /**
     * Returns the element count of this sequence layout (if any).
     *
     * @return the element count of this sequence layout (if any).
     */
    public OptionalLong elementCount() {
        return elemCount;
    }

    /**
     * Obtains a new sequence layout with same element layout, alignment constraints and name as this sequence layout
     * but with the new specified element count.
     * @param elementCount the new element count.
     * @return a new sequence with given element count.
     * @throws IllegalArgumentException if {@code elementCount < 0}.
     */
    public SequenceLayout withElementCount(long elementCount) {
        AbstractLayout.checkSize(elementCount, true);
        return new SequenceLayout(OptionalLong.of(elementCount), elementLayout, alignment, attributes);
    }

    /**
     * Returns a new sequence layout where element layouts in the flattened projection of this
     * sequence layout (see {@link #flatten()}) are re-arranged into one or more nested sequence layouts
     * according to the provided element counts. This transformation preserves the layout size;
     * that is, multiplying the provided element counts must yield the same element count
     * as the flattened projection of this sequence layout.
     * <p>
     * For instance, given a sequence layout of the kind:
     * <pre>{@code
    var seq = MemoryLayout.sequenceLayout(4, MemoryLayout.sequenceLayout(3, MemoryLayouts.JAVA_INT));
     * }</pre>
     * calling {@code seq.reshape(2, 6)} will yield the following sequence layout:
     * <pre>{@code
    var reshapeSeq = MemoryLayout.sequenceLayout(2, MemoryLayout.sequenceLayout(6, MemoryLayouts.JAVA_INT));
     * }</pre>
     * <p>
     * If one of the provided element count is the special value {@code -1}, then the element
     * count in that position will be inferred from the remaining element counts and the
     * element count of the flattened projection of this layout. For instance, a layout equivalent to
     * the above {@code reshapeSeq} can also be computed in the following ways:
     * <pre>{@code
    var reshapeSeqImplicit1 = seq.reshape(-1, 6);
    var reshapeSeqImplicit2 = seq.reshape(2, -1);
     * }</pre>
     * @param elementCounts an array of element counts, of which at most one can be {@code -1}.
     * @return a new sequence layout where element layouts in the flattened projection of this
     * sequence layout (see {@link #flatten()}) are re-arranged into one or more nested sequence layouts.
     * @throws UnsupportedOperationException if this sequence layout does not have an element count.
     * @throws IllegalArgumentException if two or more element counts are set to {@code -1}, or if one
     * or more element count is {@code <= 0} (but other than {@code -1}) or, if, after any required inference,
     * multiplying the element counts does not yield the same element count as the flattened projection of this
     * sequence layout.
     */
    public SequenceLayout reshape(long... elementCounts) {
        Objects.requireNonNull(elementCounts);
        if (elementCounts.length == 0) {
            throw new IllegalArgumentException();
        }
        if (!elementCount().isPresent()) {
            throw new UnsupportedOperationException("Cannot reshape a sequence layout whose element count is unspecified");
        }
        SequenceLayout flat = flatten();
        long expectedCount = flat.elementCount().getAsLong();

        long actualCount = 1;
        int inferPosition = -1;
        for (int i = 0 ; i < elementCounts.length ; i++) {
            if (elementCounts[i] == -1) {
                if (inferPosition == -1) {
                    inferPosition = i;
                } else {
                    throw new IllegalArgumentException("Too many unspecified element counts");
                }
            } else if (elementCounts[i] <= 0) {
                throw new IllegalArgumentException("Invalid element count: " + elementCounts[i]);
            } else {
                actualCount = elementCounts[i] * actualCount;
            }
        }

        // infer an unspecified element count (if any)
        if (inferPosition != -1) {
            long inferredCount = expectedCount / actualCount;
            elementCounts[inferPosition] = inferredCount;
            actualCount = actualCount * inferredCount;
        }

        if (actualCount != expectedCount) {
            throw new IllegalArgumentException("Element counts do not match expected size: " + expectedCount);
        }

        MemoryLayout res = flat.elementLayout();
        for (int i = elementCounts.length - 1 ; i >= 0 ; i--) {
            res = MemoryLayout.sequenceLayout(elementCounts[i], res);
        }
        return (SequenceLayout)res;
    }

    /**
     * Returns a new, flattened sequence layout whose element layout is the first non-sequence
     * element layout found by recursively traversing the element layouts of this sequence layout.
     * This transformation preserves the layout size; nested sequence layout in this sequence layout will
     * be dropped and their element counts will be incorporated into that of the returned sequence layout.
     * For instance, given a sequence layout of the kind:
     * <pre>{@code
    var seq = MemoryLayout.sequenceLayout(4, MemoryLayout.sequenceLayout(3, MemoryLayouts.JAVA_INT));
     * }</pre>
     * calling {@code seq.flatten()} will yield the following sequence layout:
     * <pre>{@code
    var flattenedSeq = MemoryLayout.sequenceLayout(12, MemoryLayouts.JAVA_INT);
     * }</pre>
     * @return a new sequence layout with the same size as this layout (but, possibly, with different
     * element count), whose element layout is not a sequence layout.
     * @throws UnsupportedOperationException if this sequence layout, or one of the nested sequence layouts being
     * flattened, does not have an element count.
     */
    public SequenceLayout flatten() {
        if (!elementCount().isPresent()) {
            throw badUnboundSequenceLayout();
        }
        long count = elementCount().getAsLong();
        MemoryLayout elemLayout = elementLayout();
        while (elemLayout instanceof SequenceLayout) {
            SequenceLayout elemSeq = (SequenceLayout)elemLayout;
            count = count * elemSeq.elementCount().orElseThrow(this::badUnboundSequenceLayout);
            elemLayout = elemSeq.elementLayout();
        }
        return MemoryLayout.sequenceLayout(count, elemLayout);
    }

    private UnsupportedOperationException badUnboundSequenceLayout() {
        return new UnsupportedOperationException("Cannot flatten a sequence layout whose element count is unspecified");
    }

    @Override
    public String toString() {
        return decorateLayoutString(String.format("[%s:%s]",
                elemCount.isPresent() ? elemCount.getAsLong() : "", elementLayout));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        if (!(other instanceof SequenceLayout)) {
            return false;
        }
        SequenceLayout s = (SequenceLayout)other;
        return elemCount.equals(s.elemCount) && elementLayout.equals(s.elementLayout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), elemCount, elementLayout);
    }

    @Override
    SequenceLayout dup(long alignment, Map<String, Constable> attributes) {
        return new SequenceLayout(elementCount(), elementLayout, alignment, attributes);
    }

    @Override
    boolean hasNaturalAlignment() {
        return alignment == elementLayout.bitAlignment();
    }

    @Override
    public Optional<DynamicConstantDesc<SequenceLayout>> describeConstable() {
        return Optional.of(decorateLayoutConstant(elemCount.isPresent() ?
                DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, "value",
                        CD_SEQUENCE_LAYOUT, MH_SIZED_SEQUENCE, elemCount.getAsLong(), elementLayout.describeConstable().get()) :
                DynamicConstantDesc.ofNamed(ConstantDescs.BSM_INVOKE, "value",
                        CD_SEQUENCE_LAYOUT, MH_UNSIZED_SEQUENCE, elementLayout.describeConstable().get())));
    }

    //hack: the declarations below are to make javadoc happy; we could have used generics in AbstractLayout
    //but that causes issues with javadoc, see JDK-8224052

    /**
     * {@inheritDoc}
     */
    @Override
    public SequenceLayout withName(String name) {
        return (SequenceLayout)super.withName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequenceLayout withBitAlignment(long alignmentBits) {
        return (SequenceLayout)super.withBitAlignment(alignmentBits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequenceLayout withAttribute(String name, Constable value) {
        return (SequenceLayout)super.withAttribute(name, value);
    }
}
