/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.uncommontrap;

import jdk.test.lib.Asserts;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Utility tool aimed to verify presence or absence of specified uncommon trap
 * in compilation log.
 */
public class Verifier {
    public static final String PROPERTIES_FILE_SUFFIX = ".verify.properties";
    public static final String VERIFICATION_SHOULD_BE_SKIPPED
            = "uncommon.trap.verification.skipped";
    public static final String UNCOMMON_TRAP_NAME = "uncommon.trap.name";
    public static final String UNCOMMON_TRAP_BCI = "uncommon.trap.bci";
    public static final String UNCOMMON_TRAP_COMMENT = "uncommon.trap.comment";
    public static final String UNCOMMON_TRAP_ACTION = "uncommon.trap.action";
    public static final String UNCOMMON_TRAP_SHOULD_EMITTED
            = "uncommon.trap.emitted";
    public static final String UNCOMMON_TRAP_SHOULD_FIRED
            = "uncommon.trap.fired";

    private static final String EMITTED_TRAP_PATTERN
            = "<uncommon_trap bci='%s' reason='%s' action='%s' "
                    + "comment='%s'";
    private static final String FIRED_TRAP_PATTERN
            = "<uncommon_trap thread='[0-9]*' reason='%s' action='%s'";
    private static final String JVMS_PATTERN = "<jvms bci='%s'";

    public static void main(String args[]) {
        if (args.length == 0) {
            throw new Error("At least one argument containing name of "
                    + "compilation log file is expected");
        }

        for (String compLogFile : args) {
            try {
                verify(Paths.get(compLogFile));
            } catch (IOException e) {
                throw new Error("Unable to process compilation log.", e);
            }
        }
    }

    private static void verify(Path compLogFile) throws IOException {
        Path propertiesFile = Paths.get(compLogFile.toString() +
                PROPERTIES_FILE_SUFFIX);

        Properties properties = new Properties();
        try (FileReader reader = new FileReader(propertiesFile.toFile())) {
            properties.load(reader);
        }

        if (Boolean.valueOf(properties.getProperty(
                VERIFICATION_SHOULD_BE_SKIPPED, "false"))) {
            System.out.println("Skipping verification for log file: "
                    + compLogFile.toString());
            return;
        }

        System.out.println("Verifying log file: " + compLogFile.toString());

        List<String> compLogContent = Files.readAllLines(compLogFile);
        verifyUncommonTrapEmitted(properties, compLogContent);
        verifyUncommonTrapFired(properties, compLogContent);
    }

    private static void verifyUncommonTrapEmitted(Properties properties,
            List<String> compLogContent)  {
        String emittedTrapRE = String.format(EMITTED_TRAP_PATTERN,
                properties.getProperty(UNCOMMON_TRAP_BCI, ".*"),
                properties.getProperty(UNCOMMON_TRAP_NAME, ".*"),
                properties.getProperty(UNCOMMON_TRAP_ACTION, ".*"),
                properties.getProperty(UNCOMMON_TRAP_COMMENT, ".*"));
        Pattern pattern = Pattern.compile(emittedTrapRE);

        long trapsCount = compLogContent.stream()
                .filter(line -> pattern.matcher(line).find())
                .count();

        boolean shouldBeEmitted = Boolean.valueOf(
                properties.getProperty(UNCOMMON_TRAP_SHOULD_EMITTED));

        Asserts.assertEQ(shouldBeEmitted, trapsCount > 0, String.format(
                "Uncommon trap that matches following string in compilation log"
                        + " should %sbe emitted: %s.",
                (shouldBeEmitted ? " " : "not "), emittedTrapRE));
    }

    private static void verifyUncommonTrapFired(Properties properties,
            List<String> compLogContent) {
        String firedTrapRE = String.format(FIRED_TRAP_PATTERN,
                properties.getProperty(UNCOMMON_TRAP_NAME, ".*"),
                properties.getProperty(UNCOMMON_TRAP_ACTION, ".*"));
        String jvmsRE = String.format(JVMS_PATTERN,
                properties.getProperty(UNCOMMON_TRAP_BCI, ".*"));

        boolean trapFired = false;
        Pattern firedTrapPattern = Pattern.compile(firedTrapRE);
        Pattern jvmsPattern = Pattern.compile(jvmsRE);

        Iterator<String> iterator = compLogContent.iterator();
        while (iterator.hasNext() && !trapFired) {
            trapFired = firedTrapPattern.matcher(iterator.next()).find()
                    && jvmsPattern.matcher(iterator.next()).find();
        }

        boolean shouldBeFired = Boolean.valueOf(
                properties.getProperty(UNCOMMON_TRAP_SHOULD_FIRED));
        Asserts.assertEQ(shouldBeFired, trapFired, String.format(
                "Uncommon trap that matches following string in compilation log"
                        + " should %sbe fired: %s.",
                (shouldBeFired ? "" : "not "), firedTrapRE));
    }
}

