/*
 * Copyright (c) 1997, Oracle and/or its affiliates. All rights reserved.
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
   @summary Stochastic test of LineNumberReader
 */

import java.io.*;


public class LineNumbers {

    static String enc = "UTF8";
    static String inEnc;
    static String outEnc;
    static int limit = 500;

    public static void main(String[] args) throws Exception {
        PrintWriter log
            = new PrintWriter(new OutputStreamWriter(System.err), true);

        if (inEnc == null)
            inEnc = enc;
        if (outEnc == null)
            outEnc = enc;

        PipedOutputStream co = new PipedOutputStream();
        PipedInputStream ci = new PipedInputStream(co);

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(co, outEnc));
        LineNumberReader r = new LineNumberReader(new InputStreamReader(ci, inEnc));

        Thread t1 = new Thread(new RandomLineSource(w, limit));
        Thread t2 = new Thread(new LineNumberSink(r, limit, log));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

}


class LineNumberSink implements Runnable {

    LineNumberReader r;
    int limit;
    PrintWriter log;

    LineNumberSink(LineNumberReader r, int limit, PrintWriter log) {
        this.r = r;
        this.limit = limit;
        this.log = log;
    }

    public void run() {
        String s;
        int n = 0;

        try {
            while ((s = r.readLine()) != null) {
                n++;
                int ln = r.getLineNumber();
                log.println("[" + ln + "] " + s.length());
                log.println(s);
                if (log.checkError())
                    log.println("Conversion errors"); /* #### */
                if (n != ln)
                    throw new RuntimeException("Line number mismatch: Expected " + n + ", got " + ln);
            }
            if (n != limit)
                throw new RuntimeException("Incorrect line count");
        }
        catch (IOException x) {
            throw new RuntimeException(x.toString());
        }
        log.println(n + " lines read");
    }

}
