/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import jdk.internal.net.http.hpack.HPACK.Logger;

import java.util.List;
import java.util.NoSuchElementException;

import static jdk.internal.net.http.common.Utils.pow2Size;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.EXTRA;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.NORMAL;
import static java.lang.String.format;

/*
 * A header table consists of two tables, the static table and the dynamic
 * table. Following the vocabulary of RFC 7541, the length of the header table
 * is the total number of entries in both the static and the dynamic tables.
 * The size of the table is the sum of the sizes of its dynamic table's entries.
 */
class SimpleHeaderTable {

    /* An immutable list of static header fields */
    protected static final List<HeaderField> staticTable = List.of(
            new HeaderField(""), // A dummy to make the list index 1-based, instead of 0-based
            new HeaderField(":authority"),
            new HeaderField(":method", "GET"),
            new HeaderField(":method", "POST"),
            new HeaderField(":path", "/"),
            new HeaderField(":path", "/index.html"),
            new HeaderField(":scheme", "http"),
            new HeaderField(":scheme", "https"),
            new HeaderField(":status", "200"),
            new HeaderField(":status", "204"),
            new HeaderField(":status", "206"),
            new HeaderField(":status", "304"),
            new HeaderField(":status", "400"),
            new HeaderField(":status", "404"),
            new HeaderField(":status", "500"),
            new HeaderField("accept-charset"),
            new HeaderField("accept-encoding", "gzip, deflate"),
            new HeaderField("accept-language"),
            new HeaderField("accept-ranges"),
            new HeaderField("accept"),
            new HeaderField("access-control-allow-origin"),
            new HeaderField("age"),
            new HeaderField("allow"),
            new HeaderField("authorization"),
            new HeaderField("cache-control"),
            new HeaderField("content-disposition"),
            new HeaderField("content-encoding"),
            new HeaderField("content-language"),
            new HeaderField("content-length"),
            new HeaderField("content-location"),
            new HeaderField("content-range"),
            new HeaderField("content-type"),
            new HeaderField("cookie"),
            new HeaderField("date"),
            new HeaderField("etag"),
            new HeaderField("expect"),
            new HeaderField("expires"),
            new HeaderField("from"),
            new HeaderField("host"),
            new HeaderField("if-match"),
            new HeaderField("if-modified-since"),
            new HeaderField("if-none-match"),
            new HeaderField("if-range"),
            new HeaderField("if-unmodified-since"),
            new HeaderField("last-modified"),
            new HeaderField("link"),
            new HeaderField("location"),
            new HeaderField("max-forwards"),
            new HeaderField("proxy-authenticate"),
            new HeaderField("proxy-authorization"),
            new HeaderField("range"),
            new HeaderField("referer"),
            new HeaderField("refresh"),
            new HeaderField("retry-after"),
            new HeaderField("server"),
            new HeaderField("set-cookie"),
            new HeaderField("strict-transport-security"),
            new HeaderField("transfer-encoding"),
            new HeaderField("user-agent"),
            new HeaderField("vary"),
            new HeaderField("via"),
            new HeaderField("www-authenticate"));

    protected static final int STATIC_TABLE_LENGTH = staticTable.size() - 1;
    protected static final int ENTRY_SIZE = 32;

    private final Logger logger;

    private int maxSize;
    private int size;

    public SimpleHeaderTable(int maxSize, Logger logger) {
        this.logger = logger;
        setMaxSize(maxSize);
    }

    public int size() {
        return size;
    }

    public int maxSize() {
        return maxSize;
    }

    public int length() {
        return STATIC_TABLE_LENGTH + buffer.size;
    }

    HeaderField get(int index) {
        checkIndex(index);
        if (index <= STATIC_TABLE_LENGTH) {
            return staticTable.get(index);
        } else {
            return buffer.get(index - STATIC_TABLE_LENGTH - 1);
        }
    }

    void put(CharSequence name, CharSequence value) {
        // Invoking toString() will possibly allocate Strings. But that's
        // unavoidable at this stage. If a CharSequence is going to be stored in
        // the table, it must not be mutable (e.g. for the sake of hashing).
        put(new HeaderField(name.toString(), value.toString()));
    }

    private void put(HeaderField h) {
        if (logger.isLoggable(NORMAL)) {
            logger.log(NORMAL, () -> format("adding ('%s', '%s')",
                                            h.name, h.value));
        }
        int entrySize = sizeOf(h);
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format("size of ('%s', '%s') is %s",
                                           h.name, h.value, entrySize));
        }
        while (entrySize > maxSize - size && size != 0) {
            if (logger.isLoggable(EXTRA)) {
                logger.log(EXTRA, () -> format("insufficient space %s, must evict entry",
                                               (maxSize - size)));
            }
            evictEntry();
        }
        if (entrySize > maxSize - size) {
            if (logger.isLoggable(EXTRA)) {
                logger.log(EXTRA, () -> format("not adding ('%s, '%s'), too big",
                                               h.name, h.value));
            }
            return;
        }
        size += entrySize;
        add(h);
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format("('%s, '%s') added", h.name, h.value));
            logger.log(EXTRA, this::toString);
        }
    }

    void setMaxSize(int maxSize) {
        if (maxSize < 0) {
            throw new IllegalArgumentException(
                    "maxSize >= 0: maxSize=" + maxSize);
        }
        while (maxSize < size && size != 0) {
            evictEntry();
        }
        this.maxSize = maxSize;
        // A header table cannot accommodate more entries than this
        int upperBound = maxSize / ENTRY_SIZE;
        buffer.resize(upperBound);
    }

    HeaderField evictEntry() {
        HeaderField f = remove();
        int s = sizeOf(f);
        this.size -= s;
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format("evicted entry ('%s', '%s') of size %s",
                                           f.name, f.value, s));
            logger.log(EXTRA, this::toString);
        }
        return f;
    }

    @Override
    public String toString() {
        double used = maxSize == 0 ? 0 : 100 * (((double) size) / maxSize);
        return format("dynamic length: %d, full length: %s, used space: %s/%s (%.1f%%)",
                      buffer.size, length(), size, maxSize, used);
    }

    private int checkIndex(int index) {
        int len = length();
        if (index < 1 || index > len) {
            throw new IndexOutOfBoundsException(
                    format("1 <= index <= length(): index=%s, length()=%s",
                           index, len));
        }
        return index;
    }

    int sizeOf(HeaderField f) {
        return f.name.length() + f.value.length() + ENTRY_SIZE;
    }

    //
    // Diagnostic information in the form used in the RFC 7541
    //
    String getStateString() {
        if (size == 0) {
            return "empty.";
        }

        StringBuilder b = new StringBuilder();
        for (int i = 1, size = buffer.size; i <= size; i++) {
            HeaderField e = buffer.get(i - 1);
            b.append(format("[%3d] (s = %3d) %s: %s\n", i,
                            sizeOf(e), e.name, e.value));
        }
        b.append(format("      Table size:%4s", this.size));
        return b.toString();
    }

    // Convert to a Value Object (JDK-8046159)?
    protected static final class HeaderField {

        final String name;
        final String value;

        public HeaderField(String name) {
            this(name, "");
        }

        public HeaderField(String name, String value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return value.isEmpty() ? name : name + ": " + value;
        }
    }

    private final CircularBuffer<HeaderField> buffer = new CircularBuffer<>(0);

    protected void add(HeaderField f) {
        buffer.add(f);
    }

    protected HeaderField remove() {
        return buffer.remove();
    }

    //                    head
    //                    v
    // [ ][ ][A][B][C][D][ ][ ][ ]
    //        ^
    //        tail
    //
    //       |<- size ->| (4)
    // |<------ capacity ------->| (9)
    //
    static final class CircularBuffer<E> {

        int tail, head, size, capacity;
        Object[] elements;

        CircularBuffer(int capacity) {
            this.capacity = pow2Size(capacity);
            elements = new Object[this.capacity];
        }

        void add(E elem) {
            if (size == capacity) {
                throw new IllegalStateException(
                        format("No room for '%s': capacity=%s", elem, capacity));
            }
            elements[head] = elem;
            head = (head + 1) & (capacity - 1);
            size++;
        }

        @SuppressWarnings("unchecked")
        E remove() {
            if (size == 0) {
                throw new NoSuchElementException("Empty");
            }
            E elem = (E) elements[tail];
            elements[tail] = null;
            tail = (tail + 1) & (capacity - 1);
            size--;
            return elem;
        }

        @SuppressWarnings("unchecked")
        E get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException(
                        format("0 <= index <= capacity: index=%s, capacity=%s",
                               index, capacity));
            }
            int idx = (tail + (size - index - 1)) & (capacity - 1);
            return (E) elements[idx];
        }

        public void resize(int newCapacity) {
            if (newCapacity < size) {
                throw new IllegalStateException(
                        format("newCapacity >= size: newCapacity=%s, size=%s",
                               newCapacity, size));
            }

            int capacity = pow2Size(newCapacity);
            Object[] newElements = new Object[capacity];

            if (tail < head || size == 0) {
                System.arraycopy(elements, tail, newElements, 0, size);
            } else {
                System.arraycopy(elements, tail, newElements, 0, elements.length - tail);
                System.arraycopy(elements, 0, newElements, elements.length - tail, head);
            }

            elements = newElements;
            tail = 0;
            head = size;
            this.capacity = capacity;
        }
    }
}
