/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8192936
 * @summary RI does not follow the JVMTI RedefineClasses spec; need to disallow adding and deleting methods
 * @requires vm.jvmti
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @modules java.compiler
 *          java.instrument
 *          jdk.jartool/sun.tools.jar
 * @run main RedefineClassHelper
 * @run main/othervm -javaagent:redefineagent.jar TestAddDeleteMethods AllowAddDelete=no
 * @run main/othervm -javaagent:redefineagent.jar -XX:+AllowRedefinitionToAddDeleteMethods TestAddDeleteMethods AllowAddDelete=yes
 */

import static jdk.test.lib.Asserts.assertEquals;
import java.lang.Runnable;

// package access top-level class to avoid problem with RedefineClassHelper
// and nested types.
class A implements Runnable {
    private        void foo()       { System.out.println(" OLD foo called"); }
    public         void publicFoo() { System.out.println(" OLD publicFoo called"); }
    private final  void finalFoo()  { System.out.println(" OLD finalFoo called");  }
    private static void staticFoo() { System.out.println(" OLD staticFoo called"); }
    public         void run()       { foo(); publicFoo(); finalFoo(); staticFoo(); }
}

class B implements Runnable {
    public         void run() { }
}

public class TestAddDeleteMethods {
    static private boolean allowAddDeleteMethods = false;

    static private A a;
    static private B b;

    // This redefinition is expected to always succeed.
    public static String newA =
        "class A implements Runnable {" +
            "private        void foo()       { System.out.println(\" NEW foo called\"); }" +
            "public         void publicFoo() { System.out.println(\" NEW publicFoo called\"); }" +
            "private final  void finalFoo()  { System.out.println(\" NEW finalFoo called\");  }" +
            "private static void staticFoo() { System.out.println(\" NEW staticFoo called\"); }" +
            "public         void run()       { foo(); publicFoo(); finalFoo(); staticFoo(); }" +
        "}";

    // This redefinition is expected to always fail.
    public static String ADeleteFoo =
        "class A implements Runnable {" +
            "public         void publicFoo() { System.out.println(\" NEW publicFoo called\"); }" +
            "private final  void finalFoo()  { System.out.println(\" NEW finalFoo called\");  }" +
            "private static void staticFoo() { System.out.println(\" NEW staticFoo called\"); }" +
            "public         void run()       { publicFoo(); finalFoo(); staticFoo(); }" +
        "}";

    // This redefinition is expected to always fail.
    public static String ADeletePublicFoo =
        "class A implements Runnable {" +
            "private        void foo()       { System.out.println(\" NEW foo called\"); }" +
            "private final  void finalFoo()  { System.out.println(\" NEW finalFoo called\");  }" +
            "private static void staticFoo() { System.out.println(\" NEW staticFoo called\"); }" +
            "public         void run()       { foo(); finalFoo(); staticFoo(); }" +
        "}";

    // This redefinition is expected to succeed with option -XX:+AllowRedefinitionToAddDeleteMethods.
    public static String ADeleteFinalFoo =
        "class A implements Runnable {" +
            "private        void foo()       { System.out.println(\" NEW foo called\"); }" +
            "public         void publicFoo() { System.out.println(\" NEW publicFoo called\"); }" +
            "private static void staticFoo() { System.out.println(\" NEW staticFoo called\"); }" +
            "public         void run()       { foo(); publicFoo(); staticFoo(); }" +
        "}";

    // This redefinition is expected to succeed with option -XX:+AllowRedefinitionToAddDeleteMethods.
    // With compatibility option redefinition ADeleteFinalFoo already deleted finalFoo method.
    // So, this redefinition will add it back which is expected to work.
    public static String ADeleteStaticFoo =
        "class A implements Runnable {" +
            "private        void foo()       { System.out.println(\" NEW foo called\"); }" +
            "public         void publicFoo() { System.out.println(\" NEW publicFoo called\"); }" +
            "private final  void finalFoo()  { System.out.println(\" NEW finalFoo called\");  }" +
            "public         void run()       { foo(); publicFoo(); finalFoo(); }" +
        "}";

    // This redefinition is expected to always fail.
    public static String BAddBar =
        "class B implements Runnable {" +
            "private        void bar()       { System.out.println(\" bar called\"); }" +
            "public         void run()       { bar(); }" +
        "}";

    // This redefinition is expected to always fail.
    public static String BAddPublicBar =
        "class B implements Runnable {" +
            "public         void publicBar() { System.out.println(\" publicBar called\"); }" +
            "public         void run()       { publicBar(); }" +
        "}";

    // This redefinition is expected to succeed with option -XX:+AllowRedefinitionToAddDeleteMethods.
    public static String BAddFinalBar =
        "class B implements Runnable {" +
            "private final  void finalBar()  { System.out.println(\" finalBar called\"); }" +
            "public         void run()       { finalBar(); }" +
        "}";

    // This redefinition is expected to succeed with option -XX:+AllowRedefinitionToAddDeleteMethods.
    // With compatibility option redefinition BAddFinalBar added finalBar method.
    // So, this redefinition will deleate it back which is expected to work.
    public static String BAddStaticBar =
        "class B implements Runnable {" +
            "private static void staticBar() { System.out.println(\" staticBar called\"); }" +
            "public         void run()       { staticBar(); }" +
        "}";

    static private final String ExpMsgPrefix = "attempted to ";
    static private final String ExpMsgPostfix = " a method";

    static private void log(String msg) { System.out.println(msg); }

    public static void test(Runnable obj, String newBytes, String expSuffix, String methodName,
                            boolean expectedRedefToPass) throws Exception {
        String expectedMessage = ExpMsgPrefix + expSuffix + ExpMsgPostfix;
        Class klass = obj.getClass();
        String className = klass.getName();
        String expResult = expectedRedefToPass ? "PASS" : "FAIL";

        log("");
        log("## Test " + expSuffix + " method \'" + methodName + "\' in class " + className +
            "; redefinition expected to " + expResult);

        try {
            RedefineClassHelper.redefineClass(klass, newBytes);

            if (expectedRedefToPass) {
                log(" Did not get UOE at redefinition as expected");
            } else {
                throw new RuntimeException("Failed, expected UOE");
            }
            obj.run();
            log("");
        } catch (UnsupportedOperationException uoe) {
            String message = uoe.getMessage();

            if (expectedRedefToPass) {
                throw new RuntimeException("Failed, unexpected UOE: " + message);
            } else {
                log(" Got expected UOE: " + message);
                if (!message.endsWith(expectedMessage)) {
                    throw new RuntimeException("Expected UOE error message to end with: " + expectedMessage);
                }
            }
        }
    }

    static {
        a = new A();
        b = new B();
    }

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("AllowAddDelete=yes")) {
            allowAddDeleteMethods = true;
        }

        log("## Test original class A");
        a.run();
        log("");

        log("## Test with modified method bodies in class A; redefinition expected to pass: true");
        RedefineClassHelper.redefineClass(A.class, newA);
        a.run();

        test(a, ADeleteFoo,       "delete", "foo",       false);
        test(a, ADeletePublicFoo, "delete", "publicFoo", false);
        test(a, ADeleteFinalFoo,  "delete", "finalFoo",  allowAddDeleteMethods);
        test(a, ADeleteStaticFoo, "delete", "staticFoo", allowAddDeleteMethods);

        test(b, BAddBar,          "add", "bar",       false);
        test(b, BAddPublicBar,    "add", "publicBar", false);
        test(b, BAddFinalBar,     "add", "finalBar",  allowAddDeleteMethods);
        test(b, BAddStaticBar,    "add", "staticBar", allowAddDeleteMethods);
    }
}
