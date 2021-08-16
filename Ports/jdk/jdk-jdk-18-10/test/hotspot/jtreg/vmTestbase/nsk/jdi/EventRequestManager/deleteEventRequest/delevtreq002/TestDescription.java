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
 * @summary converted from VM Testbase nsk/jdi/EventRequestManager/deleteEventRequest/delevtreq002.
 * VM Testbase keywords: [jpda, jdi, quarantine]
 * VM Testbase comments: 4613913
 * VM Testbase readme:
 * DESCRIPTION:
 *     The test for the implementation of an object of the type
 *     EventRequestManager.
 *     The test checks up that a result of the method
 *     com.sun.jdi.EventRequestManager.deleteEventRequest()
 *     complies with its spec:
 *     public void deleteEventRequest(EventRequest eventRequest)
 *      Removes an eventRequest. The eventRequest is disabled and the removed from the
 *      requests managed by this EventRequestManager.
 *      Once the eventRequest is deleted, no operations
 *      (for example, EventRequest.setEnabled(boolean)) are permitted -
 *      attempts to do so will generally cause an InvalidRequestStateException.
 *      No other eventRequests are effected.
 *      Because this method changes the underlying lists of event requests,
 *      attempting to directly delete from a list returned by a request accessor
 *      (e.g. below):
 *          Iterator iter = requestManager.stepRequests().iterator();
 *          while (iter.hasNext()) {
 *              requestManager.deleteEventRequest(iter.next());
 *          }
 *      may cause a ConcurrentModificationException.
 *      Instead use deleteEventRequests(List) or copy the list before iterating.
 *      Parameters: eventRequest - the eventRequest to remove
 *     The test checks up on the following assertion:
 *         Once the eventRequest is deleted, no operations
 *         (for example, EventRequest.setEnabled(boolean)) are permitted -
 *         attempts to do so will generally cause an InvalidRequestStateException.
 *     Testcases unclude all 12 sublcasses of EventRequest created by EventRequestManager.
 *     The test works as follows:
 *     The debugger program - nsk.jdi.EventRequestManager.deleteEventRequest.delevtreq002;
 *     the debuggee program - nsk.jdi.EventRequestManager.deleteEventRequest.delevtreq002a.
 *     Using nsk.jdi.share classes,
 *     the debugger gets the debuggee running on another JavaVM,
 *     creates the object debuggee.VM, and waits for VMStartEvent.
 *     Upon getting the debuggee VM started,
 *     the debugger calls corresponding debuggee.VM methods to get
 *     needed data and to perform checks.
 *     In case of error the test produces the return value 97 and
 *     a corresponding error message(s).
 *     Otherwise, the test is passed and produces
 *     the return value 95 and no message.
 * COMMENTS:
 *
 * @library /vmTestbase
 *          /test/lib
 * @build nsk.jdi.EventRequestManager.deleteEventRequest.delevtreq002
 *        nsk.jdi.EventRequestManager.deleteEventRequest.delevtreq002a
 * @run main/othervm
 *      nsk.jdi.EventRequestManager.deleteEventRequest.delevtreq002
 *      -verbose
 *      -arch=${os.family}-${os.simpleArch}
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

