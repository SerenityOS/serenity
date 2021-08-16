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

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.ResourceScope;
import jdk.incubator.foreign.SymbolLookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.security.Permission;

import static jdk.incubator.foreign.CLinker.C_POINTER;

public class ThrowingUpcall {

    private static final MethodHandle downcall;
    public static final MethodHandle MH_throwException;

    static {
        System.loadLibrary("TestUpcall");
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        downcall = CLinker.getInstance().downcallHandle(
            lookup.lookup("f0_V__").orElseThrow(),
            MethodType.methodType(void.class, MemoryAddress.class),
            FunctionDescriptor.ofVoid(C_POINTER)
        );

        try {
            MH_throwException = MethodHandles.lookup().findStatic(ThrowingUpcall.class, "throwException",
                    MethodType.methodType(void.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void throwException() throws Throwable {
        throw new Throwable("Testing upcall exceptions");
    }

    public static void main(String[] args) throws Throwable {
        test();
    }

    public static void test() throws Throwable {
        MethodHandle handle = MH_throwException;
        MethodHandle invoker = MethodHandles.exactInvoker(MethodType.methodType(void.class));
        handle = MethodHandles.insertArguments(invoker, 0, handle);

        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemoryAddress stub = CLinker.getInstance().upcallStub(handle, FunctionDescriptor.ofVoid(), scope);

            downcall.invokeExact(stub); // should call Shutdown.exit(1);
        }
    }

}
