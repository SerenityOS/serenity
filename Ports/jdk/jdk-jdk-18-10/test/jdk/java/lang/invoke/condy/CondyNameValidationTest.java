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
 * @summary Test invalid name in name and type
 * @library /lib/testlibrary/bytecode /java/lang/invoke/common
 * @build jdk.experimental.bytecode.BasicClassBuilder test.java.lang.invoke.lib.InstructionHelper
 * @run testng CondyNameValidationTest
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyNameValidationTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

public class CondyNameValidationTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();
    static final MethodType BSM_TYPE = methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class);

    @DataProvider
    public Object[][] invalidNamesProvider() throws Exception {
        return Stream.of("",
                         ".",
                         ";",
                         "[",
                         "/")
                .map(e -> new Object[]{e}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "invalidNamesProvider", expectedExceptions = java.lang.ClassFormatError.class)
    public void testInvalidNames(String name) throws Exception {
        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, name, Object.class,
                "bsm", BSM_TYPE,
                S -> {
                });
    }

    @DataProvider
    public Object[][] validNamesProvider() throws Exception {
        return Stream.of("<clinit>",
                         "<init>",
                         "<foo>")
                .map(e -> new Object[]{e}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "validNamesProvider")
    public void testValidNames(String name) throws Exception {
        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, name, Object.class,
                "bsm", BSM_TYPE,
                S -> {
                });
    }
}
