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

import java.util.Iterator;

/**
 * This class is a registry for the supported drag and drop protocols.
 *
 * @since 1.5
 */
final class XDropTargetEventProcessor {
    private static final XDropTargetEventProcessor theInstance =
        new XDropTargetEventProcessor();
    private static boolean active = false;

    // The current drop protocol.
    private XDropTargetProtocol protocol = null;

    private XDropTargetEventProcessor() {}

    private boolean doProcessEvent(XEvent ev) {
        if (ev.get_type() == XConstants.DestroyNotify &&
            protocol != null &&
            ev.get_xany().get_window() == protocol.getSourceWindow()) {
            protocol.cleanup();
            protocol = null;
            return false;
        }

        if (ev.get_type() == XConstants.PropertyNotify) {
            XPropertyEvent xproperty = ev.get_xproperty();
            if (xproperty.get_atom() ==
                MotifDnDConstants.XA_MOTIF_DRAG_RECEIVER_INFO.getAtom()) {

                XDropTargetRegistry.getRegistry().updateEmbedderDropSite(xproperty.get_window());
            }
        }

        if (ev.get_type() != XConstants.ClientMessage) {
            return false;
        }

        boolean processed = false;
        XClientMessageEvent xclient = ev.get_xclient();

        XDropTargetProtocol curProtocol = protocol;

        if (protocol != null) {
            if (protocol.getMessageType(xclient) !=
                XDropTargetProtocol.UNKNOWN_MESSAGE) {
                processed = protocol.processClientMessage(xclient);
            } else {
                protocol = null;
            }
        }

        if (protocol == null) {
            Iterator<XDropTargetProtocol> dropTargetProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();

            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
                // Don't try to process it again with the current protocol.
                if (dropTargetProtocol == curProtocol) {
                    continue;
                }

                if (dropTargetProtocol.getMessageType(xclient) ==
                    XDropTargetProtocol.UNKNOWN_MESSAGE) {
                    continue;
                }

                protocol = dropTargetProtocol;
                processed = protocol.processClientMessage(xclient);
                break;
            }
        }

        return processed;
    }

    static void reset() {
        theInstance.protocol = null;
    }

    static void activate() {
        active = true;
    }

    // Fix for 4915454 - do not call doProcessEvent() until the first drop
    // target is registered to avoid premature loading of DnD protocol
    // classes.
    static boolean processEvent(XEvent ev) {
        return active ? theInstance.doProcessEvent(ev) : false;
    }
}
