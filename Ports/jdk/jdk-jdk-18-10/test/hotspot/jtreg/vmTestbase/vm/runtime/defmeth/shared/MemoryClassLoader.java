/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import nsk.share.TestFailure;

/**
 * Class loader for classes from internal in-memory cache
 */
public class MemoryClassLoader extends ClassLoader {
    private Map<String, byte[]> classes;

    public MemoryClassLoader(Map<String, byte[]> classes) {
        this.classes = new LinkedHashMap<>(classes);
    }

    public MemoryClassLoader(Map<String, byte[]> classes, ClassLoader parent) {
        super(parent);
        this.classes = new LinkedHashMap<>(classes);
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        // Lookup classes from default package eagerly.
        // Otherwise, it may interfere with other classes on classpath in testbase
        if (!name.contains(".") && classes.containsKey(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                byte[] code = classes.get(name);
                c = defineClass(name, code, 0, code.length);
            }
            if (resolve)  resolveClass(c);
            return c;
        } else {
            return super.loadClass(name, resolve);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (classes.containsKey(name)) {
            byte[] code = classes.get(name);
            return defineClass(name, code, 0, code.length);
        }
        return null;
    }

    /**
     * Check whether a class called {@code className} is loaded by any of
     * ancestor class loaders.
     *
     * @param className
     * @return
     */
    private boolean loadedByParent(String className) {
        try {
            return (getParent().loadClass(className) != null);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Pre-load all classes which are present in internal storage.
     *
     */
    public void tryPreloadClasses() {
        for (String name : classes.keySet()) {
            try {
                loadClass(name);
            } catch (ClassNotFoundException e) {
                // ignore errors during class loading
            }
        }
    }

    /**
     * Redefine all classes, loaded by this class loader with their very same
     * versions. Used to stress class redefinition code.
     *
     * @return
     */
    public MemoryClassLoader modifyClasses(boolean retransform) {
        for (Entry<String, byte[]> entry : classes.entrySet()) {
            modifyClass(entry, retransform);
        }
        return this;
    }

    public void modifyClasses(Map<String, byte[]> redefine, boolean retransform) {
        for (Entry<String,byte[]> entry : redefine.entrySet()) {
            modifyClass(entry, retransform);
        }
    }

    private void modifyClass(Entry<String,byte[]> entry, boolean retransform) {
        try {
            String name = entry.getKey();

            // Skip classes which are loaded by parent class loader and that are not
            // in the unnamed package (otherwise it'll pick up classes from other Testbase
            // tests, m.class for instance, that are on the regular classpath) e.g.
            // j.l.AbstractMethodError. Their placeholders are used during test creation,
            // but don't participate in test execution.
            if (name.contains(".") && loadedByParent(name)) {
                return;
            }

            Class clz = loadClass(name);
            byte[] code = entry.getValue();

            if (clz == null) {
                // Ignore not "loadable" classes.
                // For example, reflection scenarios don't generate classes for testers
                return;
            }

            if (retransform) {
                Util.retransformClass(clz, code);
            } else {
                Util.redefineClass(clz, code);
            }

        } catch (LinkageError e) {
            // Ignore errors during linkage, since some tests produce
            // incorrect class files intentionally.
        } catch (ClassNotFoundException ex) {
            throw new TestFailure(ex);
        }
    }

    /** Prepare test context (vm.runtime.defmeth.shared.TestContext) */
    private Class<?> contextClass;

    public synchronized Class<?> getTestContext() {
        if (contextClass == null) {
            String context = TestContext.class.getName();
            byte[] classFile = getClassFileFromParent(context);
            contextClass = defineClass(context, classFile, 0, classFile.length);
        }
        return contextClass;
    }

    /** Get class file content from parent class loader */
    private byte[] getClassFileFromParent(String name) {
        String resourceName = name.replaceAll("\\.","/")+".class";
        InputStream resource = getParent().getResourceAsStream(resourceName);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[8092];
        int count = -1;
        try {
            while ((count = resource.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            resource.close();
        } catch (NullPointerException npe) {
            // getResourceAsStream returns null for IOException
            System.out.println("Unable to get resourceName " + resourceName);
            throw new Error(npe);
        } catch (IOException ex) {
            throw new Error(ex);
        }

        return output.toByteArray();
    }
}
