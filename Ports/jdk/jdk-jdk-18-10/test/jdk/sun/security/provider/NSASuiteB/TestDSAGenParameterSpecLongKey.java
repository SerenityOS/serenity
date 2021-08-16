/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8075286 8163498
 * @summary Verify that DSAGenParameterSpec can and can only be used to generate
 *          DSA within some certain range of key sizes as described in the class
 *          specification (L, N) as (1024, 160), (2048, 224), (2048, 256) and
 *          (3072, 256) should be OK for DSAGenParameterSpec.
 *          This test has been split based on lower/higher key sizes in order to
 *          reduce individual execution times and run in parallel
 *          (see TestDSAGenParameterSpec.java)
 * @run main/timeout=700 TestDSAGenParameterSpec 3072 256 true
 * @run main TestDSAGenParameterSpec 4096 256
 */