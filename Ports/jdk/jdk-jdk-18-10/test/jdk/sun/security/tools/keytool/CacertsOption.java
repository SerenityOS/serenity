/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8162739
 * @summary Create new keytool option to access cacerts file
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.tools
 * @run main/othervm -Duser.language=en -Duser.country=US CacertsOption
 */

import sun.security.tools.KeyStoreUtil;
import sun.security.tools.keytool.Main;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.security.KeyStore;
import java.util.Collections;

public class CacertsOption {

    public static void main(String[] args) throws Exception {

        run("-help -list");
        if (!msg.contains("-cacerts")) {
            throw new Exception("No cacerts in help:\n" + msg);
        }

        String cacerts = KeyStoreUtil.getCacerts();

        run("-list -keystore " + cacerts);
        if (!msg.contains("Warning:")) {
            throw new Exception("No warning in output:\n" + msg);
        }

        run("-list -cacerts");
        KeyStore ks = KeyStore.getInstance(new File(cacerts), (char[])null);
        for (String alias: Collections.list(ks.aliases())) {
            if (!msg.contains(alias)) {
                throw new Exception(alias + " not found in\n" + msg);
            }
        }

        try {
            run("-list -cacerts -storetype jks");
            throw new Exception("Should fail");
        } catch (IllegalArgumentException iae) {
            if (!msg.contains("cannot be used with")) {
                throw new Exception("Bad error msg\n" + msg);
            }
        }
    }

    private static String msg = null;

    private static void run(String cmd) throws Exception {
        msg = null;
        cmd += " -storepass changeit -debug";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bout);
        PrintStream oldOut = System.out;
        PrintStream oldErr = System.err;
        try {
            System.setOut(ps);
            System.setErr(ps);
            Main.main(cmd.split(" "));
        } finally {
            System.setErr(oldErr);
            System.setOut(oldOut);
            msg = new String(bout.toByteArray());
        }
    }
}
