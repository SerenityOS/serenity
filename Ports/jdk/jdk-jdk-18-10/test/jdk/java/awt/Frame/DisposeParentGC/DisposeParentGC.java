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
 * @summary Display a dialog with a parent, the dialog contains all awt components
 *          added to it & each components are setted with different cursors types.
 *          Dispose the parent & collect GC. Garbage collection should happen
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build ExtendedRobot
 * @run main/othervm -Xmx20m DisposeParentGC
 */

public class DisposeParentGC {
    Frame parentFrame;
    ExtendedRobot robot;

    ArrayList<PhantomReference<Dialog>> refs = new ArrayList<PhantomReference<Dialog>>();
    ReferenceQueue<Dialog> que = new ReferenceQueue<>();

    public static void main(String []args) throws Exception {
        new DisposeParentGC().doTest();
    }

    DisposeParentGC() throws Exception {
        robot = new ExtendedRobot();
        EventQueue.invokeAndWait(this::initGui);
    }

    void initGui(){
        parentFrame = new Frame("Parent Frame");
        parentFrame.setLayout(new FlowLayout());

        for (int i = 1; i <= 3; i++)
            createDialog(i);

        parentFrame.setLocation(250, 20);
        parentFrame.pack();
        parentFrame.setVisible(true);
    }

    public void doTest() throws Exception{
        robot.waitForIdle();

        parentFrame.dispose();
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
        for (; count <= 3; count++)
            if(que.remove(5000) == null)
                break;

        if (count < 3)
            throw new RuntimeException("Count = "+count+". GC didn't collect the objects after the parent is disposed!");
    }

    public void createDialog(int number) {
        Dialog child = new Dialog(parentFrame);
        child.setTitle("Dialog " + number);
        child.setLayout(new FlowLayout());
        child.setLocation(20, 140 * number);

        Button button = new Button("Press Me") ;
        TextArea textArea = new TextArea(5,5);
        TextField textField = new TextField(10);
        Choice choice = new Choice();
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
        Checkbox checkBox = new Checkbox("Hai");
        Scrollbar scrollBar = new Scrollbar(Scrollbar.VERTICAL,0,1,0,200);
        CheckboxGroup checkboxGroup = new CheckboxGroup();
        Checkbox radioButton = new Checkbox("Hello" ,true, checkboxGroup);
        Canvas canvas = new Canvas();
        Label label = new Label("I am label!");
        Cursor customCursor = null;

        child.setLayout(new FlowLayout());
        canvas.setSize(100,100);
        canvas.setBackground(Color.red);

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
        child.add(label);
        child.add(button);
        child.add(choice);
        child.add(list);
        child.add(checkBox);
        child.add(radioButton);
        child.add(scrollBar);
        child.add(canvas);
        child.add(textArea);
        child.add(textField);
        child.add(button);
        child.revalidate();

        child.pack();
        child.setVisible(true);
        refs.add(new PhantomReference<Dialog>(child, que));
    }
}
