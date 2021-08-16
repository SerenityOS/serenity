/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import jdk.internal.vm.annotation.Stable;

/**
 * An immutable container for a key and a value, suitable for use
 * in creating and populating {@code Map} instances.
 *
 * <p>This is a <a href="{@docRoot}/java.base/java/lang/doc-files/ValueBased.html">value-based</a>
 * class; programmers should treat instances that are
 * {@linkplain #equals(Object) equal} as interchangeable and should not
 * use instances for synchronization, or unpredictable behavior may
 * occur. For example, in a future release, synchronization may fail.
 *
 * @apiNote
 * This class is not public. Instances can be created using the
 * {@link Map#entry Map.entry(k, v)} factory method, which is public.
 *
 * <p>This class differs from AbstractMap.SimpleImmutableEntry in the following ways:
 * it is not serializable, it is final, and its key and value must be non-null.
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @see Map#ofEntries Map.ofEntries()
 * @since 9
 */
@jdk.internal.ValueBased
final class KeyValueHolder<K,V> implements Map.Entry<K,V> {
    @Stable
    final K key;
    @Stable
    final V value;

    KeyValueHolder(K k, V v) {
        key = Objects.requireNonNull(k);
        value = Objects.requireNonNull(v);
    }

    /**
     * Gets the key from this holder.
     *
     * @return the key
     */
    @Override
    public K getKey() {
        return key;
    }

    /**
     * Gets the value from this holder.
     *
     * @return the value
     */
    @Override
    public V getValue() {
        return value;
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     *
     * @param value ignored
     * @return never returns normally
     */
    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * Compares the specified object with this entry for equality.
     * Returns {@code true} if the given object is also a map entry and
     * the two entries' keys and values are equal. Note that key and
     * value are non-null, so equals() can be called safely on them.
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Map.Entry<?, ?> e
                && key.equals(e.getKey())
                && value.equals(e.getValue());
    }

    /**
     * Returns the hash code value for this map entry. The hash code
     * is {@code key.hashCode() ^ value.hashCode()}. Note that key and
     * value are non-null, so hashCode() can be called safely on them.
     */
    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    /**
     * Returns a String representation of this map entry.  This
     * implementation returns the string representation of this
     * entry's key followed by the equals character ("{@code =}")
     * followed by the string representation of this entry's value.
     *
     * @return a String representation of this map entry
     */
    @Override
    public String toString() {
        return key + "=" + value;
    }
}
