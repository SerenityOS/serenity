/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6208996
 * @summary using -Djavax.security.auth.login.Configuration==foo doesn't ignore other configs
 *
 * @run main/othervm -Djava.security.properties=${test.src}/Override.props -Djava.security.auth.login.config==file:${test.src}/Override.good.config Override
 */

import javax.security.auth.login.*;
import com.sun.security.auth.login.*;

public class Override {
    public static void main(String[] args) throws Exception {
        ConfigFile c = new ConfigFile();
        AppConfigurationEntry[] good = c.getAppConfigurationEntry("good");
        AppConfigurationEntry[] bad = c.getAppConfigurationEntry("bad");

        if (good != null && bad == null) {
            System.out.println("test passed");
        } else {
            if (good == null) {
                throw new SecurityException("could not get good entries");
            } else {
                throw new SecurityException("incorrectly got bad entries");
            }
        }
    }
}
