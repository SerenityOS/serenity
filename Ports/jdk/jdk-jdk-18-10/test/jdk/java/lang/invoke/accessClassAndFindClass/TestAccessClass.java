/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8150782 8207027 8266269
 * @compile TestAccessClass.java TestCls.java p/Foo.java q/Bar.java
 * @run testng/othervm -ea -esa test.java.lang.invoke.TestAccessClass
 */
package test.java.lang.invoke;

import java.lang.invoke.*;
import java.lang.reflect.Modifier;

import p.Foo;
import q.Bar;

import static java.lang.invoke.MethodHandles.*;

import static org.testng.AssertJUnit.*;

import org.testng.annotations.*;

public class TestAccessClass {

    private static boolean initializedClass1;

    private static class Class1 {
        static {
            initializedClass1 = true;
        }
    }

    @Test
    public void initializerNotRun() throws IllegalAccessException {
        lookup().accessClass(Class1.class);
        assertFalse(initializedClass1);
    }

    @Test
    public void returnsSameClassInSamePackage() throws IllegalAccessException {
        Class<?> aClass = lookup().accessClass(Class1.class);
        assertEquals(Class1.class, aClass);
    }

    @Test
    public void returnsSameArrayClassInSamePackage() throws IllegalAccessException {
        Class<?> aClass = lookup().accessClass(Class1[].class);
        assertEquals(Class1[].class, aClass);
    }

    @DataProvider
    Object[][] illegalAccessAccess() {
        return new Object[][] {
                {publicLookup(), Class1.class},
                {publicLookup(), TestCls.getPrivateSIC()}
        };
    }

    @Test(dataProvider = "illegalAccessAccess", expectedExceptions = {IllegalAccessException.class})
    public void illegalAccessExceptionTest(Lookup lookup, Class<?> klass) throws IllegalAccessException {
        lookup.accessClass(klass);
    }

    @Test
    public void okAccess() throws IllegalAccessException {
        lookup().accessClass(TestCls.getPrivateSIC());
    }

    /**
     * Verify that a protected Q is as accessible as a public Q during linkage
     * (see JLS 15.12.4.3).
     */
    @Test
    public void protectedInnerClass() throws Throwable {
        lookup().accessClass(Bar.T_CLS);
        lookup().accessClass(Bar.T_ARRAY_CLS);
        MethodHandle mh = lookup().findStatic(Bar.class, "meth", MethodType.methodType(void.class, Bar.T_ARRAY_CLS));
        mh.invoke(null);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void illegalArgument() throws IllegalAccessException {
        lookup().accessClass(null);
    }
}
