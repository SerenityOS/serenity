/*
 * Copyright 2009 Goldman Sachs International.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

/*
 * @test
 * @bug 6865031
 * @summary Application gives bad result (throws bad exception) with compressed oops
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UseCompressedOops
 *      -XX:HeapBaseMinAddress=32g -XX:-LoopUnswitching
 *      -XX:CompileCommand=inline,compiler.c2.cr6865031.AbstractMemoryEfficientList::equals
 *      compiler.c2.cr6865031.Test hello goodbye
 */

package compiler.c2.cr6865031;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

interface MyList {
    public int size();
    public Object set(final int index, final Object element);
    public Object get(final int index);
}

abstract class AbstractMemoryEfficientList implements MyList {
    abstract public int size();
    abstract public Object get(final int index);
    abstract public Object set(final int index, final Object element);

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof MyList)) {
            return false;
        }

        final MyList that = (MyList) o;
        if (this.size() != that.size()) {
            return false;
        }

        for (int i = 0; i < this.size(); i++) {
            try {
                if (!((this.get(i)).equals(that.get(i)))) {
                    return false;
                }
            } catch (IndexOutOfBoundsException e) {
                System.out.println("THROWING RT EXC");
                System.out.println("concurrent modification of this:" + this.getClass() + ":" + System.identityHashCode(this) + "; that:" + that.getClass() + ":" + System.identityHashCode(that) + "; i:" + i);
                e.printStackTrace();
                System.exit(97);
                throw new RuntimeException("concurrent modification of this:" + this.getClass() + ":" + System.identityHashCode(this) + "; that:" + that.getClass() + ":" + System.identityHashCode(that) + "; i:" + i, e);
            }
        }
        return true;
    }

    public int hashCode() {
        int hashCode = 1;
        for (int i = 0; i < this.size(); i++) {
            Object obj = this.get(i);
            hashCode = 31 * hashCode + (obj == null ? 0 : obj.hashCode());
        }
        return hashCode;
    }
}

final class SingletonList extends AbstractMemoryEfficientList {
    private Object element1;

    SingletonList(final Object obj1) {
        super();
        this.element1 = obj1;
    }

    public int size() {
        return 1;
    }

    public Object get(final int index) {
        if (index == 0) {
            return this.element1;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
        }
    }

    public Object set(final int index, final Object element) {
        if (index == 0) {
            final Object previousElement = this.element1;
            this.element1 = element;
            return previousElement;
        } else {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
        }
    }
}

final class DoubletonList extends AbstractMemoryEfficientList {
    private Object element1;
    private Object element2;

    DoubletonList(final Object obj1, final Object obj2) {
        this.element1 = obj1;
        this.element2 = obj2;
    }

    public int size() {
        return 2;
    }

    public Object get(final int index) {
        switch (index) {
            case 0 : return this.element1;
            case 1 : return this.element2;
            default: throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
        }
    }

    public Object set(final int index, final Object element) {
        switch (index) {
            case 0 :
            {
                final Object previousElement = this.element1;
                this.element1 = element;
                return previousElement;
            }
            case 1 :
            {
                final Object previousElement = this.element2;
                this.element2 = element;
                return previousElement;
            }
            default : throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + this.size());
        }
    }
}

class WeakPool<V> {
    protected static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    protected Entry<V>[] table;

    private int size;
    protected int threshold;
    private final float loadFactor;
    private final ReferenceQueue<V> queue = new ReferenceQueue<V>();

    public WeakPool()
    {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
        threshold = DEFAULT_INITIAL_CAPACITY;
        table = new Entry[DEFAULT_INITIAL_CAPACITY];
    }

    /**
     * Check for equality of non-null reference x and possibly-null y.  By
     * default uses Object.equals.
     */
    private boolean eq(Object x, Object y)
    {
        return x == y || x.equals(y);
    }

    /**
     * Return index for hash code h.
     */
    private int indexFor(int h, int length)
    {
        return h & length - 1;
    }

    /**
     * Expunge stale entries from the table.
     */
    private void expungeStaleEntries()
    {
        Object r;
        while ((r = queue.poll()) != null)
        {
            Entry e = (Entry) r;
            int h = e.hash;
            int i = indexFor(h, table.length);

            // System.out.println("EXPUNGING " + h);
            Entry<V> prev = table[i];
            Entry<V> p = prev;
            while (p != null)
            {
                Entry<V> next = p.next;
                if (p == e)
                {
                    if (prev == e)
                    {
                        table[i] = next;
                    }
                    else
                    {
                        prev.next = next;
                    }
                    e.next = null;  // Help GC
                    size--;
                    break;
                }
                prev = p;
                p = next;
            }
        }
    }

    /**
     * Return the table after first expunging stale entries
     */
    private Entry<V>[] getTable()
    {
        expungeStaleEntries();
        return table;
    }

    /**
     * Returns the number of key-value mappings in this map.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public int size()
    {
        if (size == 0)
        {
            return 0;
        }
        expungeStaleEntries();
        return size;
    }

    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     * This result is a snapshot, and may not reflect unprocessed
     * entries that will be removed before next attempted access
     * because they are no longer referenced.
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * Returns the value stored in the pool that equals the requested key
     * or <tt>null</tt> if the map contains no mapping for
     * this key (or the key is null)
     *
     * @param key the key whose equals value is to be returned.
     * @return the object that is equal the specified key, or
     *         <tt>null</tt> if key is null or no object in the pool equals the key.
     */
    public V get(V key)
    {
        if (key == null)
        {
            return null;
        }
        int h = key.hashCode();
        Entry<V>[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry<V> e = tab[index];
        while (e != null)
        {
            V candidate = e.get();
            if (e.hash == h && eq(key, candidate))
            {
                return candidate;
            }
            e = e.next;
        }
        return null;
    }

    /**
     * Returns the entry associated with the specified key in the HashMap.
     * Returns null if the HashMap contains no mapping for this key.
     */
    Entry getEntry(Object key)
    {
        int h = key.hashCode();
        Entry[] tab = getTable();
        int index = indexFor(h, tab.length);
        Entry e = tab[index];
        while (e != null && !(e.hash == h && eq(key, e.get())))
        {
            e = e.next;
        }
        return e;
    }

    /**
     * Places the object into the pool. If the object is null, nothing happens.
     * If an equal object already exists, it is not replaced.
     *
     * @param key the object to put into the pool. key may be null.
     * @return the object in the pool that is equal to the key, or the newly placed key if no such object existed when put was called
     */
    public V put(V key)
    {
        if (key == null)
        {
            return null;
        }
        int h = key.hashCode();
        Entry<V>[] tab = getTable();
        int i = indexFor(h, tab.length);

        for (Entry<V> e = tab[i]; e != null; e = e.next)
        {
            V candidate = e.get();
            if (h == e.hash && eq(key, candidate))
            {
                return candidate;
            }
        }

        tab[i] = new Entry<V>(key, queue, h, tab[i]);

        if (++size >= threshold)
        {
            resize(tab.length * 2);
        }

    // System.out.println("Added " + key + " to pool");
        return key;
    }

    /**
     * Rehashes the contents of this map into a new array with a
     * larger capacity.  This method is called automatically when the
     * number of keys in this map reaches its threshold.
     * <p/>
     * If current capacity is MAXIMUM_CAPACITY, this method does not
     * resize the map, but but sets threshold to Integer.MAX_VALUE.
     * This has the effect of preventing future calls.
     *
     * @param newCapacity the new capacity, MUST be a power of two;
     *                    must be greater than current capacity unless current
     *                    capacity is MAXIMUM_CAPACITY (in which case value
     *                    is irrelevant).
     */
    void resize(int newCapacity)
    {
        Entry<V>[] oldTable = getTable();
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY)
        {
            threshold = Integer.MAX_VALUE;
            return;
        }

        Entry<V>[] newTable = new Entry[newCapacity];
        transfer(oldTable, newTable);
        table = newTable;

        /*
         * If ignoring null elements and processing ref queue caused massive
         * shrinkage, then restore old table.  This should be rare, but avoids
         * unbounded expansion of garbage-filled tables.
         */
        if (size >= threshold / 2)
        {
            threshold = (int) (newCapacity * loadFactor);
        }
        else
        {
            expungeStaleEntries();
            transfer(newTable, oldTable);
            table = oldTable;
        }
    }

    /**
     * Transfer all entries from src to dest tables
     */
    private void transfer(Entry[] src, Entry[] dest)
    {
        for (int j = 0; j < src.length; ++j)
        {
            Entry e = src[j];
            src[j] = null;
            while (e != null)
            {
                Entry next = e.next;
                Object key = e.get();
                if (key == null)
                {
                    e.next = null;  // Help GC
                    size--;
                }
                else
                {
                    int i = indexFor(e.hash, dest.length);
                    e.next = dest[i];
                    dest[i] = e;
                }
                e = next;
            }
        }
    }

    /**
     * Removes the object in the pool that equals the key.
     *
     * @param key
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key or the key is null.
     */
    public V removeFromPool(V key)
    {
        if (key == null)
        {
            return null;
        }
        int h = key.hashCode();
        Entry<V>[] tab = getTable();
        int i = indexFor(h, tab.length);
        Entry<V> prev = tab[i];
        Entry<V> e = prev;

        while (e != null)
        {
            Entry<V> next = e.next;
            V candidate = e.get();
            if (h == e.hash && eq(key, candidate))
            {
                size--;
                if (prev == e)
                {
                    tab[i] = next;
                }
                else
                {
                    prev.next = next;
                }
                return candidate;
            }
            prev = e;
            e = next;
        }

        return null;
    }

    /**
     * Removes all mappings from this map.
     */
    public void clear()
    {
        // clear out ref queue. We don't need to expunge entries
        // since table is getting cleared.
        while (queue.poll() != null)
        {
            // nop
        }

        table = new Entry[DEFAULT_INITIAL_CAPACITY];
        threshold = DEFAULT_INITIAL_CAPACITY;
        size = 0;

        // Allocation of array may have caused GC, which may have caused
        // additional entries to go stale.  Removing these entries from the
        // reference queue will make them eligible for reclamation.
        while (queue.poll() != null)
        {
            // nop
        }
    }

    /**
     * The entries in this hash table extend WeakReference, using its main ref
     * field as the key.
     */
    protected static class Entry<V>
    extends WeakReference<V>
    {
        private final int hash;
        private Entry<V> next;

        /**
         * Create new entry.
         */
        Entry(final V key, final ReferenceQueue<V> queue, final int hash, final Entry<V> next)
        {
            super(key, queue);
            this.hash = hash;
            this.next = next;
        }

        public V getKey()
        {
            return super.get();
        }

        public boolean equals(Object o)
        {
            if (!(o instanceof WeakPool.Entry))
            {
                return false;
            }
            WeakPool.Entry<V> that = (WeakPool.Entry<V>) o;
            V k1 = this.getKey();
            V k2 = that.getKey();
            return (k1==k2 || k1.equals(k2));
        }

        public int hashCode()
        {
            return this.hash;
        }

        public String toString()
        {
            return String.valueOf(this.getKey());
        }
    }
}

final class MultiSynonymKey {
    private List<MyList> keys;

    public MultiSynonymKey() {
        keys = new ArrayList<MyList>();
    }

    public MultiSynonymKey(MyList... arg) {
        keys = Arrays.asList(arg);
    }

    public List<MyList> getKeys() {
        return keys;
    }

    public int hashCode() {
        return this.getKeys().hashCode();
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof MultiSynonymKey)) {
            return false;
        }

        MultiSynonymKey that = (MultiSynonymKey) obj;
        return this.getKeys().equals(that.getKeys());
    }

    public String toString() {
        return this.getClass().getName() + this.getKeys().toString();
    }
}

public class Test extends Thread {
    static public Test test;
    static private byte[] arg1;
    static private byte[] arg2;
    static public WeakPool<MultiSynonymKey> wp;
    public volatile MultiSynonymKey ml1;
    public volatile MultiSynonymKey ml2;
    private volatile MultiSynonymKey ml3;

    public void run() {
        int count=0;
        while (true) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {}
            synchronized (wp) {
                ml2 = new MultiSynonymKey(new DoubletonList(new String(arg1), new String(arg2)));
                wp.put(ml2);
                ml3 = new MultiSynonymKey(new DoubletonList(new String(arg1), new String(arg2)));
            }
            try {
                Thread.sleep(10);
            } catch (Exception e) {}
            synchronized (wp) {
                ml1 = new MultiSynonymKey(new SingletonList(new String(arg1)));
                wp.put(ml1);
                ml3 = new MultiSynonymKey(new SingletonList(new String(arg1)));
            }
            if (count++==100)
                System.exit(95);
        }
    }

    public static void main(String[] args) throws Exception {
        wp = new WeakPool<MultiSynonymKey>();
        test = new Test();

        test.arg1 = args[0].getBytes();
        test.arg2 = args[1].getBytes();

        test.ml1 = new MultiSynonymKey(new SingletonList(new String(test.arg1)));
        test.ml2 = new MultiSynonymKey(new DoubletonList(new String(test.arg1), new String(test.arg2)));
        test.ml3 = new MultiSynonymKey(new DoubletonList(new String(test.arg1), new String(test.arg2)));

        wp.put(test.ml1);
        wp.put(test.ml2);

        test.setDaemon(true);
        test.start();

        int counter = 0;
        while (true) {
            synchronized (wp) {
                MultiSynonymKey foo = test.ml3;

                if (wp.put(foo) == foo) {
                    // System.out.println("foo " + counter);
                    // System.out.println(foo);
                }
            }
            counter++;
        }
    }

    private boolean eq(Object x, Object y) {
        return x == y || x.equals(y);
    }
}
