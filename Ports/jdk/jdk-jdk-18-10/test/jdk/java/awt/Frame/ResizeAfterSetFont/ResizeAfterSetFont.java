/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2012 IBM Corporation
 */

/*
 @test
 @key headful
 @bug 7170655
 @summary Frame size does not change after changing font
 @author Jonathan Lu
 @library ../../regtesthelpers
 @build Util
 @run main ResizeAfterSetFont
 */

import java.awt.*;
import test.java.awt.regtesthelpers.Util;

public class ResizeAfterSetFont {

    public static void main(String[] args) throws Exception {
        Frame frame = new Frame("bug7170655");
        frame.setLayout(new BorderLayout());
        frame.setBackground(Color.LIGHT_GRAY);

        Panel panel = new Panel();
        panel.setLayout(new GridLayout(0, 1, 1, 1));

        Label label = new Label("Test Label");
        label.setBackground(Color.white);
        label.setForeground(Color.RED);
        label.setFont(new Font("Dialog", Font.PLAIN, 12));

        panel.add(label);
        frame.add(panel, "South");
        frame.pack();
        frame.setVisible(true);

        Util.waitForIdle(null);

        Dimension dimBefore = frame.getSize();
        label.setFont(new Font("Dialog", Font.PLAIN, 24));

        frame.validate();
        frame.pack();
        Dimension dimAfter = frame.getSize();

        if (dimBefore.equals(dimAfter)) {
            throw new Exception(
                    "Frame size does not change after Label.setFont()!");
        }
    }
}
