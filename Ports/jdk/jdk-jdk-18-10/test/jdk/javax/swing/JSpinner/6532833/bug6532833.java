/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6532833 7077259
 * @summary PIT: Metal LAF - The right side border is not shown for the Spinner after the removing the buttons
 * @author Pavel Porvatov
 * @run main/othervm -Dswing.defaultlaf=javax.swing.plaf.metal.MetalLookAndFeel bug6532833
 */

import javax.swing.*;
import java.awt.*;

public class bug6532833 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JSpinner[] spinners = new JSpinner[2];

                for (int i = 0; i < spinners.length; i++) {
                    JSpinner spinner = new JSpinner();

                    spinner.setValue(2010);

                    Component arrowUp = spinner.getComponent(0);
                    Component arrowDown = spinner.getComponent(1);

                    LayoutManager layout = spinner.getLayout();

                    layout.removeLayoutComponent(arrowUp);
                    layout.removeLayoutComponent(arrowDown);

                    if (i == 1) {
                        spinner.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
                    }

                    spinners[i] = spinner;
                }

                // Do layout of spinners components
                JFrame frame = new JFrame();

                for (JSpinner spinner : spinners) {
                    frame.getContentPane().add(spinner);
                }

                frame.pack();

                for (JSpinner spinner : spinners) {
                    Insets insets = spinner.getInsets();

                    if (spinner.getWidth() != insets.left + insets.right + spinner.getEditor().getWidth()) {
                        throw new RuntimeException("Spinner editor width is invalid");
                    }
                }

                frame.dispose();
            }
        });
    }
}
