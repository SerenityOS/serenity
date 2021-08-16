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
 * @key randomness
 *
 * @summary converted from VM Testbase nsk/jvmti/ObjectFree/objfree001.
 * VM Testbase keywords: [jpda, jvmti, noras, nonconcurrent]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test exercises the JVMTI event ObjectFree.
 *     It verifies that the the JVMTI raw monitor, memory management,
 *     and environment local storage functions can be used during
 *     processing this event.
 *     The test works as follows. The special class 'objfree001u' is
 *     loaded from directory 'loadclass' by a custom class loader.
 *     Instance of objfree001u stored in field 'unClsObj' is tagged by
 *     the agent via SetTag(). Then the class is tried to be unloaded.
 *     Received ObjectFree event is checked to have previously set tag.
 *     The JVMTI functions mentioned above should be invoked sucessfully
 *     during the event callback as well. If there are no ObjectFree
 *     events, the test passes with appropriate message.
 * COMMENTS
 *     The test has been fixed due to the bug 4940250.
 *     The tested assertion has been changed due to the bugs 4944001, 4937789.
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jvmti.ObjectFree.objfree001
 *
 * @comment compile loadclassXX to bin/loadclassXX
 * @run driver nsk.share.ExtraClassesBuilder
 *      loadclass
 *
 * @run main/othervm/native
 *      -agentlib:objfree001=-waittime=5
 *      -XX:-UseGCOverheadLimit
 *      nsk.jvmti.ObjectFree.objfree001
 *      ./bin 5 ./bin
 */

