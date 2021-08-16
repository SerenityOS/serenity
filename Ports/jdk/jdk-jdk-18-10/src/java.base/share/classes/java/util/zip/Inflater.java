/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * This class provides support for general purpose decompression using the
 * popular ZLIB compression library. The ZLIB compression library was
 * initially developed as part of the PNG graphics standard and is not
 * protected by patents. It is fully described in the specifications at
 * the <a href="package-summary.html#package-description">java.util.zip
 * package description</a>.
 * <p>
 * This class inflates sequences of ZLIB compressed bytes. The input byte
 * sequence is provided in either byte array or byte buffer, via one of the
 * {@code setInput()} methods. The output byte sequence is written to the
 * output byte array or byte buffer passed to the {@code deflate()} methods.
 * <p>
 * The following code fragment demonstrates a trivial compression
 * and decompression of a string using {@code Deflater} and
 * {@code Inflater}.
 *
 * <blockquote><pre>
 * try {
 *     // Encode a String into bytes
 *     String inputString = "blahblahblah\u20AC\u20AC";
 *     byte[] input = inputString.getBytes("UTF-8");
 *
 *     // Compress the bytes
 *     byte[] output = new byte[100];
 *     Deflater compresser = new Deflater();
 *     compresser.setInput(input);
 *     compresser.finish();
 *     int compressedDataLength = compresser.deflate(output);
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
 * To release resources used by this {@code Inflater}, the {@link #end()} method
 * should be called explicitly. Subclasses are responsible for the cleanup of resources
 * acquired by the subclass. Subclasses that override {@link #finalize()} in order
 * to perform cleanup should be modified to use alternative cleanup mechanisms such
 * as {@link java.lang.ref.Cleaner} and remove the overriding {@code finalize} method.
 *
 * @see         Deflater
 * @author      David Connelly
 * @since 1.1
 *
 */

public class Inflater {

    private final InflaterZStreamRef zsRef;
    private ByteBuffer input = ZipUtils.defaultBuf;
    private byte[] inputArray;
    private int inputPos, inputLim;
    private boolean finished;
    private boolean needDict;
    private long bytesRead;
    private long bytesWritten;

    /*
     * These fields are used as an "out" parameter from JNI when a
     * DataFormatException is thrown during the inflate operation.
     */
    private int inputConsumed;
    private int outputConsumed;

    static {
        ZipUtils.loadLibrary();
        initIDs();
    }

    /**
     * Creates a new decompressor. If the parameter 'nowrap' is true then
     * the ZLIB header and checksum fields will not be used. This provides
     * compatibility with the compression format used by both GZIP and PKZIP.
     * <p>
     * Note: When using the 'nowrap' option it is also necessary to provide
     * an extra "dummy" byte as input. This is required by the ZLIB native
     * library in order to support certain optimizations.
     *
     * @param nowrap if true then support GZIP compatible compression
     */
    public Inflater(boolean nowrap) {
        this.zsRef = new InflaterZStreamRef(this, init(nowrap));
    }

    /**
     * Creates a new decompressor.
     */
    public Inflater() {
        this(false);
    }

    /**
     * Sets input data for decompression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     *
     * @param input the input data bytes
     * @param off the start offset of the input data
     * @param len the length of the input data
     * @see Inflater#needsInput
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
     * Sets input data for decompression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     *
     * @param input the input data bytes
     * @see Inflater#needsInput
     */
    public void setInput(byte[] input) {
        setInput(input, 0, input.length);
    }

    /**
     * Sets input data for decompression.
     * <p>
     * One of the {@code setInput()} methods should be called whenever
     * {@code needsInput()} returns true indicating that more input data
     * is required.
     * <p>
     * The given buffer's position will be advanced as inflate
     * operations are performed, up to the buffer's limit.
     * The input buffer may be modified (refilled) between inflate
     * operations; doing so is equivalent to creating a new buffer
     * and setting it with this method.
     * <p>
     * Modifying the input buffer's contents, position, or limit
     * concurrently with an inflate operation will result in
     * undefined behavior, which may include incorrect operation
     * results or operation failure.
     *
     * @param input the input data bytes
     * @see Inflater#needsInput
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
     * Sets the preset dictionary to the given array of bytes. Should be
     * called when inflate() returns 0 and needsDictionary() returns true
     * indicating that a preset dictionary is required. The method getAdler()
     * can be used to get the Adler-32 value of the dictionary needed.
     * @param dictionary the dictionary data bytes
     * @param off the start offset of the data
     * @param len the length of the data
     * @see Inflater#needsDictionary
     * @see Inflater#getAdler
     */
    public void setDictionary(byte[] dictionary, int off, int len) {
        Preconditions.checkFromIndexSize(len, off, dictionary.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (zsRef) {
            ensureOpen();
            setDictionary(zsRef.address(), dictionary, off, len);
            needDict = false;
        }
    }

    /**
     * Sets the preset dictionary to the given array of bytes. Should be
     * called when inflate() returns 0 and needsDictionary() returns true
     * indicating that a preset dictionary is required. The method getAdler()
     * can be used to get the Adler-32 value of the dictionary needed.
     * @param dictionary the dictionary data bytes
     * @see Inflater#needsDictionary
     * @see Inflater#getAdler
     */
    public void setDictionary(byte[] dictionary) {
        setDictionary(dictionary, 0, dictionary.length);
    }

    /**
     * Sets the preset dictionary to the bytes in the given buffer. Should be
     * called when inflate() returns 0 and needsDictionary() returns true
     * indicating that a preset dictionary is required. The method getAdler()
     * can be used to get the Adler-32 value of the dictionary needed.
     * <p>
     * The bytes in given byte buffer will be fully consumed by this method.  On
     * return, its position will equal its limit.
     *
     * @param dictionary the dictionary data bytes
     * @see Inflater#needsDictionary
     * @see Inflater#getAdler
     * @since 11
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
            needDict = false;
        }
    }

    /**
     * Returns the total number of bytes remaining in the input buffer.
     * This can be used to find out what bytes still remain in the input
     * buffer after decompression has finished.
     * @return the total number of bytes remaining in the input buffer
     */
    public int getRemaining() {
        synchronized (zsRef) {
            ByteBuffer input = this.input;
            return input == null ? inputLim - inputPos : input.remaining();
        }
    }

    /**
     * Returns true if no data remains in the input buffer. This can
     * be used to determine if one of the {@code setInput()} methods should be
     * called in order to provide more input.
     *
     * @return true if no data remains in the input buffer
     */
    public boolean needsInput() {
        synchronized (zsRef) {
            ByteBuffer input = this.input;
            return input == null ? inputLim == inputPos : ! input.hasRemaining();
        }
    }

    /**
     * Returns true if a preset dictionary is needed for decompression.
     * @return true if a preset dictionary is needed for decompression
     * @see Inflater#setDictionary
     */
    public boolean needsDictionary() {
        synchronized (zsRef) {
            return needDict;
        }
    }

    /**
     * Returns true if the end of the compressed data stream has been
     * reached.
     * @return true if the end of the compressed data stream has been
     * reached
     */
    public boolean finished() {
        synchronized (zsRef) {
            return finished;
        }
    }

    /**
     * Uncompresses bytes into specified buffer. Returns actual number
     * of bytes uncompressed. A return value of 0 indicates that
     * needsInput() or needsDictionary() should be called in order to
     * determine if more input data or a preset dictionary is required.
     * In the latter case, getAdler() can be used to get the Adler-32
     * value of the dictionary required.
     * <p>
     * If the {@link #setInput(ByteBuffer)} method was called to provide a buffer
     * for input, the input buffer's position will be advanced by the number of bytes
     * consumed by this operation, even in the event that a {@link DataFormatException}
     * is thrown.
     * <p>
     * The {@linkplain #getRemaining() remaining byte count} will be reduced by
     * the number of consumed input bytes.  If the {@link #setInput(ByteBuffer)}
     * method was called to provide a buffer for input, the input buffer's position
     * will be advanced the number of consumed bytes.
     * <p>
     * These byte totals, as well as
     * the {@linkplain #getBytesRead() total bytes read}
     * and the {@linkplain #getBytesWritten() total bytes written}
     * values, will be updated even in the event that a {@link DataFormatException}
     * is thrown to reflect the amount of data consumed and produced before the
     * exception occurred.
     *
     * @param output the buffer for the uncompressed data
     * @param off the start offset of the data
     * @param len the maximum number of uncompressed bytes
     * @return the actual number of uncompressed bytes
     * @throws DataFormatException if the compressed data format is invalid
     * @see Inflater#needsInput
     * @see Inflater#needsDictionary
     */
    public int inflate(byte[] output, int off, int len)
        throws DataFormatException
    {
        Preconditions.checkFromIndexSize(len, off, output.length, Preconditions.AIOOBE_FORMATTER);
        synchronized (zsRef) {
            ensureOpen();
            ByteBuffer input = this.input;
            long result;
            int inputPos;
            try {
                if (input == null) {
                    inputPos = this.inputPos;
                    try {
                        result = inflateBytesBytes(zsRef.address(),
                            inputArray, inputPos, inputLim - inputPos,
                            output, off, len);
                    } catch (DataFormatException e) {
                        this.inputPos = inputPos + inputConsumed;
                        throw e;
                    }
                } else {
                    inputPos = input.position();
                    try {
                        int inputRem = Math.max(input.limit() - inputPos, 0);
                        if (input.isDirect()) {
                            try {
                                long inputAddress = ((DirectBuffer) input).address();
                                result = inflateBufferBytes(zsRef.address(),
                                    inputAddress + inputPos, inputRem,
                                    output, off, len);
                            } finally {
                                Reference.reachabilityFence(input);
                            }
                        } else {
                            byte[] inputArray = ZipUtils.getBufferArray(input);
                            int inputOffset = ZipUtils.getBufferOffset(input);
                            result = inflateBytesBytes(zsRef.address(),
                                inputArray, inputOffset + inputPos, inputRem,
                                output, off, len);
                        }
                    } catch (DataFormatException e) {
                        input.position(inputPos + inputConsumed);
                        throw e;
                    }
                }
            } catch (DataFormatException e) {
                bytesRead += inputConsumed;
                inputConsumed = 0;
                int written = outputConsumed;
                bytesWritten += written;
                outputConsumed = 0;
                throw e;
            }
            int read = (int) (result & 0x7fff_ffffL);
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            if ((result >>> 62 & 1) != 0) {
                finished = true;
            }
            if ((result >>> 63 & 1) != 0) {
                needDict = true;
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
     * Uncompresses bytes into specified buffer. Returns actual number
     * of bytes uncompressed. A return value of 0 indicates that
     * needsInput() or needsDictionary() should be called in order to
     * determine if more input data or a preset dictionary is required.
     * In the latter case, getAdler() can be used to get the Adler-32
     * value of the dictionary required.
     * <p>
     * The {@linkplain #getRemaining() remaining byte count} will be reduced by
     * the number of consumed input bytes.  If the {@link #setInput(ByteBuffer)}
     * method was called to provide a buffer for input, the input buffer's position
     * will be advanced the number of consumed bytes.
     * <p>
     * These byte totals, as well as
     * the {@linkplain #getBytesRead() total bytes read}
     * and the {@linkplain #getBytesWritten() total bytes written}
     * values, will be updated even in the event that a {@link DataFormatException}
     * is thrown to reflect the amount of data consumed and produced before the
     * exception occurred.
     *
     * @param output the buffer for the uncompressed data
     * @return the actual number of uncompressed bytes
     * @throws DataFormatException if the compressed data format is invalid
     * @see Inflater#needsInput
     * @see Inflater#needsDictionary
     */
    public int inflate(byte[] output) throws DataFormatException {
        return inflate(output, 0, output.length);
    }

    /**
     * Uncompresses bytes into specified buffer. Returns actual number
     * of bytes uncompressed. A return value of 0 indicates that
     * needsInput() or needsDictionary() should be called in order to
     * determine if more input data or a preset dictionary is required.
     * In the latter case, getAdler() can be used to get the Adler-32
     * value of the dictionary required.
     * <p>
     * On success, the position of the given {@code output} byte buffer will be
     * advanced by as many bytes as were produced by the operation, which is equal
     * to the number returned by this method.  Note that the position of the
     * {@code output} buffer will be advanced even in the event that a
     * {@link DataFormatException} is thrown.
     * <p>
     * The {@linkplain #getRemaining() remaining byte count} will be reduced by
     * the number of consumed input bytes.  If the {@link #setInput(ByteBuffer)}
     * method was called to provide a buffer for input, the input buffer's position
     * will be advanced the number of consumed bytes.
     * <p>
     * These byte totals, as well as
     * the {@linkplain #getBytesRead() total bytes read}
     * and the {@linkplain #getBytesWritten() total bytes written}
     * values, will be updated even in the event that a {@link DataFormatException}
     * is thrown to reflect the amount of data consumed and produced before the
     * exception occurred.
     *
     * @param output the buffer for the uncompressed data
     * @return the actual number of uncompressed bytes
     * @throws DataFormatException if the compressed data format is invalid
     * @throws ReadOnlyBufferException if the given output buffer is read-only
     * @see Inflater#needsInput
     * @see Inflater#needsDictionary
     * @since 11
     */
    public int inflate(ByteBuffer output) throws DataFormatException {
        if (output.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        synchronized (zsRef) {
            ensureOpen();
            ByteBuffer input = this.input;
            long result;
            int inputPos;
            int outputPos = output.position();
            int outputRem = Math.max(output.limit() - outputPos, 0);
            try {
                if (input == null) {
                    inputPos = this.inputPos;
                    try {
                        if (output.isDirect()) {
                            long outputAddress = ((DirectBuffer) output).address();
                            try {
                                result = inflateBytesBuffer(zsRef.address(),
                                    inputArray, inputPos, inputLim - inputPos,
                                    outputAddress + outputPos, outputRem);
                            } finally {
                                Reference.reachabilityFence(output);
                            }
                        } else {
                            byte[] outputArray = ZipUtils.getBufferArray(output);
                            int outputOffset = ZipUtils.getBufferOffset(output);
                            result = inflateBytesBytes(zsRef.address(),
                                inputArray, inputPos, inputLim - inputPos,
                                outputArray, outputOffset + outputPos, outputRem);
                        }
                    } catch (DataFormatException e) {
                        this.inputPos = inputPos + inputConsumed;
                        throw e;
                    }
                } else {
                    inputPos = input.position();
                    int inputRem = Math.max(input.limit() - inputPos, 0);
                    try {
                        if (input.isDirect()) {
                            long inputAddress = ((DirectBuffer) input).address();
                            try {
                                if (output.isDirect()) {
                                    long outputAddress = ((DirectBuffer) output).address();
                                    try {
                                        result = inflateBufferBuffer(zsRef.address(),
                                            inputAddress + inputPos, inputRem,
                                            outputAddress + outputPos, outputRem);
                                    } finally {
                                        Reference.reachabilityFence(output);
                                    }
                                } else {
                                    byte[] outputArray = ZipUtils.getBufferArray(output);
                                    int outputOffset = ZipUtils.getBufferOffset(output);
                                    result = inflateBufferBytes(zsRef.address(),
                                        inputAddress + inputPos, inputRem,
                                        outputArray, outputOffset + outputPos, outputRem);
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
                                    result = inflateBytesBuffer(zsRef.address(),
                                        inputArray, inputOffset + inputPos, inputRem,
                                        outputAddress + outputPos, outputRem);
                                } finally {
                                    Reference.reachabilityFence(output);
                                }
                            } else {
                                byte[] outputArray = ZipUtils.getBufferArray(output);
                                int outputOffset = ZipUtils.getBufferOffset(output);
                                result = inflateBytesBytes(zsRef.address(),
                                    inputArray, inputOffset + inputPos, inputRem,
                                    outputArray, outputOffset + outputPos, outputRem);
                            }
                        }
                    } catch (DataFormatException e) {
                        input.position(inputPos + inputConsumed);
                        throw e;
                    }
                }
            } catch (DataFormatException e) {
                bytesRead += inputConsumed;
                inputConsumed = 0;
                int written = outputConsumed;
                output.position(outputPos + written);
                bytesWritten += written;
                outputConsumed = 0;
                throw e;
            }
            int read = (int) (result & 0x7fff_ffffL);
            int written = (int) (result >>> 31 & 0x7fff_ffffL);
            if ((result >>> 62 & 1) != 0) {
                finished = true;
            }
            if ((result >>> 63 & 1) != 0) {
                needDict = true;
            }
            if (input != null) {
                input.position(inputPos + read);
            } else {
                this.inputPos = inputPos + read;
            }
            // Note: this method call also serves to keep the byteBuffer ref alive
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
     * Returns the total number of compressed bytes input so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesRead()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of compressed bytes input so far
     */
    public int getTotalIn() {
        return (int) getBytesRead();
    }

    /**
     * Returns the total number of compressed bytes input so far.
     *
     * @return the total (non-negative) number of compressed bytes input so far
     * @since 1.5
     */
    public long getBytesRead() {
        synchronized (zsRef) {
            ensureOpen();
            return bytesRead;
        }
    }

    /**
     * Returns the total number of uncompressed bytes output so far.
     *
     * <p>Since the number of bytes may be greater than
     * Integer.MAX_VALUE, the {@link #getBytesWritten()} method is now
     * the preferred means of obtaining this information.</p>
     *
     * @return the total number of uncompressed bytes output so far
     */
    public int getTotalOut() {
        return (int) getBytesWritten();
    }

    /**
     * Returns the total number of uncompressed bytes output so far.
     *
     * @return the total (non-negative) number of uncompressed bytes output so far
     * @since 1.5
     */
    public long getBytesWritten() {
        synchronized (zsRef) {
            ensureOpen();
            return bytesWritten;
        }
    }

    /**
     * Resets inflater so that a new set of input data can be processed.
     */
    public void reset() {
        synchronized (zsRef) {
            ensureOpen();
            reset(zsRef.address());
            input = ZipUtils.defaultBuf;
            inputArray = null;
            finished = false;
            needDict = false;
            bytesRead = bytesWritten = 0;
        }
    }

    /**
     * Closes the decompressor and discards any unprocessed input.
     *
     * This method should be called when the decompressor is no longer
     * being used. Once this method is called, the behavior of the
     * Inflater object is undefined.
     */
    public void end() {
        synchronized (zsRef) {
            zsRef.clean();
            input = ZipUtils.defaultBuf;
            inputArray = null;
        }
    }


    private void ensureOpen () {
        assert Thread.holdsLock(zsRef);
        if (zsRef.address() == 0)
            throw new NullPointerException("Inflater has been closed");
    }

    private static native void initIDs();
    private static native long init(boolean nowrap);
    private static native void setDictionary(long addr, byte[] b, int off,
                                             int len);
    private static native void setDictionaryBuffer(long addr, long bufAddress, int len);
    private native long inflateBytesBytes(long addr,
        byte[] inputArray, int inputOff, int inputLen,
        byte[] outputArray, int outputOff, int outputLen) throws DataFormatException;
    private native long inflateBytesBuffer(long addr,
        byte[] inputArray, int inputOff, int inputLen,
        long outputAddress, int outputLen) throws DataFormatException;
    private native long inflateBufferBytes(long addr,
        long inputAddress, int inputLen,
        byte[] outputArray, int outputOff, int outputLen) throws DataFormatException;
    private native long inflateBufferBuffer(long addr,
        long inputAddress, int inputLen,
        long outputAddress, int outputLen) throws DataFormatException;
    private static native int getAdler(long addr);
    private static native void reset(long addr);
    private static native void end(long addr);

    /**
     * A reference to the native zlib's z_stream structure. It also
     * serves as the "cleaner" to clean up the native resource when
     * the Inflater is ended, closed or cleaned.
     */
    static class InflaterZStreamRef implements Runnable {

        private long address;
        private final Cleanable cleanable;

        private InflaterZStreamRef(Inflater owner, long addr) {
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
