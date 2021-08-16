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
 * @summary converted from VM Testbase nsk/jvmti/ObjectFree/objfree002.
 * VM Testbase keywords: [quick, jpda, jvmti, noras]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JVMTI event ObjectFree.
 *     It verifies that that the events are only sent for tagged objects.
 *     The test also checks that the JVMTI raw monitor, memory management,
 *     and environment local storage functions can be used during processing
 *     this event.
 *     The test works as follows. An array of instances of the special
 *     class 'objfree002t' is used for testing. The objects with odd array
 *     indexes are skipped from tagging, all other ones are tagged. Then,
 *     after nulling the objects and GC() call, all received ObjectFree
 *     events, if so, should have only the even tag numbers.
 *     If there are no ObjectFree events, the test passes with appropriate
 *     message.
 * COMMENTS
 *     The tested assertion has been slightly changed due to the bugs 4944001,
 *     4937789.
 *     The test has been modified due to the rfe 4990048
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm/native -agentlib:objfree002=-waittime=5 nsk.jvmti.ObjectFree.objfree002 5
 */

