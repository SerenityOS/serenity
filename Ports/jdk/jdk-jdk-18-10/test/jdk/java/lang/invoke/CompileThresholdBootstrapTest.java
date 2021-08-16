/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8143232
 * @summary Test verifies that LF bootstraps properly when run with COMPILE_THRESHOLD set
 * @compile CompileThresholdBootstrapTest.java
 * @run testng/othervm -Djava.lang.invoke.MethodHandle.COMPILE_THRESHOLD=30 test.java.lang.invoke.CompileThresholdBootstrapTest
 */
package test.java.lang.invoke;

import java.lang.invoke.MethodHandles;
import org.testng.*;
import org.testng.annotations.*;

public final class CompileThresholdBootstrapTest {

    @Test
    public void testBootstrap() throws Throwable {
        Assert.assertEquals((int)MethodHandles.constant(int.class, (int)0).invokeExact(), 0);
    }

    public static void main(String ... args) {
        try {
            CompileThresholdBootstrapTest test = new CompileThresholdBootstrapTest();
            test.testBootstrap();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
