/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

import javax.security.auth.login.LoginContext;

/**
 * @test
 * @bug 8050461
 * @summary Test should throw Configuration error if configuration file contains
 * syntax error
 * @build SampleLoginModule JAASConfigSyntaxTest
 * @run main/othervm -Djava.security.auth.login.config=file:${test.src}/JAASSynWithOutApplication.config JAASConfigSyntaxTest
 * @run main/othervm -Djava.security.auth.login.config=file:${test.src}/JAASSynWithOutBraces.config JAASConfigSyntaxTest
 * @run main/othervm -Djava.security.auth.login.config=file:${test.src}/JAASSynWithOutFlag.config JAASConfigSyntaxTest
 * @run main/othervm -Djava.security.auth.login.config=file:${test.src}/JAASSynWithOutLoginModule.config JAASConfigSyntaxTest
 * @run main/othervm -Djava.security.auth.login.config=file:${test.src}/JAASSynWithOutSemiColen.config JAASConfigSyntaxTest
 */
public class JAASConfigSyntaxTest {

    private static final String TEST_NAME = "JAASConfigSyntaxTest";

    public static void main(String[] args) throws Exception {
        try {
            LoginContext lc = new LoginContext(TEST_NAME);
            lc.login();
            throw new RuntimeException("Test Case Failed, did not get "
                    + "expected exception");
        } catch (Exception ex) {
            if (ex.getMessage().contains("java.io.IOException: "
                    + "Configuration Error:")) {
                System.out.println("Test case passed");
            } else {
                throw new RuntimeException(ex);
            }
        }
    }
}
