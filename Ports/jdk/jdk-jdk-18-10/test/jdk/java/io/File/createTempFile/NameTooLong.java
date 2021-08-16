/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8148023
 * @summary Verify that createTempFile() will not fail for long component names.
 */

import java.io.File;
import java.io.IOException;

public class NameTooLong {
    public static void main(String[] args) {
        String[][] prefixSuffix = new String[][] {
            new String[] {"1234567890123456789012345678901234567xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx89012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890","txt"},
            new String[] {"prefix","1234567890123456789012345678901234567xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx89012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890.txt"},
            new String[] {"prefix",".txt1234567890123456789012345678901234567xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx89012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890"}
        };

        int failures = 0;
        int index = 0;
        for (String[] ps : prefixSuffix) {
            File f;
            try {
                f = File.createTempFile(ps[0], ps[1],
                        new File(System.getProperty("test.dir", ".")));
                String s = f.toPath().getFileName().toString();
                if (!s.startsWith(ps[0].substring(0, 3))) {
                    System.err.printf("%s did not start with %s%n", s,
                        ps[0].substring(0, 3));
                    failures++;
                }
                if (ps[1].startsWith(".")
                    && !s.contains(ps[1].substring(0, 4))) {
                    System.err.printf("%s did not contain %s%n", s,
                        ps[1].substring(0, 4));;
                    failures++;
                }
            } catch (IOException e) {
                failures++;
                System.err.println();
                e.printStackTrace();
                System.err.println();
            }
            index++;
        }

        if (failures != 0) {
            throw new RuntimeException("Test failed!");
        }
    }
}
