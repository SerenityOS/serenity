/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.X11;

//import static sun.awt.X11.XEmbed.*;
import java.awt.*;
import java.awt.event.*;
import sun.util.logging.PlatformLogger;
import static sun.awt.X11.XConstants.*;
import java.util.LinkedList;

/**
 * Test XEmbed server implementation. See file:///home/dom/bugs/4931668/test_plan.html for
 * specification and references.
 */
public class XEmbedServerTester implements XEventDispatcher {
    private static final PlatformLogger xembedLog = PlatformLogger.getLogger("sun.awt.X11.xembed.XEmbedServerTester");
    private final Object EVENT_LOCK = new Object();
    static final int SYSTEM_EVENT_MASK = 0x8000;
    int my_version, server_version;
    XEmbedHelper xembed = new XEmbedHelper();
    boolean focused;
    int focusedKind;
    int focusedServerComponent;
    boolean reparent;
    long parent;
    boolean windowActive;
    boolean xembedActive;
    XBaseWindow window;
    volatile int eventWaited = -1, eventReceived = -1;
    int mapped;
    int accel_key, accel_keysym, accel_mods;
    static Rectangle initialBounds = new Rectangle(0, 0, 100, 100);
    Robot robot;
    Rectangle[] serverBounds; // first rectangle is for the server frame, second is for dummy frame, others are for its children
    private static final int SERVER_BOUNDS = 0, OTHER_FRAME = 1, SERVER_FOCUS = 2, SERVER_MODAL = 3, MODAL_CLOSE = 4;

    LinkedList<Integer> events = new LinkedList<Integer>();

    private XEmbedServerTester(Rectangle[] serverBounds, long parent) {
        this.parent = parent;
        focusedKind = -1;
        focusedServerComponent = -1;
        reparent = false;
        windowActive = false;
        xembedActive = false;
        my_version = XEmbedHelper.XEMBED_VERSION;
        mapped = XEmbedHelper.XEMBED_MAPPED;
        this.serverBounds = serverBounds;
        if (serverBounds.length < 5) {
            throw new IllegalArgumentException("There must be at least five areas: server-activation, server-deactivation, server-focus, " +
                                               "server-modal show, modal-close");
        }
        try {
            robot = new Robot();
            robot.setAutoDelay(100);
        } catch (Exception e) {
            throw new RuntimeException("Can't create robot");
        }
        initAccel();
        if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
            xembedLog.finer("XEmbed client(tester), embedder window: " + Long.toHexString(parent));
        }
    }

    public static XEmbedServerTester getTester(Rectangle[] serverBounds, long parent) {
        return new XEmbedServerTester(serverBounds, parent);
    }

    private void dumpReceivedEvents() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
            xembedLog.finer("Events received so far:");
            int pos = 0;
            for (Integer event : events) {
                xembedLog.finer((pos++) + ":" + XEmbedHelper.msgidToString(event));
            }
            xembedLog.finer("End of event dump");
        }
    }

    public void test1_1() {
        int res = embedCompletely();
        waitWindowActivated(res);
        requestFocus();
        deactivateServer();
        res = activateServer(getEventPos());
        waitFocusGained(res);
        checkFocusGained(XEmbedHelper.XEMBED_FOCUS_CURRENT);
    }

    public void test1_2() {
        int res = embedCompletely();
        waitWindowActivated(res);
        requestFocus();
        checkFocusGained(XEmbedHelper.XEMBED_FOCUS_CURRENT);
    }

    public void test1_3() {
        embedCompletely();
        deactivateServer();
        requestFocusNoWait();
        checkNotFocused();
    }

    public void test1_4() {
        embedCompletely();
        deactivateServer();
        requestFocusNoWait();
        checkNotFocused();
        int res = getEventPos();
        activateServer(res);
        waitFocusGained(res);
        checkFocusGained(XEmbedHelper.XEMBED_FOCUS_CURRENT);
    }

    public void test1_5() {
        int res = embedCompletely();
        waitWindowActivated(res);
        checkWindowActivated();
    }

    public void test1_6() {
        int res = embedCompletely();
        waitWindowActivated(res);
        requestFocus();
        res = deactivateServer();
        checkFocused();
    }

    public void test1_7() {
        int res = embedCompletely();
        waitWindowActivated(res);
        requestFocus();
        focusServer();
        checkFocusLost();
    }

    public void test2_5() {
        int res = embedCompletely();
        waitWindowActivated(res);
        requestFocus();
        focusServerNext();
        checkFocusedServerNext();
        checkFocusLost();
    }

    public void test2_6() {
        int res = embedCompletely();
        waitWindowActivated(res);
        requestFocus();
        focusServerPrev();
        checkFocusedServerPrev();
        checkFocusLost();
    }

    public void test3_1() {
        reparent = false;
        embedCompletely();
    }

    public void test3_3() {
        reparent = true;
        embedCompletely();
    }

    public void test3_4() {
        my_version = 10;
        embedCompletely();
        if (server_version != XEmbedHelper.XEMBED_VERSION) {
            throw new RuntimeException("Version " + server_version + " is not minimal");
        }
    }

    public void test3_5() {
        embedCompletely();

        window.destroy();
        // TODO: how can we detect that XEmbed ended?  So far we are
        // just checking that XEmbed server won't end up with an
        // exception, which should end up testing, hopefully.

        // Sleep before exiting the tester application
        sleep(1000);
    }

    public void test3_6() {
        embedCompletely();

        sleep(1000);
        XToolkit.awtLock();
        try {
            XlibWrapper.XUnmapWindow(XToolkit.getDisplay(), window.getWindow());
            XlibWrapper.XReparentWindow(XToolkit.getDisplay(), window.getWindow(), XToolkit.getDefaultRootWindow(), 0, 0);
        } finally {
            XToolkit.awtUnlock();
        }

        int res = getEventPos();

        activateServerNoWait(res);

        sleep(1000);
        if (checkEventList(res, XEmbedHelper.XEMBED_WINDOW_ACTIVATE) != -1) {
            throw new RuntimeException("Focus was been given to the client after XEmbed has ended");
        }
    }

    public void test4_1() {
        mapped = XEmbedHelper.XEMBED_MAPPED;
        int res = getEventPos();
        embedCompletely();
        sleep(1000);
        checkMapped();
    }

    public void test4_2() {
        mapped = 0;
        embedCompletely();
        sleep(1000);

        int res = getEventPos();
        mapped = XEmbedHelper.XEMBED_MAPPED;
        updateEmbedInfo();
        sleep(1000);
        checkMapped();
    }

    public void test4_3() {
        int res = getEventPos();
        mapped = XEmbedHelper.XEMBED_MAPPED;
        embedCompletely();

        res = getEventPos();
        mapped = 0;
        updateEmbedInfo();
        sleep(1000);
        checkNotMapped();
    }

    public void test4_4() {
        mapped = 0;
        embedCompletely();
        sleep(1000);
        if (XlibUtil.getWindowMapState(window.getWindow()) != IsUnmapped) {
            throw new RuntimeException("Client has been mapped");
        }
    }

    public void test6_1_1() {
        embedCompletely();
        registerAccelerator();
        focusServer();
        int res = pressAccelKey();
        waitForEvent(res, XEmbedHelper.XEMBED_ACTIVATE_ACCELERATOR);
    }

    public void test6_1_2() {
        embedCompletely();
        registerAccelerator();
        focusServer();
        deactivateServer();
        int res = pressAccelKey();
        sleep(1000);
        if (checkEventList(res, XEmbedHelper.XEMBED_ACTIVATE_ACCELERATOR) != -1) {
            throw new RuntimeException("Accelerator has been activated in inactive embedder");
        }
    }

    public void test6_1_3() {
        embedCompletely();
        registerAccelerator();
        focusServer();
        deactivateServer();
        unregisterAccelerator();
        int res = pressAccelKey();
        sleep(1000);
        if (checkEventList(res, XEmbedHelper.XEMBED_ACTIVATE_ACCELERATOR) != -1) {
            throw new RuntimeException("Accelerator has been activated after unregistering");
        }
    }

    public void test6_1_4() {
        embedCompletely();
        registerAccelerator();
        requestFocus();
        int res = pressAccelKey();
        sleep(1000);
        if (checkEventList(res, XEmbedHelper.XEMBED_ACTIVATE_ACCELERATOR) != -1) {
            throw new RuntimeException("Accelerator has been activated in focused client");
        }
    }
    public void test6_2_1() {
        embedCompletely();
        grabKey();
        focusServer();
        int res = pressAccelKey();
        waitSystemEvent(res, KeyPress);
    }

    public void test6_2_2() {
        embedCompletely();
        grabKey();
        focusServer();
        deactivateServer();
        int res = pressAccelKey();
        sleep(1000);
        if (checkEventList(res, SYSTEM_EVENT_MASK | KeyPress) != -1) {
            throw new RuntimeException("Accelerator has been activated in inactive embedder");
        }
    }

    public void test6_2_3() {
        embedCompletely();
        grabKey();
        focusServer();
        deactivateServer();
        ungrabKey();
        int res = pressAccelKey();
        sleep(1000);
        if (checkEventList(res, SYSTEM_EVENT_MASK | KeyPress) != -1) {
            throw new RuntimeException("Accelerator has been activated after unregistering");
        }
    }

    public void test6_2_4() {
        embedCompletely();
        grabKey();
        requestFocus();
        int res = pressAccelKey();
        sleep(1000);
        int pos = checkEventList(res, SYSTEM_EVENT_MASK | KeyPress);
        if (pos != -1) {
            pos = checkEventList(pos+1, SYSTEM_EVENT_MASK | KeyPress);
            if (pos != -1) { // Second event
                throw new RuntimeException("Accelerator has been activated in focused client");
            }
        }
    }

    public void test7_1() {
        embedCompletely();
        int res = showModalDialog();
        waitForEvent(res, XEmbedHelper.XEMBED_MODALITY_ON);
    }

    public void test7_2() {
        embedCompletely();
        int res = showModalDialog();
        waitForEvent(res, XEmbedHelper.XEMBED_MODALITY_ON);
        res = hideModalDialog();
        waitForEvent(res, XEmbedHelper.XEMBED_MODALITY_OFF);
    }

    public void test9_1() {
        embedCompletely();
        requestFocus();
        int res = pressAccelKey();
        waitForEvent(res, SYSTEM_EVENT_MASK | KeyPress);
    }

    private int embed() {
        int res = getEventPos();
        XToolkit.awtLock();
        try {
            XCreateWindowParams params =
                new XCreateWindowParams(new Object[] {
                    XBaseWindow.PARENT_WINDOW, Long.valueOf(reparent?XToolkit.getDefaultRootWindow():parent),
                    XBaseWindow.BOUNDS, initialBounds,
                    XBaseWindow.EMBEDDED, Boolean.TRUE,
                    XBaseWindow.VISIBLE, Boolean.valueOf(mapped == XEmbedHelper.XEMBED_MAPPED),
                    XBaseWindow.EVENT_MASK, Long.valueOf(VisibilityChangeMask | StructureNotifyMask |
                                                     SubstructureNotifyMask | KeyPressMask)});
            window = new XBaseWindow(params);

            if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                xembedLog.finer("Created tester window: " + window);
            }

            XToolkit.addEventDispatcher(window.getWindow(), this);
            updateEmbedInfo();
            if (reparent) {
                xembedLog.finer("Reparenting to embedder");
                XlibWrapper.XReparentWindow(XToolkit.getDisplay(), window.getWindow(), parent, 0, 0);
            }
        } finally {
            XToolkit.awtUnlock();
        }
        return res;
    }

    private void updateEmbedInfo() {
        long[] info = new long[] { my_version, mapped };
        long data = Native.card32ToData(info);
        try {
            XEmbedHelper.XEmbedInfo.setAtomData(window.getWindow(), data, info.length);
        } finally {
            XEmbedHelper.unsafe.freeMemory(data);
        }
    }

    private int getEventPos() {
        synchronized(EVENT_LOCK) {
            return events.size();
        }
    }

    private int embedCompletely() {
        xembedLog.fine("Embedding completely");
        int res = getEventPos();
        embed();
        waitEmbeddedNotify(res);
        return res;
    }
    private int requestFocus() {
        xembedLog.fine("Requesting focus");
        int res = getEventPos();
        sendMessage(XEmbedHelper.XEMBED_REQUEST_FOCUS);
        waitFocusGained(res);
        return res;
    }
    private int requestFocusNoWait() {
        xembedLog.fine("Requesting focus without wait");
        int res = getEventPos();
        sendMessage(XEmbedHelper.XEMBED_REQUEST_FOCUS);
        return res;
    }
    private int activateServer(int prev) {
        int res = activateServerNoWait(prev);
        waitWindowActivated(res);
        return res;
    }
    @SuppressWarnings("deprecation")
    private int activateServerNoWait(int prev) {
        xembedLog.fine("Activating server");
        int res = getEventPos();
        if (checkEventList(prev, XEmbedHelper.XEMBED_WINDOW_ACTIVATE) != -1) {
            xembedLog.fine("Activation already received");
            return res;
        }
        Point loc = serverBounds[SERVER_BOUNDS].getLocation();
        loc.x += serverBounds[SERVER_BOUNDS].getWidth()/2;
        loc.y += 5;
        robot.mouseMove(loc.x, loc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        return res;
    }
    @SuppressWarnings("deprecation")
    private int deactivateServer() {
        xembedLog.fine("Deactivating server");
        int res = getEventPos();
        Point loc = serverBounds[OTHER_FRAME].getLocation();
        loc.x += serverBounds[OTHER_FRAME].getWidth()/2;
        loc.y += serverBounds[OTHER_FRAME].getHeight()/2;
        robot.mouseMove(loc.x, loc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        waitWindowDeactivated(res);
        return res;
    }
    @SuppressWarnings("deprecation")
    private int focusServer() {
        xembedLog.fine("Focusing server");
        boolean weFocused = focused;
        int res = getEventPos();
        Point loc = serverBounds[SERVER_FOCUS].getLocation();
        loc.x += 5;
        loc.y += 5;
        robot.mouseMove(loc.x, loc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        if (weFocused) {
            waitFocusLost(res);
        }
        return res;
    }
    private int focusServerNext() {
        xembedLog.fine("Focusing next server component");
        int res = getEventPos();
        sendMessage(XEmbedHelper.XEMBED_FOCUS_NEXT);
        waitFocusLost(res);
        return res;
    }
    private int focusServerPrev() {
        xembedLog.fine("Focusing previous server component");
        int res = getEventPos();
        sendMessage(XEmbedHelper.XEMBED_FOCUS_PREV);
        waitFocusLost(res);
        return res;
    }

    private void waitEmbeddedNotify(int pos) {
        waitForEvent(pos, XEmbedHelper.XEMBED_EMBEDDED_NOTIFY);
    }
    private void waitFocusGained(int pos) {
        waitForEvent(pos, XEmbedHelper.XEMBED_FOCUS_IN);
    }
    private void waitFocusLost(int pos) {
        waitForEvent(pos, XEmbedHelper.XEMBED_FOCUS_OUT);
    }
    private void waitWindowActivated(int pos) {
        waitForEvent(pos, XEmbedHelper.XEMBED_WINDOW_ACTIVATE);
    }
    private void waitWindowDeactivated(int pos) {
        waitForEvent(pos, XEmbedHelper.XEMBED_WINDOW_DEACTIVATE);
    }

    private void waitSystemEvent(int position, int event) {
        waitForEvent(position, event | SYSTEM_EVENT_MASK);
    }

    private void waitForEvent(int position, int event) {
        synchronized(EVENT_LOCK) {
            // Check for already received events after the request
            if (checkEventList(position, event) != -1) {
                if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                    xembedLog.finer("The event " + XEmbedHelper.msgidToString(event) + " has already been received");
                }
                return;
            }

            if (eventReceived == event) {
                // Already received
                if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                    xembedLog.finer("Already received " + XEmbedHelper.msgidToString(event));
                }
                return;
            }
            eventReceived = -1;
            eventWaited = event;
            if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                xembedLog.finer("Waiting for " + XEmbedHelper.msgidToString(event) + " starting from " + position);
            }
            try {
                EVENT_LOCK.wait(3000);
            } catch (InterruptedException ie) {
                xembedLog.warning("Event wait interrupted", ie);
            }
            eventWaited = -1;
            if (checkEventList(position, event) == -1) {
                dumpReceivedEvents();
                throw new RuntimeException("Didn't receive event " + XEmbedHelper.msgidToString(event) + " but recevied " + XEmbedHelper.msgidToString(eventReceived));
            } else {
                if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                    xembedLog.finer("Successfully recevied " + XEmbedHelper.msgidToString(event));
                }
            }
        }
    }
    /**
     * Checks if the {@code event} is already in a list at position >= {@code position}
     */
    private int checkEventList(int position, int event) {
        if (position == -1) {
            return -1;
        }
        synchronized(EVENT_LOCK) {
            for (int i = position; i < events.size(); i++) {
                if (events.get(i) == event) {
                    return i;
                }
            }
            return -1;
        }
    }

    private void checkFocusedServerNext() {
        if (focusedServerComponent != 0) {
            throw new RuntimeException("Wrong focused server component, should be 0, but it is " + focusedServerComponent);
        }
    }
    private void checkFocusedServerPrev() {
        if (focusedServerComponent != 2) {
            throw new RuntimeException("Wrong focused server component, should be 2, but it is " + focusedServerComponent);
        }
    }
    private void checkFocusGained(int kind) {
        if (!focused) {
            throw new RuntimeException("Didn't receive FOCUS_GAINED");
        }
        if (focusedKind != kind) {
            throw new RuntimeException("Kinds don't match, required: " + kind + ", current: " + focusedKind);
        }
    }
    private void checkNotFocused() {
        if (focused) {
            throw new RuntimeException("Focused");
        }
    }
    private void checkFocused() {
        if (!focused) {
            throw new RuntimeException("Not Focused");
        }
    }

    private void checkFocusLost() {
        checkNotFocused();
        if (focusedKind != XEmbedHelper.XEMBED_FOCUS_OUT) {
            throw new RuntimeException("Didn't receive FOCUS_LOST");
        }
    }
    private void checkWindowActivated() {
        if (!windowActive) {
            throw new RuntimeException("Window is not active");
        }
    }
    private void checkMapped() {
        if (XlibUtil.getWindowMapState(window.getWindow()) == IsUnmapped) {
            throw new RuntimeException("Client is not mapped");
        }
    }
    private void checkNotMapped() {
        if (XlibUtil.getWindowMapState(window.getWindow()) != IsUnmapped) {
            throw new RuntimeException("Client is mapped");
        }
    }

    private void sendMessage(int message) {
        xembed.sendMessage(parent, message);
    }
    private void sendMessage(int message, int detail, long data1, long data2) {
        xembed.sendMessage(parent, message, detail, data1, data2);
    }

    public void dispatchEvent(XEvent ev) {
        if (ev.get_type() == ClientMessage) {
            XClientMessageEvent msg = ev.get_xclient();
            if (msg.get_message_type() == XEmbedHelper.XEmbed.getAtom()) {
                if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                    xembedLog.fine("Embedded message: " + XEmbedHelper.msgidToString((int)msg.get_data(1)));
                }
                switch ((int)msg.get_data(1)) {
                  case XEmbedHelper.XEMBED_EMBEDDED_NOTIFY: // Notification about embedding protocol start
                      xembedActive = true;
                      server_version = (int)msg.get_data(3);
                      break;
                  case XEmbedHelper.XEMBED_WINDOW_ACTIVATE:
                      windowActive = true;
                      break;
                  case XEmbedHelper.XEMBED_WINDOW_DEACTIVATE:
                      windowActive = false;
                      break;
                  case XEmbedHelper.XEMBED_FOCUS_IN: // We got focus!
                      focused = true;
                      focusedKind = (int)msg.get_data(2);
                      break;
                  case XEmbedHelper.XEMBED_FOCUS_OUT:
                      focused = false;
                      focusedKind = XEmbedHelper.XEMBED_FOCUS_OUT;
                      focusedServerComponent = (int)msg.get_data(2);
                      break;
                }
                synchronized(EVENT_LOCK) {
                    events.add((int)msg.get_data(1));

                    if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                        xembedLog.finer("Tester is waiting for " +  XEmbedHelper.msgidToString(eventWaited));
                    }
                    if ((int)msg.get_data(1) == eventWaited) {
                        eventReceived = (int)msg.get_data(1);
                        if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                            xembedLog.finer("Notifying waiting object for event " + System.identityHashCode(EVENT_LOCK));
                        }
                        EVENT_LOCK.notifyAll();
                    }
                }
            }
        } else {
            synchronized(EVENT_LOCK) {
                int eventID = ev.get_type() | SYSTEM_EVENT_MASK;
                events.add(eventID);

                if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                    xembedLog.finer("Tester is waiting for " + XEmbedHelper.msgidToString(eventWaited) + ", but we received " + ev + "(" + XEmbedHelper.msgidToString(eventID) + ")");
                }
                if (eventID == eventWaited) {
                    eventReceived = eventID;
                    if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                        xembedLog.finer("Notifying waiting object" + System.identityHashCode(EVENT_LOCK));
                    }
                    EVENT_LOCK.notifyAll();
                }
            }
        }
    }

    private void sleep(int amount) {
        try {
            Thread.sleep(amount);
        } catch (Exception e) {
        }
    }

    private void registerAccelerator() {
        sendMessage(XEmbedHelper.XEMBED_REGISTER_ACCELERATOR, 1, accel_keysym, accel_mods);
    }

    private void unregisterAccelerator() {
        sendMessage(XEmbedHelper.XEMBED_UNREGISTER_ACCELERATOR, 1, 0, 0);
    }

    private int pressAccelKey() {
        int res = getEventPos();
        robot.keyPress(accel_key);
        robot.keyRelease(accel_key);
        return res;
    }

    private void initAccel() {
        accel_key = KeyEvent.VK_A;
        accel_keysym = XWindow.getKeySymForAWTKeyCode(accel_key);
        accel_mods = 0;
    }

    private void grabKey() {
        sendMessage(XEmbedHelper.NON_STANDARD_XEMBED_GTK_GRAB_KEY, 0, accel_keysym, accel_mods);
    }
    private void ungrabKey() {
        sendMessage(XEmbedHelper.NON_STANDARD_XEMBED_GTK_UNGRAB_KEY, 0, accel_keysym, accel_mods);
    }
    @SuppressWarnings("deprecation")
    private int showModalDialog() {
        xembedLog.fine("Showing modal dialog");
        int res = getEventPos();
        Point loc = serverBounds[SERVER_MODAL].getLocation();
        loc.x += 5;
        loc.y += 5;
        robot.mouseMove(loc.x, loc.y);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        return res;
    }
    private int hideModalDialog() {
        xembedLog.fine("Hide modal dialog");
        int res = getEventPos();
//         Point loc = serverBounds[MODAL_CLOSE].getLocation();
//         loc.x += 5;
//         loc.y += 5;
//         robot.mouseMove(loc.x, loc.y);
//         robot.mousePress(InputEvent.BUTTON1_MASK);
//         robot.delay(50);
//         robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.keyPress(KeyEvent.VK_SPACE);
        robot.keyRelease(KeyEvent.VK_SPACE);
        return res;
    }

}
