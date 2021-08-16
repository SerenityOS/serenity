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
 * @summary converted from VM Testbase nsk/jvmti/unit/FollowReferences/followref001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras, feature_jdk6_jpda, vm6]
 * VM Testbase readme:
 * DESCRIPTION
 *     This JVMTI test exercises JVMTI heap function FollowReferences().
 *     The tests checks:
 *       - that FollowReferences() function iterates over all tagged objects
 *         reachable from the tested root object and does not iterate over
 *         unreachable tagged objects;
 *       - that FollowReferences() passes correct values to the callbacks;
 *     The test creates and tags the following tested objects:
 *       - tested root objects             - object to be tested
 *       - chain of reachable objects      - objects to be iterated
 *       - chain of unreachable objects    - objects to be not iterated
 *     The test marks reachable objects with positive tags and
 *     unreachable objects with negative tags.
 *     Local JNI references are explicitly deleted to ensure them
 *     not to affect the objects' reachability.
 *     The test supports the following special options:
 *       objects=<number>
 *           - print number of objects in each objects chain
 *       info=[none|all|objref|stackref|heaproot|heapobj]
 *           - print trace message for each invocation of a particular callback
 * COMMENTS
 *     The test was cloned and modified from the original NSK test:
 *       nsk/jvmti/IterateOverObjectsReachableFromObject/iterobjreachobj001
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.unit.FollowReferences.followref001
 * @run main/othervm/native
 *      -agentlib:followref001=-waittime=5,-verbose,objects=3
 *      nsk.jvmti.unit.FollowReferences.followref001
 */

