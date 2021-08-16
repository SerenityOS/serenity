/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes the {@link Local}s for a Java method.
 *
 * @see "https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.7.13"
 */
public class LocalVariableTable {

    private final Local[] locals;

    /**
     * Creates an object describing the {@link Local}s for a Java method.
     *
     * @param locals array of objects describing local variables. This array is now owned by this
     *            object and must not be mutated by the caller.
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "caller transfers ownership of `locals`")
    public LocalVariableTable(Local[] locals) {
        this.locals = locals;
    }

    /**
     * Gets a description of a local variable that occupies the bytecode frame slot indexed by
     * {@code slot} and is live at the bytecode index {@code bci}.
     *
     * @return a description of the requested local variable or null if no such variable matches
     *         {@code slot} and {@code bci}
     */
    public Local getLocal(int slot, int bci) {
        Local result = null;
        for (Local local : locals) {
            if (local.getSlot() == slot && local.getStartBCI() <= bci && local.getEndBCI() >= bci) {
                if (result == null) {
                    result = local;
                } else {
                    throw new IllegalStateException("Locals overlap!");
                }
            }
        }
        return result;
    }

    /**
     * Gets a copy of the array of {@link Local}s that was passed to this object's constructor.
     */
    public Local[] getLocals() {
        return locals.clone();
    }

    /**
     * Gets a description of all the local variables live at the bytecode index {@code bci}.
     */
    public Local[] getLocalsAt(int bci) {
        List<Local> result = new ArrayList<>();
        for (Local l : locals) {
            if (l.getStartBCI() <= bci && bci <= l.getEndBCI()) {
                result.add(l);
            }
        }
        return result.toArray(new Local[result.size()]);
    }
}
