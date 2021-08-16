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

/**/

import java.io.*;


class LineLengthsSource implements Runnable {

    DataOutputStream uo;
    BufferedWriter to;
    PrintWriter log;

    public LineLengthsSource(OutputStream us, BufferedWriter ts,
                             PrintWriter log)
        throws IOException
    {
        uo = new DataOutputStream(us);
        to = ts;
        this.log = log;
    }

    private void flush() throws IOException {
        uo.flush();
        Thread.currentThread().yield();
        to.flush();
        Thread.currentThread().yield();
    }

    private String termString(int t) {
        switch (t) {
        case 0: return "\n";
        case 1: return "\r";
        case 2: return "\r\n";
        default: return "";
        }
    }

    private String termName(int t) {
        switch (t) {
        case 0: return "\\n";
        case 1: return "\\r";
        case 2: return "\\r\\n";
        default: return "";
        }
    }

    private void go(int t) throws IOException {
        for (int ln = 0; ln < 128; ln++) {
            String ts = termString(t);
            StringBuffer s = new StringBuffer(ln + ts.length());
            for (int i = 0; i < ln; i++)
                s.append('x');
            log.println("[" + ln + "]" + termName(t));
            uo.writeUTF(s.toString());
            s.append(ts);
            to.write(s.toString());
            flush();
        }
    }

    public void run() {
        try {
            go(0);
            go(1);
            go(2);
            uo.close();
            Thread.currentThread().yield();
            to.close();
            Thread.currentThread().yield();
        }
        catch (IOException x) {
            return;             /* Probably pipe broken */
        }
    }

}
