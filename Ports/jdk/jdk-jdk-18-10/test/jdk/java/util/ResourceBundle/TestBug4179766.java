/*
 * Copyright (c) 2007, 2010, Oracle and/or its affiliates. All rights reserved.
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
/*
    @test  1.3 99/02/15
    @summary test Resource Bundle for bug 4179766
    @build Bug4179766Class Bug4179766Resource Bug4179766Getter
    @run main TestBug4179766
    @bug 4179766
*/
/*
 *
 *
 * (C) Copyright IBM Corp. 1996 - 1999 - All Rights Reserved
 *
 * Portions copyright (c) 2007 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * The original version of this source code and documentation
 * is copyrighted and owned by Taligent, Inc., a wholly-owned
 * subsidiary of IBM. These materials are provided under terms
 * of a License Agreement between Taligent and Sun. This technology
 * is protected by multiple US and International patents.
 *
 * This notice and attribution to Taligent may not be removed.
 * Taligent is a registered trademark of Taligent, Inc.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies. Please refer to the file "copyright.html"
 * for further important copyright and licensing information.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

import java.util.Hashtable;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Hashtable;
import java.io.File;
import java.io.FileInputStream;

/**
 * This class tests the behavior of the ResourceBundle cache with
 * respect to ClassLoaders.  The same resource loaded by different
 * loaders should be cached as separate objects, one for each loader.
 * In order to test this behavior, this test constructs a custom
 * class loader to load its resources.  It does not delegate resource
 * loading to the system loader to load the class files, but loads
 * them from the current directory instead.  This is so that the
 * defining class loader for the resources is different.  If it
 * delegated to the system loader to load the resources, the
 * defining ClassLoader would be the same even though the initiating
 * loader differered, and the resource would only be cached once.
 */
public class TestBug4179766 extends RBTestFmwk {
    //hash code used by class loaders when sameHash is true
    private static final int SAME_HASH_CODE = 0;
    //the next unique hash code
    private static int nextHashCode = SAME_HASH_CODE + 1;
    //suffix on class files
    private static final String CLASS_SUFFIX = ".class";

    //generate a unique hashcode for a class loader
    private static synchronized int getNextHashCode() {
        return nextHashCode++;
    }

    public static void main(String[] args) throws Exception {
        //static links so all needed classes get compiled
        Object o1 = new Bug4179766Class();
        Object o2 = new Bug4179766Resource();
        new TestBug4179766().run(args);
    }

    /**
    * Ensure the resource cache is working correctly for a single
    * resource from a single loader.  If we get the same resource
    * from the same loader twice, we should get the same resource.
    */
    public void testCache() throws Exception {
        Loader loader = new Loader(false);
        ResourceBundle b1 = getResourceBundle(loader, "Bug4179766Resource");
        if (b1 == null) {
            errln("Resource not found: Bug4179766Resource");
        }
        ResourceBundle b2 = getResourceBundle(loader, "Bug4179766Resource");
        if (b2 == null) {
            errln("Resource not found: Bug4179766Resource");
        }
        printIDInfo("[bundle1]",b1);
        printIDInfo("[bundle2]",b2);
        if (b1 != b2) {
            errln("Different objects returned by same ClassLoader");
        }
    }

    /**
    * Test that loaders with the same hash key still
    * cache resources seperately
    */
    public void testSameHash() throws Exception {
        doTest(true);
    }

    /**
    * Test that loaders with different hash keys
    * cache resources seperately
    */
    public void testDifferentHash() throws Exception {
        doTest(false);
    }

    /**
     * Ensure that cached resources for different ClassLoaders
     * are cached seperately
     */
    private void doTest(boolean sameHash) throws Exception {
        ResourceBundle b1 = getResourceBundle(new Loader(sameHash), "Bug4179766Resource");
        if (b1 == null) {
           errln("Resource not found: Bug4179766Resource");
        }
        ResourceBundle b2 = getResourceBundle(new Loader(sameHash), "Bug4179766Resource");
        if (b2 == null) {
           errln("Resource not found: Bug4179766Resource");
        }
        printIDInfo("[bundle1]",b1);
        printIDInfo("[bundle2]",b2);
        if (b1 == b2) {
           errln("Same object returned by different ClassLoaders");
        }
    }

    /**
     * Get a resource using a specified class loader to load the resource
     */
    private ResourceBundle getResourceBundle(Loader loader, String name) throws Exception {
        try {
            Class c = loader.loadClass("Bug4179766Class");
            Bug4179766Getter test = (Bug4179766Getter)c.newInstance();
            return test.getResourceBundle(name);
        } catch (ClassNotFoundException e) {
            errln("Class not found by custom class loader: "+name);
            throw e;
        } catch (InstantiationException e) {
            errln("Error instantiating: "+name);
            throw e;
        } catch (IllegalAccessException e) {
            errln("IllegalAccessException instantiating: "+name);
            throw e;
        }
    }

    /**
     * Print information about an object
     * [message][object's identity][object's class][object's loader][loaders hash][loaders identity]
     */
    private void printIDInfo(String message, Object o) {
        if (o == null) {
            return;
        }
        Class c = o.getClass();
        ClassLoader l = c.getClassLoader();
        int hash = -1;
        if (l != null) {
            hash = l.hashCode();
        }
        logln(message + System.identityHashCode(o) + "  Class: " + c
                + "  ClassLoader: " + l + "  loaderHash: " + hash
                + "  loaderPrimHash: " + System.identityHashCode(l));
    }

    /**
     * A simple class loader that loads classes from the current
     * working directory.  The hash code of the loader can be
     * set to be either the loaders identity or 0, allowing several
     * loaders to have the same hashCode value.
     */
    public class Loader extends ClassLoader {
        private int thisHashCode;

        /**
         * Create a new loader
         */
        public Loader(boolean sameHash) {
            super(Loader.class.getClassLoader());
            if (sameHash) {
                thisHashCode = SAME_HASH_CODE;
            } else {
                thisHashCode = getNextHashCode();
            }
        }

        /**
         * Return the hash code for this loader.
         */
        public int hashCode() {
            return thisHashCode;
        }

        /**
         * Get the data from the class file for the specified class.  If
         * the file can't be found, or the class is not one of the
         * special ones listed below, return null.
         *    Bug4179766Class
         *    Bug4179766Resource
         */
        private byte[] getClassData(String className) {
            boolean shouldLoad = className.equals("Bug4179766Class");
            shouldLoad = shouldLoad || className.equals("Bug4179766Resource");

            if (shouldLoad) {
                try {
                    File file = new File(System.getProperty("test.classes", "."), className+CLASS_SUFFIX);
                    FileInputStream fi = new FileInputStream(file);
                    byte[] result = new byte[fi.available()];
                    fi.read(result);
                    return result;
                } catch (Exception e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        /**
         * Load a class.  Files we can load take preference over ones the system
         * can load.
         */
        public synchronized Class loadClass(String className, boolean resolveIt)
                throws ClassNotFoundException {

            Class result = findLoadedClass(className);
            if (result != null) {
                printInfo("        ***Returning cached class: "+className, result);
                return result;
            }

            byte[] classData = getClassData(className);
            if (classData == null) {
                //we don't have a local copy of this one
                return loadFromSystem(className);
            }

            result = defineClass(classData, 0, classData.length);
            if (result == null) {
                //there was an error defining the class
                return loadFromSystem(className);
            }

            if (resolveIt) {
                resolveClass(result);
            }

            printInfo("        ***Loaded local class: "+className, result);
            return result;
        }

        /**
         * Delegate loading to the system loader
         */
        private Class loadFromSystem(String className) throws ClassNotFoundException {
            try {
                Class result = getParent().loadClass(className);
                printInfo("        ***Returning system class: "+className, result);
                return result;
            } catch (ClassNotFoundException e) {
                printInfo("        ***Class not found: "+className, null);
                throw e;
            }
        }

        /**
         * Print information about a class that was loaded
         * [loader identity][message][class identity]
         */
        private void printInfo(String message, Class c) {
            if (c != null) {
                logln(""+System.identityHashCode(this)+"  "+message+"  "+System.identityHashCode(c));
            } else {
                logln(""+System.identityHashCode(this)+"  "+message);
            }
        }
    }
}
