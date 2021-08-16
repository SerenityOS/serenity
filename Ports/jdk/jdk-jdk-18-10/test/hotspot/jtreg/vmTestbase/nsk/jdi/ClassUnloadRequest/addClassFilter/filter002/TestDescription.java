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
 * @summary converted from VM Testbase nsk/jdi/ClassUnloadRequest/addClassFilter/filter002.
 * VM Testbase keywords: [jpda, jdi]
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test checks up the method
 *     com.sun.jdi.ClassUnloadRequest.addClassFilter(String)
 *     on the following assertion:
 *         This method throws InvalidRequestStateException - if this request
 *         is currently enabled or has been deleted.
 *     The test works as follows:
 *     The debugger program -
 *         nsk.jdi.ClassUnloadRequest.addClassFilter.filter002;
 *     the debuggee program -
 *         nsk.jdi.ClassUnloadRequest.addClassFilter.filter002a.
 *     Using nsk.jdi.share classes, the debugger gets the debuggee running on
 *     another JavaVM and attempts to add filter for:
 *         1. the disabled request;
 *         2. the deleted request.
 *     When InvalidRequestStateException is not thorwn, the test fails.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.ClassUnloadRequest.addClassFilter.filter002
 *        nsk.jdi.ClassUnloadRequest.addClassFilter.filter002a
 * @run main/othervm
 *      nsk.jdi.ClassUnloadRequest.addClassFilter.filter002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

