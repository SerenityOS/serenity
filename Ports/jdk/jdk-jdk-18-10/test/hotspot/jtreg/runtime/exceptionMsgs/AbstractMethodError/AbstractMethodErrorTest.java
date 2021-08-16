/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018 SAP SE. All rights reserved.
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

/**
 * @test
 * @summary Check that the verbose message of the AME is printed correctly.
 * @requires !(os.arch=="arm") & vm.flavor == "server" & !vm.emulatedClient & vm.compMode=="Xmixed" & !vm.graal.enabled & vm.opt.UseJVMCICompiler != true & (vm.opt.TieredStopAtLevel == null | vm.opt.TieredStopAtLevel==4)
 * @library /test/lib /
 * @build sun.hotspot.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller sun.hotspot.WhiteBox
 * @compile AbstractMethodErrorTest.java
 * @compile AME1_E.jasm AME2_C.jasm AME3_C.jasm AME4_E.jasm AME5_B.jasm AME6_B.jasm
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileThreshold=1000 -XX:-BackgroundCompilation -XX:-Inline
 *                   -XX:CompileCommand=exclude,AbstractMethodErrorTest::test_ame1
 *                   AbstractMethodErrorTest
 */

import sun.hotspot.WhiteBox;
import compiler.whitebox.CompilerWhiteBoxTest;
import java.lang.reflect.Method;

// This test assembles an errorneous installation of classes.
// First, compile the test by @compile. This results in a legal set
// of classes.
// Then, with jasm, generate incompatible classes that overwrite
// the class files in the build directory.
// Last, call the real test throwing an AbstractMethodError and
// check the message generated.
public class AbstractMethodErrorTest {

    private static final WhiteBox WHITE_BOX = WhiteBox.getWhiteBox();

    private static boolean enableChecks = true;

    private static boolean compile(Class<?> clazz, String name) {
        try {
            Method method = clazz.getMethod(name);
            boolean enqueued = WHITE_BOX.enqueueMethodForCompilation(method, CompilerWhiteBoxTest.COMP_LEVEL_FULL_OPTIMIZATION);
            if (!enqueued) {
                System.out.println("Warning: Blocking compilation failed for " + clazz.getName() + "." + name + " (timeout?)");
                return false;
            } else if (!WHITE_BOX.isMethodCompiled(method)) {
                throw new RuntimeException(clazz.getName() + "." + name + " is not compiled");
            }
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(clazz.getName() + "." + name + " not found", e);
        }
        return true;
    }

    public static boolean setup_test() {
        // Assure all exceptions are loaded.
        new AbstractMethodError();
        new IncompatibleClassChangeError();

        enableChecks = false;
        // Warmup
        System.out.println("warmup:");
        test_ame5_compiled_vtable_stub();
        test_ame6_compiled_itable_stub();
        enableChecks = true;

        // Compile
        if (!compile(AbstractMethodErrorTest.class, "test_ame5_compiled_vtable_stub") ||
            !compile(AbstractMethodErrorTest.class, "test_ame6_compiled_itable_stub") ||
            !compile(AME5_C.class, "mc") ||
            !compile(AME5_D.class, "mc") ||
            !compile(AME5_E.class, "mc") ||
            !compile(AME6_C.class, "mc") ||
            !compile(AME6_D.class, "mc") ||
            !compile(AME6_E.class, "mc")) {
            return false;
        }

        System.out.println("warmup done.");
        return true;
    }

    private static String expectedErrorMessageAME1_1 =
        "Missing implementation of resolved method 'abstract " +
        "java.lang.String anAbstractMethod()' of abstract class AME1_B.";
    private static String expectedErrorMessageAME1_2 =
        "Receiver class AME1_E does not define or inherit an implementation of the " +
        "resolved method 'abstract java.lang.String aFunctionOfMyInterface()' of " +
        "interface AME1_C.";

    public static void test_ame1() {
        AME1_B objectAbstract = new AME1_D();
        AME1_C objectInterface = new AME1_D();
        objectInterface.secondFunctionOfMyInterface();
        objectAbstract.anAbstractMethod();
        objectInterface.aFunctionOfMyInterface();

        try {
            objectAbstract = new AME1_E();
            // AbstractMethodError gets thrown in the interpreter at:
            // InterpreterGenerator::generate_abstract_entry
            objectAbstract.anAbstractMethod();
            throw new RuntimeException("Expected AbstractRuntimeError was not thrown.");
        } catch (AbstractMethodError e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                throw new RuntimeException("Caught AbstractMethodError with empty message.");
            } else if (!errorMsg.equals(expectedErrorMessageAME1_1)) {
                System.out.println("Expected: " + expectedErrorMessageAME1_1 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException("Caught unexpected exception: " + e);
        }

        try {
            objectInterface = new AME1_E();
            // AbstractMethodError gets thrown in:
            // TemplateTable::invokeinterface or C-Interpreter loop
            objectInterface.aFunctionOfMyInterface();
            throw new RuntimeException("Expected AbstractRuntimeError was not thrown.");
        } catch (AbstractMethodError e) {
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                throw new RuntimeException("Caught AbstractMethodError with empty message.");
            } else if (!errorMsg.equals(expectedErrorMessageAME1_2)) {
                // Thrown via InterpreterRuntime::throw_AbstractMethodErrorVerbose().
                System.out.println("Expected: " + expectedErrorMessageAME1_2 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            } else {
                System.out.println("Passed with message: " + errorMsg);
            }
        } catch (Throwable e) {
            throw new RuntimeException("Caught unexpected exception: " + e);
        }
    }

    private static String expectedErrorMessageAME2_Interpreted =
        "Missing implementation of resolved method 'abstract " +
        "void aFunctionOfMyInterface()' of interface AME2_A.";
    private static String expectedErrorMessageAME2_Compiled =
        "Receiver class AME2_C does not define or inherit an implementation of the resolved method " +
        "'abstract void aFunctionOfMyInterface()' of interface AME2_A.";

    public AbstractMethodErrorTest() throws InstantiationException, IllegalAccessException {
        try {
            AME2_B myAbstract = new ImplementsAllFunctions();
            myAbstract.fun2();
            myAbstract.aFunctionOfMyInterface();

            // AME2_C does not implement the method
            // aFunctionOfMyInterface(). Expected runtime behavior is
            // throwing an AbstractMethodError.
            // The error will be thrown via throw_AbstractMethodErrorWithMethod()
            // if the template interpreter calls an abstract method by
            // entering the abstract method entry.
            myAbstract = new AME2_C();
            myAbstract.fun2();
            myAbstract.aFunctionOfMyInterface();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // Loop so that method gets eventually compiled/osred.
    public static void test_ame2() throws Exception {
        boolean seenInterpreted = false;
        boolean seenCompiled = false;

        // Loop to test both, the interpreted and the compiled case.
        for (int i = 0; i < 10000 && !(seenInterpreted && seenCompiled); ++i) {
            try {
                // Supposed to throw AME with verbose message.
                new AbstractMethodErrorTest();

                throw new RuntimeException("Expected AbstractMethodError was not thrown.");
            } catch (AbstractMethodError e) {
                String errorMsg = e.getMessage();

                // Check the message obtained.
                if (errorMsg == null) {
                    throw new RuntimeException("Caught AbstractMethodError with empty message.");
                } else if (errorMsg.equals(expectedErrorMessageAME2_Interpreted)) {
                    seenInterpreted = true;
                } else if (errorMsg.equals(expectedErrorMessageAME2_Compiled)) {
                    // Sparc and the other platforms behave differently here:
                    // Sparc throws the exception via SharedRuntime::handle_wrong_method_abstract(),
                    // x86, ppc and s390 via LinkResolver::runtime_resolve_virtual_method(). Thus,
                    // sparc misses the test case for LinkResolver::runtime_resolve_virtual_method().
                    seenCompiled = true;
                } else {
                    System.out.println("Expected: " + expectedErrorMessageAME2_Interpreted + "\n" +
                                       "or:       " + expectedErrorMessageAME2_Compiled + "\n" +
                                       "but got:  " + errorMsg);
                    throw new RuntimeException("Wrong error message of AbstractMethodError.");
                }
            }
        }
        if (!(seenInterpreted && seenCompiled)) {
            if (seenInterpreted) { System.out.println("Saw interpreted message."); }
            if (seenCompiled)    { System.out.println("Saw compiled message."); }
            throw new RuntimeException("Test did not produce wrong error messages for AbstractMethodError, " +
                                       "but it did not test both cases (interpreted and compiled).");
        }
    }

    private static String expectedErrorMessageAME3_1 =
        "Receiver class AME3_C does not define or inherit an implementation of the resolved method " +
        "'void ma()' of class AME3_A. Selected method is 'abstract void AME3_B.ma()'.";

    // Testing abstract class that extends a class that has an implementation.
    // Loop so that method gets eventually compiled/osred.
    public static void test_ame3_1() throws Exception {
        AME3_A c = new AME3_C();

        try {
            // Supposed to throw AME with verbose message.
            c.ma();

            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError e) {
            String errorMsg = e.getMessage();

            // Check the message obtained.
            if (errorMsg == null) {
                throw new RuntimeException("Caught AbstractMethodError with empty message.");
            } else if (errorMsg.equals(expectedErrorMessageAME3_1)) {
                // Expected test case thrown via LinkResolver::runtime_resolve_virtual_method().
                System.out.println("Passed with message: " + errorMsg);
            } else {
                System.out.println("Expected: " + expectedErrorMessageAME3_1 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            }
        }
    }

    private static String expectedErrorMessageAME3_2 =
        "Receiver class AME3_C does not define or inherit an implementation of " +
        "the resolved method 'abstract void ma()' of abstract class AME3_B.";

    // Testing abstract class that extends a class that has an implementation.
    // Loop so that method gets eventually compiled/osred.
    public static void test_ame3_2() throws Exception {
        AME3_C c = new AME3_C();

        try {
            // Supposed to throw AME with verbose message.
            c.ma();

            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError e) {
            String errorMsg = e.getMessage();

            // Check the message obtained.
            if (errorMsg == null) {
                throw new RuntimeException("Caught AbstractMethodError with empty message.");
            } else if (errorMsg.equals(expectedErrorMessageAME3_2)) {
                // Expected test case thrown via LinkResolver::runtime_resolve_virtual_method().
                System.out.println("Passed with message: " + errorMsg);
            } else {
                System.out.println("Expected: " + expectedErrorMessageAME3_2 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            }
        }
    }

    private static String expectedErrorMessageAME4 =
        "Missing implementation of resolved method 'abstract void ma()' of " +
        "abstract class AME4_B.";

    // Testing abstract class that extends a class that has an implementation.
    public static void test_ame4() throws Exception {
        AME4_C c = new AME4_C();
        AME4_D d = new AME4_D();
        AME4_E e = new AME4_E();  // Errorneous.

        AME4_A a;
        try {
            // Test: calls errorneous e.ma() in the last iteration.
            final int iterations = 10;
            for (int i = 0; i < iterations; i++) {
                a = e;
                if (i % 2 == 0 && i < iterations - 1) {
                    a = c;
                }
                if (i % 2 == 1 && i < iterations - 1) {
                    a = d;
                }

                // AbstractMethodError gets thrown in the interpreter at:
                // InterpreterGenerator::generate_abstract_entry
                a.ma();
            }

            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        } catch (AbstractMethodError exc) {
            System.out.println();
            String errorMsg = exc.getMessage();

                // Check the message obtained.
            if (enableChecks && errorMsg == null) {
                throw new RuntimeException("Caught AbstractMethodError with empty message.");
            } else if (errorMsg.equals(expectedErrorMessageAME4)) {
                // Expected test case.
                System.out.println("Passed with message: " + errorMsg);
            } else if (enableChecks) {
                System.out.println("Expected: " + expectedErrorMessageAME4 + "\n" +
                                   "but got:  " + errorMsg);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            }
        }
    }

    private static String expectedErrorMessageAME5_VtableStub =
        "Receiver class AME5_B does not define or inherit an implementation of the resolved method 'abstract void mc()' " +
        "of abstract class AME5_A.";

    // AbstractMethodErrors detected in vtable stubs.
    // Note: How can we verify that we really stepped through the vtable stub?
    // - Bimorphic inlining should not happen since we have no profiling data when
    //   we compile the method
    // - As a result, an inline cache call should be generated
    // - This inline cache call is patched into a real vtable call at the first
    //   re-resolve, which happens constantly during the first 10 iterations of the loop.
    // => we should be fine! :-)
    public static void test_ame5_compiled_vtable_stub() {
        // Allocated the objects we need and call a valid method.
        boolean caught_ame = false;
        AME5_B b = new AME5_B();
        AME5_C c = new AME5_C();
        AME5_D d = new AME5_D();
        AME5_E e = new AME5_E();
        b.ma();
        c.ma();
        d.ma();
        e.ma();

        try {
            final int iterations = 10;
            // Test: calls b.c() in the last iteration.
            for (int i = 0; i < iterations; i++) {
                AME5_A a = b;
                if (i % 3 == 0 && i < iterations - 1) {
                    a = c;
                }
                if (i % 3 == 1 && i < iterations - 1) {
                    a = d;
                }
                if (i % 3 == 2 && i < iterations - 1) {
                    a = e;
                }

              a.mc();
            }
            System.out.println();
        } catch (AbstractMethodError exc) {
            caught_ame = true;
            System.out.println();
            String errorMsg = exc.getMessage();
            if (enableChecks && errorMsg == null) {
                System.out.println(exc);
                throw new RuntimeException("Empty error message of AbstractMethodError.");
            }
            if (enableChecks &&
                !errorMsg.equals(expectedErrorMessageAME5_VtableStub)) {
                // Thrown via SharedRuntime::handle_wrong_method_abstract().
                System.out.println("Expected: " + expectedErrorMessageAME5_VtableStub + "\n" +
                                   "but got:  " + errorMsg);
                System.out.println(exc);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            }
            if (enableChecks) {
                System.out.println("Passed with message: " + errorMsg);
            }
        } catch (Throwable exc) {

        throw exc;
        }

        // Check that we got the exception at some point.
        if (enableChecks && !caught_ame) {
            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        }
    }

    private static String expectedErrorMessageAME6_ItableStub =
        "Receiver class AME6_B does not define or inherit an implementation of the resolved" +
        " method 'abstract void mc()' of interface AME6_A.";

    // -------------------------------------------------------------------------
    // AbstractMethodErrors detected in itable stubs.
    // Note: How can we verify that we really stepped through the itable stub?
    // - Bimorphic inlining should not happen since we have no profiling data when
    //   we compile the method
    // - As a result, an inline cache call should be generated
    // - This inline cache call is patched into a real vtable call at the first
    //   re-resolve, which happens constantly during the first 10 iterations of the loop.
    // => we should be fine! :-)
    public static void test_ame6_compiled_itable_stub() {
        // Allocated the objects we need and call a valid method.
        boolean caught_ame = false;
        AME6_B b = new AME6_B();
        AME6_C c = new AME6_C();
        AME6_D d = new AME6_D();
        AME6_E e = new AME6_E();
        b.ma();
        c.ma();
        d.ma();
        e.ma();

        try {
            final int iterations = 10;
            // Test: calls b.c() in the last iteration.
            for (int i = 0; i < iterations; i++) {
                AME6_A a = b;
                if (i % 3 == 0 && i < iterations - 1) {
                    a = c;
                }
                if (i % 3 == 1 && i < iterations - 1) {
                    a = d;
                }
                if (i % 3 == 2 && i < iterations - 1) {
                    a = e;
                }
                a.mc();
            }
            System.out.println();
        } catch (AbstractMethodError exc) {
            caught_ame = true;
            System.out.println();
            String errorMsg = exc.getMessage();
            if (enableChecks && errorMsg == null) {
                System.out.println(exc);
                throw new RuntimeException("Empty error message of AbstractMethodError.");
            }
            if (enableChecks &&
                !errorMsg.equals(expectedErrorMessageAME6_ItableStub)) {
                // Thrown via LinkResolver::runtime_resolve_interface_method().
                System.out.println("Expected: " + expectedErrorMessageAME6_ItableStub + "\n" +
                                   "but got:  " + errorMsg);
                System.out.println(exc);
                throw new RuntimeException("Wrong error message of AbstractMethodError.");
            }
            if (enableChecks) {
                System.out.println("Passed with message: " + errorMsg);
            }
        } catch (Throwable exc) {
            throw exc;
        }

        // Check that we got the exception at some point.
        if (enableChecks && !caught_ame) {
            throw new RuntimeException("Expected AbstractMethodError was not thrown.");
        }
    }


    public static void main(String[] args) throws Exception {
        if (!setup_test()) {
          return;
        }
        test_ame1();
        test_ame2();
        test_ame3_1();
        test_ame3_2();
        test_ame4();
        test_ame5_compiled_vtable_stub();
        test_ame6_compiled_itable_stub();
    }
}

// Helper classes to test abstract method error.
//
// Errorneous versions of these classes are implemented in java
// assembler.


// -------------------------------------------------------------------------
// This error should be detected interpreted.
//
// Class hierachy:
//
//            C     // interface, defines aFunctionOfMyInterface()
//            |
//      A     |     // interface
//      |     |
//      B     |     // abstract class, defines anAbstractMethod()
//       \   /
//         E        // errorneous class implementation lacks methods C::aFunctionOfMyInterface()
//                                                                   B::anAbstractMethod()
interface AME1_A {

    public String firstFunctionOfMyInterface0();

    public String secondFunctionOfMyInterface0();
}

abstract class AME1_B implements AME1_A {

    abstract public String firstAbstractMethod();

    abstract public String secondAbstractMethod();

    abstract public String anAbstractMethod();
}

interface AME1_C {

    public String firstFunctionOfMyInterface();

    public String secondFunctionOfMyInterface();

    public String aFunctionOfMyInterface();
}

class AME1_D extends AME1_B implements AME1_C {

    public AME1_D() {
    }

    public String firstAbstractMethod() {
        return this.getClass().getName();
    }

    public String secondAbstractMethod() {
        return this.getClass().getName();
    }

    public String anAbstractMethod() {
        return this.getClass().getName();
    }

    public String firstFunctionOfMyInterface0() {
        return this.getClass().getName();
    }

    public String secondFunctionOfMyInterface0() {
        return this.getClass().getName();
    }

    public String firstFunctionOfMyInterface() {
        return this.getClass().getName();
    }

    public String secondFunctionOfMyInterface() {
        return this.getClass().getName();
    }

    public String aFunctionOfMyInterface() {
        return this.getClass().getName();
    }
}

class AME1_E extends AME1_B implements AME1_C {

    public AME1_E() {
    }

    public String firstAbstractMethod() {
        return this.getClass().getName();
    }

    public String secondAbstractMethod() {
        return this.getClass().getName();
    }

    // This method is missing in the .jasm implementation.
    public String anAbstractMethod() {
        return this.getClass().getName();
    }

    public String firstFunctionOfMyInterface0() {
        return this.getClass().getName();
    }

    public String secondFunctionOfMyInterface0() {
        return this.getClass().getName();
    }

    public String firstFunctionOfMyInterface() {
        return this.getClass().getName();
    }

    public String secondFunctionOfMyInterface() {
        return this.getClass().getName();
    }

    // This method is missing in the .jasm implementation.
    public String aFunctionOfMyInterface() {
        return this.getClass().getName();
    }
}

// -------------------------------------------------------------------------
// This error should be detected interpreted.
//
// Class hierachy:
//
//      A   // an interface declaring aFunctionOfMyInterface()
//      |
//      B   // an abstract class
//      |
//      C   // errorneous implementation lacks method A::aFunctionOfMyInterface()
//
interface AME2_A {
    public void aFunctionOfMyInterface();
}

abstract class AME2_B implements AME2_A {
    abstract public void fun2();
}

class ImplementsAllFunctions extends AME2_B {

    public ImplementsAllFunctions() {}

    public void fun2() {
        //System.out.print("You called public void ImplementsAllFunctions::fun2().\n");
    }

    public void aFunctionOfMyInterface() {
        //System.out.print("You called public void ImplementsAllFunctions::aFunctionOfMyInterface()\n");
    }
}

class AME2_C extends AME2_B {

    public AME2_C() {}

    public void fun2() {
        //System.out.print("You called public void AME2_C::fun2().\n");
    }

    // This method is missing in the .jasm implementation.
    public void aFunctionOfMyInterface() {
        //System.out.print("You called public void AME2_C::aFunctionOfMyInterface()\n");
    }
}

// -----------------------------------------------------------------------
// Test AbstractMethod error shadowing existing implementation.
//
// Class hierachy:
//
//           A           // a class implementing m()
//           |
//           B           // an abstract class defining m() abstract
//           |
//           C           // an errorneous class lacking an implementation of m()
//
class AME3_A {
    public void ma() {
        System.out.print("A.ma() ");
    }
}

abstract class AME3_B extends AME3_A {
    public abstract void ma();
}

class AME3_C extends AME3_B {
    // This method is missing in the .jasm implementation.
    public void ma() {
        System.out.print("C.ma() ");
    }
}

// -----------------------------------------------------------------------
// Test AbstractMethod error shadowing existing implementation. In
// this test there are several subclasses of the abstract class.
//
// Class hierachy:
//
//           A           // A: a class implementing ma()
//           |
//           B           // B: an abstract class defining ma() abstract
//        /  | \
//       C   D  E        // E: an errorneous class lacking an implementation of ma()
//
class AME4_A {
    public void ma() {
        System.out.print("A.ma() ");
    }
}

abstract class AME4_B extends AME4_A {
    public abstract void ma();
}

class AME4_C extends AME4_B {
    public void ma() {
        System.out.print("C.ma() ");
    }
}

class AME4_D extends AME4_B {
    public void ma() {
        System.out.print("D.ma() ");
    }
}

class AME4_E extends AME4_B {
    // This method is missing in the .jasm implementation.
    public void ma() {
        System.out.print("E.ma() ");
    }
}

// -------------------------------------------------------------------------
// This error should be detected while processing the vtable stub.
//
// Class hierachy:
//
//              A__     // abstract
//             /|\ \
//            C D E \
//                   B  // Bad class, missing method implementation.
//
// Test:
// - Call D.mc() / E.mc() / F.mc() several times to force real vtable call constrution
// - Call errorneous B.mc() in the end to raise the AbstraceMethodError

abstract class AME5_A {
    public abstract void ma();
    public abstract void mb();
    public abstract void mc();
}

class AME5_B extends AME5_A {
    public void ma() {
        System.out.print("B.ma() ");
    }

    public void mb() {
        System.out.print("B.mb() ");
    }

    // This method is missing in the .jasm implementation.
    public void mc() {
        System.out.print("B.mc() ");
    }
}

class AME5_C extends AME5_A {
    public void ma() {
        System.out.print("C.ma() ");
    }

    public void mb() {
        System.out.print("C.mb() ");
    }

    public void mc() {
        System.out.print("C.mc() ");
    }
}

class AME5_D extends AME5_A {
    public void ma() {
        System.out.print("D.ma() ");
    }

    public void mb() {
        System.out.print("D.mb() ");
    }

    public void mc() {
        System.out.print("D.mc() ");
    }
}

class AME5_E extends AME5_A {
    public  void ma() {
        System.out.print("E.ma() ");
    }

    public void mb() {
        System.out.print("E.mb() ");
    }

    public void mc() {
        System.out.print("E.mc() ");
    }
}

//-------------------------------------------------------------------------
// Test AbstractMethod error detected while processing
// the itable stub.
//
// Class hierachy:
//
//           A__   (interface)
//          /|\ \
//         C D E \
//                B (bad class, missing method)
//
// Test:
// - Call D.mc() / E.mc() / F.mc() several times to force real itable call constrution
// - Call errorneous B.mc() in the end to raise the AbstraceMethodError

interface AME6_A {
    abstract void ma();
    abstract void mb();
    abstract void mc();
}

class AME6_B implements AME6_A {
    public void ma() {
        System.out.print("B.ma() ");
    }

    public void mb() {
        System.out.print("B.mb() ");
    }

    // This method is missing in the .jasm implementation.
    public void mc() {
        System.out.print("B.mc() ");
    }
}

class AME6_C implements AME6_A {
    public void ma() {
        System.out.print("C.ma() ");
    }

    public void mb() {
        System.out.print("C.mb() ");
    }

    public void mc() {
        System.out.print("C.mc() ");
    }
}

class AME6_D implements AME6_A {
    public void ma() {
        System.out.print("D.ma() ");
    }

    public void mb() {
        System.out.print("D.mb() ");
    }

    public void mc() {
        System.out.print("D.mc() ");
    }
}

class AME6_E implements AME6_A {
    public void ma() {
        System.out.print("E.ma() ");
    }

    public void mb() {
        System.out.print("E.mb() ");
    }

    public void mc() {
        System.out.print("E.mc() ");
    }
}
