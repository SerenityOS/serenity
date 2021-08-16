/*
 * Copyright (c) 2018 by SAP AG, Walldorf, Germany.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */

package test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * This is a class loader which can load the same classes as another class loader.
 * <p>
 * This is mainly useful for tests when you want to load a class, but do it with a class
 * loader you can dispose. The clone loader just asks the loader to be cloned to get
 * the bytecodes, but defines the class itself.
 * <p>
 * Additionally you can specify a set of classes the loader should not be able to load.
 */
public class IAE_Loader2 extends SecureClassLoader {

    /**
     * The class loaded to clone.
     */
    private final ClassLoader toClone;

    /**
     * The strings we cannot load.
     */
    private final HashSet<String> notLoadable;

    /**
     * The strings we just delegate.
     */
    private final HashSet<String> simpleDelegate;

    /**
     * Creates a class loader which can load the same classes as the loader which
     * loaded the <code>IAE_Loader2</code> class itself.
     * <p>
     * Only the classes which are loadable by the 'parent' loader are delegated to that
     * loader (to make it possible mix classes).
     *
     * @param name the name of the class loader.
     * @param parent the parent loader which is first asked to load a class.
     * @param toClone the class loader to mimic. The clone class loader will be able to
     *                load the same classes as the 'toClone' loader.
     * @param notLoadable The classes we should not be able to load via this loader.
     * @param simpleDelegate The names of the classes for which we simply delegate.
     */
    public IAE_Loader2(String name, ClassLoader parent, ClassLoader toClone,
                       String[] notLoadable, String[] simpleDelegate) {
        super(name, parent);

        this.toClone = toClone;
        this.notLoadable    = Arrays.stream(notLoadable).collect(Collectors.toCollection(HashSet<String>::new));
        this.simpleDelegate = Arrays.stream(simpleDelegate).collect(Collectors.toCollection(HashSet<String>::new));
    }

    /**
     * @see java.lang.ClassLoader#findClass(java.lang.String)
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (notLoadable.contains(name)) {
            throw new ClassNotFoundException("The clone class loader explicitely " +
                    "didn't found the class");
        }

        if (simpleDelegate.contains(name)) {
            return toClone.loadClass(name);
        }

        // We just ask the wrapper class loader to find the resource for us
        URL res = toClone.getResource(name.replace('.', '/') + ".class");

        if (res == null) {
            throw new ClassNotFoundException(name);
        }

        try {
            InputStream is = res.openStream();
            byte[] code = readStreamIntoBuffer(is, 8192);
            is.close();
            return defineClass(name, code, 0, code.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }
    }

    /**
     * Reads all data of a stream into a byte array. The method allocates as
     * much memory as necessary to put the whole data into that byte
     * array. The data is read in chunks of <code>chunkSize</code>
     * chunks.<br><br>
     * <b>Implementation Note: </b> The data is read in chunks of
     * <code>chunkSize</code> bytes. The data is copied to the result
     * array. The memory consumption at the end of the reading is
     * <code>2 x [size of resulting array] + chunkSize</code>.
     *
     * @param is the stream to read the data from
     * @param chunkSize the size of the chunks the data should be read in
     * @return the <b>whole</b> data of the stream read into an byte array
     * @throws IllegalArgumentException if chunkSize <= 0
     * @throws NullPointerException if is == null
     * @throws IOException thrown if the provided stream encounters IO problems
     */
    public static byte[] readStreamIntoBuffer(InputStream is, int chunkSize)
            throws IOException {

        // Check preconditions.
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize <= 0");
        }
        else if (is == null) {
            throw new NullPointerException("is is null");
        }

        // Temporary buffer for read operations and result buffer.
        byte[] tempBuffer = new byte[chunkSize];
        byte[] buffer     = new byte[0];

        int bytesRead = 0;  // bytes actual read
        int oldSize   = 0;  // size of the resulting buffer

        while ((bytesRead = is.read(tempBuffer)) > 0) {

            // Temporary reference to the buffer for the copy operation.
            byte[] oldBuffer = buffer;

            // Create a new buffer with the size needed and copy data.
            buffer = new byte[oldSize + bytesRead];
            System.arraycopy(oldBuffer,  0, buffer, 0,       oldBuffer.length);

            // Copy the new data.
            System.arraycopy(tempBuffer, 0, buffer, oldSize, bytesRead);
            oldSize += bytesRead;
        }

        return buffer;
    }
}
