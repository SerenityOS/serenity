/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4406033
 * @summary     ConfigFile throws an inconsistent error message
 *              when the configuration file is not found
 * @run main/othervm -Duser.language=en InconsistentError
 */

import com.sun.security.auth.login.*;
import javax.security.auth.login.*;

public class InconsistentError {

    public static void main(String[] args) {

        try {
            System.setProperty("java.security.auth.login.config",
                                "=nofile");
            ConfigFile config = new ConfigFile();
            throw new SecurityException("test 1 failed");
        } catch (SecurityException se) {
            if (se.getMessage().indexOf("No such file or directory") > 0) {
                System.out.println("test 1 succeeded");
            } else {
                System.out.println("test 1 failed");
                throw se;
            }
        }

        try {
            System.setProperty("java.security.auth.login.config",
                                "=file:/nofile");
            ConfigFile config = new ConfigFile();
            throw new SecurityException("test 2 failed");
        } catch (SecurityException se) {
            if (se.getMessage().indexOf("No such file or directory") > 0) {
                System.out.println("test 2 succeeded");
            }
        }

        System.setProperty("java.security.auth.login.config",
                                "=file:${test.src}/InconsistentError.config");
        ConfigFile config = new ConfigFile();

        System.out.println("test succeeded");

    }
}
