/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InterfaceType;
import com.sun.jdi.InternalException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class ObjectReferenceImpl extends ValueImpl
             implements ObjectReference, VMListener
{
    protected long ref;
    private ReferenceType type = null;
    private int gcDisableCount = 0;
    boolean addedListener = false;

    // This is cached only while the VM is suspended
    protected static class Cache {
        JDWP.ObjectReference.MonitorInfo monitorInfo = null;
    }

    private static final Cache noInitCache = new Cache();
    private static final Cache markerCache = new Cache();
    private Cache cache = noInitCache;

    private void disableCache() {
        synchronized (vm.state()) {
            cache = null;
        }
    }

    private void enableCache() {
        synchronized (vm.state()) {
            cache = markerCache;
        }
    }

    // Override in subclasses
    protected Cache newCache() {
        return new Cache();
    }

    protected Cache getCache() {
        synchronized (vm.state()) {
            if (cache == noInitCache) {
                if (vm.state().isSuspended()) {
                    // Set cache now, otherwise newly created objects are
                    // not cached until resuspend
                    enableCache();
                } else {
                    disableCache();
                }
            }
            if (cache == markerCache) {
                cache = newCache();
            }
            return cache;
        }
    }

    // Return the ClassTypeImpl upon which to invoke a method.
    // By default it is our very own referenceType() but subclasses
    // can override.
    protected ClassTypeImpl invokableReferenceType(Method method) {
        return (ClassTypeImpl)referenceType();
    }

    ObjectReferenceImpl(VirtualMachine aVm,long aRef) {
        super(aVm);

        ref = aRef;
    }

    protected String description() {
        return "ObjectReference " + uniqueID();
    }

    /*
     * VMListener implementation
     */
    public boolean vmSuspended(VMAction action) {
        enableCache();
        return true;
    }

    public boolean vmNotSuspended(VMAction action) {
        // make sure that cache and listener management are synchronized
        synchronized (vm.state()) {
            if (cache != null && (vm.traceFlags & VirtualMachine.TRACE_OBJREFS) != 0) {
                vm.printTrace("Clearing temporary cache for " + description());
            }
            disableCache();
            if (addedListener) {
                /*
                 * If a listener was added (i.e. this is not a
                 * ObjectReference that adds a listener on startup),
                 * remove it here.
                 */
                addedListener = false;
                return false;  // false says remove
            } else {
                return true;
            }
        }
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof ObjectReferenceImpl)) {
            ObjectReferenceImpl other = (ObjectReferenceImpl)obj;
            return (ref() == other.ref()) &&
                   super.equals(obj);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return(int)ref();
    }

    public Type type() {
        return referenceType();
    }

    public ReferenceType referenceType() {
        if (type == null) {
            try {
                JDWP.ObjectReference.ReferenceType rtinfo =
                    JDWP.ObjectReference.ReferenceType.process(vm, this);
                type = vm.referenceType(rtinfo.typeID,
                                        rtinfo.refTypeTag);
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        }
        return type;
    }

    public Value getValue(Field sig) {
        List<Field> list = new ArrayList<>(1);
        list.add(sig);
        Map<Field, Value> map = getValues(list);
        return map.get(sig);
    }

    public Map<Field,Value> getValues(List<? extends Field> theFields) {
        validateMirrors(theFields);

        List<Field> staticFields = new ArrayList<>(0);
        int size = theFields.size();
        List<Field> instanceFields = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Field field = theFields.get(i);

            // Make sure the field is valid
            ((ReferenceTypeImpl)referenceType()).validateFieldAccess(field);

            // FIX ME! We need to do some sanity checking
            // here; make sure the field belongs to this
            // object.
            if (field.isStatic())
                staticFields.add(field);
            else {
                instanceFields.add(field);
            }
        }

        Map<Field, Value> map;
        if (staticFields.size() > 0) {
            map = referenceType().getValues(staticFields);
        } else {
            map = new HashMap<Field, Value>(size);
        }

        size = instanceFields.size();

        JDWP.ObjectReference.GetValues.Field[] queryFields =
                         new JDWP.ObjectReference.GetValues.Field[size];
        for (int i=0; i<size; i++) {
            FieldImpl field = (FieldImpl)instanceFields.get(i);/* thanks OTI */
            queryFields[i] = new JDWP.ObjectReference.GetValues.Field(
                                         field.ref());
        }
        ValueImpl[] values;
        try {
            values = JDWP.ObjectReference.GetValues.
                                     process(vm, this, queryFields).values;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }

        if (size != values.length) {
            throw new InternalException(
                         "Wrong number of values returned from target VM");
        }
        for (int i=0; i<size; i++) {
            FieldImpl field = (FieldImpl)instanceFields.get(i);
            map.put(field, values[i]);
        }

        return map;
    }

    public void setValue(Field field, Value value)
                   throws InvalidTypeException, ClassNotLoadedException {

        validateMirror(field);
        validateMirrorOrNull(value);

        // Make sure the field is valid
        ((ReferenceTypeImpl)referenceType()).validateFieldSet(field);

        if (field.isStatic()) {
            ReferenceType type = referenceType();
            if (type instanceof ClassType) {
                ((ClassType)type).setValue(field, value);
                return;
            } else {
                throw new IllegalArgumentException(
                                    "Invalid type for static field set");
            }
        }

        try {
            JDWP.ObjectReference.SetValues.FieldValue[] fvals =
                      new JDWP.ObjectReference.SetValues.FieldValue[1];
            fvals[0] = new JDWP.ObjectReference.SetValues.FieldValue(
                           ((FieldImpl)field).ref(),
                           // Validate and convert if necessary
                           ValueImpl.prepareForAssignment(value,
                                                          (FieldImpl)field));
            try {
                JDWP.ObjectReference.SetValues.process(vm, this, fvals);
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

    void validateMethodInvocation(Method method, int options)
                                         throws InvalidTypeException,
                                         InvocationException {
        /*
         * Method must be in this object's class, a superclass, or
         * implemented interface
         */
        ReferenceTypeImpl declType = (ReferenceTypeImpl)method.declaringType();

        if (!declType.isAssignableFrom(this)) {
            throw new IllegalArgumentException("Invalid method");
        }

        if (declType instanceof ClassTypeImpl) {
            validateClassMethodInvocation(method, options);
        } else if (declType instanceof InterfaceTypeImpl) {
            validateIfaceMethodInvocation(method, options);
        } else {
            throw new InvalidTypeException();
        }
    }

    void validateClassMethodInvocation(Method method, int options)
                                         throws InvalidTypeException,
                                         InvocationException {
        /*
         * Method must be a non-constructor
         */
        if (method.isConstructor()) {
            throw new IllegalArgumentException("Cannot invoke constructor");
        }

        /*
         * For nonvirtual invokes, method must have a body
         */
        if (isNonVirtual(options)) {
            if (method.isAbstract()) {
                throw new IllegalArgumentException("Abstract method");
            }
        }
    }

    void validateIfaceMethodInvocation(Method method, int options)
                                         throws InvalidTypeException,
                                         InvocationException {
        /*
         * For nonvirtual invokes, method must have a body
         */
        if (isNonVirtual(options)) {
            if (method.isAbstract()) {
                throw new IllegalArgumentException("Abstract method");
            }
        }
    }

    PacketStream sendInvokeCommand(final ThreadReferenceImpl thread,
                                   final ClassTypeImpl refType,
                                   final MethodImpl method,
                                   final ValueImpl[] args,
                                   final int options) {
        CommandSender sender =
            new CommandSender() {
                public PacketStream send() {
                    return JDWP.ObjectReference.InvokeMethod.enqueueCommand(
                                          vm, ObjectReferenceImpl.this,
                                          thread, refType,
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

    public Value invokeMethod(ThreadReference threadIntf, Method methodIntf,
                              List<? extends Value> origArguments, int options)
                              throws InvalidTypeException,
                                     IncompatibleThreadStateException,
                                     InvocationException,
                                     ClassNotLoadedException {

        validateMirror(threadIntf);
        validateMirror(methodIntf);
        validateMirrorsOrNulls(origArguments);

        MethodImpl method = (MethodImpl)methodIntf;
        ThreadReferenceImpl thread = (ThreadReferenceImpl)threadIntf;

        if (method.isStatic()) {
            if (referenceType() instanceof InterfaceType) {
                InterfaceType type = (InterfaceType)referenceType();
                return type.invokeMethod(thread, method, origArguments, options);
            } else if (referenceType() instanceof ClassType) {
                ClassType type = (ClassType)referenceType();
                return type.invokeMethod(thread, method, origArguments, options);
            } else {
                throw new IllegalArgumentException("Invalid type for static method invocation");
            }
        }

        validateMethodInvocation(method, options);

        List<Value> arguments = method.validateAndPrepareArgumentsForInvoke(
                                                  origArguments);

        ValueImpl[] args = arguments.toArray(new ValueImpl[0]);
        JDWP.ObjectReference.InvokeMethod ret;
        try {
            PacketStream stream =
                sendInvokeCommand(thread, invokableReferenceType(method),
                                  method, args, options);
            ret = JDWP.ObjectReference.InvokeMethod.waitForReply(vm, stream);
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
            return ret.returnValue;
        }
    }

    /* leave synchronized to keep count accurate */
    public synchronized void disableCollection() {
        if (gcDisableCount == 0) {
            try {
                JDWP.ObjectReference.DisableCollection.process(vm, this);
            } catch (JDWPException exc) {
                throw exc.toJDIException();
            }
        }
        gcDisableCount++;
    }

    /* leave synchronized to keep count accurate */
    public synchronized void enableCollection() {
        gcDisableCount--;

        if (gcDisableCount == 0) {
            try {
                JDWP.ObjectReference.EnableCollection.process(vm, this);
            } catch (JDWPException exc) {
                // If already collected, no harm done, no exception
                if (exc.errorCode() != JDWP.Error.INVALID_OBJECT) {
                    throw exc.toJDIException();
                }
                return;
            }
        }
    }

    public boolean isCollected() {
        try {
            return JDWP.ObjectReference.IsCollected.process(vm, this).
                                                              isCollected;
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    public long uniqueID() {
        return ref();
    }

    JDWP.ObjectReference.MonitorInfo jdwpMonitorInfo()
                             throws IncompatibleThreadStateException {
        JDWP.ObjectReference.MonitorInfo info = null;
        try {
            Cache local;

            // getCache() and addlistener() must be synchronized
            // so that no events are lost.
            synchronized (vm.state()) {
                local = getCache();

                if (local != null) {
                    info = local.monitorInfo;

                    // Check if there will be something to cache
                    // and there is not already a listener
                    if (info == null && !vm.state().hasListener(this)) {
                        /* For other, less numerous objects, this is done
                         * in the constructor. Since there can be many
                         * ObjectReferences, the VM listener is installed
                         * and removed as needed.
                         * Listener must be installed before process()
                         */
                        vm.state().addListener(this);
                        addedListener = true;
                    }
                }
            }
            if (info == null) {
                info = JDWP.ObjectReference.MonitorInfo.process(vm, this);
                if (local != null) {
                    local.monitorInfo = info;
                    if ((vm.traceFlags & VirtualMachine.TRACE_OBJREFS) != 0) {
                        vm.printTrace("ObjectReference " + uniqueID() +
                                      " temporarily caching monitor info");
                    }
                }
            }
        } catch (JDWPException exc) {
             if (exc.errorCode() == JDWP.Error.THREAD_NOT_SUSPENDED) {
                 throw new IncompatibleThreadStateException();
             } else {
                 throw exc.toJDIException();
             }
         }
        return info;
    }

    public List<ThreadReference> waitingThreads() throws IncompatibleThreadStateException {
        return Arrays.asList((ThreadReference[])jdwpMonitorInfo().waiters);
    }

    public ThreadReference owningThread() throws IncompatibleThreadStateException {
        return jdwpMonitorInfo().owner;
    }

    public int entryCount() throws IncompatibleThreadStateException {
        return jdwpMonitorInfo().entryCount;
    }


    public List<ObjectReference> referringObjects(long maxReferrers) {
        if (!vm.canGetInstanceInfo()) {
            throw new UnsupportedOperationException(
                "target does not support getting referring objects");
        }

        if (maxReferrers < 0) {
            throw new IllegalArgumentException("maxReferrers is less than zero: "
                                              + maxReferrers);
        }

        int intMax = (maxReferrers > Integer.MAX_VALUE)?
            Integer.MAX_VALUE: (int)maxReferrers;
        // JDWP can't currently handle more than this (in mustang)

        try {
            return Arrays.asList((ObjectReference[])JDWP.ObjectReference.ReferringObjects.
                                process(vm, this, intMax).referringObjects);
        } catch (JDWPException exc) {
            throw exc.toJDIException();
        }
    }

    long ref() {
        return ref;
    }

    boolean isClassObject() {
        /*
         * Don't need to worry about subclasses since java.lang.Class is final.
         */
        return referenceType().name().equals("java.lang.Class");
    }

    ValueImpl prepareForAssignmentTo(ValueContainer destination)
                                 throws InvalidTypeException,
                                        ClassNotLoadedException {

        validateAssignment(destination);
        return this;            // conversion never necessary
    }

    void validateAssignment(ValueContainer destination)
                            throws InvalidTypeException, ClassNotLoadedException {

        /*
         * Do these simpler checks before attempting a query of the destination's
         * type which might cause a confusing ClassNotLoadedException if
         * the destination is primitive or an array.
         */

        JNITypeParser destSig = new JNITypeParser(destination.signature());
        if (destSig.isPrimitive()) {
            throw new InvalidTypeException("Can't assign object value to primitive");
        }
        if (destSig.isArray()) {
            JNITypeParser sourceSig = new JNITypeParser(type().signature());
            if (!sourceSig.isArray()) {
                throw new InvalidTypeException("Can't assign non-array value to an array");
            }
        }
        if (destSig.isVoid()) {
            throw new InvalidTypeException("Can't assign object value to a void");
        }

        // Validate assignment
        ReferenceType destType = (ReferenceTypeImpl)destination.type();
        ReferenceTypeImpl myType = (ReferenceTypeImpl)referenceType();
        if (!myType.isAssignableTo(destType)) {
            JNITypeParser parser = new JNITypeParser(destType.signature());
            String destTypeName = parser.typeName();
            throw new InvalidTypeException("Can't assign " +
                                           type().name() +
                                           " to " + destTypeName);
        }
    }

    public String toString() {
        return "instance of " + referenceType().name() + "(id=" + uniqueID() + ")";
    }

    byte typeValueKey() {
        return JDWP.Tag.OBJECT;
    }

    private static boolean isNonVirtual(int options) {
        return (options & INVOKE_NONVIRTUAL) != 0;
    }
}
