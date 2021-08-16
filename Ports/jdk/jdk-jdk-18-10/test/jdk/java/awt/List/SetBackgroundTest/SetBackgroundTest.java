/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6246467
  @key headful
  @requires os.family == "linux"
  @summary List does not honor user specified background, foreground colors on XToolkit
  @author Dmitry Cherepanov  area=awt.list
  @library /lib/client
  @build ExtendedRobot
  @run main SetBackgroundTest
*/

/**
 * SetBackgroundTest.java
 *
 * summary:
 */

import java.awt.*;
import java.awt.event.*;

public class SetBackgroundTest
{

    private static boolean isXAWT = (Toolkit.getDefaultToolkit().getClass().getName().equals("sun.awt.X11.XToolkit"));
    private static ExtendedRobot robot = null;
    private static Frame frame = null;

    private static final Color color = Color.red;
    private static Color roughColor = null;

    private static void initRoughColor(){

        Canvas canvas = new Canvas();
        canvas.setBackground(color);
        frame.add(canvas, BorderLayout.CENTER);
        frame.validate();
        robot.waitForIdle(500);

        Point loc = canvas.getLocationOnScreen();
        Color robotColor = robot.getPixelColor(loc.x + canvas.getWidth()/2, loc.y + canvas.getHeight()/2);
        roughColor = robotColor;

        System.out.println(" --- init rough color ... ");
        System.out.println("     color = "+color);
        System.out.println("     roughColor = "+roughColor);

        frame.remove(canvas);
        robot.waitForIdle(500);
    }


    private static void test() {
        if (!isXAWT){
            System.out.println(" this is XAWT-only test. ");
            return;
        }

        frame = new Frame();
        frame.setBounds(400,400,200,200);
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);


        try{
            robot = new ExtendedRobot();
        }catch(AWTException e){
            throw new RuntimeException(e.getMessage());
        }

        robot.waitForIdle(500);

        initRoughColor();
        Component[] components = new Component[] {
            new Button(), new Checkbox(), new Label(), new List(3, false),
            new TextArea(), new TextField(), new Choice()
        };

        for (Component component : components) {
            testComponent(new Panel(), component, color);
        }

        robot.waitForIdle(1500);
        frame.dispose();
    }

    private static void testComponent(Container container, Component component, Color color){

        component.setBackground(color);

        container.setLayout(new BorderLayout());
        container.add(component, BorderLayout.CENTER);
        frame.add(container, BorderLayout.CENTER);
        frame.add("Center", container);
        frame.validate();
        robot.waitForIdle(500);

        Point loc = component.getLocationOnScreen();
        Color robotColor = robot.getPixelColor(loc.x + component.getWidth()/2, loc.y + component.getHeight()/2);

        System.out.println(" --- test ... ");
        System.out.println("     container = "+container);
        System.out.println("     component = "+component);
        System.out.println("     color = "+color);
        System.out.println("     roughColor = "+roughColor);
        System.out.println("     robotColor = "+robotColor);

        if(robotColor.getRGB() != roughColor.getRGB()){
            throw new RuntimeException(" the case failed. ");
        } else {
            System.out.println(" the case passed. ");
        }

        container.remove(component);
        frame.remove(container);
        robot.waitForIdle(500);
    }
    public static void main( String args[] ) throws Exception
    {
        test();
    }
}

