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
 *      vm.runtime.defmeth.AccessibilityFlagsTest
 */
package vm.runtime.defmeth;

import java.util.Set;

import vm.runtime.defmeth.shared.DefMethTest;
import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.data.method.body.EmptyBody;
import vm.runtime.defmeth.shared.builder.TestBuilder;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import static vm.runtime.defmeth.shared.ExecutionMode.*;

/*
 * Test allowed combinations of access flags on methods in interfaces.
 * Based on assertions from JVMS.
 */
public class AccessibilityFlagsTest extends DefMethTest {
    public static void main(String[] args) {
        DefMethTest.runTest(AccessibilityFlagsTest.class,
                /* majorVer */ Set.of(MAX_MAJOR_VER),
                /* flags    */ Set.of(0, ACC_SYNCHRONIZED),
                /* redefine */ Set.of(false, true),
                /* execMode */ Set.of(DIRECT, REFLECTION, INVOKE_EXACT, INVOKE_GENERIC, INVOKE_WITH_ARGS, INDY));
    }

    /* ====================================================================== */

    // Methods of interfaces may set any of the flags in Table 4.5 except
    // ACC_PROTECTED, ACC_FINAL, ACC_NATIVE, and ACC_SYNCHRONIZED (9.4);

    /**
     * interface I { protected void m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testProtectedMethodAbstract() {
        expectClassFormatError(
                createAbstractMethodInterface(ACC_PROTECTED));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_PROTECTED | ACC_PUBLIC));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_PROTECTED | ACC_PRIVATE));

    }

    /**
     * interface I { protected void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testProtectedMethodDefault() {
        expectClassFormatError(
                createDefaultMethodInterface(ACC_PROTECTED));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_PROTECTED | ACC_PUBLIC));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_PROTECTED | ACC_PRIVATE));
    }

    /**
     * interface I { final void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testFinalMethodDefault() {
        expectClassFormatError(
                createDefaultMethodInterface(ACC_FINAL));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_FINAL | ACC_PUBLIC));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_FINAL | ACC_PRIVATE));
    }

    /**
     * interface I { native void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testNativeMethodDefault() {
        expectClassFormatError(
                createDefaultMethodInterface(ACC_NATIVE));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_NATIVE | ACC_PUBLIC));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_NATIVE | ACC_PRIVATE));
    }


    /**
     * interface I { synchronized void m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testSynchronizedMethodAbstract() {
        expectClassFormatError(
                createAbstractMethodInterface(ACC_SYNCHRONIZED));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_SYNCHRONIZED | ACC_PUBLIC));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_SYNCHRONIZED | ACC_PRIVATE));
    }

    /**
     * interface I { synchronized void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testSynchronizedMethodDefault() {
        expectClassFormatError(
                createDefaultMethodInterface(ACC_SYNCHRONIZED));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_SYNCHRONIZED | ACC_PUBLIC));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_SYNCHRONIZED | ACC_PRIVATE));
    }

    /* ===================================================================== */

    // [methods of interfaces] must have exactly one of the ACC_PUBLIC or ACC_PRIVATE flags set.

    /**
     * interface I { private void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") == succeeds
    */
    public void testPrivateMethodDefault() {
        loadClass(
                createDefaultMethodInterface(ACC_PRIVATE));
    }

    /**
     * interface I { public void m(); }
     *
     * TEST: ClassLoader.loadClass("I") == succeeds
     */
    public void testPublicMethodAbstract() {
        loadClass(
                createAbstractMethodInterface(ACC_PUBLIC));
    }

    /**
     * interface I { public void m() default {}; }
     *
     */
    public void testPublicMethodDefault() {
        loadClass(
                createDefaultMethodInterface(ACC_PUBLIC));
    }

    /**
     * interface I { private public void m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testPrivatePublicMethodAbstract() {
        expectClassFormatError(
                createAbstractMethodInterface(ACC_PUBLIC | ACC_PRIVATE));
    }

    /**
     * interface I { private public void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testPrivatePublicMethodDefault() {
        expectClassFormatError(
                createDefaultMethodInterface(ACC_PUBLIC | ACC_PRIVATE));
    }

    /* ===================================================================== */

    // Methods of classes may set any of the flags in Table 4.5 except
    // ACC_PROTECTED, ACC_FINAL, ACC_NATIVE, and ACC_SYNCHRONIZED (9.4);
    // they must have exactly one of the ACC_PUBLIC or ACC_PRIVATE flags set.
    //
    // The following flags are allowed:
    //    ACC_PUBLIC    0x0001  Declared public; may be accessed from outside its package.
    //    ACC_PRIVATE   0x0002  Declared private; accessible only within the defining class.
    //    ACC_STATIC    0x0008  Declared static.
    //    ACC_BRIDGE    0x0040  A bridge method, generated by the compiler.
    //    ACC_VARARGS   0x0080  Declared with variable number of arguments.
    //    ACC_SYNTHETIC 0x1000  Declared synthetic; not present in the source code.

    /**
     * interface I { static void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") == succeeds
    */
    public void testStaticMethodDefault() {
        loadClass(
                createDefaultMethodInterface(ACC_STATIC | ACC_PUBLIC));
        loadClass(
                createDefaultMethodInterface(ACC_STATIC | ACC_PRIVATE));
    }

    /* =============================================================================== */

    // If a specific method of a class or interface has its ACC_ABSTRACT flag set,
    // it must not have any of its ACC_FINAL, ACC_NATIVE, ACC_PRIVATE, ACC_STATIC,
    // or ACC_SYNCHRONIZED flags set (8.4.3.1, 8.4.3.3, 8.4.3.4).

    /**
     * interface I      {          final void m(); }
     * abstract class C { abstract final void m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     * TEST: ClassLoader.loadClass("C") ==> ClassFormatError
     */
    public void testFinalMethodAbstract() {
        /* interface I */
        expectClassFormatError(
                createAbstractMethodInterface(ACC_FINAL));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_FINAL | ACC_PUBLIC));

        /* abstract class C */
        expectClassFormatError(
                createAbstractMethodClass(ACC_FINAL));
    }

    /**
     * interface I      { native void m(); }
     * interface K      { native void m() default {}; }
     * abstract class C { abstract native m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     * TEST: ClassLoader.loadClass("K") ==> ClassFormatError
     * TEST: ClassLoader.loadClass("C") ==> ClassFormatError
     */
    public void testNativeMethodAbstract() {
        /* interface I */
        expectClassFormatError(
                createDefaultMethodInterface(ACC_NATIVE));

        expectClassFormatError(
                createDefaultMethodInterface(ACC_NATIVE | ACC_PUBLIC));

        /* interface K */
        expectClassFormatError(
                createAbstractMethodInterface(ACC_NATIVE));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_NATIVE | ACC_PUBLIC));

        /* abstract class C */
        expectClassFormatError(
                createAbstractMethodClass(ACC_NATIVE));
    }

    /**
     * interface I      {          private void m(); }
     * abstract class C { abstract private void m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     * TEST: ClassLoader.loadClass("C") ==> ClassFormatError
     */
    public void testPrivateMethodAbstract() {
        /* interface I */
        expectClassFormatError(
                createAbstractMethodInterface(ACC_PRIVATE));

        /* abstract class C */
        expectClassFormatError(
                createAbstractMethodClass(ACC_PRIVATE));
    }

    /**
     * interface I      {          static void m(); }
     * abstract class C { abstract static void m(); }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     * TEST: ClassLoader.loadClass("C") ==> ClassFormatError
     */
    public void testStaticMethodAbstract() {
        /* interface I */
        expectClassFormatError(
                createAbstractMethodInterface(ACC_STATIC));

        expectClassFormatError(
                createAbstractMethodInterface(ACC_STATIC | ACC_PUBLIC));

        /* abstract class C */
        expectClassFormatError(
                createAbstractMethodClass(ACC_STATIC));
    }

    /* =============================================================================== */

    /**
     * interface I { abstract void m() default {}; }
     *
     * TEST: ClassLoader.loadClass("I") ==> ClassFormatError
     */
    public void testAbstractMethodDefault() {
        expectClassFormatError(
                createDefaultMethodInterface(ACC_ABSTRACT));
    }

    /* ====================================================================== */

    // Helper methods
    private Interface createAbstractMethodInterface(int acc) {
        return factory.getBuilder()
            .intf("I")
                .abstractMethod("m", "()V").flags(acc)
                .build()
            .build();
    }

    private Clazz createAbstractMethodClass(int acc) {
        return factory.getBuilder()
            .clazz("I")
                .abstractMethod("m", "()V").flags(acc)
                .build()
            .build();
    }

    private Interface createDefaultMethodInterface(int acc) {
        return factory.getBuilder()
            .intf("I")
                .defaultMethod("m", "()V").flags(acc)
                    .body(new EmptyBody())
                .build()
            .build();
    }

    private void expectException(Clazz clz, Class<? extends Throwable> exc) {
        TestBuilder b = factory.getBuilder()
                .register(clz);

        b.test().loadClass(clz).throws_(exc).done()
        .run();
    }

    private void loadClass(Clazz clz) {
        TestBuilder b = factory.getBuilder()
                .register(clz);

        b.test().loadClass(clz).ignoreResult().done()
        .run();
    }

    private void expectClassFormatError(Clazz clz) {
        expectException(clz, ClassFormatError.class);
    }
}
