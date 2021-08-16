/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.code.site;

import java.util.Objects;

import jdk.vm.ci.meta.VMConstant;

/**
 * Represents a code site that references some data. The associated data can be either a
 * {@link DataSectionReference reference} to the data section, or it may be an inlined
 * {@link VMConstant} that needs to be patched.
 */
public final class DataPatch extends Site {

    public Reference reference;
    public Object note;

    public DataPatch(int pcOffset, Reference reference) {
        super(pcOffset);
        this.reference = reference;
        this.note = null;
    }

    public DataPatch(int pcOffset, Reference reference, Object note) {
        super(pcOffset);
        this.reference = reference;
        this.note = note;
    }

    @Override
    public String toString() {
        if (note != null) {
            return String.format("%d[<data patch referring to %s>, note: %s]", pcOffset, reference.toString(), note.toString());
        } else {
            return String.format("%d[<data patch referring to %s>]", pcOffset, reference.toString());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DataPatch) {
            DataPatch that = (DataPatch) obj;
            if (this.pcOffset == that.pcOffset && Objects.equals(this.reference, that.reference) && Objects.equals(this.note, that.note)) {
                return true;
            }
        }
        return false;
    }
}
