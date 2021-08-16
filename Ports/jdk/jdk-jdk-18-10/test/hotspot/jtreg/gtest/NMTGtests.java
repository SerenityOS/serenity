/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021 SAP SE. All rights reserved.
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

/*
 * This tests NMT by running gtests with NMT enabled.
 *
 * To save time, we just run them for debug builds (where we would catch assertions) and only a selection of tests
 * (namely, NMT tests themselves, and - for the detail statistics - os tests, since those reserve a lot and stress NMT)
 */

/* @test id=nmt-summary
 * @summary Run NMT-related gtests with summary statistics
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.xml
 * @requires vm.debug
 * @run main/native GTestWrapper --gtest_filter=NMT* -XX:NativeMemoryTracking=summary
 */

/* @test id=nmt-detail
 * @summary Run NMT-related gtests with detailed statistics
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.xml
 * @requires vm.debug
 * @run main/native GTestWrapper --gtest_filter=NMT*:os* -XX:NativeMemoryTracking=detail
 */
