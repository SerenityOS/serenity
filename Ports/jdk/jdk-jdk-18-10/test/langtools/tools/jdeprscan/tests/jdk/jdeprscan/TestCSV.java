/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Basic tests CSV printing and parsing
 * @modules jdk.jdeps/com.sun.tools.jdeprscan
 * @build TestCSV
 * @run testng jdk.jdeprscan.TestCSV
 */

package jdk.jdeprscan;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import com.sun.tools.jdeprscan.CSV;
import com.sun.tools.jdeprscan.CSVParseException;

@Test
public class TestCSV {
    static String NL = System.lineSeparator();
    ByteArrayOutputStream baos;
    PrintStream out;

    @BeforeMethod
    public void setup() throws UnsupportedEncodingException {
        baos = new ByteArrayOutputStream();
        out = new PrintStream(baos, true, "UTF-8");
    }

    String result() {
        out.flush();
        return new String(baos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Asserts string equality after checking for and removing a trailing line separator.
     *
     * @param expected expected result
     */
    void checkString(String expected) {
        String actual = result();
        assertTrue(actual.endsWith(NL));
        assertEquals(actual.substring(0, actual.length() - NL.length()), expected);
    }

    @DataProvider(name = "csvdata")
    public Object[][] getCSVData() {
        return new Object[][] {
            { "",               List.of("") },
            { "a",              List.of("a") },
            { "a,b",            List.of("a", "b") },
            { "a,b,c",          List.of("a", "b", "c") },
            { ",a,b",           List.of("", "a", "b") },
            { "a,b,",           List.of("a", "b", "") },
            { ",a,b,",          List.of("", "a", "b", "") },
            { ",a,,b,",         List.of("", "a", "", "b", "") },
            { ",",              List.of("", "") },
            { ",,",             List.of("", "", "") },
            { ",,,",            List.of("", "", "", "") },
            { " a , b ",        List.of(" a ", " b ") },
            { "a,\",\",b",      List.of("a", ",", "b") },
            { "a,\"b\"\"c\",d", List.of("a", "b\"c", "d") },
            { "a,\"b,c\",d",    List.of("a", "b,c", "d") },

            // from https://en.wikipedia.org/wiki/Comma-separated_values
            // slightly modified to enable round-tripping

            { "Year,Make,Model,Description,Price",
                List.of("Year", "Make", "Model", "Description", "Price") },
            { "1997,Ford,E350,\"ac, abs, moon\",3000.00",
                List.of("1997", "Ford", "E350", "ac, abs, moon", "3000.00") },
            { "1999,Chevy,\"Venture \"\"Extended Edition\"\"\",,4900.00",
                List.of("1999", "Chevy", "Venture \"Extended Edition\"", "", "4900.00") },
            { "1999,Chevy,\"Venture \"\"Extended Edition, Very Large\"\"\",,5000.00",
                List.of("1999", "Chevy", "Venture \"Extended Edition, Very Large\"", "", "5000.00") },
            { "1996,Jeep,Grand Cherokee,\"MUST SELL!\nair, moon roof, loaded\",4799.00",
                List.of("1996", "Jeep", "Grand Cherokee", "MUST SELL!\nair, moon roof, loaded", "4799.00") }
        };
    }

    @Test(dataProvider = "csvdata")
    public void roundTrip(String input, List<String> parsed) {
        List<String> actual = CSV.split(input);
        assertEquals(actual, parsed);
        CSV.write(out, actual.toArray());
        checkString(input);
    }

    // won't round-trip
    public void testExtraQuote() {
        assertEquals(CSV.split("a,\"b\",c"), List.of("a", "b", "c"));
    }

    // won't round-trip
    public void testEmptyQuote() {
        assertEquals(CSV.split("a,\"\",b"), List.of("a", "", "b"));
    }

    @Test(expectedExceptions=CSVParseException.class)
    public void errorUnexpectedQuote() {
        CSV.split("ab\"cd");
    }

    @Test(expectedExceptions=CSVParseException.class)
    public void errorCharacterAfterQuote() {
        CSV.split("a,\"b\"c,d");
    }

    @Test(expectedExceptions=CSVParseException.class)
    public void errorUnclosedQuote() {
        CSV.split("a,\"b");
    }
}
