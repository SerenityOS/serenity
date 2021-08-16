
/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025815
 * @summary Child FileDialog of modal dialog does not get focus on Gnome
 * @author Semyon Sadetsky
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class FileDialogModalFocusTest {
    public static void main(String[] args) throws Exception {
        Frame frame = new Frame();
        FileDialog fileDialog = new FileDialog((Frame) null);
        test(frame, fileDialog);
        frame = new Frame();
        fileDialog = new FileDialog(frame);
        test(frame, fileDialog);
        System.out.println("ok");
    }

    private static void test(final Frame frame, final FileDialog fileDialog)
            throws InterruptedException, InvocationTargetException,
            AWTException {
        Button button = new Button();
        button.setBackground(Color.RED);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileDialog.setVisible(true);
            }
        });
        frame.add(button);
        frame.setSize(200, 200);
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
        Robot robot = new Robot();
        robot.setAutoDelay(200);
        robot.waitForIdle();
        Point point = button.getLocationOnScreen();
        point.translate(100, 100);
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        int delay = 0;
        while (frame.isFocused() && delay < 2000) {
            robot.delay(50);
            delay += 50;
        }
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        button.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                lock.lock();
                condition.signal();
                lock.unlock();
            }
        });
        lock.lock();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        });
        condition.await(5, TimeUnit.SECONDS);
        lock.unlock();
        robot.delay(200);
        robot.waitForIdle();
        EventQueue.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                button.requestFocus();
                Point p = new Point(button.getWidth() - 10, button.getHeight() - 10);
                SwingUtilities.convertPointToScreen(p, button);
                robot.mouseMove(p.x, p.y);
                robot.mousePress(InputEvent.BUTTON1_MASK);
                robot.mouseRelease(InputEvent.BUTTON1_MASK);
            }
        });
        robot.waitForIdle();
        Point p = new Point(100, 100);
        SwingUtilities.convertPointToScreen(p, button);
        BufferedImage image = robot.createScreenCapture(
                new Rectangle(p,
                        new Dimension(button.getWidth() - 200,
                                button.getHeight() - 200)));
        boolean found = false;
        for (int x = 0; x < image.getWidth(); x+=50) {
            for (int y = 0; y < image.getHeight(); y+=50) {
                if( (image.getRGB(x, y) & 0x00FFFF) != 0 ) {
                    found = true;
                    break;
                };
            }
        }
        frame.dispose();
        robot.waitForIdle();
        fileDialog.dispose();
        if(!found) {
            throw new RuntimeException("file chooser is underneath");
        }
    }
}
