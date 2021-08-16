/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7192202
 * @summary Make sure keytool prints both unknown and unparseable extensions
 * @modules java.base/sun.security.tools.keytool
 *          java.base/sun.security.util
 *          java.base/sun.security.x509
 * @compile -XDignore.symbol.file UnknownAndUnparseable.java
 * @run main UnknownAndUnparseable
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import sun.security.x509.PKIXExtensions;

public class UnknownAndUnparseable {
    public static void main(String[] args) throws Exception {

        String s = "-keystore ks -storepass changeit -keypass changeit -debug ";
        new File("ks").delete();

        // Create a cert with an unknown extension: 1.2.3.4, and an invalid
        // KeyUsage extension
        String genkey = s
                + "-genkeypair -alias a -dname CN=A -ext 1.2.3.4=1234 -keyalg rsa "
                + "-ext " + PKIXExtensions.KeyUsage_Id.toString() + "=5678";
        sun.security.tools.keytool.Main.main(genkey.split(" "));

        // Get the list output to a string
        String list = s + "-list -v";
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream oldOut = System.out;
        System.setOut(new PrintStream(bout));
        sun.security.tools.keytool.Main.main(list.split(" "));
        System.setOut(oldOut);
        String out = bout.toString();
        System.out.println(out);

        if (!out.contains("1.2.3.4")) {
            throw new Exception("Unknown extension 1.2.3.4 is not listed");
        }
        if (!out.contains("0000: 12 34")) {
            throw new Exception("Unknown extension 1.2.3.4 has no content");
        }
        if (!out.contains("Unparseable KeyUsage")) {
            throw new Exception("Unparseable KeyUsage is not listed");
        }
        if (!out.contains("0000: 56 78")) {
            throw new Exception("Unparseable KeyUsage has no content");
        }
    }
}
