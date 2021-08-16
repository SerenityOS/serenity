/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8047771
 * @summary check permissions and principals from various modules
 * @modules java.desktop
 *          java.logging
 *          java.management
 *          java.security.jgss
 *          java.smartcardio
 *          java.sql
 *          java.xml
 *          jdk.attach
 *          jdk.jdi
 *          jdk.net
 *          jdk.security.auth
 *          jdk.security.jgss
 * @run main/othervm/java.security.policy==modules.policy Modules
 */

import java.security.AccessController;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;

public class Modules {

    private final static Permission[] perms = new Permission[] {
        // java.base module
        new java.io.SerializablePermission("enableSubstitution"),
        new java.lang.reflect.ReflectPermission("suppressAccessChecks"),
        new java.nio.file.LinkPermission("hard"),
        new javax.net.ssl.SSLPermission("getSSLSessionContext"),
        new javax.security.auth.AuthPermission("doAsPrivileged"),
        new javax.security.auth.PrivateCredentialPermission("* * \"*\"",
                                                            "read"),
        // java.desktop module
        new java.awt.AWTPermission("createRobot"),
        new javax.sound.sampled.AudioPermission("play"),
        // java.logging module
        new java.util.logging.LoggingPermission("control", ""),
        // java.management module
        new java.lang.management.ManagementPermission("control"),
        new javax.management.MBeanPermission("*", "getAttribute"),
        new javax.management.MBeanServerPermission("createMBeanServer"),
        new javax.management.MBeanTrustPermission("register"),
        new javax.management.remote.SubjectDelegationPermission("*"),
        // java.security.jgss module
        new javax.security.auth.kerberos.DelegationPermission("\"*\" \"*\""),
        new javax.security.auth.kerberos.ServicePermission("*", "accept"),
        // java.sql module
        new java.sql.SQLPermission("setLog"),
        // java.smartcardio module
        new javax.smartcardio.CardPermission("*", "*"),
        // jdk.attach module (@jdk.Exported Permissions)
        new com.sun.tools.attach.AttachPermission("attachVirtualMachine"),
        // jdk.jdi module (@jdk.Exported Permissions)
        new com.sun.jdi.JDIPermission("virtualMachineManager"),
        // jdk.security.jgss module (@jdk.Exported Permissions)
        new com.sun.security.jgss.InquireSecContextPermission("*"),
    };

    private final static Principal[] princs = new Principal[] {
        // java.base module
        new javax.security.auth.x500.X500Principal("CN=Duke"),
        // java.management module
        new javax.management.remote.JMXPrincipal("Duke"),
        // java.security.jgss module
        new javax.security.auth.kerberos.KerberosPrincipal("duke@openjdk.org"),
        new com.sun.security.auth.UserPrincipal("Duke"),
        new com.sun.security.auth.NTDomainPrincipal("openjdk.org"),
        new com.sun.security.auth.NTSid(
            "S-1-5-21-3623811015-3361044348-30300820-1013"),
        new com.sun.security.auth.NTUserPrincipal("Duke"),
        new com.sun.security.auth.UnixNumericUserPrincipal("0"),
        new com.sun.security.auth.UnixPrincipal("duke"),
    };

    public static void main(String[] args) throws Exception {

        for (Permission perm : perms) {
            AccessController.checkPermission(perm);
        }

        Permission princPerm = new java.util.PropertyPermission("user.home",
                                                                "read");
        Set<Principal> princSet = new HashSet<>(Arrays.asList(princs));
        Subject subject = new Subject(true, princSet, Collections.emptySet(),
                                      Collections.emptySet());
        PrivilegedAction<Void> pa = () -> {
            AccessController.checkPermission(princPerm);
            return null;
        };
        Subject.doAsPrivileged(subject, pa, null);
    }
}
