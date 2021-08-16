/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.util.jar;

import java.util.zip.*;
import java.io.*;
import sun.security.util.ManifestEntryVerifier;
import jdk.internal.util.jar.JarIndex;

/**
 * The {@code JarInputStream} class is used to read the contents of
 * a JAR file from any input stream. It extends the class
 * {@code java.util.zip.ZipInputStream} with support for reading
 * an optional {@code Manifest} entry. The {@code Manifest}
 * can be used to store meta-information about the JAR file and its entries.
 *
 * @author  David Connelly
 * @see     Manifest
 * @see     java.util.zip.ZipInputStream
 * @since   1.2
 */
public class JarInputStream extends ZipInputStream {
    private Manifest man;
    private JarEntry first;
    private JarVerifier jv;
    private ManifestEntryVerifier mev;
    private final boolean doVerify;
    private boolean tryManifest;

    /**
     * Creates a new {@code JarInputStream} and reads the optional
     * manifest. If a manifest is present, also attempts to verify
     * the signatures if the JarInputStream is signed.
     * @param in the actual input stream
     * @throws    IOException if an I/O error has occurred
     */
    public JarInputStream(InputStream in) throws IOException {
        this(in, true);
    }

    /**
     * Creates a new {@code JarInputStream} and reads the optional
     * manifest. If a manifest is present and verify is true, also attempts
     * to verify the signatures if the JarInputStream is signed.
     *
     * @param in the actual input stream
     * @param verify whether or not to verify the JarInputStream if
     * it is signed.
     * @throws    IOException if an I/O error has occurred
     */
    public JarInputStream(InputStream in, boolean verify) throws IOException {
        super(in);
        this.doVerify = verify;

        // This implementation assumes the META-INF/MANIFEST.MF entry
        // should be either the first or the second entry (when preceded
        // by the dir META-INF/). It skips the META-INF/ and then
        // "consumes" the MANIFEST.MF to initialize the Manifest object.
        JarEntry e = (JarEntry)super.getNextEntry();
        if (e != null && e.getName().equalsIgnoreCase("META-INF/"))
            e = (JarEntry)super.getNextEntry();
        first = checkManifest(e);
    }

    private JarEntry checkManifest(JarEntry e)
        throws IOException
    {
        if (e != null && JarFile.MANIFEST_NAME.equalsIgnoreCase(e.getName())) {
            man = new Manifest();
            byte[] bytes = readAllBytes();
            man.read(new ByteArrayInputStream(bytes));
            closeEntry();
            if (doVerify) {
                jv = new JarVerifier(e.getName(), bytes);
                mev = new ManifestEntryVerifier(man);
            }
            return (JarEntry)super.getNextEntry();
        }
        return e;
    }

    /**
     * Returns the {@code Manifest} for this JAR file, or
     * {@code null} if none.
     *
     * @return the {@code Manifest} for this JAR file, or
     *         {@code null} if none.
     */
    public Manifest getManifest() {
        return man;
    }

    /**
     * Reads the next ZIP file entry and positions the stream at the
     * beginning of the entry data. If verification has been enabled,
     * any invalid signature detected while positioning the stream for
     * the next entry will result in an exception.
     * @throws    ZipException if a ZIP file error has occurred
     * @throws    IOException if an I/O error has occurred
     * @throws    SecurityException if any of the jar file entries
     *         are incorrectly signed.
     */
    public ZipEntry getNextEntry() throws IOException {
        JarEntry e;
        if (first == null) {
            e = (JarEntry)super.getNextEntry();
            if (tryManifest) {
                e = checkManifest(e);
                tryManifest = false;
            }
        } else {
            e = first;
            if (first.getName().equalsIgnoreCase(JarIndex.INDEX_NAME))
                tryManifest = true;
            first = null;
        }
        if (jv != null && e != null) {
            // At this point, we might have parsed all the meta-inf
            // entries and have nothing to verify. If we have
            // nothing to verify, get rid of the JarVerifier object.
            if (jv.nothingToVerify() == true) {
                jv = null;
                mev = null;
            } else {
                jv.beginEntry(e, mev);
            }
        }
        return e;
    }

    /**
     * Reads the next JAR file entry and positions the stream at the
     * beginning of the entry data. If verification has been enabled,
     * any invalid signature detected while positioning the stream for
     * the next entry will result in an exception.
     * @return the next JAR file entry, or null if there are no more entries
     * @throws    ZipException if a ZIP file error has occurred
     * @throws    IOException if an I/O error has occurred
     * @throws    SecurityException if any of the jar file entries
     *         are incorrectly signed.
     */
    public JarEntry getNextJarEntry() throws IOException {
        return (JarEntry)getNextEntry();
    }

    /**
     * Reads from the current JAR file entry into an array of bytes.
     * If {@code len} is not zero, the method
     * blocks until some input is available; otherwise, no
     * bytes are read and {@code 0} is returned.
     * If verification has been enabled, any invalid signature
     * on the current entry will be reported at some point before the
     * end of the entry is reached.
     * @param b the buffer into which the data is read
     * @param off the start offset in the destination array {@code b}
     * @param len the maximum number of bytes to read
     * @return the actual number of bytes read, or -1 if the end of the
     *         entry is reached
     * @throws     NullPointerException If {@code b} is {@code null}.
     * @throws     IndexOutOfBoundsException If {@code off} is negative,
     * {@code len} is negative, or {@code len} is greater than
     * {@code b.length - off}
     * @throws    ZipException if a ZIP file error has occurred
     * @throws    IOException if an I/O error has occurred
     * @throws    SecurityException if any of the jar file entries
     *         are incorrectly signed.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int n;
        if (first == null) {
            n = super.read(b, off, len);
        } else {
            n = -1;
        }
        if (jv != null) {
            jv.update(n, b, off, len, mev);
        }
        return n;
    }

    /**
     * Creates a new {@code JarEntry} ({@code ZipEntry}) for the
     * specified JAR file entry name. The manifest attributes of
     * the specified JAR file entry name will be copied to the new
     * <CODE>JarEntry</CODE>.
     *
     * @param name the name of the JAR/ZIP file entry
     * @return the {@code JarEntry} object just created
     */
    protected ZipEntry createZipEntry(String name) {
        JarEntry e = new JarEntry(name);
        if (man != null) {
            e.attr = man.getAttributes(name);
        }
        return e;
    }
}
