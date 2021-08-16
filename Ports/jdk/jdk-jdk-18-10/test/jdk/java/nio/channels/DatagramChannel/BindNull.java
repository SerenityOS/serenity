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

/* @test
 * @bug 7161881
 * @library /test/lib
 * @run main/othervm -Djava.net.preferIPv6Addresses=true BindNull
 * @summary Make sure the bind method uses an ipv4 address for the null case
 *          when the DatagramChannel is connected to an IPv4 socket and
 *          -Djava.net.preferIPv6Addresses=true.
 */

import java.io.*;
import java.net.*;
import java.nio.channels.*;
import jdk.test.lib.net.IPSupport;

public class BindNull {
    public static void main(String[] args) throws IOException {
        try (DatagramChannel dc = DatagramChannel.open()) {
            dc.bind(null);
        }
        if (IPSupport.hasIPv4()) {
            try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET)) {
                dc.bind(null);
            }
        }
        if (IPSupport.hasIPv6()) {
            try (DatagramChannel dc = DatagramChannel.open(StandardProtocolFamily.INET6)) {
                dc.bind(null);
            }
        }
    }
}
