/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

/**
 * @test
 * @key headful
 * @bug 8008728
 * @summary [macosx] Swing. JDialog. Modal dialog goes to background
 * @author Alexandr Scherbatiy
 * @library /lib/client
 * @build ExtendedRobot
 * @run main ModalDialogOrderingTest
 */
public class ModalDialogOrderingTest {

    private static final Color DIALOG_COLOR = Color.GREEN;
    private static final Color FRAME_COLOR = Color.BLUE;

    public static void main(String[] args) {

        final Frame frame = new Frame("Test");
        frame.setSize(400, 400);
        frame.setBackground(FRAME_COLOR);
        frame.setVisible(true);

        final Dialog modalDialog = new Dialog(null, true);
        modalDialog.setTitle("Modal Dialog");
        modalDialog.setSize(400, 200);
        modalDialog.setBackground(DIALOG_COLOR);
        modalDialog.setModal(true);

        new Thread(new Runnable() {

            @Override
            public void run() {
                runTest(modalDialog, frame);
            }
        }).start();

        modalDialog.setVisible(true);
    }

    private static void runTest(Dialog dialog, Frame frame) {
        try {
            ExtendedRobot robot = new ExtendedRobot();
            robot.setAutoDelay(50);
            robot.mouseMove(300, 300);

            while (!dialog.isVisible()) {
                robot.waitForIdle(1000);
            }

            Rectangle dialogBounds = dialog.getBounds();
            Rectangle frameBounds = frame.getBounds();

            int y1 = dialogBounds.y + dialogBounds.height;
            int y2 = frameBounds.y + frameBounds.height;

            int clickX = frameBounds.x + frameBounds.width / 2;
            int clickY = y1 + (y2 - y1) / 2;

            robot.mouseMove(clickX, clickY);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle(1000);

            int colorX = dialogBounds.x + dialogBounds.width / 2;
            int colorY = dialogBounds.y + dialogBounds.height / 2;

            Color color = robot.getPixelColor(colorX, colorY);


            if (!DIALOG_COLOR.equals(color)) {
                throw new RuntimeException("The frame is on top"
                        + " of the modal dialog!");
            }else{
                frame.dispose();
                dialog.dispose();
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
