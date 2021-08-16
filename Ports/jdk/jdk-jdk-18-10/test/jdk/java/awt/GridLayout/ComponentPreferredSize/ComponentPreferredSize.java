/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.event.InputEvent;

/*
 * @test
 * @key headful
 * @summary Have different components having different preferred sizes
 *          added to a grid layout having various values of row/columns.
 *          Check if the compnents are correctly laid out.
 *          The strategy followed is to calculate the component location
 *          depending on the preferred sizes and gaps and click the cornors
 *          of the components to check if events are triggered
 * @library ../../../../lib/testlibrary/
 * @run main ComponentPreferredSize
 * @run main ComponentPreferredSize -hg 20 -vg 20
 */

public class ComponentPreferredSize {

    private int width = 300;
    private int height = 200;
    private final int hGap, vGap;
    private final int rows = 3;
    private final int columns = 2;
    private final int componentCount = 6;

    private Button[] buttons;
    private Frame frame;

    private Robot robot;
    private GridLayout layout;

    private volatile boolean actionPerformed = false;

    public ComponentPreferredSize(int hGap, int vGap) throws Exception {
        this.hGap = hGap;
        this.vGap = vGap;
        robot = new Robot();
        EventQueue.invokeAndWait( () -> {
            frame = new Frame("Test frame");
            frame.setSize(width, height);
            layout = new GridLayout(rows, columns, hGap, vGap);
            frame.setLayout(layout);

            buttons = new Button[componentCount];
            for (int i = 0; i < componentCount; i++) {
                buttons[i] = new Button("Button" + i);
                buttons[i].setPreferredSize(new Dimension((int) Math.random() * 100,
                        (int) Math.random() * 100));
                frame.add(buttons[i]);
                buttons[i].addActionListener((event) -> {actionPerformed = true;});
            }

            frame.setVisible(true);
        });
    }

    public static void main(String[] args) throws Exception {
        int hGap = 0;
        int vGap = 0;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-hg":
                    hGap = Integer.parseInt(args[++i]);
                    break;
                case "-vg":
                    vGap = Integer.parseInt(args[++i]);
                    break;
            }
        }
        new ComponentPreferredSize(hGap, vGap).doTest();
    }

    private void resizeFrame() throws Exception {
        EventQueue.invokeAndWait(() -> {
            Insets insets = frame.getInsets();
            double dH = (height-insets.top-insets.bottom - vGap*(rows-1)) % rows;
            double dW = (width-insets.left-insets.right - hGap*(columns-1)) % columns;
            height -= dH;
            width -= dW;
            frame.setSize(width, height);
            frame.revalidate();
        });
        robot.waitForIdle();
    }

    public void testBoundaries(int topLeftX, int topLeftY, int bottomRightX, int bottomRightY) throws Exception {

        actionPerformed = false;
        robot.mouseMove(topLeftX, topLeftY);
        robot.delay(500);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(500);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(3000);

        if (!actionPerformed) {
            frame.dispose();
            throw new RuntimeException("Clicking on the left top of button did not trigger action event");
        }

        actionPerformed = false;
        robot.mouseMove(bottomRightX, bottomRightY);
        robot.delay(500);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(500);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(3000);

        if (!actionPerformed) {
            frame.dispose();
            throw new RuntimeException("Clicking on the bottom right of button did not trigger action event");
        }
    }

    private void doTest() throws Exception {
        robot.waitForIdle();
        resizeFrame();

        int availableWidth = width - frame.getInsets().left -
                frame.getInsets().right;
        int componentWidth = (availableWidth + hGap) / columns - hGap;
        int availableHeight = height - frame.getInsets().top -
                frame.getInsets().bottom;
        int componentHeight = (availableHeight + vGap) / rows - vGap;

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].getSize().width != componentWidth ||
                    buttons[i].getSize().height != componentHeight) {
                frame.dispose();
                throw new RuntimeException(
                        "FAIL: Button " + i + " not of proper size" +
                        "Expected: " + componentWidth + "*" + componentHeight +
                        "Actual: " + buttons[i].getSize().width + "*" + buttons[i].getSize().height);
            }
        }

        // Components are visible. They should trigger events.
        // Now you can check for the actual size shown.
        int currentRow = 1;
        int currentColumn = 0;
        for (int i = 0; i < buttons.length; i++) {
            currentColumn++;
            if (currentColumn > columns) {
                currentColumn = 1;
                currentRow++;
            }

            int topPosX = frame.getLocationOnScreen().x +
                    frame.getInsets().left +
                    (currentColumn - 1) * (componentWidth + hGap);
            int topPosY = frame.getLocationOnScreen().y +
                    frame.getInsets().top +
                    (currentRow - 1) * (componentHeight + vGap);

            int bottomPosX = topPosX + componentWidth - 1;
            int bottomPosY = topPosY + componentHeight - 1;
            testBoundaries(topPosX, topPosY, bottomPosX, bottomPosY);
        }

        frame.dispose();
    }
}
