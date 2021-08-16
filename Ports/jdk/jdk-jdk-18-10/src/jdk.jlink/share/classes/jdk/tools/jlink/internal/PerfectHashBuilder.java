/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

package jdk.tools.jlink.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jdk.internal.jimage.ImageStringsReader;

/*
 * The algorithm used here is outlined in Applications of Finite Automata
 * Representing Large Vocabularies - Claudio L Lucchesi and Tomasz Kowaltowski,
 * 1992, and A Practical Minimal Perfect Hashing Method - Fabiano C. Botelho1,
 * Yoshiharu Kohayakawa, and Nivio Ziviani, 2005.
 *
 * The primary JDK use of this algorithm is managing the jimage location index.
 *
 * The goal of PerfectHashBuilder is to construct an automaton which maps a
 * string key to a unique index 0..N-1, where N is the number of key-value pairs.
 * What makes MPHM effective is that the size of the lookup table is N or very
 * near N, and the minimum lookup is O(1) maximum lookup is O(2).
 *
 * The result of PerfectHashBuilder is two integer arrays, redirect and order.
 * The redirect table provides a 1-1 mapping to the order table, using the
 * reader algorithm described further on.  The order table provides a mapping
 * to entries.  If entries are fixed size and can be put in a direct table, then
 * the order table can be used to construct the direct table and then discarded.
 *
 * The steps for constructing the lookup tables are as follows;
 *
 *   - Compute an MPHM hash for each key, based on a fixed base value modulo N.
 *     Note, the hash is based on the modified UTF-8 of the key, simplifying
 *     computation in native code.
 *
 *   - Combine keys that map to the same hash code (collisions) into bucket
 *     chains.
 *
 *   - Sort bucket chains by length of chains, longest first (most collisions.)
 *     Sorting is done to pack the redirect table with the worst collision
 *     offenders first.
 *
 *   - For each chain, recompute the hash of each key using a new base value.
 *     Recomputation should give a different key distribution. A tally is kept
 *     of where the key maps, using the order table. The tally is used to detect
 *     new collisions. If there are further collisions, then restart
 *     redistribution using a different hash base value.  If a chain is
 *     successfully distributed, then the base value used to compute the hash
 *     is recorded in the redirect table.
 *
 *   - Once all colliding chains are resolved (length > 1), then the chains with
 *     only one entry are used to fill in the empty slots in the order table.
 *     These keys are recorded in the redirect table using the twos complement
 *     of the order index.
 *
 *   - It is possible that a given set of keys cannot be packed into a table of
 *     size N.  If this situation occurs then the size of the table is
 *     adjusted so that keys distribute differently.
 *
 * Readers algoritm;
 *
 *   - Compute the hash for the key using the fixed base value modulo N.  This
 *     will provide an index into the redirect table. The integer value in the
 *     redirect table will determine the next step.
 *
 *   - If the value in the redirect table is positive, then that value is used
 *     to rehash the key to get the index into the order table.
 *
 *   - If the value in the redirect table is negative, then that value is the
 *     twos complement of the index into the order table.
 *
 *   - If the value in the redirect table is zero, then there is no matching
 *     entry.
 *
 *   - Note that the resulting entry needs to be validated to ensure a match.
 *     This is typically done by comparing the key with the key in entry.
 */
public class PerfectHashBuilder<E> {
    private static final int RETRY_LIMIT = 1000;

    private Class<?> entryComponent;
    private Class<?> bucketComponent;

    private final Map<String, Entry<E>> map = new LinkedHashMap<>();
    private int[] redirect;
    private Entry<E>[] order;
    private int count = 0;

    @SuppressWarnings("EqualsAndHashcode")
    public static class Entry<E> {
        private final String key;
        private final E value;

        Entry() {
            this("", null);
        }

        Entry(String key, E value) {
            this.key = key;
            this.value = value;
        }

        String getKey() {
            return key;
        }

        E getValue() {
            return value;
        }

        int hashCode(int seed) {
            return ImageStringsReader.hashCode(key, seed);
        }

        @Override
        public int hashCode() {
            return ImageStringsReader.hashCode(key);
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            if (!(other instanceof Entry)) {
                return false;
            }
            Entry<?> entry = (Entry<?>) other;
            return entry.key.equals(key);
        }
    }

    static class Bucket<E> implements Comparable<Bucket<E>> {
        final List<Entry<E>> list = new ArrayList<>();

        void add(Entry<E> entry) {
            list.add(entry);
        }

        int getSize() {
            return list.size();
        }

        List<Entry<E>> getList() {
            return list;
        }

        Entry<E> getFirst() {
            assert !list.isEmpty() : "bucket should never be empty";
            return list.get(0);
        }

        @Override
        public int hashCode() {
            return getFirst().hashCode();
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int compareTo(Bucket<E> o) {
            return o.getSize() - getSize();
        }
    }

    public PerfectHashBuilder(Class<?> entryComponent, Class<?> bucketComponent) {
        this.entryComponent = entryComponent;
        this.bucketComponent = bucketComponent;
    }

    public int getCount() {
        return map.size();
    }

    public int[] getRedirect() {
        return redirect.clone();
    }

    public Entry<E>[] getOrder() {
        return order.clone();
    }

    public Entry<E> put(String key, E value) {
        return put(new Entry<>(key, value));
    }

    public Entry<E> put(Entry<E> entry) {
        Entry<E> old = map.put(entry.key, entry);

        if (old == null) {
            count++;
        }

        return old;
    }

    @SuppressWarnings("unchecked")
    public void generate() {
        // If the table is empty then exit early.
        boolean redo = count != 0;

        // Repeat until a valid packing is achieved.
        while (redo) {
            redo = false;

            // Allocate the resulting redirect and order tables.
            redirect = new int[count];
            order = (Entry<E>[])Array.newInstance(entryComponent, count);

            // Place all the entries in bucket chains based on hash. Sort by
            // length of chain.
            Bucket<E>[] sorted = createBuckets();
            int free = 0;

            // Iterate through the chains, longest first.
            for (Bucket<E> bucket : sorted) {
                if (bucket.getSize() != 1) {
                    // Attempt to pack entries until no collisions occur.
                    if (!collidedEntries(bucket, count)) {
                        // Failed to pack. Meed to grow table.
                        redo = true;
                        break;
                    }
                } else {
                    // A no collision entry (bucket.getSize() == 1). Find a free
                    // spot in the order table.
                    for ( ; free < count && order[free] != null; free++) {}

                    // If none found, then grow table.
                    if (free >= count) {
                        redo = true;
                        break;
                    }

                    // Store entry in order table.
                    order[free] = bucket.getFirst();
                    // Twos complement of order index stired in the redirect table.
                    redirect[(bucket.hashCode() & 0x7FFFFFFF) % count] = -1 - free;
                    // Update free slot index.
                    free++;
                }
            }

            // If packing failed, then bump table size. Make odd to increase
            // chances of being relatively prime.
            if (redo) {
                count = (count + 1) | 1;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Bucket<E>[] createBuckets() {
        // Build bucket chains based on key hash.  Collisions end up in same chain.
        Bucket<E>[] buckets = (Bucket<E>[])Array.newInstance(bucketComponent, count);

        map.values().stream().forEach((entry) -> {
            int index = (entry.hashCode() & 0x7FFFFFFF) % count;
            Bucket<E> bucket = buckets[index];

            if (bucket == null) {
                buckets[index] = bucket = new Bucket<>();
            }

            bucket.add(entry);
        });

        // Sort chains, longest first.
        Bucket<E>[] sorted = Arrays.asList(buckets).stream()
                .filter((bucket) -> (bucket != null))
                .sorted()
                .toArray((length) -> {
                    return (Bucket<E>[])Array.newInstance(bucketComponent, length);
                });

        return sorted;
    }

    private boolean collidedEntries(Bucket<E> bucket, int count) {
        // Track packing attempts.
        List<Integer> undo = new ArrayList<>();
        // Start with a new hash seed.
        int seed = ImageStringsReader.HASH_MULTIPLIER + 1;
        int retry = 0;

        // Attempt to pack all the entries in a single chain.
        redo:
        while (true) {
            for (Entry<E> entry : bucket.getList()) {
                // Compute new hash.
                int index = entry.hashCode(seed) % count;

                // If a collision is detected.
                if (order[index] != null) {
                    // Only retry so many times with current table size.
                    if (++retry > RETRY_LIMIT) {
                        return false;
                    }

                    // Undo the attempted packing.
                    undo.stream().forEach((i) -> {
                        order[i] = null;
                    });

                    // Reset the undo list and bump up the hash seed.
                    undo.clear();
                    seed++;

                    // Zero seed is not valid.
                    if (seed == 0) {
                        seed = 1;
                    }

                    // Try again.
                    continue redo;
                }

                // No collision.
                order[index] = entry;
                undo.add(index);
            }

            // Entire chain packed. Record hash seed used.
            redirect[(bucket.hashCode() & 0x7FFFFFFF) % count] = seed;

            break;
        }

        return true;
    }
 }
