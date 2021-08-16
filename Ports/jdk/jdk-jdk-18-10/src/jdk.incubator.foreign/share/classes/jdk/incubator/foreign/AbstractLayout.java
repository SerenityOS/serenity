/*
 *  Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.constant.ClassDesc;
import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicConstantDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.constant.ConstantDescs.BSM_GET_STATIC_FINAL;
import static java.lang.constant.ConstantDescs.BSM_INVOKE;
import static java.lang.constant.ConstantDescs.CD_String;
import static java.lang.constant.ConstantDescs.CD_long;

abstract non-sealed class AbstractLayout implements MemoryLayout {

    private final OptionalLong size;
    final long alignment;
    final Map<String, Constable> attributes;

    public AbstractLayout(OptionalLong size, long alignment, Map<String, Constable> attributes) {
        this.size = size;
        this.alignment = alignment;
        this.attributes = Collections.unmodifiableMap(attributes);
    }

    @Override
    public AbstractLayout withName(String name) {
        Objects.requireNonNull(name);
        return withAttribute(LAYOUT_NAME, name);
    }

    @Override
    public final Optional<String> name() {
        return attribute(LAYOUT_NAME).map(String.class::cast);
    }

    @Override
    public Optional<Constable> attribute(String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(attributes.get(name));
    }

    @Override
    public Stream<String> attributes() {
        return attributes.keySet().stream();
    }

    @Override
    public AbstractLayout withAttribute(String name, Constable value) {
        Objects.requireNonNull(name);
        Map<String, Constable> newAttributes = new HashMap<>(attributes);
        newAttributes.put(name, value);
        return dup(alignment, newAttributes);
    }

    abstract AbstractLayout dup(long alignment, Map<String, Constable> annos);

    @Override
    public AbstractLayout withBitAlignment(long alignmentBits) {
        checkAlignment(alignmentBits);
        return dup(alignmentBits, attributes);
    }

    void checkAlignment(long alignmentBitCount) {
        if (((alignmentBitCount & (alignmentBitCount - 1)) != 0L) || //alignment must be a power of two
                (alignmentBitCount < 8)) { //alignment must be greater than 8
            throw new IllegalArgumentException("Invalid alignment: " + alignmentBitCount);
        }
    }

    static void checkSize(long size) {
        checkSize(size, false);
    }

    static void checkSize(long size, boolean includeZero) {
        if (size < 0 || (!includeZero && size == 0)) {
            throw new IllegalArgumentException("Invalid size for layout: " + size);
        }
    }

    @Override
    public final long bitAlignment() {
        return alignment;
    }

    @Override
    public boolean hasSize() {
        return size.isPresent();
    }

    @Override
    public long bitSize() {
        return size.orElseThrow(AbstractLayout::badSizeException);
    }

    static OptionalLong optSize(MemoryLayout layout) {
        return ((AbstractLayout)layout).size;
    }

    private static UnsupportedOperationException badSizeException() {
        return new UnsupportedOperationException("Cannot compute size of a layout which is, or depends on a sequence layout with unspecified size");
    }

    String decorateLayoutString(String s) {
        if (name().isPresent()) {
            s = String.format("%s(%s)", s, name().get());
        }
        if (!hasNaturalAlignment()) {
            s = alignment + "%" + s;
        }
        if (!attributes.isEmpty()) {
            s += attributes.entrySet().stream()
                                      .map(e -> e.getKey() + "=" + e.getValue())
                                      .collect(Collectors.joining(",", "[", "]"));
        }
        return s;
    }

    <T> DynamicConstantDesc<T> decorateLayoutConstant(DynamicConstantDesc<T> desc) {
        if (!hasNaturalAlignment()) {
            desc = DynamicConstantDesc.ofNamed(BSM_INVOKE, "withBitAlignment", desc.constantType(), MH_WITH_BIT_ALIGNMENT,
                    desc, bitAlignment());
        }
        for (var e : attributes.entrySet()) {
            desc = DynamicConstantDesc.ofNamed(BSM_INVOKE, "withAttribute", desc.constantType(), MH_WITH_ATTRIBUTE,
                    desc, e.getKey(), e.getValue().describeConstable().orElseThrow());
        }

        return desc;
    }

    boolean hasNaturalAlignment() {
        return size.isPresent() && size.getAsLong() == alignment;
    }

    @Override
    public boolean isPadding() {
        return this instanceof PaddingLayout;
    }

    @Override
    public int hashCode() {
        return attributes.hashCode() << Long.hashCode(alignment);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof AbstractLayout)) {
            return false;
        }

        return Objects.equals(attributes, ((AbstractLayout) other).attributes) &&
                Objects.equals(alignment, ((AbstractLayout) other).alignment);
    }

    /*** Helper constants for implementing Layout::describeConstable ***/

    static final ClassDesc CD_MEMORY_LAYOUT = MemoryLayout.class.describeConstable().get();

    static final ClassDesc CD_VALUE_LAYOUT = ValueLayout.class.describeConstable().get();

    static final ClassDesc CD_SEQUENCE_LAYOUT = SequenceLayout.class.describeConstable().get();

    static final ClassDesc CD_GROUP_LAYOUT = GroupLayout.class.describeConstable().get();

    static final ClassDesc CD_BYTEORDER = ByteOrder.class.describeConstable().get();

    static final ClassDesc CD_FUNCTION_DESC = FunctionDescriptor.class.describeConstable().get();

    static final ClassDesc CD_Constable = Constable.class.describeConstable().get();

    static final ConstantDesc BIG_ENDIAN = DynamicConstantDesc.ofNamed(BSM_GET_STATIC_FINAL, "BIG_ENDIAN", CD_BYTEORDER, CD_BYTEORDER);

    static final ConstantDesc LITTLE_ENDIAN = DynamicConstantDesc.ofNamed(BSM_GET_STATIC_FINAL, "LITTLE_ENDIAN", CD_BYTEORDER, CD_BYTEORDER);

    static final MethodHandleDesc MH_PADDING = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, CD_MEMORY_LAYOUT, "paddingLayout",
                MethodTypeDesc.of(CD_MEMORY_LAYOUT, CD_long));

    static final MethodHandleDesc MH_VALUE = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, CD_MEMORY_LAYOUT, "valueLayout",
                MethodTypeDesc.of(CD_VALUE_LAYOUT, CD_long, CD_BYTEORDER));

    static final MethodHandleDesc MH_SIZED_SEQUENCE = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, CD_MEMORY_LAYOUT, "sequenceLayout",
                MethodTypeDesc.of(CD_SEQUENCE_LAYOUT, CD_long, CD_MEMORY_LAYOUT));

    static final MethodHandleDesc MH_UNSIZED_SEQUENCE = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, CD_MEMORY_LAYOUT, "sequenceLayout",
                MethodTypeDesc.of(CD_SEQUENCE_LAYOUT, CD_MEMORY_LAYOUT));

    static final MethodHandleDesc MH_STRUCT = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, CD_MEMORY_LAYOUT, "structLayout",
                MethodTypeDesc.of(CD_GROUP_LAYOUT, CD_MEMORY_LAYOUT.arrayType()));

    static final MethodHandleDesc MH_UNION = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_STATIC, CD_MEMORY_LAYOUT, "unionLayout",
                MethodTypeDesc.of(CD_GROUP_LAYOUT, CD_MEMORY_LAYOUT.arrayType()));

    static final MethodHandleDesc MH_VOID_FUNCTION = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, CD_FUNCTION_DESC, "ofVoid",
                MethodTypeDesc.of(CD_FUNCTION_DESC, CD_MEMORY_LAYOUT.arrayType()));

    static final MethodHandleDesc MH_FUNCTION = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.STATIC, CD_FUNCTION_DESC, "of",
                MethodTypeDesc.of(CD_FUNCTION_DESC, CD_MEMORY_LAYOUT, CD_MEMORY_LAYOUT.arrayType()));

    static final MethodHandleDesc MH_WITH_BIT_ALIGNMENT = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, CD_MEMORY_LAYOUT, "withBitAlignment",
                MethodTypeDesc.of(CD_MEMORY_LAYOUT, CD_long));

    static final MethodHandleDesc MH_WITH_ATTRIBUTE = MethodHandleDesc.ofMethod(DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL, CD_MEMORY_LAYOUT, "withAttribute",
                MethodTypeDesc.of(CD_MEMORY_LAYOUT, CD_String, CD_Constable));
}
