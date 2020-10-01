/*
 * Copyright (c) 2020, The SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
class CastToCommand;
class CastToList;
class CloseFdRedirection;
class CommandLiteral;
class Comment;
class DynamicEvaluate;
class DoubleQuotedString;
class Fd2FdRedirection;
class FunctionDeclaration;
class ForLoop;
class Glob;
class Execute;
class IfCond;
class Join;
class MatchExpr;
class Or;
class Pipe;
class ReadRedirection;
class ReadWriteRedirection;
class Sequence;
class Subshell;
class SimpleVariable;
class SpecialVariable;
class Juxtaposition;
class StringLiteral;
class StringPartCompose;
class SyntaxError;
class Tilde;
class VariableDeclarations;
class WriteAppendRedirection;
class WriteRedirection;

}

namespace Shell {

class Shell;

}
