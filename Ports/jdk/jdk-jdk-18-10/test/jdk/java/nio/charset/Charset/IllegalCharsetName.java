/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6330020 8184665
 * @summary Ensure Charset.forName/isSupport throws the correct exception
 *          if the charset names passed in are illegal.
 */

import java.nio.charset.*;

public class IllegalCharsetName {
    public static void main(String[] args) throws Exception {
        String[] illegalNames = {
            ".",
            "_",
            ":",
            "-",
            ".name",
            "_name",
            ":name",
            "-name",
            "name*name",
            "name?name"
        };
        for (int i = 0; i < illegalNames.length; i++) {
            try {
                Charset.forName(illegalNames[i]);
                throw new Exception("Charset.forName(): No exception thrown");
            } catch (IllegalCharsetNameException x) { //expected
            }

            try {
                Charset.isSupported(illegalNames[i]);
                throw new Exception("Charset.isSupported(): No exception thrown");
            } catch (IllegalCharsetNameException x) { //expected
            }
        }

        // Standard charsets may bypass alias checking during startup, test that
        // they're all well-behaved as a sanity test
        checkAliases(StandardCharsets.ISO_8859_1);
        checkAliases(StandardCharsets.US_ASCII);
        checkAliases(StandardCharsets.UTF_8);
    }

    private static void checkAliases(Charset cs) {
        for (String alias : cs.aliases()) {
            Charset.forName(alias);
            Charset.isSupported(alias);
        }
    }
}
