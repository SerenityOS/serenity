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
  @bug 4274234
  @summary Tests that DataFlavor.getReaderForText() doesn't throw UnsupportedEncodingException for unicode text
  @author prs@sparc.spb.su: area=
  @modules java.datatransfer
  @run main ReaderForUnicodeText
*/

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class ReaderForUnicodeText {

   public static void main(String[] args) throws Exception {
      DataFlavor df = DataFlavor.plainTextFlavor;
      TextTransferable t = new TextTransferable();
      Reader reader;
      try {
          reader = df.getReaderForText(t);
      } catch (UnsupportedEncodingException e) {
           throw new RuntimeException("FAILED: Exception thrown in getReaderForText()");
      }
    }
}

class TextTransferable implements Transferable {

    String text = "Try to test me...";

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        DataFlavor flavors[] = {DataFlavor.plainTextFlavor};
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        if (flavor.match(DataFlavor.plainTextFlavor)) {
            return true;
        }
        return false;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws
        UnsupportedFlavorException, IOException {

        byte[] textBytes = null;

        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        String encoding = flavor.getParameter("charset");
        if (encoding == null) {
            textBytes = text.getBytes();
        } else {
            textBytes = text.getBytes(encoding);
        }
        return new ByteArrayInputStream(textBytes);
    }
}
