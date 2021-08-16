/*
 * Copyright (c) 1999, 2001, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.*;

/*
 * Debuggee which exercises various types method calls
 */

class MethodCalls {

    public static void main(String args[]) throws Exception {
        (new MethodCalls()).go();
    }

    static void staticCaller(MethodCalls mc) throws Exception {
        System.out.println("Called staticCaller");
        staticCallee();
        mc.instanceCallee();

        /*
         * Invocation by reflection. This also exercises native method calls
         * since Method.invoke is a native method.
         */
        Method m = MethodCalls.class.getDeclaredMethod("staticCallee", new Class[0]);
        m.invoke(mc, new Object[0]);
    }

    void instanceCaller() throws Exception {
        System.out.println("Called instanceCaller");
        staticCallee();
        instanceCallee();

        /*
         * Invocation by reflection. This also exercises native method calls
         * since Method.invoke is a native method.
         */
        Method m = getClass().getDeclaredMethod("instanceCallee", new Class[0]);
        m.invoke(this, new Object[0]);
    }

    static void staticCallee() {
        System.out.println("Called staticCallee");
    }

    void instanceCallee() {
        System.out.println("Called instanceCallee");
    }

    void go() throws Exception {
        instanceCaller();
        staticCaller(this);
    }
}
