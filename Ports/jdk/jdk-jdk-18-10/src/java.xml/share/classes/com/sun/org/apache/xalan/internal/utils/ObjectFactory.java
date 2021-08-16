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

package com.sun.org.apache.xalan.internal.utils;

import java.util.function.Supplier;
import jdk.xml.internal.SecuritySupport;

/**
 * This class is duplicated for each JAXP subpackage so keep it in sync.
 * It is package private and therefore is not exposed as part of the JAXP
 * API.
 * <p>
 * This class was moved from the <code>javax.xml.parsers.ObjectFactory</code>
 * class and modified to be used as a general utility for creating objects
 * dynamically.
 *
 * @LastModified: Oct 2017
 */
public class ObjectFactory {

    //
    // Constants
    //
     private static final String JAXP_INTERNAL = "com.sun.org.apache";
     private static final String STAX_INTERNAL = "com.sun.xml.internal";

    /** Set to true for debugging */
    private static final boolean DEBUG = false;


    /** Prints a message to standard error if debugging is enabled. */
    private static void debugPrintln(Supplier<String> msgGen) {
        if (DEBUG) {
            System.err.println("JAXP: " + msgGen.get());
        }
    } // debugPrintln(String)

    /**
     * Figure out which ClassLoader to use.  For JDK 1.2 and later use
     * the context ClassLoader.
     */
    @SuppressWarnings("removal")
    public static ClassLoader findClassLoader()
    {
        if (System.getSecurityManager()!=null) {
            //this will ensure bootclassloader is used
            return null;
        }

        // Figure out which ClassLoader to use for loading the provider
        // class.  If there is a Context ClassLoader then use it.
        ClassLoader context = SecuritySupport.getContextClassLoader();
        ClassLoader system = SecuritySupport.getSystemClassLoader();

        ClassLoader chain = system;
        while (true) {
            if (context == chain) {
                // Assert: we are on JDK 1.1 or we have no Context ClassLoader
                // or any Context ClassLoader in chain of system classloader
                // (including extension ClassLoader) so extend to widest
                // ClassLoader (always look in system ClassLoader if Xalan
                // is in boot/extension/system classpath and in current
                // ClassLoader otherwise); normal classloaders delegate
                // back to system ClassLoader first so this widening doesn't
                // change the fact that context ClassLoader will be consulted
                ClassLoader current = ObjectFactory.class.getClassLoader();

                chain = system;
                while (true) {
                    if (current == chain) {
                        // Assert: Current ClassLoader in chain of
                        // boot/extension/system ClassLoaders
                        return system;
                    }
                    if (chain == null) {
                        break;
                    }
                    chain = SecuritySupport.getParentClassLoader(chain);
                }

                // Assert: Current ClassLoader not in chain of
                // boot/extension/system ClassLoaders
                return current;
            }

            if (chain == null) {
                // boot ClassLoader reached
                break;
            }

            // Check for any extension ClassLoaders in chain up to
            // boot ClassLoader
            chain = SecuritySupport.getParentClassLoader(chain);
        }

        // Assert: Context ClassLoader not in chain of
        // boot/extension/system ClassLoaders
        return context;
    } // findClassLoader():ClassLoader

    /**
     * Create an instance of a class using the same class loader for the ObjectFactory by default
     * or boot class loader when Security Manager is in place
     */
    public static Object newInstance(String className, boolean doFallback)
        throws ConfigurationError
    {
        @SuppressWarnings("removal")
        ClassLoader cl = System.getSecurityManager()!=null ? null : findClassLoader();
        try{
            Class<?> providerClass = findProviderClass(className, cl, doFallback);
            Object instance = providerClass.getConstructor().newInstance();
            debugPrintln(()->"created new instance of " + providerClass +
                             " using ClassLoader: " + cl);
            return instance;
        } catch (ClassNotFoundException x) {
            throw new ConfigurationError(
                "Provider " + className + " not found", x);
        } catch (Exception x) {
            throw new ConfigurationError(
                "Provider " + className + " could not be instantiated: " + x,
                x);
        }
    }

    /**
     * Find a Class using the same class loader for the ObjectFactory by default
     * or boot class loader when Security Manager is in place
     */
    public static Class<?> findProviderClass(String className, boolean doFallback)
        throws ClassNotFoundException, ConfigurationError
    {
        return findProviderClass (className,
                findClassLoader (), doFallback);
    }

    /**
     * Find a Class using the specified ClassLoader
     */
    private static Class<?> findProviderClass(String className, ClassLoader cl,
                                           boolean doFallback)
        throws ClassNotFoundException, ConfigurationError
    {
        //throw security exception if the calling thread is not allowed to access the
        //class. Restrict the access to the package classes as specified in java.security policy.
        @SuppressWarnings("removal")
        SecurityManager security = System.getSecurityManager();
        try{
            if (security != null){
                if (className.startsWith(JAXP_INTERNAL) ||
                    className.startsWith(STAX_INTERNAL)) {
                    cl = null;
                } else {
                    final int lastDot = className.lastIndexOf(".");
                    String packageName = className;
                    if (lastDot != -1) packageName = className.substring(0, lastDot);
                    security.checkPackageAccess(packageName);
                }
             }
        }catch(SecurityException e){
            throw e;
        }

        Class<?> providerClass;
        if (cl == null) {
            providerClass = Class.forName(className, false, ObjectFactory.class.getClassLoader());
        } else {
            try {
                providerClass = cl.loadClass(className);
            } catch (ClassNotFoundException x) {
                if (doFallback) {
                    // Fall back to current classloader
                    ClassLoader current = ObjectFactory.class.getClassLoader();
                    if (current == null) {
                        providerClass = Class.forName(className);
                    } else if (cl != current) {
                        cl = current;
                        providerClass = cl.loadClass(className);
                    } else {
                        throw x;
                    }
                } else {
                    throw x;
                }
            }
        }

        return providerClass;
    }

} // class ObjectFactory
