/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159752
 * @summary Test that default policy permissions are always granted
 * @run main/othervm DefaultPolicy
 */

import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.security.AllPermission;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.URIParameter;

public class DefaultPolicy {

    public static void main(String[] args) throws Exception {

        // Check policy with no java.security.policy property set
        Policy p = Policy.getPolicy();
        checkPolicy(p);

        // Check policy with java.security.policy '=' option
        System.setProperty("java.security.policy", "Extra.policy");
        p.refresh();
        checkPolicy(p);

        // Check policy with java.security.policy override '==' option
        System.setProperty("java.security.policy", "=Extra.policy");
        p.refresh();
        checkPolicy(p);

        // Check Policy.getInstance
        URI policyURI = Paths.get(System.getProperty("test.src"),
                                  "Extra.policy").toUri();
        p = Policy.getInstance("JavaPolicy", new URIParameter(policyURI));
        checkPolicy(p);
    }

    private static void checkPolicy(Policy p) throws Exception {
        // check if jdk.crypto.ec module has been de-privileged
        CodeSource cs =
            new CodeSource(new URL("jrt:/jdk.crypto.ec"), (CodeSigner[])null);
        ProtectionDomain pd = new ProtectionDomain(cs, null, null, null);
        if (p.implies(pd, new AllPermission())) {
            throw new Exception("module should not be granted AllPermission");
        }
        if (!p.implies(pd, new RuntimePermission("loadLibrary.sunec"))) {
            throw new Exception("module should be granted RuntimePermission");
        }
    }
}
