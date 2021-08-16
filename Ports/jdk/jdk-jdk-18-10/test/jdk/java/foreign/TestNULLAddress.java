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
 * @modules jdk.incubator.foreign
 * @run testng/othervm
 *     --enable-native-access=ALL-UNNAMED
 *     TestNULLAddress
 */

import jdk.incubator.foreign.Addressable;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.charset.Charset;

public class TestNULLAddress {

    static final CLinker LINKER = CLinker.getInstance();

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNULLLinking() {
        LINKER.downcallHandle(
                MemoryAddress.NULL,
                MethodType.methodType(void.class),
                FunctionDescriptor.ofVoid());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNULLVirtual() throws Throwable {
        MethodHandle mh = LINKER.downcallHandle(
                MethodType.methodType(void.class),
                FunctionDescriptor.ofVoid());
        mh.invokeExact((Addressable) MemoryAddress.NULL);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNULLtoJavaString() {
        CLinker.toJavaString(MemoryAddress.NULL);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNULLfreeMemory() {
        CLinker.freeMemory(MemoryAddress.NULL);
    }
}
