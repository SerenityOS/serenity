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

package handle.lookup;

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SegmentAllocator;

import org.testng.annotations.*;

public class MethodHandleLookup {

    @Test(dataProvider = "restrictedMethods")
    public void testRestrictedHandles(MethodHandle handle, String testName) throws Throwable {
        new handle.invoker.MethodHandleInvoker().call(handle);
    }

    @DataProvider(name = "restrictedMethods")
    static Object[][] restrictedMethods() {
        try {
            return new Object[][]{
                    { MethodHandles.lookup().findStatic(CLinker.class, "getInstance",
                            MethodType.methodType(CLinker.class)), "CLinker::getInstance" },
                    { MethodHandles.lookup().findStatic(CLinker.class, "toJavaString",
                            MethodType.methodType(String.class, MemoryAddress.class)),
                            "CLinker::toJavaString" },
                    { MethodHandles.lookup().findStatic(CLinker.class, "allocateMemory",
                            MethodType.methodType(MemoryAddress.class, long.class)),
                            "CLinker::allocateMemory" },
                    { MethodHandles.lookup().findStatic(CLinker.class, "freeMemory",
                            MethodType.methodType(void.class, MemoryAddress.class)),
                            "CLinker::freeMemory" },
                    { MethodHandles.lookup().findStatic(CLinker.VaList.class, "ofAddress",
                            MethodType.methodType(CLinker.VaList.class, MemoryAddress.class)),
                            "VaList::ofAddress/1" },
                    { MethodHandles.lookup().findStatic(CLinker.VaList.class, "ofAddress",
                            MethodType.methodType(CLinker.VaList.class, MemoryAddress.class, ResourceScope.class)),
                            "VaList::ofAddress/2" },
                    { MethodHandles.lookup().findStatic(CLinker.class, "systemLookup",
                            MethodType.methodType(SymbolLookup.class)),
                            "CLinker::systemLookup" },
                    { MethodHandles.lookup().findStatic(SymbolLookup.class, "loaderLookup",
                            MethodType.methodType(SymbolLookup.class)),
                            "SymbolLookup::loaderLookup" },
                    { MethodHandles.lookup().findVirtual(MemoryAddress.class, "asSegment",
                            MethodType.methodType(MemorySegment.class, long.class, ResourceScope.class)),
                            "MemoryAddress::asSegment/1" },
                    { MethodHandles.lookup().findVirtual(MemoryAddress.class, "asSegment",
                            MethodType.methodType(MemorySegment.class, long.class, Runnable.class, ResourceScope.class)),
                            "MemoryAddress::asSegment/2" },
                    { MethodHandles.lookup().findStatic(MemorySegment.class, "globalNativeSegment",
                            MethodType.methodType(MemorySegment.class)),
                            "MemoryAddress::globalNativeSegment" }
            };
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError((ex));
        }
    }
}
