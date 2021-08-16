/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.VMCannotBeModifiedException;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

/**
 * A supertype for ReferenceTypes allowing method invocations
 */
abstract class InvokableTypeImpl extends ReferenceTypeImpl {

    /**
     * The invocation result wrapper
     * It is necessary because both ClassType and InterfaceType
     * use their own type to represent the invocation result
     */
    static interface InvocationResult {
        ObjectReferenceImpl getException();
        ValueImpl getResult();
    }

    InvokableTypeImpl(VirtualMachine aVm, long aRef) {
        super(aVm, aRef);
    }

    /**
     * Method invocation support.
     * Shared by ClassType and InterfaceType
     * @param threadIntf the thread in which to invoke.
     * @param methodIntf method the {@link Method} to invoke.
     * @param origArguments the list of {@link Value} arguments bound to the
     * invoked method. Values from the list are assigned to arguments
     * in the order they appear in the method signature.
     * @param options the integer bit flag options.
     * @return a {@link Value} mirror of the invoked method's return value.
     * @throws java.lang.IllegalArgumentException if the method is not
     * a member of this type, if the size of the argument list
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
     *         loaded through the enclosing class's class loader.
     *         Primitive arguments must be either assignment compatible with the
     *         argument type or must be convertible to the argument type without loss
     *         of information. See JLS section 5.2 for more information on assignment
     *         compatibility.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    final public Value invokeMethod(ThreadReference threadIntf, Method methodIntf,
                                    List<? extends Value> origArguments, int options)
                                        throws InvalidTypeException,
                                               ClassNotLoadedException,
                                               IncompatibleThreadStateException,
                                               InvocationException {
        validateMirror(threadIntf);
        validateMirror(methodIntf);
        validateMirrorsOrNulls(origArguments);
        MethodImpl method = (MethodImpl) methodIntf;
        ThreadReferenceImpl thread = (ThreadReferenceImpl) threadIntf;
        validateMethodInvocation(method);
        List<? extends Value> arguments = method.validateAndPrepareArgumentsForInvoke(origArguments);
        ValueImpl[] args = arguments.toArray(new ValueImpl[0]);
        InvocationResult ret;
        try {
            PacketStream stream = sendInvokeCommand(thread, method, args, options);
            ret = waitForReply(stream);
        } catch (JDWPException exc) {
            if (exc.errorCode() == JDWP.Error.INVALID_THREAD) {
                throw new IncompatibleThreadStateException();
            } else {
                throw exc.toJDIException();
            }
        }
        /*
         * There is an implict VM-wide suspend at the conclusion
         * of a normal (non-single-threaded) method invoke
         */
        if ((options & ClassType.INVOKE_SINGLE_THREADED) == 0) {
            vm.notifySuspend();
        }
        if (ret.getException() != null) {
            throw new InvocationException(ret.getException());
        } else {
            return ret.getResult();
        }
    }

    @Override
    boolean isAssignableTo(ReferenceType type) {
        ClassTypeImpl superclazz = (ClassTypeImpl) superclass();
        if (this.equals(type)) {
            return true;
        } else if ((superclazz != null) && superclazz.isAssignableTo(type)) {
            return true;
        } else {
            List<InterfaceType> interfaces = interfaces();
            Iterator<InterfaceType> iter = interfaces.iterator();
            while (iter.hasNext()) {
                InterfaceTypeImpl interfaze = (InterfaceTypeImpl) iter.next();
                if (interfaze.isAssignableTo(type)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    final void addVisibleMethods(Map<String, Method> methodMap, Set<InterfaceType> seenInterfaces) {
        /*
         * Add methods from
         * parent types first, so that the methods in this class will
         * overwrite them in the hash table
         */
        Iterator<InterfaceType> iter = interfaces().iterator();
        while (iter.hasNext()) {
            InterfaceTypeImpl interfaze = (InterfaceTypeImpl) iter.next();
            if (!seenInterfaces.contains(interfaze)) {
                interfaze.addVisibleMethods(methodMap, seenInterfaces);
                seenInterfaces.add(interfaze);
            }
        }
        ClassTypeImpl clazz = (ClassTypeImpl) superclass();
        if (clazz != null) {
            clazz.addVisibleMethods(methodMap, seenInterfaces);
        }
        addToMethodMap(methodMap, methods());
    }

    final void addInterfaces(List<InterfaceType> list) {
        List<InterfaceType> immediate = interfaces();
        list.addAll(interfaces());
        Iterator<InterfaceType> iter = immediate.iterator();
        while (iter.hasNext()) {
            InterfaceTypeImpl interfaze = (InterfaceTypeImpl) iter.next();
            interfaze.addInterfaces(list);
        }
        ClassTypeImpl superclass = (ClassTypeImpl) superclass();
        if (superclass != null) {
            superclass.addInterfaces(list);
        }
    }

    /**
     * Returns all the implemented interfaces recursively
     * @return A list of all the implemented interfaces (recursively)
     */
    final List<InterfaceType> getAllInterfaces() {
        List<InterfaceType> all = new ArrayList<>();
        addInterfaces(all);
        return all;
    }

    /**
     * Shared implementation of {@linkplain ClassType#allMethods()} and
     * {@linkplain InterfaceType#allMethods()}
     * @return A list of all methods (recursively)
     */
    public final List<Method> allMethods() {
        ArrayList<Method> list = new ArrayList<>(methods());
        ClassType clazz = superclass();
        while (clazz != null) {
            list.addAll(clazz.methods());
            clazz = clazz.superclass();
        }
        /*
         * Avoid duplicate checking on each method by iterating through
         * duplicate-free allInterfaces() rather than recursing
         */
        for (InterfaceType interfaze : getAllInterfaces()) {
            list.addAll(interfaze.methods());
        }
        return list;
    }

    @Override
    final List<ReferenceType> inheritedTypes() {
        List<ReferenceType> inherited = new ArrayList<>();
        if (superclass() != null) {
            inherited.add(0, superclass()); /* insert at front */
        }
        for (ReferenceType rt : interfaces()) {
            inherited.add(rt);
        }
        return inherited;
    }

    private PacketStream sendInvokeCommand(final ThreadReferenceImpl thread,
                                           final MethodImpl method,
                                           final ValueImpl[] args,
                                           final int options) {

        CommandSender sender = getInvokeMethodSender(thread, method, args, options);
        PacketStream stream;
        if ((options & ClassType.INVOKE_SINGLE_THREADED) != 0) {
            stream = thread.sendResumingCommand(sender);
        } else {
            stream = vm.sendResumingCommand(sender);
        }
        return stream;
    }

    private void validateMethodInvocation(Method method)
                                            throws InvalidTypeException,
                                                   InvocationException {
        if (!canInvoke(method)) {
            throw new IllegalArgumentException("Invalid method");
        }
        /*
         * Method must be a static and not a static initializer
         */
        if (!method.isStatic()) {
            throw new IllegalArgumentException("Cannot invoke instance method on a class/interface type");
        } else if (method.isStaticInitializer()) {
            throw new IllegalArgumentException("Cannot invoke static initializer");
        }
    }

    /**
     * A subclass will provide specific {@linkplain CommandSender}
     * @param thread the current invocation thread
     * @param method the method to invoke
     * @param args the arguments to pass to the method
     * @param options the integer bit flag options
     * @return the specific {@literal CommandSender} instance
     */
    abstract CommandSender getInvokeMethodSender(ThreadReferenceImpl thread,
                                                 MethodImpl method,
                                                 ValueImpl[] args,
                                                 int options);

    /**
     * Waits for the reply to the last sent command
     * @param stream the stream to listen for the reply on
     * @return the {@linkplain InvocationResult} instance
     * @throws JDWPException when something goes wrong in JDWP
     */
    abstract InvocationResult waitForReply(PacketStream stream) throws JDWPException;

    /**
     * Get the {@linkplain ReferenceType} superclass
     * @return the superclass or null
     */
    abstract ClassType superclass();

    /**
     * Get the implemented/extended interfaces
     * @return the list of implemented/extended interfaces
     */
    abstract List<InterfaceType> interfaces();

    /**
     * Checks the provided method whether it can be invoked
     * @param method the method to check
     * @return {@code TRUE} if the implementation knows how to invoke the method,
     *         {@code FALSE} otherwise
     */
    abstract boolean canInvoke(Method method);
}
