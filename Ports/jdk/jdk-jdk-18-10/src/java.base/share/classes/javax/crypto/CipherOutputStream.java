/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto;

import java.io.*;

/**
 * A CipherOutputStream is composed of an OutputStream and a Cipher so
 * that write() methods first process the data before writing them out
 * to the underlying OutputStream.  The cipher must be fully
 * initialized before being used by a CipherOutputStream.
 *
 * <p> For example, if the cipher is initialized for encryption, the
 * CipherOutputStream will attempt to encrypt data before writing out the
 * encrypted data.
 *
 * <p> This class adheres strictly to the semantics, especially the
 * failure semantics, of its ancestor classes
 * java.io.OutputStream and java.io.FilterOutputStream.  This class
 * has exactly those methods specified in its ancestor classes, and
 * overrides them all.  Moreover, this class catches all exceptions
 * that are not thrown by its ancestor classes. In particular, this
 * class catches BadPaddingException and other exceptions thrown by
 * failed integrity checks during decryption. These exceptions are not
 * re-thrown, so the client will not be informed that integrity checks
 * failed. Because of this behavior, this class may not be suitable
 * for use with decryption in an authenticated mode of operation (e.g. GCM)
 * if the application requires explicit notification when authentication
 * fails. Such an application can use the Cipher API directly as an
 * alternative to using this class.
 *
 * <p> It is crucial for a programmer using this class not to use
 * methods that are not defined or overridden in this class (such as a
 * new method or constructor that is later added to one of the super
 * classes), because the design and implementation of those methods
 * are unlikely to have considered security impact with regard to
 * CipherOutputStream.
 *
 * @author  Li Gong
 * @see     java.io.OutputStream
 * @see     java.io.FilterOutputStream
 * @see     javax.crypto.Cipher
 * @see     javax.crypto.CipherInputStream
 *
 * @since 1.4
 */

public class CipherOutputStream extends FilterOutputStream {

    // the cipher engine to use to process stream data
    private Cipher cipher;

    // the underlying output stream
    private OutputStream output;

    /* the buffer holding one byte of incoming data */
    private byte[] ibuffer = new byte[1];

    // the buffer holding data ready to be written out
    private byte[] obuffer = null;

    // stream status
    private boolean closed = false;

    /**
     * Ensure obuffer is big enough for the next update or doFinal
     * operation, given the input length <code>inLen</code> (in bytes)
     *
     * @param inLen the input length (in bytes)
     */
    private void ensureCapacity(int inLen) {
        int minLen = cipher.getOutputSize(inLen);
        if (obuffer == null || obuffer.length < minLen) {
            obuffer = new byte[minLen];
        }
    }

    /**
     *
     * Constructs a CipherOutputStream from an OutputStream and a
     * Cipher.
     * <br>Note: if the specified output stream or cipher is
     * null, a NullPointerException may be thrown later when
     * they are used.
     *
     * @param os  the OutputStream object
     * @param c   an initialized Cipher object
     */
    public CipherOutputStream(OutputStream os, Cipher c) {
        super(os);
        output = os;
        cipher = c;
    };

    /**
     * Constructs a CipherOutputStream from an OutputStream without
     * specifying a Cipher. This has the effect of constructing a
     * CipherOutputStream using a NullCipher.
     * <br>Note: if the specified output stream is null, a
     * NullPointerException may be thrown later when it is used.
     *
     * @param os  the OutputStream object
     */
    protected CipherOutputStream(OutputStream os) {
        super(os);
        output = os;
        cipher = new NullCipher();
    }

    /**
     * Writes the specified byte to this output stream.
     *
     * @param      b   the <code>byte</code>.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void write(int b) throws IOException {
        ibuffer[0] = (byte) b;
        ensureCapacity(1);
        try {
            int ostored = cipher.update(ibuffer, 0, 1, obuffer);
            if (ostored > 0) {
                output.write(obuffer, 0, ostored);
            }
        } catch (ShortBufferException sbe) {
            // should never happen; re-throw just in case
            throw new IOException(sbe);
        }
    };

    /**
     * Writes <code>b.length</code> bytes from the specified byte array
     * to this output stream.
     * <p>
     * The <code>write</code> method of
     * <code>CipherOutputStream</code> calls the <code>write</code>
     * method of three arguments with the three arguments
     * <code>b</code>, <code>0</code>, and <code>b.length</code>.
     *
     * @param      b   the data.
     * @exception  NullPointerException if <code>b</code> is null.
     * @exception  IOException  if an I/O error occurs.
     * @see        javax.crypto.CipherOutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte b[]) throws IOException {
        write(b, 0, b.length);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     *
     * @param      b     the data.
     * @param      off   the start offset in the data.
     * @param      len   the number of bytes to write.
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        ensureCapacity(len);
        try {
            int ostored = cipher.update(b, off, len, obuffer);
            if (ostored > 0) {
                output.write(obuffer, 0, ostored);
            }
        } catch (ShortBufferException e) {
            // should never happen; re-throw just in case
            throw new IOException(e);
        }
    }

    /**
     * Flushes this output stream by forcing any buffered output bytes
     * that have already been processed by the encapsulated cipher object
     * to be written out.
     *
     * <p>Any bytes buffered by the encapsulated cipher
     * and waiting to be processed by it will not be written out. For example,
     * if the encapsulated cipher is a block cipher, and the total number of
     * bytes written using one of the <code>write</code> methods is less than
     * the cipher's block size, no bytes will be written out.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void flush() throws IOException {
        // simply call output.flush() since 'obuffer' content is always
        // written out immediately
        output.flush();
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream.
     * <p>
     * This method invokes the <code>doFinal</code> method of the encapsulated
     * cipher object, which causes any bytes buffered by the encapsulated
     * cipher to be processed. The result is written out by calling the
     * <code>flush</code> method of this output stream.
     * <p>
     * This method resets the encapsulated cipher object to its initial state
     * and calls the <code>close</code> method of the underlying output
     * stream.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;
        ensureCapacity(0);
        try {
            int ostored = cipher.doFinal(obuffer, 0);
            if (ostored > 0) {
                output.write(obuffer, 0, ostored);
            }
        } catch (IllegalBlockSizeException | BadPaddingException
                | ShortBufferException e) {
        }
        obuffer = null;
        try {
            flush();
        } catch (IOException ignored) {}
        output.close();
    }
}
