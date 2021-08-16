/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @summary Test of mouse move messages to lightweight components
 * @library ../../regtesthelpers
 * @build Util
 * @compile LightweightEventTest.java
 * @run main LightweightEventTest
 */
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.AWTException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;
import test.java.awt.regtesthelpers.Util;


/*
There are 3 steps to this test :
1. Two frames are created one with heavy weight component and
   another with light weight component. Each frame has a centrally placed
   button
2. Mouse is dragged along diagonals of each window using Robot object
3. Events are noted for mouse in and out of frames & buttons and asserted
*/

public class LightweightEventTest {

    private static EventBug HeavyComponent;
    private static EventBug LightComponent;
    private static Robot testRobot;

    public static void main(String[] args) throws Throwable {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                constructTestUI();
            }
        });

        try {
            testRobot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException("Could not initiate a drag operation");
        }

        testRobot.waitForIdle();

        // Method performing auto test operation
        boolean result = test();

        disposeTestUI();

        if (result == false) {
            throw new RuntimeException("Test FAILED!");
        }
    }

    private static boolean test() {
        // Test events for HeavyComponent
        Point loc = HeavyComponent.getLocationOnScreen();
        Dimension size = HeavyComponent.getSize();

        Util.mouseMove(testRobot,
                new Point((int) loc.x + 4, (int) loc.y + 4),
                new Point((int) loc.x + size.width, (int) loc.y + size.height));

        testRobot.waitForIdle();

        boolean HeavyComponentAssert = HeavyComponent.assertEvents(2, 1);

        // Test events for LightComponent
        loc = LightComponent.getLocationOnScreen();
        size = LightComponent.getSize();

        Util.mouseMove(testRobot,
                new Point((int) loc.x + 4, (int) loc.y + 4),
                new Point((int) loc.x + size.width, (int) loc.y + size.height));

        testRobot.waitForIdle();

        boolean LightComponentAssert = LightComponent.assertEvents(2, 1);

        return (HeavyComponentAssert && LightComponentAssert);
    }

    private static void constructTestUI() {
        // here, create the items that will be tested for correct behavior
        HeavyComponent = new EventBug();
        Button b = (Button) HeavyComponent.add("Center", new Button("Heavy"));

        LightComponent = new EventBug();
        BorderedLabel b1 = (BorderedLabel) LightComponent.add("Center",
                new BorderedLabel("Lite"));

        HeavyComponent.addListeners(b);
        LightComponent.addListeners(b1);

        LightComponent.setLocation(200, 0);
        HeavyComponent.setVisible(true);
        LightComponent.setVisible(true);
    }

    private static void disposeTestUI() {
        HeavyComponent.setVisible(false);
        LightComponent.setVisible(false);

        HeavyComponent.dispose();
        LightComponent.dispose();
    }
}

/*
 * Lightweight component
 */
class BorderedLabel extends Component {

    boolean superIsButton = false;
    String labelString;

    BorderedLabel(String labelString) {
        this.labelString = labelString;

        Component thisComponent = this;
        superIsButton = (thisComponent instanceof Button);
        if (superIsButton) {
            ((Button) thisComponent).setLabel(labelString);
        }
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension minSize = new Dimension();

        if (superIsButton) {
            minSize = super.getMinimumSize();
        } else {

            Graphics g = getGraphics();
            FontMetrics metrics = g.getFontMetrics();

            minSize.width = metrics.stringWidth(labelString) + 14;
            minSize.height = metrics.getMaxAscent()
                    + metrics.getMaxDescent() + 9;

            g.dispose();
            g = null;
        }
        return minSize;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension prefSize;
        if (superIsButton) {
            prefSize = super.getPreferredSize();
        } else {
            prefSize = getMinimumSize();
        }
        return prefSize;
    }

    @Override
    public void paint(Graphics g) {

        super.paint(g);
        Rectangle bounds = getBounds();
        if (superIsButton) {
            return;
        }
        Dimension size = getSize();
        Color oldColor = g.getColor();

        // draw border
        g.setColor(getBackground());
        g.fill3DRect(0, 0, size.width, size.height, false);
        g.fill3DRect(3, 3, size.width - 6, size.height - 6, true);

        // draw text
        FontMetrics metrics = g.getFontMetrics();
        int centerX = size.width / 2;
        int centerY = size.height / 2;
        int textX = centerX - (metrics.stringWidth(labelString) / 2);
        int textY = centerY
                + ((metrics.getMaxAscent() + metrics.getMaxDescent()) / 2);
        g.setColor(getForeground());
        g.drawString(labelString, textX, textY);

        g.setColor(oldColor);
    }
} // class BorderedLabel

class EventBug extends Container {

    Frame testFrame;
    int frameEnters = 0;
    int frameExits = 0;
    int buttonEnters = 0;
    int buttonExits = 0;

    public EventBug() {
        super();
        testFrame = new Frame();
        testFrame.setLayout(new BorderLayout());
        this.setLayout(new BorderLayout());
        testFrame.add("Center", this);
        testFrame.pack();
        testFrame.setVisible(true);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(100, 100);
    }

    @Override
    public Insets getInsets() {
        return new Insets(20, 20, 20, 20);
    }

    public boolean assertEvents(int expectedFrameEnterEvents,
            int expectedButtonEnterEvents) {
        return (frameEnters == expectedFrameEnterEvents)
                && (buttonEnters == expectedButtonEnterEvents);
    }

    // Forward to the Window
    @Override
    public void setLocation(int x, int y) {
        testFrame.setLocation(x, y);
    }

    @Override
    public void setVisible(boolean b) {
        testFrame.setVisible(b);
    }

    public void dispose() {
        testFrame.dispose();
    }

    // Add listeners to Frame and button
    public void addListeners(Component b) {
        b.setName("Button");
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonEnters++;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                buttonExits++;
            }

        });
        testFrame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                frameEnters++;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                frameExits++;
            }
        });
    }
} // class EventBug
