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
 * @bug 8177552
 * @modules jdk.localedata
 * @summary Checks the serialization feature of CompactNumberFormat
 * @run testng/othervm TestSerialization
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.RoundingMode;
import java.text.CompactNumberFormat;
import java.text.NumberFormat;
import java.util.Locale;
import static org.testng.Assert.*;

public class TestSerialization {

    private static final NumberFormat FORMAT_HI = NumberFormat.getCompactNumberInstance(
            new Locale("hi"), NumberFormat.Style.SHORT);
    private static final CompactNumberFormat FORMAT_EN_US = (CompactNumberFormat) NumberFormat
            .getCompactNumberInstance(Locale.US, NumberFormat.Style.LONG);
    private static final NumberFormat FORMAT_JA_JP = NumberFormat.getCompactNumberInstance(
            Locale.JAPAN, NumberFormat.Style.SHORT);
    private static final NumberFormat FORMAT_FR_FR = NumberFormat.getCompactNumberInstance(
            Locale.FRANCE, NumberFormat.Style.LONG);
    private static final NumberFormat FORMAT_DE_DE = NumberFormat.getCompactNumberInstance(
            Locale.GERMANY, NumberFormat.Style.SHORT);
    private static final NumberFormat FORMAT_KO_KR = NumberFormat.getCompactNumberInstance(
            Locale.KOREA, NumberFormat.Style.SHORT);

    @BeforeTest
    public void mutateInstances() {
        FORMAT_HI.setMinimumFractionDigits(2);
        FORMAT_HI.setMinimumIntegerDigits(5);

        FORMAT_EN_US.setRoundingMode(RoundingMode.HALF_UP);
        FORMAT_EN_US.setGroupingSize(3);
        FORMAT_EN_US.setParseBigDecimal(true);

        FORMAT_JA_JP.setMaximumFractionDigits(30);
        FORMAT_JA_JP.setMaximumIntegerDigits(30);

        FORMAT_FR_FR.setParseIntegerOnly(true);
        FORMAT_FR_FR.setGroupingUsed(true);

        // Setting minimum integer digits beyond the allowed range
        FORMAT_DE_DE.setMinimumIntegerDigits(320);

        // Setting minimum fraction digits beyond the allowed range
        FORMAT_KO_KR.setMinimumFractionDigits(350);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        // Serialize
        serialize("cdf.ser", FORMAT_HI, FORMAT_EN_US, FORMAT_JA_JP, FORMAT_FR_FR, FORMAT_DE_DE, FORMAT_KO_KR);
        // Deserialize
        deserialize("cdf.ser", FORMAT_HI, FORMAT_EN_US, FORMAT_JA_JP, FORMAT_FR_FR, FORMAT_DE_DE, FORMAT_KO_KR);
    }

    private void serialize(String fileName, NumberFormat... formats)
            throws IOException {
        try (ObjectOutputStream os = new ObjectOutputStream(
                new FileOutputStream(fileName))) {
            for (NumberFormat fmt : formats) {
                os.writeObject(fmt);
            }
        }
    }

    private static void deserialize(String fileName, NumberFormat... formats)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream os = new ObjectInputStream(
                new FileInputStream(fileName))) {
            for (NumberFormat fmt : formats) {
                NumberFormat obj = (NumberFormat) os.readObject();
                assertEquals(fmt, obj, "Serialized and deserialized"
                        + " objects do not match");

                long number = 123456789789L;
                String expected = fmt.format(number);
                String actual = obj.format(number);
                assertEquals(actual, expected, "Serialized and deserialized"
                        + " objects are expected to return same formatted"
                        + " output for number: " + number);
            }
        }
    }
}
