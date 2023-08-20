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

class Node;
using NullableTree = RefPtr<Node>;
using Tree = NonnullRefPtr<Node>;
class Statement;
class Expression;
class ErrorNode;

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
class Variable;
class FunctionPointer;
using FunctionPointerRef = NonnullRefPtr<FunctionPointer>;

// Compiler/GenericASTPass.h
class RecursiveASTVisitor;

// Parser/SpecParser.h
class AlgorithmStep;
class AlgorithmStepList;
class Algorithm;
class SpecFunction;

// Function.h
class ExecutionContext;
class Function;
using FunctionRef = Function*;

}
