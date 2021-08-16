/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6372782 6292765
 * @summary possibly invalid "abstract method not implemented" emitted by compiler
 * @author  Alex Lam S.L.
 * @compile PlainPlanarVector.java
 *          PlanarVectorVariable.java
 *          PlainForm.java
 *          State.java
 *          VariableForm.java
 *          Ring.java
 *          Scalar.java
 *          AbstractPlanarVector.java
 *          AbstractVector.java
 *          AbstractElement.java
 *          Element.java
 *          Vector.java
 *          Value.java
 *          AdditiveClosure.java
 *          AdditionDefined.java
 *          MultiplicationDefined.java
 *          PlanarVector.java
 */
