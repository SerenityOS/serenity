/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Hashtable;

/*
 * Private storage mechanism for Action key-value pairs.
 * In most cases this will be an array of alternating
 * key-value pairs.  As it grows larger it is scaled
 * up to a Hashtable.
 * <p>
 * This does no synchronization, if you need thread safety synchronize on
 * another object before calling this.
 *
 * @author Georges Saab
 * @author Scott Violet
 */
class ArrayTable implements Cloneable {
    // Our field for storage
    private Object table = null;
    private static final int ARRAY_BOUNDARY = 8;


    /**
     * Writes the passed in ArrayTable to the passed in ObjectOutputStream.
     * The data is saved as an integer indicating how many key/value
     * pairs are being archived, followed by the key/value pairs. If
     * <code>table</code> is null, 0 will be written to <code>s</code>.
     * <p>
     * This is a convenience method that ActionMap/InputMap and
     * AbstractAction use to avoid having the same code in each class.
     */
    static void writeArrayTable(ObjectOutputStream s, ArrayTable table) throws IOException {
        Object[] keys;

        if (table == null || (keys = table.getKeys(null)) == null) {
            s.writeInt(0);
        }
        else {
            // Determine how many keys have Serializable values, when
            // done all non-null values in keys identify the Serializable
            // values.
            int validCount = 0;

            for (int counter = 0; counter < keys.length; counter++) {
                Object key = keys[counter];

                /* include in Serialization when both keys and values are Serializable */
                if (    (key instanceof Serializable
                         && table.get(key) instanceof Serializable)
                             ||
                         /* include these only so that we get the appropriate exception below */
                        (key instanceof ClientPropertyKey
                         && ((ClientPropertyKey)key).getReportValueNotSerializable())) {

                    validCount++;
                } else {
                    keys[counter] = null;
                }
            }
            // Write ou the Serializable key/value pairs.
            s.writeInt(validCount);
            if (validCount > 0) {
                for (Object key : keys) {
                    if (key != null) {
                        s.writeObject(key);
                        s.writeObject(table.get(key));
                        if (--validCount == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }


    /*
     * Put the key-value pair into storage
     */
    public void put(Object key, Object value){
        if (table==null) {
            table = new Object[] {key, value};
        } else {
            int size = size();
            if (size < ARRAY_BOUNDARY) {              // We are an array
                if (containsKey(key)) {
                    Object[] tmp = (Object[])table;
                    for (int i = 0; i<tmp.length-1; i+=2) {
                        if (tmp[i].equals(key)) {
                            tmp[i+1]=value;
                            break;
                        }
                    }
                } else {
                    Object[] array = (Object[])table;
                    int i = array.length;
                    Object[] tmp = new Object[i+2];
                    System.arraycopy(array, 0, tmp, 0, i);

                    tmp[i] = key;
                    tmp[i+1] = value;
                    table = tmp;
                }
            } else {                 // We are a hashtable
                if ((size==ARRAY_BOUNDARY) && isArray()) {
                    grow();
                }
                @SuppressWarnings("unchecked")
                Hashtable<Object,Object> tmp = (Hashtable<Object,Object>)table;
                tmp.put(key, value);
            }
        }
    }

    /*
     * Gets the value for key
     */
    public Object get(Object key) {
        Object value = null;
        if (table !=null) {
            if (isArray()) {
                Object[] array = (Object[])table;
                for (int i = 0; i<array.length-1; i+=2) {
                    if (array[i].equals(key)) {
                        value = array[i+1];
                        break;
                    }
                }
            } else {
                value = ((Hashtable)table).get(key);
            }
        }
        return value;
    }

    /*
     * Returns the number of pairs in storage
     */
    public int size() {
        int size;
        if (table==null)
            return 0;
        if (isArray()) {
            size = ((Object[])table).length/2;
        } else {
            size = ((Hashtable)table).size();
        }
        return size;
    }

    /*
     * Returns true if we have a value for the key
     */
    public boolean containsKey(Object key) {
        boolean contains = false;
        if (table !=null) {
            if (isArray()) {
                Object[] array = (Object[])table;
                for (int i = 0; i<array.length-1; i+=2) {
                    if (array[i].equals(key)) {
                        contains = true;
                        break;
                    }
                }
            } else {
                contains = ((Hashtable)table).containsKey(key);
            }
        }
        return contains;
    }

    /*
     * Removes the key and its value
     * Returns the value for the pair removed
     */
    public Object remove(Object key){
        Object value = null;
        if (key==null) {
            return null;
        }
        if (table !=null) {
            if (isArray()){
                // Is key on the list?
                int index = -1;
                Object[] array = (Object[])table;
                for (int i = array.length-2; i>=0; i-=2) {
                    if (array[i].equals(key)) {
                        index = i;
                        value = array[i+1];
                        break;
                    }
                }

                // If so,  remove it
                if (index != -1) {
                    Object[] tmp = new Object[array.length-2];
                    // Copy the list up to index
                    System.arraycopy(array, 0, tmp, 0, index);
                    // Copy from two past the index, up to
                    // the end of tmp (which is two elements
                    // shorter than the old list)
                    if (index < tmp.length)
                        System.arraycopy(array, index+2, tmp, index,
                                         tmp.length - index);
                    // set the listener array to the new array or null
                    table = (tmp.length == 0) ? null : tmp;
                }
            } else {
                value = ((Hashtable)table).remove(key);
            }
            if (size()==ARRAY_BOUNDARY - 1 && !isArray()) {
                shrink();
            }
        }
        return value;
    }

    /**
     * Removes all the mappings.
     */
    public void clear() {
        table = null;
    }

    /*
     * Returns a clone of the <code>ArrayTable</code>.
     */
    public Object clone() {
        ArrayTable newArrayTable = new ArrayTable();
        if (table != null) {
            if (isArray()) {
                Object[] array = (Object[]) table;
                for (int i = 0; i < array.length - 1; i += 2) {
                    newArrayTable.put(array[i], array[i + 1]);
                }
            } else {
                Hashtable<?, ?> tmp = (Hashtable) table;
                Enumeration<?> keys = tmp.keys();
                while (keys.hasMoreElements()) {
                    Object o = keys.nextElement();
                    newArrayTable.put(o, tmp.get(o));
                }
            }
        }
        return newArrayTable;
    }

    /**
     * Returns the keys of the table, or <code>null</code> if there
     * are currently no bindings.
     * @param keys  array of keys
     * @return an array of bindings
     */
    public Object[] getKeys(Object[] keys) {
        if (table == null) {
            return null;
        }
        if (isArray()) {
            Object[] array = (Object[])table;
            if (keys == null) {
                keys = new Object[array.length / 2];
            }
            for (int i = 0, index = 0 ;i < array.length-1 ; i+=2,
                     index++) {
                keys[index] = array[i];
            }
        } else {
            Hashtable<?,?> tmp = (Hashtable)table;
            Enumeration<?> enum_ = tmp.keys();
            int counter = tmp.size();
            if (keys == null) {
                keys = new Object[counter];
            }
            while (counter > 0) {
                keys[--counter] = enum_.nextElement();
            }
        }
        return keys;
    }

    /*
     * Returns true if the current storage mechanism is
     * an array of alternating key-value pairs.
     */
    private boolean isArray(){
        return (table instanceof Object[]);
    }

    /*
     * Grows the storage from an array to a hashtable.
     */
    private void grow() {
        Object[] array = (Object[])table;
        Hashtable<Object, Object> tmp = new Hashtable<Object, Object>(array.length/2);
        for (int i = 0; i<array.length; i+=2) {
            tmp.put(array[i], array[i+1]);
        }
        table = tmp;
    }

    /*
     * Shrinks the storage from a hashtable to an array.
     */
    private void shrink() {
        Hashtable<?,?> tmp = (Hashtable)table;
        Object[] array = new Object[tmp.size()*2];
        Enumeration<?> keys = tmp.keys();
        int j = 0;

        while (keys.hasMoreElements()) {
            Object o = keys.nextElement();
            array[j] = o;
            array[j+1] = tmp.get(o);
            j+=2;
        }
        table = array;
    }
}
