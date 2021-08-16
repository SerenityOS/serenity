/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851363 8025988 8025990
 * @summary Tests the save to file dialog has a title.
 * @run main/manual=yesno/othervm SaveDialogTitleTest
 */

import java.awt.*;

public class SaveDialogTitleTest {

    public static void main(String args[]) {

        System.out.print("Once the dialog appears, press OK and the ");
        System.out.print("Save to File dialog should appear and it ");
        System.out.println("must have a window title else the test fails.");
        System.out.println("To test 8025988: Range should be selected with pages 3 to 8.");
        System.out.println("To test 8025990: Paper should be Legal and in Landscape.");
        Toolkit tk = Toolkit.getDefaultToolkit();
        JobAttributes jobAttributes = new JobAttributes();
        jobAttributes.setDestination(JobAttributes.DestinationType.FILE);
        jobAttributes.setDefaultSelection(JobAttributes.DefaultSelectionType.RANGE);
        jobAttributes.setPageRanges(new int[][]{new int[]{3,8}});
        PageAttributes page = new PageAttributes();
        page.setMedia(PageAttributes.MediaType.LEGAL);
        page.setOrientationRequested(PageAttributes.
                                        OrientationRequestedType.LANDSCAPE);

        PrintJob printJob =
            tk.getPrintJob(new Frame(), "Save Title Test",
                           jobAttributes, page);
        if (printJob != null) { // in case user cancels.
          printJob.end();
        }
        System.exit(0); // safe because use 'othervm'
    }
}
