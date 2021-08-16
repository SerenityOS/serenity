/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xerces.internal.impl.xs.util;

import com.sun.org.apache.xerces.internal.xs.LSInputList;
import java.lang.reflect.Array;
import java.util.AbstractList;
import org.w3c.dom.ls.LSInput;

/**
 * Contains a list of LSInputs.
 *
 * @xerces.internal
 *
 * @author Michael Glavassevich, IBM
 *
 * @LastModified: Oct 2017
 */
@SuppressWarnings("unchecked") // method <T>toArray(T[])
public final class LSInputListImpl extends AbstractList<LSInput> implements LSInputList {

    /**
     * An immutable empty list.
     */
    public static final LSInputListImpl EMPTY_LIST = new LSInputListImpl(new LSInput[0], 0);

    // The array to hold all data
    private final LSInput[] fArray;
    // Number of elements in this list
    private final int fLength;

    /**
     * Construct an LSInputList implementation
     *
     * @param array     the data array
     * @param length    the number of elements
     */
    public LSInputListImpl(LSInput[] array, int length) {
        fArray = array;
        fLength = length;
    }

    /**
     * The number of <code>LSInput</code>s in the list. The range of valid
     * child object indices is 0 to <code>length-1</code> inclusive.
     */
    public int getLength() {
        return fLength;
    }

    /**
     * Returns the <code>index</code>th item in the collection or
     * <code>null</code> if <code>index</code> is greater than or equal to
     * the number of objects in the list. The index starts at 0.
     * @param index  index into the collection.
     * @return  The <code>LSInput</code> at the <code>index</code>th
     *   position in the <code>LSInputList</code>, or <code>null</code> if
     *   the index specified is not valid.
     */
    public LSInput item(int index) {
        if (index < 0 || index >= fLength) {
            return null;
        }
        return fArray[index];
    }

    /*
     * List methods
     */

    public LSInput get(int index) {
        if (index >= 0 && index < fLength) {
            return fArray[index];
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    public int size() {
        return getLength();
    }

    public Object[] toArray() {
        Object[] a = new Object[fLength];
        toArray0(a);
        return a;
    }

    public Object[] toArray(Object[] a) {
        if (a.length < fLength) {
            Class<?> arrayClass = a.getClass();
            Class<?> componentType = arrayClass.getComponentType();
            a = (Object[]) Array.newInstance(componentType, fLength);
        }
        toArray0(a);
        if (a.length > fLength) {
            a[fLength] = null;
        }
        return a;
    }

    private void toArray0(Object[] a) {
        if (fLength > 0) {
            System.arraycopy(fArray, 0, a, 0, fLength);
        }
    }

} // LSInputListImpl
