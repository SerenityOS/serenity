/*
 * Copyright (c) 2001, 2011, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.reflect;

import java.util.List;
import java.util.ArrayList;

/** Allows forward references in bytecode streams emitted by
    ClassFileAssembler. Assumes that the start of the method body is
    the first byte in the assembler's buffer. May be used at more than
    one branch site. */

class Label {
    static class PatchInfo {
        PatchInfo(ClassFileAssembler asm,
                  short instrBCI,
                  short patchBCI,
                  int stackDepth)
        {
            this.asm = asm;
            this.instrBCI   = instrBCI;
            this.patchBCI   = patchBCI;
            this.stackDepth = stackDepth;
        }
        // This won't work for more than one assembler anyway, so this is
        // unnecessary
        final ClassFileAssembler asm;
        final short instrBCI;
        final short patchBCI;
        final int   stackDepth;
    }
    private final List<PatchInfo> patches = new ArrayList<>();

    public Label() {
    }

    void add(ClassFileAssembler asm,
             short instrBCI,
             short patchBCI,
             int stackDepth)
    {
        patches.add(new PatchInfo(asm, instrBCI, patchBCI, stackDepth));
    }

    public void bind() {
        for (PatchInfo patch : patches){
            short curBCI = patch.asm.getLength();
            short offset = (short) (curBCI - patch.instrBCI);
            patch.asm.emitShort(patch.patchBCI, offset);
            patch.asm.setStack(patch.stackDepth);
        }
    }
}
