/*
 * Copyright (c) 2000, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4112090 8008577
 * @summary verify that MessageFormat can handle large numbers of arguments
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI LargeMessageFormat
 */

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class LargeMessageFormat {

    public static void main(String[] args) throws ParseException {
        Locale reservedLocale = Locale.getDefault();
        TimeZone reservedTimeZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"));
            testFormat();
            testParse();
        } finally {
            // restore the reserved locale and time zone
            Locale.setDefault(reservedLocale);
            TimeZone.setDefault(reservedTimeZone);
        }
    }

    private static final int REPEATS = 89;

    private static void testFormat() {
        // construct large argument array
        @SuppressWarnings("deprecation")
        Object[] sample = {
                 0, // replace with running count below
                "hello",
                new Date(89, 10, 9),
                567890,
                1234.50
        };
        int samples = sample.length;
        Object[] arguments = new Object[REPEATS * (samples + 1)];
        for (int i = 0; i < REPEATS; i++) {
            System.arraycopy(sample, 0, arguments, i * samples, samples);
            arguments[i * samples] = i;
        }

        // construct large template
        StringBuffer template = new StringBuffer();
        for (int i = 0; i < REPEATS; i++) {
            template.append("section {" + (i * samples) + ", number} - ");
            template.append("string: {" + (i * samples + 1) + "}; ");
            template.append("date: {" + (i * samples + 2) + ", date}; ");
            template.append("integer: {" + (i * samples + 3) + ", number}; ");
            template.append("currency: {" + (i * samples + 4) + ", number, currency};\n");
        }

        // construct expected result string
        StringBuffer expected = new StringBuffer();
        for (int i = 0; i < REPEATS; i++) {
            expected.append("section " + i + " - ");
            expected.append("string: hello; ");
            expected.append("date: 09.11.1989; ");
            expected.append("integer: 567.890; ");
            expected.append("currency: 1.234,50 \u20AC;\n");
        }

        // create message format
        MessageFormat format = new MessageFormat(template.toString());
        String result = format.format(arguments);
        if (!result.equals(expected.toString())) {
           System.out.println("Template:");
           System.out.println(template);
           System.out.println("Expected result: ");
           System.out.println(expected);
           System.out.println("Actual result: ");
           System.out.println(result);
           throw new RuntimeException();
       }
    }

    private static void testParse() throws ParseException {
        StringBuffer parseTemplate = new StringBuffer();
        StringBuffer parseInput = new StringBuffer();
        for (int i = 0; i < REPEATS; i++) {
            parseTemplate.append("{" + i + ", number} ");
            parseInput.append(i + " ");
        }
        MessageFormat parseFormat = new MessageFormat(parseTemplate.toString());
        Object[] parseResult = parseFormat.parse(parseInput.toString());
        for (int i = 0; i < REPEATS; i++) {
            if (((Number) parseResult[i]).intValue() != i) {
                throw new RuntimeException("got wrong parse result");
            }
        }
    }
}
