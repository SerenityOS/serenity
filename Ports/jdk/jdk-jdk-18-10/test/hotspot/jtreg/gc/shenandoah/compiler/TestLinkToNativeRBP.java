/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8259937
 * @summary guarantee(loc != NULL) failed: missing saved register with native invoke
 *
 * @requires vm.flavor == "server"
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @requires vm.gc.Shenandoah
 *
 * @modules jdk.incubator.foreign
 *
 * @run main/othervm --enable-native-access=ALL-UNNAMED -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive TestLinkToNativeRBP
 *
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import static jdk.incubator.foreign.CLinker.C_INT;

public class TestLinkToNativeRBP {
    static {
        System.loadLibrary("LinkToNativeRBP");
    }

    final static CLinker abi = CLinker.getInstance();
    static final SymbolLookup lookup = SymbolLookup.loaderLookup();
    final static MethodHandle foo = abi.downcallHandle(lookup.lookup("foo").get(),
            MethodType.methodType(int.class),
            FunctionDescriptor.of(C_INT));

    static int foo() throws Throwable {
        return (int)foo.invokeExact();
    }
    public static void main(String[] args) throws Throwable {
        for (int i = 0; i < 20_000; i++) {
            test(5);
        }
        for (int i = 0; i < 100; i++) {
            test(1_000_000);
        }
    }

    static volatile Integer field = 0;

    private static int test(int stop) throws Throwable {
        int res = 0;
        for (int i = 0; i < stop; i++) {
            Integer v = field;
            res = foo() + v.intValue();
        }
        return res;
    }

}
