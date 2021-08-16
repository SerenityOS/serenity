/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6569218
 * @summary  Specification of some BasicPermission method does not fit with implementation
 */

import java.security.BasicPermission;

public class ExitVMEquals {
    public static void main(String[] args) throws Exception {
        BasicPermission bp1 = new BP("exitVM");
        BasicPermission bp2 = new BP("exitVM.*");

        StringBuffer sb = new StringBuffer();

        // First, make sure the old restrictions on exitVM and exitVM.* still hold.
        if (!bp1.implies(bp2)) sb.append("bp1 does not implies bp2\n");
        if (!bp2.implies(bp1)) sb.append("bp2 does not implies bp1\n");

        // Test against hashCode spec
        if (bp1.hashCode() != bp1.getName().hashCode())
            sb.append("bp1 hashCode not spec consistent\n");
        if (bp2.hashCode() != bp2.getName().hashCode())
            sb.append("bp2 hashCode not spec consistent\n");

        // Test against equals spec
        if (bp1.getName().equals(bp2.getName())) {
            if (!bp1.equals(bp2)) {
                sb.append("BP breaks equals spec\n");
            }
        }
        if (!bp1.getName().equals(bp2.getName())) {
            if (bp1.equals(bp2)) {
                sb.append("BP breaks equals spec in another way\n");
            }
        }

        // Tests against common knowledge: If equals, then hashCode should be same
        if (bp1.equals(bp2)) {
            if (bp1.hashCode() != bp2.hashCode()) {
                sb.append("Equal objects have unequal hashCode?\n");
            }
        }

        if (sb.length() > 0) {
            throw new Exception(sb.toString());
        }
    }
}

class BP extends BasicPermission {
    BP(String name) {
        super(name);
    }
}
