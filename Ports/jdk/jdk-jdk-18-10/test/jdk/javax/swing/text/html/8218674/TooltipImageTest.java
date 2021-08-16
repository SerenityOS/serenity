/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8218674
 * @summary Tests if Images are rendered and scaled correctly in JToolTip.
 * @run main TooltipImageTest
 */
import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;

public class TooltipImageTest {

    private static void checkSize(JToolTip tip, int width, int height) {
        Dimension d = tip.getPreferredSize();
        Insets insets = tip.getInsets();
        //6 seems to be the extra width being allocated for some reason
        //for a tooltip window.
        if (!((d.width - insets.right - insets.left - 6) == width) &&
            !((d.height - insets.top - insets.bottom) == height)) {
            throw new RuntimeException("Test case fails: Expected width, height is " + width + ", " + height +
                    " whereas actual width, height are " + (d.width - insets.right - insets.left - 6) + " " +
                    (d.height - insets.top - insets.bottom));
        }
     }

    public static void main(String[] args) throws Exception {
        String PATH = TooltipImageTest.class.getResource("circle.png").getPath();
        SwingUtilities.invokeAndWait(() -> {
            JToolTip tip = new JToolTip();
            tip.setTipText("<html><img width=\"100\" src=\"file:///" + PATH + "\"></html>");
            checkSize(tip, 100, 100);

            tip.setTipText("<html><img height=\"100\" src=\"file:///" + PATH + "\"></html>");
            checkSize(tip, 100, 100);

            tip.setTipText("<html><img src=\"file:///" + PATH + "\"></html>");
            checkSize(tip, 200, 200);

            tip.setTipText("<html><img width=\"50\" src=\"file:///" + PATH + "\"></html>");
            checkSize(tip, 50, 50);

            tip.setTipText("<html><img height=\"50\" src=\"file:///" + PATH + "\"></html>");
            checkSize(tip, 50, 50);

            tip.setTipText("<html><img width=\"100\" height=\"50\" src=\"file:///" + PATH + "\"></html>");
            checkSize(tip, 100, 50);
        });

        System.out.println("Test case passed.");
    }
}
