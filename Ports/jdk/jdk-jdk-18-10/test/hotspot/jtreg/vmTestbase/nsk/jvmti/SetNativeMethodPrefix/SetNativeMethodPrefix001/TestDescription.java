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
 * @summary converted from VM Testbase nsk/jvmti/SetNativeMethodPrefix/SetNativeMethodPrefix001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test is designed to test whether SetNativeMethodPrefix()/SetNativeMethodPrefixes()
 *     JVMTI functions work according to specification.
 *     The test verifies that:
 *         - explicit and implicit(automatic) method binding works correctly,
 *           when native prefix is set using JVMTI function SetNativeMethodPrefix()
 *         - explicit and implicit(automatic) method binding works correctly,
 *           when native prefix is unset using JVMTI function SetNativeMethodPrefix(NULL)
 *         - both SetNativeMethodPrefix(prefix) and SetNativeMethodPrefixes({ prefix1 })
 *           behave equally
 *         - both SetNativeMethodPrefix(NULL) and SetNativeMethodPrefixes(0)
 *           behave equally
 *     Also there are some checks that JVMTI error codes to be correctly returned.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:SetNativeMethodPrefix001
 *      nsk.jvmti.SetNativeMethodPrefix.SetNativeMethodPrefix001
 */

