/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

import java.lang.Package;

public class PackageSealingTest {
    public static void main(String args[]) {
        if (args.length != 4) {
            throw new RuntimeException("Expecting 4 arguments");
        }
        try {
            Class c1 = PackageSealingTest.class.forName(args[0].replace('/', '.'));
            Class c2 = PackageSealingTest.class.forName(args[2].replace('/', '.'));
            Package p1 = c1.getPackage();
            System.out.println("Package 1: " + p1.toString());
            Package p2 = c2.getPackage();
            System.out.println("Package 2: " + p2.toString());

            if (args[1].equals("sealed") && !p1.isSealed()) {
                System.out.println("Failed: " + p1.toString() + " is not sealed.");
                System.exit(0);
            }

            if (args[3].equals("notSealed") && p2.isSealed()) {
                System.out.println("Failed: " + p2.toString() + " is sealed.");
                System.exit(0);
            }

            System.out.println("OK");
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
