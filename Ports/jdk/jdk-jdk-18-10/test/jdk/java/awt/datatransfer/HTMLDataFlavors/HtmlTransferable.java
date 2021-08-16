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

/**
 * A transferable that mimic ie html data
 */
class HtmlTransferable implements Transferable {

    final static String SOURCE_HTML = "<html><head><title>Simple html content</title></head>" +
            "<body><ol><li>Dasha</li><li>Masha</li><li>Lida</li></ol></body></html>";

    // Data identical to ie output for the next html without end of lines,
    // that is gotten by java system clipboard
    // <html>
    // <head>
    // <title>Simple html content</title>
    // </head>
    // <body>
    // <ol>
    // <li>Dasha</li>
    // <li>Masha</li>
    // <li>Lida</li>
    // </ol>
    // </body>
    // </html>

    final static String ALL_HTML_AS_STRING = "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n" +
            "\n" +
            "<HTML><HEAD><TITLE>Simple html content</TITLE></HEAD>\n" +
            "\n" +
            "<BODY>\n" +
            "\n" +
            "<OL><!--StartFragment--><LI>Masha\n" +
            "<LI>Lida</LI><!--EndFragment--></OL>\n" +
            "</BODY>\n" +
            "</HTML>";

    final static String FRAGMENT_HTML_AS_STRING = "<LI>Masha\n" +
            "<LI>Lida</LI>";

    final static String SELECTION_HTML_AS_STRING =  "<LI>Masha" +
            "<LI>Lida</LI>";

    private DataFlavor[] supportedDataFlavors;

    final static DataFlavor[] htmlDataFlavors = new DataFlavor [] {
            DataFlavor.allHtmlFlavor,
            DataFlavor.fragmentHtmlFlavor,
            DataFlavor.selectionHtmlFlavor
    };

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return supportedDataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor supportedDataFlavor : supportedDataFlavors) {
            if (supportedDataFlavor.equals(flavor)) {
                return true;
            }
        }
        return false;
    }

    HtmlTransferable(DataFlavor[] supportedDataFlavors) {
        this.supportedDataFlavors = supportedDataFlavors;
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {

        if (isDataFlavorSupported(flavor)) {
            if (flavor.equals(DataFlavor.allHtmlFlavor)) {
                return ALL_HTML_AS_STRING;
            } else if (flavor.equals(DataFlavor.fragmentHtmlFlavor)) {
                return FRAGMENT_HTML_AS_STRING;
            } else if (flavor.equals(DataFlavor.selectionHtmlFlavor)) {
                return SELECTION_HTML_AS_STRING;
            }
        }

        throw new UnsupportedFlavorException(flavor);
    }

}
