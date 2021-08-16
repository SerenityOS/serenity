/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng VarHandleTestReflection
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Method;
import java.util.stream.Stream;

public class VarHandleTestReflection extends VarHandleBaseTest {
    String string;

    @DataProvider
    public static Object[][] accessModesProvider() {
        return Stream.of(VarHandle.AccessMode.values()).
                map(am -> new Object[]{am}).
                toArray(Object[][]::new);
    }

    static VarHandle handle() throws Exception {
        return MethodHandles.lookup().
                findVarHandle(VarHandleTestReflection.class, "string", String.class);
    }

    @Test(dataProvider = "accessModesProvider", expectedExceptions = IllegalArgumentException.class)
    public void methodInvocation(VarHandle.AccessMode accessMode) throws Exception {
        VarHandle v = handle();

        // Try a reflective invoke using a Method

        Method vhm = VarHandle.class.getMethod(accessMode.methodName(), Object[].class);
        vhm.invoke(v, new Object[]{});
    }

    @Test(dataProvider = "accessModesProvider", expectedExceptions = UnsupportedOperationException.class)
    public void methodHandleInvoke(VarHandle.AccessMode accessMode) throws Throwable {
        VarHandle v = handle();

        // Try a reflective invoke using a MethodHandle

        MethodHandle mh = MethodHandles.lookup().unreflect(
                VarHandle.class.getMethod(accessMode.methodName(), Object[].class));
        // Use invoke to avoid WrongMethodTypeException for
        // non-signature-polymorphic return types
        Object o = (Object) mh.invoke(v, new Object[]{});
    }

    @Test(dataProvider = "accessModesProvider", expectedExceptions = IllegalArgumentException.class)
    public void methodInvocationFromMethodInfo(VarHandle.AccessMode accessMode) throws Exception {
        VarHandle v = handle();

        // Try a reflective invoke using a Method obtained from cracking
        // a MethodHandle

        MethodHandle mh = MethodHandles.lookup().unreflect(
                VarHandle.class.getMethod(accessMode.methodName(), Object[].class));
        MethodHandleInfo info = MethodHandles.lookup().revealDirect(mh);
        Method im = info.reflectAs(Method.class, MethodHandles.lookup());
        im.invoke(v, new Object[]{});
    }

    @Test(dataProvider = "accessModesProvider", expectedExceptions = IllegalArgumentException.class)
    public void reflectAsFromVarHandleInvoker(VarHandle.AccessMode accessMode) throws Exception {
        VarHandle v = handle();

        MethodHandle mh = MethodHandles.varHandleInvoker(
                accessMode, v.accessModeType(accessMode));

        MethodHandleInfo info = MethodHandles.lookup().revealDirect(mh);

        info.reflectAs(Method.class, MethodHandles.lookup());
    }

    @Test(dataProvider = "accessModesProvider", expectedExceptions = IllegalArgumentException.class)
    public void reflectAsFromFindVirtual(VarHandle.AccessMode accessMode) throws Exception {
        VarHandle v = handle();

        MethodHandle mh = MethodHandles.publicLookup().findVirtual(
                VarHandle.class, accessMode.methodName(), v.accessModeType(accessMode));

        MethodHandleInfo info = MethodHandles.lookup().revealDirect(mh);

        info.reflectAs(Method.class, MethodHandles.lookup());
    }
}
