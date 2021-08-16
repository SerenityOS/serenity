/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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

package test.java.awt.regtesthelpers;
/**
 * <p>This class contains utilities useful for regression testing.
 * <p>When using jtreg you would include this class into the build
 * list via something like:
 * <pre>
     @library ../../../regtesthelpers
     @build Util
     @run main YourTest
   </pre>
 * Note that if you are about to create a test based on
 * Applet-template, then put those lines into html-file, not in java-file.
 * <p> And put an
 * import test.java.awt.regtesthelpers.Util;
 * into the java source of test.
*/

import java.awt.Component;
import java.awt.Frame;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.Button;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.IllegalComponentStateException;
import java.awt.AWTException;
import java.awt.AWTEvent;
import java.awt.Color;

import java.awt.event.InputEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowFocusListener;
import java.awt.event.FocusListener;
import java.awt.event.ActionListener;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.security.PrivilegedAction;
import java.security.AccessController;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Util {
    private Util() {} // this is a helper class with static methods :)

    private volatile static Robot robot;

    /*
     * @throws RuntimeException when creation failed
     */
    public static Robot createRobot() {
        try {
            if (robot == null) {
                robot = new Robot();
            }
            return robot;
        } catch (AWTException e) {
            throw new RuntimeException("Error: unable to create robot", e);
        }
    }


    /**
     * Makes the window visible and waits until it's shown.
     */
    public static void showWindowWait(Window win) {
        win.setVisible(true);
        waitTillShown(win);
    }

    /**
     * Moves mouse pointer in the center of given {@code comp} component
     * using {@code robot} parameter.
     */
    public static void pointOnComp(final Component comp, final Robot robot) {
        Rectangle bounds = new Rectangle(comp.getLocationOnScreen(), comp.getSize());
        robot.mouseMove(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    /**
     * Moves mouse pointer in the center of a given {@code comp} component
     * and performs a left mouse button click using the {@code robot} parameter
     * with the {@code delay} delay between press and release.
     */
    public static void clickOnComp(final Component comp, final Robot robot, int delay) {
        pointOnComp(comp, robot);
        robot.delay(delay);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(delay);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    /**
     * Moves mouse pointer in the center of a given {@code comp} component
     * and performs a left mouse button click using the {@code robot} parameter
     * with the default delay between press and release.
     */
    public static void clickOnComp(final Component comp, final Robot robot) {
        clickOnComp(comp, robot, 50);
    }

    public static Point getTitlePoint(Window decoratedWindow) {
        Point p = decoratedWindow.getLocationOnScreen();
        Dimension d = decoratedWindow.getSize();
        return new Point(p.x + (int)(d.getWidth()/2),
                         p.y + (int)(decoratedWindow.getInsets().top/2));
    }

    /*
     * Clicks on a title of Frame/Dialog.
     * WARNING: it may fail on some platforms when the window is not wide enough.
     */
    public static void clickOnTitle(final Window decoratedWindow, final Robot robot) {
        if (decoratedWindow instanceof Frame || decoratedWindow instanceof Dialog) {
            Point p = getTitlePoint(decoratedWindow);
            robot.mouseMove(p.x, p.y);
            robot.delay(50);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }
    }

    /**
     * Tests whether screen pixel has the expected color performing several
     * attempts. This method is useful for asynchronous window manager where
     * it's impossible to determine when drawing actually takes place.
     *
     * @param x X position of pixel
     * @param y Y position of pixel
     * @param color expected color
     * @param attempts number of attempts to undertake
     * @param delay delay before each attempt
     * @param robot a robot to use for retrieving pixel color
     * @return true if pixel color matches the color expected, otherwise false
     */
    public static boolean testPixelColor(int x, int y, final Color color, int attempts, int delay, final Robot robot) {
        while (attempts-- > 0) {
            robot.delay(delay);
            Color screen = robot.getPixelColor(x, y);
            if (screen.equals(color)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests whether the area within boundaries has the expected color
     * performing several attempts. This method is useful for asynchronous
     * window manager where it's impossible to determine when drawing actually
     * takes place.
     *
     * @param bounds position of area
     * @param color expected color
     * @param attempts number of attempts to undertake
     * @param delay delay before each attempt
     * @param robot a robot to use for retrieving pixel color
     * @return true if area color matches the color expected, otherwise false
     */
    public static boolean testBoundsColor(final Rectangle bounds, final Color color, int attempts, int delay, final Robot robot) {
        int right = bounds.x + bounds.width - 1;
        int bottom = bounds.y + bounds.height - 1;
        while (attempts-- > 0) {
            if (testPixelColor(bounds.x, bounds.y, color, 1, delay, robot)
                && testPixelColor(right, bounds.y, color, 1, 0, robot)
                && testPixelColor(right, bottom, color, 1, 0, robot)
                && testPixelColor(bounds.x, bottom, color, 1, 0, robot)) {
                return true;
            }
        }
        return false;
    }

    public static void waitForIdle(Robot robot) {
        if (robot == null) {
            robot = createRobot();
        }
        robot.waitForIdle();
    }


    /*
     * Waits for a notification and for a boolean condition to become true.
     * The method returns when the above conditions are fullfilled or when the timeout
     * occurs.
     *
     * @param condition the object to be notified and the booelan condition to wait for
     * @param timeout the maximum time to wait in milliseconds
     * @param catchExceptions if {@code true} the method catches InterruptedException
     * @return the final boolean value of the {@code condition}
     * @throws InterruptedException if the awaiting proccess has been interrupted
     */
    public static boolean waitForConditionEx(final AtomicBoolean condition, long timeout)
      throws InterruptedException
        {
            synchronized (condition) {
                long startTime = System.currentTimeMillis();
                while (!condition.get()) {
                    condition.wait(timeout);
                    if (System.currentTimeMillis() - startTime >= timeout ) {
                        break;
                    }
                }
            }
            return condition.get();
        }

    /*
     * The same as {@code waitForConditionEx(AtomicBoolean, long)} except that it
     * doesn't throw InterruptedException.
     */
    public static boolean waitForCondition(final AtomicBoolean condition, long timeout) {
        try {
            return waitForConditionEx(condition, timeout);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error: unexpected exception caught!", e);
        }
    }

    /*
     * The same as {@code waitForConditionEx(AtomicBoolean, long)} but without a timeout.
     */
    public static void waitForConditionEx(final AtomicBoolean condition)
      throws InterruptedException
        {
            synchronized (condition) {
                while (!condition.get()) {
                    condition.wait();
                }
            }
        }

    /*
     * The same as {@code waitForConditionEx(AtomicBoolean)} except that it
     * doesn't throw InterruptedException.
     */
    public static void waitForCondition(final AtomicBoolean condition) {
        try {
            waitForConditionEx(condition);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error: unexpected exception caught!", e);
        }
    }

    public static void waitTillShownEx(final Component comp) throws InterruptedException {
        while (true) {
            try {
                Thread.sleep(100);
                comp.getLocationOnScreen();
                break;
            } catch (IllegalComponentStateException e) {}
        }
    }
    public static void waitTillShown(final Component comp) {
        try {
            waitTillShownEx(comp);
        } catch (InterruptedException e) {
            throw new RuntimeException("Error: unexpected exception caught!", e);
        }
    }

    /**
     * Drags from one point to another with the specified mouse button pressed.
     *
     * @param robot a robot to use for moving the mouse, etc.
     * @param startPoint a start point of the drag
     * @param endPoint an end point of the drag
     * @param button one of {@code InputEvent.BUTTON1_MASK},
     *     {@code InputEvent.BUTTON2_MASK}, {@code InputEvent.BUTTON3_MASK}
     *
     * @throws IllegalArgumentException if {@code button} is not one of
     *     {@code InputEvent.BUTTON1_MASK}, {@code InputEvent.BUTTON2_MASK},
     *     {@code InputEvent.BUTTON3_MASK}
     */
    public static void drag(Robot robot, Point startPoint, Point endPoint, int button) {
        if (!(button == InputEvent.BUTTON1_MASK || button == InputEvent.BUTTON2_MASK
                || button == InputEvent.BUTTON3_MASK))
        {
            throw new IllegalArgumentException("invalid mouse button");
        }

        robot.mouseMove(startPoint.x, startPoint.y);
        robot.mousePress(button);
        try {
            mouseMove(robot, startPoint, endPoint);
        } finally {
            robot.mouseRelease(button);
        }
    }

    /**
     * Moves the mouse pointer from one point to another.
     * Uses Bresenham's algorithm.
     *
     * @param robot a robot to use for moving the mouse
     * @param startPoint a start point of the drag
     * @param endPoint an end point of the drag
     */
    public static void mouseMove(Robot robot, Point startPoint, Point endPoint) {
        int dx = endPoint.x - startPoint.x;
        int dy = endPoint.y - startPoint.y;

        int ax = Math.abs(dx) * 2;
        int ay = Math.abs(dy) * 2;

        int sx = signWOZero(dx);
        int sy = signWOZero(dy);

        int x = startPoint.x;
        int y = startPoint.y;

        int d = 0;

        if (ax > ay) {
            d = ay - ax/2;
            while (true){
                robot.mouseMove(x, y);
                robot.delay(50);

                if (x == endPoint.x){
                    return;
                }
                if (d >= 0){
                    y = y + sy;
                    d = d - ax;
                }
                x = x + sx;
                d = d + ay;
            }
        } else {
            d = ax - ay/2;
            while (true){
                robot.mouseMove(x, y);
                robot.delay(50);

                if (y == endPoint.y){
                    return;
                }
                if (d >= 0){
                    x = x + sx;
                    d = d - ay;
                }
                y = y + sy;
                d = d + ax;
            }
        }
    }

    private static int signWOZero(int i){
        return (i > 0)? 1: -1;
    }

    private static int sign(int n) {
        return n < 0 ? -1 : n == 0 ? 0 : 1;
    }

    /** Returns {@code WindowListener} instance that diposes {@code Window} on
     *  "window closing" event.
     *
     * @return    the {@code WindowListener} instance that could be set
     *            on a {@code Window}. After that
     *            the {@code Window} is disposed when "window closed"
     *            event is sent to the {@code Window}
     */
    public static WindowListener getClosingWindowAdapter() {
        return new WindowAdapter () {
            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
            }
        };
    }

    /*
     * The values directly map to the ones of
     * sun.awt.X11.XWM & sun.awt.motif.MToolkit classes.
     */
    public final static int
        UNDETERMINED_WM = 1,
        NO_WM = 2,
        OTHER_WM = 3,
        OPENLOOK_WM = 4,
        MOTIF_WM = 5,
        CDE_WM = 6,
        ENLIGHTEN_WM = 7,
        KDE2_WM = 8,
        SAWFISH_WM = 9,
        ICE_WM = 10,
        METACITY_WM = 11,
        COMPIZ_WM = 12,
        LG3D_WM = 13,
        CWM_WM = 14,
        MUTTER_WM = 15;

    /*
     * Returns -1 in case of not X Window or any problems.
     */
    public static int getWMID() {
        Class clazz = null;
        try {
            if ("sun.awt.X11.XToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {
                clazz = Class.forName("sun.awt.X11.XWM");
            } else if ("sun.awt.motif.MToolkit".equals(Toolkit.getDefaultToolkit().getClass().getName())) {
                clazz = Class.forName("sun.awt.motif.MToolkit");
            }
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        if (clazz == null) {
            return -1;
        }

        try {
            final Class _clazz = clazz;
            Method m_addExports = Class.forName("java.awt.Helper").getDeclaredMethod("addExports", String.class, java.lang.Module.class);
            // No MToolkit anymore: nothing to do about it.
            // We may be called from non-X11 system, and this permission cannot be delegated to a test.
            m_addExports.invoke(null, "sun.awt.X11", Util.class.getModule());
            Method m_getWMID = (Method)AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        try {
                            Method method = _clazz.getDeclaredMethod("getWMID", new Class[] {});
                            if (method != null) {
                                method.setAccessible(true);
                            }
                            return method;
                        } catch (NoSuchMethodException e) {
                            assert false;
                        } catch (SecurityException e) {
                            assert false;
                        }
                        return null;
                    }
                });
            return ((Integer)m_getWMID.invoke(null, new Object[] {})).intValue();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } catch (NoSuchMethodException nsme) {
            nsme.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
        return -1;
    }

    //Cleans all the references
    public static void cleanUp() {
        apListener = null;
        fgListener = null;
        wgfListener = null;
    }


    ////////////////////////////
    // Some stuff to test focus.
    ////////////////////////////

    private static WindowGainedFocusListener wgfListener = new WindowGainedFocusListener();
    private static FocusGainedListener fgListener = new FocusGainedListener();
    private static ActionPerformedListener apListener = new ActionPerformedListener();

    private abstract static class EventListener {
        AtomicBoolean notifier = new AtomicBoolean(false);
        Component comp;
        boolean printEvent;

        public void listen(Component comp, boolean printEvent) {
            this.comp = comp;
            this.printEvent = printEvent;
            notifier.set(false);
            setListener(comp);
        }

        public AtomicBoolean getNotifier() {
            return notifier;
        }

        abstract void setListener(Component comp);

        void printAndNotify(AWTEvent e) {
            if (printEvent) {
                System.err.println(e);
            }
            synchronized (notifier) {
                notifier.set(true);
                notifier.notifyAll();
            }
        }
    }

    private static class WindowGainedFocusListener extends EventListener implements WindowFocusListener {

        void setListener(Component comp) {
            ((Window)comp).addWindowFocusListener(this);
        }

        public void windowGainedFocus(WindowEvent e) {

            ((Window)comp).removeWindowFocusListener(this);
            printAndNotify(e);
        }

        public void windowLostFocus(WindowEvent e) {}
    }

    private static class FocusGainedListener extends EventListener implements FocusListener {

        void setListener(Component comp) {
            comp.addFocusListener(this);
        }

        public void focusGained(FocusEvent e) {
            comp.removeFocusListener(this);
            printAndNotify(e);
        }

        public void focusLost(FocusEvent e) {}
    }

    private static class ActionPerformedListener extends EventListener implements ActionListener {

        void setListener(Component comp) {
            ((Button)comp).addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            ((Button)comp).removeActionListener(this);
            printAndNotify(e);
        }
    }

    private static boolean trackEvent(int eventID, Component comp, Runnable action, int time, boolean printEvent) {
        EventListener listener = null;

        switch (eventID) {
        case WindowEvent.WINDOW_GAINED_FOCUS:
            listener = wgfListener;
            break;
        case FocusEvent.FOCUS_GAINED:
            listener = fgListener;
            break;
        case ActionEvent.ACTION_PERFORMED:
            listener = apListener;
            break;
        }

        listener.listen(comp, printEvent);
        action.run();
        return Util.waitForCondition(listener.getNotifier(), time);
    }

    /*
     * Tracks WINDOW_GAINED_FOCUS event for a window caused by an action.
     * @param window the window to track the event for
     * @param action the action to perform
     * @param time the max time to wait for the event
     * @param printEvent should the event received be printed or doesn't
     * @return true if the event has been received, otherwise false
     */
    public static boolean trackWindowGainedFocus(Window window, Runnable action, int time, boolean printEvent) {
        return trackEvent(WindowEvent.WINDOW_GAINED_FOCUS, window, action, time, printEvent);
    }

    /*
     * Tracks FOCUS_GAINED event for a component caused by an action.
     * @see #trackWindowGainedFocus
     */
    public static boolean trackFocusGained(Component comp, Runnable action, int time, boolean printEvent) {
        return trackEvent(FocusEvent.FOCUS_GAINED, comp, action, time, printEvent);
    }

    /*
     * Tracks ACTION_PERFORMED event for a button caused by an action.
     * @see #trackWindowGainedFocus
     */
    public static boolean trackActionPerformed(Button button, Runnable action, int time, boolean printEvent) {
        return trackEvent(ActionEvent.ACTION_PERFORMED, button, action, time, printEvent);
    }

    /*
     * Requests focus on the component provided and waits for the result.
     * @return true if the component has been focused, false otherwise.
     */
    public static boolean focusComponent(Component comp, int time) {
        return focusComponent(comp, time, false);
    }
    public static boolean focusComponent(final Component comp, int time, boolean printEvent) {
        return trackFocusGained(comp,
                                new Runnable() {
                                    public void run() {
                                        comp.requestFocus();
                                    }
                                },
                                time, printEvent);

    }


    /**
     * Invokes the <code>task</code> on the EDT thread.
     *
     * @return result of the <code>task</code>
     */
    public static <T> T invokeOnEDT(final java.util.concurrent.Callable<T> task) throws Exception {
        final java.util.List<T> result = new java.util.ArrayList<T>(1);
        final Exception[] exception = new Exception[1];

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {
                try {
                    result.add(task.call());
                } catch (Exception e) {
                    exception[0] = e;
                }
            }
        });

        if (exception[0] != null) {
            throw exception[0];
        }

        return result.get(0);
    }

}
