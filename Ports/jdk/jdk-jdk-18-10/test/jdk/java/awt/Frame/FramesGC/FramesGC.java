/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Vector;

/*
 * @test
 * @key headful
 * @summary Verify that disposed frames are collected with GC
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build ExtendedRobot
 * @run main/othervm -Xmx20m FramesGC
 */


public class FramesGC {

    ExtendedRobot robot;
    ArrayList<PhantomReference<Frame>> refs = new ArrayList<PhantomReference<Frame>>();
    ReferenceQueue<Frame> que = new ReferenceQueue<Frame>();

    public static void main(String []args) throws Exception {
        new FramesGC().doTest();
    }

    FramesGC() throws Exception{
        robot = new ExtendedRobot();
    }

    void doTest() throws Exception {
        for( int i = 1; i <= 3; i++) {
            final int j = i;
            EventQueue.invokeAndWait(() -> {
                createFrame(j);
            });
        }
        robot.waitForIdle();

        for (Frame f : Frame.getFrames())
            f.dispose();

        robot.waitForIdle();

        Vector garbage = new Vector();
        while (true) {
            try {
                garbage.add(new byte[1000]);
            } catch (OutOfMemoryError er) {
                break;
            }
        }
        garbage = null;

        int count = 1;
        for(; count <= 3; count++)
            if(que.remove(5000) == null)
                break;

        System.out.println("Total no of instances eligible for GC = " + count);
        if(count < 3)
            throw new RuntimeException("Count = "+count+". Test failed!");

    }

    void createFrame(int i){
        Frame frame = new Frame("Frame " + i);

        Button button=new Button("Press Me");
        TextArea textArea=new TextArea(5,5);
        TextField textField=new TextField(10);
        Choice choice=new Choice();
        choice.add("One");
        choice.add("Two");
        choice.add("Three");
        choice.add("Four");
        choice.add("Five");
        List list = new List();
        list.add("One");
        list.add("Two");
        list.add("Three");
        list.add("Four");
        list.add("Five");
        Checkbox checkBox= new Checkbox("Hai");
        Scrollbar scrollBar=new Scrollbar(Scrollbar.VERTICAL,0,1,0,200);
        CheckboxGroup checkboxGroup=new CheckboxGroup();
        Checkbox radioButton=new Checkbox("Hello" ,true, checkboxGroup);
        Canvas canvas=new Canvas();
        canvas.setSize(100, 100);
        canvas.setBackground(java.awt.Color.red);
        Label label=new Label("I am label.!");
        Cursor customCursor=null;

        frame.setLayout(new java.awt.FlowLayout());

        button.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
        label.setCursor(new Cursor(Cursor.TEXT_CURSOR));
        choice.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        list.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkBox.setCursor(new Cursor(Cursor.MOVE_CURSOR));
        radioButton.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
        scrollBar.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
        canvas.setCursor(new Cursor(Cursor.W_RESIZE_CURSOR));
        textField.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

        /* create a custom cursor */
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension d = toolkit.getBestCursorSize(32,32);
        int color = toolkit.getMaximumCursorColors();

        if(!d.equals(new Dimension(0,0)) && color != 0 )
            customCursor = toolkit.createCustomCursor(new BufferedImage( 16, 16, BufferedImage.TYPE_INT_RGB ), new Point(10, 10), "custom cursor.");
        else
            System.err.println("Platform doesn't support to create a custom cursor.");

        textArea.setCursor(customCursor);
        frame.add(label);
        frame.add(button);
        frame.add(choice);
        frame.add(list);
        frame.add(checkBox);
        frame.add(radioButton);
        frame.add(scrollBar);
        frame.add(canvas);
        frame.add(textArea);
        frame.add(textField);
        frame.add(button);

        frame.setLocation(20, 140 * i);
        frame.pack();
        frame.setVisible(true);
        refs.add(new PhantomReference<Frame>(frame, que));
    }

}
