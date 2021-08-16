/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6401380
 * @summary JSlider - mouse click ont the left side of the knob is ignored.
 * @library /lib/client
 * @build ExtendedRobot
 * @author Alexander Potochkin
 * @run main bug6401380
 */

import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.InputEvent;

public class bug6401380 extends JFrame {
    private static JSlider slider;

    public bug6401380() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        slider = new JSlider();
        slider.setMajorTickSpacing(0);
        slider.setMaximum(50);
        slider.setMinorTickSpacing(10);
        slider.setPaintLabels(true);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);

        // MetalSliderUI overrides scrollDueToClickInTrack() method
        // so this test doens't work for Metal
        slider.setUI(new BasicSliderUI(slider));

        add(slider);
        setSize(200, 200);
    }

    public static void main(String[] args) throws Exception {

        ExtendedRobot robot = new ExtendedRobot();
        robot.setAutoDelay(10);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new bug6401380().setVisible(true);
            }
        });
        robot.waitForIdle();

        Point l = slider.getLocationOnScreen();
        robot.glide(0, 0, l.x + slider.getWidth() / 2, l.y + slider.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        robot.waitForIdle();

        if (slider.getValue() == slider.getMaximum()) {
            throw new RuntimeException("Slider value unchanged");
        }
    }
}
