/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6412896
 * @summary Make sure that an IllegalArgumentException is thrown
 *    if the length of any row in zoneStrings array is less than 5
 */

import java.text.*;

public class bug6412896 {

    static final String[][] zoneOK = {{"America/Los_Angeles", "Pacific Standard Time", "PST", "Pacific Daylight Time", "PDT"}};
    static final String[][] zoneNG = {{"America/Los_Angeles", "Pacific Standard Time", "PST", "Pacific Daylight Time"}};

    public static void main(String[] args) {

        DateFormatSymbols dfs = DateFormatSymbols.getInstance();

        dfs.setZoneStrings(zoneOK);

        try {
            dfs.setZoneStrings(zoneNG);
            throw new RuntimeException("should throw an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
    }
}
