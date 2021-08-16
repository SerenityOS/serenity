/*
 *  Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test id=default_gc
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions
 *   -XX:+WhiteBoxAPI
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   TestStackWalk
 *
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions
 *   -XX:+WhiteBoxAPI
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   TestStackWalk
 */

/*
 * @test id=zgc
 * @requires (((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64")
 * @requires vm.gc.Z
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions
 *   -XX:+WhiteBoxAPI
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   -XX:+UseZGC
 *   TestStackWalk
 *
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions
 *   -XX:+WhiteBoxAPI
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   -XX:+UseZGC
 *   TestStackWalk
 */
/*
 * @test id=shenandoah
 * @requires (((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64")
 * @requires vm.gc.Shenandoah
 * @library /test/lib
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions
 *   -XX:+WhiteBoxAPI
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=true
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   -XX:+UseShenandoahGC
 *   TestStackWalk
 *
 * @run main/othervm
 *   -Xbootclasspath/a:.
 *   -XX:+UnlockDiagnosticVMOptions
 *   -XX:+WhiteBoxAPI
 *   -Djdk.internal.foreign.ProgrammableInvoker.USE_INTRINSICS=false
 *   --enable-native-access=ALL-UNNAMED
 *   -Xbatch
 *   -XX:+UseShenandoahGC
 *   TestStackWalk
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.ref.Reference;

import jdk.incubator.foreign.ResourceScope;
import sun.hotspot.WhiteBox;

import static java.lang.invoke.MethodHandles.lookup;
import static jdk.incubator.foreign.CLinker.C_POINTER;

public class TestStackWalk {
    static final WhiteBox WB = WhiteBox.getWhiteBox();

    static final CLinker linker = CLinker.getInstance();

    static final MethodHandle MH_foo;
    static final MethodHandle MH_m;

    static {
        try {
            System.loadLibrary("StackWalk");
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            MH_foo = linker.downcallHandle(
                    lookup.lookup("foo").get(),
                    MethodType.methodType(void.class, MemoryAddress.class),
                    FunctionDescriptor.ofVoid(C_POINTER));
            MH_m = lookup().findStatic(TestStackWalk.class, "m", MethodType.methodType(void.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    static boolean armed;

    public static void main(String[] args) throws Throwable {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemoryAddress stub = linker.upcallStub(MH_m, FunctionDescriptor.ofVoid(), scope);
            MemoryAddress stubAddress = stub.address();
            armed = false;
            for (int i = 0; i < 20_000; i++) {
                payload(stubAddress); // warmup
            }

            armed = true;
            payload(stubAddress); // test
        }
    }

    static void payload(MemoryAddress cb) throws Throwable {
        MH_foo.invokeExact(cb);
        Reference.reachabilityFence(cb); // keep oop alive across call
    }

    static void m() {
        if (armed) {
            WB.verifyFrames(/*log=*/true, /*updateRegisterMap=*/true);
            WB.verifyFrames(/*log=*/true, /*updateRegisterMap=*/false); // triggers different code paths
        }
    }

}
