/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.URI;
import java.util.List;

class URIListTransferable implements Transferable {

    private final DataFlavor supportedFlavor;

    private List<URI> list;

    public URIListTransferable(List<URI> list) {
        try {
            this.supportedFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("FAILED: could not create a DataFlavor");
        }
        this.list = list;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { supportedFlavor };
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return supportedFlavor.equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (supportedFlavor.equals(flavor)) {
            return list.stream()
                    .map(URI::toASCIIString)
                    .collect(StringBuilder::new,
                             (builder, uri)-> { builder.append(uri).append("\r\n"); },
                             StringBuilder::append).toString();
        }
        throw new UnsupportedFlavorException(flavor);
    }
}
