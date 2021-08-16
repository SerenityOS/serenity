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
 * @bug 7082949
 * @summary JSR 292: missing ResourceMark in methodOopDesc::make_invoke_method
 *
 * @run main compiler.jsr292.Test7082949
 */

package compiler.jsr292;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

public class Test7082949 implements Runnable {
    public static void main(String... args) throws Throwable {
        new Thread(new Test7082949()).start();
    }

    public static Test7082949 test() {
        return null;
    }

    public void run() {
        try {
            MethodHandle m1 = MethodHandles.lookup().findStatic(Test7082949.class, "test",  methodType(Test7082949.class));
            Test7082949 v = (Test7082949)m1.invokeExact();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
