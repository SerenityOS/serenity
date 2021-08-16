/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.util;

import java.lang.ref.WeakReference;

/**
 * Implementation of Name.Table that stores names in individual arrays
 * using weak references. It is recommended for use when a single shared
 * byte array is unsuitable.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class UnsharedNameTable extends Name.Table {
    public static Name.Table create(Names names) {
        return new UnsharedNameTable(names);
    }

    static class HashEntry extends WeakReference<NameImpl> {
        HashEntry next;
        HashEntry(NameImpl referent) {
            super(referent);
        }
    }

    /** The hash table for names.
     */
    private HashEntry[] hashes = null;

    /** The mask to be used for hashing
     */
    private int hashMask;

    /** Index counter for names in this table.
     */
    public int index;

    /** Allocator
     *  @param names The main name table
     *  @param hashSize the (constant) size to be used for the hash table
     *                  needs to be a power of two.
     */
    public UnsharedNameTable(Names names, int hashSize) {
        super(names);
        hashMask = hashSize - 1;
        hashes = new HashEntry[hashSize];
    }

    public UnsharedNameTable(Names names) {
        this(names, 0x8000);
    }


    @Override
    public Name fromChars(char[] cs, int start, int len) {
        byte[] name = new byte[len * 3];
        int nbytes = Convert.chars2utf(cs, start, name, 0, len);
        return fromUtf(name, 0, nbytes);
    }

    @Override
    public Name fromUtf(byte[] cs, int start, int len) {
        int h = hashValue(cs, start, len) & hashMask;

        HashEntry element = hashes[h];

        NameImpl n = null;

        HashEntry previousNonNullTableEntry = null;
        HashEntry firstTableEntry = element;

        while (element != null) {
            n = element.get();

            if (n == null) {
                if (firstTableEntry == element) {
                    hashes[h] = firstTableEntry = element.next;
                }
                else {
                    Assert.checkNonNull(previousNonNullTableEntry, "previousNonNullTableEntry cannot be null here.");
                    previousNonNullTableEntry.next = element.next;
                }
            }
            else {
                if (n.getByteLength() == len && equals(n.bytes, 0, cs, start, len)) {
                    return n;
                }
                previousNonNullTableEntry = element;
            }

            element = element.next;
        }

        byte[] bytes = new byte[len];
        System.arraycopy(cs, start, bytes, 0, len);
        n = new NameImpl(this, bytes, index++);

        HashEntry newEntry = new HashEntry(n);

        if (previousNonNullTableEntry == null) { // We are not the first name with that hashCode.
            hashes[h] = newEntry;
        }
        else {
            Assert.checkNull(previousNonNullTableEntry.next, "previousNonNullTableEntry.next must be null.");
            previousNonNullTableEntry.next = newEntry;
        }

        return n;
    }

    @Override
    public void dispose() {
        hashes = null;
    }

    static class NameImpl extends Name {
        NameImpl(UnsharedNameTable table, byte[] bytes, int index) {
            super(table);
            this.bytes = bytes;
            this.index = index;
        }

        final byte[] bytes;
        final int index;

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public int getByteLength() {
            return bytes.length;
        }

        @Override
        public byte getByteAt(int i) {
            return bytes[i];
        }

        @Override
        public byte[] getByteArray() {
            return bytes;
        }

        @Override
        public int getByteOffset() {
            return 0;
        }

    }

}
