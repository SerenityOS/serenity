/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.tests.java.lang.invoke;

import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static org.testng.Assert.assertEquals;

/**
 * MHProxiesTest -- regression test for MH library bug
 */
@Test
public class MHProxiesTest {
    public interface Sam { double m(int arg); }

    public static Byte m(int arg) { return (byte) arg; }

    public void testProxy() throws NoSuchMethodException, IllegalAccessException {
        MethodHandle m = MethodHandles.lookup().findStatic(MHProxiesTest.class, "m",
                                                           MethodType.methodType(Byte.class, int.class));
        Sam s = MethodHandleProxies.asInterfaceInstance(Sam.class, m);
        assertEquals(66d, s.m(66));
    }
}
