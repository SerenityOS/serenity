/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

/**
 * @test
 * @bug 8212003 8214024
 * @summary Deprecating the default keytool -keyalg option
 * @library /test/lib
 */

public class RemoveKeyAlgDefault {

    private static final String COMMON = "-keystore ks -storetype jceks "
            + "-storepass changeit -keypass changeit";

    public static void main(String[] args) throws Throwable {

        kt("-genkeypair -keyalg DSA -alias a -dname CN=A")
                .shouldHaveExitValue(0)
                .shouldContain("Generating")
                .shouldNotContain("-keyalg option must be specified");

        kt("-genkeypair -alias b -dname CN=B")
                .shouldHaveExitValue(1)
                .shouldContain("-keyalg option must be specified");

        kt("-genseckey -keyalg DES -alias c")
                .shouldHaveExitValue(0)
                .shouldContain("Generated")
                .shouldNotContain("-keyalg option must be specified");

        kt("-genseckey -alias d")
                .shouldHaveExitValue(1)
                .shouldContain("-keyalg option must be specified");
    }

    private static OutputAnalyzer kt(String cmd) throws Throwable {
        return SecurityTools.keytool(COMMON + " " + cmd);
    }
}
