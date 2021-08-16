/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8194327
 * @summary [macosx] AWT windows have incorrect main/key window behaviors
 * @author Alan Snyder
 * @library /test/lib
 * @run main/othervm/native TestMainKeyWindow
 * @requires (os.family == "mac")
 */

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import javax.swing.*;

import jdk.test.lib.process.ProcessTools;

public class TestMainKeyWindow
{
    static TestMainKeyWindow theTest;

    KeyStroke commandT = KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.META_DOWN_MASK);

    int nextX = 130;

    private final MyFrame frame1;
    private final MyFrame frame2;
    private final Object COLOR_PANEL = "Color Panel";
    private final Object NATIVE_WINDOW = "Native Window";

    // these bounds must agree with the native code that creates the windows
    private Rectangle colorPanelBounds = new Rectangle(130, 300, 225, 400);  // approximate is OK
    private Rectangle nativeWindowBounds = new Rectangle(130, 200, 200, 100);

    private Robot robot;

    private int actionCounter;
    private Object actionTarget;

    private int failureCount;
    private Process process;

    public TestMainKeyWindow()
    {
        System.loadLibrary("testMainKeyWindow");

        JMenuBar defaultMenuBar = createMenuBar("Application", true);
        Desktop.getDesktop().setDefaultMenuBar(defaultMenuBar);

        setup();

        frame1 = new MyFrame("Frame 1");
        frame2 = new MyFrame("Frame 2");
        frame1.setVisible(true);
        frame2.setVisible(true);

        try {
            robot = new Robot();
            robot.setAutoDelay(150);
        } catch (AWTException ex) {
            throw new RuntimeException(ex);
        }
    }

    class MyFrame
        extends JFrame
    {
        public MyFrame(String title)
            throws HeadlessException
        {
            super(title);

            JMenuBar mainMenuBar = createMenuBar(title, true);
            setJMenuBar(mainMenuBar);
            setBounds(nextX, 60, 200, 90);
            nextX += 250;
            JComponent contentPane = new JPanel();
            setContentPane(contentPane);
            contentPane.setLayout(new FlowLayout());
            contentPane.add(new JCheckBox("foo", true));
            InputMap inputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(commandT, "test");
            ActionMap actionMap = contentPane.getActionMap();
            actionMap.put("test", new MyAction(title + " Key"));
        }
    }

    private void runTest()
    {
        failureCount = 0;
        robot.waitForIdle();
        performTest(frame1, false);
        performTest(frame1, true);
        performTest(frame2, false);
        performTest(frame2, true);
        performTest(NATIVE_WINDOW, false);
        performTest(NATIVE_WINDOW, true);
        performTest(COLOR_PANEL, false);
        if (failureCount > 0) {
            throw new RuntimeException("Test failed: " + failureCount + " failure(s)");
        }
    }

    private void performTest(Object windowIdentification, boolean selectColorPanel)
    {
        setupWindows(windowIdentification, selectColorPanel);

        performMenuShortcutTest(windowIdentification, selectColorPanel);
        performMenuItemTest(windowIdentification, selectColorPanel);

        // test deactivating and reactivating the application
        // the window state and behavior should be restored

        openOtherApplication();
        activateApplication();
        robot.delay(1000);

        performMenuShortcutTest(windowIdentification, selectColorPanel);
        performMenuItemTest(windowIdentification, selectColorPanel);
    }

    private Process execute() {
        try {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    TestMainKeyWindow.class.getSimpleName(), "mark");
            return ProcessTools.startProcess("Other frame", pb);
        } catch (IOException ex) {
            throw new RuntimeException("Unable to execute command");
        }
    }

    private void openOtherApplication() {
        if (process != null) {
            process.destroyForcibly();
        }
        process = execute();
        robot.delay(1000);
    }

    private void performMenuShortcutTest(Object windowIdentification, boolean selectColorPanel)
    {
        int currentActionCount = actionCounter;

        // Perform the menu shortcut
        robot.keyPress(KeyEvent.VK_META);
        robot.keyPress(KeyEvent.VK_T);
        robot.keyRelease(KeyEvent.VK_T);
        robot.keyRelease(KeyEvent.VK_META);
        robot.waitForIdle();

        Object target = waitForAction(currentActionCount + 1);
        boolean isDirectKey = windowIdentification instanceof Window && !selectColorPanel;
        Object expectedTarget = getExpectedTarget(windowIdentification, isDirectKey);
        if (!Objects.equals(target, expectedTarget)) {
            failureCount++;
            String configuration = getConfigurationName(windowIdentification, selectColorPanel);
            System.err.println("***** Menu shortcut test failed for " + configuration + ". Expected: " + expectedTarget + ", Actual: " + target);
        }
    }

    private void performMenuItemTest(Object windowIdentification, boolean selectColorPanel)
    {
        int currentActionCount = actionCounter;

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

        Object target = waitForAction(currentActionCount + 1);
        Object expectedTarget = getExpectedTarget(windowIdentification, false);
        if (!Objects.equals(target, expectedTarget)) {
            failureCount++;
            String configuration = getConfigurationName(windowIdentification, selectColorPanel);
            System.err.println("***** Menu item test failed for " + configuration + ". Expected: " + expectedTarget + ", Actual: " + target);
        }
    }

    private String getConfigurationName(Object windowIdentification, boolean selectColorPanel)
    {
        String name = "Unknown";
        if (windowIdentification instanceof Window) {
            Window w = (Window) windowIdentification;
            name = getWindowTitle(w);
        } else if (windowIdentification == NATIVE_WINDOW) {
            name = "Native Window";
        } else if (windowIdentification == COLOR_PANEL) {
            name = "Color Panel";
        }
        if (selectColorPanel) {
            return name + " with color panel";
        } else {
            return name;
        }
    }

    private Object getExpectedTarget(Object windowIdentification, boolean isDirectKey)
    {
        if (windowIdentification instanceof Window) {
            Window w = (Window) windowIdentification;
            String title = getWindowTitle(w);
            if (isDirectKey) {
                title = title + " Key";
            }
            return title;
        }
        return "Application";
    }

    private String getWindowTitle(Window w)
    {
        if (w instanceof Frame) {
            Frame f = (Frame) w;
            return f.getTitle();
        }
        if (w instanceof Dialog) {
            Dialog d = (Dialog) w;
            return d.getTitle();
        }
        throw new IllegalStateException();
    }

    private synchronized void registerAction(Object target)
    {
        actionCounter++;
        actionTarget = target;
    }

    private synchronized Object waitForAction(int count)
    {
        try {
            for (int i = 0; i < 10; i++) {
                if (actionCounter == count) {
                    return actionTarget;
                }
                if (actionCounter > count) {
                    throw new IllegalStateException();
                }
                wait(100);
            }
        } catch (InterruptedException ex) {
        }
        return "No Action";
    }

    private void setupWindows(Object windowIdentification, boolean selectColorPanel)
    {
        clickOnWindowTitleBar(windowIdentification);
        if (selectColorPanel) {
            clickOnWindowTitleBar(COLOR_PANEL);
        }
    }

    private void clickOnWindowTitleBar(Object windowIdentification)
    {
        Rectangle bounds = getWindowBounds(windowIdentification);
        int x = bounds.x + 70;  // to the right of the stoplight buttons
        int y = bounds.y + 12;  // in the title bar
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }

    private Rectangle getWindowBounds(Object windowIdentification)
    {
        if (windowIdentification instanceof Window) {
            Window w = (Window) windowIdentification;
            return w.getBounds();
        }
        if (windowIdentification == COLOR_PANEL) {
            return colorPanelBounds;
        }
        if (windowIdentification == NATIVE_WINDOW) {
            return nativeWindowBounds;
        }
        throw new IllegalArgumentException();
    }

    JMenuBar createMenuBar(String text, boolean isEnabled)
    {
        JMenuBar mb = new JMenuBar();
        // A very long name makes it more likely that the robot will hit the menu
        JMenu menu = new JMenu("TestTestTestTestTestTestTestTestTestTest");
        mb.add(menu);
        JMenuItem item = new JMenuItem("TestTestTestTestTestTestTestTestTestTest");
        item.setAccelerator(commandT);
        item.setEnabled(isEnabled);
        item.addActionListener(ev -> {
            registerAction(text);
        });
        menu.add(item);
        return mb;
    }

    void dispose()
    {
        frame1.setVisible(false);
        frame2.setVisible(false);
        frame1.dispose();
        frame2.dispose();
        takedown();
        Desktop.getDesktop().setDefaultMenuBar(null);
        if (process != null) {
            process.destroyForcibly();
        }
    }

    class MyAction
        extends AbstractAction
    {
        String text;

        public MyAction(String text)
        {
            super("Test");

            this.text = text;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            registerAction(text);
        }
    }

    private static native void setup();
    private static native void takedown();
    private static native void activateApplication();

    public static void main(String[] args) throws Exception
    {
        if (!System.getProperty("os.name").contains("OS X")) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        System.setProperty("apple.laf.useScreenMenuBar", "true");

        if (args.length != 0) {
            Frame frame = new Frame();
            MenuBar mb = new MenuBar();
            mb.add(new Menu("Hello"));
            frame.setMenuBar(mb);
            frame.setBounds(400, 180, 300, 300);
            frame.setVisible(true);
            frame.toFront();
            Thread.sleep(20_000);
            System.exit(0);
            return;
        }

        try {
            runSwing(() -> {
                theTest = new TestMainKeyWindow();
            });
            theTest.runTest();
        } finally {
            if (theTest != null) {
                runSwing(() -> {
                    theTest.dispose();
                });
            }
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
}
