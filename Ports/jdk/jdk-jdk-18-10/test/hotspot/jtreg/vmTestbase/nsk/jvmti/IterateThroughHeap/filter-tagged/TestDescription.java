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
 * @summary converted from VM Testbase nsk/jvmti/IterateThroughHeap/filter-tagged.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * This test exercises JVMTI function IterateOverHeap().
 * Test checks that for slected heap-filter all expected objects will be reached
 * and reported by appropriate callback.
 * Test consist of two mostly idential classes.
 * Agent tags instance and class of first one of them as well as it's non-primitive fields.
 * Depending on heap-filter selected by agent's 'filter' option for each field in instances
 * of those two classes flag that indicates whether or not field should be reported by
 * any callback is calculated.
 * Classes of non-primitive fields are not tagged.
 * Following table illustrates which fields depending on their type and selected heap
 * filter are expected to be reported:
 * object\Filter             TAGGED        UNTAGGED        CLASS_TAGGED    CLASS_UNTAGGED
 * Tagged class
 * primitive static field    -             +               +               -
 * primitive instance field  -             +               -               +
 * primitive array field     -             +               +               -
 * String field              -             +               +               -
 * Untagged class
 * primitive static field    +             -               +               -
 * primitive instance field  +             -               +               -
 * primitive array field     +             -               +               -
 * String field              +             -               +               -
 * HeapFilter agent support option filter=<heap-filter>, where <heap-filter> is one of the following:
 * JVMTI_HEAP_FILTER_TAGGED
 * JVMTI_HEAP_FILTER_UNTAGGED
 * JVMTI_HEAP_FILTER_CLASS_TAGGED
 * JVMTI_HEAP_FILTER_CLASS_UNTAGGED
 * Test filter-tagged uses JVMTI_HEAP_FILTER_TAGGED.
 * Test filter-untagged uses JVMTI_HEAP_FILTER_UNTAGGED.
 * Test filter-class-tagged uses JVMTI_HEAP_FILTER_CLASS_TAGGED.
 * Test filter-class-untagged uses JVMTI_HEAP_FILTER_CLASS_UNTAGGED.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.IterateThroughHeap.filter_tagged.HeapFilter
 * @run main/othervm/native
 *      -agentlib:HeapFilter=-waittime=5,filter=JVMTI_HEAP_FILTER_TAGGED
 *      nsk.jvmti.IterateThroughHeap.filter_tagged.HeapFilter
 */

