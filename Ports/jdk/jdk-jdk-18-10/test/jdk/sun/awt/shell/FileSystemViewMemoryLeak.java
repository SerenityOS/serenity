/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8175015
 * @summary FileSystemView.isDrive(File) memory leak on "C:\" file reference
 * @modules java.desktop/sun.awt.shell
 * @requires (os.family == "windows")
 * @run main/othervm/timeout=320 -Xmx8m FileSystemViewMemoryLeak
 */
import java.io.File;
import java.text.NumberFormat;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

public class FileSystemViewMemoryLeak {

    public static void main(String[] args) {
        test();
    }

    // Will run the test no more than 300 seconds
    static long endtime = System.nanoTime() + TimeUnit.SECONDS.toNanos(300);

    private static void test() {

        File root = new File("C:\\");
        System.out.println("Root Exists: " + root.exists());
        System.out.println("Root Absolute Path: " + root.getAbsolutePath());
        System.out.println("Root Is Directory?: " + root.isDirectory());

        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        NumberFormat nf = NumberFormat.getNumberInstance();

        int iMax = 50000;
        long lastPercentFinished = 0L;
        for (int i = 0; i < iMax; i++) {
            if (isComplete()) {
                System.out.println("Time is over");
                return;
            }

            long percentFinished = Math.round(((i * 1000d) / (double) iMax));

            if (lastPercentFinished != percentFinished) {
                double pf = ((double) percentFinished) / 10d;
                String pfMessage = String.valueOf(pf) + " % (" + i + "/" + iMax + ")";

                long totalMemory = Runtime.getRuntime().totalMemory() / 1024;
                long freeMemory = Runtime.getRuntime().freeMemory() / 1024;
                long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
                String memMessage = "[Memory Used: " + nf.format(totalMemory) +
                                    " kb Free=" + nf.format(freeMemory) +
                                    " kb Max: " + nf.format(maxMemory) + " kb]";

                System.out.println(pfMessage + " " + memMessage);
                lastPercentFinished = percentFinished;
            }

            boolean floppyDrive = fileSystemView.isFloppyDrive(root);
            boolean computerNode = fileSystemView.isComputerNode(root);

            // "isDrive()" seems to be the painful method...
            boolean drive = fileSystemView.isDrive(root);
        }
    }

    private static boolean isComplete() {
        return endtime - System.nanoTime() < 0;
    }
}

