/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8251544
 * @summary A dead data loop in dying code is not correctly removed resulting in unremovable data nodes.
 * @requires vm.compiler2.enabled
 * @library /test/lib /
 * @modules java.base/jdk.internal.misc
 *          java.management
 *          java.base/jdk.internal.access
 *          java.base/jdk.internal.reflect
 *
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI -XX:-TieredCompilation -Xbatch
 *                   compiler.c2.TestDeadDataLoopIGVN
 */

package compiler.c2;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Vector;
import java.util.ListIterator;
import jdk.internal.access.SharedSecrets;
import jdk.internal.misc.Unsafe;
import jdk.internal.reflect.ConstantPool;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Executable;

import compiler.whitebox.CompilerWhiteBoxTest;
import sun.hotspot.WhiteBox;

public class TestDeadDataLoopIGVN {

    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int TIERED_STOP_AT_LEVEL = WB.getIntxVMFlag("TieredStopAtLevel").intValue();
    private static final Unsafe UNSAFE = Unsafe.getUnsafe();

    // The original test only failed with CTW due to different inlining and virtual call decisions compared to an
    // execution with -Xcomp. This test adapts the behavior of CTW and compiles the methods with the Whitebox API
    // in order to reproduce the bug.
    public static void main(String[] strArr) throws Exception {
        // Required to get the same inlining/virtual call decisions as for CTW
        callSomeMethods(new ArrayList<String>());
        callSomeMethods(new Vector<String>());

        if (TIERED_STOP_AT_LEVEL != CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION) {
            throw new RuntimeException("Sanity check if C2 is available");
        }

        // To trigger the assertion, we only need to compile Test
        compileClass(Test.class);
    }

    private static void callSomeMethods(List<String> list) {
        list.add("bla");
        list.add("foo");
        ListIterator<String> it = list.listIterator();
        it.hasNext();
        for (String s : list) {
            s.charAt(0);
        }
    }

    // Adaptation from CTW to compile and deoptimize the same methods
    private static void compileClass(Class<?> aClass) throws Exception {
        aClass = Class.forName(aClass.getCanonicalName(), true, aClass.getClassLoader());
        ConstantPool constantPool = SharedSecrets.getJavaLangAccess().getConstantPool(aClass);
        preloadClasses(constantPool);
        UNSAFE.ensureClassInitialized(aClass);
        WB.enqueueInitializerForCompilation(aClass, 4); // Level 4 for C2

        for (Executable method : aClass.getDeclaredConstructors()) {
            WB.deoptimizeMethod(method);
            WB.enqueueMethodForCompilation(method, 4);
            WB.deoptimizeMethod(method);
        }

        for (Executable method : aClass.getDeclaredMethods()) {
            WB.deoptimizeMethod(method);
            WB.enqueueMethodForCompilation(method, 4);
            WB.deoptimizeMethod(method);
        }
    }

    private static void preloadClasses(ConstantPool constantPool) throws Exception {
        for (int i = 0, n = constantPool.getSize(); i < n; ++i) {
            try {
                constantPool.getClassAt(i);
            } catch (IllegalArgumentException ignore) {
            }
        }
    }
}

// The actual class that failed by executing it with CTW
class Test {

    public static A a = new A();

    Test() {
        LinkedList<A> l = new LinkedList<A>();
        for (int i = 0; i < 34; i++) {
            A instance = new A();
            instance.id = i;
            l.add(instance);
        }
        test(l, 34);
    }

    public void test(LinkedList<A> list, int max) {
        Integer[] numbers = new Integer[max + 1];
        A[] numbers2 = new A[max + 1];
        int n = 0;
        ListIterator<A> it = list.listIterator();
        while (it.hasNext()) {
            A b = it.next();
            numbers[b.get()] = n;
            numbers2[n] = b;
            n++;
        }

        Integer[] iArr = new Integer[max + 1];

        A a = getA();
        Integer x = numbers[a.get()];
        iArr[x] = x;

        boolean flag = true;
        while (flag) {
            flag = false;
            it = list.listIterator(34);
            while (it.hasPrevious()) {
                A b = it.previous();
                if (b == a) {
                    continue;
                }
            }
        }

        HashMap<A, A> map = new HashMap<A, A>();
        for (Integer i = 0; i < max - 34; i++) {
            map.put(numbers2[i], numbers2[iArr[i]]);
        }
    }

    public A getA() {
        return a;
    }
}

// Helper class
class A {
    int id;

    public int get() {
        return id;
    }
}
