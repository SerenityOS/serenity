/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.util.resources;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * ParallelListResourceBundle is another variant of ListResourceBundle
 * supporting "parallel" contents provided by another resource bundle
 * (OpenListResourceBundle). Parallel contents, if any, are added into this
 * bundle on demand.
 *
 * @author Masayoshi Okutsu
 */
public abstract class ParallelListResourceBundle extends ResourceBundle {
    private volatile ConcurrentMap<String, Object> lookup;
    private volatile Set<String> keyset;
    private final AtomicMarkableReference<Object[][]> parallelContents
            = new AtomicMarkableReference<>(null, false);

    /**
     * Sole constructor.  (For invocation by subclass constructors, typically
     * implicit.)
     */
    protected ParallelListResourceBundle() {
    }

    /**
     * Returns an array in which each item is a pair of objects in an
     * Object array. The first element of each pair is the key, which
     * must be a String, and the second element is the value
     * associated with that key. See the class description for
     * details.
     *
     * @return an array of an Object array representing a key-value pair.
     */
    protected abstract Object[][] getContents();

    /**
     * Returns the parent of this resource bundle or null if there's no parent.
     *
     * @return the parent or null if no parent
     */
    ResourceBundle getParent() {
        return parent;
    }

    /**
     * Sets the parallel contents to the data given by rb. If rb is null, this
     * bundle will be marked as `complete'.
     *
     * @param rb an OpenResourceBundle for parallel contents, or null indicating
     * there are no parallel contents for this bundle
     */
    public void setParallelContents(OpenListResourceBundle rb) {
        if (rb == null) {
            parallelContents.compareAndSet(null, null, false, true);
        } else {
            parallelContents.compareAndSet(null, rb.getContents(), false, false);
        }
    }

    /**
     * Returns true if any parallel contents have been set or if this bundle is
     * marked as complete.
     *
     * @return true if any parallel contents have been processed
     */
    boolean areParallelContentsComplete() {
        // Quick check for `complete'
        if (parallelContents.isMarked()) {
            return true;
        }
        boolean[] done = new boolean[1];
        Object[][] data = parallelContents.get(done);
        return data != null || done[0];
    }

    @Override
    protected Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }

        loadLookupTablesIfNecessary();
        return lookup.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(keySet());
    }

    @Override
    public boolean containsKey(String key) {
        return keySet().contains(key);
    }

    @Override
    protected Set<String> handleKeySet() {
        loadLookupTablesIfNecessary();
        return lookup.keySet();
    }

    @Override
    @SuppressWarnings("UnusedAssignment")
    public Set<String> keySet() {
        Set<String> ks;
        while ((ks = keyset) == null) {
            ks = new KeySet(handleKeySet(), parent);
            synchronized (this) {
                if (keyset == null) {
                    keyset = ks;
                }
            }
        }
        return ks;
    }

    /**
     * Discards any cached keyset value. This method is called from
     * LocaleData for re-creating a KeySet.
     */
    synchronized void resetKeySet() {
        keyset = null;
    }

    /**
     * Loads the lookup table if they haven't been loaded already.
     */
    void loadLookupTablesIfNecessary() {
        ConcurrentMap<String, Object> map = lookup;
        if (map == null) {
            map = new ConcurrentHashMap<>();
            for (Object[] item : getContents()) {
                map.put((String) item[0], item[1]);
            }
        }

        // If there's any parallel contents data, merge the data into map.
        Object[][] data = parallelContents.getReference();
        if (data != null) {
            for (Object[] item : data) {
                map.putIfAbsent((String) item[0], item[1]);
            }
            parallelContents.set(null, true);
        }
        if (lookup == null) {
            synchronized (this) {
                if (lookup == null) {
                    lookup = map;
                }
            }
        }
    }

    /**
     * This class implements the Set interface for
     * ParallelListResourceBundle methods.
     */
    private static class KeySet extends AbstractSet<String> {
        private final Set<String> set;
        private final ResourceBundle parent;

        private KeySet(Set<String> set, ResourceBundle parent) {
            this.set = set;
            this.parent = parent;
        }

        @Override
        public boolean contains(Object o) {
            if (set.contains(o)) {
                return true;
            }
            return (parent != null) ? parent.containsKey((String) o) : false;
        }

        @Override
        public Iterator<String> iterator() {
            if (parent == null) {
                return set.iterator();
            }
            return new Iterator<>() {
                private Iterator<String> itr = set.iterator();
                private boolean usingParent;

                @Override
                public boolean hasNext() {
                    if (itr.hasNext()) {
                        return true;
                    }
                    if (!usingParent) {
                        Set<String> nextset = new HashSet<>(parent.keySet());
                        nextset.removeAll(set);
                        itr = nextset.iterator();
                        usingParent = true;
                    }
                    return itr.hasNext();
                }

                @Override
                public String next() {
                    if (hasNext()) {
                        return itr.next();
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            if (parent == null) {
                return set.size();
            }
            Set<String> allset = new HashSet<>(set);
            allset.addAll(parent.keySet());
            return allset.size();
        }
    }
}
