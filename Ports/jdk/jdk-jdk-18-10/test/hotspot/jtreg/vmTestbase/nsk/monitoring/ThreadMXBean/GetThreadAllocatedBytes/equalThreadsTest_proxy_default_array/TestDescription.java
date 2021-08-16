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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/monitoring/ThreadMXBean/GetThreadAllocatedBytes/equalThreadsTest_proxy_default_array.
 * VM Testbase keywords: [quick, monitoring, feature_memory_alloc]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test executes EqualThreadsTest
 *     For more info please refer to EqualThreadsTest.README
 *     Test configuration:
 *     Access to management metrics via default MBean server proxy
 *     Allocation objects are primitive arrays (IntArrayProducer GarbageProducer)
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.monitoring.ThreadMXBean.GetThreadAllocatedBytes.EqualThreadsTest
 *      -testMode=proxy
 *      -MBeanServer=default
 *      -gp intArr
 */

