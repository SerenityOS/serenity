/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @modules jdk.incubator.foreign/jdk.internal.foreign
 * @build NativeTestHelper CallGeneratorHelper TestUpcallHighArity
 *
 * @run testng/othervm/native
 *   --enable-native-access=ALL-UNNAMED
 *   TestUpcallHighArity
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.MemoryLayout;
import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static jdk.incubator.foreign.CLinker.*;
import static org.testng.Assert.assertEquals;

public class TestUpcallHighArity extends CallGeneratorHelper {
    static final MethodHandle MH_do_upcall;
    static final MethodHandle MH_passAndSave;
    static final CLinker LINKER = CLinker.getInstance();

    // struct S_PDI { void* p0; double p1; int p2; };
    static final MemoryLayout S_PDI_LAYOUT = MemoryLayout.structLayout(
        C_POINTER.withName("p0"),
        C_DOUBLE.withName("p1"),
        C_INT.withName("p2")
    );

    static {
        try {
            System.loadLibrary("TestUpcallHighArity");
            SymbolLookup lookup = SymbolLookup.loaderLookup();
            MH_do_upcall = LINKER.downcallHandle(
                lookup.lookup("do_upcall").get(),
                MethodType.methodType(void.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER)
            );
            MH_passAndSave = MethodHandles.lookup().findStatic(TestUpcallHighArity.class, "passAndSave",
                    MethodType.methodType(void.class, Object[].class, AtomicReference.class));
        } catch (ReflectiveOperationException e) {
            throw new InternalError(e);
        }
    }

    static void passAndSave(Object[] o, AtomicReference<Object[]> ref) {
        for (int i = 0; i < o.length; i++) {
            if (o[i] instanceof MemorySegment) {
                MemorySegment ms = (MemorySegment) o[i];
                MemorySegment copy = MemorySegment.allocateNative(ms.byteSize(), ResourceScope.newImplicitScope());
                copy.copyFrom(ms);
                o[i] = copy;
            }
        }
        ref.set(o);
    }

    @Test(dataProvider = "args")
    public void testUpcall(MethodHandle downcall, MethodType upcallType,
                           FunctionDescriptor upcallDescriptor) throws Throwable {
        AtomicReference<Object[]> capturedArgs = new AtomicReference<>();
        MethodHandle target = MethodHandles.insertArguments(MH_passAndSave, 1, capturedArgs)
                                         .asCollector(Object[].class, upcallType.parameterCount())
                                         .asType(upcallType);
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemoryAddress upcallStub = LINKER.upcallStub(target, upcallDescriptor, scope);
            Object[] args = new Object[upcallType.parameterCount() + 1];
            args[0] = upcallStub.address();
            List<MemoryLayout> argLayouts = upcallDescriptor.argumentLayouts();
            for (int i = 1; i < args.length; i++) {
                args[i] = makeArg(argLayouts.get(i - 1), null, false);
            }

            downcall.invokeWithArguments(args);

            Object[] capturedArgsArr = capturedArgs.get();
            for (int i = 0; i < capturedArgsArr.length; i++) {
                if (upcallType.parameterType(i) == MemorySegment.class) {
                    assertStructEquals((MemorySegment) capturedArgsArr[i], (MemorySegment) args[i + 1], argLayouts.get(i));
                } else {
                    assertEquals(capturedArgsArr[i], args[i + 1]);
                }
            }
        }
    }

    @DataProvider
    public static Object[][] args() {
        return new Object[][]{
            { MH_do_upcall,
                MethodType.methodType(void.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class,
                    MemorySegment.class, int.class, double.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER,
                    S_PDI_LAYOUT, C_INT, C_DOUBLE, C_POINTER)
            }
        };
    }

}
