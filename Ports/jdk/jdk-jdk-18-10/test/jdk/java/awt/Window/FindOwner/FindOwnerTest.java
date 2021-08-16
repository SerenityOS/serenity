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

/* @bug 8139227
   @summary Text fields in JPopupMenu structure do not receive focus in hosted
            Applets
   @author Semyon Sadetsky
*/

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FindOwnerTest extends Applet
{

    private boolean gained;

    public void init() {
        super.init();
    }

    @Override
    public void start() {
        Window owner = SwingUtilities.windowForComponent(this);

        Window window1 = new Window(owner);
        window1.setVisible(true);

        Window window2 = new Window(window1);
        window2.setFocusable(true);
        JTextField field = new JTextField("JTextField");
        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                gained = true;
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
        window2.setBounds(100, 100, 200, 200);
        window2.add(field);
        window2.setVisible(true);

        try {
            gained = false;
            Robot robot = new Robot();
            robot.setAutoDelay(50);
            robot.waitForIdle();
            robot.delay(200);

            Point p = field.getLocationOnScreen();
            System.out.println(p);
            robot.mouseMove(p.x + 1, p.y + 1);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.waitForIdle();
            robot.delay(200);

            if (!gained) {
                throw new Exception("Focus is not gained upon mouse click");
            }
            System.out.println("ok");
        } catch (SecurityException e) {

            JOptionPane optionPane = new JOptionPane(
                    "You are in the browser so test is manual. Try to " +
                    "click \"JTextField\" in the opened window then press OK " +
                    "below",
                    JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
            JDialog dialog =
                    optionPane.createDialog(null,"FindOwnerTest instruction");
            dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
            dialog.setVisible(true);
            if (!gained) {
                throw new RuntimeException(
                        "Focus is not gained upon mouse click");
            }
            System.out.println("ok");
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            window1.dispose();
            stop();
        }
    }
}
