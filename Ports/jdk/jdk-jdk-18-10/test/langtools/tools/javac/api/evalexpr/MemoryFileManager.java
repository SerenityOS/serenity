/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
 */

package evalexpr;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.CharBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

/**
 * A file manager for compiling strings to byte arrays.
 * This file manager delegates to another file manager
 * to lookup classes on boot class path.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own
 * risk.  This code and its internal interfaces are subject to change
 * or deletion without notice.</b></p>
 * @author Peter von der Ah&eacute;
 */
public final class MemoryFileManager extends ForwardingJavaFileManager {
    /**
     * Maps binary class names to class files stored as byte arrays.
     */
    private Map<String, byte[]> classes;

    /**
     * Creates a JavaFileObject representing the given compilation unit.
     * @param name a name representing this source code, for example, the name of a class
     * @param code a compilation unit (source code for a Java program)
     * @return a JavaFileObject represtenting the given compilation unit
     */
    public static JavaFileObject makeSource(String name, String code) {
        return new JavaSourceFromString(name, code);
    }

    /**
     * Construct a memory file manager which delegates to the specified
     * file manager for unknown sources.
     * @param fileManager a file manager used to look up class files on class path, etc.
     */
    public MemoryFileManager(JavaFileManager fileManager) {
        super(fileManager);
        classes = new HashMap<String, byte[]>();
    }

    /**
     * Get a class loader which first search the classes stored
     * by this file mananger.
     * @return a class loader for compiled files
     */
    @Override
    public ClassLoader getClassLoader(Location location) {
        return new ByteArrayClassLoader(classes);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location,
                                               String name,
                                               Kind kind,
                                               FileObject originatingSource)
        throws UnsupportedOperationException
    {
        if (originatingSource instanceof JavaSourceFromString) {
            return new JavaClassInArray(name);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected static URI uriFromString(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * A file object representing a Java class file stored in a byte array.
     */
    private class JavaClassInArray extends SimpleJavaFileObject {

        private String name;

        /**
         * Constructs a JavaClassInArray object.
         * @param name binary name of the class to be stored in this file object
         */
        JavaClassInArray(String name) {
            super(uriFromString("mfm:///" + name.replace('.','/') + Kind.CLASS.extension),
                  Kind.CLASS);
            this.name = name;
        }

        public OutputStream openOutputStream() {
            return new FilterOutputStream(new ByteArrayOutputStream()) {
                public void close() throws IOException {
                    out.close();
                    ByteArrayOutputStream bos = (ByteArrayOutputStream)out;
                    classes.put(name, bos.toByteArray());
                    System.out.println("compiled " + name);
                }
            };
        }
    }

    /**
     * A file object used to represent source coming from a string.
     */
    private static class JavaSourceFromString extends SimpleJavaFileObject {
        /**
         * The source code of this "file".
         */
        final String code;

        /**
         * Constructs a new JavaSourceFromString.
         * @param name the name of the compilation unit represented by this file object
         * @param code the source code for the compilation unit represented by this file object
         */
        JavaSourceFromString(String name, String code) {
            super(uriFromString("mfm:///" + name.replace('.','/') + Kind.SOURCE.extension),
                  Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }

}
