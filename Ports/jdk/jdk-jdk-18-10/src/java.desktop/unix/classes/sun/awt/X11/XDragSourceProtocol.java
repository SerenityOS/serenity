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

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.InvalidDnDOperationException;

import java.util.Map;

/**
 * An abstract class for drag protocols on X11 systems.
 * Contains protocol-independent drag source code.
 *
 * @since 1.5
 */
abstract class XDragSourceProtocol {
    private final XDragSourceProtocolListener listener;

    private boolean initialized = false;

    private long targetWindow = 0;
    private long targetProxyWindow = 0;
    private int targetProtocolVersion = 0;
    private long targetWindowMask = 0;

    // Always use the XAWT root window as the drag source window.
    static long getDragSourceWindow() {
        return XWindow.getXAWTRootWindow().getWindow();
    }

    protected XDragSourceProtocol(XDragSourceProtocolListener listener) {
        if (listener == null) {
            throw new NullPointerException("Null XDragSourceProtocolListener");
        }
        this.listener = listener;
    }

    protected final XDragSourceProtocolListener getProtocolListener() {
        return listener;
    }

    /**
     * Returns the protocol name. The protocol name cannot be null.
     */
    public abstract String getProtocolName();

    /**
     * Initializes a drag operation with the specified supported drop actions,
     * contents and data formats.
     *
     * @param actions a bitwise mask of {@code DnDConstants} that represent
     *                the supported drop actions.
     * @param contents the contents for the drag operation.
     * @param formats an array of Atoms that represent the supported data formats.
     * @param formats an array of Atoms that represent the supported data formats.
     * @throws InvalidDnDOperationException if a drag operation is already
     * initialized.
     * @throws IllegalArgumentException if some argument has invalid value.
     * @throws XException if some X call failed.
     */
    public final void initializeDrag(int actions, Transferable contents,
                                     Map<Long, DataFlavor> formatMap, long[] formats)
      throws InvalidDnDOperationException,
             IllegalArgumentException, XException {
        XToolkit.awtLock();
        try {
            try {
                if (initialized) {
                    throw new InvalidDnDOperationException("Already initialized");
                }

                initializeDragImpl(actions, contents, formatMap, formats);

                initialized = true;
            } finally {
                if (!initialized) {
                    cleanup();
                }
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /* The caller must hold AWT_LOCK. */
    protected abstract void initializeDragImpl(int actions,
                                               Transferable contents,
                                               Map<Long, DataFlavor> formatMap,
                                               long[] formats)
      throws InvalidDnDOperationException, IllegalArgumentException, XException;

    /**
     * Terminates the current drag operation (if any) and resets the internal
     * state of this object.
     *
     * @throws XException if some X call failed.
     */
    public void cleanup() {
        initialized = false;
        cleanupTargetInfo();
    }

    /**
     * Clears the information on the current drop target.
     *
     * @throws XException if some X call failed.
     */
    public void cleanupTargetInfo() {
        targetWindow = 0;
        targetProxyWindow = 0;
        targetProtocolVersion = 0;
    }

    /**
     * Processes the specified client message event.
     *
     * @return true if the event was successfully processed.
     */
    public abstract boolean processClientMessage(XClientMessageEvent xclient)
      throws XException;

    /* The caller must hold AWT_LOCK. */
    public final boolean attachTargetWindow(long window, long time) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        TargetWindowInfo info = getTargetWindowInfo(window);
        if (info == null) {
            return false;
        } else {
            targetWindow = window;
            targetProxyWindow = info.getProxyWindow();
            targetProtocolVersion = info.getProtocolVersion();
            return true;
        }
    }

    /* The caller must hold AWT_LOCK. */
    public abstract TargetWindowInfo getTargetWindowInfo(long window);

    /* The caller must hold AWT_LOCK. */
    public abstract void sendEnterMessage(long[] formats, int sourceAction,
                                          int sourceActions, long time);
    /* The caller must hold AWT_LOCK. */
    public abstract void sendMoveMessage(int xRoot, int yRoot,
                                         int sourceAction, int sourceActions,
                                         long time);
    /* The caller must hold AWT_LOCK. */
    public abstract void sendLeaveMessage(long time);

    /* The caller must hold AWT_LOCK. */
    protected abstract void sendDropMessage(int xRoot, int yRoot,
                                            int sourceAction, int sourceActions,
                                            long time);

    public final void initiateDrop(int xRoot, int yRoot,
                                   int sourceAction, int sourceActions,
                                   long time) {
        XWindowAttributes wattr = new XWindowAttributes();
        try {
            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
            int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                          targetWindow, wattr.pData);

            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((status == 0) ||
                ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success))) {
                throw new XException("XGetWindowAttributes failed");
            }

            targetWindowMask = wattr.get_your_event_mask();
        } finally {
            wattr.dispose();
        }

        XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
        XlibWrapper.XSelectInput(XToolkit.getDisplay(), targetWindow,
                                 targetWindowMask |
                                 XConstants.StructureNotifyMask);

        XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

        if ((XErrorHandlerUtil.saved_error != null) &&
            (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
            throw new XException("XSelectInput failed");
        }

        sendDropMessage(xRoot, yRoot, sourceAction, sourceActions, time);
    }

    protected final void finalizeDrop() {
        XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
        XlibWrapper.XSelectInput(XToolkit.getDisplay(), targetWindow,
                                 targetWindowMask);
        XErrorHandlerUtil.RESTORE_XERROR_HANDLER();
    }

    public abstract boolean processProxyModeEvent(XClientMessageEvent xclient,
                                                  long sourceWindow);

    protected final long getTargetWindow() {
        return targetWindow;
    }

    protected final long getTargetProxyWindow() {
        if (targetProxyWindow != 0) {
            return targetProxyWindow;
        } else {
            return targetWindow;
        }
    }

    protected final int getTargetProtocolVersion() {
        return targetProtocolVersion;
    }

    public static class TargetWindowInfo {
        private final long proxyWindow;
        private final int protocolVersion;
        public TargetWindowInfo(long proxy, int version) {
            proxyWindow = proxy;
            protocolVersion = version;
        }
        public long getProxyWindow() {
            return proxyWindow;
        }
        public int getProtocolVersion() {
            return protocolVersion;
        }
    }
}
