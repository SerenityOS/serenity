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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class MacroCodeBuilder<S, T, E, C extends MacroCodeBuilder<S, T, E, C>> extends CodeBuilder<S, T, E, C> {

    JumpMode jumpMode = JumpMode.NARROW;

    Map<CharSequence, Integer> labels = new HashMap<>();
    List<PendingJump> pendingJumps = new LinkedList<>();

    class PendingJump {
        CharSequence label;
        int pc;

        PendingJump(CharSequence label, int pc) {
            this.label = label;
            this.pc = pc;
        }

        boolean resolve(CharSequence label, int offset) {
            if (this.label.equals(label)) {
                //patch offset
                code.withOffset(pc + 1, buf -> emitOffset(buf, jumpMode, offset - pc));
                return true;
            } else {
                return false;
            }
        }
    }

    public enum InvocationKind {
        INVOKESTATIC,
        INVOKEVIRTUAL,
        INVOKESPECIAL,
        INVOKEINTERFACE;
    }

    public enum FieldAccessKind {
        STATIC,
        INSTANCE;
    }

    public enum CondKind {
        EQ(0),
        NE(1),
        LT(2),
        GE(3),
        GT(4),
        LE(5);

        int offset;

        CondKind(int offset) {
            this.offset = offset;
        }

        public CondKind negate() {
            switch (this) {
                case EQ:
                    return NE;
                case NE:
                    return EQ;
                case LT:
                    return GE;
                case GE:
                    return LT;
                case GT:
                    return LE;
                case LE:
                    return GT;
                default:
                    throw new IllegalStateException("Unknown cond");
            }
        }
    }

    static class WideJumpException extends RuntimeException {
        static final long serialVersionUID = 42L;
    }

    public MacroCodeBuilder(MethodBuilder<S, T, E> methodBuilder) {
        super(methodBuilder);
    }

    public C load(TypeTag type, int n) {
        if (type == TypeTag.Q) {
            return vload(n);
        } else {
            switch (n) {
                case 0:
                    return emitOp(Opcode.ILOAD_0.at(type, 4));
                case 1:
                    return emitOp(Opcode.ILOAD_1.at(type, 4));
                case 2:
                    return emitOp(Opcode.ILOAD_2.at(type, 4));
                case 3:
                    return emitOp(Opcode.ILOAD_3.at(type, 4));
                default:
                    return emitWideIfNeeded(Opcode.ILOAD.at(type), n);
            }
        }
    }

    public C store(TypeTag type, int n) {
        if (type == TypeTag.Q) {
            return vstore(n);
        } else {
            switch (n) {
                case 0:
                    return emitOp(Opcode.ISTORE_0.at(type, 4));
                case 1:
                    return emitOp(Opcode.ISTORE_1.at(type, 4));
                case 2:
                    return emitOp(Opcode.ISTORE_2.at(type, 4));
                case 3:
                    return emitOp(Opcode.ISTORE_3.at(type, 4));
                default:
                    return emitWideIfNeeded(Opcode.ISTORE.at(type), n);
            }
        }
    }

    public C arrayload(TypeTag type) {
        return emitOp(Opcode.IALOAD.at(type));
    }

    public C arraystore(TypeTag type, int n) {
        return emitOp(Opcode.IASTORE.at(type));
    }

    public C const_(int i) {
        switch (i) {
            case -1:
                return iconst_m1();
            case 0:
                return iconst_0();
            case 1:
                return iconst_1();
            case 2:
                return iconst_2();
            case 3:
                return iconst_3();
            case 4:
                return iconst_4();
            case 5:
                return iconst_5();
            default:
                if (i > 0 && i <= Byte.MAX_VALUE) {
                    return bipush(i);
                } else if (i >= Short.MIN_VALUE && i <= Short.MAX_VALUE) {
                    return sipush(i);
                } else {
                    return ldc(i);
                }
        }
    }

    public C const_(long l) {
        if (l == 0) {
            return lconst_0();
        } else if (l == 1) {
            return lconst_1();
        } else {
            return ldc(l);
        }
    }

    public C const_(float f) {
        if (f == 0) {
            return fconst_0();
        } else if (f == 1) {
            return fconst_1();
        } else if (f == 2) {
            return fconst_2();
        } else {
            return ldc(f);
        }
    }

    public C const_(double d) {
        if (d == 0) {
            return dconst_0();
        } else if (d == 1) {
            return dconst_1();
        } else {
            return ldc(d);
        }
    }

    public C getfield(FieldAccessKind fak, S owner, CharSequence name, T type) {
        switch (fak) {
            case INSTANCE:
                return getfield(owner, name, type);
            case STATIC:
                return getstatic(owner, name, type);
            default:
                throw new IllegalStateException();
        }
    }

    public C putfield(FieldAccessKind fak, S owner, CharSequence name, T type) {
        switch (fak) {
            case INSTANCE:
                return putfield(owner, name, type);
            case STATIC:
                return putstatic(owner, name, type);
            default:
                throw new IllegalStateException();
        }
    }

    public C invoke(InvocationKind ik, S owner, CharSequence name, T type, boolean isInterface) {
        switch (ik) {
            case INVOKESTATIC:
                return invokestatic(owner, name, type, isInterface);
            case INVOKEVIRTUAL:
                return invokevirtual(owner, name, type, isInterface);
            case INVOKESPECIAL:
                return invokespecial(owner, name, type, isInterface);
            case INVOKEINTERFACE:
                if (!isInterface) throw new AssertionError();
                return invokeinterface(owner, name, type);
            default:
                throw new IllegalStateException();
        }
    }

    public C add(TypeTag type) {
        return emitOp(Opcode.IADD.at(type));
    }

    public C sub(TypeTag type) {
        return emitOp(Opcode.ISUB.at(type));
    }

    public C mul(TypeTag type) {
        return emitOp(Opcode.IMUL.at(type));
    }

    public C div(TypeTag type) {
        return emitOp(Opcode.IDIV.at(type));
    }

    public C rem(TypeTag type) {
        return emitOp(Opcode.IREM.at(type));
    }

    public C neg(TypeTag type) {
        return emitOp(Opcode.INEG.at(type));
    }

    public C shl(TypeTag type) {
        return emitOp(Opcode.ISHL.at(type));
    }

    public C shr(TypeTag type) {
        return emitOp(Opcode.ISHR.at(type));
    }

    public C ushr(TypeTag type) {
        return emitOp(Opcode.ISHR.at(type));
    }

    public C and(TypeTag type) {
        return emitOp(Opcode.IAND.at(type));
    }

    public C or(TypeTag type) {
        return emitOp(Opcode.IOR.at(type));
    }

    public C xor(TypeTag type) {
        return emitOp(Opcode.IXOR.at(type));
    }

    public C return_(TypeTag type) {
        switch (type) {
            case V:
                return return_();
            case Q:
                return vreturn();
            default:
                return emitOp(Opcode.IRETURN.at(type));
        }
    }

    @Override
    public LabelledTypedBuilder typed(TypeTag typeTag) {
        return super.typed(typeTag, _unused -> new LabelledTypedBuilder());
    }

    public class LabelledTypedBuilder extends TypedBuilder {
        public C if_acmpeq(CharSequence target) {
            return ifcmp(TypeTag.A, CondKind.EQ, target);
        }

        public C if_acmpne(CharSequence target) {
            return ifcmp(TypeTag.A, CondKind.NE, target);
        }
    }

    public C conv(TypeTag from, TypeTag to) {
        switch (from) {
            case B:
            case C:
            case S:
                switch (to) {
                    case J:
                        return i2l();
                    case F:
                        return i2f();
                    case D:
                        return i2d();
                }
                break;
            case I:
                switch (to) {
                    case J:
                        return i2l();
                    case F:
                        return i2f();
                    case D:
                        return i2d();
                    case B:
                        return i2b();
                    case C:
                        return i2c();
                    case S:
                        return i2s();
                }
                break;
            case J:
                switch (to) {
                    case I:
                        return l2i();
                    case F:
                        return l2f();
                    case D:
                        return l2d();
                }
                break;
            case F:
                switch (to) {
                    case I:
                        return f2i();
                    case J:
                        return f2l();
                    case D:
                        return f2d();
                }
                break;
            case D:
                switch (to) {
                    case I:
                        return d2i();
                    case J:
                        return d2l();
                    case F:
                        return d2f();
                }
                break;
        }
        //no conversion is necessary - do nothing!
        return thisBuilder();
    }

    public C if_null(CharSequence label) {
        return emitCondJump(Opcode.IF_NULL, Opcode.IF_NONNULL, label);
    }

    public C if_nonnull(CharSequence label) {
        return emitCondJump(Opcode.IF_NONNULL, Opcode.IF_NULL, label);
    }

    public C ifcmp(TypeTag type, CondKind cond, CharSequence label) {
        switch (type) {
            case I:
                return emitCondJump(Opcode.IF_ICMPEQ, cond, label);
            case A:
                return emitCondJump(Opcode.IF_ACMPEQ, cond, label);
            case J:
                return lcmp().emitCondJump(Opcode.IFEQ, cond, label);
            case D:
                return dcmpg().emitCondJump(Opcode.IFEQ, cond, label);
            case F:
                return fcmpg().emitCondJump(Opcode.IFEQ, cond, label);
            default:
                throw new IllegalArgumentException("Bad cmp type");
        }
    }

    public C goto_(CharSequence label) {
        emitOp(jumpMode == JumpMode.NARROW ? Opcode.GOTO_ : Opcode.GOTO_W);
        emitOffset(code, jumpMode, labelOffset(label));
        return thisBuilder();
    }

    protected int labelOffset(CharSequence label) {
        int pc = code.offset - 1;
        Integer labelPc = labels.get(label);
        if (labelPc == null) {
            addPendingJump(label, pc);
        }
        return labelPc == null ? 0 : (labelPc - pc);
    }

    public C label(CharSequence s) {
        int pc = code.offset;
        Object old = labels.put(s, pc);
        if (old != null) {
            throw new IllegalStateException("label already exists");
        }
        resolveJumps(s, pc);
        return thisBuilder();
    }

    //FIXME: address this jumpy mess - i.e. offset and state update work against each other!
    public C emitCondJump(Opcode opcode, CondKind ck, CharSequence label) {
        return emitCondJump(opcode.at(ck), opcode.at(ck.negate()), label);
    }

    public C emitCondJump(Opcode pos, Opcode neg, CharSequence label) {
        if (jumpMode == JumpMode.NARROW) {
            emitOp(pos);
            emitOffset(code, jumpMode, labelOffset(label));
        } else {
            emitOp(neg);
            emitOffset(code, JumpMode.NARROW, 8);
            goto_w(labelOffset(label));
        }
        return thisBuilder();
    }

    void addPendingJump(CharSequence label, int pc) {
        pendingJumps.add(new PendingJump(label, pc));
    }

    void resolveJumps(CharSequence label, int pc) {
        Iterator<PendingJump> jumpsIt = pendingJumps.iterator();
        while (jumpsIt.hasNext()) {
            PendingJump jump = jumpsIt.next();
            if (jump.resolve(label, pc)) {
                jumpsIt.remove();
            }
        }
    }

    @Override
    protected void emitOffset(GrowableByteBuffer buf, JumpMode jumpMode, int offset) {
        if (jumpMode == JumpMode.NARROW && (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE)) {
            throw new WideJumpException();
        }
        super.emitOffset(buf, jumpMode, offset);
    }

    public C jsr(CharSequence label) {
        emitOp(jumpMode == JumpMode.NARROW ? Opcode.JSR : Opcode.JSR_W);
        emitOffset(code, jumpMode, labelOffset(label));
        return thisBuilder();
    }

    @SuppressWarnings("unchecked")
    public C withTry(Consumer<? super C> tryBlock, Consumer<? super CatchBuilder> catchBlocks) {
        int start = code.offset;
        tryBlock.accept((C) this);
        int end = code.offset;
        CatchBuilder catchBuilder = makeCatchBuilder(start, end);
        catchBlocks.accept(catchBuilder);
        catchBuilder.build();
        return thisBuilder();
    }

    void clear() {
        code.offset = 0;
        catchers.offset = 0;
        ncatchers = 0;
        labels.clear();
        pendingJumps = null;
    }

    protected CatchBuilder makeCatchBuilder(int start, int end) {
        return new CatchBuilder(start, end);
    }

    public class CatchBuilder {
        int start, end;

        String endLabel = labelName();

        Map<S, Consumer<? super C>> catchers = new LinkedHashMap<>();
        public Consumer<? super C> finalizer;
        List<Integer> pendingGaps = new ArrayList<>();

        public CatchBuilder(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public CatchBuilder withCatch(S exc, Consumer<? super C> catcher) {
            catchers.put(exc, catcher);
            return this;
        }

        public CatchBuilder withFinally(Consumer<? super C> finalizer) {
            this.finalizer = finalizer;
            return this;
        }

        @SuppressWarnings("unchecked")
        void build() {
            if (finalizer != null) {
                finalizer.accept((C) MacroCodeBuilder.this);
            }
            goto_(endLabel);
            for (Map.Entry<S, Consumer<? super C>> catcher_entry : catchers.entrySet()) {
                emitCatch(catcher_entry.getKey(), catcher_entry.getValue());
            }
            if (finalizer != null) {
                emitFinalizer();
            }
            resolveJumps(endLabel, code.offset);
        }

        @SuppressWarnings("unchecked")
        protected void emitCatch(S exc, Consumer<? super C> catcher) {
            int offset = code.offset;
            MacroCodeBuilder.this.withCatch(exc, start, end, offset);
            catcher.accept((C) MacroCodeBuilder.this);
            if (finalizer != null) {
                int startFinalizer = code.offset;
                finalizer.accept((C) MacroCodeBuilder.this);
                pendingGaps.add(startFinalizer);
                pendingGaps.add(code.offset);
            }
            goto_(endLabel);
        }

        @SuppressWarnings("unchecked")
        protected void emitFinalizer() {
            int offset = code.offset;
            pop();
            for (int i = 0; i < pendingGaps.size(); i += 2) {
                MacroCodeBuilder.this.withCatch(null, pendingGaps.get(i), pendingGaps.get(i + 1), offset);
            }
            MacroCodeBuilder.this.withCatch(null, start, end, offset);
            finalizer.accept((C) MacroCodeBuilder.this);
        }

//        @SuppressWarnings("unchecked")
//        CatchBuilder withCatch(S exc, Consumer<? super C> catcher) {
//            int offset = code.offset;
//            MacroCodeBuilder.this.withCatch(exc, start, end, offset);
//            catcher.accept((C)MacroCodeBuilder.this);
//            return this;
//        }
//
//        @SuppressWarnings("unchecked")
//        CatchBuilder withFinally(Consumer<? super C> catcher) {
//            int offset = code.offset;
//            MacroCodeBuilder.this.withCatch(null, start, end, offset);
//            catcher.accept((C)MacroCodeBuilder.this);
//            return this;
//        }
    }

    @SuppressWarnings("unchecked")
    public C switch_(Consumer<? super SwitchBuilder> consumer) {
        int start = code.offset;
        SwitchBuilder sb = makeSwitchBuilder();
        consumer.accept(sb);
        int nlabels = sb.cases.size();
        switch (sb.switchCode()) {
            case LOOKUPSWITCH: {
                int[] lookupOffsets = new int[nlabels * 2];
                int i = 0;
                for (Integer v : sb.cases.keySet()) {
                    lookupOffsets[i] = v;
                    i += 2;
                }
                lookupswitch(0, lookupOffsets);
                //backpatch lookup
                int curr = code.offset - (8 * nlabels) - 8;
                int defaultOffset = code.offset - start;
                code.withOffset(curr, buf -> emitOffset(buf, JumpMode.WIDE, defaultOffset));
                sb.defaultCase.accept((C) this);
                curr += 12;
                for (Consumer<? super C> case_ : sb.cases.values()) {
                    int offset = code.offset;
                    code.withOffset(curr, buf -> emitOffset(buf, JumpMode.WIDE, offset - start));
                    case_.accept((C) this);
                    curr += 8;
                }
                break;
            }
            case TABLESWITCH: {
                int[] tableOffsets = new int[sb.hi - sb.lo + 1];
                tableswitch(sb.lo, sb.hi, 0, tableOffsets);
                //backpatch table
                int curr = code.offset - (4 * tableOffsets.length) - 12;
                int defaultOffset = code.offset - start;
                code.withOffset(curr, buf -> emitOffset(buf, JumpMode.WIDE, defaultOffset));
                sb.defaultCase.accept((C) this);
                curr += 12;
                int lastCasePc = -1;
                for (int i = sb.lo; i <= sb.hi; i++) {
                    Consumer<? super C> case_ = sb.cases.get(i);
                    if (case_ != null) {
                        lastCasePc = code.offset;
                        case_.accept((C) this);
                    }
                    int offset = lastCasePc - start;
                    code.withOffset(curr, buf -> emitOffset(buf, JumpMode.WIDE, offset));
                    curr += 4;
                }
            }
        }
        resolveJumps(sb.endLabel, code.offset);
        return thisBuilder();
    }

    private static int labelCount = 0;

    String labelName() {
        return "label" + labelCount++;
    }

    protected SwitchBuilder makeSwitchBuilder() {
        return new SwitchBuilder();
    }

    public class SwitchBuilder {
        Map<Integer, Consumer<? super C>> cases = new TreeMap<>();
        int lo = Integer.MAX_VALUE;
        int hi = Integer.MIN_VALUE;
        String endLabel = labelName();

        public Consumer<? super C> defaultCase;

        @SuppressWarnings("unchecked")
        public SwitchBuilder withCase(int value, Consumer<? super C> case_, boolean fallthrough) {
            if (value > hi) {
                hi = value;
            }
            if (value < lo) {
                lo = value;
            }
            if (!fallthrough) {
                Consumer<? super C> prevCase = case_;
                case_ = C -> {
                    prevCase.accept(C);
                    C.goto_(endLabel);
                };
            }
            cases.put(value, case_);
            return this;
        }

        @SuppressWarnings("unchecked")
        public SwitchBuilder withDefault(Consumer<? super C> defaultCase) {
            if (this.defaultCase != null) {
                throw new IllegalStateException("default already set");
            }
            this.defaultCase = defaultCase;
            return this;
        }

        Opcode switchCode() {
            int nlabels = cases.size();
            // Determine whether to issue a tableswitch or a lookupswitch
            // instruction.
            long table_space_cost = 4 + ((long) hi - lo + 1); // words
            long lookup_space_cost = 3 + 2 * (long) nlabels;
            return
                    nlabels > 0 &&
                            table_space_cost <= lookup_space_cost
                            ?
                            Opcode.TABLESWITCH : Opcode.LOOKUPSWITCH;
        }
    }
}
