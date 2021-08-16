/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Window;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.InvalidDnDOperationException;

import java.util.*;

import sun.java2d.pipe.Region;
import sun.util.logging.PlatformLogger;

import sun.awt.dnd.SunDragSourceContextPeer;
import sun.awt.dnd.SunDropTargetContextPeer;
import sun.awt.SunToolkit;
import sun.awt.AWTAccessor;

/**
 * The XDragSourceContextPeer class is the class responsible for handling
 * the interaction between the XDnD/Motif DnD subsystem and Java drag sources.
 *
 * @since 1.5
 */
public final class XDragSourceContextPeer
    extends SunDragSourceContextPeer implements XDragSourceProtocolListener {
    private static final PlatformLogger logger =
        PlatformLogger.getLogger("sun.awt.X11.xembed.xdnd.XDragSourceContextPeer");

    /* The events selected on the root window when the drag begins. */
    private static final int ROOT_EVENT_MASK = (int)XConstants.ButtonMotionMask |
        (int)XConstants.KeyPressMask | (int)XConstants.KeyReleaseMask;
    /* The events to be delivered during grab. */
    private static final int GRAB_EVENT_MASK = (int)XConstants.ButtonPressMask |
        (int)XConstants.ButtonMotionMask | (int)XConstants.ButtonReleaseMask;

    /* The event mask of the root window before the drag operation starts. */
    private long rootEventMask = 0;
    private boolean dndInProgress = false;
    private boolean dragInProgress = false;
    private long dragRootWindow = 0;

    /* The protocol chosen for the communication with the current drop target. */
    private XDragSourceProtocol dragProtocol = null;
    /* The drop action chosen by the current drop target. */
    private int targetAction = DnDConstants.ACTION_NONE;
    /* The set of drop actions supported by the drag source. */
    private int sourceActions = DnDConstants.ACTION_NONE;
    /* The drop action selected by the drag source based on the modifiers state
       and the action selected by the current drop target. */
    private int sourceAction = DnDConstants.ACTION_NONE;
    /* The data formats supported by the drag source for the current drag
       operation. */
    private long[] sourceFormats = null;
    /* The XID of the root subwindow that contains the current target. */
    private long targetRootSubwindow = 0;
    /* window scale factor */
    int windowScale = 1;
    /* The pointer location. */
    private int xRoot = 0;
    private int yRoot = 0;
    /* Keyboard modifiers state. */
    private int eventState = 0;

    /* XEmbed DnD support. We act as a proxy between source and target. */
    private long proxyModeSourceWindow = 0;

    /* The singleton instance. */
    private static final XDragSourceContextPeer theInstance =
        new XDragSourceContextPeer(null);

    private XDragSourceContextPeer(DragGestureEvent dge) {
        super(dge);
    }

    static XDragSourceProtocolListener getXDragSourceProtocolListener() {
        return theInstance;
    }

    static XDragSourceContextPeer createDragSourceContextPeer(DragGestureEvent dge)
      throws InvalidDnDOperationException {
    theInstance.setTrigger(dge);
        return theInstance;
    }

    protected void startDrag(Transferable transferable,
                             long[] formats, Map<Long, DataFlavor> formatMap) {
        Component component = getTrigger().getComponent();
        Component c = null;
        XWindowPeer wpeer = null;

        for (c = component; c != null && !(c instanceof Window);
             c = AWTAccessor.getComponentAccessor().getParent(c));

        if (c instanceof Window) {
            wpeer = AWTAccessor.getComponentAccessor().getPeer(c);
        }

        if (wpeer == null) {
            throw new InvalidDnDOperationException(
                "Cannot find top-level for the drag source component");
        }

        long xcursor = 0;
        long rootWindow = 0;
        long timeStamp = 0;
        windowScale = wpeer.getScale();

        /* Retrieve the X cursor for the drag operation. */
        {
            Cursor cursor = getCursor();
            if (cursor != null) {
                xcursor = XGlobalCursorManager.getCursor(cursor);
            }
        }

        XToolkit.awtLock();
        try {
            if (proxyModeSourceWindow != 0) {
                throw new InvalidDnDOperationException("Proxy drag in progress");
            }
            if (dndInProgress) {
                throw new InvalidDnDOperationException("Drag in progress");
            }

            /* Determine the root window for the drag operation. */
            {
                long screen = XlibWrapper.XScreenNumberOfScreen(wpeer.getScreen());
                rootWindow = XlibWrapper.RootWindow(XToolkit.getDisplay(), screen);
            }

            timeStamp = XToolkit.getCurrentServerTime();

            int dropActions = getDragSourceContext().getSourceActions();

            Iterator<XDragSourceProtocol> dragProtocols =
                XDragAndDropProtocols.getDragSourceProtocols();
            while (dragProtocols.hasNext()) {
                XDragSourceProtocol dragProtocol = dragProtocols.next();
                try {
                    dragProtocol.initializeDrag(dropActions, transferable,
                                                formatMap, formats);
                } catch (XException xe) {
                    throw (InvalidDnDOperationException)
                        new InvalidDnDOperationException().initCause(xe);
                }
            }

            /* Install X grabs. */
            {
                int status;
                XWindowAttributes wattr = new XWindowAttributes();
                try {
                    status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                              rootWindow, wattr.pData);

                    if (status == 0) {
                        throw new InvalidDnDOperationException("XGetWindowAttributes failed");
                    }

                    rootEventMask = wattr.get_your_event_mask();

                    XlibWrapper.XSelectInput(XToolkit.getDisplay(), rootWindow,
                                             rootEventMask | ROOT_EVENT_MASK);
                } finally {
                    wattr.dispose();
                }

                XBaseWindow.ungrabInput();

                status = XlibWrapper.XGrabPointer(XToolkit.getDisplay(), rootWindow,
                                                  0, GRAB_EVENT_MASK,
                                                  XConstants.GrabModeAsync,
                                                  XConstants.GrabModeAsync,
                                                  XConstants.None, xcursor, timeStamp);

                if (status != XConstants.GrabSuccess) {
                    cleanup(timeStamp);
                    throwGrabFailureException("Cannot grab pointer", status);
                    return;
                }

                status = XlibWrapper.XGrabKeyboard(XToolkit.getDisplay(), rootWindow,
                                                   0,
                                                   XConstants.GrabModeAsync,
                                                   XConstants.GrabModeAsync,
                                                   timeStamp);

                if (status != XConstants.GrabSuccess) {
                    cleanup(timeStamp);
                    throwGrabFailureException("Cannot grab keyboard", status);
                    return;
                }
            }

            /* Update the global state. */
            dndInProgress = true;
            dragInProgress = true;
            dragRootWindow = rootWindow;
            sourceActions = dropActions;
            sourceFormats = formats;
        } finally {
            XToolkit.awtUnlock();
        }

        /* This implementation doesn't use native context */
        setNativeContext(0);

        SunDropTargetContextPeer.setCurrentJVMLocalSourceTransferable(transferable);
    }

    public long getProxyModeSourceWindow() {
        return proxyModeSourceWindow;
    }

    private void setProxyModeSourceWindowImpl(long window) {
        proxyModeSourceWindow = window;
    }

    public static void setProxyModeSourceWindow(long window) {
        theInstance.setProxyModeSourceWindowImpl(window);
    }

    /**
     * set cursor
     */

    public void setCursor(Cursor c) throws InvalidDnDOperationException {
        XToolkit.awtLock();
        try {
            super.setCursor(c);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    protected void setNativeCursor(long nativeCtxt, Cursor c, int cType) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        if (c == null) {
            return;
        }

        long xcursor = XGlobalCursorManager.getCursor(c);

        if (xcursor == 0) {
            return;
        }

        XlibWrapper.XChangeActivePointerGrab(XToolkit.getDisplay(),
                                             GRAB_EVENT_MASK,
                                             xcursor,
                                             XConstants.CurrentTime);
    }

    protected boolean needsBogusExitBeforeDrop() {
        return false;
    }

    private void throwGrabFailureException(String msg, int grabStatus)
      throws InvalidDnDOperationException {
        String msgCause = "";
        switch (grabStatus) {
        case XConstants.GrabNotViewable:  msgCause = "not viewable";    break;
        case XConstants.AlreadyGrabbed:   msgCause = "already grabbed"; break;
        case XConstants.GrabInvalidTime:  msgCause = "invalid time";    break;
        case XConstants.GrabFrozen:       msgCause = "grab frozen";     break;
        default:                           msgCause = "unknown failure"; break;
        }
        throw new InvalidDnDOperationException(msg + ": " + msgCause);
    }

    /**
     * The caller must own awtLock.
     */
    public void cleanup(long time) {
        if (dndInProgress) {
            if (dragProtocol != null) {
                dragProtocol.sendLeaveMessage(time);
            }

            if (targetAction != DnDConstants.ACTION_NONE) {
                dragExit(xRoot, yRoot);
            }

            dragDropFinished(false, DnDConstants.ACTION_NONE, xRoot, yRoot);
        }

        Iterator<XDragSourceProtocol> dragProtocols =
            XDragAndDropProtocols.getDragSourceProtocols();
        while (dragProtocols.hasNext()) {
            XDragSourceProtocol dragProtocol = dragProtocols.next();
            try {
                dragProtocol.cleanup();
            } catch (XException xe) {
                // Ignore the exception.
            }
        }

        dndInProgress = false;
        dragInProgress = false;
        dragRootWindow = 0;
        sourceFormats = null;
        sourceActions = DnDConstants.ACTION_NONE;
        sourceAction = DnDConstants.ACTION_NONE;
        eventState = 0;
        xRoot = 0;
        yRoot = 0;

        cleanupTargetInfo();

        removeDnDGrab(time);
    }

    /**
     * The caller must own awtLock.
     */
    private void cleanupTargetInfo() {
        targetAction = DnDConstants.ACTION_NONE;
        dragProtocol = null;
        targetRootSubwindow = 0;
    }

    private void removeDnDGrab(long time) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        XlibWrapper.XUngrabPointer(XToolkit.getDisplay(), time);
        XlibWrapper.XUngrabKeyboard(XToolkit.getDisplay(), time);

        /* Restore the root event mask if it was changed. */
        if ((rootEventMask | ROOT_EVENT_MASK) != rootEventMask &&
            dragRootWindow != 0) {

            XlibWrapper.XSelectInput(XToolkit.getDisplay(),
                                     dragRootWindow,
                                     rootEventMask);
        }

        rootEventMask = 0;
        dragRootWindow = 0;
    }

    private boolean processClientMessage(XClientMessageEvent xclient) {
        if (dragProtocol != null) {
            return dragProtocol.processClientMessage(xclient);
        }
        return false;
    }

    /**
     * Updates the source action according to the specified state.
     *
     * @return true if the source
     */
    private boolean updateSourceAction(int state) {
        int action = SunDragSourceContextPeer.convertModifiersToDropAction(XWindow.getModifiers(state, 0, 0),
                                                                           sourceActions);
        if (sourceAction == action) {
            return false;
        }
        sourceAction = action;
        return true;
    }

    /**
     * Returns the client window under the specified root subwindow.
     */
    private static long findClientWindow(long window) {
        if (XlibUtil.isTrueToplevelWindow(window)) {
            return window;
        }

        Set<Long> children = XlibUtil.getChildWindows(window);
        for (Long child : children) {
            long win = findClientWindow(child);
            if (win != 0) {
                return win;
            }
        }

        return 0;
    }

    private void doUpdateTargetWindow(long subwindow, long time) {
        long clientWindow = 0;
        long proxyWindow = 0;
        XDragSourceProtocol protocol = null;
        boolean isReceiver = false;

        if (subwindow != 0) {
            clientWindow = findClientWindow(subwindow);
        }

        if (clientWindow != 0) {
            Iterator<XDragSourceProtocol> dragProtocols =
                XDragAndDropProtocols.getDragSourceProtocols();
            while (dragProtocols.hasNext()) {
                XDragSourceProtocol dragProtocol = dragProtocols.next();
                if (dragProtocol.attachTargetWindow(clientWindow, time)) {
                    protocol = dragProtocol;
                    break;
                }
            }
        }

        /* Update the global state. */
        dragProtocol = protocol;
        targetAction = DnDConstants.ACTION_NONE;
        targetRootSubwindow = subwindow;
    }

    private void updateTargetWindow(XMotionEvent xmotion) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        int x = scaleDown(xmotion.get_x_root());
        int y = scaleDown(xmotion.get_y_root());
        long time = xmotion.get_time();
        long subwindow = xmotion.get_subwindow();

        /*
         * If this event had occurred before the pointer was grabbed,
         * query the server for the current root subwindow.
         */
        if (xmotion.get_window() != xmotion.get_root()) {
            XlibWrapper.XQueryPointer(XToolkit.getDisplay(),
                                      xmotion.get_root(),
                                      XlibWrapper.larg1,  // root
                                      XlibWrapper.larg2,  // subwindow
                                      XlibWrapper.larg3,  // x_root
                                      XlibWrapper.larg4,  // y_root
                                      XlibWrapper.larg5,  // x
                                      XlibWrapper.larg6,  // y
                                      XlibWrapper.larg7); // modifiers
            subwindow = Native.getLong(XlibWrapper.larg2);
        }

        if (targetRootSubwindow != subwindow) {
            if (dragProtocol != null) {
                dragProtocol.sendLeaveMessage(time);

                /*
                 * Neither Motif DnD nor XDnD provide a mean for the target
                 * to notify the source that the pointer exits the drop site
                 * that occupies the whole top level.
                 * We detect this situation and post dragExit.
                 */
                if (targetAction != DnDConstants.ACTION_NONE) {
                    dragExit(x, y);
                }
            }

            /* Update the global state. */
            doUpdateTargetWindow(subwindow, time);

            if (dragProtocol != null) {
                dragProtocol.sendEnterMessage(sourceFormats,
                                              sourceAction,
                                              sourceActions,
                                              time);
            }
        }
    }

    /*
     * DO NOT USE is_hint field of xmotion since it could not be set when we
     * convert XKeyEvent or XButtonRelease to XMotionEvent.
     */
    private void processMouseMove(XMotionEvent xmotion) {
        if (!dragInProgress) {
            return;
        }

        int motionXRoot = scaleDown(xmotion.get_x_root());
        int motionYRoot = scaleDown(xmotion.get_y_root());

        if (xRoot != motionXRoot || yRoot != motionYRoot) {
            xRoot = motionXRoot;
            yRoot = motionYRoot;

            postDragSourceDragEvent(targetAction,
                                    XWindow.getModifiers(xmotion.get_state(),0,0),
                                    xRoot, yRoot, DISPATCH_MOUSE_MOVED);
        }

        if (eventState != xmotion.get_state()) {
            if (updateSourceAction(xmotion.get_state()) && dragProtocol != null) {
                postDragSourceDragEvent(targetAction,
                                        XWindow.getModifiers(xmotion.get_state(),0,0),
                                        xRoot, yRoot, DISPATCH_CHANGED);
            }
            eventState = xmotion.get_state();
        }

        updateTargetWindow(xmotion);

        if (dragProtocol != null) {
            dragProtocol.sendMoveMessage(xmotion.get_x_root(),
                                         xmotion.get_y_root(),
                                         sourceAction, sourceActions,
                                         xmotion.get_time());
        }
    }

    private void processDrop(XButtonEvent xbutton) {
        try {
            dragProtocol.initiateDrop(xbutton.get_x_root(),
                                      xbutton.get_y_root(),
                                      sourceAction, sourceActions,
                                      xbutton.get_time());
        } catch (XException e) {
            cleanup(xbutton.get_time());
        }
    }

    private boolean processProxyModeEvent(XEvent ev) {
        if (getProxyModeSourceWindow() == 0) {
            return false;
        }

        if (ev.get_type() != XConstants.ClientMessage) {
            return false;
        }

        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
            logger.finest("        proxyModeSourceWindow=" +
                          getProxyModeSourceWindow() +
                          " ev=" + ev);
        }

        XClientMessageEvent xclient = ev.get_xclient();

        Iterator<XDragSourceProtocol> dragProtocols =
            XDragAndDropProtocols.getDragSourceProtocols();
        while (dragProtocols.hasNext()) {
            XDragSourceProtocol dragProtocol = dragProtocols.next();
            if (dragProtocol.processProxyModeEvent(xclient,
                                                   getProxyModeSourceWindow())) {
                return true;
            }
        }

        return false;
    }

    /**
     * The caller must own awtLock.
     *
     * @return true if the event was processed and shouldn't be passed along.
     */
    private boolean doProcessEvent(XEvent ev) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        if (processProxyModeEvent(ev)) {
            return true;
        }

        if (!dndInProgress) {
            return false;
        }

        switch (ev.get_type()) {
        case XConstants.ClientMessage: {
            XClientMessageEvent xclient = ev.get_xclient();
            return processClientMessage(xclient);
        }
        case XConstants.DestroyNotify: {
            XDestroyWindowEvent xde = ev.get_xdestroywindow();

            /* Target crashed during drop processing - cleanup. */
            if (!dragInProgress &&
                dragProtocol != null &&
                xde.get_window() == dragProtocol.getTargetWindow()) {
                cleanup(XConstants.CurrentTime);
                return true;
            }
            /* Pass along */
            return false;
        }
        }

        if (!dragInProgress) {
            return false;
        }

        /* Process drag-only messages. */
        switch (ev.get_type()) {
        case XConstants.KeyRelease:
        case XConstants.KeyPress: {
            XKeyEvent xkey = ev.get_xkey();
            long keysym = XlibWrapper.XKeycodeToKeysym(XToolkit.getDisplay(),
                                                       xkey.get_keycode(), 0);
            switch ((int)keysym) {
            case (int)XKeySymConstants.XK_Escape: {
                if (ev.get_type() == XConstants.KeyRelease) {
                    cleanup(xkey.get_time());
                }
                break;
            }
            case (int)XKeySymConstants.XK_Control_R:
            case (int)XKeySymConstants.XK_Control_L:
            case (int)XKeySymConstants.XK_Shift_R:
            case (int)XKeySymConstants.XK_Shift_L: {
                XlibWrapper.XQueryPointer(XToolkit.getDisplay(),
                                          xkey.get_root(),
                                          XlibWrapper.larg1,  // root
                                          XlibWrapper.larg2,  // subwindow
                                          XlibWrapper.larg3,  // x_root
                                          XlibWrapper.larg4,  // y_root
                                          XlibWrapper.larg5,  // x
                                          XlibWrapper.larg6,  // y
                                          XlibWrapper.larg7); // modifiers
                XMotionEvent xmotion = new XMotionEvent();
                try {
                    xmotion.set_type(XConstants.MotionNotify);
                    xmotion.set_serial(xkey.get_serial());
                    xmotion.set_send_event(xkey.get_send_event());
                    xmotion.set_display(xkey.get_display());
                    xmotion.set_window(xkey.get_window());
                    xmotion.set_root(xkey.get_root());
                    xmotion.set_subwindow(xkey.get_subwindow());
                    xmotion.set_time(xkey.get_time());
                    xmotion.set_x(xkey.get_x());
                    xmotion.set_y(xkey.get_y());
                    xmotion.set_x_root(xkey.get_x_root());
                    xmotion.set_y_root(xkey.get_y_root());
                    xmotion.set_state(Native.getInt(XlibWrapper.larg7));
                    // we do not use this field, so it's unset for now
                    // xmotion.set_is_hint(???);
                    xmotion.set_same_screen(xkey.get_same_screen());

                    //It's safe to use key event as motion event since we use only their common fields.
                    processMouseMove(xmotion);
                } finally {
                    xmotion.dispose();
                }
                break;
            }
            }
            return true;
        }
        case XConstants.ButtonPress:
            return true;
        case XConstants.MotionNotify:
            processMouseMove(ev.get_xmotion());
            return true;
        case XConstants.ButtonRelease: {
            XButtonEvent xbutton = ev.get_xbutton();
            /*
             * Ignore the buttons above 20 due to the bit limit for
             * InputEvent.BUTTON_DOWN_MASK.
             * One more bit is reserved for FIRST_HIGH_BIT.
             */
            if (xbutton.get_button() > SunToolkit.MAX_BUTTONS_SUPPORTED) {
                return true;
            }

            /*
             * On some X servers it could happen that ButtonRelease coordinates
             * differ from the latest MotionNotify coordinates, so we need to
             * process it as a mouse motion.
             */
            XMotionEvent xmotion = new XMotionEvent();
            try {
                xmotion.set_type(XConstants.MotionNotify);
                xmotion.set_serial(xbutton.get_serial());
                xmotion.set_send_event(xbutton.get_send_event());
                xmotion.set_display(xbutton.get_display());
                xmotion.set_window(xbutton.get_window());
                xmotion.set_root(xbutton.get_root());
                xmotion.set_subwindow(xbutton.get_subwindow());
                xmotion.set_time(xbutton.get_time());
                xmotion.set_x(xbutton.get_x());
                xmotion.set_y(xbutton.get_y());
                xmotion.set_x_root(xbutton.get_x_root());
                xmotion.set_y_root(xbutton.get_y_root());
                xmotion.set_state(xbutton.get_state());
                // we do not use this field, so it's unset for now
                // xmotion.set_is_hint(???);
                xmotion.set_same_screen(xbutton.get_same_screen());

                //It's safe to use key event as motion event since we use only their common fields.
                processMouseMove(xmotion);
            } finally {
                xmotion.dispose();
            }
            if (xbutton.get_button() == XConstants.buttons[0]
                || xbutton.get_button() == XConstants.buttons[1]) {
                // drag is initiated with Button1 or Button2 pressed and
                // ended on release of either of these buttons (as the same
                // behavior was with our old Motif DnD-based implementation)
                removeDnDGrab(xbutton.get_time());
                dragInProgress = false;
                if (dragProtocol != null && targetAction != DnDConstants.ACTION_NONE) {
                    /*
                     * ACTION_NONE indicates that either the drop target rejects the
                     * drop or it haven't responded yet. The latter could happen in
                     * case of fast drag, slow target-server connection or slow
                     * drag notifications processing on the target side.
                     */
                    processDrop(xbutton);
                } else {
                    cleanup(xbutton.get_time());
                }
            }
            return true;
        }
        }

        return false;
    }

    static boolean processEvent(XEvent ev) {
        XToolkit.awtLock();
        try {
            try {
                return theInstance.doProcessEvent(ev);
            } catch (XException e) {
                e.printStackTrace();
                return false;
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /* XDragSourceProtocolListener implementation */

    public void handleDragReply(int action) {
        // NOTE: we have to use the current pointer location, since
        // the target didn't specify the coordinates for the reply.
        handleDragReply(action, xRoot, yRoot);
    }

    public void handleDragReply(int action, int x, int y) {
        // NOTE: we have to use the current modifiers state, since
        // the target didn't specify the modifiers state for the reply.
        handleDragReply(action, xRoot, yRoot, XWindow.getModifiers(eventState,0,0));
    }

    public void handleDragReply(int action, int x, int y, int modifiers) {
        if (action == DnDConstants.ACTION_NONE &&
            targetAction != DnDConstants.ACTION_NONE) {
            dragExit(x, y);
        } else if (action != DnDConstants.ACTION_NONE) {
            int type = 0;

            if (targetAction == DnDConstants.ACTION_NONE) {
                type = SunDragSourceContextPeer.DISPATCH_ENTER;
            } else {
                type = SunDragSourceContextPeer.DISPATCH_MOTION;
            }

            // Note that we use the modifiers state a
            postDragSourceDragEvent(action, modifiers, x, y, type);
        }

        targetAction = action;
    }

    public void handleDragFinished() {
        /* Assume that the drop was successful. */
        handleDragFinished(true);
    }

    public void handleDragFinished(boolean success) {
        /* Assume that the performed drop action is the latest drop action
           accepted by the drop target. */
        handleDragFinished(true, targetAction);
    }

    public void handleDragFinished(boolean success, int action) {
        // NOTE: we have to use the current pointer location, since
        // the target didn't specify the coordinates for the reply.
        handleDragFinished(success, action, xRoot, yRoot);
    }

    public void handleDragFinished(boolean success, int action, int x, int y) {
        dragDropFinished(success, action, x, y);

        dndInProgress = false;
        cleanup(XConstants.CurrentTime);
    }

    public int scaleUp(int x) {
        return Region.clipRound(x * (double)windowScale);
    }

    public int scaleDown(int x) {
        return Region.clipRound(x / (double)windowScale);
    }
}
