/*
 * Copyright (c) 2009, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;
import java.lang.reflect.*;

public class DelegatingLoader extends URLClassLoader {

    private DelegatingLoader delLoader;
    private String[] delClasses;

    static {
        boolean supportParallel = false;
        try {
            Class c = Class.forName("java.lang.ClassLoader");
            Method m = c.getDeclaredMethod("registerAsParallelCapable",
                    new Class[0]);
            m.setAccessible(true);
            Object result = (Boolean) m.invoke(null);
            if (result instanceof Boolean) {
                supportParallel = ((Boolean) result).booleanValue();
            } else {
                // Should never happen
                System.out.println("Error: ClassLoader.registerAsParallelCapable() did not return a boolean!");
                System.exit(1);
            }
        } catch (NoSuchMethodException nsme) {
            System.out.println("No ClassLoader.registerAsParallelCapable() API");
        } catch (NoSuchMethodError nsme2) {
            System.out.println("No ClassLoader.registerAsParallelCapable() API");
        } catch (Exception ex) {
            ex.printStackTrace();
            // Exit immediately to indicate an error
            System.exit(1);
        }
        System.out.println("Parallel ClassLoader registration: " +
                    supportParallel);
    }

    public DelegatingLoader(URL urls[]) {
        super(urls);
        System.out.println("DelegatingLoader using URL " + urls[0]);
    }

    public void setDelegate(String[] delClasses, DelegatingLoader delLoader) {
        this.delClasses = delClasses;
        this.delLoader = delLoader;
    }

    public Class loadClass(String className, boolean resolve)
            throws ClassNotFoundException {
        for (int i = 0; i < delClasses.length; i++) {
            if (delClasses[i].equals(className)) {
                DelegateTest.log("Delegating class loading for " + className);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    return null;
                }
                return delLoader.loadClass(className, resolve);
            }
        }

        DelegateTest.log("Loading local class " + className);
//        synchronized (getClassLoadingLock(className)) {
            return super.loadClass(className, resolve);
//        }
    }
}
