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

import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.EXTRA;
import static jdk.internal.net.http.hpack.HPACK.Logger.Level.NORMAL;

/**
 * Encodes headers to their binary representation.
 *
 * <p> Typical lifecycle looks like this:
 *
 * <p> {@link #Encoder(int) new Encoder}
 * ({@link #setMaxCapacity(int) setMaxCapacity}?
 * {@link #encode(ByteBuffer) encode})*
 *
 * <p> Suppose headers are represented by {@code Map<String, List<String>>}.
 * A supplier and a consumer of {@link ByteBuffer}s in forms of
 * {@code Supplier<ByteBuffer>} and {@code Consumer<ByteBuffer>} respectively.
 * Then to encode headers, the following approach might be used:
 *
 * <pre>{@code
 *     for (Map.Entry<String, List<String>> h : headers.entrySet()) {
 *         String name = h.getKey();
 *         for (String value : h.getValue()) {
 *             encoder.header(name, value);        // Set up header
 *             boolean encoded;
 *             do {
 *                 ByteBuffer b = buffersSupplier.get();
 *                 encoded = encoder.encode(b);    // Encode the header
 *                 buffersConsumer.accept(b);
 *             } while (!encoded);
 *         }
 *     }
 * }</pre>
 *
 * <p> Though the specification <a href="https://tools.ietf.org/html/rfc7541#section-2">does not define</a>
 * how an encoder is to be implemented, a default implementation is provided by
 * the method {@link #header(CharSequence, CharSequence, boolean)}.
 *
 * <p> To provide a custom encoding implementation, {@code Encoder} has to be
 * extended. A subclass then can access methods for encoding using specific
 * representations (e.g. {@link #literal(int, CharSequence, boolean) literal},
 * {@link #indexed(int) indexed}, etc.)
 *
 * @apiNote
 *
 * <p> An Encoder provides an incremental way of encoding headers.
 * {@link #encode(ByteBuffer)} takes a buffer a returns a boolean indicating
 * whether, or not, the buffer was sufficiently sized to hold the
 * remaining of the encoded representation.
 *
 * <p> This way, there's no need to provide a buffer of a specific size, or to
 * resize (and copy) the buffer on demand, when the remaining encoded
 * representation will not fit in the buffer's remaining space. Instead, an
 * array of existing buffers can be used, prepended with a frame that encloses
 * the resulting header block afterwards.
 *
 * <p> Splitting the encoding operation into header set up and header encoding,
 * separates long lived arguments ({@code name}, {@code value},
 * {@code sensitivity}, etc.) from the short lived ones (e.g. {@code buffer}),
 * simplifying each operation itself.
 *
 * @implNote
 *
 * <p> The default implementation does not use dynamic table. It reports to a
 * coupled Decoder a size update with the value of {@code 0}, and never changes
 * it afterwards.
 *
 * @since 9
 */
public class Encoder {

    private static final AtomicLong ENCODERS_IDS = new AtomicLong();

    /* Used to calculate the number of bytes required for Huffman encoding */
    private final QuickHuffman.Writer huffmanWriter = new QuickHuffman.Writer();

    private final Logger logger;
    private final long id;
    private final IndexedWriter indexedWriter = new IndexedWriter();
    private final LiteralWriter literalWriter = new LiteralWriter();
    private final LiteralNeverIndexedWriter literalNeverIndexedWriter
            = new LiteralNeverIndexedWriter();
    private final LiteralWithIndexingWriter literalWithIndexingWriter
            = new LiteralWithIndexingWriter();
    private final SizeUpdateWriter sizeUpdateWriter = new SizeUpdateWriter();
    private final BulkSizeUpdateWriter bulkSizeUpdateWriter
            = new BulkSizeUpdateWriter();

    private BinaryRepresentationWriter writer;
    // The default implementation of Encoder does not use dynamic region of the
    // HeaderTable. Thus the performance profile should be similar to that of
    // SimpleHeaderTable.
    private final HeaderTable headerTable;

    private boolean encoding;

    private int maxCapacity;
    private int currCapacity;
    private int lastCapacity;
    private long minCapacity;
    private boolean capacityUpdate;
    private boolean configuredCapacityUpdate;

    /**
     * Constructs an {@code Encoder} with the specified maximum capacity of the
     * header table.
     *
     * <p> The value has to be agreed between decoder and encoder out-of-band,
     * e.g. by a protocol that uses HPACK
     * (see <a href="https://tools.ietf.org/html/rfc7541#section-4.2">4.2. Maximum Table Size</a>).
     *
     * @param maxCapacity
     *         a non-negative integer
     *
     * @throws IllegalArgumentException
     *         if maxCapacity is negative
     */
    public Encoder(int maxCapacity) {
        id = ENCODERS_IDS.incrementAndGet();
        this.logger = HPACK.getLogger().subLogger("Encoder#" + id);
        if (logger.isLoggable(NORMAL)) {
            logger.log(NORMAL, () -> format("new encoder with maximum table size %s",
                                            maxCapacity));
        }
        if (logger.isLoggable(EXTRA)) {
            /* To correlate with logging outside HPACK, knowing
               hashCode/toString is important */
            logger.log(EXTRA, () -> {
                String hashCode = Integer.toHexString(
                        System.identityHashCode(this));
                /* Since Encoder can be subclassed hashCode AND identity
                   hashCode might be different. So let's print both. */
                return format("toString='%s', hashCode=%s, identityHashCode=%s",
                              toString(), hashCode(), hashCode);
            });
        }
        if (maxCapacity < 0) {
            throw new IllegalArgumentException(
                    "maxCapacity >= 0: " + maxCapacity);
        }
        // Initial maximum capacity update mechanics
        minCapacity = Long.MAX_VALUE;
        currCapacity = -1;
        setMaxCapacity0(maxCapacity);
        headerTable = new HeaderTable(lastCapacity, logger.subLogger("HeaderTable"));
    }

    /**
     * Sets up the given header {@code (name, value)}.
     *
     * <p> Fixates {@code name} and {@code value} for the duration of encoding.
     *
     * @param name
     *         the name
     * @param value
     *         the value
     *
     * @throws NullPointerException
     *         if any of the arguments are {@code null}
     * @throws IllegalStateException
     *         if the encoder hasn't fully encoded the previous header, or
     *         hasn't yet started to encode it
     * @see #header(CharSequence, CharSequence, boolean)
     */
    public void header(CharSequence name, CharSequence value)
            throws IllegalStateException {
        header(name, value, false);
    }

    /**
     * Sets up the given header {@code (name, value)} with possibly sensitive
     * value.
     *
     * <p> If the {@code value} is sensitive (think security, secrecy, etc.)
     * this encoder will compress it using a special representation
     * (see <a href="https://tools.ietf.org/html/rfc7541#section-6.2.3">6.2.3.  Literal Header Field Never Indexed</a>).
     *
     * <p> Fixates {@code name} and {@code value} for the duration of encoding.
     *
     * @param name
     *         the name
     * @param value
     *         the value
     * @param sensitive
     *         whether or not the value is sensitive
     *
     * @throws NullPointerException
     *         if any of the arguments are {@code null}
     * @throws IllegalStateException
     *         if the encoder hasn't fully encoded the previous header, or
     *         hasn't yet started to encode it
     * @see #header(CharSequence, CharSequence)
     * @see DecodingCallback#onDecoded(CharSequence, CharSequence, boolean)
     */
    public void header(CharSequence name,
                       CharSequence value,
                       boolean sensitive) throws IllegalStateException {
        if (logger.isLoggable(NORMAL)) {
            logger.log(NORMAL, () -> format("encoding ('%s', '%s'), sensitive: %s",
                                            name, value, sensitive));
        }
        // Arguably a good balance between complexity of implementation and
        // efficiency of encoding
        requireNonNull(name, "name");
        requireNonNull(value, "value");
        HeaderTable t = getHeaderTable();
        int index = t.indexOf(name, value);
        if (index > 0) {
            indexed(index);
        } else {
            boolean huffmanValue = isHuffmanBetterFor(value);
            if (index < 0) {
                if (sensitive) {
                    literalNeverIndexed(-index, value, huffmanValue);
                } else {
                    literal(-index, value, huffmanValue);
                }
            } else {
                boolean huffmanName = isHuffmanBetterFor(name);
                if (sensitive) {
                    literalNeverIndexed(name, huffmanName, value, huffmanValue);
                } else {
                    literal(name, huffmanName, value, huffmanValue);
                }
            }
        }
    }

    private boolean isHuffmanBetterFor(CharSequence value) {
        // prefer Huffman encoding only if it is strictly smaller than Latin-1
        return huffmanWriter.lengthOf(value) < value.length();
    }

    /**
     * Sets a maximum capacity of the header table.
     *
     * <p> The value has to be agreed between decoder and encoder out-of-band,
     * e.g. by a protocol that uses HPACK
     * (see <a href="https://tools.ietf.org/html/rfc7541#section-4.2">4.2. Maximum Table Size</a>).
     *
     * <p> May be called any number of times after or before a complete header
     * has been encoded.
     *
     * <p> If the encoder decides to change the actual capacity, an update will
     * be encoded before a new encoding operation starts.
     *
     * @param capacity
     *         a non-negative integer
     *
     * @throws IllegalArgumentException
     *         if capacity is negative
     * @throws IllegalStateException
     *         if the encoder hasn't fully encoded the previous header, or
     *         hasn't yet started to encode it
     */
    public void setMaxCapacity(int capacity) {
        if (logger.isLoggable(NORMAL)) {
            logger.log(NORMAL, () -> format("setting maximum table size to %s",
                                            capacity));
        }
        setMaxCapacity0(capacity);
    }

    private void setMaxCapacity0(int capacity) {
        checkEncoding();
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity >= 0: " + capacity);
        }
        int calculated = calculateCapacity(capacity);
        if (logger.isLoggable(NORMAL)) {
            logger.log(NORMAL, () -> format("actual maximum table size will be %s",
                                            calculated));
        }
        if (calculated < 0 || calculated > capacity) {
            throw new IllegalArgumentException(
                    format("0 <= calculated <= capacity: calculated=%s, capacity=%s",
                           calculated, capacity));
        }
        capacityUpdate = true;
        // maxCapacity needs to be updated unconditionally, so the encoder
        // always has the newest one (in case it decides to update it later
        // unsolicitedly)
        // Suppose maxCapacity = 4096, and the encoder has decided to use only
        // 2048. It later can choose anything else from the region [0, 4096].
        maxCapacity = capacity;
        lastCapacity = calculated;
        minCapacity = Math.min(minCapacity, lastCapacity);
    }

    /**
     * Calculates actual capacity to be used by this encoder in response to
     * a request to update maximum table size.
     *
     * <p> Default implementation does not add anything to the headers table,
     * hence this method returns {@code 0}.
     *
     * <p> It is an error to return a value {@code c}, where {@code c < 0} or
     * {@code c > maxCapacity}.
     *
     * @param maxCapacity
     *         upper bound
     *
     * @return actual capacity
     */
    protected int calculateCapacity(int maxCapacity) {
        return 0;
    }

    /**
     * Encodes the {@linkplain #header(CharSequence, CharSequence) set up}
     * header into the given buffer.
     *
     * <p> The encoder writes as much as possible of the header's binary
     * representation into the given buffer, starting at the buffer's position,
     * and increments its position to reflect the bytes written. The buffer's
     * mark and limit will not be modified.
     *
     * <p> Once the method has returned {@code true}, the current header is
     * deemed encoded. A new header may be set up.
     *
     * @param headerBlock
     *         the buffer to encode the header into, may be empty
     *
     * @return {@code true} if the current header has been fully encoded,
     *         {@code false} otherwise
     *
     * @throws NullPointerException
     *         if the buffer is {@code null}
     * @throws ReadOnlyBufferException
     *         if this buffer is read-only
     * @throws IllegalStateException
     *         if there is no set up header
     */
    public final boolean encode(ByteBuffer headerBlock) {
        if (!encoding) {
            throw new IllegalStateException("A header hasn't been set up");
        }
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format("writing to %s", headerBlock));
        }
        if (!prependWithCapacityUpdate(headerBlock)) { // TODO: log
            return false;
        }
        boolean done = writer.write(headerTable, headerBlock);
        if (done) {
            writer.reset(); // FIXME: WHY?
            encoding = false;
        }
        return done;
    }

    private boolean prependWithCapacityUpdate(ByteBuffer headerBlock) {
        if (capacityUpdate) {
            if (!configuredCapacityUpdate) {
                List<Integer> sizes = new LinkedList<>();
                if (minCapacity < currCapacity) {
                    sizes.add((int) minCapacity);
                    if (minCapacity != lastCapacity) {
                        sizes.add(lastCapacity);
                    }
                } else if (lastCapacity != currCapacity) {
                    sizes.add(lastCapacity);
                }
                bulkSizeUpdateWriter.maxHeaderTableSizes(sizes);
                configuredCapacityUpdate = true;
            }
            boolean done = bulkSizeUpdateWriter.write(headerTable, headerBlock);
            if (done) {
                minCapacity = lastCapacity;
                currCapacity = lastCapacity;
                bulkSizeUpdateWriter.reset();
                capacityUpdate = false;
                configuredCapacityUpdate = false;
            }
            return done;
        }
        return true;
    }

    protected final void indexed(int index) throws IndexOutOfBoundsException {
        checkEncoding();
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format("indexed %s", index));
        }
        encoding = true;
        writer = indexedWriter.index(index);
    }

    protected final void literal(int index,
                                 CharSequence value,
                                 boolean useHuffman)
            throws IndexOutOfBoundsException {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format(
                    "literal without indexing (%s, '%s', huffman=%b)",
                    index, value, useHuffman));
        }
        checkEncoding();
        encoding = true;
        writer = literalWriter
                .index(index).value(value, useHuffman);
    }

    protected final void literal(CharSequence name,
                                 boolean nameHuffman,
                                 CharSequence value,
                                 boolean valueHuffman)
    {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format(
                    "literal without indexing ('%s', huffman=%b, '%s', huffman=%b)",
                    name, nameHuffman, value, valueHuffman));
        }
        checkEncoding();
        encoding = true;
        writer = literalWriter
                .name(name, nameHuffman).value(value, valueHuffman);
    }

    protected final void literalNeverIndexed(int index,
                                             CharSequence value,
                                             boolean valueHuffman)
            throws IndexOutOfBoundsException
    {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format(
                    "literal never indexed (%s, '%s', huffman=%b)",
                    index, value, valueHuffman));
        }
        checkEncoding();
        encoding = true;
        writer = literalNeverIndexedWriter
                .index(index).value(value, valueHuffman);
    }

    protected final void literalNeverIndexed(CharSequence name,
                                             boolean nameHuffman,
                                             CharSequence value,
                                             boolean valueHuffman) {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format(
                    "literal never indexed ('%s', huffman=%b, '%s', huffman=%b)",
                    name, nameHuffman, value, valueHuffman));
        }
        checkEncoding();
        encoding = true;
        writer = literalNeverIndexedWriter
                .name(name, nameHuffman).value(value, valueHuffman);
    }

    protected final void literalWithIndexing(int index,
                                             CharSequence value,
                                             boolean valueHuffman)
            throws IndexOutOfBoundsException {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format(
                    "literal with incremental indexing (%s, '%s', huffman=%b)",
                    index, value, valueHuffman));
        }
        checkEncoding();
        encoding = true;
        writer = literalWithIndexingWriter
                .index(index).value(value, valueHuffman);
    }

    protected final void literalWithIndexing(CharSequence name,
                                             boolean nameHuffman,
                                             CharSequence value,
                                             boolean valueHuffman) {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format(
                    "literal with incremental indexing ('%s', huffman=%b, '%s', huffman=%b)",
                    name, nameHuffman, value, valueHuffman));
        }
        checkEncoding();
        encoding = true;
        writer = literalWithIndexingWriter
                .name(name, nameHuffman).value(value, valueHuffman);
    }

    protected final void sizeUpdate(int capacity)
            throws IllegalArgumentException {
        if (logger.isLoggable(EXTRA)) {
            logger.log(EXTRA, () -> format("dynamic table size update %s",
                                           capacity));
        }
        checkEncoding();
        // Ensure subclass follows the contract
        if (capacity > this.maxCapacity) {
            throw new IllegalArgumentException(
                    format("capacity <= maxCapacity: capacity=%s, maxCapacity=%s",
                           capacity, maxCapacity));
        }
        writer = sizeUpdateWriter.maxHeaderTableSize(capacity);
    }

    protected final int getMaxCapacity() {
        return maxCapacity;
    }

    protected final HeaderTable getHeaderTable() {
        return headerTable;
    }

    protected final void checkEncoding() { // TODO: better name e.g. checkIfEncodingInProgress()
        if (encoding) {
            throw new IllegalStateException(
                    "Previous encoding operation hasn't finished yet");
        }
    }
}
