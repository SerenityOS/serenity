/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase vm/mlvm/mixed/stress/java/findDeadlock.
 * VM Testbase keywords: [feature_mlvm, nonconcurrent, quarantine, monitoring]
 * VM Testbase comments: 8208278
 * VM Testbase readme:
 * DESCRIPTION
 *    The test does the following in a loop:
 *    1. Enters a deadlock involving methodhandles and invokedynamic target and bootstrap methods
 *       (deadlock is created using both java.util.concurrency.lock.ReentrantLock and
 *       synchronized() Java syntax)
 *    2. Finds that deadlock using ThreadMXBean
 *    3. Unlocks one thread to let others go
 *
 * @library /vmTestbase
 *          /test/lib
 *
 * @comment build test class and indify classes
 * @build vm.mlvm.mixed.stress.java.findDeadlock.INDIFY_Test
 * @run driver vm.mlvm.share.IndifiedClassesBuilder
 * @run main/othervm -Xlog:gc,safepoint vm.mlvm.mixed.stress.java.findDeadlock.INDIFY_Test
 *
 * To see code that takes more time to safepoint run with:
 * main/othervm -XX:+SafepointTimeout -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+AbortVMOnSafepointTimeout
 *                   -XX:SafepointTimeoutDelay=500
 *                   -XX:+PrintSystemDictionaryAtExit
 *                   -Xlog:gc,safepoint
 *                   vm.mlvm.mixed.stress.java.findDeadlock.INDIFY_Test
 */

