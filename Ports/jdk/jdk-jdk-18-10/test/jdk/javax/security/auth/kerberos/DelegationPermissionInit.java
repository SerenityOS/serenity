/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import javax.security.auth.kerberos.DelegationPermission;
import java.util.List;

/*
 * @test
 * @bug 8231196 8234267
 * @summary DelegationPermission input check
 */
public class DelegationPermissionInit {
    public static void main(String[] args) throws Exception {
        var goodOnes = List.of(
                "\"aaa\" \"bbb\"",
                "\"aaa\"  \"bbb\""
        );
        var badOnes = List.of(
                "\"user@REALM\"",
                "\"\"\" \"bbb\"",
                "\"aaa\" \"\"\"",
                "\"\"\" \"\"\"",
                "\"aaa\" \"bbb",
                "\"\"aaa\"\" \"\"bbb\"\"",
                "\"aaa\" \"bbb\"\"\"",
                "\"aaa\"-\"bbb\"",
                "\"aaa\" - \"bbb\"",
                "\"aaa\"- \"bbb\"",
                "\"aaa\" \"bbb\"  ",
                "aaa\" \"bbb\"  "
        );
        boolean failed = false;
        for (var good : goodOnes) {
            System.out.println(">>> " + good);
            try {
                new DelegationPermission(good);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Failed");
                failed = true;
            }
        }
        for (var bad : badOnes) {
            System.out.println(">>> " + bad);
            try {
                new DelegationPermission(bad);
                System.out.println("Failed");
                failed = true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace(System.out);
            } catch (Exception e) {
                e.printStackTrace(System.out);
                System.out.println("Failed");
                failed = true;
            }
        }
        if (failed) {
            throw new Exception("Failed");
        }
    }
}
