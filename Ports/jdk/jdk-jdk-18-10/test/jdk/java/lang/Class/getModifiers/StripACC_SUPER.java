/*
 * Copyright (c) 1998, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4109635
   @summary VM adds ACC_SUPER bit to access flags of a class. This must
            be stripped by the Class.getModifiers method, or else this
            shows up as though the class is synchronized and that doesn't
            make any sense.
   @author Anand Palaniswamy
 */
public class StripACC_SUPER {
    public static void main(String[] args) throws Exception {
        int access = StripACC_SUPER.class.getModifiers();
        if (java.lang.reflect.Modifier.isSynchronized(access))
            throw new Exception("ACC_SUPER bit is not being stripped");
    }
}
