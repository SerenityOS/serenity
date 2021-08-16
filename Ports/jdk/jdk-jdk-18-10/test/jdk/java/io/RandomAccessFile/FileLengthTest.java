/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4823133
 * @summary optimize RandomAccessFile.length() and length() is thread safe now.
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author vyom.tewari@oracle.com
 */
public class FileLengthTest {

    private static final int BUF_SIZE = 4096;
    private static RandomAccessFile randomAccessFile;
    private static Thread fileLengthCaller;
    private static Thread fileContentReader;
    private static StringBuilder fileContents;
    private static volatile boolean isFailed = false;

    /**
     * this thread will call length() in loop
     */
    private static void startLengthThread() {
        if (randomAccessFile == null) {
            return;
        }
        fileLengthCaller = new Thread(() -> {
            while (true) {
                try {
                    long length = randomAccessFile.length();
                    if (length < 0) {
                        return;
                    }
                } catch (IOException ex) {
                    return;
                }
            }
        });
        fileLengthCaller.setName("RandomAccessFile-length-caller");
        fileLengthCaller.setDaemon(true);
        fileLengthCaller.start();
    }

    /**
     * this thread will call read() and store the content in internal buffer.
     */
    private static void startReaderThread() {
        if (randomAccessFile == null) {
            return;
        }
        fileContentReader = new Thread(() -> {
            StringBuilder sb = new StringBuilder(BUF_SIZE);
            int i;
            byte arr[] = new byte[8];
            try {
                while ((i = randomAccessFile.read(arr)) != -1) {
                    sb.append(new String(arr, 0, i));
                }
                if (!sb.toString().equals(fileContents.toString())) {
                    isFailed = true;
                }
            } catch (IOException ex) {
            }
        });
        fileContentReader.setName("RandomAccessFile-content-reader");
        fileContentReader.setDaemon(true);
        fileContentReader.start();
    }

    public static void main(String args[]) {
        byte arr[] = new byte[BUF_SIZE];
        String testFile = "testfile.txt";
        try {
            createDummyFile(testFile);
            File file = new File(testFile);
            file.deleteOnExit();
            randomAccessFile = new RandomAccessFile(file, "r");
            int count = randomAccessFile.read(arr);
            randomAccessFile.seek(0);
            fileContents = new StringBuilder(BUF_SIZE);
            fileContents.append(new String(arr, 0, count));
            startLengthThread();
            startReaderThread();
            fileContentReader.join();
        } catch (FileNotFoundException | InterruptedException ex) {
        } catch (IOException ex) {
        } finally {
            try {
                randomAccessFile.close();
            } catch (IOException ex) {
            }
        }
        if (isFailed) {
            throw new RuntimeException("RandomAccessFile.length() changed the underlying file pointer.");
        }
    }

    private static void createDummyFile(String fileName) throws FileNotFoundException, IOException {
        try (FileOutputStream outputStream = new FileOutputStream(new File(fileName))) {
            String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
            int count = 0;
            while ((count + str.length()) < BUF_SIZE) {
                outputStream.write(str.getBytes());
                count += str.length();
            }
            outputStream.flush();
        }
    }
}
