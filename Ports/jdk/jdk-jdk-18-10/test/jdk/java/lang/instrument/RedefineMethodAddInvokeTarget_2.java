/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;

public class RedefineMethodAddInvokeTarget {
    public void test(int counter) throws Exception {
        Method method = getClass().getDeclaredMethod("myMethod" +
            (counter == 0 ? "" : counter));
        method.setAccessible(true);
        method.invoke(this);
    }

    public void myMethod() {
        System.out.println("Hello from the non-EMCP again myMethod()!");
    }

    private final void myMethod1() {
        System.out.println("Hello from myMethod1()!");
        System.out.println("Calling myMethod() from myMethod1():");
        myMethod();
    }

    private final void myMethod2() {
        System.out.println("Hello from myMethod2()!");
        System.out.println("Calling myMethod1() from myMethod2():");
        myMethod1();
    }
}
