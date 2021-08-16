/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6549506
 * @summary Specification of Permission.toString() method contradicts with
 *      JDK implementation
 */

import java.security.*;

public class ToString {

    public static void main(String[]args) throws Exception {
        DummyWritePermission dummyPerm = new DummyWritePermission();
        NullActionPermission nullActionPerm = new NullActionPermission();
        System.out.println(dummyPerm.toString());
        System.out.println(dummyPerm.getDescription());
        System.out.println(nullActionPerm.toString());
        System.out.println(nullActionPerm.getDescription());
        if (!dummyPerm.toString().equals(dummyPerm.getDescription())) {
            throw new Exception("The expected permission.toString() is " +
                dummyPerm.getDescription() + ", but " +
                dummyPerm.toString() + " returned!");
        }

        if (!nullActionPerm.toString().equals(nullActionPerm.getDescription())) {
            throw new Exception("The expected permission.toString() is " +
                nullActionPerm.getDescription() + ", but " +
                nullActionPerm.toString() + " returned!");
        }
    }

    private static abstract class SimplePermission extends Permission {
        public SimplePermission(String name) {
            super(name);
        }

        public boolean implies(Permission permission) {
            return false;
        }

        public boolean equals(Object obj) {
            return false;
        }

        public int hashCode() {
            return 13;
        }
    }

    private static class DummyWritePermission extends SimplePermission {
        public DummyWritePermission() {
            super("permit to");
        }

        public String getActions() {
            return "write";
        }

        public String getDescription() {
            return "(\"ToString$DummyWritePermission\" \"permit to\" \"write\")";
        }
    }

    private static class NullActionPermission extends SimplePermission {
        public NullActionPermission() {
            super("permit to");
        }

        public String getActions() {
            return null;
        }

        public String getDescription() {
            return "(\"ToString$NullActionPermission\" \"permit to\")";
        }
    }

}
