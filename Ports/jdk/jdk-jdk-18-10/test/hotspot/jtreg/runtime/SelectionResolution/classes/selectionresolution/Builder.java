/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

package selectionresolution;

import java.util.HashMap;

abstract class Builder {
    protected final SelectionResolutionTestCase testcase;
    protected final HierarchyShape hier;
    protected final HashMap<Integer,ClassData> classdata;

    public Builder(SelectionResolutionTestCase testcase) {
        this.testcase = testcase;
        this.hier = testcase.hier;
        this.classdata = testcase.classdata;
    }

    protected String getName(int id) {
        StringBuilder name = new StringBuilder();

        name.append(getPackageName(classdata.get(id).packageId.ordinal()));

        // Name classes C<id> and interfaces I<id>
        name.append(getClassName(id));

        return name.toString();
    }

    protected String getPackageName(int packageId) {
        return "P" + packageId + "/";
    }

    protected String getClassName(int id) {
        // Name classes C<id> and interfaces I<id>
        if (isClass(id)) {
            return "C" + id;
        } else {
            return "I" + id;
        }
    }

    protected boolean isClass(int id) {
        return hier.isClass(id);
    }
}
