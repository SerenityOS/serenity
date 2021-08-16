/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 *                   -XX:CompileCommand=compileonly,*::testHelper -XX:CompileCommand=inline,*::testHelper
 *                   -Xbatch -Xmixed -XX:+WhiteBoxAPI
 *                   -XX:-TieredCompilation
 *                      compiler.cha.StrengthReduceInterfaceCall
 *
 * @run main/othervm -Xbootclasspath/a:. -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+PrintCompilation -XX:+PrintInlining -XX:+TraceDependencies -verbose:class -XX:CompileCommand=quiet
 *                   -XX:CompileCommand=compileonly,*::m
 *                   -XX:CompileCommand=compileonly,*::test -XX:CompileCommand=dontinline,*::test
 *                   -XX:CompileCommand=compileonly,*::testHelper -XX:CompileCommand=inline,*::testHelper
 *                   -Xbatch -Xmixed -XX:+WhiteBoxAPI
 *                   -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                      compiler.cha.StrengthReduceInterfaceCall
 */
package compiler.cha;

import jdk.internal.vm.annotation.DontInline;

import static compiler.cha.Utils.*;

public class StrengthReduceInterfaceCall {
    public static void main(String[] args) {
        run(ObjectToString.class);
        run(ObjectHashCode.class);
        run(TwoLevelHierarchyLinear.class);
        run(ThreeLevelHierarchyLinear.class);
        run(ThreeLevelHierarchyAbstractVsDefault.class);
        run(ThreeLevelDefaultHierarchy.class);
        run(ThreeLevelDefaultHierarchy1.class);
        System.out.println("TEST PASSED");
    }

    public static class ObjectToString extends ATest<ObjectToString.I> {
        public ObjectToString() { super(I.class, C.class); }

        interface J           { String toString(); }
        interface I extends J {}

        static class C implements I {}

        interface K1 extends I {}
        interface K2 extends I { String toString(); } // K2.tS() ABSTRACT
        // interface K3 extends I { default String toString() { return "K3"; } // K2.tS() DEFAULT

        static class D implements I { public String toString() { return "D"; }}

        static class DJ1 implements J {}
        static class DJ2 implements J { public String toString() { return "DJ2"; }}

        @Override
        public Object test(I i) { return ObjectToStringHelper.testHelper(i); /* invokeinterface I.toString() */ }

        @TestCase
        public void testMono() {
            // 0. Trigger compilation of a monomorphic call site
            compile(monomophic()); // C1 <: C <: intf I <: intf J <: Object.toString()
            assertCompiled();

            // Dependency: none

            call(new C() { public String toString() { return "Cn"; }}); // Cn.tS <: C.tS <: intf I
            assertCompiled();
        }

        @TestCase
        public void testBi() {
            // 0. Trigger compilation of a bimorphic call site
            compile(bimorphic()); // C1 <: C <: intf I <: intf J <: Object.toString()
            assertCompiled();

            // Dependency: none

            call(new C() { public String toString() { return "Cn"; }}); // Cn.tS <: C.tS <: intf I
            assertCompiled();
        }

        @TestCase
        public void testMega() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C <: intf I <: intf J <: Object.toString()
            assertCompiled();

            // Dependency: none
            // compiler.cha.StrengthReduceInterfaceCall$ObjectToString::test (5 bytes)
            //     @ 1   compiler.cha.StrengthReduceInterfaceCall$ObjectToStringHelper::test (7 bytes)   inline (hot)
            //       @ 1   java.lang.Object::toString (36 bytes)   virtual call

            // No dependency - no invalidation
            repeat(100, () -> call(new C(){})); // Cn <: C <: intf I
            assertCompiled();

            initialize(K1.class,   // intf  K1             <: intf I <: intf J
                       K2.class,   // intf  K2.tS ABSTRACT <: intf I <: intf J
                       DJ1.class,  //      DJ1                       <: intf J
                       DJ2.class); //      DJ2.tS                    <: intf J
            assertCompiled();

            initialize(D.class); // D.tS <: intf I <: intf J
            assertCompiled();

            call(new C() { public String toString() { return "Cn"; }}); // Cn.tS <: C.tS <: intf I
            assertCompiled();
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ObjectHashCode extends ATest<ObjectHashCode.I> {
        public ObjectHashCode() { super(I.class, C.class); }

        interface J {}
        interface I extends J {}

        static class C implements I {}

        interface K1 extends I {}
        interface K2 extends I { int hashCode(); } // K2.hC() ABSTRACT
        // interface K3 extends I { default int hashCode() { return CORRECT; } // K2.hC() DEFAULT

        static class D implements I { public int hashCode() { return super.hashCode(); }}

        static class DJ1 implements J {}
        static class DJ2 implements J { public int hashCode() { return super.hashCode(); }}

        @Override
        public Object test(I i) {
            return ObjectHashCodeHelper.testHelper(i); /* invokeinterface I.hashCode() */
        }

        @TestCase
        public void testMono() {
            // 0. Trigger compilation of a monomorphic call site
            compile(monomophic()); // C1 <: C <: intf I <: intf J <: Object.hashCode()
            assertCompiled();

            // Dependency: none

            call(new C() { public int hashCode() { return super.hashCode(); }}); // Cn.hC <: C.hC <: intf I
            assertCompiled();
        }

        @TestCase
        public void testBi() {
            // 0. Trigger compilation of a bimorphic call site
            compile(bimorphic()); // C1 <: C <: intf I <: intf J <: Object.toString()
            assertCompiled();

            // Dependency: none

            call(new C() { public int hashCode() { return super.hashCode(); }}); // Cn.hC <: C.hC <: intf I
            assertCompiled();
        }

        @TestCase
        public void testMega() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C <: intf I <: intf J <: Object.hashCode()
            assertCompiled();

            // Dependency: none

            // No dependency - no invalidation
            repeat(100, () -> call(new C(){})); // Cn <: C <: intf I
            assertCompiled();

            initialize(K1.class,   // intf  K1             <: intf I <: intf J
                       K2.class,   // intf  K2.hC ABSTRACT <: intf I <: intf J
                       DJ1.class,  //      DJ1                       <: intf J
                       DJ2.class); //      DJ2.hC                    <: intf J
            assertCompiled();

            initialize(D.class); // D.hC <: intf I <: intf J
            assertCompiled();

            call(new C() { public int hashCode() { return super.hashCode(); }}); // Cn.hC <: C.hC <: intf I
            assertCompiled();
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }

    public static class TwoLevelHierarchyLinear extends ATest<TwoLevelHierarchyLinear.I> {
        public TwoLevelHierarchyLinear() { super(I.class, C.class); }

        interface J { default Object m() { return WRONG; } }

        interface I extends J { Object m(); }
        static class C implements I { public Object m() { return CORRECT; }}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends I { default Object m() { return WRONG;   }}
        interface K4 extends I { default Object m() { return CORRECT; }}

        static class D implements I { public Object m() { return WRONG;   }}

        static class DJ1 implements J {}
        static class DJ2 implements J { public Object m() { return WRONG; }}

        @DontInline
        public Object test(I i) {
            return i.m();
        }

        @TestCase
        public void testMega1() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m ABSTRACT <: intf J.m ABSTRACT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check is preserved

            // 1. No deoptimization/invalidation on not-yet-seen receiver
            repeat(100, () -> call(new C(){})); // Cn <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            // 2. No dependency invalidation on class loading of unrelated classes: different context
            initialize(K1.class,   // intf  K1            <: intf I.m ABSTRACT <: intf J.m DEFAULT
                       K2.class,   // intf  K2.m ABSTRACT <: intf I.m ABSTRACT <: intf J.m DEFAULT
                       DJ1.class,  //      DJ1                                 <: intf J.m DEFAULT
                       DJ2.class); //      DJ2.m                               <: intf J.m DEFAULT
            assertCompiled();

            // 3. Dependency invalidation on D <: I
            initialize(D.class); // D.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertNotCompiled();

            // 4. Recompilation: no inlining, no dependencies
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
        }

        @TestCase
        public void testMega2() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No invalidation: interfaces don't participate in CHA.
            initialize(K3.class); // intf K3.m DEFAULT <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            // 2. Dependency invalidation on K3n <: I with concrete method.
            call(new K3() { public Object m() { return CORRECT; }}); // K3n.m <: intf K3.m DEFAULT <: intf I.m ABSTRACT <: intf J.m ABSTRACT
            assertNotCompiled();

            // 3. Recompilation: no inlining, no dependencies
            compile(megamorphic());
            call(new K3() { public Object m() { return CORRECT; }}); // K3n.m <: intf K3.m DEFAULT  <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
        }

        @TestCase
        public void testMega3() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No invalidation: interfaces don't participate in CHA.
            initialize(K4.class); // intf K4.m DEFAULT <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            // 2. Dependency invalidation on K4n <: I with default method.
            call(new K4() { /* default method K4.m */ }); // K4n <: intf K4.m DEFAULT <: intf I.m ABSTRACT <: intf J.m ABSTRACT
            assertNotCompiled();

            // 3. Recompilation: no inlining, no dependencies
            compile(megamorphic());
            call(new K4() { /* default method K4.m */  }); // K4n <: intf K3.m DEFAULT  <: intf I.m ABSTRACT <: intf J.m DEFAULT
            assertCompiled();

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelHierarchyLinear extends ATest<ThreeLevelHierarchyLinear.I> {
        public ThreeLevelHierarchyLinear() { super(I.class, C.class); }

        interface J           { Object m(); }
        interface I extends J {}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends I { default Object m() { return WRONG;   }}
        interface K4 extends I { default Object m() { return CORRECT; }}

        static class C  implements I { public Object m() { return CORRECT; }}

        static class DI implements I { public Object m() { return WRONG;   }}
        static class DJ implements J { public Object m() { return WRONG;   }}

        @DontInline
        public Object test(I i) {
            return i.m(); // I <: J.m ABSTRACT
        }

        @TestCase
        public void testMega1() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No deoptimization/invalidation on not-yet-seen receiver
            repeat(100, () -> call(new C(){})); // Cn <: C.m <: intf I
            assertCompiled(); // No deopt on not-yet-seen receiver

            // 2. No dependency invalidation: different context
            initialize(DJ.class,  //      DJ.m                    <: intf J.m ABSTRACT
                       K1.class,  // intf K1            <: intf I <: intf J.m ABSTRACT
                       K2.class); // intf K2.m ABSTRACT <: intf I <: intf J.m ABSTRACT
            assertCompiled();

            // 3. Dependency invalidation: DI.m <: I
            initialize(DI.class); //      DI.m          <: intf I <: intf J.m ABSTRACT
            assertNotCompiled();

            // 4. Recompilation w/o a dependency
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled(); // no dependency

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
        }

        @TestCase
        public void testMega2() {
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // No invalidation: interfaces don't participate in CHA.
            initialize(K3.class); // intf K3.m DEFAULT <: intf I;
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            call(new K3() { public Object m() { return CORRECT; }}); // Kn.m <: K3.m DEFAULT <: intf I <: intf J.m ABSTRACT
            assertNotCompiled();

            // Recompilation w/o a dependency
            compile(megamorphic());
            // Dependency: none
            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
            call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled();
        }

        @TestCase
        public void testMega3() {
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // No invalidation: interfaces don't participate in CHA.
            initialize(K4.class); // intf K3.m DEFAULT <: intf I;
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            call(new K4() { /* default method K4.m */ }); // K4n <: K4.m DEFAULT <: intf I <: intf J.m ABSTRACT
            assertNotCompiled();

            // Recompilation w/o a dependency
            compile(megamorphic());
            // Dependency: none
            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
            call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled();
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() { public Object m() { return WRONG; }}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelHierarchyAbstractVsDefault extends ATest<ThreeLevelHierarchyAbstractVsDefault.I> {
        public ThreeLevelHierarchyAbstractVsDefault() { super(I.class, C.class); }

        interface J1                { default Object m() { return WRONG; } } // intf J1.m DEFAULT
        interface J2 extends J1     { Object m(); }                          // intf J2.m ABSTRACT <: intf J1
        interface I  extends J1, J2 {}                                       // intf  I.m OVERPASS <: intf J1,J2

        static class C  implements I { public Object m() { return CORRECT; }}

        @DontInline
        public Object test(I i) {
            return i.m(); // intf I.m OVERPASS
        }

        static class DI implements I { public Object m() { return WRONG;   }}

        static class DJ11 implements J1 {}
        static class DJ12 implements J1 { public Object m() { return WRONG; }}

        static class DJ2 implements J2 { public Object m() { return WRONG;   }}

        interface K11 extends J1 {}
        interface K12 extends J1 { Object m(); }
        interface K13 extends J1 { default Object m() { return WRONG; }}
        interface K21 extends J2 {}
        interface K22 extends J2 { Object m(); }
        interface K23 extends J2 { default Object m() { return WRONG; }}


        public void testMega1() {
            // 0. Trigger compilation of megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I.m OVERPASS <: intf J2.m ABSTRACT <: intf J1.m DEFAULT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No deopt/invalidation on not-yet-seen receiver
            repeat(100, () -> call(new C(){})); // Cn <: C.m <: intf I.m OVERPASS <: intf J2.m ABSTRACT <: intf J1.m DEFAULT
            assertCompiled();

            // 2. No dependency invalidation: different context
            initialize(K11.class, K12.class, K13.class,
                       K21.class, K22.class, K23.class);

            // 3. Dependency invalidation: Cn.m <: C <: I
            call(new C() { public Object m() { return CORRECT; }}); // Cn.m <: C.m <: intf I.m OVERPASS <: intf J2.m ABSTRACT <: intf J1.m DEFAULT
            assertNotCompiled();

            // 4. Recompilation w/o a dependency
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); // no inlining

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
        }

        public void testMega2() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic());
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No dependency invalidation: different context
            initialize(DJ11.class,
                       DJ12.class,
                       DJ2.class);
            assertCompiled();

            // 2. Dependency invalidation: DI.m <: I
            initialize(DI.class);
            assertNotCompiled();

            // 3. Recompilation w/o a dependency
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); // no inlining

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J1() {}); // super interface
                test(j);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J2() { public Object m() { return WRONG; }}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelDefaultHierarchy extends ATest<ThreeLevelDefaultHierarchy.I> {
        public ThreeLevelDefaultHierarchy() { super(I.class, C.class); }

        interface J           { default Object m() { return WRONG; }}
        interface I extends J {}

        static class C  implements I { public Object m() { return CORRECT; }}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends J { default Object m() { return WRONG; }}

        static class DI implements I { public Object m() { return WRONG; }}
        static class DJ implements J { public Object m() { return WRONG; }}
        static class DK3 implements K3 {}

        @DontInline
        public Object test(I i) {
            return i.m();
        }

        @TestCase
        public void testMega() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic()); // C1,C2,C3 <: C.m <: intf I <: intf J.m ABSTRACT
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No deoptimization/invalidation on not-yet-seen receiver
            repeat(100, () -> call(new C() {}));
            assertCompiled();

            // 2. No dependency invalidation
            initialize(DJ.class,    //      DJ.m                               <: intf J.m ABSTRACT
                       K1.class,   // intf  K1            <: intf I            <: intf J.m ABSTRACT
                       K2.class,   // intf  K2.m ABSTRACT <: intf I            <: intf J.m ABSTRACT
                       DK3.class); //      DK3.m          <: intf K3.m DEFAULT <: intf J.m ABSTRACT
            assertCompiled();

            // 3. Dependency invalidation
            initialize(DI.class); // DI.m <: intf I <: intf J.m ABSTRACT
            assertNotCompiled();

            // 4. Recompilation w/o a dependency
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); // no inlining
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J() {}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }

    public static class ThreeLevelDefaultHierarchy1 extends ATest<ThreeLevelDefaultHierarchy1.I> {
        public ThreeLevelDefaultHierarchy1() { super(I.class, C.class); }

        interface J1                { Object m();}
        interface J2 extends J1     { default Object m() { return WRONG; }  }
        interface I  extends J1, J2 {}

        static class C  implements I { public Object m() { return CORRECT; }}

        interface K1 extends I {}
        interface K2 extends I { Object m(); }
        interface K3 extends I { default Object m() { return WRONG; }}

        static class DI implements I { public Object m() { return WRONG; }}
        static class DJ1 implements J1 { public Object m() { return WRONG; }}
        static class DJ2 implements J2 { public Object m() { return WRONG; }}

        @DontInline
        public Object test(I i) {
            return i.m();
        }

        @TestCase
        public void testMega() {
            // 0. Trigger compilation of a megamorphic call site
            compile(megamorphic());
            assertCompiled();

            // Dependency: type = unique_concrete_method, context = I, method = C.m

            checkInvalidReceiver(); // ensure proper type check on receiver is preserved

            // 1. No deoptimization/invalidation on not-yet-seen receiver
            repeat(100, () -> call(new C() {}));
            assertCompiled();

            // 2. No dependency invalidation
            initialize(DJ1.class,
                       DJ2.class,
                       K1.class,
                       K2.class,
                       K3.class);
            assertCompiled();

            // 3. Dependency invalidation
            initialize(DI.class); // DI.m <: intf I <: intf J.m ABSTRACT
            assertNotCompiled();

            // 4. Recompilation w/o a dependency
            compile(megamorphic());
            call(new C() { public Object m() { return CORRECT; }});
            assertCompiled(); // no inlining
        }

        @Override
        public void checkInvalidReceiver() {
            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I o = (I) unsafeCastMH(I.class).invokeExact(new Object()); // unrelated
                test(o);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J1() { public Object m() { return WRONG; } }); // super interface
                test(j);
            });
            assertCompiled();

            shouldThrow(IncompatibleClassChangeError.class, () -> {
                I j = (I) unsafeCastMH(I.class).invokeExact((Object)new J2() {}); // super interface
                test(j);
            });
            assertCompiled();
        }
    }
}
