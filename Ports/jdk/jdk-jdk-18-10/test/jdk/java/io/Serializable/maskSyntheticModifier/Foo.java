/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4897937
 * @summary Verify that the presence of the JVM_ACC_SYNTHETIC bit in the
 *          modifiers of fields and methods does not affect default
 *          serialVersionUID calculation.
 */

/*
 * This file is compiled with JDK 1.4.2 in order to obtain the Foo.class
 * "golden file" used in the test, which contains a synthetic field and method
 * for implementing the class literal reference.  This file is not itself used
 * directly by the test, but is kept here for reference.
 */
public class Foo implements java.io.Serializable {
    Foo() {
        Class cl = Integer.class;
    }
}
