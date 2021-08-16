/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8244148
 * @library /test/lib
 * @library /test/jdk/sun/security/util/module_patch
 * @build java.base/sun.security.util.FilePaths
 * @modules java.base/jdk.internal.misc
 * @run main MyOwnCacerts
 */

import jdk.test.lib.SecurityTools;
import jdk.test.lib.process.OutputAnalyzer;

public class MyOwnCacerts {

    // The --patch-module must be explicitly specified on the keytool
    // command line because it's in a separate process
    private static final String PATCH_OPTION;

    static {
        String tmp = "";
        for (String a : jdk.internal.misc.VM.getRuntimeArguments()) {
            if (a.startsWith("--patch-module")) {
                tmp = "-J" + a + " ";
                break;
            }
        }
        PATCH_OPTION = tmp;
    }

    public static void main(String[] args) throws Exception {
        kt("-keystore mycacerts -genkeypair -alias a" +
                " -dname CN=root -keyalg EC -storepass changeit")
                .shouldContain("Warning: use -cacerts option");
        kt("-list -cacerts -storepass changeit")
                .shouldContain("Your keystore contains 1 entry");
    }

    static OutputAnalyzer kt(String s) throws Exception {
        return SecurityTools.keytool(PATCH_OPTION + s);
    }
}
