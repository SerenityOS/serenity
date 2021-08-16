/*
 * Copyright (c) 1997, 2000, Oracle and/or its affiliates. All rights reserved.
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

/**/

import java.io.*;


class LineSink implements Runnable {

    DataInputStream ui;
    BufferedReader ti;
    int count;
    PrintWriter log;

    public LineSink(InputStream us, BufferedReader ts,
                    int count, PrintWriter log)
        throws IOException
        {
            this(us, ts, log);
            this.count = count;
        }

    public LineSink(InputStream us, BufferedReader ts, PrintWriter log)
        throws IOException
        {
            ui = new DataInputStream(us);
            ti = ts;
            this.count = Integer.MAX_VALUE;
            this.log = log;
        }

    private String readUTFLine() throws IOException {
        String s;
        try {
            s = ui.readUTF();
        }
        catch (EOFException x) {
            return null;
        }
        return s;
    }

    public void run() {
        try {
            for (int ln = 0; ln < count; ln++) {
                String us = readUTFLine();
                if (us == null) {
                    if (count < Integer.MAX_VALUE)
                        throw new RuntimeException("Premature EOF on UTF stream");
                    log.println("EOF on UTF stream");
                    break;
                }

                String ts = ti.readLine();
                if (ts == null) {
                    if (count < Integer.MAX_VALUE)
                        throw new RuntimeException("Premature EOF on char stream");
                    log.println("EOF on char stream");
                    break;
                }

                if (us.length() != ts.length()) {
                    log.println("Length mismatch: us = \""
                                + us + "\", ts = \""
                                + ts + "\"");
                    throw new RuntimeException("Line " + ln +
                                               ": Length mismatch: " +
                                               us.length() + " " + ts.length());
                }

                for (int i = 0; i < us.length(); i++) {
                    if (us.charAt(i) != ts.charAt(i))
                        throw new RuntimeException("Line " + ln +
                                                   ": Char mismatch: [" + i + "] " +
                                                   Integer.toHexString(us.charAt(i)) +
                                                   " " + Integer.toHexString(ts.charAt(i)));
                }
                log.println(ln + " " + ts.length());
            }

            if (readUTFLine() != null)
                throw new RuntimeException("Expected EOF on UTF stream");
            if (ti.readLine() != null)
                throw new RuntimeException("Expected EOF on char stream");
        } catch (IOException x) {
            throw new RuntimeException("Unexpected IOException: " + x);
        }
    }

}
