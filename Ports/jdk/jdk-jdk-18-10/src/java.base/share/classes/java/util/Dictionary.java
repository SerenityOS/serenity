/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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

package java.util;

/**
 * The {@code Dictionary} class is the abstract parent of any
 * class, such as {@code Hashtable}, which maps keys to values.
 * Every key and every value is an object. In any one {@code Dictionary}
 * object, every key is associated with at most one value. Given a
 * {@code Dictionary} and a key, the associated element can be looked up.
 * Any non-{@code null} object can be used as a key and as a value.
 * <p>
 * As a rule, the {@code equals} method should be used by
 * implementations of this class to decide if two keys are the same.
 * <p>
 * <strong>NOTE: This class is obsolete.  New implementations should
 * implement the Map interface, rather than extending this class.</strong>
 *
 * @see     java.util.Map
 * @see     java.lang.Object#equals(java.lang.Object)
 * @see     java.lang.Object#hashCode()
 * @see     java.util.Hashtable
 * @since   1.0
 */
public abstract
class Dictionary<K,V> {
    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    public Dictionary() {
    }

    /**
     * Returns the number of entries (distinct keys) in this dictionary.
     *
     * @return  the number of keys in this dictionary.
     */
    public abstract int size();

    /**
     * Tests if this dictionary maps no keys to value. The general contract
     * for the {@code isEmpty} method is that the result is true if and only
     * if this dictionary contains no entries.
     *
     * @return  {@code true} if this dictionary maps no keys to values;
     *          {@code false} otherwise.
     */
    public abstract boolean isEmpty();

    /**
     * Returns an enumeration of the keys in this dictionary. The general
     * contract for the keys method is that an {@code Enumeration} object
     * is returned that will generate all the keys for which this dictionary
     * contains entries.
     *
     * @return  an enumeration of the keys in this dictionary.
     * @see     java.util.Dictionary#elements()
     * @see     java.util.Enumeration
     */
    public abstract Enumeration<K> keys();

    /**
     * Returns an enumeration of the values in this dictionary. The general
     * contract for the {@code elements} method is that an
     * {@code Enumeration} is returned that will generate all the elements
     * contained in entries in this dictionary.
     *
     * @return  an enumeration of the values in this dictionary.
     * @see     java.util.Dictionary#keys()
     * @see     java.util.Enumeration
     */
    public abstract Enumeration<V> elements();

    /**
     * Returns the value to which the key is mapped in this dictionary.
     * The general contract for the {@code isEmpty} method is that if this
     * dictionary contains an entry for the specified key, the associated
     * value is returned; otherwise, {@code null} is returned.
     *
     * @return  the value to which the key is mapped in this dictionary;
     * @param   key   a key in this dictionary.
     *          {@code null} if the key is not mapped to any value in
     *          this dictionary.
     * @throws    NullPointerException if the {@code key} is {@code null}.
     * @see     java.util.Dictionary#put(java.lang.Object, java.lang.Object)
     */
    public abstract V get(Object key);

    /**
     * Maps the specified {@code key} to the specified
     * {@code value} in this dictionary. Neither the key nor the
     * value can be {@code null}.
     * <p>
     * If this dictionary already contains an entry for the specified
     * {@code key}, the value already in this dictionary for that
     * {@code key} is returned, after modifying the entry to contain the
     *  new element. <p>If this dictionary does not already have an entry
     *  for the specified {@code key}, an entry is created for the
     *  specified {@code key} and {@code value}, and {@code null} is
     *  returned.
     * <p>
     * The {@code value} can be retrieved by calling the
     * {@code get} method with a {@code key} that is equal to
     * the original {@code key}.
     *
     * @param      key     the hashtable key.
     * @param      value   the value.
     * @return     the previous value to which the {@code key} was mapped
     *             in this dictionary, or {@code null} if the key did not
     *             have a previous mapping.
     * @throws     NullPointerException  if the {@code key} or
     *               {@code value} is {@code null}.
     * @see        java.lang.Object#equals(java.lang.Object)
     * @see        java.util.Dictionary#get(java.lang.Object)
     */
    public abstract V put(K key, V value);

    /**
     * Removes the {@code key} (and its corresponding
     * {@code value}) from this dictionary. This method does nothing
     * if the {@code key} is not in this dictionary.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the {@code key} had been mapped in this
     *          dictionary, or {@code null} if the key did not have a
     *          mapping.
     * @throws    NullPointerException if {@code key} is {@code null}.
     */
    public abstract V remove(Object key);
}
