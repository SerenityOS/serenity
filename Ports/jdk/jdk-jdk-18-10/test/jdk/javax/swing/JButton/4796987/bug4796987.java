/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4796987
 * @key headful
 * @requires (os.family == "windows")
 * @summary XP Only: JButton.setBorderPainted() does not work with XP L&F
 * @author Alexander Scherbatiy
 * @library ../../regtesthelpers
 * @library /test/lib
 * @modules java.desktop/com.sun.java.swing.plaf.windows
 *          java.desktop/sun.awt
 * @build jdk.test.lib.OSVersion jdk.test.lib.Platform
 * @build Util
 * @run main bug4796987
 */

import jdk.test.lib.Platform;
import jdk.test.lib.OSVersion;
import java.awt.*;
import javax.swing.*;
import com.sun.java.swing.plaf.windows.WindowsLookAndFeel;

public class bug4796987 {

    private static JButton button1;
    private static JButton button2;
    private static JFrame frame;

    public static void main(String[] args) throws Exception {
        try {
            if (Platform.isWindows()
                && OSVersion.current().equals(OSVersion.WINDOWS_XP)) {
                UIManager.setLookAndFeel(new WindowsLookAndFeel());
                testButtonBorder();
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static void testButtonBorder() throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(50);

        SwingUtilities.invokeAndWait(new Runnable() {

            public void run() {
                createAndShowGUI();
            }
        });

        robot.waitForIdle();
        Thread.sleep(500);

        Point p1 = Util.getCenterPoint(button1);
        Point p2 = Util.getCenterPoint(button2);

        Color color = robot.getPixelColor(p1.x, p2.x);
        for (int dx = p1.x; dx < p2.x - p1.x; dx++) {
            robot.mouseMove(p1.x + dx, p1.y);
            if (!color.equals(robot.getPixelColor(p1.x + dx, p1.y))) {
                throw new RuntimeException("Button has border and background!");
            }
        }
    }

    private static JButton getButton() {
        JButton button = new JButton();
        button.setBorderPainted(false);
        button.setFocusable(false);
        return button;
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(200, 200);

        JButton button = new JButton();
        button.setBorder(null);

        JPanel panel = new JPanel(new BorderLayout(50, 50));
        panel.add(getButton(), BorderLayout.CENTER);
        panel.add(button1 = getButton(), BorderLayout.WEST);
        panel.add(button2 = getButton(), BorderLayout.EAST);
        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }
}
