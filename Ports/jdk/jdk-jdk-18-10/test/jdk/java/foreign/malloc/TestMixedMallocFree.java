/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng/othervm --enable-native-access=ALL-UNNAMED TestMixedMallocFree
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAccess;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SymbolLookup;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static jdk.incubator.foreign.CLinker.*;
import static org.testng.Assert.assertEquals;

public class TestMixedMallocFree {

    static final MethodHandle MH_my_malloc;

    static {
        System.loadLibrary("Malloc");
        SymbolLookup MALLOC = SymbolLookup.loaderLookup();

        MH_my_malloc = CLinker.getInstance().downcallHandle(
            MALLOC.lookup("my_malloc").orElseThrow(),
            MethodType.methodType(MemoryAddress.class, long.class),
            FunctionDescriptor.of(C_POINTER, C_LONG_LONG));
    }

    @Test
    public void testMalloc() throws Throwable {
        MemoryAddress ma = (MemoryAddress) MH_my_malloc.invokeExact(4L);
        MemorySegment seg = ma.asSegment(4L, ResourceScope.newImplicitScope());
        MemoryAccess.setInt(seg, 42);
        assertEquals(MemoryAccess.getInt(seg), 42);
        // Test if this free crashes the VM, which might be the case if we load the wrong default library
        // and end up mixing two allocators together.
        CLinker.freeMemory(ma);
    }

}
