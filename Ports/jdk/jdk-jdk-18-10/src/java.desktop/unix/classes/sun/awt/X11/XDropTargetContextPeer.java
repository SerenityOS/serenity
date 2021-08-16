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
import java.awt.peer.ComponentPeer;

import java.io.IOException;

import java.util.Iterator;

import sun.awt.AWTAccessor;
import sun.util.logging.PlatformLogger;

import sun.awt.AppContext;
import sun.awt.SunToolkit;

import sun.awt.dnd.SunDropTargetContextPeer;
import sun.awt.dnd.SunDropTargetEvent;

import jdk.internal.misc.Unsafe;

/**
 * The XDropTargetContextPeer is the class responsible for handling
 * the interaction between the XDnD/Motif DnD subsystem and Java drop targets.
 *
 * @since 1.5
 */
final class XDropTargetContextPeer extends SunDropTargetContextPeer {
    private static final PlatformLogger logger =
        PlatformLogger.getLogger("sun.awt.X11.xembed.xdnd.XDropTargetContextPeer");

    private static final Unsafe unsafe = XlibWrapper.unsafe;

    /*
     * A key to store a peer instance for an AppContext.
     */
    private static final Object DTCP_KEY = "DropTargetContextPeer";

    private XDropTargetContextPeer() {}

    static XDropTargetContextPeer getPeer(AppContext appContext) {
        synchronized (_globalLock) {
            XDropTargetContextPeer peer =
                (XDropTargetContextPeer)appContext.get(DTCP_KEY);
            if (peer == null) {
                peer = new XDropTargetContextPeer();
                appContext.put(DTCP_KEY, peer);
            }

            return peer;
        }
    }

    static XDropTargetProtocolListener getXDropTargetProtocolListener() {
        return XDropTargetProtocolListenerImpl.getInstance();
    }

    /*
     * @param returnValue the drop action selected by the Java drop target.
     */
    protected void eventProcessed(SunDropTargetEvent e, int returnValue,
                                  boolean dispatcherDone) {
        /* The native context is the pointer to the XClientMessageEvent
           structure. */
        long ctxt = getNativeDragContext();
        /* If the event was not consumed, send a response to the source. */
        try {
            if (ctxt != 0 && !e.isConsumed()) {
                Iterator<XDropTargetProtocol> dropTargetProtocols =
                    XDragAndDropProtocols.getDropTargetProtocols();

                while (dropTargetProtocols.hasNext()) {
                    XDropTargetProtocol dropTargetProtocol =
                        dropTargetProtocols.next();
                    if (dropTargetProtocol.sendResponse(ctxt, e.getID(),
                                                        returnValue)) {
                        break;
                    }
                }
            }
        } finally {
            if (dispatcherDone && ctxt != 0) {
                unsafe.freeMemory(ctxt);
            }
        }
    }

    protected void doDropDone(boolean success, int dropAction,
                              boolean isLocal) {
        /* The native context is the pointer to the XClientMessageEvent
           structure. */
        long ctxt = getNativeDragContext();

        if (ctxt != 0) {
            try {
                Iterator<XDropTargetProtocol> dropTargetProtocols =
                    XDragAndDropProtocols.getDropTargetProtocols();

                while (dropTargetProtocols.hasNext()) {
                    XDropTargetProtocol dropTargetProtocol =
                        dropTargetProtocols.next();
                    if (dropTargetProtocol.sendDropDone(ctxt, success,
                                                        dropAction)) {
                        break;
                    }
                }
            } finally {
                unsafe.freeMemory(ctxt);
            }
        }
    }

    protected Object getNativeData(long format)
      throws IOException {
        /* The native context is the pointer to the XClientMessageEvent
           structure. */
        long ctxt = getNativeDragContext();

        if (ctxt != 0) {
            Iterator<XDropTargetProtocol> dropTargetProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();

            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol =
                    dropTargetProtocols.next();
                // getData throws IAE if ctxt is not for this protocol.
                try {
                    return dropTargetProtocol.getData(ctxt, format);
                } catch (IllegalArgumentException iae) {
                }
            }
        }

        return null;
    }

    private void cleanup() {
    }

    protected void processEnterMessage(SunDropTargetEvent event) {
        if (!processSunDropTargetEvent(event)) {
            super.processEnterMessage(event);
        }
    }

    protected void processExitMessage(SunDropTargetEvent event) {
        if (!processSunDropTargetEvent(event)) {
            super.processExitMessage(event);
        }
    }

    protected void processMotionMessage(SunDropTargetEvent event,
                                        boolean operationChanged) {
        if (!processSunDropTargetEvent(event)) {
            super.processMotionMessage(event, operationChanged);
        }
    }

    protected void processDropMessage(SunDropTargetEvent event) {
        if (!processSunDropTargetEvent(event)) {
            super.processDropMessage(event);
        }
    }

    // If source is an XEmbedCanvasPeer, passes the event to it for processing and
    // return true if the event is forwarded to the XEmbed child.
    // Otherwise, does nothing and return false.
    private boolean processSunDropTargetEvent(SunDropTargetEvent event) {
        Object source = event.getSource();

        if (source instanceof Component) {
            Object peer = AWTAccessor.getComponentAccessor()
                                     .getPeer((Component) source);
            if (peer instanceof XEmbedCanvasPeer) {
                XEmbedCanvasPeer xEmbedCanvasPeer = (XEmbedCanvasPeer)peer;
                /* The native context is the pointer to the XClientMessageEvent
                   structure. */
                long ctxt = getNativeDragContext();

                if (logger.isLoggable(PlatformLogger.Level.FINER)) {
                    logger.finer("        processing " + event + " ctxt=" + ctxt +
                                 " consumed=" + event.isConsumed());
                }
                /* If the event is not consumed, pass it to the
                   XEmbedCanvasPeer for processing. */
                if (!event.isConsumed()) {
                    // NOTE: ctxt can be zero at this point.
                    if (xEmbedCanvasPeer.processXEmbedDnDEvent(ctxt,
                                                               event.getID())) {
                        event.consume();
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public void forwardEventToEmbedded(long embedded, long ctxt,
                                       int eventID) {
        Iterator<XDropTargetProtocol> dropTargetProtocols =
            XDragAndDropProtocols.getDropTargetProtocols();

        while (dropTargetProtocols.hasNext()) {
            XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
            if (dropTargetProtocol.forwardEventToEmbedded(embedded, ctxt,
                                                          eventID)) {
                break;
            }
        }
    }

    static final class XDropTargetProtocolListenerImpl
        implements XDropTargetProtocolListener {

        private static final XDropTargetProtocolListener theInstance =
            new XDropTargetProtocolListenerImpl();

        private XDropTargetProtocolListenerImpl() {}

        static XDropTargetProtocolListener getInstance() {
            return theInstance;
        }

        public void handleDropTargetNotification(XWindow xwindow, int x, int y,
                                                 int dropAction, int actions,
                                                 long[] formats, long nativeCtxt,
                                                 int eventID) {
            Object target = xwindow.getTarget();

            // The Every component is associated with some AppContext.
            assert target instanceof Component;

            Component component = (Component)target;

            AppContext appContext = SunToolkit.targetToAppContext(target);

            // Every component is associated with some AppContext.
            assert appContext != null;

            XDropTargetContextPeer peer = XDropTargetContextPeer.getPeer(appContext);

            peer.postDropTargetEvent(component, x, y, dropAction, actions, formats,
                                     nativeCtxt, eventID,
                                     !SunDropTargetContextPeer.DISPATCH_SYNC);
        }
    }
}
