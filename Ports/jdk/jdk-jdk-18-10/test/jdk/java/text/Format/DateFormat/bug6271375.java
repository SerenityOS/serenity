/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6271375 7059546
 * @summary Make sure DateFormatSymbols serialization works
 *    correctly for 'zoneStrings' field
 */

import java.util.*;
import java.text.*;
import java.io.*;

public class bug6271375 {

    public static void main(String[] args) throws Exception {
        DateFormatSymbols dfsSrc = DateFormatSymbols.getInstance();

        try (FileOutputStream fos = new FileOutputStream("dfs.ser");
             ObjectOutputStream oStream = new ObjectOutputStream(fos)) {
            oStream.writeObject(dfsSrc);
        } catch (Exception e) {
            throw new RuntimeException("An exception is thrown.", e);
        }

        try (FileInputStream fis = new FileInputStream("dfs.ser");
             ObjectInputStream iStream = new ObjectInputStream(fis)) {
            DateFormatSymbols dfsDest = (DateFormatSymbols)iStream.readObject();

            String[][] zoneStringsSrc = dfsSrc.getZoneStrings();
            String[][] zoneStringsDest = dfsDest.getZoneStrings();

            if (!Arrays.deepEquals(zoneStringsSrc, zoneStringsDest)) {
                throw new RuntimeException("src and dest zone strings are not equal");
            }
        } catch (Exception e) {
            throw new RuntimeException("An exception is thrown.", e);
        }
    }
}
