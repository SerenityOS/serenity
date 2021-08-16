/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Unit test for ISR/OSW constructors that take coders
 */

import java.io.*;
import java.nio.*;
import java.nio.charset.*;


public class IOCoders {

    static Charset ascii = Charset.forName("US-ASCII");

    static void isrPositive() throws Exception {
        ByteArrayInputStream bis
            = new ByteArrayInputStream(new byte[] { (byte)'h', (byte)'i' });
        InputStreamReader isr
            = new InputStreamReader(bis,
                                    ascii.newDecoder()
                                    .onMalformedInput(CodingErrorAction.REPORT)
                                    .onUnmappableCharacter(CodingErrorAction.REPORT));
        BufferedReader br = new BufferedReader(isr);
        if (!br.readLine().equals("hi"))
            throw new Exception();
    }

    static void isrNegative() throws Exception {
        ByteArrayInputStream bis
            = new ByteArrayInputStream(new byte[] { (byte)0xff, (byte)0xff });
        InputStreamReader isr
            = new InputStreamReader(bis,
                                    ascii.newDecoder()
                                    .onMalformedInput(CodingErrorAction.REPORT)
                                    .onUnmappableCharacter(CodingErrorAction.REPORT));
        BufferedReader br = new BufferedReader(isr);
        try {
            br.readLine();
        } catch (MalformedInputException x) {
            return;
        }
        throw new Exception();
    }

    static void oswPositive() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter osw
            = new OutputStreamWriter(bos,
                                     ascii.newEncoder()
                                     .onMalformedInput(CodingErrorAction.REPORT)
                                     .onUnmappableCharacter(CodingErrorAction.REPORT));
        osw.write("hi");
        osw.close();
        if (!ascii.decode(ByteBuffer.wrap(bos.toByteArray()))
            .toString().equals("hi"))
            throw new Exception();
    }

    static void oswNegative() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStreamWriter osw
            = new OutputStreamWriter(bos,
                                     ascii.newEncoder()
                                     .onMalformedInput(CodingErrorAction.REPORT)
                                     .onUnmappableCharacter(CodingErrorAction.REPORT));
        try {
            osw.write("\u00A0\u00A1");
        } catch (UnmappableCharacterException x) {
            return;
        }
        throw new Exception();
    }

    public static void main(String[] args) throws Exception {
        isrPositive();
        isrNegative();
        oswPositive();
        oswNegative();
    }

}
