/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.resources.CompilerProperties.Errors;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.sun.tools.javac.jvm.ByteCodes.*;
import static com.sun.tools.javac.comp.Operators.OperatorType.*;

/**
 * This class contains the logic for unary and binary operator resolution/lookup.
 *
 * <p><b>This is NOT part of any supported API.
 * If you write code that depends on this, you do so at your own risk.
 * This code and its internal interfaces are subject to change or
 * deletion without notice.</b>
 */
public class Operators {
    protected static final Context.Key<Operators> operatorsKey = new Context.Key<>();

    private final Names names;
    private final Log log;
    private final Symtab syms;
    private final Types types;

    /** Unary operators map. */
    private Map<Name, List<UnaryOperatorHelper>> unaryOperators = new HashMap<>(Tag.getNumberOfOperators());

    /** Binary operators map. */
    private Map<Name, List<BinaryOperatorHelper>> binaryOperators = new HashMap<>(Tag.getNumberOfOperators());

    /** The names of all operators. */
    private Name[] opname = new Name[Tag.getNumberOfOperators()];

    public static Operators instance(Context context) {
        Operators instance = context.get(operatorsKey);
        if (instance == null)
            instance = new Operators(context);
        return instance;
    }

    protected Operators(Context context) {
        context.put(operatorsKey, this);
        syms = Symtab.instance(context);
        names = Names.instance(context);
        log = Log.instance(context);
        types = Types.instance(context);
        noOpSymbol = new OperatorSymbol(names.empty, Type.noType, -1, syms.noSymbol);
        initOperatorNames();
        initUnaryOperators();
        initBinaryOperators();
    }

    /**
     * Perform unary promotion of a type; this routine implements JLS 5.6.1.
     * If the input type is not supported by unary promotion, it is returned unaltered.
     */
    Type unaryPromotion(Type t) {
        Type unboxed = types.unboxedTypeOrType(t);
        switch (unboxed.getTag()) {
            case BYTE:
            case SHORT:
            case CHAR:
                return syms.intType;
            default:
                return unboxed;
        }
    }

    /**
     * Perform binary promotion of a pair of types; this routine implements JLS 5.6.2.
     * If the input types are not supported by unary promotion, if such types are identical to
     * a type C, then C is returned, otherwise Object is returned.
     */
    Type binaryPromotion(Type t1, Type t2) {
        Type unboxedT1 = types.unboxedTypeOrType(t1);
        Type unboxedT2 = types.unboxedTypeOrType(t2);

        if (unboxedT1.isNumeric() && unboxedT2.isNumeric()) {
            if (unboxedT1.hasTag(TypeTag.DOUBLE) || unboxedT2.hasTag(TypeTag.DOUBLE)) {
                return syms.doubleType;
            } else if (unboxedT1.hasTag(TypeTag.FLOAT) || unboxedT2.hasTag(TypeTag.FLOAT)) {
                return syms.floatType;
            } else if (unboxedT1.hasTag(TypeTag.LONG) || unboxedT2.hasTag(TypeTag.LONG)) {
                return syms.longType;
            } else {
                return syms.intType;
            }
        } else if (types.isSameType(unboxedT1, unboxedT2)) {
            return unboxedT1;
        } else {
            return syms.objectType;
        }
    }

    /**
     * Entry point for resolving a unary operator given an operator tag and an argument type.
     */
    OperatorSymbol resolveUnary(DiagnosticPosition pos, JCTree.Tag tag, Type op) {
        return resolve(tag,
                unaryOperators,
                unop -> unop.test(op),
                unop -> unop.resolve(op),
                () -> reportErrorIfNeeded(pos, tag, op));
    }

    /**
     * Entry point for resolving a binary operator given an operator tag and a pair of argument types.
     */
    OperatorSymbol resolveBinary(DiagnosticPosition pos, JCTree.Tag tag, Type op1, Type op2) {
        return resolve(tag,
                binaryOperators,
                binop -> binop.test(op1, op2),
                binop -> binop.resolve(op1, op2),
                () -> reportErrorIfNeeded(pos, tag, op1, op2));
    }

    /**
     * Main operator lookup routine; lookup an operator (either unary or binary) in its corresponding
     * map. If there's a matching operator, its resolve routine is called and the result is returned;
     * otherwise the result of a fallback function is returned.
     */
    private <O> OperatorSymbol resolve(Tag tag, Map<Name, List<O>> opMap, Predicate<O> opTestFunc,
                       Function<O, OperatorSymbol> resolveFunc, Supplier<OperatorSymbol> noResultFunc) {
        return opMap.get(operatorName(tag)).stream()
                .filter(opTestFunc)
                .map(resolveFunc)
                .findFirst()
                .orElseGet(noResultFunc);
    }

    /**
     * Creates an operator symbol.
     */
    private OperatorSymbol makeOperator(Name name, List<OperatorType> formals, OperatorType res, int... opcodes) {
        MethodType opType = new MethodType(
                formals.stream()
                        .map(o -> o.asType(syms))
                        .collect(List.collector()),
                res.asType(syms), List.nil(), syms.methodClass);
        return new OperatorSymbol(name, opType, mergeOpcodes(opcodes), syms.noSymbol);
    }

    /**
     * Fold two opcodes in a single int value (if required).
     */
    private int mergeOpcodes(int... opcodes) {
        int opcodesLen = opcodes.length;
        Assert.check(opcodesLen == 1 || opcodesLen == 2);
        return (opcodesLen == 1) ?
                opcodes[0] :
                ((opcodes[0] << ByteCodes.preShift) | opcodes[1]);
    }

    /** A symbol that stands for a missing operator.
     */
    public final OperatorSymbol noOpSymbol;

    /**
     * Report an operator lookup error.
     */
    private OperatorSymbol reportErrorIfNeeded(DiagnosticPosition pos, Tag tag, Type... args) {
        if (Stream.of(args).noneMatch(t -> t.isErroneous() || t.hasTag(TypeTag.NONE))) {
            Name opName = operatorName(tag);
            JCDiagnostic.Error opError = (args.length) == 1 ?
                    Errors.OperatorCantBeApplied(opName, args[0]) :
                    Errors.OperatorCantBeApplied1(opName, args[0], args[1]);
            log.error(pos, opError);
        }
        return noOpSymbol;
    }

    /**
     * Return name of operator with given tree tag.
     */
    public Name operatorName(JCTree.Tag tag) {
        return opname[tag.operatorIndex()];
    }

    /**
     * The constants in this enum represent the types upon which all the operator helpers
     * operate upon. This allows lazy and concise mapping between a type name and a type instance.
     */
    enum OperatorType {
        BYTE(syms -> syms.byteType),
        SHORT(syms -> syms.shortType),
        INT(syms -> syms.intType),
        LONG(syms -> syms.longType),
        FLOAT(syms -> syms.floatType),
        DOUBLE(syms -> syms.doubleType),
        CHAR(syms -> syms.charType),
        BOOLEAN(syms -> syms.booleanType),
        OBJECT(syms -> syms.objectType),
        STRING(syms -> syms.stringType),
        BOT(syms -> syms.botType);

        final Function<Symtab, Type> asTypeFunc;

        OperatorType(Function<Symtab, Type> asTypeFunc) {
            this.asTypeFunc = asTypeFunc;
        }

        Type asType(Symtab syms) {
            return asTypeFunc.apply(syms);
        }
    }

    /**
     * Common root for all operator helpers. An operator helper instance is associated with a
     * given operator (i.e. '+'); it contains routines to perform operator lookup, i.e. find
     * which version of the '+' operator is the best given an argument type list. Supported
     * operator symbols are initialized lazily upon first lookup request - this is in order to avoid
     * initialization circularities between this class and {@code Symtab}.
     */
    abstract class OperatorHelper {

        /** The operator name. */
        final Name name;

        /** The list of symbols associated with this operator (lazily populated). */
        Optional<OperatorSymbol[]> alternatives = Optional.empty();

        /** An array of operator symbol suppliers (used to lazily populate the symbol list). */
        List<Supplier<OperatorSymbol>> operatorSuppliers = List.nil();

        @SuppressWarnings("varargs")
        OperatorHelper(Tag tag) {
            this.name = operatorName(tag);
        }

        /**
         * This routine implements the main operator lookup process. Each operator is tested
         * using an applicability predicate; if the test succeeds that same operator is returned,
         * otherwise a dummy symbol is returned.
         */
        final OperatorSymbol doLookup(Predicate<OperatorSymbol> applicabilityTest) {
            return Stream.of(alternatives.orElseGet(this::initOperators))
                    .filter(applicabilityTest)
                    .findFirst()
                    .orElse(noOpSymbol);
        }

        /**
         * This routine performs lazy instantiation of the operator symbols supported by this helper.
         * After initialization is done, the suppliers are cleared, to free up memory.
         */
        private OperatorSymbol[] initOperators() {
            OperatorSymbol[] operators = operatorSuppliers.stream()
                    .map(Supplier::get)
                    .toArray(OperatorSymbol[]::new);
            alternatives = Optional.of(operators);
            operatorSuppliers = null; //let GC do its work
            return operators;
        }
    }

    /**
     * Common superclass for all unary operator helpers.
     */
    abstract class UnaryOperatorHelper extends OperatorHelper implements Predicate<Type> {

        UnaryOperatorHelper(Tag tag) {
            super(tag);
        }

        /**
         * This routine implements the unary operator lookup process. It customizes the behavior
         * of the shared lookup routine in {@link OperatorHelper}, by using an unary applicability test
         * (see {@link UnaryOperatorHelper#isUnaryOperatorApplicable(OperatorOperatorSymbol, Type)}
         */
        final OperatorSymbol doLookup(Type t) {
            return doLookup(op -> isUnaryOperatorApplicable(op, t));
        }

        /**
         * Unary operator applicability test - is the input type the same as the expected operand type?
         */
        boolean isUnaryOperatorApplicable(OperatorSymbol op, Type t) {
            return types.isSameType(op.type.getParameterTypes().head, t);
        }

        /**
         * Adds a unary operator symbol.
         */
        final UnaryOperatorHelper addUnaryOperator(OperatorType arg, OperatorType res, int... opcode) {
            operatorSuppliers = operatorSuppliers.prepend(() -> makeOperator(name, List.of(arg), res, opcode));
            return this;
        }

        /**
         * This method will be overridden by unary operator helpers to provide custom resolution
         * logic.
         */
        abstract OperatorSymbol resolve(Type t);
    }

    abstract class BinaryOperatorHelper extends OperatorHelper implements BiPredicate<Type, Type> {

        BinaryOperatorHelper(Tag tag) {
            super(tag);
        }

        /**
         * This routine implements the binary operator lookup process. It customizes the behavior
         * of the shared lookup routine in {@link OperatorHelper}, by using an unary applicability test
         * (see {@link BinaryOperatorHelper#isBinaryOperatorApplicable(OperatorSymbol, Type, Type)}
         */
        final OperatorSymbol doLookup(Type t1, Type t2) {
            return doLookup(op -> isBinaryOperatorApplicable(op, t1, t2));
        }

        /**
         * Binary operator applicability test - are the input types the same as the expected operand types?
         */
        boolean isBinaryOperatorApplicable(OperatorSymbol op, Type t1, Type t2) {
            List<Type> formals = op.type.getParameterTypes();
            return types.isSameType(formals.head, t1) &&
                    types.isSameType(formals.tail.head, t2);
        }

        /**
         * Adds a binary operator symbol.
         */
        final BinaryOperatorHelper addBinaryOperator(OperatorType arg1, OperatorType arg2, OperatorType res, int... opcode) {
            operatorSuppliers = operatorSuppliers.prepend(() -> makeOperator(name, List.of(arg1, arg2), res, opcode));
            return this;
        }

        /**
         * This method will be overridden by binary operator helpers to provide custom resolution
         * logic.
         */
        abstract OperatorSymbol resolve(Type t1, Type t2);
    }

    /**
     * Class representing unary operator helpers that operate on reference types.
     */
    class UnaryReferenceOperator extends UnaryOperatorHelper {

        UnaryReferenceOperator(Tag tag) {
            super(tag);
        }

        @Override
        public boolean test(Type type) {
            return type.isNullOrReference();
        }

        @Override
        public OperatorSymbol resolve(Type arg) {
            return doLookup(syms.objectType);
        }
    }

    /**
     * Class representing unary operator helpers that operate on numeric types (either boxed or unboxed).
     * Operator lookup is performed after applying numeric promotion of the input type.
     */
    class UnaryNumericOperator extends UnaryOperatorHelper {

        Predicate<Type> numericTest;

        UnaryNumericOperator(Tag tag) {
            this(tag, Type::isNumeric);
        }

        UnaryNumericOperator(Tag tag, Predicate<Type> numericTest) {
            super(tag);
            this.numericTest = numericTest;
        }

        @Override
        public boolean test(Type type) {
            return numericTest.test(unaryPromotion(type));
        }

        @Override
        public OperatorSymbol resolve(Type arg) {
            return doLookup(unaryPromotion(arg));
        }
    }

    /**
     * Class representing unary operator helpers that operate on boolean types  (either boxed or unboxed).
     * Operator lookup is performed assuming the input type is a boolean type.
     */
    class UnaryBooleanOperator extends UnaryOperatorHelper {

        UnaryBooleanOperator(Tag tag) {
            super(tag);
        }

        @Override
        public boolean test(Type type) {
            return types.unboxedTypeOrType(type).hasTag(TypeTag.BOOLEAN);
        }

        @Override
        public OperatorSymbol resolve(Type arg) {
            return doLookup(syms.booleanType);
        }
    }

    /**
     * Class representing prefix/postfix unary operator helpers. Operates on numeric types (either
     * boxed or unboxed). Operator lookup is performed on the unboxed version of the input type.
     */
    class UnaryPrefixPostfixOperator extends UnaryNumericOperator {

        UnaryPrefixPostfixOperator(Tag tag) {
            super(tag);
        }

        @Override
        public OperatorSymbol resolve(Type arg) {
            return doLookup(types.unboxedTypeOrType(arg));
        }
    }

    /**
     * Class representing binary operator helpers that operate on numeric types (either boxed or unboxed).
     * Operator lookup is performed after applying binary numeric promotion of the input types.
     */
    class BinaryNumericOperator extends BinaryOperatorHelper {

        Predicate<Type> numericTest;

        BinaryNumericOperator(Tag tag) {
            this(tag, Type::isNumeric);
        }

        BinaryNumericOperator(Tag tag, Predicate<Type> numericTest) {
            super(tag);
            this.numericTest = numericTest;
        }

        @Override
        public OperatorSymbol resolve(Type arg1, Type arg2) {
            Type t = binaryPromotion(arg1, arg2);
            return doLookup(t, t);
        }

        @Override
        public boolean test(Type arg1, Type arg2) {
            return numericTest.test(unaryPromotion(arg1)) &&
                    numericTest.test(unaryPromotion(arg2));
        }
    }

    /**
     * Class representing bitwise operator helpers that operate on boolean types (either boxed or unboxed).
     * Operator lookup is performed assuming both input types are boolean types.
     */
    class BinaryBooleanOperator extends BinaryOperatorHelper {

        BinaryBooleanOperator(Tag tag) {
            super(tag);
        }

        @Override
        public OperatorSymbol resolve(Type arg1, Type arg2) {
            return doLookup(syms.booleanType, syms.booleanType);
        }

        @Override
        public boolean test(Type arg1, Type arg2) {
            return types.unboxedTypeOrType(arg1).hasTag(TypeTag.BOOLEAN) &&
                    types.unboxedTypeOrType(arg2).hasTag(TypeTag.BOOLEAN);
        }
    }

    /**
     * Class representing string concatenation operator helper that operates on at least an
     * string operand. Input types subject to an operator lookup undergoes a special string promotion
     * (see {@link BinaryStringOperator#stringPromotion(Type)}.
     */
    class BinaryStringOperator extends BinaryOperatorHelper {

        BinaryStringOperator(Tag tag) {
            super(tag);
        }

        @Override
        public OperatorSymbol resolve(Type arg1, Type arg2) {
            return doLookup(stringPromotion(arg1), stringPromotion(arg2));
        }

        @Override
        public boolean test(Type arg1, Type arg2) {
            boolean hasStringOp = types.isSameType(arg1, syms.stringType) ||
                    types.isSameType(arg2, syms.stringType);
            boolean hasVoidOp = arg1.hasTag(TypeTag.VOID) || arg2.hasTag(TypeTag.VOID);
            return hasStringOp && !hasVoidOp;
        }

        /**
         * This routine applies following mappings:
         * - if input type is primitive, apply numeric promotion
         * - if input type is either 'void', 'null' or 'String' leave it untouched
         * - otherwise return 'Object'
         */
        private Type stringPromotion(Type t) {
            if (t.isPrimitive()) {
                return unaryPromotion(t);
            } else if (t.hasTag(TypeTag.VOID) || t.hasTag(TypeTag.BOT) ||
                    types.isSameType(t, syms.stringType)) {
                return t;
            } else if (t.hasTag(TypeTag.TYPEVAR)) {
                return stringPromotion(t.getUpperBound());
            } else {
                return syms.objectType;
            }
        }
    }

    /**
     * Class representing shift operator helper that operates on integral operand types (either boxed
     * or unboxed). Operator lookup is performed after applying unary numeric promotion to each input type.
     */
    class BinaryShiftOperator extends BinaryOperatorHelper {

        BinaryShiftOperator(Tag tag) {
            super(tag);
        }

        @Override
        public OperatorSymbol resolve(Type arg1, Type arg2) {
            return doLookup(unaryPromotion(arg1), unaryPromotion(arg2));
        }

        @Override
        public boolean test(Type arg1, Type arg2) {
            TypeTag op1 = unaryPromotion(arg1).getTag();
            TypeTag op2 = unaryPromotion(arg2).getTag();
            return (op1 == TypeTag.LONG || op1 == TypeTag.INT) &&
                    (op2 == TypeTag.LONG || op2 == TypeTag.INT);
        }
    }

    /**
     * This enum represent the possible kinds of an comparison test ('==' and '!=').
     */
    enum ComparisonKind {
        /** equality between numeric or boolean operands. */
        NUMERIC_OR_BOOLEAN,
        /** equality between reference operands. */
        REFERENCE,
        /** erroneous equality */
        INVALID
    }

    /**
     * Class representing equality operator helper that operates on either numeric, boolean or reference
     * types. Operator lookup for numeric/boolean equality test is performed after binary numeric
     * promotion to the input types. Operator lookup for reference equality test is performed assuming
     * the input type is 'Object'.
     */
    class BinaryEqualityOperator extends BinaryOperatorHelper {

        BinaryEqualityOperator(Tag tag) {
            super(tag);
        }

        @Override
        public boolean test(Type arg1, Type arg2) {
            return getKind(arg1, arg2) != ComparisonKind.INVALID;
        }

        @Override
        public OperatorSymbol resolve(Type t1, Type t2) {
            ComparisonKind kind = getKind(t1, t2);
            Type t = (kind == ComparisonKind.NUMERIC_OR_BOOLEAN) ?
                    binaryPromotion(t1, t2) :
                    syms.objectType;
            return doLookup(t, t);
        }

        /**
         * Retrieve the comparison kind associated with the given argument type pair.
         */
        private ComparisonKind getKind(Type arg1, Type arg2) {
            boolean arg1Primitive = arg1.isPrimitive();
            boolean arg2Primitive = arg2.isPrimitive();
            if (arg1Primitive && arg2Primitive) {
                return ComparisonKind.NUMERIC_OR_BOOLEAN;
            } else if (arg1Primitive) {
                return unaryPromotion(arg2).isPrimitive() ?
                        ComparisonKind.NUMERIC_OR_BOOLEAN : ComparisonKind.INVALID;
            } else if (arg2Primitive) {
                return unaryPromotion(arg1).isPrimitive() ?
                        ComparisonKind.NUMERIC_OR_BOOLEAN : ComparisonKind.INVALID;
            } else {
                return arg1.isNullOrReference() && arg2.isNullOrReference() ?
                        ComparisonKind.REFERENCE : ComparisonKind.INVALID;
            }
        }
    }

    /**
     * Initialize all unary operators.
     */
    private void initUnaryOperators() {
        initOperators(unaryOperators,
                new UnaryNumericOperator(Tag.POS)
                        .addUnaryOperator(DOUBLE, DOUBLE, nop)
                        .addUnaryOperator(FLOAT, FLOAT, nop)
                        .addUnaryOperator(LONG, LONG, nop)
                        .addUnaryOperator(INT, INT, nop),
                new UnaryNumericOperator(Tag.NEG)
                        .addUnaryOperator(DOUBLE, DOUBLE, dneg)
                        .addUnaryOperator(FLOAT, FLOAT, fneg)
                        .addUnaryOperator(LONG, LONG, lneg)
                        .addUnaryOperator(INT, INT, ineg),
                new UnaryNumericOperator(Tag.COMPL, Type::isIntegral)
                        .addUnaryOperator(LONG, LONG, lxor)
                        .addUnaryOperator(INT, INT, ixor),
                new UnaryPrefixPostfixOperator(Tag.POSTINC)
                        .addUnaryOperator(DOUBLE, DOUBLE, dadd)
                        .addUnaryOperator(FLOAT, FLOAT, fadd)
                        .addUnaryOperator(LONG, LONG, ladd)
                        .addUnaryOperator(INT, INT, iadd)
                        .addUnaryOperator(CHAR, CHAR, iadd)
                        .addUnaryOperator(SHORT, SHORT, iadd)
                        .addUnaryOperator(BYTE, BYTE, iadd),
                new UnaryPrefixPostfixOperator(Tag.POSTDEC)
                        .addUnaryOperator(DOUBLE, DOUBLE, dsub)
                        .addUnaryOperator(FLOAT, FLOAT, fsub)
                        .addUnaryOperator(LONG, LONG, lsub)
                        .addUnaryOperator(INT, INT, isub)
                        .addUnaryOperator(CHAR, CHAR, isub)
                        .addUnaryOperator(SHORT, SHORT, isub)
                        .addUnaryOperator(BYTE, BYTE, isub),
                new UnaryBooleanOperator(Tag.NOT)
                        .addUnaryOperator(BOOLEAN, BOOLEAN, bool_not),
                new UnaryReferenceOperator(Tag.NULLCHK)
                        .addUnaryOperator(OBJECT, OBJECT, nullchk));
    }

    /**
     * Initialize all binary operators.
     */
    private void initBinaryOperators() {
        initOperators(binaryOperators,
            new BinaryStringOperator(Tag.PLUS)
                    .addBinaryOperator(STRING, OBJECT, STRING, string_add)
                    .addBinaryOperator(OBJECT, STRING, STRING, string_add)
                    .addBinaryOperator(STRING, STRING, STRING, string_add)
                    .addBinaryOperator(STRING, INT, STRING, string_add)
                    .addBinaryOperator(STRING, LONG, STRING, string_add)
                    .addBinaryOperator(STRING, FLOAT, STRING, string_add)
                    .addBinaryOperator(STRING, DOUBLE, STRING, string_add)
                    .addBinaryOperator(STRING, BOOLEAN, STRING, string_add)
                    .addBinaryOperator(STRING, BOT, STRING, string_add)
                    .addBinaryOperator(INT, STRING, STRING, string_add)
                    .addBinaryOperator(LONG, STRING, STRING, string_add)
                    .addBinaryOperator(FLOAT, STRING, STRING, string_add)
                    .addBinaryOperator(DOUBLE, STRING, STRING, string_add)
                    .addBinaryOperator(BOOLEAN, STRING, STRING, string_add)
                    .addBinaryOperator(BOT, STRING, STRING, string_add),
            new BinaryNumericOperator(Tag.PLUS)
                    .addBinaryOperator(DOUBLE, DOUBLE, DOUBLE, dadd)
                    .addBinaryOperator(FLOAT, FLOAT, FLOAT, fadd)
                    .addBinaryOperator(LONG, LONG, LONG, ladd)
                    .addBinaryOperator(INT, INT, INT, iadd),
            new BinaryNumericOperator(Tag.MINUS)
                    .addBinaryOperator(DOUBLE, DOUBLE, DOUBLE, dsub)
                    .addBinaryOperator(FLOAT, FLOAT, FLOAT, fsub)
                    .addBinaryOperator(LONG, LONG, LONG, lsub)
                    .addBinaryOperator(INT, INT, INT, isub),
            new BinaryNumericOperator(Tag.MUL)
                    .addBinaryOperator(DOUBLE, DOUBLE, DOUBLE, dmul)
                    .addBinaryOperator(FLOAT, FLOAT, FLOAT, fmul)
                    .addBinaryOperator(LONG, LONG, LONG, lmul)
                    .addBinaryOperator(INT, INT, INT, imul),
            new BinaryNumericOperator(Tag.DIV)
                    .addBinaryOperator(DOUBLE, DOUBLE, DOUBLE, ddiv)
                    .addBinaryOperator(FLOAT, FLOAT, FLOAT, fdiv)
                    .addBinaryOperator(LONG, LONG, LONG, ldiv)
                    .addBinaryOperator(INT, INT, INT, idiv),
            new BinaryNumericOperator(Tag.MOD)
                    .addBinaryOperator(DOUBLE, DOUBLE, DOUBLE, dmod)
                    .addBinaryOperator(FLOAT, FLOAT, FLOAT, fmod)
                    .addBinaryOperator(LONG, LONG, LONG, lmod)
                    .addBinaryOperator(INT, INT, INT, imod),
            new BinaryBooleanOperator(Tag.BITAND)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, iand),
            new BinaryNumericOperator(Tag.BITAND, Type::isIntegral)
                    .addBinaryOperator(LONG, LONG, LONG, land)
                    .addBinaryOperator(INT, INT, INT, iand),
            new BinaryBooleanOperator(Tag.BITOR)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, ior),
            new BinaryNumericOperator(Tag.BITOR, Type::isIntegral)
                    .addBinaryOperator(LONG, LONG, LONG, lor)
                    .addBinaryOperator(INT, INT, INT, ior),
            new BinaryBooleanOperator(Tag.BITXOR)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, ixor),
            new BinaryNumericOperator(Tag.BITXOR, Type::isIntegral)
                    .addBinaryOperator(LONG, LONG, LONG, lxor)
                    .addBinaryOperator(INT, INT, INT, ixor),
            new BinaryShiftOperator(Tag.SL)
                    .addBinaryOperator(INT, INT, INT, ishl)
                    .addBinaryOperator(INT, LONG, INT, ishll)
                    .addBinaryOperator(LONG, INT, LONG, lshl)
                    .addBinaryOperator(LONG, LONG, LONG, lshll),
            new BinaryShiftOperator(Tag.SR)
                    .addBinaryOperator(INT, INT, INT, ishr)
                    .addBinaryOperator(INT, LONG, INT, ishrl)
                    .addBinaryOperator(LONG, INT, LONG, lshr)
                    .addBinaryOperator(LONG, LONG, LONG, lshrl),
            new BinaryShiftOperator(Tag.USR)
                    .addBinaryOperator(INT, INT, INT, iushr)
                    .addBinaryOperator(INT, LONG, INT, iushrl)
                    .addBinaryOperator(LONG, INT, LONG, lushr)
                    .addBinaryOperator(LONG, LONG, LONG, lushrl),
            new BinaryNumericOperator(Tag.LT)
                    .addBinaryOperator(DOUBLE, DOUBLE, BOOLEAN, dcmpg, iflt)
                    .addBinaryOperator(FLOAT, FLOAT, BOOLEAN, fcmpg, iflt)
                    .addBinaryOperator(LONG, LONG, BOOLEAN, lcmp, iflt)
                    .addBinaryOperator(INT, INT, BOOLEAN, if_icmplt),
            new BinaryNumericOperator(Tag.GT)
                    .addBinaryOperator(DOUBLE, DOUBLE, BOOLEAN, dcmpl, ifgt)
                    .addBinaryOperator(FLOAT, FLOAT, BOOLEAN, fcmpl, ifgt)
                    .addBinaryOperator(LONG, LONG, BOOLEAN, lcmp, ifgt)
                    .addBinaryOperator(INT, INT, BOOLEAN, if_icmpgt),
            new BinaryNumericOperator(Tag.LE)
                    .addBinaryOperator(DOUBLE, DOUBLE, BOOLEAN, dcmpg, ifle)
                    .addBinaryOperator(FLOAT, FLOAT, BOOLEAN, fcmpg, ifle)
                    .addBinaryOperator(LONG, LONG, BOOLEAN, lcmp, ifle)
                    .addBinaryOperator(INT, INT, BOOLEAN, if_icmple),
            new BinaryNumericOperator(Tag.GE)
                    .addBinaryOperator(DOUBLE, DOUBLE, BOOLEAN, dcmpl, ifge)
                    .addBinaryOperator(FLOAT, FLOAT, BOOLEAN, fcmpl, ifge)
                    .addBinaryOperator(LONG, LONG, BOOLEAN, lcmp, ifge)
                    .addBinaryOperator(INT, INT, BOOLEAN, if_icmpge),
            new BinaryEqualityOperator(Tag.EQ)
                    .addBinaryOperator(OBJECT, OBJECT, BOOLEAN, if_acmpeq)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, if_icmpeq)
                    .addBinaryOperator(DOUBLE, DOUBLE, BOOLEAN, dcmpl, ifeq)
                    .addBinaryOperator(FLOAT, FLOAT, BOOLEAN, fcmpl, ifeq)
                    .addBinaryOperator(LONG, LONG, BOOLEAN, lcmp, ifeq)
                    .addBinaryOperator(INT, INT, BOOLEAN, if_icmpeq),
            new BinaryEqualityOperator(Tag.NE)
                    .addBinaryOperator(OBJECT, OBJECT, BOOLEAN, if_acmpne)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, if_icmpne)
                    .addBinaryOperator(DOUBLE, DOUBLE, BOOLEAN, dcmpl, ifne)
                    .addBinaryOperator(FLOAT, FLOAT, BOOLEAN, fcmpl, ifne)
                    .addBinaryOperator(LONG, LONG, BOOLEAN, lcmp, ifne)
                    .addBinaryOperator(INT, INT, BOOLEAN, if_icmpne),
            new BinaryBooleanOperator(Tag.AND)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, bool_and),
            new BinaryBooleanOperator(Tag.OR)
                    .addBinaryOperator(BOOLEAN, BOOLEAN, BOOLEAN, bool_or));
    }

    OperatorSymbol lookupBinaryOp(Predicate<OperatorSymbol> applicabilityTest) {
        return binaryOperators.values().stream()
                .flatMap(List::stream)
                .map(helper -> helper.doLookup(applicabilityTest))
                .distinct()
                .filter(sym -> sym != noOpSymbol)
                .findFirst().get();
    }

    /**
     * Complete the initialization of an operator helper by storing it into the corresponding operator map.
     */
    @SafeVarargs
    private final <O extends OperatorHelper> void initOperators(Map<Name, List<O>> opsMap, O... ops) {
        for (O o : ops) {
            Name opName = o.name;
            List<O> helpers = opsMap.getOrDefault(opName, List.nil());
            opsMap.put(opName, helpers.prepend(o));
        }
    }

    /**
     * Initialize operator name array.
     */
    private void initOperatorNames() {
        setOperatorName(Tag.POS, "+");
        setOperatorName(Tag.NEG, "-");
        setOperatorName(Tag.NOT, "!");
        setOperatorName(Tag.COMPL, "~");
        setOperatorName(Tag.PREINC, "++");
        setOperatorName(Tag.PREDEC, "--");
        setOperatorName(Tag.POSTINC, "++");
        setOperatorName(Tag.POSTDEC, "--");
        setOperatorName(Tag.NULLCHK, "<*nullchk*>");
        setOperatorName(Tag.OR, "||");
        setOperatorName(Tag.AND, "&&");
        setOperatorName(Tag.EQ, "==");
        setOperatorName(Tag.NE, "!=");
        setOperatorName(Tag.LT, "<");
        setOperatorName(Tag.GT, ">");
        setOperatorName(Tag.LE, "<=");
        setOperatorName(Tag.GE, ">=");
        setOperatorName(Tag.BITOR, "|");
        setOperatorName(Tag.BITXOR, "^");
        setOperatorName(Tag.BITAND, "&");
        setOperatorName(Tag.SL, "<<");
        setOperatorName(Tag.SR, ">>");
        setOperatorName(Tag.USR, ">>>");
        setOperatorName(Tag.PLUS, "+");
        setOperatorName(Tag.MINUS, names.hyphen);
        setOperatorName(Tag.MUL, names.asterisk);
        setOperatorName(Tag.DIV, names.slash);
        setOperatorName(Tag.MOD, "%");
    }
    //where
        private void setOperatorName(Tag tag, String name) {
            setOperatorName(tag, names.fromString(name));
        }

        private void setOperatorName(Tag tag, Name name) {
            opname[tag.operatorIndex()] = name;
        }
}
