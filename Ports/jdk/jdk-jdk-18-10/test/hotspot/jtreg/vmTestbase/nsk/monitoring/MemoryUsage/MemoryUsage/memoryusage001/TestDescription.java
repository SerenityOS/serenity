/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase nsk/monitoring/MemoryUsage/MemoryUsage/memoryusage001.
 * VM Testbase keywords: [quick, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that
 *         MemoryUsage(long init, long used, long committed, long max)
 *     correctly throws IllegalArgumentException, if:
 *         1. init is negative, but not -1
 *         2. max is negative, but not -1
 *         3. used is negative
 *         4. committed is negative
 *         5. used is greater than committed
 *         6. used is greater than max, while max is not -1
 *     The method also checks that the constructor does not throw
 *     IllegalArgumentException, if:
 *         1. init is -1
 *         2. max is -1
 *         3. used is equal to max
 *         4. used is less than max
 *         5. committed is less than init
 *         6. max is less than committed
 * COMMENT
 *     Fixed the bug:
 *     5050603 memoryusage001 needs to be updated for MemoryUsage
 *             spec change.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm nsk.monitoring.MemoryUsage.MemoryUsage.memoryusage001
 */

