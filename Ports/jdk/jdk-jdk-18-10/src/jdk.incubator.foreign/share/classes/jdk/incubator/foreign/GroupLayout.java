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
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.LongBinaryOperator;
import java.util.stream.Collectors;

/**
 * A group layout is used to combine together multiple <em>member layouts</em>. There are two ways in which member layouts
 * can be combined: if member layouts are laid out one after the other, the resulting group layout is said to be a <em>struct</em>
 * (see {@link MemoryLayout#structLayout(MemoryLayout...)}); conversely, if all member layouts are laid out at the same starting offset,
 * the resulting group layout is said to be a <em>union</em> (see {@link MemoryLayout#unionLayout(MemoryLayout...)}).
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
public final class GroupLayout extends AbstractLayout implements MemoryLayout {

    /**
     * The group kind.
     */
    enum Kind {
        /**
         * A 'struct' kind.
         */
        STRUCT("", MH_STRUCT, Long::sum),
        /**
         * A 'union' kind.
         */
        UNION("|", MH_UNION, Math::max);

        final String delimTag;
        final MethodHandleDesc mhDesc;
        final LongBinaryOperator sizeOp;

        Kind(String delimTag, MethodHandleDesc mhDesc, LongBinaryOperator sizeOp) {
            this.delimTag = delimTag;
            this.mhDesc = mhDesc;
            this.sizeOp = sizeOp;
        }

        OptionalLong sizeof(List<MemoryLayout> elems) {
            long size = 0;
            for (MemoryLayout elem : elems) {
                if (AbstractLayout.optSize(elem).isPresent()) {
                    size = sizeOp.applyAsLong(size, elem.bitSize());
                } else {
                    return OptionalLong.empty();
                }
            }
            return OptionalLong.of(size);
        }

        long alignof(List<MemoryLayout> elems) {
            return elems.stream().mapToLong(MemoryLayout::bitAlignment).max() // max alignment in case we have member layouts
                    .orElse(1); // or minimal alignment if no member layout is given
        }
    }

    private final Kind kind;
    private final List<MemoryLayout> elements;

    GroupLayout(Kind kind, List<MemoryLayout> elements) {
        this(kind, elements, kind.alignof(elements), Map.of());
    }

    GroupLayout(Kind kind, List<MemoryLayout> elements, long alignment, Map<String, Constable> attributes) {
        super(kind.sizeof(elements), alignment, attributes);
        this.kind = kind;
        this.elements = elements;
    }

    /**
     * Returns the member layouts associated with this group.
     *
     * @apiNote the order in which member layouts are returned is the same order in which member layouts have
     * been passed to one of the group layout factory methods (see {@link MemoryLayout#structLayout(MemoryLayout...)},
     * {@link MemoryLayout#unionLayout(MemoryLayout...)}).
     *
     * @return the member layouts associated with this group.
     */
    public List<MemoryLayout> memberLayouts() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public String toString() {
        return decorateLayoutString(elements.stream()
                .map(Object::toString)
                .collect(Collectors.joining(kind.delimTag, "[", "]")));
    }

    /**
     * Is this group layout a <em>struct</em>?
     *
     * @return true, if this group layout is a <em>struct</em>.
     */
    public boolean isStruct() {
        return kind == Kind.STRUCT;
    }

    /**
     * Is this group layout a <em>union</em>?
     *
     * @return true, if this group layout is a <em>union</em>.
     */
    public boolean isUnion() {
        return kind == Kind.UNION;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!super.equals(other)) {
            return false;
        }
        if (!(other instanceof GroupLayout)) {
            return false;
        }
        GroupLayout g = (GroupLayout)other;
        return kind.equals(g.kind) && elements.equals(g.elements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), kind, elements);
    }

    @Override
    GroupLayout dup(long alignment, Map<String, Constable> attributes) {
        return new GroupLayout(kind, elements, alignment, attributes);
    }

    @Override
    boolean hasNaturalAlignment() {
        return alignment == kind.alignof(elements);
    }

    @Override
    public Optional<DynamicConstantDesc<GroupLayout>> describeConstable() {
        ConstantDesc[] constants = new ConstantDesc[1 + elements.size()];
        constants[0] = kind.mhDesc;
        for (int i = 0 ; i < elements.size() ; i++) {
            constants[i + 1] = elements.get(i).describeConstable().get();
        }
        return Optional.of(decorateLayoutConstant(DynamicConstantDesc.ofNamed(
                    ConstantDescs.BSM_INVOKE, kind.name().toLowerCase(),
                CD_GROUP_LAYOUT, constants)));
    }

    //hack: the declarations below are to make javadoc happy; we could have used generics in AbstractLayout
    //but that causes issues with javadoc, see JDK-8224052

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupLayout withName(String name) {
        return (GroupLayout)super.withName(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupLayout withBitAlignment(long alignmentBits) {
        return (GroupLayout)super.withBitAlignment(alignmentBits);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GroupLayout withAttribute(String name, Constable value) {
        return (GroupLayout)super.withAttribute(name, value);
    }
}
