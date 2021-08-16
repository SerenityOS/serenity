/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.List;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.Method;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class ArrayReferenceImpl extends ObjectReferenceImpl
    implements ArrayReference
{
    int length = -1;

    ArrayReferenceImpl(VirtualMachine aVm, long aRef) {
        super(aVm, aRef);
    }

    protected ClassTypeImpl invokableReferenceType(Method method) {
        // The method has to be a method on Object since
        // arrays don't have methods nor any other 'superclasses'
        // So, use the ClassTypeImpl for Object instead of
        // the ArrayTypeImpl for the array itself.
        return (ClassTypeImpl)method.declaringType();
    }

    ArrayTypeImpl arrayType() {
        return (ArrayTypeImpl)type();
    }

    /**
     * Return array length.
     * Need not be synchronized since it cannot be provably stale.
     */
    public int length() {
        if(length == -1) {
            try {
                length = JDWP.ArrayReference.Length.
                    process(vm, this).arrayLength;
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        }
        return length;
    }

    public Value getValue(int index) {
        List<Value> list = getValues(index, 1);
        return list.get(0);
    }

    public List<Value> getValues() {
        return getValues(0, -1);
    }

    /**
     * Validate that the range to set/get is valid.
     * length of -1 (meaning rest of array) has been converted
     * before entry.
     */
    private void validateArrayAccess(int index, int length) {
        // because length can be computed from index,
        // index must be tested first for correct error message
        if ((index < 0) || (index > length())) {
            throw new IndexOutOfBoundsException(
                        "Invalid array index: " + index);
        }
        if (length < 0) {
            throw new IndexOutOfBoundsException(
                        "Invalid array range length: " + length);
        }
        if (index + length > length()) {
            throw new IndexOutOfBoundsException(
                        "Invalid array range: " +
                        index + " to " + (index + length - 1));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T cast(Object x) {
        return (T)x;
    }

    public List<Value> getValues(int index, int length) {
        if (length == -1) { // -1 means the rest of the array
           length = length() - index;
        }
        validateArrayAccess(index, length);
        if (length == 0) {
            return new ArrayList<Value>();
        }

        List<Value> vals;
        try {
            vals = cast(JDWP.ArrayReference.GetValues.process(vm, this, index, length).values);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }

        return vals;
    }

    public void setValue(int index, Value value)
            throws InvalidTypeException,
                   ClassNotLoadedException {
        List<Value> list = new ArrayList<Value>(1);
        list.add(value);
        setValues(index, list, 0, 1);
    }

    public void setValues(List<? extends Value> values)
            throws InvalidTypeException,
                   ClassNotLoadedException {
        setValues(0, values, 0, -1);
    }

    public void setValues(int index, List<? extends Value> values,
                          int srcIndex, int length)
            throws InvalidTypeException,
                   ClassNotLoadedException {

        if (length == -1) { // -1 means the rest of the array
            // shorter of, the rest of the array and rest of
            // the source values
            length = Math.min(length() - index,
                              values.size() - srcIndex);
        }
        validateMirrorsOrNulls(values);
        validateArrayAccess(index, length);

        if ((srcIndex < 0) || (srcIndex > values.size())) {
            throw new IndexOutOfBoundsException(
                        "Invalid source index: " + srcIndex);
        }
        if (srcIndex + length > values.size()) {
            throw new IndexOutOfBoundsException(
                        "Invalid source range: " +
                        srcIndex + " to " +
                        (srcIndex + length - 1));
        }

        boolean somethingToSet = false;;
        ValueImpl[] setValues = new ValueImpl[length];

        for (int i = 0; i < length; i++) {
            ValueImpl value = (ValueImpl)values.get(srcIndex + i);

            try {
                // Validate and convert if necessary
                setValues[i] =
                  ValueImpl.prepareForAssignment(value,
                                                 new Component());
                somethingToSet = true;
            } catch (ClassNotLoadedException e) {
                /*
                 * Since we got this exception,
                 * the component must be a reference type.
                 * This means the class has not yet been loaded
                 * through the defining class's class loader.
                 * If the value we're trying to set is null,
                 * then setting to null is essentially a
                 * no-op, and we should allow it without an
                 * exception.
                 */
                if (value != null) {
                    throw e;
                }
            }
        }
        if (somethingToSet) {
            try {
                JDWP.ArrayReference.SetValues.
                    process(vm, this, index, setValues);
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        }
    }

    public String toString() {
        return "instance of " + arrayType().componentTypeName() +
               "[" + length() + "] (id=" + uniqueID() + ")";
    }

    byte typeValueKey() {
        return JDWP.Tag.ARRAY;
    }

    void validateAssignment(ValueContainer destination)
                            throws InvalidTypeException, ClassNotLoadedException {
        try {
            super.validateAssignment(destination);
        } catch (ClassNotLoadedException e) {
            /*
             * An array can be used extensively without the
             * enclosing loader being recorded by the VM as an
             * initiating loader of the array type. In addition, the
             * load of an array class is fairly harmless as long as
             * the component class is already loaded. So we relax the
             * rules a bit and allow the assignment as long as the
             * ultimate component types are assignable.
             */
            boolean valid = false;
            JNITypeParser destParser = new JNITypeParser(
                                       destination.signature());
            JNITypeParser srcParser = new JNITypeParser(
                                       arrayType().signature());
            int destDims = destParser.dimensionCount();
            if (destDims <= srcParser.dimensionCount()) {
                /*
                 * Remove all dimensions from the destination. Remove
                 * the same number of dimensions from the source.
                 * Get types for both and check to see if they are
                 * compatible.
                 */
                String destComponentSignature =
                    destParser.componentSignature(destDims);
                Type destComponentType =
                    destination.findType(destComponentSignature);
                String srcComponentSignature =
                    srcParser.componentSignature(destDims);
                Type srcComponentType =
                    arrayType().findType(srcComponentSignature);
                valid = ArrayTypeImpl.isComponentAssignable(destComponentType,
                                                          srcComponentType);
            }

            if (!valid) {
                throw new InvalidTypeException("Cannot assign " +
                                               arrayType().name() +
                                               " to " +
                                               destination.typeName());
            }
        }
    }

    /*
     * Represents an array component to other internal parts of this
     * implementation. This is not exposed at the JDI level. Currently,
     * this class is needed only for type checking so it does not even
     * reference a particular component - just a generic component
     * of this array. In the future we may need to expand its use.
     */
    class Component implements ValueContainer {
        public Type type() throws ClassNotLoadedException {
            return arrayType().componentType();
        }
        public String typeName() {
            return arrayType().componentTypeName();
        }
        public String signature() {
            return arrayType().componentSignature();
        }
        public Type findType(String signature) throws ClassNotLoadedException {
            return arrayType().findType(signature);
        }
    }
}
