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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/general_functions/GF01/gf01t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test exercises the JVMTI target area "General Functions".
 *     It implements the following scenario:
 *         Check that Hotspot provides "highly recommended" properties with
 *         GetSystemProperties, GetSystemProperty in the OnLoad and Live phases:
 *              java.vm.vendor
 *              java.vm.version
 *              java.vm.name
 *              java.vm.info
 *              java.library.path
 *              java.class.path
 *     The tested functions are verified thrice, in particular, inside
 *     Agent_OnLoad() (i.e. during the OnLoad phase), VMInit callback and
 *     VMDeath callback (the Live phase). All the highly recommended
 *     properties mentioned above must be found.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:gf01t001=-waittime=5
 *      nsk.jvmti.scenarios.general_functions.GF01.gf01t001
 */

