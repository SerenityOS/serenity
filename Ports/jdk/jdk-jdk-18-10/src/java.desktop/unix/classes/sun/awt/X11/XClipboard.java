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

import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.util.SortedMap;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import sun.awt.UNIXToolkit;
import sun.awt.datatransfer.DataTransferer;
import sun.awt.datatransfer.SunClipboard;
import sun.awt.datatransfer.ClipboardTransferable;
import sun.security.action.GetIntegerAction;

/**
 * A class which interfaces with the X11 selection service in order to support
 * data transfer via Clipboard operations.
 */
public final class XClipboard extends SunClipboard implements OwnershipListener
{
    private final XSelection selection;
    // Time of calling XConvertSelection().
    private long convertSelectionTime;
    // The flag used not to call XConvertSelection() if the previous SelectionNotify
    // has not been processed by checkChange().
    private volatile boolean isSelectionNotifyProcessed;
    // The property in which the owner should place requested targets
    // when tracking changes of available data flavors (practically targets).
    private volatile XAtom targetsPropertyAtom;

    private static final Object classLock = new Object();

    private static final int defaultPollInterval = 200;

    private static int pollInterval;

    private static Map<Long, XClipboard> targetsAtom2Clipboard;

    /**
     * Creates a system clipboard object.
     */
    public XClipboard(String name, String selectionName) {
        super(name);
        selection = new XSelection(XAtom.get(selectionName));
        selection.registerOwershipListener(this);
    }

    /*
     * NOTE: This method may be called by privileged threads.
     *       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
     */
    public void ownershipChanged(final boolean isOwner) {
        if (isOwner) {
            checkChangeHere(contents);
        } else {
            lostOwnershipImpl();
        }
    }

    protected synchronized void setContentsNative(Transferable contents) {
        SortedMap<Long,DataFlavor> formatMap =
            DataTransferer.getInstance().getFormatsForTransferable
                (contents, DataTransferer.adaptFlavorMap(getDefaultFlavorTable()));
        long[] formats = DataTransferer.keysToLongArray(formatMap);

        if (!selection.setOwner(contents, formatMap, formats,
                                XToolkit.getCurrentServerTime())) {
            this.owner = null;
            this.contents = null;
        }
    }

    public long getID() {
        return selection.getSelectionAtom().getAtom();
    }

    @Override
    public synchronized Transferable getContents(Object requestor) {
        if (contents != null) {
            return contents;
        }
        return new ClipboardTransferable(this);
    }

    /* Caller is synchronized on this. */
    protected void clearNativeContext() {
        selection.reset();
    }


    protected long[] getClipboardFormats() {
        return selection.getTargets(XToolkit.getCurrentServerTime());
    }

    protected byte[] getClipboardData(long format) throws IOException {
        return selection.getData(format, XToolkit.getCurrentServerTime());
    }

    private void checkChangeHere(Transferable contents) {
        if (areFlavorListenersRegistered()) {
            checkChange(DataTransferer.getInstance().
                        getFormatsForTransferableAsArray(contents, getDefaultFlavorTable()));
        }
    }

    @SuppressWarnings("removal")
    private static int getPollInterval() {
        synchronized (XClipboard.classLock) {
            if (pollInterval <= 0) {
                pollInterval = AccessController.doPrivileged(
                        new GetIntegerAction("awt.datatransfer.clipboard.poll.interval",
                                             defaultPollInterval));
                if (pollInterval <= 0) {
                    pollInterval = defaultPollInterval;
                }
            }
            return pollInterval;
        }
    }

    private XAtom getTargetsPropertyAtom() {
        if (null == targetsPropertyAtom) {
            targetsPropertyAtom =
                    XAtom.get("XAWT_TARGETS_OF_SELECTION:" + selection.getSelectionAtom().getName());
        }
        return targetsPropertyAtom;
    }

    protected void registerClipboardViewerChecked() {
        // for XConvertSelection() to be called for the first time in getTargetsDelayed()
        isSelectionNotifyProcessed = true;

        boolean mustSchedule = false;
        XToolkit.awtLock();
        try {
            synchronized (XClipboard.classLock) {
                if (targetsAtom2Clipboard == null) {
                    targetsAtom2Clipboard = new HashMap<Long, XClipboard>(2);
                }
                mustSchedule = targetsAtom2Clipboard.isEmpty();
                targetsAtom2Clipboard.put(getTargetsPropertyAtom().getAtom(), this);
                if (mustSchedule) {
                    XToolkit.addEventDispatcher(XWindow.getXAWTRootWindow().getWindow(),
                                                new SelectionNotifyHandler());
                }
            }
            if (mustSchedule) {
                XToolkit.schedule(new CheckChangeTimerTask(), XClipboard.getPollInterval());
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    private static class CheckChangeTimerTask implements Runnable {
        public void run() {
            for (XClipboard clpbrd : targetsAtom2Clipboard.values()) {
                clpbrd.getTargetsDelayed();
            }
            synchronized (XClipboard.classLock) {
                if (targetsAtom2Clipboard != null && !targetsAtom2Clipboard.isEmpty()) {
                    // The viewer is still registered, schedule next poll.
                    XToolkit.schedule(this, XClipboard.getPollInterval());
                }
            }
        }
    }

    private static class SelectionNotifyHandler implements XEventDispatcher {
        public void dispatchEvent(XEvent ev) {
            if (ev.get_type() == XConstants.SelectionNotify) {
                final XSelectionEvent xse = ev.get_xselection();
                XClipboard clipboard = null;
                synchronized (XClipboard.classLock) {
                    if (targetsAtom2Clipboard != null && targetsAtom2Clipboard.isEmpty()) {
                        // The viewer was unregistered, remove the dispatcher.
                        XToolkit.removeEventDispatcher(XWindow.getXAWTRootWindow().getWindow(), this);
                        return;
                    }
                    final long propertyAtom = xse.get_property();
                    clipboard = targetsAtom2Clipboard.get(propertyAtom);
                }
                if (null != clipboard) {
                    clipboard.checkChange(xse);
                }
            }
        }
    }

    protected void unregisterClipboardViewerChecked() {
        isSelectionNotifyProcessed = false;
        synchronized (XClipboard.classLock) {
            targetsAtom2Clipboard.remove(getTargetsPropertyAtom().getAtom());
        }
    }

    // checkChange() will be called on SelectionNotify
    private void getTargetsDelayed() {
        XToolkit.awtLock();
        try {
            long curTime = System.currentTimeMillis();
            if (isSelectionNotifyProcessed || curTime >= (convertSelectionTime + UNIXToolkit.getDatatransferTimeout()))
            {
                convertSelectionTime = curTime;
                XlibWrapper.XConvertSelection(XToolkit.getDisplay(),
                                              selection.getSelectionAtom().getAtom(),
                                              XDataTransferer.TARGETS_ATOM.getAtom(),
                                              getTargetsPropertyAtom().getAtom(),
                                              XWindow.getXAWTRootWindow().getWindow(),
                                              XConstants.CurrentTime);
                isSelectionNotifyProcessed = false;
            }
        } finally {
            XToolkit.awtUnlock();
        }
    }

    /*
     * Tracks changes of available formats.
     * NOTE: This method may be called by privileged threads.
     *       DO NOT INVOKE CLIENT CODE ON THIS THREAD!
     */
    private void checkChange(XSelectionEvent xse) {
        final long propertyAtom = xse.get_property();
        if (propertyAtom != getTargetsPropertyAtom().getAtom()) {
            // wrong atom
            return;
        }

        final XAtom selectionAtom = XAtom.get(xse.get_selection());
        final XSelection changedSelection = XSelection.getSelection(selectionAtom);

        if (null == changedSelection || changedSelection != selection) {
            // unknown selection - do nothing
            return;
        }

        isSelectionNotifyProcessed = true;

        if (selection.isOwner()) {
            // selection is owner - do not need formats
            return;
        }

        long[] formats = null;

        if (propertyAtom == XConstants.None) {
            // We treat None property atom as "empty selection".
            formats = new long[0];
        } else {
            WindowPropertyGetter targetsGetter =
                new WindowPropertyGetter(XWindow.getXAWTRootWindow().getWindow(),
                                         XAtom.get(propertyAtom), 0,
                                         XSelection.MAX_LENGTH, true,
                                         XConstants.AnyPropertyType);
            try {
                targetsGetter.execute();
                formats = XSelection.getFormats(targetsGetter);
            } finally {
                targetsGetter.dispose();
            }
        }

        XToolkit.awtUnlock();
        try {
            checkChange(formats);
        } finally {
            XToolkit.awtLock();
        }
    }
}
