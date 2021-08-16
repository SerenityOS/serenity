/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.*;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetListener;
import java.awt.event.*;
import sun.awt.*;
import sun.awt.AWTAccessor;
import sun.util.logging.PlatformLogger;
import java.util.*;
import static sun.awt.X11.XEmbedHelper.*;

import java.security.AccessController;
import sun.security.action.GetBooleanAction;

public class XEmbedCanvasPeer extends XCanvasPeer implements WindowFocusListener, KeyEventPostProcessor, ModalityListener, WindowIDProvider {
    private static final PlatformLogger xembedLog = PlatformLogger.getLogger("sun.awt.X11.xembed.XEmbedCanvasPeer");

    boolean applicationActive; // Whether the application is active(has focus)
    XEmbedServer xembed = new XEmbedServer(); // Helper object, contains XEmbed intrinsics
    Map<Long, AWTKeyStroke> accelerators = new HashMap<Long, AWTKeyStroke>(); // Maps accelerator ID into AWTKeyStroke
    Map<AWTKeyStroke, Long> accel_lookup = new HashMap<AWTKeyStroke, Long>(); // Maps AWTKeyStroke into accelerator ID
    Set<GrabbedKey> grabbed_keys = new HashSet<GrabbedKey>(); // A set of keys grabbed by client
    Object ACCEL_LOCK = accelerators; // Lock object for working with accelerators;
    Object GRAB_LOCK = grabbed_keys; // Lock object for working with keys grabbed by client

    XEmbedCanvasPeer() {}

    XEmbedCanvasPeer(XCreateWindowParams params) {
        super(params);
    }

    XEmbedCanvasPeer(Component target) {
        super(target);
    }

    protected void postInit(XCreateWindowParams params) {
        super.postInit(params);

        installActivateListener();
        installAcceleratorListener();
        installModalityListener();

        // XEmbed canvas should be non-traversable.
        // FIXME: Probably should be removed and enforced setting of it by the users
        target.setFocusTraversalKeysEnabled(false);
    }

    protected void preInit(XCreateWindowParams params) {
        super.preInit(params);

        params.put(EVENT_MASK,
                   XConstants.KeyPressMask       | XConstants.KeyReleaseMask
                   | XConstants.FocusChangeMask  | XConstants.ButtonPressMask | XConstants.ButtonReleaseMask
                   | XConstants.EnterWindowMask  | XConstants.LeaveWindowMask | XConstants.PointerMotionMask
                   | XConstants.ButtonMotionMask | XConstants.ExposureMask    | XConstants.StructureNotifyMask | XConstants.SubstructureNotifyMask);

    }

    void installModalityListener() {
        ((SunToolkit)Toolkit.getDefaultToolkit()).addModalityListener(this);
    }

    void deinstallModalityListener() {
        ((SunToolkit)Toolkit.getDefaultToolkit()).removeModalityListener(this);
    }

    void installAcceleratorListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(this);
    }

    void deinstallAcceleratorListener() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventPostProcessor(this);
    }

    void installActivateListener() {
        // FIXME: should watch for hierarchy changes
        Window toplevel = getTopLevel(target);
        if (toplevel != null) {
            toplevel.addWindowFocusListener(this);
            applicationActive = toplevel.isFocused();
        }
    }

    void deinstallActivateListener() {
        Window toplevel = getTopLevel(target);
        if (toplevel != null) {
            toplevel.removeWindowFocusListener(this);
        }
    }

    boolean isXEmbedActive() {
        return xembed.handle != 0;
    }

    boolean isApplicationActive() {
        return applicationActive;
    }

    void initDispatching() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine("Init embedding for " + Long.toHexString(xembed.handle));
        }
        XToolkit.awtLock();
        try {
            XToolkit.addEventDispatcher(xembed.handle, xembed);
            XlibWrapper.XSelectInput(XToolkit.getDisplay(), xembed.handle,
                                     XConstants.StructureNotifyMask | XConstants.PropertyChangeMask);

            XDropTargetRegistry.getRegistry().registerXEmbedClient(getWindow(), xembed.handle);
        } finally {
            XToolkit.awtUnlock();
        }
        xembed.processXEmbedInfo();

        notifyChildEmbedded();
    }

    void endDispatching() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine("End dispatching for " + Long.toHexString(xembed.handle));
        }
        XToolkit.awtLock();
        try {
            XDropTargetRegistry.getRegistry().unregisterXEmbedClient(getWindow(), xembed.handle);
            // We can't deselect input since someone else might be interested in it
            XToolkit.removeEventDispatcher(xembed.handle, xembed);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    void embedChild(long child) {
        if (xembed.handle != 0) {
            detachChild();
        }
        xembed.handle = child;
        initDispatching();
    }

    void childDestroyed() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine("Child " + Long.toHexString(xembed.handle) + " has self-destroyed.");
        }
        endDispatching();
        xembed.handle = 0;
    }

    public void handleEvent(AWTEvent e) {
        super.handleEvent(e);
        if (isXEmbedActive()) {
            switch (e.getID()) {
              case FocusEvent.FOCUS_GAINED:
                  canvasFocusGained((FocusEvent)e);
                  break;
              case FocusEvent.FOCUS_LOST:
                  canvasFocusLost((FocusEvent)e);
                  break;
              case KeyEvent.KEY_PRESSED:
              case KeyEvent.KEY_RELEASED:
                  if (!((InputEvent)e).isConsumed()) {
                      forwardKeyEvent((KeyEvent)e);
                  }
                  break;
            }
        }
    }

    public void dispatchEvent(XEvent ev) {
        super.dispatchEvent(ev);
        switch (ev.get_type()) {
          case XConstants.CreateNotify:
              XCreateWindowEvent cr = ev.get_xcreatewindow();
              if (xembedLog.isLoggable(PlatformLogger.Level.FINEST)) {
                  xembedLog.finest("Message on embedder: " + cr);
              }
              if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                  xembedLog.finer("Create notify for parent " + Long.toHexString(cr.get_parent()) +
                                  ", window " + Long.toHexString(cr.get_window()));
              }
              embedChild(cr.get_window());
              break;
          case XConstants.DestroyNotify:
              XDestroyWindowEvent dn = ev.get_xdestroywindow();
              if (xembedLog.isLoggable(PlatformLogger.Level.FINEST)) {
                  xembedLog.finest("Message on embedder: " + dn);
              }
              if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                  xembedLog.finer("Destroy notify for parent: " + dn);
              }
              childDestroyed();
              break;
          case XConstants.ReparentNotify:
              XReparentEvent rep = ev.get_xreparent();
              if (xembedLog.isLoggable(PlatformLogger.Level.FINEST)) {
                  xembedLog.finest("Message on embedder: " + rep);
              }
              if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                  xembedLog.finer("Reparent notify for parent " + Long.toHexString(rep.get_parent()) +
                                  ", window " + Long.toHexString(rep.get_window()) +
                                  ", event " + Long.toHexString(rep.get_event()));
              }
              if (rep.get_parent() == getWindow()) {
                  // Reparented into us - embed it
                  embedChild(rep.get_window());
              } else {
                  // Reparented out of us - detach it
                  childDestroyed();
              }
              break;
        }
    }

    public Dimension getPreferredSize() {
        if (isXEmbedActive()) {
            XToolkit.awtLock();
            try {
                long p_hints = XlibWrapper.XAllocSizeHints();
                XSizeHints hints = new XSizeHints(p_hints);
                XlibWrapper.XGetWMNormalHints(XToolkit.getDisplay(), xembed.handle, p_hints, XlibWrapper.larg1);
                Dimension res = new Dimension(hints.get_width(), hints.get_height());
                XlibWrapper.XFree(p_hints);
                return res;
            } finally {
                XToolkit.awtUnlock();
            }
        } else {
            return super.getPreferredSize();
        }
    }
    public Dimension getMinimumSize() {
        if (isXEmbedActive()) {
            XToolkit.awtLock();
            try {
                long p_hints = XlibWrapper.XAllocSizeHints();
                XSizeHints hints = new XSizeHints(p_hints);
                XlibWrapper.XGetWMNormalHints(XToolkit.getDisplay(), xembed.handle, p_hints, XlibWrapper.larg1);
                Dimension res = new Dimension(hints.get_min_width(), hints.get_min_height());
                XlibWrapper.XFree(p_hints);
                return res;
            } finally {
                XToolkit.awtUnlock();
            }
        } else {
            return super.getMinimumSize();
        }
    }
    public void dispose() {
        if (isXEmbedActive()) {
            detachChild();
        }
        deinstallActivateListener();
        deinstallModalityListener();
        deinstallAcceleratorListener();

        // BUG: Focus traversal doesn't become enabled after the one round of embedding
        //target.setFocusTraversalKeysEnabled(true);

        super.dispose();
    }

    // Focusable is true in order to enable focus traversal through this Canvas
    public boolean isFocusable() {
        return true;
    }

    Window getTopLevel(Component comp) {
        while (comp != null && !(comp instanceof Window)) {
            comp = comp.getParent();
        }
        return (Window)comp;
    }

    Rectangle getClientBounds() {
        XToolkit.awtLock();
        try {
            XWindowAttributes wattr = new XWindowAttributes();
            try {
                XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                              xembed.handle, wattr.pData);

                XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                if ((status == 0) ||
                    ((XErrorHandlerUtil.saved_error != null) &&
                    (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success))) {
                    return null;
                }

                return new Rectangle(wattr.get_x(), wattr.get_y(), wattr.get_width(), wattr.get_height());
            } finally {
                wattr.dispose();
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    void childResized() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
            Rectangle bounds = getClientBounds();
            xembedLog.finer("Child resized: " + bounds);
            // It is not required to update embedder's size when client size changes
            // However, since there is no any means to get client size it seems to be the
            // only way to provide it. However, it contradicts with Java layout concept -
            // so it is disabled for now.
//             Rectangle my_bounds = getBounds();
//             setBounds(my_bounds.x, my_bounds.y, bounds.width, bounds.height, SET_BOUNDS);
        }
        XToolkit.postEvent(XToolkit.targetToAppContext(target), new ComponentEvent(target, ComponentEvent.COMPONENT_RESIZED));
    }

    void focusNext() {
        if (isXEmbedActive()) {
            xembedLog.fine("Requesting focus for the next component after embedder");
            postEvent(new InvocationEvent(target, new Runnable() {
                    public void run() {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(target);
                    }
                }));
        } else {
            xembedLog.fine("XEmbed is not active - denying focus next");
        }
    }

    void focusPrev() {
        if (isXEmbedActive()) {
            xembedLog.fine("Requesting focus for the next component after embedder");
            postEvent(new InvocationEvent(target, new Runnable() {
                    public void run() {
                        KeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(target);
                    }
                }));
        } else {
            xembedLog.fine("XEmbed is not active - denying focus prev");
        }
    }

    void requestXEmbedFocus() {
        if (isXEmbedActive()) {
            xembedLog.fine("Requesting focus for client");
            postEvent(new InvocationEvent(target, new Runnable() {
                    public void run() {
                        target.requestFocus();
                    }
                }));
        } else {
            xembedLog.fine("XEmbed is not active - denying request focus");
        }
    }

    void notifyChildEmbedded() {
        xembed.sendMessage(xembed.handle, XEMBED_EMBEDDED_NOTIFY, getWindow(), Math.min(xembed.version, XEMBED_VERSION), 0);
        if (isApplicationActive()) {
            xembedLog.fine("Sending WINDOW_ACTIVATE during initialization");
            xembed.sendMessage(xembed.handle, XEMBED_WINDOW_ACTIVATE);
            if (hasFocus()) {
                xembedLog.fine("Sending FOCUS_GAINED during initialization");
                xembed.sendMessage(xembed.handle, XEMBED_FOCUS_IN, XEMBED_FOCUS_CURRENT, 0, 0);
            }
        }
    }

    void detachChild() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine("Detaching child " + Long.toHexString(xembed.handle));
        }
        /**
         *  XEmbed specification:
         *  "The embedder can unmap the client and reparent the client window to the root window. If the
         *  client receives an ReparentNotify event, it should check the parent field of the XReparentEvent
         *  structure. If this is the root window of the window's screen, then the protocol is finished and
         *  there is no further interaction. If it is a window other than the root window, then the protocol
         *  continues with the new parent acting as the embedder window."
         */
        XToolkit.awtLock();
        try {
            XlibWrapper.XUnmapWindow(XToolkit.getDisplay(), xembed.handle);
            XlibWrapper.XReparentWindow(XToolkit.getDisplay(), xembed.handle, XToolkit.getDefaultRootWindow(), 0, 0);
        } finally {
            XToolkit.awtUnlock();
        }
        endDispatching();
        xembed.handle = 0;
    }

    public void windowGainedFocus(WindowEvent e) {
        applicationActive = true;
        if (isXEmbedActive()) {
            xembedLog.fine("Sending WINDOW_ACTIVATE");
            xembed.sendMessage(xembed.handle, XEMBED_WINDOW_ACTIVATE);
        }
    }

    public void windowLostFocus(WindowEvent e) {
        applicationActive = false;
        if (isXEmbedActive()) {
            xembedLog.fine("Sending WINDOW_DEACTIVATE");
            xembed.sendMessage(xembed.handle, XEMBED_WINDOW_DEACTIVATE);
        }
    }

    void canvasFocusGained(FocusEvent e) {
        if (isXEmbedActive()) {
            xembedLog.fine("Forwarding FOCUS_GAINED");
            int flavor = XEMBED_FOCUS_CURRENT;
            if (e.getCause() == FocusEvent.Cause.TRAVERSAL_FORWARD) {
                flavor = XEMBED_FOCUS_FIRST;
            } else if (e.getCause() == FocusEvent.Cause.TRAVERSAL_BACKWARD) {
                flavor = XEMBED_FOCUS_LAST;
            }
            xembed.sendMessage(xembed.handle, XEMBED_FOCUS_IN, flavor, 0, 0);
        }
    }

    @SuppressWarnings("removal")
    void canvasFocusLost(FocusEvent e) {
        if (isXEmbedActive() && !e.isTemporary()) {
            xembedLog.fine("Forwarding FOCUS_LOST");
            int num = 0;
            if (AccessController.doPrivileged(new GetBooleanAction("sun.awt.xembed.testing"))) {
                Component opp = e.getOppositeComponent();
                try {
                    num = Integer.parseInt(opp.getName());
                } catch (NumberFormatException nfe) {
                }
            }
            xembed.sendMessage(xembed.handle, XEMBED_FOCUS_OUT, num, 0, 0);
        }
    }

    static byte[] getBData(KeyEvent e) {
        return AWTAccessor.getAWTEventAccessor().getBData(e);
    }

    void forwardKeyEvent(KeyEvent e) {
        xembedLog.fine("Try to forward key event");
        byte[] bdata = getBData(e);
        long data = Native.toData(bdata);
        if (data == 0) {
            return;
        }
        try {
            XKeyEvent ke = new XKeyEvent(data);
            ke.set_window(xembed.handle);
            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                xembedLog.fine("Forwarding native key event: " + ke);
            }
            XToolkit.awtLock();
            try {
                XlibWrapper.XSendEvent(XToolkit.getDisplay(), xembed.handle, false, XConstants.NoEventMask, data);
            } finally {
                XToolkit.awtUnlock();
            }
        } finally {
            XlibWrapper.unsafe.freeMemory(data);
        }
    }


    /**
     * Grab/ungrab key functionality is an unofficial API supported by
     * GTK.  Unfortunately, it doesn't support accelerator API, so,
     * since this is the ONLY shortcut-processing API available, we
     * must support it.  See XEmbed.NON_STANDARD_XEMBED_GTK_*
     * messages.  The format of these messages is as follows:
     * - request from client:
     * data[1] = NON_STANDARD_XEMBED_GTK_GRAB_KEY or NON_STANDARD_XEMBED_GTK_UNGRAB_KEY
     * data[3] = X keysym
     * data[4] = X modifiers
     *
     * - response from server (in case the grabbed key has been pressed):
     * forwarded XKeyEvent that matches keysym/modifiers pair
     */
    void grabKey(final long keysym, final long modifiers) {
        postEvent(new InvocationEvent(target, new Runnable() {
                public void run() {
                    GrabbedKey grab = new GrabbedKey(keysym, modifiers);
                    if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                        xembedLog.fine("Grabbing key: " + grab);
                    }
                    synchronized(GRAB_LOCK) {
                        grabbed_keys.add(grab);
                    }
                }
            }));
    }

    void ungrabKey(final long keysym, final long modifiers) {
        postEvent(new InvocationEvent(target, new Runnable() {
                public void run() {
                    GrabbedKey grab = new GrabbedKey(keysym, modifiers);
                    if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                        xembedLog.fine("UnGrabbing key: " + grab);
                    }
                    synchronized(GRAB_LOCK) {
                        grabbed_keys.remove(grab);
                    }
                }
            }));
    }

    void registerAccelerator(final long accel_id, final long keysym, final long modifiers) {
        postEvent(new InvocationEvent(target, new Runnable() {
                public void run() {
                    AWTKeyStroke stroke = xembed.getKeyStrokeForKeySym(keysym, modifiers);
                    if (stroke != null) {
                        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                            xembedLog.fine("Registering accelerator " + accel_id + " for " + stroke);
                        }
                        synchronized(ACCEL_LOCK) {
                            accelerators.put(accel_id, stroke);
                            accel_lookup.put(stroke, accel_id);
                        }
                    }
                    propogateRegisterAccelerator(stroke);
                }
            }));
    }

    void unregisterAccelerator(final long accel_id) {
        postEvent(new InvocationEvent(target, new Runnable() {
                public void run() {
                    AWTKeyStroke stroke = null;
                    synchronized(ACCEL_LOCK) {
                        stroke = accelerators.get(accel_id);
                        if (stroke != null) {
                            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                                xembedLog.fine("Unregistering accelerator: " + accel_id);
                            }
                            accelerators.remove(accel_id);
                            accel_lookup.remove(stroke); // FIXME: How about several accelerators with the same stroke?
                        }
                    }
                    propogateUnRegisterAccelerator(stroke);
                }
            }));
    }

    void propogateRegisterAccelerator(AWTKeyStroke stroke) {
        // Find the top-level and see if it is XEmbed client. If so, ask him to
        // register the accelerator
        XWindowPeer parent = getToplevelXWindow();
        if (parent != null && parent instanceof XEmbeddedFramePeer) {
            XEmbeddedFramePeer embedded = (XEmbeddedFramePeer)parent;
            embedded.registerAccelerator(stroke);
        }
    }

    void propogateUnRegisterAccelerator(AWTKeyStroke stroke) {
        // Find the top-level and see if it is XEmbed client. If so, ask him to
        // register the accelerator
        XWindowPeer parent = getToplevelXWindow();
        if (parent != null && parent instanceof XEmbeddedFramePeer) {
            XEmbeddedFramePeer embedded = (XEmbeddedFramePeer)parent;
            embedded.unregisterAccelerator(stroke);
        }
    }

    public boolean postProcessKeyEvent(KeyEvent e) {
        // Processing events only if we are in the focused window but
        // we are not focus owner since otherwise we will get
        // duplicate shortcut events in the client - one is from
        // activate_accelerator, another from forwarded event
        // FIXME: This is probably an incompatibility, protocol
        // doesn't say anything about disable accelerators when client
        // is focused.

        XWindowPeer parent = getToplevelXWindow();
        if (parent == null || !((Window)parent.getTarget()).isFocused() || target.isFocusOwner()) {
            return false;
        }

        boolean result = false;

        if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
            xembedLog.finer("Post-processing event " + e);
        }

        // Process ACCELERATORS
        AWTKeyStroke stroke = AWTKeyStroke.getAWTKeyStrokeForEvent(e);
        long accel_id = 0;
        boolean exists = false;
        synchronized(ACCEL_LOCK) {
            exists = accel_lookup.containsKey(stroke);
            if (exists) {
                accel_id = accel_lookup.get(stroke).longValue();
            }
        }
        if (exists) {
            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                xembedLog.fine("Activating accelerator " + accel_id);
            }
            xembed.sendMessage(xembed.handle, XEMBED_ACTIVATE_ACCELERATOR, accel_id, 0, 0); // FIXME: How about overloaded?
            result = true;
        }

        // Process Grabs, unofficial GTK feature
        exists = false;
        GrabbedKey key = new GrabbedKey(e);
        synchronized(GRAB_LOCK) {
            exists = grabbed_keys.contains(key);
        }
        if (exists) {
            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                xembedLog.fine("Forwarding grabbed key " + e);
            }
            forwardKeyEvent(e);
            result = true;
        }

        return result;
    }

    public void modalityPushed(ModalityEvent ev) {
        xembed.sendMessage(xembed.handle, XEMBED_MODALITY_ON);
    }

    public void modalityPopped(ModalityEvent ev) {
        xembed.sendMessage(xembed.handle, XEMBED_MODALITY_OFF);
    }

    public void handleClientMessage(XEvent xev) {
        super.handleClientMessage(xev);
        XClientMessageEvent msg = xev.get_xclient();
        if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
            xembedLog.finer("Client message to embedder: " + msg);
        }
        if (msg.get_message_type() == XEmbedHelper.XEmbed.getAtom()) {
            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                xembedLog.fine(XEmbedHelper.XEmbedMessageToString(msg));
            }
        }
        if (isXEmbedActive()) {
            switch ((int)msg.get_data(1)) {
              case XEMBED_REQUEST_FOCUS:
                  requestXEmbedFocus();
                  break;
              case XEMBED_FOCUS_NEXT:
                  focusNext();
                  break;
              case XEMBED_FOCUS_PREV:
                  focusPrev();
                  break;
              case XEMBED_REGISTER_ACCELERATOR:
                  registerAccelerator(msg.get_data(2), msg.get_data(3), msg.get_data(4));
                  break;
              case XEMBED_UNREGISTER_ACCELERATOR:
                  unregisterAccelerator(msg.get_data(2));
                  break;
              case NON_STANDARD_XEMBED_GTK_GRAB_KEY:
                  grabKey(msg.get_data(3), msg.get_data(4));
                  break;
              case NON_STANDARD_XEMBED_GTK_UNGRAB_KEY:
                  ungrabKey(msg.get_data(3), msg.get_data(4));
                  break;
            }
        } else {
            xembedLog.finer("But XEmbed is not Active!");
        }
    }

    @SuppressWarnings("serial") // JDK-implementation class
    private static class XEmbedDropTarget extends DropTarget {
        public void addDropTargetListener(DropTargetListener dtl)
          throws TooManyListenersException {
            // Drop target listeners registered with this target will never be
            // notified, since all drag notifications are routed to the XEmbed
            // client. To avoid confusion we prohibit listeners registration
            // by throwing TooManyListenersException as if there is a listener
            // registered with this target already.
            throw new TooManyListenersException();
        }
    }

    public void setXEmbedDropTarget() {
        // Register a drop site on the top level.
        Runnable r = new Runnable() {
                public void run() {
                    target.setDropTarget(new XEmbedDropTarget());
                }
            };
        SunToolkit.executeOnEventHandlerThread(target, r);
    }

    public void removeXEmbedDropTarget() {
        // Unregister a drop site on the top level.
        Runnable r = new Runnable() {
                public void run() {
                    if (target.getDropTarget() instanceof XEmbedDropTarget) {
                        target.setDropTarget(null);
                    }
                }
            };
        SunToolkit.executeOnEventHandlerThread(target, r);
    }

    public boolean processXEmbedDnDEvent(long ctxt, int eventID) {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINEST)) {
            xembedLog.finest("     Drop target=" + target.getDropTarget());
        }
        if (target.getDropTarget() instanceof XEmbedDropTarget) {
            AppContext appContext = XToolkit.targetToAppContext(getTarget());
            XDropTargetContextPeer peer =
                XDropTargetContextPeer.getPeer(appContext);
            peer.forwardEventToEmbedded(xembed.handle, ctxt, eventID);
            return true;
        } else {
            return false;
        }
    }

    class XEmbedServer extends XEmbedHelper implements XEventDispatcher {
        long handle; // Handle to XEmbed client
        long version;
        long flags;

        boolean processXEmbedInfo() {
            long xembed_info_data = Native.allocateLongArray(2);
            try {
                if (!XEmbedInfo.getAtomData(handle, xembed_info_data, 2)) {
                    // No more XEMBED_INFO? This is not XEmbed client!
                    // Unfortunately this is the initial state of the most clients
                    // FIXME: add 5-state processing
                    //childDestroyed();
                    xembedLog.finer("Unable to get XEMBED_INFO atom data");
                    return false;
                }
                version = Native.getCard32(xembed_info_data, 0);
                flags = Native.getCard32(xembed_info_data, 1);
                boolean new_mapped = (flags & XEMBED_MAPPED) != 0;
                boolean currently_mapped = XlibUtil.getWindowMapState(handle) != XConstants.IsUnmapped;
                if (new_mapped != currently_mapped) {
                    if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                        xembedLog.finer("Mapping state of the client has changed, old state: " + currently_mapped + ", new state: " + new_mapped);
                    }
                    if (new_mapped) {
                        XToolkit.awtLock();
                        try {
                            XlibWrapper.XMapWindow(XToolkit.getDisplay(), handle);
                        } finally {
                            XToolkit.awtUnlock();
                        }
                    } else {
                        XToolkit.awtLock();
                        try {
                            XlibWrapper.XUnmapWindow(XToolkit.getDisplay(), handle);
                        } finally {
                            XToolkit.awtUnlock();
                        }
                    }
                } else {
                    if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                        xembedLog.finer("Mapping state didn't change, mapped: " + currently_mapped);
                    }
                }
                return true;
            } finally {
                XlibWrapper.unsafe.freeMemory(xembed_info_data);
            }
        }

        public void handlePropertyNotify(XEvent xev) {
            if (isXEmbedActive()) {
                XPropertyEvent ev = xev.get_xproperty();
                if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                    xembedLog.finer("Property change on client: " + ev);
                }
                if (ev.get_atom() == XAtom.XA_WM_NORMAL_HINTS) {
                    childResized();
                } else if (ev.get_atom() == XEmbedInfo.getAtom()) {
                    processXEmbedInfo();
                } else if (ev.get_atom() ==
                           XDnDConstants.XA_XdndAware.getAtom()) {
                    XDropTargetRegistry.getRegistry().unregisterXEmbedClient(getWindow(),
                                                                             xembed.handle);
                    if (ev.get_state() == XConstants.PropertyNewValue) {
                        XDropTargetRegistry.getRegistry().registerXEmbedClient(getWindow(),
                                                                                xembed.handle);
                    }
                }
            } else {
                xembedLog.finer("XEmbed is not active");
            }
        }
        void handleConfigureNotify(XEvent xev) {
            if (isXEmbedActive()) {
                XConfigureEvent ev = xev.get_xconfigure();
                if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                    xembedLog.finer("Bounds change on client: " + ev);
                }
                if (xev.get_xany().get_window() == handle) {
                    childResized();
                }
            }
        }
        public void dispatchEvent(XEvent xev) {
            int type = xev.get_type();
            switch (type) {
              case XConstants.PropertyNotify:
                  handlePropertyNotify(xev);
                  break;
              case XConstants.ConfigureNotify:
                  handleConfigureNotify(xev);
                  break;
              case XConstants.ClientMessage:
                  handleClientMessage(xev);
                  break;
            }
        }
    }

    static class GrabbedKey {
        long keysym;
        long modifiers;
        GrabbedKey(long keysym, long modifiers) {
            this.keysym = keysym;
            this.modifiers = modifiers;
        }

        GrabbedKey(KeyEvent ev) {
            init(ev);
        }

        private void init(KeyEvent e) {
            byte[] bdata = getBData(e);
            long data = Native.toData(bdata);
            if (data == 0) {
                return;
            }
            try {
                XToolkit.awtLock();
                try {
                    keysym = XWindow.getKeySymForAWTKeyCode(e.getKeyCode());
                } finally {
                    XToolkit.awtUnlock();
                }
                XKeyEvent ke = new XKeyEvent(data);

                // We recognize only these masks
                modifiers = ke.get_state() & (XConstants.ShiftMask | XConstants.ControlMask | XConstants.LockMask);
                if (xembedLog.isLoggable(PlatformLogger.Level.FINEST)) {
                    xembedLog.finest("Mapped " + e + " to " + this);
                }
            } finally {
                XlibWrapper.unsafe.freeMemory(data);
            }
        }

        public int hashCode() {
            return (int)keysym & 0xFFFFFFFF;
        }

        public boolean equals(Object o) {
            if (!(o instanceof GrabbedKey)) {
                return false;
            }
            GrabbedKey key = (GrabbedKey)o;
            return (keysym == key.keysym && modifiers == key.modifiers);
        }

        public String toString() {
            return "Key combination[keysym=" + keysym + ", mods=" + modifiers + "]";
        }
    }
}
