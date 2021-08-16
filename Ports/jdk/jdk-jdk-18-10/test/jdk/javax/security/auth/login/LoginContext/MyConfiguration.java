/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import javax.security.auth.login.AppConfigurationEntry;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.OPTIONAL;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.SUFFICIENT;
import javax.security.auth.login.Configuration;

/**
 * This class is used to test LoginContext constructor API. It simply contains
 * one configuration entry: PT.
 */
public class MyConfiguration extends Configuration {

    private static final AppConfigurationEntry[] ptAE
            = new AppConfigurationEntry[2];
    private static final HashMap<String, String> map = new HashMap<>();
    private boolean optionOrder = false;

    public MyConfiguration() {
        setupConfiguration();
    }

    public MyConfiguration(boolean optionOrder) {
        this.optionOrder = optionOrder;
        setupConfiguration();
    }

    private void setupConfiguration() {
        ptAE[0] = new AppConfigurationEntry("SmartLoginModule",
                optionOrder ? OPTIONAL : REQUIRED,
                map);
        ptAE[1] = new AppConfigurationEntry("DummyLoginModule",
                optionOrder ? SUFFICIENT : REQUIRED,
                map);
    }

    @Override
    public AppConfigurationEntry[]
            getAppConfigurationEntry(String applicationName) {
        if (applicationName.equals("PT")) {
            return ptAE;
        } else {
            return null;
        }
    }

}
