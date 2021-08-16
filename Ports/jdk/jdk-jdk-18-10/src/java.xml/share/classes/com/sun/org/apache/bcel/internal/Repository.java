/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.org.apache.bcel.internal;


import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.util.SyntheticRepository;

/**
 * The repository maintains informations about class interdependencies, e.g.,
 * whether a class is a sub-class of another. Delegates actual class loading
 * to SyntheticRepository with current class path by default.
 *
 * @see com.sun.org.apache.bcel.internal.util.Repository
 * @see SyntheticRepository
 *
 * @LastModified: Jan 2020
 */
public abstract class Repository {

    private static com.sun.org.apache.bcel.internal.util.Repository repository
            = SyntheticRepository.getInstance();


    /**
     * @return currently used repository instance
     */
    public static com.sun.org.apache.bcel.internal.util.Repository getRepository() {
        return repository;
    }


    /**
     * Sets repository instance to be used for class loading
     */
    public static void setRepository( final com.sun.org.apache.bcel.internal.util.Repository rep ) {
        repository = rep;
    }


    /**
     * Lookups class somewhere found on your CLASSPATH, or whereever the
     * repository instance looks for it.
     *
     * @return class object for given fully qualified class name
     * @throws ClassNotFoundException if the class could not be found or
     * parsed correctly
     */
    public static JavaClass lookupClass( final String class_name ) throws ClassNotFoundException {
        return repository.loadClass(class_name);
    }


    /**
     * Tries to find class source using the internal repository instance.
     *
     * @see Class
     * @return JavaClass object for given runtime class
     * @throws ClassNotFoundException if the class could not be found or
     * parsed correctly
     */
    public static JavaClass lookupClass( final Class<?> clazz ) throws ClassNotFoundException {
        return repository.loadClass(clazz);
    }


    /**
     * Clear the repository.
     */
    public static void clearCache() {
        repository.clear();
    }


    /**
     * Adds clazz to repository if there isn't an equally named class already in there.
     *
     * @return old entry in repository
     */
    public static JavaClass addClass( final JavaClass clazz ) {
        final JavaClass old = repository.findClass(clazz.getClassName());
        repository.storeClass(clazz);
        return old;
    }


    /**
     * Removes class with given (fully qualified) name from repository.
     */
    public static void removeClass( final String clazz ) {
        repository.removeClass(repository.findClass(clazz));
    }


    /**
     * Removes given class from repository.
     */
    public static void removeClass( final JavaClass clazz ) {
        repository.removeClass(clazz);
    }


    /**
     * @return list of super classes of clazz in ascending order, i.e.,
     * Object is always the last element
     * @throws ClassNotFoundException if any of the superclasses can't be found
     */
    public static JavaClass[] getSuperClasses( final JavaClass clazz ) throws ClassNotFoundException {
        return clazz.getSuperClasses();
    }


    /**
     * @return list of super classes of clazz in ascending order, i.e.,
     * Object is always the last element.
     * @throws ClassNotFoundException if the named class or any of its
     *  superclasses can't be found
     */
    public static JavaClass[] getSuperClasses( final String class_name ) throws ClassNotFoundException {
        final JavaClass jc = lookupClass(class_name);
        return getSuperClasses(jc);
    }


    /**
     * @return all interfaces implemented by class and its super
     * classes and the interfaces that those interfaces extend, and so on.
     * (Some people call this a transitive hull).
     * @throws ClassNotFoundException if any of the class's
     *  superclasses or superinterfaces can't be found
     */
    public static JavaClass[] getInterfaces( final JavaClass clazz ) throws ClassNotFoundException {
        return clazz.getAllInterfaces();
    }


    /**
     * @return all interfaces implemented by class and its super
     * classes and the interfaces that extend those interfaces, and so on
     * @throws ClassNotFoundException if the named class can't be found,
     *   or if any of its superclasses or superinterfaces can't be found
     */
    public static JavaClass[] getInterfaces( final String class_name ) throws ClassNotFoundException {
        return getInterfaces(lookupClass(class_name));
    }


    /**
     * Equivalent to runtime "instanceof" operator.
     * @return true, if clazz is an instance of super_class
     * @throws ClassNotFoundException if any superclasses or superinterfaces
     *   of clazz can't be found
     */
    public static boolean instanceOf( final JavaClass clazz, final JavaClass super_class )
            throws ClassNotFoundException {
        return clazz.instanceOf(super_class);
    }


    /**
     * @return true, if clazz is an instance of super_class
     * @throws ClassNotFoundException if either clazz or super_class
     *   can't be found
     */
    public static boolean instanceOf( final String clazz, final String super_class )
            throws ClassNotFoundException {
        return instanceOf(lookupClass(clazz), lookupClass(super_class));
    }


    /**
     * @return true, if clazz is an instance of super_class
     * @throws ClassNotFoundException if super_class can't be found
     */
    public static boolean instanceOf( final JavaClass clazz, final String super_class )
            throws ClassNotFoundException {
        return instanceOf(clazz, lookupClass(super_class));
    }


    /**
     * @return true, if clazz is an instance of super_class
     * @throws ClassNotFoundException if clazz can't be found
     */
    public static boolean instanceOf( final String clazz, final JavaClass super_class )
            throws ClassNotFoundException {
        return instanceOf(lookupClass(clazz), super_class);
    }


    /**
     * @return true, if clazz is an implementation of interface inter
     * @throws ClassNotFoundException if any superclasses or superinterfaces
     *   of clazz can't be found
     */
    public static boolean implementationOf( final JavaClass clazz, final JavaClass inter )
            throws ClassNotFoundException {
        return clazz.implementationOf(inter);
    }


    /**
     * @return true, if clazz is an implementation of interface inter
     * @throws ClassNotFoundException if clazz, inter, or any superclasses
     *   or superinterfaces of clazz can't be found
     */
    public static boolean implementationOf( final String clazz, final String inter )
            throws ClassNotFoundException {
        return implementationOf(lookupClass(clazz), lookupClass(inter));
    }


    /**
     * @return true, if clazz is an implementation of interface inter
     * @throws ClassNotFoundException if inter or any superclasses
     *   or superinterfaces of clazz can't be found
     */
    public static boolean implementationOf( final JavaClass clazz, final String inter )
            throws ClassNotFoundException {
        return implementationOf(clazz, lookupClass(inter));
    }


    /**
     * @return true, if clazz is an implementation of interface inter
     * @throws ClassNotFoundException if clazz or any superclasses or
     *   superinterfaces of clazz can't be found
     */
    public static boolean implementationOf( final String clazz, final JavaClass inter )
            throws ClassNotFoundException {
        return implementationOf(lookupClass(clazz), inter);
    }
}
