/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6176051 4858457
   @summary Check whether reserved names are handled correctly on Windows
 */

import java.io.File;
import java.io.IOException;

public class WinDeviceName {
    private static String devnames[] = {
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
        "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2",
        "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        "CLOCK$"
    };
    public static void main(String[] args) {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            return;
        }

        for (int i = 0; i < devnames.length; i++) {
            String names[] = { devnames[i], devnames[i] + ".TXT",
                               devnames[i].toLowerCase(),
                               devnames[i].toLowerCase() + ".txt" };

            for (String name : names) {
                File f = new File(name);
                if (f.isFile()) {
                    if ("CLOCK$".equals(devnames[i]) &&
                        (osName.startsWith("Windows 9") ||
                         osName.startsWith("Windows Me"))) {
                        //"CLOCK$" is a reserved device name for NT
                        continue;
                    }
                    throw new RuntimeException("isFile() returns true for " +
                            "Device name " + devnames[i]);
                }

                if (!"CLOCK$".equals(devnames[i])) {
                    try {
                        System.out.println((new File(name)).getCanonicalPath());
                    } catch(IOException ie) {
                        throw new RuntimeException("Fail to get canonical " +
                                "path for " + name);
                    }
                }
            }
        }
    }
}
