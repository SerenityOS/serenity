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

import com.sun.org.apache.xerces.internal.xs.StringList;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.List;

/**
 * Containts a list of Object's.
 *
 * @xerces.internal
 *
 * @author Sandy Gao, IBM
 *
 * @LastModified: Oct 2017
 */
@SuppressWarnings("unchecked") // method <T>toArray(T[])
public final class StringListImpl extends AbstractList<String> implements StringList {

    /**
     * An immutable empty list.
     */
    public static final StringListImpl EMPTY_LIST = new StringListImpl(new String[0], 0);

    // The array to hold all data
    private final String[] fArray;
    // Number of elements in this list
    private final int fLength;

    // REVISIT: this is temp solution. In general we need to use this class
    //          instead of the Vector.
    private final List<String> fVector;

    public StringListImpl(List<String> v) {
        fVector = v;
        fLength = (v == null) ? 0 : v.size();
        fArray = null;
    }

    /**
     * Construct an XSObjectList implementation
     *
     * @param array     the data array
     * @param length    the number of elements
     */
    public StringListImpl(String[] array, int length) {
        fArray = array;
        fLength = length;
        fVector = null;
    }

    /**
     * The number of <code>Objects</code> in the list. The range of valid
     * child node indices is 0 to <code>length-1</code> inclusive.
     */
    public int getLength() {
        return fLength;
    }

    /**
     *  Checks if the <code>GenericString</code> <code>item</code> is a member
     * of this list.
     * @param item  <code>GenericString</code> whose presence in this list is
     *   to be tested.
     * @return  True if this list contains the <code>GenericString</code>
     *   <code>item</code>.
     */
    public boolean contains(String item) {
        if (fVector != null) {
            return fVector.contains(item);
        }
        if (item == null) {
            for (int i = 0; i < fLength; i++) {
                if (fArray[i] == null)
                    return true;
            }
        }
        else {
            for (int i = 0; i < fLength; i++) {
                if (item.equals(fArray[i]))
                    return true;
            }
        }
        return false;
    }

    public String item(int index) {
        if (index < 0 || index >= fLength) {
            return null;
        }
        if (fVector != null) {
            return fVector.get(index);
        }
        return fArray[index];
    }

    /*
     * List methods
     */

    public String get(int index) {
        if (index >= 0 && index < fLength) {
            if (fVector != null) {
                return fVector.get(index);
            }
            return fArray[index];
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    public int size() {
        return getLength();
    }

    public Object[] toArray() {
        if (fVector != null) {
            return fVector.toArray();
        }
        Object[] a = new Object[fLength];
        toArray0(a);
        return a;
    }

    public Object[] toArray(Object[] a) {
        if (fVector != null) {
            return fVector.toArray(a);
        }
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

} // class StringListImpl
