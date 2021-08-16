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
 * INVOKE_INTERFACE EXPECTED RESULTS
 *
 * Let C be the class of objectref. The actual method to be invoked is selected
 * by the following lookup procedure:
 *     - If C contains a declaration for an instance method with the same name
 *     and descriptor as the resolved method, then this is the method to be
 *     invoked, and the lookup procedure terminates.
 *
 *     - Otherwise, if C has a superclass, this same lookup procedure is
 *     performed recursively using the direct superclass of C; the method to be
 *     invoked is the result of the recursive invocation of this lookup
 *     procedure.
 *
 * Otherwise, if the class of objectref does not implement the resolved
 * interface, invokeinterface throws an IncompatibleClassChangeError?.
 *
 * Otherwise, if no method matching the resolved name and descriptor is
 * selected, invokeinterface throws an AbstractMethodError?.
 *
 * Otherwise, if the selected method is not public, invokeinterface throws an
 * IllegalAccessError. Note that it cannot be private because private methods
 * are ignored when searching for an interface method.
 *
 * My translation:
 *      1. Resolve compile-time class/method.
 *      2. Look up runtime class C, if it contains a name/signature match,
 *      and it is not private, invoke it.
 *      3. If it does not, recursively lookup direct superclass of C.
 *      4. If selected method is not public, throw IllegalAccessError
 *
 * InvokeInterface Results:
 *    - A interface class, declares A.m
 *    - A compile-time resolved class
 *    - C runtime resolved class
 *    - InvokeInterface will ALWAYS invoke C.m if C.m exists and is not private,
 *    regardless of overriding or accessibility
 *    - InvokeInterface will invoke a non-private B.m if C.m does not exist,
 *    regardless of overriding or accessibility
 *
 * Note: assuming Interface is public
 *
 * TODO: member interfaces can be protected and private and have special hiding
 * rules (JLS 8.5)
 */

package invokeinterface;

import static jdk.internal.org.objectweb.asm.Opcodes.*;
import shared.AbstractGenerator;
import shared.AccessType;
import shared.Utils;

import java.util.HashMap;
import java.util.Map;

public class Generator extends AbstractGenerator {
    public Generator(String[] args) {
        super(args);
    }

    protected Checker getChecker(Class paramClass, Class targetClass) {
        return new Checker(paramClass, targetClass);
    }

    public static void main (String[] args) throws Exception {
        new Generator(args).run();
    }

    private void run() throws Exception {
        // Specify package names
        String pkg1 = "a.";
        String pkg2 = "b.";
        String pkg3 = "c.";
        String pkgIntf = "i.";
        String[] packages = new String[] { "", pkg1, pkg2, pkg3, pkgIntf };

        int testNum = 0;
        boolean isPassed = true;

        // Hierarchy
        // The following triples will be used during further
        // hierarchy construction and will specify packages for A, B and C
        String[][] packageSets = new String[][] {
              {   "",   "",   "", ""}
            , {   "",   "",   "", pkgIntf }

            , {   "", pkg1, pkg1, "" }
            , {   "", pkg1, pkg1, pkg1 }
            , {   "", pkg1, pkg1, pkgIntf }

            , {   "", pkg1, pkg2, "" }
            , {   "", pkg1, pkg2, pkg1}
            , {   "", pkg1, pkg2, pkg2}
            , {   "", pkg1, pkg2, pkgIntf}

            , { pkg1, pkg1, pkg1, pkg1 }
            , { pkg1, pkg1, pkg1, pkgIntf }

            , { pkg1, pkg1, pkg2, pkg1 }
            , { pkg1, pkg1, pkg2, pkg2 }
            , { pkg1, pkg1, pkg2, pkgIntf }

            , { pkg1, pkg2, pkg1, pkg1 }
            , { pkg1, pkg2, pkg1, pkg2 }
            , { pkg1, pkg2, pkg1, pkgIntf }

            , { pkg1, pkg2, pkg2, pkg1 }
            , { pkg1, pkg2, pkg2, pkg2 }
            , { pkg1, pkg2, pkg2, pkgIntf }
        };

        String [] header = new String[] {
            String.format("%30s %68s %25s", "Method access modifiers", "Call site location", "Status")
                , String.format("%5s  %-12s %-12s %-12s %-12s   %7s %7s %7s %7s %7s %7s %7s"
                        , "  # "
                        , "A.m()"
                        , "B.m()"
                        , "C.m()"
                        , "I.m()"
                        , "  C  "
                        , "pkgC "
                        , "  B  "
                        , " pkgB"
                        , "  A  "
                        , "pkgA"
                        , "Intf"
                        )
                , "--------------------------------------------------------------------------------------------------------------------"
        };

        for (String aHeader : header) {
            System.out.println(aHeader);
        }

        for (String[] pkgSet : packageSets) {
            String packageA = pkgSet[0];
            String packageB = pkgSet[1];
            String packageC = pkgSet[2];

            String packageIntf = pkgSet[3];

            String classNameA = packageA + "A";
            String classNameB = packageB + "B";
            String classNameC = packageC + "C";
            String classNameIntf = packageIntf + "I";

            // For all possible access modifier combinations
            for (AccessType accessA : AccessType.values()) {
                for (AccessType accessB : AccessType.values()) {
                    for (AccessType accessC : AccessType.values()) {
                        for (AccessType accessIntf : AccessType.values()) {

                            if (accessIntf == AccessType.UNDEF) {
                                continue;
                            }

                            for (int I = 0; I < 4; I++) {
                                boolean isAbstractA = ((I & 1) != 0);
                                boolean isAbstractB = ((I & 2) != 0);

                                testNum++;

                                Map<String, byte[]> classes = new HashMap<String, byte[]>();

                                // TODO: add non-PUBLIC interfaces, then particular call sites will affect the results

                                // Generate interface Intf
                                classes.put(
                                        classNameIntf
                                        , new ClassGenerator( classNameIntf
                                                            , "java.lang.Object"
                                                            , ACC_ABSTRACT | ACC_INTERFACE | accessIntf.value())
                                            .addTargetMethod(AccessType.PUBLIC)
                                            .getClassFile()
                                        );

                                // Generate class A
                                classes.put(
                                        classNameA
                                        , new ClassGenerator( classNameA
                                                            , "java.lang.Object"
                                                            , ACC_PUBLIC | ( isAbstractA ? ACC_ABSTRACT : 0))
                                            .addTargetMethod(accessA)
                                            .addCaller(classNameIntf)
                                            .getClassFile()
                                        );

                                // Generate class B
                                classes.put(
                                        classNameB
                                        , new ClassGenerator( classNameB
                                                            , classNameA
                                                            , ACC_PUBLIC | ( isAbstractB ? ACC_ABSTRACT : 0)
                                                            , new String[] { Utils.getInternalName(classNameIntf) })
                                            .addTargetMethod(accessB)
                                            .addCaller(classNameIntf)
                                            .getClassFile()
                                        );

                                // Generate class C
                                classes.put( classNameC
                                           , new ClassGenerator( classNameC, classNameB )
                                               .addTargetMethod(accessC)
                                               .addCaller(classNameIntf)
                                               .getClassFile()
                                           );

                                // Generate package callers
                                for (String pkg : packages) {
                                    classes.put( pkg+"Caller"
                                               , new ClassGenerator(pkg+"Caller")
                                                   .addCaller(classNameIntf)
                                                   .getClassFile()
                                               );
                                }

                                String caseDescription =
                                        String.format("%-12s %-12s %-12s %-12s| "
                                            , (isAbstractA ? "! " : "  ") + classNameA + " " + accessA
                                            , (isAbstractB ? "! " : "  ") + classNameB + " " + accessB
                                            , classNameC + " " + accessC
                                            , accessIntf + " " + classNameIntf
                                            );

                                String[] callSites = new String[] {
                                        classNameC
                                        , packageC+"Caller"
                                        , classNameB
                                        , packageB+"Caller"
                                        , classNameA
                                        , packageA+"Caller"
                                        , packageIntf+"Caller"
                                };

                                boolean result = exec(classes, caseDescription, classNameIntf, classNameC, callSites);
                                isPassed = isPassed && result;
                            }
                        }
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
