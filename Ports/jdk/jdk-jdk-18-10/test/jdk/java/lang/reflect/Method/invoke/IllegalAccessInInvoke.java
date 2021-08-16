/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4109289
   @summary Turning off access checks now enables illegal reflection.
   @author Anand Palaniswamy
*/

import java.lang.reflect.Method;

/**
 * Try to call a private method with Method.invoke(). If that doesn't
 * throw a IllegalAccessException, then access checks are disabled,
 * which is a bad idea.
 */
public class IllegalAccessInInvoke {
    public static void main(String[] argv) {
        Class[] argTypes = new Class[0];
        Object[] args = new Object[0];
        Method pm = null;

        try {
            pm = Foo.class.getDeclaredMethod("privateMethod", argTypes);
        } catch (NoSuchMethodException nsme) {
            throw new
                RuntimeException("Bizzare: privateMethod *must* be there");
        }

        boolean ethrown = false;
        try {
            pm.invoke(new Foo(), args);
        } catch (IllegalAccessException iae) {
            ethrown = true;
        } catch (Exception e) {
            throw new RuntimeException("Unexpected " + e.toString());
        }

        if (!ethrown) {
            throw new
                RuntimeException("Reflection access checks are disabled");
        }
    }
}

class Foo {
    private void privateMethod() {
    }
}
