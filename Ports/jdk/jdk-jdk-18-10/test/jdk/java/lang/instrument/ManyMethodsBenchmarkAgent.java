/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8046246
 * @summary Tests and benchmarks the JVMTI RedefineClasses when a
 *          single class (and its parent) contains many methods.
 *
 * @modules jdk.compiler
 *          java.instrument
 *          jdk.zipfs
 * @run build ManyMethodsBenchmarkApp ManyMethodsBenchmarkAgent
 * @run shell MakeJAR3.sh ManyMethodsBenchmarkAgent 'Can-Retransform-Classes: true'
 * @run main/othervm -javaagent:ManyMethodsBenchmarkAgent.jar ManyMethodsBenchmarkApp
 */
import java.lang.instrument.*;

public class ManyMethodsBenchmarkAgent
{
    public  static boolean fail = false;
    public  static boolean completed = false;
    private static  Instrumentation instrumentation;

    public static void
    premain(    String agentArgs,
                Instrumentation instrumentation) {
        System.out.println("ManyMethodsBenchmarkAgent started");
        ManyMethodsBenchmarkAgent.instrumentation = instrumentation;
        System.out.println("ManyMethodsBenchmarkAgent finished");
    }

    static void instr() {
        System.out.println("ManyMethodsBenchmarkAgent.instr started");

        Class[] allClasses = instrumentation.getAllLoadedClasses();

        for (int i = 0; i < allClasses.length; i++) {
            Class klass = allClasses[i];
            String name = klass.getName();
            if (!name.equals("Base")) {
                continue;
            }
            System.err.println("Instrumenting the class: " + klass);

            try {
                instrumentation.retransformClasses(klass);
            } catch (Throwable e) {
                System.err.println("Error: bad return from retransform: " + klass);
                System.err.println("  ERROR: " + e);
                fail = true;
            }
        }
        completed = true;
        System.out.println("ManyMethodsBenchmarkAgent.instr finished");
    }

}
