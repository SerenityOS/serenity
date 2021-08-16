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
 * @key stress
 *
 * @summary converted from VM Testbase nsk/monitoring/stress/thread/strace018.
 * VM Testbase keywords: [stress, monitoring, nonconcurrent, jdk_desktop]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test starts 50 recursive threads, switches them  to the various
 *     state after reaching 200 depth and checks up their stack traces
 *     and states gotten via the ThreadMXBean interface.
 *     Executable class of the test is the same as for the strace015 test.
 *     In contrast to the strace015 test, the strace018 test is performed for
 *     the case when recursive method is native and pure java by turn.
 *     Access to the management metrics is accomplished by calling via
 *     custom MBeanServer the methods in the MBean.
 * COMMENTS
 *     Reduced recursion depth value up to 100 because the test fails
 *     due to timeout in -Xint mode on solaris-sparc(Sun Ultra-10, 333 MHz, 256Mb)
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm
 *      nsk.monitoring.stress.thread.strace010
 *      -testMode=server
 *      -MBeanServer=custom
 *      -depth=100
 *      -threadCount=30
 *      -invocationType=mixed
 */

