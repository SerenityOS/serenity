/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046171
 * @summary Test the various rules for nest members and nest-hosts by
 * triggering nestmate access checks on all possible paths
 * @compile TestNestmateMembership.java
 *          PackagedNestHost.java
 *          PackagedNestHost2.java
 *          InvalidNestHost.java
 *
 * @compile TargetNoHost.jcod
 *          CallerNoHost.jcod
 *          TargetSelfHost.jcod
 *          CallerSelfHost.jcod
 *          TargetMissingHost.jcod
 *          CallerMissingHost.jcod
 *          TargetNotInstanceHost.jcod
 *          CallerNotInstanceHost.jcod
 *          TargetNotOurHost.jcod
 *          CallerNotOurHost.jcod
 *          PackagedNestHost.jcod
 *          PackagedNestHost2Member.jcod
 *          PackagedNestHostMember.jcod
 *
 * @run main/othervm TestNestmateMembership method
 * @run main/othervm TestNestmateMembership constructor
 * @run main/othervm TestNestmateMembership getField
 * @run main/othervm TestNestmateMembership putField
 * @run main/othervm -Xcomp TestNestmateMembership getField
 */

// We test all the "illegal" relationships between a nest member and its nest-host
// except for the case where the name of the nest-member matches the name listed
// in the nest-host, but resolves to a different class. There doesn't seem to
// be a way to construct that scenario.
// For each nested class below there is a corresponding .jcod file which breaks one
// of the rules regarding nest membership. For the package related tests we have
// additional PackageNestHost*.java sources.[1]
//
// Note that all the .java files must be compiled in the same step, while all
// .jcod files must be compiled in a later step.

// We test all the different nestmate access check paths: method invocation, constructor
// invocations, field get and field put. The test is invoked four times with each using
// a different test mode. Plus an extra Xcomp run for field access to hit ciField path.
//
// As access checking requires resolution and validation of the nest-host of
// both the caller class and the target class, we must check that all
// combinations of good/bad caller/target are checked for each of the
// possible errors:
// - no nest-host attribute
// - nest-host refers to self
// - nest-host class can not be found
// - nest-host class is not an instance class (but is in same package)
// - class is not a member of nest-host's nest (but is in same package)
// - class and nest-host are in different packages
//
// To provide coverage for reflection and MethodHandle paths through
// JVM_AreNestmates, we add reflection/MH accesses to a subset of the tests.
// We only need to test one case (for Caller.xxx) as all cases use the same path; further
// we don't need to test all failure cases, as all exceptions are equivalent in that regard,
// but for good measure we test the four basic error situations (eliding the different
// package test for simplicity).
//
// [1] In earlier versions the package-test was the final check done in nest membership
//     validation, so we needed actual test classes in different packages that claimed
//     membership. The final spec requires the package test to be done first, so it can
//     be trivially tested by using Object as the nest-host. But we leave the explicit
//     package tests as they are, and adjust the other tests so that a "bad host" is
//     always in the same package.

import java.lang.invoke.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

public class TestNestmateMembership {

   static final MethodType VOID_T = MethodType.methodType(void.class);

    static class Caller {

        private Caller() {}

        private static void m() {
            System.out.println("Caller.m()");
        }

        // direct static method invocations

        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNoHost() {
            TargetNoHost.m();
        }
        public static void invokeTargetSelfHost() {
            TargetSelfHost.m();
        }
        public static void invokeTargetMissingHost() {
            TargetMissingHost.m();
        }
        public static void invokeTargetNotInstanceHost() {
            TargetNotInstanceHost.m();
        }
        public static void invokeTargetNotOurHost() {
            TargetNotOurHost.m();
        }

        // reflective static method invocations

        public static void invokeTargetNoHostReflectively() throws Throwable {
            TargetNoHost.class.getDeclaredMethod("m", new Class<?>[0]).invoke(null, new Object[0]);
        }
        public static void invokeTargetSelfHostReflectively() throws Throwable {
            TargetSelfHost.class.getDeclaredMethod("m", new Class<?>[0]).invoke(null, new Object[0]);
        }
        public static void invokeTargetMissingHostReflectively() throws Throwable {
            TargetMissingHost.class.getDeclaredMethod("m", new Class<?>[0]).invoke(null, new Object[0]);
        }
        public static void invokeTargetNotInstanceHostReflectively() throws Throwable {
            TargetNotInstanceHost.class.getDeclaredMethod("m", new Class<?>[0]).invoke(null, new Object[0]);
        }
        public static void invokeTargetNotOurHostReflectively() throws Throwable {
            TargetNotOurHost.class.getDeclaredMethod("m", new Class<?>[0]).invoke(null, new Object[0]);
        }

        // MethodHandle static method lookup (no invoke as the lookup should fail)

        public static void invokeTargetNoHostMH() throws Throwable {
            MethodHandle mh = lookup().findStatic(TargetNoHost.class, "m", VOID_T);
        }
        public static void invokeTargetSelfHostMH() throws Throwable {
            MethodHandle mh = lookup().findStatic(TargetSelfHost.class, "m", VOID_T);
        }
        public static void invokeTargetMissingHostMH() throws Throwable {
            MethodHandle mh = lookup().findStatic(TargetMissingHost.class, "m", VOID_T);
        }
        public static void invokeTargetNotInstanceHostMH() throws Throwable {
            MethodHandle mh = lookup().findStatic(TargetNotInstanceHost.class, "m", VOID_T);
        }
        public static void invokeTargetNotOurHostMH() throws Throwable {
            MethodHandle mh = lookup().findStatic(TargetNotOurHost.class, "m", VOID_T);
        }


        // direct constructor invocations

        public static void newTarget() {
            Object o = new Target();
        }
        public static void newTargetNoHost() {
            Object o = new TargetNoHost();
        }
        public static void newTargetSelfHost() {
            Object o = new TargetSelfHost();
        }
        public static void newTargetMissingHost() {
            Object o = new TargetMissingHost();
        }
        public static void newTargetNotInstanceHost() {
            Object o = new TargetNotInstanceHost();
        }
        public static void newTargetNotOurHost() {
            Object o = new TargetNotOurHost();
        }

        // reflective constructor invocations

        public static void newTargetNoHostReflectively() throws Throwable {
            Object o = TargetNoHost.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        public static void newTargetSelfHostReflectively() throws Throwable {
            Object o = TargetSelfHost.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        public static void newTargetMissingHostReflectively() throws Throwable {
            Object o = TargetMissingHost.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        public static void newTargetNotInstanceHostReflectively() throws Throwable {
            Object o = TargetNotInstanceHost.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }
        public static void newTargetNotOurHostReflectively() throws Throwable {
            Object o = TargetNotOurHost.class.getDeclaredConstructor(new Class<?>[0]).newInstance(new Object[0]);
        }

        // MethodHandle constructor lookup (no invoke as the lookup should fail)

        public static void newTargetNoHostMH() throws Throwable {
            MethodHandle mh = lookup().findConstructor(TargetNoHost.class, VOID_T);
        }
        public static void newTargetSelfHostMH() throws Throwable {
            MethodHandle mh = lookup().findConstructor(TargetSelfHost.class, VOID_T);
        }
        public static void newTargetMissingHostMH() throws Throwable {
            MethodHandle mh = lookup().findConstructor(TargetMissingHost.class, VOID_T);
        }
        public static void newTargetNotInstanceHostMH() throws Throwable {
            MethodHandle mh = lookup().findConstructor(TargetNotInstanceHost.class, VOID_T);
        }
        public static void newTargetNotOurHostMH() throws Throwable {
            MethodHandle mh = lookup().findConstructor(TargetNotOurHost.class, VOID_T);
        }

        private static int f;

        // direct field accesses

        public static void getFieldTarget() {
            int x = Target.f;
        }
        public static void getFieldTargetNoHost() {
            int x = TargetNoHost.f;
        }
        public static void getFieldTargetSelfHost() {
            int x = TargetSelfHost.f;
        }
        public static void getFieldTargetMissingHost() {
            int x = TargetMissingHost.f;
        }
        public static void getFieldTargetNotInstanceHost() {
            int x = TargetNotInstanceHost.f;
        }
        public static void getFieldTargetNotOurHost() {
            int x = TargetNotOurHost.f;
        }

        public static void putFieldTarget() {
            Target.f = 42;
        }
        public static void putFieldTargetNoHost() {
            TargetNoHost.f = 42;
        }
        public static void putFieldTargetSelfHost() {
            TargetSelfHost.f = 42;
        }
        public static void putFieldTargetMissingHost() {
            TargetMissingHost.f = 42;
        }
        public static void putFieldTargetNotInstanceHost() {
            TargetNotInstanceHost.f = 42;
        }
        public static void putFieldTargetNotOurHost() {
            TargetNotOurHost.f = 42;
        }

        // reflective field accesses

        public static void getFieldTargetNoHostReflectively() throws Throwable {
            int x = TargetNoHost.class.getDeclaredField("f").getInt(null);
        }
        public static void getFieldTargetSelfHostReflectively() throws Throwable {
            int x = TargetSelfHost.class.getDeclaredField("f").getInt(null);
        }
        public static void getFieldTargetMissingHostReflectively() throws Throwable {
            int x = TargetMissingHost.class.getDeclaredField("f").getInt(null);
        }
        public static void getFieldTargetNotInstanceHostReflectively() throws Throwable {
            int x = TargetNotInstanceHost.class.getDeclaredField("f").getInt(null);
        }
        public static void getFieldTargetNotOurHostReflectively() throws Throwable {
            int x = TargetNotOurHost.class.getDeclaredField("f").getInt(null);
        }

        public static void putFieldTargetNoHostReflectively() throws Throwable {
            TargetNoHost.class.getDeclaredField("f").setInt(null, 42);
        }
        public static void putFieldTargetSelfHostReflectively() throws Throwable {
            TargetSelfHost.class.getDeclaredField("f").setInt(null, 42);
        }
        public static void putFieldTargetMissingHostReflectively() throws Throwable {
            TargetMissingHost.class.getDeclaredField("f").setInt(null, 42);
        }
        public static void putFieldTargetNotInstanceHostReflectively() throws Throwable {
            TargetNotInstanceHost.class.getDeclaredField("f").setInt(null, 42);
        }
        public static void putFieldTargetNotOurHostReflectively() throws Throwable {
            TargetNotOurHost.class.getDeclaredField("f").setInt(null, 42);
        }

        // MethodHandle field lookup (no access as the lookup will fail)

        public static void getFieldTargetNoHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(TargetNoHost.class, "f", int.class);
        }
        public static void getFieldTargetSelfHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(TargetSelfHost.class, "f", int.class);
        }
        public static void getFieldTargetMissingHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(TargetMissingHost.class, "f", int.class);
        }
        public static void getFieldTargetNotInstanceHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(TargetNotInstanceHost.class, "f", int.class);
        }
        public static void getFieldTargetNotOurHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticGetter(TargetNotOurHost.class, "f", int.class);
        }

        public static void putFieldTargetNoHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticSetter(TargetNoHost.class, "f", int.class);
        }
        public static void putFieldTargetSelfHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticSetter(TargetSelfHost.class, "f", int.class);
        }
        public static void putFieldTargetMissingHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticSetter(TargetMissingHost.class, "f", int.class);
        }
        public static void putFieldTargetNotInstanceHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticSetter(TargetNotInstanceHost.class, "f", int.class);
        }
        public static void putFieldTargetNotOurHostMH() throws Throwable {
            MethodHandle mh = lookup().findStaticSetter(TargetNotOurHost.class, "f", int.class);
        }

    }

    static class CallerNoHost {

        // method invocations

        private static void m() {
            System.out.println("CallerNoHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNoHost() {
            TargetNoHost.m();
        }

        // constructor invocations

        private CallerNoHost() {}

        public static void newTarget() {
            Object o = new Target();
        }
        public static void newTargetNoHost() {
            Object o = new TargetNoHost();
        }

        // field accesses

        private static int f;

        public static void getFieldTarget() {
            int x = Target.f;
        }
        public static void getFieldTargetNoHost() {
            int x = TargetNoHost.f;
        }

        public static void putFieldTarget() {
            Target.f = 42;
        }
        public static void putFieldTargetNoHost() {
            TargetNoHost.f = 42;
        }

    }

    static class CallerSelfHost {

        // method invocations

        private static void m() {
            System.out.println("CallerSelfHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetSelfHost() {
            TargetSelfHost.m();
        }

        // constructor invocations

        private CallerSelfHost() {}

        public static void newTarget() {
            Object o = new Target();
        }
        public static void newTargetSelfHost() {
            Object o = new TargetSelfHost();
        }

        // field accesses

        private static int f;

        public static void getFieldTarget() {
            int x = Target.f;
        }
        public static void getFieldTargetSelfHost() {
            int x = TargetSelfHost.f;
        }

        public static void putFieldTarget() {
            Target.f = 42;
        }
        public static void putFieldTargetSelfHost() {
            TargetSelfHost.f = 42;
        }

    }

    static class CallerMissingHost {
        String msg = "NoCallerMissingHost"; // for cp entry

        // method invocations

        private static void m() {
            System.out.println("CallerMissingHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetMissingHost() {
            TargetMissingHost.m();
        }

        // constructor invocations

        private CallerMissingHost() {}

        public static void newTarget() {
            Object o = new Target();
        }
        public static void newTargetMissingHost() {
            Object o = new TargetMissingHost();
        }

        // field accesses

        private static int f;

        public static void getFieldTarget() {
            int x = Target.f;
        }
        public static void getFieldTargetMissingHost() {
            int x = TargetMissingHost.f;
        }
        public static void putFieldTarget() {
            Target.f = 42;
        }
        public static void putFieldTargetMissingHost() {
            TargetMissingHost.f = 42;
        }

    }

    static class CallerNotInstanceHost {
        Object[] oa; // create CP entry to use in jcod change

        // method invocations

        private static void m() {
            System.out.println("CallerNotInstanceHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNotInstanceHost() {
            TargetNotInstanceHost.m();
        }

        // constructor invocations

        private CallerNotInstanceHost() {}

        public static void newTarget() {
            Object o = new Target();
        }
        public static void newTargetNotInstanceHost() {
            Object o = new TargetNotInstanceHost();
        }

        // field accesses

        private static int f;

        public static void getFieldTarget() {
            int x = Target.f;
        }
        public static void getFieldTargetNotInstanceHost() {
            int x = TargetNotInstanceHost.f;
        }
        public static void putFieldTarget() {
            Target.f = 42;
        }
        public static void putFieldTargetNotInstanceHost() {
            TargetNotInstanceHost.f = 42;
        }
    }

    static class CallerNotOurHost {

        // method invocations

        private static void m() {
            System.out.println("CallerNotOurHost.m() - java version");
        }
        public static void invokeTarget() {
            Target.m();
        }
        public static void invokeTargetNotOurHost() {
            TargetNotOurHost.m();
        }

        // constructor invocations

        private CallerNotOurHost() {}

        public static void newTarget() {
            Object o = new Target();
        }
        public static void newTargetNotOurHost() {
            Object o = new TargetNotOurHost();
        }

        // field accesses

        private static int f;

        public static void getFieldTarget() {
            int x = Target.f;
        }
        public static void getFieldTargetNotOurHost() {
            int x = TargetNotOurHost.f;
        }
        public static void putFieldTarget() {
            Target.f = 42;
        }
        public static void putFieldTargetNotOurHost() {
            TargetNotOurHost.f = 42;
        }

    }

    static class Target {
        private Target() {}
        private static int f;
        private static void m() {
            System.out.println("Target.m()");
        }
    }

    static class TargetNoHost {
        private TargetNoHost() {}
        private static int f;
        private static void m() {
            System.out.println("TargetNoHost.m() - java version");
        }
    }

    static class TargetSelfHost {
        private TargetSelfHost() {}
        private static int f;
        private static void m() {
            System.out.println("TargetSelfHost.m() - java version");
        }
    }

    static class TargetMissingHost {
        String msg = "NoTargetMissingHost";  // for cp entry
        private TargetMissingHost() {}
        private static int f;
        private static void m() {
            System.out.println("TargetMissingHost.m() - java version");
        }
    }

    static class TargetNotInstanceHost {
        Object[] oa; // create CP entry to use in jcod change
        private TargetNotInstanceHost() {}
        private static int f;
        private static void m() {
            System.out.println("TargetNotInstanceHost.m() - java version");
        }
    }

    static class TargetNotOurHost {
        private TargetNotOurHost() {}
        private static int f;
        private static void m() {
            System.out.println("TargetNotOurHost.m() - java version");
        }
    }

    public static void main(String[] args) throws Throwable {
        if (args.length < 1) {
            throw new Error("Test mode argument must be one of: method, constructor, getField or putField");
        }
        switch(args[0]) {
        case "method":
            System.out.println("TESTING METHOD INVOCATIONS:");
            test_GoodInvoke();
            test_NoHostInvoke();
            test_SelfHostInvoke();
            test_MissingHostInvoke();
            test_NotInstanceHostInvoke();
            test_NotOurHostInvoke();
            test_WrongPackageHostInvoke();
            break;
        case "constructor":
            System.out.println("TESTING CONSTRUCTOR INVOCATIONS:");
            test_GoodConstruct();
            test_NoHostConstruct();
            test_SelfHostConstruct();
            test_MissingHostConstruct();
            test_NotInstanceHostConstruct();
            test_NotOurHostConstruct();
            test_WrongPackageHostConstruct();
            break;
        case "getField":
            System.out.println("TESTING GETFIELD INVOCATIONS:");
            test_GoodGetField();
            test_NoHostGetField();
            test_SelfHostGetField();
            test_MissingHostGetField();
            test_NotInstanceHostGetField();
            test_NotOurHostGetField();
            test_WrongPackageHostGetField();
            break;
        case "putField":
            System.out.println("TESTING PUTFIELD INVOCATIONS:");
            test_GoodPutField();
            test_NoHostPutField();
            test_SelfHostPutField();
            test_MissingHostPutField();
            test_NotInstanceHostPutField();
            test_NotOurHostPutField();
            test_WrongPackageHostPutField();
            break;
        default:
            throw new Error("Uknown mode: " + args[0] +
                            ". Must be one of: method, constructor, getField or putField");
        }
    }

    static void test_GoodInvoke(){
        try {
            Caller.invokeTarget();
        }
        catch (Exception e) {
            throw new Error("Unexpected exception on good invocation " + e);
        }
    }

    static void test_NoHostInvoke() throws Throwable {
        System.out.println("Testing for missing nest-host attribute");
        String msg = "class TestNestmateMembership$Caller tried to access " +
            "private method 'void TestNestmateMembership$TargetNoHost.m()'";
        try {
            Caller.invokeTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNoHost with modifiers \"private static\"";
        try {
            Caller.invokeTargetNoHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such method: TestNestmateMembership$TargetNoHost.m()void/invokeStatic";
        try {
            Caller.invokeTargetNoHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "method 'void TestNestmateMembership$Target.m()'";
        try {
            CallerNoHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "method 'void TestNestmateMembership$TargetNoHost.m()'";
        try {
            CallerNoHost.invokeTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_SelfHostInvoke() throws Throwable {
        System.out.println("Testing for class that lists itself as nest-host");
        String msg = "Type TestNestmateMembership$TargetSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$TargetSelfHost (loader: 'app'): current type is not listed as a nest member)";
        try {
            Caller.invokeTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetSelfHost with modifiers \"private static\"";
        try {
            Caller.invokeTargetSelfHostReflectively();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such method: TestNestmateMembership$TargetSelfHost.m()void/invokeStatic";
        try {
            Caller.invokeTargetSelfHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$CallerSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerSelfHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerSelfHost.invokeTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_MissingHostInvoke() throws Throwable {
        System.out.println("Testing for nest-host class that does not exist");
        String msg = "Nest host resolution of TestNestmateMembership$TargetMissingHost with host" +
            " NoTargetMissingHost failed: java.lang.NoClassDefFoundError: NoTargetMissingHost";
        try {
            Caller.invokeTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class" +
            " TestNestmateMembership$TargetMissingHost with modifiers \"private static\"";
        try {
            Caller.invokeTargetMissingHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such method: TestNestmateMembership$TargetMissingHost.m()void/invokeStatic";
        try {
            Caller.invokeTargetMissingHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such method: TestNestmateMembership$TargetMissingHost.m()void/invokeStatic";
        try {
            Caller.invokeTargetMissingHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Nest host resolution of TestNestmateMembership$CallerMissingHost with host" +
            " NoCallerMissingHost failed: java.lang.NoClassDefFoundError: NoCallerMissingHost";
        try {
            CallerMissingHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerMissingHost.invokeTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotInstanceHostInvoke() throws Throwable {
        System.out.println("Testing for nest-host class that is not an instance class");
        String msg = "Type TestNestmateMembership$TargetNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            Caller.invokeTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class "+
            "TestNestmateMembership$TargetNotInstanceHost with modifiers \"private static\"";
        try {
            Caller.invokeTargetNotInstanceHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such method: TestNestmateMembership$TargetNotInstanceHost.m()void/invokeStatic";
        try {
            Caller.invokeTargetNotInstanceHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotInstanceHost (loader: 'app') is not a " +
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            CallerNotInstanceHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotInstanceHost.invokeTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotOurHostInvoke() throws Throwable {
        System.out.println("Testing for nest-host class that does not list us in its nest");
        String msg = "Type TestNestmateMembership$TargetNotOurHost (loader: 'app') is not a " +
            "nest member of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.invokeTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotOurHost with modifiers \"private static\"";
        try {
            Caller.invokeTargetNotOurHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such method: TestNestmateMembership$TargetNotOurHost.m()void/invokeStatic";
        try {
            Caller.invokeTargetNotOurHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerNotOurHost.invokeTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotOurHost.invokeTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_WrongPackageHostInvoke() {
        System.out.println("Testing for nest-host and nest-member in different packages");
        String msg = "Type P2.PackagedNestHost2$Member (loader: 'app') is not a nest member of " +
            "type P1.PackagedNestHost (loader: 'app'): types are in different packages";
        try {
            P1.PackagedNestHost.doInvoke();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            P2.PackagedNestHost2.Member.doInvoke();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    // constructor tests

   static void test_GoodConstruct(){
        try {
            Caller.newTarget();
        }
        catch (Exception e) {
            throw new Error("Unexpected exception on good construction: " + e);
        }
    }

    static void test_NoHostConstruct() throws Throwable {
        System.out.println("Testing for missing nest-host attribute");
        String msg = "class TestNestmateMembership$Caller tried to access private " +
            "method 'void TestNestmateMembership$TargetNoHost.<init>()'";
        try {
            Caller.newTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNoHost with modifiers \"private\"";
        try {
            Caller.newTargetNoHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such constructor: TestNestmateMembership$TargetNoHost.<init>()void/newInvokeSpecial";
        try {
            Caller.newTargetNoHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "method 'void TestNestmateMembership$Target.<init>()'";
        try {
            CallerNoHost.newTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "method 'void TestNestmateMembership$TargetNoHost.<init>()'";
        try {
            CallerNoHost.newTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_SelfHostConstruct() throws Throwable {
        System.out.println("Testing for class that lists itself as nest-host");
        String msg = "Type TestNestmateMembership$TargetSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$TargetSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.newTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetSelfHost with modifiers \"private\"";
        try {
            Caller.newTargetSelfHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such constructor: TestNestmateMembership$TargetSelfHost.<init>()void/newInvokeSpecial";
        try {
            Caller.newTargetSelfHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$CallerSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerSelfHost.newTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerSelfHost.newTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_MissingHostConstruct() throws Throwable {
        System.out.println("Testing for nest-host class that does not exist");
        String msg = "Nest host resolution of TestNestmateMembership$TargetMissingHost with " +
            "host NoTargetMissingHost failed: java.lang.NoClassDefFoundError: NoTargetMissingHost";
        try {
            Caller.newTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetMissingHost with modifiers \"private\"";
        try {
            Caller.newTargetMissingHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such constructor: TestNestmateMembership$TargetMissingHost.<init>()void/newInvokeSpecial";
        try {
            Caller.newTargetMissingHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Nest host resolution of TestNestmateMembership$CallerMissingHost with host " +
            "NoCallerMissingHost failed: java.lang.NoClassDefFoundError: NoCallerMissingHost";
        try {
            CallerMissingHost.newTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerMissingHost.newTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotInstanceHostConstruct() throws Throwable {
        System.out.println("Testing for nest-host class that is not an instance class");
        String msg = "Type TestNestmateMembership$TargetNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            Caller.newTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotInstanceHost with modifiers \"private\"";
        try {
            Caller.newTargetNotInstanceHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such constructor: TestNestmateMembership$TargetNotInstanceHost.<init>()void/newInvokeSpecial";
        try {
            Caller.newTargetNotInstanceHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            CallerNotInstanceHost.newTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotInstanceHost.newTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotOurHostConstruct() throws Throwable {
        System.out.println("Testing for nest-host class that does not list us in its nest");
        String msg = "Type TestNestmateMembership$TargetNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.newTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotOurHost with modifiers \"private\"";
        try {
            Caller.newTargetNotOurHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "no such constructor: TestNestmateMembership$TargetNotOurHost.<init>()void/newInvokeSpecial";
        try {
            Caller.newTargetNotOurHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerNotOurHost.newTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "Type TestNestmateMembership$CallerNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerNotOurHost.newTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_WrongPackageHostConstruct() {
        System.out.println("Testing for nest-host and nest-member in different packages");
        String msg = "Type P2.PackagedNestHost2$Member (loader: 'app') is not a nest member of " +
            "type P1.PackagedNestHost (loader: 'app'): types are in different packages";
        try {
            P1.PackagedNestHost.doConstruct();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            P2.PackagedNestHost2.Member.doConstruct();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    // field tests

   static void test_GoodGetField(){
        try {
            Caller.getFieldTarget();
        }
        catch (Exception e) {
            throw new Error("Unexpected exception on good field access: " + e);
        }
    }

    static void test_NoHostGetField() throws Throwable {
        System.out.println("Testing for missing nest-host attribute");
        String msg = "class TestNestmateMembership$Caller tried to access private " +
            "field TestNestmateMembership$TargetNoHost.f";
        try {
            Caller.getFieldTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNoHost with modifiers \"private static\"";
        try {
            Caller.getFieldTargetNoHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetNoHost.f/int/getStatic";
        try {
            Caller.getFieldTargetNoHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "field TestNestmateMembership$Target.f";
        try {
            CallerNoHost.getFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "field TestNestmateMembership$TargetNoHost.f";
        try {
            CallerNoHost.getFieldTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_SelfHostGetField() throws Throwable {
        System.out.println("Testing for class that lists itself as nest-host");
        String msg = "Type TestNestmateMembership$TargetSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$TargetSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.getFieldTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetSelfHost with modifiers \"private static\"";
        try {
            Caller.getFieldTargetSelfHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetSelfHost.f/int/getStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.getFieldTargetSelfHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$CallerSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerSelfHost.getFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerSelfHost.getFieldTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_MissingHostGetField() throws Throwable {
        System.out.println("Testing for nest-host class that does not exist");
        String msg = "Nest host resolution of TestNestmateMembership$TargetMissingHost with " +
            "host NoTargetMissingHost failed: java.lang.NoClassDefFoundError: NoTargetMissingHost";
        try {
            Caller.getFieldTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetMissingHost with modifiers \"private static\"";
        try {
            Caller.getFieldTargetMissingHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetMissingHost.f/int/getStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.getFieldTargetMissingHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Nest host resolution of TestNestmateMembership$CallerMissingHost with " +
            "host NoCallerMissingHost failed: java.lang.NoClassDefFoundError: NoCallerMissingHost";
        try {
            CallerMissingHost.getFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerMissingHost.getFieldTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotInstanceHostGetField() throws Throwable {
        System.out.println("Testing for nest-host class that is not an instance class");
        String msg = "Type TestNestmateMembership$TargetNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            Caller.getFieldTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotInstanceHost with modifiers \"private static\"";
        try {
            Caller.getFieldTargetNotInstanceHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetNotInstanceHost.f/int/getStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.getFieldTargetNotInstanceHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            CallerNotInstanceHost.getFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotInstanceHost.getFieldTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotOurHostGetField() throws Throwable {
        System.out.println("Testing for nest-host class that does not list us in its nest");
        String msg = "Type TestNestmateMembership$TargetNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.getFieldTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotOurHost with modifiers \"private static\"";
        try {
            Caller.getFieldTargetNotOurHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetNotOurHost.f/int/getStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.getFieldTargetNotOurHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerNotOurHost.getFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotOurHost.getFieldTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_WrongPackageHostGetField() {
        System.out.println("Testing for nest-host and nest-member in different packages");
        String msg = "Type P2.PackagedNestHost2$Member (loader: 'app') is not a nest member of " +
            "type P1.PackagedNestHost (loader: 'app'): types are in different packages";
        try {
            P1.PackagedNestHost.doGetField();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            P2.PackagedNestHost2.Member.doGetField();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

   static void test_GoodPutField(){
        try {
            Caller.putFieldTarget();
        }
        catch (Exception e) {
            throw new Error("Unexpected exception on good field access: " + e);
        }
    }

    static void test_NoHostPutField() throws Throwable {
        System.out.println("Testing for missing nest-host attribute");
        String msg = "class TestNestmateMembership$Caller tried to access private " +
            "field TestNestmateMembership$TargetNoHost.f";
        try {
            Caller.putFieldTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNoHost with modifiers \"private static\"";
        try {
            Caller.putFieldTargetNoHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetNoHost.f/int/putStatic";
        try {
            Caller.putFieldTargetNoHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "field TestNestmateMembership$Target.f";
        try {
            CallerNoHost.putFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$CallerNoHost tried to access private " +
            "field TestNestmateMembership$TargetNoHost.f";
        try {
            CallerNoHost.putFieldTargetNoHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_SelfHostPutField() throws Throwable {
        System.out.println("Testing for class that lists itself as nest-host");
        String msg = "Type TestNestmateMembership$TargetSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$TargetSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.putFieldTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetSelfHost with modifiers \"private static\"";
        try {
            Caller.putFieldTargetSelfHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetSelfHost.f/int/putStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.putFieldTargetSelfHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerSelfHost (loader: 'app') is not a nest member" +
            " of type TestNestmateMembership$CallerSelfHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerSelfHost.putFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerSelfHost.putFieldTargetSelfHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_MissingHostPutField() throws Throwable {
        System.out.println("Testing for nest-host class that does not exist");
        String msg = "Nest host resolution of TestNestmateMembership$TargetMissingHost with " +
            "host NoTargetMissingHost failed: java.lang.NoClassDefFoundError: NoTargetMissingHost";
        try {
            Caller.putFieldTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetMissingHost with modifiers \"private static\"";
        try {
            Caller.putFieldTargetMissingHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetMissingHost.f/int/putStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.putFieldTargetMissingHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "Nest host resolution of TestNestmateMembership$CallerMissingHost with host " +
            "NoCallerMissingHost failed: java.lang.NoClassDefFoundError: NoCallerMissingHost";
        try {
            CallerMissingHost.putFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerMissingHost.putFieldTargetMissingHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotInstanceHostPutField() throws Throwable {
        System.out.println("Testing for nest-host class that is not an instance class");
        String msg = "Type TestNestmateMembership$TargetNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            Caller.putFieldTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotInstanceHost with modifiers \"private static\"";
        try {
            Caller.putFieldTargetNotInstanceHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetNotInstanceHost.f/int/putStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.putFieldTargetNotInstanceHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }

        msg = "Type TestNestmateMembership$CallerNotInstanceHost (loader: 'app') is not a "+
            "nest member of type [LInvalidNestHost; (loader: 'app'): host is not an instance class";
        try {
            CallerNotInstanceHost.putFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotInstanceHost.putFieldTargetNotInstanceHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_NotOurHostPutField() throws Throwable {
        System.out.println("Testing for nest-host class that does not list us in its nest");
        String msg = "Type TestNestmateMembership$TargetNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            Caller.putFieldTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        msg = "class TestNestmateMembership$Caller cannot access a member of class " +
            "TestNestmateMembership$TargetNotOurHost with modifiers \"private static\"";
        try {
            Caller.putFieldTargetNotOurHostReflectively();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "member is private: TestNestmateMembership$TargetNotOurHost.f/int/putStatic, " +
            "from class TestNestmateMembership$Caller";
        try {
            Caller.putFieldTargetNotOurHostMH();
            throw new Error("Missing IllegalAccessException: " + msg);
        }
        catch (IllegalAccessException expected) {
            check_expected(expected, msg);
        }
        msg = "Type TestNestmateMembership$CallerNotOurHost (loader: 'app') is not a nest member" +
            " of type InvalidNestHost (loader: 'app'): current type is not listed as a nest member";
        try {
            CallerNotOurHost.putFieldTarget();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            CallerNotOurHost.putFieldTargetNotOurHost();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    static void test_WrongPackageHostPutField() {
        System.out.println("Testing for nest-host and nest-member in different packages");
        String msg = "Type P2.PackagedNestHost2$Member (loader: 'app') is not a nest member of " +
            "type P1.PackagedNestHost (loader: 'app'): types are in different packages";
        try {
            P1.PackagedNestHost.doPutField();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
        try {
            P2.PackagedNestHost2.Member.doPutField();
            throw new Error("Missing IllegalAccessError: " + msg);
        }
        catch (IllegalAccessError expected) {
            check_expected(expected, msg);
        }
    }

    // utilities

    static void check_expected(Throwable expected, String msg) {
        if (!expected.getMessage().contains(msg)) {
            throw new Error("Wrong " + expected.getClass().getSimpleName() +": \"" +
                            expected.getMessage() + "\" does not contain \"" +
                            msg + "\"", expected);
        }
        System.out.println("OK - got expected exception: " + expected);
    }
}
