/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4428205
 * @summary ACC_STRICT is set for methods of strictfp interface
 * @author gafter
 *
 * @compile StrictAbstract.java
 * @run main StrictAbstract
 */

import java.lang.reflect.*;

public class StrictAbstract {

    static strictfp interface I {
        void f();
    }

    static abstract strictfp class C {
        void f() {}
        abstract void g();
    }

    static Class[] ca = new Class[0];

    public static void main(String[] args) throws Exception {
        check(I.class.getDeclaredMethod("f", ca));
        check(C.class.getDeclaredMethod("f", ca));
        check(C.class.getDeclaredMethod("g", ca));
    }
    static void check(Method m) throws Exception {
        int mask=Modifier.ABSTRACT | Modifier.STRICT;
        if ((m.getModifiers() & mask) == mask) throw new Error();
    }

}
