/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8192987
 * @summary keytool should remember real storetype if it is not provided
 * @library /test/lib
 * @build jdk.test.lib.SecurityTools
 *        jdk.test.lib.Utils
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 * @run main/othervm RealType
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

import java.nio.file.Files;
import java.nio.file.Paths;

public class RealType {

    public static void main(String[] args) throws Throwable {

        kt("-genkeypair -keyalg DSA -alias a -dname CN=A -keypass changeit -storetype jks")
                .shouldHaveExitValue(0);

        // -keypasswd command should be allowed on JKS
        kt("-keypasswd -alias a -new t0ps3cr3t")
                .shouldHaveExitValue(0);

        Files.delete(Paths.get("ks"));

        kt("-genkeypair -keyalg DSA -alias a -dname CN=A -keypass changeit -storetype pkcs12")
                .shouldHaveExitValue(0);

        // A pkcs12 keystore cannot be loaded as a JCEKS keystore
        kt("-list -storetype jceks").shouldHaveExitValue(1);
    }

    static OutputAnalyzer kt(String arg) throws Exception {
        return SecurityTools.keytool("-debug -keystore ks -storepass changeit " + arg);
    }
}
