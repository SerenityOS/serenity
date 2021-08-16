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
import java.util.regex.*;
import java.security.*;
import javax.net.ssl.*;

/*
 * This class handles one client connection. It will interpret and act on the
 * commands (like USER, GET, PUT etc...) sent through the socket passed to
 * the constructor.
 *
 * To function it needs to be provided 2 handlers, one for the filesystem
 * and one for authentication.
 * @see FileSystemHandler
 * @see AuthHandler
 * @see #setHandlers(FtpFileSystemHandler,FtpAuthHandler)
 */

public class FtpCommandHandler extends Thread {
    private FtpServer parent = null;
    private Socket cmd = null;
    private Socket oldCmd = null;
    private InetAddress clientAddr = null;
    private ServerSocket pasv = null;

    private BufferedReader in = null;

    private PrintStream out = null;

    private FtpFileSystemHandler fsh = null;
    private FtpAuthHandler auth = null;

    private boolean done = false;

    private String username = null;
    private String password = null;
    private String account = null;
    private boolean logged = false;
    private boolean epsvAll = false;
    private int dataPort = 0;
    private InetAddress dataAddress = null;
    private boolean pasvEnabled = true;
    private boolean portEnabled = true;
    private boolean extendedEnabled = true;
    private boolean binary = true;
    private String renameFrom = null;
    private long restart = 0;
    private boolean useCrypto = false;
    private boolean useDataCrypto = false;
    private SSLSocketFactory sslFact = null;

    private final int ERROR = -1;
    private final int QUIT = 0;
    private final int USER = 1;
    private final int PASS = 2;
    private final int CWD = 3;
    private final int CDUP = 4;
    private final int PWD = 5;
    private final int TYPE = 6;
    private final int NOOP = 7;
    private final int RETR = 8;
    private final int PORT = 9;
    private final int PASV = 10;
    private final int EPSV = 11;
    private final int EPRT = 12;
    private final int SYST = 13;
    private final int STOR = 14;
    private final int STOU = 15;
    private final int LIST = 16;
    private final int NLST = 17;
    private final int RNFR = 18;
    private final int RNTO = 19;
    private final int DELE = 20;
    private final int REST = 21;
    private final int AUTH = 22;
    private final int FEAT = 23;
    private final int CCC = 24;
    private final int PROT = 25;
    private final int PBSZ = 26;

    private String[] commands =
    { "QUIT", "USER", "PASS", "CWD", "CDUP", "PWD", "TYPE", "NOOP", "RETR",
      "PORT", "PASV", "EPSV", "EPRT", "SYST", "STOR", "STOU", "LIST", "NLST",
      "RNFR", "RNTO", "DELE", "REST", "AUTH", "FEAT", "CCC", "PROT", "PBSZ"
    };

    private boolean isPasvSet() {
        if (pasv != null && !pasvEnabled) {
            try {
                pasv.close();
            } catch ( IOException e) {

            }
            pasv = null;
        }
        if (pasvEnabled && pasv != null)
            return true;
        return false;
    }

    private OutputStream getOutDataStream() throws IOException {
        if (isPasvSet()) {
            Socket s = pasv.accept();
            if (useCrypto && useDataCrypto) {
                SSLSocket ssl = (SSLSocket) sslFact.createSocket(s, clientAddr.getHostName(), s.getPort(), true);
                ssl.setUseClientMode(false);
                s = ssl;
            }
            return s.getOutputStream();
        }
        if (dataAddress != null) {
            Socket s;
            if (useCrypto) {
                s = sslFact.createSocket(dataAddress, dataPort);
            } else
                s = new Socket(dataAddress, dataPort);
            dataAddress = null;
            dataPort = 0;
            return s.getOutputStream();
        }
        return null;
    }

    private InputStream getInDataStream() throws IOException {
        if (isPasvSet()) {
            Socket s = pasv.accept();
            if (useCrypto && useDataCrypto) {
                SSLSocket ssl = (SSLSocket) sslFact.createSocket(s, clientAddr.getHostName(), s.getPort(), true);
                ssl.setUseClientMode(false);
                s = ssl;
            }
            return s.getInputStream();
        }
        if (dataAddress != null) {
            Socket s;
            if (useCrypto) {
                s = sslFact.createSocket(dataAddress, dataPort);
            } else
                s = new Socket(dataAddress, dataPort);
            dataAddress = null;
            dataPort = 0;
            return s.getInputStream();
        }
        return null;
    }

    private void parsePort(String port_arg) throws IOException {
        if (epsvAll) {
            out.println("501 PORT not allowed after EPSV ALL.");
            return;
        }
        if (!portEnabled) {
            out.println("500 PORT command is disabled, please use PASV.");
            return;
        }
        StringBuffer host;
        int i = 0, j = 4;
        while (j > 0) {
            i = port_arg.indexOf(',', i + 1);
            if (i < 0)
                break;
            j--;
        }
        if (j != 0) {
            out.println("500 '" + port_arg + "': command not understood.");
            return;
        }
        try {
            host = new StringBuffer(port_arg.substring(0, i));
            for (j = 0; j < host.length(); j++)
                if (host.charAt(j) == ',')
                    host.setCharAt(j, '.');
            String ports = port_arg.substring(i + 1);
            i = ports.indexOf(',');
            dataPort = Integer.parseInt(ports.substring(0, i)) << 8;
            dataPort += (Integer.parseInt(ports.substring(i + 1)));
            dataAddress = InetAddress.getByName(host.toString());
            out.println("200 Command okay.");
        } catch (Exception ex3) {
            dataPort = 0;
            dataAddress = null;
            out.println("500 '" + port_arg + "': command not understood.");
        }
    }

    private void parseEprt(String arg) {
        if (epsvAll) {
            out.println("501 PORT not allowed after EPSV ALL");
            return;
        }
        if (!extendedEnabled || !portEnabled) {
            out.println("500 EPRT is disabled, use PASV instead");
            return;
        }
        Pattern p = Pattern.compile("\\|(\\d)\\|(.*)\\|(\\d+)\\|");
        Matcher m = p.matcher(arg);
        if (!m.find()) {
            out.println("500 '" + arg + "': command not understood.");
            return;
        }
        try {
            dataAddress = InetAddress.getByName(m.group(2));
        } catch (UnknownHostException e) {
            out.println("500 " + arg + ": invalid address.");
            dataAddress = null;
            return;
        }
        dataPort = Integer.parseInt(m.group(3));
        out.println("200 Command okay.");
    }

    private void doPasv() {
        if (!pasvEnabled) {
            out.println("500 PASV is disabled, use PORT.");
            return;
        }
        try {
            InetAddress rAddress = cmd.getLocalAddress();
            if (rAddress instanceof Inet6Address) {
                out.println("500 PASV illegal over IPv6 addresses, use EPSV.");
                return;
            }
            if (pasv == null)
                pasv = new ServerSocket(0, 0, rAddress);
            int port = pasv.getLocalPort();
            byte[] a = rAddress.getAddress();
            out.println("227 Entering Passive Mode " + a[0] + "," + a[1] + "," + a[2] + "," + a[3] + "," +
                        (port >> 8) + "," + (port & 0xff) );
        } catch (IOException e) {
            out.println("425 can't build data connection: Connection refused.");
        }
    }

    private void doEpsv(String arg) {
        if (!extendedEnabled || !pasvEnabled) {
            out.println("500 EPSV disabled, use PORT or PASV.");
            return;
        }
        if ("all".equalsIgnoreCase(arg)) {
            out.println("200 EPSV ALL Command successful.");
            epsvAll = true;
            return;
        }
        try {
            if (pasv == null)
                pasv = new ServerSocket(0, 0, parent.getInetAddress());
            int port = pasv.getLocalPort();
            out.println("229 Entering Extended Passive Mode (|||" + port + "|)");
        } catch (IOException e) {
            out.println("500 Can't create data connection.");
        }
    }

    private void doRetr(String arg) {
        try {
            OutputStream dOut = getOutDataStream();
            if (dOut != null) {
                InputStream dIn = fsh.getFile(arg);
                if (dIn == null) {
                    out.println("550 File not found.");
                    dOut.close();
                    return;
                }
                out.println("150 Opening " + (binary ? "BINARY " : "ASCII ") + " data connection for file " + arg +
                            "(" + fsh.getFileSize(arg) + " bytes).");
                if (binary) {
                    byte[] buf = new byte[2048];
                    dOut = new BufferedOutputStream(dOut);
                    int count;
                    if (restart > 0) {
                        dIn.skip(restart);
                        restart = 0;
                    }
                    do {
                        count = dIn.read(buf);
                        if (count > 0)
                            dOut.write(buf, 0, count);
                    } while (count >= 0);
                    dOut.close();
                    dIn.close();
                    out.println("226 Transfer complete.");
                }
            }
        } catch (IOException e) {

        }
    }

    private void doStor(String arg, boolean unique) {
        try {
            InputStream dIn = getInDataStream();
            if (dIn != null) {
                OutputStream dOut = fsh.putFile(arg);
                if (dOut == null) {
                    out.println("500 Can't create file " + arg);
                    dIn.close();
                    return;
                }
                out.println("150 Opening " + (binary ? "BINARY " : "ASCII ") + " data connection for file " + arg);
                if (binary) {
                    byte[] buf = new byte[2048];
                    dOut = new BufferedOutputStream(dOut);
                    int count;
                    do {
                        count = dIn.read(buf);
                        if (count > 0)
                            dOut.write(buf, 0, count);
                    } while (count >= 0);
                    dOut.close();
                    dIn.close();
                    out.println("226 Transfer complete.");
                }
            }
        } catch (IOException e) {

        }
    }

    private void doList() {
        try {
            OutputStream dOut = getOutDataStream();
            if (dOut != null) {
                InputStream dIn = fsh.listCurrentDir();
                if (dIn == null) {
                    out.println("550 File not found.");
                    dOut.close();
                    return;
                }
                out.println("150 Opening ASCII data connection for file list");
                byte[] buf = new byte[2048];
                dOut = new BufferedOutputStream(dOut);
                int count;
                do {
                    count = dIn.read(buf);
                    if (count > 0)
                        dOut.write(buf, 0, count);
                } while (count >= 0);
                dOut.close();
                dIn.close();
                out.println("226 Transfer complete.");
            }
        } catch (IOException e) {

        }
    }

    private boolean useTLS() {
        if (sslFact == null) {
            sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
        if (sslFact == null)
            return false;
        return true;
    }

    private void stopTLS() {
        if (useCrypto) {
            SSLSocket ssl = (SSLSocket) cmd;
            try {
                ssl.close();
            } catch (IOException e) {
                // nada
            }
            cmd = oldCmd;
            oldCmd = null;
            try {
                in = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                out = new PrintStream(cmd.getOutputStream(), true, "ISO8859_1");
            } catch (Exception ex) {

            }
        }
    }

    public void setHandlers(FtpFileSystemHandler f, FtpAuthHandler a) {
        fsh = f;
        auth = a;
    }

    public FtpCommandHandler(Socket cl, FtpServer p) {
        parent = p;
        cmd = cl;
        clientAddr = cl.getInetAddress();
    }

    public void terminate() {
        done = true;
    }

    private int parseCmd(StringBuffer cmd) {

        if (cmd == null || cmd.length() < 3) // Shortest command is 3 char long
            return ERROR;
        int blank = cmd.indexOf(" ");
        if (blank < 0)
            blank = cmd.length();
        if (blank < 3)
            return ERROR;
        String s = cmd.substring(0,blank);
        cmd.delete(0, blank + 1);
        System.out.println("parse: cmd = " + s + " arg = " +cmd.toString());
        for (int i = 0; i < commands.length; i++)
            if (s.equalsIgnoreCase(commands[i]))
                return i;
        // Unknown command
        return ERROR;
    }

    private boolean checkLogged() {
        if (!logged) {
            out.println("530 Not logged in.");
            return false;
        }
        return true;
    }

    public void run() {
        try {
            // cmd.setSoTimeout(2000);
            in = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
            out = new PrintStream(cmd.getOutputStream(), true, "ISO8859_1");
            // Below corrupted message style was intentional to test 8151586, please
            // make sure each message line not broken ftp communication (such as for
            // message line lenght >=4, the 4th char required '-' to allow
            // implementation thinks that it has seen multi-line reply '###-'
            // sequence), otherwise it will affect normal ftp tests which depends
            // on this.
            out.println("---------------------------------\n220 Java FTP test server"
                    + " (j2se 6.0) ready.\n \n   -            Please send commands\n"
                    + "-----------------------------\n\n\n");
            out.flush();
            if (auth.authType() == 0) // No auth needed
                logged = true;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        String str;
        StringBuffer buf;
        int res;
        while (!done) {
            try {
                str = in.readLine();
                System.out.println("line: " + str);
                if (str == null) {
                    System.out.println("EOF read from input");
                    break;
                }
                buf = new StringBuffer(str);
                res = parseCmd(buf);
                switch (res) {
                case ERROR:
                    out.println("500 '" + str +"': command not understood.");
                    break;
                case QUIT:
                    out.println("221 Goodbye.");
                    done = true;
                    break;
                case USER:
                    logged = false;
                    username = buf.toString();
                    if (auth.authType() > 1)
                        out.println("331 User name okay, need password.");
                    else {
                        if (auth.authenticate(username, null)) {
                            out.println("230 User logged in, proceed.");
                            logged = true;
                        } else {
                            out.println("331 User name okay, need password.");
                        }
                    }
                    break;
                case PASS:
                    if (logged || (username == null)) {
                        out.println("503 Login with USER first.");
                        break;
                    }
                    password = buf.toString();
                    if (auth.authType() == 3) {
                        out.println("332 Need account for login.");
                        break;
                    }
                    if (auth.authenticate(username, password)) {
                        logged = true;
                        out.println("230 User " + username + " logged in.");
                        break;
                    }
                    out.println("530 Login incorrect.");
                    username = null;
                    break;
                case CWD:
                    if (checkLogged()) {
                        String path = buf.toString();
                        if (fsh.cd(path)) {
                            out.println("250 CWD command successful.");
                        } else {
                            out.println("550 " + path + ": no such file or directory.");
                        }
                    }
                    break;
                case CDUP:
                    if (checkLogged()) {
                        if (fsh.cdUp())
                            out.println("250 CWD command successful.");
                        else
                            out.println("550 invalid path.");
                    }
                    break;
                case PWD:
                    if (checkLogged()) {
                        String s = fsh.pwd();
                        out.println("257 \"" + s + "\" is current directory");
                    }
                    break;
                case NOOP:
                    if (checkLogged()) {
                        out.println("200 NOOP command successful.");
                    }
                    break;
                case PORT:
                    if (checkLogged()) {
                        parsePort(buf.toString());
                    }
                    break;
                case EPRT:
                    if (checkLogged()) {
                        parseEprt(buf.toString());
                    }
                    break;
                case PASV:
                    if (checkLogged())
                        doPasv();
                    break;
                case EPSV:
                    if (checkLogged())
                        doEpsv(buf.toString());
                    break;
                case RETR:
                    if (checkLogged()) {
                        doRetr(buf.toString());
                    }
                    break;
                case SYST:
                    if (checkLogged()) {
                        out.println("215 UNIX Type: L8 Version: Java 6.0");
                    }
                    break;
                case TYPE:
                    if (checkLogged()) {
                        String arg = buf.toString();
                        if (arg.length() != 1 || "AIE".indexOf(arg.charAt(0)) < 0) {
                            out.println("500 'TYPE " + arg + "' command not understood.");
                            continue;
                        }
                        out.println("200 Type set to " + buf.toString() + ".");
                        if (arg.charAt(0) == 'I')
                            binary = true;
                        else
                            binary = false;
                    }
                    break;
                case STOR:
                case STOU:
                    // TODO: separate STOR and STOU (Store Unique)
                    if (checkLogged()) {
                        doStor(buf.toString(), false);
                    }
                    break;
                case LIST:
                    if (checkLogged()) {
                        doList();
                    }
                    break;
                case NLST:
                    // TODO: implememt
                    break;
                case DELE:
                    if (checkLogged()) {
                        String arg = buf.toString();
                        if (fsh.removeFile(arg)) {
                            out.println("250 file " + arg + " deleted.");
                            break;
                        }
                        out.println("550 " + arg + ": no such file or directory.");
                    }
                    break;
                case RNFR:
                    if (checkLogged()) {
                        if (renameFrom != null) {
                            out.println("503 Bad sequence of commands.");
                            break;
                        }
                        renameFrom = buf.toString();
                        if (fsh.fileExists(renameFrom)) {
                            out.println("350 File or directory exists, ready for destination name.");
                        } else {
                            out.println("550 " + renameFrom + ": no such file or directory");
                            renameFrom = null;
                        }
                    }
                    break;
                case RNTO:
                    if (checkLogged()) {
                        if (renameFrom == null) {
                            out.println("503 Bad sequence of commands.");
                            break;
                        }
                        if (fsh.rename(renameFrom, buf.toString())) {
                            out.println("250 Rename successful");
                        } else {
                            out.println("550 Rename ");
                        }
                        renameFrom = null;
                    }
                    break;
                case REST:
                    if (checkLogged()) {
                        String arg = buf.toString();
                        restart = Long.parseLong(arg);
                        if (restart > 0)
                            out.println("350 Restarting at " + restart + ". Send STORE or RETRIEVE to initiate transfer");
                        else
                            out.println("501 Syntax error in command of arguments.");
                    }
                    break;
                case FEAT:
                    out.println("211-Features:");
                    out.println(" REST STREAM");
                    out.println(" PBSZ");
                    out.println(" AUTH TLS");
                    out.println(" PROT P");
                    out.println(" CCC");
                    out.println("211 End");
                    break;
                case AUTH:
                    if ("TLS".equalsIgnoreCase(buf.toString()) && useTLS()) {
                        out.println("234 TLS Authentication OK.");
                        out.flush();
                        SSLSocket ssl;
                        String[] suites = sslFact.getSupportedCipherSuites();
                        try {
                            ssl = (SSLSocket) sslFact.createSocket(cmd, cmd.getInetAddress().getHostName(), cmd.getPort(), false);
                            ssl.setUseClientMode(false);
                            ssl.setEnabledCipherSuites(suites);
                            ssl.startHandshake();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                            out.println("550 Unable to create secure channel.");
                            break;
                        }
                        oldCmd = cmd;
                        cmd = ssl;
                        out = new PrintStream(cmd.getOutputStream(), true, "ISO8859_1");
                        in = new BufferedReader(new InputStreamReader(cmd.getInputStream()));
                        System.out.println("Secure socket created!");
                        useCrypto = true;
                        break;
                    }
                    out.println("501 Unknown or unsupported AUTH type");
                    break;
                case CCC:
                    out.println("200 Command OK.");
                    stopTLS();
                    break;
                case PROT:
                    String arg = buf.toString();
                    if ("C".equalsIgnoreCase(arg)) {
                        // PROT C : Clear protection level
                        // No protection on data channel;
                        useDataCrypto = false;
                        out.println("200 Command OK.");
                        break;
                    }
                    if ("P".equalsIgnoreCase(arg)) {
                        // PROT P : Private protection level
                        // Data channel is integrity and confidentiality protected
                        useDataCrypto = true;
                        out.println("200 Command OK.");
                        break;
                    }
                    out.println("537 Requested PROT level not supported by security mechanism.");
                    break;
                case PBSZ:
                    // TODO: finish
                    out.println("200 Command OK.");
                    break;

                }

            } catch (InterruptedIOException ie) {
                // loop
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            in.close();
            out.close();
            cmd.close();
        } catch (IOException e) {
        }
        parent.removeClient(this);
    }
}
