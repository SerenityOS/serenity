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
 * @summary converted from VM Testbase nsk/jvmti/ForceEarlyReturn/ForceEarlyReturn002.
 * VM Testbase keywords: [quick, jpda, jvmti, onload_only_caps, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test checks that JVMTI function ForceEarlyReturnObject() correctly
 *     handle passed return value object.
 *     JVMTI_ERROR_TYPE_MISMATCH error should be returned only when return value
 *     is not assignable to the return type of method which is forced to return.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ForceEarlyReturn001
 *      nsk.jvmti.ForceEarlyReturn.ForceEarlyReturn002
 */

