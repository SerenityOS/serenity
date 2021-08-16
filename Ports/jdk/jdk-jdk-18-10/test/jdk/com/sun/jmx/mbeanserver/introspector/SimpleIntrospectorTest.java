
/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.lang.reflect.Method;

/*
 * @test
 * @bug 8129215
 * @summary The test checks whether the SimpleIntrospector is honoring the
 *          the JavaBeans property naming convention of always starting
 *          with a lower-case letter
 *
 * @author Jaroslav Bachorik
 * @modules java.management/com.sun.jmx.mbeanserver:open
 * @run clean SimpleIntrospectorTest
 * @run build SimpleIntrospectorTest BeanClass
 * @run main SimpleIntrospectorTest
 */
public class SimpleIntrospectorTest {
    private static Method INTROSPECT_GETTER;

    public static void main(String ... args) throws Exception {
        Class clz = Class.forName(
            "com.sun.jmx.mbeanserver.Introspector$SimpleIntrospector"
        );
        INTROSPECT_GETTER = clz.getDeclaredMethod(
            "getReadMethod",
            Class.class,
            String.class
        );
        INTROSPECT_GETTER.setAccessible(true);
        boolean result = true;
        result &= checkNumberValid();
        result &= checkNumberInvalid();
        result &= checkAvailableValid();
        result &= checkAvailableInvalid();

        if (!result) {
            throw new Error();
        }
    }

    private static boolean checkNumberValid() throws Exception {
        return checkGetter(false, "number");
    }

    private static boolean checkNumberInvalid() throws Exception {
        return checkGetter(true, "Number");
    }

    private static boolean checkAvailableValid() throws Exception {
        return checkGetter(false, "available");
    }

    private static boolean checkAvailableInvalid() throws Exception {
        return checkGetter(true, "Available");
    }

    private static boolean checkGetter(boolean nullExpected, String name)
    throws Exception {
        Method m = getReadMethod(BeanClass.class, name);
        boolean result = (m != null);
        if (nullExpected) result = !result;

        if (result) {
            return true;
        }
        if (nullExpected) {
            System.err.println("SimpleIntrospector resolved an unknown getter " +
                               "for attribute '"+ name +"'");
        } else {
            System.err.println("SimpleIntrospector fails to resolve getter " +
                               "for attribute '"+ name +"'");
        }
        return false;
    }

    private static Method getReadMethod(Class clz, String attr)
    throws Exception {
        return (Method)INTROSPECT_GETTER.invoke(null, clz, attr);
    }
}
