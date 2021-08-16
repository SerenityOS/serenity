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

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;

import java.awt.dnd.DnDConstants;
import java.awt.dnd.InvalidDnDOperationException;

import java.util.Map;

import jdk.internal.misc.Unsafe;

/**
 * XDragSourceProtocol implementation for Motif DnD protocol.
 *
 * @since 1.5
 */
class MotifDnDDragSourceProtocol extends XDragSourceProtocol
    implements XEventDispatcher {

    private static final Unsafe unsafe = XlibWrapper.unsafe;

    private long targetEnterServerTime = XConstants.CurrentTime;

    protected MotifDnDDragSourceProtocol(XDragSourceProtocolListener listener) {
        super(listener);
        XToolkit.addEventDispatcher(XWindow.getXAWTRootWindow().getWindow(), this);
    }

    /**
     * Creates an instance associated with the specified listener.
     *
     * @throws NullPointerException if listener is {@code null}.
     */
    static XDragSourceProtocol createInstance(XDragSourceProtocolListener listener) {
        return new MotifDnDDragSourceProtocol(listener);
    }

    public String getProtocolName() {
        return XDragAndDropProtocols.MotifDnD;
    }

    protected void initializeDragImpl(int actions, Transferable contents,
                                      Map<Long, DataFlavor> formatMap, long[] formats)
      throws InvalidDnDOperationException,
        IllegalArgumentException, XException {

        long window = XDragSourceProtocol.getDragSourceWindow();

        /* Write the Motif DnD initiator info on the root XWindow. */
        try {
            int index = MotifDnDConstants.getIndexForTargetList(formats);

            MotifDnDConstants.writeDragInitiatorInfoStruct(window, index);
        } catch (XException xe) {
            cleanup();
            throw xe;
        } catch (InvalidDnDOperationException idoe) {
            cleanup();
            throw idoe;
        }

        if (!MotifDnDConstants.MotifDnDSelection.setOwner(contents, formatMap,
                                                          formats,
                                                          XConstants.CurrentTime)) {
            cleanup();
            throw new InvalidDnDOperationException("Cannot acquire selection ownership");
        }
    }

    /**
     * Processes the specified client message event.
     *
     * @return true if the event was successfully processed.
     */
    public boolean processClientMessage(XClientMessageEvent xclient) {
        if (xclient.get_message_type() !=
            MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom()) {
            return false;
        }

        long data = xclient.get_data();
        byte reason = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_REASON_MASK);
        byte origin = (byte)(unsafe.getByte(data) &
            MotifDnDConstants.MOTIF_MESSAGE_SENDER_MASK);
        byte byteOrder = unsafe.getByte(data + 1);
        boolean swapNeeded = byteOrder != MotifDnDConstants.getByteOrderByte();
        int action = DnDConstants.ACTION_NONE;
        int x = 0;
        int y = 0;

        /* Only receiver messages should be handled. */
        if (origin != MotifDnDConstants.MOTIF_MESSAGE_FROM_RECEIVER) {
            return false;
        }

        switch (reason) {
        case MotifDnDConstants.DROP_SITE_ENTER:
        case MotifDnDConstants.DROP_SITE_LEAVE:
        case MotifDnDConstants.DRAG_MOTION:
        case MotifDnDConstants.OPERATION_CHANGED:
            break;
        default:
            // Unknown reason.
            return false;
        }

        int t = unsafe.getInt(data + 4);
        if (swapNeeded) {
            t = MotifDnDConstants.Swapper.swap(t);
        }
        long time = t & 0xffffffffL;
             // with correction of (32-bit unsigned to 64-bit signed) implicit conversion.

        /* Discard events from the previous receiver. */
        if (targetEnterServerTime == XConstants.CurrentTime ||
            time < targetEnterServerTime) {
            return true;
        }

        if (reason != MotifDnDConstants.DROP_SITE_LEAVE) {
            short flags = unsafe.getShort(data + 2);
            if (swapNeeded) {
                flags = MotifDnDConstants.Swapper.swap(flags);
            }

            byte status = (byte)((flags & MotifDnDConstants.MOTIF_DND_STATUS_MASK) >>
                MotifDnDConstants.MOTIF_DND_STATUS_SHIFT);
            byte motif_action = (byte)((flags & MotifDnDConstants.MOTIF_DND_ACTION_MASK) >>
                MotifDnDConstants.MOTIF_DND_ACTION_SHIFT);

            if (status == MotifDnDConstants.MOTIF_VALID_DROP_SITE) {
                action = MotifDnDConstants.getJavaActionsForMotifActions(motif_action);
            } else {
                action = DnDConstants.ACTION_NONE;
            }

            short tx = unsafe.getShort(data + 8);
            short ty = unsafe.getShort(data + 10);
            if (swapNeeded) {
                tx = MotifDnDConstants.Swapper.swap(tx);
                ty = MotifDnDConstants.Swapper.swap(ty);
            }
            x = tx;
            y = ty;
        }

        getProtocolListener().handleDragReply(action, x, y);

        return true;
    }

    public TargetWindowInfo getTargetWindowInfo(long window) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        WindowPropertyGetter wpg =
            new WindowPropertyGetter(window,
                                     MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO,
                                     0, 0xFFFF, false,
                                     XConstants.AnyPropertyType);

        try {
            int status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

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

                long data = wpg.getData();
                byte byteOrderByte = unsafe.getByte(data);
                byte dragProtocolStyle = unsafe.getByte(data + 2);
                switch (dragProtocolStyle) {
                case MotifDnDConstants.MOTIF_PREFER_PREREGISTER_STYLE :
                case MotifDnDConstants.MOTIF_PREFER_DYNAMIC_STYLE :
                case MotifDnDConstants.MOTIF_DYNAMIC_STYLE :
                case MotifDnDConstants.MOTIF_PREFER_RECEIVER_STYLE :
                    int proxy = unsafe.getInt(data + 4);
                    if (byteOrderByte != MotifDnDConstants.getByteOrderByte()) {
                        proxy = MotifDnDConstants.Swapper.swap(proxy);
                    }

                    int protocolVersion = unsafe.getByte(data + 1);

                    return new TargetWindowInfo(proxy, protocolVersion);
                default:
                    // Unsupported protocol style.
                    return null;
                }
            } else {
                return null;
            }
        } finally {
            wpg.dispose();
        }
    }

    public void sendEnterMessage(long[] formats,
                                 int sourceAction, int sourceActions, long time) {
        assert XToolkit.isAWTLockHeldByCurrentThread();
        assert getTargetWindow() != 0;
        assert formats != null;

        targetEnterServerTime = time;

        XClientMessageEvent msg = new XClientMessageEvent();
        try {
            msg.set_type(XConstants.ClientMessage);
            msg.set_window(getTargetWindow());
            msg.set_format(8);
            msg.set_message_type(MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom());

            long data = msg.get_data();
            int flags =
                (MotifDnDConstants.getMotifActionsForJavaActions(sourceAction) <<
                 MotifDnDConstants.MOTIF_DND_ACTION_SHIFT) |
                (MotifDnDConstants.getMotifActionsForJavaActions(sourceActions) <<
                 MotifDnDConstants.MOTIF_DND_ACTIONS_SHIFT);

            unsafe.putByte(data,
                           (byte)(MotifDnDConstants.TOP_LEVEL_ENTER |
                                  MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR));
            unsafe.putByte(data + 1,
                           MotifDnDConstants.getByteOrderByte());
            unsafe.putShort(data + 2, (short)flags);
            unsafe.putInt(data + 4, (int)time);
            unsafe.putInt(data + 8, (int)XDragSourceProtocol.getDragSourceWindow());
            unsafe.putInt(data + 12, (int)MotifDnDConstants.XA_MOTIF_ATOM_0.getAtom());

            XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                                   getTargetProxyWindow(),
                                   false, XConstants.NoEventMask,
                                   msg.pData);
        } finally {
            msg.dispose();
        }
    }

    public void sendMoveMessage(int xRoot, int yRoot,
                                int sourceAction, int sourceActions, long time) {
        assert XToolkit.isAWTLockHeldByCurrentThread();
        assert getTargetWindow() != 0;

        XClientMessageEvent msg = new XClientMessageEvent();
        try {
            msg.set_type(XConstants.ClientMessage);
            msg.set_window(getTargetWindow());
            msg.set_format(8);
            msg.set_message_type(MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom());

            long data = msg.get_data();
            int flags =
                (MotifDnDConstants.getMotifActionsForJavaActions(sourceAction) <<
                 MotifDnDConstants.MOTIF_DND_ACTION_SHIFT) |
                (MotifDnDConstants.getMotifActionsForJavaActions(sourceActions) <<
                 MotifDnDConstants.MOTIF_DND_ACTIONS_SHIFT);

            unsafe.putByte(data,
                           (byte)(MotifDnDConstants.DRAG_MOTION |
                                  MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR));
            unsafe.putByte(data + 1,
                           MotifDnDConstants.getByteOrderByte());
            unsafe.putShort(data + 2, (short)flags);
            unsafe.putInt(data + 4, (int)time);
            unsafe.putShort(data + 8, (short)xRoot);
            unsafe.putShort(data + 10, (short)yRoot);

            XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                                   getTargetProxyWindow(),
                                   false, XConstants.NoEventMask,
                                   msg.pData);
        } finally {
            msg.dispose();
        }
    }

    public void sendLeaveMessage(long time) {
        assert XToolkit.isAWTLockHeldByCurrentThread();
        assert getTargetWindow() != 0;

        XClientMessageEvent msg = new XClientMessageEvent();
        try {
            msg.set_type(XConstants.ClientMessage);
            msg.set_window(getTargetWindow());
            msg.set_format(8);
            msg.set_message_type(MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom());

            long data = msg.get_data();

            unsafe.putByte(data,
                           (byte)(MotifDnDConstants.TOP_LEVEL_LEAVE |
                                  MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR));
            unsafe.putByte(data + 1,
                           MotifDnDConstants.getByteOrderByte());
            unsafe.putShort(data + 2, (short)0);
            unsafe.putInt(data + 4, (int)time);
            unsafe.putInt(data + 8, (int)XDragSourceProtocol.getDragSourceWindow());

            XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                                   getTargetProxyWindow(),
                                   false, XConstants.NoEventMask,
                                   msg.pData);
        } finally {
            msg.dispose();
        }
    }

    protected void sendDropMessage(int xRoot, int yRoot,
                                   int sourceAction, int sourceActions,
                                   long time) {
        assert XToolkit.isAWTLockHeldByCurrentThread();
        assert getTargetWindow() != 0;

        /*
         * Motif drop sites expect TOP_LEVEL_LEAVE before DROP_START.
         */
        sendLeaveMessage(time);

        XClientMessageEvent msg = new XClientMessageEvent();
        try {
            msg.set_type(XConstants.ClientMessage);
            msg.set_window(getTargetWindow());
            msg.set_format(8);
            msg.set_message_type(MotifDnDConstants.XA_MOTIF_DRAG_AND_DROP_MESSAGE.getAtom());

            long data = msg.get_data();
            int flags =
                (MotifDnDConstants.getMotifActionsForJavaActions(sourceAction) <<
                 MotifDnDConstants.MOTIF_DND_ACTION_SHIFT) |
                (MotifDnDConstants.getMotifActionsForJavaActions(sourceActions) <<
                 MotifDnDConstants.MOTIF_DND_ACTIONS_SHIFT);

            unsafe.putByte(data,
                           (byte)(MotifDnDConstants.DROP_START |
                                  MotifDnDConstants.MOTIF_MESSAGE_FROM_INITIATOR));
            unsafe.putByte(data + 1,
                           MotifDnDConstants.getByteOrderByte());
            unsafe.putShort(data + 2, (short)flags);
            unsafe.putInt(data + 4, (int)time);
            unsafe.putShort(data + 8, (short)xRoot);
            unsafe.putShort(data + 10, (short)yRoot);
            unsafe.putInt(data + 12, (int)MotifDnDConstants.XA_MOTIF_ATOM_0.getAtom());
            unsafe.putInt(data + 16, (int)XDragSourceProtocol.getDragSourceWindow());

            XlibWrapper.XSendEvent(XToolkit.getDisplay(),
                                   getTargetProxyWindow(),
                                   false, XConstants.NoEventMask,
                                   msg.pData);
        } finally {
            msg.dispose();
        }
    }

    public boolean processProxyModeEvent(XClientMessageEvent xclient,
                                         long sourceWindow) {
        // Motif DnD for XEmbed is not implemented.
        return false;
    }

    public void cleanupTargetInfo() {
        super.cleanupTargetInfo();
        targetEnterServerTime = XConstants.CurrentTime;
    }

    public void dispatchEvent(XEvent ev) {
        switch (ev.get_type()) {
        case XConstants.SelectionRequest:
            XSelectionRequestEvent xsre = ev.get_xselectionrequest();
            long atom = xsre.get_selection();

            if (atom == MotifDnDConstants.XA_MOTIF_ATOM_0.getAtom()) {
                long target = xsre.get_target();
                if (target == MotifDnDConstants.XA_XmTRANSFER_SUCCESS.getAtom()) {
                    getProtocolListener().handleDragFinished(true);
                } else if (target == MotifDnDConstants.XA_XmTRANSFER_FAILURE.getAtom()) {
                    getProtocolListener().handleDragFinished(false);
                }
            }
            break;
        }
    }
}
