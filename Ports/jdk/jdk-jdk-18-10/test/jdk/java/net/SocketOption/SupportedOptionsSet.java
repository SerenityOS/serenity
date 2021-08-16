/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import static java.lang.System.out;
import jdk.test.lib.net.IPSupport;

/*
 * @test
 * @bug 8143923
 * @library /test/lib
 * @summary java.net socket supportedOptions set depends on call order
 * @run main/othervm SupportedOptionsSet first
 * @run main/othervm SupportedOptionsSet second
 * @run main/othervm -Djava.net.preferIPv4Stack=true SupportedOptionsSet first
 * @run main/othervm -Djava.net.preferIPv4Stack=true SupportedOptionsSet second
 */

// Run with othervm as the implementation of the supported options sets, once
// calculated, stores them in a private static fields.

public class SupportedOptionsSet {

    public static void main(String[] args) throws IOException {
        IPSupport.throwSkippedExceptionIfNonOperational();

        if (args[0].equals("first"))
            first();
        else if (args[0].equals("second"))
            second();
    }

    static void first() throws IOException {
        try (Socket s = new Socket();
             ServerSocket ss = new ServerSocket())
        {
            Set<?> first = s.supportedOptions();
            Set<?> second = ss.supportedOptions();
            assertNotEqual(first, second,
                 "Socket and ServerSocket should have different options.");
        }
    }

    /** Tests with the order of access to supportedOptions reversed.  */
    static void second() throws IOException {
        try (ServerSocket ss = new ServerSocket();
             Socket s = new Socket())
        {
            Set<?> first = ss.supportedOptions();
            Set<?> second = s.supportedOptions();
            assertNotEqual(first, second,
                "ServerSocket and Socket should have different options.");
        }
    }

    static void assertNotEqual(Set<?> s1, Set<?> s2, String message) {
        if (s1.equals(s2)) {
            out.println("s1: " + s1);
            out.println("s2: " + s2);
            throw new RuntimeException(message);
        }
    }
}
