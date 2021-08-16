/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/*
 * @test
 * @run testng TestRestricted
 */
public class TestRestricted {
    @Test(expectedExceptions = InvocationTargetException.class)
    public void testReflection() throws Throwable {
        Method method = MemorySegment.class.getDeclaredMethod("globalNativeSegment");
        method.invoke(null);
    }

    @Test(expectedExceptions = IllegalCallerException.class)
    public void testInvoke() throws Throwable {
        var mh = MethodHandles.lookup().findStatic(MemorySegment.class,
                "globalNativeSegment", MethodType.methodType(MemorySegment.class));
        var seg = (MemorySegment)mh.invokeExact();
    }

    @Test(expectedExceptions = IllegalCallerException.class)
    public void testDirectAccess() throws Throwable {
        MemorySegment.globalNativeSegment();
    }

    @Test(expectedExceptions = InvocationTargetException.class)
    public void testReflection2() throws Throwable {
        Method method = MemoryAddress.class.getDeclaredMethod("asSegment", long.class, ResourceScope.class);
        method.invoke(MemoryAddress.NULL, 4000L, ResourceScope.globalScope());
    }

    @Test(expectedExceptions = IllegalCallerException.class)
    public void testInvoke2() throws Throwable {
        var mh = MethodHandles.lookup().findVirtual(MemoryAddress.class, "asSegment",
            MethodType.methodType(MemorySegment.class, long.class, ResourceScope.class));
        var seg = (MemorySegment)mh.invokeExact(MemoryAddress.NULL, 4000L, ResourceScope.globalScope());
    }

    @Test(expectedExceptions = IllegalCallerException.class)
    public void testDirectAccess2() throws Throwable {
        MemoryAddress.NULL.asSegment(4000L, ResourceScope.globalScope());
    }
}
