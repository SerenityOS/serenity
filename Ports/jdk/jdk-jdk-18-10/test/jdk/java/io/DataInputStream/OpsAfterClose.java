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
 *  @summary Test if DataInputStream methods will check if the stream
 *          has been closed.
 */

import java.io.*;

public enum OpsAfterClose {

        READ { boolean check(DataInputStream is) {
                    try {
                        int read = is.read();
                        System.out.println("read returns: " + read);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },

        READ_BUF { boolean check(DataInputStream is) {
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
        READ_BUF_OFF { boolean check(DataInputStream is) {
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
        AVAILABLE { boolean check(DataInputStream is) {
                    try {
                        int avail = is.available();
                        System.out.println("available() returns: " + avail);
                        return false;
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
             } },
        SKIP { boolean check(DataInputStream is) {
                    try {
                        long skipped = is.skip(1);
                        System.out.println("skip() returns: " + skipped);
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        MARK { boolean check(DataInputStream is) {
                    is.mark(20);
                    return true;
             } },
        RESET { boolean check(DataInputStream is) {
                    try {
                        is.reset();
                    } catch (IOException io) {
                        System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                        return true;
                    }
                    return false;
             } },
        MARK_SUPPORTED { boolean check(DataInputStream is) {
                    is.markSupported();
                    return true;
             } },
        CLOSE { boolean check(DataInputStream is) {
                try {
                    is.close();
                    return true; // No Exception thrown on windows
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true; // Exception thrown on solaris and linux
                }
            }},
        READ_BYTE { boolean check(DataInputStream is) {
                try {
                    is.readByte();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_CHAR { boolean check(DataInputStream is) {
                try {
                    is.readChar();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_DOUBLE { boolean check(DataInputStream is) {
                try {
                    is.readDouble();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },

        READ_FLOAT { boolean check(DataInputStream is) {
                try {
                    is.readFloat();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_INT { boolean check(DataInputStream is) {
                try {
                    is.readInt();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_LONG { boolean check(DataInputStream is) {
                try {
                    is.readLong();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_SHORT { boolean check(DataInputStream is) {
                try {
                    is.readShort();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_UnsignedByte { boolean check(DataInputStream is) {
                try {
                    is.readUnsignedByte();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_UnsignedShort { boolean check(DataInputStream is) {
                try {
                    is.readUnsignedShort();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_UTF { boolean check(DataInputStream is) {
                try {
                    is.readUTF();
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        SKIP_BYTES { boolean check(DataInputStream is) {
                try {
                    is.skipBytes(1);
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_FULLY { boolean check(DataInputStream is) {
                try {
                    is.readFully(new byte[1]);
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } },
        READ_FULLY_BUF { boolean check(DataInputStream is) {
                try {
                    is.readFully(new byte[1], 0, 1);
                } catch (IOException io) {
                    System.out.print("Excep Msg: "+ io.getMessage() + ", ");
                    return true;
                }
                return false;
             } };


    abstract boolean check(DataInputStream is);

    public static void main(String args[]) throws Exception {

        boolean failed = false;

        File f = new File(System.getProperty("test.dir", "."),
                          "f.txt");
        f.createNewFile();
        f.deleteOnExit();

        FileInputStream fis = new FileInputStream(f);
        try {
            DataInputStream dis = new DataInputStream(
                                    new FileInputStream(f));
            try {
                if (testDataInputStream(dis)) {
                    failed = true;
                }
            } finally {
                dis.close();
            }
        } finally {
            fis.close();
        }
    }

    private static boolean testDataInputStream(DataInputStream is)
            throws Exception {
        is.close();
        boolean failed = false;
        boolean result;
        System.out.println("Testing :" + is);
        for (OpsAfterClose op : OpsAfterClose.values()) {

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
}
