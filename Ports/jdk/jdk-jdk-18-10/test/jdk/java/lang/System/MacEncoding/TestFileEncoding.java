/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

/*
 * @test
 * @bug 8011194
 * @summary Test value of file.encoding for corresponding value of LANG, etc
 * @library ../../../../tools/launcher/ ../
 * @modules jdk.compiler
 * @build TestHelper TestFileEncoding ExpectedEncoding
 * @run main TestFileEncoding UTF-8
 * @run main/othervm -Dfile.encoding=MyEncoding -DuserEncoding=MyEncoding TestFileEncoding MyEncoding
 * @run main TestFileEncoding UTF-8 en_US.UTF-8
 * @run main/othervm -Dfile.encoding=MyEncoding -DuserEncoding=MyEncoding TestFileEncoding MyEncoding en_US.UTF-8
 * @run main TestFileEncoding US-ASCII C
 * @run main/othervm -Dfile.encoding=MyEncoding -DuserEncoding=MyEncoding TestFileEncoding MyEncoding C
 * @author Brent Christian
 */

/**
 * Setup the environment and run a sub-test to check the expected value of
 * file.encoding, based on the value(s) of encoding-related environment vars
 * (LANG, LC_ALL, LC_CTYPE).
 *
 * The first argument (required) is the expected value of the
 * file.encoding System property.
 * The second argument (optional) is the value to set to the LANG/etc env vars.
 */
public class TestFileEncoding {
    private static final String TEST_NAME = "ExpectedEncoding";

    private String expectedEncoding; // Expected value for file.encoding
    private String langVar = null; // Value to set for LANG, etc

    private static Set<String> envToRm = new HashSet<>(3);
    static {
        // Take these vars out of the test's run environment, possibly adding
        // our own value back in.
        envToRm.add("LANG");
        envToRm.add("LC_ALL");
        envToRm.add("LC_CTYPE");
    }

    public TestFileEncoding(String expectedEncoding) {
        this.expectedEncoding = expectedEncoding;
    }

    public TestFileEncoding(String expectedEncoding, String langVar) {
        this.expectedEncoding = expectedEncoding;
        this.langVar = langVar;
    }

    /*
     * Launch ExpectedEncoding with the given parameters, check for the
     * expected file.encoding.
     */
    private void run() {
        String testClasses = System.getProperty("test.classes");

        // Pick up VM opts
        String vmOptsStr = System.getProperty("test.vm.opts");
        System.out.println("test.vm.opts: " + vmOptsStr);
        String[] vmOpts = new String[0];
        if (vmOptsStr != null && !"".equals(vmOptsStr)) {
            vmOpts = vmOptsStr.split(" ");
            System.out.println("found vm options:");
            for (String opt : vmOpts) {
                System.out.println("  <" + opt + ">");
            }
        }

        // Build java cmd
        LinkedList<String> cmdList = new LinkedList<>();
        cmdList.add(TestHelper.javaCmd);
        for (String vmOpt : vmOpts) {
            if (vmOpt != null && !vmOpt.equals("")) {
                cmdList.add(vmOpt);
            }
        }

        // See if the user specified a file.encoding that we should pass through
        String userEncoding = System.getProperty("userEncoding");
        if (userEncoding != null) {
            cmdList.add("-Dfile.encoding="+userEncoding);
        }

        cmdList.add("-cp");
        cmdList.add(testClasses);
        cmdList.add(TEST_NAME);
        cmdList.add(expectedEncoding);
        cmdList.add("skip"); // ignore sun.jnu.encoding for this test

        String cmdArray[] = new String[cmdList.size()];
        cmdList.toArray(cmdArray);

        // Run the test(s)
        if (langVar == null) {
            System.out.println("TestFileEncoding: Running with no envvars set");
            TestHelper.TestResult tr = TestHelper.doExec(null, envToRm,
                                                         cmdArray);
            checkResult(tr);
        } else {
            runWithEnvVar("LANG", cmdArray);
            runWithEnvVar("LC_ALL", cmdArray);
            runWithEnvVar("LC_CTYPE", cmdArray);
        }
    }

    /*
     * Run the test, setting the environment named by envVarName to the value
     * in langVar.
     */
    private void runWithEnvVar(String envVarName, String[] cmdArray) {
        Map<String, String> envToAdd = new HashMap<>(1);
        TestHelper.TestResult tr = null;

        System.out.println("TestFileEncoding: Running with " + envVarName + "=" + langVar);
        envToAdd.put(envVarName, langVar);
        tr = TestHelper.doExec(envToAdd, envToRm, cmdArray);
        checkResult(tr);
    }

    private void checkResult(TestHelper.TestResult tr) {
        System.out.println(tr);
        if (!tr.isOK()) {
            throw new RuntimeException("TEST FAILED: !tr.isOK()");
        }
    }

    public static void main(String[] args) {
        TestFileEncoding cfe = null;
        if (!TestHelper.isMacOSX) {
            System.out.println("Test is currently only for Mac OS X - pass.");
            return;
        }
        if (args.length == 1) {
            cfe = new TestFileEncoding(args[0]);
        } else if (args.length == 2) {
            cfe = new TestFileEncoding(args[0], args[1]);
        } else {
            System.out.println("Usage: TestFileEncoding <expected file.encoding>");
            System.out.println("       TestFileEncoding <expected file.encoding> <value for LANG/etc env var>");
            return;
        }
        cfe.run();
    }
}
