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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import sun.util.logging.PlatformLogger;

import java.awt.Point;


/**
 * The class responsible for registration/deregistration of drop sites.
 *
 * @since 1.5
 */
final class XDropTargetRegistry {
    private static final PlatformLogger logger =
        PlatformLogger.getLogger("sun.awt.X11.xembed.xdnd.XDropTargetRegistry");

    private static final long DELAYED_REGISTRATION_PERIOD = 200;

    private static final XDropTargetRegistry theInstance =
        new XDropTargetRegistry();

    private final HashMap<Long, Runnable> delayedRegistrationMap =
        new HashMap<Long, Runnable>();

    private XDropTargetRegistry() {}

    static XDropTargetRegistry getRegistry() {
        return theInstance;
    }

    /**
     * Returns the XID of the topmost window with WM_STATE set in the ancestor
     * hierarchy of the specified window or 0 if none found.
     */
    private long getToplevelWindow(long window) {
        XBaseWindow candWindow = XToolkit.windowToXWindow(window);
        if (candWindow != null) {
            XWindowPeer toplevel = candWindow.getToplevelXWindow();
            if (toplevel != null && !(toplevel instanceof XEmbeddedFramePeer)) {
                return toplevel.getWindow();
            }
        }

        /* Traverse the ancestor tree from window up to the root and find
           the top-level client window nearest to the root. */
        do {
            if (XlibUtil.isTrueToplevelWindow(window)) {
                return window;
            }

            window = XlibUtil.getParentWindow(window);

        } while (window != 0);

        return window;
    }

    static long getDnDProxyWindow() {
        return XWindow.getXAWTRootWindow().getWindow();
    }

    private static final class EmbeddedDropSiteEntry {
        private final long root;
        private final long event_mask;
        private List<XDropTargetProtocol> supportedProtocols;
        private final HashSet<Long> nonXEmbedClientSites = new HashSet<Long>();
        private final List<Long> sites = new ArrayList<Long>();

        public EmbeddedDropSiteEntry(long root, long event_mask,
                                     List<XDropTargetProtocol> supportedProtocols) {
            if (supportedProtocols == null) {
                throw new NullPointerException("Null supportedProtocols");
            }
            this.root = root;
            this.event_mask = event_mask;
            this.supportedProtocols = supportedProtocols;
        }

        public long getRoot() {
            return root;
        }
        public long getEventMask() {
            return event_mask;
        }
        public boolean hasNonXEmbedClientSites() {
            return !nonXEmbedClientSites.isEmpty();
        }
        public synchronized void addSite(long window, boolean isXEmbedClient) {
            Long lWindow = Long.valueOf(window);
            if (!sites.contains(lWindow)) {
                sites.add(lWindow);
            }
            if (!isXEmbedClient) {
                nonXEmbedClientSites.add(lWindow);
            }
        }
        public synchronized void removeSite(long window) {
            Long lWindow = Long.valueOf(window);
            sites.remove(lWindow);
            nonXEmbedClientSites.remove(lWindow);
        }
        public void setSupportedProtocols(List<XDropTargetProtocol> list) {
            supportedProtocols = list;
        }
        public List<XDropTargetProtocol> getSupportedProtocols() {
            return supportedProtocols;
        }
        public boolean hasSites() {
            return !sites.isEmpty();
        }
        public long[] getSites() {
            long[] ret = new long[sites.size()];
            Iterator<Long> iter = sites.iterator();
            int index = 0;
            while (iter.hasNext()) {
                Long l = iter.next();
                ret[index++] = l.longValue();
            }
            return ret;
        }
        public long getSite(int x, int y) {
            assert XToolkit.isAWTLockHeldByCurrentThread();

            Iterator<Long> iter = sites.iterator();
            while (iter.hasNext()) {
                Long l = iter.next();
                long window = l.longValue();

                Point p = XBaseWindow.toOtherWindow(getRoot(), window, x, y);

                if (p == null) {
                    continue;
                }

                int dest_x = p.x;
                int dest_y = p.y;
                if (dest_x >= 0 && dest_y >= 0) {
                    XWindowAttributes wattr = new XWindowAttributes();
                    try {
                        XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                        int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                                      window, wattr.pData);
                        XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                        if ((status == 0) ||
                            ((XErrorHandlerUtil.saved_error != null) &&
                            (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success))) {
                            continue;
                        }

                        if (wattr.get_map_state() != XConstants.IsUnmapped
                            && dest_x < wattr.get_width()
                            && dest_y < wattr.get_height()) {
                            return window;
                        }
                    } finally {
                        wattr.dispose();
                    }
                }
            }
            return 0;
        }
    }

    private final HashMap<Long, EmbeddedDropSiteEntry> embeddedDropSiteRegistry =
        new HashMap<Long, EmbeddedDropSiteEntry>();

    private EmbeddedDropSiteEntry registerEmbedderDropSite(long embedder) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        Iterator<XDropTargetProtocol> dropTargetProtocols =
            XDragAndDropProtocols.getDropTargetProtocols();
        // The list of protocols supported by the embedder.
        List<XDropTargetProtocol> embedderProtocols = new ArrayList<>();

        while (dropTargetProtocols.hasNext()) {
            XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
            if (dropTargetProtocol.isProtocolSupported(embedder)) {
                embedderProtocols.add(dropTargetProtocol);
            }
        }

        embedderProtocols = Collections.unmodifiableList(embedderProtocols);

        /* Grab server, since we are working with the window that belongs to
           another client. */
        XlibWrapper.XGrabServer(XToolkit.getDisplay());
        try {
            long root = 0;
            long event_mask = 0;
            XWindowAttributes wattr = new XWindowAttributes();
            try {
                XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                int status = XlibWrapper.XGetWindowAttributes(XToolkit.getDisplay(),
                                                              embedder, wattr.pData);
                XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                if ((status == 0) ||
                    ((XErrorHandlerUtil.saved_error != null) &&
                    (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success))) {
                    throw new XException("XGetWindowAttributes failed");
                }

                event_mask = wattr.get_your_event_mask();
                root = wattr.get_root();
            } finally {
                wattr.dispose();
            }

            if ((event_mask & XConstants.PropertyChangeMask) == 0) {
                XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                XlibWrapper.XSelectInput(XToolkit.getDisplay(), embedder,
                                         event_mask | XConstants.PropertyChangeMask);
                XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                if ((XErrorHandlerUtil.saved_error != null) &&
                    (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                    throw new XException("XSelectInput failed");
                }
            }

            return new EmbeddedDropSiteEntry(root, event_mask, embedderProtocols);
        } finally {
            XlibWrapper.XUngrabServer(XToolkit.getDisplay());
        }
    }

    private static final boolean XEMBED_PROTOCOLS = true;
    private static final boolean NON_XEMBED_PROTOCOLS = false;

    private void registerProtocols(long embedder, boolean protocols,
                                   List<XDropTargetProtocol> supportedProtocols) {
        Iterator<XDropTargetProtocol> dropTargetProtocols = null;

        /*
         * By default, we register a drop site that supports all dnd
         * protocols. This approach is not appropriate in plugin
         * scenario if the browser supports Motif DnD and doesn't support
         * XDnD. If we forcibly set XdndAware on the browser toplevel, any drag
         * source that supports both protocols and prefers XDnD will be unable
         * to drop anything on the browser.
         * The solution for this problem is not to register XDnD drop site
         * if the browser supports only Motif DnD.
         * In general, if the browser already supports some protocols, we
         * register the embedded drop site only for those protocols. Otherwise
         * we register the embedded drop site for all protocols.
         */
        if (!supportedProtocols.isEmpty()) {
            dropTargetProtocols = supportedProtocols.iterator();
        } else {
            dropTargetProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();
        }

        /* Grab server, since we are working with the window that belongs to
           another client. */
        XlibWrapper.XGrabServer(XToolkit.getDisplay());
        try {
            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
                if ((protocols == XEMBED_PROTOCOLS) ==
                    dropTargetProtocol.isXEmbedSupported()) {
                    dropTargetProtocol.registerEmbedderDropSite(embedder);
                }
            }
        } finally {
            XlibWrapper.XUngrabServer(XToolkit.getDisplay());
        }
    }

    public void updateEmbedderDropSite(long embedder) {
        XBaseWindow xbaseWindow = XToolkit.windowToXWindow(embedder);
        // No need to update our own drop sites.
        if (xbaseWindow != null) {
            return;
        }

        assert XToolkit.isAWTLockHeldByCurrentThread();

        Iterator<XDropTargetProtocol> dropTargetProtocols =
            XDragAndDropProtocols.getDropTargetProtocols();
        // The list of protocols supported by the embedder.
        List<XDropTargetProtocol> embedderProtocols = new ArrayList<>();

        while (dropTargetProtocols.hasNext()) {
            XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
            if (dropTargetProtocol.isProtocolSupported(embedder)) {
                embedderProtocols.add(dropTargetProtocol);
            }
        }

        embedderProtocols = Collections.unmodifiableList(embedderProtocols);

        Long lToplevel = Long.valueOf(embedder);
        boolean isXEmbedServer = false;
        synchronized (this) {
            EmbeddedDropSiteEntry entry = embeddedDropSiteRegistry.get(lToplevel);
            if (entry == null) {
                return;
            }
            entry.setSupportedProtocols(embedderProtocols);
            isXEmbedServer = !entry.hasNonXEmbedClientSites();
        }

        /*
         * By default, we register a drop site that supports all dnd
         * protocols. This approach is not appropriate in plugin
         * scenario if the browser supports Motif DnD and doesn't support
         * XDnD. If we forcibly set XdndAware on the browser toplevel, any drag
         * source that supports both protocols and prefers XDnD will be unable
         * to drop anything on the browser.
         * The solution for this problem is not to register XDnD drop site
         * if the browser supports only Motif DnD.
         * In general, if the browser already supports some protocols, we
         * register the embedded drop site only for those protocols. Otherwise
         * we register the embedded drop site for all protocols.
         */
        if (!embedderProtocols.isEmpty()) {
            dropTargetProtocols = embedderProtocols.iterator();
        } else {
            dropTargetProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();
        }

        /* Grab server, since we are working with the window that belongs to
           another client. */
        XlibWrapper.XGrabServer(XToolkit.getDisplay());
        try {
            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
                if (!isXEmbedServer || !dropTargetProtocol.isXEmbedSupported()) {
                    dropTargetProtocol.registerEmbedderDropSite(embedder);
                }
            }
        } finally {
            XlibWrapper.XUngrabServer(XToolkit.getDisplay());
        }
    }

    private void unregisterEmbedderDropSite(long embedder,
                                            EmbeddedDropSiteEntry entry) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        Iterator<XDropTargetProtocol> dropTargetProtocols =
            XDragAndDropProtocols.getDropTargetProtocols();

        /* Grab server, since we are working with the window that belongs to
           another client. */
        XlibWrapper.XGrabServer(XToolkit.getDisplay());
        try {
            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
                dropTargetProtocol.unregisterEmbedderDropSite(embedder);
            }

            long event_mask = entry.getEventMask();

            /* Restore the original event mask for the embedder. */
            if ((event_mask & XConstants.PropertyChangeMask) == 0) {
                XErrorHandlerUtil.WITH_XERROR_HANDLER(XErrorHandler.IgnoreBadWindowHandler.getInstance());
                XlibWrapper.XSelectInput(XToolkit.getDisplay(), embedder,
                                         event_mask);
                XErrorHandlerUtil.RESTORE_XERROR_HANDLER();

                if ((XErrorHandlerUtil.saved_error != null) &&
                    (XErrorHandlerUtil.saved_error.get_error_code() != XConstants.Success)) {
                    throw new XException("XSelectInput failed");
                }
            }
        } finally {
            XlibWrapper.XUngrabServer(XToolkit.getDisplay());
        }
    }

    private void registerEmbeddedDropSite(long toplevel, long window) {
        XBaseWindow xBaseWindow = XToolkit.windowToXWindow(window);
        boolean isXEmbedClient =
            (xBaseWindow instanceof XEmbeddedFramePeer) &&
            ((XEmbeddedFramePeer)xBaseWindow).isXEmbedActive();

        XEmbedCanvasPeer peer = null;
        {
            XBaseWindow xbaseWindow = XToolkit.windowToXWindow(toplevel);
            if (xbaseWindow != null) {
                if (xbaseWindow instanceof XEmbedCanvasPeer) {
                    peer = (XEmbedCanvasPeer)xbaseWindow;
                } else {
                    throw new UnsupportedOperationException();
                }
            }
        }

        Long lToplevel = Long.valueOf(toplevel);
        EmbeddedDropSiteEntry entry = null;
        synchronized (this) {
            entry = embeddedDropSiteRegistry.get(lToplevel);
            if (entry == null) {
                if (peer != null) {
                    // Toplevel is an XEmbed server within this VM.
                    // Register an XEmbed drop site.
                    peer.setXEmbedDropTarget();
                    // Create a dummy entry to register the embedded site.
                    entry = new EmbeddedDropSiteEntry(0, 0,
                                                      Collections.<XDropTargetProtocol>emptyList());
                } else {
                    // Foreign toplevel.
                    // Select for PropertyNotify events on the toplevel, so that
                    // we can track changes of the properties relevant to DnD
                    // protocols.
                    entry = registerEmbedderDropSite(toplevel);
                    // Register the toplevel with all DnD protocols that are not
                    // supported by XEmbed - actually setup a proxy, so that
                    // all DnD notifications sent to the toplevel are first
                    // routed to us.
                    registerProtocols(toplevel, NON_XEMBED_PROTOCOLS,
                                      entry.getSupportedProtocols());
                }
                embeddedDropSiteRegistry.put(lToplevel, entry);
            }
        }

        assert entry != null;

        synchronized (entry) {
            // For a foreign toplevel.
            if (peer == null) {
                if (!isXEmbedClient) {
                    // Since this is not an XEmbed client we can no longer rely
                    // on XEmbed to route DnD notifications even for DnD
                    // protocols that are supported by XEmbed.
                    // We rollback to the XEmbed-unfriendly solution - setup
                    // a proxy, so that all DnD notifications sent to the
                    // toplevel are first routed to us.
                    registerProtocols(toplevel, XEMBED_PROTOCOLS,
                                      entry.getSupportedProtocols());
                } else {
                    Iterator<XDropTargetProtocol> dropTargetProtocols =
                        XDragAndDropProtocols.getDropTargetProtocols();

                    // Register the embedded window as a plain drop site with
                    // all DnD protocols that are supported by XEmbed.
                    while (dropTargetProtocols.hasNext()) {
                        XDropTargetProtocol dropTargetProtocol =
                            dropTargetProtocols.next();
                        if (dropTargetProtocol.isXEmbedSupported()) {
                            dropTargetProtocol.registerEmbedderDropSite(window);
                        }
                    }
                }
            }

            entry.addSite(window, isXEmbedClient);
        }
    }

    private void unregisterEmbeddedDropSite(long toplevel, long window) {
        Long lToplevel = Long.valueOf(toplevel);
        EmbeddedDropSiteEntry entry = null;
        synchronized (this) {
            entry = embeddedDropSiteRegistry.get(lToplevel);
            if (entry == null) {
                return;
            }
            entry.removeSite(window);
            if (!entry.hasSites()) {
                embeddedDropSiteRegistry.remove(lToplevel);

                XBaseWindow xbaseWindow = XToolkit.windowToXWindow(toplevel);
                if (xbaseWindow != null) {
                    if (xbaseWindow instanceof XEmbedCanvasPeer) {
                        XEmbedCanvasPeer peer = (XEmbedCanvasPeer)xbaseWindow;
                        // Unregister an XEmbed drop site.
                        peer.removeXEmbedDropTarget();
                    } else {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    unregisterEmbedderDropSite(toplevel, entry);
                }
            }
        }
    }

    /*
     * Returns a drop site that is embedded in the specified embedder window and
     * contains the point with the specified root coordinates.
     */
    public long getEmbeddedDropSite(long embedder, int x, int y) {
        Long lToplevel = Long.valueOf(embedder);
        EmbeddedDropSiteEntry entry = embeddedDropSiteRegistry.get(lToplevel);
        if (entry == null) {
            return 0;
        }
        return entry.getSite(x, y);
    }

    /*
     * Note: this method should be called under AWT lock.
     */
    public void registerDropSite(long window) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        if (window == 0) {
            throw new IllegalArgumentException();
        }

        XDropTargetEventProcessor.activate();

        long toplevel = getToplevelWindow(window);

        /*
         * No window with WM_STATE property is found.
         * Since the window can be a plugin window reparented to the browser
         * toplevel, we cannot determine which window will eventually have
         * WM_STATE property set. So we schedule a timer callback that will
         * periodically attempt to find an ancestor with WM_STATE and
         * register the drop site appropriately.
         */
        if (toplevel == 0) {
            addDelayedRegistrationEntry(window);
            return;
        }

        if (toplevel == window) {
            Iterator<XDropTargetProtocol> dropTargetProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();

            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol =
                    dropTargetProtocols.next();
                dropTargetProtocol.registerDropTarget(toplevel);
            }
        } else {
            registerEmbeddedDropSite(toplevel, window);
        }
    }

    /*
     * Note: this method should be called under AWT lock.
     */
    public void unregisterDropSite(long window) {
        assert XToolkit.isAWTLockHeldByCurrentThread();

        if (window == 0) {
            throw new IllegalArgumentException();
        }

        long toplevel = getToplevelWindow(window);

        if (toplevel == window) {
            Iterator<XDropTargetProtocol> dropProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();

            removeDelayedRegistrationEntry(window);

            while (dropProtocols.hasNext()) {
                XDropTargetProtocol dropProtocol = dropProtocols.next();
                dropProtocol.unregisterDropTarget(window);
            }
        } else {
            unregisterEmbeddedDropSite(toplevel, window);
        }
    }

    public void registerXEmbedClient(long canvasWindow, long clientWindow) {
        // If the client has an associated XDnD drop site, add a drop target
        // to the XEmbedCanvasPeer's target to route drag notifications to the
        // client.

        XDragSourceProtocol xdndDragProtocol =
            XDragAndDropProtocols.getDragSourceProtocol(XDragAndDropProtocols.XDnD);
        XDragSourceProtocol.TargetWindowInfo info =
            xdndDragProtocol.getTargetWindowInfo(clientWindow);
        if (info != null &&
            info.getProtocolVersion() >= XDnDConstants.XDND_MIN_PROTOCOL_VERSION) {

            if (logger.isLoggable(PlatformLogger.Level.FINE)) {
                logger.fine("        XEmbed drop site will be registered for " + Long.toHexString(clientWindow));
            }
            registerEmbeddedDropSite(canvasWindow, clientWindow);

            Iterator<XDropTargetProtocol> dropTargetProtocols =
                XDragAndDropProtocols.getDropTargetProtocols();

            while (dropTargetProtocols.hasNext()) {
                XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
                dropTargetProtocol.registerEmbeddedDropSite(clientWindow);
            }

            if (logger.isLoggable(PlatformLogger.Level.FINE)) {
                logger.fine("        XEmbed drop site has been registered for " + Long.toHexString(clientWindow));
            }
        }
    }

    public void unregisterXEmbedClient(long canvasWindow, long clientWindow) {
        if (logger.isLoggable(PlatformLogger.Level.FINE)) {
            logger.fine("        XEmbed drop site will be unregistered for " + Long.toHexString(clientWindow));
        }
        Iterator<XDropTargetProtocol> dropTargetProtocols =
            XDragAndDropProtocols.getDropTargetProtocols();

        while (dropTargetProtocols.hasNext()) {
            XDropTargetProtocol dropTargetProtocol = dropTargetProtocols.next();
            dropTargetProtocol.unregisterEmbeddedDropSite(clientWindow);
        }

        unregisterEmbeddedDropSite(canvasWindow, clientWindow);

        if (logger.isLoggable(PlatformLogger.Level.FINE)) {
            logger.fine("        XEmbed drop site has beed unregistered for " + Long.toHexString(clientWindow));
        }
    }

    /**************** Delayed drop site registration *******************************/

    private void addDelayedRegistrationEntry(final long window) {
        Long lWindow = Long.valueOf(window);
        Runnable runnable = new Runnable() {
                public void run() {
                    removeDelayedRegistrationEntry(window);
                    registerDropSite(window);
                }
            };

        XToolkit.awtLock();
        try {
            removeDelayedRegistrationEntry(window);
            delayedRegistrationMap.put(lWindow, runnable);
            XToolkit.schedule(runnable, DELAYED_REGISTRATION_PERIOD);
        } finally {
            XToolkit.awtUnlock();
        }
    }

    private void removeDelayedRegistrationEntry(long window) {
        Long lWindow = Long.valueOf(window);

        XToolkit.awtLock();
        try {
            Runnable runnable = delayedRegistrationMap.remove(lWindow);
            if (runnable != null) {
                XToolkit.remove(runnable);
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }
    /*******************************************************************************/
}
