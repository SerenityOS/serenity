/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell.execution;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import jdk.jshell.spi.ExecutionControl;
import static java.util.stream.Collectors.toMap;

/**
 * Abstract JDI implementation of {@link jdk.jshell.spi.ExecutionControl}.
 *
 * @since 9
 */
public abstract class JdiExecutionControl extends StreamingExecutionControl implements ExecutionControl {

    /**
     * Mapping from class names to JDI {@link ReferenceType}.
     */
    private final Map<String, ReferenceType> toReferenceType = new HashMap<>();

    /**
     * Create an instance.
     * @param out the output from the remote agent
     * @param in the input to the remote agent
     */
    protected JdiExecutionControl(ObjectOutput out, ObjectInput in) {
        super(out, in);
    }

    /**
     * Returns the JDI {@link VirtualMachine} instance.
     *
     * @return the virtual machine
     * @throws EngineTerminationException if the VM is dead/disconnected
     */
    protected abstract VirtualMachine vm() throws EngineTerminationException;

    /**
     * Redefine the specified classes. Where 'redefine' is, as in JDI and JVMTI,
     * an in-place replacement of the classes (preserving class identity) --
     * that is, existing references to the class do not need to be recompiled.
     * This implementation uses JDI
     * {@link com.sun.jdi.VirtualMachine#redefineClasses(java.util.Map) }.
     * It will be unsuccessful if
     * the signature of the class has changed (see the JDI spec). The
     * JShell-core is designed to adapt to unsuccessful redefine.
     */
    @Override
    public void redefine(ClassBytecodes[] cbcs)
            throws ClassInstallException, EngineTerminationException {
        try {
            // Convert to the JDI ReferenceType to class bytes map form needed
            // by JDI.
            VirtualMachine vm = vm();
            Map<ReferenceType, byte[]> rmp = Stream.of(cbcs)
                    .collect(toMap(
                            cbc -> referenceType(vm, cbc.name()),
                            cbc -> cbc.bytecodes()));
            // Attempt redefine.  Throws exceptions on failure.
            vm().redefineClasses(rmp);
        } catch (EngineTerminationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ClassInstallException("redefine: " + ex.getMessage(), new boolean[cbcs.length]);
        }
        // forward the redefine to remote-end to register the redefined bytecode
        try {
            super.redefine(cbcs);
        } catch (NotImplementedException ex) {
            // this remote doesn't care about registering bytecode, so we don't either
        }
    }

    /**
     * Returns the JDI {@link ReferenceType} corresponding to the specified
     * class name.
     *
     * @param vm the current JDI {@link VirtualMachine} as returned by
     * {@code vm()}
     * @param name the class name to look-up
     * @return the corresponding {@link ReferenceType}
     */
    protected ReferenceType referenceType(VirtualMachine vm, String name) {
        return toReferenceType.computeIfAbsent(name, n -> nameToRef(vm, n));
    }

    private static ReferenceType nameToRef(VirtualMachine vm, String name) {
        List<ReferenceType> rtl = vm.classesByName(name);
        if (rtl.size() != 1) {
            return null;
        }
        return rtl.get(0);
    }

}
