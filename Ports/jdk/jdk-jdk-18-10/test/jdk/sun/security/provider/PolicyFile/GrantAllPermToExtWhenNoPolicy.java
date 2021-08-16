/*
 * Copyright (c) 1999, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4231980 4233913
 * @summary Make sure that when no system policy and user policy files exist,
 * the built-in default policy will be used, which - amongst other things -
 * grants standard extensions the AllPermission. This regression is for 2
 * bugs (listed above). This test is ignored for now, because it temporarily
 * removes the system policy file, which is not a safe thing to do (may
 * affect other tests that are run concurrently).
 *
 * @build SomeExtensionClass
 * @build GrantAllPermToExtWhenNoPolicy
 * @run shell/manual GrantAllPermToExtWhenNoPolicy.sh
 */

import java.security.*;

public class GrantAllPermToExtWhenNoPolicy {

    public static void main(String[] args) throws Exception {

        SomeExtensionClass sec = new SomeExtensionClass();
        try {
            sec.getUserName();
        } catch (AccessControlException ace) {
            throw new Exception("Cannot read user name");
        }
    }
}
