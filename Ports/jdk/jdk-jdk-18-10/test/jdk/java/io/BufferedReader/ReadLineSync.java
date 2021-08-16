/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 5073414
 * @summary Ensure that there is no race condition in BufferedReader.readLine()
 *          when a line is terminated by '\r\n' is read by multiple threads.
 */

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReadLineSync {

    public static int lineCount = 0;

    public static void main( String[] args ) throws Exception {

        String dir = System.getProperty(".", ".");
        File f = new File(dir, "test.txt");
        createFile(f);
        f.deleteOnExit();

        BufferedReader reader = new BufferedReader(
                                new FileReader(f));
        try {
            int threadCount = 2;

            ExecutorService es = Executors.newFixedThreadPool(threadCount);

            for (int i=0; i < threadCount; i++)
                es.execute(new BufferedReaderConsumer(reader));

            // Wait for the tasks to complete
            es.shutdown();
            while (!es.awaitTermination(60, TimeUnit.SECONDS));
        } finally {
            reader.close();
        }
    }

    static class BufferedReaderConsumer extends Thread {
        BufferedReader reader;

        public BufferedReaderConsumer( BufferedReader reader ) {
            this.reader = reader;
        }

        public void run() {
            try {
                String record = reader.readLine();

                if ( record == null ) {
                    // if the first thread is too fast the second will hit
                    // this which is ok
                    System.out.println( "File already finished" );
                    return;
                }

                if ( record.length() == 0 ) {
                    // usually it comes out here indicating the first read
                    // done by the second thread to run failed
                    System.out.println("Empty string on first read." +
                                Thread.currentThread().getName() );
                }

                while ( record != null ) {
                    lineCount++;

                    // Verify the token count
                    if ( record.length() == 0 ) {
                        // very occasionally it will fall over here
                        throw new Exception( "Invalid tokens with string '" +
                                record + "' on line " + lineCount );
                    }
                    record = reader.readLine();
                }
            }
            catch ( Exception e ) {
                e.printStackTrace();
            }
        }
    }


    // Create a relatively big file

    private static void createFile(File f) throws IOException {
        BufferedWriter w = new BufferedWriter(
                           new FileWriter(f));
        int count = 10000;
        while (count > 0) {

            w.write("abcd \r\n");
            w.write("efg \r\n");
            w.write("hijk \r\n");
            w.write("lmnop \r\n");
            w.write("qrstuv \r\n");
            w.write("wxy and z \r\n");
            w.write("now you \r\n");
            w.write("know your \r\n");
            w.write("abc \r\n");
            w.write("next time \r\n");
            w.write("want you \r\n");
            w.write("sing with me \r\n");

            count--;
        }
        w.close();
    }
}
