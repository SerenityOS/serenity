/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package util;

import java.io.FilePermission;
import java.lang.reflect.ReflectPermission;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.SecurityPermission;
import java.sql.SQLPermission;
import java.util.Enumeration;
import java.util.PropertyPermission;
import java.util.StringJoiner;
import java.util.logging.LoggingPermission;

/*
 * Simple Policy class that supports the required Permissions to validate the
 * JDBC concrete classes
 */
public class TestPolicy extends Policy {
    static final Policy DEFAULT_POLICY = Policy.getPolicy();

    final PermissionCollection permissions = new Permissions();

    /**
     * Constructor which sets the minimum permissions allowing testNG to work
     * with a SecurityManager
     */
    public TestPolicy() {
        setMinimalPermissions();
    }

    /*
     * Constructor which determines which permissions are defined for this
     * Policy used by the JDBC tests Possible values are: all (ALLPermissions),
     * setLog (SQLPemission("setLog"), deregisterDriver
     * (SQLPermission("deregisterDriver") (SQLPermission("deregisterDriver"),
     * setSyncFactory(SQLPermission(setSyncFactory), and also
     * LoggerPermission("control", null) when setting a Level
     *
     * @param policy Permissions to set
     */
    public TestPolicy(String policy) {

        switch (policy) {
            case "all":
                permissions.add(new AllPermission());
                break;
            case "setLog":
                setMinimalPermissions();
                permissions.add(new SQLPermission("setLog"));
                break;
            case "deregisterDriver":
                setMinimalPermissions();
                permissions.add(new SQLPermission("deregisterDriver"));
                break;
            case "setSyncFactory":
                setMinimalPermissions();
                permissions.add(new SQLPermission("setSyncFactory"));
                break;
            case "setSyncFactoryLogger":
                setMinimalPermissions();
                permissions.add(new SQLPermission("setSyncFactory"));
                permissions.add(new LoggingPermission("control", null));
                break;
            default:
                setMinimalPermissions();
        }
    }

    /*
     * Defines the minimal permissions required by testNG when running these
     * tests
     */
    private void setMinimalPermissions() {
        permissions.add(new SecurityPermission("getPolicy"));
        permissions.add(new SecurityPermission("setPolicy"));
        permissions.add(new RuntimePermission("getClassLoader"));
        permissions.add(new RuntimePermission("setSecurityManager"));
        permissions.add(new RuntimePermission("createSecurityManager"));
        permissions.add(new PropertyPermission("line.separator", "read"));
        permissions.add(new PropertyPermission("fileStringBuffer", "read"));
        permissions.add(new PropertyPermission("dataproviderthreadcount", "read"));
        permissions.add(new PropertyPermission("java.io.tmpdir", "read"));
        permissions.add(new PropertyPermission("testng.show.stack.frames",
                "read"));
        permissions.add(new PropertyPermission("testng.thread.affinity", "read"));
        permissions.add(new PropertyPermission("testng.memory.friendly", "read"));
        permissions.add(new PropertyPermission("testng.mode.dryrun", "read"));
        permissions.add(new PropertyPermission("testng.report.xml.name", "read"));
        permissions.add(new PropertyPermission("testng.timezone", "read"));
        permissions.add(new ReflectPermission("suppressAccessChecks"));
        permissions.add(new FilePermission("<<ALL FILES>>",
                "read, write, delete"));
    }

    /*
     * Overloaded methods from the Policy class
     */
    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner("\n", "policy: ", "");
        Enumeration<Permission> perms = permissions.elements();
        while (perms.hasMoreElements()) {
            sj.add(perms.nextElement().toString());
        }
        return sj.toString();

    }

    @Override
    public PermissionCollection getPermissions(ProtectionDomain domain) {
        return permissions;
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return permissions;
    }

    @Override
    public boolean implies(ProtectionDomain domain, Permission perm) {
        return permissions.implies(perm) || DEFAULT_POLICY.implies(domain, perm);
    }
}
