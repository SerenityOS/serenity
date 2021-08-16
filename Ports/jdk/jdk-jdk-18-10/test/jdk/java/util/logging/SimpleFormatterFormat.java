/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6381464 8153666
 * @summary Test the custom simple formatter output
 *
 * @run main/othervm SimpleFormatterFormat
 */

import java.io.*;
import java.util.logging.*;
import java.util.regex.*;

public class SimpleFormatterFormat {
    private static final String key = "java.util.logging.SimpleFormatter.format";
    private static final String origFormat = System.getProperty(key);
    private static final PrintStream err = System.err;
    public static void main(String[] args) throws Exception {
        try {
            File dir = new File(System.getProperty("user.dir", "."));
            File log = new File(dir, "simpleformat.txt");
            java.nio.file.Files.deleteIfExists(log.toPath());
            PrintStream logps = new PrintStream(log);
            System.setProperty(key, "%3$s:%4$s: %5$s [%1$tc] source: %2$s%6$s%n");
            writeLogRecords(logps);
            checkLogRecords(log);
        } finally {
            if (origFormat == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, origFormat);
            }
            System.setErr(err);
       }
    }

    private static String[] loggers = new String[] {
        "test.foo",
        "test.foo",
        "test.bar",
        "test.bar",
        "test.bar",
        "test.bar"
    };
    private static String[] messages = new String[] {
        "severe hello world",
        "warning lost connection",
        "info welcome",
        "warning beware of traps",
        "warning { {ok7} }",
        // keep exception logging as last test case to avoid having
        // to skip the exception stack trace in the output
        "warning exception thrown"
    };
    private static void writeLogRecords(PrintStream logps) throws Exception {
        try {
            System.setErr(logps);

            Logger foo = Logger.getLogger("test.foo");
            foo.log(Level.SEVERE, "{0} {1} {2}", new Object[] {"severe", "hello", "world"});
            foo.warning(messages[1]);

            Logger bar = Logger.getLogger("test.bar");
            bar.finest("Dummy message");
            bar.info(messages[2]);
            bar.log(Level.WARNING, "{0}", new Object[] { messages[3] });
            bar.log(Level.WARNING, "warning '{' '{'{7}} }", new Object[] {"ok", "ok1", "ok2", "ok3", "ok4", "ok5", "ok6", "ok7", "ok8", "ok9", "ok10"});

            // Keep this one last - as it also prints the exception stack trace...
            bar.log(Level.WARNING, messages[messages.length-1], new IllegalArgumentException());
        } finally {
            logps.flush();
            logps.close();
            System.setErr(err);
        }
    }

    private static void checkLogRecords(File log) throws Exception {
        System.out.println("Checking log records in file: " + log);
        Pattern p = Pattern.compile("([\\.a-zA-Z:]+) (.*) \\[.*\\] source: (.*)");

        try (FileInputStream in = new FileInputStream(log);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                ) {
            String line;
            int i = 0;
            while (i < messages.length &&
                      (line = reader.readLine()) != null) {
                String expectedLogger = loggers[i];
                String expectedMsg = messages[i];
                i++;

                line = line.trim();
                System.out.println(line);

                Matcher m = p.matcher(line);
                if (!m.matches()) {
                    throw new RuntimeException("Unexpected output format: " + line);
                }
                if (m.groupCount() != 3) {
                    throw new RuntimeException("Unexpected group count = " +
                        m.groupCount());
                }
                // verify logger name and level
                String[] ss = m.group(1).split(":");
                int len = ss.length;
                if (len != 2) {
                    throw new RuntimeException("Unexpected logger name and level" +
                        m.group(1));
                }

                verify(expectedLogger, expectedMsg, ss[0], ss[1], m.group(2), m.group(3));
            }

            // expect IllegalArgumentException following it
            line = reader.readLine().trim();
            if (!line.equals("java.lang.IllegalArgumentException")) {
                throw new RuntimeException("Invalid line: " + line);
            }
        }
    }

    private static void verify(String expectedLogger, String expectedMsg,
                               String logger, String level,
                               String msg, String source) {
        if (!logger.equals(expectedLogger)) {
            throw new RuntimeException("Unexpected logger: " + logger);
        }
        if (!msg.equals(expectedMsg)) {
            throw new RuntimeException("Unexpected message: " + msg);
        }

        String[] ss = expectedMsg.split("\\s+");
        String expectedLevel = ss[0].toUpperCase();
        if (!level.equals(expectedLevel)) {
            throw new RuntimeException("Unexpected level: " + level);
        }

        ss = source.split("\\s+");
        int len = ss.length;
        if (!(len == 2 &&
              ss[0].equals("SimpleFormatterFormat") &&
              ss[1].equals("writeLogRecords"))) {
            throw new RuntimeException("Unexpected source: " + source);
        }
    }
}
