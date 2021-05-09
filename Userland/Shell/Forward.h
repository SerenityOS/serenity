/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Shell::AST {

struct Command;
class Node;
class Value;
class SyntaxError;
class Pipeline;
struct Rewiring;
class NodeVisitor;

class PathRedirectionNode;
class And;
class ListConcatenate;
class Background;
class BarewordLiteral;
class BraceExpansion;
class CastToCommand;
class CastToList;
class CloseFdRedirection;
class CommandLiteral;
class Comment;
class ContinuationControl;
class DynamicEvaluate;
class DoubleQuotedString;
class Fd2FdRedirection;
class FunctionDeclaration;
class ForLoop;
class Glob;
class Heredoc;
class HistoryEvent;
class Execute;
class IfCond;
class ImmediateExpression;
class Join;
class MatchExpr;
class Or;
class Pipe;
class Range;
class ReadRedirection;
class ReadWriteRedirection;
class Sequence;
class Subshell;
class Slice;
class SimpleVariable;
class SpecialVariable;
class Juxtaposition;
class StringLiteral;
class StringPartCompose;
class SyntaxError;
class SyntheticNode;
class Tilde;
class VariableDeclarations;
class WriteAppendRedirection;
class WriteRedirection;

}

namespace Shell {

class Shell;

}
