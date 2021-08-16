/*
 * Copyright (c) 2003, 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Point;

import java.awt.dnd.DnDConstants;

import java.awt.event.MouseEvent;

import java.io.IOException;

import jdk.internal.misc.Unsafe;

/**
 * XDropTargetProtocol implementation for Motif DnD protocol.
 *
 * @since 1.5
 */
class MotifDnDDropTargetProtocol extends XDropTargetProtocol {
    private static final Unsafe unsafe = XlibWrapper.unsafe;

    private long sourceWindow = 0;
    private long sourceWindowMask = 0;
    private int sourceProtocolVersion = 0;
    private int sourceActions = DnDConstants.ACTION_NONE;
    private long[] sourceFormats = null;
    private long sourceAtom = 0;
    private int userAction = DnDConstants.ACTION_NONE;
    private int sourceX = 0;
    private int sourceY = 0;
    private XWindow targetXWindow = null;
    private boolean topLevelLeavePostponed = false;

    protected MotifDnDDropTargetProtocol(XDropTargetProtocolListener listener) {
        super(listener);
    }

    /**
     * Creates an instance associated with the specified listener.
     *
     * @throws NullPointerException if listener is {@code null}.
     */
    static XDropTargetProtocol createInstance(XDropTargetProtocolListener listener) {
        return new MotifDnDDropTargetProtocol(listener);
    }

    public String getProtocolName() {
        return XDragAndDropProtocols.MotifDnD;
    }

    public void registerDropTarget(long window) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        MotifDnDConstants.writeDragReceiverInfoStruct(window);
    }

    public void unregisterDropTarget(long window) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        MotifDnDConstants.XA_MOTIF_ATOM_0.DeleteProperty(window);
    }

    public void registerEmbedderDropSite(long embedder) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        boolean overriden = false;
        int version = 0;
        long proxy = 0;
        long newProxy = XDropTargetRegistry.getDnDProxyWindow();
        int status = 0;
        long data = 0;
        int dataSize = MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE;

        WindowPropertyGetter wpg =
            new WindowPropertyGetter(embedder,
                                     MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO,
                                     0, 0xFFFF, false,
                                     XConstants.AnyPropertyType);

        try {
            status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

            /*
             * DragICCI.h:
             *
             * typedef struct _xmDragReceiverInfoStruct{
             *     BYTE byte_order;
             *     BYTE protocol_version;
             *     BYTE drag_protocol_style;
             *     BYTE pad1;
             *     CARD32       proxy_window B32;
             *     CARD16       num_drop_sites B16;
             *     CARD16       pad2 B16;
             *     CARD32       heap_offset B32;
             * } xmDragReceiverInfoStruct;
             */
            if (status == XConstants.Success && wpg.getData() != 0 &&
                wpg.getActualType() != 0 && wpg.getActualFormat() == 8 &&
                wpg.getNumberOfItems() >=
                MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE) {

                overriden = true;
                data = wpg.getData();
                dataSize = wpg.getNumberOfItems();

                byte byteOrderByte = unsafe.getByte(data);

                {
                    int tproxy = unsafe.getInt(data + 4);
                    if (byteOrderByte != MotifDnDConstants.getByteOrderByte()) {
                        tproxy = MotifDnDConstants.Swapper.swap(tproxy);
                    }
                    proxy = tproxy;
                }

                if (proxy == newProxy) {
                    // Embedder already registered.
                    return;
                }

                {
                    int tproxy = (int)newProxy;
                    if (byteOrderByte != MotifDnDConstants.getByteOrderByte()) {
                        tproxy = MotifDnDConstants.Swapper.swap(tproxy);
                    }
                    unsafe.putInt(data + 4, tproxy);
                }
            } else {
                data = unsafe.allocateMemory(dataSize);

                unsafe.putByte(data, MotifDnDConstants.getByteOrderByte()); /* byte order */
                unsafe.putByte(data + 1, MotifDnDConstants.MOTIF_DND_PROTOCOL_VERSION); /* protocol version */
                unsafe.putByte(data + 2, (byte)MotifDnDConstants.MOTIF_DYNAMIC_STYLE); /* protocol style */
                unsafe.putByte(data + 3, (byte)0); /* pad */
                unsafe.putInt(data + 4, (int)newProxy); /* proxy window */
                unsafe.putShort(data + 8, (short)0); /* num_drop_sites */
                unsafe.putShort(data + 10, (short)0); /* pad */
                unsafe.putInt(data + 12, dataSize);
            }

            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
            XlibWrapper.XChangeProperty(XToolkit.getDisplay(), embedder,
                                        MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO.getAtom(),
                                        MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO.getAtom(),
                                        8, XConstants.PropModeReplace,
                                        data, dataSize);
            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                throw new XException("Cannot write Motif receiver info property");
            }
        } finally {
            if (!overriden) {
                unsafe.freeMemory(data);
                data = 0;
            }
            wpg.dispose();
        }

        putEmbedderRegistryEntry(embedder, overriden, version, proxy);
    }

    public void unregisterEmbedderDropSite(long embedder) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        EmbedderRegistryEntry entry = getEmbedderRegistryEntry(embedder);

        if (entry == null) {
            return;
        }

        if (entry.isOverriden()) {
            int status = 0;

            WindowPropertyGetter wpg =
                new WindowPropertyGetter(embedder,
                                         MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO,
                                         0, 0xFFFF, false,
                                         XConstants.AnyPropertyType);

            try {
                status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

                /*
                 * DragICCI.h:
                 *
                 * typedef struct _xmDragReceiverInfoStruct{
                 *     BYTE     byte_order;
                 *     BYTE     protocol_version;
                 *     BYTE     drag_protocol_style;
                 *     BYTE     pad1;
                 *     CARD32   proxy_window B32;
                 *     CARD16   num_drop_sites B16;
                 *     CARD16   pad2 B16;
                 *     CARD32   heap_offset B32;
                 * } xmDragReceiverInfoStruct;
                 */
                if (status == XConstants.Success && wpg.getData() != 0 &&
                    wpg.getActualType() != 0 && wpg.getActualFormat() == 8 &&
                    wpg.getNumberOfItems() >=
                    MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE) {

                    int dataSize = MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE;
                    long data = wpg.getData();
                    byte byteOrderByte = unsafe.getByte(data);

                    int tproxy = (int)entry.getProxy();
                    if (MotifDnDConstants.getByteOrderByte() != byteOrderByte) {
                        tproxy = MotifDnDConstants.Swapper.swap(tproxy);
                    }

                    unsafe.putInt(data + 4, tproxy);

                    XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
                    XlibWrapper.XChangeProperty(XToolkit.getDisplay(), embedder,
                                                MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO.getAtom(),
                                                MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO.getAtom(),
                                                8, XConstants.PropModeReplace,
                                                data, dataSize);
                    XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                    if ((XErrorHandlerUtil.saved_error != null) &&
                        (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                        throw new XException("Cannot write Motif receiver info property");
                    }
                }
            } finally {
                wpg.dispose();
            }
        } else {
            MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO.DeleteProperty(embedder);
        }
    }

    /*
     * Gets and stores in the registry the embedder's Motif DnD drop site info
     * from the embedded.
     */
    public void registerEmbeddedDropSite(long embedded) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        boolean overriden = false;
        int version = 0;
        long proxy = 0;
        int status = 0;

        WindowPropertyGetter wpg =
            new WindowPropertyGetter(embedded,
                                     MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO,
                                     0, 0xFFFF, false,
                                     XConstants.AnyPropertyType);

        try {
            status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

            /*
             * DragICCI.h:
             *
             * typedef struct _xmDragReceiverInfoStruct{
             *     BYTE byte_order;
             *     BYTE protocol_version;
             *     BYTE drag_protocol_style;
             *     BYTE pad1;
             *     CARD32       proxy_window B32;
             *     CARD16       num_drop_sites B16;
             *     CARD16       pad2 B16;
             *     CARD32       heap_offset B32;
             * } xmDragReceiverInfoStruct;
             */
            if (status == XConstants.Success && wpg.getData() != 0 &&
                wpg.getActualType() != 0 && wpg.getActualFormat() == 8 &&
                wpg.getNumberOfItems() >=
                MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE) {

                overriden = true;
                long data = wpg.getData();

                byte byteOrderByte = unsafe.getByte(data);

                {
                    int tproxy = unsafe.getInt(data + 4);
                    if (byteOrderByte != MotifDnDConstants.getByteOrderByte()) {
                        tproxy = MotifDnDConstants.Swapper.swap(tproxy);
                    }
                    proxy = tproxy;
                }
            }
        } finally {
            wpg.dispose();
        }

        putEmbedderRegistryEntry(embedded, overriden, version, proxy);
    }

    public boolean isProtocolSupported(long window) {
        WindowPropertyGetter wpg =
            new WindowPropertyGetter(window,
                                     MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO,
                                     0, 0xFFFF, false,
                                     XConstants.AnyPropertyType);

        try {
            int status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

            if (status == XConstants.Success && wpg.getData() != 0 &&
                wpg.getActualType() != 0 && wpg.getActualFormat() == 8 &&
                wpg.getNumberOfItems() >=
                MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE) {
                return true;
            } else {
                return false;
            }
        } finally {
            wpg.dispose();
        }
    }

    private boolean processTopLevelEnter(XClientMessageEvent xclient) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        if (targetXWindow != null || sourceWindow != 0) {
            return false;
        }

        if (!(XToolkit.windowToXWindow(xclient.get_window()) instanceof XWindow)
            && getEmbedderRegistryEntry(xclient.get_window()) == null) {
            return false;
        }

        long source_win = 0;
        long source_win_mask = 0;
        int protocol_version = 0;
        long property_atom = 0;
        long[] formats = null;

        {
            long data = xclient.get_data();
            byte eventByteOrder = unsafe.getByte(data + 1);
            source_win = MotifDnDConstants.Swapper.getInt(data + 8, eventByteOrder);
            property_atom = MotifDnDConstants.Swapper.getInt(data + 12, eventByteOrder);
        }

        /* Extract the available data types. */
        {
            WindowPropertyGetter wpg =
                new WindowPropertyGetter(source_win,
                                         XAtom.get(property_atom),
                                         0, 0xFFFF,
                                         false,
                                         MotifDnDConstants.XA_MOTIF_DRAG_INITIATOR_INFO.getAtom());

            try {
                int status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

                if (status == XConstants.Success && wpg.getData() != 0 &&
                    wpg.getActualType() ==
                    MotifDnDConstants.XA_MOTIF_DRAG_INITIATOR_INFO.getAtom() &&
                    wpg.getActualFormat() == 8 &&
                    wpg.getNumberOfItems() ==
                    MotifDnDConstants.MOTIF_INITIATOR_INFO_SIZE) {

                    long data = wpg.getData();
                    byte propertyByteOrder = unsafe.getByte(data);

                    protocol_version = unsafe.getByte(data + 1);

                    if (protocol_version !=
                        MotifDnDConstants.MOTIF_DND_PROTOCOL_VERSION) {
                        return false;
                    }

                    int index =
                        MotifDnDConstants.Swapper.getShort(data + 2, propertyByteOrder);

                    formats = MotifDnDConstants.getTargetListForIndex(index);
                } else {
                    formats = new long[0];
                }
            } finally {
                wpg.dispose();
            }
        }

        /*
         * Select for StructureNotifyMask to receive DestroyNotify in case of source
         * crash.
         */
        XWindowAttributes wattr = new XWindowAttributes();
        try {
            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
            int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                          source_win, wattr.pData);

            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((status == 0) ||
                ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success))) {
                throw new XException("XGetWindowAttributes failed");
            }

            source_win_mask = wattr.get_your_event_mask();
        } finally {
            wattr.dispose();
        }

        XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
        XlibWrapper.XSelectInput(XToolkit.getDisplay(), source_win,
                                 source_win_mask |
                                 XConstants.StructureNotifyMask);

        XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

        if ((XErrorHandlerUtil.saved_error != null) &&
            (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
            throw new XException("XSelectInput failed");
        }

        sourceWindow = source_win;
        sourceWindowMask = source_win_mask;
        sourceProtocolVersion = protocol_version;
        /*
         * TOP_LEVEL_ENTER doesn't communicate the list of supported actions
         * They are provided in DRAG_MOTION.
         */
        sourceActions = DnDConstants.ACTION_NONE;
        sourceFormats = formats;
        sourceAtom = property_atom;

        return true;
    }

    private boolean processDragMotion(XClientMessageEvent xclient) {
        long data = xclient.get_data();
        byte eventByteOrder = unsafe.getByte(data + 1);
        byte eventReason = (byte)(unsafe.getByte(data) &
                                  MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);
        int x = 0;
        int y = 0;

        short flags = MotifDnDConstants.Swapper.getShort(data + 2, eventByteOrder);

        int motif_action = (flags & MotifDnDConstants.MOTIF_DND_ACTION_MASK) >>
            MotifDnDConstants.MOTIF_DND_ACTION_SHIFT;
        int motif_actions = (flags & MotifDnDConstants.MOTIF_DND_ACTIONS_MASK) >>
            MotifDnDConstants.MOTIF_DND_ACTIONS_SHIFT;

        int java_action = MotifDnDConstants.getJavaActionsForMotifActions(motif_action);
        int java_actions = MotifDnDConstants.getJavaActionsForMotifActions(motif_actions);

        /* Append source window id to the event data, so that we can send the
           response properly. */
        {
            int win = (int)sourceWindow;
            if (eventByteOrder != MotifDnDConstants.getByteOrderByte()) {
                win = MotifDnDConstants.Swapper.swap(win);
            }
            unsafe.putInt(data + 12, win);
        }

        XWindow xwindow = null;
        {
            XBaseWindow xbasewindow = XToolkit.windowToXWindow(xclient.get_window());
            if (xbasewindow instanceof XWindow) {
                xwindow = (XWindow)xbasewindow;
            }
        }

        if (eventReason == MotifDnDConstants.OPERATION_CHANGED) {
            /* OPERATION_CHANGED event doesn't provide coordinates, so we use
               previously stored position and component ref. */
            x = sourceX;
            y = sourceY;

            if (xwindow == null) {
                xwindow = targetXWindow;
            }
        } else {
            x = MotifDnDConstants.Swapper.getShort(data + 8, eventByteOrder);
            y = MotifDnDConstants.Swapper.getShort(data + 10, eventByteOrder);

            if (xwindow == null) {
                long receiver =
                    XDropTargetRegistry.getRegistry().getEmbeddedDropSite(
                        xclient.get_window(), x, y);

                if (receiver != 0) {
                    XBaseWindow xbasewindow = XToolkit.windowToXWindow(receiver);
                    if (xbasewindow instanceof XWindow) {
                        xwindow = (XWindow)xbasewindow;
                    }
                }
            }

            if (xwindow != null) {
                Point p = xwindow.toLocal(x, y);
                x = p.x;
                y = p.y;
            }
        }

        if (xwindow == null) {
            if (targetXWindow != null) {
                notifyProtocolListener(targetXWindow, x, y,
                                       DnDConstants.ACTION_NONE, java_actions,
                                       xclient, MouseEvent.MOUSE_EXITED);
            }
        } else {
            int java_event_id = 0;

            if (targetXWindow == null) {
                java_event_id = MouseEvent.MOUSE_ENTERED;
            } else {
                java_event_id = MouseEvent.MOUSE_DRAGGED;
            }

            notifyProtocolListener(xwindow, x, y, java_action, java_actions,
                                   xclient, java_event_id);
        }

        sourceActions = java_actions;
        userAction = java_action;
        sourceX = x;
        sourceY = y;
        targetXWindow = xwindow;

        return true;
    }

    private boolean processTopLevelLeave(XClientMessageEvent xclient) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        long data = xclient.get_data();
        byte eventByteOrder = unsafe.getByte(data + 1);

        long source_win = MotifDnDConstants.Swapper.getInt(data + 8, eventByteOrder);

        /* Ignore Motif DnD messages from all other windows. */
        if (source_win != sourceWindow) {
            return false;
        }

        /*
         * Postpone upcall to java, so that we can abort it in case
         * if drop immediatelly follows (see BugTraq ID 4395290).
         * Send a dummy ClientMessage event to guarantee that a postponed java
         * upcall will be processed.
         */
        topLevelLeavePostponed = true;
        {
            long proxy;

            /*
             * If this is an embedded drop site, the event should go to the
             * awt_root_window as this is a proxy for all embedded drop sites.
             * Otherwise the event should go to the event->window, as we don't use
             * proxies for normal drop sites.
             */
            if (getEmbedderRegistryEntry(xclient.get_window()) != null) {
                proxy = XDropTargetRegistry.getDnDProxyWindow();
            } else {
                proxy = xclient.get_window();
            }

            XClientMessageEvent dummy = new XClientMessageEvent();

            try {
                dummy.set_type(XConstants.ClientMessage);
                dummy.set_window(xclient.get_window());
                dummy.set_format(32);
                dummy.set_message_type(0);
                dummy.set_data(0, 0);
                dummy.set_data(1, 0);
                dummy.set_data(2, 0);
                dummy.set_data(3, 0);
                dummy.set_data(4, 0);
                XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                                       proxy, false, XConstants.NoEventMask,
                                       dummy.pData);
            } finally {
                dummy.dispose();
            }
        }
        return true;
    }

    private boolean processDropStart(XClientMessageEvent xclient) {
        long data = xclient.get_data();
        byte eventByteOrder = unsafe.getByte(data + 1);

        long source_win =
            MotifDnDConstants.Swapper.getInt(data + 16, eventByteOrder);

        /* Ignore Motif DnD messages from all other windows. */
        if (source_win != sourceWindow) {
            return false;
        }

        long property_atom =
            MotifDnDConstants.Swapper.getInt(data + 12, eventByteOrder);

        short flags =
            MotifDnDConstants.Swapper.getShort(data + 2, eventByteOrder);

        int motif_action = (flags & MotifDnDConstants.MOTIF_DND_ACTION_MASK) >>
            MotifDnDConstants.MOTIF_DND_ACTION_SHIFT;
        int motif_actions = (flags & MotifDnDConstants.MOTIF_DND_ACTIONS_MASK) >>
            MotifDnDConstants.MOTIF_DND_ACTIONS_SHIFT;

        int java_action = MotifDnDConstants.getJavaActionsForMotifActions(motif_action);
        int java_actions = MotifDnDConstants.getJavaActionsForMotifActions(motif_actions);

        int x = MotifDnDConstants.Swapper.getShort(data + 8, eventByteOrder);
        int y = MotifDnDConstants.Swapper.getShort(data + 10, eventByteOrder);

        XWindow xwindow = null;
        {
            XBaseWindow xbasewindow = XToolkit.windowToXWindow(xclient.get_window());
            if (xbasewindow instanceof XWindow) {
                xwindow = (XWindow)xbasewindow;
            }
        }

        if (xwindow == null) {
            long receiver =
                XDropTargetRegistry.getRegistry().getEmbeddedDropSite(
                    xclient.get_window(), x, y);

            if (receiver != 0) {
                XBaseWindow xbasewindow = XToolkit.windowToXWindow(receiver);
                if (xbasewindow instanceof XWindow) {
                    xwindow = (XWindow)xbasewindow;
                }
            }
        }

        if (xwindow != null) {
            Point p = xwindow.toLocal(x, y);
            x = p.x;
            y = p.y;
        }

        if (xwindow != null) {
            notifyProtocolListener(xwindow, x, y, java_action, java_actions,
                                   xclient, MouseEvent.MOUSE_RELEASED);
        } else if (targetXWindow != null) {
            notifyProtocolListener(targetXWindow, x, y,
                                   DnDConstants.ACTION_NONE, java_actions,
                                   xclient, MouseEvent.MOUSE_EXITED);
        }

        return true;
    }

    public int getMessageType(XClientMessageEvent xclient) {
        if (xclient.get_message_type() !=
            MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom()) {

            return UNKNOWN_MESSAGE;
        }

        long data = xclient.get_data();
        byte reason = (byte)(unsafe.getByte(data) &
                             MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);

        switch (reason) {
        case MotifDnDConstants.TOP_LEVEL_ENTER :
            return ENTER_MESSAGE;
        case MotifDnDConstants.DRAG_MOTION :
        case MotifDnDConstants.OPERATION_CHANGED :
            return MOTION_MESSAGE;
        case MotifDnDConstants.TOP_LEVEL_LEAVE :
            return LEAVE_MESSAGE;
        case MotifDnDConstants.DROP_START :
            return DROP_MESSAGE;
        default:
            return UNKNOWN_MESSAGE;
        }
    }

    protected boolean processClientMessageImpl(XClientMessageEvent xclient) {
        if (xclient.get_message_type() !=
            MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom()) {
            if (topLevelLeavePostponed) {
                topLevelLeavePostponed = false;
                cleanup();
            }

            return false;
        }

        long data = xclient.get_data();
        byte reason = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);
        byte origin = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_SENDER_MASK);

        if (topLevelLeavePostponed) {
            topLevelLeavePostponed = false;
            if (reason != MotifDnDConstants.DROP_START) {
                cleanup();
            }
        }

        /* Only initiator messages should be handled. */
        if (origin != MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR) {
            return false;
        }

        switch (reason) {
        case MotifDnDConstants.TOP_LEVEL_ENTER :
            return processTopLevelEnter(xclient);
        case MotifDnDConstants.DRAG_MOTION :
        case MotifDnDConstants.OPERATION_CHANGED :
            return processDragMotion(xclient);
        case MotifDnDConstants.TOP_LEVEL_LEAVE :
            return processTopLevelLeave(xclient);
        case MotifDnDConstants.DROP_START :
            return processDropStart(xclient);
        default:
            return false;
        }
    }

    /*
     * Currently we don't synthesize enter/leave messages for Motif DnD
     * protocol. See comments in XDropTargetProtocol.postProcessClientMessage.
     */
    protected void sendEnterMessageToToplevel(long win,
                                              XClientMessageEvent xclient) {
        throw new Error("UNIMPLEMENTED");
    }

    protected void sendLeaveMessageToToplevel(long win,
                                              XClientMessageEvent xclient) {
        throw new Error("UNIMPLEMENTED");
    }

    public boolean forwardEventToEmbedded(long embedded, long ctxt,
                                          int eventID) {
        // UNIMPLEMENTED.
        return false;
    }

    public boolean isXEmbedSupported() {
        return false;
    }

    public boolean sendResponse(long ctxt, int eventID, int action) {
        XClientMessageEvent xclient = new XClientMessageEvent(ctxt);
        if (xclient.get_message_type() !=
            MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom()) {
            return false;
        }

        long data = xclient.get_data();
        byte reason = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);
        byte origin = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_SENDER_MASK);
        byte eventByteOrder = unsafe.getByte(data + 1);
        byte response_reason = (byte)0;

        /* Only initiator messages should be handled. */
        if (origin != MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR) {
            return false;
        }

        switch (reason) {
        case MotifDnDConstants.TOP_LEVEL_ENTER:
        case MotifDnDConstants.TOP_LEVEL_LEAVE:
            /* Receiver shouldn't rely to these messages. */
            return false;
        case MotifDnDConstants.DRAG_MOTION:
            switch (eventID) {
            case MouseEvent.MOUSE_ENTERED:
                response_reason = MotifDnDConstants.DROP_SITE_ENTER;
                break;
            case MouseEvent.MOUSE_DRAGGED:
                response_reason = MotifDnDConstants.DRAG_MOTION;
                break;
            case MouseEvent.MOUSE_EXITED:
                response_reason = MotifDnDConstants.DROP_SITE_LEAVE;
                break;
            }
            break;
        case MotifDnDConstants.OPERATION_CHANGED:
        case MotifDnDConstants.DROP_START:
            response_reason = reason;
            break;
        default:
            // Unknown reason. Shouldn't get here.
            assert false;
        }

        XClientMessageEvent msg = new XClientMessageEvent();

        try {
            msg.set_type(XConstants.ClientMessage);
            msg.set_window(MotifDnDConstants.Swapper.getInt(data + 12, eventByteOrder));
            msg.set_format(8);
            msg.set_message_type(MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom());

            long responseData = msg.get_data();

            unsafe.putByte(responseData, (byte)(response_reason |
                           MotifDnDConstants.MOTIF_MESSAGE_FROM_RECEIVER));
            unsafe.putByte(responseData + 1, MotifDnDConstants.getByteOrderByte());

            int response_flags = 0;

            if (response_reason != MotifDnDConstants.DROP_SITE_LEAVE) {
                short flags = MotifDnDConstants.Swapper.getShort(data + 2,
                                                                 eventByteOrder);
                byte dropSiteStatus = (action == DnDConstants.ACTION_NONE) ?
                    MotifDnDConstants.MOTIF_INVALID_DROP_SITE :
                    MotifDnDConstants.MOTIF_VALID_DROP_SITE;

                /* Clear action and drop site status bits. */
                response_flags = flags &
                    ~MotifDnDConstants.MOTIF_DND_ACTION_MASK &
                    ~MotifDnDConstants.MOTIF_DND_STATUS_MASK;
                /* Fill in new action and drop site status. */
                response_flags |=
                    MotifDnDConstants.getMotifActionsForJavaActions(action) <<
                    MotifDnDConstants.MOTIF_DND_ACTION_SHIFT;
                response_flags |=
                    dropSiteStatus << MotifDnDConstants.MOTIF_DND_STATUS_SHIFT;
            } else {
                response_flags = 0;
            }

            unsafe.putShort(responseData + 2, (short)response_flags);

            /* Write time stamp. */
            int time = MotifDnDConstants.Swapper.getInt(data + 4, eventByteOrder);
            unsafe.putInt(responseData + 4, time);

            /* Write coordinates. */
            if (response_reason != MotifDnDConstants.DROP_SITE_LEAVE) {
                short x = MotifDnDConstants.Swapper.getShort(data + 8,
                                                             eventByteOrder);
                short y = MotifDnDConstants.Swapper.getShort(data + 10,
                                                             eventByteOrder);
                unsafe.putShort(responseData + 8, x); // x
                unsafe.putShort(responseData + 10, y); // y
            } else {
                unsafe.putShort(responseData + 8, (short)0); // x
                unsafe.putShort(responseData + 10, (short)0); // y
            }

            XToolkit.awtLock();
            try {
                XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                                       msg.get_window(),
                                       false, XConstants.NoEventMask,
                                       msg.pData);
            } finally {
                XToolkit.awtUnlock();
            }
        } finally {
            msg.dispose();
        }

        return true;
    }

    public Object getData(long ctxt, long format)
      throws IllegalArgumentException, IOException {
        XClientMessageEvent xclient = new XClientMessageEvent(ctxt);

        if (xclient.get_message_type() !=
            MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom()) {
            throw new IllegalArgumentException();
        }

        long data = xclient.get_data();
        byte reason = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);
        byte origin = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_SENDER_MASK);
        byte eventByteOrder = unsafe.getByte(data + 1);

        if (origin != MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR) {
            throw new IOException("Cannot get data: corrupted context");
        }

        long selatom = 0;

        switch (reason) {
        case MotifDnDConstants.DRAG_MOTION :
        case MotifDnDConstants.OPERATION_CHANGED :
            selatom = sourceAtom;
            break;
        case MotifDnDConstants.DROP_START :
            selatom = MotifDnDConstants.Swapper.getInt(data + 12, eventByteOrder);
            break;
        default:
            throw new IOException("Cannot get data: invalid message reason");
        }

        if (selatom == 0) {
            throw new IOException("Cannot get data: drag source property atom unavailable");
        }

        long time_stamp = MotifDnDConstants.Swapper.getInt(data + 4, eventByteOrder) & 0xffffffffL;
                          // with correction of (32-bit unsigned to 64-bit signed) implicit conversion.

        XAtom selectionAtom = XAtom.get(selatom);

        XSelection selection = XSelection.getSelection(selectionAtom);
        if (selection == null) {
            selection = new XSelection(selectionAtom);
        }

        return selection.getData(format, time_stamp);
    }

    public boolean sendDropDone(long ctxt, boolean success, int dropAction) {
        XClientMessageEvent xclient = new XClientMessageEvent(ctxt);

        if (xclient.get_message_type() !=
            MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom()) {
            return false;
        }

        long data = xclient.get_data();
        byte reason = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);
        byte origin = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_SENDER_MASK);
        byte eventByteOrder = unsafe.getByte(data + 1);

        if (origin != MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR) {
            return false;
        }

        if (reason != MotifDnDConstants.DROP_START) {
            return false;
        }

        long time_stamp = MotifDnDConstants.Swapper.getInt(data + 4, eventByteOrder) & 0xffffffffL;
                          // with correction of (32-bit unsigned to 64-bit signed) implicit conversion.

        long sel_atom = MotifDnDConstants.Swapper.getInt(data + 12, eventByteOrder);

        long status_atom = 0;

        if (success) {
            status_atom = MotifDnDConstants.XA_XmTRANSFER_SUCCESS.getAtom();
        } else {
            status_atom = MotifDnDConstants.XA_XmTRANSFER_FAILURE.getAtom();
        }

        XToolkit.awtLock();
        try {
            XlibWrapper.XConvertSelection(XToolkit.getDisplay(),
                                          sel_atom,
                                          status_atom,
                                          MotifDnDConstants.XA_MOTIF_ATOM_0.getAtom(),
                                          XWindow.getXAWTRootWindow().getWindow(),
                                          time_stamp);

            /*
             * Flush the buffer to guarantee that the drop completion event is sent
             * to the source before the method returns.
             */
            XlibWrapper.XFlush(XToolkit.getDisplay());
        } finally {
            XToolkit.awtUnlock();
        }

        /* Trick to prevent cleanup() from posting dragExit */
        targetXWindow = null;

        /* Cannot do cleanup before the drop finishes as we may need
           source protocol version to send drop finished message. */
        cleanup();
        return true;
    }

    public final long getSourceWindow() {
        return sourceWindow;
    }

    /**
     * Reset the state of the object.
     */
    public void cleanup() {
        // Clear the reference to this protocol.
        XDropTargetEventProcessor.reset();

        if (targetXWindow != null) {
            notifyProtocolListener(targetXWindow, 0, 0,
                                   DnDConstants.ACTION_NONE, sourceActions,
                                   null, MouseEvent.MOUSE_EXITED);
        }

        if (sourceWindow != 0) {
            XToolkit.awtLock();
            try {
                XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                XlibWrapper.XSelectInput(XToolkit.getDisplay(), sourceWindow,
                                         sourceWindowMask);
                XErrorHandlerUtil.RESTORE_XERROR_HANDLER();
            } finally {
                XToolkit.awtUnlock();
            }
        }

        sourceWindow = 0;
        sourceWindowMask = 0;
        sourceProtocolVersion = 0;
        sourceActions = DnDConstants.ACTION_NONE;
        sourceFormats = null;
        sourceAtom = 0;
        userAction = DnDConstants.ACTION_NONE;
        sourceX = 0;
        sourceY = 0;
        targetXWindow = null;
        topLevelLeavePostponed = false;
    }

    public boolean isDragOverComponent() {
        return targetXWindow != null;
    }

    private void notifyProtocolListener(XWindow xwindow, int x, int y,
                                        int dropAction, int actions,
                                        XClientMessageEvent xclient,
                                        int eventID) {
        long nativeCtxt = 0;

        // Make a copy of the passed XClientMessageEvent structure, since
        // the original structure can be freed before this
        // SunDropTargetEvent is dispatched.
        if (xclient != null) {
            int size = XClientMessageEvent.getSize();

            nativeCtxt = unsafe.allocateMemory(size + 4 * Native.getLongSize());

            unsafe.copyMemory(xclient.pData, nativeCtxt, size);
        }

        getProtocolListener().handleDropTargetNotification(xwindow, x, y,
                                                           dropAction,
                                                           actions,
                                                           sourceFormats,
                                                           nativeCtxt,
                                                           eventID);
    }
}
