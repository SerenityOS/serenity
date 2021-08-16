/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package java.util.zip;

import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Objects;

import jdk.internal.ref.CleanerFactory;
import jdk.internal.util.Preconditions;
import sun.nio.ch.DirectBuffer;

/**
 * This class provides support for general purpose compression using the
 * popular ZLIB compression library. The ZLIB compression library was
 * initially developed as part of the PNG graphics standard and is not
 * protected by patents. It is fully described in the specifications at
 * the <a href="package-summary.html#package-description">java.util.zip
 * package description</a>.
 * <p>
 * This class deflates sequences of bytes into ZLIB compressed data format.
 * The input byte sequence is provided in either byte array or byte buffer,
 * via one of the {@code setInput()} methods. The output byte sequence is
 * written to the output byte array or byte buffer passed to the
 * {@code deflate()} methods.
 * <p>
 * The following code fragment demonstrates a trivial compression
 * and decompression of a string using {@code Deflater} and
 * {@code Inflater}.
 *
 * <blockquote><pre>
 * try {
 *     // Encode a String into bytes
 *     String inputString = "blahblahblah";
 *     byte[] input = inputString.getBytes("UTF-8");
 *
 *     // Compress the bytes
 *     byte[] output = new byte[100];
 *     Deflater compresser = new Deflater();
 *     compresser.setInput(input);
 *     compresser.finish();
 *     int compressedDataLength = compresser.deflate(output);
 *     compresser.end();
 *
 *     // Decompress the bytes
 *     Inflater decompresser = new Inflater();
 *     decompresser.setInput(output, 0, compressedDataLength);
 *     byte[] result = new byte[100];
 *     int resultLength = decompresser.inflate(result);
 *     decompresser.end();
 *
 *     // Decode the bytes into a String
 *     String outputString = new String(result, 0, resultLength, "UTF-8");
 * } catch (java.io.UnsupportedEncodingException ex) {
 *     // handle
 * } catch (java.util.zip.DataFormatException ex) {
 *     // handle
 * }
 * </pre></blockquote>
 *
 * @apiNote
 * To release resources used by this {@code Deflater}, the {@link #end()} method
 * should be called explicitly. Subclasses are responsible for the cleanup of resources
 * acquired by the subclass. Subclasses that override {@link #finalize()} in order
 * to perform cleanup should be modified to use alternative cleanup mechanisms such
 * as {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 *
 * @see         Inflater
 * @author      David Connelly
 * @since 1.1
 */

public class Deflater {

    private final DeflaterZStreamRef zsRef;
    private ByteBuffer input = ZipUtils.defaultBuf;
    private byte[] inputArray;
    private int inputPos, inputLim;
    private int level, strategy;
    private boolean setParams;
    private boolean finish, finished;
    private long bytesRead;
    private long bytesWritten;

    /**
     * Compression method for the deflate algorithm (the only one currently
     * supported).
     */
    public static final int DEFLATED = 8;

    /**
     * Compression level for no compression.
     */
    public static final int NO_COMPRESSION = 0;

    /**
     * Compression level for fastest compression.
     */
    public static final int BEST_SPEED = 1;

    /**
     * Compression level for best compression.
     */
    public static final int BEST_COMPRESSION = 9;

    /**
     * Default compression level.
     */
    public static final int DEFAULT_COMPRESSION = -1;

    /**
     * Compression strategy best used for data consisting mostly of small
     * values with a somewhat random distribution. Forces more Huffman coding
     * and less string matching.
     */
    public static final int FILTERED = 1;

    /**
     * Compression strategy for Huffman coding only.
     */
    public static final int HUFFMAN_ONLY = 2;

    /**
     * Default compression strategy.
     */
    public static final int DEFAULT_STRATEGY = 0;

    /**
     * Compression flush mode used to achieve best compression result.
     *
     * @see Deflater#deflate(byte[], int, int, int)
     * @since 1.7
     */
    public static final int NO_FLUSH = 0;

    /**
     * Compression flush mode used to flush out all pending output; may
     * degrade compression for some compression algorithms.
     *
     * @see Deflater#deflate(byte[], int, int, int)
     * @since 1.7
     */
    public static final int SYNC_FLUSH = 2;

    /**
     * Compression flush mode used to flush out all pending output and
     * reset the deflater. Using this mode too often can seriously degrade
     * compression.
     *
     * @see Deflater#deflate(byte[], int, int, int)
     * @since 1.7
     */
    public static final int FULL_FLUSH = 3;

    /**
     * Flush mode to use at the end of output.  Can only be provided by the
     * user by way of {@link #finish()}.
     */
    private static final int FINISH = 4;

    static {
        ZipUtils.loadLibrary();
    }

    /**
     * Creates a new compressor using the specified compression level.
     * If 'nowrap' is true then the ZLIB header and checksum fields will
     * not be used in order to support the compression format used in
     * both GZIP and PKZIP.
     * @param level the compression level (0-9)
     * @param nowrap if true then use GZIP compatible compression
     */
    public Deflater(int level, boolean nowrap) {
        this.level = level;
        this.strategy = DEFAULT_STRATEGY;
        this.zsRef = new DeflaterZStreamRef(this,
                init(level, DEFAULT_STRATEGY, nowrap));
    }

    /**
     * Creates a new compressor using the specified compression level.
     * Compressed data will be generated in ZLIB format.
     * @param level the compression level (0-9)
     */
    public Deflater(int level) {
        this(level, false);
    }

    /**
     * Creates a new compressor with the default compression level.
     * Compressed data will be generated in ZLIB format.
     */
    public Deflater() {
        this(DEFAULT_COMPRESSION, false);
    }

    /**
     * Sets input data for compression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * @param input the input data bytes
     * @param off the start offset of the data
     * @param len the length of the data
     * @see Deflater#needsInput
     */
    public void setInput(byte[] input, int off, int len) {
        Preconditions.checkFromIndexSize(len, off, input.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (zsRef) {
            this.input = null;
            this.inputArray = input;
            this.inputPos = off;
            this.inputLim = off + len;
        }
    }

    /**
     * Sets input data for compression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * @param input the input data bytes
     * @see Deflater#needsInput
     */
    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    /**
     * Sets input data for compression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * <p>
     * The given buffer's position will be advanced as deflate
     * operations are performed, up to the buffer's limit.
     * The input buffer may be modified (refilled) between deflate
     * operations; doing so is equivalent to creating a new buffer
     * and setting it with this method.
     * <p>
     * Modifying the input buffer's contents, position, or limit
     * concurrently with an deflate operation will result in
     * undefined behavior, which may include incorrect operation
     * results or operation failure.
     *
     * @param input the input data bytes
     * @see Deflater#needsInput
     * @since 11
     */
    public void setInput(ByteBuffer input) {
        Objects.requireNonNull(input);
        synchronized (zsRef) {
            this.input = input;
            this.inputArray = null;
        }
    }

    /**
     * Sets preset dictionary for compression. A preset dictionary is used
     * when the history buffer can be predetermined. When the data is later
     * uncompressed with Inflater.inflate(), Inflater.getAdler() can be called
     * in order to get the Adler-32 value of the dictionary required for
     * decompression.
     * @param dictionary the dictionary data bytes
     * @param off the start offset of the data
     * @param len the length of the data
     * @see Inflater#inflate
     * @see Inflater#getAdler
     */
    public void setDictionary(byte[] dictionary, int off, int len) {
        Preconditions.checkFromIndexSize(len, off, dictionary.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (zsRef) {
            ensureOpen();
            setDictionary(zsRef.address(), dictionary, off, len);
        }
    }

    /**
     * Sets preset dictionary for compression. A preset dictionary is used
     * when the history buffer can be predetermined. When the data is later
     * uncompressed with Inflater.inflate(), Inflater.getAdler() can be called
     * in order to get the Adler-32 value of the dictionary required for
     * decompression.
     * @param dictionary the dictionary data bytes
     * @see Inflater#inflate
     * @see Inflater#getAdler
     */
    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }

    /**
     * Sets preset dictionary for compression. A preset dictionary is used
     * when the history buffer can be predetermined. When the data is later
     * uncompressed with Inflater.inflate(), Inflater.getAdler() can be called
     * in order to get the Adler-32 value of the dictionary required for
     * decompression.
     * <p>
     * The bytes in given byte buffer will be fully consumed by this method.  On
     * return, its position will equal its limit.
     *
     * @param dictionary the dictionary data bytes
     * @see Inflater#inflate
     * @see Inflater#getAdler
     */
    public void setDictionary(ByteBuffer dictionary) {
        synchronized (zsRef) {
            int position = dictionary.position();
            int remaining = Math.max(dictionary.limit() - position, 0);
            ensureOpen();
            if (dictionary.isDirect()) {
                long address = ((DirectBuffer) dictionary).address();
                try {
                    setDictionaryBuffer(zsRef.address(), address + position, remaining);
                } finally {
                    Reference.reachabilityFence(dictionary);
                }
            } else {
                byte[] array = ZipUtils.getBufferArray(dictionary);
                int offset = ZipUtils.getBufferOffset(dictionary);
                setDictionary(zsRef.address(), array, offset + position, remaining);
            }
            dictionary.position(position + remaining);
        }
    }

    /**
     * Sets the compression strategy to the specified value.
     *
     * <p> If the compression strategy is changed, the next invocation
     * of {@code deflate} will compress the input available so far with
     * the old strategy (and may be flushed); the new strategy will take
     * effect only after that invocation.
     *
     * @param strategy the new compression strategy
     * @throws    IllegalArgumentException if the compression strategy is
     *                                     invalid
     */
    public void setStrategy(int strategy) {
        switch (strategy) {
          case DEFAULT_STRATEGY:
          case FILTERED:
          case HUFFMAN_ONLY:
            break;
          default:
            throw new IllegalArgumentException();
        }
        synchronized (zsRef) {
            if (this.strategy != strategy) {
                this.strategy = strategy;
                setParams = true;
            }
        }
    }

    /**
     * Sets the compression level to the specified value.
     *
     * <p> If the compression level is changed, the next invocation
     * of {@code deflate} will compress the input available so far
     * with the old level (and may be flushed); the new level will
     * take effect only after that invocation.
     *
     * @param level the new compression level (0-9)
     * @throws    IllegalArgumentException if the compression level is invalid
     */
    public void setLevel(int level) {
        if ((level < 0 || level > 9) && level != DEFAULT_COMPRESSION) {
            throw new IllegalArgumentException("invalid compression level");
        }
        synchronized (zsRef) {
            if (this.level != level) {
                this.level = level;
                setParams = true;
            }
        }
    }

    /**
     * Returns true if no data remains in the input buffer. This can
     * be used to determine if one of the {@code setInput()} methods should be
     * called in order to provide more input.
     *
     * @return true if the input data buffer is empty and setInput()
     * should be called in order to provide more input
     */
    public boolean needsInput() {
        synchronized (zsRef) {
            ByteBuffer input = this.input;
            return input == null ? inputLim == inputPos : ! input.hasRemaining();
        }
    }

    /**
     * When called, indicates that compression should end with the current
     * contents of the input buffer.
     */
    public void finish() {
        synchronized (zsRef) {
            finish = true;
        }
    }

    /**
     * Returns true if the end of the compressed data output stream has
     * been reached.
     * @return true if the end of the compressed data output stream has
     * been reached
     */
    public boolean finished() {
        synchronized (zsRef) {
            return finished;
        }
    }

    /**
     * Compresses the input data and fills specified buffer with compressed
     * data. Returns actual number of bytes of compressed data. A return value
     * of 0 indicates that {@link #needsInput() needsInput} should be called
     * in order to determine if more input data is required.
     *
     * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
     * An invocation of this method of the form {@code deflater.deflate(b, off, len)}
     * yields the same result as the invocation of
     * {@code deflater.deflate(b, off, len, Deflater.NO_FLUSH)}.
     *
     * @param output the buffer for the compressed data
     * @param off the start offset of the data
     * @param len the maximum number of bytes of compressed data
     * @return the actual number of bytes of compressed data written to the
     *         output buffer
     */
    public int deflate(byte[] output, int off, int len) {
        return deflate(output, off, len, NO_FLUSH);
    }

    /**
     * Compresses the input data and fills specified buffer with compressed
     * data. Returns actual number of bytes of compressed data. A return value
     * of 0 indicates that {@link #needsInput() needsInput} should be called
     * in order to determine if more input data is required.
     *
     * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
     * An invocation of this method of the form {@code deflater.deflate(b)}
     * yields the same result as the invocation of
     * {@code deflater.deflate(b, 0, b.length, Deflater.NO_FLUSH)}.
     *
     * @param output the buffer for the compressed data
     * @return the actual number of bytes of compressed data written to the
     *         output buffer
     */
    public int deflate(byte[] output) {
        return deflate(output, 0, output.length, NO_FLUSH);
    }

    /**
     * Compresses the input data and fills specified buffer with compressed
     * data. Returns actual number of bytes of compressed data. A return value
     * of 0 indicates that {@link #needsInput() needsInput} should be called
     * in order to determine if more input data is required.
     *
     * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
     * An invocation of this method of the form {@code deflater.deflate(output)}
     * yields the same result as the invocation of
     * {@code deflater.deflate(output, Deflater.NO_FLUSH)}.
     *
     * @param output the buffer for the compressed data
     * @return the actual number of bytes of compressed data written to the
     *         output buffer
     * @since 11
     */
    public int deflate(ByteBuffer output) {
        return deflate(output, NO_FLUSH);
    }

    /**
     * Compresses the input data and fills the specified buffer with compressed
     * data. Returns actual number of bytes of data compressed.
     *
     * <p>Compression flush mode is one of the following three modes:
     *
     * <ul>
     * <li>{@link #NO_FLUSH}: allows the deflater to decide how much data
     * to accumulate, before producing output, in order to achieve the best
     * compression (should be used in normal use scenario). A return value
     * of 0 in this flush mode indicates that {@link #needsInput()} should
     * be called in order to determine if more input data is required.
     *
     * <li>{@link #SYNC_FLUSH}: all pending output in the deflater is flushed,
     * to the specified output buffer, so that an inflater that works on
     * compressed data can get all input data available so far (In particular
     * the {@link #needsInput()} returns {@code true} after this invocation
     * if enough output space is provided). Flushing with {@link #SYNC_FLUSH}
     * may degrade compression for some compression algorithms and so it
     * should be used only when necessary.
     *
     * <li>{@link #FULL_FLUSH}: all pending output is flushed out as with
     * {@link #SYNC_FLUSH}. The compression state is reset so that the inflater
     * that works on the compressed output data can restart from this point
     * if previous compressed data has been damaged or if random access is
     * desired. Using {@link #FULL_FLUSH} too often can seriously degrade
     * compression.
     * </ul>
     *
     * <p>In the case of {@link #FULL_FLUSH} or {@link #SYNC_FLUSH}, if
     * the return value is {@code len}, the space available in output
     * buffer {@code b}, this method should be invoked again with the same
     * {@code flush} parameter and more output space. Make sure that
     * {@code len} is greater than 6 to avoid flush marker (5 bytes) being
     * repeatedly output to the output buffer every time this method is
     * invoked.
     *
     * <p>If the {@link #setInput(ByteBuffer)} method was called to provide a buffer
     * for input, the input buffer's position will be advanced by the number of bytes
     * consumed by this operation.
     *
     * @param output the buffer for the compressed data
     * @param off the start offset of the data
     * @param len the maximum number of bytes of compressed data
     * @param flush the compression flush mode
     * @return the actual number of bytes of compressed data written to
     *         the output buffer
     *
     * @throws IllegalArgumentException if the flush mode is invalid
     * @since 1.7
     */
    public int deflate(byte[] output, int off, int len, int flush) {
        Preconditions.checkFromIndexSize(len, off, output.length, Preconditions.AIOOBE_FORMATTER);
        if (flush != NO_FLUSH && flush != SYNC_FLUSH && flush != FULL_FLUSH) {
            throw new IllegalArgumentException();
        }
        synchronized (zsRef) {
            ensureOpen();

            ByteBuffer input = this.input;
            if (finish) {
                // disregard given flush mode in this case
                flush = FINISH;
            }
            int params;
            if (setParams) {
                // bit 0: true to set params
                // bit 1-2: strategy (0, 1, or 2)
                // bit 3-31: level (0..9 or -1)
                params = 1 | strategy << 1 | level << 3;
            } else {
                params = 0;
            }
            int inputPos;
            long result;
            if (input == null) {
                inputPos = this.inputPos;
                result = deflateBytesBytes(zsRef.address(),
                    inputArray, inputPos, inputLim - inputPos,
                    output, off, len,
                    flush, params);
            } else {
                inputPos = input.position();
                int inputRem = Math.max(input.limit() - inputPos, 0);
                if (input.isDirect()) {
                    try {
                        long inputAddress = ((DirectBuffer) input).address();
                        result = deflateBufferBytes(zsRef.address(),
                            inputAddress + inputPos, inputRem,
                            output, off, len,
                            flush, params);
                    } finally {
                        Reference.reachabilityFence(input);
                    }
                } else {
                    byte[] inputArray = ZipUtils.getBufferArray(input);
                    int inputOffset = ZipUtils.getBufferOffset(input);
                    result = deflateBytesBytes(zsRef.address(),
                        inputArray, inputOffset + inputPos, inputRem,
                        output, off, len,
                        flush, params);
                }
            }
            int read = (int) (result & 0x7fff_ffffL);
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            if ((result >>> 62 & 1) != 0) {
                finished = true;
            }
            if (params != 0 && (result >>> 63 & 1) == 0) {
                setParams = false;
            }
            if (input != null) {
                input.position(inputPos + read);
            } else {
                this.inputPos = inputPos + read;
            }
            bytesWritten += written;
            bytesRead += read;
            return written;
        }
    }

    /**
     * Compresses the input data and fills the specified buffer with compressed
     * data. Returns actual number of bytes of data compressed.
     *
     * <p>Compression flush mode is one of the following three modes:
     *
     * <ul>
     * <li>{@link #NO_FLUSH}: allows the deflater to decide how much data
     * to accumulate, before producing output, in order to achieve the best
     * compression (should be used in normal use scenario). A return value
     * of 0 in this flush mode indicates that {@link #needsInput()} should
     * be called in order to determine if more input data is required.
     *
     * <li>{@link #SYNC_FLUSH}: all pending output in the deflater is flushed,
     * to the specified output buffer, so that an inflater that works on
     * compressed data can get all input data available so far (In particular
     * the {@link #needsInput()} returns {@code true} after this invocation
     * if enough output space is provided). Flushing with {@link #SYNC_FLUSH}
     * may degrade compression for some compression algorithms and so it
     * should be used only when necessary.
     *
     * <li>{@link #FULL_FLUSH}: all pending output is flushed out as with
     * {@link #SYNC_FLUSH}. The compression state is reset so that the inflater
     * that works on the compressed output data can restart from this point
     * if previous compressed data has been damaged or if random access is
     * desired. Using {@link #FULL_FLUSH} too often can seriously degrade
     * compression.
     * </ul>
     *
     * <p>In the case of {@link #FULL_FLUSH} or {@link #SYNC_FLUSH}, if
     * the return value is equal to the {@linkplain ByteBuffer#remaining() remaining space}
     * of the buffer, this method should be invoked again with the same
     * {@code flush} parameter and more output space. Make sure that
     * the buffer has at least 6 bytes of remaining space to avoid the
     * flush marker (5 bytes) being repeatedly output to the output buffer
     * every time this method is invoked.
     *
     * <p>On success, the position of the given {@code output} byte buffer will be
     * advanced by as many bytes as were produced by the operation, which is equal
     * to the number returned by this method.
     *
     * <p>If the {@link #setInput(ByteBuffer)} method was called to provide a buffer
     * for input, the input buffer's position will be advanced by the number of bytes
     * consumed by this operation.
     *
     * @param output the buffer for the compressed data
     * @param flush the compression flush mode
     * @return the actual number of bytes of compressed data written to
     *         the output buffer
     *
     * @throws IllegalArgumentException if the flush mode is invalid
     * @since 11
     */
    public int deflate(ByteBuffer output, int flush) {
        if (output.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        if (flush != NO_FLUSH && flush != SYNC_FLUSH && flush != FULL_FLUSH) {
            throw new IllegalArgumentException();
        }
        synchronized (zsRef) {
            ensureOpen();

            ByteBuffer input = this.input;
            if (finish) {
                // disregard given flush mode in this case
                flush = FINISH;
            }
            int params;
            if (setParams) {
                // bit 0: true to set params
                // bit 1-2: strategy (0, 1, or 2)
                // bit 3-31: level (0..9 or -1)
                params = 1 | strategy << 1 | level << 3;
            } else {
                params = 0;
            }
            int outputPos = output.position();
            int outputRem = Math.max(output.limit() - outputPos, 0);
            int inputPos;
            long result;
            if (input == null) {
                inputPos = this.inputPos;
                if (output.isDirect()) {
                    long outputAddress = ((DirectBuffer) output).address();
                    try {
                        result = deflateBytesBuffer(zsRef.address(),
                            inputArray, inputPos, inputLim - inputPos,
                            outputAddress + outputPos, outputRem,
                            flush, params);
                    } finally {
                        Reference.reachabilityFence(output);
                    }
                } else {
                    byte[] outputArray = ZipUtils.getBufferArray(output);
                    int outputOffset = ZipUtils.getBufferOffset(output);
                    result = deflateBytesBytes(zsRef.address(),
                        inputArray, inputPos, inputLim - inputPos,
                        outputArray, outputOffset + outputPos, outputRem,
                        flush, params);
                }
            } else {
                inputPos = input.position();
                int inputRem = Math.max(input.limit() - inputPos, 0);
                if (input.isDirect()) {
                    long inputAddress = ((DirectBuffer) input).address();
                    try {
                        if (output.isDirect()) {
                            long outputAddress = outputPos + ((DirectBuffer) output).address();
                            try {
                                result = deflateBufferBuffer(zsRef.address(),
                                    inputAddress + inputPos, inputRem,
                                    outputAddress, outputRem,
                                    flush, params);
                            } finally {
                                Reference.reachabilityFence(output);
                            }
                        } else {
                            byte[] outputArray = ZipUtils.getBufferArray(output);
                            int outputOffset = ZipUtils.getBufferOffset(output);
                            result = deflateBufferBytes(zsRef.address(),
                                inputAddress + inputPos, inputRem,
                                outputArray, outputOffset + outputPos, outputRem,
                                flush, params);
                        }
                    } finally {
                        Reference.reachabilityFence(input);
                    }
                } else {
                    byte[] inputArray = ZipUtils.getBufferArray(input);
                    int inputOffset = ZipUtils.getBufferOffset(input);
                    if (output.isDirect()) {
                        long outputAddress = ((DirectBuffer) output).address();
                        try {
                            result = deflateBytesBuffer(zsRef.address(),
                                inputArray, inputOffset + inputPos, inputRem,
                                outputAddress + outputPos, outputRem,
                                flush, params);
                        } finally {
                            Reference.reachabilityFence(output);
                        }
                    } else {
                        byte[] outputArray = ZipUtils.getBufferArray(output);
                        int outputOffset = ZipUtils.getBufferOffset(output);
                        result = deflateBytesBytes(zsRef.address(),
                            inputArray, inputOffset + inputPos, inputRem,
                            outputArray, outputOffset + outputPos, outputRem,
                            flush, params);
                    }
                }
            }
            int read = (int) (result & 0x7fff_ffffL);
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            if ((result >>> 62 & 1) != 0) {
                finished = true;
            }
            if (params != 0 && (result >>> 63 & 1) == 0) {
                setParams = false;
            }
            if (input != null) {
                input.position(inputPos + read);
            } else {
                this.inputPos = inputPos + read;
            }
            output.position(outputPos + written);
            bytesWritten += written;
            bytesRead += read;
            return written;
        }
    }

    /**
     * Returns the ADLER-32 value of the uncompressed data.
     * @return the ADLER-32 value of the uncompressed data
     */
    public int getAdler() {
        synchronized (zsRef) {
            ensureOpen();
            return getAdler(zsRef.address());
        }
    }

    /**
     * Returns the total number of uncompressed bytes input so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesRead()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of uncompressed bytes input so far
     */
    public int getTotalIn() {
        return (int) getBytesRead();
    }

    /**
     * Returns the total number of uncompressed bytes input so far.
     *
     * @return the total (non-negative) number of uncompressed bytes input so far
     * @since 1.5
     */
    public long getBytesRead() {
        synchronized (zsRef) {
            ensureOpen();
            return bytesRead;
        }
    }

    /**
     * Returns the total number of compressed bytes output so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesWritten()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of compressed bytes output so far
     */
    public int getTotalOut() {
        return (int) getBytesWritten();
    }

    /**
     * Returns the total number of compressed bytes output so far.
     *
     * @return the total (non-negative) number of compressed bytes output so far
     * @since 1.5
     */
    public long getBytesWritten() {
        synchronized (zsRef) {
            ensureOpen();
            return bytesWritten;
        }
    }

    /**
     * Resets deflater so that a new set of input data can be processed.
     * Keeps current compression level and strategy settings.
     */
    public void reset() {
        synchronized (zsRef) {
            ensureOpen();
            reset(zsRef.address());
            finish = false;
            finished = false;
            input = ZipUtils.defaultBuf;
            inputArray = null;
            bytesRead = bytesWritten = 0;
        }
    }

    /**
     * Closes the compressor and discards any unprocessed input.
     *
     * This method should be called when the compressor is no longer
     * being used. Once this method is called, the behavior of the
     * Deflater object is undefined.
     */
    public void end() {
        synchronized (zsRef) {
            zsRef.clean();
            input = ZipUtils.defaultBuf;
        }
    }

    private void ensureOpen() {
        assert Thread.holdsLock(zsRef);
        if (zsRef.address() == 0)
            throw new NullPointerException("Deflater has been closed");
    }

    private static native long init(int level, int strategy, boolean nowrap);
    private static native void setDictionary(long addr, byte[] b, int off,
                                             int len);
    private static native void setDictionaryBuffer(long addr, long bufAddress, int len);
    private native long deflateBytesBytes(long addr,
        byte[] inputArray, int inputOff, int inputLen,
        byte[] outputArray, int outputOff, int outputLen,
        int flush, int params);
    private native long deflateBytesBuffer(long addr,
        byte[] inputArray, int inputOff, int inputLen,
        long outputAddress, int outputLen,
        int flush, int params);
    private native long deflateBufferBytes(long addr,
        long inputAddress, int inputLen,
        byte[] outputArray, int outputOff, int outputLen,
        int flush, int params);
    private native long deflateBufferBuffer(long addr,
        long inputAddress, int inputLen,
        long outputAddress, int outputLen,
        int flush, int params);
    private static native int getAdler(long addr);
    private static native void reset(long addr);
    private static native void end(long addr);

    /**
     * A reference to the native zlib's z_stream structure. It also
     * serves as the "cleaner" to clean up the native resource when
     * the Deflater is ended, closed or cleaned.
     */
    static class DeflaterZStreamRef implements Runnable {

        private long address;
        private final Cleanable cleanable;

        private DeflaterZStreamRef(Deflater owner, long addr) {
            this.cleanable = (owner != null) ? CleanerFactory.cleaner().register(owner, this) : null;
            this.address = addr;
        }

        long address() {
            return address;
        }

        void clean() {
            cleanable.clean();
        }

        public synchronized void run() {
            long addr = address;
            address = 0;
            if (addr != 0) {
                end(addr);
            }
        }

    }
}
