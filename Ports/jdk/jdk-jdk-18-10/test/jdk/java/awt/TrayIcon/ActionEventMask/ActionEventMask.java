/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.image.BufferedImage;

/*
 * @test
 * @key headful
 * @summary Check if ActionEvent triggered when a TrayIcon is double
 *          (single, on Mac) clicked is visible by an AWTEventListener
 *          added to the Toolkit. It also checks if all listeners are
 *          triggered when multiple AWTEventListeners and ActionListeners
 *          are added.
 * @author Dmitriy Ermashov (dmitriy.ermashov@oracle.com)
 * @modules java.desktop/java.awt:open
 * @library /java/awt/patchlib
 * @library /lib/client ../
 * @build java.desktop/java.awt.Helper
 * @build ExtendedRobot SystemTrayIconHelper
 * @run main ActionEventMask
 */

public class ActionEventMask {

    private Image image;

    TrayIcon icon;
    ExtendedRobot robot;

    boolean actionPerformed = false;
    boolean listenersInvoked = false;
    Object actionLock = new Object();
    Object listenersLock = new Object();
    static boolean isMacOS = false;
    static final int clickDelay = 50;

    ActionListener[] listeners;
    boolean[] listenerStatus;

    Object lLock = new Object();
    boolean doTest, listenerAdded;
    Button b1;

    public static void main(String[] args) throws Exception {
        if (! SystemTray.isSupported()) {
            System.out.println("SystemTray not supported on the platform under test. " +
                               "Marking the test passed");
        } else {
            if (System.getProperty("os.name").toLowerCase().startsWith("mac")) {
                isMacOS = true;
            } else if (SystemTrayIconHelper.isOel7()) {
                System.out.println("OEL 7 doesn't support double click in " +
                        "systray. Skipped");
                return;
            }
            new ActionEventMask().doTest();
        }
    }

    public ActionEventMask() throws Exception {
        EventQueue.invokeAndWait(this::initializeGUI);
    }

    void initializeGUI() {
        SystemTray tray = SystemTray.getSystemTray();
        icon = new TrayIcon(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "Sample Icon");

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (doTest) {
                System.out.println("ActionListener AWTEventListener");
                if (! icon.equals(event.getSource())) {
                    throw new RuntimeException("FAIL: ActionEvent not triggered for icon");
                }
                actionPerformed = true;
                synchronized (actionLock) {
                    try {
                        actionLock.notifyAll();
                    } catch (Exception e) {
                    }
                }
            }
        }, AWTEvent.ACTION_EVENT_MASK);

        try {
            tray.add(icon);
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }

        listeners = new ActionListener[3];
        listenerStatus = new boolean[3];
        for (int i = 0; i < listeners.length; i++) {
            final int index = i;
            listeners[i] = event -> {
                listenerStatus[index] = true;
                System.out.println("ActionListener listeners[" + index + "]");
                for (int j = 0; j < listenerStatus.length; j++) {
                    if (! listenerStatus[j]) {
                        break;
                    }
                    listenersInvoked = true;
                    synchronized (listenersLock) {
                        try {
                            listenersLock.notifyAll();
                        } catch (Exception e) {
                        }
                    }
                }
            };
        }

        Frame frame = new Frame("Test frame");
        b1 = new Button("Add ActionListener");
        b1.addActionListener(event -> {
            for (int i = 0; i < listeners.length; i++) {
                icon.addActionListener(listeners[i]);
            }
            listenerAdded = true;
            synchronized (lLock) {
                try {
                    lLock.notifyAll();
                } catch (Exception e) {
                }
            }
        });
        frame.setLayout(new FlowLayout());
        frame.add(b1);
        frame.setSize(200, 200);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                System.err.println("User closed the window");
                System.exit(1);
            }
        });
        frame.setVisible(true);
    }

    private void doTest() throws Exception {

        robot = new ExtendedRobot();

        Point iconPosition = SystemTrayIconHelper.getTrayIconLocation(icon);
        if (iconPosition == null)
            throw new RuntimeException("Unable to find the icon location!");

        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();

        actionPerformed = false;
        doTest = true;

        if(isMacOS) {
            robot.click(InputEvent.BUTTON3_MASK);
        }else{
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(clickDelay);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(clickDelay);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(clickDelay);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }

        if (! actionPerformed) {
            synchronized (actionLock) {
                try {
                    actionLock.wait(3000);
                } catch (Exception e) {
                }
            }
        }
        if (! actionPerformed)
            throw new RuntimeException("FAIL: AWTEventListener not notified when TrayIcon " +
                               "is "+(isMacOS ? "" :"double ")+ "clicked");

        doTest = false;
        listenerAdded = false;
        robot.mouseMove(b1.getLocationOnScreen().x + b1.getSize().width / 2,
                        b1.getLocationOnScreen().y + b1.getSize().height / 2);
        robot.waitForIdle();
        robot.click();

        if (! listenerAdded) {
            synchronized (lLock) {
                try {
                    lLock.wait(3000);
                } catch (Exception e) {
                }
            }
        }
        if (! listenerAdded)
            throw new RuntimeException("FAIL: ActionListener could not be added at runtime. " +
                               "b1 did not trigger ActionEvent");

        doTest = true;
        actionPerformed = false;
        robot.mouseMove(iconPosition.x, iconPosition.y);
        robot.waitForIdle();

        if(isMacOS) {
            robot.click(InputEvent.BUTTON3_MASK);
        }else{
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(clickDelay);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
            robot.delay(clickDelay);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(clickDelay);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }

        if (! listenersInvoked) {
            synchronized (listenersLock) {
                try {
                    listenersLock.wait(3000);
                } catch (Exception e) {
                }
            }
        }
        if (! listenersInvoked) {
            System.err.println("FAIL: All the listeners not invoked!");
            for (int i = 0; i < listenerStatus.length; i++)
                throw new RuntimeException("Listener[" + i + "] invoked: " + listenerStatus[i]);
        }
        if (! actionPerformed) {
            synchronized (actionLock) {
                try {
                    actionLock.wait(3000);
                } catch (Exception e) {
                }
            }
        }
        if (! actionPerformed)
            throw new RuntimeException("FAIL: AWTEventListener not notified when TrayIcon " +
                    "is "+(isMacOS? "" : "double ")+ "clicked. A set of listeners were added after it");

    }
}
