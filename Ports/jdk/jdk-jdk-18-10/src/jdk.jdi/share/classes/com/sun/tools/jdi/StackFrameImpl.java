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

package com.sun.tools.jdi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ClassNotLoadedException;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.InternalException;
import com.sun.jdi.InvalidStackFrameException;
import com.sun.jdi.InvalidTypeException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;

public class StackFrameImpl extends MirrorImpl
                            implements StackFrame, ThreadListener
{
    /* Once false, frame should not be used.
     * access synchronized on (vm.state())
     */
    private boolean isValid = true;

    private final ThreadReferenceImpl thread;
    private final long id;
    private final Location location;
    private Map<String, LocalVariable> visibleVariables =  null;
    private ObjectReference thisObject = null;

    StackFrameImpl(VirtualMachine vm, ThreadReferenceImpl thread,
                   long id, Location location) {
        super(vm);
        this.thread = thread;
        this.id = id;
        this.location = location;
        thread.addListener(this);
    }

    /*
     * ThreadListener implementation
     * Must be synchronized since we must protect against
     * sending defunct (isValid == false) stack ids to the back-end.
     */
    public boolean threadResumable(ThreadAction action) {
        synchronized (vm.state()) {
            if (isValid) {
                isValid = false;
                return false;   /* remove this stack frame as a listener */
            } else {
                throw new InternalException(
                                  "Invalid stack frame thread listener");
            }
        }
    }

    void validateStackFrame() {
        if (!isValid) {
            throw new InvalidStackFrameException("Thread has been resumed");
        }
    }

    /**
     * Return the frame location.
     * Need not be synchronized since it cannot be provably stale.
     */
    public Location location() {
        validateStackFrame();
        return location;
    }

    /**
     * Return the thread holding the frame.
     * Need not be synchronized since it cannot be provably stale.
     */
    public ThreadReference thread() {
        validateStackFrame();
        return thread;
    }

    public boolean equals(Object obj) {
        if ((obj != null) && (obj instanceof StackFrameImpl)) {
            StackFrameImpl other = (StackFrameImpl)obj;
            return (id == other.id) &&
                   (thread().equals(other.thread())) &&
                   (location().equals(other.location())) &&
                    super.equals(obj);
        } else {
            return false;
        }
    }

    public int hashCode() {
        return (thread().hashCode() << 4) + ((int)id);
    }

    public ObjectReference thisObject() {
        validateStackFrame();
        MethodImpl currentMethod = (MethodImpl)location.method();
        if (currentMethod.isStatic() || currentMethod.isNative()) {
            return null;
        } else {
            if (thisObject == null) {
                PacketStream ps;

                /* protect against defunct frame id */
                synchronized (vm.state()) {
                    validateStackFrame();
                    ps = JDWP.StackFrame.ThisObject.
                                      enqueueCommand(vm, thread, id);
                }

                /* actually get it, now that order is guaranteed */
                try {
                    thisObject = JDWP.StackFrame.ThisObject.
                                      waitForReply(vm, ps).objectThis;
                } catch (JDWPException exc) {
                    switch (exc.errorCode()) {
                    case JDWP.Error.INVALID_FRAMEID:
                    case JDWP.Error.THREAD_NOT_SUSPENDED:
                    case JDWP.Error.INVALID_THREAD:
                        throw new InvalidStackFrameException();
                    default:
                        throw exc.toJDIException();
                    }
                }
            }
        }
        return thisObject;
    }

    /**
     * Build the visible variable map.
     * Need not be synchronized since it cannot be provably stale.
     */
    private void createVisibleVariables() throws AbsentInformationException {
        if (visibleVariables == null) {
            List<LocalVariable> allVariables = location.method().variables();
            Map<String, LocalVariable> map = new HashMap<>(allVariables.size());

            for (LocalVariable variable : allVariables) {
                String name = variable.name();
                if (variable.isVisible(this)) {
                    LocalVariable existing = map.get(name);
                    if ((existing == null) ||
                        ((LocalVariableImpl)variable).hides(existing)) {
                        map.put(name, variable);
                    }
                }
            }
            visibleVariables = map;
        }
    }

    /**
     * Return the list of visible variable in the frame.
     * Need not be synchronized since it cannot be provably stale.
     */
    public List<LocalVariable> visibleVariables() throws AbsentInformationException {
        validateStackFrame();
        createVisibleVariables();
        List<LocalVariable> mapAsList = new ArrayList<>(visibleVariables.values());
        Collections.sort(mapAsList);
        return mapAsList;
    }

    /**
     * Return a particular variable in the frame.
     * Need not be synchronized since it cannot be provably stale.
     */
    public LocalVariable visibleVariableByName(String name) throws AbsentInformationException  {
        validateStackFrame();
        createVisibleVariables();
        return visibleVariables.get(name);
    }

    public Value getValue(LocalVariable variable) {
        List<LocalVariable> list = new ArrayList<>(1);
        list.add(variable);
        return getValues(list).get(variable);
    }

    public Map<LocalVariable, Value> getValues(List<? extends LocalVariable> variables) {
        validateStackFrame();
        validateMirrors(variables);

        int count = variables.size();
        JDWP.StackFrame.GetValues.SlotInfo[] slots =
                           new JDWP.StackFrame.GetValues.SlotInfo[count];

        for (int i=0; i<count; ++i) {
            LocalVariableImpl variable = (LocalVariableImpl)variables.get(i);
            if (!variable.isVisible(this)) {
                throw new IllegalArgumentException(variable.name() +
                                 " is not valid at this frame location");
            }
            slots[i] = new JDWP.StackFrame.GetValues.SlotInfo(variable.slot(),
                                      (byte)variable.signature().charAt(0));
        }

        PacketStream ps;

        /* protect against defunct frame id */
        synchronized (vm.state()) {
            validateStackFrame();
            ps = JDWP.StackFrame.GetValues.enqueueCommand(vm, thread, id, slots);
        }

        /* actually get it, now that order is guaranteed */
        ValueImpl[] values;
        try {
            values = JDWP.StackFrame.GetValues.waitForReply(vm, ps).values;
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
                case JDWP.Error.INVALID_FRAMEID:
                case JDWP.Error.THREAD_NOT_SUSPENDED:
                case JDWP.Error.INVALID_THREAD:
                    throw new InvalidStackFrameException();
                default:
                    throw exc.toJDIException();
            }
        }

        if (count != values.length) {
            throw new InternalException(
                      "Wrong number of values returned from target VM");
        }
        Map<LocalVariable, Value> map = new HashMap<>(count);
        for (int i = 0; i < count; ++i) {
            LocalVariableImpl variable = (LocalVariableImpl)variables.get(i);
            map.put(variable, values[i]);
        }
        return map;
    }

    public void setValue(LocalVariable variableIntf, Value valueIntf)
        throws InvalidTypeException, ClassNotLoadedException {

        validateStackFrame();
        validateMirror(variableIntf);
        validateMirrorOrNull(valueIntf);

        LocalVariableImpl variable = (LocalVariableImpl)variableIntf;
        ValueImpl value = (ValueImpl)valueIntf;

        if (!variable.isVisible(this)) {
            throw new IllegalArgumentException(variable.name() +
                             " is not valid at this frame location");
        }

        try {
            // Validate and convert value if necessary
            value = ValueImpl.prepareForAssignment(value, variable);

            JDWP.StackFrame.SetValues.SlotInfo[] slotVals =
                new JDWP.StackFrame.SetValues.SlotInfo[1];
            slotVals[0] = new JDWP.StackFrame.SetValues.
                                       SlotInfo(variable.slot(), value);

            PacketStream ps;

            /* protect against defunct frame id */
            synchronized (vm.state()) {
                validateStackFrame();
                ps = JDWP.StackFrame.SetValues.
                                     enqueueCommand(vm, thread, id, slotVals);
            }

            /* actually set it, now that order is guaranteed */
            try {
                JDWP.StackFrame.SetValues.waitForReply(vm, ps);
            } catch (JDWPException exc) {
                switch (exc.errorCode()) {
                case JDWP.Error.INVALID_FRAMEID:
                case JDWP.Error.THREAD_NOT_SUSPENDED:
                case JDWP.Error.INVALID_THREAD:
                    throw new InvalidStackFrameException();
                default:
                    throw exc.toJDIException();
                }
            }
        } catch (ClassNotLoadedException e) {
            /*
             * Since we got this exception,
             * the variable type must be a reference type. The value
             * we're trying to set is null, but if the variable's
             * class has not yet been loaded through the enclosing
             * class loader, then setting to null is essentially a
             * no-op, and we should allow it without an exception.
             */
            if (value != null) {
                throw e;
            }
        }
    }

    public List<Value> getArgumentValues() {
        validateStackFrame();
        MethodImpl mmm = (MethodImpl)location.method();
        List<String> argSigs = mmm.argumentSignatures();
        int count = argSigs.size();
        JDWP.StackFrame.GetValues.SlotInfo[] slots =
                           new JDWP.StackFrame.GetValues.SlotInfo[count];

        int slot;
        if (mmm.isStatic()) {
            slot = 0;
        } else {
            slot = 1;
        }
        for (int ii = 0; ii < count; ++ii) {
            char sigChar = argSigs.get(ii).charAt(0);
            slots[ii] = new JDWP.StackFrame.GetValues.SlotInfo(slot++,(byte)sigChar);
            if (sigChar == 'J' || sigChar == 'D') {
                slot++;
            }
        }

        PacketStream ps;

        /* protect against defunct frame id */
        synchronized (vm.state()) {
            validateStackFrame();
            ps = JDWP.StackFrame.GetValues.enqueueCommand(vm, thread, id, slots);
        }

        ValueImpl[] values;
        try {
            values = JDWP.StackFrame.GetValues.waitForReply(vm, ps).values;
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
                case JDWP.Error.INVALID_FRAMEID:
                case JDWP.Error.THREAD_NOT_SUSPENDED:
                case JDWP.Error.INVALID_THREAD:
                    throw new InvalidStackFrameException();
                default:
                    throw exc.toJDIException();
            }
        }

        if (count != values.length) {
            throw new InternalException(
                      "Wrong number of values returned from target VM");
        }
        return Arrays.asList((Value[])values);
    }

    void pop() throws IncompatibleThreadStateException {
        validateStackFrame();
        // flush caches and disable caching until command completion
        CommandSender sender =
            new CommandSender() {
                public PacketStream send() {
                    return JDWP.StackFrame.PopFrames.enqueueCommand(vm,
                                 thread, id);
                }
        };
        try {
            PacketStream stream = thread.sendResumingCommand(sender);
            JDWP.StackFrame.PopFrames.waitForReply(vm, stream);
        } catch (JDWPException exc) {
            switch (exc.errorCode()) {
            case JDWP.Error.THREAD_NOT_SUSPENDED:
                throw new IncompatibleThreadStateException(
                         "Thread not current or suspended");
            case JDWP.Error.INVALID_THREAD:   /* zombie */
                throw new IncompatibleThreadStateException("zombie");
            case JDWP.Error.NO_MORE_FRAMES:
                throw new InvalidStackFrameException(
                         "No more frames on the stack");
            default:
                throw exc.toJDIException();
            }
        }

        // enable caching - suspended again
        vm.state().freeze();
    }

    public String toString() {
       return location.toString() + " in thread " + thread.toString();
    }
}
