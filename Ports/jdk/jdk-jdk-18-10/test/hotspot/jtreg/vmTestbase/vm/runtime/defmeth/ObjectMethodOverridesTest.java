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
 *      vm.runtime.defmeth.ObjectMethodOverridesTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import nsk.share.TestFailure;
import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.data.method.body.*;
import vm.runtime.defmeth.shared.builder.TestBuilder;

import static vm.runtime.defmeth.shared.ExecutionMode.*;
import static vm.runtime.defmeth.shared.data.method.body.CallMethod.IndexbyteOp.*;

/**
 * Test that default methods don't override methods inherited from Object class.
 */
public class ObjectMethodOverridesTest extends DefMethTest {

    public static void main(String[] args) {
        DefMethTest.runTest(ObjectMethodOverridesTest.class,
                /* majorVer */ Set.of(MAX_MAJOR_VER),
                /* flags    */ Set.of(0),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }

    /* protected Object clone() */
    public void testClone(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("clone", "()Ljava/lang/Object;")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .concreteMethod("m", "()V")
                    // force an invokevirtual MR
                    .invoke(CallMethod.Invoke.VIRTUAL,
                            b.clazzByName("C"), b.clazzByName("C"),
                            "clone", "()Ljava/lang/Object;", METHODREF)
                .build()
            .build();

        b.test().callSite(C, C, "m", "()V")
                .throws_(CloneNotSupportedException.class)
                .done();
    }

    /* boolean equals(Object obj) */
    public void testEquals(TestBuilder b) throws ReflectiveOperationException {
        Interface I = b.intf("I")
                .defaultMethod("equals", "(Ljava/lang/Object;)Z")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(I, C, "equals", "(Ljava/lang/Object;)Z")
                .ignoreResult()
                .done();
        b.test().callSite(C, C, "equals", "(Ljava/lang/Object;)Z")
                .ignoreResult()
                .done();
    }

    /* void finalize() */
    public void testFinalize(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("finalize", "()V")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I)
                .concreteMethod("m", "()V")
                    // force an invokevirtual MR
                    .invoke(CallMethod.Invoke.VIRTUAL,
                            b.clazzByName("C"), b.clazzByName("C"), "finalize", "()V", METHODREF)
                .build()
            .build();

        b.test().callSite(C, C, "m", "()V")
                .ignoreResult()
                .done();
    }

    /* final Class<?> getClass() */
    public void testGetClass(TestBuilder b) throws Exception {
        Interface I = b.intf("I")
                .defaultMethod("getClass", "()Ljava/lang/Class;")
                    .sig("()Ljava/lang/Class<*>;")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().loadClass(I).throws_(IncompatibleClassChangeError.class).done();
        b.test().loadClass(C).throws_(IncompatibleClassChangeError.class).done();
    }

    /* int hashCode() */
    public void testHashCode(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("hashCode", "()I")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().callSite(I, C, "hashCode", "()I")
                .ignoreResult()
                .done();
        b.test().callSite(C, C, "hashCode", "()I")
                .ignoreResult()
                .done();
    }


    /* final void notify() */
    public void testNotify(TestBuilder b) throws Exception {
        Interface I = b.intf("I")
                .defaultMethod("notify", "()V")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().loadClass(I).throws_(IncompatibleClassChangeError.class).done();
        b.test().loadClass(C).throws_(IncompatibleClassChangeError.class).done();
    }

    /* void notifyAll() */
    public void testNotifyAll(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("notifyAll", "()V")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().loadClass(I).throws_(IncompatibleClassChangeError.class).done();
        b.test().loadClass(C).throws_(IncompatibleClassChangeError.class).done();
    }

    /* String toString() */
    public void testToString(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("toString()", "()Ljava/lang/String;")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        if (factory.getExecutionMode() == "REFLECTION") {
            // Class.get*Method() don't find any implicitly declared method from Object on interfaces.
            b.test().callSite(I, C, "toString", "()Ljava/lang/String;")
                    .throws_(NoSuchMethodException.class)
                    .done();
        } else {
            b.test().callSite(I, C, "toString", "()Ljava/lang/String;")
                    .ignoreResult()
                    .done();
        }
        b.test().callSite(C, C, "toString", "()Ljava/lang/String;")
                .ignoreResult()
                .done();
    }

    /* final void wait() */
    public void testWait(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("wait", "()V")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().loadClass(I).throws_(IncompatibleClassChangeError.class).done();
        b.test().loadClass(C).throws_(IncompatibleClassChangeError.class).done();
    }

    /* final void wait(long timeout) */
    public void testTimedWait(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("wait", "(J)V")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().loadClass(I).throws_(IncompatibleClassChangeError.class).done();
        b.test().loadClass(C).throws_(IncompatibleClassChangeError.class).done();
    }

    /* final void wait(long timeout, int nanos) */
    public void testTimedWait1(TestBuilder b) {
        Interface I = b.intf("I")
                .defaultMethod("wait", "(JI)V")
                    .throw_(TestFailure.class)
                .build()
            .build();

        ConcreteClass C = b.clazz("C").implement(I).build();

        b.test().loadClass(I).throws_(IncompatibleClassChangeError.class).done();
        b.test().loadClass(C).throws_(IncompatibleClassChangeError.class).done();
    }
}
