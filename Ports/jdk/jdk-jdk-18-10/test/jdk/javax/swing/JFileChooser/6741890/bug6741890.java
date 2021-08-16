/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6741890 8171363
   @summary Deadlock in Win32ShellFolderManager2
   @author Pavel Porvatov
   @modules java.desktop/sun.awt
            java.desktop/sun.awt.shell:+open
   @run main bug6741890
*/

import sun.awt.shell.ShellFolder;
import sun.awt.OSInfo;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;

public class bug6741890 {
    /**
     * This mux is used to prevent NPE in the isLink and isFileSystem methods
     */
    private static final Object mux = new Object();

    private static final int COUNT = 100000;

    public static void main(String[] args) throws Exception {
        if (OSInfo.getOSType() != OSInfo.OSType.WINDOWS) {
            System.out.println("The test is applicable only for Windows. Skipped.");

            return;
        }

        String tmpDir = System.getProperty("java.io.tmpdir");

        if (tmpDir.length() == 0) { //'java.io.tmpdir' isn't guaranteed to be defined
            tmpDir = System.getProperty("user.home");
        }

        final ShellFolder tmpFile = ShellFolder.getShellFolder(new File(tmpDir));

        System.out.println("Temp directory: " + tmpDir);

        System.out.println("Stress test was run");

        Thread thread = new Thread() {
            public void run() {
                while (!isInterrupted()) {
                    ShellFolder.invoke(new Callable<Void>() {
                        public Void call() throws Exception {
                            synchronized (mux) {
                                tmpFile.isFileSystem();
                                tmpFile.isLink();
                            }

                            return null;
                        }
                    });
                }
            }
        };

        thread.start();

        for (int i = 0; i < COUNT; i++) {
            synchronized (mux) {
                clearField(tmpFile, "cachedIsLink");
                clearField(tmpFile, "cachedIsFileSystem");
            }

            tmpFile.isFileSystem();
            tmpFile.isLink();
        }

        thread.interrupt();
        thread.join();

        System.out.println("Test passed successfully");
    }

    private static void clearField(Object o, String fieldName) throws Exception {
        Field field = o.getClass().getDeclaredField(fieldName);

        field.setAccessible(true);

        field.set(o, null);
    }
}
