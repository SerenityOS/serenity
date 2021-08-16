/*
 * Copyright (c) 2018, 2020, Red Hat, Inc. All rights reserved.
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
 * common code to run and validate tests of code generation for
 * volatile ops on AArch64
 *
 * incoming args are <testclass> <testtype>
 *
 * where <testclass> in {TestVolatileLoad,
 *                       TestVolatileStore,
 *                       TestUnsafeVolatileLoad,
 *                       TestUnsafeVolatileStore,
 *                       TestUnsafeVolatileCAS,
 *                       TestUnsafeVolatileWeakCAS,
 *                       TestUnsafeVolatileCAE,
 *                       TestUnsafeVolatileGAS}
 * and <testtype> in {G1,
 *                    Serial,
 *                    Parallel,
 *                    Shenandoah,
 *                    ShenandoahIU}
 */


package compiler.c2.aarch64;

import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.io.*;

import jdk.test.lib.Asserts;
import jdk.test.lib.compiler.InMemoryJavaCompiler;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import sun.hotspot.WhiteBox;

// runner class that spawns a new JVM to exercises a combination of
// volatile MemOp and GC. The ops are compiled with the dmb -->
// ldar/stlr transforms either enabled or disabled. this runner parses
// the PrintOptoAssembly output checking that the generated code is
// correct.

public class TestVolatiles {
    public void runtest(String classname, String testType) throws Throwable {
        // n.b. clients omit the package name for the class
        String fullclassname = "compiler.c2.aarch64." + classname;
        // build up a command line for the spawned JVM
        String[] procArgs;
        int argcount;
        // add one or two extra arguments according to test type
        // i.e. GC type plus GC conifg
        switch(testType) {
        case "G1":
            argcount = 9;
            procArgs = new String[argcount];
            procArgs[argcount - 2] = "-XX:+UseG1GC";
            break;
        case "Parallel":
            argcount = 9;
            procArgs = new String[argcount];
            procArgs[argcount - 2] = "-XX:+UseParallelGC";
            break;
        case "Serial":
            argcount = 9;
            procArgs = new String[argcount];
            procArgs[argcount - 2] = "-XX:+UseSerialGC";
            break;
        case "Shenandoah":
            argcount = 10;
            procArgs = new String[argcount];
            procArgs[argcount - 3] = "-XX:+UnlockExperimentalVMOptions";
            procArgs[argcount - 2] = "-XX:+UseShenandoahGC";
            break;
        case "ShenandoahIU":
            argcount = 11;
            procArgs = new String[argcount];
            procArgs[argcount - 4] = "-XX:+UnlockExperimentalVMOptions";
            procArgs[argcount - 3] = "-XX:+UseShenandoahGC";
            procArgs[argcount - 2] = "-XX:ShenandoahGCMode=iu";
            break;
        default:
            throw new RuntimeException("unexpected test type " + testType);
        }

        // fill in arguments common to all cases

        // the first round of test enables transform of barriers to
        // use acquiring loads and releasing stores by setting arg
        // zero appropriately. this arg is reset in the second run to
        // disable the transform.

        procArgs[0] = "-XX:+UseCompressedOops";
        procArgs[1] = "-XX:-BackgroundCompilation";
        procArgs[2] = "-XX:-TieredCompilation";
        procArgs[3] = "-XX:+PrintOptoAssembly";
        procArgs[4] = "-XX:CompileCommand=compileonly," + fullclassname + "::" + "test*";
        procArgs[5] = "--add-exports";
        procArgs[6] = "java.base/jdk.internal.misc=ALL-UNNAMED";
        procArgs[argcount - 1] = fullclassname;

        runtest(classname, testType, true, procArgs);

        if (!classname.equals("TestUnsafeVolatileGAA")) {
            procArgs[0] = "-XX:-UseCompressedOops";
            runtest(classname, testType, false, procArgs);
        }
    }


    public void runtest(String classname, String testType, boolean useCompressedOops, String[] procArgs) throws Throwable {
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(procArgs);
        OutputAnalyzer output = new OutputAnalyzer(pb.start());

        output.stderrShouldBeEmptyIgnoreVMWarnings();
        output.stdoutShouldNotBeEmpty();
        output.shouldHaveExitValue(0);

        // check the output for the correct asm sequence as
        // appropriate to test class, test type and whether transform
        // was applied

        checkoutput(output, classname, testType, useCompressedOops);
    }

    // skip through output returning a line containing the desireed
    // substring or null
    private String skipTo(Iterator<String> iter, String substring)
    {
        while (iter.hasNext()) {
            String nextLine = iter.next();
            if (nextLine.matches(".*" + substring + ".*")) {
                return nextLine;
            }
        }
        return null;
    }

    // locate the start of compiler output for the desired method and
    // then check that each expected instruction occurs in the output
    // in the order supplied. throw an excpetion if not found.
    // n.b. the spawned JVM's output is included in the exception
    // message to make it easeir to identify what is missing.

    private boolean checkCompile(Iterator<String> iter, String methodname, String[] expected, OutputAnalyzer output, boolean do_throw)
    {
        // trace call to allow eyeball check of what we are checking against
        System.out.println("checkCompile(" + methodname + ",");
        String sepr = "  { ";
        for (String s : expected) {
            System.out.print(sepr);
            System.out.print(s);
            sepr = ",\n    ";
        }
        System.out.println(" })");

        // look for the start of an opto assembly print block
        String match = skipTo(iter, Pattern.quote("{method}"));
        if (match == null) {
            if (do_throw) {
                throw new RuntimeException("Missing compiler output for " + methodname + "!\n\n" + output.getOutput());
            }
            return false;
        }
        // check the compiled method name is right
        match = skipTo(iter, Pattern.quote("- name:"));
        if (match == null) {
            if (do_throw) {
                throw new RuntimeException("Missing compiled method name!\n\n" + output.getOutput());
            }
            return false;
        }
        if (!match.contains(methodname)) {
            if (do_throw) {
                throw new RuntimeException("Wrong method " + match + "!\n  -- expecting " + methodname + "\n\n" + output.getOutput());
            }
            return false;
        }
        // make sure we can match each expected term in order
        for (String s : expected) {
            match = skipTo(iter, s);
            if (match == null) {
                if (do_throw) {
                    throw new RuntimeException("Missing expected output " + s + "!\n\n" + output.getOutput());
                }
                return false;
            }
        }
        return true;
    }

    // check for expected asm output from a volatile load

    private void checkload(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        // we shoud see this same sequence for normal or unsafe volatile load
        // for both int and Object fields

        String[] matches;
        matches = new String[] {
            "ldarw",
            "membar_acquire \\(elided\\)",
            "ret"
        };
        checkCompile(iter, "testInt", matches, output, true);

        matches = new String[] {
            useCompressedOops ? "ldarw?" : "ldar",
            "membar_acquire \\(elided\\)",
            "ret"
        };
        checkCompile(iter, "testObj", matches, output, true);

    }

    // check for expected asm output from a volatile store

    private void checkstore(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;

        // non object stores are straightforward
        // this is the sequence of instructions for all cases
        matches = new String[] {
            "membar_release \\(elided\\)",
            "stlrw",
            "membar_volatile \\(elided\\)",
            "ret"
        };
        checkCompile(iter, "testInt", matches, output, true);

        // object stores will be as above except for when the GC
        // introduces barriers for card marking
        switch (testType) {
        default:
            // this is the basic sequence of instructions
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "stlrw?" : "stlr",
                "membar_volatile \\(elided\\)",
                "ret"
            };
            break;
        case "G1":
            // a card mark volatile barrier should be generated
            // before the card mark strb
            //
            // following the fix for 8225776 the G1 barrier is now
            // scheduled out of line after the membar volatile and
            // and subsequent return
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "stlrw?" : "stlr",
                "membar_volatile \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
             // Shenandoah generates normal object graphs for
             // volatile stores
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "stlrw?" : "stlr",
                "membar_volatile \\(elided\\)",
                "ret"
            };
            break;
        }

        checkCompile(iter, "testObj", matches, output, true);
    }

    // check for expected asm output from a volatile cas

    private void checkcas(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "cmpxchgw" },
            { "testLong", "cmpxchg" },
            { "testByte", "cmpxchgb" },
            { "testShort", "cmpxchgs" },
        };

        for (String[] test : tests) {
            // non object stores are straightforward
            // this is the sequence of instructions for all cases
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

        // object stores will be as above except for when the GC
        // introduces barriers for card marking
        switch (testType) {
        default:
            // this is the basic sequence of instructions
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "strb",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        case "G1":
            // a card mark volatile barrier should be generated
            // before the card mark strb
            //
            // following the fix for 8225776 the G1 barrier is now
            // scheduled out of line after the membar acquire and
            // and subsequent return
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "membar_acquire \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            // For volatile CAS, Shenanodoah generates normal
            // graphs with a shenandoah-specific cmpxchg
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq_shenandoah" : "cmpxchg_acq_shenandoah",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        }
        checkCompile(iter, "testObj", matches, output, true);
    }

    private void checkcae(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        ListIterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "cmpxchgw" },
            { "testLong", "cmpxchg" },
            { "testByte", "cmpxchgb" },
            { "testShort", "cmpxchgs" },
        };

        for (String[] test : tests) {
            // non object stores are straightforward
            // this is the sequence of instructions for all cases
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

        // object stores will be as above except for when the GC
        // introduces barriers for card marking
        switch (testType) {
        default:
            // this is the basic sequence of instructions
            matches = new String[] {
                "membar_release \\(elided\\)",
                "strb",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };

            // card marking store may be scheduled before or after
            // the cmpxchg so try both sequences.
            int idx = iter.nextIndex();
            if (!checkCompile(iter, "testObj", matches, output, false)) {
                iter = output.asLines().listIterator(idx);

                matches = new String[] {
                    "membar_release \\(elided\\)",
                    useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                    "strb",
                    "membar_acquire \\(elided\\)",
                    "ret"
                };

                checkCompile(iter, "testObj", matches, output, true);
            }
            return;

        case "G1":
            // a card mark volatile barrier should be generated
            // before the card mark strb
            //
            // following the fix for 8225776 the G1 barrier is now
            // scheduled out of line after the membar acquire and
            // and subsequent return
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq" : "cmpxchg_acq",
                "membar_acquire \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            // For volatile CAS, Shenanodoah generates normal
            // graphs with a shenandoah-specific cmpxchg
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "cmpxchgw?_acq_shenandoah" : "cmpxchg_acq_shenandoah",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        }
        checkCompile(iter, "testObj", matches, output, true);
    }

    private void checkgas(OutputAnalyzer output, String testType, boolean useCompressedOops) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "atomic_xchgw" },
            { "testLong", "atomic_xchg" },
        };

        for (String[] test : tests) {
            // non object stores are straightforward
            // this is the sequence of instructions for all cases
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

        // object stores will be as above except for when the GC
        // introduces barriers for card marking
        switch (testType) {
        default:
            // this is the basic sequence of instructions
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "atomic_xchgw?_acq" : "atomic_xchg_acq",
                "strb",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        case "G1":
            // a card mark volatile barrier should be generated
            // before the card mark strb
            //
            // following the fix for 8225776 the G1 barrier is now
            // scheduled out of line after the membar acquire and
            // and subsequent return
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "atomic_xchgw?_acq" : "atomic_xchg_acq",
                "membar_acquire \\(elided\\)",
                "ret",
                "membar_volatile",
                "dmb ish",
                "strb"
            };
            break;
        case "Shenandoah":
        case "ShenandoahIU":
            matches = new String[] {
                "membar_release \\(elided\\)",
                useCompressedOops ? "atomic_xchgw?_acq" : "atomic_xchg_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            break;
        }

        checkCompile(iter, "testObj", matches, output, true);
    }

    private void checkgaa(OutputAnalyzer output, String testType) throws Throwable
    {
        Iterator<String> iter = output.asLines().listIterator();

        String[] matches;
        String[][] tests = {
            { "testInt", "get_and_addI" },
            { "testLong", "get_and_addL" },
        };

        for (String[] test : tests) {
            // non object stores are straightforward
            // this is the sequence of instructions for all cases
            matches = new String[] {
                "membar_release \\(elided\\)",
                test[1] + "_acq",
                "membar_acquire \\(elided\\)",
                "ret"
            };
            checkCompile(iter, test[0], matches, output, true);
        }

    }

    // perform a check appropriate to the classname

    private void checkoutput(OutputAnalyzer output, String classname, String testType, boolean useCompressedOops) throws Throwable
    {
        // trace call to allow eyeball check of what is being checked
        System.out.println("checkoutput(" +
                           classname + ", " +
                           testType + ")\n" +
                           output.getOutput());

        switch (classname) {
        case "TestVolatileLoad":
            checkload(output, testType, useCompressedOops);
            break;
        case "TestVolatileStore":
            checkstore(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileLoad":
            checkload(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileStore":
            checkstore(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileCAS":
        case "TestUnsafeVolatileWeakCAS":
            checkcas(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileCAE":
            checkcae(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileGAS":
            checkgas(output, testType, useCompressedOops);
            break;
        case "TestUnsafeVolatileGAA":
            checkgaa(output, testType);
            break;
        }
    }
}
