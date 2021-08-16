/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4278402
 * @library /java/text/testlib
 * @build DateFormatSymbolsSerializationTest HexDumpReader
 * @run main DateFormatSymbolsSerializationTest
 * @summary Make sure DateFormatSymbols serialization
 */

import java.util.*;
import java.text.*;
import java.io.*;

public class DateFormatSymbolsSerializationTest {

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        try {
            boolean err = false;
            Locale.setDefault(Locale.ENGLISH);
            SimpleDateFormat sdf =
                    new SimpleDateFormat("yyyy.MM.dd E hh.mm.ss zzz",
                                                        Locale.ENGLISH);
            Calendar calendar =
                    new GregorianCalendar(TimeZone.getTimeZone("GMT"),
                                                      Locale.ENGLISH);
            calendar.setTime(new Date(0L));
            DecimalFormat df = new DecimalFormat("");
            df.setDecimalSeparatorAlwaysShown(true);
            df.setGroupingSize(3);
            df.setMultiplier(1);
            df.setNegativePrefix("-");
            df.setNegativeSuffix("");
            df.setPositivePrefix("");
            df.setPositiveSuffix("");
            df.setMaximumFractionDigits(3); //
            df.setMinimumIntegerDigits(1);  // for compatibility 1.2 and 1.3
            df.setMaximumIntegerDigits(40); //

            sdf.setCalendar(calendar);
            sdf.setNumberFormat(df);

            SimpleDateFormat sdf1;

            if (args.length > 0) {
                try (FileOutputStream fos =
                        new FileOutputStream("SDFserialized.ser")) {
                    ObjectOutputStream oStream = new ObjectOutputStream(fos);
                    oStream.writeObject(sdf);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try (InputStream is =
                     HexDumpReader.getStreamFromHexDump("SDFserialized.ser.txt")) {
                    ObjectInputStream iStream = new ObjectInputStream(is);
                    sdf1 = (SimpleDateFormat)iStream.readObject();
                }

                DateFormatSymbols dfs = sdf.getDateFormatSymbols();
                DateFormatSymbols dfs1 = sdf1.getDateFormatSymbols();
                System.out.println(sdf + "," + sdf.toPattern());
                System.out.println(sdf1 + "," + sdf1.toPattern());

                // time zone display names should not be a part of this
                // compatibility test. See 4112924 and 4282899.
                dfs.setZoneStrings(dfs1.getZoneStrings());
                // localPatternChars should not be a part of this
                // compatibility test. See 4322313.
                dfs.setLocalPatternChars(dfs1.getLocalPatternChars());
                sdf.setDateFormatSymbols(dfs);

                // decimal format symbols should not be part of this
                // compatibility test - old decimal format symbols get filled
                // in with the root locale (4290801)
                DecimalFormat df1 = (DecimalFormat) sdf1.getNumberFormat();
                df1.setDecimalFormatSymbols(df.getDecimalFormatSymbols());

                if (!dfs.equals(dfs1)) {
                    err = true;
                    System.err.println(
                        "Error: serialized DateFormatSymbols is different");
                }
                if (!sdf.equals(sdf1)) {
                    err = true;
                    System.err.println(
                        "Error: serialized SimpleDateFormat is different");
                }
                if (err) {
                    throw new Exception("Serialization failed.");
                }
            }
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }
}
