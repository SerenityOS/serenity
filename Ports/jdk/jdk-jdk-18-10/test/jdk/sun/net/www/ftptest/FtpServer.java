/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.net.*;
import java.io.*;
import java.util.ArrayList;

/**
 * This class implements a simple FTP server that can handle multiple
 * connections concurrently. This is mostly meant as a test environment.
 * You have to provide 2 handlers for it to be effective, one to simulate
 * (or access) a filesystem and one to deal with authentication.
 * See {@link FtpFileSystemHandler} and {@link FtpAuthHandler}.
 *
 * Since it is a subclass of Thread, you have to call <code>start()</code>
 * To get it running.
 *
 * Usage example:<p>
 *
 * <code>
 * FtpServer server = new FtpServer(0);
 * int port = server.getLocalPort();
 * server.setFileSystemHandler(myFSHandler);
 * server.setAuthHandler(myAuthHandler);
 * server.start();
 * </code>
 *
 */

public class FtpServer extends Thread implements AutoCloseable {
    private ServerSocket listener = null;
    private FtpFileSystemHandler fsh = null;
    private FtpAuthHandler auth = null;
    private boolean done = false;
    private ArrayList<FtpCommandHandler> clients = new ArrayList<FtpCommandHandler>();

    /**
     * Creates an instance of an FTP server which will listen for incoming
     * connections on the specified port. If the port is set to 0, it will
     * automatically select an available ephemeral port.
     */
    public FtpServer(InetAddress addr, int port) throws IOException {
        listener = new ServerSocket();
        listener.bind(new InetSocketAddress(addr, port));
    }

    /**
     * Creates an instance of an FTP server which will listen for incoming
     * connections on the specified port. If the port is set to 0, it will
     * automatically select an available ephemeral port.
     */
    public FtpServer(int port) throws IOException {
        listener = new ServerSocket(port);
    }

    /**
     * Creates an instance of an FTP server that will listen on the default
     * FTP port, usually 21.
     */
    public FtpServer() throws IOException {
        this(21);
    }

    public void setFileSystemHandler(FtpFileSystemHandler f) {
        fsh = f;
    }

    public void setAuthHandler(FtpAuthHandler a) {
        auth = a;
    }

    public void terminate() {
        done = true;
        interrupt();
    }

    public void killClients() {
        synchronized (clients) {
            int c = clients.size();
            while (c > 0) {
                c--;
                FtpCommandHandler cl = clients.get(c);
                cl.terminate();
                cl.interrupt();
            }
        }
    }

    public int getLocalPort() {
        return listener.getLocalPort();
    }

    public InetAddress getInetAddress() {
        return listener.getInetAddress();
    }

    public String getAuthority() {
        InetAddress address = getInetAddress();
        String hostaddr = address.isAnyLocalAddress()
            ? "localhost" : address.getHostAddress();
        if (hostaddr.indexOf(':') > -1) {
            hostaddr = "[" + hostaddr + "]";
        }
        return hostaddr + ":" + getLocalPort();
    }


    void addClient(Socket client) {
        FtpCommandHandler h = new FtpCommandHandler(client, this);
        h.setHandlers(fsh, auth);
        synchronized (clients) {
            clients.add(h);
        }
        h.start();
    }

    void removeClient(FtpCommandHandler cl) {
        synchronized (clients) {
            clients.remove(cl);
        }
    }

    public int activeClientsCount() {
        synchronized (clients) {
            return clients.size();
        }
    }

    public void run() {
        Socket client;

        try {
            while (!done) {
                client = listener.accept();
                addClient(client);
            }
            listener.close();
        } catch (IOException e) {

        }
    }

    @Override
    public void close() throws Exception {
        terminate();
        listener.close();
        if (activeClientsCount() > 0) {
            killClients();
        }
    }
}
