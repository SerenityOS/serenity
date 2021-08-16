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
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED SafeFunctionAccessTest
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class SafeFunctionAccessTest {
    static {
        System.loadLibrary("SafeAccess");
    }

    static MemoryLayout POINT = MemoryLayout.structLayout(
            CLinker.C_INT, CLinker.C_INT
    );

    static final SymbolLookup LOOKUP = SymbolLookup.loaderLookup();

    @Test(expectedExceptions = IllegalStateException.class)
    public void testClosedStruct() throws Throwable {
        MemorySegment segment;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            segment = MemorySegment.allocateNative(POINT, scope);
        }
        assertFalse(segment.scope().isAlive());
        MethodHandle handle = CLinker.getInstance().downcallHandle(
                LOOKUP.lookup("struct_func").get(),
                MethodType.methodType(void.class, MemorySegment.class),
                FunctionDescriptor.ofVoid(POINT));

        handle.invokeExact(segment);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testClosedPointer() throws Throwable {
        MemoryAddress address;
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            address = MemorySegment.allocateNative(POINT, scope).address();
        }
        assertFalse(address.scope().isAlive());
        MethodHandle handle = CLinker.getInstance().downcallHandle(
                LOOKUP.lookup("addr_func").get(),
                MethodType.methodType(void.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(CLinker.C_POINTER));

        handle.invokeExact(address);
    }
}
