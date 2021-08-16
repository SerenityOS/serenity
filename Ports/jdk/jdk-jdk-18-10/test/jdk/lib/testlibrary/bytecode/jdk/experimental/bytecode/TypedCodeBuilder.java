/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.experimental.bytecode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public class TypedCodeBuilder<S, T, E, C extends TypedCodeBuilder<S, T, E, C>> extends MacroCodeBuilder<S, T, E, C> {

    State lastStackMapState;
    int lastStackMapPc = -1;
    Map<CharSequence, LocalVarInfo> lvarOffsets = new HashMap<>();
    protected State state;
    int depth = 0;
    int currLocalOffset = 0;

    class StatefulPendingJump extends PendingJump {

        State state;

        StatefulPendingJump(CharSequence label, int pc, State state) {
            super(label, pc);
            this.state = state;
        }

        @Override
        boolean resolve(CharSequence label, int pc) {
            boolean b = super.resolve(label, pc);
            if (b) {
                TypedCodeBuilder.this.state = TypedCodeBuilder.this.state.merge(state);
            }
            return b;
        }
    }

    class LocalVarInfo {
        CharSequence name;
        int offset;
        int depth;
        TypeTag type;

        LocalVarInfo(CharSequence name, int offset, int depth, TypeTag type) {
            this.name = name;
            this.offset = offset;
            this.depth = depth;
            this.type = type;
        }
    }

    public TypedCodeBuilder(MethodBuilder<S, T, E> methodBuilder) {
        super(methodBuilder);
        T t = methodBuilder.desc;
        state = new State();
        if ((methodBuilder.flags & Flag.ACC_STATIC.flag) == 0) {
            T clazz = typeHelper.type(methodBuilder.thisClass);
            state.load(clazz, currLocalOffset++); //TODO: uninit??
        }
        Iterator<T> paramsIt = typeHelper.parameterTypes(t);
        while (paramsIt.hasNext()) {
            T p = paramsIt.next();
            state.load(p, currLocalOffset);
            currLocalOffset += typeHelper.tag(p).width;
        }
        lastStackMapState = state.dup();
        stacksize = state.stack.size();
        localsize = state.locals.size();
    }

    @Override
    protected C emitOp(Opcode opcode, Object optPoolValue) {
        updateState(opcode, optPoolValue);
        return super.emitOp(opcode, optPoolValue);
    }

    @Override
    protected SwitchBuilder makeSwitchBuilder() {
        return new TypedSwitchBuilder();
    }

    class TypedSwitchBuilder extends SwitchBuilder {

        @Override
        public SwitchBuilder withCase(int value, Consumer<? super C> case_, boolean fallthrough) {
            super.withCase(value, c -> {
                withLocalScope(() -> {
                    State prevState = state;
                    state = prevState.dup();
                    emitStackMap(c.offset());
                    case_.accept(c);
                    state = prevState;
                });
            }, fallthrough);
            return this;
        }

        @Override
        public SwitchBuilder withDefault(Consumer<? super C> defaultCase) {
            super.withDefault(c -> {
                withLocalScope(() -> {
                    State prevState = state;
                    state = prevState.dup();
                    emitStackMap(c.offset());
                    defaultCase.accept(c);
                    state = prevState;
                });
            });
            return this;
        }
    }

    @Override
    public StatefulTypedBuilder typed(TypeTag tag) {
        return super.typed(tag, StatefulTypedBuilder::new);
    }

    public class StatefulTypedBuilder extends LabelledTypedBuilder {

        TypeTag tag;

        StatefulTypedBuilder(TypeTag tag) {
            this.tag = tag;
        }

        @Override
        public C astore_0() {
            return storeAndUpdate(super::astore_0);
        }

        @Override
        public C astore_1() {
            return storeAndUpdate(super::astore_1);
        }

        @Override
        public C astore_2() {
            return storeAndUpdate(super::astore_2);
        }

        @Override
        public C astore_3() {
            return storeAndUpdate(super::astore_3);
        }

        @Override
        public C astore(int n) {
            return storeAndUpdate(() -> super.astore(n));
        }

        @Override
        public C aastore() {
            return storeAndUpdate(super::aastore);
        }

        @Override
        public C areturn() {
            state.pop(tag);
            state.push(typeHelper.nullType());
            return super.areturn();
        }

        @Override
        public C anewarray(S s) {
            super.anewarray(s);
            state.pop();
            state.push(typeHelper.arrayOf(typeHelper.type(s)));
            return thisBuilder();
        }

        @Override
        public C aconst_null() {
            super.aconst_null();
            state.pop();
            state.push(tag);
            return thisBuilder();
        }

        public C if_acmpeq(CharSequence label) {
            return jumpAndUpdate(() -> super.if_acmpeq(label));
        }

        public C if_acmpne(CharSequence label) {
            return jumpAndUpdate(() -> super.if_acmpne(label));
        }

        private C storeAndUpdate(Supplier<C> op) {
            state.pop(tag);
            state.push(typeHelper.nullType());
            return op.get();
        }

        private C jumpAndUpdate(Supplier<C> op) {
            state.pop(tag);
            state.pop(tag);
            state.push(typeHelper.nullType());
            state.push(typeHelper.nullType());
            return op.get();
        }
    }

    public class State {
        public final ArrayList<T> stack;
        public final Vector<T> locals;
        boolean alive;

        State(ArrayList<T> stack, Vector<T> locals) {
            this.stack = stack;
            this.locals = locals;
        }

        State() {
            this(new ArrayList<>(), new Vector<>());
        }

        void push(TypeTag tag) {
            switch (tag) {
                case A:
                case V:
                    throw new IllegalStateException("Bad type tag");
                default:
                    push(typeHelper.fromTag(tag));
            }
        }

        void push(T t) {
            stack.add(t);
            if (width(t) == 2) {
                stack.add(null);
            }
            if (stack.size() > stacksize) {
                stacksize = stack.size();
            }
        }

        T peek() {
            return stack.get(stack.size() - 1);
        }

        T tosType() {
            T tos = peek();
            if (tos == null) {
                //double slot
                tos = stack.get(stack.size() - 2);
            }
            return tos;
        }

        T popInternal() {
            return stack.remove(stack.size() - 1);
        }

        @SuppressWarnings("unchecked")
        T pop() {
            if (stack.size() == 0 || peek() == null) throw new IllegalStateException();
            return popInternal();
        }

        T pop2() {
            T o = stack.get(stack.size() - 2);
            TypeTag t = typeHelper.tag(o);
            if (t.width != 2) throw new IllegalStateException();
            popInternal();
            popInternal();
            return o;
        }

        T pop(TypeTag t) {
            return (t.width() == 2) ?
                pop2() : pop();
        }

        void load(TypeTag tag, int index) {
            if (tag == TypeTag.A) throw new IllegalStateException("Bad type tag");
            load(typeHelper.fromTag(tag), index);
        }

        void load(T t, int index) {
            ensureDefined(index);
            locals.set(index, t);
            if (width(t) == 2) {
                locals.add(null);
            }
            if (locals.size() > localsize) {
                localsize = locals.size();
            }
        }

        void ensureDefined(int index) {
            if (index >= locals.size()) {
                locals.setSize(index + 1);
            }
        }

        State dup() {
            State newState = new State(new ArrayList<>(stack), new Vector<>(locals));
            return newState;
        }

        State merge(State that) {
            if (!alive) { return that; }
            if (that.stack.size() != stack.size()) {
                throw new IllegalStateException("Bad stack size at merge point");
            }
            for (int i = 0; i < stack.size(); i++) {
                T t1 = stack.get(i);
                T t2 = that.stack.get(i);
                stack.set(i, merge(t1, t2, "Bad stack type at merge point"));
            }
            int nlocals = locals.size() > that.locals.size() ? that.locals.size() : locals.size();
            for (int i = 0; i < nlocals; i++) {
                T t1 = locals.get(i);
                T t2 = that.locals.get(i);
                locals.set(i, merge(t1, t2, "Bad local type at merge point"));
            }
            if (locals.size() > nlocals) {
                for (int i = nlocals; i < locals.size(); i++) {
                    locals.remove(i);
                }
            }
            return this;
        }

        T merge(T t1, T t2, String msg) {
            if (t1 == null && t2 == null) {
                return t1;
            }
            T res;
            TypeTag tag1 = typeHelper.tag(t1);
            TypeTag tag2 = typeHelper.tag(t2);
            if (tag1 != TypeTag.A && tag2 != TypeTag.A &&
                    tag1 != TypeTag.Q && tag2 != TypeTag.Q) {
                res = typeHelper.fromTag(TypeTag.commonSupertype(tag1, tag2));
            } else if (t1 == typeHelper.nullType()) {
                res = t2;
            } else if (t2 == typeHelper.nullType()) {
                res = t1;
            } else {
                res = typeHelper.commonSupertype(t1, t2);
            }
            if (res == null) {
                throw new IllegalStateException(msg);
            }
            return res;
        }

        @Override
        public String toString() {
            return String.format("[locals = %s, stack = %s]", locals, stack);
        }
    }

    int width(T o) {
        return o == typeHelper.nullType() ?
                TypeTag.A.width() :
                typeHelper.tag(o).width;
    }

    @SuppressWarnings("unchecked")
    public void updateState(Opcode op, Object optValue) {
        switch (op) {
            case VALOAD:
            case AALOAD:
                state.pop();
                state.push(typeHelper.elemtype(state.pop()));
                break;
            case GOTO_:
                state.alive = false;
                break;
            case NOP:
            case INEG:
            case LNEG:
            case FNEG:
            case DNEG:
                break;
            case ACONST_NULL:
                state.push(typeHelper.nullType());
                break;
            case ICONST_M1:
            case ICONST_0:
            case ICONST_1:
            case ICONST_2:
            case ICONST_3:
            case ICONST_4:
            case ICONST_5:
                state.push(TypeTag.I);
                break;
            case LCONST_0:
            case LCONST_1:
                state.push(TypeTag.J);
                break;
            case FCONST_0:
            case FCONST_1:
            case FCONST_2:
                state.push(TypeTag.F);
                break;
            case DCONST_0:
            case DCONST_1:
                state.push(TypeTag.D);
                break;
            case ILOAD_0:
            case FLOAD_0:
            case ALOAD_0:
            case LLOAD_0:
            case DLOAD_0:
                state.push(state.locals.get(0));
                break;
            case ILOAD_1:
            case FLOAD_1:
            case ALOAD_1:
            case LLOAD_1:
            case DLOAD_1:
                state.push(state.locals.get(1));
                break;
            case ILOAD_2:
            case FLOAD_2:
            case ALOAD_2:
            case LLOAD_2:
            case DLOAD_2:
                state.push(state.locals.get(2));
                break;
            case ILOAD_3:
            case FLOAD_3:
            case ALOAD_3:
            case LLOAD_3:
            case DLOAD_3:
                state.push(state.locals.get(3));
                break;
            case ILOAD:
            case FLOAD:
            case ALOAD:
            case LLOAD:
            case DLOAD:
            case VLOAD:
                state.push(state.locals.get((Integer) optValue));
                break;
            case IALOAD:
            case BALOAD:
            case CALOAD:
            case SALOAD:
                state.pop();
                state.pop();
                state.push(TypeTag.I);
                break;
            case LALOAD:
                state.pop();
                state.pop();
                state.push(TypeTag.J);
                break;
            case FALOAD:
                state.pop();
                state.pop();
                state.push(TypeTag.F);
                break;
            case DALOAD:
                state.pop();
                state.pop();
                state.push(TypeTag.D);
                break;
            case ISTORE_0:
            case FSTORE_0:
            case ASTORE_0:
                state.load(state.pop(), 0);
                break;
            case ISTORE_1:
            case FSTORE_1:
            case ASTORE_1:
                state.load(state.pop(), 1);
                break;
            case ISTORE_2:
            case FSTORE_2:
            case ASTORE_2:
                state.load(state.pop(), 2);
                break;
            case ISTORE_3:
            case FSTORE_3:
            case ASTORE_3:
                state.load(state.pop(), 3);
                break;
            case ISTORE:
            case FSTORE:
            case ASTORE:
            case VSTORE:
                state.load(state.pop(), (int) optValue);
                break;
            case LSTORE_0:
            case DSTORE_0:
                state.load(state.pop2(), 0);
                break;
            case LSTORE_1:
            case DSTORE_1:
                state.load(state.pop2(), 1);
                break;
            case LSTORE_2:
            case DSTORE_2:
                state.load(state.pop2(), 2);
                break;
            case LSTORE_3:
            case DSTORE_3:
                state.load(state.pop2(), 3);
                break;
            case LSTORE:
            case DSTORE:
                state.load(state.pop2(), (int) optValue);
                break;
            case POP:
            case LSHR:
            case LSHL:
            case LUSHR:
                state.pop();
                break;
            case VRETURN:
            case ARETURN:
            case IRETURN:
            case FRETURN:
                state.pop();
                break;
            case ATHROW:
                state.pop();
                break;
            case POP2:
                state.pop2();
                break;
            case LRETURN:
            case DRETURN:
                state.pop2();
                break;
            case DUP:
                state.push(state.peek());
                break;
            case RETURN:
                break;
            case ARRAYLENGTH:
                state.pop();
                state.push(TypeTag.I);
                break;
            case ISUB:
            case IADD:
            case IMUL:
            case IDIV:
            case IREM:
            case ISHL:
            case ISHR:
            case IUSHR:
            case IAND:
            case IOR:
            case IXOR:
                state.pop();
                state.pop();
                state.push(TypeTag.I);
                break;
            case VASTORE:
            case AASTORE:
                state.pop();
                state.pop();
                state.pop();
                break;
            case LAND:
            case LOR:
            case LXOR:
            case LREM:
            case LDIV:
            case LMUL:
            case LSUB:
            case LADD:
                state.pop2();
                state.pop2();
                state.push(TypeTag.J);
                break;
            case LCMP:
                state.pop2();
                state.pop2();
                state.push(TypeTag.I);
                break;
            case L2I:
                state.pop2();
                state.push(TypeTag.I);
                break;
            case I2L:
                state.pop();
                state.push(TypeTag.J);
                break;
            case I2F:
                state.pop();
                state.push(TypeTag.F);
                break;
            case I2D:
                state.pop();
                state.push(TypeTag.D);
                break;
            case L2F:
                state.pop2();
                state.push(TypeTag.F);
                break;
            case L2D:
                state.pop2();
                state.push(TypeTag.D);
                break;
            case F2I:
                state.pop();
                state.push(TypeTag.I);
                break;
            case F2L:
                state.pop();
                state.push(TypeTag.J);
                break;
            case F2D:
                state.pop();
                state.push(TypeTag.D);
                break;
            case D2I:
                state.pop2();
                state.push(TypeTag.I);
                break;
            case D2L:
                state.pop2();
                state.push(TypeTag.J);
                break;
            case D2F:
                state.pop2();
                state.push(TypeTag.F);
                break;
            case TABLESWITCH:
            case LOOKUPSWITCH:
                state.pop();
                break;
            case DUP_X1: {
                T val1 = state.pop();
                T val2 = state.pop();
                state.push(val1);
                state.push(val2);
                state.push(val1);
                break;
            }
            case BASTORE:
                state.pop();
                state.pop();
                state.pop();
                break;
            case I2B:
            case I2C:
            case I2S:
                break;
            case FMUL:
            case FADD:
            case FSUB:
            case FDIV:
            case FREM:
                state.pop();
                state.pop();
                state.push(TypeTag.F);
                break;
            case CASTORE:
            case IASTORE:
            case FASTORE:
            case SASTORE:
                state.pop();
                state.pop();
                state.pop();
                break;
            case LASTORE:
            case DASTORE:
                state.pop2();
                state.pop();
                state.pop();
                break;
            case DUP2:
                if (state.peek() != null) {
                    //form 1
                    T value1 = state.pop();
                    T value2 = state.pop();
                    state.push(value2);
                    state.push(value1);
                    state.push(value2);
                    state.push(value1);
                } else {
                    //form 2
                    T value = state.pop2();
                    state.push(value);
                    state.push(value);
                }
                break;
            case DUP2_X1:
                if (state.peek() != null) {
                    T value1 = state.pop();
                    T value2 = state.pop();
                    T value3 = state.pop();
                    state.push(value2);
                    state.push(value1);
                    state.push(value3);
                    state.push(value2);
                    state.push(value1);
                } else {
                    T value1 = state.pop2();
                    T value2 = state.pop();
                    state.push(value1);
                    state.push(value2);
                    state.push(value1);
                }
                break;
            case DUP2_X2:
                if (state.peek() != null) {
                    T value1 = state.pop();
                    T value2 = state.pop();
                    if (state.peek() != null) {
                        // form 1
                        T value3 = state.pop();
                        T value4 = state.pop();
                        state.push(value2);
                        state.push(value1);
                        state.push(value4);
                        state.push(value3);
                        state.push(value2);
                        state.push(value1);
                    } else {
                        // form 3
                        T value3 = state.pop2();
                        state.push(value2);
                        state.push(value1);
                        state.push(value3);
                        state.push(value2);
                        state.push(value1);
                    }
                } else {
                    T value1 = state.pop2();
                    if (state.peek() != null) {
                        // form 2
                        T value2 = state.pop();
                        T value3 = state.pop();
                        state.push(value1);
                        state.push(value3);
                        state.push(value2);
                        state.push(value1);
                    } else {
                        // form 4
                        T value2 = state.pop2();
                        state.push(value1);
                        state.push(value2);
                        state.push(value1);
                    }
                }
                break;
            case DUP_X2: {
                T value1 = state.pop();
                if (state.peek() != null) {
                    // form 1
                    T value2 = state.pop();
                    T value3 = state.pop();
                    state.push(value1);
                    state.push(value3);
                    state.push(value2);
                    state.push(value1);
                } else {
                    // form 2
                    T value2 = state.pop2();
                    state.push(value1);
                    state.push(value2);
                    state.push(value1);
                }
            }
            break;
            case FCMPL:
            case FCMPG:
                state.pop();
                state.pop();
                state.push(TypeTag.I);
                break;
            case DCMPL:
            case DCMPG:
                state.pop2();
                state.pop2();
                state.push(TypeTag.I);
                break;
            case SWAP: {
                T value1 = state.pop();
                T value2 = state.pop();
                state.push(value1);
                state.push(value2);
                break;
            }
            case DADD:
            case DSUB:
            case DMUL:
            case DDIV:
            case DREM:
                state.pop2();
                state.pop2();
                state.push(TypeTag.D);
                break;
            case RET:
                break;
            case WIDE:
                // must be handled by the caller.
                return;
            case MONITORENTER:
            case MONITOREXIT:
                state.pop();
                break;
            case VNEW:
            case NEW:
                state.push(typeHelper.type((S) optValue));
                break;
            case NEWARRAY:
                state.pop();
                state.push(typeHelper.arrayOf(typeHelper.fromTag((TypeTag) optValue)));
                break;
            case ANEWARRAY:
                state.pop();
                state.push(typeHelper.arrayOf(typeHelper.arrayOf(typeHelper.type((S)optValue))));
                break;
            case VNEWARRAY:
            case VBOX:
            case VUNBOX:
                state.pop();
                state.push(typeHelper.type((S) optValue));
                break;
            case MULTIVNEWARRAY:
            case MULTIANEWARRAY:
                for (int i = 0; i < (byte) ((Object[]) optValue)[1]; i++) {
                    state.pop();
                }
                state.push(typeHelper.type((S) ((Object[]) optValue)[0]));
                break;
            case INVOKEINTERFACE:
            case INVOKEVIRTUAL:
            case INVOKESPECIAL:
            case INVOKESTATIC:
            case INVOKEDYNAMIC:
                processInvoke(op, (T) optValue);
                break;
            case GETSTATIC:
                state.push((T) optValue);
                break;
            case VGETFIELD:
            case GETFIELD:
                state.pop();
                state.push((T) optValue);
                break;
            case PUTSTATIC: {
                TypeTag tag = typeHelper.tag((T) optValue);
                if (tag.width == 1) {
                    state.pop();
                } else {
                    state.pop2();
                }
                break;
            }
            case PUTFIELD: {
                TypeTag tag = typeHelper.tag((T) optValue);
                if (tag.width == 1) {
                    state.pop();
                } else {
                    state.pop2();
                }
                state.pop();
                break;
            }
            case BIPUSH:
            case SIPUSH:
                state.push(TypeTag.I);
                break;
            case LDC:
            case LDC_W:
            case LDC2_W:
                state.push((T)optValue);
                break;
            case IF_ACMPEQ:
            case IF_ICMPEQ:
            case IF_ACMPNE:
            case IF_ICMPGE:
            case IF_ICMPGT:
            case IF_ICMPLE:
            case IF_ICMPLT:
            case IF_ICMPNE:
                state.pop();
                state.pop();
                break;
            case IF_NONNULL:
            case IF_NULL:
            case IFEQ:
            case IFGE:
            case IFGT:
            case IFLE:
            case IFLT:
            case IFNE:
                state.pop();
                break;
            case INSTANCEOF:
                state.pop();
                state.push(TypeTag.Z);
                break;
            case TYPED:
            case CHECKCAST:
                break;

            default:
                throw new UnsupportedOperationException("Unsupported opcode: " + op);
        }
    }

    void processInvoke(Opcode opcode, T invokedType) {
        Iterator<T> paramsIt = typeHelper.parameterTypes(invokedType);
        while (paramsIt.hasNext()) {
            T t = paramsIt.next();
            TypeTag tag = typeHelper.tag(t);
            if (tag.width == 2) {
                state.popInternal();
                state.popInternal();
            } else {
                state.popInternal();
            }
        }
        if (opcode != Opcode.INVOKESTATIC && opcode != Opcode.INVOKEDYNAMIC) {
            state.pop(); //receiver
        }
        T retType = typeHelper.returnType(invokedType);
        TypeTag retTag = typeHelper.tag(retType);
        if (retTag != TypeTag.V)
            state.push(retType);
    }

    @Override
    protected C ldc(ToIntFunction<PoolHelper<S, T, E>> indexFunc, boolean fat) {
        LdcPoolHelper ldcPoolHelper = new LdcPoolHelper();
        int index = indexFunc.applyAsInt(ldcPoolHelper);
        fat = typeHelper.tag(ldcPoolHelper.type).width() == 2;
        return super.ldc(index, ldcPoolHelper.type, fat);
    }
    //where
        class LdcPoolHelper implements PoolHelper<S, T, E> {

            T type;

            @Override
            public int putClass(S symbol) {
                type = typeHelper.type(symbol);
                return poolHelper.putClass(symbol);
            }

            @Override
            public int putInt(int i) {
                type = typeHelper.fromTag(TypeTag.I);
                return poolHelper.putInt(i);
            }

            @Override
            public int putFloat(float f) {
                type = typeHelper.fromTag(TypeTag.F);
                return poolHelper.putFloat(f);
            }

            @Override
            public int putLong(long l) {
                type = typeHelper.fromTag(TypeTag.J);
                return poolHelper.putLong(l);
            }

            @Override
            public int putDouble(double d) {
                type = typeHelper.fromTag(TypeTag.D);
                return poolHelper.putDouble(d);
            }

            @Override
            public int putString(String s) {
                type = typeHelper.type(typeHelper.symbolFrom("java/lang/String"));
                return poolHelper.putString(s);
            }

            @Override
            public int putDynamicConstant(CharSequence constName, T constType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, E>> staticArgs) {
                type = constType;
                return poolHelper.putDynamicConstant(constName, constType, bsmClass, bsmName, bsmType, staticArgs);
            }

            @Override
            public int putFieldRef(S owner, CharSequence name, T type) {
                throw new IllegalStateException();
            }

            @Override
            public int putMethodRef(S owner, CharSequence name, T type, boolean isInterface) {
                throw new IllegalStateException();
            }

            @Override
            public int putUtf8(CharSequence s) {
                throw new IllegalStateException();
            }

            @Override
            public int putType(T t) {
                throw new IllegalStateException();
            }

            @Override
            public int putMethodType(T t) {
                type = typeHelper.type(typeHelper.symbolFrom("java/lang/invoke/MethodType"));
                return poolHelper.putMethodType(t);
            }

            @Override
            public int putHandle(int refKind, S owner, CharSequence name, T t) {
                type = typeHelper.type(typeHelper.symbolFrom("java/lang/invoke/MethodHandle"));
                return poolHelper.putHandle(refKind, owner, name, t);
            }

            @Override
            public int putHandle(int refKind, S owner, CharSequence name, T t, boolean isInterface) {
                type = typeHelper.type(typeHelper.symbolFrom("java/lang/invoke/MethodHandle"));
                return poolHelper.putHandle(refKind, owner, name, t, isInterface);
            }

            @Override
            public int putInvokeDynamic(CharSequence invokedName, T invokedType, S bsmClass, CharSequence bsmName, T bsmType, Consumer<StaticArgListBuilder<S, T, E>> staticArgs) {
                throw new IllegalStateException();
            }

            @Override
            public int size() {
                throw new IllegalStateException();
            }

            @Override
            public E entries() {
                throw new IllegalStateException();
            }
    }

    public C load(int index) {
        return load(typeHelper.tag(state.locals.get(index)), index);
    }

    public C store(int index) {
        return store(typeHelper.tag(state.tosType()), index);
    }

    @Override
    public C withLocalSize(int localsize) {
        throw new IllegalStateException("Local size automatically computed");
    }

    @Override
    public C withStackSize(int stacksize) {
        throw new IllegalStateException("Stack size automatically computed");
    }

    public C withLocal(CharSequence name, T type) {
        int offset = currLocalOffset;
        TypeTag tag = typeHelper.tag(type);
        lvarOffsets.put(name, new LocalVarInfo(name, offset, depth, tag));
        state.load(type, offset);
        currLocalOffset += tag.width;
        return thisBuilder();
    }

    public C load(CharSequence local) {
        return load(lvarOffsets.get(local).offset);
    }

    public C store(CharSequence local) {
        return store(lvarOffsets.get(local).offset);
    }

    @Override
    public C withTry(Consumer<? super C> tryBlock, Consumer<? super CatchBuilder> catchBlocks) {
        return super.withTry(c -> {
            withLocalScope(() -> {
                tryBlock.accept(c);
            });
        }, catchBlocks);
    }

    @Override
    protected CatchBuilder makeCatchBuilder(int start, int end) {
        return new TypedCatchBuilder(start, end);
    }

    class TypedCatchBuilder extends CatchBuilder {

        State initialState = state.dup();

        TypedCatchBuilder(int start, int end) {
            super(start, end);
        }

        @Override
        protected void emitCatch(S exc, Consumer<? super C> catcher) {
            withLocalScope(() -> {
                state.push(typeHelper.type(exc));
                emitStackMap(code.offset);
                super.emitCatch(exc, catcher);
                state = initialState;
            });
        }

        @Override
        protected void emitFinalizer() {
            withLocalScope(() -> {
                state.push(typeHelper.type(typeHelper.symbolFrom("java/lang/Throwable")));
                emitStackMap(code.offset);
                super.emitFinalizer();
            });
        }
    }

    protected void withLocalScope(Runnable runnable) {
        int prevDepth = depth;
        try {
            depth++;
            runnable.run();
        } finally {
            Iterator<Entry<CharSequence, LocalVarInfo>> lvarIt = lvarOffsets.entrySet().iterator();
            while (lvarIt.hasNext()) {
                LocalVarInfo lvi = lvarIt.next().getValue();
                if (lvi.depth == depth) {
                    int width = lvi.type.width;
                    currLocalOffset -= width;
                    lvarIt.remove();
                }
            }
            depth = prevDepth;
        }
    }

    @Override
    void addPendingJump(CharSequence label, int pc) {
        pendingJumps.add(new StatefulPendingJump(label, pc, state.dup()));
    }

    @Override
    void resolveJumps(CharSequence label, int pc) {
        super.resolveJumps(label, pc);
        emitStackMap(pc);
    }

    //TODO: optimize stackmap generation by avoiding intermediate classes
    protected void emitStackMap(int pc) {
        //stack map generation
        if (pc > lastStackMapPc) {
            writeStackMapFrame(pc);
            lastStackMapState = state.dup();
            lastStackMapPc = pc;
            nstackmaps++;
        }
    }

    @Override
    void build(GrowableByteBuffer buf) {
        if (stacksize == -1) {
            throw new IllegalStateException("Bad stack size");
        }
        if (localsize == -1) {
            throw new IllegalStateException("Bad locals size");
        }
        if (nstackmaps > 0) {
            GrowableByteBuffer stackmapsAttr = new GrowableByteBuffer();
            stackmapsAttr.writeChar(nstackmaps);
            stackmapsAttr.writeBytes(stackmaps);
            withAttribute("StackMapTable", stackmapsAttr.bytes());
        }
        super.build(buf);
    }

    /**
     * Compare this frame with the previous frame and produce
     * an entry of compressed stack map frame.
     */
    void writeStackMapFrame(int pc) {
        List<T> locals = state.locals;
        List<T> stack = state.stack;
        List<T> prev_locals = lastStackMapState.locals;
        int offset_delta = lastStackMapPc == -1 ? pc : pc - lastStackMapPc - 1;
        if (stack.size() == 1) {
            if (locals.size() == prev_locals.size() && prev_locals.equals(locals)) {
                sameLocals1StackItemFrame(offset_delta, stack.get(stack.size() - 1));
                return;
            }
        } else if (stack.size() == 0) {
            int diff_length = prev_locals.size() - locals.size();
            if (diff_length == 0) {
                sameFrame(offset_delta);
                return;
            } else if (-MAX_LOCAL_LENGTH_DIFF < diff_length && diff_length < 0) {
                appendFrame(offset_delta, prev_locals.size(), locals);
                return;
            } else if (0 < diff_length && diff_length < MAX_LOCAL_LENGTH_DIFF) {
                chopFrame(offset_delta, diff_length);
                return;
            }
        }
        fullFrame(offset_delta, locals, stack);
    }
}
