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

/*
 * @test
 * @requires !vm.graal.enabled & vm.opt.final.UseVtableBasedCHA == true
 * @modules java.base/jdk.internal.org.objectweb.asm
 *          java.base/jdk.internal.misc
 *          java.base/jdk.internal.vm.annotation
 * @library /test/lib /
 * @compile Utils.java
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+PrintCompilation -XX:+PrintInlining -XX:+TraceDependencies -verbose:class -XX:CompileCommand=quiet
 *                   -XX:CompileCommand=compileonly,*::m
 *                   -XX:CompileCommand=compileonly,*::test -XX:CompileCommand=dontinline,*::test
 *                   -Xbatch -Xmixed -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation
 *                      compiler.cha.DefaultRootMethod
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+PrintCompilation -XX:+PrintInlining -XX:+TraceDependencies -verbose:class -XX:CompileCommand=quiet
 *                   -XX:CompileCommand=compileonly,*::m
 *                   -XX:CompileCommand=compileonly,*::test -XX:CompileCommand=dontinline,*::test
 *                   -Xbatch -Xmixed -XX:+WhiteBoxAPI
 *                   -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                      compiler.cha.DefaultRootMethod
 */
package compiler.cha;

import static compiler.cha.Utils.*;

public class DefaultRootMethod {
    public static void main(String[] args) {
        run(DefaultRoot.class);
        run(InheritedDefault.class);
        System.out.println("TEST PASSED");
    }

    public static class DefaultRoot extends ATest<DefaultRoot.C> {
        public DefaultRoot() {
            super(C.class, D.class);
        }

        interface I { default Object m() { return CORRECT; } }

        static class C implements I { /* inherited I.m */}

        static class D extends C { /* inherited I.m */ }

        static abstract class E1 extends C { /* empty */ }
        static abstract class E2 extends C { public abstract Object m(); }
        static abstract class E3 extends C { public Object m() { return "E3.m"; } }

        interface I1 extends I { Object m(); }
        interface I2 extends I { default Object m() { return "I2.m"; } }

        static abstract class F1 extends C implements I1 { }
        static abstract class F2 extends C implements I2 { }

        static          class G  extends C { public Object m() { return CORRECT; } }

        @Override
        public Object test(C obj) {
            return obj.m(); // invokevirtual C.m()
        }

        @Override
        public void checkInvalidReceiver() {
            // nothing to do: concrete class types are enforced by the verifier
        }

        @TestCase
        public void test() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // Dn <: D.m <: C <: I.m DEFAULT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = C, method = D.m

            // 1. No invalidation: abstract classes don't participate in CHA.
            initialize(E1.class,  // ABSTRACT E1            <: C <: I.m DEFAULT
                       E2.class,  // ABSTRACT E2.m ABSTRACT <: C <: I.m DEFAULT
                       E3.class,  // ABSTRACT E3.m          <: C <: I.m DEFAULT
                       F1.class,  // ABSTRACT F1            <: C <: I.m DEFAULT, I1.m ABSTRACT
                       F2.class); // ABSTRACT F2            <: C <: I.m DEFAULT, I2.m DEFAULT
            assertCompiled();

            // 2. Dependency invalidation: G.m <: C <: I.m DEFAULT
            load(G.class);
            assertCompiled();

            // 3. Dependency invalidation: G.m <: C <: I.m DEFAULT
            initialize(G.class);
            assertNotCompiled();

            // 4. Recompilation: no inlining, no dependencies
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; } }); //  Cn.m <: C <: I.m DEFAULT
            call(new G() { public Object m() { return CORRECT; } }); //  Gn <: G.m <: C <: I.m DEFAULT
            assertCompiled();
        }
    }

    public static class InheritedDefault extends ATest<InheritedDefault.C> {
        public InheritedDefault() {
            super(C.class, D.class);
        }

        interface I           { Object m(); }
        interface J extends I { default Object m() { return CORRECT; } }

        static abstract class C implements I { /* inherits I.m ABSTRACT */}

        // NB! The class is marked abstract to avoid abstract_with_unique_concrete_subtype dependency
        static abstract class D extends C implements J { /* inherits J.m DEFAULT*/ }

        static abstract class E1 extends C { /* empty */ }
        static abstract class E2 extends C { public abstract Object m(); }
        static abstract class E3 extends C { public Object m() { return "E3.m"; } }

        interface I1 extends I { Object m(); }
        interface I2 extends I { default Object m() { return "I2.m"; } }

        static abstract class F1 extends C implements I1 { }
        static abstract class F2 extends C implements I2 { }

        interface K extends I { default Object m() { return CORRECT; } }
        static class G extends C implements K { /* inherits K.m DEFAULT */ }

        @Override
        public Object test(C obj) {
            return obj.m(); // invokevirtual C.m()
        }

        @Override
        public void checkInvalidReceiver() {
            // nothing to do: concrete class types are enforced by the verifier
        }

        @TestCase
        public void test() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // Dn <: D.m <: C <: I.m ABSTRACT, J.m DEFAULT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = C, method = D.m

            // 1. No invalidation: abstract classes don't participate in CHA.
            initialize(E1.class,  // ABSTRACT E1            <: C <: I.m ABSTRACT
                       E2.class,  // ABSTRACT E2.m ABSTRACT <: C <: I.m ABSTRACT
                       E3.class,  // ABSTRACT E3.m          <: C <: I.m ABSTRACT
                       F1.class,  // ABSTRACT F1            <: C <: I.m ABSTRACT, I1.m ABSTRACT
                       F2.class); // ABSTRACT F2            <: C <: I.m ABSTRACT, I2.m DEFAULT
            assertCompiled();

            // 2. No invalidation: not yet linked classes don't participate in CHA.
            load(G.class);
            assertCompiled();

            // 3. Dependency invalidation: G.m <: C <: I.m DEFAULT
            initialize(G.class);
            assertNotCompiled();

            // 4. Recompilation: no inlining, no dependencies
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; } }); //  Cn.m <: C <: I.m DEFAULT
            call(new G() { public Object m() { return CORRECT; } }); //  Gn <: G.m <: C <: I.m DEFAULT
            assertCompiled();
        }
    }
}
