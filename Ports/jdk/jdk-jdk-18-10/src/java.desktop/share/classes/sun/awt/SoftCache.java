/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt;

import java.lang.ref.SoftReference;
import java.lang.ref.ReferenceQueue;

import java.util.Iterator;
import java.util.Map;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;
import java.util.AbstractSet;
import java.util.NoSuchElementException;


/**
 * A memory-sensitive implementation of the <code>Map</code> interface.
 *
 * <p> A <code>SoftCache</code> object uses {@link java.lang.ref.SoftReference
 * soft references} to implement a memory-sensitive hash map.  If the garbage
 * collector determines at a certain point in time that a value object in a
 * <code>SoftCache</code> entry is no longer strongly reachable, then it may
 * remove that entry in order to release the memory occupied by the value
 * object.  All <code>SoftCache</code> objects are guaranteed to be completely
 * cleared before the virtual machine will throw an
 * <code>OutOfMemoryError</code>.  Because of this automatic clearing feature,
 * the behavior of this class is somewhat different from that of other
 * <code>Map</code> implementations.
 *
 * <p> Both null values and the null key are supported.  This class has the
 * same performance characteristics as the <code>HashMap</code> class, and has
 * the same efficiency parameters of <em>initial capacity</em> and <em>load
 * factor</em>.
 *
 * <p> Like most collection classes, this class is not synchronized.  A
 * synchronized <code>SoftCache</code> may be constructed using the
 * <code>Collections.synchronizedMap</code> method.
 *
 * <p> In typical usage this class will be subclassed and the <code>fill</code>
 * method will be overridden.  When the <code>get</code> method is invoked on a
 * key for which there is no mapping in the cache, it will in turn invoke the
 * <code>fill</code> method on that key in an attempt to construct a
 * corresponding value.  If the <code>fill</code> method returns such a value
 * then the cache will be updated and the new value will be returned.  Thus,
 * for example, a simple URL-content cache can be constructed as follows:
 *
 * <pre>
 *     public class URLCache extends SoftCache {
 *         protected Object fill(Object key) {
 *             return ((URL)key).getContent();
 *         }
 *     }
 * </pre>
 *
 * <p> The behavior of the <code>SoftCache</code> class depends in part upon
 * the actions of the garbage collector, so several familiar (though not
 * required) <code>Map</code> invariants do not hold for this class.  <p>
 * Because entries are removed from a <code>SoftCache</code> in response to
 * dynamic advice from the garbage collector, a <code>SoftCache</code> may
 * behave as though an unknown thread is silently removing entries.  In
 * particular, even if you synchronize on a <code>SoftCache</code> instance and
 * invoke none of its mutator methods, it is possible for the <code>size</code>
 * method to return smaller values over time, for the <code>isEmpty</code>
 * method to return <code>false</code> and then <code>true</code>, for the
 * <code>containsKey</code> method to return <code>true</code> and later
 * <code>false</code> for a given key, for the <code>get</code> method to
 * return a value for a given key but later return <code>null</code>, for the
 * <code>put</code> method to return <code>null</code> and the
 * <code>remove</code> method to return <code>false</code> for a key that
 * previously appeared to be in the map, and for successive examinations of the
 * key set, the value set, and the entry set to yield successively smaller
 * numbers of elements.
 *
 * @author      Mark Reinhold
 * @since       1.2
 * @see         java.util.HashMap
 * @see         java.lang.ref.SoftReference
 * @deprecated No direct replacement; {@link java.util.WeakHashMap}
 * addresses a related by different use-case.
 */

@Deprecated
public class SoftCache extends AbstractMap<Object, Object> implements Map<Object, Object> {

    /* The basic idea of this implementation is to maintain an internal HashMap
       that maps keys to soft references whose referents are the keys' values;
       the various accessor methods dereference these soft references before
       returning values.  Because we don't have access to the innards of the
       HashMap, each soft reference must contain the key that maps to it so
       that the processQueue method can remove keys whose values have been
       discarded.  Thus the HashMap actually maps keys to instances of the
       ValueCell class, which is a simple extension of the SoftReference class.
     */


    private static class ValueCell extends SoftReference<Object> {
        private static Object INVALID_KEY = new Object();
        private static int dropped = 0;
        private Object key;

        private ValueCell(Object key, Object value, ReferenceQueue<Object> queue) {
            super(value, queue);
            this.key = key;
        }

        private static ValueCell create(Object key, Object value,
                                        ReferenceQueue<Object> queue)
        {
            if (value == null) return null;
            return new ValueCell(key, value, queue);
        }

        private static Object strip(Object val, boolean drop) {
            if (val == null) return null;
            ValueCell vc = (ValueCell)val;
            Object o = vc.get();
            if (drop) vc.drop();
            return o;
        }

        private boolean isValid() {
            return (key != INVALID_KEY);
        }

        private void drop() {
            super.clear();
            key = INVALID_KEY;
            dropped++;
        }

    }


    /* Hash table mapping keys to ValueCells */
    private Map<Object, Object> hash;

    /* Reference queue for cleared ValueCells */
    private ReferenceQueue<Object> queue = new ReferenceQueue<>();


    /* Process any ValueCells that have been cleared and enqueued by the
       garbage collector.  This method should be invoked once by each public
       mutator in this class.  We don't invoke this method in public accessors
       because that can lead to surprising ConcurrentModificationExceptions.
     */
    private void processQueue() {
        ValueCell vc;
        while ((vc = (ValueCell)queue.poll()) != null) {
            if (vc.isValid()) hash.remove(vc.key);
            else ValueCell.dropped--;
        }
    }


    /* -- Constructors -- */

    /**
     * Construct a new, empty <code>SoftCache</code> with the given
     * initial capacity and the given load factor.
     *
     * @param  initialCapacity  The initial capacity of the cache
     *
     * @param  loadFactor       A number between 0.0 and 1.0
     *
     * @throws IllegalArgumentException  If the initial capacity is less than
     *                                   or equal to zero, or if the load
     *                                   factor is less than zero
     */
    public SoftCache(int initialCapacity, float loadFactor) {
        hash = new HashMap<>(initialCapacity, loadFactor);
    }

    /**
     * Construct a new, empty <code>SoftCache</code> with the given
     * initial capacity and the default load factor.
     *
     * @param  initialCapacity  The initial capacity of the cache
     *
     * @throws IllegalArgumentException  If the initial capacity is less than
     *                                   or equal to zero
     */
    public SoftCache(int initialCapacity) {
        hash = new HashMap<>(initialCapacity);
    }

    /**
     * Construct a new, empty <code>SoftCache</code> with the default
     * capacity and the default load factor.
     */
    public SoftCache() {
        hash = new HashMap<>();
    }


    /* -- Simple queries -- */

    /**
     * Return the number of key-value mappings in this cache.  The time
     * required by this operation is linear in the size of the map.
     */
    public int size() {
        return entrySet().size();
    }

    /**
     * Return <code>true</code> if this cache contains no key-value mappings.
     */
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    /**
     * Return <code>true</code> if this cache contains a mapping for the
     * specified key.  If there is no mapping for the key, this method will not
     * attempt to construct one by invoking the <code>fill</code> method.
     *
     * @param   key   The key whose presence in the cache is to be tested
     */
    public boolean containsKey(Object key) {
        return ValueCell.strip(hash.get(key), false) != null;
    }


    /* -- Lookup and modification operations -- */

    /**
     * Create a value object for the given <code>key</code>.  This method is
     * invoked by the <code>get</code> method when there is no entry for
     * <code>key</code>.  If this method returns a non-<code>null</code> value,
     * then the cache will be updated to map <code>key</code> to that value,
     * and that value will be returned by the <code>get</code> method.
     *
     * <p> The default implementation of this method simply returns
     * <code>null</code> for every <code>key</code> value.  A subclass may
     * override this method to provide more useful behavior.
     *
     * @param  key  The key for which a value is to be computed
     *
     * @return      A value for <code>key</code>, or <code>null</code> if one
     *              could not be computed
     * @see #get
     */
    protected Object fill(Object key) {
        return null;
    }

    /**
     * Return the value to which this cache maps the specified
     * <code>key</code>.  If the cache does not presently contain a value for
     * this key, then invoke the <code>fill</code> method in an attempt to
     * compute such a value.  If that method returns a non-<code>null</code>
     * value, then update the cache and return the new value.  Otherwise,
     * return <code>null</code>.
     *
     * <p> Note that because this method may update the cache, it is considered
     * a mutator and may cause <code>ConcurrentModificationException</code>s to
     * be thrown if invoked while an iterator is in use.
     *
     * @param  key  The key whose associated value, if any, is to be returned
     *
     * @see #fill
     */
    public Object get(Object key) {
        processQueue();
        Object v = hash.get(key);
        if (v == null) {
            v = fill(key);
            if (v != null) {
                hash.put(key, ValueCell.create(key, v, queue));
                return v;
            }
        }
        return ValueCell.strip(v, false);
    }

    /**
     * Update this cache so that the given <code>key</code> maps to the given
     * <code>value</code>.  If the cache previously contained a mapping for
     * <code>key</code> then that mapping is replaced and the old value is
     * returned.
     *
     * @param  key    The key that is to be mapped to the given
     *                <code>value</code>
     * @param  value  The value to which the given <code>key</code> is to be
     *                mapped
     *
     * @return  The previous value to which this key was mapped, or
     *          <code>null</code> if there was no mapping for the key
     */
    public Object put(Object key, Object value) {
        processQueue();
        ValueCell vc = ValueCell.create(key, value, queue);
        return ValueCell.strip(hash.put(key, vc), true);
    }

    /**
     * Remove the mapping for the given <code>key</code> from this cache, if
     * present.
     *
     * @param  key  The key whose mapping is to be removed
     *
     * @return  The value to which this key was mapped, or <code>null</code> if
     *          there was no mapping for the key
     */
    public Object remove(Object key) {
        processQueue();
        return ValueCell.strip(hash.remove(key), true);
    }

    /**
     * Remove all mappings from this cache.
     */
    public void clear() {
        processQueue();
        hash.clear();
    }


    /* -- Views -- */

    private static boolean valEquals(Object o1, Object o2) {
        return (o1 == null) ? (o2 == null) : o1.equals(o2);
    }


    /* Internal class for entries.
       Because it uses SoftCache.this.queue, this class cannot be static.
     */
    private class Entry implements Map.Entry<Object, Object> {
        private Map.Entry<Object, Object> ent;
        private Object value;   /* Strong reference to value, to prevent the GC
                                   from flushing the value while this Entry
                                   exists */

        Entry(Map.Entry<Object, Object> ent, Object value) {
            this.ent = ent;
            this.value = value;
        }

        public Object getKey() {
            return ent.getKey();
        }

        public Object getValue() {
            return value;
        }

        public Object setValue(Object value) {
            return ent.setValue(ValueCell.create(ent.getKey(), value, queue));
        }

        @SuppressWarnings("unchecked")
        public boolean equals(Object o) {
            if (! (o instanceof Map.Entry)) return false;
            Map.Entry<Object, Object> e = (Map.Entry<Object, Object>)o;
            return (valEquals(ent.getKey(), e.getKey())
                    && valEquals(value, e.getValue()));
        }

        public int hashCode() {
            Object k;
            return ((((k = getKey()) == null) ? 0 : k.hashCode())
                    ^ ((value == null) ? 0 : value.hashCode()));
        }

    }


    /* Internal class for entry sets */
    private class EntrySet extends AbstractSet<Map.Entry<Object, Object>> {
        Set<Map.Entry<Object, Object>> hashEntries = hash.entrySet();

        public Iterator<Map.Entry<Object, Object>> iterator() {

            return new Iterator<Map.Entry<Object, Object>>() {
                Iterator<Map.Entry<Object, Object>> hashIterator = hashEntries.iterator();
                Entry next = null;

                public boolean hasNext() {
                    while (hashIterator.hasNext()) {
                        Map.Entry<Object, Object> ent = hashIterator.next();
                        ValueCell vc = (ValueCell)ent.getValue();
                        Object v = null;
                        if ((vc != null) && ((v = vc.get()) == null)) {
                            /* Value has been flushed by GC */
                            continue;
                        }
                        next = new Entry(ent, v);
                        return true;
                    }
                    return false;
                }

                public Map.Entry<Object, Object> next() {
                    if ((next == null) && !hasNext())
                        throw new NoSuchElementException();
                    Entry e = next;
                    next = null;
                    return e;
                }

                public void remove() {
                    hashIterator.remove();
                }

            };
        }

        public boolean isEmpty() {
            return !(iterator().hasNext());
        }

        public int size() {
            int j = 0;
            for (Iterator<Map.Entry<Object, Object>> i = iterator(); i.hasNext(); i.next()) j++;
            return j;
        }

        public boolean remove(Object o) {
            processQueue();
            if (o instanceof Entry) return hashEntries.remove(((Entry)o).ent);
            else return false;
        }

    }


    private Set<Map.Entry<Object, Object>> entrySet = null;

    /**
     * Return a <code>Set</code> view of the mappings in this cache.
     */
    public Set<Map.Entry<Object, Object>> entrySet() {
        if (entrySet == null) entrySet = new EntrySet();
        return entrySet;
    }

}
