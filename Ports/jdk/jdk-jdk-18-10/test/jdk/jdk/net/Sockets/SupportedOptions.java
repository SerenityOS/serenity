/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8062744
 * @modules jdk.net
 * @run main SupportedOptions
 */

import java.net.*;
import java.io.IOException;
import jdk.net.*;

public class SupportedOptions {

    public static void main(String[] args) throws Exception {
        if (!Sockets.supportedOptions(ServerSocket.class)
              .contains(StandardSocketOptions.IP_TOS)) {
            throw new RuntimeException("Test failed");
        }
        // Now set the option
        ServerSocket ss = new ServerSocket();
        if (!ss.supportedOptions().contains(StandardSocketOptions.IP_TOS)) {
            throw new RuntimeException("Test failed");
        }
        Sockets.setOption(ss, java.net.StandardSocketOptions.IP_TOS, 128);
    }
}
