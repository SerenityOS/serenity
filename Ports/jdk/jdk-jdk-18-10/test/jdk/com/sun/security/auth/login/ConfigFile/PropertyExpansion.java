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
 * @bug 4337769
 * @summary     ConfigFile should support system property expansion
 * @run main/othervm/policy=PropertyExpansion.policy -Djava.security.auth.login.config==file:${test.src}/PropertyExpansion.config PropertyExpansion
 */

import com.sun.security.auth.login.*;
import javax.security.auth.login.*;

public class PropertyExpansion {

    public static void main(String[] args) {

        try {
            ConfigFile config = new ConfigFile();
            throw new IllegalStateException("test 1 failed");
        } catch (SecurityException se) {
            // good
            se.printStackTrace();
        }

        Configuration config = null;
        try {
            config = Configuration.getConfiguration();
        } catch (SecurityException se) {
            System.out.println("test 2 failed");
            throw se;
        }

        AppConfigurationEntry[] entries =
                config.getAppConfigurationEntry("PropertyExpansion");

        // there are 2 entries
        if (entries.length != 2)
                throw new IllegalStateException("test 2 failed");

        for (int i = 0; i < 2; i++) {
                System.out.println("module " + i + " = " +
                        entries[i].getLoginModuleName());
                System.out.println("control flag " + i + " = " +
                        entries[i].getControlFlag());
                java.util.Map map = entries[i].getOptions();
                System.out.println("option " + i + " = useFile, " +
                        "value = " + map.get("useFile"));
                System.out.println("option " + i + " = debug, " +
                        "value = " + map.get("debug"));

                if (i == 0 && map.get("useFile") == null ||
                    i == 0 && map.get("debug") != null) {
                    throw new IllegalStateException("test 3 failed");
                }
                if (i == 1 && map.get("useFile") != null ||
                    i == 1 && map.get("debug") == null) {
                    throw new IllegalStateException("test 4 failed");
                }
        }

        System.out.println("test succeeded");

    }
}
