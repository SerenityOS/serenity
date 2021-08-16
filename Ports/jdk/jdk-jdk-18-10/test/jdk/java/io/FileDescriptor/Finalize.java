/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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

/**
 *
 * @test
 * @bug 6322678
 * @summary Test for making sure that FIS/FOS.finalize() will
 *          not disturb the FD that is still in use.
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class Finalize {

    static byte data[] = new byte[] {48, 49, 50, 51, 52, 53, 54, 55, 56, 57,};
    static String inFileName = "fd-in-test.txt";
    static String outFileName = "fd-out-test.txt";
    static File inFile;
    static File outFile;

    public static void main(String[] args)
                throws Exception {
        Thread.sleep(5000);

        inFile= new File(System.getProperty("test.dir", "."),
                        inFileName);
        inFile.createNewFile();
        inFile.deleteOnExit();
        writeToInFile();

        doFileInputStream();

        outFile  = new File(System.getProperty("test.dir", "."),
                        outFileName);
        outFile.createNewFile();
        outFile.deleteOnExit();

        doFileOutputStream();
        doRandomAccessFile();
        doFileChannel();
    }

    private static void doFileInputStream() throws Exception {

        /* Create initial FIS for file */
        FileInputStream fis1 = new FileInputStream(inFile);

       /* Get the FileDescriptor from the fis */
       FileDescriptor fd = fis1.getFD();

        /*
         * Create a new FIS based on the existing FD
         *    (so the two FIS's share the same native fd)
         */
        FileInputStream fis2 = new FileInputStream(fd);

        /* allow fis1 to be gc'ed */
        fis1 = null;
        int ret = 0;

        /* encourage gc */
        System.gc();
        Thread.sleep(200);

        while((ret = fis2.read()) != -1 ) {
            /*
             * read from fis2 - when fis1 is gc'ed and finalizer is run,
             * read should not fail
             */
            System.out.println("read from fis2:" + ret);
        }
        fis2.close();
  }

    private static void writeToInFile() throws IOException {
        FileOutputStream out = new FileOutputStream(inFile);
        out.write(data);
        out.close();
    }

    private static void doFileOutputStream()
                throws Exception {

        System.out.println("--------FileOutputStream Test Started----------");

       /*Create initial FIS for file */
        FileOutputStream fos1 = new FileOutputStream(outFile);

       /* Get the FileDescriptor from the fos */
        FileDescriptor fd = fos1.getFD();
        FileOutputStream fos2 = new FileOutputStream(fd);

        /* allow fos1 to be gc'ed */
        fos1 = null;

        /* encourage gc */
        System.gc();
        Thread.sleep(200);

        /*
         * write to fos2 - when fos1 is gc'ed and finalizer is run,
         * write to fos2 should not fail
         */
        fos2.write(data);
        System.out.println("wrote:" + data.length + " bytes to fos2");
        fos2.close();

        System.out.println("--------FileOutputStream Test Over----------");
        System.out.println();
    }


    private static void doRandomAccessFile()
                throws Exception {

        System.out.println("--------RandomAccessFile Read Test Started----------");

        // Create initial FIS for file
        RandomAccessFile raf = new RandomAccessFile(inFile, "r");

        /* Get the FileDescriptor from the fis */
        FileDescriptor fd = raf.getFD();

        /* Create a new FIS based on the existing FD
         * (so the two FIS's share the same native fd)
         */
        FileInputStream fis = new FileInputStream(fd);

       /* allow fis to be gc'ed */
        fis = null;
        int ret = 0;

        /* encourage gc */
        System.gc();
        Thread.sleep(50);

        /*
         * read from raf - when fis is gc'ed and finalizer is run,
         * read from raf should not fail
         */
        while((ret = raf.read()) != -1 ) {
            System.out.println("read from raf:" + ret);
        }
        raf.close();
        Thread.sleep(200);

        System.out.println("--------RandomAccessFile Write Test Started----------");
        System.out.println();

        raf = new RandomAccessFile(outFile, "rw");
        fd = raf.getFD();
        FileOutputStream fos = new FileOutputStream(fd);

        /* allow fos to be gc'ed */
        fos = null;

        /* encourage gc */
        System.gc();
        Thread.sleep(200);

        /*
         * write to raf - when fos is gc'ed and finalizer is run,
         * write to raf should not fail
         */
        raf.write(data);
        System.out.println("wrote:" + data.length + " bytes to raf");
        raf.close();

        System.out.println("--------RandomAccessFile Write Test Over----------");
        System.out.println();
    }

     private static void doFileChannel() throws Exception {

        System.out.println("--------FileChannel Read Test Started----------");
        System.out.println();

        FileInputStream fis1 = new FileInputStream(inFile);

        /* Get the FileDescriptor from the fis */
        FileDescriptor fd = fis1.getFD();

        /* Create a new FIS based on the existing FD
         * (so the two FIS's share the same native fd)
         */
        FileInputStream fis2 = new FileInputStream(fd);
        FileChannel fc2 = fis2.getChannel();

        /**
         * Encourage the GC
         */
        fis1 = null;
        System.gc();
        Thread.sleep(200);

        int ret = 1;
        ByteBuffer bb = ByteBuffer.allocateDirect(1);
        ret = fc2.read(bb);
        System.out.println("read " + ret + " bytes from fc2:");
        fc2.close();

        System.out.println("--------FileChannel Read Test Over----------");
        System.out.println();

        System.out.println("--------FileChannel Write Test Started----------");

        FileOutputStream fos1 = new FileOutputStream(outFile);

       /* Get the FileDescriptor from the fos */
        fd = fos1.getFD();
        FileOutputStream fos2 = new FileOutputStream(fd);
        fc2 = fos2.getChannel();

        /**
         * Encourage the GC
         */
        fos1 = null;
        System.gc();
        Thread.sleep(200);

        /*
         * write to fc2 - when fos1 is gc'ed and finalizer is run,
         * write to fc2 should not fail
         */
        bb = ByteBuffer.allocateDirect(data.length)
                       .put(data)
                       .flip();

        ret = fc2.write(bb);
        System.out.println("Wrote:" +  ret + " bytes to fc2");
        fc2.close();

        System.out.println("--------Channel Write Test Over----------");
    }
}
