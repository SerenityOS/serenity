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

/**
 * @test
 * @bug 6236533
 * @summary verify that JKS throws the correct exception if an incorrect password is specified
 * @author Andreas Sterbenz
 */

import java.io.*;
import java.util.*;

import java.security.*;
import java.security.KeyStore.*;

public class WrongPassword {

    private final static String BASE = System.getProperty("test.src", ".");

    public static void main(String[] args) throws Exception {
        File ksFile = new File(BASE, "pw.jks");
        char[] pw = "test12".toCharArray();
        char[] wrongPW = "foobar".toCharArray();
        String alias = "mykey";
        String otherAlias = "foo";

        KeyStore ks;
        InputStream in;
        Entry entry;

        ks = KeyStore.getInstance("JKS");
        in = new FileInputStream(ksFile);
        ks.load(in, null);
        in.close();
        System.out.println(Collections.list(ks.aliases()));

        ks = KeyStore.getInstance("JKS");
        in = new FileInputStream(ksFile);
        try {
            ks.load(in, wrongPW);
        } catch (IOException e) {
            e.printStackTrace();
            Throwable cause = e.getCause();
            if (cause instanceof UnrecoverableKeyException == false) {
                throw new Exception("not an UnrecoverableKeyException: " + cause);
            }
        }
        in.close();

        ks = KeyStore.getInstance("JKS");
        in = new FileInputStream(ksFile);
        ks.load(in, pw);
        in.close();
        System.out.println(Collections.list(ks.aliases()));

        try {
            entry = ks.getEntry(alias, null);
            throw new Exception("no exception");
        } catch (UnrecoverableKeyException e) {
            System.out.println(e);
        }

        try {
            entry = ks.getEntry(alias, new PasswordProtection(wrongPW));
            throw new Exception("no exception");
        } catch (UnrecoverableKeyException e) {
            System.out.println(e);
        }

        try {
            entry = ks.getEntry(alias, new PasswordProtection(new char[0]));
            throw new Exception("no exception");
        } catch (UnrecoverableKeyException e) {
            System.out.println(e);
        }

        entry = ks.getEntry(alias, new PasswordProtection(pw));
        System.out.println(entry.toString().split("\n")[0]);

        try {
            ks.getKey(alias, null);
            throw new Exception("no exception");
        } catch (UnrecoverableKeyException e) {
            System.out.println(e);
        }

        try {
            ks.getKey(alias, wrongPW);
            throw new Exception("no exception");
        } catch (UnrecoverableKeyException e) {
            System.out.println(e);
        }

        try {
            ks.getKey(alias, new char[0]);
            throw new Exception("no exception");
        } catch (UnrecoverableKeyException e) {
            System.out.println(e);
        }

        Key k = ks.getKey(alias, pw);
        System.out.println(k.toString().split("\n")[0]);

        System.out.println(ks.getEntry(otherAlias, null));
        System.out.println(ks.getEntry(otherAlias, new PasswordProtection(wrongPW)));
        System.out.println(ks.getKey(otherAlias, null));
        System.out.println(ks.getKey(otherAlias, wrongPW));

        System.out.println("OK");
    }
}
