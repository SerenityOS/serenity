/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4387882
 * @summary Need to revisit the javadocs for JSSE, especially the
 *      promoted classes.
 * @library /javax/net/ssl/templates
 * @modules jdk.crypto.ec
 * @run main/othervm SSLSessionNulls
 *
 *     SunJSSE does not support dynamic system properties, no way to re-use
 *     system properties in samevm/agentvm mode.
 * @author Brad Wetmore
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;

public class SSLSessionNulls extends SSLSocketTemplate {

    @Override
    protected void runServerApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslIS.read();
        sslOS.write(85);
        sslOS.flush();
    }

    @Override
    protected void runClientApplication(SSLSocket socket) throws Exception {
        InputStream sslIS = socket.getInputStream();
        OutputStream sslOS = socket.getOutputStream();

        sslOS.write(280);
        sslOS.flush();
        sslIS.read();

        SSLSession sslSession = socket.getSession();

        try {
            sslSession.getValue(null);
        } catch (IllegalArgumentException e) {
            System.out.print("Caught proper exception: ");
            System.out.println(e.getMessage());
        }

        try {
            sslSession.putValue(null, null);
        } catch (IllegalArgumentException e) {
            System.out.print("Caught proper exception: ");
            System.out.println(e.getMessage());
        }

        try {
            sslSession.removeValue(null);
        } catch (IllegalArgumentException e) {
            System.out.print("Caught proper exception: ");
            System.out.println(e.getMessage());
        }

        String [] names = sslSession.getValueNames();
        if ((names == null) || (names.length != 0)) {
            throw new IOException(
                "getValueNames didn't return 0-length arrary");
        }
    }

    public static void main(String[] args) throws Exception {
        new SSLSessionNulls().run();
    }
}
