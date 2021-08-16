/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * INVOKESPECIAL EXPECTED RESULTS
 *
 * From JVMS 3rd edition: invokespecial instruction:
 *
 * Invoke instance method; special handling for superclass, private, and instance
 * initialization method invocations
 *
 * The named method is resolved (5.4.3.3). Finally, if the resolved method is
 * protected (4.7), and it is a member of a superclass of the current class, and
 * the method is not declared in the same run-time package (5.3) as the current
 * class, then the class of objectref must be either the current class or a
 * subclass of the current class.
 *
 * Next, the resolved method is selected for invocation unless all of the
 * following conditions are true:
 *     * The ACC_SUPER flag (see Table 4.1, "Class access and property modifiers") is set for the current class.
 *     * The class of the resolved method is a superclass of the current class.
 *     * The resolved method is not an instance initialization method (3.9).
 *
 * If the above conditions are true, the actual method to be invoked is selected
 * by the following lookup procedure. Let C be the direct superclass of the
 * current class:
 *     * If C contains a declaration for an instance method with the same name and
 *       descriptor as the resolved method, then this method will be invoked.
 *       The lookup procedure terminates.
 *
 *     * Otherwise, if C has a superclass, this same lookup procedure is performed
 *       recursively using the direct superclass of C. The method to be invoked is
 *       the result of the recursive invocation of this lookup procedure.
 *
 *     * Otherwise, an AbstractMethodError? is raised.
 *
 * During resolution of the symbolic reference to the method, any of the
 * exceptions pertaining to method resolution documented in Section 5.4.3.3 can be
 * thrown.
 *
 * Otherwise, if the resolved method is an instance initialization method, and the
 * class in which it is declared is not the class symbolically referenced by the
 * instruction, a NoSuchMethodError? is thrown.
 *
 * Otherwise, if the resolved method is a class (static) method, the invokespecial
 * instruction throws an IncompatibleClassChangeError?.
 *
 * Otherwise, if no method matching the resolved name and descriptor is selected,
 * invokespecial throws an AbstractMethodError?.
 *
 * Otherwise, if the selected method is abstract, invokespecial throws an
 * AbstractMethodError?.
 *
 * RUNTIME EXCEPTIONS
 *
 * Otherwise, if objectref is null, the invokespecial instruction throws a NullPointerException?.
 *
 * Otherwise, if the selected method is native and the code that implements the
 * method cannot be bound, invokespecial throws an UnsatisfiedLinkError?.
 *
 * NOTES
 *
 * The difference between the invokespecial and the invokevirtual instructions is
 * that invokevirtual invokes a method based on the class of the object. The
 * invokespecial instruction is used to invoke instance initialization methods
 * (3.9) as well as private methods and methods of a superclass of the current
 * class.
 *
 * ACC_SUPER:
 *
 * The setting of the ACC_SUPER flag indicates which of two alternative semantics
 * for its invokespecial instruction the Java virtual machine is to express; the
 * ACC_SUPER flag exists for backward compatibility for code compiled by Sun's
 * older compilers for the Java programming language. All new implementations of
 * the Java virtual machine should implement the semantics for invokespecial
 * documented in this specification. All new compilers to the instruction set of
 * the Java virtual machine should set the ACC_SUPER flag. Sun's older compilers
 * generated ClassFile? flags with ACC_SUPER unset. Sun's older Java virtual
 * machine implementations ignore the flag if it is set.
 *
 * ACC_SUPER 0x0020 Treat superclass methods specially when invoked by the
 * invokespecial instruction.
 *
 * My Translation:
 *     1. compile-time resolved class B
 *     2. A,B,C direct superclass relationships
 *     3. If B.m is protected
 *          - if the caller is in B
 *                then runtime resolved class must be in B or C
 *          - if the caller is in C
 *                then runtime resolved class must be in C
 *     TODO: otherwise what is thrown? <noWikiWord>AbstractMethodError?
 *     4. If B.m is an instance initialization method,
 *          invoke B.m
 *     5. If backward compatible caller does not set ACC_SUPER,
 *          invoke B.m
 *     6. If B is not a superclass of the caller, e.g. A is caller, or unrelated X
 *        is the caller, invoke B.m
 *     7. Otherwise:
 *        If superclass of caller contains name/sig match, use it
 *        Else, recursively through that superclass
 *     8. If none found, throw AbstractMethodError
 *
 * Note: there is NO mention of overriding or accessibility in determining
 * resolved method, except for if the compile-time type is protected.
 *
 * Case 1: B.m is protected
 *         Caller in A: if runtime resolved class in A.m, AbstractMethodError
 *         Caller in B: if runtime resolved class in A.m, AbstractMethodError
 * Case 2: B.m is an instance initialization method
 *         Always invoke B.m
 * Case 3: older javac, caller does not set ACC_SUPER
 *         Always invoke B.m
 * Case 4: A or X (not in hierarchy) calls invokespecial on B.m, invoke B.m
 * Case 5: Caller in B:
 *           if A.m exists, call it, else <noWikiWord>AbstractMethodError
 *         Caller in C:
 *           if B.m exists, call it
 *           if B.m does not exist, and A.m exists, call it
 */

//   TODO: classes without ACC_SUPER attribute
//   TODO: B.m is an instance initialization method

/*
 *   invokespecial <method-spec>
 *
 * invokespecial is used in certain special cases to invoke a method
 * Specifically, invokespecial is used to invoke:
 *      - the instance initialization method, <init>
 *      - a private method of this
 *      - a method in a superclass of this
 *
 * The main use of invokespecial is to invoke an object's instance
 * initialization method, <init>, during the construction phase for a new object.
 * For example, when you write in Java:
 *
 *      new StringBuffer()
 *
 * code like the following is generated:
 *      new java/lang/StringBuffer         ; create a new StringBuffer
 *      dup                                ; make an extra reference to the new instance
 *                                         ; now call an instance initialization method
 *      invokespecial java/lang/StringBuffer/<init>()V
 *                                         ; stack now contains an initialized StringBuffer.
 *
 * invokespecial is also used by the Java language by the 'super' keyword to
 * access a superclass's version of a method. For example, in the class:
 *
 *     class Example {
 *         // override equals
 *         public boolean equals(Object x) {
 *              // call Object's version of equals
 *              return super.equals(x);
 *         }
 *     }
 *
 * the 'super.equals(x)' expression is compiled to:
 *
 *     aload_0  ; push 'this' onto the stack
 *     aload_1  ; push the first argument (i.e. x) onto the stack
 *              ; now invoke Object's equals() method.
 *     invokespecial java/lang/Object/equals(Ljava/lang/Object;)Z
 *
 * Finally, invokespecial is used to invoke a private method. Remember that
 * private methods are only visible to other methods belonging the same class as
 * the private method.
 *
 * Before performing the method invocation, the class and the method identified
 * by <method-spec> are resolved. See Chapter 9 for a description of how methods
 * are resolved.
 *
 * invokespecial first looks at the descriptor given in <method-spec>, and
 * determines how many argument words the method takes (this may be zero). It
 * pops these arguments off the operand stack. Next it pops objectref (a
 * reference to an object) off the operand stack. objectref must be an instance
 * of the class named in <method-spec>, or one of its subclasses. The interpreter
 * searches the list of methods defined by the class named in <method-spec>,
 * looking for a method called methodname whose descriptor is descriptor. This
 * search is not based on the runtime type of objectref, but on the compile time
 * type given in <method-spec>.
 *
 * Once a method has been located, invokespecial calls the method. First, if
 * the method is marked as synchronized, the monitor associated with objectref is
 * entered. Next, a new stack frame structure is established on the call stack.
 * Then the arguments for the method (which were popped off the current method's
 * operand stack) are placed in local variables of the new stack frame structure.
 * arg1 is stored in local variable 1, arg2 is stored in local variable 2 and so
 * on. objectref is stored in local variable 0 (the local variable used for the
 * special Java variable this). Finally, execution continues at the first
 *instruction in the bytecode of the new method.
 *
 * Methods marked as native are handled slightly differently. For native
 * methods, the runtime system locates the platform-specific code for the method,
 * loading it and linking it into the JVM if necessary. Then the native method
 * code is executed with the arguments popped from the operand stack. The exact
 * mechanism used to invoke native methods is implementation-specific.
 *
 * When the method called by invokespecial returns, any single (or double) word
 * return result is placed on the operand stack of the current method. If the
 * invoked method was marked as synchronized, the monitor associated with
 * objectref is exited. Execution continues at the instruction that follows
 * invokespecial in the bytecode.
 *
 * Notes
 *
 * 1. In Java Virtual Machine implementations prior to version JDK 1.02, this
 * instruction was called invokenonvirtual, and was less restrictive than
 * invokespecial - it wasn't limited to invoking only superclass, private or
 * <init> methods. The class access flag ACC_SUPER (see Chapter 4) is used to
 * indicate which semantics are used by a class. In older class files, the
 * ACC_SUPER flag is unset. In all new classes, the ACC_SUPER flag should be set,
 * indicating that the restrictions enforced by invokespecial are obeyed. (In
 * practice, all the common uses of invokenonvirtual continue to be supported
 * by invokespecial, so this change should have little impact on JVM users).
 *
 */

package invokespecial;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import shared.AbstractGenerator;
import shared.AccessType;

import java.util.HashMap;
import java.util.Map;

public class Generator extends AbstractGenerator {
    public static void main (String[] args) throws Exception {
        new Generator(args).run();
    }
    public Generator(String[] args) {
        super(args);
    }

    protected Checker getChecker(Class paramClass, Class targetClass) {
        return new Checker(paramClass, targetClass);
    }

    public void run() throws Exception {
        // Specify package names
        String pkg1 = "a.";
        String pkg2 = "b.";
        String[] packages = new String[] { "", pkg1, pkg2 };

        boolean isPassed = true;

        // HIERARCHIES
        // The following triples will be used during further
        // hierarchy construction and will specify packages for A, B and C
        String[][] packageSets = new String[][] {
              {   "",   "",   "" }
            , {   "", pkg1, pkg1 }
            , {   "", pkg1, pkg2 }
            , { pkg1,   "", pkg1 }
            , { pkg1,   "", pkg2 }
            , { pkg1, pkg1,   "" }
            , { pkg1, pkg2,   "" }
            , { pkg1, pkg1, pkg1 }
            , { pkg1, pkg1, pkg2 }
            , { pkg1, pkg2, pkg1 }
            , { pkg1, pkg2, pkg2 }
        };

        String [] header = new String[] {
            String.format("%30s %35s", "Method access modifiers", "Call site location")
                , String.format("%4s  %-10s %-10s %-10s   %7s %7s %7s %7s %7s %7s %7s"
                        , "  # "
                        , "A.m()"
                        , "B.m()"
                        , "C.m()"
                        , "  A  "
                        , "pkgA"
                        , "  B  "
                        , " pkgB"
                        , "  C  "
                        , "pkgC "
                        , "  X  "
                        )
                , "-----------------------------------------------------------------------------------------------------------"
        };

        // Print header
        for (String str : header) {
            System.out.println(str);
        }

        // Iterate over all interesting package combinations
        for (String[] pkgSet : packageSets) {
            String packageA = pkgSet[0];
            String packageB = pkgSet[1];
            String packageC = pkgSet[2];

            String classNameA = packageA + "A";
            String classNameB = packageB + "B";
            String classNameC = packageC + "C";

            // For all possible access modifier combinations
            for (AccessType accessFlagA : AccessType.values()) {
                for (AccessType accessFlagB : AccessType.values()) {
                    for (AccessType accessFlagC : AccessType.values()) {
                        Map<String, byte[]> classes = new HashMap<String, byte[]>();

                        String calleeClassName = classNameB;
                        int classFlags = ACC_PUBLIC;

                        // The following hierarhcy is created:
                        //     c.C extends b.B extends a.A extends Object - base hierarchy
                        //     X extends Object - external caller
                        //     c.Caller, b.Caller, a.Caller extends Object - package callers

                        // Generate result storage
                        classes.put(
                                "Result"
                                , new ClassGenerator(
                                    "Result"
                                    , "java.lang.Object"
                                    , ACC_PUBLIC
                                    )
                                .addField(
                                    ACC_PUBLIC | ACC_STATIC
                                    , "value"
                                    , "java.lang.String"
                                    )
                                .getClassFile()
                                );

                        // Generate class A
                        classes.put(
                                classNameA
                                , new ClassGenerator(
                                    classNameA
                                    , "java.lang.Object"
                                    , classFlags
                                    )
                                .addTargetConstructor(accessFlagA)
                                .addTargetMethod(accessFlagA)
                                .addCaller(calleeClassName)
                                .getClassFile()
                                );

                        // Generate class B
                        classes.put(
                                classNameB
                                , new ClassGenerator(
                                    classNameB
                                    , classNameA
                                    , classFlags
                                    )
                                .addTargetConstructor(accessFlagB)
                                .addTargetMethod(accessFlagB)
                                .addCaller(calleeClassName)
                                .getClassFile()
                                );

                        // Generate class C
                        classes.put(
                                classNameC
                                , new ClassGenerator(
                                    classNameC
                                    , classNameB
                                    , classFlags
                                    )
                                .addTargetConstructor(accessFlagC)
                                .addTargetMethod(accessFlagC)
                                .addCaller(calleeClassName)
                                .getClassFile()
                                );

                        // Generate class X
                        String classNameX = "x.X";
                        classes.put(
                                classNameX
                                , new ClassGenerator(
                                    classNameX
                                    , "java.lang.Object"
                                    , classFlags
                                    )
                                .addTargetMethod(accessFlagC)
                                .addCaller(calleeClassName)
                                .getClassFile()
                                );

                        // Generate package callers
                        for (String pkg : packages) {
                            classes.put(
                                    pkg+"Caller"
                                    , new ClassGenerator(
                                        pkg+"Caller"
                                        , "java.lang.Object"
                                        , classFlags
                                        )
                                    .addCaller(calleeClassName)
                                    .getClassFile()
                                    );
                        }

                        String[] callSites = new String[] {
                                classNameA
                                , packageA+"Caller"
                                , classNameB
                                , packageB+"Caller"
                                , classNameC
                                , packageC+"Caller"
                                , classNameX
                        };

                        String caseDescription = String.format(
                                    "%-10s %-10s %-10s| "
                                    , classNameA + " " + accessFlagA
                                    , classNameB + " " + accessFlagB
                                    , classNameC + " " + accessFlagC
                                    );

                        boolean result = exec(classes, caseDescription, calleeClassName, classNameC, callSites);
                        isPassed = isPassed && result;
                    }
                }
            }
        }

        // Print footer
        for (int i = header.length-1; i >= 0; i--) {
            System.out.println(header[i]);
        }

        if (executeTests) {
            System.out.printf("\nEXECUTION STATUS: %s\n", (isPassed? "PASSED" : "FAILED"));
        }
    }
}
