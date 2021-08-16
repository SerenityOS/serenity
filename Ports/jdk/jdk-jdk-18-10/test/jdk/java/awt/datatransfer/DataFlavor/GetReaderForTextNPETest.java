/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  @bug 4260874
  @summary Tests that DataFlavor.getReaderForText do not throw NPE when transferObject is null
  @author tdv@sparc.spb.su: area=
  @modules java.datatransfer
  @run main GetReaderForTextNPETest
*/

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.Reader;

public class GetReaderForTextNPETest {

    public static void main(String[] args) {
        DataFlavor df = new DataFlavor();
        FakeTransferable t = new FakeTransferable();
        Reader reader;
        try {
            reader = df.getReaderForText(null);
        } catch (Exception e) {
            if (!(e instanceof NullPointerException)) {
                throw new RuntimeException("TEST FAILED: not a NPE thrown on a null argument.");
            }
        }
        try {
            reader = df.getReaderForText(t);
        } catch (Exception e) {
            if (!(e instanceof IllegalArgumentException)) {
                throw new RuntimeException("FAILED: not an IllegalArgumentException thrown on a transferable with null transfer data .");
            }
        }
    }
}

class FakeTransferable implements Transferable {
    public DataFlavor[] getTransferDataFlavors() {
        return null;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return false;
    }

    public Object getTransferData(DataFlavor flavor) throws
            UnsupportedFlavorException, IOException {
        return null;
    }
}
