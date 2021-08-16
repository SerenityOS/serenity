/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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
package sun.net.ftp.impl;



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.text.DateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import sun.net.ftp.FtpDirEntry;
import sun.net.ftp.FtpDirParser;
import sun.net.ftp.FtpProtocolException;
import sun.net.ftp.FtpReplyCode;
import sun.net.util.IPAddressUtil;
import sun.util.logging.PlatformLogger;


public class FtpClient extends sun.net.ftp.FtpClient {

    private static int defaultSoTimeout;
    private static int defaultConnectTimeout;
    private static final PlatformLogger logger =
             PlatformLogger.getLogger("sun.net.ftp.FtpClient");
    private Proxy proxy;
    private Socket server;
    private PrintStream out;
    private InputStream in;
    private int readTimeout = -1;
    private int connectTimeout = -1;

    /* Name of encoding to use for output */
    private static String encoding = "ISO8859_1";
    /** remember the ftp server name because we may need it */
    private InetSocketAddress serverAddr;
    private boolean replyPending = false;
    private boolean loggedIn = false;
    private boolean useCrypto = false;
    private SSLSocketFactory sslFact;
    private Socket oldSocket;
    /** Array of strings (usually 1 entry) for the last reply from the server. */
    private Vector<String> serverResponse = new Vector<String>(1);
    /** The last reply code from the ftp daemon. */
    private FtpReplyCode lastReplyCode = null;
    /** Welcome message from the server, if any. */
    private String welcomeMsg;
    /**
     * Only passive mode used in JDK. See Bug 8010784.
     */
    private final boolean passiveMode = true;
    private TransferType type = TransferType.BINARY;
    private long restartOffset = 0;
    private long lastTransSize = -1; // -1 means 'unknown size'
    private String lastFileName;
    /**
     * Static members used by the parser
     */
    private static String[] patStrings = {
        // drwxr-xr-x  1 user01        ftp   512 Jan 29 23:32 prog
        "([\\-ld](?:[r\\-][w\\-][x\\-]){3})\\s*\\d+ (\\w+)\\s*(\\w+)\\s*(\\d+)\\s*([A-Z][a-z][a-z]\\s*\\d+)\\s*(\\d\\d:\\d\\d)\\s*(\\p{Print}*)",
        // drwxr-xr-x  1 user01        ftp   512 Jan 29 1997 prog
        "([\\-ld](?:[r\\-][w\\-][x\\-]){3})\\s*\\d+ (\\w+)\\s*(\\w+)\\s*(\\d+)\\s*([A-Z][a-z][a-z]\\s*\\d+)\\s*(\\d{4})\\s*(\\p{Print}*)",
        // 04/28/2006  09:12a               3,563 genBuffer.sh
        "(\\d{2}/\\d{2}/\\d{4})\\s*(\\d{2}:\\d{2}[ap])\\s*((?:[0-9,]+)|(?:<DIR>))\\s*(\\p{Graph}*)",
        // 01-29-97    11:32PM <DIR> prog
        "(\\d{2}-\\d{2}-\\d{2})\\s*(\\d{2}:\\d{2}[AP]M)\\s*((?:[0-9,]+)|(?:<DIR>))\\s*(\\p{Graph}*)"
    };
    private static int[][] patternGroups = {
        // 0 - file, 1 - size, 2 - date, 3 - time, 4 - year, 5 - permissions,
        // 6 - user, 7 - group
        {7, 4, 5, 6, 0, 1, 2, 3},
        {7, 4, 5, 0, 6, 1, 2, 3},
        {4, 3, 1, 2, 0, 0, 0, 0},
        {4, 3, 1, 2, 0, 0, 0, 0}};
    private static Pattern[] patterns;
    private static Pattern linkp = Pattern.compile("(\\p{Print}+) \\-\\> (\\p{Print}+)$");
    private DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, java.util.Locale.US);
    private static final boolean acceptPasvAddressVal;
    static {
        final int vals[] = {0, 0};
        final String acceptPasvAddress[] = {null};
        @SuppressWarnings("removal")
        final String enc = AccessController.doPrivileged(
                new PrivilegedAction<String>() {
                    public String run() {
                        acceptPasvAddress[0] = System.getProperty("jdk.net.ftp.trustPasvAddress", "false");
                        vals[0] = Integer.getInteger("sun.net.client.defaultReadTimeout", 300_000).intValue();
                        vals[1] = Integer.getInteger("sun.net.client.defaultConnectTimeout", 300_000).intValue();
                        return System.getProperty("file.encoding", "ISO8859_1");
                    }
                });
        if (vals[0] == 0) {
            defaultSoTimeout = -1;
        } else {
            defaultSoTimeout = vals[0];
        }

        if (vals[1] == 0) {
            defaultConnectTimeout = -1;
        } else {
            defaultConnectTimeout = vals[1];
        }

        encoding = enc;
        try {
            if (!isASCIISuperset(encoding)) {
                encoding = "ISO8859_1";
            }
        } catch (Exception e) {
            encoding = "ISO8859_1";
        }

        patterns = new Pattern[patStrings.length];
        for (int i = 0; i < patStrings.length; i++) {
            patterns[i] = Pattern.compile(patStrings[i]);
        }

        acceptPasvAddressVal = Boolean.parseBoolean(acceptPasvAddress[0]);
    }

    /**
     * Test the named character encoding to verify that it converts ASCII
     * characters correctly. We have to use an ASCII based encoding, or else
     * the NetworkClients will not work correctly in EBCDIC based systems.
     * However, we cannot just use ASCII or ISO8859_1 universally, because in
     * Asian locales, non-ASCII characters may be embedded in otherwise
     * ASCII based protocols (e.g. HTTP). The specifications (RFC2616, 2398)
     * are a little ambiguous in this matter. For instance, RFC2398 [part 2.1]
     * says that the HTTP request URI should be escaped using a defined
     * mechanism, but there is no way to specify in the escaped string what
     * the original character set is. It is not correct to assume that
     * UTF-8 is always used (as in URLs in HTML 4.0).  For this reason,
     * until the specifications are updated to deal with this issue more
     * comprehensively, and more importantly, HTTP servers are known to
     * support these mechanisms, we will maintain the current behavior
     * where it is possible to send non-ASCII characters in their original
     * unescaped form.
     */
    private static boolean isASCIISuperset(String encoding) throws Exception {
        String chkS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyz-_.!~*'();/?:@&=+$,";

        // Expected byte sequence for string above
        byte[] chkB = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70, 71, 72,
            73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 97, 98, 99,
            100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114,
            115, 116, 117, 118, 119, 120, 121, 122, 45, 95, 46, 33, 126, 42, 39, 40, 41, 59,
            47, 63, 58, 64, 38, 61, 43, 36, 44};

        byte[] b = chkS.getBytes(encoding);
        return java.util.Arrays.equals(b, chkB);
    }

    private class DefaultParser implements FtpDirParser {

        /**
         * Possible patterns:
         *
         *  drwxr-xr-x  1 user01        ftp   512 Jan 29 23:32 prog
         *  drwxr-xr-x  1 user01        ftp   512 Jan 29 1997 prog
         *  drwxr-xr-x  1 1             1     512 Jan 29 23:32 prog
         *  lrwxr-xr-x  1 user01        ftp   512 Jan 29 23:32 prog -> prog2000
         *  drwxr-xr-x  1 username      ftp   512 Jan 29 23:32 prog
         *  -rw-r--r--  1 jcc      staff     105009 Feb  3 15:05 test.1
         *
         *  01-29-97    11:32PM <DIR> prog
         *  04/28/2006  09:12a               3,563 genBuffer.sh
         *
         *  drwxr-xr-x  folder   0       Jan 29 23:32 prog
         *
         *  0 DIR 01-29-97 23:32 PROG
         */
        private DefaultParser() {
        }

        public FtpDirEntry parseLine(String line) {
            String fdate = null;
            String fsize = null;
            String time = null;
            String filename = null;
            String permstring = null;
            String username = null;
            String groupname = null;
            boolean dir = false;
            Calendar now = Calendar.getInstance();
            int year = now.get(Calendar.YEAR);

            Matcher m = null;
            for (int j = 0; j < patterns.length; j++) {
                m = patterns[j].matcher(line);
                if (m.find()) {
                    // 0 - file, 1 - size, 2 - date, 3 - time, 4 - year,
                    // 5 - permissions, 6 - user, 7 - group
                    filename = m.group(patternGroups[j][0]);
                    fsize = m.group(patternGroups[j][1]);
                    fdate = m.group(patternGroups[j][2]);
                    if (patternGroups[j][4] > 0) {
                        fdate += (", " + m.group(patternGroups[j][4]));
                    } else if (patternGroups[j][3] > 0) {
                        fdate += (", " + String.valueOf(year));
                    }
                    if (patternGroups[j][3] > 0) {
                        time = m.group(patternGroups[j][3]);
                    }
                    if (patternGroups[j][5] > 0) {
                        permstring = m.group(patternGroups[j][5]);
                        dir = permstring.startsWith("d");
                    }
                    if (patternGroups[j][6] > 0) {
                        username = m.group(patternGroups[j][6]);
                    }
                    if (patternGroups[j][7] > 0) {
                        groupname = m.group(patternGroups[j][7]);
                    }
                    // Old DOS format
                    if ("<DIR>".equals(fsize)) {
                        dir = true;
                        fsize = null;
                    }
                }
            }

            if (filename != null) {
                Date d;
                try {
                    d = df.parse(fdate);
                } catch (Exception e) {
                    d = null;
                }
                if (d != null && time != null) {
                    int c = time.indexOf(':');
                    now.setTime(d);
                    now.set(Calendar.HOUR, Integer.parseInt(time, 0, c, 10));
                    now.set(Calendar.MINUTE, Integer.parseInt(time, c + 1, time.length(), 10));
                    d = now.getTime();
                }
                // see if it's a symbolic link, i.e. the name if followed
                // by a -> and a path
                Matcher m2 = linkp.matcher(filename);
                if (m2.find()) {
                    // Keep only the name then
                    filename = m2.group(1);
                }
                boolean[][] perms = new boolean[3][3];
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        perms[i][j] = (permstring.charAt((i * 3) + j) != '-');
                    }
                }
                FtpDirEntry file = new FtpDirEntry(filename);
                file.setUser(username).setGroup(groupname);
                file.setSize(Long.parseLong(fsize)).setLastModified(d);
                file.setPermissions(perms);
                file.setType(dir ? FtpDirEntry.Type.DIR : (line.charAt(0) == 'l' ? FtpDirEntry.Type.LINK : FtpDirEntry.Type.FILE));
                return file;
            }
            return null;
        }
    }

    private static class MLSxParser implements FtpDirParser {
        public FtpDirEntry parseLine(String line) {
            String name = null;
            int i = line.lastIndexOf(';');
            if (i > 0) {
                name = line.substring(i + 1).trim();
                line = line.substring(0, i);
            } else {
                name = line.trim();
                line = "";
            }
            FtpDirEntry file = new FtpDirEntry(name);
            while (!line.isEmpty()) {
                String s;
                i = line.indexOf(';');
                if (i > 0) {
                    s = line.substring(0, i);
                    line = line.substring(i + 1);
                } else {
                    s = line;
                    line = "";
                }
                i = s.indexOf('=');
                if (i > 0) {
                    String fact = s.substring(0, i);
                    String value = s.substring(i + 1);
                    file.addFact(fact, value);
                }
            }
            String s = file.getFact("Size");
            if (s != null) {
                file.setSize(Long.parseLong(s));
            }
            s = file.getFact("Modify");
            if (s != null) {
                Date d = parseRfc3659TimeValue(s);
                if (d != null) {
                    file.setLastModified(d);
                }
            }
            s = file.getFact("Create");
            if (s != null) {
                Date d = parseRfc3659TimeValue(s);
                if (d != null) {
                    file.setCreated(d);
                }
            }
            s = file.getFact("Type");
            if (s != null) {
                if (s.equalsIgnoreCase("file")) {
                    file.setType(FtpDirEntry.Type.FILE);
                }
                if (s.equalsIgnoreCase("dir")) {
                    file.setType(FtpDirEntry.Type.DIR);
                }
                if (s.equalsIgnoreCase("cdir")) {
                    file.setType(FtpDirEntry.Type.CDIR);
                }
                if (s.equalsIgnoreCase("pdir")) {
                    file.setType(FtpDirEntry.Type.PDIR);
                }
            }
            return file;
        }
    };
    private FtpDirParser parser = new DefaultParser();
    private FtpDirParser mlsxParser = new MLSxParser();
    private static Pattern transPat = null;

    private void getTransferSize() {
        lastTransSize = -1;
        /**
         * If it's a start of data transfer response, let's try to extract
         * the size from the response string. Usually it looks like that:
         *
         * 150 Opening BINARY mode data connection for foo (6701 bytes).
         */
        String response = getLastResponseString();
        if (transPat == null) {
            transPat = Pattern.compile("150 Opening .*\\((\\d+) bytes\\).");
        }
        Matcher m = transPat.matcher(response);
        if (m.find()) {
            String s = m.group(1);
            lastTransSize = Long.parseLong(s);
        }
    }

    /**
     * extract the created file name from the response string:
     * 226 Transfer complete (unique file name:toto.txt.1).
     * Usually happens when a STOU (store unique) command had been issued.
     */
    private void getTransferName() {
        lastFileName = null;
        String response = getLastResponseString();
        int i = response.indexOf("unique file name:");
        int e = response.lastIndexOf(')');
        if (i >= 0) {
            i += 17; // Length of "unique file name:"
            lastFileName = response.substring(i, e);
        }
    }

    /**
     * Pulls the response from the server and returns the code as a
     * number. Returns -1 on failure.
     */
    private int readServerResponse() throws IOException {
        StringBuilder replyBuf = new StringBuilder(32);
        int c;
        int continuingCode = -1;
        int code;
        String response;

        serverResponse.setSize(0);
        while (true) {
            while ((c = in.read()) != -1) {
                if (c == '\r') {
                    if ((c = in.read()) != '\n') {
                        replyBuf.append('\r');
                    }
                }
                replyBuf.append((char) c);
                if (c == '\n') {
                    break;
                }
            }
            response = replyBuf.toString();
            replyBuf.setLength(0);
            if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
                logger.finest("Server [" + serverAddr + "] --> " + response);
            }

            if (response.isEmpty()) {
                code = -1;
            } else {
                try {
                    code = Integer.parseInt(response, 0, 3, 10);
                } catch (NumberFormatException e) {
                    code = -1;
                } catch (IndexOutOfBoundsException e) {
                    /* this line doesn't contain a response code, so
                    we just completely ignore it */
                    continue;
                }
            }
            serverResponse.addElement(response);
            if (continuingCode != -1) {
                /* we've seen a ###- sequence */
                if (code != continuingCode ||
                        (response.length() >= 4 && response.charAt(3) == '-')) {
                    continue;
                } else {
                    /* seen the end of code sequence */
                    continuingCode = -1;
                    break;
                }
            } else if (response.length() >= 4 && response.charAt(3) == '-') {
                continuingCode = code;
                continue;
            } else {
                break;
            }
        }

        return code;
    }

    /** Sends command <i>cmd</i> to the server. */
    private void sendServer(String cmd) {
        out.print(cmd);
        if (logger.isLoggable(PlatformLogger.Level.FINEST)) {
            logger.finest("Server [" + serverAddr + "] <-- " + cmd);
        }
    }

    /** converts the server response into a string. */
    private String getResponseString() {
        return serverResponse.elementAt(0);
    }

    /** Returns all server response strings. */
    private Vector<String> getResponseStrings() {
        return serverResponse;
    }

    /**
     * Read the reply from the FTP server.
     *
     * @return <code>true</code> if the command was successful
     * @throws IOException if an error occurred
     */
    private boolean readReply() throws IOException {
        lastReplyCode = FtpReplyCode.find(readServerResponse());

        if (lastReplyCode.isPositivePreliminary()) {
            replyPending = true;
            return true;
        }
        if (lastReplyCode.isPositiveCompletion() || lastReplyCode.isPositiveIntermediate()) {
            if (lastReplyCode == FtpReplyCode.CLOSING_DATA_CONNECTION) {
                getTransferName();
            }
            return true;
        }
        return false;
    }

    /**
     * Sends a command to the FTP server and returns the error code
     * (which can be a "success") sent by the server.
     *
     * @param cmd
     * @return <code>true</code> if the command was successful
     * @throws IOException
     */
    private boolean issueCommand(String cmd) throws IOException,
            sun.net.ftp.FtpProtocolException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected");
        }
        if (replyPending) {
            try {
                completePending();
            } catch (sun.net.ftp.FtpProtocolException e) {
                // ignore...
            }
        }
        if (cmd.indexOf('\n') != -1) {
            sun.net.ftp.FtpProtocolException ex
                    = new sun.net.ftp.FtpProtocolException("Illegal FTP command");
            ex.initCause(new IllegalArgumentException("Illegal carriage return"));
            throw ex;
        }
        sendServer(cmd + "\r\n");
        return readReply();
    }

    /**
     * Send a command to the FTP server and check for success.
     *
     * @param cmd String containing the command
     *
     * @throws FtpProtocolException if an error occurred
     */
    private void issueCommandCheck(String cmd) throws sun.net.ftp.FtpProtocolException, IOException {
        if (!issueCommand(cmd)) {
            throw new sun.net.ftp.FtpProtocolException(cmd + ":" + getResponseString(), getLastReplyCode());
        }
    }
    private static Pattern epsvPat = null;
    private static Pattern pasvPat = null;

    /**
     * Opens a "PASSIVE" connection with the server and returns the connected
     * <code>Socket</code>.
     *
     * @return the connected <code>Socket</code>
     * @throws IOException if the connection was unsuccessful.
     */
    private Socket openPassiveDataConnection(String cmd) throws sun.net.ftp.FtpProtocolException, IOException {
        String serverAnswer;
        int port;
        InetSocketAddress dest = null;

        /**
         * Here is the idea:
         *
         * - First we want to try the new (and IPv6 compatible) EPSV command
         *   But since we want to be nice with NAT software, we'll issue the
         *   EPSV ALL command first.
         *   EPSV is documented in RFC2428
         * - If EPSV fails, then we fall back to the older, yet ok, PASV
         * - If PASV fails as well, then we throw an exception and the calling
         *   method will have to try the EPRT or PORT command
         */
        if (issueCommand("EPSV ALL")) {
            // We can safely use EPSV commands
            issueCommandCheck("EPSV");
            serverAnswer = getResponseString();

            // The response string from a EPSV command will contain the port number
            // the format will be :
            //  229 Entering Extended PASSIVE Mode (|||58210|)
            //
            // So we'll use the regular expresions package to parse the output.

            if (epsvPat == null) {
                epsvPat = Pattern.compile("^229 .* \\(\\|\\|\\|(\\d+)\\|\\)");
            }
            Matcher m = epsvPat.matcher(serverAnswer);
            if (!m.find()) {
                throw new sun.net.ftp.FtpProtocolException("EPSV failed : " + serverAnswer);
            }
            // Yay! Let's extract the port number
            String s = m.group(1);
            port = Integer.parseInt(s);
            InetAddress add = server.getInetAddress();
            if (add != null) {
                dest = new InetSocketAddress(add, port);
            } else {
                // This means we used an Unresolved address to connect in
                // the first place. Most likely because the proxy is doing
                // the name resolution for us, so let's keep using unresolved
                // address.
                dest = InetSocketAddress.createUnresolved(serverAddr.getHostName(), port);
            }
        } else {
            // EPSV ALL failed, so Let's try the regular PASV cmd
            issueCommandCheck("PASV");
            serverAnswer = getResponseString();

            // Let's parse the response String to get the IP & port to connect
            // to. The String should be in the following format :
            //
            // 227 Entering PASSIVE Mode (A1,A2,A3,A4,p1,p2)
            //
            // Note that the two parenthesis are optional
            //
            // The IP address is A1.A2.A3.A4 and the port is p1 * 256 + p2
            //
            // The regular expression is a bit more complex this time, because
            // the parenthesis are optionals and we have to use 3 groups.
            if (pasvPat == null) {
                pasvPat = Pattern.compile("227 .* \\(?(\\d{1,3},\\d{1,3},\\d{1,3},\\d{1,3}),(\\d{1,3}),(\\d{1,3})\\)?");
            }
            Matcher m = pasvPat.matcher(serverAnswer);
            if (!m.find()) {
                throw new sun.net.ftp.FtpProtocolException("PASV failed : " + serverAnswer);
            }
            // Get port number out of group 2 & 3
            port = Integer.parseInt(m.group(3)) + (Integer.parseInt(m.group(2)) << 8);
            // IP address is simple
            String s = m.group(1).replace(',', '.');
            if (!IPAddressUtil.isIPv4LiteralAddress(s))
                throw new FtpProtocolException("PASV failed : "  + serverAnswer);
            if (acceptPasvAddressVal) {
                dest = new InetSocketAddress(s, port);
            } else {
                dest = validatePasvAddress(port, s, server.getInetAddress());
            }
        }

        // Got everything, let's open the socket!
        Socket s;
        if (proxy != null) {
            if (proxy.type() == Proxy.Type.SOCKS) {
                PrivilegedAction<Socket> pa = () -> new Socket(proxy);
                @SuppressWarnings("removal")
                var tmp = AccessController.doPrivileged(pa);
                s = tmp;
            } else {
                s = new Socket(Proxy.NO_PROXY);
            }
        } else {
            s = new Socket();
        }

        PrivilegedAction<InetAddress> pa = () -> server.getLocalAddress();
        @SuppressWarnings("removal")
        InetAddress serverAddress = AccessController.doPrivileged(pa);

        // Bind the socket to the same address as the control channel. This
        // is needed in case of multi-homed systems.
        s.bind(new InetSocketAddress(serverAddress, 0));
        if (connectTimeout >= 0) {
            s.connect(dest, connectTimeout);
        } else {
            if (defaultConnectTimeout > 0) {
                s.connect(dest, defaultConnectTimeout);
            } else {
                s.connect(dest);
            }
        }
        if (readTimeout >= 0) {
            s.setSoTimeout(readTimeout);
        } else if (defaultSoTimeout > 0) {
            s.setSoTimeout(defaultSoTimeout);
        }
        if (useCrypto) {
            try {
                s = sslFact.createSocket(s, dest.getHostName(), dest.getPort(), true);
            } catch (Exception e) {
                throw new sun.net.ftp.FtpProtocolException("Can't open secure data channel: " + e);
            }
        }
        if (!issueCommand(cmd)) {
            s.close();
            if (getLastReplyCode() == FtpReplyCode.FILE_UNAVAILABLE) {
                // Ensure backward compatibility
                throw new FileNotFoundException(cmd);
            }
            throw new sun.net.ftp.FtpProtocolException(cmd + ":" + getResponseString(), getLastReplyCode());
        }
        return s;
    }

    static final String ERROR_MSG = "Address should be the same as originating server";

    /**
     * Returns an InetSocketAddress, based on value of acceptPasvAddressVal
     * and other conditions such as the server address returned by pasv
     * is not a hostname, is a socks proxy, or the loopback. An exception
     * is thrown if none of the valid conditions are met.
     */
    private InetSocketAddress validatePasvAddress(int port, String s, InetAddress address)
        throws FtpProtocolException
    {
        if (address == null) {
            return InetSocketAddress.createUnresolved(serverAddr.getHostName(), port);
        }
        String serverAddress = address.getHostAddress();
        if (serverAddress.equals(s)) {
            return new InetSocketAddress(s, port);
        } else if (address.isLoopbackAddress() && s.startsWith("127.")) { // can be 127.0
            return new InetSocketAddress(s, port);
        } else if (address.isLoopbackAddress()) {
            if (privilegedLocalHost().getHostAddress().equals(s)) {
                return new InetSocketAddress(s, port);
            } else {
                throw new FtpProtocolException(ERROR_MSG);
            }
        } else if (s.startsWith("127.")) {
            if (privilegedLocalHost().equals(address)) {
                return new InetSocketAddress(s, port);
            } else {
                throw new FtpProtocolException(ERROR_MSG);
            }
        }
        String hostName = address.getHostName();
        if (!(IPAddressUtil.isIPv4LiteralAddress(hostName) || IPAddressUtil.isIPv6LiteralAddress(hostName))) {
            InetAddress[] names = privilegedGetAllByName(hostName);
            String resAddress = Arrays
                .stream(names)
                .map(InetAddress::getHostAddress)
                .filter(s::equalsIgnoreCase)
                .findFirst()
                .orElse(null);
            if (resAddress != null) {
                return new InetSocketAddress(s, port);
            }
        }
        throw new FtpProtocolException(ERROR_MSG);
    }

    private static InetAddress privilegedLocalHost() throws FtpProtocolException {
        PrivilegedExceptionAction<InetAddress> action = InetAddress::getLocalHost;
        try {
            @SuppressWarnings("removal")
            var tmp = AccessController.doPrivileged(action);
            return tmp;
        } catch (Exception e) {
            var ftpEx = new FtpProtocolException(ERROR_MSG);
            ftpEx.initCause(e);
            throw ftpEx;
        }
    }

    private static InetAddress[] privilegedGetAllByName(String hostName) throws FtpProtocolException {
        PrivilegedExceptionAction<InetAddress[]> pAction = () -> InetAddress.getAllByName(hostName);
        try {
            @SuppressWarnings("removal")
            var tmp =AccessController.doPrivileged(pAction);
            return tmp;
        } catch (Exception e) {
            var ftpEx = new FtpProtocolException(ERROR_MSG);
            ftpEx.initCause(e);
            throw ftpEx;
        }
    }

    /**
     * Opens a data connection with the server according to the set mode
     * (ACTIVE or PASSIVE) then send the command passed as an argument.
     *
     * @param cmd the <code>String</code> containing the command to execute
     * @return the connected <code>Socket</code>
     * @throws IOException if the connection or command failed
     */
    private Socket openDataConnection(String cmd) throws sun.net.ftp.FtpProtocolException, IOException {
        Socket clientSocket;
        if (passiveMode) {
            try {
                return openPassiveDataConnection(cmd);
            } catch (sun.net.ftp.FtpProtocolException e) {
                // If Passive mode failed, fall back on PORT
                // Otherwise throw exception
                String errmsg = e.getMessage();
                if (!errmsg.startsWith("PASV") && !errmsg.startsWith("EPSV")) {
                    throw e;
                }
            }
        }
        ServerSocket portSocket;
        InetAddress myAddress;
        String portCmd;

        if (proxy != null && proxy.type() == Proxy.Type.SOCKS) {
            // We're behind a firewall and the passive mode fail,
            // since we can't accept a connection through SOCKS (yet)
            // throw an exception
            throw new sun.net.ftp.FtpProtocolException("Passive mode failed");
        }
        // Bind the ServerSocket to the same address as the control channel
        // This is needed for multi-homed systems
        portSocket = new ServerSocket(0, 1, server.getLocalAddress());
        try {
            myAddress = portSocket.getInetAddress();
            if (myAddress.isAnyLocalAddress()) {
                myAddress = server.getLocalAddress();
            }
            // Let's try the new, IPv6 compatible EPRT command
            // See RFC2428 for specifics
            // Some FTP servers (like the one on Solaris) are bugged, they
            // will accept the EPRT command but then, the subsequent command
            // (e.g. RETR) will fail, so we have to check BOTH results (the
            // EPRT cmd then the actual command) to decide whether we should
            // fall back on the older PORT command.
            portCmd = "EPRT |" + ((myAddress instanceof Inet6Address) ? "2" : "1") + "|" +
                    myAddress.getHostAddress() + "|" + portSocket.getLocalPort() + "|";
            if (!issueCommand(portCmd) || !issueCommand(cmd)) {
                // The EPRT command failed, let's fall back to good old PORT
                portCmd = "PORT ";
                byte[] addr = myAddress.getAddress();

                /* append host addr */
                for (int i = 0; i < addr.length; i++) {
                    portCmd = portCmd + (addr[i] & 0xFF) + ",";
                }

                /* append port number */
                portCmd = portCmd + ((portSocket.getLocalPort() >>> 8) & 0xff) + "," + (portSocket.getLocalPort() & 0xff);
                issueCommandCheck(portCmd);
                issueCommandCheck(cmd);
            }
            // Either the EPRT or the PORT command was successful
            // Let's create the client socket
            if (connectTimeout >= 0) {
                portSocket.setSoTimeout(connectTimeout);
            } else {
                if (defaultConnectTimeout > 0) {
                    portSocket.setSoTimeout(defaultConnectTimeout);
                }
            }
            clientSocket = portSocket.accept();
            if (readTimeout >= 0) {
                clientSocket.setSoTimeout(readTimeout);
            } else {
                if (defaultSoTimeout > 0) {
                    clientSocket.setSoTimeout(defaultSoTimeout);
                }
            }
        } finally {
            portSocket.close();
        }
        if (useCrypto) {
            try {
                clientSocket = sslFact.createSocket(clientSocket, serverAddr.getHostName(), serverAddr.getPort(), true);
            } catch (Exception ex) {
                throw new IOException(ex.getLocalizedMessage());
            }
        }
        return clientSocket;
    }

    private InputStream createInputStream(InputStream in) {
        if (type == TransferType.ASCII) {
            return new sun.net.TelnetInputStream(in, false);
        }
        return in;
    }

    private OutputStream createOutputStream(OutputStream out) {
        if (type == TransferType.ASCII) {
            return new sun.net.TelnetOutputStream(out, false);
        }
        return out;
    }

    /**
     * Creates an instance of FtpClient. The client is not connected to any
     * server yet.
     *
     */
    protected FtpClient() {
    }

    /**
     * Creates an instance of FtpClient. The client is not connected to any
     * server yet.
     *
     */
    public static sun.net.ftp.FtpClient create() {
        return new FtpClient();
    }

    /**
     * Set the transfer mode to <I>passive</I>. In that mode, data connections
     * are established by having the client connect to the server.
     * This is the recommended default mode as it will work best through
     * firewalls and NATs.
     *
     * @return This FtpClient
     * @see #setActiveMode()
     */
    public sun.net.ftp.FtpClient enablePassiveMode(boolean passive) {

        // Only passive mode used in JDK. See Bug 8010784.
        // passiveMode = passive;
        return this;
    }

    /**
     * Gets the current transfer mode.
     *
     * @return the current <code>FtpTransferMode</code>
     */
    public boolean isPassiveModeEnabled() {
        return passiveMode;
    }

    /**
     * Sets the timeout value to use when connecting to the server,
     *
     * @param timeout the timeout value, in milliseconds, to use for the connect
     *        operation. A value of zero or less, means use the default timeout.
     *
     * @return This FtpClient
     */
    public sun.net.ftp.FtpClient setConnectTimeout(int timeout) {
        connectTimeout = timeout;
        return this;
    }

    /**
     * Returns the current connection timeout value.
     *
     * @return the value, in milliseconds, of the current connect timeout.
     * @see #setConnectTimeout(int)
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Sets the timeout value to use when reading from the server,
     *
     * @param timeout the timeout value, in milliseconds, to use for the read
     *        operation. A value of zero or less, means use the default timeout.
     * @return This FtpClient
     */
    public sun.net.ftp.FtpClient setReadTimeout(int timeout) {
        readTimeout = timeout;
        return this;
    }

    /**
     * Returns the current read timeout value.
     *
     * @return the value, in milliseconds, of the current read timeout.
     * @see #setReadTimeout(int)
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    public sun.net.ftp.FtpClient setProxy(Proxy p) {
        proxy = p;
        return this;
    }

    /**
     * Get the proxy of this FtpClient
     *
     * @return the <code>Proxy</code>, this client is using, or <code>null</code>
     *         if none is used.
     * @see #setProxy(Proxy)
     */
    public Proxy getProxy() {
        return proxy;
    }

    /**
     * Connects to the specified destination.
     *
     * @param dest the <code>InetSocketAddress</code> to connect to.
     * @throws IOException if the connection fails.
     */
    private void tryConnect(InetSocketAddress dest, int timeout) throws IOException {
        if (isConnected()) {
            disconnect();
        }
        server = doConnect(dest, timeout);
        try {
            out = new PrintStream(new BufferedOutputStream(server.getOutputStream()),
                    true, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding + "encoding not found", e);
        }
        in = new BufferedInputStream(server.getInputStream());
    }

    private Socket doConnect(InetSocketAddress dest, int timeout) throws IOException {
        Socket s;
        if (proxy != null) {
            if (proxy.type() == Proxy.Type.SOCKS) {
                PrivilegedAction<Socket> pa = () -> new Socket(proxy);
                @SuppressWarnings("removal")
                var tmp = AccessController.doPrivileged(pa);
                s = tmp;
            } else {
                s = new Socket(Proxy.NO_PROXY);
            }
        } else {
            s = new Socket();
        }
        // Instance specific timeouts do have priority, that means
        // connectTimeout & readTimeout (-1 means not set)
        // Then global default timeouts
        // Then no timeout.
        if (timeout >= 0) {
            s.connect(dest, timeout);
        } else {
            if (connectTimeout >= 0) {
                s.connect(dest, connectTimeout);
            } else {
                if (defaultConnectTimeout > 0) {
                    s.connect(dest, defaultConnectTimeout);
                } else {
                    s.connect(dest);
                }
            }
        }
        if (readTimeout >= 0) {
            s.setSoTimeout(readTimeout);
        } else if (defaultSoTimeout > 0) {
            s.setSoTimeout(defaultSoTimeout);
        }
        return s;
    }

    private void disconnect() throws IOException {
        if (isConnected()) {
            server.close();
        }
        server = null;
        in = null;
        out = null;
        lastTransSize = -1;
        lastFileName = null;
        restartOffset = 0;
        welcomeMsg = null;
        lastReplyCode = null;
        serverResponse.setSize(0);
    }

    /**
     * Tests whether this client is connected or not to a server.
     *
     * @return <code>true</code> if the client is connected.
     */
    public boolean isConnected() {
        return server != null;
    }

    public SocketAddress getServerAddress() {
        return server == null ? null : server.getRemoteSocketAddress();
    }

    public sun.net.ftp.FtpClient connect(SocketAddress dest) throws sun.net.ftp.FtpProtocolException, IOException {
        return connect(dest, -1);
    }

    /**
     * Connects the FtpClient to the specified destination.
     *
     * @param dest the address of the destination server
     * @throws IOException if connection failed.
     */
    public sun.net.ftp.FtpClient connect(SocketAddress dest, int timeout) throws sun.net.ftp.FtpProtocolException, IOException {
        if (!(dest instanceof InetSocketAddress)) {
            throw new IllegalArgumentException("Wrong address type");
        }
        serverAddr = (InetSocketAddress) dest;
        tryConnect(serverAddr, timeout);
        if (!readReply()) {
            throw new sun.net.ftp.FtpProtocolException("Welcome message: " +
                    getResponseString(), lastReplyCode);
        }
        welcomeMsg = getResponseString().substring(4);
        return this;
    }

    private void tryLogin(String user, char[] password) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("USER " + user);

        /*
         * Checks for "331 User name okay, need password." answer
         */
        if (lastReplyCode == FtpReplyCode.NEED_PASSWORD) {
            if ((password != null) && (password.length > 0)) {
                issueCommandCheck("PASS " + String.valueOf(password));
            }
        }
    }

    /**
     * Attempts to log on the server with the specified user name and password.
     *
     * @param user The user name
     * @param password The password for that user
     * @return <code>true</code> if the login was successful.
     * @throws IOException if an error occurred during the transmission
     */
    public sun.net.ftp.FtpClient login(String user, char[] password) throws sun.net.ftp.FtpProtocolException, IOException {
        if (!isConnected()) {
            throw new sun.net.ftp.FtpProtocolException("Not connected yet", FtpReplyCode.BAD_SEQUENCE);
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("User name can't be null or empty");
        }
        tryLogin(user, password);

        // keep the welcome message around so we can
        // put it in the resulting HTML page.
        String l;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < serverResponse.size(); i++) {
            l = serverResponse.elementAt(i);
            if (l != null) {
                if (l.length() >= 4 && l.startsWith("230")) {
                    // get rid of the "230-" prefix
                    l = l.substring(4);
                }
                sb.append(l);
            }
        }
        welcomeMsg = sb.toString();
        loggedIn = true;
        return this;
    }

    /**
     * Attempts to log on the server with the specified user name, password and
     * account name.
     *
     * @param user The user name
     * @param password The password for that user.
     * @param account The account name for that user.
     * @return <code>true</code> if the login was successful.
     * @throws IOException if an error occurs during the transmission.
     */
    public sun.net.ftp.FtpClient login(String user, char[] password, String account) throws sun.net.ftp.FtpProtocolException, IOException {

        if (!isConnected()) {
            throw new sun.net.ftp.FtpProtocolException("Not connected yet", FtpReplyCode.BAD_SEQUENCE);
        }
        if (user == null || user.isEmpty()) {
            throw new IllegalArgumentException("User name can't be null or empty");
        }
        tryLogin(user, password);

        /*
         * Checks for "332 Need account for login." answer
         */
        if (lastReplyCode == FtpReplyCode.NEED_ACCOUNT) {
            issueCommandCheck("ACCT " + account);
        }

        // keep the welcome message around so we can
        // put it in the resulting HTML page.
        StringBuilder sb = new StringBuilder();
        if (serverResponse != null) {
            for (String l : serverResponse) {
                if (l != null) {
                    if (l.length() >= 4 && l.startsWith("230")) {
                        // get rid of the "230-" prefix
                        l = l.substring(4);
                    }
                    sb.append(l);
                }
            }
        }
        welcomeMsg = sb.toString();
        loggedIn = true;
        return this;
    }

    /**
     * Logs out the current user. This is in effect terminates the current
     * session and the connection to the server will be closed.
     *
     */
    public void close() throws IOException {
        if (isConnected()) {
            try {
                issueCommand("QUIT");
            } catch (FtpProtocolException e) {
            }
            loggedIn = false;
        }
        disconnect();
    }

    /**
     * Checks whether the client is logged in to the server or not.
     *
     * @return <code>true</code> if the client has already completed a login.
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Changes to a specific directory on a remote FTP server
     *
     * @param remoteDirectory path of the directory to CD to.
     * @return <code>true</code> if the operation was successful.
     * @exception <code>FtpProtocolException</code>
     */
    public sun.net.ftp.FtpClient changeDirectory(String remoteDirectory) throws sun.net.ftp.FtpProtocolException, IOException {
        if (remoteDirectory == null || remoteDirectory.isEmpty()) {
            throw new IllegalArgumentException("directory can't be null or empty");
        }

        issueCommandCheck("CWD " + remoteDirectory);
        return this;
    }

    /**
     * Changes to the parent directory, sending the CDUP command to the server.
     *
     * @return <code>true</code> if the command was successful.
     * @throws IOException
     */
    public sun.net.ftp.FtpClient changeToParentDirectory() throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("CDUP");
        return this;
    }

    /**
     * Returns the server current working directory, or <code>null</code> if
     * the PWD command failed.
     *
     * @return a <code>String</code> containing the current working directory,
     *         or <code>null</code>
     * @throws IOException
     */
    public String getWorkingDirectory() throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("PWD");
        /*
         * answer will be of the following format :
         *
         * 257 "/" is current directory.
         */
        String answ = getResponseString();
        if (!answ.startsWith("257")) {
            return null;
        }
        return answ.substring(5, answ.lastIndexOf('"'));
    }

    /**
     * Sets the restart offset to the specified value.  That value will be
     * sent through a <code>REST</code> command to server before a file
     * transfer and has the effect of resuming a file transfer from the
     * specified point. After a transfer the restart offset is set back to
     * zero.
     *
     * @param offset the offset in the remote file at which to start the next
     *        transfer. This must be a value greater than or equal to zero.
     * @throws IllegalArgumentException if the offset is negative.
     */
    public sun.net.ftp.FtpClient setRestartOffset(long offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset can't be negative");
        }
        restartOffset = offset;
        return this;
    }

    /**
     * Retrieves a file from the ftp server and writes it to the specified
     * <code>OutputStream</code>.
     * If the restart offset was set, then a <code>REST</code> command will be
     * sent before the RETR in order to restart the tranfer from the specified
     * offset.
     * The <code>OutputStream</code> is not closed by this method at the end
     * of the transfer.
     *
     * @param name a {@code String} containing the name of the file to
     *        retreive from the server.
     * @param local the <code>OutputStream</code> the file should be written to.
     * @throws IOException if the transfer fails.
     */
    public sun.net.ftp.FtpClient getFile(String name, OutputStream local) throws sun.net.ftp.FtpProtocolException, IOException {
        if (restartOffset > 0) {
            Socket s;
            try {
                s = openDataConnection("REST " + restartOffset);
            } finally {
                restartOffset = 0;
            }
            issueCommandCheck("RETR " + name);
            getTransferSize();
            try (InputStream remote = createInputStream(s.getInputStream())) {
                remote.transferTo(local);
            }
        } else {
            Socket s = openDataConnection("RETR " + name);
            getTransferSize();
            try (InputStream remote = createInputStream(s.getInputStream())) {
                remote.transferTo(local);
            }
        }
        return completePending();
    }

    /**
     * Retrieves a file from the ftp server, using the RETR command, and
     * returns the InputStream from* the established data connection.
     * {@link #completePending()} <b>has</b> to be called once the application
     * is done reading from the returned stream.
     *
     * @param name the name of the remote file
     * @return the {@link java.io.InputStream} from the data connection, or
     *         <code>null</code> if the command was unsuccessful.
     * @throws IOException if an error occurred during the transmission.
     */
    public InputStream getFileStream(String name) throws sun.net.ftp.FtpProtocolException, IOException {
        Socket s;
        if (restartOffset > 0) {
            try {
                s = openDataConnection("REST " + restartOffset);
            } finally {
                restartOffset = 0;
            }
            if (s == null) {
                return null;
            }
            issueCommandCheck("RETR " + name);
            getTransferSize();
            return createInputStream(s.getInputStream());
        }

        s = openDataConnection("RETR " + name);
        if (s == null) {
            return null;
        }
        getTransferSize();
        return createInputStream(s.getInputStream());
    }

    /**
     * Transfers a file from the client to the server (aka a <I>put</I>)
     * by sending the STOR or STOU command, depending on the
     * <code>unique</code> argument, and returns the <code>OutputStream</code>
     * from the established data connection.
     * {@link #completePending()} <b>has</b> to be called once the application
     * is finished writing to the stream.
     *
     * A new file is created at the server site if the file specified does
     * not already exist.
     *
     * If <code>unique</code> is set to <code>true</code>, the resultant file
     * is to be created under a name unique to that directory, meaning
     * it will not overwrite an existing file, instead the server will
     * generate a new, unique, file name.
     * The name of the remote file can be retrieved, after completion of the
     * transfer, by calling {@link #getLastFileName()}.
     *
     * @param name the name of the remote file to write.
     * @param unique <code>true</code> if the remote files should be unique,
     *        in which case the STOU command will be used.
     * @return the {@link java.io.OutputStream} from the data connection or
     *         <code>null</code> if the command was unsuccessful.
     * @throws IOException if an error occurred during the transmission.
     */
    public OutputStream putFileStream(String name, boolean unique)
        throws sun.net.ftp.FtpProtocolException, IOException
    {
        String cmd = unique ? "STOU " : "STOR ";
        Socket s = openDataConnection(cmd + name);
        if (s == null) {
            return null;
        }
        boolean bm = (type == TransferType.BINARY);
        return new sun.net.TelnetOutputStream(s.getOutputStream(), bm);
    }

    /**
     * Transfers a file from the client to the server (aka a <I>put</I>)
     * by sending the STOR command. The content of the <code>InputStream</code>
     * passed in argument is written into the remote file, overwriting any
     * existing data.
     *
     * A new file is created at the server site if the file specified does
     * not already exist.
     *
     * @param name the name of the remote file to write.
     * @param local the <code>InputStream</code> that points to the data to
     *        transfer.
     * @param unique <code>true</code> if the remote file should be unique
     *        (i.e. not already existing), <code>false</code> otherwise.
     * @return <code>true</code> if the transfer was successful.
     * @throws IOException if an error occurred during the transmission.
     * @see #getLastFileName()
     */
    public sun.net.ftp.FtpClient putFile(String name, InputStream local, boolean unique) throws sun.net.ftp.FtpProtocolException, IOException {
        String cmd = unique ? "STOU " : "STOR ";
        if (type == TransferType.BINARY) {
            Socket s = openDataConnection(cmd + name);
            try (OutputStream remote = createOutputStream(s.getOutputStream())) {
                local.transferTo(remote);
            }
        }
        return completePending();
    }

    /**
     * Sends the APPE command to the server in order to transfer a data stream
     * passed in argument and append it to the content of the specified remote
     * file.
     *
     * @param name A <code>String</code> containing the name of the remote file
     *        to append to.
     * @param local The <code>InputStream</code> providing access to the data
     *        to be appended.
     * @return <code>true</code> if the transfer was successful.
     * @throws IOException if an error occurred during the transmission.
     */
    public sun.net.ftp.FtpClient appendFile(String name, InputStream local) throws sun.net.ftp.FtpProtocolException, IOException {
        Socket s = openDataConnection("APPE " + name);
        try (OutputStream remote = createOutputStream(s.getOutputStream())) {
            local.transferTo(remote);
        }
        return completePending();
    }

    /**
     * Renames a file on the server.
     *
     * @param from the name of the file being renamed
     * @param to the new name for the file
     * @throws IOException if the command fails
     */
    public sun.net.ftp.FtpClient rename(String from, String to) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("RNFR " + from);
        issueCommandCheck("RNTO " + to);
        return this;
    }

    /**
     * Deletes a file on the server.
     *
     * @param name a <code>String</code> containing the name of the file
     *        to delete.
     * @return <code>true</code> if the command was successful
     * @throws IOException if an error occurred during the exchange
     */
    public sun.net.ftp.FtpClient deleteFile(String name) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("DELE " + name);
        return this;
    }

    /**
     * Creates a new directory on the server.
     *
     * @param name a <code>String</code> containing the name of the directory
     *        to create.
     * @return <code>true</code> if the operation was successful.
     * @throws IOException if an error occurred during the exchange
     */
    public sun.net.ftp.FtpClient makeDirectory(String name) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("MKD " + name);
        return this;
    }

    /**
     * Removes a directory on the server.
     *
     * @param name a <code>String</code> containing the name of the directory
     *        to remove.
     *
     * @return <code>true</code> if the operation was successful.
     * @throws IOException if an error occurred during the exchange.
     */
    public sun.net.ftp.FtpClient removeDirectory(String name) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("RMD " + name);
        return this;
    }

    /**
     * Sends a No-operation command. It's useful for testing the connection
     * status or as a <I>keep alive</I> mechanism.
     *
     * @throws FtpProtocolException if the command fails
     */
    public sun.net.ftp.FtpClient noop() throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("NOOP");
        return this;
    }

    /**
     * Sends the STAT command to the server.
     * This can be used while a data connection is open to get a status
     * on the current transfer, in that case the parameter should be
     * <code>null</code>.
     * If used between file transfers, it may have a pathname as argument
     * in which case it will work as the LIST command except no data
     * connection will be created.
     *
     * @param name an optional <code>String</code> containing the pathname
     *        the STAT command should apply to.
     * @return the response from the server or <code>null</code> if the
     *         command failed.
     * @throws IOException if an error occurred during the transmission.
     */
    public String getStatus(String name) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck((name == null ? "STAT" : "STAT " + name));
        /*
         * A typical response will be:
         *  213-status of t32.gif:
         * -rw-r--r--   1 jcc      staff     247445 Feb 17  1998 t32.gif
         * 213 End of Status
         *
         * or
         *
         * 211-jsn FTP server status:
         *     Version wu-2.6.2+Sun
         *     Connected to localhost (::1)
         *     Logged in as jccollet
         *     TYPE: ASCII, FORM: Nonprint; STRUcture: File; transfer MODE: Stream
         *      No data connection
         *     0 data bytes received in 0 files
         *     0 data bytes transmitted in 0 files
         *     0 data bytes total in 0 files
         *     53 traffic bytes received in 0 transfers
         *     485 traffic bytes transmitted in 0 transfers
         *     587 traffic bytes total in 0 transfers
         * 211 End of status
         *
         * So we need to remove the 1st and last line
         */
        Vector<String> resp = getResponseStrings();
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < resp.size() - 1; i++) {
            sb.append(resp.get(i));
        }
        return sb.toString();
    }

    /**
     * Sends the FEAT command to the server and returns the list of supported
     * features in the form of strings.
     *
     * The features are the supported commands, like AUTH TLS, PROT or PASV.
     * See the RFCs for a complete list.
     *
     * Note that not all FTP servers support that command, in which case
     * the method will return <code>null</code>
     *
     * @return a <code>List</code> of <code>Strings</code> describing the
     *         supported additional features, or <code>null</code>
     *         if the command is not supported.
     * @throws IOException if an error occurs during the transmission.
     */
    public List<String> getFeatures() throws sun.net.ftp.FtpProtocolException, IOException {
        /*
         * The FEAT command, when implemented will return something like:
         *
         * 211-Features:
         *   AUTH TLS
         *   PBSZ
         *   PROT
         *   EPSV
         *   EPRT
         *   PASV
         *   REST STREAM
         *  211 END
         */
        ArrayList<String> features = new ArrayList<String>();
        issueCommandCheck("FEAT");
        Vector<String> resp = getResponseStrings();
        // Note that we start at index 1 to skip the 1st line (211-...)
        // and we stop before the last line.
        for (int i = 1; i < resp.size() - 1; i++) {
            String s = resp.get(i);
            // Get rid of leading space and trailing newline
            features.add(s.substring(1, s.length() - 1));
        }
        return features;
    }

    /**
     * sends the ABOR command to the server.
     * It tells the server to stop the previous command or transfer.
     *
     * @return <code>true</code> if the command was successful.
     * @throws IOException if an error occurred during the transmission.
     */
    public sun.net.ftp.FtpClient abort() throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("ABOR");
        // TODO: Must check the ReplyCode:
        /*
         * From the RFC:
         * There are two cases for the server upon receipt of this
         * command: (1) the FTP service command was already completed,
         * or (2) the FTP service command is still in progress.
         * In the first case, the server closes the data connection
         * (if it is open) and responds with a 226 reply, indicating
         * that the abort command was successfully processed.
         * In the second case, the server aborts the FTP service in
         * progress and closes the data connection, returning a 426
         * reply to indicate that the service request terminated
         * abnormally.  The server then sends a 226 reply,
         * indicating that the abort command was successfully
         * processed.
         */


        return this;
    }

    /**
     * Some methods do not wait until completion before returning, so this
     * method can be called to wait until completion. This is typically the case
     * with commands that trigger a transfer like {@link #getFileStream(String)}.
     * So this method should be called before accessing information related to
     * such a command.
     * <p>This method will actually block reading on the command channel for a
     * notification from the server that the command is finished. Such a
     * notification often carries extra information concerning the completion
     * of the pending action (e.g. number of bytes transfered).</p>
     * <p>Note that this will return true immediately if no command or action
     * is pending</p>
     * <p>It should be also noted that most methods issuing commands to the ftp
     * server will call this method if a previous command is pending.
     * <p>Example of use:
     * <pre>
     * InputStream in = cl.getFileStream("file");
     * ...
     * cl.completePending();
     * long size = cl.getLastTransferSize();
     * </pre>
     * On the other hand, it's not necessary in a case like:
     * <pre>
     * InputStream in = cl.getFileStream("file");
     * // read content
     * ...
     * cl.logout();
     * </pre>
     * <p>Since {@link #logout()} will call completePending() if necessary.</p>
     * @return <code>true</code> if the completion was successful or if no
     *         action was pending.
     * @throws IOException
     */
    public sun.net.ftp.FtpClient completePending() throws sun.net.ftp.FtpProtocolException, IOException {
        while (replyPending) {
            replyPending = false;
            if (!readReply()) {
                throw new sun.net.ftp.FtpProtocolException(getLastResponseString(), lastReplyCode);
            }
        }
        return this;
    }

    /**
     * Reinitializes the USER parameters on the FTP server
     *
     * @throws FtpProtocolException if the command fails
     */
    public sun.net.ftp.FtpClient reInit() throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("REIN");
        loggedIn = false;
        if (useCrypto) {
            if (server instanceof SSLSocket) {
                javax.net.ssl.SSLSession session = ((SSLSocket) server).getSession();
                session.invalidate();
                // Restore previous socket and streams
                server = oldSocket;
                oldSocket = null;
                try {
                    out = new PrintStream(new BufferedOutputStream(server.getOutputStream()),
                            true, encoding);
                } catch (UnsupportedEncodingException e) {
                    throw new InternalError(encoding + "encoding not found", e);
                }
                in = new BufferedInputStream(server.getInputStream());
            }
        }
        useCrypto = false;
        return this;
    }

    /**
     * Changes the transfer type (binary, ascii, ebcdic) and issue the
     * proper command (e.g. TYPE A) to the server.
     *
     * @param type the <code>FtpTransferType</code> to use.
     * @return This FtpClient
     * @throws IOException if an error occurs during transmission.
     */
    public sun.net.ftp.FtpClient setType(TransferType type) throws sun.net.ftp.FtpProtocolException, IOException {
        String cmd = "NOOP";

        this.type = type;
        if (type == TransferType.ASCII) {
            cmd = "TYPE A";
        }
        if (type == TransferType.BINARY) {
            cmd = "TYPE I";
        }
        if (type == TransferType.EBCDIC) {
            cmd = "TYPE E";
        }
        issueCommandCheck(cmd);
        return this;
    }

    /**
     * Issues a LIST command to the server to get the current directory
     * listing, and returns the InputStream from the data connection.
     * {@link #completePending()} <b>has</b> to be called once the application
     * is finished writing to the stream.
     *
     * @param path the pathname of the directory to list, or <code>null</code>
     *        for the current working directory.
     * @return the <code>InputStream</code> from the resulting data connection
     * @throws IOException if an error occurs during the transmission.
     * @see #changeDirectory(String)
     * @see #listFiles(String)
     */
    public InputStream list(String path) throws sun.net.ftp.FtpProtocolException, IOException {
        Socket s;
        s = openDataConnection(path == null ? "LIST" : "LIST " + path);
        if (s != null) {
            return createInputStream(s.getInputStream());
        }
        return null;
    }

    /**
     * Issues a NLST path command to server to get the specified directory
     * content. It differs from {@link #list(String)} method by the fact that
     * it will only list the file names which would make the parsing of the
     * somewhat easier.
     *
     * {@link #completePending()} <b>has</b> to be called once the application
     * is finished writing to the stream.
     *
     * @param path a <code>String</code> containing the pathname of the
     *        directory to list or <code>null</code> for the current working
     *        directory.
     * @return the <code>InputStream</code> from the resulting data connection
     * @throws IOException if an error occurs during the transmission.
     */
    public InputStream nameList(String path) throws sun.net.ftp.FtpProtocolException, IOException {
        Socket s;
        s = openDataConnection(path == null ? "NLST" : "NLST " + path);
        if (s != null) {
            return createInputStream(s.getInputStream());
        }
        return null;
    }

    /**
     * Issues the SIZE [path] command to the server to get the size of a
     * specific file on the server.
     * Note that this command may not be supported by the server. In which
     * case -1 will be returned.
     *
     * @param path a <code>String</code> containing the pathname of the
     *        file.
     * @return a <code>long</code> containing the size of the file or -1 if
     *         the server returned an error, which can be checked with
     *         {@link #getLastReplyCode()}.
     * @throws IOException if an error occurs during the transmission.
     */
    public long getSize(String path) throws sun.net.ftp.FtpProtocolException, IOException {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path can't be null or empty");
        }
        issueCommandCheck("SIZE " + path);
        if (lastReplyCode == FtpReplyCode.FILE_STATUS) {
            String s = getResponseString();
            s = s.substring(4, s.length() - 1);
            return Long.parseLong(s);
        }
        return -1;
    }

    private static final DateTimeFormatter RFC3659_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss[.SSS]")
                                                                                      .withZone(ZoneOffset.UTC);

    /**
     * Issues the MDTM [path] command to the server to get the modification
     * time of a specific file on the server.
     * Note that this command may not be supported by the server, in which
     * case <code>null</code> will be returned.
     *
     * @param path a <code>String</code> containing the pathname of the file.
     * @return a <code>Date</code> representing the last modification time
     *         or <code>null</code> if the server returned an error, which
     *         can be checked with {@link #getLastReplyCode()}.
     * @throws IOException if an error occurs during the transmission.
     */
    public Date getLastModified(String path) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("MDTM " + path);
        if (lastReplyCode == FtpReplyCode.FILE_STATUS) {
            String s = getResponseString();
            return parseRfc3659TimeValue(s.substring(4, s.length() - 1));
        }
        return null;
    }

    private static Date parseRfc3659TimeValue(String s) {
        Date result = null;
        try {
            var d = ZonedDateTime.parse(s, RFC3659_DATETIME_FORMAT);
            result = Date.from(d.toInstant());
        } catch (DateTimeParseException ex) {
        }
        return result;
    }

    /**
     * Sets the parser used to handle the directory output to the specified
     * one. By default the parser is set to one that can handle most FTP
     * servers output (Unix base mostly). However it may be necessary for
     * and application to provide its own parser due to some uncommon
     * output format.
     *
     * @param p The <code>FtpDirParser</code> to use.
     * @see #listFiles(String)
     */
    public sun.net.ftp.FtpClient setDirParser(FtpDirParser p) {
        parser = p;
        return this;
    }

    private static class FtpFileIterator implements Iterator<FtpDirEntry>, Closeable {

        private BufferedReader in = null;
        private FtpDirEntry nextFile = null;
        private FtpDirParser fparser = null;
        private boolean eof = false;

        public FtpFileIterator(FtpDirParser p, BufferedReader in) {
            this.in = in;
            this.fparser = p;
            readNext();
        }

        private void readNext() {
            nextFile = null;
            if (eof) {
                return;
            }
            String line = null;
            try {
                do {
                    line = in.readLine();
                    if (line != null) {
                        nextFile = fparser.parseLine(line);
                        if (nextFile != null) {
                            return;
                        }
                    }
                } while (line != null);
                in.close();
            } catch (IOException iOException) {
            }
            eof = true;
        }

        public boolean hasNext() {
            return nextFile != null;
        }

        public FtpDirEntry next() {
            FtpDirEntry ret = nextFile;
            readNext();
            return ret;
        }

        public void remove() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void close() throws IOException {
            if (in != null && !eof) {
                in.close();
            }
            eof = true;
            nextFile = null;
        }
    }

    /**
     * Issues a MLSD command to the server to get the specified directory
     * listing and applies the current parser to create an Iterator of
     * {@link java.net.ftp.FtpDirEntry}. Note that the Iterator returned is also a
     * {@link java.io.Closeable}.
     * If the server doesn't support the MLSD command, the LIST command is used
     * instead.
     *
     * {@link #completePending()} <b>has</b> to be called once the application
     * is finished iterating through the files.
     *
     * @param path the pathname of the directory to list or <code>null</code>
     *        for the current working directoty.
     * @return a <code>Iterator</code> of files or <code>null</code> if the
     *         command failed.
     * @throws IOException if an error occurred during the transmission
     * @see #setDirParser(FtpDirParser)
     * @see #changeDirectory(String)
     */
    public Iterator<FtpDirEntry> listFiles(String path) throws sun.net.ftp.FtpProtocolException, IOException {
        Socket s = null;
        BufferedReader sin = null;
        try {
            s = openDataConnection(path == null ? "MLSD" : "MLSD " + path);
        } catch (sun.net.ftp.FtpProtocolException FtpException) {
            // The server doesn't understand new MLSD command, ignore and fall
            // back to LIST
        }

        if (s != null) {
            sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
            return new FtpFileIterator(mlsxParser, sin);
        } else {
            s = openDataConnection(path == null ? "LIST" : "LIST " + path);
            if (s != null) {
                sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
                return new FtpFileIterator(parser, sin);
            }
        }
        return null;
    }

    private boolean sendSecurityData(byte[] buf) throws IOException,
            sun.net.ftp.FtpProtocolException {
        String s = Base64.getMimeEncoder().encodeToString(buf);
        return issueCommand("ADAT " + s);
    }

    private byte[] getSecurityData() {
        String s = getLastResponseString();
        if (s.substring(4, 9).equalsIgnoreCase("ADAT=")) {
            // Need to get rid of the leading '315 ADAT='
            // and the trailing newline
            return Base64.getMimeDecoder().decode(s.substring(9, s.length() - 1));
        }
        return null;
    }

    /**
     * Attempts to use Kerberos GSSAPI as an authentication mechanism with the
     * ftp server. This will issue an <code>AUTH GSSAPI</code> command, and if
     * it is accepted by the server, will followup with <code>ADAT</code>
     * command to exchange the various tokens until authentification is
     * successful. This conforms to Appendix I of RFC 2228.
     *
     * @return <code>true</code> if authentication was successful.
     * @throws IOException if an error occurs during the transmission.
     */
    public sun.net.ftp.FtpClient useKerberos() throws sun.net.ftp.FtpProtocolException, IOException {
        /*
         * Comment out for the moment since it's not in use and would create
         * needless cross-package links.
         *
        issueCommandCheck("AUTH GSSAPI");
        if (lastReplyCode != FtpReplyCode.NEED_ADAT)
        throw new sun.net.ftp.FtpProtocolException("Unexpected reply from server");
        try {
        GSSManager manager = GSSManager.getInstance();
        GSSName name = manager.createName("SERVICE:ftp@"+
        serverAddr.getHostName(), null);
        GSSContext context = manager.createContext(name, null, null,
        GSSContext.DEFAULT_LIFETIME);
        context.requestMutualAuth(true);
        context.requestReplayDet(true);
        context.requestSequenceDet(true);
        context.requestCredDeleg(true);
        byte []inToken = new byte[0];
        while (!context.isEstablished()) {
        byte[] outToken
        = context.initSecContext(inToken, 0, inToken.length);
        // send the output token if generated
        if (outToken != null) {
        if (sendSecurityData(outToken)) {
        inToken = getSecurityData();
        }
        }
        }
        loggedIn = true;
        } catch (GSSException e) {

        }
         */
        return this;
    }

    /**
     * Returns the Welcome string the server sent during initial connection.
     *
     * @return a <code>String</code> containing the message the server
     *         returned during connection or <code>null</code>.
     */
    public String getWelcomeMsg() {
        return welcomeMsg;
    }

    /**
     * Returns the last reply code sent by the server.
     *
     * @return the lastReplyCode
     */
    public FtpReplyCode getLastReplyCode() {
        return lastReplyCode;
    }

    /**
     * Returns the last response string sent by the server.
     *
     * @return the message string, which can be quite long, last returned
     *         by the server.
     */
    public String getLastResponseString() {
        StringBuilder sb = new StringBuilder();
        if (serverResponse != null) {
            for (String l : serverResponse) {
                if (l != null) {
                    sb.append(l);
                }
            }
        }
        return sb.toString();
    }

    /**
     * Returns, when available, the size of the latest started transfer.
     * This is retreived by parsing the response string received as an initial
     * response to a RETR or similar request.
     *
     * @return the size of the latest transfer or -1 if either there was no
     *         transfer or the information was unavailable.
     */
    public long getLastTransferSize() {
        return lastTransSize;
    }

    /**
     * Returns, when available, the remote name of the last transfered file.
     * This is mainly useful for "put" operation when the unique flag was
     * set since it allows to recover the unique file name created on the
     * server which may be different from the one submitted with the command.
     *
     * @return the name the latest transfered file remote name, or
     *         <code>null</code> if that information is unavailable.
     */
    public String getLastFileName() {
        return lastFileName;
    }

    /**
     * Attempts to switch to a secure, encrypted connection. This is done by
     * sending the "AUTH TLS" command.
     * <p>See <a href="http://www.ietf.org/rfc/rfc4217.txt">RFC 4217</a></p>
     * If successful this will establish a secure command channel with the
     * server, it will also make it so that all other transfers (e.g. a RETR
     * command) will be done over an encrypted channel as well unless a
     * {@link #reInit()} command or a {@link #endSecureSession()} command is issued.
     *
     * @return <code>true</code> if the operation was successful.
     * @throws IOException if an error occurred during the transmission.
     * @see #endSecureSession()
     */
    public sun.net.ftp.FtpClient startSecureSession() throws sun.net.ftp.FtpProtocolException, IOException {
        if (!isConnected()) {
            throw new sun.net.ftp.FtpProtocolException("Not connected yet", FtpReplyCode.BAD_SEQUENCE);
        }
        if (sslFact == null) {
            try {
                sslFact = (SSLSocketFactory) SSLSocketFactory.getDefault();
            } catch (Exception e) {
                throw new IOException(e.getLocalizedMessage());
            }
        }
        issueCommandCheck("AUTH TLS");
        Socket s = null;
        try {
            s = sslFact.createSocket(server, serverAddr.getHostName(), serverAddr.getPort(), true);
        } catch (javax.net.ssl.SSLException ssle) {
            try {
                disconnect();
            } catch (Exception e) {
            }
            throw ssle;
        }
        // Remember underlying socket so we can restore it later
        oldSocket = server;
        server = s;
        try {
            out = new PrintStream(new BufferedOutputStream(server.getOutputStream()),
                    true, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding + "encoding not found", e);
        }
        in = new BufferedInputStream(server.getInputStream());

        issueCommandCheck("PBSZ 0");
        issueCommandCheck("PROT P");
        useCrypto = true;
        return this;
    }

    /**
     * Sends a <code>CCC</code> command followed by a <code>PROT C</code>
     * command to the server terminating an encrypted session and reverting
     * back to a non crypted transmission.
     *
     * @return <code>true</code> if the operation was successful.
     * @throws IOException if an error occurred during transmission.
     * @see #startSecureSession()
     */
    public sun.net.ftp.FtpClient endSecureSession() throws sun.net.ftp.FtpProtocolException, IOException {
        if (!useCrypto) {
            return this;
        }

        issueCommandCheck("CCC");
        issueCommandCheck("PROT C");
        useCrypto = false;
        // Restore previous socket and streams
        server = oldSocket;
        oldSocket = null;
        try {
            out = new PrintStream(new BufferedOutputStream(server.getOutputStream()),
                    true, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding + "encoding not found", e);
        }
        in = new BufferedInputStream(server.getInputStream());

        return this;
    }

    /**
     * Sends the "Allocate" (ALLO) command to the server telling it to
     * pre-allocate the specified number of bytes for the next transfer.
     *
     * @param size The number of bytes to allocate.
     * @return <code>true</code> if the operation was successful.
     * @throws IOException if an error occurred during the transmission.
     */
    public sun.net.ftp.FtpClient allocate(long size) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("ALLO " + size);
        return this;
    }

    /**
     * Sends the "Structure Mount" (SMNT) command to the server. This let the
     * user mount a different file system data structure without altering his
     * login or accounting information.
     *
     * @param struct a <code>String</code> containing the name of the
     *        structure to mount.
     * @return <code>true</code> if the operation was successful.
     * @throws IOException if an error occurred during the transmission.
     */
    public sun.net.ftp.FtpClient structureMount(String struct) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("SMNT " + struct);
        return this;
    }

    /**
     * Sends a SYST (System) command to the server and returns the String
     * sent back by the server describing the operating system at the
     * server.
     *
     * @return a <code>String</code> describing the OS, or <code>null</code>
     *         if the operation was not successful.
     * @throws IOException if an error occurred during the transmission.
     */
    public String getSystem() throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("SYST");
        /*
         * 215 UNIX Type: L8 Version: SUNOS
         */
        String resp = getResponseString();
        // Get rid of the leading code and blank
        return resp.substring(4);
    }

    /**
     * Sends the HELP command to the server, with an optional command, like
     * SITE, and returns the text sent back by the server.
     *
     * @param cmd the command for which the help is requested or
     *        <code>null</code> for the general help
     * @return a <code>String</code> containing the text sent back by the
     *         server, or <code>null</code> if the command failed.
     * @throws IOException if an error occurred during transmission
     */
    public String getHelp(String cmd) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("HELP " + cmd);
        /**
         *
         * HELP
         * 214-The following commands are implemented.
         *   USER    EPRT    STRU    ALLO    DELE    SYST    RMD     MDTM    ADAT
         *   PASS    EPSV    MODE    REST    CWD     STAT    PWD     PROT
         *   QUIT    LPRT    RETR    RNFR    LIST    HELP    CDUP    PBSZ
         *   PORT    LPSV    STOR    RNTO    NLST    NOOP    STOU    AUTH
         *   PASV    TYPE    APPE    ABOR    SITE    MKD     SIZE    CCC
         * 214 Direct comments to ftp-bugs@jsn.
         *
         * HELP SITE
         * 214-The following SITE commands are implemented.
         *   UMASK           HELP            GROUPS
         *   IDLE            ALIAS           CHECKMETHOD
         *   CHMOD           CDPATH          CHECKSUM
         * 214 Direct comments to ftp-bugs@jsn.
         */
        Vector<String> resp = getResponseStrings();
        if (resp.size() == 1) {
            // Single line response
            return resp.get(0).substring(4);
        }
        // on multiple lines answers, like the ones above, remove 1st and last
        // line, concat the others.
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < resp.size() - 1; i++) {
            sb.append(resp.get(i).substring(3));
        }
        return sb.toString();
    }

    /**
     * Sends the SITE command to the server. This is used by the server
     * to provide services specific to his system that are essential
     * to file transfer.
     *
     * @param cmd the command to be sent.
     * @return <code>true</code> if the command was successful.
     * @throws IOException if an error occurred during transmission
     */
    public sun.net.ftp.FtpClient siteCmd(String cmd) throws sun.net.ftp.FtpProtocolException, IOException {
        issueCommandCheck("SITE " + cmd);
        return this;
    }
}
