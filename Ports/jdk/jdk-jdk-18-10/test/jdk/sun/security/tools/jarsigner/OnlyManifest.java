/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7004035
 * @summary signed jar with only META-INF/* inside is not verifiable
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OnlyManifest {
    static OutputAnalyzer kt(String cmd) throws Exception {
        return SecurityTools.keytool("-storepass changeit -keypass changeit "
                + "-keystore ks -keyalg rsa " + cmd);
    }

    static void gencert(String owner, String cmd) throws Exception {
        kt("-certreq -alias " + owner + " -file tmp.req");
        kt("-gencert -infile tmp.req -outfile tmp.cert " + cmd);
        kt("-import -alias " + owner + " -file tmp.cert");
    }

    public static void main(String[] args) throws Exception {
        // Create an empty jar file with only MANIFEST.MF
        Files.write(Path.of("manifest"), List.of("Key: Value"));
        SecurityTools.jar("cvfm a.jar manifest");

        kt("-alias ca -dname CN=ca -genkey -validity 300 -ext bc:c")
                .shouldHaveExitValue(0);
        kt("-alias a -dname CN=a -genkey -validity 300")
                .shouldHaveExitValue(0);
        gencert("a", "-alias ca -validity 300");

        SecurityTools.jarsigner("-keystore ks -storepass changeit"
                + " a.jar a -debug -strict")
                .shouldHaveExitValue(0);
        SecurityTools.jarsigner("-keystore ks -storepass changeit"
                + " -verify a.jar a -debug -strict")
                .shouldHaveExitValue(0)
                .shouldNotContain("unsigned");
    }
}
