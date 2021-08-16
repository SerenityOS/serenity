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
 * @summary converted from VM Testbase nsk/jvmti/scenarios/allocation/AP05/ap05t001.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *   The test implements AP05 scenario of test plan for Allocation
 *   Profiling.
 *   The test checks the following common assertion from specs
 *   of IterateOverReachableObjects and
 *   IterateOverObjectsReachableFromObject:
 *      The callback (jvmtiObjectReferenceCallback) is called
 *      exactly once for each reference from a referrer;
 *      this is true even if there are reference cycles or
 *      multiple paths to the referrer.
 *   The test class 'ap05t001' has 'referrer' field of
 *   'ap05t001Subclass' type. The 'ap05t001Subclass' class
 *   extends 'ap05t001Superclass' class and implements
 *   'ap05t001Interface' interface. Each of the classes and
 *   the interface defines several static and instance fields
 *   of Object type but with dirrerent modifiers, i.e. private,
 *   public, protected or package-private. All these fields
 *   named as 'referreeXX' where XX is some decimal number.
 *   The test class initializes 'referrer' field and
 *   all 'referreeXX' fields. The last ones are assigned to
 *   the same Object instance. Then the tags are assigned
 *   to 'referrer', to Class instance of 'ap05t001Subclass'
 *   type and to the referred Object instance.
 *   Then the test agent runs IterateOverReachableObjects and
 *   IterateOverObjectsReachableFromObject. The test fails
 *   if defined jvmtiObjectReferenceCallback founds unexpected
 *   number of references between tagged objects.
 * COMMENTS
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native
 *      -agentlib:ap05t001=-waittime=5
 *      nsk.jvmti.scenarios.allocation.AP05.ap05t001
 */

