/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 7076310
 * @summary Test AclEntry.Builder setFlags and setPermissions with empty sets
 */

import java.nio.file.attribute.*;
import java.util.*;

/*
 * Test for bug 7076310 "(file) AclEntry.Builder setFlags throws
 * IllegalArgumentException if set argument is empty"
 * The bug is only applies when the given Set is NOT an instance of EnumSet.
 *
 * The setPermissions method also has the same problem.
 */
public class EmptySet {
    public static void main(String[] args) {
        Set<AclEntryFlag> flags = new HashSet<>();
        AclEntry.newBuilder().setFlags(flags);

        Set<AclEntryPermission> perms = new HashSet<>();
        AclEntry.newBuilder().setPermissions(perms);
    }
}
