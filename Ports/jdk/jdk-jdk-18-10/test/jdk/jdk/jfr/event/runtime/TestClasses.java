/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Array;
import static java.lang.invoke.MethodHandles.Lookup.ClassOption.*;

public class TestClasses {

    protected TestClassPrivate testClassPrivate;
    protected TestClassPrivateStatic testClassPrivateStatic;

    public TestClasses() {
        testClassPrivate = new TestClassPrivate();
        testClassPrivateStatic = new TestClassPrivateStatic();
    }

    // Classes TestClassPrivate and TestClassPrivateStatic should be loaded at
    // the same time
    // as the base class TestClasses
    private class TestClassPrivate {
    }

    private static class TestClassPrivateStatic {
    }

    protected class TestClassProtected {
    }

    protected static class TestClassProtectedStatic {
    }

    // When loadClasses() is run, 3 new classes should be loaded.
    public void loadClasses() throws ClassNotFoundException {
        final ClassLoader cl = getClass().getClassLoader();
        cl.loadClass("jdk.jfr.event.runtime.TestClasses$TestClassProtected1");
        cl.loadClass("jdk.jfr.event.runtime.TestClasses$TestClassProtectedStatic1");
    }

    protected class TestClassProtected1 {
    }

    protected static class TestClassProtectedStatic1 {
        protected TestClassProtectedStaticInner testClassProtectedStaticInner = new TestClassProtectedStaticInner();

        protected static class TestClassProtectedStaticInner {
        }
    }

    public static class TestClassPublicStatic {
        public static class TestClassPublicStaticInner {
        }
    }

}

class TestClass {
    static {
        // force creation of hidden class (for the lambda form)
        Runnable r = () -> System.out.println("Hello");
        r.run();
    }

    public static void createNonFindableClasses(byte[] klassbuf) throws Throwable {
        // Create a hidden class and an array of hidden classes.
        Lookup lookup = MethodHandles.lookup();
        Class<?> clh = lookup.defineHiddenClass(klassbuf, false, NESTMATE).lookupClass();
        Class<?> arrayOfHidden = Array.newInstance(clh, 10).getClass(); // HAS ISSUES?
    }
}
