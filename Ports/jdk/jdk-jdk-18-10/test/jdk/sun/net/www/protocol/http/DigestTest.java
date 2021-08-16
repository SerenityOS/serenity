/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4432213
 * @modules java.base/sun.net.www
 * @run main/othervm -Dhttp.auth.digest.validateServer=true DigestTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=true
 *                   -Dhttp.auth.digest.validateServer=true DigestTest
 * @run main/othervm -Dhttp.auth.digest.validateServer=true
                     -Dtest.succeed=true DigestTest
 * @run main/othervm -Djava.net.preferIPv6Addresses=true
 *                   -Dhttp.auth.digest.validateServer=true
                     -Dtest.succeed=true DigestTest
 * @summary  Need to support Digest Authentication for Proxies
 */

import java.io.*;
import java.util.*;
import java.net.*;
import java.security.*;
import sun.net.www.*;

/* This is one simple test of the RFC2617 digest authentication behavior
 * It specifically tests that the client correctly checks the returned
 * Authentication-Info header field from the server and throws an exception
 * if the password is wrong
 */

class DigestServer extends Thread {

    ServerSocket s;
    Socket   s1;
    InputStream  is;
    OutputStream os;
    int port;

    String reply1 = "HTTP/1.1 401 Unauthorized\r\n"+
        "WWW-Authenticate: Digest realm=\""+realm+"\" domain=/ "+
        "nonce=\""+nonce+"\" qop=\"auth\"\r\n\r\n";

    String reply2 = "HTTP/1.1 200 OK\r\n" +
        "Date: Mon, 15 Jan 2001 12:18:21 GMT\r\n" +
        "Server: Apache/1.3.14 (Unix)\r\n" +
        "Content-Type: text/html; charset=iso-8859-1\r\n" +
        "Transfer-encoding: chunked\r\n";
    String body =
        "B\r\nHelloWorld1\r\n"+
        "B\r\nHelloWorld2\r\n"+
        "B\r\nHelloWorld3\r\n"+
        "B\r\nHelloWorld4\r\n"+
        "B\r\nHelloWorld5\r\n"+
        "0\r\n\r\n";
    String authInfo =
        "Authentication-Info: ";

    DigestServer (ServerSocket y) {
        s = y;
        port = s.getLocalPort();
    }

    public void run () {
        try {
                s1 = s.accept ();
                is = s1.getInputStream ();
                os = s1.getOutputStream ();
                is.read ();
                os.write (reply1.getBytes());
                Thread.sleep (2000);
                s1.close ();

                s1 = s.accept ();
                is = s1.getInputStream ();
                os = s1.getOutputStream ();
                //is.read ();
                // need to get the cnonce out of the response
                MessageHeader header = new MessageHeader (is);
                String raw = header.findValue ("Authorization");
                HeaderParser parser = new HeaderParser (raw);
                String cnonce = parser.findValue ("cnonce");
                String cnstring = parser.findValue ("nc");

                String reply = reply2 + authInfo + getAuthorization (uri, "GET", cnonce, cnstring) +"\r\n" + body;
                os.write (reply.getBytes());
                Thread.sleep (2000);
                s1.close ();
        } catch (Exception e) {
            System.out.println (e);
            e.printStackTrace();
        } finally {
            try { s.close(); } catch (IOException unused) {}
        }
    }

    static char[] passwd = "password".toCharArray();
    static String username = "user";
    static String nonce = "abcdefghijklmnopqrstuvwxyz";
    static String realm = "wallyworld";
    static String uri = "/foo.html";

    private String getAuthorization (String uri, String method, String cnonce, String cnstring) {
        String response;

        try {
            response = computeDigest(false, username,passwd,realm,
                                        method, uri, nonce, cnonce, cnstring);
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }

        String value = "Digest"
                        + " qop=auth\""
                        + "\", cnonce=\"" + cnonce
                        + "\", rspauth=\"" + response
                        + "\", nc=\"" + cnstring + "\"";
        return (value+ "\r\n");
    }

    private String computeDigest(
                        boolean isRequest, String userName, char[] password,
                        String realm, String connMethod,
                        String requestURI, String nonceString,
                        String cnonce, String ncValue
                    ) throws NoSuchAlgorithmException
    {

        String A1, HashA1;

        MessageDigest md = MessageDigest.getInstance("MD5");

        {
            A1 = userName + ":" + realm + ":";
            HashA1 = encode(A1, password, md);
        }

        String A2;
        if (isRequest) {
            A2 = connMethod + ":" + requestURI;
        } else {
            A2 = ":" + requestURI;
        }
        String HashA2 = encode(A2, null, md);
        String combo, finalHash;

        { /* RRC2617 when qop=auth */
            combo = HashA1+ ":" + nonceString + ":" + ncValue + ":" +
                        cnonce + ":auth:" +HashA2;

        }
        finalHash = encode(combo, null, md);
        return finalHash;
    }

    private String encode(String src, char[] passwd, MessageDigest md) {
        md.update(src.getBytes());
        if (passwd != null) {
            byte[] passwdBytes = new byte[passwd.length];
            for (int i=0; i<passwd.length; i++)
                passwdBytes[i] = (byte)passwd[i];
            md.update(passwdBytes);
            Arrays.fill(passwdBytes, (byte)0x00);
        }
        byte[] digest = md.digest();
        return HexFormat.of().formatHex(digest);
    }

}

public class DigestTest {

    static final boolean SUCCEED =
        Boolean.parseBoolean(System.getProperty("test.succeed", "false"));

    static class MyAuthenticator extends Authenticator {
        public MyAuthenticator () {
            super ();
        }

        public PasswordAuthentication getPasswordAuthentication ()
        {
            char[] passwd = SUCCEED ? DigestServer.passwd.clone()
                                      : "Wrongpassword".toCharArray();
            return new PasswordAuthentication("user", passwd);
        }
    }


    public static void main(String[] args) throws Exception {
        int port;
        DigestServer server;
        ServerSocket sock;

        InetAddress loopback = InetAddress.getLoopbackAddress();
        try {
            sock = new ServerSocket();
            sock.bind(new InetSocketAddress(loopback, 0));
            port = sock.getLocalPort();
        }
        catch (Exception e) {
            System.out.println ("Exception: " + e);
            throw e;
        }

        server = new DigestServer(sock);
        server.start ();
        boolean passed = false;
        ProtocolException exception = null;

        try  {
            Authenticator.setDefault (new MyAuthenticator ());
            String address = loopback.getHostAddress();
            if (address.indexOf(':') > -1)  address = "[" + address + "]";
            String s = "http://" + address + ":" + port + DigestServer.uri;
            URL url = new URL(s);
            java.net.URLConnection conURL =  url.openConnection(Proxy.NO_PROXY);

            InputStream in = conURL.getInputStream();
            while (in.read () != -1) {}
            in.close ();
            if (SUCCEED) passed = true;
        } catch(ProtocolException e) {
            exception = e;
            if (!SUCCEED) passed = true;
        }

        if (!passed) {
            if (!SUCCEED) {
                throw new RuntimeException("Expected a ProtocolException from wrong password");
            } else {
                assert exception != null;
                throw new RuntimeException("Unexpected ProtocolException from correct password: "
                                            + exception, exception);
            }
        }
    }
}
