/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.security.util;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.Arrays;

/**
 * A utility class for reading passwords
 *
 */
public class Password {
    /** Reads user password from given input stream. */
    public static char[] readPassword(InputStream in) throws IOException {
        return readPassword(in, false);
    }

    /** Reads user password from given input stream.
     * @param isEchoOn true if the password should be echoed on the screen
     */
    @SuppressWarnings("fallthrough")
    public static char[] readPassword(InputStream in, boolean isEchoOn)
            throws IOException {

        char[] consoleEntered = null;
        byte[] consoleBytes = null;

        try {
            // Use the new java.io.Console class
            Console con = null;
            if (!isEchoOn && in == System.in && ((con = System.console()) != null)) {
                consoleEntered = con.readPassword();
                // readPassword returns "" if you just print ENTER,
                // to be compatible with old Password class, change to null
                if (consoleEntered != null && consoleEntered.length == 0) {
                    return null;
                }
                consoleBytes = convertToBytes(consoleEntered);
                in = new ByteArrayInputStream(consoleBytes);
            }

            // Rest of the lines still necessary for KeyStoreLoginModule
            // and when there is no console.

            char[] lineBuffer;
            char[] buf;
            int i;

            buf = lineBuffer = new char[128];

            int room = buf.length;
            int offset = 0;
            int c;

            boolean done = false;
            while (!done) {
                switch (c = in.read()) {
                  case -1:
                  case '\n':
                      done = true;
                      break;

                  case '\r':
                    int c2 = in.read();
                    if ((c2 != '\n') && (c2 != -1)) {
                        if (!(in instanceof PushbackInputStream)) {
                            in = new PushbackInputStream(in);
                        }
                        ((PushbackInputStream)in).unread(c2);
                    } else {
                        done = true;
                        break;
                    }
                    /* fall through */
                  default:
                    if (--room < 0) {
                        buf = new char[offset + 128];
                        room = buf.length - offset - 1;
                        System.arraycopy(lineBuffer, 0, buf, 0, offset);
                        Arrays.fill(lineBuffer, ' ');
                        lineBuffer = buf;
                    }
                    buf[offset++] = (char) c;
                    break;
                }
            }

            if (offset == 0) {
                return null;
            }

            char[] ret = new char[offset];
            System.arraycopy(buf, 0, ret, 0, offset);
            Arrays.fill(buf, ' ');

            return ret;
        } finally {
            if (consoleEntered != null) {
                Arrays.fill(consoleEntered, ' ');
            }
            if (consoleBytes != null) {
                Arrays.fill(consoleBytes, (byte)0);
            }
        }
    }

    /**
     * Change a password read from Console.readPassword() into
     * its original bytes.
     *
     * @param pass a char[]
     * @return its byte[] format, similar to new String(pass).getBytes()
     */
    private static byte[] convertToBytes(char[] pass) {
        if (enc == null) {
            synchronized (Password.class) {
                enc = System.console()
                        .charset()
                        .newEncoder()
                        .onMalformedInput(CodingErrorAction.REPLACE)
                        .onUnmappableCharacter(CodingErrorAction.REPLACE);
            }
        }
        byte[] ba = new byte[(int)(enc.maxBytesPerChar() * pass.length)];
        ByteBuffer bb = ByteBuffer.wrap(ba);
        synchronized (enc) {
            enc.reset().encode(CharBuffer.wrap(pass), bb, true);
        }
        if (bb.position() < ba.length) {
            ba[bb.position()] = '\n';
        }
        return ba;
    }
    private static volatile CharsetEncoder enc;
}
