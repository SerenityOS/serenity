/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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
 *  @test
 *  @bug 5085148
 *  @summary Test if PrintWriter methods check if the stream
 *           has been closed.
 */

import java.io.*;

public enum OpsAfterClose {

        WRITE_BUF { boolean check(PrintWriter w) {
                    char buf[] = new char[2];
                    w.write(buf);
                    return w.checkError();
            } },

        WRITE_BUF_OFF { boolean check(PrintWriter w) {
                    char buf[] = new char[2];
                    int len = 1;
                    w.write(buf, 0, len);
                    return w.checkError();
             } },
        WRITE_INT { boolean check(PrintWriter w) {
                    w.write(1);
                    return w.checkError();
             } },
        WRITE_STR { boolean check(PrintWriter w) {
                    String s = "abc";
                    w.write(s);
                    return w.checkError();
             } },
        WRITE_STR_OFF { boolean check(PrintWriter w) {
                    String s = "abc";
                    w.write(s, 0, s.length());
                    return w.checkError();
             } };

    abstract boolean check(PrintWriter w);

    public static void main(String args[]) throws Exception {

        System.out.println("Testing PrintWriter");
        boolean failed = false;
        boolean result = false;
        File f = new File(System.getProperty("test.dir", "."),
                          "print-writer.out");
        f.deleteOnExit();

        for (OpsAfterClose op : OpsAfterClose.values()) {
            PrintWriter pw = new PrintWriter(
                                new FileWriter(f));
            pw.close();
            result = op.check(pw);
            if (!result) {
                failed = true;
            }
           System.out.println(op + ":" + result);
        }
        if (failed) {
            throw new Exception(
                "Test failed for the failed operation{s} " +
                "above for the PrintWriter");
        }
    }
}
