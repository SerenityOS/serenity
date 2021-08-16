/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6825352 6937978
 * @summary support self-issued certificate in keytool and let -gencert generate the chain
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

public class SelfIssued {
    public static void main(String[] args) throws Exception {
        keytool("-alias ca -dname CN=CA -genkeypair");
        keytool("-alias ca1 -dname CN=CA1 -genkeypair");
        keytool("-alias ca2 -dname CN=CA2 -genkeypair");
        keytool("-alias e1 -dname CN=E1 -genkeypair");

        // ca signs ca1, ca1 signs ca2, all self-issued
        keytool("-alias ca1 -certreq -file ca1.req");
        keytool("-alias ca -gencert -ext san=dns:ca1 "
                + "-infile ca1.req -outfile ca1.crt");
        keytool("-alias ca1 -importcert -file ca1.crt");

        keytool("-alias ca2 -certreq -file ca2.req");
        keytool("-alias ca1 -gencert -ext san=dns:ca2 "
                + "-infile ca2.req -outfile ca2.crt");
        keytool("-alias ca2 -importcert -file ca2.crt");

        // Import e1 signed by ca2, should add ca2 and ca1, at least 3 certs in the chain
        keytool("-alias e1 -certreq -file e1.req");
        keytool("-alias ca2 -gencert -infile e1.req -outfile e1.crt");

        keytool("-alias ca1 -delete");
        keytool("-alias ca2 -delete");
        keytool("-alias e1 -importcert -file e1.crt");
        keytool("-alias e1 -list -v")
                .shouldContain("[3]");
    }

    static OutputAnalyzer keytool(String s) throws Exception {
        return SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa " + s);
    }
}

