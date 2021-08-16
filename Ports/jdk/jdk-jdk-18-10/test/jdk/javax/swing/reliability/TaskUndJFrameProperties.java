/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.swing.*;
import java.awt.*;

/*
 * @test
 * @key headful
 * @summary Construct a Undecorated JFrame, try to change the properties
 *          using setVisible() method.
 * @author Aruna Samji
 * @library /lib/client
 * @build ExtendedRobot
 * @run main TaskUndJFrameProperties
 */

public class TaskUndJFrameProperties extends Task<GUIUndFrame> {

    public static void main (String[] args) throws Exception {
        new TaskUndJFrameProperties(GUIUndFrame.class, new ExtendedRobot()).task();
    }

    TaskUndJFrameProperties(Class guiClass, ExtendedRobot robot) throws Exception {
        super(guiClass, robot);
    }

    public void task() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            gui.jframe2.setVisible(true);
            gui.jframe1.setVisible(true);

            gui.jframe1.getContentPane().removeAll();
            gui.jframe2.getContentPane().removeAll();
            gui.jframe1.getContentPane().add(gui.jbutton1);
            gui.jframe2.getContentPane().add(gui.jbutton2);
            gui.jframe1.revalidate();
            gui.jframe2.revalidate();
        });
        robot.waitForIdle(1000);

        Point button1Origin = gui.jbutton1.getLocationOnScreen();
        Point button1Center = gui.jbutton1.getLocationOnScreen();
        button1Center.translate(gui.jbutton1.getWidth()/2, gui.jbutton1.getHeight()/2);
        Point button2Origin = gui.jbutton2.getLocationOnScreen();
        Point button2Center = gui.jbutton2.getLocationOnScreen();
        button2Center.translate(gui.jbutton2.getWidth()/2, gui.jbutton2.getHeight()/2);

        robot.glide(button1Origin, button1Center);
        robot.waitForIdle(1000);
        robot.click();
        //After Hide
        if (gui.win_deact == false)
            throw new RuntimeException("Undecorated JFrame has not triggered " +
                    "WINDOW_DEACTIVATED event when in Hide state\n");

        if (gui.jframe1.hasFocus() == true)
            throw new RuntimeException("Undecorated Frame Still has Focus even " +
                    "when in Hide state\n");

        //click on the jbutton2 in jframe2
        SwingUtilities.invokeAndWait(gui.jframe2::toFront);
        robot.waitForIdle(1000);
        robot.glide(button2Origin, button2Center);
        robot.waitForIdle(1000);
        robot.click();
        //After Show
        if (gui.win_act == false)
            throw new RuntimeException("Undecorated Frame can't trigger " +
                    "WINDOW_ACTIVATED when visible\n");
    }
}
