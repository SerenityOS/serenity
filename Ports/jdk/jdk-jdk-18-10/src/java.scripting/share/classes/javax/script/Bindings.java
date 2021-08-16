/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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

package javax.script;
import java.util.Map;

/**
 * A mapping of key/value pairs, all of whose keys are
 * {@code Strings}.
 *
 * @author Mike Grogan
 * @since 1.6
 */
public interface Bindings extends Map<String, Object> {
    /**
     * Set a named value.
     *
     * @param name The name associated with the value.
     * @param value The value associated with the name.
     *
     * @return The value previously associated with the given name.
     * Returns null if no value was previously associated with the name.
     *
     * @throws NullPointerException if the name is null.
     * @throws IllegalArgumentException if the name is empty String.
     */
    public Object put(String name, Object value);

    /**
     * Adds all the mappings in a given {@code Map} to this {@code Bindings}.
     * @param toMerge The {@code Map} to merge with this one.
     *
     * @throws NullPointerException
     *         if toMerge map is null or if some key in the map is null.
     * @throws IllegalArgumentException
     *         if some key in the map is an empty String.
     */
    public void putAll(Map<? extends String, ? extends Object> toMerge);

    /**
     * Returns {@code true} if this map contains a mapping for the specified
     * key.  More formally, returns {@code true} if and only if
     * this map contains a mapping for a key {@code k} such that
     * {@code (key==null ? k==null : key.equals(k))}.  (There can be
     * at most one such mapping.)
     *
     * @param key key whose presence in this map is to be tested.
     * @return {@code true} if this map contains a mapping for the specified
     *         key.
     *
     * @throws NullPointerException if key is null
     * @throws ClassCastException if key is not String
     * @throws IllegalArgumentException if key is empty String
     */
    public boolean containsKey(Object key);

    /**
     * Returns the value to which this map maps the specified key.  Returns
     * {@code null} if the map contains no mapping for this key.  A return
     * value of {@code null} does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to {@code null}.  The {@code containsKey}
     * operation may be used to distinguish these two cases.
     *
     * <p>More formally, if this map contains a mapping from a key
     * {@code k} to a value {@code v} such that
     * {@code (key==null ? k==null : key.equals(k))},
     * then this method returns {@code v}; otherwise
     * it returns {@code null}.  (There can be at most one such mapping.)
     *
     * @param key key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or
     *         {@code null} if the map contains no mapping for this key.
     *
     * @throws NullPointerException if key is null
     * @throws ClassCastException if key is not String
     * @throws IllegalArgumentException if key is empty String
     */
    public Object get(Object key);

    /**
     * Removes the mapping for this key from this map if it is present
     * (optional operation).   More formally, if this map contains a mapping
     * from key {@code k} to value {@code v} such that
     * {@code (key==null ?  k==null : key.equals(k))}, that mapping
     * is removed.  (The map can contain at most one such mapping.)
     *
     * <p>Returns the value to which the map previously associated the key, or
     * {@code null} if the map contained no mapping for this key.  (A
     * {@code null} return can also indicate that the map previously
     * associated {@code null} with the specified key if the implementation
     * supports {@code null} values.)  The map will not contain a mapping for
     * the specified  key once the call returns.
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or {@code null}
     *         if there was no mapping for key.
     *
     * @throws NullPointerException if key is null
     * @throws ClassCastException if key is not String
     * @throws IllegalArgumentException if key is empty String
     */
    public Object remove(Object key);
}
