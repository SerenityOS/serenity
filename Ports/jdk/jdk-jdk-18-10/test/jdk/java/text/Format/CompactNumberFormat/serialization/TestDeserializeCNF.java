/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @summary Checks deserialization of compact number format
 * @library /java/text/testlib
 * @build TestDeserializeCNF HexDumpReader
 * @run testng/othervm TestDeserializeCNF
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.RoundingMode;
import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import static org.testng.Assert.*;

public class TestDeserializeCNF {

    // This object is serialized in cnf1.ser.txt with HALF_UP
    // rounding mode, groupingsize = 3 and parseBigDecimal = true
    private static final CompactNumberFormat COMPACT_FORMAT1 = new CompactNumberFormat("#,##0.###",
            DecimalFormatSymbols.getInstance(Locale.US),
            new String[]{"", "", "", "0K", "00K", "000K", "0M", "00M", "000M", "0B", "00B", "000B", "0T", "00T", "000T"});

    // This object is serialized in cnf2.ser.txt with min integer digits = 20
    // and min fraction digits = 5
    private static final CompactNumberFormat COMPACT_FORMAT2 = new CompactNumberFormat("#,##0.###",
            DecimalFormatSymbols.getInstance(Locale.JAPAN),
            new String[]{"", "", "", "0", "0\u4e07", "00\u4e07", "000\u4e07", "0000\u4e07", "0\u5104", "00\u5104", "000\u5104", "0000\u5104", "0\u5146", "00\u5146", "000\u5146"});

    private static final String FILE_COMPACT_FORMAT1 = "cnf1.ser.txt";
    private static final String FILE_COMPACT_FORMAT2 = "cnf2.ser.txt";

    @BeforeTest
    public void mutateInstances() {
        COMPACT_FORMAT1.setRoundingMode(RoundingMode.HALF_UP);
        COMPACT_FORMAT1.setGroupingSize(3);
        COMPACT_FORMAT1.setParseBigDecimal(true);

        COMPACT_FORMAT2.setMinimumIntegerDigits(20);
        COMPACT_FORMAT2.setMinimumFractionDigits(5);
    }

    @Test
    public void testDeserialization() throws IOException, ClassNotFoundException {
        try (InputStream istream1 = HexDumpReader.getStreamFromHexDump(FILE_COMPACT_FORMAT1);
                ObjectInputStream ois1 = new ObjectInputStream(istream1);
                InputStream istream2 = HexDumpReader.getStreamFromHexDump(FILE_COMPACT_FORMAT2);
                ObjectInputStream ois2 = new ObjectInputStream(istream2);) {

            CompactNumberFormat obj1 = (CompactNumberFormat) ois1.readObject();
            assertEquals(obj1, COMPACT_FORMAT1, "Deserialized instance is not"
                    + " equal to the instance serialized in " + FILE_COMPACT_FORMAT1);

            CompactNumberFormat obj2 = (CompactNumberFormat) ois2.readObject();
            assertEquals(obj2, COMPACT_FORMAT2, "Deserialized instance is not"
                    + " equal to the instance serialized in " + FILE_COMPACT_FORMAT2);
        }
    }

    // The objects are serialized using the serialize() method, the hex
    // dump printed is copied to respective object files
//    void serialize(CompactNumberFormat cnf) {
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
//            oos.writeObject(cnf);
//        } catch (IOException ioe) {
//            throw new RuntimeException(ioe);
//        }
//        byte[] ser = baos.toByteArray();
//        for (byte b : ser) {
//            System.out.print("" + String.format("%02x", b));
//        }
//    }

}

