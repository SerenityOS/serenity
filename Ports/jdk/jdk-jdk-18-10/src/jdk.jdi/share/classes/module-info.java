/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Defines the Java Debug Interface.
 * <p>
 * The Java Debug Interface (JDI) is a high level Java API providing
 * information useful for debuggers and similar systems needing access to the
 * running state of a (usually remote) virtual machine.
 * <p>
 * JDI provides introspective access to a running virtual machine's state,
 * Class, Array, Interface, and primitive types, and instances of those types.
 * <p>
 * JDI also provides explicit control over a virtual machine's execution.
 * The ability to suspend and resume threads, and to set breakpoints,
 * watchpoints, etc. Notification of exceptions, class loading, thread
 * creation, etc. The ability to inspect a suspended thread's state, local
 * variables, stack backtrace, etc.
 * <p>
 * JDI is the highest-layer of the
 * <a href="{@docRoot}/../specs/jpda/jpda.html">
 * Java Platform Debugger Architecture (JPDA)</a>.
 * <p>
 * This module includes a simple command-line debugger,
 * <em>{@index jdb jdb tool}</em>.
 *
 * <h2>Global Exceptions</h2>
 * <p>
 * This section documents exceptions which apply to the entire API and are thus
 * not documented on individual methods.
 * <blockquote>
 *   <p>
 *   <b>{@link com.sun.jdi.VMMismatchException}</b>
 *   <p>
 *   Any method on a {@link com.sun.jdi.Mirror} that takes a
 *   {@code Mirror} as an parameter directly or indirectly (e.g., as a
 *   element in a {@code List}) will throw {@link
 *   com.sun.jdi.VMMismatchException} if the mirrors are from different virtual
 *   machines.
 *   <p>
 *   <b>{@link java.lang.NullPointerException}</b>
 *   <p>
 *   Any method which takes a {@link java.lang.Object} as an parameter will
 *   throw {@link java.lang.NullPointerException} if null is passed directly or
 *   indirectly -- unless null is explicitly mentioned as a valid parameter.
 * </blockquote>
 * NOTE: The exceptions below may be thrown whenever the specified conditions
 * are met but a guarantee that they are thrown only exists when a valid result
 * cannot be returned.
 * <blockquote>
 *   <p>
 *   <b>{@link com.sun.jdi.VMDisconnectedException}</b>
 *   <p>
 *   Any method on {@link com.sun.jdi.ObjectReference}, {@link
 *   com.sun.jdi.ReferenceType}, {@link com.sun.jdi.request.EventRequest},
 *   {@link com.sun.jdi.StackFrame}, or {@link com.sun.jdi.VirtualMachine} or
 *   which takes one of these directly or indirectly as an parameter may throw
 *   {@link com.sun.jdi.VMDisconnectedException} if the target VM is
 *   disconnected and the {@link com.sun.jdi.event.VMDisconnectEvent} has been
 *   or is available to be read from the {@link com.sun.jdi.event.EventQueue}.
 *   <p>
 *   <b>{@link com.sun.jdi.VMOutOfMemoryException}</b>
 *   <p>
 *   Any method on {@link com.sun.jdi.ObjectReference}, {@link
 *   com.sun.jdi.ReferenceType}, {@link com.sun.jdi.request.EventRequest},
 *   {@link com.sun.jdi.StackFrame}, or {@link com.sun.jdi.VirtualMachine} or
 *   which takes one of these directly or indirectly as an parameter may throw
 *   {@link com.sun.jdi.VMOutOfMemoryException} if the target VM has run out of
 *   memory.
 *   <p>
 *   <b>{@link com.sun.jdi.ObjectCollectedException}</b>
 *   <p>
 *   Any method on {@link com.sun.jdi.ObjectReference} or which directly or
 *   indirectly takes {@code ObjectReference} as parameter may throw
 *   {@link com.sun.jdi.ObjectCollectedException} if the mirrored object has
 *   been garbage collected.
 *   <p>
 *   Any method on {@link com.sun.jdi.ReferenceType} or which directly or
 *   indirectly takes {@code ReferenceType} as parameter may throw {@link
 *   com.sun.jdi.ObjectCollectedException} if the mirrored type has been
 *   unloaded.
 * </blockquote>
 *
 *
 * @toolGuide jdb
 *
 * @provides com.sun.jdi.connect.Connector
 *
 * @uses com.sun.jdi.connect.Connector
 * @uses com.sun.jdi.connect.spi.TransportService
 *
 * @moduleGraph
 * @since 9
 * @see <a href="{@docRoot}/../specs/jpda/jpda.html">
 * Java Platform Debugger Architecture (JPDA)</a>
 */
module jdk.jdi {
    requires jdk.attach;
    requires jdk.jdwp.agent;

    exports com.sun.jdi;
    exports com.sun.jdi.connect;
    exports com.sun.jdi.connect.spi;
    exports com.sun.jdi.event;
    exports com.sun.jdi.request;

    uses com.sun.jdi.connect.Connector;
    uses com.sun.jdi.connect.spi.TransportService;

    // windows shared memory connector providers are added at build time
    provides com.sun.jdi.connect.Connector with
        com.sun.tools.jdi.ProcessAttachingConnector,
        com.sun.tools.jdi.RawCommandLineLauncher,
        com.sun.tools.jdi.SocketAttachingConnector,
        com.sun.tools.jdi.SocketListeningConnector,
        com.sun.tools.jdi.SunCommandLineLauncher;
}
