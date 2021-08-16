/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.security.*;
import java.util.PropertyPermission;
import java.net.SocketPermission;
import java.lang.*;

public class DynamicPolicy extends Policy{

    static final Policy DEFAULT_POLICY = Policy.getPolicy(); // do this early before setPolicy is called
    static int refresher = 0;


    public DynamicPolicy() {
    }

    public PermissionCollection getPermissions(CodeSource cs) {

        Permissions perms = new Permissions();
        initStaticPolicy(perms);
        // Defalut policy in the beginning...
        // toggle from refresh to refresh
        if (refresher == 1)
            perms.add(new PropertyPermission("user.name","read"));

        System.err.println("perms=[" + perms + "]");
        return perms;
    }

    public boolean implies(ProtectionDomain pd, Permission p) {
        return getPermissions(pd).implies(p) || DEFAULT_POLICY.implies(pd, p);
    }

    public PermissionCollection getPermissions(ProtectionDomain pd) {

        Permissions perms = new Permissions();
        initStaticPolicy(perms);
        // Defalut policy in the beginning...
        // toggle from refresh to refresh
        if (refresher == 1)
            perms.add(new PropertyPermission("user.name","read"));

        return perms;
    }

    public void refresh() {
        refresher++;
    }

    private void initStaticPolicy(PermissionCollection perms) {

        perms.add(new java.security.SecurityPermission("getPolicy"));
        perms.add(new java.security.SecurityPermission("setPolicy"));
        perms.add(new java.lang.RuntimePermission("stopThread"));
        perms.add(new java.net.SocketPermission("localhost:1024-", "listen"));
        perms.add(new PropertyPermission("java.version","read"));
        perms.add(new PropertyPermission("java.vendor","read"));
        perms.add(new PropertyPermission("java.vendor.url","read"));
        perms.add(new PropertyPermission("java.class.version","read"));
        perms.add(new PropertyPermission("os.name","read"));
        perms.add(new PropertyPermission("os.version","read"));
        perms.add(new PropertyPermission("os.arch","read"));
        perms.add(new PropertyPermission("file.separator","read"));
        perms.add(new PropertyPermission("path.separator","read"));
        perms.add(new PropertyPermission("line.separator","read"));
        perms.add(new PropertyPermission("java.specification.version", "read"));
        perms.add(new PropertyPermission("java.specification.vendor", "read"));
        perms.add(new PropertyPermission("java.specification.name", "read"));
        perms.add(new PropertyPermission("java.vm.specification.version", "read"));
        perms.add(new PropertyPermission("java.vm.specification.vendor", "read"));
        perms.add(new PropertyPermission("java.vm.specification.name", "read"));
        perms.add(new PropertyPermission("java.vm.version", "read"));
        perms.add(new PropertyPermission("java.vm.vendor", "read"));
        perms.add(new PropertyPermission("java.vm.name", "read"));
        return;
    }
}
