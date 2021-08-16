/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.util.EnumSet;

public enum Flag {
    ACC_PUBLIC(0x0001),
    ACC_PROTECTED(0x0004),
    ACC_PRIVATE(0x0002),
    ACC_INTERFACE(0x0200),
    ACC_ENUM(0x4000),
    ACC_ANNOTATION(0x2000),
    ACC_SUPER(0x0020),
    ACC_ABSTRACT(0x0400),
    ACC_VOLATILE(0x0040),
    ACC_TRANSIENT(0x0080),
    ACC_SYNTHETIC(0x1000),
    ACC_STATIC(0x0008),
    ACC_FINAL(0x0010),
    ACC_SYNCHRONIZED(0x0020),
    ACC_BRIDGE(0x0040),
    ACC_VARARGS(0x0080),
    ACC_NATIVE(0x0100),
    ACC_STRICT(0x0800);

    public int flag;

    Flag(int flag) {
        this.flag = flag;
    }

    static Flag[] parse(int flagsMask) {
        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        for (Flag f : Flag.values()) {
            if ((f.flag & flagsMask) != 0) {
                flags.add(f);
            }
        }
        return flags.stream().toArray(Flag[]::new);
    }
}
