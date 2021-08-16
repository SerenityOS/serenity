/*
 * Copyright (c) 2004, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.management;

import java.io.Serializable;
import java.util.*;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.TabularType;

/**
 * This abstract class provides the implementation of the CompositeData
 * interface.  A CompositeData object will be lazily created only when
 * the CompositeData interface is used.
 *
 * Classes that extends this abstract class will implement the
 * getCompositeData() method. The object returned by the
 * getCompositeData() is an instance of CompositeData such that
 * the instance serializes itself as the type CompositeDataSupport.
 */
public abstract class LazyCompositeData
        implements CompositeData, Serializable {

    @SuppressWarnings("serial") // Not statically typed as Serializable
    private CompositeData compositeData;

    // Implementation of the CompositeData interface
    @Override
    public boolean containsKey(String key) {
        return compositeData().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return compositeData().containsValue(value);
    }

    @Override
    public boolean equals(Object obj) {
        return compositeData().equals(obj);
    }

    @Override
    public Object get(String key) {
        return compositeData().get(key);
    }

    @Override
    public Object[] getAll(String[] keys) {
        return compositeData().getAll(keys);
    }

    @Override
    public CompositeType getCompositeType() {
        return compositeData().getCompositeType();
    }

    @Override
    public int hashCode() {
        return compositeData().hashCode();
    }

    @Override
    public String toString() {
        /** FIXME: What should this be?? */
        return compositeData().toString();
    }

    @Override
    public Collection<?> values() {
        return compositeData().values();
    }

    /* Lazy creation of a CompositeData object
     * only when the CompositeData interface is used.
     */
    private synchronized CompositeData compositeData() {
        if (compositeData != null)
            return compositeData;
        compositeData = getCompositeData();
        return compositeData;
    }

    /**
     * Designate to a CompositeData object when writing to an
     * output stream during serialization so that the receiver
     * only requires JMX 1.2 classes but not any implementation
     * specific class.
     */
    protected Object writeReplace() throws java.io.ObjectStreamException {
        return compositeData();
    }

    /**
     * Returns the CompositeData representing this object.
     * The returned CompositeData object must be an instance
     * of javax.management.openmbean.CompositeDataSupport class
     * so that no implementation specific class is required
     * for unmarshalling besides JMX 1.2 classes.
     */
    protected abstract CompositeData getCompositeData();

    // Helper methods
    public static String getString(CompositeData cd, String itemName) {
        if (cd == null)
            throw new IllegalArgumentException("Null CompositeData");

        return (String) cd.get(itemName);
    }

    public static boolean getBoolean(CompositeData cd, String itemName) {
        if (cd == null)
            throw new IllegalArgumentException("Null CompositeData");

        return ((Boolean) cd.get(itemName));
    }

    public static long getLong(CompositeData cd, String itemName) {
        if (cd == null)
            throw new IllegalArgumentException("Null CompositeData");

        return ((Long) cd.get(itemName));
    }

    public static int getInt(CompositeData cd, String itemName) {
        if (cd == null)
            throw new IllegalArgumentException("Null CompositeData");

        return ((Integer) cd.get(itemName));
    }

    /**
     * Compares two CompositeTypes and returns true if
     * all items in type1 exist in type2 and their item types
     * are the same.
     * @param type1 the base composite type
     * @param type2 the checked composite type
     * @return {@code true} if all items in type1 exist in type2 and their item
     *         types are the same.
     */
    protected static boolean isTypeMatched(CompositeType type1, CompositeType type2) {
        if (type1 == type2) return true;

        // We can't use CompositeType.isValue() since it returns false
        // if the type name doesn't match.
        Set<String> allItems = type1.keySet();

        // Check all items in the type1 exist in type2
        if (!type2.keySet().containsAll(allItems))
            return false;

        return allItems.stream().allMatch(
            item -> isTypeMatched(type1.getType(item), type2.getType(item))
        );
    }

    protected static boolean isTypeMatched(TabularType type1, TabularType type2) {
        if (type1 == type2) return true;

        List<String> list1 = type1.getIndexNames();
        List<String> list2 = type2.getIndexNames();

        // check if the list of index names are the same
        if (!list1.equals(list2))
            return false;

        return isTypeMatched(type1.getRowType(), type2.getRowType());
    }

    protected static boolean isTypeMatched(ArrayType<?> type1, ArrayType<?> type2) {
        if (type1 == type2) return true;

        int dim1 = type1.getDimension();
        int dim2 = type2.getDimension();

        // check if the array dimensions are the same
        if (dim1 != dim2)
            return false;

        return isTypeMatched(type1.getElementOpenType(), type2.getElementOpenType());
    }

    private static boolean isTypeMatched(OpenType<?> ot1, OpenType<?> ot2) {
        if (ot1 instanceof CompositeType) {
            if (! (ot2 instanceof CompositeType))
                return false;
            if (!isTypeMatched((CompositeType) ot1, (CompositeType) ot2))
                return false;
        } else if (ot1 instanceof TabularType) {
            if (! (ot2 instanceof TabularType))
                return false;
            if (!isTypeMatched((TabularType) ot1, (TabularType) ot2))
                return false;
        } else if (ot1 instanceof ArrayType) {
            if (! (ot2 instanceof ArrayType))
                return false;
            if (!isTypeMatched((ArrayType<?>) ot1, (ArrayType<?>) ot2)) {
                return false;
            }
        } else if (!ot1.equals(ot2)) {
            return false;
        }
        return true;
    }

    private static final long serialVersionUID = -2190411934472666714L;
}
