/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.internal;

import java.util.BitSet;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

@SuppressWarnings("unchecked")
public final class LongMap<T> {
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final long[] EMPTY_KEYS = new long[0];
    private static final Object[] EMPTY_OBJECTS = new Object[0];
    private static final int DEFAULT_SIZE = 32;
    private static final Object NULL_OBJECT = new Object();

    private final int bitCount;
    private BitSet bitSet;
    private long[] keys = EMPTY_KEYS;
    private T[] objects = (T[]) EMPTY_OBJECTS;
    private int count;
    private int shift;

    public LongMap() {
        this.bitCount = 0;
    }

    public LongMap(int markBits) {
        this.bitCount = markBits;
        this.bitSet = new BitSet();
    }

    // Should be 2^n
    private void initialize(int capacity) {
        keys = new long[capacity];
        objects = (T[]) new Object[capacity];
        shift = 64 - (31 - Integer.numberOfLeadingZeros(capacity));
    }

    public void claimBits() {
        // flip last bit back and forth to make bitset expand to max size
        int lastBit = bitSetIndex(objects.length - 1, bitCount -1);
        bitSet.flip(lastBit);
        bitSet.flip(lastBit);
    }

    public void setId(long id, int bitIndex) {
        int bitSetIndex = bitSetIndex(tableIndexOf(id), bitIndex);
        bitSet.set(bitSetIndex, true);
    }

    public void clearId(long id, int bitIndex) {
        int bitSetIndex = bitSetIndex(tableIndexOf(id), bitIndex);
        bitSet.set(bitSetIndex, false);
    }

    public void clearId(long id) {
        int bitSetIndex = bitSetIndex(tableIndexOf(id), 0);
        for (int i = 0; i < bitCount; i++) {
            bitSet.set(bitSetIndex + i, false);
        }
    }

    public boolean isSetId(long id, int bitIndex) {
        int bitSetIndex = bitSetIndex(tableIndexOf(id), bitIndex);
        return bitSet.get(bitSetIndex);
    }

    private int bitSetIndex(int tableIndex, int bitIndex) {
        return bitCount * tableIndex + bitIndex;
    }

    private int tableIndexOf(long id) {
        int index = index(id);
        while (true) {
            if (objects[index] == null) {
                throw new InternalError("Unknown id");
            }
            if (keys[index] == id) {
                return index;
            }
            index++;
            if (index == keys.length) {
                index = 0;
            }
        }
    }

    public boolean hasKey(long id) {
        if (keys == EMPTY_KEYS) {
            return false;
        }
        int index = index(id);
        while (true) {
            if (objects[index] == null) {
               return false;
            }
            if (keys[index] == id) {
                return true;
            }
            index++;
            if (index == keys.length) {
                index = 0;
            }
        }
    }

    public void expand(int size) {
        int l = 4 * size / 3;
        if (l <= keys.length) {
            return;
        }
        int n = tableSizeFor(l);
        LongMap<T> temp = new LongMap<>(bitCount);
        temp.initialize(n);
        // Optimization, avoid growing while copying bits
        if (bitCount > 0 && !bitSet.isEmpty()) {
           temp.claimBits();
           claimBits();
        }
        for (int tIndex = 0; tIndex < keys.length; tIndex++) {
            T o = objects[tIndex];
            if (o != null) {
                long key = keys[tIndex];
                temp.put(key, o);
                if (bitCount != 0) {
                    for (int bIndex = 0; bIndex < bitCount; bIndex++) {
                        boolean bitValue = isSetId(key, bIndex);
                        if (bitValue) {
                            temp.setId(key, bIndex);
                        }
                    }
                }
            }
        }
        keys = temp.keys;
        objects = temp.objects;
        shift = temp.shift;
        bitSet = temp.bitSet;
    }

    public void put(long id, T object) {
        if (keys == EMPTY_KEYS) {
            // Lazy initialization
            initialize(DEFAULT_SIZE);
        }
        if (object == null) {
            object = (T) NULL_OBJECT;
        }

        int index = index(id);
        // probe for empty slot
        while (true) {
            if (objects[index] == null) {
                keys[index] = id;
                objects[index] = object;
                count++;
                // Don't expand lazy since it
                // can cause resize when replacing
                // an object.
                if (count > 3 * keys.length / 4) {
                    expand(2 * keys.length);
                }
                return;
            }
            // if it already exists, replace
            if (keys[index] == id) {
                objects[index] = object;
                return;
            }
            index++;
            if (index == keys.length) {
                index = 0;
            }
        }
    }
    public T getAt(int tableIndex) {
        T o =  objects[tableIndex];
        return o == NULL_OBJECT ? null : o;
    }

    public T get(long id) {
        if (keys == EMPTY_KEYS) {
            return null;
        }
        int index = index(id);
        while (true) {
            if (objects[index] == null) {
                return null;
            }
            if (keys[index] == id) {
                return getAt(index);
            }
            index++;
            if (index == keys.length) {
                index = 0;
            }
        }
    }

    private int index(long id) {
        return (int) ((id * -7046029254386353131L) >>> shift);
    }

    // Copied from HashMap::tableSizeFor
    private static final int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    public void forEachKey(LongConsumer keyTraverser) {
        for (int i = 0; i < keys.length; i++) {
            if (objects[i] != null) {
                keyTraverser.accept(keys[i]);
            }
        }
    }

    public void forEach(Consumer<T> consumer) {
        for (int i = 0; i < keys.length; i++) {
            T o = objects[i];
            if (o != null) {
                consumer.accept(o);
            }
        }
    }

    public int size() {
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            sb.append(i);
            sb.append(": id=");
            sb.append(keys[i]);
            sb.append(" ");
            sb.append(objects[i]);
            sb.append("\n");
        }
        return sb.toString();
    }
}
