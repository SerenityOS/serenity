/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8186046
 * @summary Test bootstrap methods throwing an exception
 * @library /lib/testlibrary/bytecode  /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng CondyBSMException
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyBSMException
 */

import org.testng.Assert;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;

import static java.lang.invoke.MethodType.methodType;

public class CondyBSMException {

    @Test
    public void testThrowable() {
        test("Throwable", BootstrapMethodError.class, Throwable.class);
    }

    @Test
    public void testError() {
        test("Error", Error.class);
    }

    @Test
    public void testBootstrapMethodError() {
        test("BootstrapMethodError", BootstrapMethodError.class);
    }

    @Test
    public void testRuntimeException() {
        test("RuntimeException", BootstrapMethodError.class, RuntimeException.class);
    }

    @Test
    public void testException() throws Throwable {
        test("Exception", BootstrapMethodError.class, Exception.class);
    }

    static void test(String message, Class<? extends Throwable>... ts) {
        MethodHandle mh = thrower(message, ts[ts.length - 1]);
        Throwable caught = null;
        try {
            mh.invoke();
        }
        catch (Throwable t) {
            caught = t;
        }

        if (caught == null) {
            Assert.fail("Throwable expected");
        }

        String actualMessage = null;
        for (int i = 0; i < ts.length; i++) {
            actualMessage = caught.getMessage();
            Assert.assertNotNull(caught);
            Assert.assertTrue(ts[i].isAssignableFrom(caught.getClass()));
            caught = caught.getCause();
        }

        Assert.assertEquals(actualMessage, message);
    }

    static Throwable throwingBsm(MethodHandles.Lookup l, String name, Class<Throwable> type) throws Throwable {
        Throwable t;
        try {
            Constructor<Throwable> c = type.getDeclaredConstructor(String.class);
            t = c.newInstance(name);
        }
        catch (Exception e) {
            throw new InternalError();
        }
        throw t;
    }

    static MethodHandle thrower(String message, Class<? extends Throwable> t) {
        try {
            return InstructionHelper.ldcDynamicConstant(
                    MethodHandles.lookup(),
                    message, t,
                    "throwingBsm", methodType(Throwable.class, MethodHandles.Lookup.class, String.class, Class.class),
                    S -> { });
        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
