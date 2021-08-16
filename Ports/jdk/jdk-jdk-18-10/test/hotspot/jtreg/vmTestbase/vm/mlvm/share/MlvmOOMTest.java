/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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

package vm.mlvm.share;

import java.util.LinkedList;
import java.util.List;

/**
 * The base class for Mlvm tests checking various OOM Errors.
 * Subclasses are expected to implement {@link #eatMemory(List)}/
 * {@link checkOOME(OutOfMemoryError)} methods consuming memory in various ways.
 */
public abstract class MlvmOOMTest extends MlvmTest {
    private static Object garbage;

    /**
     * A template method.
     * Implements logic of the tests:
     * consumes memory in loop until OOM is thrown, checks the OOM type.
     */
    @Override
    public final boolean run() {
        Env.display("Test started.");
        LinkedList<Object> objects = new LinkedList<Object>();
        // to trick EA
        garbage = objects;
        try {
            eatMemory(objects);
        } catch (OutOfMemoryError oome) {
            objects.clear();
            Env.display("Caught OOME : " + oome.getMessage());
            checkOOME(oome);
            return true;
        }
        throw new RuntimeException("TEST FAIL : no OOME");
    }

    /**
     * Consumes memory.
     * Subclasses implement their logic of memory consuming. Created objects are expected
     * to be stored in the given parameter.
     * The normal termination of the method is throwing OOM exception, which will be checked
     * by the {@link #checkOOME(OutOfMemoryError)}
     * Not throwing the OOM will be interpreted as test failure.
     * @param garbage a list to store generated garbage
     */
    protected abstract void eatMemory(List<Object> garbage);

    /**
     * Checks the OOME type is expected.
     * Method just exits if OOME is expected and throws an exeption if not.
     * @param oome thrown by {@link #eatMemory(List)}
     */
    protected abstract void checkOOME(OutOfMemoryError oome);
}
