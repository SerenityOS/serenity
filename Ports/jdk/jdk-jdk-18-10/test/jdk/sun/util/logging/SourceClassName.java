/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6985460
 * @summary Test the source class name and method output by the platform
 *          logger.
 *
 * @modules java.base/sun.util.logging
 *          java.logging
 * @compile -XDignore.symbol.file SourceClassName.java
 * @run main/othervm SourceClassName
 */

import java.util.logging.*;
import java.io.*;
import sun.util.logging.PlatformLogger;

public class SourceClassName {
    public static void main(String[] args) throws Exception {
        File dir = new File(System.getProperty("user.dir", "."));
        File log = new File(dir, "testlog.txt");
        PrintStream logps = new PrintStream(log);
        writeLogRecords(logps);
        checkLogRecords(log);
    }

    private static void writeLogRecords(PrintStream logps) throws Exception {
        PrintStream err = System.err;
        try {
            System.setErr(logps);

            Object[] params = new Object[] { new Long(1), "string"};
            PlatformLogger plog = PlatformLogger.getLogger("test.log.foo");
            plog.severe("Log message {0} {1}", (Object[]) params);

            // create a java.util.logging.Logger
            // now java.util.logging.Logger should be created for each platform
            // logger
            Logger logger = Logger.getLogger("test.log.bar");
            logger.log(Level.SEVERE, "Log message {0} {1}", params);

            plog.severe("Log message {0} {1}", (Object[]) params);
        } finally {
            logps.flush();
            logps.close();
            System.setErr(err);
        }
    }

    private static void checkLogRecords(File log) throws Exception {
        System.out.println("Checking log records in file: " + log);
        FileInputStream in = new FileInputStream(log);
        String EXPECTED_LOG = "SEVERE: Log message 1 string";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            String[] record = new String[2];
            int count = 0;
            int i = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                System.out.println(line);
                record[i++] = line;
                if (i == 2) {
                    i = 0;
                    // check log message
                    if (!record[1].equals(EXPECTED_LOG)) {
                        // it can sometime happen that some static initializer
                        // in the system will log an error message - due to e.g.
                        // some kind of misconfiguration or system settings.
                        // For instance - somethink like:
                        //   INFO: currency.properties entry for FR ignored
                        //         because the value format is not recognized.
                        // instead of failing if we get such an unexpected
                        // message, we will simply print that out.
                        System.out.println("*** WARNING: Unexpected log: " + record[1]);
                        continue;
                    }
                    count++;
                    // check source class name and method
                    String[] ss = record[0].split("\\s+");
                    int len = ss.length;
                    if (!ss[len-2].equals("SourceClassName") ||
                        !ss[len-1].equals("writeLogRecords")) {
                        throw new RuntimeException("Unexpected source: " +
                            ss[len-2] + " " + ss[len-1]);
                    }

                }
            }
            if (count != 3) {
                throw new RuntimeException("Unexpected number of records: " + count);
            }
        } finally {
            in.close();
        }
    }
}
