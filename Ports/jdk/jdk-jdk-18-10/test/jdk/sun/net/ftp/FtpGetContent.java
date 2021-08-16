/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;

/*
 * @test
 * @bug 4255280
 * @summary URL.getContent() loses first six bytes for ftp URLs
 * @run main FtpGetContent
 * @run main/othervm -Djava.net.preferIPv6Addresses=true FtpGetContent
 */

public class FtpGetContent {
    static int filesize = 2048;

    /**
     * A class that simulates, on a separate, an FTP server.
     */

    private class FtpServer extends Thread {
        private final ServerSocket    server;
        private int port;
        private boolean done = false;
        private boolean portEnabled = true;
        private boolean pasvEnabled = true;
        private boolean extendedEnabled = true;
        private String username;
        private String password;
        private String cwd;
        private String filename;
        private String type;
        private boolean list = false;

        /**
         * This Inner class will handle ONE client at a time.
         * That's where 99% of the protocol handling is done.
         */

        private class FtpServerHandler extends Thread {
            BufferedReader in;
            PrintWriter out;
            Socket client;
            private final int ERROR = 0;
            private final int USER = 1;
            private final int PASS = 2;
            private final int CWD = 3;
            private final int CDUP = 4;
            private final int PWD = 5;
            private final int TYPE = 6;
            private final int NOOP = 7;
            private final int RETR = 8;
            private final int PASV = 9;
            private final int PORT = 10;
            private final int LIST = 11;
            private final int REIN = 12;
            private final int QUIT = 13;
            private final int STOR = 14;
            private final int NLST = 15;
            private final int RNFR = 16;
            private final int RNTO = 17;
            private final int EPSV = 18;
            String[] cmds = { "USER", "PASS", "CWD", "CDUP", "PWD", "TYPE",
                              "NOOP", "RETR", "PASV", "PORT", "LIST", "REIN",
                              "QUIT", "STOR", "NLST", "RNFR", "RNTO", "EPSV"};
            private String arg = null;
            private ServerSocket pasv = null;
            private int data_port = 0;
            private InetAddress data_addr = null;

            /**
             * Parses a line to match it with one of the supported FTP commands.
             * Returns the command number.
             */

            private int parseCmd(String cmd) {
                System.out.println("FTP server received command: " + cmd);
                if (cmd == null || cmd.length() < 3)
                    return ERROR;
                int blank = cmd.indexOf(' ');
                if (blank < 0)
                    blank = cmd.length();
                if (blank < 3)
                    return ERROR;
                String s = cmd.substring(0, blank);
                if (cmd.length() > blank+1)
                    arg = cmd.substring(blank+1, cmd.length());
                else
                    arg = null;
                for (int i = 0; i < cmds.length; i++) {
                    if (s.equalsIgnoreCase(cmds[i]))
                        return i+1;
                }
                return ERROR;
            }

            public FtpServerHandler(Socket cl) {
                client = cl;
            }

            protected boolean isPasvSet() {
                if (pasv != null && !pasvEnabled) {
                    try {
                        pasv.close();
                    } catch (IOException ex) {
                    }
                    pasv = null;
                }
                if (pasvEnabled && pasv != null)
                    return true;
                return false;
            }

            /**
             * Open the data socket with the client. This can be the
             * result of a "PASV" or "PORT" command.
             */

            protected OutputStream getOutDataStream() {
                try {
                    if (isPasvSet()) {
                        Socket s = pasv.accept();
                        return s.getOutputStream();
                    }
                    if (data_addr != null) {
                        Socket s = new Socket(data_addr, data_port);
                        data_addr = null;
                        data_port = 0;
                        return s.getOutputStream();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            protected InputStream getInDataStream() {
                try {
                    if (isPasvSet()) {
                        Socket s = pasv.accept();
                        return s.getInputStream();
                    }
                    if (data_addr != null) {
                        Socket s = new Socket(data_addr, data_port);
                        data_addr = null;
                        data_port = 0;
                        return s.getInputStream();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * Handles the protocol exchange with the client.
             */

            public void run() {
                boolean done = false;
                String str;
                int res;
                boolean logged = false;
                boolean waitpass = false;

                try {
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    out = new PrintWriter(client.getOutputStream(), true);
                    out.println("220 tatooine FTP server (SunOS 5.8) ready.");
                } catch (Exception ex) {
                    return;
                }
                while (!done) {
                    try {
                        str = in.readLine();
                        res = parseCmd(str);
                        if ((res > PASS && res != QUIT) && !logged) {
                            out.println("530 Not logged in.");
                            continue;
                        }
                        switch (res) {
                        case ERROR:
                            out.println("500 '" + str + "': command not understood.");
                            break;
                        case USER:
                            if (!logged && !waitpass) {
                                username = str.substring(5);
                                password = null;
                                cwd = null;
                                if ("user2".equals(username)) {
                                    out.println("230 Guest login ok, access restrictions apply.");
                                    logged = true;
                                } else {
                                    out.println("331 Password required for " + arg);
                                    waitpass = true;
                                }
                            } else {
                                out.println("503 Bad sequence of commands.");
                            }
                            break;
                        case PASS:
                            if (!logged && waitpass) {
                                out.println("230 Guest login ok, access restrictions apply.");
                                password = str.substring(5);
                                logged = true;
                                waitpass = false;
                            } else
                                out.println("503 Bad sequence of commands.");
                            break;
                        case QUIT:
                            out.println("221 Goodbye.");
                            out.flush();
                            out.close();
                            if (pasv != null)
                                pasv.close();
                            done = true;
                            break;
                        case TYPE:
                            out.println("200 Type set to " + arg + ".");
                            type = arg;
                            break;
                        case CWD:
                            out.println("250 CWD command successful.");
                            if (cwd == null)
                                cwd = str.substring(4);
                            else
                                cwd = cwd + "/" + str.substring(4);
                            break;
                        case CDUP:
                            out.println("250 CWD command successful.");
                            break;
                        case PWD:
                            out.println("257 \"" + cwd + "\" is current directory");
                            break;
                        case EPSV:
                            if (!extendedEnabled || !pasvEnabled) {
                                out.println("500 EPSV is disabled, " +
                                                "use PORT instead.");
                                continue;
                            }
                            if (!(server.getInetAddress() instanceof Inet6Address)) {
                                // pretend EPSV is not implemented
                                out.println("500 '" + str + "': command not understood.");
                                break;
                            }
                            if ("all".equalsIgnoreCase(arg)) {
                                out.println("200 EPSV ALL command successful.");
                                continue;
                            }
                            try {
                                if (pasv == null)
                                    pasv = new ServerSocket(0, 0, server.getInetAddress());
                                int port = pasv.getLocalPort();
                                out.println("229 Entering Extended" +
                                        " Passive Mode (|||" + port + "|)");
                            } catch (IOException ssex) {
                                out.println("425 Can't build data connection:" +
                                                " Connection refused.");
                            }
                            break;
                        case PASV:
                            if (!pasvEnabled) {
                                out.println("500 PASV is disabled, use PORT instead.");
                                continue;
                            }
                            try {
                                if (pasv == null) {
                                    pasv = new ServerSocket();
                                    pasv.bind(new InetSocketAddress("127.0.0.1", 0));
                                }
                                int port = pasv.getLocalPort();
                                out.println("227 Entering Passive Mode (127,0,0,1," +
                                            (port >> 8) + "," + (port & 0xff) +")");
                            } catch (IOException ssex) {
                                out.println("425 Can't build data connection: Connection refused.");
                            }
                            break;
                        case PORT:
                            if (!portEnabled) {
                                out.println("500 PORT is disabled, use PASV instead");
                                continue;
                            }
                            StringBuffer host;
                            int i=0, j=4;
                            while (j>0) {
                                i = arg.indexOf(',', i+1);
                                if (i < 0)
                                    break;
                                j--;
                            }
                            if (j != 0) {
                                out.println("500 '" + arg + "': command not understood.");
                                continue;
                            }
                            try {
                                host = new StringBuffer(arg.substring(0,i));
                                for (j=0; j < host.length(); j++)
                                    if (host.charAt(j) == ',')
                                        host.setCharAt(j, '.');
                                String ports = arg.substring(i+1);
                                i = ports.indexOf(',');
                                data_port = Integer.parseInt(ports.substring(0,i)) << 8;
                                data_port += (Integer.parseInt(ports.substring(i+1)));
                                data_addr = InetAddress.getByName(host.toString());
                                out.println("200 Command okay.");
                            } catch (Exception ex3) {
                                data_port = 0;
                                data_addr = null;
                                out.println("500 '" + arg + "': command not understood.");
                            }
                            break;
                        case RETR:
                            {
                                filename = str.substring(5);
                                OutputStream dout = getOutDataStream();
                                if (dout != null) {
                                    out.println("200 Command okay.");
                                    BufferedOutputStream pout = new BufferedOutputStream(dout);
                                    for (int x = 0; x < filesize ; x++)
                                        pout.write(0);
                                    pout.flush();
                                    pout.close();
                                    list = false;
                                } else
                                    out.println("425 Can't build data connection: Connection refused.");
                            }
                            break;
                        case NLST:
                            filename = arg;
                        case LIST:
                            {
                                OutputStream dout = getOutDataStream();
                                if (dout != null) {
                                    out.println("200 Command okay.");
                                    PrintWriter pout = new PrintWriter(new BufferedOutputStream(dout));
                                    pout.println("total 130");
                                    pout.println("drwxrwxrwt   7 sys      sys          577 May 12 03:30 .");
                                    pout.println("drwxr-xr-x  39 root     root        1024 Mar 27 12:55 ..");
                                    pout.println("drwxrwxr-x   2 root     root         176 Apr 10 12:02 .X11-pipe");
                                    pout.println("drwxrwxr-x   2 root     root         176 Apr 10 12:02 .X11-unix");
                                    pout.println("drwxrwxrwx   2 root     root         179 Mar 30 15:09 .pcmcia");
                                    pout.println("drwxrwxrwx   2 jladen   staff        117 Mar 30 18:18 .removable");
                                    pout.println("drwxrwxrwt   2 root     root         327 Mar 30 15:08 .rpc_door");
                                    pout.println("-rw-r--r--   1 root     other         21 May  5 16:59 hello2.txt");
                                    pout.println("-rw-rw-r--   1 root     sys         5968 Mar 30 15:08 ps_data");
                                    pout.flush();
                                    pout.close();
                                    list = true;
                                } else
                                    out.println("425 Can't build data connection: Connection refused.");
                            }
                            break;
                        case STOR:
                            {
                                InputStream is = getInDataStream();
                                if (is != null) {
                                    out.println("200 Command okay.");
                                    BufferedInputStream din = new BufferedInputStream(is);
                                    int val;
                                    do {
                                        val = din.read();
                                    } while (val != -1);
                                    din.close();
                                } else
                                    out.println("425 Can't build data connection: Connection refused.");
                            }
                            break;
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        try {
                            out.close();
                        } catch (Exception ex2) {
                        }
                        done = true;
                    }
                }
            }
        }

        public FtpServer(int port) {
            this(null, 0);
        }

        public FtpServer(InetAddress address, int port) {
            this.port = port;
            try {
                server = new ServerSocket();
                server.bind(new InetSocketAddress(address, port));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public FtpServer() {
            this(21);
        }

        public int getPort() {
            if (server != null)
                return server.getLocalPort();
            return 0;
        }

        public String getAuthority() {
            InetAddress address = server.getInetAddress();
            String hostaddr = address.isAnyLocalAddress()
                ? "localhost" : address.getHostAddress();
            if (hostaddr.indexOf(':') > -1) {
                hostaddr = "[" + hostaddr +"]";
            }
            return hostaddr + ":" + getPort();
        }

        /**
         * A way to tell the server that it can stop.
         */
        synchronized public void terminate() {
            done = true;
        }

        synchronized boolean done() {
            return done;
        }

        synchronized public void setPortEnabled(boolean ok) {
            portEnabled = ok;
        }

        synchronized public void setPasvEnabled(boolean ok) {
            pasvEnabled = ok;
        }

        String getUsername() {
            return username;
        }

        String getPassword() {
            return password;
        }

        String pwd() {
            return cwd;
        }

        String getFilename() {
            return filename;
        }

        String getType() {
            return type;
        }

        boolean getList() {
            return list;
        }

        /*
         * All we got to do here is create a ServerSocket and wait for connections.
         * When a connection happens, we just have to create a thread that will
         * handle it.
         */
        public void run() {
            try {
                Socket client;
                while (!done()) {
                    client = server.accept();
                    (new FtpServerHandler(client)).start();
                }
            } catch(Exception e) {
            } finally {
                try { server.close(); } catch (IOException unused) {}
            }
        }
    }
    public static void main(String[] args) throws Exception {
        FtpGetContent test = new FtpGetContent();
    }

    public FtpGetContent() throws Exception {
        FtpServer server = null;
        try {
            InetAddress loopback = InetAddress.getLoopbackAddress();
            server = new FtpServer(loopback, 0);
            server.start();
            String authority = server.getAuthority();

            // Now let's check the URL handler

            URL url = new URL("ftp://" + authority + "/pub/BigFile");
            InputStream stream = (InputStream)url.openConnection(Proxy.NO_PROXY)
                                 .getContent();
            byte[] buffer = new byte[1024];
            int totalBytes = 0;
            int bytesRead = stream.read(buffer);
            while (bytesRead != -1) {
                totalBytes += bytesRead;
                bytesRead = stream.read(buffer);
            }
            stream.close();
            if (totalBytes != filesize)
                throw new RuntimeException("wrong file size!");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            server.terminate();
            server.server.close();
        }
    }
}
