/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8170455
 * @summary C2: Access to [].clone from interfaces fails.
 * @library /test/lib /
 *
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xcomp -Xbatch -Xbootclasspath/a:.  -XX:-TieredCompilation  -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=compileonly,*TestDefaultMethodArrayCloneDeoptC2Interface::test
 *                   compiler.arraycopy.TestDefaultMethodArrayCloneDeoptC2
 */

package compiler.arraycopy;

import sun.hotspot.WhiteBox;
import java.lang.reflect.Method;
import compiler.whitebox.CompilerWhiteBoxTest;



interface TestDefaultMethodArrayCloneDeoptC2Interface {
    default int[] test(int[] arr) {
        return arr.clone();
    }

    default TDMACDC2InterfaceTypeTest[] test(TDMACDC2InterfaceTypeTest[] arr) {
        return arr.clone();
    }

    default TDMACDC2ClassTypeTest[] test(TDMACDC2ClassTypeTest[] arr) {
        return arr.clone();
    }
}

public class TestDefaultMethodArrayCloneDeoptC2 implements TestDefaultMethodArrayCloneDeoptC2Interface {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    public static TestDefaultMethodArrayCloneDeoptC2 a = new TestDefaultMethodArrayCloneDeoptC2();

    public static void main(String[] args) throws Exception {
        testPrimitiveArr();
        testIntfArr();
        testClassArr();
    }

    public static void testPrimitiveArr() throws Exception {
        Method m = TestDefaultMethodArrayCloneDeoptC2Interface.class.getMethod("test", int[].class);
        a.test(new int[1]); // Compiled
        a.test(new int[1]);
        if (!WB.isMethodCompiled(m)) {
            WB.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        }
        a.test(new int[1]);
        if (!WB.isMethodCompiled(m)) {
            throw new Exception("Method should be compiled");
        }
    }

    public static void testIntfArr() throws Exception {
        Method m = TestDefaultMethodArrayCloneDeoptC2Interface.class.getMethod("test", TDMACDC2InterfaceTypeTest[].class);
        a.test(new TDMACDC2InterfaceTypeTest[1]); // Compiled, Decompile unloaded
        a.test(new TDMACDC2InterfaceTypeTest[1]); // Compiled
        a.test(new TDMACDC2InterfaceTypeTest[1]);
        if (!WB.isMethodCompiled(m)) {
            WB.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        }
        a.test(new TDMACDC2InterfaceTypeTest[1]);
        if (!WB.isMethodCompiled(m)) {
            throw new Exception("Method should be compiled");
        }
    }

    public static void testClassArr() throws Exception {
        Method m = TestDefaultMethodArrayCloneDeoptC2Interface.class.getMethod("test", TDMACDC2ClassTypeTest[].class);
        a.test(new TDMACDC2ClassTypeTest[1]); // Compiled, Decompile unloaded
        a.test(new TDMACDC2ClassTypeTest[1]); // Compiled
        a.test(new TDMACDC2ClassTypeTest[1]);
        if (!WB.isMethodCompiled(m)) {
            WB.enqueueMethodForCompilation(m, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
        }
        a.test(new TDMACDC2ClassTypeTest[1]);
        if (!WB.isMethodCompiled(m)) {
            throw new Exception("Method should be compiled");
        }
    }
}

interface TDMACDC2InterfaceTypeTest {
}

class TDMACDC2ClassTypeTest {
}

