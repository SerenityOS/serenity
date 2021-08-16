/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.jvm;

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import static com.sun.tools.javac.code.TypeTag.BOT;
import static com.sun.tools.javac.code.TypeTag.INT;
import static com.sun.tools.javac.jvm.ByteCodes.*;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Class;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Double;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Fieldref;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Float;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Integer;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_InterfaceMethodref;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Long;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_MethodHandle;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_MethodType;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_Methodref;
import static com.sun.tools.javac.jvm.ClassFile.CONSTANT_String;
import static com.sun.tools.javac.jvm.UninitializedType.*;
import static com.sun.tools.javac.jvm.ClassWriter.StackMapTableFrame;
import java.util.Arrays;

/** An internal structure that corresponds to the code attribute of
 *  methods in a classfile. The class also provides some utility operations to
 *  generate bytecode instructions.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class Code {

    public final boolean debugCode;
    public final boolean needStackMap;

    public enum StackMapFormat {
        NONE,
        CLDC {
            Name getAttributeName(Names names) {
                return names.StackMap;
            }
        },
        JSR202 {
            Name getAttributeName(Names names) {
                return names.StackMapTable;
            }
        };
        Name getAttributeName(Names names) {
            return names.empty;
        }
    }

    final Types types;
    final Symtab syms;
    final PoolWriter poolWriter;

/*---------- classfile fields: --------------- */

    /** The maximum stack size.
     */
    public int max_stack = 0;

    /** The maximum number of local variable slots.
     */
    public int max_locals = 0;

    /** The code buffer.
     */
    public byte[] code = new byte[64];

    /** the current code pointer.
     */
    public int cp = 0;

    /** Check the code against VM spec limits; if
     *  problems report them and return true.
     */
    public boolean checkLimits(DiagnosticPosition pos, Log log) {
        if (cp > ClassFile.MAX_CODE) {
            log.error(pos, Errors.LimitCode);
            return true;
        }
        if (max_locals > ClassFile.MAX_LOCALS) {
            log.error(pos, Errors.LimitLocals);
            return true;
        }
        if (max_stack > ClassFile.MAX_STACK) {
            log.error(pos, Errors.LimitStack);
            return true;
        }
        return false;
    }

    /** A buffer for expression catch data. Each enter is a vector
     *  of four unsigned shorts.
     */
    ListBuffer<char[]> catchInfo = new ListBuffer<>();

    /** A buffer for line number information. Each entry is a vector
     *  of two unsigned shorts.
     */
    List<char[]> lineInfo = List.nil(); // handled in stack fashion

    /** The CharacterRangeTable
     */
    public CRTable crt;

/*---------- internal fields: --------------- */

    /** Are we generating code with jumps &ge; 32K?
     */
    public boolean fatcode;

    /** Code generation enabled?
     */
    private boolean alive = true;

    /** The current machine state (registers and stack).
     */
    State state;

    /** Is it forbidden to compactify code, because something is
     *  pointing to current location?
     */
    private boolean fixedPc = false;

    /** The next available register.
     */
    public int nextreg = 0;

    /** A chain for jumps to be resolved before the next opcode is emitted.
     *  We do this lazily to avoid jumps to jumps.
     */
    Chain pendingJumps = null;

    /** The position of the currently statement, if we are at the
     *  start of this statement, NOPOS otherwise.
     *  We need this to emit line numbers lazily, which we need to do
     *  because of jump-to-jump optimization.
     */
    int pendingStatPos = Position.NOPOS;

    /** Set true when a stackMap is needed at the current PC. */
    boolean pendingStackMap = false;

    /** The stack map format to be generated. */
    StackMapFormat stackMap;

    /** Switch: emit variable debug info.
     */
    boolean varDebugInfo;

    /** Switch: emit line number info.
     */
    boolean lineDebugInfo;

    /** Emit line number info if map supplied
     */
    Position.LineMap lineMap;

    final MethodSymbol meth;

    private int letExprStackPos = 0;

    /** Construct a code object, given the settings of the fatcode,
     *  debugging info switches and the CharacterRangeTable.
     */
    public Code(MethodSymbol meth,
                boolean fatcode,
                Position.LineMap lineMap,
                boolean varDebugInfo,
                StackMapFormat stackMap,
                boolean debugCode,
                CRTable crt,
                Symtab syms,
                Types types,
                PoolWriter poolWriter) {
        this.meth = meth;
        this.fatcode = fatcode;
        this.lineMap = lineMap;
        this.lineDebugInfo = lineMap != null;
        this.varDebugInfo = varDebugInfo;
        this.crt = crt;
        this.syms = syms;
        this.types = types;
        this.poolWriter = poolWriter;
        this.debugCode = debugCode;
        this.stackMap = stackMap;
        switch (stackMap) {
        case CLDC:
        case JSR202:
            this.needStackMap = true;
            break;
        default:
            this.needStackMap = false;
        }
        state = new State();
        lvar = new LocalVar[20];
    }


/* **************************************************************************
 * Typecodes & related stuff
 ****************************************************************************/

    /** Given a type, return its type code (used implicitly in the
     *  JVM architecture).
     */
    public static int typecode(Type type) {
        switch (type.getTag()) {
        case BYTE: return BYTEcode;
        case SHORT: return SHORTcode;
        case CHAR: return CHARcode;
        case INT: return INTcode;
        case LONG: return LONGcode;
        case FLOAT: return FLOATcode;
        case DOUBLE: return DOUBLEcode;
        case BOOLEAN: return BYTEcode;
        case VOID: return VOIDcode;
        case CLASS:
        case ARRAY:
        case METHOD:
        case BOT:
        case TYPEVAR:
        case UNINITIALIZED_THIS:
        case UNINITIALIZED_OBJECT:
            return OBJECTcode;
        default: throw new AssertionError("typecode " + type.getTag());
        }
    }

    /** Collapse type code for subtypes of int to INTcode.
     */
    public static int truncate(int tc) {
        switch (tc) {
        case BYTEcode: case SHORTcode: case CHARcode: return INTcode;
        default: return tc;
        }
    }

    /** The width in bytes of objects of the type.
     */
    public static int width(int typecode) {
        switch (typecode) {
        case LONGcode: case DOUBLEcode: return 2;
        case VOIDcode: return 0;
        default: return 1;
        }
    }

    public static int width(Type type) {
        return type == null ? 1 : width(typecode(type));
    }

    /** The total width taken up by a vector of objects.
     */
    public static int width(List<Type> types) {
        int w = 0;
        for (List<Type> l = types; l.nonEmpty(); l = l.tail)
            w = w + width(l.head);
        return w;
    }

    /** Given a type, return its code for allocating arrays of that type.
     */
    public static int arraycode(Type type) {
        switch (type.getTag()) {
        case BYTE: return 8;
        case BOOLEAN: return 4;
        case SHORT: return 9;
        case CHAR: return 5;
        case INT: return 10;
        case LONG: return 11;
        case FLOAT: return 6;
        case DOUBLE: return 7;
        case CLASS: return 0;
        case ARRAY: return 1;
        default: throw new AssertionError("arraycode " + type);
        }
    }


/* **************************************************************************
 * Emit code
 ****************************************************************************/

    /** The current output code pointer.
     */
    public int curCP() {
        /*
         * This method has side-effects because calling it can indirectly provoke
         *  extra code generation, like goto instructions, depending on the context
         *  where it's called.
         *  Use with care or even better avoid using it.
         */
        if (pendingJumps != null) {
            resolvePending();
        }
        if (pendingStatPos != Position.NOPOS) {
            markStatBegin();
        }
        fixedPc = true;
        return cp;
    }

    /** Emit a byte of code.
     */
    private  void emit1(int od) {
        if (!alive) return;
        code = ArrayUtils.ensureCapacity(code, cp);
        code[cp++] = (byte)od;
    }

    /** Emit two bytes of code.
     */
    private void emit2(int od) {
        if (!alive) return;
        if (cp + 2 > code.length) {
            emit1(od >> 8);
            emit1(od);
        } else {
            code[cp++] = (byte)(od >> 8);
            code[cp++] = (byte)od;
        }
    }

    /** Emit four bytes of code.
     */
    public void emit4(int od) {
        if (!alive) return;
        if (cp + 4 > code.length) {
            emit1(od >> 24);
            emit1(od >> 16);
            emit1(od >> 8);
            emit1(od);
        } else {
            code[cp++] = (byte)(od >> 24);
            code[cp++] = (byte)(od >> 16);
            code[cp++] = (byte)(od >> 8);
            code[cp++] = (byte)od;
        }
    }

    /** Emit an opcode.
     */
    private void emitop(int op) {
        if (pendingJumps != null) resolvePending();
        if (alive) {
            if (pendingStatPos != Position.NOPOS)
                markStatBegin();
            if (pendingStackMap) {
                pendingStackMap = false;
                emitStackMap();
            }
            if (debugCode)
                System.err.println("emit@" + cp + " stack=" +
                                   state.stacksize + ": " +
                                   mnem(op));
            emit1(op);
        }
    }

    void postop() {
        Assert.check(alive || isStatementStart());
    }

    /** Emit a ldc (or ldc_w) instruction, taking into account operand size
    */
    public void emitLdc(LoadableConstant constant) {
        int od = poolWriter.putConstant(constant);
        if (od <= 255) {
            emitop1(ldc1, od, constant);
        }
        else {
            emitop2(ldc2, od, constant);
        }
    }

    /** Emit a multinewarray instruction.
     */
    public void emitMultianewarray(int ndims, int type, Type arrayType) {
        emitop(multianewarray);
        if (!alive) return;
        emit2(type);
        emit1(ndims);
        state.pop(ndims);
        state.push(arrayType);
    }

    /** Emit newarray.
     */
    public void emitNewarray(int elemcode, Type arrayType) {
        emitop(newarray);
        if (!alive) return;
        emit1(elemcode);
        state.pop(1); // count
        state.push(arrayType);
    }

    /** Emit anewarray.
     */
    public void emitAnewarray(int od, Type arrayType) {
        emitop(anewarray);
        if (!alive) return;
        emit2(od);
        state.pop(1);
        state.push(arrayType);
    }

    /** Emit an invokeinterface instruction.
     */
    public void emitInvokeinterface(Symbol member, Type mtype) {
        int argsize = width(mtype.getParameterTypes());
        emitop(invokeinterface);
        if (!alive) return;
        emit2(poolWriter.putMember(member));
        emit1(argsize + 1);
        emit1(0);
        state.pop(argsize + 1);
        state.push(mtype.getReturnType());
    }

    /** Emit an invokespecial instruction.
     */
    public void emitInvokespecial(Symbol member, Type mtype) {
        int argsize = width(mtype.getParameterTypes());
        emitop(invokespecial);
        if (!alive) return;
        emit2(poolWriter.putMember(member));
        state.pop(argsize);
        if (member.isConstructor())
            state.markInitialized((UninitializedType)state.peek());
        state.pop(1);
        state.push(mtype.getReturnType());
    }

    /** Emit an invokestatic instruction.
     */
    public void emitInvokestatic(Symbol member, Type mtype) {
        int argsize = width(mtype.getParameterTypes());
        emitop(invokestatic);
        if (!alive) return;
        emit2(poolWriter.putMember(member));
        state.pop(argsize);
        state.push(mtype.getReturnType());
    }

    /** Emit an invokevirtual instruction.
     */
    public void emitInvokevirtual(Symbol member, Type mtype) {
        int argsize = width(mtype.getParameterTypes());
        emitop(invokevirtual);
        if (!alive) return;
        emit2(poolWriter.putMember(member));
        state.pop(argsize + 1);
        state.push(mtype.getReturnType());
    }

    /** Emit an invokedynamic instruction.
     */
    public void emitInvokedynamic(DynamicMethodSymbol dynMember, Type mtype) {
        int argsize = width(mtype.getParameterTypes());
        emitop(invokedynamic);
        if (!alive) return;
        emit2(poolWriter.putDynamic(dynMember));
        emit2(0);
        state.pop(argsize);
        state.push(mtype.getReturnType());
    }

    /** Emit an opcode with no operand field.
     */
    public void emitop0(int op) {
        emitop(op);
        if (!alive) return;
        switch (op) {
        case aaload: {
            state.pop(1);// index
            Type a = state.stack[state.stacksize-1];
            Assert.check(!a.hasTag(BOT)); // null type as is cannot be indexed.
            state.pop(1);
            state.push(types.erasure(types.elemtype(a))); }
            break;
        case goto_:
            markDead();
            break;
        case nop:
        case ineg:
        case lneg:
        case fneg:
        case dneg:
            break;
        case aconst_null:
            state.push(syms.botType);
            break;
        case iconst_m1:
        case iconst_0:
        case iconst_1:
        case iconst_2:
        case iconst_3:
        case iconst_4:
        case iconst_5:
        case iload_0:
        case iload_1:
        case iload_2:
        case iload_3:
            state.push(syms.intType);
            break;
        case lconst_0:
        case lconst_1:
        case lload_0:
        case lload_1:
        case lload_2:
        case lload_3:
            state.push(syms.longType);
            break;
        case fconst_0:
        case fconst_1:
        case fconst_2:
        case fload_0:
        case fload_1:
        case fload_2:
        case fload_3:
            state.push(syms.floatType);
            break;
        case dconst_0:
        case dconst_1:
        case dload_0:
        case dload_1:
        case dload_2:
        case dload_3:
            state.push(syms.doubleType);
            break;
        case aload_0:
            state.push(lvar[0].sym.type);
            break;
        case aload_1:
            state.push(lvar[1].sym.type);
            break;
        case aload_2:
            state.push(lvar[2].sym.type);
            break;
        case aload_3:
            state.push(lvar[3].sym.type);
            break;
        case iaload:
        case baload:
        case caload:
        case saload:
            state.pop(2);
            state.push(syms.intType);
            break;
        case laload:
            state.pop(2);
            state.push(syms.longType);
            break;
        case faload:
            state.pop(2);
            state.push(syms.floatType);
            break;
        case daload:
            state.pop(2);
            state.push(syms.doubleType);
            break;
        case istore_0:
        case istore_1:
        case istore_2:
        case istore_3:
        case fstore_0:
        case fstore_1:
        case fstore_2:
        case fstore_3:
        case astore_0:
        case astore_1:
        case astore_2:
        case astore_3:
        case pop:
        case lshr:
        case lshl:
        case lushr:
            state.pop(1);
            break;
        case areturn:
        case ireturn:
        case freturn:
            Assert.check(state.nlocks == 0);
            state.pop(1);
            markDead();
            break;
        case athrow:
            state.pop(1);
            markDead();
            break;
        case lstore_0:
        case lstore_1:
        case lstore_2:
        case lstore_3:
        case dstore_0:
        case dstore_1:
        case dstore_2:
        case dstore_3:
        case pop2:
            state.pop(2);
            break;
        case lreturn:
        case dreturn:
            Assert.check(state.nlocks == 0);
            state.pop(2);
            markDead();
            break;
        case dup:
            state.push(state.stack[state.stacksize-1]);
            break;
        case return_:
            Assert.check(state.nlocks == 0);
            markDead();
            break;
        case arraylength:
            state.pop(1);
            state.push(syms.intType);
            break;
        case isub:
        case iadd:
        case imul:
        case idiv:
        case imod:
        case ishl:
        case ishr:
        case iushr:
        case iand:
        case ior:
        case ixor:
            state.pop(1);
            // state.pop(1);
            // state.push(syms.intType);
            break;
        case aastore:
            state.pop(3);
            break;
        case land:
        case lor:
        case lxor:
        case lmod:
        case ldiv:
        case lmul:
        case lsub:
        case ladd:
            state.pop(2);
            break;
        case lcmp:
            state.pop(4);
            state.push(syms.intType);
            break;
        case l2i:
            state.pop(2);
            state.push(syms.intType);
            break;
        case i2l:
            state.pop(1);
            state.push(syms.longType);
            break;
        case i2f:
            state.pop(1);
            state.push(syms.floatType);
            break;
        case i2d:
            state.pop(1);
            state.push(syms.doubleType);
            break;
        case l2f:
            state.pop(2);
            state.push(syms.floatType);
            break;
        case l2d:
            state.pop(2);
            state.push(syms.doubleType);
            break;
        case f2i:
            state.pop(1);
            state.push(syms.intType);
            break;
        case f2l:
            state.pop(1);
            state.push(syms.longType);
            break;
        case f2d:
            state.pop(1);
            state.push(syms.doubleType);
            break;
        case d2i:
            state.pop(2);
            state.push(syms.intType);
            break;
        case d2l:
            state.pop(2);
            state.push(syms.longType);
            break;
        case d2f:
            state.pop(2);
            state.push(syms.floatType);
            break;
        case tableswitch:
        case lookupswitch:
            state.pop(1);
            // the caller is responsible for patching up the state
            break;
        case dup_x1: {
            Type val1 = state.pop1();
            Type val2 = state.pop1();
            state.push(val1);
            state.push(val2);
            state.push(val1);
            break;
        }
        case bastore:
            state.pop(3);
            break;
        case int2byte:
        case int2char:
        case int2short:
            break;
        case fmul:
        case fadd:
        case fsub:
        case fdiv:
        case fmod:
            state.pop(1);
            break;
        case castore:
        case iastore:
        case fastore:
        case sastore:
            state.pop(3);
            break;
        case lastore:
        case dastore:
            state.pop(4);
            break;
        case dup2:
            if (state.stack[state.stacksize-1] != null) {
                Type value1 = state.pop1();
                Type value2 = state.pop1();
                state.push(value2);
                state.push(value1);
                state.push(value2);
                state.push(value1);
            } else {
                Type value = state.pop2();
                state.push(value);
                state.push(value);
            }
            break;
        case dup2_x1:
            if (state.stack[state.stacksize-1] != null) {
                Type value1 = state.pop1();
                Type value2 = state.pop1();
                Type value3 = state.pop1();
                state.push(value2);
                state.push(value1);
                state.push(value3);
                state.push(value2);
                state.push(value1);
            } else {
                Type value1 = state.pop2();
                Type value2 = state.pop1();
                state.push(value1);
                state.push(value2);
                state.push(value1);
            }
            break;
        case dup2_x2:
            if (state.stack[state.stacksize-1] != null) {
                Type value1 = state.pop1();
                Type value2 = state.pop1();
                if (state.stack[state.stacksize-1] != null) {
                    // form 1
                    Type value3 = state.pop1();
                    Type value4 = state.pop1();
                    state.push(value2);
                    state.push(value1);
                    state.push(value4);
                    state.push(value3);
                    state.push(value2);
                    state.push(value1);
                } else {
                    // form 3
                    Type value3 = state.pop2();
                    state.push(value2);
                    state.push(value1);
                    state.push(value3);
                    state.push(value2);
                    state.push(value1);
                }
            } else {
                Type value1 = state.pop2();
                if (state.stack[state.stacksize-1] != null) {
                    // form 2
                    Type value2 = state.pop1();
                    Type value3 = state.pop1();
                    state.push(value1);
                    state.push(value3);
                    state.push(value2);
                    state.push(value1);
                } else {
                    // form 4
                    Type value2 = state.pop2();
                    state.push(value1);
                    state.push(value2);
                    state.push(value1);
                }
            }
            break;
        case dup_x2: {
            Type value1 = state.pop1();
            if (state.stack[state.stacksize-1] != null) {
                // form 1
                Type value2 = state.pop1();
                Type value3 = state.pop1();
                state.push(value1);
                state.push(value3);
                state.push(value2);
                state.push(value1);
            } else {
                // form 2
                Type value2 = state.pop2();
                state.push(value1);
                state.push(value2);
                state.push(value1);
            }
        }
            break;
        case fcmpl:
        case fcmpg:
            state.pop(2);
            state.push(syms.intType);
            break;
        case dcmpl:
        case dcmpg:
            state.pop(4);
            state.push(syms.intType);
            break;
        case swap: {
            Type value1 = state.pop1();
            Type value2 = state.pop1();
            state.push(value1);
            state.push(value2);
            break;
        }
        case dadd:
        case dsub:
        case dmul:
        case ddiv:
        case dmod:
            state.pop(2);
            break;
        case ret:
            markDead();
            break;
        case wide:
            // must be handled by the caller.
            return;
        case monitorenter:
        case monitorexit:
            state.pop(1);
            break;

        default:
            throw new AssertionError(mnem(op));
        }
        postop();
    }

    /** Emit an opcode with a one-byte operand field.
     */
    public void emitop1(int op, int od) {
        emitop1(op, od, null);
    }

    public void emitop1(int op, int od, PoolConstant data) {
        emitop(op);
        if (!alive) return;
        emit1(od);
        switch (op) {
        case bipush:
            state.push(syms.intType);
            break;
        case ldc1:
            state.push(types.constantType((LoadableConstant)data));
            break;
        default:
            throw new AssertionError(mnem(op));
        }
        postop();
    }

    /** Emit an opcode with a one-byte operand field;
     *  widen if field does not fit in a byte.
     */
    public void emitop1w(int op, int od) {
        if (od > 0xFF) {
            emitop(wide);
            emitop(op);
            emit2(od);
        } else {
            emitop(op);
            emit1(od);
        }
        if (!alive) return;
        switch (op) {
        case iload:
            state.push(syms.intType);
            break;
        case lload:
            state.push(syms.longType);
            break;
        case fload:
            state.push(syms.floatType);
            break;
        case dload:
            state.push(syms.doubleType);
            break;
        case aload:
            state.push(lvar[od].sym.type);
            break;
        case lstore:
        case dstore:
            state.pop(2);
            break;
        case istore:
        case fstore:
        case astore:
            state.pop(1);
            break;
        case ret:
            markDead();
            break;
        default:
            throw new AssertionError(mnem(op));
        }
        postop();
    }

    /** Emit an opcode with two one-byte operand fields;
     *  widen if either field does not fit in a byte.
     */
    public void emitop1w(int op, int od1, int od2) {
        if (od1 > 0xFF || od2 < -128 || od2 > 127) {
            emitop(wide);
            emitop(op);
            emit2(od1);
            emit2(od2);
        } else {
            emitop(op);
            emit1(od1);
            emit1(od2);
        }
        if (!alive) return;
        switch (op) {
        case iinc:
            break;
        default:
            throw new AssertionError(mnem(op));
        }
    }

    /** Emit an opcode with a two-byte operand field.
     */
    public <P extends PoolConstant> void emitop2(int op, P constant, ToIntBiFunction<PoolWriter, P> poolFunc) {
        int od = poolFunc.applyAsInt(poolWriter, constant);
        emitop2(op, od, constant);
    }

    public void emitop2(int op, int od) {
        emitop2(op, od, null);
    }

    public void emitop2(int op, int od, PoolConstant data) {
        emitop(op);
        if (!alive) return;
        emit2(od);
        switch (op) {
        case getstatic:
            state.push(((Symbol)data).erasure(types));
            break;
        case putstatic:
            state.pop(((Symbol)data).erasure(types));
            break;
        case new_: {
            Type t = (Type)data;
            state.push(uninitializedObject(t.tsym.erasure(types), cp-3));
            break;
        }
        case sipush:
            state.push(syms.intType);
            break;
        case if_acmp_null:
        case if_acmp_nonnull:
        case ifeq:
        case ifne:
        case iflt:
        case ifge:
        case ifgt:
        case ifle:
            state.pop(1);
            break;
        case if_icmpeq:
        case if_icmpne:
        case if_icmplt:
        case if_icmpge:
        case if_icmpgt:
        case if_icmple:
        case if_acmpeq:
        case if_acmpne:
            state.pop(2);
            break;
        case goto_:
            markDead();
            break;
        case putfield:
            state.pop(((Symbol)data).erasure(types));
            state.pop(1); // object ref
            break;
        case getfield:
            state.pop(1); // object ref
            state.push(((Symbol)data).erasure(types));
            break;
        case checkcast: {
            state.pop(1); // object ref
            Type t = types.erasure((Type)data);
            state.push(t);
            break; }
        case ldc2w:
            state.push(types.constantType((LoadableConstant)data));
            break;
        case instanceof_:
            state.pop(1);
            state.push(syms.intType);
            break;
        case ldc2:
            state.push(types.constantType((LoadableConstant)data));
            break;
        case jsr:
            break;
        default:
            throw new AssertionError(mnem(op));
        }
        // postop();
    }

    /** Emit an opcode with a four-byte operand field.
     */
    public void emitop4(int op, int od) {
        emitop(op);
        if (!alive) return;
        emit4(od);
        switch (op) {
        case goto_w:
            markDead();
            break;
        case jsr_w:
            break;
        default:
            throw new AssertionError(mnem(op));
        }
        // postop();
    }

    /** Align code pointer to next `incr' boundary.
     */
    public void align(int incr) {
        if (alive)
            while (cp % incr != 0) emitop0(nop);
    }

    /** Place a byte into code at address pc.
     *  Pre: {@literal pc + 1 <= cp }.
     */
    private void put1(int pc, int op) {
        code[pc] = (byte)op;
    }

    /** Place two bytes into code at address pc.
     *  Pre: {@literal pc + 2 <= cp }.
     */
    private void put2(int pc, int od) {
        // pre: pc + 2 <= cp
        put1(pc, od >> 8);
        put1(pc+1, od);
    }

    /** Place four  bytes into code at address pc.
     *  Pre: {@literal pc + 4 <= cp }.
     */
    public void put4(int pc, int od) {
        // pre: pc + 4 <= cp
        put1(pc  , od >> 24);
        put1(pc+1, od >> 16);
        put1(pc+2, od >> 8);
        put1(pc+3, od);
    }

    /** Return code byte at position pc as an unsigned int.
     */
    private int get1(int pc) {
        return code[pc] & 0xFF;
    }

    /** Return two code bytes at position pc as an unsigned int.
     */
    private int get2(int pc) {
        return (get1(pc) << 8) | get1(pc+1);
    }

    /** Return four code bytes at position pc as an int.
     */
    public int get4(int pc) {
        // pre: pc + 4 <= cp
        return
            (get1(pc) << 24) |
            (get1(pc+1) << 16) |
            (get1(pc+2) << 8) |
            (get1(pc+3));
    }

    /** Is code generation currently enabled?
     */
    public boolean isAlive() {
        return alive || pendingJumps != null;
    }

    /** Switch code generation on/off.
     */
    public void markDead() {
        alive = false;
    }

    /** Declare an entry point; return current code pointer
     */
    public int entryPoint() {
        int pc = curCP();
        alive = true;
        pendingStackMap = needStackMap;
        return pc;
    }

    /** Declare an entry point with initial state;
     *  return current code pointer
     */
    public int entryPoint(State state) {
        int pc = curCP();
        alive = true;
        State newState = state.dup();
        setDefined(newState.defined);
        this.state = newState;
        Assert.check(state.stacksize <= max_stack);
        if (debugCode) System.err.println("entry point " + state);
        pendingStackMap = needStackMap;
        return pc;
    }

    /** Declare an entry point with initial state plus a pushed value;
     *  return current code pointer
     */
    public int entryPoint(State state, Type pushed) {
        int pc = curCP();
        alive = true;
        State newState = state.dup();
        setDefined(newState.defined);
        this.state = newState;
        Assert.check(state.stacksize <= max_stack);
        this.state.push(pushed);
        if (debugCode) System.err.println("entry point " + state);
        pendingStackMap = needStackMap;
        return pc;
    }

    public int setLetExprStackPos(int pos) {
        int res = letExprStackPos;
        letExprStackPos = pos;
        return res;
    }

    public boolean isStatementStart() {
        return !alive || state.stacksize == letExprStackPos;
    }

/**************************************************************************
 * Stack map generation
 *************************************************************************/

    /** An entry in the stack map. */
    static class StackMapFrame {
        int pc;
        Type[] locals;
        Type[] stack;
    }

    /** A buffer of cldc stack map entries. */
    StackMapFrame[] stackMapBuffer = null;

    /** A buffer of compressed StackMapTable entries. */
    StackMapTableFrame[] stackMapTableBuffer = null;
    int stackMapBufferSize = 0;

    /** The last PC at which we generated a stack map. */
    int lastStackMapPC = -1;

    /** The last stack map frame in StackMapTable. */
    StackMapFrame lastFrame = null;

    /** The stack map frame before the last one. */
    StackMapFrame frameBeforeLast = null;

    /** Emit a stack map entry.  */
    public void emitStackMap() {
        int pc = curCP();
        if (!needStackMap) return;



        switch (stackMap) {
            case CLDC:
                emitCLDCStackMap(pc, getLocalsSize());
                break;
            case JSR202:
                emitStackMapFrame(pc, getLocalsSize());
                break;
            default:
                throw new AssertionError("Should have chosen a stackmap format");
        }
        // DEBUG code follows
        if (debugCode) state.dump(pc);
    }

    private int getLocalsSize() {
        int nextLocal = 0;
        for (int i=max_locals-1; i>=0; i--) {
            if (state.defined.isMember(i) && lvar[i] != null) {
                nextLocal = i + width(lvar[i].sym.erasure(types));
                break;
            }
        }
        return nextLocal;
    }

    /** Emit a CLDC stack map frame. */
    void emitCLDCStackMap(int pc, int localsSize) {
        if (lastStackMapPC == pc) {
            // drop existing stackmap at this offset
            stackMapBuffer[--stackMapBufferSize] = null;
        }
        lastStackMapPC = pc;

        if (stackMapBuffer == null) {
            stackMapBuffer = new StackMapFrame[20];
        } else {
            stackMapBuffer = ArrayUtils.ensureCapacity(stackMapBuffer, stackMapBufferSize);
        }
        StackMapFrame frame =
            stackMapBuffer[stackMapBufferSize++] = new StackMapFrame();
        frame.pc = pc;

        frame.locals = new Type[localsSize];
        for (int i=0; i<localsSize; i++) {
            if (state.defined.isMember(i) && lvar[i] != null) {
                Type vtype = lvar[i].sym.type;
                if (!(vtype instanceof UninitializedType))
                    vtype = types.erasure(vtype);
                frame.locals[i] = vtype;
            }
        }
        frame.stack = new Type[state.stacksize];
        for (int i=0; i<state.stacksize; i++)
            frame.stack[i] = state.stack[i];
    }

    void emitStackMapFrame(int pc, int localsSize) {
        if (lastFrame == null) {
            // first frame
            lastFrame = getInitialFrame();
        } else if (lastFrame.pc == pc) {
            // drop existing stackmap at this offset
            stackMapTableBuffer[--stackMapBufferSize] = null;
            lastFrame = frameBeforeLast;
            frameBeforeLast = null;
        }

        StackMapFrame frame = new StackMapFrame();
        frame.pc = pc;

        int localCount = 0;
        Type[] locals = new Type[localsSize];
        for (int i=0; i<localsSize; i++, localCount++) {
            if (state.defined.isMember(i) && lvar[i] != null) {
                Type vtype = lvar[i].sym.type;
                if (!(vtype instanceof UninitializedType))
                    vtype = types.erasure(vtype);
                locals[i] = vtype;
                if (width(vtype) > 1) i++;
            }
        }
        frame.locals = new Type[localCount];
        for (int i=0, j=0; i<localsSize; i++, j++) {
            Assert.check(j < localCount);
            frame.locals[j] = locals[i];
            if (width(locals[i]) > 1) i++;
        }

        int stackCount = 0;
        for (int i=0; i<state.stacksize; i++) {
            if (state.stack[i] != null) {
                stackCount++;
            }
        }
        frame.stack = new Type[stackCount];
        stackCount = 0;
        for (int i=0; i<state.stacksize; i++) {
            if (state.stack[i] != null) {
                frame.stack[stackCount++] = types.erasure(state.stack[i]);
            }
        }

        if (stackMapTableBuffer == null) {
            stackMapTableBuffer = new StackMapTableFrame[20];
        } else {
            stackMapTableBuffer = ArrayUtils.ensureCapacity(
                                    stackMapTableBuffer,
                                    stackMapBufferSize);
        }
        stackMapTableBuffer[stackMapBufferSize++] =
                StackMapTableFrame.getInstance(frame, lastFrame.pc, lastFrame.locals, types);

        frameBeforeLast = lastFrame;
        lastFrame = frame;
    }

    StackMapFrame getInitialFrame() {
        StackMapFrame frame = new StackMapFrame();
        List<Type> arg_types = ((MethodType)meth.externalType(types)).argtypes;
        int len = arg_types.length();
        int count = 0;
        if (!meth.isStatic()) {
            Type thisType = meth.owner.type;
            frame.locals = new Type[len+1];
            if (meth.isConstructor() && thisType != syms.objectType) {
                frame.locals[count++] = UninitializedType.uninitializedThis(thisType);
            } else {
                frame.locals[count++] = types.erasure(thisType);
            }
        } else {
            frame.locals = new Type[len];
        }
        for (Type arg_type : arg_types) {
            frame.locals[count++] = types.erasure(arg_type);
        }
        frame.pc = -1;
        frame.stack = null;
        return frame;
    }


/**************************************************************************
 * Operations having to do with jumps
 *************************************************************************/

    /** A chain represents a list of unresolved jumps. Jump locations
     *  are sorted in decreasing order.
     */
    public static class Chain {

        /** The position of the jump instruction.
         */
        public final int pc;

        /** The machine state after the jump instruction.
         *  Invariant: all elements of a chain list have the same stacksize
         *  and compatible stack and register contents.
         */
        Code.State state;

        /** The next jump in the list.
         */
        public final Chain next;

        /** Construct a chain from its jump position, stacksize, previous
         *  chain, and machine state.
         */
        public Chain(int pc, Chain next, Code.State state) {
            this.pc = pc;
            this.next = next;
            this.state = state;
        }
    }

    /** Negate a branch opcode.
     */
    public static int negate(int opcode) {
        if (opcode == if_acmp_null) return if_acmp_nonnull;
        else if (opcode == if_acmp_nonnull) return if_acmp_null;
        else return ((opcode + 1) ^ 1) - 1;
    }

    /** Emit a jump instruction.
     *  Return code pointer of instruction to be patched.
     */
    public int emitJump(int opcode) {
        if (fatcode) {
            if (opcode == goto_ || opcode == jsr) {
                emitop4(opcode + goto_w - goto_, 0);
            } else {
                emitop2(negate(opcode), 8);
                emitop4(goto_w, 0);
                alive = true;
                pendingStackMap = needStackMap;
            }
            return cp - 5;
        } else {
            emitop2(opcode, 0);
            return cp - 3;
        }
    }

    /** Emit a branch with given opcode; return its chain.
     *  branch differs from jump in that jsr is treated as no-op.
     */
    public Chain branch(int opcode) {
        Chain result = null;
        if (opcode == goto_) {
            result = pendingJumps;
            pendingJumps = null;
        }
        if (opcode != dontgoto && isAlive()) {
            result = new Chain(emitJump(opcode),
                               result,
                               state.dup());
            fixedPc = fatcode;
            if (opcode == goto_) alive = false;
        }
        return result;
    }

    /** Resolve chain to point to given target.
     */
    public void resolve(Chain chain, int target) {
        boolean changed = false;
        State newState = state;
        for (; chain != null; chain = chain.next) {
            Assert.check(state != chain.state
                    && (target > chain.pc || isStatementStart()));
            if (target >= cp) {
                target = cp;
            } else if (get1(target) == goto_) {
                if (fatcode) target = target + get4(target + 1);
                else target = target + get2(target + 1);
            }
            if (get1(chain.pc) == goto_ &&
                chain.pc + 3 == target && target == cp && !fixedPc) {
                // If goto the next instruction, the jump is not needed:
                // compact the code.
                if (varDebugInfo) {
                    adjustAliveRanges(cp, -3);
                }
                cp = cp - 3;
                target = target - 3;
                if (chain.next == null) {
                    // This is the only jump to the target. Exit the loop
                    // without setting new state. The code is reachable
                    // from the instruction before goto_.
                    alive = true;
                    break;
                }
            } else {
                if (fatcode)
                    put4(chain.pc + 1, target - chain.pc);
                else if (target - chain.pc < Short.MIN_VALUE ||
                         target - chain.pc > Short.MAX_VALUE)
                    fatcode = true;
                else
                    put2(chain.pc + 1, target - chain.pc);
                Assert.check(!alive ||
                    chain.state.stacksize == newState.stacksize &&
                    chain.state.nlocks == newState.nlocks);
            }
            fixedPc = true;
            if (cp == target) {
                changed = true;
                if (debugCode)
                    System.err.println("resolving chain state=" + chain.state);
                if (alive) {
                    newState = chain.state.join(newState);
                } else {
                    newState = chain.state;
                    alive = true;
                }
            }
        }
        Assert.check(!changed || state != newState);
        if (state != newState) {
            setDefined(newState.defined);
            state = newState;
            pendingStackMap = needStackMap;
        }
    }

    /** Resolve chain to point to current code pointer.
     */
    public void resolve(Chain chain) {
        Assert.check(
            !alive ||
            chain==null ||
            state.stacksize == chain.state.stacksize &&
            state.nlocks == chain.state.nlocks);
        pendingJumps = mergeChains(chain, pendingJumps);
    }

    /** Resolve any pending jumps.
     */
    public void resolvePending() {
        Chain x = pendingJumps;
        pendingJumps = null;
        resolve(x, cp);
    }

    /** Merge the jumps in of two chains into one.
     */
    public static Chain mergeChains(Chain chain1, Chain chain2) {
        // recursive merge sort
        if (chain2 == null) return chain1;
        if (chain1 == null) return chain2;
        Assert.check(
            chain1.state.stacksize == chain2.state.stacksize &&
            chain1.state.nlocks == chain2.state.nlocks);
        if (chain1.pc < chain2.pc)
            return new Chain(
                chain2.pc,
                mergeChains(chain1, chain2.next),
                chain2.state);
        return new Chain(
                chain1.pc,
                mergeChains(chain1.next, chain2),
                chain1.state);
    }


/* **************************************************************************
 * Catch clauses
 ****************************************************************************/

    /** Add a catch clause to code.
     */
    public void addCatch(char startPc, char endPc,
                         char handlerPc, char catchType) {
            catchInfo.append(new char[]{startPc, endPc, handlerPc, catchType});
        }


    public void compressCatchTable() {
        ListBuffer<char[]> compressedCatchInfo = new ListBuffer<>();
        List<Integer> handlerPcs = List.nil();
        for (char[] catchEntry : catchInfo) {
            handlerPcs = handlerPcs.prepend((int)catchEntry[2]);
        }
        for (char[] catchEntry : catchInfo) {
            int startpc = catchEntry[0];
            int endpc = catchEntry[1];
            if (startpc == endpc ||
                    (startpc == (endpc - 1) &&
                    handlerPcs.contains(startpc))) {
                continue;
            } else {
                compressedCatchInfo.append(catchEntry);
            }
        }
        catchInfo = compressedCatchInfo;
    }


/* **************************************************************************
 * Line numbers
 ****************************************************************************/

    /** Add a line number entry.
     */
    public void addLineNumber(char startPc, char lineNumber) {
        if (lineDebugInfo) {
            if (lineInfo.nonEmpty() && lineInfo.head[0] == startPc)
                lineInfo = lineInfo.tail;
            if (lineInfo.isEmpty() || lineInfo.head[1] != lineNumber)
                lineInfo = lineInfo.prepend(new char[]{startPc, lineNumber});
        }
    }

    /** Mark beginning of statement.
     */
    public void statBegin(int pos) {
        if (pos != Position.NOPOS) {
            pendingStatPos = pos;
        }
    }

    /** Force stat begin eagerly
     */
    public void markStatBegin() {
        if (alive && lineDebugInfo) {
            int line = lineMap.getLineNumber(pendingStatPos);
            char cp1 = (char)cp;
            char line1 = (char)line;
            if (cp1 == cp && line1 == line)
                addLineNumber(cp1, line1);
        }
        pendingStatPos = Position.NOPOS;
    }


/* **************************************************************************
 * Simulated VM machine state
 ****************************************************************************/

    class State implements Cloneable {
        /** The set of registers containing values. */
        Bits defined;

        /** The (types of the) contents of the machine stack. */
        Type[] stack;

        /** The first stack position currently unused. */
        int stacksize;

        /** The numbers of registers containing locked monitors. */
        int[] locks;
        int nlocks;

        State() {
            defined = new Bits();
            stack = new Type[16];
        }

        State dup() {
            try {
                State state = (State)super.clone();
                state.defined = new Bits(defined);
                state.stack = stack.clone();
                if (locks != null) state.locks = locks.clone();
                if (debugCode) {
                    System.err.println("duping state " + this);
                    dump();
                }
                return state;
            } catch (CloneNotSupportedException ex) {
                throw new AssertionError(ex);
            }
        }

        void lock(int register) {
            if (locks == null) {
                locks = new int[20];
            } else {
                locks = ArrayUtils.ensureCapacity(locks, nlocks);
            }
            locks[nlocks] = register;
            nlocks++;
        }

        void unlock(int register) {
            nlocks--;
            Assert.check(locks[nlocks] == register);
            locks[nlocks] = -1;
        }

        void push(Type t) {
            if (debugCode) System.err.println("   pushing " + t);
            switch (t.getTag()) {
            case VOID:
                return;
            case BYTE:
            case CHAR:
            case SHORT:
            case BOOLEAN:
                t = syms.intType;
                break;
            default:
                break;
            }
            stack = ArrayUtils.ensureCapacity(stack, stacksize+2);
            stack[stacksize++] = t;
            switch (width(t)) {
            case 1:
                break;
            case 2:
                stack[stacksize++] = null;
                break;
            default:
                throw new AssertionError(t);
            }
            if (stacksize > max_stack)
                max_stack = stacksize;
        }

        Type pop1() {
            if (debugCode) System.err.println("   popping " + 1);
            stacksize--;
            Type result = stack[stacksize];
            stack[stacksize] = null;
            Assert.check(result != null && width(result) == 1);
            return result;
        }

        Type peek() {
            return stack[stacksize-1];
        }

        Type pop2() {
            if (debugCode) System.err.println("   popping " + 2);
            stacksize -= 2;
            Type result = stack[stacksize];
            stack[stacksize] = null;
            Assert.check(stack[stacksize+1] == null
                    && result != null && width(result) == 2);
            return result;
        }

        void pop(int n) {
            if (debugCode) System.err.println("   popping " + n);
            while (n > 0) {
                stack[--stacksize] = null;
                n--;
            }
        }

        void pop(Type t) {
            pop(width(t));
        }

        /** Force the top of the stack to be treated as this supertype
         *  of its current type. */
        void forceStackTop(Type t) {
            if (!alive) return;
            switch (t.getTag()) {
            case CLASS:
            case ARRAY:
                int width = width(t);
                Type old = stack[stacksize-width];
                Assert.check(types.isSubtype(types.erasure(old),
                                       types.erasure(t)));
                stack[stacksize-width] = t;
                break;
            default:
            }
        }

        void markInitialized(UninitializedType old) {
            Type newtype = old.initializedType();
            for (int i=0; i<stacksize; i++) {
                if (stack[i] == old) stack[i] = newtype;
            }
            for (int i=0; i<lvar.length; i++) {
                LocalVar lv = lvar[i];
                if (lv != null && lv.sym.type == old) {
                    VarSymbol sym = lv.sym;
                    sym = sym.clone(sym.owner);
                    sym.type = newtype;
                    LocalVar newlv = lvar[i] = new LocalVar(sym);
                    newlv.aliveRanges = lv.aliveRanges;
                }
            }
        }

        State join(State other) {
            defined.andSet(other.defined);
            Assert.check(stacksize == other.stacksize
                    && nlocks == other.nlocks);
            for (int i=0; i<stacksize; ) {
                Type t = stack[i];
                Type tother = other.stack[i];
                Type result =
                    t==tother ? t :
                    types.isSubtype(t, tother) ? tother :
                    types.isSubtype(tother, t) ? t :
                    error();
                int w = width(result);
                stack[i] = result;
                if (w == 2) Assert.checkNull(stack[i+1]);
                i += w;
            }
            return this;
        }

        Type error() {
            throw new AssertionError("inconsistent stack types at join point");
        }

        void dump() {
            dump(-1);
        }

        void dump(int pc) {
            System.err.print("stackMap for " + meth.owner + "." + meth);
            if (pc == -1)
                System.out.println();
            else
                System.out.println(" at " + pc);
            System.err.println(" stack (from bottom):");
            for (int i=0; i<stacksize; i++)
                System.err.println("  " + i + ": " + stack[i]);

            int lastLocal = 0;
            for (int i=max_locals-1; i>=0; i--) {
                if (defined.isMember(i)) {
                    lastLocal = i;
                    break;
                }
            }
            if (lastLocal >= 0)
                System.err.println(" locals:");
            for (int i=0; i<=lastLocal; i++) {
                System.err.print("  " + i + ": ");
                if (defined.isMember(i)) {
                    LocalVar var = lvar[i];
                    if (var == null) {
                        System.err.println("(none)");
                    } else if (var.sym == null)
                        System.err.println("UNKNOWN!");
                    else
                        System.err.println("" + var.sym + " of type " +
                                           var.sym.erasure(types));
                } else {
                    System.err.println("undefined");
                }
            }
            if (nlocks != 0) {
                System.err.print(" locks:");
                for (int i=0; i<nlocks; i++) {
                    System.err.print(" " + locks[i]);
                }
                System.err.println();
            }
        }
    }

    static final Type jsrReturnValue = new JCPrimitiveType(INT, null);


/* **************************************************************************
 * Local variables
 ****************************************************************************/

    /** A live range of a local variable. */
    static class LocalVar {
        final VarSymbol sym;
        final char reg;

        class Range {
            char start_pc = Character.MAX_VALUE;
            char length = Character.MAX_VALUE;

            Range() {}

            Range(char start) {
                this.start_pc = start;
            }

            Range(char start, char length) {
                this.start_pc = start;
                this.length = length;
            }

            boolean closed() {
                return start_pc != Character.MAX_VALUE && length != Character.MAX_VALUE;
            }

            @Override
            public String toString() {
                int currentStartPC = start_pc;
                int currentLength = length;
                return "startpc = " + currentStartPC + " length " + currentLength;
            }
        }

        java.util.List<Range> aliveRanges = new java.util.ArrayList<>();

        LocalVar(VarSymbol v) {
            this.sym = v;
            this.reg = (char)v.adr;
        }
        public LocalVar dup() {
            return new LocalVar(sym);
        }

        Range firstRange() {
            return aliveRanges.isEmpty() ? null : aliveRanges.get(0);
        }

        Range lastRange() {
            return aliveRanges.isEmpty() ? null : aliveRanges.get(aliveRanges.size() - 1);
        }

        void removeLastRange() {
            Range lastRange = lastRange();
            if (lastRange != null) {
                aliveRanges.remove(lastRange);
            }
        }

        @Override
        public String toString() {
            if (aliveRanges == null) {
                return "empty local var";
            }
            StringBuilder sb = new StringBuilder().append(sym)
                    .append(" in register ").append((int)reg).append(" \n");
            for (Range r : aliveRanges) {
                sb.append(" starts at pc=").append(Integer.toString(((int)r.start_pc)))
                    .append(" length=").append(Integer.toString(((int)r.length)))
                    .append("\n");
            }
            return sb.toString();
        }

        public void openRange(char start) {
            if (!hasOpenRange()) {
                aliveRanges.add(new Range(start));
            }
        }

        public void closeRange(char length) {
            if (isLastRangeInitialized() && length > 0) {
                Range range = lastRange();
                if (range != null) {
                    if (range.length == Character.MAX_VALUE) {
                        range.length = length;
                    }
                }
            } else {
                removeLastRange();
            }
        }

        public boolean hasOpenRange() {
            if (aliveRanges.isEmpty()) {
                return false;
            }
            return lastRange().length == Character.MAX_VALUE;
        }

        public boolean isLastRangeInitialized() {
            if (aliveRanges.isEmpty()) {
                return false;
            }
            return lastRange().start_pc != Character.MAX_VALUE;
        }

        public Range getWidestRange() {
            if (aliveRanges.isEmpty()) {
                return new Range();
            } else {
                Range firstRange = firstRange();
                Range lastRange = lastRange();
                char length = (char)(lastRange.length + (lastRange.start_pc - firstRange.start_pc));
                return new Range(firstRange.start_pc, length);
            }
         }

    }

    /** Local variables, indexed by register. */
    LocalVar[] lvar;

    /** Add a new local variable. */
    private void addLocalVar(VarSymbol v) {
        int adr = v.adr;
        lvar = ArrayUtils.ensureCapacity(lvar, adr+1);
        Assert.checkNull(lvar[adr]);
        if (pendingJumps != null) {
            resolvePending();
        }
        lvar[adr] = new LocalVar(v);
        state.defined.excl(adr);
    }

    void adjustAliveRanges(int oldCP, int delta) {
        for (LocalVar localVar: lvar) {
            if (localVar != null) {
                for (LocalVar.Range range: localVar.aliveRanges) {
                    if (range.closed() && range.start_pc + range.length >= oldCP) {
                        range.length += delta;
                    }
                }
            }
        }
    }

    /**
     * Calculates the size of the LocalVariableTable.
     */
    public int getLVTSize() {
        int result = varBufferSize;
        for (int i = 0; i < varBufferSize; i++) {
            LocalVar var = varBuffer[i];
            result += var.aliveRanges.size() - 1;
        }
        return result;
    }

    /** Set the current variable defined state. */
    public void setDefined(Bits newDefined) {
        if (alive && newDefined != state.defined) {
            Bits diff = new Bits(state.defined).xorSet(newDefined);
            for (int adr = diff.nextBit(0);
                 adr >= 0;
                 adr = diff.nextBit(adr+1)) {
                if (adr >= nextreg)
                    state.defined.excl(adr);
                else if (state.defined.isMember(adr))
                    setUndefined(adr);
                else
                    setDefined(adr);
            }
        }
    }

    /** Mark a register as being (possibly) defined. */
    public void setDefined(int adr) {
        LocalVar v = lvar[adr];
        if (v == null) {
            state.defined.excl(adr);
        } else {
            state.defined.incl(adr);
            if (cp < Character.MAX_VALUE) {
                v.openRange((char)cp);
            }
        }
    }

    /** Mark a register as being undefined. */
    public void setUndefined(int adr) {
        state.defined.excl(adr);
        if (adr < lvar.length &&
            lvar[adr] != null &&
            lvar[adr].isLastRangeInitialized()) {
            LocalVar v = lvar[adr];
            char length = (char)(curCP() - v.lastRange().start_pc);
            if (length < Character.MAX_VALUE) {
                lvar[adr] = v.dup();
                v.closeRange(length);
                putVar(v);
                fillLocalVarPosition(v);
            } else {
                v.removeLastRange();
            }
        }
    }

    /** End the scope of a variable. */
    private void endScope(int adr) {
        LocalVar v = lvar[adr];
        if (v != null) {
            if (v.isLastRangeInitialized()) {
                char length = (char)(curCP() - v.lastRange().start_pc);
                if (length < Character.MAX_VALUE) {
                    v.closeRange(length);
                    putVar(v);
                    fillLocalVarPosition(v);
                }
            }
            /** the call to curCP() can implicitly adjust the current cp, if so
             * the alive range of local variables may be modified. Thus we need
             * all of them. For this reason assigning null to the given address
             * should be the last action to do.
             */
            lvar[adr] = null;
        }
        state.defined.excl(adr);
    }

    private void fillLocalVarPosition(LocalVar lv) {
        if (lv == null || lv.sym == null || lv.sym.isExceptionParameter()|| !lv.sym.hasTypeAnnotations())
            return;
        LocalVar.Range[] validRanges = lv.aliveRanges.stream().filter(r -> r.closed() && r.length > 0).toArray(s -> new LocalVar.Range[s]);
        if (validRanges.length == 0)
            return ;
        int[] lvarOffset = Arrays.stream(validRanges).mapToInt(r -> r.start_pc).toArray();
        int[] lvarLength = Arrays.stream(validRanges).mapToInt(r -> r.length).toArray();
        int[] lvarIndex = Arrays.stream(validRanges).mapToInt(r -> lv.reg).toArray();
        for (Attribute.TypeCompound ta : lv.sym.getRawTypeAttributes()) {
            TypeAnnotationPosition p = ta.position;
            p.lvarOffset = appendArray(p.lvarOffset, lvarOffset);
            p.lvarLength = appendArray(p.lvarLength, lvarLength);
            p.lvarIndex = appendArray(p.lvarIndex, lvarIndex);
            p.isValidOffset = true;
        }
    }

    private int[] appendArray(int[] source, int[] append) {
        if (source == null || source.length == 0) return append;

        int[] result = new int[source.length + append.length];

        System.arraycopy(source, 0, result, 0, source.length);
        System.arraycopy(append, 0, result, source.length, append.length);
        return result;
    }

    // Method to be called after compressCatchTable to
    // fill in the exception table index for type
    // annotations on exception parameters.
    public void fillExceptionParameterPositions() {
        for (int i = 0; i < varBufferSize; ++i) {
            LocalVar lv = varBuffer[i];
            if (lv == null || lv.sym == null
                    || !lv.sym.hasTypeAnnotations()
                    || !lv.sym.isExceptionParameter())
                continue;

            for (Attribute.TypeCompound ta : lv.sym.getRawTypeAttributes()) {
                TypeAnnotationPosition p = ta.position;
                if (p.hasCatchType()) {
                    final int idx = findExceptionIndex(p);
                    if (idx == -1)
                        Assert.error("Could not find exception index for type annotation " +
                                     ta + " on exception parameter");
                    p.setExceptionIndex(idx);
                }
            }
        }
    }

    private int findExceptionIndex(TypeAnnotationPosition p) {
        final int catchType = p.getCatchType();
        final int startPos = p.getStartPos();
        final int len = catchInfo.length();
        List<char[]> iter = catchInfo.toList();
        for (int i = 0; i < len; ++i) {
            char[] catchEntry = iter.head;
            iter = iter.tail;
            int ct = catchEntry[3];
            int sp = catchEntry[0];
            if (catchType == ct && sp == startPos) {
                return i;
            }
        }
        return -1;
    }

    /** Put a live variable range into the buffer to be output to the
     *  class file.
     */
    void putVar(LocalVar var) {
        // Keep local variables if
        // 1) we need them for debug information
        // 2) it is an exception type and it contains type annotations
        boolean keepLocalVariables = varDebugInfo ||
            (var.sym.isExceptionParameter() && var.sym.hasTypeAnnotations());
        if (!keepLocalVariables) return;
        //don't keep synthetic vars, unless they are lambda method parameters
        boolean ignoredSyntheticVar = (var.sym.flags() & Flags.SYNTHETIC) != 0 &&
                ((var.sym.owner.flags() & Flags.LAMBDA_METHOD) == 0 ||
                 (var.sym.flags() & Flags.PARAMETER) == 0);
        if (ignoredSyntheticVar) return;
        if (varBuffer == null)
            varBuffer = new LocalVar[20];
        else
            varBuffer = ArrayUtils.ensureCapacity(varBuffer, varBufferSize);
        varBuffer[varBufferSize++] = var;
    }

    /** Previously live local variables, to be put into the variable table. */
    LocalVar[] varBuffer;
    int varBufferSize;

    /** Create a new local variable address and return it.
     */
    private int newLocal(int typecode) {
        int reg = nextreg;
        int w = width(typecode);
        nextreg = reg + w;
        if (nextreg > max_locals) max_locals = nextreg;
        return reg;
    }

    private int newLocal(Type type) {
        return newLocal(typecode(type));
    }

    public int newLocal(VarSymbol v) {
        int reg = v.adr = newLocal(v.erasure(types));
        addLocalVar(v);
        return reg;
    }

    /** Start a set of fresh registers.
     */
    public void newRegSegment() {
        nextreg = max_locals;
    }

    /** End scopes of all variables with registers &ge; first.
     */
    public void endScopes(int first) {
        int prevNextReg = nextreg;
        nextreg = first;
        for (int i = nextreg; i < prevNextReg; i++) endScope(i);
    }

/**************************************************************************
 * static tables
 *************************************************************************/

    public static String mnem(int opcode) {
        return Mneumonics.mnem[opcode];
    }

    private static class Mneumonics {
        private static final String[] mnem = new String[ByteCodeCount];
        static {
            mnem[nop] = "nop";
            mnem[aconst_null] = "aconst_null";
            mnem[iconst_m1] = "iconst_m1";
            mnem[iconst_0] = "iconst_0";
            mnem[iconst_1] = "iconst_1";
            mnem[iconst_2] = "iconst_2";
            mnem[iconst_3] = "iconst_3";
            mnem[iconst_4] = "iconst_4";
            mnem[iconst_5] = "iconst_5";
            mnem[lconst_0] = "lconst_0";
            mnem[lconst_1] = "lconst_1";
            mnem[fconst_0] = "fconst_0";
            mnem[fconst_1] = "fconst_1";
            mnem[fconst_2] = "fconst_2";
            mnem[dconst_0] = "dconst_0";
            mnem[dconst_1] = "dconst_1";
            mnem[bipush] = "bipush";
            mnem[sipush] = "sipush";
            mnem[ldc1] = "ldc1";
            mnem[ldc2] = "ldc2";
            mnem[ldc2w] = "ldc2w";
            mnem[iload] = "iload";
            mnem[lload] = "lload";
            mnem[fload] = "fload";
            mnem[dload] = "dload";
            mnem[aload] = "aload";
            mnem[iload_0] = "iload_0";
            mnem[lload_0] = "lload_0";
            mnem[fload_0] = "fload_0";
            mnem[dload_0] = "dload_0";
            mnem[aload_0] = "aload_0";
            mnem[iload_1] = "iload_1";
            mnem[lload_1] = "lload_1";
            mnem[fload_1] = "fload_1";
            mnem[dload_1] = "dload_1";
            mnem[aload_1] = "aload_1";
            mnem[iload_2] = "iload_2";
            mnem[lload_2] = "lload_2";
            mnem[fload_2] = "fload_2";
            mnem[dload_2] = "dload_2";
            mnem[aload_2] = "aload_2";
            mnem[iload_3] = "iload_3";
            mnem[lload_3] = "lload_3";
            mnem[fload_3] = "fload_3";
            mnem[dload_3] = "dload_3";
            mnem[aload_3] = "aload_3";
            mnem[iaload] = "iaload";
            mnem[laload] = "laload";
            mnem[faload] = "faload";
            mnem[daload] = "daload";
            mnem[aaload] = "aaload";
            mnem[baload] = "baload";
            mnem[caload] = "caload";
            mnem[saload] = "saload";
            mnem[istore] = "istore";
            mnem[lstore] = "lstore";
            mnem[fstore] = "fstore";
            mnem[dstore] = "dstore";
            mnem[astore] = "astore";
            mnem[istore_0] = "istore_0";
            mnem[lstore_0] = "lstore_0";
            mnem[fstore_0] = "fstore_0";
            mnem[dstore_0] = "dstore_0";
            mnem[astore_0] = "astore_0";
            mnem[istore_1] = "istore_1";
            mnem[lstore_1] = "lstore_1";
            mnem[fstore_1] = "fstore_1";
            mnem[dstore_1] = "dstore_1";
            mnem[astore_1] = "astore_1";
            mnem[istore_2] = "istore_2";
            mnem[lstore_2] = "lstore_2";
            mnem[fstore_2] = "fstore_2";
            mnem[dstore_2] = "dstore_2";
            mnem[astore_2] = "astore_2";
            mnem[istore_3] = "istore_3";
            mnem[lstore_3] = "lstore_3";
            mnem[fstore_3] = "fstore_3";
            mnem[dstore_3] = "dstore_3";
            mnem[astore_3] = "astore_3";
            mnem[iastore] = "iastore";
            mnem[lastore] = "lastore";
            mnem[fastore] = "fastore";
            mnem[dastore] = "dastore";
            mnem[aastore] = "aastore";
            mnem[bastore] = "bastore";
            mnem[castore] = "castore";
            mnem[sastore] = "sastore";
            mnem[pop] = "pop";
            mnem[pop2] = "pop2";
            mnem[dup] = "dup";
            mnem[dup_x1] = "dup_x1";
            mnem[dup_x2] = "dup_x2";
            mnem[dup2] = "dup2";
            mnem[dup2_x1] = "dup2_x1";
            mnem[dup2_x2] = "dup2_x2";
            mnem[swap] = "swap";
            mnem[iadd] = "iadd";
            mnem[ladd] = "ladd";
            mnem[fadd] = "fadd";
            mnem[dadd] = "dadd";
            mnem[isub] = "isub";
            mnem[lsub] = "lsub";
            mnem[fsub] = "fsub";
            mnem[dsub] = "dsub";
            mnem[imul] = "imul";
            mnem[lmul] = "lmul";
            mnem[fmul] = "fmul";
            mnem[dmul] = "dmul";
            mnem[idiv] = "idiv";
            mnem[ldiv] = "ldiv";
            mnem[fdiv] = "fdiv";
            mnem[ddiv] = "ddiv";
            mnem[imod] = "imod";
            mnem[lmod] = "lmod";
            mnem[fmod] = "fmod";
            mnem[dmod] = "dmod";
            mnem[ineg] = "ineg";
            mnem[lneg] = "lneg";
            mnem[fneg] = "fneg";
            mnem[dneg] = "dneg";
            mnem[ishl] = "ishl";
            mnem[lshl] = "lshl";
            mnem[ishr] = "ishr";
            mnem[lshr] = "lshr";
            mnem[iushr] = "iushr";
            mnem[lushr] = "lushr";
            mnem[iand] = "iand";
            mnem[land] = "land";
            mnem[ior] = "ior";
            mnem[lor] = "lor";
            mnem[ixor] = "ixor";
            mnem[lxor] = "lxor";
            mnem[iinc] = "iinc";
            mnem[i2l] = "i2l";
            mnem[i2f] = "i2f";
            mnem[i2d] = "i2d";
            mnem[l2i] = "l2i";
            mnem[l2f] = "l2f";
            mnem[l2d] = "l2d";
            mnem[f2i] = "f2i";
            mnem[f2l] = "f2l";
            mnem[f2d] = "f2d";
            mnem[d2i] = "d2i";
            mnem[d2l] = "d2l";
            mnem[d2f] = "d2f";
            mnem[int2byte] = "int2byte";
            mnem[int2char] = "int2char";
            mnem[int2short] = "int2short";
            mnem[lcmp] = "lcmp";
            mnem[fcmpl] = "fcmpl";
            mnem[fcmpg] = "fcmpg";
            mnem[dcmpl] = "dcmpl";
            mnem[dcmpg] = "dcmpg";
            mnem[ifeq] = "ifeq";
            mnem[ifne] = "ifne";
            mnem[iflt] = "iflt";
            mnem[ifge] = "ifge";
            mnem[ifgt] = "ifgt";
            mnem[ifle] = "ifle";
            mnem[if_icmpeq] = "if_icmpeq";
            mnem[if_icmpne] = "if_icmpne";
            mnem[if_icmplt] = "if_icmplt";
            mnem[if_icmpge] = "if_icmpge";
            mnem[if_icmpgt] = "if_icmpgt";
            mnem[if_icmple] = "if_icmple";
            mnem[if_acmpeq] = "if_acmpeq";
            mnem[if_acmpne] = "if_acmpne";
            mnem[goto_] = "goto_";
            mnem[jsr] = "jsr";
            mnem[ret] = "ret";
            mnem[tableswitch] = "tableswitch";
            mnem[lookupswitch] = "lookupswitch";
            mnem[ireturn] = "ireturn";
            mnem[lreturn] = "lreturn";
            mnem[freturn] = "freturn";
            mnem[dreturn] = "dreturn";
            mnem[areturn] = "areturn";
            mnem[return_] = "return_";
            mnem[getstatic] = "getstatic";
            mnem[putstatic] = "putstatic";
            mnem[getfield] = "getfield";
            mnem[putfield] = "putfield";
            mnem[invokevirtual] = "invokevirtual";
            mnem[invokespecial] = "invokespecial";
            mnem[invokestatic] = "invokestatic";
            mnem[invokeinterface] = "invokeinterface";
            mnem[invokedynamic] = "invokedynamic";
            mnem[new_] = "new_";
            mnem[newarray] = "newarray";
            mnem[anewarray] = "anewarray";
            mnem[arraylength] = "arraylength";
            mnem[athrow] = "athrow";
            mnem[checkcast] = "checkcast";
            mnem[instanceof_] = "instanceof_";
            mnem[monitorenter] = "monitorenter";
            mnem[monitorexit] = "monitorexit";
            mnem[wide] = "wide";
            mnem[multianewarray] = "multianewarray";
            mnem[if_acmp_null] = "if_acmp_null";
            mnem[if_acmp_nonnull] = "if_acmp_nonnull";
            mnem[goto_w] = "goto_w";
            mnem[jsr_w] = "jsr_w";
            mnem[breakpoint] = "breakpoint";
        }
    }
}
