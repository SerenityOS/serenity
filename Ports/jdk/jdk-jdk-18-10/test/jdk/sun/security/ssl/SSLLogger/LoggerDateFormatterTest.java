/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8245306
 * @summary Replace ThreadLocal date format with DateTimeFormatter
 * @modules java.base/sun.security.ssl:+open
 * @compile LoggerDateFormatterTest.java
 * @run testng/othervm -Djavax.net.debug=all LoggerDateFormatterTest
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import sun.security.ssl.SSLLogger;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.out;
import static org.testng.Assert.fail;

public class LoggerDateFormatterTest {

    SSLPrintStream sslStream;
    static String year = "(\\|\\d\\d\\d\\d-\\d\\d-\\d\\d";
    static String hour = "\\s\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d\\s";
    static String zone = "([A-Za-z]+([\\+\\-][0-2]?[0-9](\\:[0-5]?[0-9]))?))";
    static Pattern pattern;
    Matcher matcher;

    @BeforeTest
    public void setUp() {
        sslStream = new SSLPrintStream(System.err);
        System.setErr(sslStream);
        String format = year + hour + zone;
        pattern = Pattern.compile(format);
    }

    @Test
    public void testDateFormat() {
        SSLLogger.info("logging");
        System.out.println("The value is: " + sslStream.bos.toString());
        matcher = pattern.matcher(sslStream.bos.toString());
        if (matcher.find()) {
            out.println("Test Passed with value :" + matcher.group());
        }
        else {
            fail("Test failed wrong SSL DateFormat");
        }
    }

    public static class SSLPrintStream extends PrintStream {

        public ByteArrayOutputStream bos; // Stream that accumulates System.err

        public SSLPrintStream(OutputStream out) {
            super(out);
            bos = new ByteArrayOutputStream();
        }

        @Override
        public void write(int b) {
            super.write(b);
            bos.write(b);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            super.write(buf, off, len);
            bos.write(buf, off, len);
        }

        @Override
        public void write(byte[] buf) throws IOException {
            super.write(buf);
            bos.write(buf);
        }

        @Override
        public void writeBytes(byte[] buf) {
            super.writeBytes(buf);
            bos.writeBytes(buf);
        }
    }

}
