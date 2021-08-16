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
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:UseBootstrapCallInfo=3 CondyTypeValidationTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import test.java.lang.invoke.lib.InstructionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

public class CondyTypeValidationTest {
    static final MethodHandles.Lookup L = MethodHandles.lookup();
    static final String BSM_TYPE = methodType(Object.class, MethodHandles.Lookup.class, String.class, Object.class)
            .toMethodDescriptorString();

    @DataProvider
    public Object[][] invalidTypesProvider() throws Exception {
        return Stream.of(
//                         ByteCode API checks for the following invalid types
//                         "",
//                         "[",
//                         "A",
//                         "a",
                "L/java/lang/Object",
                Stream.generate(() -> "[").limit(256).collect(Collectors.joining("", "", "I")))
                .map(e -> new Object[]{e}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "invalidTypesProvider", expectedExceptions = ClassFormatError.class)
    public void testInvalidTypes(String type) throws Exception {
        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, "name", type,
                "bsm", BSM_TYPE,
                S -> {
                });
    }

    @DataProvider
    public Object[][] validTypesProvider() throws Exception {
        List<String> t = new ArrayList<>(List.of("B", "C", "D", "F", "I", "J", "Ljava/lang/Object;", "S", "Z"));
        int l = t.size();
        for (int i = 0; i < l; i++) {
            t.add("[" + t.get(i));
        }

        return t.stream()
                .map(e -> new Object[]{e}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "validTypesProvider")
    public void testValidTypes(String type) throws Exception {
        MethodHandle mh = InstructionHelper.ldcDynamicConstant(
                L, "name", type,
                "bsm", BSM_TYPE,
                S -> {
                });
    }
}
