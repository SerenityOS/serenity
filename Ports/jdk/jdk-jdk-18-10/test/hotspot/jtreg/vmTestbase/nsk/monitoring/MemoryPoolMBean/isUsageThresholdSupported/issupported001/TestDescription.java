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
 * @summary converted from VM Testbase nsk/monitoring/MemoryPoolMBean/isUsageThresholdSupported/issupported001.
 * VM Testbase keywords: [quick, monitoring]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that
 *         MemoryPoolMBean.isUsageThresholdSupported()
 *     returns correct results.
 *     The test checks that for each memory pool
 *         1. isUsageThresholdSupported() returns true and getUsageThreshold()
 *            returns not -1 value, or
 *         2. isUsageThresholdSupported() returns false and getUsageThreshold()
 *            returns -1 value
 *     Otherwise, the test fails.
 *     The test implements direct access to the metrics.
 * COMMENT
 *     Fixed the bug
 *     4989235 TEST: The spec is updated accoring to 4982289, 4985742
 *     Adjusted the bug to Tiger-b40.
 *     Fixed the bug:
 *     5035038 Chain of JMX exceptions impact on monitoring tests
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm nsk.monitoring.MemoryPoolMBean.isUsageThresholdSupported.issupported001
 */

