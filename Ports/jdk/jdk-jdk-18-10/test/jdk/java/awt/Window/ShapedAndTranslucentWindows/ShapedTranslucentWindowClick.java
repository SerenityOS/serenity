/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.BitSet;

/*
 * @test
 * @key headful
 * @summary Check if a translucent shaped window trigger events correctly.
 *
 * Test Description: Check if TRANSLUCENT and PERPIXEL_TRANSPARENT traslucency
 *      types are supported on the current platform. Proceed if both are
 *      supported. Create a window with some components in it and a shape
 *      applied. Apply the shape such that some components are partially
 *      clipped. Set an opacity value less than 1. Check if the components
 *      behave correctly on mouse and key events. Do this test for awt Window.
 * Expected Result: Mouse and key events must work correctly. Only that portion
 *      of a component within the shape should trigger events. Key events
 *      should be tested for focus events and TextField/TextArea.
 *
 * @author mrkam
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @library /lib/client
 * @build Common ExtendedRobot
 * @run main ShapedTranslucentWindowClick
 */

public class ShapedTranslucentWindowClick extends Common {

    Component south, center, north;

    volatile BitSet backgroundFlags = new BitSet(11);
    volatile BitSet southFlags = new BitSet(11);
    volatile BitSet centerFlags = new BitSet(11);
    volatile BitSet northFlags = new BitSet(11);
    static BitSet reference = BitSet.valueOf(new long[] {2047}); // 111 1111 1111

    public static void main(String[] ignored) throws Exception{
        if (checkTranslucencyMode(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT) &&
                checkTranslucencyMode(GraphicsDevice.WindowTranslucency.TRANSLUCENT))
            new ShapedTranslucentWindowClick(Window.class).doTest();
    }

    public ShapedTranslucentWindowClick(Class windowClass) throws Exception {
        super(windowClass);
        addListeners(south, southFlags);
        addListeners(center, centerFlags);
        addListeners(north, northFlags);
        addListeners(background, backgroundFlags);
    }

    @Override
    public void initGUI() {

        window = new Window(background);
        south = new Button("South Button");
        center = new TextArea("Center Text Area");
        north = new TextField("North Text Field");

        window.setLocation(250, 250);
        window.setPreferredSize(new Dimension(200, 200));
        window.setOpacity(0.7f);
        applyShape();
        window.setLayout(new BorderLayout());

        window.add(south, BorderLayout.SOUTH);
        window.add(center, BorderLayout.CENTER);
        window.add(north, BorderLayout.NORTH);

        window.pack();
        window.setVisible(true);
        window.toFront();

        System.out.println("Checking " + window.getClass().getName() + "...");
    }

    @Override
    public void doTest() throws Exception {

        robot.waitForIdle();
        robot.mouseMove(background.getLocationOnScreen().x+50, background.getLocationOnScreen().y+50);
        robot.waitForIdle();
        robot.click();

        Point wls = window.getLocationOnScreen();
        Point ls;
        int y;

        robot.waitForIdle();

        ls = north.getLocationOnScreen();
        checkClickAndType(ls.x + north.getWidth() / 3, ls.y + north.getHeight() / 2, northFlags);

        ls = center.getLocationOnScreen();
        checkClickAndType(ls.x + center.getWidth() / 4, ls.y + center.getHeight() / 4, centerFlags);

        ls = center.getLocationOnScreen();
        checkClickAndType(ls.x + center.getWidth() * 3 / 4, ls.y + center.getHeight() * 3 / 4, centerFlags);

        ls = south.getLocationOnScreen();
        checkClickAndType(ls.x + south.getWidth() * 2 / 3, ls.y + south.getHeight() / 2, southFlags);

        ls = north.getLocationOnScreen();
        y = ls.y + north.getHeight() / 2;
        checkClickAndType(wls.x + 200 - (y - wls.y), y, backgroundFlags);

        EventQueue.invokeAndWait(window::toFront);
        robot.waitForIdle();

        ls = center.getLocationOnScreen();
        y = ls.y + center.getHeight() / 2;
        checkClickAndType(wls.x + 200 - (y - wls.y), y, backgroundFlags);

        EventQueue.invokeAndWait(window::toFront);
        robot.waitForIdle();

        ls = south.getLocationOnScreen();
        y = ls.y + south.getHeight() / 2;
        checkClickAndType(wls.x + 200 - (y - wls.y), y, backgroundFlags);

        EventQueue.invokeAndWait(window::dispose);
        EventQueue.invokeAndWait(background::dispose);

        robot.waitForIdle();
    }

    @Override
    public void applyShape() {
        Area shape = new Area(new Rectangle2D.Float(0, 0, 200, 200));
        GeneralPath gp;
        gp = new GeneralPath();
        gp.moveTo(190, 0);
        gp.lineTo(200, 0);
        gp.lineTo(200, 10);
        gp.lineTo(10, 200);
        gp.lineTo(0, 200);
        gp.lineTo(0, 190);
        gp.closePath();
        shape.subtract(new Area(gp));

        window.setShape(shape);
    }

    void checkClickAndType(int x, int y, BitSet bits){
        bits.clear(0, 10);

        robot.mouseMove(x, y);
        robot.click();
        robot.dragAndDrop(MouseInfo.getPointerInfo().getLocation(), new Point(x+5, y));
        robot.mouseWheel(1);
        robot.waitForIdle();

        robot.type('a');

        robot.mouseMove(350, 50);
        robot.waitForIdle();

        //robot.delay(20*1000);
        if (!bits.equals(reference)){
            for( int i = 0; i < 11; i++)
                System.err.print(( bits.get(i) ? 1 : 0 ) + ", ");
            System.err.println();
            throw new RuntimeException("Bit mask is not fully set: "+bits);
        }
    }

    static void addListeners(Component component, BitSet bits) {
        component.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) { bits.set(0);}
            public void mousePressed(MouseEvent e) { bits.set(1); }
            public void mouseReleased(MouseEvent e) { bits.set(2); }
            public void mouseEntered(MouseEvent e) { bits.set(3); }
            public void mouseExited(MouseEvent e) { bits.set(4); }
        });
        component.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) { bits.set(5); }
            public void mouseMoved(MouseEvent e) { bits.set(6); }
        });
        component.addMouseWheelListener((e) -> bits.set(7));
        component.addKeyListener(new KeyListener() {
            public void keyTyped(KeyEvent e) { bits.set(8); }
            public void keyPressed(KeyEvent e) { bits.set(9); }
            public void keyReleased(KeyEvent e) { bits.set(10); }
        });
    };
}
