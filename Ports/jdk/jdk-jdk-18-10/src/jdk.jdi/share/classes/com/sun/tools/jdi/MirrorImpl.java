/*
 * Copyright (c) 1998, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;

import com.sun.jdi.Mirror;
import com.sun.jdi.VMMismatchException;
import com.sun.jdi.VirtualMachine;

abstract class MirrorImpl extends Object implements Mirror {

    protected VirtualMachineImpl vm;

    MirrorImpl(VirtualMachine aVm) {
        super();

        // Yes, its a bit of a hack. But by doing it this
        // way, this is the only place we have to change
        // typing to substitute a new impl.
        vm = (VirtualMachineImpl)aVm;
    }

    public VirtualMachine virtualMachine() {
        return vm;
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof Mirror)) {
            Mirror other = (Mirror)obj;
            return vm.equals(other.virtualMachine());
        } else {
            return false;
        }
    }

    public int hashCode() {
        return vm.hashCode();
    }

    /**
     * Throw NullPointerException on null mirror.
     * Throw VMMismatchException on wrong VM.
     */
    void validateMirror(Mirror mirror) {
        if (!vm.equals(mirror.virtualMachine())) {
            throw new VMMismatchException(mirror.toString());
        }
    }

    /**
     * Allow null mirror.
     * Throw VMMismatchException on wrong VM.
     */
    void validateMirrorOrNull(Mirror mirror) {
        if ((mirror != null) && !vm.equals(mirror.virtualMachine())) {
            throw new VMMismatchException(mirror.toString());
        }
    }

    /**
     * Throw NullPointerException on null mirrors.
     * Throw VMMismatchException on wrong VM.
     */
    void validateMirrors(Collection<? extends Mirror> mirrors) {
        for (Mirror mirror : mirrors) {
            validateMirror(mirror);
        }
    }

    /**
     * Allow null mirrors.
     * Throw VMMismatchException on wrong VM.
     */
    void validateMirrorsOrNulls(Collection<? extends Mirror> mirrors) {
        for (Mirror mirror : mirrors) {
            validateMirrorOrNull(mirror);
        }
    }
}
