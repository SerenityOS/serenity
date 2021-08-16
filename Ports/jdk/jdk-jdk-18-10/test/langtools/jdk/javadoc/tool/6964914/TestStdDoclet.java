/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6964914
 * @summary javadoc does not output number of warnings using user written doclet
 */

import java.io.*;
import java.util.*;

/**
 * Dummy javadoc comment.
 * @author jjg
 * @see DoesNotExist
 */
public class TestStdDoclet {
    public static void main(String... args) throws Exception {
        new TestStdDoclet().run();
    }

    /**
     * More dummy comments.
     * @throws DoesNotExist   oops, javadoc does not see this
     * @see DoesNotExist
     */
    void run() throws Exception {
        File javaHome = new File(System.getProperty("java.home"));
        File javadoc = new File(new File(javaHome, "bin"), "javadoc");
        File testSrc = new File(System.getProperty("test.src"));

        // run javadoc in separate process to ensure doclet executed under
        // normal user conditions w.r.t. classloader
        String thisClassName = TestStdDoclet.class.getName();
        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add(javadoc.getPath());
        cmdArgs.addAll(Arrays.asList(
                "-classpath", ".", // insulates us from ambient classpath
                "-Xdoclint:none",
                "-package",
                new File(testSrc, thisClassName + ".java").getPath()
        ));
        Process p = new ProcessBuilder()
            .command(cmdArgs)
            .redirectErrorStream(true)
            .start();

        int actualDocletWarnCount = 0;
        int reportedDocletWarnCount = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.err.println(line);
                if (line.contains("TestStdDoclet.java") && line.contains("DoesNotExist")) {
                    actualDocletWarnCount++;
                }
                if (line.matches("[0-9]+ warning(s)?")) {
                    reportedDocletWarnCount =
                            Integer.valueOf(line.substring(0, line.indexOf(" ")));
                }
            }
        } finally {
            in.close();
        }
        int rc = p.waitFor();
        if (rc != 0)
            System.err.println("javadoc failed, rc:" + rc);

        int expectedDocletWarnCount = 2;
        checkEqual("actual", actualDocletWarnCount, "expected", expectedDocletWarnCount);
        checkEqual("actual", actualDocletWarnCount, "reported", reportedDocletWarnCount);
    }

    /**
     * Private method should not cause a warning.
     * @see DoesNotExist
     */
    private void checkEqual(String l1, int i1, String l2, int i2) throws Exception {
        if (i1 != i2)
            throw new Exception(l1 + " warn count, " + i1 + ", does not match "
                        + l2 + " warn count, " + i2);
    }

}
