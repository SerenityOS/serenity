/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jdi.ClassUnloadRequest.addClassExclusionFilter;

// These classes will be loaded by ClassUnloader

class Superexclfilter001b {
    static String name = "Superexclfilter001b";

}

class Subexclfilter0011 extends Superexclfilter001b {
    static String name = "Subexclfilter0011";

}

class Subexclfilter0021 extends Superexclfilter001b {
    static String name = "Subexclfilter0021";

}

class Subexclfilter0031 extends Superexclfilter001b {
    static String name = "Subexclfilter0031";

}

class Superexclfilter002b {
    static String name = "Superexclfilter002b";

}

class Subexclfilter0012 extends Superexclfilter002b {
    static String name = "Subexclfilter0012";

}

class Subexclfilter0022 extends Superexclfilter002b {
    static String name = "Subexclfilter0022";

}

class Subexclfilter0032 extends Superexclfilter002b {
    static String name = "Subexclfilter0032";

}
