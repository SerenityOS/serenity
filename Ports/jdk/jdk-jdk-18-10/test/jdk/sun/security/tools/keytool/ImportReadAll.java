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
 * @bug 6819272
 * @summary keytool -importcert should read the whole input
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ImportReadAll {
    public static void main(String[] args) throws Exception {
        keytool("-genkeypair -alias a -dname CN=a").shouldHaveExitValue(0);
        keytool("-genkeypair -alias ca -dname CN=ca").shouldHaveExitValue(0);

        keytool("-certreq -alias a -file a.req").shouldHaveExitValue(0);
        keytool("-gencert -alias ca -infile a.req -outfile a.crt")
                .shouldHaveExitValue(0);
        keytool("-importcert -alias a -file a.crt").shouldHaveExitValue(0);
    }

    static OutputAnalyzer keytool(String s) throws Exception {
        return SecurityTools.keytool(
                "-keystore importreadall.jks "
                + "-storepass changeit -keypass changeit -keyalg rsa " + s);
    }
}
