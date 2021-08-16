/*
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
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This is an incomplete implementation of a wrapper class
 * that places read-write locks around unsynchronized Maps.
 * Exists as a sample input for MapLoops test.
 */
public class RWMap implements Map {
    private final Map m;
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public RWMap(Map m) {
        if (m == null)
            throw new NullPointerException();
        this.m = m;
    }

    public RWMap() {
        this(new TreeMap()); // use TreeMap by default
    }

    public int size() {
        rwl.readLock().lock();
        try { return m.size(); }
        finally { rwl.readLock().unlock(); }
    }

    public boolean isEmpty() {
        rwl.readLock().lock();
        try { return m.isEmpty(); }
        finally { rwl.readLock().unlock(); }
    }

    public Object get(Object key) {
        rwl.readLock().lock();
        try { return m.get(key); }
        finally { rwl.readLock().unlock(); }
    }

    public boolean containsKey(Object key) {
        rwl.readLock().lock();
        try { return m.containsKey(key); }
        finally { rwl.readLock().unlock(); }
    }

    public boolean containsValue(Object value) {
        rwl.readLock().lock();
        try { return m.containsValue(value); }
        finally { rwl.readLock().unlock(); }
    }

    public Set keySet() { // Not implemented
        return null;
    }

    public Set entrySet() { // Not implemented
        return null;
    }

    public Collection values() { // Not implemented
        return null;
    }

    public boolean equals(Object o) {
        rwl.readLock().lock();
        try { return m.equals(o); }
        finally { rwl.readLock().unlock(); }
    }

    public int hashCode() {
        rwl.readLock().lock();
        try { return m.hashCode(); }
        finally { rwl.readLock().unlock(); }
    }

    public String toString() {
        rwl.readLock().lock();
        try { return m.toString(); }
        finally { rwl.readLock().unlock(); }
    }

    public Object put(Object key, Object value) {
        rwl.writeLock().lock();
        try { return m.put(key, value); }
        finally { rwl.writeLock().unlock(); }
    }

    public Object remove(Object key) {
        rwl.writeLock().lock();
        try { return m.remove(key); }
        finally { rwl.writeLock().unlock(); }
    }

    public void putAll(Map map) {
        rwl.writeLock().lock();
        try { m.putAll(map); }
        finally { rwl.writeLock().unlock(); }
    }

    public void clear() {
        rwl.writeLock().lock();
        try { m.clear(); }
        finally { rwl.writeLock().unlock(); }
    }

}
