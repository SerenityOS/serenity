/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6990212
 * @summary JSR 292 JVMTI MethodEnter hook is not called for JSR 292 bootstrap and target methods
 *
 * @run main compiler.jsr292.cr6990212.Test6990212
 */

package compiler.jsr292.cr6990212;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

interface intf {
    public Object target();
}

public class Test6990212 implements intf {
    public Object target() {
        return null;
    }

    public static void main(String[] args) throws Throwable {
        // Build an interface invoke and then invoke it on something
        // that doesn't implement the interface to test the
        // raiseException path.
        MethodHandle target = MethodHandles.lookup().findVirtual(intf.class, "target",  MethodType.methodType(Object.class));
        try {
            target.invoke(new Object());
        } catch (ClassCastException cce) {
            // everything is ok
            System.out.println("got expected ClassCastException");
        }
    }
}
