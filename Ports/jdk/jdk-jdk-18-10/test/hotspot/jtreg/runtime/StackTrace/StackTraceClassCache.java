/*
 * Copyright (c) 2019, Red Hat, Inc. All rights reserved.
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
 * @test
 * @bug 8216302
 * @summary Check that stack trace contains proper strings even with class caching
 * @modules java.base/java.lang:open
 * @compile StackTraceClassCache.java
 * @run main StackTraceClassCache
 */

import java.lang.reflect.*;

public class StackTraceClassCache  {
    public static void main(String... args) throws Exception {
        Outer.Inner o = new Outer().new Inner();
        Class cl = o.getClass();

        // Check out of the box class name
        try {
            o.work();
        } catch (Exception e) {
            checkException(e, 42);
        }

        // Clear and populate class caches via getName
        clearNameCache(cl);
        cl.getName();
        try {
            o.work();
        } catch (Exception e) {
            checkException(e, 51);
        }

        // Clear and populate class caches via stack trace
        clearNameCache(cl);
        try {
            o.work();
        } catch (Exception e) {
            checkException(e, 59);
        }
    }

    static void checkException(Exception e, int line) throws Exception {
        StackTraceElement[] fs = e.getStackTrace();

        if (fs.length < 2) {
            throw new IllegalStateException("Exception should have at least two frames", e);
        }

        assertCorrect("StackTraceClassCache$Outer$Inner.work(StackTraceClassCache.java:95)", fs[0].toString(), e);
        assertCorrect("StackTraceClassCache.main(StackTraceClassCache.java:" + line + ")",   fs[1].toString(), e);
    }

    static void assertCorrect(String expected, String actual, Exception e) throws Exception {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected: " + expected + "; Actual: " + actual, e);
        }
    }

    static void clearNameCache(Class cl) {
        try {
            Field f = Class.class.getDeclaredField("name");
            f.setAccessible(true);
            f.set(cl, null);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static class Outer {
       class Inner {
           void work() throws Exception {
               throw new Exception("Sample exception");
           }
       }
    }

}
