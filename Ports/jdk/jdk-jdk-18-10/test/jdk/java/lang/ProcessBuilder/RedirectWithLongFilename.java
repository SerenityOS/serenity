/*
 * Copyright (c) 2015 SAP SE. All rights reserved.
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
 * @bug 8072611
 * @requires (os.family == "windows")
 * @summary ProcessBuilder Redirect to file appending on Windows should work with long file names
 * @author Thomas Stuefe
 */

import java.io.File;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RedirectWithLongFilename {

    public static void main(String[] args) throws Exception {

        // Redirect ProcessBuilder output to a file whose pathlen is > 255.
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        File dir2 = null;
        File longFileName = null;

        try {
            dir2 = Files.createTempDirectory(tmpDir, "RedirectWithLongFilename").toFile();
            dir2.mkdirs();
            longFileName = new File(dir2,
                "012345678901234567890123456789012345678901234567890123456789" +
                "012345678901234567890123456789012345678901234567890123456789" +
                "012345678901234567890123456789012345678901234567890123456789" +
                "012345678901234567890123456789012345678901234567890123456789" +
                "0123456789");

            ProcessBuilder pb = new ProcessBuilder("hostname.exe");
            pb.redirectOutput(Redirect.appendTo(longFileName));
            Process p = pb.start();
            p.waitFor();

            if (longFileName.exists()) {
                System.out.println("OK");
            } else {
                throw new RuntimeException("Test failed.");
            }

        } finally {
            longFileName.delete();
            dir2.delete();
        }

    }

}
