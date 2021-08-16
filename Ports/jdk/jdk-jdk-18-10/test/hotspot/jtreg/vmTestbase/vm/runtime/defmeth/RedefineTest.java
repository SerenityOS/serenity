/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @modules java.base/jdk.internal.org.objectweb.asm:+open java.base/jdk.internal.org.objectweb.asm.util:+open
 * @library /vmTestbase /test/lib
 *
 * @comment build retransform.jar in current dir
 * @run driver vm.runtime.defmeth.shared.BuildJar
 *
 * @run driver jdk.test.lib.FileInstaller . .
 * @run main/othervm/native
 *      -agentlib:redefineClasses
 *      -javaagent:retransform.jar
 *      vm.runtime.defmeth.RedefineTest
 */
package vm.runtime.defmeth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nsk.share.Pair;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.DefMethTestFailure;
import vm.runtime.defmeth.shared.MemoryClassLoader;
import vm.runtime.defmeth.shared.annotation.NotApplicableFor;
import vm.runtime.defmeth.shared.builder.TestBuilder;
import vm.runtime.defmeth.shared.executor.TestExecutor;
import vm.runtime.defmeth.shared.data.Clazz;
import vm.runtime.defmeth.shared.data.ConcreteClass;
import vm.runtime.defmeth.shared.data.Interface;
import vm.runtime.defmeth.shared.data.Tester;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/*
 * Basic scenarios on class redefinition.
 */
public class RedefineTest extends DefMethTest {

    public static void main(String[] args) {
        DefMethTest.runTest(RedefineTest.class,
                /* majorVer */ Set.of(MIN_MAJOR_VER, MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(true),
                /* execMode */ Set.of(DIRECT, INVOKE_EXACT, INVOKE_GENERIC, INDY));
    }

    @Override
    protected void configure() {
        // There are no testers being generated for reflection-based scenarios,
        // so scenarios on class redefinition don't work
        String mode = factory.getExecutionMode();
        if ("REFLECTION".equals(mode) || "INVOKE_WITH_ARGS".equals(mode)) {
            getLog().warn("RedefineTest isn't applicable to reflection-based execution scenario (REDEFINE & INVOKE_WITH_ARGS).");
        }
    }

    /**
     * Run test {@code b1} w/ redefined {@code classes} from {@code b2}.
     *
     * @param b1
     * @param b2
     * @param classes
     */
    private void redefineAndRun(TestBuilder b1, TestBuilder b2, Clazz... classes) {
        TestExecutor executor = b1.prepare();

        getLog().info("Before");
        List<Pair<Tester,Throwable>> errorsBefore =
                executor.run(); // run b1

        // redefine in b1
        MemoryClassLoader cl = executor.getLoader(); // b1.cl
        Map<String,byte[]> cf = b2.produce(); //
        Map<String,byte[]> forRedef = new HashMap<>();
        for (Clazz clz : classes) {
            String name = clz.name();
            forRedef.put(name, cf.get(name));
        }

        cl.modifyClasses(forRedef, factory.isRetransformClasses());

        getLog().info("After");
        List<Pair<Tester,Throwable>> errorsAfter =
                executor.run();

        if (!errorsBefore.isEmpty()) {
            throw new DefMethTestFailure(errorsBefore);
        }

        if (!errorsAfter.isEmpty()) {
            throw new DefMethTestFailure(errorsAfter);
        }
    }

    /*
     * Before redefinition:
     *   interface I { public int m() { return 1; } }
     *   class C extends I { public int m() { return 2; } }
     *
     * TEST: I i = new C(); i.m() == 2
     * TEST: C c = new C(); c.m() == 2
     *
     * After redefinition:
     *   interface I { public int m() { return 1; } }
     *   class C extends I { public int m() { return 3; } }
     *
     * TEST: I i = new C(); i.m() == 3
     * TEST: C c = new C(); c.m() == 3
     */
    @NotApplicableFor(modes = { REFLECTION, INVOKE_WITH_ARGS }) // reflection-based scenarios rely on checks in bytecode
    public void testRedefineConcreteMethod() {
        TestBuilder before = factory.getBuilder();
        { // Before redefinition
            Interface I = before.intf("I")
                    .defaultMethod("m", "()I").returns(1).build()
                .build();
            ConcreteClass C = before.clazz("C").implement(I)
                    .concreteMethod("m", "()I").returns(2).build()
                .build();

            before.test().callSite(I, C, "m", "()I").returns(2).done()
                  .test().callSite(C, C, "m", "()I").returns(2).done();
        }

        { // After redefinition
            TestBuilder after = factory.getBuilder();

            Interface I = after.intf("I")
                    .defaultMethod("m", "()I").returns(1).build()
                .build();
            ConcreteClass C = after.clazz("C").implement(I)
                    .concreteMethod("m", "()I").returns(3).build()
                .build();

            Tester T1 = after.test().callSite(I, C, "m", "()I").returns(3).build();
            Tester T2 = after.test().callSite(C, C, "m", "()I").returns(3).build();

            redefineAndRun(before, after, C, T1, T2);
        }
    }

    /*
     * Before redefinition:
     *   interface I { public int m() { return 1; } }
     *   class C extends I { public int m() { return 2; } }
     *
     * TEST: I i = new C(); i.m() == 2
     * TEST: C c = new C(); c.m() == 2
     *
     * After redefinition:
     *   interface I { public int m() { return 3; } }
     *   class C extends I { public int m() { return 2; } }
     *
     * TEST: I i = new C(); i.m() == 2
     * TEST: C c = new C(); c.m() == 2
     */
    @NotApplicableFor(modes = { REFLECTION, INVOKE_WITH_ARGS }) // reflection-based scenarios rely on checks in bytecode
    public void testRedefineDefaultMethod() {
        TestBuilder before = factory.getBuilder();
        { // Before redefinition
            Interface I = before.intf("I")
                    .defaultMethod("m", "()I").returns(1).build()
                .build();
            ConcreteClass C = before.clazz("C").implement(I)
                    .concreteMethod("m", "()I").returns(2).build()
                .build();

            before.test().callSite(I, C, "m", "()I").returns(2).done()
                  .test().callSite(C, C, "m", "()I").returns(2).done();
        }

        { // After redefinition
            TestBuilder after = factory.getBuilder();

            Interface I = after.intf("I")
                    .defaultMethod("m", "()I").returns(3).build()
                .build();
            ConcreteClass C = after.clazz("C").implement(I)
                    .concreteMethod("m", "()I").returns(2).build()
                .build();

            redefineAndRun(before, after, C);
        }
    }

    /*
     * Before redefinition:
     *   interface I { public int m() { return 1; } }
     *   class C extends I {}
     *
     * TEST: I i = new C(); i.m() == 1
     * TEST: C c = new C(); c.m() == 1
     *
     * After redefinition:
     *   interface I { public int m() { return 2; } }
     *   class C extends I {}
     *
     * TEST: I i = new C(); i.m() == 2
     * TEST: C c = new C(); c.m() == 2
     */
    @NotApplicableFor(modes = { REFLECTION, INVOKE_WITH_ARGS }) // reflection-based scenarios rely on checks in bytecode
    public void testRedefineDefMethInConcreteClass() {
        TestBuilder before = factory.getBuilder();
        { // Before redefinition
            Interface I = before.intf("I")
                    .defaultMethod("m", "()I").returns(1).build()
                .build();
            ConcreteClass C = before.clazz("C").implement(I).build();

            before.test().callSite(I, C, "m", "()I").returns(1).done()
                  .test().callSite(C, C, "m", "()I").returns(1).done();
        }

        { // After redefinition
            TestBuilder after = factory.getBuilder();

            Interface I = after.intf("I")
                    .defaultMethod("m", "()I").returns(2).build()
                .build();
            ConcreteClass C = after.clazz("C").implement(I).build();

            Tester T1 = after.test().callSite(I, C, "m", "()I").returns(2).build();
            Tester T2 = after.test().callSite(C, C, "m", "()I").returns(2).build();

            redefineAndRun(before, after, I, T1, T2);
        }
    }
}
