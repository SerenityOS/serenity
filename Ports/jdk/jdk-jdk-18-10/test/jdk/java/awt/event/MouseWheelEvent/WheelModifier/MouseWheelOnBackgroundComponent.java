/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
  @bug 8231991
  @summary Mouse wheel change focus on awt/swing windows
  @run main MouseWheelOnBackgroundComponent
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowEvent;

/**
 * MouseWheelOnBackgroundComponent
 *
 * Tests that wheel events don't change focus on background components
 */
public class MouseWheelOnBackgroundComponent {

    private static final String FG = "Foreground";
    private static final String BG = "Background";

    private static final String SCROLL_PANE = "scroller_";
    private static final String TEXT_PANE = "text_";

    private static JFrame background;
    private static JFrame foreground;

    private static final String LOREM_IPSUM = "Sed ut perspiciatis unde omnis iste natus " +
            "error sit voluptatem accusantium doloremque laudantium, totam " +
            "rem aperiam, eaque ipsa quae ab illo inventore veritatis et " +
            "quasi architecto beatae vitae dicta sunt explicabo. Nemo enim " +
            "ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, " +
            "sed quia consequuntur magni dolores eos qui ratione voluptatem sequi " +
            "nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit " +
            "amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora " +
            "incidunt ut labore et dolore magnam aliquam quaerat voluptatem. " +
            "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis " +
            "suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis " +
            "autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil " +
            "molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla " +
            "pariatur?";

    private static class FocusReporter implements FocusListener {

        private JComponent pane;

        Component lastFocusedComponent;

        public FocusReporter() {
        }

        @Override
        public void focusGained(FocusEvent e) {
            lastFocusedComponent = e.getComponent();
        }

        @Override
        public void focusLost(FocusEvent e) {
            lastFocusedComponent = null;
        }
    }

    private static FocusReporter reporter;

    public static void main(String[] args) throws Exception  {

        boolean passed = false;

        SwingUtilities.invokeAndWait(() -> {
            reporter = new FocusReporter();

            foreground = createFrame(FG, 100, 0, reporter);
            background = createFrame(BG, 0, 100, reporter);

            background.pack();
            background.setVisible(true);

            foreground.pack();
            foreground.setVisible(true);
        });

        SwingUtilities.invokeAndWait(() -> {
            foreground.toFront();
        });

        Robot robot = new Robot();
        robot.waitForIdle();

        robot.mouseMove(50, 300);
        robot.waitForIdle();

        robot.mouseWheel(-100);
        robot.waitForIdle();

        String shouldBeFocusedComponentName = TEXT_PANE + FG;
        Component actual = reporter.lastFocusedComponent;
        if (reporter.lastFocusedComponent != null &&
            shouldBeFocusedComponentName.equals(actual.getName()))
        {
            passed = true;
        }

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(() -> {
            foreground.dispatchEvent(new WindowEvent(foreground, WindowEvent.WINDOW_CLOSING));
            background.dispatchEvent(new WindowEvent(background, WindowEvent.WINDOW_CLOSING));
        });

        robot.waitForIdle();

        if (!passed) {
            throw new RuntimeException("Wrong component has focus: " + actual);
        }
    }

    private static JFrame createFrame(String name, int x, int y, FocusReporter reporter) {
        JFrame frame = new JFrame(name);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setBounds(x, y, 600, 600);
        frame.setPreferredSize(new Dimension(600, 600));

        JTextArea text = new JTextArea();
        text.setText(LOREM_IPSUM);
        for (int i = 0; i < 10; i++) {
            text.append(LOREM_IPSUM);
        }

        text.setLineWrap(true);
        text.setName(TEXT_PANE + name);
        text.addFocusListener(reporter);

        JScrollPane scroller = new JScrollPane(text);
        scroller.setName(SCROLL_PANE + name);
        scroller.setWheelScrollingEnabled(true);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroller.addFocusListener(reporter);

        frame.add(scroller);

        return frame;
    }
}
