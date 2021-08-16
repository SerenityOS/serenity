/*
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package jdk.internal.org.objectweb.asm.tree.analysis;

import java.util.ArrayList;
import java.util.List;
import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;

/**
 * A method subroutine (corresponds to a JSR instruction).
 *
 * @author Eric Bruneton
 */
final class Subroutine {

    /** The start of this subroutine. */
    final LabelNode start;

    /**
      * The local variables that are read or written by this subroutine. The i-th element is true if
      * and only if the local variable at index i is read or written by this subroutine.
      */
    final boolean[] localsUsed;

    /** The JSR instructions that jump to this subroutine. */
    final List<JumpInsnNode> callers;

    /**
      * Constructs a new {@link Subroutine}.
      *
      * @param start the start of this subroutine.
      * @param maxLocals the local variables that are read or written by this subroutine.
      * @param caller a JSR instruction that jump to this subroutine.
      */
    Subroutine(final LabelNode start, final int maxLocals, final JumpInsnNode caller) {
        this.start = start;
        this.localsUsed = new boolean[maxLocals];
        this.callers = new ArrayList<>();
        callers.add(caller);
    }

    /**
      * Constructs a copy of the given {@link Subroutine}.
      *
      * @param subroutine the subroutine to copy.
      */
    Subroutine(final Subroutine subroutine) {
        this.start = subroutine.start;
        this.localsUsed = subroutine.localsUsed.clone();
        this.callers = new ArrayList<>(subroutine.callers);
    }

    /**
      * Merges the given subroutine into this subroutine. The local variables read or written by the
      * given subroutine are marked as read or written by this one, and the callers of the given
      * subroutine are added as callers of this one (if both have the same start).
      *
      * @param subroutine another subroutine. This subroutine is left unchanged by this method.
      * @return whether this subroutine has been modified by this method.
      */
    public boolean merge(final Subroutine subroutine) {
        boolean changed = false;
        for (int i = 0; i < localsUsed.length; ++i) {
            if (subroutine.localsUsed[i] && !localsUsed[i]) {
                localsUsed[i] = true;
                changed = true;
            }
        }
        if (subroutine.start == start) {
            for (int i = 0; i < subroutine.callers.size(); ++i) {
                JumpInsnNode caller = subroutine.callers.get(i);
                if (!callers.contains(caller)) {
                    callers.add(caller);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
