/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 8071668
  @summary Check whether clipboard see changes from external process after taking ownership
  @author Anton Nashatyrev: area=datatransfer
  @library /test/lib
  @build jdk.test.lib.Utils
  @run main ClipboardInterVMTest
*/

import jdk.test.lib.Utils;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClipboardInterVMTest {

    static CountDownLatch lostOwnershipMonitor = new CountDownLatch(1);
    static CountDownLatch flavorChangedMonitor = new CountDownLatch(1);
    static Process process;

    public static void main(String[] args) throws Throwable {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();

        if (args.length > 0) {
            System.out.println("Changing clip...");
            clip.setContents(new StringSelection("pong"), null);
            System.out.println("done");
            // keeping this process running for a while since on Mac the clipboard
            // will be invalidated via NSApplicationDidBecomeActiveNotification
            // callback in the main process after this child process finishes
            Thread.sleep(60 * 1000);
            return;
        };


        clip.setContents(new CustomSelection(), new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                System.out.println("ClipboardInterVMTest.lostOwnership");
                lostOwnershipMonitor.countDown();
            }
        });

        clip.addFlavorListener(new FlavorListener() {
            @Override
            public void flavorsChanged(FlavorEvent e) {
                System.out.println("ClipboardInterVMTest.flavorsChanged");
                flavorChangedMonitor.countDown();
            }
        });

        System.out.println("Starting external clipboard modifier...");
        new Thread(() -> runTest(ClipboardInterVMTest.class.getCanonicalName(), "pong")).start();

        String content = "";
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 30 * 1000) {
            Transferable c = clip.getContents(null);
            if (c.isDataFlavorSupported(DataFlavor.plainTextFlavor)) {
                Reader reader = DataFlavor.plainTextFlavor.getReaderForText(c);
                content = new BufferedReader(reader).readLine();
                System.out.println(content);
                if (content.equals("pong")) {
                    break;
                }
            }
            Thread.sleep(200);
        }

        if (!lostOwnershipMonitor.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("No LostOwnership event received.");
        };

        if (!flavorChangedMonitor.await(10, TimeUnit.SECONDS)) {
            throw new RuntimeException("No FlavorsChanged event received.");
        };

        if (!content.equals("pong")) {
            throw new RuntimeException("Content was not passed.");
        }

        process.destroy();

        System.out.println("Passed.");
    }

    private static void runTest(String main, String... args)  {

        try {
            List<String> opts = new ArrayList<>();
            opts.add(getJavaExe());
            opts.addAll(Arrays.asList(Utils.getTestJavaOpts()));
            opts.add("-cp");
            opts.add(System.getProperty("test.class.path", System.getProperty("java.class.path")));

            opts.add(main);
            opts.addAll(Arrays.asList(args));

            ProcessBuilder pb = new ProcessBuilder(opts.toArray(new String[0]));
            process = pb.start();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private static String getJavaExe() throws IOException {
        File p  = new File(System.getProperty("java.home"), "bin");
        File j = new File(p, "java");
        if (!j.canRead()) {
            j = new File(p, "java.exe");
        }
        if (!j.canRead()) {
            throw new RuntimeException("Can't find java executable in " + p);
        }
        return j.getCanonicalPath();
    }

    static class CustomSelection implements Transferable {
        private static final DataFlavor[] flavors = { DataFlavor.allHtmlFlavor };

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors[0].equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, java.io.IOException {
            if (isDataFlavorSupported(flavor)) {
                return "ping";
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }
    }
}
