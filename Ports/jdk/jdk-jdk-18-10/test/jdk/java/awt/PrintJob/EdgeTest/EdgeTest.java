/*
 * Copyright (c) 1998, 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4092755
 * @summary Verifies that (0, 0) is the upper-left corner of the page, not
 *          the upper-left corner adjusted for the margins.
 * @author dpm
 * @run main/manual EdgeTest
 */

import java.awt.*;
import java.awt.event.*;

public class EdgeTest extends Panel {
    public void init() {
        Frame f = new Frame("EdgeTest");
        f.setSize(50, 50);
        f.addWindowListener( new WindowAdapter() {
                                    public void windowClosing(WindowEvent ev) {
                                        System.exit(0);
                                    }
                                }
                            );
        f.setVisible(true);
        JobAttributes job = new JobAttributes();
        job.setDialog(JobAttributes.DialogType.NONE);
        PrintJob pj = getToolkit().getPrintJob(f, "EdgeTest", job, null);
        if (pj != null) {
            Graphics g = pj.getGraphics();
            Dimension d = pj.getPageDimension();
            g.setColor(Color.black);
            g.setFont(new Font("Serif", Font.PLAIN, 12));

            //top
            g.drawLine(0, 0, d.width, 0);

            //left
            g.drawLine(0, 0, 0, d.height);

            //bottom
            g.drawLine(0, d.height, d.width, d.height);

            //right
            g.drawLine(d.width, 0, d.width, d.height);

            g.drawString("This page should have no borders!",
                         d.width / 2 - 100, d.height / 2 - 10);
            g.dispose();
            pj.end();
        }
    }

    public static void main(String[] args) {
        new EdgeTest().init();
    }
}
