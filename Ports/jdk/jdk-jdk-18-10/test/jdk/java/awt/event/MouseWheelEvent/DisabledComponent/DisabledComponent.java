/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @bug 6847958
  @summary MouseWheel event is getting triggered for the disabled Textarea in jdk7 b60 pit build.
  @author Dmitry Cherepanov: area=awt.event
  @library ../../../regtesthelpers
  @build Util
  @run main DisabledComponent
*/

/**
 * DisabledComponent.java
 *
 * summary: Tests that wheel events aren't coming on disabled component
 */

import java.awt.*;
import java.awt.event.*;

import test.java.awt.regtesthelpers.Util;

public class DisabledComponent
{

    private static volatile boolean passed = true;

    public static void main(String []s) throws Exception
    {
        Frame frame = new Frame();
        frame.setBounds(100,100,400,400);
        frame.setLayout(new FlowLayout());

        TextArea textArea = new TextArea("TextArea", 6, 15);
        frame.add(textArea);

        List list = new List(3);
        list.add("1");
        list.add("2");
        list.add("3");
        frame.add(list);

        MouseWheelListener listener = new MouseWheelListener(){
            @Override
                public void mouseWheelMoved(MouseWheelEvent mwe){
                    System.err.println(mwe);
                    passed = false;
                }
            };

        Robot robot = new Robot();


        list.addMouseWheelListener(listener);
        textArea.addMouseWheelListener(listener);

        frame.setVisible(true);
        robot.waitForIdle();

        // point and wheel on the list
        Util.pointOnComp(list, robot);
        robot.waitForIdle();

        robot.mouseWheel(2);
        robot.waitForIdle();

        // disable the text area
        System.err.println(" disable text area ");
        textArea.setEnabled(false);
        passed = true;

        // point and wheel on the text area
        Util.pointOnComp(textArea, robot);
        robot.waitForIdle();

        robot.mouseWheel(2);
        robot.waitForIdle();

        if (!passed) {
            throw new RuntimeException(" wrong wheel events ");
        }
    }
}
