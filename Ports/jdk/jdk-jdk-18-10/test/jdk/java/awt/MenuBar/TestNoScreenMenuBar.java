/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8146310
 * @summary [macosx] setDefaultMenuBar does not initialize screen menu bar
 * @author Alan Snyder
 * @library /test/lib
 * @run main/othervm TestNoScreenMenuBar
 * @requires (os.family == "mac")
 */

import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import jdk.test.lib.process.ProcessTools;

public class TestNoScreenMenuBar
{
    static TestNoScreenMenuBar theTest;
    private Robot robot;
    private Process process;
    private boolean isActionPerformed;

    public TestNoScreenMenuBar()
    {
        try {
            robot = new Robot();
            robot.setAutoDelay(50);
        } catch (AWTException ex) {
            throw new RuntimeException(ex);
        }

        // activate another java application
        openOtherApplication();
        robot.delay(2000);

        // The failure mode is installing the default menu bar while the application is inactive
        Desktop desktop = Desktop.getDesktop();
        desktop.setDefaultMenuBar(createMenuBar());

        robot.delay(500);
        desktop.requestForeground(true);
        robot.delay(500);
    }

    JMenuBar createMenuBar()
    {
        JMenuBar mb = new JMenuBar();
        // A very long name makes it more likely that the robot will hit the menu
        JMenu menu = new JMenu("TestTestTestTestTestTestTestTestTestTest");
        mb.add(menu);
        JMenuItem item = new JMenuItem("TestTestTestTestTestTestTestTestTestTest");
        item.addActionListener(ev -> {
            isActionPerformed = true;
        });
        menu.add(item);
        return mb;
    }

    void dispose()
    {
        closeOtherApplication();
        Desktop.getDesktop().setDefaultMenuBar(null);
    }

    private void performMenuItemTest()
    {
        // Find the menu on the screen menu bar
        // The location depends upon the application name which is the name of the first menu.
        // Unfortunately, the application name can vary based on how the application is run.
        // The work around is to make the menu and the menu item names very long.

        int menuBarX = 250;
        int menuBarY = 11;
        int menuItemX = menuBarX;
        int menuItemY = 34;

        robot.mouseMove(menuBarX, menuBarY);
        robot.delay(100);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(100);
        robot.mouseMove(menuItemX, menuItemY);
        robot.delay(100);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();

        waitForAction();
    }

    private synchronized void waitForAction()
    {
        try {
            for (int i = 0; i < 10; i++) {
                if (isActionPerformed) {
                    return;
                }
                wait(100);
            }
        } catch (InterruptedException ex) {
        }
        throw new RuntimeException("Test failed: menu item action was not performed");
    }

    private void openOtherApplication() {
        process = execute();
    }

    private void closeOtherApplication() {
        if (process != null) {
            process.destroyForcibly();
        }
    }

    private Process execute() {
        try {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    TestNoScreenMenuBar.class.getSimpleName(), "mark");
            return ProcessTools.startProcess("Other frame", pb);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to execute command");
        }
    }

    private static void runSwing(Runnable r)
    {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args)
    {
        if (!System.getProperty("os.name").contains("OS X")) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }
        if (args.length != 0) {
            Frame frame = new Frame();
            MenuBar mb = new MenuBar();
            mb.add(new Menu("Hello"));
            frame.setMenuBar(mb);
            frame.setSize(300, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            frame.toFront();
            return;
        }
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        try {
            runSwing(() -> theTest = new TestNoScreenMenuBar());
            theTest.performMenuItemTest();
        } finally {
            if (theTest != null) {
                runSwing(() -> theTest.dispose());
            }
        }
    }
}
