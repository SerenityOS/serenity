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

import java.util.List;

/**
 * A mirror of an interface in the target VM. An InterfaceType is
 * a refinement of {@link ReferenceType} that applies to true interfaces
 * in the JLS  sense of the definition (not a class, not an array type).
 * An interface type will never be returned by
 * {@link ObjectReference#referenceType}, but it may be in the list
 * of implemented interfaces for a {@link ClassType} that is returned
 * by that method.
 *
 * @see ObjectReference
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface InterfaceType extends ReferenceType {

    /**
     * Gets the interfaces directly extended by this interface.
     * The returned list contains only those interfaces this
     * interface has declared to be extended.
     *
     * @return a List of {@link InterfaceType} objects each mirroring
     * an interface extended by this interface.
     * If none exist, returns a zero length List.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<InterfaceType> superinterfaces();

    /**
     * Gets the currently prepared interfaces which directly extend this
     * interface. The returned list contains only those interfaces that
     * declared this interface in their "extends" clause.
     *
     * @return a List of {@link InterfaceType} objects each mirroring
     * an interface extending this interface.
     * If none exist, returns a zero length List.
     */
    List<InterfaceType> subinterfaces();

    /**
     * Gets the currently prepared classes which directly implement this
     * interface. The returned list contains only those classes that
     * declared this interface in their "implements" clause.
     *
     * @return a List of {@link ClassType} objects each mirroring
     * a class implementing this interface.
     * If none exist, returns a zero length List.
     */
    List<ClassType> implementors();

    /**
     * Invokes the specified static {@link Method} in the
     * target VM. The
     * specified method must be defined in this interface.
     * The method must be a static method
     * but not a static initializer.
     * <p>
     * The method invocation will occur in the specified thread.
     * Method invocation can occur only if the specified thread
     * has been suspended by an event which occurred in that thread.
     * Method invocation is not supported
     * when the target VM has been suspended through
     * {@link VirtualMachine#suspend} or when the specified thread
     * is suspended through {@link ThreadReference#suspend}.
     * <p>
     * The specified method is invoked with the arguments in the specified
     * argument list.  The method invocation is synchronous; this method
     * does not return until the invoked method returns in the target VM.
     * If the invoked method throws an exception, this method will throw
     * an {@link InvocationException} which contains a mirror to the exception
     * object thrown.
     * <p>
     * Object arguments must be assignment compatible with the argument type
     * (This implies that the argument type must be loaded through the
     * enclosing class' class loader). Primitive arguments must be
     * either assignment compatible with the argument type or must be
     * convertible to the argument type without loss of information.
     * If the method being called accepts a variable number of arguments,
     * then the last argument type is an array of some component type.
     * The argument in the matching position can be omitted, or can be null,
     * an array of the same component type, or an argument of the
     * component type followed by any number of other arguments of the same
     * type. If the argument is omitted, then a 0 length array of the
     * component type is passed.  The component type can be a primitive type.
     * Autoboxing is not supported.
     *
     * See Section 5.2 of
     * <cite>The Java Language Specification</cite>
     * for more information on assignment compatibility.
     * <p>
     * By default, all threads in the target VM are resumed while
     * the method is being invoked if they were previously
     * suspended by an event or by {@link VirtualMachine#suspend} or
     * {@link ThreadReference#suspend}. This is done to prevent the deadlocks
     * that will occur if any of the threads own monitors
     * that will be needed by the invoked method.
     * Note, however, that this implicit resume acts exactly like
     * {@link ThreadReference#resume}, so if the thread's suspend
     * count is greater than 1, it will remain in a suspended state
     * during the invocation and thus a deadlock could still occur.
     * By default, when the invocation completes,
     * all threads in the target VM are suspended, regardless their state
     * before the invocation.
     * It is possible that
     * breakpoints or other events might occur during the invocation.
     * This can cause deadlocks as described above. It can also cause a deadlock
     * if invokeMethod is called from the client's event handler thread.  In this
     * case, this thread will be waiting for the invokeMethod to complete and
     * won't read the EventSet that comes in for the new event.  If this
     * new EventSet is SUSPEND_ALL, then a deadlock will occur because no
     * one will resume the EventSet.  To avoid this, all EventRequests should
     * be disabled before doing the invokeMethod, or the invokeMethod should
     * not be done from the client's event handler thread.
     * <p>
     * The resumption of other threads during the invocation can be prevented
     * by specifying the {@link ClassType#INVOKE_SINGLE_THREADED}
     * bit flag in the <code>options</code> argument; however,
     * there is no protection against or recovery from the deadlocks
     * described above, so this option should be used with great caution.
     * Only the specified thread will be resumed (as described for all
     * threads above). Upon completion of a single threaded invoke, the invoking thread
     * will be suspended once again. Note that any threads started during
     * the single threaded invocation will not be suspended when the
     * invocation completes.
     * <p>
     * If the target VM is disconnected during the invoke (for example, through
     * {@link VirtualMachine#dispose}) the method invocation continues.
     *
     * @param thread the thread in which to invoke.
     * @param method the {@link Method} to invoke.
     * @param arguments the list of {@link Value} arguments bound to the
     * invoked method. Values from the list are assigned to arguments
     * in the order they appear in the method signature.
     * @param options the integer bit flag options.
     * @return a {@link Value} mirror of the invoked method's return value.
     * @throws java.lang.IllegalArgumentException if the method is not
     * a member of this interface, if the size of the argument list
     * does not match the number of declared arguments for the method, or
     * if the method is not static or is a static initializer.
     * @throws ClassNotLoadedException if any argument type has not yet been loaded
     * through the appropriate class loader.
     * @throws IncompatibleThreadStateException if the specified thread has not
     * been suspended by an event.
     * @throws InvocationException if the method invocation resulted in
     * an exception in the target VM.
     * @throws InvalidTypeException If the arguments do not meet this requirement --
     *         Object arguments must be assignment compatible with the argument
     *         type.  This implies that the argument type must be
     *         loaded through the enclosing class' class loader.
     *         Primitive arguments must be either assignment compatible with the
     *         argument type or must be convertible to the argument type without loss
     *         of information. See JLS section 5.2 for more information on assignment
     *         compatibility.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     *
     * @since 1.8
     */
    default Value invokeMethod(ThreadReference thread, Method method,
                               List<? extends Value> arguments, int options)
            throws InvalidTypeException,
                   ClassNotLoadedException,
                   IncompatibleThreadStateException,
                   InvocationException
    {
        throw new UnsupportedOperationException();
    }
}
