/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.security.SecureClassLoader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@code ByteCodeLoader} can be used for easy loading of byte code already
 * present in memory.
 *
 * {@code InMemoryCompiler} can be used for compiling source code in a string
 * into byte code, which then can be loaded with {@code ByteCodeLoader}.
 *
 * @see InMemoryCompiler
 */
public class ByteCodeLoader extends SecureClassLoader {
    private final Map<String,byte[]> classBytesMap;
    private final Map<String,Class<?>> cache;

    public ByteCodeLoader(Map<String,byte[]> classBytesMap, ClassLoader parent) {
        super(parent);
        this.classBytesMap = classBytesMap;
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new {@code ByteCodeLoader} ready to load a class with the
     * given name and the given byte code, using the default parent class
     * loader for delegation.
     *
     * @param className The name of the class
     * @param byteCode The byte code of the class
     */
    public ByteCodeLoader(String className, byte[] byteCode) {
        super();
        classBytesMap = Map.of(className, byteCode);
        cache = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new {@code ByteCodeLoader} ready to load a class with the
     * given name and the given byte code, using the specified parent class
     * loader for delegation.
     *
     * @param className The name of the class
     * @param byteCode The byte code of the class
     * @param parent The parent class loader for delegation
     */
    public ByteCodeLoader(String className, byte[] byteCode, ClassLoader parent) {
        this(Map.of(className, byteCode), parent);
    }

    private static final Object lock = new Object();

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        if (classBytesMap.get(name) == null) {
            return super.loadClass(name);
        }
        Class<?> cls = cache.get(name);
        if (cls != null) {
            return cls;
        }
        synchronized (lock) {
            cls = cache.get(name);
            if (cls == null) {
                cls = findClass(name);
                cache.put(name, cls);
            }
        }
        return cls;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] byteCode = classBytesMap.get(name);
        if (byteCode == null) {
            throw new ClassNotFoundException(name);
        }
        return defineClass(name, byteCode, 0, byteCode.length);
    }

    /**
     * Utility method for creating a new {@code ByteCodeLoader} and then
     * directly load the given byte code.
     *
     * @param className The name of the class
     * @param byteCode The byte code for the class
     * @throws ClassNotFoundException if the class can't be loaded
     * @return A {@see Class} object representing the class
     */
    public static Class<?> load(String className, byte[] byteCode) throws ClassNotFoundException {
        return new ByteCodeLoader(className, byteCode).loadClass(className);
    }
}
