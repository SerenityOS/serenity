/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/hotswap/HS101/hs101t007.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, redefine, feature_hotswap]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is for HS101 scenario of "HotSwap class file replacement".
 *     Periodically hotswap class(es) with a changed version in synchronous
 *     manner within incoming Exception events. The VM is provoked to work
 *     in compiled mode by a tight loop in a test.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.scenarios.hotswap.HS101.hs101t007
 *        nsk.share.jvmti.ProfileCollector
 * @run main/othervm/native
 *      -agentlib:HotSwap=-waittime=5,package=nsk,sync=1000,mode=compiled,bci=alloc
 *      nsk.jvmti.scenarios.hotswap.HS101.hs101t007
 */

