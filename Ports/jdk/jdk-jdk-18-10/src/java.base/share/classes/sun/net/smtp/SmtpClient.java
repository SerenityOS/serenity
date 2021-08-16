/*
 * Copyright (c) 1995, 2017, Oracle and/or its affiliates. All rights reserved.
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

package sun.net.smtp;

import java.io.*;
import java.net.*;
import sun.net.TransferProtocolClient;
import sun.security.action.GetPropertyAction;

/**
 * This class implements the SMTP client.
 * You can send a piece of mail by creating a new SmtpClient, calling
 * the "to" method to add destinations, calling "from" to name the
 * sender, calling startMessage to return a stream to which you write
 * the message (with RFC733 headers) and then you finally close the Smtp
 * Client.
 *
 * @author      James Gosling
 */

public class SmtpClient extends TransferProtocolClient {

    private static int DEFAULT_SMTP_PORT = 25;
    String mailhost;
    SmtpPrintStream message;

    /**
     * issue the QUIT command to the SMTP server and close the connection.
     */
    public void closeServer() throws IOException {
        if (serverIsOpen()) {
            closeMessage();
            issueCommand("QUIT\r\n", 221);
            super.closeServer();
        }
    }

    void issueCommand(String cmd, int expect) throws IOException {
        sendServer(cmd);
        int reply;
        while ((reply = readServerResponse()) != expect)
            if (reply != 220) {
                throw new SmtpProtocolException(getResponseString());
            }
    }

    private void toCanonical(String s) throws IOException {
        if (s.startsWith("<"))
            issueCommand("rcpt to: " + s + "\r\n", 250);
        else
            issueCommand("rcpt to: <" + s + ">\r\n", 250);
    }

    public void to(String s) throws IOException {
        if (s.indexOf('\n') != -1) {
            throw new IOException("Illegal SMTP command",
                    new IllegalArgumentException("Illegal carriage return"));
        }
        int st = 0;
        int limit = s.length();
        int pos = 0;
        int lastnonsp = 0;
        int parendepth = 0;
        boolean ignore = false;
        while (pos < limit) {
            int c = s.charAt(pos);
            if (parendepth > 0) {
                if (c == '(')
                    parendepth++;
                else if (c == ')')
                    parendepth--;
                if (parendepth == 0)
                    if (lastnonsp > st)
                        ignore = true;
                    else
                        st = pos + 1;
            } else if (c == '(')
                parendepth++;
            else if (c == '<')
                st = lastnonsp = pos + 1;
            else if (c == '>')
                ignore = true;
            else if (c == ',') {
                if (lastnonsp > st)
                    toCanonical(s.substring(st, lastnonsp));
                st = pos + 1;
                ignore = false;
            } else {
                if (c > ' ' && !ignore)
                    lastnonsp = pos + 1;
                else if (st == pos)
                    st++;
            }
            pos++;
        }
        if (lastnonsp > st)
            toCanonical(s.substring(st, lastnonsp));
    }

    public void from(String s) throws IOException {
        if (s.indexOf('\n') != -1) {
            throw new IOException("Illegal SMTP command",
                    new IllegalArgumentException("Illegal carriage return"));
        }
        if (s.startsWith("<")) {
            issueCommand("mail from: " + s + "\r\n", 250);
        } else {
            issueCommand("mail from: <" + s + ">\r\n", 250);
        }
    }

    /** open a SMTP connection to host <i>host</i>. */
    private void openServer(String host) throws IOException {
        mailhost = host;
        openServer(mailhost, DEFAULT_SMTP_PORT);
        issueCommand("helo "+InetAddress.getLocalHost().getHostName()+"\r\n", 250);
    }

    public PrintStream startMessage() throws IOException {
        issueCommand("data\r\n", 354);
        try {
            message = new SmtpPrintStream(serverOutput, this);
        } catch (UnsupportedEncodingException e) {
            throw new InternalError(encoding+" encoding not found", e);
        }
        return message;
    }

    void closeMessage() throws IOException {
        if (message != null)
            message.close();
    }

    /** New SMTP client connected to host <i>host</i>. */
    public SmtpClient (String host) throws IOException {
        super();
        if (host != null) {
            try {
                openServer(host);
                mailhost = host;
                return;
            } catch(Exception e) {
            }
        }
        try {
            String s;
            mailhost = GetPropertyAction.privilegedGetProperty("mail.host");
            if (mailhost != null) {
                openServer(mailhost);
                return;
            }
        } catch(Exception e) {
        }
        try {
            mailhost = "localhost";
            openServer(mailhost);
        } catch(Exception e) {
            mailhost = "mailhost";
            openServer(mailhost);
        }
    }

    /** Create an uninitialized SMTP client. */
    public SmtpClient () throws IOException {
        this(null);
    }

    public SmtpClient(int to) throws IOException {
        super();
        setConnectTimeout(to);
        try {
            String s;
            mailhost = GetPropertyAction.privilegedGetProperty("mail.host");
            if (mailhost != null) {
                openServer(mailhost);
                return;
            }
        } catch(Exception e) {
        }
        try {
            mailhost = "localhost";
            openServer(mailhost);
        } catch(Exception e) {
            mailhost = "mailhost";
            openServer(mailhost);
        }
    }

    public String getMailHost() {
        return mailhost;
    }

    String getEncoding () {
        return encoding;
    }
}

class SmtpPrintStream extends java.io.PrintStream {
    private SmtpClient target;
    private int lastc = '\n';

    SmtpPrintStream (OutputStream fos, SmtpClient cl) throws UnsupportedEncodingException {
        super(fos, false, cl.getEncoding());
        target = cl;
    }

    public void close() {
        if (target == null)
            return;
        if (lastc != '\n') {
            write('\n');
        }
        try {
            target.issueCommand(".\r\n", 250);
            target.message = null;
            out = null;
            target = null;
        } catch (IOException e) {
        }
    }

    public void write(int b) {
        try {
            // quote a dot at the beginning of a line
            if (lastc == '\n' && b == '.') {
                out.write('.');
            }

            // translate NL to CRLF
            if (b == '\n' && lastc != '\r') {
                out.write('\r');
            }
            out.write(b);
            lastc = b;
        } catch (IOException e) {
        }
    }

    public void write(byte b[], int off, int len) {
        try {
            int lc = lastc;
            while (--len >= 0) {
                int c = b[off++];

                // quote a dot at the beginning of a line
                if (lc == '\n' && c == '.')
                    out.write('.');

                // translate NL to CRLF
                if (c == '\n' && lc != '\r') {
                    out.write('\r');
                }
                out.write(c);
                lc = c;
            }
            lastc = lc;
        } catch (IOException e) {
        }
    }
    public void print(String s) {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            write(s.charAt(i));
        }
    }
}
