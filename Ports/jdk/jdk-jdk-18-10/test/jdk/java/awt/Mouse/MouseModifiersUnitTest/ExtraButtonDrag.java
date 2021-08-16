/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6315717
  @summary verifies that drag events are coming for every button if the property is set to true
  @author Andrei Dmitriev : area=awt.mouse
  @run main ExtraButtonDrag
 */

//events from standard should also come

import java.awt.*;
import java.awt.event.*;

public class ExtraButtonDrag extends Frame {
    static String tk = Toolkit.getDefaultToolkit().getClass().getName();
    static Robot robot;
    static int [] buttonsPressed;
    static int [] buttonsReleased;
    static int [] buttonsClicked;
    volatile static boolean dragged = false;
    volatile static boolean moved = false;

    public ExtraButtonDrag(){
        super("ExtraButtonDrag");
    }

    public static void main(String []s){
        Frame frame = new ExtraButtonDrag();

        MouseAdapter ma = new MouseAdapter() {
                public void mouseDragged(MouseEvent e) {
                    System.out.println("Dragged "+e);// +" : "+ e.getButton() + " : " +e.getButtonState(e.getButton()));
                    dragged = true;
                }
                public void mouseMoved(MouseEvent e) {
                    System.out.println("Moved "+e);
                    moved = true;
                }
                public void mousePressed(MouseEvent e) {
                    System.out.println(">>> "+e);
                }
                public void mouseReleased(MouseEvent e) {
                    System.out.println(">>> "+e);
                }

            };

        frame.addMouseMotionListener(ma);
        frame.addMouseListener(ma);

        frame.setSize(300, 300);
        frame.setVisible(true);

        int [] buttonMask = new int [MouseInfo.getNumberOfButtons()]; //InputEvent.getButtonMasks();

        for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
            buttonMask[i] = InputEvent.getMaskForButton(i+1);
            //            System.out.println("TEST: "+tmp[i]);
        }

        try {
            robot = new Robot();
            robot.delay(1000);
            Point centerFrame = new Point(frame.getLocationOnScreen().x + frame.getWidth()/2, frame.getLocationOnScreen().y + frame.getHeight()/2);
            Point outboundsFrame = new Point(frame.getLocationOnScreen().x + frame.getWidth()*3/2, frame.getLocationOnScreen().y + frame.getHeight()/2);

            System.out.println("areExtraMouseButtonsEnabled() == " + Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() );

            for (int i = 0; i < MouseInfo.getNumberOfButtons(); i++){
                System.out.println("button to drag = " +(i+1) + " : value passed to robot = " +buttonMask[i]);

                try {
                    dragMouse(buttonMask[i], centerFrame.x, centerFrame.y, outboundsFrame.x, outboundsFrame.y);
                } catch (IllegalArgumentException e){
                    throw new RuntimeException("Test failed. Exception occured.", e);
                }

                robot.delay(500);
                //this is a choice-case for X protocol issue: native events from extra buttons doesn't contain
                // the correct state so it's unable to decide if there is a drag or move. By default we send MOVED event.
                //XToolkit: extra buttons should report MOVED events only
                //WToolkit: extra buttons should report DRAGGED events only
                if (i > 2){ //extra buttons only
                    if (tk.equals("sun.awt.X11.XToolkit") || tk.equals("sun.awt.motif.MToolkit")) {
                        if (!moved || dragged) {
                            throw new RuntimeException("Test failed."+ tk +" Button = " +(i+1) + " moved = "+moved +" : dragged = " +dragged);
                        }
                    } else { //WToolkit
                        if (moved || !dragged) {
                            throw new RuntimeException("Test failed."+ tk +" Button = " +(i+1) + " moved = "+moved +" : dragged = " +dragged);
                        }
                    }
                } else {
                    if (moved || !dragged){
                        throw new RuntimeException("Test failed. Button = " +(i+1) + " not dragged.");
                    }
                }
            }
        } catch (Exception e){
            throw new RuntimeException("", e);
        }
    }

    public static void dragMouse(int button, int x0, int y0, int x1, int y1){
        int curX = x0;
        int curY = y0;
        int dx = x0 < x1 ? 1 : -1;
        int dy = y0 < y1 ? 1 : -1;
        robot.mouseMove(x0, y0);

        robot.delay(200);
        dragged = false;
        moved = false;

        robot.mousePress(button);

        while (curX != x1){
            curX += dx;
            robot.mouseMove(curX, curY);
            robot.delay(5);
        }
        while (curY != y1 ){
            curY += dy;
            robot.mouseMove(curX, curY);
            robot.delay(5);
        }
        robot.mouseRelease(button);
    }

}

