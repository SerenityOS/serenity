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


public class ABCInputStream extends InputStream {

    int len;
    int chunk;
    int count = 0;
    char next = firstChar();

    ABCInputStream(int len) {
        this(len, len);
    }

    ABCInputStream(int len, int chunk) {
        this.len = len;
        this.chunk = chunk;
    }

    static char firstChar() {
        return 'a';
    }

    static char nextChar(char c) {
        if (c == 'z')
            return '0';
        else if (c == '9')
            return 'a';
        else
            return (char)(c + 1);
    }

    public int read() {
        if (count >= len)
            return -1;
        char c = next;
        next = nextChar(c);
        count++;
        return (byte) c;
    }

    public int read(byte buf[], int off, int len) {
        int n = (len > chunk) ? chunk : len;
        for (int i = off; i < off + n; i++) {
            int c = read();
            if (c == -1) {
                if (i > off)
                    return i - off;
                else
                    return -1;
            }
            buf[i] = (byte) c;
        }
        return n;
    }

    public int available() {
        int remaining = len - count;
        return (remaining > chunk) ? chunk : remaining;
    }

    public void close() throws IOException {
        if (len == 0)
            throw new IOException("Already closed");
        len = 0;
    }

}
