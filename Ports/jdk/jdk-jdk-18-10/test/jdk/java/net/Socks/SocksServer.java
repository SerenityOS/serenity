/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;

public class SocksServer extends Thread implements Closeable {
    // Some useful SOCKS constant

    static final int PROTO_VERS4        = 4;
    static final int PROTO_VERS         = 5;
    static final int DEFAULT_PORT       = 1080;

    static final int NO_AUTH            = 0;
    static final int GSSAPI             = 1;
    static final int USER_PASSW         = 2;
    static final int NO_METHODS         = -1;

    static final int CONNECT            = 1;
    static final int BIND               = 2;
    static final int UDP_ASSOC          = 3;

    static final int IPV4               = 1;
    static final int DOMAIN_NAME        = 3;
    static final int IPV6               = 4;

    static final int REQUEST_OK         = 0;
    static final int GENERAL_FAILURE    = 1;
    static final int NOT_ALLOWED        = 2;
    static final int NET_UNREACHABLE    = 3;
    static final int HOST_UNREACHABLE   = 4;
    static final int CONN_REFUSED       = 5;
    static final int TTL_EXPIRED        = 6;
    static final int CMD_NOT_SUPPORTED  = 7;
    static final int ADDR_TYPE_NOT_SUP  = 8;

    private int port;
    private ServerSocket server;
    private boolean useV4 = false;
    private HashMap<String,String> users = new HashMap<>();
    private volatile boolean done = false;
    // Inner class to handle protocol with client
    // This is the bulk of the work (protocol handler)
    class ClientHandler extends Thread {
        private InputStream in;
        private OutputStream out;
        private Socket client;
        private Socket dest;

        // Simple tunneling class, moving bits from one stream to another

        class Tunnel extends Thread {
            private InputStream tin;
            private OutputStream tout;

            Tunnel(InputStream in, OutputStream out) {
                tin = in;
                tout = out;
            }

            public void run() {
                int b;
                while (true) {
                    try {
                        b = tin.read();
                        if (b == -1) {
                            tin.close();
                            tout.close();
                            return;
                        }
                        tout.write(b);
                        tout.flush();
                    } catch (IOException e) {
                        // actually exit from the thread
                        return;
                    }
                }
            }
        }

        ClientHandler(Socket s) throws IOException {
            client = s;
            in = new BufferedInputStream(client.getInputStream());
            out = new BufferedOutputStream(client.getOutputStream());
        }

        private void readBuf(InputStream is, byte[] buf) throws IOException {
            int l = buf.length;
            int count = 0;
            int i;
            do {
                i = is.read(buf, count, l - count);
                if (i == -1)
                    throw new IOException("unexpected EOF");
                count += i;
            } while (count < l);
        }


        private boolean userPassAuth() throws IOException {
            int ver = in.read();
            int ulen = in.read();
            if (ulen <= 0)
                throw new SocketException("SOCKS protocol error");
            byte[] buf = new byte[ulen];
            readBuf(in, buf);
            String uname = new String(buf);
            String password = null;
            ulen = in.read();
            if (ulen < 0)
                throw new SocketException("SOCKS protocol error");
            if (ulen > 0) {
                buf = new byte[ulen];
                readBuf(in, buf);
                password = new String(buf);
            }
            // Check username/password validity here
            System.err.println("User: '" + uname);
            System.err.println("PSWD: '" + password);
            if (users.containsKey(uname)) {
                String p1 = users.get(uname);
                System.err.println("p1 = " + p1);
                if (p1.equals(password)) {
                    out.write(PROTO_VERS);
                    out.write(REQUEST_OK);
                    out.flush();
                    return true;
                }
            }
            out.write(PROTO_VERS);
            out.write(NOT_ALLOWED);
            out.flush();
            return false;
        }

        private void purge() throws IOException {
            boolean done = false;
            int i = 0;
            client.setSoTimeout(1000);
            while(!done && i != -1) {
                try {
                    i = in.read();
                } catch(IOException e) {
                    done = true;
                }
            }
        }


        // Handle the SOCKS version 4 protocl

        private void getRequestV4() throws IOException {
            int ver = in.read();
            int cmd = in.read();
            if (ver == -1 || cmd == -1) {
                // EOF
                in.close();
                out.close();
                return;
            }

            if (ver != 0 && ver != 4) {
                out.write(PROTO_VERS4);
                out.write(91); // Bad Request
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                out.flush();
                purge();
                out.close();
                in.close();
                return;
            }

            if (cmd == CONNECT) {
                int port = ((in.read() & 0xff) << 8);
                port += (in.read() & 0xff);
                byte[] buf = new byte[4];
                readBuf(in, buf);
                InetAddress addr = InetAddress.getByAddress(buf);
                // We don't use the username...
                int c;
                do {
                    c = (in.read() & 0xff);
                } while (c!=0);
                boolean ok = true;
                try {
                    dest = new Socket(addr, port);
                } catch (IOException e) {
                    ok = false;
                }
                if (!ok) {
                    out.write(PROTO_VERS4);
                    out.write(91);
                    out.write(0);
                    out.write(0);
                    out.write(buf);
                    out.flush();
                    purge();
                    out.close();
                    in.close();
                    return;
                }
                out.write(PROTO_VERS4);
                out.write(90); // Success
                out.write((port >> 8) & 0xff);
                out.write(port & 0xff);
                out.write(buf);
                out.flush();
                InputStream in2 = new BufferedInputStream(dest.getInputStream());
                OutputStream out2 = new BufferedOutputStream(dest.getOutputStream());

                Tunnel tunnel = new Tunnel(in2, out);
                tunnel.start();

                int b = 0;
                do {
                    try {
                        b = in.read();
                        if (b == -1) {
                            in.close();
                            out2.close();
                            return;
                        }
                        out2.write(b);
                        out2.flush();
                    } catch (IOException ex) {
                    }
                } while (!client.isClosed());
            }
        }


        // Negociate the authentication scheme with the client
        private void negociate() throws IOException {
            int ver = in.read();
            int n = in.read();
            byte[] buf = null;
            if (n > 0) {
                buf = new byte[n];
                readBuf(in, buf);
            }
            int scheme = NO_AUTH;
            for (int i = 0; i < n; i++)
                if (buf[i] == USER_PASSW)
                    scheme = USER_PASSW;
            out.write(PROTO_VERS);
            out.write(scheme);
            out.flush();
            if (scheme == USER_PASSW)
                userPassAuth();
        }

        // Send error message then close the streams
        private void sendError(int code) {
            try {
                out.write(PROTO_VERS);
                out.write(code);
                out.write(0);
                out.write(IPV4);
                for (int i=0; i<6; i++)
                    out.write(0);
                out.flush();
                out.close();
            } catch (IOException ex) {
            }
        }

        // Actually connect the proxy to the destination then initiate tunneling

        private void doConnect(InetSocketAddress addr) throws IOException {
            dest = new Socket();
            try {
                dest.connect(addr, 10000);
            } catch (SocketTimeoutException ex) {
                sendError(HOST_UNREACHABLE);
                return;
            } catch (ConnectException cex) {
                sendError(CONN_REFUSED);
                return;
            }
            // Success
            InetAddress iadd = addr.getAddress();
            if (iadd instanceof Inet4Address) {
                out.write(PROTO_VERS);
                out.write(REQUEST_OK);
                out.write(0);
                out.write(IPV4);
                out.write(iadd.getAddress());
            } else if (iadd instanceof Inet6Address) {
                out.write(PROTO_VERS);
                out.write(REQUEST_OK);
                out.write(0);
                out.write(IPV6);
                out.write(iadd.getAddress());
            } else {
                sendError(GENERAL_FAILURE);
                return;
            }
            out.write((addr.getPort() >> 8) & 0xff);
            out.write((addr.getPort() >> 0) & 0xff);
            out.flush();

            InputStream in2 = new BufferedInputStream(dest.getInputStream());
            OutputStream out2 = new BufferedOutputStream(dest.getOutputStream());

            Tunnel tunnel = new Tunnel(in2, out);
            tunnel.start();

            int b = 0;
            do {
                // Note that the socket might be closed from another thread (the tunnel)
                try {
                    b = in.read();
                    if (b == -1) {
                        in.close();
                        out2.close();
                        return;
                    }
                    out2.write(b);
                    out2.flush();
                } catch(IOException ioe) {
                }
            } while (!client.isClosed());
        }

        private void doBind(InetSocketAddress addr) throws IOException {
            ServerSocket svr = new ServerSocket();
            svr.bind(null);
            InetSocketAddress bad = (InetSocketAddress) svr.getLocalSocketAddress();
            out.write(PROTO_VERS);
            out.write(REQUEST_OK);
            out.write(0);
            out.write(IPV4);
            out.write(bad.getAddress().getAddress());
            out.write((bad.getPort() >> 8) & 0xff);
            out.write((bad.getPort() & 0xff));
            out.flush();
            dest = svr.accept();
            bad = (InetSocketAddress) dest.getRemoteSocketAddress();
            out.write(PROTO_VERS);
            out.write(REQUEST_OK);
            out.write(0);
            out.write(IPV4);
            out.write(bad.getAddress().getAddress());
            out.write((bad.getPort() >> 8) & 0xff);
            out.write((bad.getPort() & 0xff));
            out.flush();
            InputStream in2 = dest.getInputStream();
            OutputStream out2 = dest.getOutputStream();

            Tunnel tunnel = new Tunnel(in2, out);
            tunnel.start();

            int b = 0;
            do {
                // Note that the socket might be close from another thread (the tunnel)
                try {
                    b = in.read();
                    if (b == -1) {
                        in.close();
                        out2.close();
                        return;
                    }
                    out2.write(b);
                    out2.flush();
                } catch(IOException ioe) {
                }
            } while (!client.isClosed());

        }

        // Handle the SOCKS v5 requests

        private void getRequest() throws IOException {
            int ver = in.read();
            int cmd = in.read();
            if (ver == -1 || cmd == -1) {
                in.close();
                out.close();
                return;
            }
            int rsv = in.read();
            int atyp = in.read();
            String addr = null;
            int port = 0;

            switch(atyp) {
            case IPV4:
                {
                byte[] buf = new byte[4];
                readBuf(in, buf);
                addr = InetAddress.getByAddress(buf).getHostAddress();
                }
                break;
            case DOMAIN_NAME:
                {
                int i = in.read();
                byte[] buf = new byte[i];
                readBuf(in, buf);
                addr = new String(buf);
                }
                break;
            case IPV6:
                {
                byte[] buf = new byte[16];
                readBuf(in, buf);
                addr = InetAddress.getByAddress(buf).getHostAddress();
                }
                break;
            }

            port = ((in.read()&0xff) << 8);
            port += (in.read()&0xff);

            InetSocketAddress socAddr = new InetSocketAddress(addr, port);
            switch(cmd) {
            case CONNECT:
                doConnect(socAddr);
                break;
            case BIND:
                doBind(socAddr);
                break;
            case UDP_ASSOC:
                // doUDP(socAddr);
                break;
            }
        }

        public void run() {
            String line = null;
            try {
                if (useV4) {
                    getRequestV4();
                } else {
                    negociate();
                    getRequest();
                }
            } catch (IOException ex) {
                try {
                    sendError(GENERAL_FAILURE);
                } catch (Exception e) {
                }
            } finally {
                try {
                    client.close();
                } catch (IOException e2) {
                }
            }
        }

    }

    public SocksServer(int port, boolean v4) throws IOException {
        this(port);
        this.useV4 = v4;
    }

    public SocksServer(int port) throws IOException {
        this.port = port;
        server = new ServerSocket();
        if (port == 0) {
            server.bind(null);
            this.port = server.getLocalPort();
        } else {
            server.bind(new InetSocketAddress(port));
        }
    }

    public SocksServer(InetAddress addr, int port, boolean useV4) throws IOException {
        this.port = port;
        this.useV4 = useV4;
        server = new ServerSocket();
        if (port == 0 && addr == null) {
            server.bind(null);
            this.port = server.getLocalPort();
        } else if (port == 0 && addr != null) {
            server.bind(new InetSocketAddress(addr, 0));
            this.port = server.getLocalPort();
        } else if (addr == null) {
            assert port != 0;
            server.bind(new InetSocketAddress(port));
        } else {
            assert port != 0;
            server.bind(new InetSocketAddress(addr, port));
        }
    }

    public SocksServer() throws IOException {
        this (DEFAULT_PORT);
    }

    public void addUser(String user, String passwd) {
        users.put(user, passwd);
    }

    public int getPort() {
        return port;
    }

    public void close() {
        done = true;
        try { server.close(); } catch (IOException unused) {}
    }

    public void run() {
        ClientHandler cl = null;
        while (!done) {
            try {
                Socket s = server.accept();
                cl = new ClientHandler(s);
                cl.start();
            } catch (IOException ex) {
                if (cl != null)
                    cl.interrupt();
            }
        }
    }
}
