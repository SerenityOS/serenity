/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4697612 6244705 7190581
 * @run main bug4697612
 */
import java.awt.Rectangle;
import java.awt.Dimension;
import java.io.InputStream;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.BadLocationException;

public class bug4697612 {

    static final int FRAME_WIDTH = 300;
    static final int FRAME_HEIGHT = 300;
    static final int FONT_HEIGHT = 16;
    private static volatile int frameHeight;
    private static volatile int fontHeight;
    private static JFrame frame;
    private static JTextArea text;
    private static JScrollPane scroller;
    private static Robot robot;

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                 UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws Throwable {
        robot = new Robot();
        robot.setAutoDelay(100);
        for (UIManager.LookAndFeelInfo laf : UIManager.getInstalledLookAndFeels()) {
            try {
                SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
                System.out.println("Test for LookAndFeel " + laf.getClassName());
                new bug4697612();
                System.out.println("Test passed for LookAndFeel " + laf.getClassName());
            }  catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public bug4697612() throws Exception {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });
            robot.waitForIdle();
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    text.requestFocus();
                }
            });
            robot.waitForIdle();

            // 4697612: pressing PgDn + PgUp should not alter caret position
            robot.keyPress(KeyEvent.VK_HOME);
            robot.keyRelease(KeyEvent.VK_HOME);
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            robot.waitForIdle();

            int pos0 = getTextCaretPosition();
            int caretHeight = getTextCaretHeight();
            fontHeight = FONT_HEIGHT;

            // iterate two times, for different (even and odd) font height
            for (int i = 0; i < 2; i++) {

                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        text.setFont(text.getFont().deriveFont(fontHeight));
                    }
                });
                frameHeight = FRAME_HEIGHT;

                for (int j = 0; j < caretHeight; j++) {

                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            frame.setSize(FRAME_WIDTH, frameHeight);
                        }
                    });
                    robot.waitForIdle();
                    robot.keyPress(KeyEvent.VK_PAGE_DOWN);
                    robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
                    robot.keyPress(KeyEvent.VK_PAGE_UP);
                    robot.keyRelease(KeyEvent.VK_PAGE_UP);
                    robot.waitForIdle();

                    int pos = getTextCaretPosition();
                    if (pos0 != pos) {
                        throw new RuntimeException("Failed 4697612: PgDn & PgUp keys scroll by different amounts");
                    }
                    frameHeight++;
                }
                fontHeight++;
            }

            // 6244705: pressing PgDn at the very bottom should not scroll
            LookAndFeel laf = UIManager.getLookAndFeel();
            if (laf.getID().equals("Aqua")) {
                robot.keyPress(KeyEvent.VK_END);
                robot.keyRelease(KeyEvent.VK_END);
            } else {
                robot.keyPress(KeyEvent.VK_CONTROL);
                robot.keyPress(KeyEvent.VK_END);
                robot.keyRelease(KeyEvent.VK_END);
                robot.keyRelease(KeyEvent.VK_CONTROL);
            }
            robot.waitForIdle();
            robot.delay(1000);

            pos0 = getScrollerViewPosition();
            robot.keyPress(KeyEvent.VK_PAGE_DOWN);
            robot.keyRelease(KeyEvent.VK_PAGE_DOWN);
            robot.waitForIdle();

            int pos = getScrollerViewPosition();

            if (pos0 != pos) {
                System.out.println("pos0 " + pos0 + " pos " + pos);
                throw new RuntimeException("Failed 6244705: PgDn at the bottom causes scrolling");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }

    private static int getTextCaretPosition() throws Exception {
        final int[] result = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                result[0] = text.getCaretPosition();
            }
        });

        return result[0];
    }

    private static int getTextCaretHeight() throws Exception {
        final int[] result = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                try {
                    int pos0 = text.getCaretPosition();
                    Rectangle dotBounds = text.modelToView(pos0);
                    result[0] = dotBounds.height;
                } catch (BadLocationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        return result[0];
    }

    private static int getScrollerViewPosition() throws Exception {
        final int[] result = new int[1];
        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                result[0] = scroller.getViewport().getViewPosition().y;
            }
        });

        return result[0];
    }

    private static void createAndShowGUI() {
        frame = new JFrame();
        frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
        frame.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        text = new JTextArea();
        try {
            InputStream is =
                    bug4697612.class.getResourceAsStream("bug4697612.txt");
            text.read(new InputStreamReader(is), null);
        } catch (IOException e) {
            throw new Error(e);
        }

        scroller = new JScrollPane(text);

        frame.getContentPane().add(scroller);

        frame.pack();
        frame.setVisible(true);
    }
}
