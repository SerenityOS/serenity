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
   @summary Check line lengths using all recognized line-termination sequences
 */

import java.io.*;


public class LineLengths {

    static String enc = "UTF8";
    static String inEnc = null;
    static String outEnc = null;

    public static void main(String[] args) throws Exception {
        PrintWriter log
            = new PrintWriter(new OutputStreamWriter(System.err), true);

        if (inEnc == null)
            inEnc = enc;
        if (outEnc == null)
            outEnc = enc;

        PipedOutputStream uo = new PipedOutputStream();
        PipedInputStream ui = new PipedInputStream(uo);
        PipedOutputStream co = new PipedOutputStream();
        PipedInputStream ci = new PipedInputStream(co);

        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(co, outEnc));
        BufferedReader r = new BufferedReader(new InputStreamReader(ci, inEnc));

        Thread t1 = new Thread(new LineLengthsSource(uo, w, log));
        Thread t2 = new Thread(new LineSink(ui, r, log));
        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

}
