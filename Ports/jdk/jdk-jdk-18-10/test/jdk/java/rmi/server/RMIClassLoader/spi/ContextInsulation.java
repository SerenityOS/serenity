/*
 * Copyright (c) 2001, 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 4450891
 * @summary verify that the java.util.ServiceLoader-based location of an
 * RMIClassLoader provider does not require any permissions of the
 * (arbitrary) protection domains that happens to be on the stack
 * when RMIClassLoader is first used.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary ServiceConfiguration TestProvider TestProvider2
 * @run main/othervm/policy=security.policy ContextInsulation
 */

import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class ContextInsulation {
    public static void main(String[] args) throws Exception {

        /*
         * If we delay setting the security manager until after the service
         * configuration file has been installed, then this test still
         * functions properly, but the -Djava.security.debug output is
         * lacking, so to ease debugging, we'll set it early-- at the cost
         * of having to specify the policy even when running standalone.
         */
        TestLibrary.suggestSecurityManager(null);

        ServiceConfiguration.installServiceConfigurationFile();

        /*
         * Execute use of RMIClassLoader within an AccessControlContext
         * that has a protection domain with no permissions, to make sure
         * that RMIClassLoader can still properly initialize itself.
         */
        CodeSource codesource = new CodeSource(null, (Certificate[]) null);
        Permissions perms = null;
        ProtectionDomain pd = new ProtectionDomain(codesource, perms);
        AccessControlContext acc =
            new AccessControlContext(new ProtectionDomain[] { pd });

        java.security.AccessController.doPrivileged(
        new java.security.PrivilegedExceptionAction() {
            public Object run() throws Exception {
                TestProvider.exerciseTestProvider(
                    TestProvider2.loadClassReturn,
                    TestProvider2.loadProxyClassReturn,
                    TestProvider2.getClassLoaderReturn,
                    TestProvider2.getClassAnnotationReturn,
                    TestProvider2.invocations);
                return null;
            }
        }, acc);
    }
}
