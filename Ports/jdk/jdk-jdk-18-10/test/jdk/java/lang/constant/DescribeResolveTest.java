/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng DescribeResolveTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.constant.Constable;
import java.lang.constant.ConstantDesc;
import java.lang.invoke.MethodHandles;

import static org.testng.Assert.assertEquals;

public class DescribeResolveTest {

    @DataProvider
    public static Object[][] constables() {
        return new Object[][]{
            { true },
            { false },
            { (short) 10 },
            { (byte) 10 },
            { (char) 10 },
        };
    }

    @Test(dataProvider = "constables")
    public void testDescribeResolve(Constable constable) throws ReflectiveOperationException {
        ConstantDesc desc = constable.describeConstable().orElseThrow();
        Object resolved = desc.resolveConstantDesc(MethodHandles.lookup());
        assertEquals(constable, resolved);
    }

}
