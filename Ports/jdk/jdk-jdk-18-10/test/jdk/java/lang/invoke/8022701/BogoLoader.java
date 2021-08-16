/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import jdk.internal.org.objectweb.asm.*;

public class BogoLoader extends ClassLoader {

    static interface VisitorMaker {
    ClassVisitor make(ClassVisitor visitor);
    }


    /**
     * Use this property to verify that the desired classloading is happening.
     */
    private final boolean verbose = Boolean.getBoolean("bogoloader.verbose");
    /**
     * Use this property to disable replacement for testing purposes.
     */
    private final boolean noReplace = Boolean.getBoolean("bogoloader.noreplace");

    /**
     * Set of class names that should be loaded with this loader.
     * Others are loaded with the system class loader, except for those
     * that are transformed.
     */
    private Set<String> nonSystem;

    /**
     * Map from class names to a bytecode transformer factory.
     */
    private Map<String, VisitorMaker> replaced;

    /**
     * Keep track (not terribly efficiently) of which classes have already
     * been loaded by this class loader.
     */
    private final Vector<String> history = new Vector<String>();

    private boolean useSystemLoader(String name) {
        return ! nonSystem.contains(name) && ! replaced.containsKey(name);
    }

    public BogoLoader(Set<String> non_system, Map<String, VisitorMaker> replaced) {
        super(Thread.currentThread().getContextClassLoader());
        this.nonSystem = non_system;
        this.replaced = replaced;
    }

    private byte[] readResource(String className) throws IOException {
        return readResource(className, "class");
    }

    private byte[] readResource(String className, String suffix) throws IOException {
        // Note to the unwary -- "/" works on Windows, leave it alone.
        String fileName = className.replace('.', '/') + "." + suffix;
        InputStream origStream = getResourceAsStream(fileName);
        if (origStream == null) {
            throw new IOException("Resource not found : " + fileName);
        }
        BufferedInputStream stream = new java.io.BufferedInputStream(origStream);
        byte[] data = new byte[stream.available()];
        int how_many = stream.read(data);
        // Really ought to deal with the corner cases of stream.available()
        return data;
    }

    protected byte[] getClass(String name) throws ClassNotFoundException,
    IOException {
        return readResource(name, "class");
    }

    /**
     * Loads the named class from the system class loader unless
     * the name appears in either replaced or nonSystem.
     * nonSystem classes are loaded into this classloader,
     * and replaced classes get their content from the specified array
     * of bytes (and are also loaded into this classloader).
     */
    protected Class<?> loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        Class<?> clazz;

        if (history.contains(name)) {
            Class<?> c = this.findLoadedClass(name);
            return c;
        }
        if (useSystemLoader(name)) {
            clazz = findSystemClass(name);
            if (verbose) System.err.println("Loading system class " + name);
        } else {
            history.add(name);
            try {
                if (verbose) {
                    System.err.println("Loading classloader class " + name);
                }
                byte[] classData = getClass(name);;
                boolean expanded = false;
                if (!noReplace && replaced.containsKey(name)) {
                    if (verbose) {
                        System.err.println("Replacing class " + name);
                    }
                    ClassReader cr = new ClassReader(classData);
                    ClassWriter cw = new ClassWriter(0);
                    VisitorMaker vm = replaced.get(name);
                    cr.accept(vm.make(cw), 0);
                    classData = cw.toByteArray();
                }
                clazz = defineClass(name, classData, 0, classData.length);
            } catch (java.io.EOFException ioe) {
                throw new ClassNotFoundException(
                        "IO Exception in reading class : " + name + " ", ioe);
            } catch (ClassFormatError ioe) {
                throw new ClassNotFoundException(
                        "ClassFormatError in reading class file: ", ioe);
            } catch (IOException ioe) {
                throw new ClassNotFoundException(
                        "IO Exception in reading class file: ", ioe);
            }
        }
        if (clazz == null) {
            throw new ClassNotFoundException(name);
        }
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }
}
