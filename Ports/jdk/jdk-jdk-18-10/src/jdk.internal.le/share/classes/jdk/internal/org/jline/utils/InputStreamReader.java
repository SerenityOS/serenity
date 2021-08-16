/*
 * Copyright (c) 2002-2016, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;


/**
 *
 * NOTE for JLine: the default InputStreamReader that comes from the JRE
 * usually read more bytes than needed from the input stream, which
 * is not usable in a character per character model used in the terminal.
 * We thus use the harmony code which only reads the minimal number of bytes.
 */

/**
 * A class for turning a byte stream into a character stream. Data read from the
 * source input stream is converted into characters by either a default or a
 * provided character converter. The default encoding is taken from the
 * "file.encoding" system property. {@code InputStreamReader} contains a buffer
 * of bytes read from the source stream and converts these into characters as
 * needed. The buffer size is 8K.
 *
 * @see OutputStreamWriter
 */
public class InputStreamReader extends Reader {
    private InputStream in;

    private static final int BUFFER_SIZE = 4;

    private boolean endOfInput = false;

    CharsetDecoder decoder;

    ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SIZE);

    char pending = (char) -1;

    /**
     * Constructs a new {@code InputStreamReader} on the {@link InputStream}
     * {@code in}. This constructor sets the character converter to the encoding
     * specified in the "file.encoding" property and falls back to ISO 8859_1
     * (ISO-Latin-1) if the property doesn't exist.
     *
     * @param in
     *            the input stream from which to read characters.
     */
    public InputStreamReader(InputStream in) {
        super(in);
        this.in = in;
        decoder = Charset.defaultCharset().newDecoder().onMalformedInput(
                CodingErrorAction.REPLACE).onUnmappableCharacter(
                CodingErrorAction.REPLACE);
        bytes.limit(0);
    }

    /**
     * Constructs a new InputStreamReader on the InputStream {@code in}. The
     * character converter that is used to decode bytes into characters is
     * identified by name by {@code enc}. If the encoding cannot be found, an
     * UnsupportedEncodingException error is thrown.
     *
     * @param in
     *            the InputStream from which to read characters.
     * @param enc
     *            identifies the character converter to use.
     * @throws NullPointerException
     *             if {@code enc} is {@code null}.
     * @throws UnsupportedEncodingException
     *             if the encoding specified by {@code enc} cannot be found.
     */
    public InputStreamReader(InputStream in, final String enc)
            throws UnsupportedEncodingException {
        super(in);
        if (enc == null) {
            throw new NullPointerException();
        }
        this.in = in;
        try {
            decoder = Charset.forName(enc).newDecoder().onMalformedInput(
                    CodingErrorAction.REPLACE).onUnmappableCharacter(
                    CodingErrorAction.REPLACE);
        } catch (IllegalArgumentException e) {
            throw (UnsupportedEncodingException)
                    new UnsupportedEncodingException(enc).initCause(e);
        }
        bytes.limit(0);
    }

    /**
     * Constructs a new InputStreamReader on the InputStream {@code in} and
     * CharsetDecoder {@code dec}.
     *
     * @param in
     *            the source InputStream from which to read characters.
     * @param dec
     *            the CharsetDecoder used by the character conversion.
     */
    public InputStreamReader(InputStream in, CharsetDecoder dec) {
        super(in);
        dec.averageCharsPerByte();
        this.in = in;
        decoder = dec;
        bytes.limit(0);
    }

    /**
     * Constructs a new InputStreamReader on the InputStream {@code in} and
     * Charset {@code charset}.
     *
     * @param in
     *            the source InputStream from which to read characters.
     * @param charset
     *            the Charset that defines the character converter
     */
    public InputStreamReader(InputStream in, Charset charset) {
        super(in);
        this.in = in;
        decoder = charset.newDecoder().onMalformedInput(
                CodingErrorAction.REPLACE).onUnmappableCharacter(
                CodingErrorAction.REPLACE);
        bytes.limit(0);
    }

    /**
     * Closes this reader. This implementation closes the source InputStream and
     * releases all local storage.
     *
     * @throws IOException
     *             if an error occurs attempting to close this reader.
     */
    @Override
    public void close() throws IOException {
        synchronized (lock) {
            decoder = null;
            if (in != null) {
                in.close();
                in = null;
            }
        }
    }

    /**
     * Returns the name of the encoding used to convert bytes into characters.
     * The value {@code null} is returned if this reader has been closed.
     *
     * @return the name of the character converter or {@code null} if this
     *         reader is closed.
     */
    public String getEncoding() {
        if (!isOpen()) {
            return null;
        }
        return decoder.charset().name();
    }

    /**
     * Reads a single character from this reader and returns it as an integer
     * with the two higher-order bytes set to 0. Returns -1 if the end of the
     * reader has been reached. The byte value is either obtained from
     * converting bytes in this reader's buffer or by first filling the buffer
     * from the source InputStream and then reading from the buffer.
     *
     * @return the character read or -1 if the end of the reader has been
     *         reached.
     * @throws IOException
     *             if this reader is closed or some other I/O error occurs.
     */
    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (!isOpen()) {
                throw new ClosedException("InputStreamReader is closed.");
            }

            if (pending != (char) -1) {
                char c = pending;
                pending = (char) -1;
                return c;
            }
            char buf[] = new char[2];
            int nb = read(buf, 0, 2);
            if (nb == 2) {
                pending = buf[1];
            }
            if (nb > 0) {
                return buf[0];
            } else {
                return -1;
            }
        }
    }

    /**
     * Reads at most {@code length} characters from this reader and stores them
     * at position {@code offset} in the character array {@code buf}. Returns
     * the number of characters actually read or -1 if the end of the reader has
     * been reached. The bytes are either obtained from converting bytes in this
     * reader's buffer or by first filling the buffer from the source
     * InputStream and then reading from the buffer.
     *
     * @param buf
     *            the array to store the characters read.
     * @param offset
     *            the initial position in {@code buf} to store the characters
     *            read from this reader.
     * @param length
     *            the maximum number of characters to read.
     * @return the number of characters read or -1 if the end of the reader has
     *         been reached.
     * @throws IndexOutOfBoundsException
     *             if {@code offset < 0} or {@code length < 0}, or if
     *             {@code offset + length} is greater than the length of
     *             {@code buf}.
     * @throws IOException
     *             if this reader is closed or some other I/O error occurs.
     */
    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        synchronized (lock) {
            if (!isOpen()) {
                throw new IOException("InputStreamReader is closed.");
            }
            if (offset < 0 || offset > buf.length - length || length < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (length == 0) {
                return 0;
            }

            CharBuffer out = CharBuffer.wrap(buf, offset, length);
            CoderResult result = CoderResult.UNDERFLOW;

            // bytes.remaining() indicates number of bytes in buffer
            // when 1-st time entered, it'll be equal to zero
            boolean needInput = !bytes.hasRemaining();

            while (out.position() == offset) {
                // fill the buffer if needed
                if (needInput) {
                    try {
                        if ((in.available() == 0)
                            && (out.position() > offset)) {
                            // we could return the result without blocking read
                            break;
                        }
                    } catch (IOException e) {
                        // available didn't work so just try the read
                    }

                    int off = bytes.arrayOffset() + bytes.limit();
                    int was_red = in.read(bytes.array(), off, 1);

                    if (was_red == -1) {
                        endOfInput = true;
                        break;
                    } else if (was_red == 0) {
                        break;
                    }
                    bytes.limit(bytes.limit() + was_red);
                }

                // decode bytes
                result = decoder.decode(bytes, out, false);

                if (result.isUnderflow()) {
                    // compact the buffer if no space left
                    if (bytes.limit() == bytes.capacity()) {
                        bytes.compact();
                        bytes.limit(bytes.position());
                        bytes.position(0);
                    }
                    needInput = true;
                } else {
                    break;
                }
            }

            if (result == CoderResult.UNDERFLOW && endOfInput) {
                result = decoder.decode(bytes, out, true);
                decoder.flush(out);
                decoder.reset();
            }
            if (result.isMalformed()) {
                throw new MalformedInputException(result.length());
            } else if (result.isUnmappable()) {
                throw new UnmappableCharacterException(result.length());
            }

            return out.position() - offset == 0 ? -1 : out.position() - offset;
        }
    }

    /*
     * Answer a boolean indicating whether or not this InputStreamReader is
     * open.
     */
    private boolean isOpen() {
        return in != null;
    }

    /**
     * Indicates whether this reader is ready to be read without blocking. If
     * the result is {@code true}, the next {@code read()} will not block. If
     * the result is {@code false} then this reader may or may not block when
     * {@code read()} is called. This implementation returns {@code true} if
     * there are bytes available in the buffer or the source stream has bytes
     * available.
     *
     * @return {@code true} if the receiver will not block when {@code read()}
     *         is called, {@code false} if unknown or blocking will occur.
     * @throws IOException
     *             if this reader is closed or some other I/O error occurs.
     */
    @Override
    public boolean ready() throws IOException {
        synchronized (lock) {
            if (in == null) {
                throw new IOException("InputStreamReader is closed.");
            }
            try {
                return bytes.hasRemaining() || in.available() > 0;
            } catch (IOException e) {
                return false;
            }
        }
    }
}
