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

import jdk.internal.misc.Unsafe;

import sun.util.logging.PlatformLogger;

import java.awt.AWTKeyStroke;
import java.awt.event.InputEvent;

/**
 * Common class for all XEmbed protocol participating classes.
 * Contains constant definitions and helper routines.
 */
public class XEmbedHelper {
    private static final PlatformLogger xembedLog = PlatformLogger.getLogger("sun.awt.X11.xembed");
    static final Unsafe unsafe = Unsafe.getUnsafe();

    static final int XEMBED_VERSION = 0,
        XEMBED_MAPPED = (1 << 0);
/* XEMBED messages */
    static final int XEMBED_EMBEDDED_NOTIFY     =       0;
    static final int XEMBED_WINDOW_ACTIVATE  =  1;
    static final int XEMBED_WINDOW_DEACTIVATE =         2;
    static final int XEMBED_REQUEST_FOCUS               =3;
    static final int XEMBED_FOCUS_IN    =       4;
    static final int XEMBED_FOCUS_OUT   =       5;
    static final int XEMBED_FOCUS_NEXT  =       6;
    static final int XEMBED_FOCUS_PREV  =       7;
/* 8-9 were used for XEMBED_GRAB_KEY/XEMBED_UNGRAB_KEY */
    static final int XEMBED_GRAB_KEY = 8;
    static final int XEMBED_UNGRAB_KEY = 9;
    static final int XEMBED_MODALITY_ON         =       10;
    static final int XEMBED_MODALITY_OFF        =       11;
    static final int XEMBED_REGISTER_ACCELERATOR =    12;
    static final int XEMBED_UNREGISTER_ACCELERATOR=   13;
    static final int XEMBED_ACTIVATE_ACCELERATOR  =   14;

    static final int NON_STANDARD_XEMBED_GTK_GRAB_KEY = 108;
    static final int NON_STANDARD_XEMBED_GTK_UNGRAB_KEY = 109;

//     A detail code is required for XEMBED_FOCUS_IN. The following values are valid:
/* Details for  XEMBED_FOCUS_IN: */
    static final int XEMBED_FOCUS_CURRENT       =       0;
    static final int XEMBED_FOCUS_FIRST         =       1;
    static final int XEMBED_FOCUS_LAST  =       2;

// Modifiers bits
    static final int XEMBED_MODIFIER_SHIFT   = (1 << 0);
    static final int XEMBED_MODIFIER_CONTROL = (1 << 1);
    static final int XEMBED_MODIFIER_ALT     = (1 << 2);
    static final int XEMBED_MODIFIER_SUPER   = (1 << 3);
    static final int XEMBED_MODIFIER_HYPER   = (1 << 4);

    static XAtom XEmbedInfo;
    static XAtom XEmbed;

    XEmbedHelper() {
        if (XEmbed == null) {
            XEmbed = XAtom.get("_XEMBED");
            if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                xembedLog.finer("Created atom " + XEmbed.toString());
            }
        }
        if (XEmbedInfo == null) {
            XEmbedInfo = XAtom.get("_XEMBED_INFO");
            if (xembedLog.isLoggable(PlatformLogger.Level.FINER)) {
                xembedLog.finer("Created atom " + XEmbedInfo.toString());
            }
        }
    }

    void sendMessage(long window, int message) {
        sendMessage(window, message, 0, 0, 0);
    }
    void sendMessage(long window, int message, long detail, long data1, long data2) {
        XClientMessageEvent msg = new XClientMessageEvent();
        msg.set_type(XConstants.ClientMessage);
        msg.set_window(window);
        msg.set_message_type(XEmbed.getAtom());
        msg.set_format(32);
        msg.set_data(0, XToolkit.getCurrentServerTime());
        msg.set_data(1, message);
        msg.set_data(2, detail);
        msg.set_data(3, data1);
        msg.set_data(4, data2);
        XToolkit.awtLock();
        try {
            if (xembedLog.isLoggable(PlatformLogger.Level.FINE)) {
                xembedLog.fine("Sending " + XEmbedMessageToString(msg));
            }
            XlibWrapper.XSendEvent(XToolkit.getDisplay(), window, false, XConstants.NoEventMask, msg.pData);
        }
        finally {
            XToolkit.awtUnlock();
        }
        msg.dispose();
    }

    static String msgidToString(int msg_id) {
        switch (msg_id) {
          case XEMBED_EMBEDDED_NOTIFY:
              return "XEMBED_EMBEDDED_NOTIFY";
          case XEMBED_WINDOW_ACTIVATE:
              return "XEMBED_WINDOW_ACTIVATE";
          case XEMBED_WINDOW_DEACTIVATE:
              return "XEMBED_WINDOW_DEACTIVATE";
          case XEMBED_FOCUS_IN:
              return "XEMBED_FOCUS_IN";
          case XEMBED_FOCUS_OUT:
              return "XEMBED_FOCUS_OUT";
          case XEMBED_REQUEST_FOCUS:
              return "XEMBED_REQUEST_FOCUS";
          case XEMBED_FOCUS_NEXT:
              return "XEMBED_FOCUS_NEXT";
          case XEMBED_FOCUS_PREV:
              return "XEMBED_FOCUS_PREV";
          case XEMBED_MODALITY_ON:
              return "XEMBED_MODALITY_ON";
          case XEMBED_MODALITY_OFF:
              return "XEMBED_MODALITY_OFF";
          case XEMBED_REGISTER_ACCELERATOR:
              return "XEMBED_REGISTER_ACCELERATOR";
          case XEMBED_UNREGISTER_ACCELERATOR:
              return "XEMBED_UNREGISTER_ACCELERATOR";
          case XEMBED_ACTIVATE_ACCELERATOR:
              return "XEMBED_ACTIVATE_ACCELERATOR";
          case XEMBED_GRAB_KEY:
              return "XEMBED_GRAB_KEY";
          case XEMBED_UNGRAB_KEY:
              return "XEMBED_UNGRAB_KEY";
          case NON_STANDARD_XEMBED_GTK_UNGRAB_KEY:
              return "NON_STANDARD_XEMBED_GTK_UNGRAB_KEY";
          case NON_STANDARD_XEMBED_GTK_GRAB_KEY:
              return "NON_STANDARD_XEMBED_GTK_GRAB_KEY";
          case XConstants.KeyPress | XEmbedServerTester.SYSTEM_EVENT_MASK:
              return "KeyPress";
          case XConstants.MapNotify | XEmbedServerTester.SYSTEM_EVENT_MASK:
              return "MapNotify";
          case XConstants.PropertyNotify | XEmbedServerTester.SYSTEM_EVENT_MASK:
              return "PropertyNotify";
          default:
              return "unknown XEMBED id " + msg_id;
        }
    }

    static String focusIdToString(int focus_id) {
        switch(focus_id) {
          case XEMBED_FOCUS_CURRENT:
              return "XEMBED_FOCUS_CURRENT";
          case XEMBED_FOCUS_FIRST:
              return "XEMBED_FOCUS_FIRST";
          case XEMBED_FOCUS_LAST:
              return "XEMBED_FOCUS_LAST";
          default:
              return "unknown focus id " + focus_id;
        }
    }

    static String XEmbedMessageToString(XClientMessageEvent msg) {
        return ("XEmbed message to " + Long.toHexString(msg.get_window()) + ": " + msgidToString((int)msg.get_data(1)) +
                ", detail: " + msg.get_data(2) +
                ", data:[" + msg.get_data(3) + "," + msg.get_data(4) + "]");

    }


    /**
     * Converts XEMBED modifiers mask into AWT InputEvent mask
     */
    int getModifiers(int state) {
        int mods = 0;
        if ((state & XEMBED_MODIFIER_SHIFT) != 0) {
            mods |= InputEvent.SHIFT_DOWN_MASK;
        }
        if ((state & XEMBED_MODIFIER_CONTROL) != 0) {
            mods |= InputEvent.CTRL_DOWN_MASK;
        }
        if ((state & XEMBED_MODIFIER_ALT) != 0) {
            mods |= InputEvent.ALT_DOWN_MASK;
        }
        // FIXME: What is super/hyper?
        // FIXME: Experiments show that SUPER is ALT. So what is Alt then?
        if ((state & XEMBED_MODIFIER_SUPER) != 0) {
            mods |= InputEvent.ALT_DOWN_MASK;
        }
//         if ((state & XEMBED_MODIFIER_HYPER) != 0) {
//             mods |= InputEvent.DOWN_MASK;
//         }
        return mods;
    }

    // Shouldn't be called on Toolkit thread.
    AWTKeyStroke getKeyStrokeForKeySym(long keysym, long state) {
        XBaseWindow.checkSecurity();

        int keycode;

        XToolkit.awtLock();
        try {
            XKeysym.Keysym2JavaKeycode kc = XKeysym.getJavaKeycode( keysym );
            if(kc == null) {
                keycode = java.awt.event.KeyEvent.VK_UNDEFINED;
            }else{
                keycode = kc.getJavaKeycode();
            }
        } finally {
            XToolkit.awtUnlock();
        }

        int modifiers = getModifiers((int)state);
        return AWTKeyStroke.getAWTKeyStroke(keycode, modifiers);
    }
}
