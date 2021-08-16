/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * Check the consistency between the regression tests and the currency data in the JRE
 */

import java.io.*;
import java.lang.reflect.*;
import java.security.*;
import java.util.Currency;

class CheckDataVersion {
    static final String datafile = "tablea1.txt";
    static final String FILEVERSIONKEY = "FILEVERSION=";
    static final String DATAVERSIONKEY = "DATAVERSION=";
    static String fileVersion;
    static String dataVersion;
    static boolean checked = false;

    static void check() {
        if (!checked) {
            try {
                FileReader fr = new FileReader(new File(System.getProperty("test.src", "."), datafile));
                BufferedReader in = new BufferedReader(fr);
                String line;

                while ((line = in.readLine()) != null) {
                    if (line.startsWith(FILEVERSIONKEY)) {
                        fileVersion = line.substring(FILEVERSIONKEY.length());
                    }
                    if (line.startsWith(DATAVERSIONKEY)) {
                        dataVersion = line.substring(DATAVERSIONKEY.length());
                    }
                    if (fileVersion != null && dataVersion != null) {
                        break;
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            }

            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    try {
                        InputStream in = Currency.class.getModule().getResourceAsStream("java/util/currency.data");
                        String sep = File.separator;
                        DataInputStream dis = new DataInputStream(in);
                        int magic = dis.readInt();
                        if (magic != 0x43757244) {
                            throw new RuntimeException("The magic number in the JRE's currency data is incorrect.  Expected: 0x43757244, Got: 0x"+magic);
                        }
                        int fileVerNumber = dis.readInt();
                        int dataVerNumber = dis.readInt();
                        if (Integer.parseInt(fileVersion) != fileVerNumber ||
                            Integer.parseInt(dataVersion) != dataVerNumber) {
                            throw new RuntimeException("Test data and JRE's currency data are inconsistent.  test: (file: "+fileVersion+" data: "+dataVersion+"), JRE: (file: "+fileVerNumber+" data: "+dataVerNumber+")");
                        }
System.out.println("test: (file: "+fileVersion+" data: "+dataVersion+"), JRE: (file: "+fileVerNumber+" data: "+dataVerNumber+")");
                    } catch (IOException ioe) {
                        throw new RuntimeException(ioe);
                    }
                    return null;
                }
            });
            checked = true;
        }
    }
}
