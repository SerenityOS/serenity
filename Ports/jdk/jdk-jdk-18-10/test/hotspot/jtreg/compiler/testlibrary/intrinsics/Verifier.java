/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

package compiler.testlibrary.intrinsics;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Verifier {
    enum VerificationStrategy {
        VERIFY_STRONG_EQUALITY {
            @Override
            void verify(Properties expectedProperties, int fullMatchCnt,
                        int suspectCnt) {
                int expectedCount = Integer.parseInt(
                        expectedProperties.getProperty(
                                Verifier.INTRINSIC_EXPECTED_COUNT_PROPERTY));
                String intrinsicID = expectedProperties.getProperty(
                        Verifier.INTRINSIC_NAME_PROPERTY);

                System.out.println("Intrinsic " + intrinsicID
                        + " verification, expected: " + expectedCount
                        + ", matched: " + fullMatchCnt
                        + ", suspected: " + suspectCnt);
                if (expectedCount != fullMatchCnt) {
                    throw new RuntimeException(
                            "Unexpected count of intrinsic  "
                                    + intrinsicID
                                    + " expected:" + expectedCount
                                    + ", matched: " + fullMatchCnt
                                    + ", suspected: " + suspectCnt);
                }
            }
        },

        VERIFY_INTRINSIC_USAGE {
            @Override
            void verify(Properties expectedProperties, int fullMatchCnt,
                        int suspectCnt) {
                boolean isExpected = Boolean.parseBoolean(
                        expectedProperties.getProperty(
                                Verifier.INTRINSIC_IS_EXPECTED_PROPERTY));
                String intrinsicID = expectedProperties.getProperty(
                        Verifier.INTRINSIC_NAME_PROPERTY);

                System.out.println("Intrinsic " + intrinsicID
                        + " verification, is expected: " + isExpected
                        + ", matched: " + fullMatchCnt
                        + ", suspected: " + suspectCnt);
                if ((fullMatchCnt == 0 && isExpected)
                        || (fullMatchCnt > 0 && !isExpected)) {
                    throw new RuntimeException(
                            "Unexpected count of intrinsic  "
                                    + intrinsicID
                                    + " is expected:" + isExpected
                                    + ", matched: " + fullMatchCnt
                                    + ", suspected: " + suspectCnt);
                }
            }
        };

        void verify(Properties expectedProperties, int fullMathCnt,
                    int suspectCnt) {
            throw new RuntimeException("Default strategy is not implemented.");
        }
    }

    public static final String PROPERTY_FILE_SUFFIX = ".verify.properties";
    public static final String INTRINSIC_NAME_PROPERTY = "intrinsic.name";
    public static final String INTRINSIC_IS_EXPECTED_PROPERTY
            = "intrinsic.expected";
    public static final String INTRINSIC_EXPECTED_COUNT_PROPERTY
            = "intrinsic.expectedCount";
    private static final String DEFAULT_STRATEGY
            = VerificationStrategy.VERIFY_STRONG_EQUALITY.name();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new RuntimeException("Test bug, nothing to verify");
        }
        for (String hsLogFile : args) {
            verify(hsLogFile);
        }
    }

    private static void verify(String hsLogFile) throws Exception {
        System.out.println("Verifying " + hsLogFile);

        Properties expectedProperties = new Properties();
        FileReader reader = new FileReader(hsLogFile
                + Verifier.PROPERTY_FILE_SUFFIX);
        expectedProperties.load(reader);
        reader.close();

        int fullMatchCnt = 0;
        int suspectCnt = 0;
        String intrinsicId = expectedProperties.getProperty(
                Verifier.INTRINSIC_NAME_PROPERTY);
        String prefix = "<intrinsic id='";
        String prefixWithId = prefix + intrinsicId + "'";

        int repeatCnt = 1;
        Pattern repeatComp = Pattern.compile("-XX:RepeatCompilation=(\\d+)*");
        try (BufferedReader compLogReader
                     = new BufferedReader(new FileReader(hsLogFile))) {
            String logLine;
            while ((logLine = compLogReader.readLine()) != null) {
                // Check if compilations are repeated. If so, normalize the
                // match and suspect counts.
                Matcher m = repeatComp.matcher(logLine);
                if (m.find()) {
                    repeatCnt = Integer.parseInt(m.group(1)) + 1;
                }
                if (logLine.startsWith(prefix)) {
                    if (logLine.startsWith(prefixWithId)) {
                        fullMatchCnt++;
                    } else {
                        suspectCnt++;
                        System.err.println(
                                "WARNING: Other intrinsic detected " + logLine);
                    }
                }
            }
        }
        fullMatchCnt /= repeatCnt;
        suspectCnt   /= repeatCnt;

        VerificationStrategy strategy = VerificationStrategy.valueOf(
                System.getProperty("verificationStrategy",
                        Verifier.DEFAULT_STRATEGY));
        strategy.verify(expectedProperties, fullMatchCnt, suspectCnt);
    }
}
