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

package com.sun.jdi;

import com.sun.jdi.request.BreakpointRequest;

/**
 * A proxy used by a debugger to examine or manipulate some entity
 * in another virtual machine. Mirror is the root of the
 * interface hierarchy for this package. Mirrors can be proxies for objects
 * in the target VM ({@link ObjectReference}), primitive values
 * (for example, {@link IntegerValue}), types (for example,
 * {@link ReferenceType}), dynamic application state (for example,
 * {@link StackFrame}), and even debugger-specific constructs (for example,
 * {@link BreakpointRequest}).
 * The {@link VirtualMachine} itself is also considered a mirror,
 * representing the composite state of the target VM.
 * <P>
 * There is no guarantee that a particular entity in the target VM will map
 * to a single instance of Mirror. Implementors are free to decide
 * whether a single mirror will be used for some or all mirrors. Clients
 * of this interface should always use <code>equals</code> to compare
 * two mirrors for equality.
 * <p>
 * Any method on a {@link Mirror} that takes a <code>Mirror</code> as an
 * parameter directly or indirectly (e.g., as a element in a <code>List</code>) will
 * throw {@link VMMismatchException} if the mirrors are from different
 * virtual machines.
 *
 * @see VirtualMachine
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface Mirror {

    /**
     * Gets the VirtualMachine to which this
     * Mirror belongs. A Mirror must be associated
     * with a VirtualMachine to have any meaning.
     *
     * @return the {@link VirtualMachine} for which this mirror is a proxy.
     */
    VirtualMachine virtualMachine();

    /**
     * Returns a String describing this mirror
     *
     * @return a string describing this mirror.
     */
    String toString();
}
