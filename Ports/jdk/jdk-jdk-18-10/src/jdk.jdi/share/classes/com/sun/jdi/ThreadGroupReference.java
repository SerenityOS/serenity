/*
 * Copyright (c) 1998, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * A thread group object from the target VM.
 * A ThreadGroupReference is an {@link ObjectReference} with additional
 * access to threadgroup-specific information from the target VM.
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface ThreadGroupReference extends ObjectReference {

    /**
     * Returns the name of this thread group.
     *
     * @return the string containing the thread group name.
     */
    String name();

    /**
     * Returns the parent of this thread group.
     *
     * @return a {@link ThreadGroupReference} mirroring the parent of this
     * thread group in the target VM, or null if this is a top-level
     * thread group.
     */
    ThreadGroupReference parent();

    /**
     * Suspends all threads in this thread group. Each thread
     * in this group and in all of its subgroups will be
     * suspended as described in {@link ThreadReference#suspend}.
     * This is not guaranteed to be an atomic
     * operation; if the target VM is not interrupted at the time
     * this method is
     * called, it is possible that new threads will be created
     * between the time that threads are enumerated and all of them
     * have been suspended.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    void suspend();

    /**
     * Resumes all threads in this thread group. Each thread
     * in this group and in all of its subgroups will be
     * resumed as described in {@link ThreadReference#resume}.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    void resume();

    /**
     * Returns a List containing a {@link ThreadReference} for each live thread
     * in this thread group. Only the live threads in this immediate thread group
     * (and not its subgroups) are returned.  A thread is alive if it
     * has been started and has not yet been stopped.
     *
     * @return a List of {@link ThreadReference} objects mirroring the
     * live threads from this thread group in the target VM.
     */
    List<ThreadReference> threads();

    /**
     * Returns a List containing each active {@link ThreadGroupReference} in this
     * thread group. Only the active thread groups in this immediate thread group
     * (and not its subgroups) are returned.
     * See {@link java.lang.ThreadGroup}
     * for information about 'active' ThreadGroups.
     * @return a List of {@link ThreadGroupReference} objects mirroring the
     * active thread groups from this thread group in the target VM.
     */
    List<ThreadGroupReference> threadGroups();
}
