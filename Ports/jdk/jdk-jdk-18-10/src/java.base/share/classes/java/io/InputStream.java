/*
 * Copyright (c) 1994, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This abstract class is the superclass of all classes representing
 * an input stream of bytes.
 *
 * <p> Applications that need to define a subclass of {@code InputStream}
 * must always provide a method that returns the next byte of input.
 *
 * @author  Arthur van Hoff
 * @see     java.io.BufferedInputStream
 * @see     java.io.ByteArrayInputStream
 * @see     java.io.DataInputStream
 * @see     java.io.FilterInputStream
 * @see     java.io.InputStream#read()
 * @see     java.io.OutputStream
 * @see     java.io.PushbackInputStream
 * @since   1.0
 */
public abstract class InputStream implements Closeable {

    // MAX_SKIP_BUFFER_SIZE is used to determine the maximum buffer size to
    // use when skipping.
    private static final int MAX_SKIP_BUFFER_SIZE = 2048;

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * Constructor for subclasses to call.
     */
    public InputStream() {}

    /**
     * Returns a new {@code InputStream} that reads no bytes. The returned
     * stream is initially open.  The stream is closed by calling the
     * {@code close()} method.  Subsequent calls to {@code close()} have no
     * effect.
     *
     * <p> While the stream is open, the {@code available()}, {@code read()},
     * {@code read(byte[])}, {@code read(byte[], int, int)},
     * {@code readAllBytes()}, {@code readNBytes(byte[], int, int)},
     * {@code readNBytes(int)}, {@code skip(long)}, {@code skipNBytes(long)},
     * and {@code transferTo()} methods all behave as if end of stream has been
     * reached.  After the stream has been closed, these methods all throw
     * {@code IOException}.
     *
     * <p> The {@code markSupported()} method returns {@code false}.  The
     * {@code mark()} method does nothing, and the {@code reset()} method
     * throws {@code IOException}.
     *
     * @return an {@code InputStream} which contains no bytes
     *
     * @since 11
     */
    public static InputStream nullInputStream() {
        return new InputStream() {
            private volatile boolean closed;

            private void ensureOpen() throws IOException {
                if (closed) {
                    throw new IOException("Stream closed");
                }
            }

            @Override
            public int available () throws IOException {
                ensureOpen();
                return 0;
            }

            @Override
            public int read() throws IOException {
                ensureOpen();
                return -1;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                if (len == 0) {
                    return 0;
                }
                ensureOpen();
                return -1;
            }

            @Override
            public byte[] readAllBytes() throws IOException {
                ensureOpen();
                return new byte[0];
            }

            @Override
            public int readNBytes(byte[] b, int off, int len)
                throws IOException {
                Objects.checkFromIndexSize(off, len, b.length);
                ensureOpen();
                return 0;
            }

            @Override
            public byte[] readNBytes(int len) throws IOException {
                if (len < 0) {
                    throw new IllegalArgumentException("len < 0");
                }
                ensureOpen();
                return new byte[0];
            }

            @Override
            public long skip(long n) throws IOException {
                ensureOpen();
                return 0L;
            }

            @Override
            public void skipNBytes(long n) throws IOException {
                ensureOpen();
                if (n > 0) {
                    throw new EOFException();
                }
            }

            @Override
            public long transferTo(OutputStream out) throws IOException {
                Objects.requireNonNull(out);
                ensureOpen();
                return 0L;
            }

            @Override
            public void close() throws IOException {
                closed = true;
            }
        };
    }

    /**
     * Reads the next byte of data from the input stream. The value byte is
     * returned as an {@code int} in the range {@code 0} to
     * {@code 255}. If no byte is available because the end of the stream
     * has been reached, the value {@code -1} is returned. This method
     * blocks until input data is available, the end of the stream is detected,
     * or an exception is thrown.
     *
     * <p> A subclass must provide an implementation of this method.
     *
     * @return     the next byte of data, or {@code -1} if the end of the
     *             stream is reached.
     * @throws     IOException  if an I/O error occurs.
     */
    public abstract int read() throws IOException;

    /**
     * Reads some number of bytes from the input stream and stores them into
     * the buffer array {@code b}. The number of bytes actually read is
     * returned as an integer.  This method blocks until input data is
     * available, end of file is detected, or an exception is thrown.
     *
     * <p> If the length of {@code b} is zero, then no bytes are read and
     * {@code 0} is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at the
     * end of the file, the value {@code -1} is returned; otherwise, at
     * least one byte is read and stored into {@code b}.
     *
     * <p> The first byte read is stored into element {@code b[0]}, the
     * next one into {@code b[1]}, and so on. The number of bytes read is,
     * at most, equal to the length of {@code b}. Let <i>k</i> be the
     * number of bytes actually read; these bytes will be stored in elements
     * {@code b[0]} through {@code b[}<i>k</i>{@code -1]},
     * leaving elements {@code b[}<i>k</i>{@code ]} through
     * {@code b[b.length-1]} unaffected.
     *
     * <p> The {@code read(b)} method for class {@code InputStream}
     * has the same effect as: <pre>{@code  read(b, 0, b.length) }</pre>
     *
     * @param      b   the buffer into which the data is read.
     * @return     the total number of bytes read into the buffer, or
     *             {@code -1} if there is no more data because the end of
     *             the stream has been reached.
     * @throws     IOException  If the first byte cannot be read for any reason
     *             other than the end of the file, if the input stream has been
     *             closed, or if some other I/O error occurs.
     * @throws     NullPointerException  if {@code b} is {@code null}.
     * @see        java.io.InputStream#read(byte[], int, int)
     */
    public int read(byte b[]) throws IOException {
        return read(b, 0, b.length);
    }

    /**
     * Reads up to {@code len} bytes of data from the input stream into
     * an array of bytes.  An attempt is made to read as many as
     * {@code len} bytes, but a smaller number may be read.
     * The number of bytes actually read is returned as an integer.
     *
     * <p> This method blocks until input data is available, end of file is
     * detected, or an exception is thrown.
     *
     * <p> If {@code len} is zero, then no bytes are read and
     * {@code 0} is returned; otherwise, there is an attempt to read at
     * least one byte. If no byte is available because the stream is at end of
     * file, the value {@code -1} is returned; otherwise, at least one
     * byte is read and stored into {@code b}.
     *
     * <p> The first byte read is stored into element {@code b[off]}, the
     * next one into {@code b[off+1]}, and so on. The number of bytes read
     * is, at most, equal to {@code len}. Let <i>k</i> be the number of
     * bytes actually read; these bytes will be stored in elements
     * {@code b[off]} through {@code b[off+}<i>k</i>{@code -1]},
     * leaving elements {@code b[off+}<i>k</i>{@code ]} through
     * {@code b[off+len-1]} unaffected.
     *
     * <p> In every case, elements {@code b[0]} through
     * {@code b[off-1]} and elements {@code b[off+len]} through
     * {@code b[b.length-1]} are unaffected.
     *
     * <p> The {@code read(b, off, len)} method
     * for class {@code InputStream} simply calls the method
     * {@code read()} repeatedly. If the first such call results in an
     * {@code IOException}, that exception is returned from the call to
     * the {@code read(b,} {@code off,} {@code len)} method.  If
     * any subsequent call to {@code read()} results in a
     * {@code IOException}, the exception is caught and treated as if it
     * were end of file; the bytes read up to that point are stored into
     * {@code b} and the number of bytes read before the exception
     * occurred is returned. The default implementation of this method blocks
     * until the requested amount of input data {@code len} has been read,
     * end of file is detected, or an exception is thrown. Subclasses are
     * encouraged to provide a more efficient implementation of this method.
     *
     * @param      b     the buffer into which the data is read.
     * @param      off   the start offset in array {@code b}
     *                   at which the data is written.
     * @param      len   the maximum number of bytes to read.
     * @return     the total number of bytes read into the buffer, or
     *             {@code -1} if there is no more data because the end of
     *             the stream has been reached.
     * @throws     IOException If the first byte cannot be read for any reason
     *             other than end of file, or if the input stream has been closed,
     *             or if some other I/O error occurs.
     * @throws     NullPointerException If {@code b} is {@code null}.
     * @throws     IndexOutOfBoundsException If {@code off} is negative,
     *             {@code len} is negative, or {@code len} is greater than
     *             {@code b.length - off}
     * @see        java.io.InputStream#read()
     */
    public int read(byte b[], int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                b[off + i] = (byte)c;
            }
        } catch (IOException ee) {
        }
        return i;
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Reads all remaining bytes from the input stream. This method blocks until
     * all remaining bytes have been read and end of stream is detected, or an
     * exception is thrown. This method does not close the input stream.
     *
     * <p> When this stream reaches end of stream, further invocations of this
     * method will return an empty byte array.
     *
     * <p> Note that this method is intended for simple cases where it is
     * convenient to read all bytes into a byte array. It is not intended for
     * reading input streams with large amounts of data.
     *
     * <p> The behavior for the case where the input stream is <i>asynchronously
     * closed</i>, or the thread interrupted during the read, is highly input
     * stream specific, and therefore not specified.
     *
     * <p> If an I/O error occurs reading from the input stream, then it may do
     * so after some, but not all, bytes have been read. Consequently the input
     * stream may not be at end of stream and may be in an inconsistent state.
     * It is strongly recommended that the stream be promptly closed if an I/O
     * error occurs.
     *
     * @implSpec
     * This method invokes {@link #readNBytes(int)} with a length of
     * {@link Integer#MAX_VALUE}.
     *
     * @return a byte array containing the bytes read from this input stream
     * @throws IOException if an I/O error occurs
     * @throws OutOfMemoryError if an array of the required size cannot be
     *         allocated.
     *
     * @since 9
     */
    public byte[] readAllBytes() throws IOException {
        return readNBytes(Integer.MAX_VALUE);
    }

    /**
     * Reads up to a specified number of bytes from the input stream. This
     * method blocks until the requested number of bytes has been read, end
     * of stream is detected, or an exception is thrown. This method does not
     * close the input stream.
     *
     * <p> The length of the returned array equals the number of bytes read
     * from the stream. If {@code len} is zero, then no bytes are read and
     * an empty byte array is returned. Otherwise, up to {@code len} bytes
     * are read from the stream. Fewer than {@code len} bytes may be read if
     * end of stream is encountered.
     *
     * <p> When this stream reaches end of stream, further invocations of this
     * method will return an empty byte array.
     *
     * <p> Note that this method is intended for simple cases where it is
     * convenient to read the specified number of bytes into a byte array. The
     * total amount of memory allocated by this method is proportional to the
     * number of bytes read from the stream which is bounded by {@code len}.
     * Therefore, the method may be safely called with very large values of
     * {@code len} provided sufficient memory is available.
     *
     * <p> The behavior for the case where the input stream is <i>asynchronously
     * closed</i>, or the thread interrupted during the read, is highly input
     * stream specific, and therefore not specified.
     *
     * <p> If an I/O error occurs reading from the input stream, then it may do
     * so after some, but not all, bytes have been read. Consequently the input
     * stream may not be at end of stream and may be in an inconsistent state.
     * It is strongly recommended that the stream be promptly closed if an I/O
     * error occurs.
     *
     * @implNote
     * The number of bytes allocated to read data from this stream and return
     * the result is bounded by {@code 2*(long)len}, inclusive.
     *
     * @param len the maximum number of bytes to read
     * @return a byte array containing the bytes read from this input stream
     * @throws IllegalArgumentException if {@code length} is negative
     * @throws IOException if an I/O error occurs
     * @throws OutOfMemoryError if an array of the required size cannot be
     *         allocated.
     *
     * @since 11
     */
    public byte[] readNBytes(int len) throws IOException {
        if (len < 0) {
            throw new IllegalArgumentException("len < 0");
        }

        List<byte[]> bufs = null;
        byte[] result = null;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;

            // read to EOF which may read more or less than buffer size
            while ((n = read(buf, nread,
                    Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }

            if (nread > 0) {
                if (MAX_BUFFER_SIZE - total < nread) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                if (nread < buf.length) {
                    buf = Arrays.copyOfRange(buf, 0, nread);
                }
                total += nread;
                if (result == null) {
                    result = buf;
                } else {
                    if (bufs == null) {
                        bufs = new ArrayList<>();
                        bufs.add(result);
                    }
                    bufs.add(buf);
                }
            }
            // if the last call to read returned -1 or the number of bytes
            // requested have been read then break
        } while (n >= 0 && remaining > 0);

        if (bufs == null) {
            if (result == null) {
                return new byte[0];
            }
            return result.length == total ?
                result : Arrays.copyOf(result, total);
        }

        result = new byte[total];
        int offset = 0;
        remaining = total;
        for (byte[] b : bufs) {
            int count = Math.min(b.length, remaining);
            System.arraycopy(b, 0, result, offset, count);
            offset += count;
            remaining -= count;
        }

        return result;
    }

    /**
     * Reads the requested number of bytes from the input stream into the given
     * byte array. This method blocks until {@code len} bytes of input data have
     * been read, end of stream is detected, or an exception is thrown. The
     * number of bytes actually read, possibly zero, is returned. This method
     * does not close the input stream.
     *
     * <p> In the case where end of stream is reached before {@code len} bytes
     * have been read, then the actual number of bytes read will be returned.
     * When this stream reaches end of stream, further invocations of this
     * method will return zero.
     *
     * <p> If {@code len} is zero, then no bytes are read and {@code 0} is
     * returned; otherwise, there is an attempt to read up to {@code len} bytes.
     *
     * <p> The first byte read is stored into element {@code b[off]}, the next
     * one in to {@code b[off+1]}, and so on. The number of bytes read is, at
     * most, equal to {@code len}. Let <i>k</i> be the number of bytes actually
     * read; these bytes will be stored in elements {@code b[off]} through
     * {@code b[off+}<i>k</i>{@code -1]}, leaving elements {@code b[off+}<i>k</i>
     * {@code ]} through {@code b[off+len-1]} unaffected.
     *
     * <p> The behavior for the case where the input stream is <i>asynchronously
     * closed</i>, or the thread interrupted during the read, is highly input
     * stream specific, and therefore not specified.
     *
     * <p> If an I/O error occurs reading from the input stream, then it may do
     * so after some, but not all, bytes of {@code b} have been updated with
     * data from the input stream. Consequently the input stream and {@code b}
     * may be in an inconsistent state. It is strongly recommended that the
     * stream be promptly closed if an I/O error occurs.
     *
     * @param  b the byte array into which the data is read
     * @param  off the start offset in {@code b} at which the data is written
     * @param  len the maximum number of bytes to read
     * @return the actual number of bytes read into the buffer
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if {@code b} is {@code null}
     * @throws IndexOutOfBoundsException If {@code off} is negative, {@code len}
     *         is negative, or {@code len} is greater than {@code b.length - off}
     *
     * @since 9
     */
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);

        int n = 0;
        while (n < len) {
            int count = read(b, off + n, len - n);
            if (count < 0)
                break;
            n += count;
        }
        return n;
    }

    /**
     * Skips over and discards {@code n} bytes of data from this input
     * stream. The {@code skip} method may, for a variety of reasons, end
     * up skipping over some smaller number of bytes, possibly {@code 0}.
     * This may result from any of a number of conditions; reaching end of file
     * before {@code n} bytes have been skipped is only one possibility.
     * The actual number of bytes skipped is returned. If {@code n} is
     * negative, the {@code skip} method for class {@code InputStream} always
     * returns 0, and no bytes are skipped. Subclasses may handle the negative
     * value differently.
     *
     * <p> The {@code skip} method implementation of this class creates a
     * byte array and then repeatedly reads into it until {@code n} bytes
     * have been read or the end of the stream has been reached. Subclasses are
     * encouraged to provide a more efficient implementation of this method.
     * For instance, the implementation may depend on the ability to seek.
     *
     * @param      n   the number of bytes to be skipped.
     * @return     the actual number of bytes skipped which might be zero.
     * @throws     IOException  if an I/O error occurs.
     * @see        java.io.InputStream#skipNBytes(long)
     */
    public long skip(long n) throws IOException {
        long remaining = n;
        int nr;

        if (n <= 0) {
            return 0;
        }

        int size = (int)Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
        byte[] skipBuffer = new byte[size];
        while (remaining > 0) {
            nr = read(skipBuffer, 0, (int)Math.min(size, remaining));
            if (nr < 0) {
                break;
            }
            remaining -= nr;
        }

        return n - remaining;
    }

    /**
     * Skips over and discards exactly {@code n} bytes of data from this input
     * stream.  If {@code n} is zero, then no bytes are skipped.
     * If {@code n} is negative, then no bytes are skipped.
     * Subclasses may handle the negative value differently.
     *
     * <p> This method blocks until the requested number of bytes has been
     * skipped, end of file is reached, or an exception is thrown.
     *
     * <p> If end of stream is reached before the stream is at the desired
     * position, then an {@code EOFException} is thrown.
     *
     * <p> If an I/O error occurs, then the input stream may be
     * in an inconsistent state. It is strongly recommended that the
     * stream be promptly closed if an I/O error occurs.
     *
     * @implNote
     * Subclasses are encouraged to provide a more efficient implementation
     * of this method.
     *
     * @implSpec
     * If {@code n} is zero or negative, then no bytes are skipped.
     * If {@code n} is positive, the default implementation of this method
     * invokes {@link #skip(long) skip()} repeatedly with its parameter equal
     * to the remaining number of bytes to skip until the requested number
     * of bytes has been skipped or an error condition occurs.  If at any
     * point the return value of {@code skip()} is negative or greater than the
     * remaining number of bytes to be skipped, then an {@code IOException} is
     * thrown.  If {@code skip()} ever returns zero, then {@link #read()} is
     * invoked to read a single byte, and if it returns {@code -1}, then an
     * {@code EOFException} is thrown.  Any exception thrown by {@code skip()}
     * or {@code read()} will be propagated.
     *
     * @param      n   the number of bytes to be skipped.
     * @throws     EOFException if end of stream is encountered before the
     *             stream can be positioned {@code n} bytes beyond its position
     *             when this method was invoked.
     * @throws     IOException  if the stream cannot be positioned properly or
     *             if an I/O error occurs.
     * @see        java.io.InputStream#skip(long)
     *
     * @since 12
     */
    public void skipNBytes(long n) throws IOException {
        while (n > 0) {
            long ns = skip(n);
            if (ns > 0 && ns <= n) {
                // adjust number to skip
                n -= ns;
            } else if (ns == 0) { // no bytes skipped
                // read one byte to check for EOS
                if (read() == -1) {
                    throw new EOFException();
                }
                // one byte read so decrement number to skip
                n--;
            } else { // skipped negative or too many bytes
                throw new IOException("Unable to skip exactly");
            }
        }
    }

    /**
     * Returns an estimate of the number of bytes that can be read (or skipped
     * over) from this input stream without blocking, which may be 0, or 0 when
     * end of stream is detected.  The read might be on the same thread or
     * another thread.  A single read or skip of this many bytes will not block,
     * but may read or skip fewer bytes.
     *
     * <p> Note that while some implementations of {@code InputStream} will
     * return the total number of bytes in the stream, many will not.  It is
     * never correct to use the return value of this method to allocate
     * a buffer intended to hold all data in this stream.
     *
     * <p> A subclass's implementation of this method may choose to throw an
     * {@link IOException} if this input stream has been closed by invoking the
     * {@link #close()} method.
     *
     * <p> The {@code available} method of {@code InputStream} always returns
     * {@code 0}.
     *
     * <p> This method should be overridden by subclasses.
     *
     * @return     an estimate of the number of bytes that can be read (or
     *             skipped over) from this input stream without blocking or
     *             {@code 0} when it reaches the end of the input stream.
     * @throws     IOException if an I/O error occurs.
     */
    public int available() throws IOException {
        return 0;
    }

    /**
     * Closes this input stream and releases any system resources associated
     * with the stream.
     *
     * <p> The {@code close} method of {@code InputStream} does
     * nothing.
     *
     * @throws     IOException  if an I/O error occurs.
     */
    public void close() throws IOException {}

    /**
     * Marks the current position in this input stream. A subsequent call to
     * the {@code reset} method repositions this stream at the last marked
     * position so that subsequent reads re-read the same bytes.
     *
     * <p> The {@code readlimit} arguments tells this input stream to
     * allow that many bytes to be read before the mark position gets
     * invalidated.
     *
     * <p> The general contract of {@code mark} is that, if the method
     * {@code markSupported} returns {@code true}, the stream somehow
     * remembers all the bytes read after the call to {@code mark} and
     * stands ready to supply those same bytes again if and whenever the method
     * {@code reset} is called.  However, the stream is not required to
     * remember any data at all if more than {@code readlimit} bytes are
     * read from the stream before {@code reset} is called.
     *
     * <p> Marking a closed stream should not have any effect on the stream.
     *
     * <p> The {@code mark} method of {@code InputStream} does
     * nothing.
     *
     * @param   readlimit   the maximum limit of bytes that can be read before
     *                      the mark position becomes invalid.
     * @see     java.io.InputStream#reset()
     */
    public synchronized void mark(int readlimit) {}

    /**
     * Repositions this stream to the position at the time the
     * {@code mark} method was last called on this input stream.
     *
     * <p> The general contract of {@code reset} is:
     *
     * <ul>
     * <li> If the method {@code markSupported} returns
     * {@code true}, then:
     *
     *     <ul><li> If the method {@code mark} has not been called since
     *     the stream was created, or the number of bytes read from the stream
     *     since {@code mark} was last called is larger than the argument
     *     to {@code mark} at that last call, then an
     *     {@code IOException} might be thrown.
     *
     *     <li> If such an {@code IOException} is not thrown, then the
     *     stream is reset to a state such that all the bytes read since the
     *     most recent call to {@code mark} (or since the start of the
     *     file, if {@code mark} has not been called) will be resupplied
     *     to subsequent callers of the {@code read} method, followed by
     *     any bytes that otherwise would have been the next input data as of
     *     the time of the call to {@code reset}. </ul>
     *
     * <li> If the method {@code markSupported} returns
     * {@code false}, then:
     *
     *     <ul><li> The call to {@code reset} may throw an
     *     {@code IOException}.
     *
     *     <li> If an {@code IOException} is not thrown, then the stream
     *     is reset to a fixed state that depends on the particular type of the
     *     input stream and how it was created. The bytes that will be supplied
     *     to subsequent callers of the {@code read} method depend on the
     *     particular type of the input stream. </ul></ul>
     *
     * <p>The method {@code reset} for class {@code InputStream}
     * does nothing except throw an {@code IOException}.
     *
     * @throws  IOException  if this stream has not been marked or if the
     *          mark has been invalidated.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.IOException
     */
    public synchronized void reset() throws IOException {
        throw new IOException("mark/reset not supported");
    }

    /**
     * Tests if this input stream supports the {@code mark} and
     * {@code reset} methods. Whether or not {@code mark} and
     * {@code reset} are supported is an invariant property of a
     * particular input stream instance. The {@code markSupported} method
     * of {@code InputStream} returns {@code false}.
     *
     * @return  {@code true} if this stream instance supports the mark
     *          and reset methods; {@code false} otherwise.
     * @see     java.io.InputStream#mark(int)
     * @see     java.io.InputStream#reset()
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * Reads all bytes from this input stream and writes the bytes to the
     * given output stream in the order that they are read. On return, this
     * input stream will be at end of stream. This method does not close either
     * stream.
     * <p>
     * This method may block indefinitely reading from the input stream, or
     * writing to the output stream. The behavior for the case where the input
     * and/or output stream is <i>asynchronously closed</i>, or the thread
     * interrupted during the transfer, is highly input and output stream
     * specific, and therefore not specified.
     * <p>
     * If an I/O error occurs reading from the input stream or writing to the
     * output stream, then it may do so after some bytes have been read or
     * written. Consequently the input stream may not be at end of stream and
     * one, or both, streams may be in an inconsistent state. It is strongly
     * recommended that both streams be promptly closed if an I/O error occurs.
     *
     * @param  out the output stream, non-null
     * @return the number of bytes transferred
     * @throws IOException if an I/O error occurs when reading or writing
     * @throws NullPointerException if {@code out} is {@code null}
     *
     * @since 9
     */
    public long transferTo(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int read;
        while ((read = this.read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, read);
            transferred += read;
        }
        return transferred;
    }
}
