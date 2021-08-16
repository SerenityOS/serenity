/*
 * Copyright (c) 2000, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.datatransfer;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;


/**
 * Reads all of the data from the system Clipboard which the data transfer
 * subsystem knows how to translate. This includes all text data, File Lists,
 * Serializable objects, Remote objects, and properly registered, arbitrary
 * data as InputStreams. The data is stored in byte format until requested
 * by client code. At that point, the data is converted, if necessary, into
 * the proper format to deliver to the application.
 *
 * This hybrid pre-fetch/delayed-rendering approach allows us to circumvent
 * the API restriction that client code cannot lock the Clipboard to discover
 * its formats before requesting data in a particular format, while avoiding
 * the overhead of fully rendering all data ahead of time.
 *
 * @author David Mendenhall
 * @author Danila Sinopalnikov
 *
 * @since 1.4 (appeared in modified form as FullyRenderedTransferable in 1.3.1)
 */
public class ClipboardTransferable implements Transferable {
    private final Map<DataFlavor, Object> flavorsToData = new HashMap<>();
    private DataFlavor[] flavors = new DataFlavor[0];

    private final class DataFactory {
        final long format;
        final byte[] data;
        DataFactory(long format, byte[] data) {
            this.format = format;
            this.data   = data;
        }

        public Object getTransferData(DataFlavor flavor) throws IOException {
            return DataTransferer.getInstance().
                translateBytes(data, flavor, format,
                               ClipboardTransferable.this);
        }
    }

    public ClipboardTransferable(SunClipboard clipboard) {

        clipboard.openClipboard(null);

        try {
            long[] formats = clipboard.getClipboardFormats();

            if (formats != null && formats.length > 0) {
                // Since the SystemFlavorMap will specify many DataFlavors
                // which map to the same format, we should cache data as we
                // read it.
                Map<Long, Object> cached_data = new HashMap<>(formats.length, 1.0f);
                DataTransferer.getInstance()
                        .getFlavorsForFormats(formats, SunClipboard.getDefaultFlavorTable())
                        .entrySet()
                        .forEach(entry -> fetchOneFlavor(clipboard, entry.getKey(), entry.getValue(), cached_data));
                flavors = DataTransferer.setToSortedDataFlavorArray(flavorsToData.keySet());

            }
        } finally {
            clipboard.closeClipboard();
        }
    }

    private boolean fetchOneFlavor(SunClipboard clipboard, DataFlavor flavor,
                                   long format, Map<Long, Object> cached_data)
    {
        if (!flavorsToData.containsKey(flavor)) {
            Object data = null;

            if (!cached_data.containsKey(format)) {
                try {
                    data = clipboard.getClipboardData(format);
                } catch (IOException e) {
                    data = e;
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                // Cache this data, even if it's null, so we don't have to go
                // to native code again for this format.
                cached_data.put(format, data);
            } else {
                data = cached_data.get(format);
            }

            // Casting IOException to byte array causes ClassCastException.
            // We should handle IOException separately - do not wrap them into
            // DataFactory and report failure.
            if (data instanceof IOException) {
                flavorsToData.put(flavor, data);
                return false;
            } else if (data != null) {
                flavorsToData.put(flavor, new DataFactory(format, (byte[])data));
                return true;
            }
        }

        return false;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors.clone();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavorsToData.containsKey(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, IOException
    {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        Object ret = flavorsToData.get(flavor);
        if (ret instanceof IOException) {
            // rethrow IOExceptions generated while fetching data
            throw new IOException("Exception fetching data: ", (IOException)ret);
        } else if (ret instanceof DataFactory) {
            // Now we can render the data
            DataFactory factory = (DataFactory)ret;
            ret = factory.getTransferData(flavor);
        }
        return ret;
    }

}
