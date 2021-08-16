/*
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.Security;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

public class WebGet {

    static String user = System.getProperty("user");
    static String pass = System.getProperty("pass");
    static String kuser = System.getProperty("kuser");
    static String kpass = System.getProperty("kpass");
    static String showhint = System.getProperty("showhint");

    static class MyAuthenticator extends Authenticator {
        public MyAuthenticator () {
            super ();
        }

        public PasswordAuthentication getPasswordAuthentication ()
        {
            // scheme is the only key for Negotiate
            if(getRequestingScheme().equalsIgnoreCase("negotiate") ||
                    getRequestingScheme().equalsIgnoreCase("kerberos")) {
                if(showhint != null)
                    System.out.println("::::: PROVIDING Kerberos PASSWORD AND USERNAME " + kuser +":"+kpass+" :::::");
                return (new PasswordAuthentication (kuser, kpass.toCharArray()));
            } else {
                if(showhint != null)
                    System.out.println("::::: PROVIDING PASSWORD AND USERNAME " + user +":"+pass+" :::::");
                return (new PasswordAuthentication (user, pass.toCharArray()));
            }
        }
    }

    /**
     * Creates a new instance of WebGet
     */
    static void url(String urls) throws Exception {
        Authenticator.setDefault (new MyAuthenticator ());
        //Security.setProperty("auth.login.defaultCallbackHandler", "WebGet$Handler");
        URL url = new URL(urls);
        InputStream ins = url.openConnection().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(ins));
        String str;
        while((str = reader.readLine()) != null)
            System.out.println(str);
    }

    /**
     * @param args 1. url
     *             2. if given, means there should be error
     */
    public static void main(String[] args) throws Exception {
        url(args[0]);
    }
}
