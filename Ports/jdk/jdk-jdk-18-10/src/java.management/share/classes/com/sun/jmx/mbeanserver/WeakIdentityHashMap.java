/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jmx.mbeanserver;

import static com.sun.jmx.mbeanserver.Util.*;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import java.util.Map;


/**
 * <p>A map where keys are compared using identity comparison (like
 * IdentityHashMap) but where the presence of an object as a key in
 * the map does not prevent it being garbage collected (like
 * WeakHashMap).  This class does not implement the Map interface
 * because it is difficult to ensure correct semantics for iterators
 * over the entrySet().</p>
 *
 * <p>Because we do not implement Map, we do not copy the questionable
 * interface where you can call get(k) or remove(k) for any type of k,
 * which of course can only have an effect if k is of type K.</p>
 *
 * <p>This map does not support null keys.</p>
 */
/*
 * The approach
 * is to wrap each key in a WeakReference and use the wrapped value as
 * a key in an ordinary HashMap.  The WeakReference has to be a
 * subclass IdentityWeakReference (IWR) where two IWRs are equal if
 * they refer to the same object.  This enables us to find the entry
 * again.
 */
class WeakIdentityHashMap<K, V> {
    private WeakIdentityHashMap() {}

    static <K, V> WeakIdentityHashMap<K, V> make() {
        return new WeakIdentityHashMap<K, V>();
    }

    V get(K key) {
        expunge();
        WeakReference<K> keyref = makeReference(key);
        return map.get(keyref);
    }

    public V put(K key, V value) {
        expunge();
        if (key == null)
            throw new IllegalArgumentException("Null key");
        WeakReference<K> keyref = makeReference(key, refQueue);
        return map.put(keyref, value);
    }

    public V remove(K key) {
        expunge();
        WeakReference<K> keyref = makeReference(key);
        return map.remove(keyref);
    }

    private void expunge() {
        Reference<? extends K> ref;
        while ((ref = refQueue.poll()) != null)
            map.remove(ref);
    }

    private WeakReference<K> makeReference(K referent) {
        return new IdentityWeakReference<K>(referent);
    }

    private WeakReference<K> makeReference(K referent, ReferenceQueue<K> q) {
        return new IdentityWeakReference<K>(referent, q);
    }

    /**
     * WeakReference where equals and hashCode are based on the
     * referent.  More precisely, two objects are equal if they are
     * identical or if they both have the same non-null referent.  The
     * hashCode is the value the original referent had.  Even if the
     * referent is cleared, the hashCode remains.  Thus, objects of
     * this class can be used as keys in hash-based maps and sets.
     */
    private static class IdentityWeakReference<T> extends WeakReference<T> {
        IdentityWeakReference(T o) {
            this(o, null);
        }

        IdentityWeakReference(T o, ReferenceQueue<T> q) {
            super(o, q);
            this.hashCode = (o == null) ? 0 : System.identityHashCode(o);
        }

        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof IdentityWeakReference<?>))
                return false;
            @SuppressWarnings("unchecked")
            IdentityWeakReference<T> wr = (IdentityWeakReference<T>) o;
            T got = get();
            return got != null && wr.refersTo(got);
        }

        public int hashCode() {
            return hashCode;
        }

        private final int hashCode;
    }

    private Map<WeakReference<K>, V> map = newMap();
    private ReferenceQueue<K> refQueue = new ReferenceQueue<K>();
}
