/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4633622
 * @summary  bug in LoginContext when Configuration is subclassed
 * @build ResetConfigModule ResetModule
 * @run main ResetConfigModule
 */

import javax.security.auth.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;
import javax.security.auth.callback.*;
import java.util.*;

public class ResetConfigModule {

    public static void main(String[] args) throws Exception {

        Configuration previousConf = Configuration.getConfiguration();
        ClassLoader previousCL = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(
                    ResetConfigModule.class.getClassLoader());
            Configuration.setConfiguration(new MyConfig());

            LoginContext lc = new LoginContext("test");
            try {
                lc.login();
                throw new SecurityException("test 1 failed");
            } catch (LoginException le) {
                if (le.getCause() != null &&
                    le.getCause() instanceof SecurityException) {
                    System.out.println("good so far");
                } else {
                    throw le;
                }
            }

            LoginContext lc2 = new LoginContext("test2");
            try {
                lc2.login();
                throw new SecurityException("test 2 failed");
            } catch (LoginException le) {
                if (le.getCause() != null &&
                    le.getCause()  instanceof SecurityException) {
                    System.out.println("test succeeded");
                } else {
                    throw le;
                }
            }
        } finally {
            Configuration.setConfiguration(previousConf);
            Thread.currentThread().setContextClassLoader(previousCL);
        }
    }
}

class MyConfig extends Configuration {
    private AppConfigurationEntry[] entries = {
        new AppConfigurationEntry("ResetModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                new HashMap()) };
    public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
        return entries;
    }
    public void refresh() { }
}
