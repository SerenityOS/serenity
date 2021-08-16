/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful printer
 * @bug 7161283 8158520
 * @summary  Toolkit.getPrintJob throws NPE if no printer available
 * @run main PrinterException
 */
import java.awt.Frame;
import java.awt.JobAttributes;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class PrinterException {

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        Thread t = new Thread (() -> {
            robot.delay(2000);
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
            robot.waitForIdle();
        });
        Toolkit tk = Toolkit.getDefaultToolkit();
        PrintJob pj = null;

        int[][] pageRange = new int[][]{new int[]{1,1}};
        JobAttributes ja = new JobAttributes(1,
                java.awt.JobAttributes.DefaultSelectionType.ALL,
                JobAttributes.DestinationType.FILE, JobAttributes.DialogType.NATIVE,
                "filename.ps", Integer.MAX_VALUE, 1,
                JobAttributes.MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES,
                 pageRange, "", JobAttributes.SidesType.ONE_SIDED);

        Frame testFrame = new Frame("print");
        try {
            if (tk != null) {
                t.start();
                pj = tk.getPrintJob(testFrame, null, ja, null);
            }
        } finally {
            testFrame.dispose();
        }
    }

}
