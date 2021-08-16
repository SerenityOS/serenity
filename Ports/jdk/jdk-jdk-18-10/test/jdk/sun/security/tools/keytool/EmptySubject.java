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
 * @bug 6847026
 * @summary keytool should be able to generate certreq and cert without subject name
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EmptySubject{
    static final String KS = "emptysubject.jks";
    public static void main(String[] args) throws Exception {
        kt("-alias", "ca", "-dname", "CN=CA", "-genkeypair");
        kt("-alias", "me", "-dname", "CN=Me", "-genkeypair");

        // When -dname is recognized, SAN must be specified, otherwise,
        // -printcert fails.
        kt("-alias", "me", "-certreq", "-dname", "", "-file", "me1.req")
                .shouldHaveExitValue(0);
        kt("-alias", "ca", "-gencert",
                "-infile", "me1.req", "-outfile", "me1.crt")
                .shouldHaveExitValue(0);
        kt("-printcert", "-file", "me1.crt").shouldNotHaveExitValue(0);

        kt("-alias", "me", "-certreq", "-file", "me2.req")
                .shouldHaveExitValue(0);
        kt("-alias", "ca", "-gencert", "-dname", "",
                "-infile", "me2.req", "-outfile", "me2.crt")
                .shouldHaveExitValue(0);
        kt("-printcert", "-file", "me2.crt").shouldNotHaveExitValue(0);

        kt("-alias", "me", "-certreq", "-dname", "", "-file", "me3.req")
                .shouldHaveExitValue(0);
        kt("-alias", "ca", "-gencert", "-ext", "san:c=email:me@me.com",
                "-infile", "me3.req", "-outfile", "me3.crt")
                .shouldHaveExitValue(0);
        kt("-printcert", "-file", "me3.crt").shouldHaveExitValue(0);

        kt("-alias", "me", "-certreq", "-file", "me4.req")
                .shouldHaveExitValue(0);
        kt("-alias", "ca", "-gencert", "-dname", "",
                "-ext", "san:c=email:me@me.com",
                "-infile", "me4.req", "-outfile", "me4.crt")
                .shouldHaveExitValue(0);
        kt("-printcert", "-file", "me4.crt").shouldHaveExitValue(0);
    }

    static OutputAnalyzer kt(String... s) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList(
                "-storepass", "changeit",
                "-keypass", "changeit",
                "-keystore", KS,
                "-keyalg", "rsa"));
        cmd.addAll(Arrays.asList(s));
        return SecurityTools.keytool(cmd);
    }
}
