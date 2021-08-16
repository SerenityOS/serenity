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
 * @test
 * @summary Test for spurious cyclic inheritance due to import
 * @author Todd Turnbridge
 *
 * @compile p1/B.java
 */

// The following javadoc comment, and its associated code, was removed
// recently from Enter.java.  This regression test ensures that the
// situation that the code was present to handle works properly.

/** A flag to enable/disable class completion. This is
 *      necessary to prevent false cyclic dependencies involving imports.
 *      Example (due to Todd Turnbridge): Consider the following three files:
 *
 *      A.java: public class A extends B {}
 *      B.java: public class B extends C {}
 *      C.java: import A; public class C {}
 *
 *      Now compile B.java. The (import A;) is not allowed to go beyond enter
 *      phase, or a false cycle will occur.
 */
// boolean completionEnabled = true;
