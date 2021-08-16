/*
 * Copyright (c) 1999, 2015, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.jvm.PoolConstant;
import com.sun.tools.javac.util.DefinedBy.Api;

/** An abstraction for internal compiler strings. They are stored in
 *  Utf8 format. Names are stored in a Name.Table, and are unique within
 *  that table.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public abstract class Name implements javax.lang.model.element.Name, PoolConstant {

    public final Table table;

    protected Name(Table table) {
        this.table = table;
    }

    /**
     * {@inheritDoc}
     */
    @DefinedBy(Api.LANGUAGE_MODEL)
    public boolean contentEquals(CharSequence cs) {
        return toString().equals(cs.toString());
    }

    @Override
    public int poolTag() {
        return ClassFile.CONSTANT_Utf8;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return toString().length();
    }

    /**
     * {@inheritDoc}
     */
    public char charAt(int index) {
        return toString().charAt(index);
    }

    /**
     * {@inheritDoc}
     */
    public CharSequence subSequence(int start, int end) {
        return toString().subSequence(start, end);
    }

    /** Return the concatenation of this name and name `n'.
     */
    public Name append(Name n) {
        int len = getByteLength();
        byte[] bs = new byte[len + n.getByteLength()];
        getBytes(bs, 0);
        n.getBytes(bs, len);
        return table.fromUtf(bs, 0, bs.length);
    }

    /** Return the concatenation of this name, the given ASCII
     *  character, and name `n'.
     */
    public Name append(char c, Name n) {
        int len = getByteLength();
        byte[] bs = new byte[len + 1 + n.getByteLength()];
        getBytes(bs, 0);
        bs[len] = (byte) c;
        n.getBytes(bs, len+1);
        return table.fromUtf(bs, 0, bs.length);
    }

    /** An arbitrary but consistent complete order among all Names.
     */
    public int compareTo(Name other) {
        return other.getIndex() - this.getIndex();
    }

    /** Return true if this is the empty name.
     */
    public boolean isEmpty() {
        return getByteLength() == 0;
    }

    /** Returns last occurrence of byte b in this name, -1 if not found.
     */
    public int lastIndexOf(byte b) {
        byte[] bytes = getByteArray();
        int offset = getByteOffset();
        int i = getByteLength() - 1;
        while (i >= 0 && bytes[offset + i] != b) i--;
        return i;
    }

    /** Does this name start with prefix?
     */
    public boolean startsWith(Name prefix) {
        byte[] thisBytes = this.getByteArray();
        int thisOffset   = this.getByteOffset();
        int thisLength   = this.getByteLength();
        byte[] prefixBytes = prefix.getByteArray();
        int prefixOffset   = prefix.getByteOffset();
        int prefixLength   = prefix.getByteLength();

        if (thisLength < prefixLength)
            return false;

        int i = 0;
        while (i < prefixLength &&
               thisBytes[thisOffset + i] == prefixBytes[prefixOffset + i])
            i++;
        return i == prefixLength;
    }

    /** Returns the sub-name starting at position start, up to and
     *  excluding position end.
     */
    public Name subName(int start, int end) {
        if (end < start) end = start;
        return table.fromUtf(getByteArray(), getByteOffset() + start, end - start);
    }

    /** Return the string representation of this name.
     */
    @Override
    public String toString() {
        return Convert.utf2string(getByteArray(), getByteOffset(), getByteLength());
    }

    /** Return the Utf8 representation of this name.
     */
    public byte[] toUtf() {
        byte[] bs = new byte[getByteLength()];
        getBytes(bs, 0);
        return bs;
    }

    /* Get a "reasonably small" value that uniquely identifies this name
     * within its name table.
     */
    public abstract int getIndex();

    /** Get the length (in bytes) of this name.
     */
    public abstract int getByteLength();

    /** Returns i'th byte of this name.
     */
    public abstract byte getByteAt(int i);

    /** Copy all bytes of this name to buffer cs, starting at start.
     */
    public void getBytes(byte cs[], int start) {
        System.arraycopy(getByteArray(), getByteOffset(), cs, start, getByteLength());
    }

    /** Get the underlying byte array for this name. The contents of the
     * array must not be modified.
     */
    public abstract byte[] getByteArray();

    /** Get the start offset of this name within its byte array.
     */
    public abstract int getByteOffset();

    public interface NameMapper<X> {
        X map(byte[] bytes, int offset, int len);
    }

    public <X> X map(NameMapper<X> mapper) {
        return mapper.map(getByteArray(), getByteOffset(), getByteLength());
    }

    /** An abstraction for the hash table used to create unique Name instances.
     */
    public static abstract class Table {
        /** Standard name table.
         */
        public final Names names;

        Table(Names names) {
            this.names = names;
        }

        /** Get the name from the characters in cs[start..start+len-1].
         */
        public abstract Name fromChars(char[] cs, int start, int len);

        /** Get the name for the characters in string s.
         */
        public Name fromString(String s) {
            char[] cs = s.toCharArray();
            return fromChars(cs, 0, cs.length);
        }

        /** Get the name for the bytes in array cs.
         *  Assume that bytes are in utf8 format.
         */
        public Name fromUtf(byte[] cs) {
            return fromUtf(cs, 0, cs.length);
        }

        /** get the name for the bytes in cs[start..start+len-1].
         *  Assume that bytes are in utf8 format.
         */
        public abstract Name fromUtf(byte[] cs, int start, int len);

        /** Release any resources used by this table.
         */
        public abstract void dispose();

        /** The hashcode of a name.
         */
        protected static int hashValue(byte bytes[], int offset, int length) {
            int h = 0;
            int off = offset;

            for (int i = 0; i < length; i++) {
                h = (h << 5) - h + bytes[off++];
            }
            return h;
        }

        /** Compare two subarrays
         */
        protected static boolean equals(byte[] bytes1, int offset1,
                byte[] bytes2, int offset2, int length) {
            int i = 0;
            while (i < length && bytes1[offset1 + i] == bytes2[offset2 + i]) {
                i++;
            }
            return i == length;
        }
    }
}
