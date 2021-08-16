/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
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

public class GetInstance {
    public static void main(String[] args) {
        SecurityManager sm = System.getSecurityManager();
        try {
            sm.checkPermission(new NoArgPermission());
            sm.checkPermission(new OneArgPermission("one"));
            sm.checkPermission(new TwoArgPermission("two", "two"));
            sm.checkPermission(new TwoArgNullActionsPermission
                                                ("two",
                                                (String)null));
        } catch (Exception e) {
            System.out.println("Test Failed");
            e.printStackTrace();
            throw new SecurityException("Test Failed");
        }
    }
}
