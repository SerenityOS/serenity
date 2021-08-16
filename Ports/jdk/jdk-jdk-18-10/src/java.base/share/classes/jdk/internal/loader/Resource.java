/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.loader;

import java.io.EOFException;
import java.net.URL;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.security.CodeSigner;
import java.util.jar.Manifest;
import java.nio.ByteBuffer;
import java.util.Arrays;
import sun.nio.ByteBuffered;

/**
 * This class is used to represent a Resource that has been loaded
 * from the class path.
 *
 * @author  David Connelly
 * @since   1.2
 */
public abstract class Resource {
    /**
     * Returns the name of the Resource.
     */
    public abstract String getName();

    /**
     * Returns the URL of the Resource.
     */
    public abstract URL getURL();

    /**
     * Returns the CodeSource URL for the Resource.
     */
    public abstract URL getCodeSourceURL();

    /**
     * Returns an InputStream for reading the Resource data.
     */
    public abstract InputStream getInputStream() throws IOException;

    /**
     * Returns the length of the Resource data, or -1 if unknown.
     */
    public abstract int getContentLength() throws IOException;

    private InputStream cis;

    /* Cache result in case getBytes is called after getByteBuffer. */
    private synchronized InputStream cachedInputStream() throws IOException {
        if (cis == null) {
            cis = getInputStream();
        }
        return cis;
    }

    /**
     * Returns the Resource data as an array of bytes.
     */
    public byte[] getBytes() throws IOException {
        byte[] b;
        // Get stream before content length so that a FileNotFoundException
        // can propagate upwards without being caught too early
        InputStream in = cachedInputStream();

        // This code has been uglified to protect against interrupts.
        // Even if a thread has been interrupted when loading resources,
        // the IO should not abort, so must carefully retry, failing only
        // if the retry leads to some other IO exception.

        boolean isInterrupted = Thread.interrupted();
        int len;
        for (;;) {
            try {
                len = getContentLength();
                break;
            } catch (InterruptedIOException iioe) {
                Thread.interrupted();
                isInterrupted = true;
            }
        }

        try {
            b = new byte[0];
            if (len == -1) len = Integer.MAX_VALUE;
            int pos = 0;
            while (pos < len) {
                int bytesToRead;
                if (pos >= b.length) { // Only expand when there's no room
                    bytesToRead = Math.min(len - pos, b.length + 1024);
                    if (bytesToRead < 0) {
                        // Can overflow only due to large b.length
                        bytesToRead = len - pos;
                    }
                    b = Arrays.copyOf(b, pos + bytesToRead);
                } else {
                    bytesToRead = b.length - pos;
                }
                int cc = 0;
                try {
                    cc = in.read(b, pos, bytesToRead);
                } catch (InterruptedIOException iioe) {
                    Thread.interrupted();
                    isInterrupted = true;
                }
                if (cc < 0) {
                    if (len != Integer.MAX_VALUE) {
                        throw new EOFException("Detect premature EOF");
                    } else {
                        if (b.length != pos) {
                            b = Arrays.copyOf(b, pos);
                        }
                        break;
                    }
                }
                pos += cc;
            }
        } finally {
            try {
                in.close();
            } catch (InterruptedIOException iioe) {
                isInterrupted = true;
            } catch (IOException ignore) {}

            if (isInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return b;
    }

    /**
     * Returns the Resource data as a ByteBuffer, but only if the input stream
     * was implemented on top of a ByteBuffer. Return {@code null} otherwise.
     * @return Resource data or null.
     */
    public ByteBuffer getByteBuffer() throws IOException {
        InputStream in = cachedInputStream();
        if (in instanceof ByteBuffered) {
            return ((ByteBuffered)in).getByteBuffer();
        }
        return null;
    }

    /**
     * Returns the Manifest for the Resource, or null if none.
     */
    public Manifest getManifest() throws IOException {
        return null;
    }

    /**
     * Returns theCertificates for the Resource, or null if none.
     */
    public java.security.cert.Certificate[] getCertificates() {
        return null;
    }

    /**
     * Returns the code signers for the Resource, or null if none.
     */
    public CodeSigner[] getCodeSigners() {
        return null;
    }
}
