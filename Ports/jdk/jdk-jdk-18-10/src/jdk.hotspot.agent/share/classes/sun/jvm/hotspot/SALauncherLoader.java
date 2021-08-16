/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

package sun.jvm.hotspot;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

/**
 * SA uses native debugger back-end library - libsaproc.so on Unix platforms.
 * Starting from 5.0, in Solaris & Linux JDK "libsaproc.so" is shipped with JDK
 * and is kept jre/lib/cpu directory (where all other JDK platform libraries
 * are kept). This implies that always that jre copy of libsaproc.so will be
 * used and the copy of libsaproc.so built from SA sources here will not
 * be used at all. We can override libsaproc.so using this class loader
 * as System class loader using "java.system.class.loader" property. This
 * class loader loads classes paths specified paths using the System property
 * "java.class.path". Because, this class loader loads SA debugger classes
 * (among other classes), JVM calls findLibrary override here. In this
 * findLibrary, we first check the library in the directories specified through
 * "sa.library.path" System property. This way updated/latest SA native library
 * can be loaded instead of the one from JDK's jre/lib directory.
 */
public class SALauncherLoader extends URLClassLoader {

    /**
     * Checks native libraries under directories specified using
     * the System property "sa.library.path".
     */
    public String findLibrary(String name) {
        name = System.mapLibraryName(name);
        for (int i = 0; i < libpaths.length; i++) {
            File file = new File(new File(libpaths[i]), name);
            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }
        return null;
    }

    public SALauncherLoader(ClassLoader parent) {
        super(getClassPath(), parent);
        String salibpath = System.getProperty("sa.library.path");
        if (salibpath != null) {
            libpaths = salibpath.split(File.pathSeparator);
        } else {
            libpaths = new String[0];
        }
    }

    /**
     * Override loadClass so we can checkPackageAccess.
     */
    public synchronized Class loadClass(String name, boolean resolve)
            throws ClassNotFoundException {
        int i = name.lastIndexOf('.');
        if (i != -1) {
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPackageAccess(name.substring(0, i));
            }
        }

        Class clazz = findLoadedClass(name);
        if (clazz != null) return clazz;

        /*
         * NOTE: Unlike 'usual' class loaders, we do *not* delegate to
         * the parent loader first. We attempt to load the class
         * ourselves first and use parent loader only if we can't load.
         * This is because the parent of this loader is 'default'
         * System loader that can 'see' all SA classes in classpath and
         * so will load those if delegated. And if parent loads SA classes,
         * then JVM won't call findNative override in this class.
         */
        try {
            return findClass(name);
        } catch (ClassNotFoundException cnfe) {
            return (super.loadClass(name, resolve));
        }
    }

    /**
     * allow any classes loaded from classpath to exit the VM.
     */
    protected PermissionCollection getPermissions(CodeSource codesource) {
        PermissionCollection perms = super.getPermissions(codesource);
        perms.add(new RuntimePermission("exitVM"));
        return perms;
    }

    //-- Internals only below this point

    private String[] libpaths;

    private static URL[] getClassPath() {
        final String s = System.getProperty("java.class.path");
        final File[] path = (s == null) ? new File[0] : getClassPath(s);

        return pathToURLs(path);
    }

    private static URL[] pathToURLs(File[] path) {
        URL[] urls = new URL[path.length];
        for (int i = 0; i < path.length; i++) {
            urls[i] = getFileURL(path[i]);
        }
        return urls;
    }

    private static File[] getClassPath(String cp) {
        String[] tmp = cp.split(File.pathSeparator);
        File[] paths = new File[tmp.length];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = new File(tmp[i].equals("")? "." : tmp[i]);
        }
        return paths;
    }

    private static URL getFileURL(File file) {
        try {
            file = file.getCanonicalFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return file.toURI().toURL();
        } catch (MalformedURLException mue) {
            throw new InternalError(mue.getMessage());
        }
    }
}
