/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4397774
 * @summary ConfigFile does not support nested static classes as Module names
 * @run main/othervm/policy=InnerClassConfig.policy -Djava.security.auth.login.config==file:${test.src}/InnerClassConfig.config InnerClassConfig
 */

import com.sun.security.auth.login.*;
import javax.security.auth.login.*;

public class InnerClassConfig {

    public static void main(String[] args) {

        Configuration config = null;
        try {
            config = Configuration.getConfiguration();
        } catch (SecurityException se) {
            System.out.println("test 1 failed");
            throw se;
        }

        AppConfigurationEntry[] entries =
                config.getAppConfigurationEntry("InnerClassConfig");

        System.out.println("module = " +
                        entries[0].getLoginModuleName());
        if (entries[0].getLoginModuleName().equals("package.Foo$Bar")) {
            System.out.println("test succeeded");
        } else {
            System.out.println("test 2 failed");
            throw new SecurityException("package name incorrect");
        }
    }
}
