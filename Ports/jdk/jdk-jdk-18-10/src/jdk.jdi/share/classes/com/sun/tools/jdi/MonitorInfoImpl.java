/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jdi.InternalException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.MonitorInfo;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

public class MonitorInfoImpl extends MirrorImpl
    implements MonitorInfo, ThreadListener {

    /* Once false, monitorInfo should not be used.
     * access synchronized on (vm.state())
     */
    private boolean isValid = true;

    ObjectReference monitor;
    ThreadReference thread;
    int stack_depth;

    MonitorInfoImpl(VirtualMachine vm, ObjectReference mon,
                    ThreadReferenceImpl thread, int dpth) {
        super(vm);
        this.monitor = mon;
        this.thread = thread;
        this.stack_depth = dpth;
        thread.addListener(this);
    }

    /*
     * ThreadListener implementation
     * Must be synchronized since we must protect against
     * sending defunct (isValid == false) stack ids to the back-end.
     */
    public boolean threadResumable(ThreadAction action) {
        synchronized (vm.state()) {
            if (isValid) {
                isValid = false;
                return false;   /* remove this stack frame as a listener */
            } else {
                throw new InternalException(
                                  "Invalid stack frame thread listener");
            }
        }
    }

    private void validateMonitorInfo() {
        if (!isValid) {
            throw new InvalidStackFrameException("Thread has been resumed");
        }
    }

    public ObjectReference monitor() {
        validateMonitorInfo();
        return monitor;
    }

    public int stackDepth() {
        validateMonitorInfo();
        return stack_depth;
    }

    public ThreadReference thread() {
        validateMonitorInfo();
        return thread;
    }
}
