/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.security.AllPermission;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A custom ClassLoader to load the concrete LoggerFinder class
 * with all permissions. The CustomSystemClassLoader class must be
 * in the BCL, otherwise when system classes - such as
 * ZoneDateTime try to load their resource bundle a MissingResourceBundle
 * caused by a SecurityException may be thrown, as the CustomSystemClassLoader
 * code base will be found in the stack called by doPrivileged.
 *
 * @author danielfuchs
 */
public class CustomSystemClassLoader extends ClassLoader {


    final List<String> finderClassNames =
            Arrays.asList("BaseLoggerFinder");
    final Map<String, Class<?>> finderClasses = new ConcurrentHashMap<>();
    Class<?> testLoggerFinderClass;

    public CustomSystemClassLoader() {
        super();
    }
    public CustomSystemClassLoader(ClassLoader parent) {
        super(parent);
    }

    private Class<?> defineFinderClass(String name)
        throws ClassNotFoundException {
        Class<?> finderClass =  finderClasses.get(name);
        if (finderClass != null) return finderClass;

        final Object obj = getClassLoadingLock(name);
        synchronized(obj) {
            finderClasses.get(name);
            if (finderClass != null) return finderClass;

            if (testLoggerFinderClass == null) {
                // Hack: we  load testLoggerFinderClass to get its code source.
                //       we can't use this.getClass() since we are in the boot.
                testLoggerFinderClass = super.loadClass("BaseDefaultLoggerFinderTest$TestLoggerFinder");
            }
            URL url = testLoggerFinderClass.getProtectionDomain().getCodeSource().getLocation();
            File file = new File(url.getPath(), name+".class");
            if (file.canRead()) {
                try {
                    byte[] b = Files.readAllBytes(file.toPath());
                    Permissions perms = new Permissions();
                    perms.add(new AllPermission());
                    finderClass = defineClass(
                            name, b, 0, b.length, new ProtectionDomain(
                            this.getClass().getProtectionDomain().getCodeSource(),
                            perms));
                    System.out.println("Loaded " + name);
                    finderClasses.put(name, finderClass);
                    return finderClass;
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    throw new ClassNotFoundException(name, ex);
                }
            } else {
                throw new ClassNotFoundException(name,
                        new IOException(file.toPath() + ": can't read"));
            }
        }
    }

    private static boolean matches(String prefix, String name) {
        return prefix.equals(name) || name.startsWith(prefix + "$");
    }

    @Override
    public synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (finderClassNames.stream().anyMatch(n -> matches(n, name))) {
            Class<?> c = defineFinderClass(name);
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (finderClassNames.stream().anyMatch(n -> matches(n, name))) {
            return defineFinderClass(name);
        }
        return super.findClass(name);
    }

}
