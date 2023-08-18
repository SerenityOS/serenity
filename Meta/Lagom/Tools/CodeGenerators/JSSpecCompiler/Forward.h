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

class Node;
using NullableTree = RefPtr<Node>;
using Tree = NonnullRefPtr<Node>;
class ErrorNode;

class ScopedBlock;
class MathematicalConstant;
class StringLiteral;
class BinaryOperation;
class UnaryOperation;
class IsOneOfOperation;
class UnresolvedReference;
class ReturnExpression;
class AssertExpression;
class IfBranch;
class ElseIfBranch;
class TreeList;
class RecordDirectListInitialization;
class FunctionCall;
class SlotName;
class Variable;
class FunctionPointer;

// Parser/SpecParser.h
class AlgorithmStep;
class AlgorithmStepList;
class Algorithm;
class SpecFunction;

}
