/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.Permission;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LoggingPermission;



/**
 * @test
 * @bug 8026499
 * @summary checks that Logger.getLogger("").setLevel() is working correctly.
 * @modules java.base/sun.util.logging
 *          java.logging
 * @build TestRootLoggerLevel
 * @run main/othervm -Djava.security.manager=allow -Dtest.security=on TestRootLoggerLevel
 * @run main/othervm -Dtest.security=off TestRootLoggerLevel
 * @author danielfuchs
 */
public class TestRootLoggerLevel {

    public static enum Loggers {
        ROOT("", "root"),
        GLOBAL(Logger.GLOBAL_LOGGER_NAME);

        private final String name;
        private final String displayName;
        Loggers(String name) {
            this(name, name);
        }
        Loggers(String name, String displayName) {
            this.name = name;
            this.displayName = displayName;
        }
    }

    public static void main(String[] args) throws Exception {

        for (Loggers log : Loggers.values()) {
            System.out.println(log.displayName + ": "
                    + Logger.getLogger(log.name)
                    + ": " + Logger.getLogger(log.name).getLevel());
        }

        if ("on".equals(System.getProperty("test.security","on"))) {
            System.out.println("*** SecurityManager is ON");
            Policy.setPolicy(new SimplePolicy());

            System.setSecurityManager(new SecurityManager());
        } else {
            System.out.println("*** SecurityManager is OFF");
        }

        // Before the fix, setting the root logger level here had only
        // a transient effect...
        for (Loggers log : Loggers.values()) {
            System.out.println("Logger.getLogger(\""
                    + log.name
                    + "\").setLevel(Level.FINEST);");
            Logger.getLogger(log.name).setLevel(Level.FINEST);
            System.out.println(log.displayName + ": "
                    + Logger.getLogger(log.name)
                    + ": " + Logger.getLogger(log.name).getLevel());

            // At this point - the root logger should have a level which is
            // FINEST - just check it here for sanity.
            if (Logger.getLogger(log.name).getLevel() != Level.FINEST) {
                throw new RuntimeException("Logger " + log.displayName
                        + " level should be FINEST: "
                        + Logger.getLogger(log.name).getLevel());
            }
        }
        // Initializing some PlatformLogger caused the root logger
        // level to be reset to INFO.
        //
        final Object platformLogger =
                sun.util.logging.PlatformLogger.getLogger("java.foo");
        System.out.println("Got platform logger: " + platformLogger);

        for (Loggers log : Loggers.values()) {
            // Check that the root logger still has the FINEST value assigned
            // to it earlier.
            System.out.println(log.displayName + ": "
                    + Logger.getLogger(log.name)
                    + ": " + Logger.getLogger(log.name).getLevel());

            if (Logger.getLogger(log.name).getLevel() != Level.FINEST) {
                throw new RuntimeException("Logger " + log.displayName
                        + " level should be FINEST: "
                        + Logger.getLogger(log.name).getLevel());
            }
        }

     }

    private static final class SimplePolicy extends Policy {

        static final Policy DEFAULT_POLICY = Policy.getPolicy();

        private final Permissions perms;

        private static final Permissions permissions(Permission... perms) {
            Permissions permissions = new Permissions();
            for (Permission perm : perms) {
                permissions.add(perm);
            }
            return permissions;
        }

        SimplePolicy() {
            this(permissions(new LoggingPermission("control", null),
                    new RuntimePermission("accessClassInPackage.sun.util.logging")
            ));
        }

        SimplePolicy(Permissions perms) {
            this.perms = perms;
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            return perms.implies(permission) || DEFAULT_POLICY.implies(domain, permission);
        }

    }

}
