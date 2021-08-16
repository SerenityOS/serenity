/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

// This file is not executed directly; it just exists to contain the
// set of test descriptions

/*
 * @test
 * @bug 5047307
 * @summary javac -nowarn improperly suppresses JLS-mandated warnings
 * @compile/ref=Test1.out -XDrawDiagnostics A.java
 * @compile/ref=Test1.out -XDrawDiagnostics -nowarn A.java
 * @compile/ref=Test1.out -XDrawDiagnostics -Xmaxwarns 1 A.java
 * @compile/ref=Test2.out -XDrawDiagnostics A.java B.java
 * @compile/ref=Test2.out -XDrawDiagnostics -nowarn A.java B.java
 * @compile/ref=Test2.out -XDrawDiagnostics -Xmaxwarns 1 A.java B.java
 * @compile/ref=Test3.out -XDrawDiagnostics -Xlint:deprecation A.java
 * @compile/ref=Test3.out -XDrawDiagnostics -nowarn -Xlint:deprecation A.java
 * @compile/ref=Test3b.out -XDrawDiagnostics -nowarn -Xlint:deprecation -Xmaxwarns 1 A.java
 * @compile/ref=Test4.out -XDrawDiagnostics -Xlint:deprecation A.java B.java
 * @compile/ref=Test4.out -XDrawDiagnostics -nowarn -Xlint:deprecation A.java B.java
 * @compile/ref=Test4b.out -XDrawDiagnostics -nowarn -Xlint:deprecation -Xmaxwarns 1 A.java B.java
 * @compile/ref=Test4c.out -XDrawDiagnostics -nowarn -Xlint:deprecation -Xmaxwarns 2 A.java B.java
 * @compile/ref=Test4d.out -XDrawDiagnostics -nowarn -Xlint:deprecation -Xmaxwarns 3 A.java B.java
 * @compile/ref=Test5.out -XDrawDiagnostics -Xlint:deprecation  P.java Q.java
 * @compile/ref=Test5b.out -XDrawDiagnostics -Xlint:deprecation -Xmaxwarns 2 P.java Q.java
 */
