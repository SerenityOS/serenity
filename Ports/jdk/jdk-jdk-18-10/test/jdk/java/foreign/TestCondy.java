/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng TestCondy
 */

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryLayout;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static jdk.incubator.foreign.CLinker.*;
import static org.testng.Assert.assertEquals;

public class TestCondy {

    @Test(dataProvider = "constables")
    public void testPublicResolve(Constable constable) throws ReflectiveOperationException {
        ConstantDesc desc = constable.describeConstable().orElseThrow();
        Object result = desc.resolveConstantDesc(MethodHandles.publicLookup());
        assertEquals(result, constable);
    }


    private static final MemoryLayout[] constants = {
        C_CHAR,
        C_SHORT,
        C_INT,
        C_LONG,
        C_LONG_LONG,
        C_FLOAT,
        C_DOUBLE,
        C_POINTER
    };

    @DataProvider
    public static Object[][] constables() {
        List<Constable> testValues = new ArrayList<>();

        testValues.addAll(Arrays.asList(constants));

        testValues.add(MemoryLayout.structLayout(constants));
        testValues.add(MemoryLayout.unionLayout(constants));

        for (MemoryLayout ml : constants) {
            testValues.add(MemoryLayout.sequenceLayout(ml));
            testValues.add(MemoryLayout.sequenceLayout(10, ml));
        }

        testValues.add(FunctionDescriptor.ofVoid(constants));
        testValues.add(FunctionDescriptor.of(C_CHAR, constants));

        return testValues.stream().map(e -> new Object[] { e }).toArray(Object[][]::new);
    }
}
