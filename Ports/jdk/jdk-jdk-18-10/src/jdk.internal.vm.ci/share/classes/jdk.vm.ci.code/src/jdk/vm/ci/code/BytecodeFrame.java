/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package jdk.vm.ci.code;

import java.util.Arrays;
import java.util.Objects;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.JavaValue;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.Value;

/**
 * Represents the Java bytecode frame state(s) at a given position including {@link Value locations}
 * where to find the local variables, operand stack values and locked objects of the bytecode
 * frame(s).
 */
public final class BytecodeFrame extends BytecodePosition {

    /**
     * An array of values representing how to reconstruct the state of the Java frame. This is array
     * is partitioned as follows:
     * <p>
     * <table summary="" border="1" cellpadding="5" frame="void" rules="all">
     * <tr>
     * <th>Start index (inclusive)</th>
     * <th>End index (exclusive)</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>0</td>
     * <td>numLocals</td>
     * <td>Local variables</td>
     * </tr>
     * <tr>
     * <td>numLocals</td>
     * <td>numLocals + numStack</td>
     * <td>Operand stack</td>
     * </tr>
     * <tr>
     * <td>numLocals + numStack</td>
     * <td>values.length</td>
     * <td>Locked objects</td>
     * </tr>
     * </table>
     * <p>
     * Note that the number of locals and the number of stack slots may be smaller than the maximum
     * number of locals and stack slots as specified in the compiled method.
     *
     * This field is intentionally exposed as a mutable array that a compiler may modify (e.g.
     * during register allocation).
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "field is intentionally mutable")//
    public final JavaValue[] values;

    /**
     * An array describing the Java kinds in {@link #values}. It records a kind for the locals and
     * the operand stack.
     */
    private final JavaKind[] slotKinds;

    /**
     * The number of locals in the values array.
     */
    public final int numLocals;

    /**
     * The number of stack slots in the values array.
     */
    public final int numStack;

    /**
     * The number of locks in the values array.
     */
    public final int numLocks;

    /**
     * True if this is a position inside an exception handler before the exception object has been
     * consumed. In this case, {@link #numStack} {@code == 1} and {@link #getStackValue(int)
     * getStackValue(0)} is the location of the exception object. If deoptimization happens at this
     * position, the interpreter will rethrow the exception instead of executing the bytecode
     * instruction at this position.
     */
    public final boolean rethrowException;

    /**
     * Specifies if this object represents a frame state in the middle of executing a call. If true,
     * the arguments to the call have been popped from the stack and the return value (for a
     * non-void call) has not yet been pushed.
     */
    public final boolean duringCall;

    /**
     * This BCI should be used for frame states that are built for code with no meaningful BCI.
     */
    public static final int UNKNOWN_BCI = -5;

    /**
     * The BCI for exception unwind. This is synthetic code and has no representation in bytecode.
     * In contrast with {@link #AFTER_EXCEPTION_BCI}, at this point, if the method is synchronized,
     * the monitor is still held.
     */
    public static final int UNWIND_BCI = -1;

    /**
     * The BCI for the state before starting to execute a method. Note that if the method is
     * synchronized, the monitor is not yet held.
     */
    public static final int BEFORE_BCI = -2;

    /**
     * The BCI for the state after finishing the execution of a method and returning normally. Note
     * that if the method was synchronized the monitor is already released.
     */
    public static final int AFTER_BCI = -3;

    /**
     * The BCI for exception unwind. This is synthetic code and has no representation in bytecode.
     * In contrast with {@link #UNWIND_BCI}, at this point, if the method is synchronized, the
     * monitor is already released.
     */
    public static final int AFTER_EXCEPTION_BCI = -4;

    /**
     * This BCI should be used for states that cannot be the target of a deoptimization, like
     * snippet frame states.
     */
    public static final int INVALID_FRAMESTATE_BCI = -6;

    /**
     * Determines if a given BCI matches one of the placeholder BCI constants defined in this class.
     */
    public static boolean isPlaceholderBci(int bci) {
        return bci < 0;
    }

    /**
     * Gets the name of a given placeholder BCI.
     */
    public static String getPlaceholderBciName(int bci) {
        assert isPlaceholderBci(bci);
        if (bci == BytecodeFrame.AFTER_BCI) {
            return "AFTER_BCI";
        } else if (bci == BytecodeFrame.AFTER_EXCEPTION_BCI) {
            return "AFTER_EXCEPTION_BCI";
        } else if (bci == BytecodeFrame.INVALID_FRAMESTATE_BCI) {
            return "INVALID_FRAMESTATE_BCI";
        } else if (bci == BytecodeFrame.BEFORE_BCI) {
            return "BEFORE_BCI";
        } else if (bci == BytecodeFrame.UNKNOWN_BCI) {
            return "UNKNOWN_BCI";
        } else {
            assert bci == BytecodeFrame.UNWIND_BCI;
            return "UNWIND_BCI";
        }
    }

    /**
     * Creates a new frame object.
     *
     * @param caller the caller frame (which may be {@code null})
     * @param method the method
     * @param bci a BCI within the method
     * @param rethrowException specifies if the VM should re-throw the pending exception when
     *            deopt'ing using this frame
     * @param values the frame state {@link #values}.
     * @param slotKinds the kinds in {@code values}. This array is now owned by this object and must
     *            not be mutated by the caller.
     * @param numLocals the number of local variables
     * @param numStack the depth of the stack
     * @param numLocks the number of locked objects
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "caller transfers ownership of `slotKinds`")
    public BytecodeFrame(BytecodeFrame caller, ResolvedJavaMethod method, int bci, boolean rethrowException, boolean duringCall, JavaValue[] values, JavaKind[] slotKinds, int numLocals, int numStack,
                    int numLocks) {
        super(caller, method, bci);
        assert values != null;
        this.rethrowException = rethrowException;
        this.duringCall = duringCall;
        this.values = values;
        this.slotKinds = slotKinds;
        this.numLocals = numLocals;
        this.numStack = numStack;
        this.numLocks = numLocks;
        assert !rethrowException || numStack == 1 : "must have exception on top of the stack";
    }

    /**
     * Ensure that the frame state is formatted as expected by the JVM, with null or Illegal in the
     * slot following a double word item. This should really be checked in FrameState itself but
     * because of Word type rewriting and alternative backends that can't be done.
     */
    public boolean validateFormat() {
        if (caller() != null) {
            caller().validateFormat();
        }
        for (int i = 0; i < numLocals + numStack; i++) {
            if (values[i] != null) {
                JavaKind kind = slotKinds[i];
                if (kind.needsTwoSlots()) {
                    assert slotKinds.length > i + 1 : String.format("missing second word %s", this);
                    assert slotKinds[i + 1] == JavaKind.Illegal : this;
                }
            }
        }
        return true;
    }

    /**
     * Gets the kind of a local variable.
     *
     * @param i the local variable to query
     * @return the kind of local variable {@code i}
     * @throw {@link IndexOutOfBoundsException} if {@code i < 0 || i >= this.numLocals}
     */
    public JavaKind getLocalValueKind(int i) {
        Objects.checkIndex(i, numLocals);
        return slotKinds[i];
    }

    /**
     * Gets the kind of a stack slot.
     *
     * @param i the local variable to query
     * @return the kind of stack slot {@code i}
     * @throw {@link IndexOutOfBoundsException} if {@code i < 0 || i >= this.numStack}
     */
    public JavaKind getStackValueKind(int i) {
        Objects.checkIndex(i, numStack);
        return slotKinds[i + numLocals];
    }

    /**
     * Gets the value representing the specified local variable.
     *
     * @param i the local variable index
     * @return the value that can be used to reconstruct the local's current value
     * @throw {@link IndexOutOfBoundsException} if {@code i < 0 || i >= this.numLocals}
     */
    public JavaValue getLocalValue(int i) {
        Objects.checkIndex(i, numLocals);
        return values[i];
    }

    /**
     * Gets the value representing the specified stack slot.
     *
     * @param i the stack index
     * @return the value that can be used to reconstruct the stack slot's current value
     * @throw {@link IndexOutOfBoundsException} if {@code i < 0 || i >= this.numStack}
     */
    public JavaValue getStackValue(int i) {
        Objects.checkIndex(i, numStack);
        return values[i + numLocals];
    }

    /**
     * Gets the value representing the specified lock.
     *
     * @param i the lock index
     * @return the value that can be used to reconstruct the lock's current value
     * @throw {@link IndexOutOfBoundsException} if {@code i < 0 || i >= this.numLocks}
     */
    public JavaValue getLockValue(int i) {
        Objects.checkIndex(i, numLocks);
        return values[i + numLocals + numStack];
    }

    /**
     * Gets the caller of this frame.
     *
     * @return {@code null} if this frame has no caller
     */
    public BytecodeFrame caller() {
        return (BytecodeFrame) getCaller();
    }

    @Override
    public int hashCode() {
        return (numLocals + 1) ^ (numStack + 11) ^ (numLocks + 7);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BytecodeFrame && super.equals(obj)) {
            BytecodeFrame that = (BytecodeFrame) obj;
            // @formatter:off
            if (this.duringCall == that.duringCall &&
                this.rethrowException == that.rethrowException &&
                this.numLocals == that.numLocals &&
                this.numLocks == that.numLocks &&
                this.numStack == that.numStack &&
                Arrays.equals(this.values, that.values)) {
                return true;
            }
            // @formatter:off
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return CodeUtil.append(new StringBuilder(100), this).toString();
    }
}
