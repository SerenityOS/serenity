/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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
 *  @bug  4181483
 *  @summary Test if InputStream methods will check if the stream
 *          has been closed.
 */

import java.io.*;

public enum OpsAfterClose {

        READ { boolean check(InputStream is) {
                    try {
                        int read = is.read();
                        System.out.println("read returns: " + read);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },

        READ_BUF { boolean check(InputStream is) {
                    try {
                        byte buf[] = new byte[2];
                        int read = is.read(buf);
                        System.out.println("read(buf) returns: " + read);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
            } },
        READ_BUF_OFF { boolean check(InputStream is) {
                    try {
                        byte buf[] = new byte[2];
                        int len = 1;
                        int read = is.read(buf, 0, len);
                        System.out.println("read(buf, 0, len) returns: " + read);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        AVAILABLE { boolean check(InputStream is) {
                    try {
                        int avail = is.available();
                        System.out.println("available() returns: " + avail);
                        return false;
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
             } },
        SKIP { boolean check(InputStream is) {
                    try {
                        long skipped = is.skip(1);
                        System.out.println("skip() returns: " + skipped);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        MARK { boolean check(InputStream is) {
                    is.mark(20);
                    return true;
             } },
        RESET { boolean check(InputStream is) {
                    try {
                        is.reset();
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        MARK_SUPPORTED { boolean check(InputStream is) {
                    is.markSupported();
                    return true;
             } },
        CLOSE { boolean check(InputStream is) {
                try {
                    is.close();
                    return true; // No Exception thrown on windows for FileInputStream
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true; // Exception thrown on solaris and linux for FileInputStream
                }
             } };

    abstract boolean check(InputStream is);

    public static void main(String args[]) throws Exception {

        boolean failed = false;

        File f = new File(System.getProperty("test.dir", "."),
                          "f.txt");
        f.createNewFile();
        f.deleteOnExit();

        FileInputStream fis = new FileInputStream(f);
        try {
            if (testInputStream(fis)) {
                failed = true;
            }
            if (testFileInputStream(fis)) {
                failed = true;
            }
        } finally {
            fis.close();
        }

        BufferedInputStream bs =  new BufferedInputStream(
                                        new FileInputStream(f));
        try {
            if (testInputStream(bs)) {
                failed = true;
            }
        } finally {
            bs.close();
        }

        DataInputStream dis = new DataInputStream(
                                new FileInputStream(f));
        try {
            if (testInputStream(dis)) {
                failed = true;
            }
        } finally {
            dis.close();
        }

        PushbackInputStream pbis = new PushbackInputStream(
                                new ByteArrayInputStream(new byte[20]));
        if (testInputStream(pbis)) {
            failed = true;
        }

        if (testPushbackInputStream(pbis)) {
            failed = true;
        }

        PipedInputStream pis = new PipedInputStream(new PipedOutputStream());
        if (testInputStream(pis)) {
            failed = true;
        }

        /**
         * The SequenceInputStream and  ObjectInputStream does not throw IOException

        SequenceInputStream sqis = new SequenceInputStream(
                                        new FileInputStream(f),
                                        new PipedInputStream(new PipedOutputStream())
                                    );
        if (testInputStream(sqis)) {
            failed = true;
        }

        String serStr = "abc";
        ObjectOutputStream oos = new ObjectOutputStream(
                                    new FileOutputStream(f));
        oos.writeObject(serStr);
        oos.close();

        ObjectInputStream ois = new ObjectInputStream(
                                    new FileInputStream(f));
        if (testInputStream(ois)) {
            failed = true;
        }

        */

        if (failed) {
            throw new Exception(
                "Some Op for some Stream failed, check the failed status above");
        }
    }

    private static boolean testInputStream(InputStream is)
            throws Exception {
        is.close();
        boolean failed = false;
        boolean result;
        System.out.println("Testing :" + is);
        for (OpsAfterClose op : OpsAfterClose.values()) {

            if (op.equals(AVAILABLE) && (is instanceof PipedInputStream)) {
                // skip the test as available() returns 0
                continue;
            }

            result = op.check(is);
            if (!result) {
                failed = true;
            }
           System.out.println(op + ":" + result);
        }
        if (failed) {
            System.out.println("Test failed for the failed operation{s}" +
                        " above for :" + is);
        }
        return failed;
    }

    private static boolean testPushbackInputStream(PushbackInputStream pis)
                throws Exception {
        boolean failed = false;
        try {
            pis.unread(1);
            System.out.println("Test failed for unread(int):" + pis);
            failed = true;
        } catch (IOException io) {
           System.out.println("UNREAD(int):true");
        }

        byte buf[] = new byte[2];
        try {
            pis.unread(buf, 0, 2);
            System.out.println("Test failed for unread(buf, offset, len):" +
                                pis);
            failed = true;
        } catch (IOException io) {
           System.out.println("UNREAD(buf, offset, len):true");
        }
        try {
            pis.unread(buf);
            System.out.println("Test failed for unread(char[] buf):" + pis);
            failed = true;
        } catch (IOException io) {
           System.out.println("UNREAD(buf):true");
        }
        return failed;
    }

    private static boolean testFileInputStream(FileInputStream fis)
                throws Exception {
        boolean failed = false;
        try {
            fis.getFD();
            System.out.println("GetFD: true");
        } catch (IOException io) {
           System.out.println("GetFD: false");
           failed = true;
        }
        fis.getChannel();
        System.out.println("GetChannel: true");
        return failed;
    }
}
