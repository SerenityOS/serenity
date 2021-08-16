/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import jdk.internal.net.http.hpack.SimpleHeaderTable.HeaderField;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jdk.internal.net.http.hpack.TestHelper.assertExceptionMessageContains;
import static jdk.internal.net.http.hpack.TestHelper.assertThrows;
import static jdk.internal.net.http.hpack.TestHelper.assertVoidThrows;
import static jdk.internal.net.http.hpack.TestHelper.newRandom;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;

public class SimpleHeaderTableTest {

    //
    // https://tools.ietf.org/html/rfc7541#appendix-A
    //
    // @formatter:off
    private static final String SPECIFICATION =
       "          | 1     | :authority                  |               |\n" +
       "          | 2     | :method                     | GET           |\n" +
       "          | 3     | :method                     | POST          |\n" +
       "          | 4     | :path                       | /             |\n" +
       "          | 5     | :path                       | /index.html   |\n" +
       "          | 6     | :scheme                     | http          |\n" +
       "          | 7     | :scheme                     | https         |\n" +
       "          | 8     | :status                     | 200           |\n" +
       "          | 9     | :status                     | 204           |\n" +
       "          | 10    | :status                     | 206           |\n" +
       "          | 11    | :status                     | 304           |\n" +
       "          | 12    | :status                     | 400           |\n" +
       "          | 13    | :status                     | 404           |\n" +
       "          | 14    | :status                     | 500           |\n" +
       "          | 15    | accept-charset              |               |\n" +
       "          | 16    | accept-encoding             | gzip, deflate |\n" +
       "          | 17    | accept-language             |               |\n" +
       "          | 18    | accept-ranges               |               |\n" +
       "          | 19    | accept                      |               |\n" +
       "          | 20    | access-control-allow-origin |               |\n" +
       "          | 21    | age                         |               |\n" +
       "          | 22    | allow                       |               |\n" +
       "          | 23    | authorization               |               |\n" +
       "          | 24    | cache-control               |               |\n" +
       "          | 25    | content-disposition         |               |\n" +
       "          | 26    | content-encoding            |               |\n" +
       "          | 27    | content-language            |               |\n" +
       "          | 28    | content-length              |               |\n" +
       "          | 29    | content-location            |               |\n" +
       "          | 30    | content-range               |               |\n" +
       "          | 31    | content-type                |               |\n" +
       "          | 32    | cookie                      |               |\n" +
       "          | 33    | date                        |               |\n" +
       "          | 34    | etag                        |               |\n" +
       "          | 35    | expect                      |               |\n" +
       "          | 36    | expires                     |               |\n" +
       "          | 37    | from                        |               |\n" +
       "          | 38    | host                        |               |\n" +
       "          | 39    | if-match                    |               |\n" +
       "          | 40    | if-modified-since           |               |\n" +
       "          | 41    | if-none-match               |               |\n" +
       "          | 42    | if-range                    |               |\n" +
       "          | 43    | if-unmodified-since         |               |\n" +
       "          | 44    | last-modified               |               |\n" +
       "          | 45    | link                        |               |\n" +
       "          | 46    | location                    |               |\n" +
       "          | 47    | max-forwards                |               |\n" +
       "          | 48    | proxy-authenticate          |               |\n" +
       "          | 49    | proxy-authorization         |               |\n" +
       "          | 50    | range                       |               |\n" +
       "          | 51    | referer                     |               |\n" +
       "          | 52    | refresh                     |               |\n" +
       "          | 53    | retry-after                 |               |\n" +
       "          | 54    | server                      |               |\n" +
       "          | 55    | set-cookie                  |               |\n" +
       "          | 56    | strict-transport-security   |               |\n" +
       "          | 57    | transfer-encoding           |               |\n" +
       "          | 58    | user-agent                  |               |\n" +
       "          | 59    | vary                        |               |\n" +
       "          | 60    | via                         |               |\n" +
       "          | 61    | www-authenticate            |               |\n";
    // @formatter:on

    static final int STATIC_TABLE_LENGTH = createStaticEntries().size();
    final Random rnd = newRandom();

    /** Creates a header table under test. Override in subclass. */
    protected SimpleHeaderTable createHeaderTable(int maxSize) {
        return new SimpleHeaderTable(maxSize, HPACK.getLogger());
    }

    @Test
    public void staticData0() {
        SimpleHeaderTable table = createHeaderTable(0);
        Map<Integer, HeaderField> staticHeaderFields = createStaticEntries();
        staticHeaderFields.forEach((index, expectedHeaderField) -> {
            SimpleHeaderTable.HeaderField actualHeaderField = table.get(index);
            assertEquals(actualHeaderField.name, expectedHeaderField.name);
            assertEquals(actualHeaderField.value, expectedHeaderField.value);
        });
    }

    @Test
    public void constructorSetsMaxSize() {
        int size = rnd.nextInt(64);
        SimpleHeaderTable table = createHeaderTable(size);
        assertEquals(table.size(), 0);
        assertEquals(table.maxSize(), size);
    }

    @Test
    public void negativeMaximumSize() {
        int maxSize = -(rnd.nextInt(100) + 1); // [-100, -1]
        SimpleHeaderTable table = createHeaderTable(0);
        IllegalArgumentException e =
                assertVoidThrows(IllegalArgumentException.class,
                                 () -> table.setMaxSize(maxSize));
        assertExceptionMessageContains(e, "maxSize");
    }

    @Test
    public void zeroMaximumSize() {
        SimpleHeaderTable table = createHeaderTable(0);
        table.setMaxSize(0);
        assertEquals(table.maxSize(), 0);
    }

    @Test
    public void negativeIndex() {
        int idx = -(rnd.nextInt(256) + 1); // [-256, -1]
        SimpleHeaderTable table = createHeaderTable(0);
        IndexOutOfBoundsException e =
                assertVoidThrows(IndexOutOfBoundsException.class,
                                 () -> table.get(idx));
        assertExceptionMessageContains(e, "index");
    }

    @Test
    public void zeroIndex() {
        SimpleHeaderTable table = createHeaderTable(0);
        IndexOutOfBoundsException e =
                assertThrows(IndexOutOfBoundsException.class,
                             () -> table.get(0));
        assertExceptionMessageContains(e, "index");
    }

    @Test
    public void length() {
        SimpleHeaderTable table = createHeaderTable(0);
        assertEquals(table.length(), STATIC_TABLE_LENGTH);
    }

    @Test
    public void indexOutsideStaticRange() {
        SimpleHeaderTable table = createHeaderTable(0);
        int idx = table.length() + (rnd.nextInt(256) + 1);
        IndexOutOfBoundsException e =
                assertThrows(IndexOutOfBoundsException.class,
                             () -> table.get(idx));
        assertExceptionMessageContains(e, "index");
    }

    @Test
    public void entryPutAfterStaticArea() {
        SimpleHeaderTable table = createHeaderTable(256);
        int idx = table.length() + 1;
        assertThrows(IndexOutOfBoundsException.class, () -> table.get(idx));

        byte[] bytes = new byte[32];
        rnd.nextBytes(bytes);
        String name = new String(bytes, StandardCharsets.ISO_8859_1);
        String value = "custom-value";

        table.put(name, value);
        SimpleHeaderTable.HeaderField f = table.get(idx);
        assertEquals(f.name, name);
        assertEquals(f.value, value);
    }

    @Test
    public void staticTableHasZeroSize() {
        SimpleHeaderTable table = createHeaderTable(0);
        assertEquals(table.size(), 0);
    }

    // TODO: negative indexes check
    // TODO: ensure full table clearance when adding huge header field
    // TODO: ensure eviction deletes minimum needed entries, not more

    @Test
    public void fifo() {
        // Let's add a series of header fields
        int NUM_HEADERS = 32;
        SimpleHeaderTable table =
                createHeaderTable((32 + 4) * NUM_HEADERS);
        //                          ^   ^
        //             entry overhead   symbols per entry (max 2x2 digits)
        for (int i = 1; i <= NUM_HEADERS; i++) {
            String s = String.valueOf(i);
            table.put(s, s);
        }
        // They MUST appear in a FIFO order:
        //   newer entries are at lower indexes
        //   older entries are at higher indexes
        for (int j = 1; j <= NUM_HEADERS; j++) {
            SimpleHeaderTable.HeaderField f = table.get(STATIC_TABLE_LENGTH + j);
            int actualName = Integer.parseInt(f.name);
            int expectedName = NUM_HEADERS - j + 1;
            assertEquals(actualName, expectedName);
        }
        // Entries MUST be evicted in the order they were added:
        //   the newer the entry the later it is evicted
        for (int k = 1; k <= NUM_HEADERS; k++) {
            SimpleHeaderTable.HeaderField f = table.evictEntry();
            assertEquals(f.name, String.valueOf(k));
        }
    }

    @Test
    public void testToString() {
        testToString0();
    }

    @Test
    public void testToStringDifferentLocale() {
        Locale locale = Locale.getDefault();
        Locale.setDefault(Locale.FRENCH);
        try {
            String s = format("%.1f", 3.1);
            assertEquals(s, "3,1"); // assumption of the test, otherwise the test is useless
            testToString0();
        } finally {
            Locale.setDefault(locale);
        }
    }

    private void testToString0() {
        SimpleHeaderTable table = createHeaderTable(0);
        {
            int maxSize = 2048;
            table.setMaxSize(maxSize);
            String expected = format(
                    "dynamic length: %s, full length: %s, used space: %s/%s (%.1f%%)",
                    0, STATIC_TABLE_LENGTH, 0, maxSize, 0.0);
            assertEquals(table.toString(), expected);
        }

        {
            String name = "custom-name";
            String value = "custom-value";
            int size = 512;

            table.setMaxSize(size);
            table.put(name, value);
            String s = table.toString();

            int used = name.length() + value.length() + 32;
            double ratio = used * 100.0 / size;

            String expected = format(
                    "dynamic length: %s, full length: %s, used space: %s/%s (%.1f%%)",
                    1, STATIC_TABLE_LENGTH + 1, used, size, ratio);
            assertEquals(s, expected);
        }

        {
            table.setMaxSize(78);
            table.put(":method", "");
            table.put(":status", "");
            String s = table.toString();
            String expected =
                    format("dynamic length: %s, full length: %s, used space: %s/%s (%.1f%%)",
                           2, STATIC_TABLE_LENGTH + 2, 78, 78, 100.0);
            assertEquals(s, expected);
        }
    }

    @Test
    public void stateString() {
        SimpleHeaderTable table = createHeaderTable(256);
        table.put("custom-key", "custom-header");
        // @formatter:off
        assertEquals(table.getStateString(),
                     "[  1] (s =  55) custom-key: custom-header\n" +
                     "      Table size:  55");
        // @formatter:on
    }

    static Map<Integer, HeaderField> createStaticEntries() {
        Pattern line = Pattern.compile(
                "\\|\\s*(?<index>\\d+?)\\s*\\|\\s*(?<name>.+?)\\s*\\|\\s*(?<value>.*?)\\s*\\|");
        Matcher m = line.matcher(SPECIFICATION);
        Map<Integer, HeaderField> result = new HashMap<>();
        while (m.find()) {
            int index = Integer.parseInt(m.group("index"));
            String name = m.group("name");
            String value = m.group("value");
            HeaderField f = new HeaderField(name, value);
            result.put(index, f);
        }
        return Collections.unmodifiableMap(result); // lol
    }
}
