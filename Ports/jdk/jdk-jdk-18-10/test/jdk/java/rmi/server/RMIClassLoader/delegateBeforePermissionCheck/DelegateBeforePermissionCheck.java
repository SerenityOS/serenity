/*
 * Copyright (c) 1999, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4191926 4349670
 * @summary When the RMIClassLoader.loadClass() methods are invoked with a
 * codebase URL that the caller does not have permission to load from, but
 * with a class name that is accessible through the caller's context class
 * loader (such as in the boot or system class paths, for an application),
 * the operations should succeed, instead of throwing a
 * ClassNotFoundException (wrapping a SecurityExcpetion) because the caller
 * does not have permission to access the codebase URL.
 * @author Peter Jones
 *
 * @library ../../../testlibrary
 * @modules java.rmi/sun.rmi.registry
 *          java.rmi/sun.rmi.server
 *          java.rmi/sun.rmi.transport
 *          java.rmi/sun.rmi.transport.tcp
 * @build TestLibrary Foo
 * @run main/othervm -Djava.security.manager=allow DelegateBeforePermissionCheck
 */

import java.net.*;
import java.rmi.*;
import java.rmi.server.*;

public class DelegateBeforePermissionCheck {

    private final static String tabooCodebase = "http://taboo/codebase/";

    public static void main(String[] args) {

        System.err.println("\nRegression test for bug 4191926\n");

        TestLibrary.suggestSecurityManager(null);

        try {
            String localClassName = Foo.class.getName();
            System.err.println("Attempting to load local class \"" +
                localClassName + "\" from codebase " + tabooCodebase);
            Class cl = RMIClassLoader.loadClass(
                tabooCodebase, localClassName);
            System.err.println("TEST PASSED: loaded " + cl + " locally");

        } catch (Exception e) {
            TestLibrary.bomb(e);
        }
    }
}
