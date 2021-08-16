
/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8193660
 * @summary Check SOURCE line in "release" file for closedjdk
 * @run main CheckSource
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CheckSource {

    public static final String SRC_HASH_REGEXP = ":((hg)|(git)):[a-z0-9]*\\+?";

    CheckSource(String dataFile, boolean isOpenJDK) {
        // Read data files
        readFile(dataFile, isOpenJDK);
    }

    private void readFile(String fileName, boolean isOpenJDK) {
        String fishForSOURCE = null;
        String implementor = null;

        File file = new File(fileName);

        // open the stream to read in for Entries
        try (BufferedReader buffRead =
            new BufferedReader(new FileReader(fileName))) {

            // this is the string read
            String readIn;

            // let's read some strings!
            while ((readIn = buffRead.readLine()) != null) {
                readIn = readIn.trim();

                // throw out blank lines
                if (readIn.length() == 0)
                    continue;

                // grab SOURCE line
                if (readIn.startsWith("SOURCE=")) {
                    fishForSOURCE = readIn;
                    continue;
                }

                // grab IMPLEMENTOR line
                if (readIn.startsWith("IMPLEMENTOR=")) {
                    implementor = readIn;
                    continue;
                }
            }
        } catch (FileNotFoundException fileExcept) {
            throw new RuntimeException("File " + fileName +
                                       " not found reading data!", fileExcept);
        } catch (IOException ioExcept) {
            throw new RuntimeException("Unexpected problem reading data!",
                                       ioExcept);
        }

        // was SOURCE even found?
        if (fishForSOURCE == null) {
            throw new RuntimeException("SOURCE line was not found!");
        }
        System.out.println("The source string found: " + fishForSOURCE);

        // Extract the value of SOURCE=
        Pattern valuePattern = Pattern.compile("SOURCE=\"(.*)\"");
        Matcher valueMatcher = valuePattern.matcher(fishForSOURCE);
        if (!valueMatcher.matches()) {
            throw new RuntimeException("SOURCE string has bad format, should be SOURCE=\"<value>\"");
        }
        String valueString = valueMatcher.group(1);

        // Check if implementor is Oracle
        boolean isOracle = (implementor != null) && implementor.contains("Oracle Corporation");

        String[] values = valueString.split(" ");

        // First value MUST start with ".:" regardless of Oracle or OpenJDK
        String rootRegexp = "\\." + SRC_HASH_REGEXP;
        if (!values[0].matches(rootRegexp)) {
            throw new RuntimeException("The test failed, first element did not match regexp: " + rootRegexp);
        }

        // If it's an Oracle build, it can be either OpenJDK or OracleJDK. Other
        // builds may have any number of additional elements in any format.
        if (isOracle) {
            if (isOpenJDK) {
                if (values.length != 1) {
                    throw new RuntimeException("The test failed, wrong number of elements in SOURCE list." +
                            " Should be 1 for Oracle built OpenJDK.");
                }
            } else {
                if (values.length != 2) {
                    throw new RuntimeException("The test failed, wrong number of elements in SOURCE list." +
                            " Should be 2 for OracleJDK.");
                }
                // Second value MUST start with "open:" for OracleJDK
                String openRegexp = "open" + SRC_HASH_REGEXP;
                if (!values[1].matches(openRegexp)) {
                    throw new RuntimeException("The test failed, second element did not match regexp: " + openRegexp);
                }
            }
        }

        // Everything was fine
        System.out.println("The test passed!");
    }

    public static void main(String args[]) {
        String jdkPath = System.getProperty("test.jdk");
        String runtime = System.getProperty("java.runtime.name");

        System.out.println("JDK Path : " + jdkPath);
        System.out.println("Runtime Name : " + runtime);

        new CheckSource(jdkPath + "/release", runtime.contains("OpenJDK"));
    }
}
