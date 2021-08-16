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

package com.sun.jdi.event;

import com.sun.jdi.VirtualMachine;

/**
 * Notification of a class unload in the target VM.
 * <p>
 * There are severe constraints on the debugger back-end during
 * garbage collection, so unload information is greatly limited.
 *
 * @see EventQueue
 * @see VirtualMachine
 *
 * @author Robert Field
 * @since  1.3
 */
public interface ClassUnloadEvent extends Event {

    /**
     * Returns the {@linkplain com.sun.jdi.Type#name() name of the class}
     * that has been unloaded. The returned string may not be a
     * <a href="{@docRoot}/java.base/java/lang/ClassLoader.html#binary-name">binary name</a>.
     *
     * @see Class#getName()
     */
    public String className();

    /**
     * Returns the {@linkplain com.sun.jdi.Type#signature() type signature of the class}
     * that has been unloaded.  The result is of the same
     * form as the string returned by {@link Class#descriptorString()}.
     * If this class can be described nominally, the returned string is a
     * type descriptor conforming to JVMS {@jvms 4.3.2}; otherwise, the returned string
     * is not a type descriptor.
     */
    public String classSignature();
}
