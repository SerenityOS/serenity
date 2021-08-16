/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 6877495
   @summary JTextField and JTextArea does not support supplementary characters
   @author Alexander Scherbatiy
   @modules java.datatransfer/sun.datatransfer
            java.desktop/sun.awt.datatransfer
   @run main SuplementaryCharactersTransferTest
*/


import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.datatransfer.*;
import sun.awt.datatransfer.*;
import sun.awt.datatransfer.DataTransferer.ReencodingInputStream;
import sun.datatransfer.DataFlavorUtil;

public class SuplementaryCharactersTransferTest {

    public static final long TEXT_FORMAT = 13;

    public static void main(String[] args) throws Exception {

        DataTransferer dataTransferer = new TestDataTransferer();
        dataTransferer.registerTextFlavorProperties("UNICODE TEXT", "utf-16le", "\r\n", "2");
        ByteTransferable transferable = new ByteTransferable();
        ReencodingInputStream is = dataTransferer.new ReencodingInputStream(transferable.getByteInputStream(), TEXT_FORMAT,
                DataFlavorUtil.getTextCharset(transferable.getDataFlavor()), transferable);

        byte[] bytes = transferable.getBytes();
        byte[] result = new byte[bytes.length];

        is.read(result);

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != result[i]) {
                throw new RuntimeException("Characters are not equal!");
            }
        }

    }

    static class ByteTransferable implements Transferable, ClipboardOwner {

        private final DataFlavor dataFlavor;

        public ByteTransferable() throws Exception {
            dataFlavor = DataFlavor.getTextPlainUnicodeFlavor();
        }

        public DataFlavor getDataFlavor() {
            return dataFlavor;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{dataFlavor};
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavor.equals(dataFlavor);
        }

        public byte[] getBytes() {
            return new byte[]{97, 0, 64, -40, 32, -36, 98, 0};
        }

        public InputStream getByteInputStream() {
            return new ByteArrayInputStream(getBytes());
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (flavor.equals(dataFlavor)) {
                return getByteInputStream();
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public void lostOwnership(Clipboard clipboard, Transferable contents) {
        }
    }

    static class TestDataTransferer extends DataTransferer {

        @Override
        public String getDefaultUnicodeEncoding() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isLocaleDependentTextFormat(long format) {
            return false;
        }

        @Override
        public boolean isFileFormat(long format) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public boolean isImageFormat(long format) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected Long getFormatForNativeAsLong(String str) {
            return TEXT_FORMAT;
        }

        @Override
        protected String getNativeForFormat(long format) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected ByteArrayOutputStream convertFileListToBytes(
                ArrayList<String> fileList) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected String[] dragQueryFile(byte[] bytes) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected byte[] imageToPlatformBytes(Image image, long format)
                throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ToolkitThreadBlockedHandler getToolkitThreadBlockedHandler() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        protected Image platformImageBytesToImage(byte[] bytes, long format) throws IOException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}

