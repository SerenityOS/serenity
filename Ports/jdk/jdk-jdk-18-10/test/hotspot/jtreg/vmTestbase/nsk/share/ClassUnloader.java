/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 *  Warning! Using this component need VM option -XX:-UseGCOverheadLimit
 *
 */

package nsk.share;

import java.util.*;
import nsk.share.gc.gp.*;
import nsk.share.test.ExecutionController;
import nsk.share.test.Stresser;

/**
 * The <code>ClassUnloader</code> class allows to force VM to unload class(es)
 * using memory stressing technique.
 *
 * <p>The method <code>unloadClass()</code> is provided which eats memory
 * to enforce GC to cleanup the heap. So, if all references to a class
 * and its loader are canceled, this may result in unloading the class.
 *
 * <p>ClassUnloader mainly intends to unload a class which was loaded
 * with especial <code>ClassUnloader.loadClass()</code> method.
 * A class is considered unloaded if its class loader is finalized.
 * If there no finalization of class loader detected for some timeout,
 * class is considered still loaded and method returns <i>false</i>.
 *
 * <p>Such finalization control applies only to a class loaded by
 * ClassUnloader's <code>loadClass()</code> method. Otherwise, if there
 * was no such class loaded, <code>unloadClass()</code> doesn't wait
 * for a timeout and always returns <i>false</i>.
 *
 * <p>By default internal class loader of <code>CustomClassLoader</code> class
 * is used for loading classes. This class loader can load class from .class file
 * located in the specified directory.
 * Application may define its own class loader, which may load classes using
 * any other technique. Such class loader should be derived from base
 * <code>CustomClassLoader</code> class, and set by <code>setClassLoader()</code>
 * method.
 *
 * @see #setClassLoader(CustomClassLoader)
 * @see #loadClass(String)
 * @see #loadClass(String, String)
 * @see #unloadClass()
 */
public class ClassUnloader {

    /**
     * Class name of default class loader.
     */
    public static final String INTERNAL_CLASS_LOADER_NAME = "nsk.share.CustomClassLoader";

    /**
     * Whole amount of time in milliseconds to wait for class loader finalization.
     */
    private static final int WAIT_TIMEOUT = 15000;

    /**
     * Piece of time in milliseconds to wait in a loop for class loader finalization.
     */
    private static final int WAIT_DELTA = 1000;

    /**
     * Has class loader been finalized or not.
     */
    volatile boolean finalized = false;

    /**
     * Current class loader used for loading classes.
     */
    private CustomClassLoader customClassLoader = null;

    /**
     * List of classes loaded with current class loader.
     */
    private Vector<Class<?>> classObjects = new Vector<Class<?>>();

    /**
     * Class object of the first class been loaded with current class loader.
     * To get the rest loaded classes use <code>getLoadedClass(int)</code>.
     * The call <code>getLoadedClass()</code> is effectively equivalent to the call
     * <code>getLoadedClass(0)</code>
     *
     * @return class object of the first loaded class.
     *
     * @see #getLoadedClass(int)
     */
    public Class<?> getLoadedClass() {
        return classObjects.get(0);
    }

    /**
     * Returns class objects at the specified index in the list of classes loaded
     * with current class loader.
     *
     * @return class objects at the specified index.
     */
    public Class<?> getLoadedClass(int index) {
        return classObjects.get(index);
    }

    /**
     * Creates new instance of <code>CustomClassLoader</code> class as the current
     * class loader and clears the list of loaded classes.
     *
     * @return created instance of <code>CustomClassLoader</code> class.
     *
     * @see #getClassLoader()
     * @see #setClassLoader(CustomClassLoader)
     */
    public CustomClassLoader createClassLoader() {
        customClassLoader = new CustomClassLoader(this);
        classObjects.removeAllElements();

        return customClassLoader;
    }

    /**
     * Sets new current class loader and clears the list of loaded classes.
     *
     * @see #getClassLoader()
     * @see #createClassLoader()
     */
    public void setClassLoader(CustomClassLoader customClassLoader) {
        this.customClassLoader = customClassLoader;
        classObjects.removeAllElements();
        customClassLoader.setClassUnloader(this);
    }

    /**
     * Returns current class loader or <i>null</i> if not yet created or set.
     *
     * @return class loader object or null.
     *
     * @see #createClassLoader()
     * @see #setClassLoader(CustomClassLoader)
     */
    public CustomClassLoader getClassLoader() {
        return customClassLoader;
    }

    /**
     * Loads class for specified class name using current class loader.
     *
     * <p>Current class loader should be set and capable to load class using only
     * given class name. No other information such a location of .class files
     * is passed to class loader.
     *
     * @param className name of class to load
     *
     * @throws ClassNotFoundException if no bytecode found for specified class name
     * @throws Failure if current class loader is not specified;
     *                 or if class was actually loaded with different class loader
     *
     * @see #loadClass(String, String)
     */
    public void loadClass(String className) throws ClassNotFoundException {

        if (customClassLoader == null) {
            throw new Failure("No current class loader defined");
        }

        Class<?> cls = Class.forName(className, true, customClassLoader);

        // ensure that class was loaded by current class loader
        if (cls.getClassLoader() != customClassLoader) {
            throw new Failure("Class was loaded by unexpected class loader: " + cls.getClassLoader());
        }

        classObjects.add(cls);
    }

    /**
     * Loads class from .class file located into specified directory using
     * current class loader.
     *
     * <p>If there is no current class loader, then default class loader
     * is created using <code>createClassLoader()</code>. Parameter <i>classDir</i>
     * is passed to class loader using <code>CustomClassLoader.setClassPath()</code>
     * method before loading class.
     *
     * @param className name of class to load
     * @param classDir path to .class file location
     *
     * @throws ClassNotFoundException if no .class file found
     *          for specified class name
     * @throws Failure if class was actually loaded with different class loader
     *
     * @see #loadClass(String)
     * @see CustomClassLoader#setClassPath(String)
     */
    public void loadClass(String className, String classDir) throws ClassNotFoundException {

        if (customClassLoader == null) {
            createClassLoader();
        }

        customClassLoader.setClassPath(classDir);
        loadClass(className);
    }

    /**
     * Forces GC to unload previously loaded classes by cleaning all references
     * to class loader with its loaded classes and eating memory.
     *
     * @return  <i>true</i> if classes unloading has been detected
             or <i>false</i> otherwise
     *
     * @throws  Failure if exception other than OutOfMemoryError
     *           is thrown while eating memory
     *
     * @see #eatMemory()
     */
    public boolean unloadClass(ExecutionController stresser) {

        finalized = false;

        // free references to class and class loader to be able for collecting by GC
        long waitTimeout = (customClassLoader == null) ? 0 : WAIT_TIMEOUT;
        classObjects.removeAllElements();
        customClassLoader = null;

        // force class unloading by eating memory pool
        eatMemory(stresser);

        // give GC chance to run and wait for finalization
        long timeToFinish = System.currentTimeMillis() + waitTimeout;
        while (!finalized && System.currentTimeMillis() < timeToFinish) {
            if (!stresser.continueExecution()) {
                return false;
            }
            try {
                // suspend thread for a while
                Thread.sleep(WAIT_DELTA);
            } catch (InterruptedException e) {
                throw new Failure("Unexpected InterruptedException while class unloading: " + e);
            }
        }

        // force GC to unload marked class loader and its classes
        if (finalized) {
            Runtime.getRuntime().gc();
            return true;
        }

        // class loader has not been finalized
        return false;
    }

    public boolean unloadClass() {
        Stresser stresser = new Stresser() {

            @Override
            public boolean continueExecution() {
                return true;
            }

        };
        return unloadClass(stresser);
    }

    /**
     * Stresses memory by allocating arrays of bytes.
     *
     * Note that this method can throw Failure if any exception
     * is thrown while eating memory. To avoid OOM while allocating
     * exception we preallocate it before the lunch starts. It means
     * that exception stack trace does not correspond to the place
     * where exception is thrown, but points at start of the method.
     *
     * @throws  Failure if exception other than OutOfMemoryError
     *           is thrown while eating memory
     */
    public static void eatMemory(ExecutionController stresser) {
        GarbageUtils.eatMemory(stresser, 50, 1024, 2);

        /*
         * System.runFinalization() may potentially fail with OOM. This is why
         * System.runFinalization() is repeated several times.
         */
        for (int i = 0; i < 10; ++i) {
            try {
                if(!stresser.continueExecution()) {
                    return;
                }
                System.runFinalization();
                break;
            } catch (OutOfMemoryError e) {
            }
        }
    }

        /**
     * Stresses memory by allocating arrays of bytes.
     *
     * Note that this method can throw Failure if any exception
     * is thrown while eating memory. To avoid OOM while allocating
     * exception we preallocate it before the lunch starts. It means
     * that exception stack trace does not correspond to the place
     * where exception is thrown, but points at start of the method.
     *
     * @throws  Failure if exception other than OutOfMemoryError
     *           is thrown while eating memory
     */
    public static void eatMemory() {
        Stresser stresser = new Stresser() {

            @Override
            public boolean continueExecution() {
                return true;
            }

        };
        GarbageUtils.eatMemory(stresser, 50, 1024, 2);
        /*
         * System.runFinalization() may potentially fail with OOM. This is why
         * System.runFinalization() is repeated several times.
         */
        for (int i = 0; i < 10; ++i) {
            try {
                if(!stresser.continueExecution()) {
                    return;
                }
                System.runFinalization();
                break;
            } catch (OutOfMemoryError e) {
            }
        }
    }
}
