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

package nsk.monitoring.share;

import java.util.*;
import nsk.share.log.Log;
import nsk.share.ClassUnloader;
import nsk.share.CustomClassLoader;
import nsk.share.test.Stresser;

/**
 * The <code>ClassLoadingController</code> class allows to operate class
 * loading/unloading process.
 */
public class ClassLoadingController extends StateControllerBase {
        // Path and name of the classes to load
        private static final String CLASSNAME_PATTERN = "nsk.monitoring.share.newclass.LoadableClass";

        private int loadedClassCount = 100;
        private int loaderCount = 1;
        private boolean singleClass = true;
        private String classDir;
        private Hashtable<String, String[]> classesTable = new Hashtable<String, String[]>();
        private ClassUnloader[] unloaders;

        private Stresser stresser;

        /**
         * Constructs a new <code>ClassLoadingController</code> with defined
         * arguments.
         *
         * @param log <code>Log</code> to print log info to.
         * @param loadedClassCount number of classes to load.
         * @param loaderCount number of loaders to use.
         * @param singleClass if class loaders are instances of the same class.
         * @param classDir directory to load classes from.
         */
        public ClassLoadingController(
                Log log,
                int loadedClassCount,
                int loaderCount,
                boolean singleClass,
                String classDir,
                Stresser stresser
        ) {
                super(log);
                setLoadedClassCount(loadedClassCount);
                setLoaderCount(loaderCount);
                setClassDir(classDir);
                singleClassLoaderClass(singleClass);
                dump();
                preloadAllClasses();
                setStresser(stresser);
        }

    private void setStresser(Stresser stresser) {
        this.stresser = stresser;
    }

    public ClassLoadingController(Log log, ArgumentHandler argHandler, Stresser stresser) {
                this(
                        log,
                        argHandler.getLoadableClassesCount(),
//                        argHandler.getLoadersCount(),
                        (int)stresser.getMaxIterations(),
                        argHandler.singleClassloaderClass(),
                        argHandler.getRawArgument(0),
                        stresser
                );
        }

        public void dump() {
                log.debug("classes to be loaded:\t" + loadedClassCount);
                log.debug("classloader instances:\t" + loaderCount);
                if (singleClass)
                        log.debug("classloader class:\tsingle");
                else
                        log.debug("classloader class:\ttwo");
                log.debug("Class dir" + classDir);

        }

        private void setLoadedClassCount(int loadedClassCount) {
                this.loadedClassCount = loadedClassCount;
        }

        // Set loaderCount value
        private void setLoaderCount(int loaderCount) {
                this.loaderCount = loaderCount;
        }

        // Set singleClass value
        private void singleClassLoaderClass(boolean singleClass) {
                this.singleClass = singleClass;
        }

        // Set classDir value
        private void setClassDir(String classDir) {
                this.classDir = classDir;
        }

        // Load classes
        private void preloadAllClasses() {
                log.debug("preloading all classes...");
                if (singleClass)
                        createUnloaders(1);
                else
                        createUnloaders(2);

                for (int i = 0; i < unloaders.length; i++) {
                        loadClasses(unloaders[i], 1, false);
                        unloaders[i].unloadClass();
                }
        }

        // Load classes
        private boolean loadClasses(ClassUnloader unloader, int classCount, boolean doKeep) {
                String newClassName;
                String[] classNames = new String[classCount + 1];
                classNames[0] = unloader.getClassLoader().getClass().getName()
                        + "@"
                        + Integer.toHexString(
                                        unloader.getClassLoader().hashCode()
                                        );


                for (int i = 1; i <= classCount; i++) {
                        newClassName = CLASSNAME_PATTERN + int2Str(i);
                        classNames[i] = newClassName;
                        try {
                                unloader.loadClass(newClassName);
                        } catch (ClassNotFoundException e) {
                                log.error(e.toString());
                                e.printStackTrace();
                                return false;
                        }
                }
                if (doKeep)
                        classesTable.put(String.valueOf(unloader.hashCode()), classNames);
                return true;
        } // loadClasses()

        /**
         * Loads all classes.
         *
         * @see ClassLoadingController#ClassLoadingController
         */
        public int loadClasses() {
                CustomClassLoader loader;
                boolean res = true;
                String loaderName;

                createUnloaders(loaderCount);

                int count = 0;
                for (int i = 0; i < unloaders.length; i++) {
                        loaderName = unloaders[i].getClassLoader().getClass().getName()
                                + "@"
                                + Integer.toHexString(
                                                unloaders[i].getClassLoader().hashCode()
                                                );
                        if (loadClasses(unloaders[i], loadedClassCount, true)) {
                                String[] values = (String[])
                                        classesTable.get(String.valueOf(unloaders[i].hashCode()));
                                int length = values.length - 1;
                                log.debug(loaderName + "(" + i + ")>>> " + length
                                                + " classes have been loaded");
                                count += length;
                        }
                }
                log.info("Total: loading is performed " + count + " times");

                return count;
        }

        // Unload classes
        public int unloadClasses() {
                String loaderName;
                int count = 0;
                long timeLeft = 0;

                for (int i = 0; i < loaderCount && (timeLeft = stresser.getTimeLeft()/1000) > 0; i++) {
                        loaderName = unloaders[i].getClassLoader().getClass().getName()
                                + "@"
                                + Integer.toHexString(
                                                unloaders[i].getClassLoader().hashCode()
                                                );
                        String hashCode = String.valueOf(unloaders[i].hashCode());
                        String[] values = (String[]) classesTable.get(hashCode);

                        if (unloaders[i].unloadClass()) {
                                int length = values.length - 1;
                                count += length;
                                log.debug(loaderName + "(" + i + ")>>> " + length
                                                + " classes have been unloaded (time left: "+timeLeft+" s)");
                                classesTable.remove(hashCode);
                        } else {
                                log.debug(loaderName + "(" + i + ")>>> "
                                                + "classes couldn't be unloaded (time left: "+timeLeft+" s)");
                        }
                }

                log.info("Total: unloading is performed " + count + " times");

                return count;
        }

        private void createUnloaders(int count) {
                CustomClassLoader loader;
                unloaders = new ClassUnloader[count];

                for (int i = 0; i < count; i++) {
                        unloaders[i] = new ClassUnloader();
                        if (singleClass) {
                                loader = unloaders[i].createClassLoader();
                        } else {
                                if (i%2 == 0)
                                        loader = new ClassLoaderA();
                                else
                                        loader = new ClassLoaderB();
                                unloaders[i].setClassLoader(loader);
                        }
                        loader.setClassPath(classDir);
                } // for
        }

        /**
         * Brings out VM into defined state. The method loads all classes via
         * {@link ClassLoadingController#loadClasses}.
         *
         * @see ClassLoadingController#loadClasses
         */
        public void run() {
                loadClasses();
        }

        /**
         * Tries to reclaim VM into initial state. The method tries to load all
         * classes via {@link ClassLoadingController#unloadClasses}.
         *
         * @see ClassLoadingController#unloadClasses
         */
        public void reset() {
                unloadClasses();
        }

        // The class extends CustomClassLoader with specific implementation of
        // toString() method
        class ClassLoaderA extends CustomClassLoader {
                public ClassLoaderA() {
                        super();
                }

                public String toString() {
                        return "ClassLoaderA";
                }
        } // ClassLoaderA

        // The class extends CustomClassLoader with specific implementation of
        // toString() method
        class ClassLoaderB extends CustomClassLoader {
                public ClassLoaderB() {
                        super();
                }

                public String toString() {
                        return "ClassLoaderB";
                }
        } // ClassLoaderB
}
