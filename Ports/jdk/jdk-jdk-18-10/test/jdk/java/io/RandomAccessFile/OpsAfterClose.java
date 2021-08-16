/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 *  @bug 6359397
 *  @summary Test if RandomAccessFile methods will check if the stream
 *          has been closed.
 */

import java.io.*;

public enum OpsAfterClose {

        READ { boolean check(RandomAccessFile r) {
                    try {
                        r.read();
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },

        READ_BUF { boolean check(RandomAccessFile r) {
                    try {
                        byte buf[] = new byte[2];
                        int len = 1;
                        r.read(buf, 0, len);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        GET_CHANNEL { boolean check(RandomAccessFile r) {
                    r.getChannel();
                    return true;
             } },
        GET_FD { boolean check(RandomAccessFile r) {
                    try {
                        r.getFD();
                        return true;
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return false;
                    }
             } },
        GET_FILE_PTR { boolean check(RandomAccessFile r) {
                    try {
                        r.getFilePointer();
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        GET_LENGTH { boolean check(RandomAccessFile r) {
                    try {
                        r.length();
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        SEEK { boolean check(RandomAccessFile r) {
                    try {
                        r.seek(1);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        SET_LENGTH { boolean check(RandomAccessFile r) {
                    try {
                        r.setLength(1);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        SKIP_BYTES { boolean check(RandomAccessFile r) {
                    try {
                        r.skipBytes(1);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        WRITE { boolean check(RandomAccessFile r) {
                    try {
                        r.write(1);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        WRITE_BUF { boolean check(RandomAccessFile r) {
                    try {
                        byte buf[] = new byte[2];
                        int len = 1;
                        r.write(buf, 0, len);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        CLOSE { boolean check(RandomAccessFile r) {
                try {
                    r.close();
                    return true; // No Exception thrown on windows
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true; // Exception thrown on solaris and linux
                }
             } };

    abstract boolean check(RandomAccessFile r);

    public static void main(String args[]) throws Exception {

        boolean failed = false;

        File f = new File(System.getProperty("test.dir", "."),
                          "raf.txt");
        f.createNewFile();
        f.deleteOnExit();

        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        if (testRandomAccessFile(raf)) {
            throw new Exception("Test failed for some of the operation{s}" +
                " on RandomAccessFile, check the messages");
        }
    }

    private static boolean testRandomAccessFile(RandomAccessFile r)
            throws Exception {
        r.close();
        boolean failed = false;
        boolean result;
        System.out.println("Testing File:" + r);
        for (OpsAfterClose op : OpsAfterClose.values()) {
            result = op.check(r);
            if (!result) {
                failed = true;
            }
           System.out.println(op + ":" + result);
        }
        if (failed) {
            System.out.println("Test failed for the failed operation{s}" +
                        " above for the RandomAccessFile:" + r);
        }
        return failed;
    }
}
