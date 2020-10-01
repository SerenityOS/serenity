/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "Forward.h"
#include "Job.h"
#include "NodeVisitor.h"
#include <AK/InlineLinkedList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibLine/Editor.h>

namespace Shell::AST {

struct HighlightMetadata {
    bool is_first_in_list { true };
};

struct Position {
    size_t start_offset { 0 };
    size_t end_offset { 0 };
    struct Line {
        size_t line_number { 0 };
        size_t line_column { 0 };

        bool operator==(const Line& other) const
        {
            return line_number == other.line_number && line_column == other.line_column;
        }
    } start_line, end_line;

    bool contains(size_t offset) const { return start_offset <= offset && offset <= end_offset; }
};

struct FdRedirection;
struct Rewiring : public RefCounted<Rewiring> {
    int source_fd { -1 };
    int dest_fd { -1 };
    FdRedirection* other_pipe_end { nullptr };
    enum class Close {
        None,
        Source,
        Destination,
        RefreshDestination,
        ImmediatelyCloseDestination,
    } fd_action { Close::None };

    Rewiring(int source, int dest, Close close = Close::None)
        : source_fd(source)
        , dest_fd(dest)
        , fd_action(close)
    {
    }

    Rewiring(int source, int dest, FdRedirection* other_end, Close close)
        : source_fd(source)
        , dest_fd(dest)
        , other_pipe_end(other_end)
        , fd_action(close)
    {
    }
};

struct Redirection : public RefCounted<Redirection> {
    virtual Result<NonnullRefPtr<Rewiring>, String> apply() const = 0;
    virtual ~Redirection();
    virtual bool is_path_redirection() const { return false; }
    virtual bool is_fd_redirection() const { return false; }
    virtual bool is_close_redirection() const { return false; }
};

struct CloseRedirection : public Redirection {
    int fd { -1 };

    virtual Result<NonnullRefPtr<Rewiring>, String> apply() const override;
    virtual ~CloseRedirection();
    CloseRedirection(int fd)
        : fd(fd)
    {
    }

private:
    virtual bool is_close_redirection() const override { return true; }
};

struct PathRedirection : public Redirection {
    String path;
    int fd { -1 };
    enum {
        Read,
        Write,
        WriteAppend,
        ReadWrite,
    } direction { Read };

    static NonnullRefPtr<PathRedirection> create(String path, int fd, decltype(direction) direction)
    {
        return adopt(*new PathRedirection(move(path), fd, direction));
    }

    virtual Result<NonnullRefPtr<Rewiring>, String> apply() const override;
    virtual ~PathRedirection();

private:
    PathRedirection(String path, int fd, decltype(direction) direction)
        : path(move(path))
        , fd(fd)
        , direction(direction)
    {
    }

    virtual bool is_path_redirection() const override { return true; }
};

struct FdRedirection : public Redirection {
public:
    static NonnullRefPtr<FdRedirection> create(int source, int dest, Rewiring::Close close)
    {
        return adopt(*new FdRedirection(source, dest, close));
    }

    static NonnullRefPtr<FdRedirection> create(int source, int dest, FdRedirection* pipe_end, Rewiring::Close close)
    {
        return adopt(*new FdRedirection(source, dest, pipe_end, close));
    }

    virtual ~FdRedirection();

    virtual Result<NonnullRefPtr<Rewiring>, String> apply() const override
    {
        return adopt(*new Rewiring(source_fd, dest_fd, other_pipe_end, action));
    }

    int source_fd { -1 };
    int dest_fd { -1 };
    FdRedirection* other_pipe_end { nullptr };
    Rewiring::Close action { Rewiring::Close::None };

private:
    FdRedirection(int source, int dest, Rewiring::Close close)
        : FdRedirection(source, dest, nullptr, close)
    {
    }

    FdRedirection(int source, int dest, FdRedirection* pipe_end, Rewiring::Close close)
        : source_fd(source)
        , dest_fd(dest)
        , other_pipe_end(pipe_end)
        , action(close)
    {
    }

    virtual bool is_fd_redirection() const override { return true; }
};

class Pipeline : public RefCounted<Pipeline> {
public:
    pid_t pgid { -1 };
};

struct NodeWithAction {
    mutable NonnullRefPtr<Node> node;
    enum Action {
        And,
        Or,
        Sequence,
    } action;

    NodeWithAction(Node& node, Action action)
        : node(node)
        , action(action)
    {
    }
};

struct Command {
    Vector<String> argv;
    NonnullRefPtrVector<Redirection> redirections;
    bool should_wait { true };
    bool is_pipe_source { false };
    bool should_notify_if_in_background { true };
    bool should_immediately_execute_next { false };

    mutable RefPtr<Pipeline> pipeline;
    Vector<NodeWithAction> next_chain;
};

struct HitTestResult {
    RefPtr<Node> matching_node;
    RefPtr<Node> closest_node_with_semantic_meaning; // This is used if matching_node is a bareword
    RefPtr<Node> closest_command_node;               // This is used if matching_node is a bareword, and it is not the first in a list
};

class Value : public RefCounted<Value> {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) = 0;
    virtual Vector<Command> resolve_as_commands(RefPtr<Shell>);
    virtual NonnullRefPtr<Value> resolve_without_cast(RefPtr<Shell>) { return *this; }
    virtual ~Value();
    virtual bool is_command() const { return false; }
    virtual bool is_glob() const { return false; }
    virtual bool is_job() const { return false; }
    virtual bool is_list() const { return false; }
    virtual bool is_string() const { return false; }
    virtual bool is_list_without_resolution() const { return false; }
};

class CommandValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual Vector<Command> resolve_as_commands(RefPtr<Shell>) override;
    virtual ~CommandValue();
    virtual bool is_command() const override { return true; }
    CommandValue(Command command)
        : m_command(move(command))
    {
    }

    CommandValue(Vector<String> argv)
        : m_command({ move(argv), {}, true, false, true, false, nullptr, {} })
    {
    }

private:
    Command m_command;
};

class CommandSequenceValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual Vector<Command> resolve_as_commands(RefPtr<Shell>) override;
    virtual ~CommandSequenceValue();
    virtual bool is_command() const override { return true; }
    CommandSequenceValue(Vector<Command> commands)
        : m_contained_values(move(commands))
    {
    }

private:
    Vector<Command> m_contained_values;
};

class JobValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override { ASSERT_NOT_REACHED(); }
    virtual Vector<Command> resolve_as_commands(RefPtr<Shell>) override { ASSERT_NOT_REACHED(); }
    virtual ~JobValue();
    virtual bool is_job() const override { return true; }
    JobValue(RefPtr<Job> job)
        : m_job(move(job))
    {
    }

    const RefPtr<Job> job() const { return m_job; }

private:
    RefPtr<Job> m_job;
};

class ListValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual NonnullRefPtr<Value> resolve_without_cast(RefPtr<Shell>) override;
    virtual ~ListValue();
    virtual bool is_list() const override { return true; }
    virtual bool is_list_without_resolution() const override { return true; }
    ListValue(Vector<String> values);
    ListValue(Vector<NonnullRefPtr<Value>> values)
        : m_contained_values(move(static_cast<NonnullRefPtrVector<Value>&>(values)))
    {
    }

    const NonnullRefPtrVector<Value>& values() const { return m_contained_values; }
    NonnullRefPtrVector<Value>& values() { return m_contained_values; }

private:
    NonnullRefPtrVector<Value> m_contained_values;
};

class StringValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual ~StringValue();
    virtual bool is_string() const override { return m_split.is_null(); }
    virtual bool is_list() const override { return !m_split.is_null(); }
    StringValue(String string, String split_by = {}, bool keep_empty = false)
        : m_string(move(string))
        , m_split(move(split_by))
        , m_keep_empty(keep_empty)
    {
    }

private:
    String m_string;
    String m_split;
    bool m_keep_empty { false };
};

class GlobValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual ~GlobValue();
    virtual bool is_glob() const override { return true; }
    GlobValue(String glob)
        : m_glob(move(glob))
    {
    }

private:
    String m_glob;
};

class SimpleVariableValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual NonnullRefPtr<Value> resolve_without_cast(RefPtr<Shell>) override;
    virtual ~SimpleVariableValue();
    SimpleVariableValue(String name)
        : m_name(move(name))
    {
    }

private:
    String m_name;
};

class SpecialVariableValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual ~SpecialVariableValue();
    SpecialVariableValue(char name)
        : m_name(name)
    {
    }

private:
    char m_name { -1 };
};

class TildeValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(RefPtr<Shell>) override;
    virtual ~TildeValue();
    virtual bool is_string() const override { return true; }
    TildeValue(String name)
        : m_username(move(name))
    {
    }

private:
    String m_username;
};

class Node : public RefCounted<Node> {
public:
    virtual void dump(int level) const = 0;
    virtual void for_each_entry(RefPtr<Shell> shell, Function<IterationDecision(NonnullRefPtr<Value>)> callback);
    virtual RefPtr<Value> run(RefPtr<Shell>) = 0;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) = 0;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&);
    Vector<Line::CompletionSuggestion> complete_for_editor(Shell& shell, size_t offset);
    virtual HitTestResult hit_test_position(size_t offset)
    {
        if (m_position.contains(offset))
            return { this, nullptr, nullptr };
        return { nullptr, nullptr, nullptr };
    }
    virtual String class_name() const { return "Node"; }
    Node(Position);
    virtual ~Node();

    virtual bool is_bareword() const { return false; }
    virtual bool is_command() const { return false; }
    virtual bool is_execute() const { return false; }
    virtual bool is_glob() const { return false; }
    virtual bool is_tilde() const { return false; }
    virtual bool is_variable_decls() const { return false; }
    virtual bool is_simple_variable() const { return false; }
    virtual bool is_syntax_error() const { return m_is_syntax_error; }

    virtual bool is_list() const { return false; }
    virtual bool would_execute() const { return false; }

    const Position& position() const { return m_position; }
    void set_is_syntax_error(const SyntaxError& error_node)
    {
        m_is_syntax_error = true;
        m_syntax_error_node = error_node;
    }
    virtual const SyntaxError& syntax_error_node() const
    {
        ASSERT(is_syntax_error());
        return *m_syntax_error_node;
    }

    virtual RefPtr<Node> leftmost_trivial_literal() const { return nullptr; }

    Vector<Command> to_lazy_evaluated_commands(RefPtr<Shell> shell);

    virtual void visit(NodeVisitor&) { ASSERT_NOT_REACHED(); }
    virtual void visit(NodeVisitor& visitor) const { const_cast<Node*>(this)->visit(visitor); }

    enum class Kind : u32 {
        And,
        ListConcatenate,
        Background,
        BarewordLiteral,
        CastToCommand,
        CastToList,
        CloseFdRedirection,
        CommandLiteral,
        Comment,
        DynamicEvaluate,
        DoubleQuotedString,
        Fd2FdRedirection,
        FunctionDeclaration,
        ForLoop,
        Glob,
        Execute,
        IfCond,
        Join,
        MatchExpr,
        Or,
        Pipe,
        ReadRedirection,
        ReadWriteRedirection,
        Sequence,
        Subshell,
        SimpleVariable,
        SpecialVariable,
        Juxtaposition,
        StringLiteral,
        StringPartCompose,
        SyntaxError,
        Tilde,
        VariableDeclarations,
        WriteAppendRedirection,
        WriteRedirection,
        __Count,
    };

    virtual Kind kind() const = 0;

protected:
    Position m_position;
    bool m_is_syntax_error { false };
    RefPtr<const SyntaxError> m_syntax_error_node;
};

#define NODE(name)                                               \
    virtual String class_name() const override { return #name; } \
    virtual Kind kind() const override { return Kind::name; }

class PathRedirectionNode : public Node {
public:
    PathRedirectionNode(Position, int, NonnullRefPtr<Node>);
    virtual ~PathRedirectionNode();
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual HitTestResult hit_test_position(size_t offset) override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& path() const { return m_path; }
    int fd() const { return m_fd; }

protected:
    int m_fd { -1 };
    NonnullRefPtr<Node> m_path;
};

class And final : public Node {
public:
    And(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>, Position and_position);
    virtual ~And();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }
    const Position& and_position() const { return m_and_position; }

private:
    NODE(And);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
    Position m_and_position;
};

class ListConcatenate final : public Node {
public:
    ListConcatenate(Position, Vector<NonnullRefPtr<Node>>);
    virtual ~ListConcatenate();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }
    const Vector<NonnullRefPtr<Node>> list() const { return m_list; }

private:
    NODE(ListConcatenate);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node> leftmost_trivial_literal() const override;

    Vector<NonnullRefPtr<Node>> m_list;
};

class Background final : public Node {
public:
    Background(Position, NonnullRefPtr<Node>);
    virtual ~Background();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& command() const { return m_command; }

private:
    NODE(Background);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;

    NonnullRefPtr<Node> m_command;
};

class BarewordLiteral final : public Node {
public:
    BarewordLiteral(Position, String);
    virtual ~BarewordLiteral();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const String& text() const { return m_text; }

private:
    NODE(BarewordLiteral);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_bareword() const override { return true; }
    virtual RefPtr<Node> leftmost_trivial_literal() const override { return this; }

    String m_text;
};

class CastToCommand final : public Node {
public:
    CastToCommand(Position, NonnullRefPtr<Node>);
    virtual ~CastToCommand();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& inner() const { return m_inner; }

private:
    NODE(CastToCommand);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node> leftmost_trivial_literal() const override;

    NonnullRefPtr<Node> m_inner;
};

class CastToList final : public Node {
public:
    CastToList(Position, RefPtr<Node>);
    virtual ~CastToList();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const RefPtr<Node>& inner() const { return m_inner; }

private:
    NODE(CastToList);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node> leftmost_trivial_literal() const override;

    RefPtr<Node> m_inner;
};

class CloseFdRedirection final : public Node {
public:
    CloseFdRedirection(Position, int);
    virtual ~CloseFdRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    int fd() const { return m_fd; }

private:
    NODE(CloseFdRedirection);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_command() const override { return true; }

    int m_fd { -1 };
};

class CommandLiteral final : public Node {
public:
    CommandLiteral(Position, Command);
    virtual ~CommandLiteral();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const Command& command() const { return m_command; }

private:
    NODE(CommandLiteral);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override { ASSERT_NOT_REACHED(); }
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }

    Command m_command;
};

class Comment : public Node {
public:
    Comment(Position, String);
    virtual ~Comment();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const String& text() const { return m_text; }

private:
    NODE(Comment);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;

    String m_text;
};

class DynamicEvaluate final : public Node {
public:
    DynamicEvaluate(Position, NonnullRefPtr<Node>);
    virtual ~DynamicEvaluate();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& inner() const { return m_inner; }

private:
    NODE(DynamicEvaluate);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;

    virtual bool is_bareword() const override { return m_inner->is_bareword(); }
    virtual bool is_command() const override { return is_list(); }
    virtual bool is_execute() const override { return true; }
    virtual bool is_glob() const override { return m_inner->is_glob(); }
    virtual bool is_list() const override
    {
        return m_inner->is_list() || m_inner->is_command() || m_inner->is_glob(); // Anything that generates a list.
    }

    NonnullRefPtr<Node> m_inner;
};

class DoubleQuotedString final : public Node {
public:
    DoubleQuotedString(Position, RefPtr<Node>);
    virtual ~DoubleQuotedString();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const RefPtr<Node>& inner() const { return m_inner; }

private:
    NODE(DoubleQuotedString);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;

    RefPtr<Node> m_inner;
};

class Fd2FdRedirection final : public Node {
public:
    Fd2FdRedirection(Position, int, int);
    virtual ~Fd2FdRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    int source_fd() const { return m_source_fd; }
    int dest_fd() const { return m_dest_fd; }

private:
    NODE(Fd2FdRedirection);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_command() const override { return true; }

    int m_source_fd { -1 };
    int m_dest_fd { -1 };
};

class FunctionDeclaration final : public Node {
public:
    struct NameWithPosition {
        String name;
        Position position;
    };
    FunctionDeclaration(Position, NameWithPosition name, Vector<NameWithPosition> argument_names, RefPtr<AST::Node> body);
    virtual ~FunctionDeclaration();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NameWithPosition& name() const { return m_name; }
    const Vector<NameWithPosition> arguments() const { return m_arguments; }
    const RefPtr<Node>& block() const { return m_block; }

private:
    NODE(FunctionDeclaration);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual bool would_execute() const override { return true; }

    NameWithPosition m_name;
    Vector<NameWithPosition> m_arguments;
    RefPtr<AST::Node> m_block;
};

class ForLoop final : public Node {
public:
    ForLoop(Position, String variable_name, NonnullRefPtr<AST::Node> iterated_expr, RefPtr<AST::Node> block, Optional<Position> in_kw_position = {});
    virtual ~ForLoop();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const String& variable_name() const { return m_variable_name; }
    const NonnullRefPtr<Node>& iterated_expression() const { return m_iterated_expression; }
    const RefPtr<Node>& block() const { return m_block; }
    const Optional<Position> in_keyword_position() const { return m_in_kw_position; }

private:
    NODE(ForLoop);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool would_execute() const override { return true; }

    String m_variable_name;
    NonnullRefPtr<AST::Node> m_iterated_expression;
    RefPtr<AST::Node> m_block;
    Optional<Position> m_in_kw_position;
};

class Glob final : public Node {
public:
    Glob(Position, String);
    virtual ~Glob();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const String& text() const { return m_text; }

private:
    NODE(Glob);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_glob() const override { return true; }
    virtual bool is_list() const override { return true; }

    String m_text;
};

class Execute final : public Node {
public:
    Execute(Position, NonnullRefPtr<Node>, bool capture_stdout = false);
    virtual ~Execute();
    void capture_stdout() { m_capture_stdout = true; }
    NonnullRefPtr<Node>& command() { return m_command; }
    virtual void for_each_entry(RefPtr<Shell> shell, Function<IterationDecision(NonnullRefPtr<Value>)> callback) override;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& command() const { return m_command; }
    bool does_capture_stdout() const { return m_capture_stdout; }

private:
    NODE(Execute);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual bool is_execute() const override { return true; }
    virtual bool would_execute() const override { return true; }

    NonnullRefPtr<Node> m_command;
    bool m_capture_stdout { false };
};

class IfCond final : public Node {
public:
    IfCond(Position, Optional<Position> else_position, NonnullRefPtr<AST::Node> cond_expr, RefPtr<AST::Node> true_branch, RefPtr<AST::Node> false_branch);
    virtual ~IfCond();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& condition() const { return m_condition; }
    const RefPtr<Node>& true_branch() const { return m_true_branch; }
    const RefPtr<Node>& false_branch() const { return m_false_branch; }
    const Optional<Position> else_position() const { return m_else_position; }

private:
    NODE(IfCond);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool would_execute() const override { return true; }

    NonnullRefPtr<AST::Node> m_condition;
    RefPtr<AST::Node> m_true_branch;
    RefPtr<AST::Node> m_false_branch;

    Optional<Position> m_else_position;
};

class Join final : public Node {
public:
    Join(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~Join();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }

private:
    NODE(Join);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node> leftmost_trivial_literal() const override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

struct MatchEntry {
    NonnullRefPtrVector<Node> options;
    Vector<Position> pipe_positions;
    RefPtr<Node> body;
};

class MatchExpr final : public Node {
public:
    MatchExpr(Position, NonnullRefPtr<Node> expr, String name, Optional<Position> as_position, Vector<MatchEntry> entries);
    virtual ~MatchExpr();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& matched_expr() const { return m_matched_expr; }
    const String& expr_name() const { return m_expr_name; }
    const Vector<MatchEntry>& entries() const { return m_entries; }
    const Optional<Position>& as_position() const { return m_as_position; }

private:
    NODE(MatchExpr);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool would_execute() const override { return true; }

    NonnullRefPtr<Node> m_matched_expr;
    String m_expr_name;
    Optional<Position> m_as_position;
    Vector<MatchEntry> m_entries;
};

class Or final : public Node {
public:
    Or(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>, Position or_position);
    virtual ~Or();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }
    const Position& or_position() const { return m_or_position; }

private:
    NODE(Or);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
    Position m_or_position;
};

class Pipe final : public Node {
public:
    Pipe(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~Pipe();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }

private:
    NODE(Pipe);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_command() const override { return true; }

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

class ReadRedirection final : public PathRedirectionNode {
public:
    ReadRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~ReadRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(ReadRedirection);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
};

class ReadWriteRedirection final : public PathRedirectionNode {
public:
    ReadWriteRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~ReadWriteRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(ReadWriteRedirection);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
};

class Sequence final : public Node {
public:
    Sequence(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>, Position separator_position);
    virtual ~Sequence();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }

    const Position& separator_position() const { return m_separator_position; }

private:
    NODE(Sequence);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_list() const override { return true; }
    virtual bool would_execute() const override { return m_left->would_execute() || m_right->would_execute(); }

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
    Position m_separator_position;
};

class Subshell final : public Node {
public:
    Subshell(Position, RefPtr<Node> block);
    virtual ~Subshell();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const RefPtr<Node>& block() const { return m_block; }

private:
    NODE(Subshell);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool would_execute() const override { return true; }

    RefPtr<AST::Node> m_block;
};

class SimpleVariable final : public Node {
public:
    SimpleVariable(Position, String);
    virtual ~SimpleVariable();

    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }
    const String& name() const { return m_name; }

private:
    NODE(SimpleVariable);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_simple_variable() const override { return true; }

    String m_name;
};

class SpecialVariable final : public Node {
public:
    SpecialVariable(Position, char);
    virtual ~SpecialVariable();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    char name() const { return m_name; }

private:
    NODE(SpecialVariable);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual HitTestResult hit_test_position(size_t) override;

    char m_name { -1 };
};

class Juxtaposition final : public Node {
public:
    Juxtaposition(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~Juxtaposition();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }

private:
    NODE(Juxtaposition);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

class StringLiteral final : public Node {
public:
    StringLiteral(Position, String);
    virtual ~StringLiteral();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const String& text() const { return m_text; }

private:
    NODE(StringLiteral);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual RefPtr<Node> leftmost_trivial_literal() const override { return this; };

    String m_text;
};

class StringPartCompose final : public Node {
public:
    StringPartCompose(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~StringPartCompose();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const NonnullRefPtr<Node>& left() const { return m_left; }
    const NonnullRefPtr<Node>& right() const { return m_right; }

private:
    NODE(StringPartCompose);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

class SyntaxError final : public Node {
public:
    SyntaxError(Position, String);
    virtual ~SyntaxError();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const String& error_text() const { return m_syntax_error_text; }

private:
    NODE(SyntaxError);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override { return { nullptr, nullptr, nullptr }; }
    virtual bool is_syntax_error() const override { return true; }
    virtual const SyntaxError& syntax_error_node() const override;

    String m_syntax_error_text;
};

class Tilde final : public Node {
public:
    Tilde(Position, String);
    virtual ~Tilde();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String text() const;

private:
    NODE(Tilde);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, const HitTestResult&) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_tilde() const override { return true; }

    String m_username;
};

class VariableDeclarations final : public Node {
public:
    struct Variable {
        NonnullRefPtr<Node> name;
        NonnullRefPtr<Node> value;
    };
    VariableDeclarations(Position, Vector<Variable> variables);
    virtual ~VariableDeclarations();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    const Vector<Variable>& variables() const { return m_variables; }

private:
    NODE(VariableDeclarations);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual bool is_variable_decls() const override { return true; }

    Vector<Variable> m_variables;
};

class WriteAppendRedirection final : public PathRedirectionNode {
public:
    WriteAppendRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~WriteAppendRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(WriteAppendRedirection);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
};

class WriteRedirection final : public PathRedirectionNode {
public:
    WriteRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~WriteRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(WriteRedirection);
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(RefPtr<Shell>) override;
};

}
