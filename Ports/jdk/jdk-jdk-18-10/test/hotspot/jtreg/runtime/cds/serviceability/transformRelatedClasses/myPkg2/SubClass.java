/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package myPkg2;

import myPkg1.SuperClazz;

public class SubClass extends SuperClazz {
    public static void main(String[] args) {
        System.out.println("SubClass: entering main()");
        test();
    }

    public static void test() {
        // The line below will be used to check for successful class transformation
        System.out.println("child-transform-check: this-should-be-transformed");
        (new SubClass()).callParent();

        // Get the system packages, which should contain myPkg1 and myPkag2
        Package[] pkgs = Package.getPackages();
        for (int i = 0; i < pkgs.length; i++) {
            if (pkgs[i].getName().equals("myPkg1")) {
                for (int j = 0; j < pkgs.length; j++) {
                    if (pkgs[j].getName().equals("myPkg2")) {
                        return; // found myPkg1 & myPkg1
                    }
                }
            }
        }
        throw new RuntimeException("Missing system package");
    }

    private void callParent() {
        super.testParent();
    }
}
