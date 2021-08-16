/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Simple tests for method signature parsing
 * @modules jdk.jdeps/com.sun.tools.jdeprscan.scan
 * @build TestMethodSig
 * @run testng jdk.jdeprscan.TestMethodSig
 */

package jdk.jdeprscan;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static com.sun.tools.jdeprscan.scan.MethodSig.fromDesc;

public class TestMethodSig {
    @Test
    public void testSimple() {
        assertEquals(fromDesc("(Ljava/rmi/RMISecurityManager;)Ljava/lang/Object;").toString(),
                     "parameters 0=Ljava/rmi/RMISecurityManager; return Ljava/lang/Object;");
    }

    @Test
    public void testMultParamVoidReturn() {
        assertEquals(fromDesc("([[IZLjava/lang/String;B[J)V").toString(),
                     "parameters 0=[[I 1=Z 2=Ljava/lang/String; 3=B 4=[J return V");
    }

    @Test
    public void testNoParams() {
        assertEquals(fromDesc("()J").toString(),
                     "parameters none return J");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMissingReturnType() {
        fromDesc("(ISJZ)");
    }
}
