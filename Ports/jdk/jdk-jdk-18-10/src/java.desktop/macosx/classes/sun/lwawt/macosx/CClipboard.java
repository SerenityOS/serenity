/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.lwawt.macosx;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.io.NotSerializableException;
import java.util.*;

import sun.awt.datatransfer.*;


/**
* A class which interfaces with Cocoa's pasteboard in order to support
 * data transfer via Clipboard operations. Most of the work is provided by
 * sun.awt.datatransfer.DataTransferer.
 */

final class CClipboard extends SunClipboard {

    public CClipboard(String name) {
        super(name);
    }

    @Override
    public long getID() {
        return 0;
    }

    @Override
    protected void clearNativeContext() {
        // Leaving Empty, as WClipboard.clearNativeContext is empty as well.
    }

    @Override
    public synchronized Transferable getContents(Object requestor) {
        checkPasteboardAndNotify();
        return super.getContents(requestor);
    }

    @Override
    protected synchronized Transferable getContextContents() {
        checkPasteboardAndNotify();
        return super.getContextContents();
    }

    @Override
    protected void setContentsNative(Transferable contents) {
        FlavorTable flavorMap = getDefaultFlavorTable();
        // Don't use delayed Clipboard rendering for the Transferable's data.
        // If we did that, we would call Transferable.getTransferData on
        // the Toolkit thread, which is a security hole.
        //
        // Get all of the target formats into which the Transferable can be
        // translated. Then, for each format, translate the data and post
        // it to the Clipboard.
        DataTransferer dataTransferer = DataTransferer.getInstance();
        long[] formatArray = dataTransferer.getFormatsForTransferableAsArray(contents, flavorMap);
        declareTypes(formatArray, this);

        Map<Long, DataFlavor> formatMap = dataTransferer.getFormatsForTransferable(contents, flavorMap);
        for (Map.Entry<Long, DataFlavor> entry : formatMap.entrySet()) {
            long format = entry.getKey();
            DataFlavor flavor = entry.getValue();

            try {
                byte[] bytes = DataTransferer.getInstance().translateTransferable(contents, flavor, format);
                setData(bytes, format);
            } catch (IOException e) {
                // Fix 4696186: don't print exception if data with
                // javaJVMLocalObjectMimeType failed to serialize.
                // May remove this if-check when 5078787 is fixed.
                if (!(flavor.isMimeTypeEqual(DataFlavor.javaJVMLocalObjectMimeType) &&
                        e instanceof NotSerializableException)) {
                    e.printStackTrace();
                }
            }
        }

        notifyChanged();
    }

    @Override
    protected native long[] getClipboardFormats();
    @Override
    protected native byte[] getClipboardData(long format) throws IOException;

    // 1.5 peer method
    @Override
    protected void unregisterClipboardViewerChecked() {
        // no-op because we lack OS support. This requires 4048791, which requires 4048792
    }

    // 1.5 peer method
    @Override
    protected void registerClipboardViewerChecked()    {
        // no-op because we lack OS support. This requires 4048791, which requires 4048792
    }

    // 1.5 peer method
    // no-op. This appears to be win32 specific. Filed 4048790 for investigation
    //protected Transferable createLocaleTransferable(long[] formats) throws IOException;

    private native void declareTypes(long[] formats, SunClipboard newOwner);
    private native void setData(byte[] data, long format);

    void checkPasteboardAndNotify() {
        if (checkPasteboardWithoutNotification()) {
            notifyChanged();
            lostOwnershipNow(null);
        }
    }

    /**
     * Invokes native check whether a change count on the general pasteboard is different
     * than when we set it. The different count value means the current owner lost
     * pasteboard ownership and someone else put data on the clipboard.
     * @since 1.7
     */
    native boolean checkPasteboardWithoutNotification();

    /*** Native Callbacks ***/
    private void notifyLostOwnership() {
        lostOwnershipImpl();
    }

    private static void notifyChanged() {
        CClipboard clipboard = (CClipboard) Toolkit.getDefaultToolkit().getSystemClipboard();
        if (!clipboard.areFlavorListenersRegistered()) {
            return;
        }
        clipboard.checkChange(clipboard.getClipboardFormats());
    }
}
