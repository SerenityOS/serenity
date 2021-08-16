/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.factories;

import java.util.Collection;
import java.util.Optional;

import jdk.test.lib.jittester.BinaryOperator;
import jdk.test.lib.jittester.Block;
import jdk.test.lib.jittester.Break;
import jdk.test.lib.jittester.CastOperator;
import jdk.test.lib.jittester.Continue;
import jdk.test.lib.jittester.Declaration;
import jdk.test.lib.jittester.IRNode;
import jdk.test.lib.jittester.If;
import jdk.test.lib.jittester.Literal;
import jdk.test.lib.jittester.LocalVariable;
import jdk.test.lib.jittester.NonStaticMemberVariable;
import jdk.test.lib.jittester.Nothing;
import jdk.test.lib.jittester.Operator;
import jdk.test.lib.jittester.OperatorKind;
import jdk.test.lib.jittester.PrintVariables;
import jdk.test.lib.jittester.ProductionFailedException;
import jdk.test.lib.jittester.ProductionParams;
import jdk.test.lib.jittester.Statement;
import jdk.test.lib.jittester.StaticMemberVariable;
import jdk.test.lib.jittester.Switch;
import jdk.test.lib.jittester.Symbol;
import jdk.test.lib.jittester.TernaryOperator;
import jdk.test.lib.jittester.Throw;
import jdk.test.lib.jittester.TryCatchBlock;
import jdk.test.lib.jittester.Type;
import jdk.test.lib.jittester.TypeList;
import jdk.test.lib.jittester.UnaryOperator;
import jdk.test.lib.jittester.VariableBase;
import jdk.test.lib.jittester.VariableDeclaration;
import jdk.test.lib.jittester.VariableDeclarationBlock;
import jdk.test.lib.jittester.VariableInitialization;
import jdk.test.lib.jittester.arrays.ArrayCreation;
import jdk.test.lib.jittester.arrays.ArrayElement;
import jdk.test.lib.jittester.arrays.ArrayExtraction;
import jdk.test.lib.jittester.classes.ClassDefinitionBlock;
import jdk.test.lib.jittester.classes.Interface;
import jdk.test.lib.jittester.classes.Klass;
import jdk.test.lib.jittester.classes.MainKlass;
import jdk.test.lib.jittester.functions.ArgumentDeclaration;
import jdk.test.lib.jittester.functions.ConstructorDefinition;
import jdk.test.lib.jittester.functions.ConstructorDefinitionBlock;
import jdk.test.lib.jittester.functions.Function;
import jdk.test.lib.jittester.functions.FunctionDeclaration;
import jdk.test.lib.jittester.functions.FunctionDeclarationBlock;
import jdk.test.lib.jittester.functions.FunctionDefinition;
import jdk.test.lib.jittester.functions.FunctionDefinitionBlock;
import jdk.test.lib.jittester.functions.FunctionInfo;
import jdk.test.lib.jittester.functions.FunctionRedefinition;
import jdk.test.lib.jittester.functions.FunctionRedefinitionBlock;
import jdk.test.lib.jittester.functions.Return;
import jdk.test.lib.jittester.functions.StaticConstructorDefinition;
import jdk.test.lib.jittester.loops.CounterInitializer;
import jdk.test.lib.jittester.loops.CounterManipulator;
import jdk.test.lib.jittester.loops.DoWhile;
import jdk.test.lib.jittester.loops.For;
import jdk.test.lib.jittester.loops.LoopingCondition;
import jdk.test.lib.jittester.loops.While;
import jdk.test.lib.jittester.types.TypeKlass;

public class IRNodeBuilder {
    //private Optional<Type> variableType = Optional.empty();
    private Optional<TypeKlass> argumentType = Optional.empty();
    private Optional<Integer> variableNumber = Optional.empty();
    private Optional<Long> complexityLimit = Optional.empty();
    private Optional<Integer> operatorLimit = Optional.empty();
    private Optional<TypeKlass> ownerClass = Optional.empty();
    private Optional<Type> resultType = Optional.empty();
    private Optional<Boolean> safe = Optional.empty();
    private Optional<Boolean> noConsts = Optional.empty();
    private Optional<OperatorKind> opKind = Optional.empty();
    private Optional<Integer> statementLimit = Optional.empty();
    private Optional<Boolean> subBlock = Optional.empty();
    private Optional<Boolean> canHaveBreaks = Optional.empty();
    private Optional<Boolean> canHaveContinues = Optional.empty();
    private Optional<Boolean> canHaveReturn = Optional.empty();
    //not in use yet because 'throw' is only placed to the locations where 'return' is allowed
    private Optional<Boolean> canHaveThrow = Optional.empty();
    private Optional<Integer> level = Optional.empty();
    private Optional<String> prefix = Optional.empty();
    private Optional<Integer> memberFunctionsLimit = Optional.empty();
    private Optional<Integer> memberFunctionsArgLimit = Optional.empty();
    private Optional<LocalVariable> localVariable = Optional.empty();
    private Optional<Boolean> isLocal = Optional.empty();
    private Optional<Boolean> isStatic = Optional.empty();
    private Optional<Boolean> isConstant = Optional.empty();
    private Optional<Boolean> isInitialized = Optional.empty();
    private Optional<String> name = Optional.empty();
    private Optional<Integer> flags = Optional.empty();
    private Optional<FunctionInfo> functionInfo = Optional.empty();
    private Optional<Boolean> semicolon = Optional.empty();

    public Factory<ArgumentDeclaration> getArgumentDeclarationFactory() {
        return new ArgumentDeclarationFactory(getArgumentType(), getVariableNumber());
    }

    public Factory<Operator> getArithmeticOperatorFactory() throws ProductionFailedException {
        return new ArithmeticOperatorFactory(getComplexityLimit(), getOperatorLimit(),
                getOwnerClass(), getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<ArrayCreation> getArrayCreationFactory() {
        return new ArrayCreationFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<ArrayElement> getArrayElementFactory() {
        return new ArrayElementFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<ArrayExtraction> getArrayExtractionFactory() {
        return new ArrayExtractionFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<Operator> getAssignmentOperatorFactory() {
        return new AssignmentOperatorFactory(getComplexityLimit(), getOperatorLimit(),
                getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());
    }

    public Factory<BinaryOperator> getBinaryOperatorFactory() throws ProductionFailedException {
        OperatorKind o = getOperatorKind();
        switch (o) {
            case ASSIGN:
                return new AssignmentOperatorImplFactory(getComplexityLimit(), getOperatorLimit(),
                        getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());
            case AND:
            case OR:
                return new BinaryLogicOperatorFactory(o, getComplexityLimit(), getOperatorLimit(),
                        getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());
            case BIT_OR:
            case BIT_XOR:
            case BIT_AND:
                return new BinaryBitwiseOperatorFactory(o, getComplexityLimit(), getOperatorLimit(),
                        getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());

            case EQ:
            case NE:
                return new BinaryEqualityOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case GT:
            case LT:
            case GE:
            case LE:
                return new BinaryComparisonOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case SHR:
            case SHL:
            case SAR:
                return new BinaryShiftOperatorFactory(o, getComplexityLimit(), getOperatorLimit(),
                        getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
                return new BinaryArithmeticOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case STRADD:
                return new BinaryStringPlusFactory(getComplexityLimit(), getOperatorLimit(),
                        getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());
            case COMPOUND_ADD:
            case COMPOUND_SUB:
            case COMPOUND_MUL:
            case COMPOUND_DIV:
            case COMPOUND_MOD:
                return new CompoundArithmeticAssignmentOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case COMPOUND_AND:
            case COMPOUND_OR:
            case COMPOUND_XOR:
                return new CompoundBitwiseAssignmentOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case COMPOUND_SHR:
            case COMPOUND_SHL:
            case COMPOUND_SAR:
                return new CompoundShiftAssignmentOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            default:
                throw new ProductionFailedException();
        }
    }

    public Factory<UnaryOperator> getUnaryOperatorFactory() throws ProductionFailedException {
        OperatorKind o = getOperatorKind();
        switch (o) {
            case NOT:
                return new LogicalInversionOperatorFactory(getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case BIT_NOT:
                return new BitwiseInversionOperatorFactory(getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case UNARY_PLUS:
            case UNARY_MINUS:
                return new UnaryPlusMinusOperatorFactory(o, getComplexityLimit(),
                        getOperatorLimit(), getOwnerClass(), resultType.orElse(null), getExceptionSafe(),
                        getNoConsts());
            case PRE_DEC:
            case POST_DEC:
            case PRE_INC:
            case POST_INC:
                return new IncDecOperatorFactory(o, getComplexityLimit(), getOperatorLimit(),
                        getOwnerClass(), resultType.orElse(null), getExceptionSafe(), getNoConsts());
            default:
                throw new ProductionFailedException();
        }
    }

    public Factory<Block> getBlockFactory() {
        return new BlockFactory(getOwnerClass(), getResultType(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel(), subBlock.orElse(false),
                canHaveBreaks.orElse(false), canHaveContinues.orElse(false),
                canHaveReturn.orElse(false), canHaveReturn.orElse(false));
        //now 'throw' can be placed only in the same positions as 'return'
    }

    public Factory<Break> getBreakFactory() {
        return new BreakFactory();
    }

    public Factory<CastOperator> getCastOperatorFactory() {
        return new CastOperatorFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<ClassDefinitionBlock> getClassDefinitionBlockFactory() {
        return new ClassDefinitionBlockFactory(getPrefix(),
                ProductionParams.classesLimit.value(),
                ProductionParams.memberFunctionsLimit.value(),
                ProductionParams.memberFunctionsArgLimit.value(),
                getComplexityLimit(),
                ProductionParams.statementLimit.value(),
                ProductionParams.operatorLimit.value(),
                getLevel());
    }

    public Factory<MainKlass> getMainKlassFactory() {
        return new MainKlassFactory(getName(), getComplexityLimit(),
                ProductionParams.memberFunctionsLimit.value(),
                ProductionParams.memberFunctionsArgLimit.value(),
                ProductionParams.statementLimit.value(),
                ProductionParams.testStatementLimit.value(),
                ProductionParams.operatorLimit.value());
    }

    public Factory<ConstructorDefinitionBlock> getConstructorDefinitionBlockFactory() {
        return new ConstructorDefinitionBlockFactory(getOwnerClass(), getMemberFunctionsLimit(),
                ProductionParams.memberFunctionsArgLimit.value(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel());
    }

    public Factory<ConstructorDefinition> getConstructorDefinitionFactory() {
        return new ConstructorDefinitionFactory(getOwnerClass(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(),
                getMemberFunctionsArgLimit(), getLevel());
    }

    public Factory<Continue> getContinueFactory() {
        return new ContinueFactory();
    }

    public Factory<CounterInitializer> getCounterInitializerFactory(int counterValue) {
        return new CounterInitializerFactory(getOwnerClass(), counterValue);
    }

    public Factory<CounterManipulator> getCounterManipulatorFactory() {
        return new CounterManipulatorFactory(getLocalVariable());
    }

    public Factory<Declaration> getDeclarationFactory() {
        return new DeclarationFactory(getOwnerClass(), getComplexityLimit(), getOperatorLimit(),
                getIsLocal(), getExceptionSafe());
    }

    public Factory<DoWhile> getDoWhileFactory() {
        return new DoWhileFactory(getOwnerClass(), getResultType(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel(), getCanHaveReturn());
    }

    public Factory<While> getWhileFactory() {
        return new WhileFactory(getOwnerClass(), getResultType(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel(), getCanHaveReturn());
    }

    public Factory<If> getIfFactory() {
        return new IfFactory(getOwnerClass(), getResultType(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel(), getCanHaveBreaks(),
                getCanHaveContinues(), getCanHaveReturn());
    }

    public Factory<For> getForFactory() {
        return new ForFactory(getOwnerClass(), getResultType(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel(), getCanHaveReturn());
    }

    public Factory<Switch> getSwitchFactory() { // TODO: switch is not used now
        return new SwitchFactory(getOwnerClass(), getComplexityLimit(), getStatementLimit(),
                getOperatorLimit(), getLevel(), getCanHaveReturn());
    }

    public Factory<IRNode> getExpressionFactory() throws ProductionFailedException {
        return new ExpressionFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<FunctionDeclarationBlock> getFunctionDeclarationBlockFactory() {
        return new FunctionDeclarationBlockFactory(getOwnerClass(), getMemberFunctionsLimit(),
                getMemberFunctionsArgLimit(), getLevel());
    }

    public Factory<FunctionDeclaration> getFunctionDeclarationFactory() {
        return new FunctionDeclarationFactory(getName(), getOwnerClass(),resultType.orElse(TypeList.VOID),
                getMemberFunctionsArgLimit(), getFlags());
    }

    public Factory<FunctionDefinitionBlock> getFunctionDefinitionBlockFactory() {
        return new FunctionDefinitionBlockFactory(getOwnerClass(), getMemberFunctionsLimit(),
                getMemberFunctionsArgLimit(), getComplexityLimit(), getStatementLimit(),
                getOperatorLimit(), getLevel(), getFlags());
    }

    public Factory<FunctionDefinition> getFunctionDefinitionFactory() {
        return new FunctionDefinitionFactory(getName(), getOwnerClass(), resultType.orElse(TypeList.VOID),
                getComplexityLimit(), getStatementLimit(), getOperatorLimit(),
                getMemberFunctionsArgLimit(), getLevel(), getFlags());
    }

    public Factory<Function> getFunctionFactory() {
        return new FunctionFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                resultType.orElse(null), getExceptionSafe());
    }

    public Factory<FunctionRedefinitionBlock> getFunctionRedefinitionBlockFactory(Collection<Symbol>
                                                                                        functionSet) {
        return new FunctionRedefinitionBlockFactory(functionSet, getOwnerClass(),
                getComplexityLimit(), getStatementLimit(), getOperatorLimit(), getLevel());
    }

    public Factory<FunctionRedefinition> getFunctionRedefinitionFactory() {
        return new FunctionRedefinitionFactory(getFunctionInfo(), getOwnerClass(),
                getComplexityLimit(), getStatementLimit(), getOperatorLimit(), getLevel(),
                getFlags());
    }

    public Factory<Interface> getInterfaceFactory() {
        return new InterfaceFactory(getName(), getMemberFunctionsLimit(),
                getMemberFunctionsArgLimit(), getLevel());
    }

    public Factory<Klass> getKlassFactory() {
        return new KlassFactory(getName(), getComplexityLimit(),
                getMemberFunctionsLimit(), getMemberFunctionsArgLimit(), getStatementLimit(),
                getOperatorLimit(), getLevel());
    }

    public Factory<IRNode> getLimitedExpressionFactory() throws ProductionFailedException {
        return new LimitedExpressionFactory(getComplexityLimit(), getOperatorLimit(),
                getOwnerClass(), getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<Literal> getLiteralFactory() {
        return new LiteralFactory(getResultType());
    }

    public Factory<LocalVariable> getLocalVariableFactory() {
        return new LocalVariableFactory(/*getVariableType()*/getResultType(), getFlags());
    }

    public Factory<Operator> getLogicOperatorFactory() throws ProductionFailedException {
        return new LogicOperatorFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<LoopingCondition> getLoopingConditionFactory(Literal _limiter) {
        return new LoopingConditionFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getLocalVariable(), _limiter);
    }

    public Factory<NonStaticMemberVariable> getNonStaticMemberVariableFactory() {
        return new NonStaticMemberVariableFactory(getComplexityLimit(), getOperatorLimit(),
                getOwnerClass(), /*getVariableType()*/getResultType(), getFlags(), getExceptionSafe());
    }

    public Factory<Nothing> getNothingFactory() {
        return new NothingFactory();
    }

    public Factory<PrintVariables> getPrintVariablesFactory() {
        return new PrintVariablesFactory(getOwnerClass(), getLevel());
    }

    public Factory<Return> getReturnFactory() {
        return new ReturnFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe());
    }

    public Factory<Throw> getThrowFactory() {
        return new ThrowFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(), getResultType(), getExceptionSafe());
    }

    public Factory<Statement> getStatementFactory() {
        return new StatementFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getExceptionSafe(), getNoConsts(), semicolon.orElse(true));
    }

    public Factory<StaticConstructorDefinition> getStaticConstructorDefinitionFactory() {
        return new StaticConstructorDefinitionFactory(getOwnerClass(), getComplexityLimit(),
                getStatementLimit(), getOperatorLimit(), getLevel());
    }

    public Factory<StaticMemberVariable> getStaticMemberVariableFactory() {
        return new StaticMemberVariableFactory(getOwnerClass(), /*getVariableType()*/getResultType(), getFlags());
    }

    public Factory<TernaryOperator> getTernaryOperatorFactory() {
        return new TernaryOperatorFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                getResultType(), getExceptionSafe(), getNoConsts());
    }

    public Factory<VariableDeclarationBlock> getVariableDeclarationBlockFactory() {
        return new VariableDeclarationBlockFactory(getOwnerClass(), getComplexityLimit(),
                getOperatorLimit(), getLevel(), getExceptionSafe());
    }

    public Factory<VariableDeclaration> getVariableDeclarationFactory() {
        return new VariableDeclarationFactory(getOwnerClass(), getIsStatic(), getIsLocal(), getResultType());
    }

    public Factory<VariableBase> getVariableFactory() {
        return new VariableFactory(getComplexityLimit(), getOperatorLimit(), getOwnerClass(),
                /*getVariableType()*/getResultType(), getIsConstant(), getIsInitialized(), getExceptionSafe(), getNoConsts());
    }

    public Factory<VariableInitialization> getVariableInitializationFactory() {
        return new VariableInitializationFactory(getOwnerClass(), getIsConstant(), getIsStatic(),
                getIsLocal(), getComplexityLimit(), getOperatorLimit(), getExceptionSafe());
    }

    public Factory<TryCatchBlock> getTryCatchBlockFactory() {
        return new TryCatchBlockFactory(getOwnerClass(), getResultType(),
                getComplexityLimit(), getStatementLimit(), getOperatorLimit(),
                getLevel(), subBlock.orElse(false), getCanHaveBreaks(),
                getCanHaveContinues(), getCanHaveReturn());
    }

/*    public IRNodeBuilder setVariableType(Type value) {
        variableType = Optional.of(value);
        return this;
    }*/

    public IRNodeBuilder setArgumentType(TypeKlass value) {
        argumentType = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setVariableNumber(int value) {
        variableNumber = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setComplexityLimit(long value) {
        complexityLimit = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setOperatorLimit(int value) {
        operatorLimit = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setStatementLimit(int value) {
        statementLimit = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setOwnerKlass(TypeKlass value) {
        ownerClass = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setResultType(Type value) {
        resultType = Optional.of(value);
        return this;
    }
    // TODO: check if safe is always true in current implementation
    public IRNodeBuilder setExceptionSafe(boolean value) {
        safe = Optional.of(value);
        return this;
    }
    // TODO: check is noconsts is always false in current implementation
    public IRNodeBuilder setNoConsts(boolean value) {
        noConsts = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setOperatorKind(OperatorKind value) {
        opKind = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setLevel(int value) {
        level = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setSubBlock(boolean value) {
        subBlock = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setCanHaveBreaks(boolean value) {
        canHaveBreaks = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setCanHaveContinues(boolean value) {
        canHaveContinues = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setCanHaveReturn(boolean value) {
        canHaveReturn = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setCanHaveThrow(boolean value) {
        canHaveThrow = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setPrefix(String value) {
        prefix = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setMemberFunctionsLimit(int value) {
        memberFunctionsLimit = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setMemberFunctionsArgLimit(int value) {
        memberFunctionsArgLimit = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setLocalVariable(LocalVariable value) {
        localVariable = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setIsLocal(boolean value) {
        isLocal = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setIsStatic(boolean value) {
        isStatic = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setIsInitialized(boolean value) {
        isInitialized = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setIsConstant(boolean value) {
        isConstant = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setName(String value) {
        name = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setFlags(int value) {
        flags = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setFunctionInfo(FunctionInfo value) {
        functionInfo = Optional.of(value);
        return this;
    }

    public IRNodeBuilder setSemicolon(boolean value) {
        semicolon = Optional.of(value);
        return this;
    }

    // getters
/*    private Type getVariableType() {
        return variableType.orElseThrow(() -> new IllegalArgumentException(
                "Variable type wasn't set"));
    }*/

    private TypeKlass getArgumentType() {
        return argumentType.orElseThrow(() -> new IllegalArgumentException(
                "Argument type wasn't set"));
    }

    private int getVariableNumber() {
        return variableNumber.orElseThrow(() -> new IllegalArgumentException(
                "Variable number wasn't set"));
    }

    private long getComplexityLimit() {
        return complexityLimit.orElseThrow(() -> new IllegalArgumentException(
                "Complexity limit wasn't set"));
    }

    private int getOperatorLimit() {
        return operatorLimit.orElseThrow(() -> new IllegalArgumentException(
                "Operator limit wasn't set"));
    }

    private int getStatementLimit() {
        return statementLimit.orElseThrow(() -> new IllegalArgumentException(
                "Statement limit wasn't set"));
    }

    private TypeKlass getOwnerClass() {
        return ownerClass.orElseThrow(() -> new IllegalArgumentException("Type_Klass wasn't set"));
    }

    private Type getResultType() {
        return resultType.orElseThrow(() -> new IllegalArgumentException("Return type wasn't set"));
    }

    private boolean getExceptionSafe() {
        return safe.orElseThrow(() -> new IllegalArgumentException("Safe wasn't set"));
    }

    private boolean getNoConsts() {
        return noConsts.orElseThrow(() -> new IllegalArgumentException("NoConsts wasn't set"));
    }

    private OperatorKind getOperatorKind() {
        return opKind.orElseThrow(() -> new IllegalArgumentException("Operator kind wasn't set"));
    }

    private int getLevel() {
        return level.orElseThrow(() -> new IllegalArgumentException("Level wasn't set"));
    }

    private String getPrefix() {
        return prefix.orElseThrow(() -> new IllegalArgumentException("Prefix wasn't set"));
    }

    private int getMemberFunctionsLimit() {
        return memberFunctionsLimit.orElseThrow(() -> new IllegalArgumentException(
                "memberFunctions limit wasn't set"));
    }

    private int getMemberFunctionsArgLimit() {
        return memberFunctionsArgLimit.orElseThrow(() -> new IllegalArgumentException(
                "memberFunctionsArg limit wasn't set"));
    }

    private LocalVariable getLocalVariable() {
        return localVariable.orElseThrow(() -> new IllegalArgumentException(
                "local variable wasn't set"));
    }

    private boolean getIsLocal() {
        return isLocal.orElseThrow(() -> new IllegalArgumentException("isLocal wasn't set"));
    }

    private boolean getIsStatic() {
        return isStatic.orElseThrow(() -> new IllegalArgumentException("isStatic wasn't set"));
    }

    private boolean getIsInitialized() {
        return isInitialized.orElseThrow(() -> new IllegalArgumentException(
                "isInitialized wasn't set"));
    }

    private boolean getIsConstant() {
        return isConstant.orElseThrow(() -> new IllegalArgumentException("isConstant wasn't set"));
    }

    private boolean getCanHaveReturn() {
        return canHaveReturn.orElseThrow(() -> new IllegalArgumentException(
                "canHaveReturn wasn't set"));
    }

    private boolean getCanHaveBreaks() {
        return canHaveBreaks.orElseThrow(() -> new IllegalArgumentException(
                "canHaveBreaks wasn't set"));
    }

    private boolean getCanHaveContinues() {
        return canHaveContinues.orElseThrow(() -> new IllegalArgumentException(
                "canHaveContinues wasn't set"));
    }

    private String getName() {
        return name.orElseThrow(() -> new IllegalArgumentException("Name wasn't set"));
    }

    private int getFlags() {
        return flags.orElseThrow(() -> new IllegalArgumentException("Flags wasn't set"));
    }

    private FunctionInfo getFunctionInfo() {
        return functionInfo.orElseThrow(() -> new IllegalArgumentException(
                "FunctionInfo wasn't set"));
    }
}
