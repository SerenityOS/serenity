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

package invokevirtual;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import shared.AbstractGenerator;
import shared.AccessType;

import java.util.HashMap;
import java.util.Map;

public class Generator extends AbstractGenerator {
    public Generator(String[] args) {
        super(args);
    }

    public static void main (String[] args) throws Exception {
        new Generator(args).run();
    }

    protected Checker getChecker(Class paramClass, Class targetClass) {
        return new Checker(paramClass, targetClass);
    }

    private void run() throws Exception {
        // Specify package names
        String pkg1 = "a.";
        String pkg2 = "b.";
        String pkg3 = "c.";
        String[] packages = new String[] { "", pkg1, pkg2, pkg3 };

        boolean isPassed = true;

        // Hierarchy
        // The following triples will be used during further
        // hierarchy construction and will specify packages for A, B and C
        String[][] packageSets = new String[][] {
                {   "",   "",   "" }
                , {   "", pkg1, pkg1 }
                , {   "", pkg1, pkg2 }
                , { pkg1, pkg1, pkg1 }
                , { pkg1, pkg1, pkg2 }
                , { pkg1, pkg2, pkg1 }
                , { pkg1, pkg2, pkg2 }
        };

        String [] header = new String[] {
                String.format("%30s %45s %20s", "Method access modifiers", "Call site location", "Status")
                , String.format("%4s  %-12s %-12s %-12s   %7s %7s %7s %7s %7s %7s"
                        , "  # "
                        , "A.m()"
                        , "B.m()"
                        , "C.m()"
                        , "  A  "
                        , "pkgA "
                        , "  B  "
                        , " pkgB"
                        , "  C  "
                        , "pkgC "
                        )
                , "-------------------------------------------------------------------------------------------------"
        };

        for (String str : header) {
            System.out.println(str);
        }

        for (String[] pkgSet : packageSets) {
            String packageA = pkgSet[0];
            String packageB = pkgSet[1];
            String packageC = pkgSet[2];

            String classNameA = packageA + "A";
            String classNameB = packageB + "B";
            String classNameC = packageC + "C";

            String staticCallerParam = classNameA;

            // For all possible access modifier combinations
            for (AccessType accessA : AccessType.values()) {
                for (AccessType accessB : AccessType.values()) {
                    for (AccessType accessC : AccessType.values()) {

                        if (accessA == AccessType.UNDEF) {
                            continue;
                        }

                        for (int I = 0; I < 4; I++) {
                            boolean isAbstractA = ((I & 1) != 0);
                            boolean isAbstractB = ((I & 2) != 0);

                            Map<String, byte[]> classes = new HashMap<String, byte[]>();

                            // Generate class A
                            classes.put(
                                    classNameA
                                    , new ClassGenerator( classNameA
                                                        , "java.lang.Object"
                                                        , ACC_PUBLIC | (isAbstractA ? ACC_ABSTRACT : 0))
                                        .addTargetMethod( accessA
                                                        , (isAbstractA ? ACC_ABSTRACT : 0))
                                        .addCaller(staticCallerParam)
                                        .getClassFile()
                            );

                            // Generate class B
                            classes.put(
                                    classNameB
                                    , new ClassGenerator( classNameB
                                                        , classNameA
                                                        , ACC_PUBLIC | (isAbstractB ? ACC_ABSTRACT : 0))
                                    .addTargetMethod( accessB
                                                    , (isAbstractB ? ACC_ABSTRACT : 0))
                                    .addCaller(staticCallerParam)
                                    .getClassFile()
                            );

                            // Generate class C
                            classes.put(
                                    classNameC
                                    , new ClassGenerator(classNameC, classNameB)
                                        .addTargetMethod(accessC)
                                        .addCaller(staticCallerParam)
                                        .getClassFile()
                            );

                            // Generate package callers
                            for (String pkg : packages) {
                                classes.put(
                                        pkg+"Caller"
                                        , new ClassGenerator(pkg+"Caller")
                                        .addCaller(staticCallerParam)
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
                            };


                            String caseDescription =
                                    String.format("%-12s %-12s %-12s| "
                                        , (isAbstractA ? "! " : "  ") + classNameA + " " + accessA
                                        , (isAbstractB ? "! " : "  ") + classNameB + " " + accessB
                                        , classNameC + " " + accessC
                                    );

                            boolean result = exec(classes, caseDescription, staticCallerParam, classNameC, callSites);
                            isPassed = isPassed && result;
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
