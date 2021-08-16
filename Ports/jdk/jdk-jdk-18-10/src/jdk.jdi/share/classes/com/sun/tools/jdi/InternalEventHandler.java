/*
 * Copyright (c) 1998, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.sun.tools.jdi;

import com.sun.jdi.ClassNotPreparedException;
import com.sun.jdi.InconsistentDebugInfoException;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.VMDisconnectedException;
import com.sun.jdi.VMOutOfMemoryException;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.ClassUnloadEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventSet;

public class InternalEventHandler implements Runnable
{
    EventQueueImpl queue;
    VirtualMachineImpl vm;

    InternalEventHandler(VirtualMachineImpl vm, EventQueueImpl queue) {
        this.vm = vm;
        this.queue = queue;
        Thread thread = new Thread(vm.threadGroupForJDI(), this,
                                   "JDI Internal Event Handler");
        thread.setDaemon(true);
        thread.start();
    }

    public void run() {
        if ((vm.traceFlags & VirtualMachine.TRACE_EVENTS) != 0) {
            vm.printTrace("Internal event handler running");
        }
        try {
            while (true) {
                try {
                    EventSet eventSet = queue.removeInternal();
                    EventIterator it = eventSet.eventIterator();
                    while (it.hasNext()) {
                        Event event = it.nextEvent();
                        if (event instanceof ClassUnloadEvent) {
                            ClassUnloadEvent cuEvent = (ClassUnloadEvent)event;
                            vm.removeReferenceType(cuEvent.classSignature());

                            if ((vm.traceFlags & VirtualMachine.TRACE_EVENTS) != 0) {
                                vm.printTrace("Handled Unload Event for " +
                                              cuEvent.classSignature());
                            }
                        } else if (event instanceof ClassPrepareEvent) {
                            ClassPrepareEvent cpEvent = (ClassPrepareEvent)event;
                            ((ReferenceTypeImpl)cpEvent.referenceType())
                                                            .markPrepared();

                            if ((vm.traceFlags & VirtualMachine.TRACE_EVENTS) != 0) {
                                vm.printTrace("Handled Prepare Event for " +
                                              cpEvent.referenceType().name());
                            }
                        }
                    }

                /*
                 * Handle exceptions that can occur in normal operation
                 * but which can't be accounted for by event builder
                 * methods. The thread should not be terminated if they
                 * occur.
                 *
                 * TO DO: We need a better way to log these conditions.
                 */
                } catch (VMOutOfMemoryException vmme) {
                    vmme.printStackTrace();
                } catch (InconsistentDebugInfoException idie) {
                    idie.printStackTrace();

                /*
                 * If any of these exceptions below occurs, there is some
                 * sort of programming error that should be addressed in
                 * the JDI implemementation. However, it would cripple
                 * the implementation if we let this thread die due to
                 * one of them. So, a notification of the exception is
                 * given and we attempt to continue.
                 */
                } catch (ObjectCollectedException oce) {
                    oce.printStackTrace();
                } catch (ClassNotPreparedException cnpe) {
                    cnpe.printStackTrace();
                }
            }
        } catch (InterruptedException e) {  // should we really die here
        } catch (VMDisconnectedException e) {  // time to die
        }
        if ((vm.traceFlags & VirtualMachine.TRACE_EVENTS) != 0) {
            vm.printTrace("Internal event handler exiting");
        }
    }
}
