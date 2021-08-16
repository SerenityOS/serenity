/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8147078
 * @run testng/othervm -ea -esa Test8147078
 */

import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

import static java.lang.invoke.MethodType.methodType;

import static org.testng.AssertJUnit.*;

public class Test8147078 {

    static int target(int x) {
        throw new RuntimeException("ieps");
    }

    static int handler(String s, int x) {
        return 4*x;
    }

    static final MethodHandle MH_target;
    static final MethodHandle MH_handler;
    static final MethodHandle MH_catchException;

    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    static {
        try {
            Class<Test8147078> C = Test8147078.class;
            MH_target = LOOKUP.findStatic(C, "target", methodType(int.class, int.class));
            MH_handler = LOOKUP.findStatic(C, "handler", methodType(int.class, String.class, int.class));
            MH_catchException = LOOKUP.findStatic(MethodHandles.class, "catchException",
                    methodType(MethodHandle.class, MethodHandle.class, Class.class, MethodHandle.class));
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    public void testNoExceptionType() {
        boolean caught = false;
        try {
            MethodHandle eek = (MethodHandle) MH_catchException.invoke(MH_target, String.class, MH_handler);
        } catch (ClassCastException cce) {
            assertEquals("java.lang.String", cce.getMessage());
            caught = true;
        } catch (Throwable t) {
            fail("unexpected exception caught: " + t);
        }
        assertTrue(caught);
    }

}