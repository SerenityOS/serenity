/*
 * Copyright (c) 2003, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTKeyStroke;
import sun.awt.SunToolkit;
import java.awt.Component;
import java.awt.Container;
import sun.util.logging.PlatformLogger;

import sun.awt.X11GraphicsConfig;
import sun.awt.X11GraphicsDevice;

/**
 * Helper class implementing XEmbed protocol handling routines(client side)
 * Window which wants to participate in a protocol should create an instance,
 * call install and forward all XClientMessageEvents to it.
 */
public class XEmbedClientHelper extends XEmbedHelper implements XEventDispatcher {
    private static final PlatformLogger xembedLog = PlatformLogger.getLogger("sun.awt.X11.xembed.XEmbedClientHelper");

    private XEmbeddedFramePeer embedded; // XEmbed client
    private long server; // XEmbed server

    private boolean active;
    private boolean applicationActive;

    XEmbedClientHelper() {
        super();
    }

    void setClient(XEmbeddedFramePeer client) {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine("XEmbed client: " + client);
        }
        if (embedded != null) {
            XToolkit.removeEventDispatcher(embedded.getWindow(), this);
            active = false;
        }
        embedded = client;
        if (embedded != null) {
            XToolkit.addEventDispatcher(embedded.getWindow(), this);
        }
    }

    void install() {
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine("Installing xembedder on " + embedded);
        }
        long[] info = new long[] { XEMBED_VERSION, XEMBED_MAPPED };
        long data = Native.card32ToData(info);
        try {
            XEmbedInfo.setAtomData(embedded.getWindow(), data, 2);
        } finally {
            unsafe.freeMemory(data);
        }
        // XEmbeddedFrame is initially created with a null parent..
        // Here it is reparented to the proper parent window.
        long parentWindow = embedded.getParentWindowHandle();
        if (parentWindow != 0) {
            XToolkit.awtLock();
            try {
                XlibWrapper.XReparentWindow(XToolkit.getDisplay(),
                                            embedded.getWindow(),
                                            parentWindow,
                                            0, 0);
            } finally {
                XToolkit.awtUnlock();
            }
        }
    }

    void handleClientMessage(XEvent xev) {
        XClientMessageEvent msg = xev.get_xclient();
        if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
            xembedLog.fine(msg.toString());
        }
        if (msg.get_message_type() == XEmbed.getAtom()) {
            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                xembedLog.fine("Embedded message: " + msgidToString((int)msg.get_data(1)));
            }
            switch ((int)msg.get_data(1)) {
              case XEMBED_EMBEDDED_NOTIFY: // Notification about embedding protocol start
                  active = true;
                  server = getEmbedder(embedded, msg);
                  // Check if window is reparented. If not - it was created with
                  // parent and so we should update it here.
                  if (!embedded.isReparented()) {
                      embedded.setReparented(true);
                      embedded.updateSizeHints();
                  }
                  embedded.notifyStarted();
                  break;
              case XEMBED_WINDOW_ACTIVATE:
                  applicationActive = true;
                  break;
              case XEMBED_WINDOW_DEACTIVATE:
                  if (applicationActive) {
                      applicationActive = false;
                      handleWindowFocusOut();
                  }
                  break;
              case XEMBED_FOCUS_IN: // We got focus!
                  // Check for direction
                  handleFocusIn((int)msg.get_data(2));
                  break;
              case XEMBED_FOCUS_OUT:
                  if (applicationActive) {
                      handleWindowFocusOut();
                  }
                  break;
            }
        }
    }
    void handleFocusIn(int detail) {
        if (embedded.focusAllowedFor()) {
            embedded.handleWindowFocusIn(0);
        }
        switch(detail) {
          case XEMBED_FOCUS_CURRENT:
              // Do nothing - just restore to the current value
              break;
          case XEMBED_FOCUS_FIRST:
              SunToolkit.executeOnEventHandlerThread(embedded.target, new Runnable() {
                      public void run() {
                          Component comp = ((Container)embedded.target).getFocusTraversalPolicy().getFirstComponent((Container)embedded.target);
                          if (comp != null) {
                              comp.requestFocusInWindow();
                          }
                      }});
              break;
          case XEMBED_FOCUS_LAST:
              SunToolkit.executeOnEventHandlerThread(embedded.target, new Runnable() {
                      public void run() {
                          Component comp = ((Container)embedded.target).getFocusTraversalPolicy().getLastComponent((Container)embedded.target);
                          if (comp != null) {
                              comp.requestFocusInWindow();
                          }
                      }});
              break;
        }
    }

    public void dispatchEvent(XEvent xev) {
        switch(xev.get_type()) {
          case XConstants.ClientMessage:
              handleClientMessage(xev);
              break;
          case XConstants.ReparentNotify:
              handleReparentNotify(xev);
              break;
        }
    }
    public void handleReparentNotify(XEvent xev) {
        XReparentEvent re = xev.get_xreparent();
        long newParent = re.get_parent();
        if (active) {
            // unregister accelerators, etc. for old parent
            embedded.notifyStopped();
            // check if newParent is a root window
            X11GraphicsConfig gc = (X11GraphicsConfig)embedded.getGraphicsConfiguration();
            X11GraphicsDevice gd = gc.getDevice();
            if ((newParent == XlibUtil.getRootWindow(gd.getScreen())) ||
                (newParent == XToolkit.getDefaultRootWindow()))
            {
                // reparenting to root means XEmbed termination
                active = false;
            } else {
                // continue XEmbed with a new parent
                server = newParent;
                embedded.notifyStarted();
            }
        }
    }
    boolean requestFocus() {
        if (active && embedded.focusAllowedFor()) {
            sendMessage(server, XEMBED_REQUEST_FOCUS);
            return true;
        }
        return false;
    }
    void handleWindowFocusOut() {
        // fix for 6269309: it is possible that we call this method twice
        // (for example, when receiving XEMBED_WINDOW_DEACTIVATE and then
        // XEMBED_FOCUS_OUT client messages), so we first need to check if
        // embedded is an active window before sending WINDOW_LOST_FOCUS
        // to shared code
        if (XKeyboardFocusManagerPeer.getInstance().getCurrentFocusedWindow() == embedded.target) {
            embedded.handleWindowFocusOut(null, 0);
        }
    }

    long getEmbedder(XWindowPeer embedded, XClientMessageEvent info) {
        // Embedder is the parent of embedded.
        return XlibUtil.getParentWindow(embedded.getWindow());
    }

    boolean isApplicationActive() {
        return applicationActive;
    }

    boolean isActive() {
        return active;
    }

    void traverseOutForward() {
        if (active) {
            sendMessage(server, XEMBED_FOCUS_NEXT);
        }
    }

    void traverseOutBackward() {
        if (active) {
            sendMessage(server, XEMBED_FOCUS_PREV);
        }
    }

    void registerAccelerator(AWTKeyStroke stroke, int id) {
        if (active) {
            long sym = getX11KeySym(stroke);
            long mods = getX11Mods(stroke);
            sendMessage(server, XEMBED_REGISTER_ACCELERATOR, id, sym, mods);
        }
    }
    void unregisterAccelerator(int id) {
        if (active) {
            sendMessage(server, XEMBED_UNREGISTER_ACCELERATOR, id, 0, 0);
        }
    }

    long getX11KeySym(AWTKeyStroke stroke) {
        XToolkit.awtLock();
        try {
            return XWindow.getKeySymForAWTKeyCode(stroke.getKeyCode());
        } finally {
            XToolkit.awtUnlock();
        }
    }

    long getX11Mods(AWTKeyStroke stroke) {
        return XWindow.getXModifiers(stroke);
    }
}
