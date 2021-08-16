/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8158802
 * @summary com.sun.jndi.ldap.SimpleClientId produces wrong hash code
 * @modules java.naming/com.sun.jndi.ldap:open
 */

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import javax.naming.ldap.Control;


public class SimpleClientIdHashCode {
    public static void main(String[] args) throws Throwable {
        Class<?> simpleClientIdClass
                = Class.forName("com.sun.jndi.ldap.SimpleClientId");
        Constructor<?> init = simpleClientIdClass.getDeclaredConstructor(
                int.class, String.class, int.class, String.class,
                Control[].class, OutputStream.class, String.class,
                String.class, Object.class);
        init.setAccessible(true);

        Object p1 = new byte[]{66,77};
        Object p2 = new char[]{'w','d'};
        Object p3 = "word";

        test(init, new byte[]{65}, new byte[]{65});
        test(init, new char[]{'p'}, new char[]{'p'});
        test(init, "pass", "pass");
        test(init, p1, p1);
        test(init, p2, p2);
        test(init, p3, p3);
        test(init, null, null);
    }

    private static void test(Constructor<?> init, Object pass1, Object pass2)
            throws Throwable {

        Object o1 = init.newInstance(1, "host", 3, "", null, System.out,
                null, null, pass1);
        Object o2 = init.newInstance(1, "host", 3, "", null, System.out,
                null, null, pass2);

        if (!o1.equals(o2))
            throw new RuntimeException("Objects not equal");

        if (o1.hashCode() != o2.hashCode())
            throw new RuntimeException("Inconsistent hash codes");
    }
}
