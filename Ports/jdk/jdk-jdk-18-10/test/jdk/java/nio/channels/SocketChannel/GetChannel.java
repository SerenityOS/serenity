/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4403255
 * @summary Tests old streams using channels in socket case
 */

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;


public class GetChannel  {
    public static void main(String args[]) throws Exception {
    InetAddress  sin = null;
    Socket       soc = null,soc1 = null;
    InputStream  is = null;
    OutputStream os = null;
    ServerSocket srv = null;
    int          port = 0;
    int          tout = 1000;

    sin = InetAddress.getLocalHost();
    srv = new ServerSocket(port);
    port = srv.getLocalPort();
    soc = new Socket(sin, port);
    soc1 = srv.accept();

    BufferedReader bin = new BufferedReader(
                         new InputStreamReader(soc.getInputStream()));
    BufferedWriter bout = new BufferedWriter(
                          new OutputStreamWriter(soc1.getOutputStream()));

    bout.write("hello");
    bout.newLine();
    bout.flush();

    String reply = bin.readLine();
    if (!reply.equals("hello"))
        throw new RuntimeException("Test failed");

    soc.close();
    soc1.close();
    srv.close();
  }
}
