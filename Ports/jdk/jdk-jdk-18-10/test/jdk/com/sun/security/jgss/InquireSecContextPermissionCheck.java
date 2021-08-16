/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6710360
 * @modules jdk.security.jgss
 * @summary export Kerberos session key to applications
 */

import com.sun.security.jgss.InquireSecContextPermission;

public class InquireSecContextPermissionCheck {

    public static void main(String[] args) throws Exception {

        InquireSecContextPermission p0, p1;
        p0 = new InquireSecContextPermission(
                "KRB5_GET_SESSION_KEY");
        p1 = new InquireSecContextPermission("*");

        if (!p1.implies(p0) || !p1.implies(p1) || !p0.implies(p0)) {
            throw new Exception("Check failed");
        }

        if (p0.implies(p1)) {
            throw new Exception("This is bad");
        }
    }
}

