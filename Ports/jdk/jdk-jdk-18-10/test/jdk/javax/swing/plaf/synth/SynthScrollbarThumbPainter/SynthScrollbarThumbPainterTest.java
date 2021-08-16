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

/*
 * @test
 * @key headful
 * @bug 7172750
 * @summary Test to check Synth ScrollBar:ScrollBarThumb[].backgroundPainter is invoked
 * @run main SynthScrollbarThumbPainterTest
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.Painter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class SynthScrollbarThumbPainterTest {

    private static Robot testRobot;
    private static Point pos = new Point();
    private static MyFrame testFrame;

    public static void main(String[] args) throws Exception {
        // Create Robot
        testRobot = new Robot();

        // Create test UI
        String lookAndFeelString = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    constructTestUI(lookAndFeelString);
                } catch (Exception ex) {
                    throw new RuntimeException("Exception creating test UI");
                }
            }
        });

        testRobot.waitForIdle();
        testRobot.delay(200);

        // Run test method
        testScrollBarThumbPainter();

        // Dispose test UI
        disposeTestUI();
    }

    private static void disposeTestUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            testFrame.dispose();
        });
    }


    private static void constructTestUI(String lookAndFeelString) throws Exception {
        // Set look and feel
        UIManager.setLookAndFeel(lookAndFeelString);

        // Set ScrollBarThumb background painters
        UIManager.getLookAndFeelDefaults().put("ScrollBar:ScrollBarThumb[Enabled].backgroundPainter", new FillPainter(Color.RED));
        UIManager.getLookAndFeelDefaults().put("ScrollBar:ScrollBarThumb[MouseOver].backgroundPainter", new FillPainter(Color.GREEN));
        UIManager.getLookAndFeelDefaults().put("ScrollBar:ScrollBarThumb[Pressed].backgroundPainter", new FillPainter(Color.BLUE));

        // Create UI
        testFrame = new MyFrame();
    }

   private static void testScrollBarThumbPainter() throws Exception {
        Point p = testFrame.getLocation();
        pos.setLocation(p.x + 185, p.y + 80); // offset where scrollbar exists

        testRobot.delay(200);

        // Get the scrollbar color
        Color ScrollbarColor = testFrame.getPixelColor(pos.x - p.x, pos.y - p.y);

        // Assert ScrollbarThumb 'Enable' state color
        if (!ScrollbarColor.equals(Color.RED)) {
            disposeTestUI();
            throw new RuntimeException("ScrollbarThumb 'Enable' state color does not match expected color");
        }

        // Move the mouse over scrollbar
        testRobot.mouseMove(pos.x, pos.y);
        testRobot.delay(200);

        ScrollbarColor = testFrame.getPixelColor(pos.x - p.x, pos.y - p.y);

        //Assert ScrollbarThumb 'MouseOver' state color
        if (!ScrollbarColor.equals(Color.GREEN)) {
            disposeTestUI();
            throw new RuntimeException("ScrollbarThumb 'MouseOver' state color does not match expected color");
        }

        // Mouse Press on scrollbar
        testRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        testRobot.delay(200);

        ScrollbarColor = testFrame.getPixelColor(pos.x - p.x, pos.y - p.y);

        testRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        testRobot.delay(200);

        //Assert ScrollbarThumb 'Pressed' state color
        if (!ScrollbarColor.equals(Color.BLUE)) {
            disposeTestUI();
            throw new RuntimeException("ScrollbarThumb 'Pressed' state color does not match expected color");
        }
    }

}


class FillPainter implements Painter<JComponent> {

    private final Color color;

    FillPainter(Color c) {
        color = c;
    }

    @Override
    public void paint(Graphics2D g, JComponent object, int width, int height) {
        g.setColor(color);
        g.fillRect(0, 0, width - 1, height - 1);
    }
}

class MyFrame extends JFrame {

    private BufferedImage bi;
    private final String content = "A\nB\nC\nD\nE\nF\nG\nH\nI\nJ\nK\nL\nM\nN\nO";

    public MyFrame() {
        JEditorPane editpane = new JEditorPane();
        editpane.setEditable(false);
        editpane.setText(content);
        editpane.setCaretPosition(0);

        JScrollPane scrollpane = new JScrollPane(editpane);

        add(scrollpane);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setSize(new Dimension(200, 200));
        bi = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        setResizable(false);
        setVisible(true);
    }


    public Color getPixelColor(int x, int y) {

        paintFrameToBufferedImage(this);

        int pixel = bi.getRGB(x, y);

        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;

        Color pixelColor = new Color(red, green, blue, alpha);
        return pixelColor;
    }

    private void paintFrameToBufferedImage(Component component) {
       component.paint(bi.getGraphics());
    }
} //MyFrame

