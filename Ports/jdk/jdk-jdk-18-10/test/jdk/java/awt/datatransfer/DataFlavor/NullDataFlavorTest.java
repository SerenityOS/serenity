/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

/*
  @test
  @key headful
  @bug 4682039
  @summary Tests that DataTransferer.getFormatsForFlavors() does not throw
           NullPointerException if some of given as parameter data flavors
           are null.
  @author gas@sparc.spb.su area=datatransfer
  @run main NullDataFlavorTest
*/

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class NullDataFlavorTest {

    private final static Clipboard clipboard =
        Toolkit.getDefaultToolkit().getSystemClipboard();

    public static void main(String[] args) throws Exception {
        boolean failed = false;

        try {
            clipboard.setContents(new NullSelection("DATA1",
                new DataFlavor[] { null, null, null }), null);
            clipboard.setContents(new NullSelection("DATA2",
                new DataFlavor[] { null, DataFlavor.stringFlavor, null }), null);
            clipboard.setContents(new NullSelection("DATA3", null), null);
        } catch (NullPointerException e) {
            failed = true;
            e.printStackTrace();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        if (failed) {
            throw new RuntimeException("test failed: NullPointerException " +
            "has been thrown");
        } else {
            System.err.println("test passed");
        }
    }
}

class NullSelection implements Transferable {

    private final DataFlavor[] flavors;

    private final String data;

    public NullSelection(String data, DataFlavor[] flavors) {
        this.data = data;
        this.flavors = flavors;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        for (DataFlavor fl : flavors) {
            if (flavor.equals(fl)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor)
        throws UnsupportedFlavorException, java.io.IOException
    {
        for (DataFlavor fl : flavors) {
            if (flavor.equals(fl)) {
                return data;
            }
        }
        throw new UnsupportedFlavorException(flavor);
    }

}
