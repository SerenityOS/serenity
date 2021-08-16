/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/* @test
 * @bug 8235238
 * @summary Checks whether custom zone names can be formatted/parsed correctly.
 * @library zoneProvider
 * @build custom.CustomZoneRulesProvider custom.CustomTimeZoneNameProvider
 * @run main/othervm -Djava.locale.providers=SPI,CLDR CustomZoneNameTest
 */
public class CustomZoneNameTest {

    private final static long now = 1575669972372L;
    private final static Instant instant = Instant.ofEpochMilli(now);
    private final static ZoneId customZone = ZoneId.of("Custom/Timezone");

    // test data
    private final static Map<String, String>  formats = Map.of(
        "yyyy-MM-dd HH:mm:ss.SSS VV", "2019-12-06 22:06:12.372 Custom/Timezone",
        "yyyy-MM-dd HH:mm:ss.SSS z", "2019-12-06 22:06:12.372 CUST_WT",
        "yyyy-MM-dd HH:mm:ss.SSS zzzz", "2019-12-06 22:06:12.372 Custom Winter Time",
        "yyyy-MM-dd HH:mm:ss.SSS v", "2019-12-06 22:06:12.372 Custom Time",
        "yyyy-MM-dd HH:mm:ss.SSS vvvv", "2019-12-06 22:06:12.372 Custom Timezone Time"
    );

    public static void main(String... args) {
        testFormatting();
        testParsing();
    }

    private static void testFormatting() {
        var customZDT = ZonedDateTime.ofInstant(instant, customZone);
        formats.entrySet().stream()
            .filter(e -> {
                var formatted = DateTimeFormatter.ofPattern(e.getKey()).format(customZDT);
                var expected = e.getValue();
                System.out.println("testFormatting. Pattern: " + e.getKey() +
                        ", expected: " + expected +
                        ", formatted: " + formatted);
                return !formatted.equals(expected);
            })
            .findAny()
            .ifPresent(e -> {
                throw new RuntimeException(
                        "Provider's custom name was not retrieved for the format " +
                        e.getKey());
            });
    }

    public static void testParsing() {
        formats.entrySet().stream()
            .filter(e -> {
                var fmt = DateTimeFormatter.ofPattern(e.getKey());
                var input = e.getValue();
                var parsedInstant = fmt.parse(input, Instant::from).toEpochMilli();
                var parsedZone = fmt.parse(input, ZonedDateTime::from).getZone();
                System.out.println("testParsing. Input: " + input +
                        ", expected instant: " + now +
                        ", expected zone: " + customZone +
                        ", parsed instant: " + parsedInstant +
                        ", parsed zone: " + parsedZone);
                return parsedInstant != now ||
                        !parsedZone.equals(customZone);
            })
            .findAny()
            .ifPresent(e -> {
                throw new RuntimeException("Parsing failed for the format " +
                                e.getKey());
            });
    }
}
