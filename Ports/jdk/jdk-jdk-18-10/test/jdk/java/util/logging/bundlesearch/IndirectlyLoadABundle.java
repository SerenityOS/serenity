/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.logging.Logger;

/**
 * This class is used to ensure that a resource bundle loadable by a classloader
 * is on the caller's stack, but not on the classpath or TCCL.  It tests that
 * Logger.getLogger() can load the bundle via the immediate caller's classloader
 *
 * @author Jim Gish
 */
public class IndirectlyLoadABundle {

    private static final String rbName = "CallerSearchableResource";

    public boolean loadAndTest() throws Throwable {
        // Make sure we can find it via the URLClassLoader
        URLClassLoader yetAnotherResourceCL = new URLClassLoader(getURLs(), null);
        if (!testForValidResourceSetup(yetAnotherResourceCL)) {
            throw new Exception("Couldn't directly load bundle " + rbName
                    + " as expected. Test config problem");
        }
        // But it shouldn't be available via the system classloader
        ClassLoader myCL = this.getClass().getClassLoader();
        if (testForValidResourceSetup(myCL)) {
            throw new Exception("Was able to directly load bundle " + rbName
                    + " from " + myCL + " but shouldn't have been"
                    + " able to. Test config problem");
        }

        Class<?> loadItUpClazz = Class.forName("LoadItUp1", true,
                                               yetAnotherResourceCL);
        ClassLoader actual = loadItUpClazz.getClassLoader();
        if (actual != yetAnotherResourceCL) {
            throw new Exception("LoadItUp1 was loaded by an unexpected CL: " + actual);
        }
        Object loadItUp = loadItUpClazz.newInstance();
        Method testMethod = loadItUpClazz.getMethod("getLogger", String.class, String.class);
        try {
            return (Logger)testMethod.invoke(loadItUp, "NestedLogger1", rbName) != null;
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }

    public boolean testGetAnonymousLogger() throws Throwable {
        // Test getAnonymousLogger()
        URLClassLoader loadItUpCL = new URLClassLoader(getURLs(), null);
        Class<?> loadItUpClazz = Class.forName("LoadItUp1", true, loadItUpCL);
        ClassLoader actual = loadItUpClazz.getClassLoader();
        if (actual != loadItUpCL) {
            throw new Exception("LoadItUp1 was loaded by an unexpected CL: "
                                 + actual);
        }
        Object loadItUpAnon = loadItUpClazz.newInstance();
        Method testAnonMethod = loadItUpClazz.getMethod("getAnonymousLogger",
                                                        String.class);
        try {
            return (Logger)testAnonMethod.invoke(loadItUpAnon, rbName) != null;
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }


    public boolean testGetLoggerGetLoggerWithBundle() throws Throwable {
        // test getLogger("NestedLogger2"); followed by
        // getLogger("NestedLogger2", rbName) to see if the bundle is found
        //
        URL[] urls = getURLs();
        if (getLoggerWithNewCL(urls, "NestedLogger2", null)) {
            return getLoggerWithNewCL(urls, "NestedLogger2", rbName);

        } else {
            throw new Exception("TEST FAILED: first call to getLogger() failed "
                                 + " in IndirectlyLoadABundle."
                                 + "testGetLoggerGetLoggerWithBundle");
        }
    }

    private URL[] getURLs() throws MalformedURLException {
        // Find out where we are running from so we can setup the URLClassLoader URLs
        // test.src and test.classes will be set if running in jtreg, but probably
        // not otherwise
        String testDir = System.getProperty("test.src", System.getProperty("user.dir"));
        String testClassesDir = System.getProperty("test.classes",
                                                   System.getProperty("user.dir"));
        URL[] urls = new URL[2];
        // Allow for both jtreg and standalone cases here
        urls[0] = Paths.get(testDir, "resources").toUri().toURL();
        urls[1] = Paths.get(testClassesDir).toUri().toURL();

        return urls;
    }

    private boolean getLoggerWithNewCL(URL[] urls, String loggerName,
                                         String bundleName) throws Throwable {
        Logger result = null;;
        // Test getLogger("foo"); getLogger("foo", "rbName");
        // First do the getLogger() call with no bundle name
        URLClassLoader getLoggerCL = new URLClassLoader(urls, null);
        Class<?> loadItUpClazz1 = Class.forName("LoadItUp1", true, getLoggerCL);
        ClassLoader actual = loadItUpClazz1.getClassLoader();
        if (actual != getLoggerCL) {
            throw new Exception("LoadItUp1 was loaded by an unexpected CL: "
                                 + actual);
        }
        Object loadItUp1 = loadItUpClazz1.newInstance();
        if (bundleName != null) {
            Method getLoggerMethod = loadItUpClazz1.getMethod("getLogger",
                                                              String.class,
                                                              String.class);
            try {
                result = (Logger) getLoggerMethod.invoke(loadItUp1, loggerName,
                                                         bundleName);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        } else {
            Method getLoggerMethod = loadItUpClazz1.getMethod("getLogger",
                                                              String.class);
            try {
                result = (Logger) getLoggerMethod.invoke(loadItUp1, loggerName);
            } catch (InvocationTargetException ex) {
                throw ex.getTargetException();
            }
        }
        return result != null;
    }

    private boolean testForValidResourceSetup(ClassLoader cl) {
        // First make sure the test environment is setup properly and the bundle
        // actually exists
        return ResourceBundleSearchTest.isOnClassPath(rbName, cl);
    }
}
