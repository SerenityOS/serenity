/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>

namespace JSSpecCompiler {

// AST/AST.h
class NodeSubtreePointer;
class VariableDeclaration;
using VariableDeclarationRef = NonnullRefPtr<VariableDeclaration>;
class NamedVariableDeclaration;
using NamedVariableDeclarationRef = NonnullRefPtr<NamedVariableDeclaration>;
class SSAVariableDeclaration;
using SSAVariableDeclarationRef = RefPtr<SSAVariableDeclaration>;

class Node;
using NullableTree = RefPtr<Node>;
using Tree = NonnullRefPtr<Node>;
class Statement;
class Expression;
class ErrorNode;
class ControlFlowOperator;

class ControlFlowFunctionReturn;
class ControlFlowJump;
class ControlFlowBranch;
class MathematicalConstant;
class StringLiteral;
class BinaryOperation;
class UnaryOperation;
class IsOneOfOperation;
class UnresolvedReference;
class ReturnNode;
class AssertExpression;
class IfBranch;
class ElseIfBranch;
class IfElseIfChain;
class TreeList;
class RecordDirectListInitialization;
class FunctionCall;
class SlotName;
class Enumerator;
using EnumeratorRef = NonnullRefPtr<Enumerator>;
class Variable;
using VariableRef = NonnullRefPtr<Variable>;
class FunctionPointer;
using FunctionPointerRef = NonnullRefPtr<FunctionPointer>;

// Compiler/ControlFlowGraph.h
class BasicBlock;
using BasicBlockRef = BasicBlock*;
class ControlFlowGraph;

// Compiler/GenericASTPass.h
class RecursiveASTVisitor;

// Parser/SpecParser.h
class SpecificationParsingContext;
class AlgorithmStep;
class AlgorithmStepList;
class Algorithm;
class SpecificationFunction;
class SpecificationClause;
class Specification;

namespace Runtime {
class Cell;
class Object;
class ObjectType;
class Realm;
}

// DiagnosticEngine.h
struct LogicalLocation;
struct Location;
class DiagnosticEngine;

// Function.h
class TranslationUnit;
using TranslationUnitRef = TranslationUnit*;
class FunctionDeclaration;
using FunctionDeclarationRef = FunctionDeclaration*;
class FunctionDefinition;
using FunctionDefinitionRef = FunctionDefinition*;

}
