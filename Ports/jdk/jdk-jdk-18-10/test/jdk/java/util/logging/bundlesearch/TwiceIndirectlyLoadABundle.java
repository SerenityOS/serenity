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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;

/**
 * This class constructs a scenario where a bundle is accessible on the call
 * stack two levels up from the call to getLogger(), but not on the immediate
 * caller.  This tests that getLogger() isn't doing a stack crawl more than one
 * level up to find a bundle.
 *
 * @author Jim Gish
 */
public class TwiceIndirectlyLoadABundle {

    private static final String rbName = "StackSearchableResource";

    public boolean loadAndTest() throws Throwable {
        // Find out where we are running from so we can setup the URLClassLoader URLs
        // test.src and test.classes will be set if running in jtreg, but probably
        // not otherwise
        String testDir = System.getProperty("test.src", System.getProperty("user.dir"));
        String testClassesDir = System.getProperty("test.classes",
                                                   System.getProperty("user.dir"));
        URL[] urls = new URL[2];

        // Allow for both jtreg and standalone cases here
        // Unlike the 1-level test where we can get the bundle from the caller's
        // class loader, for this one we don't want to expose the resource directory
        // to the next class.  That way we're invoking the LoadItUp2Invoker class
        // from this class that does have access to the resources (two levels
        // up the call stack), but the Invoker itself won't have access to resource
        urls[0] = Paths.get(testDir,"resources").toUri().toURL();
        urls[1] = Paths.get(testClassesDir).toUri().toURL();

        // Make sure we can find it via the URLClassLoader
        URLClassLoader yetAnotherResourceCL = new URLClassLoader(urls, null);
        Class<?> loadItUp2InvokerClazz = Class.forName("LoadItUp2Invoker", true,
                                                       yetAnotherResourceCL);
        ClassLoader actual = loadItUp2InvokerClazz.getClassLoader();
        if (actual != yetAnotherResourceCL) {
            throw new Exception("LoadItUp2Invoker was loaded by an unexpected CL: "
                                 + actual);
        }
        Object loadItUp2Invoker = loadItUp2InvokerClazz.newInstance();

        Method setupMethod = loadItUp2InvokerClazz.getMethod("setup",
                urls.getClass(), String.class);
        try {
            // For the next class loader we create, we want to leave off
            // the resources.  That way loadItUp2Invoker will have access to
            // them, but the next class won't.
            URL[] noResourceUrl = new URL[1];
            noResourceUrl[0] = urls[1];  // from above -- just the test classes
            setupMethod.invoke(loadItUp2Invoker, noResourceUrl, rbName);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }

        Method testMethod = loadItUp2InvokerClazz.getMethod("test");
        try {
            return (Boolean) testMethod.invoke(loadItUp2Invoker);
        } catch (InvocationTargetException ex) {
            throw ex.getTargetException();
        }
    }
}
