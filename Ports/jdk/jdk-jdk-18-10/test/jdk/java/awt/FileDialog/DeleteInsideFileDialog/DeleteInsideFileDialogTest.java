/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @bug 8075516
  @requires os.family=="windows"
  @summary Deleting a file from either the open or save java.awt.FileDialog
           hangs.
  @run main/manual DeleteInsideFileDialogTest
*/

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DeleteInsideFileDialogTest {

    private static Path dir;
    private static Path file1;
    private static Path file2;
    private static Frame f;
    private static FileDialog fd;

    public static void main(String[] args) throws Exception {

        String instructions =
                "1) Delete file deleteMe.tst in the opened File Dialog window" +
                   " using the right click popup menu\n" +
                "2) Select thenSelectMe.tst file in the File Dialog and press" +
                   " Open (if this is not possible the test fails)\n";
        dir = Files.createTempDirectory("Test");
        file1 = Files.createFile(Paths.get(dir.toString(), "deleteMe.tst"));
        file2 = Files.createFile(Paths.get(dir.toString(), "thenSelectMe.tst"));
        try {
            f = new Frame("Instructions");
            f.add(new TextArea(instructions, 6, 60, TextArea.SCROLLBARS_NONE));
            f.pack();
            f.setLocation(100, 500);
            f.setVisible(true);

            fd = new FileDialog((Frame)null);
            fd.setDirectory(dir.toString());
            fd.setVisible(true);
            if (fd.getFile() == null) {
                throw new RuntimeException("Failed");
            }
        } finally {
            if (fd != null) {
                fd.dispose();
            }
            if (f != null) {
                f.dispose();
            }
            Files.deleteIfExists(file1);
            Files.deleteIfExists(file2);
            Files.deleteIfExists(dir);
        }
    }
}
