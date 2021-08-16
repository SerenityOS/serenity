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
import java.util.Map;

import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.VMDisconnectEvent;

/**
 * The state of one method invocation on a thread's call stack.
 * As a thread executes, stack frames are pushed and popped from
 * its call stack as methods are invoked and then return. A StackFrame
 * mirrors one such frame from a target VM at some point in its
 * thread's execution. The call stack is, then, simply a List of
 * StackFrame objects. The call stack can be obtained any time a thread
 * is suspended through a call to {@link ThreadReference#frames}
 * <p>
 * StackFrames provide access to a method's local variables and their
 * current values.
 * <p>
 * The lifetime of a StackFrame is very limited. It is available only
 * for suspended threads and becomes invalid once its thread is resumed.
 * <p>
 * Any method on <code>StackFrame</code> which
 * takes <code>StackFrame</code> as an parameter may throw
 * {@link VMDisconnectedException} if the target VM is
 * disconnected and the {@link VMDisconnectEvent} has been or is
 * available to be read from the {@link EventQueue}.
 * <p>
 * Any method on <code>StackFrame</code> which
 * takes <code>StackFrame</code> as an parameter may throw
 * {@link VMOutOfMemoryException} if the target VM has run out of memory.
 *
 * @author Robert Field
 * @author Gordon Hirsch
 * @author James McIlree
 * @since  1.3
 */
public interface StackFrame extends Mirror, Locatable {

    /**
     * Returns the {@link Location} of the current instruction in the frame.
     * The method for which this frame was created can also be accessed
     * through the returned location.
     * For the top frame in the stack, this location identifies the
     * next instruction to be executed. For all other frames, this
     * location identifies the instruction that caused the next frame's
     * method to be invoked.
     * If the frame represents a native method invocation, the returned
     * location indicates the class and method, but the code index will
     * not be valid (-1).
     *
     * @return the {@link Location} of the current instruction.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     */
    Location location();

    /**
     * Returns the thread under which this frame's method is running.
     *
     * @return a {@link ThreadReference} which mirrors the frame's thread.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     */
    ThreadReference thread();

    /**
     * Returns the value of 'this' for the current frame.
     * The {@link ObjectReference} for 'this' is only available for
     * non-native instance methods.
     *
     * @return an {@link ObjectReference}, or null if the frame represents
     * a native or static method.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     */
    ObjectReference thisObject();

    /**
     * Returns a list containing each {@link LocalVariable}
     * that can be accessed from this frame's location.
     * <p>
     * Visibility is based on the code index of the current instruction of
     * this StackFrame. Each variable has a range of byte code indices in which
     * it is accessible.
     * If this stack frame's method
     * matches this variable's method and if the code index of this
     * StackFrame is within the variable's byte code range, the variable is
     * visible.
     * <p>
     * A variable's byte code range is at least as large as the scope of
     * that variable, but can continue beyond the end of the scope under
     * certain circumstances:
     * <ul>
     * <li>the compiler/VM does not immediately reuse the variable's slot.
     * <li>the compiler/VM is implemented to report the extended range that
     * would result from the item above.
     * </ul>
     * The advantage of an extended range is that variables from recently
     * exited scopes may remain available for examination (this is especially
     * useful for loop indices). If, as a result of the extensions above,
     * the current frame location is contained within the range
     * of multiple local variables of the same name, the variable with the
     * highest-starting range is chosen for the returned list.
     *
     * @return the list of {@link LocalVariable} objects currently visible;
     * the list will be empty if there are no visible variables;
     * specifically, frames in native methods will always return a
     * zero-length list.
     * @throws AbsentInformationException if there is no local variable
     * information for this method.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     * @throws NativeMethodException if the current method is native.
     */
    List<LocalVariable> visibleVariables() throws AbsentInformationException;

    /**
     * Finds a {@link LocalVariable} that matches the given name and is
     * visible at the current frame location.
     * See {@link #visibleVariables} for more information on visibility.
     *
     * @param name the variable name to find
     * @return the matching {@link LocalVariable}, or null if there is no
     * visible variable with the given name; frames in native methods
     * will always return null.
     * @throws AbsentInformationException if there is no local variable
     * information for this method.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     * @throws NativeMethodException if the current method is native.
     */
    LocalVariable visibleVariableByName(String name) throws AbsentInformationException;

    /**
     * Gets the {@link Value} of a {@link LocalVariable} in this frame.
     * The variable must be valid for this frame's method and visible
     * according to the rules described in {@link #visibleVariables}.
     *
     * @param variable the {@link LocalVariable} to be accessed
     * @return the {@link Value} of the instance field.
     * @throws java.lang.IllegalArgumentException if the variable is
     * either invalid for this frame's method or not visible.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     */
    Value getValue(LocalVariable variable);

    /**
     * Returns the values of multiple local variables in this frame.
     * Each variable must be valid for this frame's method and visible
     * according to the rules described in {@link #visibleVariables}.
     *
     * @param variables a list of {@link LocalVariable} objects to be accessed
     * @return a map associating each {@link LocalVariable} with
     * its {@link Value}
     * @throws java.lang.IllegalArgumentException if any variable is
     * either invalid for this frame's method or not visible.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     */
    Map<LocalVariable,Value> getValues(List<? extends LocalVariable> variables);

    /**
     * Sets the {@link Value} of a {@link LocalVariable} in this frame.
     * The variable must be valid for this frame's method and visible
     * according to the rules described in {@link #visibleVariables}.
     * <p>
     * Object values must be assignment compatible with the variable type
     * (This implies that the variable type must be loaded through the
     * enclosing class's class loader). Primitive values must be
     * either assignment compatible with the variable type or must be
     * convertible to the variable type without loss of information.
     * See JLS section 5.2 for more information on assignment
     * compatibility.
     *
     * @param variable the field containing the requested value
     * @param value the new value to assign
     * @throws java.lang.IllegalArgumentException if the field is not valid for
     * this object's class.
     * @throws InvalidTypeException if the value's type does not match
     * the variable's type.
     * @throws ClassNotLoadedException if the variable type has not yet been loaded
     * through the appropriate class loader.
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     * @throws VMCannotBeModifiedException if the VirtualMachine is read-only - see {@link VirtualMachine#canBeModified()}.
     */
    void setValue(LocalVariable variable, Value value)
        throws InvalidTypeException, ClassNotLoadedException;

    /**
     * Returns the values of all arguments in this frame.  Values are
     * returned even if no local variable information is present.
     *
     * @return a list containing a {@link Value} object for each argument
     * to this frame, in the order in which the arguments were
     * declared.  If the method corresponding to this frame has
     * no arguments, an empty list is returned.
     *
     * @throws InvalidStackFrameException if this stack frame has become
     * invalid. Once the frame's thread is resumed, the stack frame is
     * no longer valid.
     * @since 1.6
     */
    List<Value> getArgumentValues();
}
