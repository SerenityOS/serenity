/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6848475
 * @summary JSlider does not display the correct value of its BoundedRangeModel
 * @author Pavel Porvatov
 * @modules java.desktop/javax.swing.plaf.basic:open
 * @run main bug6848475
 */

import javax.swing.*;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.InputEvent;
import java.lang.reflect.Field;

public class bug6848475 {
    private static JFrame frame;

    private static JSlider slider;

    private static Robot robot;

    private static int thumbRectX;

    public static void main(String[] args) throws Exception {

        robot = new Robot();
        robot.setAutoDelay(100);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame = new JFrame();

                DefaultBoundedRangeModel sliderModel = new DefaultBoundedRangeModel() {
                    public void setValue(int n) {
                        // Don't allow value to be changed
                    }
                };

                slider = new JSlider(sliderModel);

                frame.getContentPane().add(slider);
                frame.pack();
                frame.setVisible(true);
            }
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Point p = slider.getLocationOnScreen();

                robot.mouseMove(p.x, p.y);
            }
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                thumbRectX = getThumbRectField().x;

                Point p = slider.getLocationOnScreen();

                robot.mouseMove(p.x, p.y);
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
                robot.mouseMove(p.x + 20, p.y);
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            }
        });

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                Rectangle newThumbRect = getThumbRectField();

                if (newThumbRect.x != thumbRectX) {
                    throw new RuntimeException("Test failed: the thumb was moved");
                }

                frame.dispose();
            }
        });
    }

    private static Rectangle getThumbRectField() {
        try {
            SliderUI ui = slider.getUI();

            Field field = BasicSliderUI.class.getDeclaredField("thumbRect");

            field.setAccessible(true);

            return (Rectangle) field.get(ui);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
