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
 * A mirror of a class in the target VM. A ClassType is a refinement
 * of {@link ReferenceType} that applies to true classes in the JLS
 * sense of the definition (not an interface, not an array type). Any
 * {@link ObjectReference} that mirrors an instance of such a class
 * will have a ClassType as its type.
 *
 * @see ObjectReference
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface ClassType extends ReferenceType {

    /**
     * Gets the superclass of this class.
     *
     * @return a {@link ClassType} that mirrors the superclass
     * of this class in the target VM. If no such class exists,
     * returns null
     */
    ClassType superclass();

    /**
     * Gets the interfaces directly implemented by this class.
     * Only the interfaces that are declared with the "implements"
     * keyword in this class are included.
     *
     * @return a List of {@link InterfaceType} objects each mirroring
     * a direct interface this ClassType in the target VM.
     * If none exist, returns a zero length List.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<InterfaceType> interfaces();

    /**
     * Gets the interfaces directly and indirectly implemented
     * by this class. Interfaces returned by {@link ClassType#interfaces}
     * are returned as well all superinterfaces.
     *
     * @return a List of {@link InterfaceType} objects each mirroring
     * an interface of this ClassType in the target VM.
     * If none exist, returns a zero length List.
     * @throws ClassNotPreparedException if this class not yet been
     * prepared.
     */
    List<InterfaceType> allInterfaces();

    /**
     * Gets the currently loaded, direct subclasses of this class.
     * No ordering of this list is guaranteed.
     *
     * @return a List of {@link ClassType} objects each mirroring a loaded
     * subclass of this class in the target VM. If no such classes
     * exist, this method returns a zero-length list.
     */
    List<ClassType> subclasses();

    /**
     * Determine if this class was declared as an enum.
     * @return <code>true</code> if this class was declared as an enum; false
     * otherwise.
     */
    boolean isEnum();

    /**
     * Assigns a value to a static field.
     * The {@link Field} must be valid for this ClassType; that is,
     * it must be from the mirrored object's class or a superclass of that class.
     * The field must not be final.
     * <p>
     * Object values must be assignment compatible with the field type
     * (This implies that the field type must be loaded through the
     * enclosing class' class loader). Primitive values must be
     * either assignment compatible with the field type or must be
     * convertible to the field type without loss of information.
     * See JLS section 5.2 for more information on assignment
     * compatibility.
     *
     * @param field the field to set.
     * @param value the value to be assigned.
     * @throws java.lang.IllegalArgumentException if the field is
     * not static, the field is final, or the field does not exist
     * in this class.
     * @throws ClassNotLoadedException if the field type has not yet been loaded
     * through the appropriate class loader.
     * @throws InvalidTypeException if the value's type does not match
     * the field's declared type.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    void setValue(Field field, Value value)
        throws InvalidTypeException, ClassNotLoadedException;

    /** Perform method invocation with only the invoking thread resumed */
    static final int INVOKE_SINGLE_THREADED = 0x1;

    /**
     * Invokes the specified static {@link Method} in the
     * target VM. The
     * specified method can be defined in this class,
     * or in a superclass.
     * The method must be a static method
     * but not a static initializer.
     * Use {@link ClassType#newInstance} to create a new object and
     * run its constructor.
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
     * by specifying the {@link #INVOKE_SINGLE_THREADED}
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
     * a member of this class or a superclass, if the size of the argument list
     * does not match the number of declared arguments for the method, or
     * if the method is an initializer, constructor or static intializer.
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
     */
    Value invokeMethod(ThreadReference thread, Method method,
                       List<? extends Value> arguments, int options)
                                   throws InvalidTypeException,
                                          ClassNotLoadedException,
                                          IncompatibleThreadStateException,
                                          InvocationException;

    /**
     * Constructs a new instance of this type, using
     * the given constructor {@link Method} in the
     * target VM. The
     * specified constructor must be defined in this class.
     * <p>
     * Instance creation will occur in the specified thread.
     * Instance creation can occur only if the specified thread
     * has been suspended by an event which occurred in that thread.
     * Instance creation is not supported
     * when the target VM has been suspended through
     * {@link VirtualMachine#suspend} or when the specified thread
     * is suspended through {@link ThreadReference#suspend}.
     * <p>
     * The specified constructor is invoked with the arguments in the specified
     * argument list.  The invocation is synchronous; this method
     * does not return until the constructor returns in the target VM.
     * If the invoked method throws an
     * exception, this method will throw an {@link InvocationException}
     * which contains a mirror to the exception object thrown.
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
     * component type, followed by any number of other arguments of the same
     * type. If the argument is omitted, then a 0 length array of the
     * component type is passed.  The component type can be a primitive type.
     * Autoboxing is not supported.
     *
     * See section 5.2 of
     * <cite>The Java Language Specification</cite>
     * for more information on assignment compatibility.
     * <p>
     * By default, all threads in the target VM are resumed while
     * the method is being invoked if they were previously
     * suspended by an event or by {@link VirtualMachine#suspend} or
     * {@link ThreadReference#suspend}. This is done to prevent the deadlocks
     * that will occur if any of the threads own monitors
     * that will be needed by the invoked method. It is possible that
     * breakpoints or other events might occur during the invocation.
     * Note, however, that this implicit resume acts exactly like
     * {@link ThreadReference#resume}, so if the thread's suspend
     * count is greater than 1, it will remain in a suspended state
     * during the invocation. By default, when the invocation completes,
     * all threads in the target VM are suspended, regardless their state
     * before the invocation.
     * <p>
     * The resumption of other threads during the invocation can be prevented
     * by specifying the {@link #INVOKE_SINGLE_THREADED}
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
     * @param method the constructor {@link Method} to invoke.
     * @param arguments the list of {@link Value} arguments bound to the
     * invoked constructor. Values from the list are assigned to arguments
     * in the order they appear in the constructor signature.
     * @param options the integer bit flag options.
     * @return an {@link ObjectReference} mirror of the newly created
     * object.
     * @throws java.lang.IllegalArgumentException if the method is not
     * a member of this class, if the size of the argument list
     * does not match the number of declared arguments for the constructor,
     * or if the method is not a constructor.
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
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only
     * - see {@link VirtualMachine#canBeModified()}.
     */
    ObjectReference newInstance(ThreadReference thread, Method method,
                                List<? extends Value> arguments, int options)
                                   throws InvalidTypeException,
                                          ClassNotLoadedException,
                                          IncompatibleThreadStateException,
                                          InvocationException;

    /**
     * Returns a the single non-abstract {@link Method} visible from
     * this class that has the given name and signature.
     * See {@link ReferenceType#methodsByName(java.lang.String, java.lang.String)}
     * for information on signature format.
     * <p>
     * The returned method (if non-null) is a component of
     * {@link ClassType}.
     *
     * @see ReferenceType#visibleMethods
     * @see ReferenceType#methodsByName(java.lang.String name)
     * @see ReferenceType#methodsByName(java.lang.String name, java.lang.String signature)
     * @param name the name of the method to find.
     * @param signature the signature of the method to find
     * @return the {@link Method} that matches the given
     * name and signature or <code>null</code> if there is no match.
     * @throws ClassNotPreparedException if methods are not yet available
     * because the class has not yet been prepared.
     */
    Method concreteMethodByName(String name, String signature);
}
