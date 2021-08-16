/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */
package jdk.incubator.foreign;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DynamicConstantDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A function descriptor is made up of zero or more argument layouts and zero or one return layout. A function descriptor
 * is used to model the signature of foreign functions.
 *
 * <p> Unless otherwise specified, passing a {@code null} argument, or an array argument containing one or more {@code null}
 * elements to a method in this class causes a {@link NullPointerException NullPointerException} to be thrown. </p>
 */
public final class FunctionDescriptor implements Constable {

    /**
     * The name of the function descriptor attribute (see {@link #attributes()} used to mark trivial functions. The
     * attribute value must be a boolean.
     */
    public static final String TRIVIAL_ATTRIBUTE_NAME = "abi/trivial";

    private final MemoryLayout resLayout;
    private final MemoryLayout[] argLayouts;
    private final Map<String, Constable> attributes;

    private FunctionDescriptor(MemoryLayout resLayout, Map<String, Constable> attributes, MemoryLayout... argLayouts) {
        this.resLayout = resLayout;
        this.attributes = attributes;
        this.argLayouts = argLayouts;
    }

    /**
     * Returns the attribute with the given name (if it exists).
     *
     * @param name the attribute name.
     * @return the attribute with the given name (if it exists).
     */
    public Optional<Constable> attribute(String name) {
        Objects.requireNonNull(name);
        return Optional.ofNullable(attributes.get(name));
    }

    /**
     * Returns a stream of the attribute names associated with this function descriptor.
     *
     * @return a stream of the attribute names associated with this function descriptor.
     */
    public Stream<String> attributes() {
        return attributes.keySet().stream();
    }

    /**
     * Returns a new function descriptor which features the same attributes as this descriptor, plus the newly specified attribute.
     * If this descriptor already contains an attribute with the same name, the existing attribute value is overwritten in the returned
     * descriptor.
     *
     * @param name the attribute name.
     * @param value the attribute value.
     * @return a new function descriptor which features the same attributes as this descriptor, plus the newly specified attribute.
     */
    public FunctionDescriptor withAttribute(String name, Constable value) {
        Objects.requireNonNull(name);
        Map<String, Constable> newAttributes = new HashMap<>(attributes);
        newAttributes.put(name, value);
        return new FunctionDescriptor(resLayout, newAttributes, argLayouts);
    }

    /**
     * Returns the return layout associated with this function.
     * @return the return layout.
     */
    public Optional<MemoryLayout> returnLayout() {
        return Optional.ofNullable(resLayout);
    }

    /**
     * Returns the argument layouts associated with this function.
     * @return the argument layouts.
     */
    public List<MemoryLayout> argumentLayouts() {
        return Arrays.asList(argLayouts);
    }

    /**
     * Create a function descriptor with given return and argument layouts.
     * @param resLayout the return layout.
     * @param argLayouts the argument layouts.
     * @return the new function descriptor.
     */
    public static FunctionDescriptor of(MemoryLayout resLayout, MemoryLayout... argLayouts) {
        Objects.requireNonNull(resLayout);
        Objects.requireNonNull(argLayouts);
        Arrays.stream(argLayouts).forEach(Objects::requireNonNull);
        return new FunctionDescriptor(resLayout, Map.of(), argLayouts);
    }

    /**
     * Create a function descriptor with given argument layouts and no return layout.
     * @param argLayouts the argument layouts.
     * @return the new function descriptor.
     */
    public static FunctionDescriptor ofVoid(MemoryLayout... argLayouts) {
        Objects.requireNonNull(argLayouts);
        Arrays.stream(argLayouts).forEach(Objects::requireNonNull);
        return new FunctionDescriptor(null, Map.of(), argLayouts);
    }

    /**
     * Create a new function descriptor with the given argument layouts appended to the argument layout array
     * of this function descriptor.
     * @param addedLayouts the argument layouts to append.
     * @return the new function descriptor.
     */
    public FunctionDescriptor withAppendedArgumentLayouts(MemoryLayout... addedLayouts) {
        Objects.requireNonNull(addedLayouts);
        Arrays.stream(addedLayouts).forEach(Objects::requireNonNull);
        MemoryLayout[] newLayouts = Arrays.copyOf(argLayouts, argLayouts.length + addedLayouts.length);
        System.arraycopy(addedLayouts, 0, newLayouts, argLayouts.length, addedLayouts.length);
        return new FunctionDescriptor(resLayout, attributes, newLayouts);
    }

    /**
     * Create a new function descriptor with the given memory layout as the new return layout.
     * @param newReturn the new return layout.
     * @return the new function descriptor.
     */
    public FunctionDescriptor withReturnLayout(MemoryLayout newReturn) {
        Objects.requireNonNull(newReturn);
        return new FunctionDescriptor(newReturn, attributes, argLayouts);
    }

    /**
     * Create a new function descriptor with the return layout dropped.
     * @return the new function descriptor.
     */
    public FunctionDescriptor withVoidReturnLayout() {
        return new FunctionDescriptor(null, attributes, argLayouts);
    }

    /**
     * Returns a string representation of this function descriptor.
     * @return a string representation of this function descriptor.
     */
    @Override
    public String toString() {
        return String.format("(%s)%s",
                Stream.of(argLayouts)
                        .map(Object::toString)
                        .collect(Collectors.joining()),
                returnLayout().map(Object::toString).orElse("v"));
    }

    /**
     * Compares the specified object with this function descriptor for equality. Returns {@code true} if and only if the specified
     * object is also a function descriptor, and all of the following conditions are met:
     * <ul>
     *     <li>the two function descriptors have equals return layouts (see {@link MemoryLayout#equals(Object)}), or both have no return layout</li>
     *     <li>the two function descriptors have argument layouts that are pair-wise equal (see {@link MemoryLayout#equals(Object)})
     * </ul>
     *
     * @param other the object to be compared for equality with this function descriptor.
     * @return {@code true} if the specified object is equal to this function descriptor.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FunctionDescriptor)) {
            return false;
        }
        FunctionDescriptor f = (FunctionDescriptor) other;
        return Objects.equals(resLayout, f.resLayout) && Arrays.equals(argLayouts, f.argLayouts);
    }

    /**
     * Returns the hash code value for this function descriptor.
     * @return the hash code value for this function descriptor.
     */
    @Override
    public int hashCode() {
        int hashCode = Arrays.hashCode(argLayouts);
        return resLayout == null ? hashCode : resLayout.hashCode() ^ hashCode;
    }

    @Override
    public Optional<DynamicConstantDesc<FunctionDescriptor>> describeConstable() {
        List<ConstantDesc> constants = new ArrayList<>();
        constants.add(resLayout == null ? AbstractLayout.MH_VOID_FUNCTION : AbstractLayout.MH_FUNCTION);
        if (resLayout != null) {
            constants.add(resLayout.describeConstable().get());
        }
        for (MemoryLayout argLayout : argLayouts) {
            constants.add(argLayout.describeConstable().get());
        }
        return Optional.of(DynamicConstantDesc.ofNamed(
                    ConstantDescs.BSM_INVOKE, "function", AbstractLayout.CD_FUNCTION_DESC, constants.toArray(new ConstantDesc[0])));
    }
}
