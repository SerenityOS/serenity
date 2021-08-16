/*
 *  Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires os.arch=="amd64" | os.arch=="x86_64" | os.arch=="aarch64"
 * @run testng/othervm
 *   --enable-native-access=ALL-UNNAMED
 *   TestVirtualCalls
 */

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import org.testng.annotations.*;

import static jdk.incubator.foreign.CLinker.*;
import static org.testng.Assert.assertEquals;

public class TestVirtualCalls {

    static final CLinker abi = CLinker.getInstance();

    static final MethodHandle func;
    static final MemoryAddress funcA;
    static final MemoryAddress funcB;
    static final MemoryAddress funcC;

    static {
        func = abi.downcallHandle(
            MethodType.methodType(int.class),
            FunctionDescriptor.of(C_INT));

        System.loadLibrary("Virtual");
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        funcA = lookup.lookup("funcA").get();
        funcB = lookup.lookup("funcB").get();
        funcC = lookup.lookup("funcC").get();
    }

    @Test
    public void testVirtualCalls() throws Throwable {
        assertEquals((int) func.invokeExact((Addressable) funcA), 1);
        assertEquals((int) func.invokeExact((Addressable) funcB), 2);
        assertEquals((int) func.invokeExact((Addressable) funcC), 3);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullTarget() throws Throwable {
        int x = (int) func.invokeExact((Addressable) null);
    }

}
