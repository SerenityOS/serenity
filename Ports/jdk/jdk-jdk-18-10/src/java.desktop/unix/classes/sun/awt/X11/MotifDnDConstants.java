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

import java.awt.dnd.DnDConstants;

import java.nio.ByteOrder;

import java.util.Arrays;

import jdk.internal.misc.Unsafe;

/**
 * Motif DnD protocol global constants and convenience routines.
 *
 * @since 1.5
 */
class MotifDnDConstants {
    // utility class can not be instantiated
    private MotifDnDConstants() {}
    // Note that offsets in all native structures below do not depend on the
    // architecture.
    private static final Unsafe unsafe = XlibWrapper.unsafe;
    static final XAtom XA_MOTIF_ATOM_0 = XAtom.get("_MOTIF_ATOM_0");
    static final XAtom XA_MOTIF_DRAG_WINDOW = XAtom.get("_MOTIF_DRAG_WINDOW");
    static final XAtom XA_MOTIF_DRAG_TARGETS = XAtom.get("_MOTIF_DRAG_TARGETS");
    static final XAtom XA_MOTIF_DRAG_INITIATOR_INFO =
        XAtom.get("_MOTIF_DRAG_INITIATOR_INFO");
    static final XAtom XA_MOTIF_DRAG_RECEIVER_INFO =
        XAtom.get("_MOTIF_DRAG_RECEIVER_INFO");
    static final XAtom XA_MOTIF_DRAG_AND_DROP_MESSAGE =
        XAtom.get("_MOTIF_DRAG_AND_DROP_MESSAGE");
    static final XAtom XA_XmTRANSFER_SUCCESS =
        XAtom.get("XmTRANSFER_SUCCESS");
    static final XAtom XA_XmTRANSFER_FAILURE =
        XAtom.get("XmTRANSFER_FAILURE");
    static final XSelection MotifDnDSelection = new XSelection(XA_MOTIF_ATOM_0);

    public static final byte MOTIF_DND_PROTOCOL_VERSION = 0;

    /* Supported protocol styles */
    public static final int MOTIF_PREFER_PREREGISTER_STYLE = 2;
    public static final int MOTIF_PREFER_DYNAMIC_STYLE     = 4;
    public static final int MOTIF_DYNAMIC_STYLE            = 5;
    public static final int MOTIF_PREFER_RECEIVER_STYLE    = 6;

    /* Info structure sizes */
    public static final int MOTIF_INITIATOR_INFO_SIZE      = 8;
    public static final int MOTIF_RECEIVER_INFO_SIZE       = 16;

    /* Sender/reason message masks */
    public static final byte MOTIF_MESSAGE_REASON_MASK      = (byte)0x7F;
    public static final byte MOTIF_MESSAGE_SENDER_MASK      = (byte)0x80;
    public static final byte MOTIF_MESSAGE_FROM_RECEIVER    = (byte)0x80;
    public static final byte MOTIF_MESSAGE_FROM_INITIATOR   = (byte)0;

    /* Message flags masks and shifts */
    public static final int MOTIF_DND_ACTION_MASK   = 0x000F;
    public static final int MOTIF_DND_ACTION_SHIFT  =      0;
    public static final int MOTIF_DND_STATUS_MASK   = 0x00F0;
    public static final int MOTIF_DND_STATUS_SHIFT  =      4;
    public static final int MOTIF_DND_ACTIONS_MASK  = 0x0F00;
    public static final int MOTIF_DND_ACTIONS_SHIFT =      8;

    /* message type constants */
    public static final byte TOP_LEVEL_ENTER   = 0;
    public static final byte TOP_LEVEL_LEAVE   = 1;
    public static final byte DRAG_MOTION       = 2;
    public static final byte DROP_SITE_ENTER   = 3;
    public static final byte DROP_SITE_LEAVE   = 4;
    public static final byte DROP_START        = 5;
    public static final byte DROP_FINISH       = 6;
    public static final byte DRAG_DROP_FINISH  = 7;
    public static final byte OPERATION_CHANGED = 8;

    /* drop action constants */
    public static final int MOTIF_DND_NOOP = 0;
    public static final int MOTIF_DND_MOVE = 1 << 0;
    public static final int MOTIF_DND_COPY = 1 << 1;
    public static final int MOTIF_DND_LINK = 1 << 2;

    /* drop site status constants */
    public static final byte MOTIF_NO_DROP_SITE      = (byte)1;
    public static final byte MOTIF_INVALID_DROP_SITE = (byte)2;
    public static final byte MOTIF_VALID_DROP_SITE   = (byte)3;

    private static long readMotifWindow() throws XException {
        long defaultScreenNumber = XlibWrapper.DefaultScreen(XToolkit.getDisplay());
        long defaultRootWindow =
            XlibWrapper.RootWindow(XToolkit.getDisplay(), defaultScreenNumber);

        long motifWindow = 0;

        WindowPropertyGetter wpg = new WindowPropertyGetter(defaultRootWindow,
                                                            XA_MOTIF_DRAG_WINDOW,
                                                            0, 1,
                                                            false,
                                                            XConstants.AnyPropertyType);
        try {
            int status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

            if (status == XConstants.Success &&
                wpg.getData() != 0 &&
                wpg.getActualType() == XAtom.XA_WINDOW &&
                wpg.getActualFormat() == 32 &&
                wpg.getNumberOfItems() == 1) {
                long data = wpg.getData();
                // XID is CARD32.
                motifWindow = Native.getLong(data);
            }

            return motifWindow;
        } finally {
            wpg.dispose();
        }
    }

    private static long createMotifWindow() throws XException {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        long defaultScreenNumber =
            XlibWrapper.DefaultScreen(XToolkit.getDisplay());
        long defaultRootWindow =
            XlibWrapper.RootWindow(XToolkit.getDisplay(), defaultScreenNumber);

        long motifWindow = 0;

        long displayString = XlibWrapper.XDisplayString(XToolkit.getDisplay());

        if (displayString == 0) {
            throw new XException("XDisplayString returns NULL");
        }

        long newDisplay = XlibWrapper.XOpenDisplay(displayString);

        if (newDisplay == 0) {
            throw new XException("XOpenDisplay returns NULL");
        }

        XlibWrapper.XGrabServer(newDisplay);

        try {
            XlibWrapper.XSetCloseDownMode(newDisplay, XConstants.RetainPermanent);

            XSetWindowAttributes xwa = new XSetWindowAttributes();

            try {
                xwa.set_override_redirect(true);
                xwa.set_event_mask(XConstants.PropertyChangeMask);

                motifWindow = XlibWrapper.XCreateWindow(newDisplay, defaultRootWindow,
                                                        -10, -10, 1, 1, 0, 0,
                                                        XConstants.InputOnly,
                                                        XConstants.CopyFromParent,
                                                        (XConstants.CWOverrideRedirect |
                                                         XConstants.CWEventMask),
                                                        xwa.pData);

                if (motifWindow == 0) {
                    throw new XException("XCreateWindow returns NULL");
                }

                XlibWrapper.XMapWindow(newDisplay, motifWindow);

                long data = Native.allocateLongArray(1);

                try {
                    Native.putLong(data, motifWindow);

                    XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
                    XlibWrapper.XChangeProperty(XToolkit.getDisplay(),
                                                defaultRootWindow,
                                                XA_MOTIF_DRAG_WINDOW.getAtom(),
                                                XAtom.XA_WINDOW, 32,
                                                XConstants.PropModeReplace,
                                                data, 1);

                    XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                    if ((XErrorHandlerUtil.saved_error != null) &&
                        (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                        throw new XException("Cannot write motif drag window handle.");
                    }

                    return motifWindow;
                } finally {
                    unsafe.freeMemory(data);
                }
            } finally {
                xwa.dispose();
            }
        } finally {
            XlibWrapper.XUngrabServer(newDisplay);
            XlibWrapper.XCloseDisplay(newDisplay);
        }
    }

    private static long getMotifWindow() throws XException {
        /*
         * Note: it is unsafe to cache the motif drag window handle, as another
         * client can change the _MOTIF_DRAG_WINDOW property on the root, the handle
         * becomes out-of-sync and all subsequent drag operations will fail.
         */
        long motifWindow = readMotifWindow();
        if (motifWindow == 0) {
            motifWindow = createMotifWindow();
        }
        return motifWindow;
    }

    public static final class Swapper {
        // utility class can not be instantiated
        private Swapper() {}

        public static short swap(short s) {
            return (short)(((s & 0xFF00) >>> 8) | ((s & 0xFF) << 8));
        }
        public static int swap(int i) {
            return ((i & 0xFF000000) >>> 24) | ((i & 0x00FF0000) >>> 8) |
                ((i & 0x0000FF00) << 8) | ((i & 0x000000FF) << 24);
        }

        public static short getShort(long data, byte order) {
            short s = unsafe.getShort(data);
            if (order != MotifDnDConstants.getByteOrderByte()) {
                return swap(s);
            } else {
                return s;
            }
        }
        public static int getInt(long data, byte order) {
            int i = unsafe.getInt(data);
            if (order != MotifDnDConstants.getByteOrderByte()) {
                return swap(i);
            } else {
                return i;
            }
        }
    }

    /**
     * DragBSI.h:
     *
     * typedef struct {
     *    BYTE          byte_order;
     *    BYTE          protocol_version;
     *    CARD16        num_target_lists B16;
     *    CARD32        heap_offset B32;
     * } xmMotifTargetsPropertyRec;
     */
    private static long[][] getTargetListTable(long motifWindow)
      throws XException {

        WindowPropertyGetter wpg = new WindowPropertyGetter(motifWindow,
                                                            XA_MOTIF_DRAG_TARGETS,
                                                            0, 100000L,
                                                            false,
                                                            XA_MOTIF_DRAG_TARGETS.getAtom());
        try {
            int status = wpg.execute(XErrorHandler.IgnoreBadWindowHandler.getInstance());

            if (status != XConstants.Success
                || wpg.getActualType() != XA_MOTIF_DRAG_TARGETS.getAtom()
                || wpg.getData() == 0) {

                return null;
            }

            long data = wpg.getData();

            if (unsafe.getByte(data + 1) != MOTIF_DND_PROTOCOL_VERSION) {
                return null;
            }

            boolean swapNeeded = unsafe.getByte(data + 0) != getByteOrderByte();

            short numTargetLists = unsafe.getShort(data + 2);

            if (swapNeeded) {
                numTargetLists = Swapper.swap(numTargetLists);
            }

            long[][] table = new long[numTargetLists][];
            ByteOrder byteOrder = ByteOrder.nativeOrder();
            if (swapNeeded) {
                byteOrder = (byteOrder == ByteOrder.LITTLE_ENDIAN) ?
                    ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            }

            long bufptr = data + 8;
            for (short i = 0; i < numTargetLists; i++) {
                short numTargets = unsafe.getShort(bufptr);
                bufptr += 2;
                if (swapNeeded) {
                    numTargets = Swapper.swap(numTargets);
                }

                table[i] = new long[numTargets];

                for (short j = 0; j < numTargets; j++) {
                    // NOTE: cannot use Unsafe.getInt(), since it crashes on
                    // Solaris/Sparc if the address is not a multiple of 4.
                    int target = 0;
                    if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                        for (int idx = 0; idx < 4; idx++) {
                            target |= (unsafe.getByte(bufptr + idx) << 8*idx)
                                & (0xFF << 8*idx);
                        }
                    } else {
                        for (int idx = 0; idx < 4; idx++) {
                            target |= (unsafe.getByte(bufptr + idx) << 8*(3-idx))
                                & (0xFF << 8*(3-idx));
                        }
                    }
                    // NOTE: don't need to swap, since we read it in the proper
                    // order already.
                    table[i][j] = target;
                    bufptr += 4;
                }
            }
            return table;
        } finally {
            wpg.dispose();
        }
    }

    private static void putTargetListTable(long motifWindow, long[][] table)
      throws XException {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        int tableSize = 8; /* The size of leading xmMotifTargetsPropertyRec. */

        for (int i = 0; i < table.length; i++) {
            tableSize += table[i].length * 4 + 2;
        }

        long data = unsafe.allocateMemory(tableSize);

        try {
            // BYTE          byte_order;
            unsafe.putByte(data + 0, getByteOrderByte());
            // BYTE          protocol_version;
            unsafe.putByte(data + 1, MOTIF_DND_PROTOCOL_VERSION);
            // CARD16        num_target_lists B16;
            unsafe.putShort(data + 2, (short)table.length);
            // CARD32        heap_offset B32;
            unsafe.putInt(data + 4, tableSize);

            long bufptr = data + 8;

            for (int i = 0; i < table.length; i++) {
                unsafe.putShort(bufptr, (short)table[i].length);
                bufptr += 2;

                for (int j = 0; j < table[i].length; j++) {
                    int target = (int)table[i][j];
                    // NOTE: cannot use Unsafe.putInt(), since it crashes on
                    // Solaris/Sparc if the address is not a multiple of 4.
                    if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
                        for (int idx = 0; idx < 4; idx++) {
                            byte b = (byte)((target & (0xFF << (8*idx))) >> (8*idx));
                            unsafe.putByte(bufptr + idx, b);
                        }
                    } else {
                        for (int idx = 0; idx < 4; idx++) {
                            byte b = (byte)((target & (0xFF << (8*idx))) >> (8*idx));
                            unsafe.putByte(bufptr + (3-idx), b);
                        }
                    }
                    bufptr += 4;
                }
            }

            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
            XlibWrapper.XChangeProperty(XToolkit.getDisplay(),
                                        motifWindow,
                                        XA_MOTIF_DRAG_TARGETS.getAtom(),
                                        XA_MOTIF_DRAG_TARGETS.getAtom(), 8,
                                        XConstants.PropModeReplace,
                                        data, tableSize);

            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {

                // Create a new motif window and retry.
                motifWindow = createMotifWindow();

                XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
                XlibWrapper.XChangeProperty(XToolkit.getDisplay(),
                                            motifWindow,
                                            XA_MOTIF_DRAG_TARGETS.getAtom(),
                                            XA_MOTIF_DRAG_TARGETS.getAtom(), 8,
                                            XConstants.PropModeReplace,
                                            data, tableSize);

                XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                if ((XErrorHandlerUtil.saved_error != null) &&
                    (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                    throw new XException("Cannot write motif drag targets property.");
                }
            }
        } finally {
            unsafe.freeMemory(data);
        }
    }

    static int getIndexForTargetList(long[] formats) throws XException {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        if (formats.length > 0) {
            // Make a defensive copy.
            formats = formats.clone();

            Arrays.sort(formats);
        }

        // NOTE: getMotifWindow() should never be called if the server is
        // grabbed. This will lock up the application as it grabs the server
        // itself.
        // Since we don't grab the server before getMotifWindow(), another
        // client might replace motif window after we read it from the root, but
        // before we grab the server.
        // We cannot resolve this problem, but we believe that this scenario is
        // very unlikely to happen.
        long motifWindow = getMotifWindow();

        XlibWrapper.XGrabServer(XToolkit.getDisplay());

        try {
            long[][] table = getTargetListTable(motifWindow);

            if (table != null) {
                for (int i = 0; i < table.length; i++) {
                    boolean equals = true;
                    if (table[i].length == formats.length) {
                        for (int j = 0; j < table[i].length; j++) {
                            if (table[i][j] != formats[j]) {
                                equals = false;
                                break;
                            }
                        }
                    } else {
                        equals = false;
                    }

                    if (equals) {
                        XlibWrapper.XUngrabServer(XToolkit.getDisplay());
                        return i;
                    }
                }
            } else {
                // Create a new table.
                // The first two entries must always be the same.
                // (see DragBS.c)
                table = new long[2][];
                table[0] = new long[] { 0 };
                table[1] = new long[] { XAtom.XA_STRING };
            }

            /* Index not found - expand the targets table. */
            long[][] new_table = new long[table.length + 1][];

            /* Copy the old contents to the new table. */
            for (int i = 0; i < table.length; i++) {
                new_table[i] = table[i];
            }

            /* Fill in the new entry */
            new_table[new_table.length - 1] = formats;

            putTargetListTable(motifWindow, new_table);

            return new_table.length - 1;
        } finally {
            XlibWrapper.XUngrabServer(XToolkit.getDisplay());
        }
    }

    static long[] getTargetListForIndex(int index) {
        long motifWindow = getMotifWindow();
        long[][] table = getTargetListTable(motifWindow);

        if (index < 0 || index >= table.length) {
            return new long[0];
        } else {
            return table[index];
        }
    }

    static byte getByteOrderByte() {
        // 'l' - for little endian, 'B' - for big endian.
        return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ?
            (byte)0x6C : (byte)0x42;
    }

    static void writeDragInitiatorInfoStruct(long window, int index) throws XException {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        long structData = unsafe.allocateMemory(MOTIF_INITIATOR_INFO_SIZE);

        try {
            // BYTE byte_order
            unsafe.putByte(structData, getByteOrderByte());
            // BYTE protocol_version
            unsafe.putByte(structData + 1, MOTIF_DND_PROTOCOL_VERSION);
            // CARD16 protocol_version
            unsafe.putShort(structData + 2, (short)index);
            // CARD32 icc_handle
            unsafe.putInt(structData + 4, (int)XA_MOTIF_ATOM_0.getAtom());

            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
            XlibWrapper.XChangeProperty(XToolkit.getDisplay(), window,
                                        XA_MOTIF_ATOM_0.getAtom(),
                                        XA_MOTIF_DRAG_INITIATOR_INFO.getAtom(),
                                        8, XConstants.PropModeReplace,
                                        structData, MOTIF_INITIATOR_INFO_SIZE);
            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                throw new XException("Cannot write drag initiator info");
            }
        } finally {
            unsafe.freeMemory(structData);
        }
    }

    static void writeDragReceiverInfoStruct(long window) throws XException {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        int dataSize = MotifDnDConstants.MOTIF_RECEIVER_INFO_SIZE;
        long data = unsafe.allocateMemory(dataSize);

        try {
            unsafe.putByte(data, MotifDnDConstants.getByteOrderByte()); /* byte order */
            unsafe.putByte(data + 1, MotifDnDConstants.MOTIF_DND_PROTOCOL_VERSION); /* protocol version */
            unsafe.putByte(data + 2, (byte)MotifDnDConstants.MOTIF_DYNAMIC_STYLE); /* protocol style */
            unsafe.putByte(data + 3, (byte)0); /* pad */
            unsafe.putInt(data + 4, (int)window); /* proxy window */
            unsafe.putShort(data + 8, (short)0); /* num_drop_sites */
            unsafe.putShort(data + 10, (short)0); /* pad */
            unsafe.putInt(data + 12, dataSize);

            XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.VerifyChangePropertyHandler.getInstance());
            XlibWrapper.XChangeProperty(XToolkit.getDisplay(), window,
                                        XA_MOTIF_DRAG_RECEIVER_INFO.getAtom(),
                                        XA_MOTIF_DRAG_RECEIVER_INFO.getAtom(),
                                        8, XConstants.PropModeReplace,
                                        data, dataSize);
            XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

            if ((XErrorHandlerUtil.saved_error != null) &&
                (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                throw new XException("Cannot write Motif receiver info property");
            }
        } finally {
            unsafe.freeMemory(data);
        }
    }

    public static int getMotifActionsForJavaActions(int javaActions) {
        int motifActions = MOTIF_DND_NOOP;

        if ((javaActions & DnDConstants.ACTION_MOVE) != 0) {
            motifActions |= MOTIF_DND_MOVE;
        }
        if ((javaActions & DnDConstants.ACTION_COPY) != 0) {
            motifActions |= MOTIF_DND_COPY;
        }
        if ((javaActions & DnDConstants.ACTION_LINK) != 0) {
            motifActions |= MOTIF_DND_LINK;
        }

        return motifActions;
    }

    public static int getJavaActionsForMotifActions(int motifActions) {
        int javaActions = DnDConstants.ACTION_NONE;

        if ((motifActions & MOTIF_DND_MOVE) != 0) {
            javaActions |= DnDConstants.ACTION_MOVE;
        }
        if ((motifActions & MOTIF_DND_COPY) != 0) {
            javaActions |= DnDConstants.ACTION_COPY;
        }
        if ((motifActions & MOTIF_DND_LINK) != 0) {
            javaActions |= DnDConstants.ACTION_LINK;
        }

        return javaActions;
    }
}
