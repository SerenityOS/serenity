/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8190198
 * @summary Test clhsdb longConstant command
 * @requires vm.hasSA
 * @library /test/lib
 * @run main/othervm ClhsdbLongConstant
 */

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jdk.test.lib.apps.LingeredApp;
import jdk.test.lib.Platform;
import jtreg.SkippedException;

public class ClhsdbLongConstant {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting ClhsdbLongConstant test");

        LingeredApp theApp = null;
        try {
            ClhsdbLauncher test = new ClhsdbLauncher();
            theApp = LingeredApp.startApp();
            System.out.println("Started LingeredApp with pid " + theApp.getPid());

            List<String> cmds = List.of(
                    "longConstant",
                    "longConstant markWord::locked_value",
                    "longConstant markWord::lock_bits",
                    "longConstant jtreg::test 6",
                    "longConstant jtreg::test");

            Map<String, List<String>> expStrMap = new HashMap<>();
            expStrMap.put("longConstant", List.of(
                    "longConstant markWord::locked_value",
                    "longConstant markWord::lock_bits",
                    "InvocationCounter::count_increment"));
            expStrMap.put("longConstant markWord::locked_value", List.of(
                    "longConstant markWord::locked_value"));
            expStrMap.put("longConstant markWord::lock_bits", List.of(
                    "longConstant markWord::lock_bits"));
            expStrMap.put("longConstant jtreg::test", List.of(
                    "longConstant jtreg::test 6"));

            Map<String, List<String>> unExpStrMap = new HashMap<>();
            unExpStrMap.put("longConstant jtreg::test", List.of(
                    "Error: java.lang.RuntimeException: No long constant named"));

            String longConstantOutput = test.run(theApp.getPid(), cmds, expStrMap, unExpStrMap);

            checkForTruncation(longConstantOutput);
        } catch (SkippedException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Test ERROR " + ex, ex);
        } finally {
            LingeredApp.stopApp(theApp);
        }
        System.out.println("Test PASSED");
    }

    private static void checkForTruncation(String longConstantOutput) throws Exception {

        // Expected values obtained from the hash_mask_in_place definition in markWord.hpp

        // Expected output snippet is of the form (on x64-64):
        // ...
        // longConstant VM_Version::CPU_SHA 17179869184
        // longConstant markWord::age_shift 3
        // longConstant markWord::hash_mask_in_place 549755813632
        // ...

        checkLongValue("markWord::hash_mask_in_place",
                       longConstantOutput,
                       Platform.is64bit() ? 549755813632L: 4294967168L);

        String arch = System.getProperty("os.arch");
        if (arch.equals("amd64") || arch.equals("i386") || arch.equals("x86")) {
            // Expected value obtained from the CPU_SHA definition in vm_version_x86.hpp
            checkLongValue("VM_Version::CPU_SHA",
                           longConstantOutput,
                           17179869184L);
        }
    }

    private static void checkLongValue(String constName, String longConstantOutput,
                                       long checkValue) throws Exception {

        String[] snippets = longConstantOutput.split(constName);
        String[] words = snippets[1].split("\\R");
        long readValue = Long.parseLong(words[0].trim());
        if (readValue != checkValue) {
            throw new Exception ("Reading " + constName + ". Expected " + checkValue +
                                 ". Obtained " + readValue + " instead.");
        }
    }
}
