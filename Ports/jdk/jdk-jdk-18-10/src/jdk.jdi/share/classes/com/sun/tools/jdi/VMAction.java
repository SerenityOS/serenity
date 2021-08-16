/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.util.EventObject;

import com.sun.jdi.ThreadReference;
import com.sun.jdi.VirtualMachine;

/*
 * The name "action" is used to avoid confusion
 * with JDI events.
 */
class VMAction extends EventObject {

    private static final long serialVersionUID = -1701944679310296090L;

    // Event ids
    static final int VM_SUSPENDED = 1;
    static final int VM_NOT_SUSPENDED = 2;

    int id;
    ThreadReference resumingThread;

    VMAction(VirtualMachine vm, int id) {
        this(vm, null, id);
    }

    // For id = VM_NOT_SUSPENDED, if resumingThread != null, then it is
    // the only thread that is being resumed.
     VMAction(VirtualMachine vm, ThreadReference resumingThread, int id) {
        super(vm);
        this.id = id;
        this.resumingThread = resumingThread;
    }

    VirtualMachine vm() {
        return (VirtualMachine)getSource();
    }

    int id() {
        return id;
    }

    ThreadReference resumingThread() {
        return resumingThread;
    }
}
