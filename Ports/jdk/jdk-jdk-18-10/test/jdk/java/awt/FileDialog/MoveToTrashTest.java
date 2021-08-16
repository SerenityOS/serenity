/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
  @key headful
  @bug 8190515 8193468
  @summary java.awt.Desktop.moveToTrash(File) prompts on Windows 7 but not on Mac.
  @run main MoveToTrashTest
*/

import java.io.File;
import java.awt.Desktop;
import java.awt.Robot;
import java.io.IOException;
import java.awt.AWTException;

public class MoveToTrashTest {
    private static File file = null;
    private static boolean fileStatus = false;

    public static void main(String[] args) {
        if (!Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            System.out.println("Move to trash action is not supported on the"+
               " platform under test. Marking the test passed");
        } else {
            try {
                file = File.createTempFile("TestFile","txt");
            } catch (IOException ex) {
                throw new RuntimeException("Test failed. Exception thrown: ", ex);
            }

            // In case any UI that may pop up while deleting the file would
            // block this thread until the user actions them. Hence do file
            // check in a different thread and we assume it takes about sometime
            // till it deletes the file(or popup) on the main thread.
            new Thread(null, MoveToTrashTest::checkFileExistence, "FileCheck", 0, false).start();
            fileStatus = Desktop.getDesktop().moveToTrash(file);
        }
    }

    private static void checkFileExistence() {
        Robot robot = null;
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Test failed. Exception thrown: ", ex);
        }

        robot.delay(1500);

        if (!fileStatus) {
            throw new RuntimeException("Test failed due to error while deleting the file");
        } else {
            if (file.exists()) {
                throw new RuntimeException("Test failed");
            } else {
                System.out.println("Test passed");
            }
        }
    }
}

