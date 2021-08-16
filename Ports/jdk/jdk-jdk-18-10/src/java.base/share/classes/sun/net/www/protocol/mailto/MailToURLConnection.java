/*
 * Copyright (c) 1996, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.www.protocol.mailto;

import java.net.URL;
import java.net.InetAddress;
import java.net.SocketPermission;
import java.io.*;
import java.security.Permission;

import jdk.internal.util.StaticProperty;
import sun.net.www.*;
import sun.net.smtp.SmtpClient;
import sun.net.www.ParseUtil;


/**
 * Handle mailto URLs. To send mail using a mailto URLConnection,
 * call <code>getOutputStream</code>, write the message to the output
 * stream, and close it.
 *
 */
public class MailToURLConnection extends URLConnection {
    InputStream is = null;
    OutputStream os = null;

    SmtpClient client;
    Permission permission;
    private int connectTimeout = -1;
    private int readTimeout = -1;

    MailToURLConnection(URL u) {
        super(u);

        MessageHeader props = new MessageHeader();
        props.add("content-type", "text/html");
        setProperties(props);
    }

    /**
     * Get the user's full email address - stolen from
     * HotJavaApplet.getMailAddress().
     */
    String getFromAddress() {
        String str = System.getProperty("user.fromaddr");
        if (str == null) {
            // Perform the property security check for user.name
            @SuppressWarnings("removal")
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPropertyAccess("user.name");
            }
            str = StaticProperty.userName();
            if (str != null) {
                String host = System.getProperty("mail.host");
                if (host == null) {
                    try {
                        host = InetAddress.getLocalHost().getHostName();
                    } catch (java.net.UnknownHostException e) {
                    }
                }
                str += "@" + host;
            } else {
                str = "";
            }
        }
        return str;
    }

    public void connect() throws IOException {
        client = new SmtpClient(connectTimeout);
        client.setReadTimeout(readTimeout);
    }

    @Override
    public synchronized OutputStream getOutputStream() throws IOException {
        if (os != null) {
            return os;
        } else if (is != null) {
            throw new IOException("Cannot write output after reading input.");
        }
        connect();

        String to = ParseUtil.decode(url.getPath());
        client.from(getFromAddress());
        client.to(to);

        os = client.startMessage();
        return os;
    }

    @Override
    public Permission getPermission() throws IOException {
        if (permission == null) {
            connect();
            String host = client.getMailHost() + ":" + 25;
            permission = new SocketPermission(host, "connect");
        }
        return permission;
    }

    @Override
    public void setConnectTimeout(int timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeouts can't be negative");
        connectTimeout = timeout;
    }

    @Override
    public int getConnectTimeout() {
        return (connectTimeout < 0 ? 0 : connectTimeout);
    }

    @Override
    public void setReadTimeout(int timeout) {
        if (timeout < 0)
            throw new IllegalArgumentException("timeouts can't be negative");
        readTimeout = timeout;
    }

    @Override
    public int getReadTimeout() {
        return readTimeout < 0 ? 0 : readTimeout;
    }
}
