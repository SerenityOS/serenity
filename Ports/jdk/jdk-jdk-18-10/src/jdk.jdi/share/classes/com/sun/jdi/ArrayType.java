/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jdi;

/**
 * Provides access to the class of an array and the type of
 * its components in the target VM.
 *
 * @see ArrayReference
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface ArrayType extends ReferenceType {

    /**
     * Creates a new instance of this array class in the target VM.
     * The array is created with the given length and each component
     * is initialized to is standard default value.
     *
     * @param length the number of components in the new array
     * @return the newly created {@link ArrayReference} mirroring
     * the new object in the target VM.
     *
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    ArrayReference newInstance(int length);

    /**
     * Gets the JNI signature of the components of this
     * array class. The signature
     * describes the declared type of the components. If the components
     * are objects, their actual type in a particular run-time context
     * may be a subclass of the declared class.
     *
     * @return a string containing the JNI signature of array components.
     */
    String componentSignature();

    /**
     * Returns a text representation of the component
     * type of this array.
     *
     * @return a text representation of the component type.
     */
    String componentTypeName();

    /**
     * Returns the component type of this array,
     * as specified in the array declaration.
     * <P>
     * Note: The component type of a array will always be
     * created or loaded before the array - see
     * <cite>The Java Virtual Machine Specification</cite>,
     * section 5.3.3 - Creating Array Classes.
     * However, although the component type will be loaded it may
     * not yet be prepared, in which case the type will be returned
     * but attempts to perform some operations on the returned type
     * (e.g. {@link ReferenceType#fields() fields()}) will throw
     * a {@link ClassNotPreparedException}.
     * Use {@link ReferenceType#isPrepared()} to determine if
     * a reference type is prepared.
     *
     * @see Type
     * @see Field#type() Field.type() - for usage examples
     * @return the {@link Type} of this array's components.
     */
    Type componentType() throws ClassNotLoadedException;
}
