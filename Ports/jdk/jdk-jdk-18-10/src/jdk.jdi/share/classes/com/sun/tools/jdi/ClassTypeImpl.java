/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.util.List;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

final public class ClassTypeImpl extends InvokableTypeImpl
                                 implements ClassType
{
    private static class IResult implements InvocationResult {
        final private JDWP.ClassType.InvokeMethod rslt;

        public IResult(JDWP.ClassType.InvokeMethod rslt) {
            this.rslt = rslt;
        }

        @Override
        public ObjectReferenceImpl getException() {
            return rslt.exception;
        }

        @Override
        public ValueImpl getResult() {
            return rslt.returnValue;
        }
    }

    private boolean cachedSuperclass = false;
    private ClassType superclass = null;
    private List<InterfaceType> interfaces = null;

    protected ClassTypeImpl(VirtualMachine aVm, long aRef) {
        super(aVm, aRef);
    }

    public ClassType superclass() {
        if (!cachedSuperclass)  {
            ClassTypeImpl sup = null;
            try {
                sup = JDWP.ClassType.Superclass.
                    process(vm, this).superclass;
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }

            /*
             * If there is a superclass, cache its
             * ClassType here. Otherwise,
             * leave the cache reference null.
             */
            if (sup != null) {
                superclass = sup;
            }
            cachedSuperclass = true;
        }

        return superclass;
    }

    @Override
    public List<InterfaceType> interfaces()  {
        if (interfaces == null) {
            interfaces = getInterfaces();
        }
        return interfaces;
    }

    @Override
    public List<InterfaceType> allInterfaces() {
        return getAllInterfaces();
    }

    public List<ClassType> subclasses() {
        List<ClassType> subs = new ArrayList<>();
        vm.forEachClass(refType -> {
            if (refType instanceof ClassType) {
                ClassType clazz = (ClassType)refType;
                ClassType superclass = clazz.superclass();
                if ((superclass != null) && superclass.equals(this)) {
                    subs.add(clazz);
                }
            }
        });
        return subs;
    }

    public boolean isEnum() {
        ClassType superclass = superclass();
        if (superclass != null &&
            superclass.name().equals("java.lang.Enum")) {
            return true;
        }
        return false;
    }

    public void setValue(Field field, Value value)
        throws InvalidTypeException, ClassNotLoadedException {

        validateMirror(field);
        validateMirrorOrNull(value);
        validateFieldSet(field);

        // More validation specific to setting from a ClassType
        if(!field.isStatic()) {
            throw new IllegalArgumentException(
                            "Must set non-static field through an instance");
        }

        try {
            JDWP.ClassType.SetValues.FieldValue[] values =
                          new JDWP.ClassType.SetValues.FieldValue[1];
            values[0] = new JDWP.ClassType.SetValues.FieldValue(
                    ((FieldImpl)field).ref(),
                    // validate and convert if necessary
                    ValueImpl.prepareForAssignment(value, (FieldImpl)field));

            try {
                JDWP.ClassType.SetValues.process(vm, this, values);
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        } catch (ClassNotLoadedException e) {
            /*
             * Since we got this exception,
             * the field type must be a reference type. The value
             * we're trying to set is null, but if the field's
             * class has not yet been loaded through the enclosing
             * class loader, then setting to null is essentially a
             * no-op, and we should allow it without an exception.
             */
            if (value != null) {
                throw e;
            }
        }
    }

    PacketStream sendNewInstanceCommand(final ThreadReferenceImpl thread,
                                        final MethodImpl method,
                                        final ValueImpl[] args,
                                        final int options) {
        CommandSender sender =
            new CommandSender() {
                public PacketStream send() {
                    return JDWP.ClassType.NewInstance.enqueueCommand(
                                          vm, ClassTypeImpl.this, thread,
                                          method.ref(), args, options);
                }
        };

        PacketStream stream;
        if ((options & INVOKE_SINGLE_THREADED) != 0) {
            stream = thread.sendResumingCommand(sender);
        } else {
            stream = vm.sendResumingCommand(sender);
        }
        return stream;
    }

    public ObjectReference newInstance(ThreadReference threadIntf,
                                       Method methodIntf,
                                       List<? extends Value> origArguments,
                                       int options)
                                   throws InvalidTypeException,
                                          ClassNotLoadedException,
                                          IncompatibleThreadStateException,
                                          InvocationException {
        validateMirror(threadIntf);
        validateMirror(methodIntf);
        validateMirrorsOrNulls(origArguments);

        MethodImpl method = (MethodImpl)methodIntf;
        ThreadReferenceImpl thread = (ThreadReferenceImpl)threadIntf;

        validateConstructorInvocation(method);

        List<Value> arguments = method.validateAndPrepareArgumentsForInvoke(
                                                       origArguments);
        ValueImpl[] args = arguments.toArray(new ValueImpl[0]);
        JDWP.ClassType.NewInstance ret = null;
        try {
            PacketStream stream =
                sendNewInstanceCommand(thread, method, args, options);
            ret = JDWP.ClassType.NewInstance.waitForReply(vm, stream);
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
        if ((options & INVOKE_SINGLE_THREADED) == 0) {
            vm.notifySuspend();
        }

        if (ret.exception != null) {
            throw new InvocationException(ret.exception);
        } else {
            return ret.newObject;
        }
    }

    public Method concreteMethodByName(String name, String signature)  {
        Method method = null;
        for (Method candidate : visibleMethods()) {
            if (candidate.name().equals(name) &&
                candidate.signature().equals(signature) &&
                !candidate.isAbstract()) {

                method = candidate;
                break;
            }
        }
        return method;
    }

    void validateConstructorInvocation(Method method)
                                   throws InvalidTypeException,
                                          InvocationException {
        /*
         * Method must be in this class.
         */
        ReferenceTypeImpl declType = (ReferenceTypeImpl)method.declaringType();
        if (!declType.equals(this)) {
            throw new IllegalArgumentException("Invalid constructor");
        }

        /*
         * Method must be a constructor
         */
        if (!method.isConstructor()) {
            throw new IllegalArgumentException("Cannot create instance with non-constructor");
        }
    }

    public String toString() {
       return "class " + name() + " (" + loaderString() + ")";
    }

    @Override
    CommandSender getInvokeMethodSender(ThreadReferenceImpl thread,
                                        MethodImpl method,
                                        ValueImpl[] args,
                                        int options) {
        return () ->
            JDWP.ClassType.InvokeMethod.enqueueCommand(vm,
                                                       ClassTypeImpl.this,
                                                       thread,
                                                       method.ref(),
                                                       args,
                                                       options);
    }

    @Override
    InvocationResult waitForReply(PacketStream stream) throws JDWPException {
        return new IResult(JDWP.ClassType.InvokeMethod.waitForReply(vm, stream));
    }

    @Override
    boolean canInvoke(Method method) {
        // Method must be in this class or a superclass.
        return ((ReferenceTypeImpl)method.declaringType()).isAssignableFrom(this);
    }
}
