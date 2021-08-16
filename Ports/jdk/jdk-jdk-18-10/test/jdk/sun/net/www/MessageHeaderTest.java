/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8003948
 * @modules java.base/sun.net.www
 * @run main MessageHeaderTest
 */
import java.io.*;
import sun.net.www.MessageHeader;

public class MessageHeaderTest {
    public static void main (String[] args) throws Exception {
        for (int i=0; i<7; i++) {
            ByteArrayInputStream bis = new ByteArrayInputStream(headers[i].getBytes());
            MessageHeader h = new MessageHeader(bis);
            String before = h.toString();
            before = before.substring(before.indexOf('{'));
            boolean result = h.filterNTLMResponses("WWW-Authenticate");
            String after = h.toString();
            after = after.substring(after.indexOf('{'));
            if (!expected[i].equals(after)) {
                throw new RuntimeException(Integer.toString(i) + " expected != after");
            }
            if (result != expectedResult[i]) {
                throw new RuntimeException(Integer.toString(i) + " result != expectedResult");
            }
        }
    }

    static String expected[] = {
        "{null: HTTP/1.1 200 Ok}{Foo: bar}{Bar: foo}{WWW-Authenticate: NTLM sdsds}",
        "{null: HTTP/1.1 200 Ok}{Foo: bar}{Bar: foo}{WWW-Authenticate: }",
        "{null: HTTP/1.1 200 Ok}{Foo: bar}{Bar: foo}{WWW-Authenticate: NTLM sdsds}",
        "{null: HTTP/1.1 200 Ok}{Foo: bar}{Bar: foo}{WWW-Authenticate: NTLM sdsds}",
        "{null: HTTP/1.1 200 Ok}{Foo: bar}{Bar: foo}{WWW-Authenticate: NTLM sdsds}{Bar: foo}",
        "{null: HTTP/1.1 200 Ok}{WWW-Authenticate: Negotiate}{Foo: bar}{Bar: foo}{WWW-Authenticate: NTLM}{Bar: foo}{WWW-Authenticate: Kerberos}",
        "{null: HTTP/1.1 200 Ok}{Foo: foo}{Bar: }{WWW-Authenticate: NTLM blob}{Bar: foo blob}"
    };

    static boolean[] expectedResult = {
        false, false, true, true, true, false, false
    };

    static String[] headers = {
        "HTTP/1.1 200 Ok\r\nFoo: bar\r\nBar: foo\r\nWWW-Authenticate: NTLM sdsds",
        "HTTP/1.1 200 Ok\r\nFoo: bar\r\nBar: foo\r\nWWW-Authenticate:",
        "HTTP/1.1 200 Ok\r\nFoo: bar\r\nBar: foo\r\nWWW-Authenticate: NTLM sdsds\r\nWWW-Authenticate: Negotiate",
        "HTTP/1.1 200 Ok\r\nFoo: bar\r\nBar: foo\r\nWWW-Authenticate: NTLM sdsds\r\nWWW-Authenticate: Negotiate\r\nWWW-Authenticate: Kerberos",
        "HTTP/1.1 200 Ok\r\nWWW-Authenticate: Negotiate\r\nFoo: bar\r\nBar: foo\r\nWWW-Authenticate: NTLM sdsds\r\nBar: foo\r\nWWW-Authenticate: Kerberos",
        "HTTP/1.1 200 Ok\r\nWWW-Authenticate: Negotiate\r\nFoo: bar\r\nBar: foo\r\nWWW-Authenticate: NTLM\r\nBar: foo\r\nWWW-Authenticate: Kerberos",
        "HTTP/1.1 200 Ok\r\nFoo: foo\r\nBar:\r\nWWW-Authenticate: NTLM blob\r\nBar: foo blob"
    };
}
