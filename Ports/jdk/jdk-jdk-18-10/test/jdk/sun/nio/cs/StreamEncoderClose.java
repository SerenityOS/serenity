/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5005426
 * @summary Check if StreamEncoder close() method works correctly from
 *          error recovery after the underneath OutputStream failed to
 *          close the first time.
 * @modules jdk.charsets
 */

import java.io.*;
public class StreamEncoderClose {
    public static void main( String arg[] ) throws Exception {
        byte[] expected = {(byte)0x1b,(byte)0x24,(byte)0x42,
                           (byte)0x30,(byte)0x6c,
                           (byte)0x1b,(byte)0x28,(byte)0x42};
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MyBufferedOutputStream mbos = new MyBufferedOutputStream(baos);
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(mbos, "ISO-2022-JP"));
        mbos.dontClose();
        pw.write("\u4e00");
        pw.close();             //  1st PrintWriter Close
        mbos.canClose();
        pw.close();             //  2nd PrintWriter Close

        //double check, probably not necessary
        byte[] out = baos.toByteArray();
        if (out.length != expected.length) {
            throw new IOException("Failed");
        }
        for (int i = 0; i < out.length; i++) {
            //System.out.printf("(byte)0x%x,", out[i] & 0xff);
            if (out[i] != expected[i])
                throw new IOException("Failed");
        }
    }

    static class MyBufferedOutputStream extends BufferedOutputStream {
        MyBufferedOutputStream(OutputStream os) {
            super(os);
        }
        private boolean status;
        public void dontClose() {
            status = false;
        }
        public void canClose() {
            status = true;
        }
        public void close() throws IOException {
            if ( status == false ) {
                throw new IOException("Can't close ");
            }
            super.close();
        }
    }
}
