/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8138881
 * @summary  typo in OSInfo.java
 * @modules  java.desktop/sun.awt
 * @requires (os.family == "windows")
 * @run main TestOSVersion
 */

import sun.awt.OSInfo;

public class TestOSVersion {

    private static final String WIN_VISTA_VERSION = "6.0";

    public static void main(String[] arg) {

        String oSVersion = System.getProperty("os.version");
        if (WIN_VISTA_VERSION.equals(oSVersion)) {
            if (OSInfo.getWindowsVersion().toString().equals("6.1") ) {
                throw new RuntimeException("Incorrect Windows VISTA OS version "
                        + "in OSInfo");
            }
        }
    }
}
