/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import java.io.*;

/**
 * The <code>CustomClassLoader</code> class is used in <code>ClassUnloader</code>.
 *
 * <p>This class loader can load classes and notify <code>ClassUnloader</code>
 * about own finalization to make sure that all loaded classes have been unloaded.
 *
 * <p>By default this class loader loads class from .class file located in directory
 * specified with <code>setClassPath()</code> method. To use any other technique
 * of class loading one should implement derived class, which would override
 * <code>findClass</code> method.
 *
 * @see nsk.share.ClassUnloader
 *
 * @see #setClassPath(String)
 * @see #findClass(String)
 */
public class CustomClassLoader extends ClassLoader {

    private ClassUnloader classUnloader;
    protected String classPath;

    /**
     * Initializes a newly created <code>CustomClassloader</code> object
     * not yet linked with any <code>ClassUnloader</code> object.
     *
     */
    public CustomClassLoader() {
        super(CustomClassLoader.class.getClassLoader());
        this.classUnloader = null;
    }

    /**
     * Initializes a newly created <code>CustomClassloader</code> object
     * linked with specified <code>ClassUnloader</code> object.
     *
     * @param classUnloader an instance of <code>ClassUnloader</code>
     */
    public CustomClassLoader(ClassUnloader classUnloader) {
        super(CustomClassLoader.class.getClassLoader());
        this.classUnloader = classUnloader;
    }

    /**
     * Links this class loader with specified <code>ClassUnloader</code> object.
     *
     * @param classUnloader an instance of <code>ClassUnloader</code>
     */
    public void setClassUnloader(ClassUnloader classUnloader) {
        this.classUnloader = classUnloader;
    }

    /**
     * Specifies path to .class file location.
     *
     * @param classPath a path to .class file location
     */
    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    /**
     * Finds and loads class for specified class name.
     * This method loads class from .class file located in a directory
     * previously specified by <code>setClassPath()</code>.
     *
     * @param name The name of the class.
     *
     * @throws ClassNotFoundException if no .class file found
     *          for specified class name
     *
     * @see #setClassPath(String)
     */
    protected synchronized Class findClass(String name) throws ClassNotFoundException {
        java.nio.file.Path path = ClassFileFinder.findClassFile(name, classPath);
        if (path == null) {
            throw new ClassNotFoundException(name);
        }
        String classFileName = path.toString();

        FileInputStream in;
        try {
            in = new FileInputStream(classFileName);
            if (in == null) {
                throw new ClassNotFoundException(classFileName);
            }
        } catch (FileNotFoundException e) {
            throw new ClassNotFoundException(classFileName, e);
        }

        int len;
        byte data[];
        try {
            len = in.available();
            data = new byte[len];
            for (int total = 0; total < data.length; ) {
                total += in.read(data, total, data.length - total);
            }
        } catch (IOException e) {
            throw new ClassNotFoundException(classFileName, e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                throw new ClassNotFoundException(classFileName, e);
            }
        }

        return defineClass(name, data, 0, data.length);
    }

    /**
     * Notifies <code>ClassUnloader</code> about finalization.
     */
    protected void finalize() throws Throwable {

        // notify ClassUnloader about finalization
        if (classUnloader != null) {
            classUnloader.finalized = true;
        }

        super.finalize();
    }
}
