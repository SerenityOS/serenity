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
 * @summary Construct a jframe with some components and zoom the frame and bring it back to normal state.
 * @author Aruna Samji
 * @library /lib/client
 * @build ExtendedRobot
 * @run main TaskZoomJFrameRepaint
 */

public class TaskZoomJFrameRepaint extends Task<GUIZoomFrame> {

    public static void main (String[] args) throws Exception {
        new TaskZoomJFrameRepaint(GUIZoomFrame.class, new ExtendedRobot()).task();
    }

    TaskZoomJFrameRepaint(Class guiClass, ExtendedRobot robot) throws Exception {
         super(guiClass, robot);
    }

    public void task() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            gui.jframe2.setVisible(true);
            gui.jframe2.getContentPane().removeAll();
            gui.jframe2.getContentPane().add(gui.jbutton);
            gui.jframe2.getContentPane().add(gui.jtextarea);
            if (gui.jframe2.getExtendedState() != Frame.NORMAL)
                gui.jframe2.setExtendedState(Frame.NORMAL);
        });
        robot.waitForIdle(1000);

        Point buttonOrigin, newbuttonOrigin,  newbuttonCenter;
        Point textareaOrigin, newtextareaOrigin,  newtextareaCenter ;

        //to find the lenght and width of the component originally
        buttonOrigin = gui.jbutton.getLocationOnScreen();
        textareaOrigin = gui.jtextarea.getLocationOnScreen();

        if (Toolkit.getDefaultToolkit().isFrameStateSupported(
            Frame.MAXIMIZED_BOTH)){
            //Maximising the Frame fully
            SwingUtilities.invokeAndWait(() ->
                gui.jframe2.setExtendedState(Frame.MAXIMIZED_BOTH)
            );
            robot.waitForIdle(1000);

            //To check whether the bitwise mask for MAXIMIZED_BOTH state is set
            if (gui.jframe2.getExtendedState() != Frame.MAXIMIZED_BOTH)
                throw new RuntimeException("The bitwise mask Frame.MAXIMIZED_BOTH " +
                        "is not set when the frame is in MAXIMIZED_BOTH state");

            //To check whether the Frame is maximized fully
            if (gui.maxBoth == false)
                throw new RuntimeException("Frame is not maximized fully");
        }

        //Normalising the Frame....
        SwingUtilities.invokeAndWait(() ->
            gui.jframe2.setExtendedState(Frame.NORMAL)
        );
        robot.waitForIdle(1000);

        //To check whether the bitwise mask for NORMAL state is set
        if (gui.jframe2.getExtendedState() != Frame.NORMAL)
            throw new RuntimeException("The bitwise mask Frame.NORMAL is not " +
                    "set when the frame is in NORMAL state");

        //To check whether the Frame is normalised programmatically
        if (! gui.normal)
            throw new RuntimeException("Frame is not restored to normal");

        //to find the lenght and width of the component after zommed and back to normal
        newbuttonOrigin = gui.jbutton.getLocationOnScreen();
        newtextareaOrigin = gui.jtextarea.getLocationOnScreen();
        newbuttonCenter = gui.jbutton.getLocationOnScreen();
        newbuttonCenter.translate(gui.jbutton.getWidth()/2, gui.jbutton.getHeight()/2);
        newtextareaCenter = gui.jtextarea.getLocationOnScreen();
        newtextareaCenter.translate(gui.jtextarea.getWidth()/2, gui.jtextarea.getHeight()/2);

        if((buttonOrigin.x != newbuttonOrigin.x) & (buttonOrigin.y != newbuttonOrigin.y))
            throw new RuntimeException("The button is not positioned back " +
                    "to the original place  on the screen after iconified");

        if((textareaOrigin.x != newtextareaOrigin.x) & (textareaOrigin.y != newtextareaOrigin.y))
            throw new RuntimeException("The TextArea is not positioned back " +
                    "to the original place  on the screen after iconified");
    }
}
