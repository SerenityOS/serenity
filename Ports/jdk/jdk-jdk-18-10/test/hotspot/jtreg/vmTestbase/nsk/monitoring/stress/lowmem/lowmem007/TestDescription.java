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
 * @key stress randomness
 *
 * @summary converted from VM Testbase nsk/monitoring/stress/lowmem/lowmem007.
 * VM Testbase keywords: [stress, monitoring, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that enabling of low memory detection does not lead to
 *     unexpected behaviour: crashes of the VM and undocumented exceptions.
 *     The test enables low memory detection: it adds NotificationListener
 *     to the MemoryMBean for notification mechanism of monitoring, or starts
 *     a special thread for polling mechanism. After that, it starts eating memory.
 *     Objects are allocated, if "heap" memory is tested; classes are loaded,
 *     if "nonheap" memory is tested; objects are allocated and classes are
 *     loaded, if "mixed" memory is tested.
 *     Notifications are received by the listener in notification mechanism and
 *     crossing of thresholds are detected in polling mechanism. The thresholds
 *     are updated as soon as notification is received, or crossing of a threshold
 *     is detected.
 *     The test also checks stderr to be empty. Otherwise, it fails. The test
 *     exits as soon as OutOfMemoryError is caught (if heap memory is tested) or
 *     all classes are loaded (if nonheap or mixed memory is tested).
 *     All options of the test are specified in *.cfg file. this particular test
 *         - performs directly access to the MBeans' methods;
 *         - fills both heap and nonheap memory;
 *         - implements notifcation mechanism of monitoring;
 *         - tests usage thresholds.
 * COMMENTS
 *     Fixed the bug
 *     4969687 TEST_BUG: The spec is updated accoring to 4956978, 4957000, 4959889
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm -XX:-UseGCOverheadLimit nsk.monitoring.stress.lowmem.lowmem001 -memory=mixed
 */

