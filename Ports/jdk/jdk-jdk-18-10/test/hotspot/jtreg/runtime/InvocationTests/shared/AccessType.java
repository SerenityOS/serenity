/*
 * Copyright (c) 2009, 2019, Oracle and/or its affiliates. All rights reserved.
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

package shared;

import static jdk.internal.org.objectweb.asm.Opcodes.*;

public enum AccessType {
      PUBLIC           ("PUB")   { public int value() { return ACC_PUBLIC; } }
    , PROTECTED        ("PROT")  { public int value() { return ACC_PROTECTED; } }
    , PACKAGE_PRIVATE  ("PP")    { public int value() { return 0; } }
    , PRIVATE          ("PRIV")  { public int value() { return ACC_PRIVATE; } }
    , UNDEF            ("UNDEF") { public int value() { return -1; } }
    ;

    private String name;

    AccessType(String name) {
        this.name = name;
    }

    public abstract int value();

    public String toString() {
        return name;
    }
};
