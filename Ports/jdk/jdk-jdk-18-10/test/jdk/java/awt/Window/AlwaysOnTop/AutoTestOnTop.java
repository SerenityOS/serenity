/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4632143
  @summary Unit test for the RFE window/frame/dialog always on top
  @author dom@sparc.spb.su: area=awt.toplevel
  @run main/othervm/timeout=600 AutoTestOnTop
*/

import java.awt.AWTEvent;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.PaintEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JWindow;

/**
 * @author tav@sparc.spb.su
 * @author dom@sparc.spb.su
 * Tests that always-on-top windows combine correctly with different kinds of window in different styles and conditions.
 *
 * !!! WARNING !!!
 * The test fails sometimes because the toFront() method doesn't guarantee
 * that after its invocation the frame will be placed above all other windows.
 */
public class AutoTestOnTop {
    private static final int X = 300;
    private static final int Y = 300;

    static Window topw;
    static Frame  parentw = new Frame();
    static Window f;
    static Frame  parentf = new Frame();

    static final Object  uncheckedSrc = new Object(); // used when no need to check event source
    static volatile Object  eventSrc = uncheckedSrc;
    static boolean dispatchedCond;

    static Semaphore STATE_SEMA = new Semaphore();
    static Semaphore VIS_SEMA = new Semaphore();
    static Vector errors = new Vector();

    static boolean isUnix = false;

    static StringBuffer msgError = new StringBuffer();
    static StringBuffer msgCase = new StringBuffer();
    static StringBuffer msgAction = new StringBuffer();
    static StringBuffer msgFunc = new StringBuffer();
    static StringBuffer msgVisibility = new StringBuffer();

    static volatile int stageNum;
    static volatile int actNum;
    static volatile int testResult = 0;

    static volatile boolean doCheckEvents;
    static volatile boolean eventsCheckPassed;
    static boolean[] eventsCheckInitVals = new boolean[] { // Whether events are checked for abcence or precence
        true, true, true, true, true, false, false, false, false
    };
    static String[] msgEventsChecks = new String[] {
        null, null, null, null, null,
        "expected WindowEvent.WINDOW_STATE_CHANGED hasn't been generated",
        "expected WindowEvent.WINDOW_STATE_CHANGED hasn't been generated",
        "expected WindowEvent.WINDOW_STATE_CHANGED hasn't been generated",
        "expected WindowEvent.WINDOW_STATE_CHANGED hasn't been generated",
    };

    static final int stagesCount = 7;
    static final int actionsCount = 9;

    static Method[] preActions = new Method[actionsCount];
    static Method[] postActions = new Method[actionsCount];
    static Method[] isActionsAllowed = new Method[actionsCount];
    static Method[] checksActionEvents = new Method[actionsCount];

    static Robot robot;

    static boolean doStartTest;
    static String osName = System.getProperty("os.name");


    public static void main(String[] args) {
        checkTesting();

    }

    public static void performTesting() {
        isUnix = osName.equals("Linux");

        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                public void eventDispatched(AWTEvent e) {
                    if (e.getID() == MouseEvent.MOUSE_CLICKED) {
                        if (eventSrc != null & eventSrc != uncheckedSrc && e.getSource() != eventSrc) {
                            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": " + msgError);
                            testResult = -1;
                        }
                        if (eventSrc != null){
                            synchronized (eventSrc) {
                                dispatchedCond = true;
                                eventSrc.notify();
                            }
                        }
                    }

                    if (doCheckEvents && (e.getSource() == topw || e.getSource() == f)) {

                        //System.err.println("AWTEventListener: catched the event " + e);

                        try {
                            checksActionEvents[actNum].invoke(null, new Object[] {e});
                        } catch (InvocationTargetException ite) {
                            ite.printStackTrace();
                        } catch (IllegalAccessException iae) {
                            iae.printStackTrace();
                        }
                        return;
                    }
                }
            }, 0xffffffffffffffffL);

        Method[] allMethods;

        try {
            allMethods = AutoTestOnTop.class.getDeclaredMethods();
        } catch (SecurityException se) {
            throw new RuntimeException(se);
        }

        for (int i = 0; i < allMethods.length; i++) {
            String name = allMethods[i].getName();
            if (name.startsWith("preAction")) {
                preActions[name.charAt(name.length() - 1) - '0'] = allMethods[i];
            } else if (name.startsWith("postAction")) {
                postActions[name.charAt(name.length() - 1) - '0'] = allMethods[i];
            } else if (name.startsWith("isActionAllowed")) {
                isActionsAllowed[name.charAt(name.length() - 1) - '0'] = allMethods[i];
            } else if (name.startsWith("checkActionEvents")) {
                checksActionEvents[name.charAt(name.length() - 1) - '0'] = allMethods[i];
            }
        }

        f = new Frame("Auxiliary Frame");
        f.setBounds(X, Y, 650, 100);
        f.setVisible(true);
        waitTillShown(f);

        try {
            robot = new Robot();
            robot.setAutoDelay(100);
        } catch (AWTException e) {
            throw new RuntimeException("Error: unable to create robot", e);
        }

        mainTest();

        if (testResult != 0) {
            System.err.println("The following errors were encountered: ");
            for (int i = 0; i < errors.size(); i++) {
                System.err.println(errors.get(i).toString());
            }
            throw new RuntimeException("Test failed.");
        } else {
            System.err.println("Test PASSED.");
        }
    }

    public static void mainTest() {
//         stageNum = 0;
//         for (int i = 0; i < 5; i++) {
//             actNum = 2;
//             System.err.println("************************* A C T I O N " + actNum + " *************************");
//             doStage(stageNum, actNum);
// //             pause(500);
//             actNum = 3;
//             System.err.println("************************* A C T I O N " + actNum + " *************************");
//             doStage(stageNum, actNum);
// //             pause(500);
//         }
        for (stageNum = 0; stageNum < stagesCount; stageNum++) {
            System.err.println("************************* S T A G E " + stageNum + " *************************");
            for (actNum = 0; actNum < actionsCount; actNum++) {
                System.err.println("************************* A C T I O N " + actNum + " *************************");
                doStage(stageNum, actNum);
            } // for thru actNum
        } // fow thru stageNum

        eventSrc = null;
    }

    private static void doStage(int stageNum, int actNum) {
        try {

            if (!((Boolean)isActionsAllowed[actNum].invoke(null, new Object[0])).booleanValue()) {
                System.err.println("Action skipped due to a platform limitations");
                return;
            }

            STATE_SEMA.reset();
            createWindow(stageNum);

            //*************************
            // Set window always-on-top
            //*************************

            preActions[actNum].invoke(null, new Object[0]);
            setAlwaysOnTop(topw, true);
            waitForIdle(true);

            if (!topw.isAlwaysOnTopSupported()) return;

            postActions[actNum].invoke(null, new Object[0]);
            waitForIdle(false);

            STATE_SEMA.reset();

            testForAlwaysOnTop();

            //*****************************
            // Set window not always-on-top
            //*****************************

            preActions[actNum].invoke(null, new Object[0]);
            setAlwaysOnTop(topw, false);
            waitForIdle(true);
            postActions[actNum].invoke(null, new Object[0]);
            waitForIdle(false);
            STATE_SEMA.reset();

            testForNotAlwaysOnTop();

        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static void checkTesting() {
        if (Toolkit.getDefaultToolkit().isAlwaysOnTopSupported()) {
            performTesting();
        }
    }

    public static void testForAlwaysOnTop() {
        System.err.println("Checking for always-on-top " + topw);

        ensureInitialWinPosition(topw);

        // Check that always-on-top window is topmost.
        // - Click on always-on-top window on the windows cross area.
        clickOn(topw, f, 10, 50, "setting " + msgVisibility +
                " window (1) always-on-top didn't make it topmost");

        // Check that we can't change z-order of always-on-top window.
        // - a) Try to put the other window on the top.
        f.toFront();
        clickOn(uncheckedSrc, f, 450, 50, ""); // coz toFront() works not always
        pause(300);

        // - b) Click on always-on-top window on the windows cross area.
        clickOn(topw, f, 10, 50, "setting " + msgVisibility +
                " window (1) always-on-top didn't make it such");

        // Ask for always-on-top property
        if (isAlwaysOnTop(topw) != true)
                error("Test failed: stage #" + stageNum + ", action #" + actNum + ": " + msgCase + ": " + msgAction +
                                   ": isAlwaysOnTop() returned 'false' for window (1) set always-on-top at state "
                                   + msgVisibility);
    }

    public static void testForNotAlwaysOnTop() {
        System.err.println("Checking for non always-on-top of " + topw);
        ensureInitialWinPosition(topw);

        if (msgVisibility.equals("visible") && actNum != 2) {
            // Check that the window remains topmost.
            // - click on the window on the windows cross area.
            clickOn(topw, f, 10, 50, "setting " + msgVisibility +
                    " window (1) not always-on-top didn't keep it topmost");
        }

        // Check that we can change z-order of not always-on-top window.
        // - a) try to put the other window on the top.
        f.toFront();
        clickOn(uncheckedSrc, f, 450, 50, ""); // coz toFront() works not always
        pause(300);

        // - b) click on not always-on-top window on the windows cross area.
        clickOn(f, f, 10, 50, "setting " + msgVisibility +
                " window (1) not always-on-top didn't make it such");

        // Ask for always-on-top property
        if (isAlwaysOnTop(topw) != false)
            error("Test failed: stage #" + stageNum + ", action #" + actNum + ": " + msgCase + ": " + msgAction +
                               ": isAlwaysOnTop() returned 'true' for window (1) set not always-on-top at state "
                               + msgVisibility);
    }


    private static void createWindow(int stageNum) {
        // Free native resourses
        if (topw != null) {
            topw.dispose();
        }

        switch (stageNum) {
        case 0:
            topw = new Frame("Top Frame");
            msgCase.replace(0, msgCase.length(), "Frame (1) over Frame (2)");
            break;
        case 1:
            topw = new JFrame("Top JFrame");
            msgCase.replace(0, msgCase.length(), "JFrame (1) over Frame (2)");
            break;
        case 2:
            topw = new Dialog(parentw, "Top Dialog");
            msgCase.replace(0, msgCase.length(), "Dialog (1) over Frame (2)");
            break;
        case 3:
            topw = new JDialog(parentw, "Top JDialog");
            msgCase.replace(0, msgCase.length(), "JDialog (1) over Frame (2)");
            break;
        case 4:
            topw = new Frame("Top Frame");
            f.dispose();
            f = new Dialog(parentf, "Auxiliary Dialog");
            f.setBounds(X, Y, 650, 100);
            f.setVisible(true);
            waitTillShown(f);
            msgCase.replace(0, msgCase.length(), "Frame (1) over Dialog (2)");
            break;
        case 5:
            topw = new Window(parentw);
            msgCase.replace(0, msgCase.length(), "Window (1) over Frame (2)");
            break;
        case 6:
            topw = new JWindow(parentw);
            msgCase.replace(0, msgCase.length(), "JWindow (1) over Frame (2)");
            break;
        }
        topw.addWindowStateListener(new WindowAdapter() {
                public void windowStateChanged(WindowEvent e) {
                    System.err.println("* " + e);
                    STATE_SEMA.raise();
                }
            });
        topw.setSize(300, 100);
    }

    /**
     * 0: setting always-on-top to invisible window
     * 1: setting always-on-top to visible window
     * 2: always-on-top on visible non-focusable window
     * 3: always-on-top on visible, dragging topw after that
     * 4: always-on-top on visible, dragging f after that
     * 5: always-on-top on (visible, maximized), make normal after that
     * 6: always-on-top on (visible, iconified), make normal after that
     * 7: always-on-top on visible, iconify/deiconify after that
     * 8: always-on-top on visible, maximize/restore after that
     */
    public static void preAction_0() {
        topw.setVisible(false);
    }
    public static void postAction_0() {
        if (topw.isShowing()) {
            error("Test failed: stage #" + stageNum + ", action #" + actNum + ": " + msgCase +
                               ": no actions with windows: changing always-on-top property at window (1) state 'invisible' makes window (1) visible");
        }
        setWindowVisible("no actions with windows", "invisible");
    }
    public static boolean isActionAllowed_0() {
        // Window on Linux is always always-on-top!
        return !((stageNum == 5 || stageNum == 6) && isUnix) && (stageNum < stagesCount);
    }
    public static void checkActionEvents_0(AWTEvent e) {
        System.err.println(e.toString());
   }

    public static void preAction_1() {
        setWindowVisible("no actions with windows", "visible");
    }
    public static void postAction_1() {}
    public static boolean isActionAllowed_1() {
        return !((stageNum == 5 || stageNum == 6) && isUnix) && (stageNum < stagesCount );
    }
    public static void checkActionEvents_1(AWTEvent e) {
        System.err.println(e.toString());
        if (e instanceof PaintEvent) {
            return;
        }
        eventsCheckPassed = false;
        error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                           ":  unexpected event " + e + " was generated");
    }

    public static void preAction_2() {
        setWindowVisible("when window (1) set not focusable", "visible");
        topw.setFocusableWindowState(false);
        f.toFront();
        pause(300);
    }
    public static void postAction_2() {}
    public static boolean isActionAllowed_2() {
        return !((stageNum == 5 || stageNum == 6) && isUnix) && (stageNum < stagesCount);
    }
    public static void checkActionEvents_2(AWTEvent e) {
        System.err.println(e.toString());
        if ( (e.getID() >= FocusEvent.FOCUS_FIRST && e.getID() <= FocusEvent.FOCUS_LAST) ||
             (e.getID() == WindowEvent.WINDOW_LOST_FOCUS && e.getID() == WindowEvent.WINDOW_GAINED_FOCUS)) {
            eventsCheckPassed = false;
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " +
                               msgAction + ": after call " + msgFunc +
                               ": unexpected event " + e + " was generated");
        }
    }

    public static void preAction_3() {
        setWindowVisible("after dragging",  "visible");
    }
    public static void postAction_3() {
        Point p = topw.getLocationOnScreen();
        int x = p.x + 150, y = p.y + 5;

        try {                      // Take a pause to avoid double click
            Thread.sleep(500);     // when called one after another.
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IllegalComponentStateException e) {
            e.printStackTrace();
        }

        // Drag the window.
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseMove(X + 150, Y + 100);
        robot.mouseMove(x, y);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }
    public static boolean isActionAllowed_3() {
        return (stageNum < 5);
    }
    public static void checkActionEvents_3(AWTEvent e) {
        System.err.println(e.toString());
    }

    public static void preAction_4() {
        setWindowVisible("after dragging window (2)",  "visible");
    }
    public static void postAction_4() {
        Point p = f.getLocationOnScreen();
        int x = p.x + 400, y = p.y + 5;

        try {                      // Take a pause to avoid double click
            Thread.sleep(500);     // when called one after another.
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (IllegalComponentStateException e) {
            e.printStackTrace();
        }

        // Drag the window.
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseMove(X + 400, Y + 100);
        robot.mouseMove(x, y);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);

        ensureInitialWinPosition(f);
    }
    public static boolean isActionAllowed_4() {
        return !((stageNum == 5 || stageNum == 6) && isUnix);
    }
    public static void checkActionEvents_4(AWTEvent e) {
        System.err.println(e.toString());
    }

    // Metacity has a bug not allowing to set a window to NORMAL state!!!

    public static void preAction_5() {
        setWindowVisible("at state 'maximized'",  "visible");
        ((Frame)topw).setExtendedState(Frame.MAXIMIZED_BOTH);
        waitForStateChange();
    }
    public static void postAction_5() {
        ((Frame)topw).setExtendedState(Frame.NORMAL);
        waitForStateChange();
    }
    public static boolean isActionAllowed_5() {
        return (stageNum < 2);
    }
    public static void checkActionEvents_5(AWTEvent e) {
        System.err.println("=" + e.toString());
        if (e.getID() == WindowEvent.WINDOW_STATE_CHANGED) {
            eventsCheckPassed = true;
        }
    }

    public static void preAction_6() {
        setWindowVisible("at state 'iconified'",  "visible");
        System.err.println("Iconifying " + topw);
        ((Frame)topw).setExtendedState(Frame.ICONIFIED);
        if (!waitForStateChange()) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                               ":  state change to ICONIFIED hasn't been generated");
        }
    }
    public static void postAction_6() {
        System.err.println("Restoring " + topw);
        ((Frame)topw).setExtendedState(Frame.NORMAL);
        if (!waitForStateChange()) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                               ":  state change to NORMAL hasn't been generated");
        }
    }
    public static boolean isActionAllowed_6() {
        return (stageNum < 2 );
    }
    public static void checkActionEvents_6(AWTEvent e) {
        System.err.println("+" + e.toString());
        if (e.getID() == WindowEvent.WINDOW_STATE_CHANGED) {
            eventsCheckPassed = true;
        }
    }

    public static void preAction_7() {
        setWindowVisible("before state 'iconified'",  "visible");
    }
    public static void postAction_7() {
        System.err.println("Setting iconified");
        ((Frame)topw).setExtendedState(Frame.ICONIFIED);
        if (!waitForStateChange()) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                               ":  state change to ICONIFIED hasn't been generated");
        }
        System.err.println("Setting normal");
        ((Frame)topw).setExtendedState(Frame.NORMAL);
        if (!waitForStateChange()) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                               ":  state change to NORMAL hasn't been generated");
        }
    }
    public static boolean isActionAllowed_7() {
        return (stageNum < 2);
    }
    public static void checkActionEvents_7(AWTEvent e) {
        System.err.println(e.toString());
        if (e.getID() == WindowEvent.WINDOW_STATE_CHANGED) {
            eventsCheckPassed = true;
        }
    }

    public static void preAction_8() {
        setWindowVisible("before state 'maximized'",  "visible");
    }
    public static void postAction_8() {
        ((Frame)topw).setExtendedState(Frame.MAXIMIZED_BOTH);
        if (!waitForStateChange()) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                               ":  state change to MAXIMIZED hasn't been generated");
        }
        ((Frame)topw).setExtendedState(Frame.NORMAL);
        if (!waitForStateChange()) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call " + msgFunc +
                               ":  state change to NORMAL hasn't been generated");
        }
    }
    public static boolean isActionAllowed_8() {
        return (stageNum < 2);
    }
    public static void checkActionEvents_8(AWTEvent e) {
        System.err.println(e.toString());
        if (e.getID() == WindowEvent.WINDOW_STATE_CHANGED) {
           eventsCheckPassed = true;
        }
    }

    //***************************************************************************

    private static void setWindowVisible(String mAction, String mVisibility) {
        msgAction.replace(0, msgAction.length(), mAction);
        msgVisibility.replace(0, msgVisibility.length(), mVisibility);

        topw.setVisible(true);
        pause(100); // Needs for Sawfish
        topw.setLocation(X, Y);
        waitTillShown(topw);
        f.toFront();
        pause(300);
    }

    private static void clickOn(Object src, Window relwin, int x, int y, String errorStr) {
        Point p = relwin.getLocationOnScreen();
        int counter = 10;
        while (--counter > 0) {
            eventSrc = src;
            msgError.replace(0, msgError.length(), errorStr);

            robot.mouseMove(p.x + x, p.y + y);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);

            synchronized (eventSrc) {
                if (!dispatchedCond) {
                    try {
                        eventSrc.wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (!dispatchedCond) {
                    //System.err.println("clickOn: MOUSE_CLICKED event losed, trying to generate it again...");
                    continue;
                }
                dispatchedCond = false;
            }
            break;
        } // end while
        if (counter <= 0) {
            eventSrc = uncheckedSrc;
            error("Test: internal error: could't catch MOUSE_CLICKED event. Skip testing this stage");
        }
    }

    private static void setAlwaysOnTop(Window w, boolean value) {
        System.err.println("Setting always on top on " + w + " to " + value);
        robot.mouseMove(X - 50, Y - 50); // Move out of the window
        msgFunc.replace(0, msgCase.length(), "setAlwaysOnTop()");
        try {
            w.setAlwaysOnTop(value);
        } catch (Exception e) {
            error("Test failed: stage#" + stageNum + "action #" + actNum + ": " + msgCase + ": " + msgAction +
                               ": setAlwaysOnTop(" + value + ") called at state " + msgVisibility +
                               " threw exeption " + e);
        }
    }

    private static boolean isAlwaysOnTop(Window w) {
        robot.mouseMove(X - 50, Y - 50); // Move out of the window
        msgFunc.replace(0, msgCase.length(), "isAlwaysOnTop()");
        boolean result = false;
        try {
            result = w.isAlwaysOnTop();
        } catch (Exception e) {
            error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction +
                               ": isAlwaysOnTop() called at state " + msgVisibility +
                               " threw exeption " + e);
        }
        return result;
    }

    private static void waitTillShown(Component c) {
        while (true) {
            try {
                Thread.sleep(100);
                c.getLocationOnScreen();
                break;
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private static void waitForIdle(boolean doCheck) {
        try {
            robot.waitForIdle();
            EventQueue.invokeAndWait( new Runnable() {
                    public void run() {} // Dummy implementation
                } );
        } catch(InterruptedException ite) {
            System.err.println("waitForIdle, non-fatal exception caught:");
            ite.printStackTrace();
        } catch(InvocationTargetException ine) {
            System.err.println("waitForIdle, non-fatal exception caught:");
            ine.printStackTrace();
        }
        doCheckEvents = doCheck;

        if (doCheck) {
            eventsCheckPassed = eventsCheckInitVals[actNum]; // Initialize
        } else if (!eventsCheckPassed &&
                 msgEventsChecks[actNum] != null) {


            // Some expected event hasn't been catched,
            // so give it one more chance...
            doCheckEvents = true;
            pause(500);
            doCheckEvents = false;

            if (!eventsCheckPassed) {
                testResult = -1;
                error("Test failed: stage #" + stageNum + ", action # " + actNum + ": " + msgCase + ": " + msgAction + ": after call "
                                   + msgFunc + ": " + msgEventsChecks[actNum]);
            }
        }
    }

    private static boolean waitForStateChange() {
        System.err.println("------- Waiting for state change");
        try {
            STATE_SEMA.doWait(3000);
        } catch (InterruptedException ie) {
            System.err.println("Wait interrupted: " + ie);
        }
        boolean state = STATE_SEMA.getState();
        STATE_SEMA.reset();
        robot.delay(1000); // animation normal <--> maximized states
        return state;
    }

    private static void ensureInitialWinPosition(Window w) {
        int counter = 30;
        while (w.getLocationOnScreen().y != Y && --counter > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }
        if (counter <= 0) {
            w.setLocation(X, Y);
            pause(100);
            System.err.println("Test: window set to initial position forcedly");
        }
    }

    private static void pause(int mls) {
        try {
            Thread.sleep(mls);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void error(String msg) {
        errors.add(msg);
        System.err.println(msg);
    }
}

class Semaphore {
    boolean state = false;
    int waiting = 0;
    public Semaphore() {
    }
    public synchronized void doWait() throws InterruptedException {
        if (state) {
            return;
        }
        waiting++;
        wait();
        waiting--;
    }
    public synchronized void doWait(int timeout) throws InterruptedException {
        if (state) {
            return;
        }
        waiting++;
        wait(timeout);
        waiting--;
    }
    public synchronized void raise() {
        state = true;
        if (waiting > 0) {
            notifyAll();
        }
    }

    public synchronized void doNotify() {
        notifyAll();
    }
    public synchronized boolean getState() {
        return state;
    }

    public synchronized void reset() {
        state = false;
    }
}
