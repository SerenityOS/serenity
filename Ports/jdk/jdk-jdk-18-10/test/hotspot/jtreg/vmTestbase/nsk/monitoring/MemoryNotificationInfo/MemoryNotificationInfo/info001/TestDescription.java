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
 * @summary converted from VM Testbase nsk/monitoring/MemoryNotificationInfo/MemoryNotificationInfo/info001.
 * VM Testbase keywords: [quick, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that constructor
 *         MemoryNotificationInfo()
 *     does not throw any exception for various sets of arguments:
 *     1. correct set of arguments;
 *     2. empty pool name;
 *     3. negative count;
 *     4. zero count;
 *     5. Long.MAX_VALUE as count;
 *     6. Long.MIN_VALUE as count.
 *     NullpointerException is expected for the following sets of arguments:
 *     1. null pool name;
 *     2. null MemoryUsage;
 * COMMENT
 *     Fixed the bug:
 *     5013995 null pointer exception in MM nsk test todata001.
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm nsk.monitoring.MemoryNotificationInfo.MemoryNotificationInfo.info001
 */

