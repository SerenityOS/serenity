/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.jmx.mbeanserver;


import static com.sun.jmx.defaults.JmxProperties.MBEANSERVER_LOGGER;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.lang.System.Logger.Level;
import javax.management.MBeanPermission;

import javax.management.ObjectName;
import javax.management.loading.PrivateClassLoader;
import sun.reflect.misc.ReflectUtil;

/**
 * This class keeps the list of Class Loaders registered in the MBean Server.
 * It provides the necessary methods to load classes using the
 * registered Class Loaders.
 *
 * @since 1.5
 */
final class ClassLoaderRepositorySupport
    implements ModifiableClassLoaderRepository {

    /* We associate an optional ObjectName with each entry so that
       we can remove the correct entry when unregistering an MBean
       that is a ClassLoader.  The same object could be registered
       under two different names (even though this is not recommended)
       so if we did not do this we could disturb the defined
       semantics for the order of ClassLoaders in the repository.  */
    private static class LoaderEntry {
        ObjectName name; // can be null
        ClassLoader loader;

        LoaderEntry(ObjectName name,  ClassLoader loader) {
            this.name = name;
            this.loader = loader;
        }
    }

    private static final LoaderEntry[] EMPTY_LOADER_ARRAY = new LoaderEntry[0];

    /**
     * List of class loaders
     * Only read-only actions should be performed on this object.
     *
     * We do O(n) operations on this array, e.g. when removing
     * a ClassLoader.  The assumption is that the number of elements
     * is small, probably less than ten, and that the vast majority
     * of operations are searches (loadClass) which are by definition
     * linear.
     */
    private LoaderEntry[] loaders = EMPTY_LOADER_ARRAY;

    /**
     * Same behavior as add(Object o) in {@link java.util.List}.
     * Replace the loader list with a new one in which the new
     * loader has been added.
     **/
    private synchronized boolean add(ObjectName name, ClassLoader cl) {
        List<LoaderEntry> l =
            new ArrayList<LoaderEntry>(Arrays.asList(loaders));
        l.add(new LoaderEntry(name, cl));
        loaders = l.toArray(EMPTY_LOADER_ARRAY);
        return true;
    }

    /**
     * Same behavior as remove(Object o) in {@link java.util.List}.
     * Replace the loader list with a new one in which the old loader
     * has been removed.
     *
     * The ObjectName may be null, in which case the entry to
     * be removed must also have a null ObjectName and the ClassLoader
     * values must match.  If the ObjectName is not null, then
     * the first entry with a matching ObjectName is removed,
     * regardless of whether ClassLoader values match.  (In fact,
     * the ClassLoader parameter will usually be null in this case.)
     **/
    private synchronized boolean remove(ObjectName name, ClassLoader cl) {
        final int size = loaders.length;
        for (int i = 0; i < size; i++) {
            LoaderEntry entry = loaders[i];
            boolean match =
                (name == null) ?
                cl == entry.loader :
                name.equals(entry.name);
            if (match) {
                LoaderEntry[] newloaders = new LoaderEntry[size - 1];
                System.arraycopy(loaders, 0, newloaders, 0, i);
                System.arraycopy(loaders, i + 1, newloaders, i,
                                 size - 1 - i);
                loaders = newloaders;
                return true;
            }
        }
        return false;
    }


    /**
     * List of valid search
     */
    private final Map<String,List<ClassLoader>> search =
        new Hashtable<String,List<ClassLoader>>(10);

    /**
     * List of named class loaders.
     */
    private final Map<ObjectName,ClassLoader> loadersWithNames =
        new Hashtable<ObjectName,ClassLoader>(10);

    // from javax.management.loading.DefaultLoaderRepository
    public final Class<?> loadClass(String className)
        throws ClassNotFoundException {
        return  loadClass(loaders, className, null, null);
    }


    // from javax.management.loading.DefaultLoaderRepository
    public final Class<?> loadClassWithout(ClassLoader without, String className)
            throws ClassNotFoundException {
        if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
            MBEANSERVER_LOGGER.log(Level.TRACE,
                    className + " without " + without);
        }

        // without is null => just behave as loadClass
        //
        if (without == null)
            return loadClass(loaders, className, null, null);

        // We must try to load the class without the given loader.
        //
        startValidSearch(without, className);
        try {
            return loadClass(loaders, className, without, null);
        } finally {
            stopValidSearch(without, className);
        }
    }


    public final Class<?> loadClassBefore(ClassLoader stop, String className)
            throws ClassNotFoundException {
        if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
            MBEANSERVER_LOGGER.log(Level.TRACE,
                    className + " before " + stop);
        }

        if (stop == null)
            return loadClass(loaders, className, null, null);

        startValidSearch(stop, className);
        try {
            return loadClass(loaders, className, null, stop);
        } finally {
            stopValidSearch(stop, className);
        }
    }


    private Class<?> loadClass(final LoaderEntry list[],
                               final String className,
                               final ClassLoader without,
                               final ClassLoader stop)
            throws ClassNotFoundException {
        ReflectUtil.checkPackageAccess(className);
        final int size = list.length;
        for(int i=0; i<size; i++) {
            try {
                final ClassLoader cl = list[i].loader;
                if (cl == null) // bootstrap class loader
                    return Class.forName(className, false, null);
                if (cl == without)
                    continue;
                if (cl == stop)
                    break;
                if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
                    MBEANSERVER_LOGGER.log(Level.TRACE, "Trying loader = " + cl);
                }
                /* We used to have a special case for "instanceof
                   MLet" here, where we invoked the method
                   loadClass(className, null) to prevent infinite
                   recursion.  But the rule whereby the MLet only
                   consults loaders that precede it in the CLR (via
                   loadClassBefore) means that the recursion can't
                   happen, and the test here caused some legitimate
                   classloading to fail.  For example, if you have
                   dependencies C->D->E with loaders {E D C} in the
                   CLR in that order, you would expect to be able to
                   load C.  The problem is that while resolving D, CLR
                   delegation is disabled, so it can't find E.  */
                return Class.forName(className, false, cl);
            } catch (ClassNotFoundException e) {
                // OK: continue with next class
            }
        }

        throw new ClassNotFoundException(className);
    }

    private synchronized void startValidSearch(ClassLoader aloader,
                                               String className)
        throws ClassNotFoundException {
        // Check if we have such a current search
        //
        List<ClassLoader> excluded = search.get(className);
        if ((excluded!= null) && (excluded.contains(aloader))) {
            if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
                MBEANSERVER_LOGGER.log(Level.TRACE,
                        "Already requested loader = " +
                        aloader + " class = " + className);
            }
            throw new ClassNotFoundException(className);
        }

        // Add an entry
        //
        if (excluded == null) {
            excluded = new ArrayList<ClassLoader>(1);
            search.put(className, excluded);
        }
        excluded.add(aloader);
        if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
            MBEANSERVER_LOGGER.log(Level.TRACE,
                    "loader = " + aloader + " class = " + className);
        }
    }

    private synchronized void stopValidSearch(ClassLoader aloader,
                                              String className) {

        // Retrieve the search.
        //
        List<ClassLoader> excluded = search.get(className);
        if (excluded != null) {
            excluded.remove(aloader);
            if (MBEANSERVER_LOGGER.isLoggable(Level.TRACE)) {
                MBEANSERVER_LOGGER.log(Level.TRACE,
                        "loader = " + aloader + " class = " + className);
            }
        }
    }

    public final void addClassLoader(ClassLoader loader) {
        add(null, loader);
    }

    public final void removeClassLoader(ClassLoader loader) {
        remove(null, loader);
    }

    public final synchronized void addClassLoader(ObjectName name,
                                                  ClassLoader loader) {
        loadersWithNames.put(name, loader);
        if (!(loader instanceof PrivateClassLoader))
            add(name, loader);
    }

    public final synchronized void removeClassLoader(ObjectName name) {
        ClassLoader loader = loadersWithNames.remove(name);
        if (!(loader instanceof PrivateClassLoader))
            remove(name, loader);
    }

    public final ClassLoader getClassLoader(ObjectName name) {
        ClassLoader instance = loadersWithNames.get(name);
        if (instance != null) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                Permission perm =
                        new MBeanPermission(instance.getClass().getName(),
                        null,
                        name,
                        "getClassLoader");
                sm.checkPermission(perm);
            }
        }
        return instance;
    }

}
