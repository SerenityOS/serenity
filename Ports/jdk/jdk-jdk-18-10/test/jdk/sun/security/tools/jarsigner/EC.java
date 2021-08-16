/*
 * Copyright (c) 2009, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6870812
 * @summary enhance security tools to use ECC algorithm
 * @library /test/lib
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class EC {
    static OutputAnalyzer kt(String cmd) throws Exception {
        return SecurityTools.keytool("-storepass changeit "
                + "-keypass changeit -keystore ks " + cmd);
    }

    static void gencert(String owner, String cmd) throws Exception {
        kt("-certreq -alias " + owner + " -file tmp.req")
                .shouldHaveExitValue(0);
        kt("-gencert -infile tmp.req -outfile tmp.cert " + cmd)
                .shouldHaveExitValue(0);
        kt("-import -alias " + owner + " -file tmp.cert")
                .shouldHaveExitValue(0);
    }

    static OutputAnalyzer js(String cmd) throws Exception {
        return SecurityTools.jarsigner("-keystore ks -storepass changeit " + cmd);
    }

    public static void main(String[] args) throws Exception {
        Files.write(Path.of("A"), List.of("A"));
        JarUtils.createJarFile(Path.of("a.jar"), Path.of("."), Path.of("A"));

        kt("-alias ca -dname CN=ca -keyalg ec -genkey -validity 300 -ext bc:c")
                .shouldHaveExitValue(0);
        kt("-alias a -dname CN=a -keyalg ec -genkey")
                .shouldHaveExitValue(0);
        gencert("a", "-alias ca -validity 300");

        kt("-alias b -dname CN=b -keyalg ec -genkey")
                .shouldHaveExitValue(0);
        gencert("b", "-alias ca -validity 300");

        // Ensure key length sufficient for intended hash (SHA512withECDSA)
        kt("-alias c -dname CN=c -keyalg ec -genkey -keysize 521")
                .shouldHaveExitValue(0);
        gencert("c", "-alias ca -validity 300");

        kt("-alias x -dname CN=x -keyalg ec -genkey -validity 300")
                .shouldHaveExitValue(0);
        gencert("x", "-alias ca -validity 300");

        js("a.jar a -debug -strict").shouldHaveExitValue(0);
        js("a.jar b -debug -strict -sigalg SHA256withECDSA").shouldHaveExitValue(0);
        js("a.jar c -debug -strict -sigalg SHA512withECDSA").shouldHaveExitValue(0);

        js("-verify a.jar a -debug -strict").shouldHaveExitValue(0);
        js("-verify a.jar b -debug -strict").shouldHaveExitValue(0);
        js("-verify a.jar c -debug -strict").shouldHaveExitValue(0);

        // Not signed by x, should exit with non-zero
        js("-verify a.jar x -debug -strict").shouldNotHaveExitValue(0);
    }
}
