/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sun.org.apache.bcel.internal.util;

import java.io.IOException;
import java.io.InputStream;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

/**
 * This repository is used in situations where a Class is created outside the
 * realm of a ClassLoader. Classes are loaded from the file systems using the
 * paths specified in the given class path. By default, this is the value
 * returned by ClassPath.getClassPath(). <br>
 * This repository uses a factory design, allowing it to maintain a collection
 * of different classpaths, and as such It is designed to be used as a singleton
 * per classpath.
 *
 * @see com.sun.org.apache.bcel.internal.Repository
 *
 * @LastModified: May 2021
 */
public class SyntheticRepository implements Repository {

    // CLASSNAME X JAVACLASS
    private final Map<String, SoftReference<JavaClass>> loadedClasses = new HashMap<>();

    private SyntheticRepository() {
    }

    public static SyntheticRepository getInstance() {
        return new SyntheticRepository();
    }

    /**
     * Store a new JavaClass instance into this Repository.
     */
    @Override
    public void storeClass(final JavaClass clazz) {
        loadedClasses.put(clazz.getClassName(), new SoftReference<>(clazz));
        clazz.setRepository(this);
        }

    /**
     * Remove class from repository
     */
    @Override
    public void removeClass(final JavaClass clazz) {
        loadedClasses.remove(clazz.getClassName());
    }

    /**
     * Find an already defined (cached) JavaClass object by name.
     */
    @Override
    public JavaClass findClass(final String className) {
        final SoftReference<JavaClass> ref = loadedClasses.get(className);
        if (ref == null) {
            return null;
}
        return ref.get();
    }

    /**
     * Finds a JavaClass object by name. If it is already in this Repository, the
     * Repository version is returned.
     *
     * @param className the name of the class
     * @return the JavaClass object
     * @throws ClassNotFoundException if the class is not in the Repository
     */
    @Override
    public JavaClass loadClass(String className) throws ClassNotFoundException {
        if ((className == null) || className.isEmpty()) {
            throw new IllegalArgumentException("Invalid class name " + className);
    }
        className = className.replace('/', '.'); // Just in case, canonical form
        final JavaClass clazz = findClass(className);
        if (clazz != null) {
            return clazz;
        }

        IOException e = new IOException("Couldn't find: " + className + ".class");
        throw new ClassNotFoundException("Exception while looking for class " +
                className + ": " + e, e);
    }

    /**
     * Find the JavaClass object for a runtime Class object. If a class with the
     * same name is already in this Repository, the Repository version is
     * returned. Otherwise, getResourceAsStream() is called on the Class object
     * to find the class's representation. If the representation is found, it is
     * added to the Repository.
     *
     * @see Class
     * @param clazz the runtime Class object
     * @return JavaClass object for given runtime class
     * @throws ClassNotFoundException if the class is not in the Repository, and
     * its representation could not be found
     */
    @Override
    public JavaClass loadClass(final Class<?> clazz) throws ClassNotFoundException {
        final String className = clazz.getName();
        final JavaClass repositoryClass = findClass(className);
        if (repositoryClass != null) {
            return repositoryClass;
    }
        String name = className;
        final int i = name.lastIndexOf('.');
        if (i > 0) {
            name = name.substring(i + 1);
        }
        JavaClass cls = null;
        try (InputStream clsStream = clazz.getResourceAsStream(name + ".class")) {
            return cls = loadClass(clsStream, className);
        } catch (final IOException e) {
            return cls;
        }

    }


    private JavaClass loadClass(final InputStream is, final String className)
            throws ClassNotFoundException {
        try {
            if (is != null) {
                final ClassParser parser = new ClassParser(is, className);
                final JavaClass clazz = parser.parse();
                storeClass(clazz);
                return clazz;
            }
        } catch (final IOException e) {
            throw new ClassNotFoundException("Exception while looking for class "
                    + className + ": " + e, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (final IOException e) {
                    // ignored
                }
            }
        }
        throw new ClassNotFoundException("SyntheticRepository could not load "
                + className);
    }

    /**
     * Clear all entries from cache.
     */
    @Override
    public void clear() {
        loadedClasses.clear();
    }
}
