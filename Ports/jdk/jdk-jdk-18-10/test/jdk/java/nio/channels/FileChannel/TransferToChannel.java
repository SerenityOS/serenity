/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4652496
 * @summary Test transferTo with different target channels
 * @run main TransferToChannel
 * @run main/othervm -Djdk.nio.enableFastFileTransfer TransferToChannel
 */

import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.io.*;
import java.util.Random;

public class TransferToChannel {

    static File file;
    static File outFile;
    static FileChannel in;
    // Chunk size should be larger than FileChannelImpl.TRANSFER_SIZE for good test
    static int CHUNK_SIZE = 1024 * 9;

    public static void main(String[] args) throws Exception {
        file = File.createTempFile("readingin", null);
        outFile = File.createTempFile("writingout", null);
        file.deleteOnExit();
        outFile.deleteOnExit();
        generateBigFile(file);
        FileInputStream fis = new FileInputStream(file);
        in = fis.getChannel();
        test1();
        test2();
        in.close();
        file.delete();
        outFile.delete();
    }

    static void test1() throws Exception {
        for (int i=0; i<10; i++) {
            transferFileToUserChannel();
            System.gc();
            System.err.println("Transferred file...");
        }
    }

    static void test2() throws Exception {
        for (int i=0; i<10; i++) {
            transferFileToTrustedChannel();
            System.gc();
            System.err.println("Transferred file...");
        }
    }

    static void transferFileToUserChannel() throws Exception {
        long remainingBytes = in.size();
        long size = remainingBytes;
        WritableByteChannel wbc = new WritableByteChannel() {
                Random rand = new Random(0);
                public int write(ByteBuffer src) throws IOException {
                    int read = src.remaining();
                    byte[] incoming = new byte[read];
                    src.get(incoming);
                    checkData(incoming, read);
                    return read == 0 ? -1 : read;
                }
                public boolean isOpen() {
                    return true;
                }
                public void close() throws IOException {
                }
                void checkData(byte[] incoming, int size) {
                    byte[] expected = new byte[size];
                    rand.nextBytes(expected);
                    for (int i=0; i<size; i++)
                        if (incoming[i] != expected[i])
                            throw new RuntimeException("Data corrupted");
                }
            };
        while (remainingBytes > 0) {
            long bytesTransferred = in.transferTo(size - remainingBytes,
                              Math.min(CHUNK_SIZE, remainingBytes), wbc);
            if (bytesTransferred >= 0)
                remainingBytes -= bytesTransferred;
            else
                throw new Exception("transfer failed");
        }
    }

    static void transferFileToTrustedChannel() throws Exception {
        long remainingBytes = in.size();
        long size = remainingBytes;
        FileOutputStream fos = new FileOutputStream(outFile);
        FileChannel out = fos.getChannel();
        while (remainingBytes > 0) {
            long bytesTransferred = in.transferTo(size - remainingBytes,
                                                  CHUNK_SIZE, out);
            if (bytesTransferred >= 0)
                remainingBytes -= bytesTransferred;
            else
                throw new Exception("transfer failed");
        }
        out.close();
    }

    static void generateBigFile(File file) throws Exception {
        OutputStream out = new BufferedOutputStream(
                           new FileOutputStream(file));
        byte[] randomBytes = new byte[1024];
        Random rand = new Random(0);
        for (int i = 0; i < 1000; i++) {
            rand.nextBytes(randomBytes);
            out.write(randomBytes);
        }
        out.flush();
        out.close();
    }
}
