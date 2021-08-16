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
 * @summary converted from VM Testbase nsk/jvmti/unit/FollowReferences/followref006.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     The test verifies that:
 *     1. FollowReferences(), GetTag(), SetTag()
 *        and GetObjectsWithTags() functions return the following error codes:
 *        - JVMTI_ERROR_NULL_POINTER if null pointer was given in parameters
 *        - JVMTI_ERROR_INVALID_CLASS if invalid class was given
 *     2. FollowReferences() function doesn't call heapIterateCallback at all
 *        when:
 *        a. class == java.io.Serializable (because it's an interface)
 *        b. class == java.lang.Calendar
 *           (because it is not used in this test and no instances are
 *           created therefore)
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.unit.FollowReferences.followref006
 * @run main/othervm/native
 *      -agentlib:followref006=-waittime=5,-verbose
 *      nsk.jvmti.unit.FollowReferences.followref006
 */

