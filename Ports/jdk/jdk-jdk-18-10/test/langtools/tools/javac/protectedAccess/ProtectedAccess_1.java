/*
 * Copyright (c) 1999, 2002, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4249096
 * @summary Verify that special access checks for inherited protected members
 * are not applied to static members, per specification revision.
 * @author maddox
 *
 * @compile ProtectedAccess_1.java
 */

/*
 * Previously, javac handled the JLS 6.6.2 access check for
 * inherited static protected members differently when accessed
 * via a static reference or via an instance.
 * The current specification draft now drops all 6.6.2 checks
 * for static members.
 */

import p.SuperClass;

class ProtectedAccess_1 extends SuperClass {
    {
        SuperClass.i = 5;        // OK
        new SuperClass().i = 5;  // OK
    }
}
