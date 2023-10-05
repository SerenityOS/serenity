/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Forward.h"
#include "Job.h"
#include "NodeVisitor.h"
#include <AK/Format.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibLine/Editor.h>
#include <LibRegex/Regex.h>

namespace Shell::AST {

using AK::make_ref_counted;

template<typename T>
static inline NonnullRefPtr<T> make_ref_counted(std::initializer_list<NonnullRefPtr<Value>> arg)
{
    return adopt_ref(*new T(arg));
}

struct HighlightMetadata {
    bool is_first_in_list { true };
};

struct Position {
    size_t start_offset { 0 };
    size_t end_offset { 0 };
    struct Line {
        size_t line_number { 0 };
        size_t line_column { 0 };

        bool operator==(Line const& other) const
        {
            return line_number == other.line_number && line_column == other.line_column;
        }
    } start_line, end_line;

    bool contains(size_t offset) const { return start_offset <= offset && offset <= end_offset; }

    Position with_end(Position const& end) const
    {
        return {
            .start_offset = start_offset,
            .end_offset = end.end_offset,
            .start_line = start_line,
            .end_line = end.end_line,
        };
    }
};

struct NameWithPosition {
    String name;
    Position position;
};

struct FdRedirection;
struct Rewiring : public RefCounted<Rewiring> {
    int old_fd { -1 };
    int new_fd { -1 };
    FdRedirection* other_pipe_end { nullptr };
    enum class Close {
        None,
        Old,
        New,
        RefreshNew,
        RefreshOld,
        ImmediatelyCloseNew,
    } fd_action { Close::None };

    Rewiring(int source, int dest, Close close = Close::None)
        : old_fd(source)
        , new_fd(dest)
        , fd_action(close)
    {
    }

    Rewiring(int source, int dest, FdRedirection* other_end, Close close)
        : old_fd(source)
        , new_fd(dest)
        , other_pipe_end(other_end)
        , fd_action(close)
    {
    }
};

struct Redirection : public RefCounted<Redirection> {
    virtual ErrorOr<NonnullRefPtr<Rewiring>> apply() const = 0;
    virtual ~Redirection();
    virtual bool is_path_redirection() const { return false; }
    virtual bool is_fd_redirection() const { return false; }
    virtual bool is_close_redirection() const { return false; }
};

struct CloseRedirection : public Redirection {
    int fd { -1 };

    virtual ErrorOr<NonnullRefPtr<Rewiring>> apply() const override;
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
        return adopt_ref(*new PathRedirection(move(path), fd, direction));
    }

    virtual ErrorOr<NonnullRefPtr<Rewiring>> apply() const override;
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
    static NonnullRefPtr<FdRedirection> create(int old_fd, int new_fd, Rewiring::Close close)
    {
        return adopt_ref(*new FdRedirection(old_fd, new_fd, close));
    }

    static NonnullRefPtr<FdRedirection> create(int old_fd, int new_fd, FdRedirection* pipe_end, Rewiring::Close close)
    {
        return adopt_ref(*new FdRedirection(old_fd, new_fd, pipe_end, close));
    }

    virtual ~FdRedirection();

    virtual ErrorOr<NonnullRefPtr<Rewiring>> apply() const override
    {
        return adopt_ref(*new Rewiring(old_fd, new_fd, other_pipe_end, action));
    }

    int old_fd { -1 };
    int new_fd { -1 };
    FdRedirection* other_pipe_end { nullptr };
    Rewiring::Close action { Rewiring::Close::None };

private:
    FdRedirection(int source, int dest, Rewiring::Close close)
        : FdRedirection(source, dest, nullptr, close)
    {
    }

    FdRedirection(int old_fd, int new_fd, FdRedirection* pipe_end, Rewiring::Close close)
        : old_fd(old_fd)
        , new_fd(new_fd)
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
    Vector<NonnullRefPtr<Redirection>> redirections;
    bool should_wait { true };
    bool is_pipe_source { false };
    bool should_notify_if_in_background { true };
    bool should_immediately_execute_next { false };

    mutable RefPtr<Pipeline> pipeline;
    Vector<NodeWithAction> next_chain;
    Optional<Position> position;
};

struct HitTestResult {
    RefPtr<Node const> matching_node;
    RefPtr<Node const> closest_node_with_semantic_meaning; // This is used if matching_node is a bareword
    RefPtr<Node const> closest_command_node;               // This is used if matching_node is a bareword, and it is not the first in a list
};

class Value : public RefCounted<Value> {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) = 0;
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell> shell);
    virtual ErrorOr<Vector<Command>> resolve_as_commands(RefPtr<Shell>);
    virtual ErrorOr<NonnullRefPtr<Value>> resolve_without_cast(RefPtr<Shell>) { return *this; }
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const = 0;
    virtual ErrorOr<NonnullRefPtr<Value>> with_slices(NonnullRefPtr<Slice> slice) const&;
    virtual ErrorOr<NonnullRefPtr<Value>> with_slices(Vector<NonnullRefPtr<Slice>> slices) const&;
    virtual ~Value();
    virtual bool is_command() const { return false; }
    virtual bool is_glob() const { return false; }
    virtual bool is_job() const { return false; }
    virtual bool is_list() const { return false; }
    virtual bool is_string() const { return false; }
    virtual bool is_list_without_resolution() const { return false; }

protected:
    Value& set_slices(Vector<NonnullRefPtr<Slice>> slices)
    {
        m_slices = move(slices);
        return *this;
    }
    Vector<NonnullRefPtr<Slice>> m_slices;
};

class CommandValue final : public Value {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<Vector<Command>> resolve_as_commands(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<CommandValue>(m_command))->set_slices(m_slices); }
    virtual ~CommandValue();
    virtual bool is_command() const override { return true; }
    CommandValue(Command command)
        : m_command(move(command))
    {
    }

    CommandValue(Vector<String> argv, Position position)
        : m_command({ move(argv), {}, true, false, true, false, nullptr, {}, move(position) })
    {
    }

private:
    Command m_command;
};

class CommandSequenceValue final : public Value {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<Vector<Command>> resolve_as_commands(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<CommandSequenceValue>(m_contained_values))->set_slices(m_slices); }
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
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override { VERIFY_NOT_REACHED(); }
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell>) override { return String::formatted("%{}", m_job->job_id()); }
    virtual ErrorOr<Vector<Command>> resolve_as_commands(RefPtr<Shell>) override { VERIFY_NOT_REACHED(); }
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<JobValue>(m_job))->set_slices(m_slices); }
    virtual ~JobValue();
    virtual bool is_job() const override { return true; }
    JobValue(RefPtr<Job> job)
        : m_job(move(job))
    {
    }

    RefPtr<Job> const job() const { return m_job; }

private:
    RefPtr<Job> m_job;
};

class ListValue final : public Value {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> resolve_without_cast(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<ListValue>(m_contained_values))->set_slices(m_slices); }
    virtual ~ListValue();
    virtual bool is_list() const override { return true; }
    virtual bool is_list_without_resolution() const override { return true; }
    ListValue(Vector<String> values);
    ListValue(Vector<NonnullRefPtr<Value>> values)
        : m_contained_values(move(values))
    {
    }

    Vector<NonnullRefPtr<Value>> const& values() const { return m_contained_values; }
    Vector<NonnullRefPtr<Value>>& values() { return m_contained_values; }

private:
    Vector<NonnullRefPtr<Value>> m_contained_values;
};

class StringValue final : public Value {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell> shell) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<StringValue>(m_string, m_split, m_keep_empty))->set_slices(m_slices); }
    virtual ~StringValue();
    virtual bool is_string() const override { return m_split.is_empty(); }
    virtual bool is_list() const override { return !m_split.is_empty(); }
    ErrorOr<NonnullRefPtr<Value>> resolve_without_cast(RefPtr<Shell>) override;
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
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<GlobValue>(m_glob, m_generation_position))->set_slices(m_slices); }
    virtual ~GlobValue();
    virtual bool is_glob() const override { return true; }
    GlobValue(String glob, Position position)
        : m_glob(move(glob))
        , m_generation_position(move(position))
    {
    }

private:
    String m_glob;
    Position m_generation_position;
};

class SimpleVariableValue final : public Value {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> resolve_without_cast(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<SimpleVariableValue>(m_name))->set_slices(m_slices); }
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
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> resolve_without_cast(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<SpecialVariableValue>(m_name))->set_slices(m_slices); }
    virtual ~SpecialVariableValue();
    SpecialVariableValue(char name)
        : m_name(name)
    {
    }

private:
    char m_name { 0 };
};

class TildeValue final : public Value {
public:
    virtual ErrorOr<Vector<String>> resolve_as_list(RefPtr<Shell>) override;
    virtual ErrorOr<String> resolve_as_string(RefPtr<Shell>) override;
    virtual ErrorOr<NonnullRefPtr<Value>> clone() const override { return TRY(try_make_ref_counted<TildeValue>(m_username))->set_slices(m_slices); }
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
    AK_MAKE_NONCOPYABLE(Node);
    AK_MAKE_NONMOVABLE(Node);

public:
    virtual ErrorOr<void> dump(int level) const = 0;
    virtual ErrorOr<void> for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback);
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) = 0;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) = 0;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const;
    ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell& shell, size_t offset);
    virtual HitTestResult hit_test_position(size_t offset) const
    {
        if (m_position.contains(offset))
            return { this, nullptr, nullptr };
        return { nullptr, nullptr, nullptr };
    }
    virtual StringView class_name() const { return "Node"sv; }
    Node(Position);
    virtual ~Node() = default;

    virtual bool is_bareword() const { return false; }
    virtual bool is_command() const { return false; }
    virtual bool is_execute() const { return false; }
    virtual bool is_glob() const { return false; }
    virtual bool is_tilde() const { return false; }
    virtual bool is_variable_decls() const { return false; }
    virtual bool is_simple_variable() const { return false; }
    virtual bool is_syntax_error() const;

    virtual bool is_list() const { return false; }
    virtual bool would_execute() const { return false; }
    virtual bool should_override_execution_in_current_process() const { return false; }

    Position const& position() const { return m_position; }
    Position& position() { return m_position; }
    virtual void clear_syntax_error();
    virtual void set_is_syntax_error(SyntaxError& error_node);
    virtual SyntaxError& syntax_error_node()
    {
        VERIFY(is_syntax_error());
        return *m_syntax_error_node;
    }

    virtual RefPtr<Node const> leftmost_trivial_literal() const { return nullptr; }

    ErrorOr<Vector<Command>> to_lazy_evaluated_commands(RefPtr<Shell> shell);

    virtual void visit(NodeVisitor&) { VERIFY_NOT_REACHED(); }
    virtual void visit(NodeVisitor& visitor) const { const_cast<Node*>(this)->visit(visitor); }

    enum class Kind : u32 {
        And,
        Background,
        BarewordLiteral,
        BraceExpansion,
        CastToCommand,
        CastToList,
        CloseFdRedirection,
        CommandLiteral,
        Comment,
        ContinuationControl,
        DoubleQuotedString,
        DynamicEvaluate,
        Execute,
        Fd2FdRedirection,
        ForLoop,
        FunctionDeclaration,
        Glob,
        Heredoc,
        HistoryEvent,
        IfCond,
        ImmediateExpression,
        Join,
        Juxtaposition,
        ListConcatenate,
        MatchExpr,
        Or,
        Pipe,
        Range,
        ReadRedirection,
        ReadWriteRedirection,
        Sequence,
        Slice,
        SimpleVariable,
        SpecialVariable,
        StringLiteral,
        StringPartCompose,
        Subshell,
        SyntaxError,
        SyntheticValue,
        Tilde,
        VariableDeclarations,
        WriteAppendRedirection,
        WriteRedirection,
        __Count,
    };

    virtual Kind kind() const = 0;

protected:
    Position m_position;
    RefPtr<SyntaxError> m_syntax_error_node;
};

#define NODE(name)                                 \
    virtual StringView class_name() const override \
    {                                              \
        return #name##sv;                          \
    }                                              \
    virtual Kind kind() const override             \
    {                                              \
        return Kind::name;                         \
    }

class PathRedirectionNode : public Node {
public:
    PathRedirectionNode(Position, int, NonnullRefPtr<Node>);
    virtual ~PathRedirectionNode();
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual HitTestResult hit_test_position(size_t offset) const override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& path() const { return m_path; }
    int fd() const { return m_fd; }

protected:
    int m_fd { -1 };
    NonnullRefPtr<Node> m_path;
};

class And final : public Node {
public:
    And(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>, Position and_position);
    virtual ~And() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& left() const { return m_left; }
    NonnullRefPtr<Node> const& right() const { return m_right; }
    Position const& and_position() const { return m_and_position; }

private:
    NODE(And);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
    Position m_and_position;
};

class ListConcatenate final : public Node {
public:
    ListConcatenate(Position, Vector<NonnullRefPtr<Node>>);
    virtual ~ListConcatenate() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }
    Vector<NonnullRefPtr<Node>> const list() const { return m_list; }

private:
    NODE(ListConcatenate);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<void> for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback) override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node const> leftmost_trivial_literal() const override;

    Vector<NonnullRefPtr<Node>> m_list;
};

class Background final : public Node {
public:
    Background(Position, NonnullRefPtr<Node>);
    virtual ~Background() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& command() const { return m_command; }

private:
    NODE(Background);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    NonnullRefPtr<Node> m_command;
};

class BarewordLiteral final : public Node {
public:
    BarewordLiteral(Position, String);
    virtual ~BarewordLiteral() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String const& text() const { return m_text; }

private:
    NODE(BarewordLiteral);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_bareword() const override { return true; }
    virtual RefPtr<Node const> leftmost_trivial_literal() const override { return this; }

    String m_text;
};

class BraceExpansion final : public Node {
public:
    BraceExpansion(Position, Vector<NonnullRefPtr<Node>>);
    virtual ~BraceExpansion() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    Vector<NonnullRefPtr<Node>> const& entries() const { return m_entries; }

private:
    NODE(BraceExpansion);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    Vector<NonnullRefPtr<Node>> m_entries;
};

class CastToCommand final : public Node {
public:
    CastToCommand(Position, NonnullRefPtr<Node>);
    virtual ~CastToCommand() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& inner() const { return m_inner; }

private:
    NODE(CastToCommand);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node const> leftmost_trivial_literal() const override;

    NonnullRefPtr<Node> m_inner;
};

class CastToList final : public Node {
public:
    CastToList(Position, RefPtr<Node>);
    virtual ~CastToList() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    RefPtr<Node> const& inner() const { return m_inner; }

private:
    NODE(CastToList);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node const> leftmost_trivial_literal() const override;

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
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_command() const override { return true; }

    int m_fd { -1 };
};

class CommandLiteral final : public Node {
public:
    CommandLiteral(Position, Command);
    virtual ~CommandLiteral();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    Command const& command() const { return m_command; }

private:
    NODE(CommandLiteral);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override { VERIFY_NOT_REACHED(); }
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }

    Command m_command;
};

class Comment : public Node {
public:
    Comment(Position, String);
    virtual ~Comment();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String const& text() const { return m_text; }

private:
    NODE(Comment);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;

    String m_text;
};

class ContinuationControl final : public Node {
public:
    enum ContinuationKind {
        Break,
        Continue,
    };
    ContinuationControl(Position position, ContinuationKind kind)
        : Node(move(position))
        , m_kind(kind)
    {
    }
    virtual ~ContinuationControl() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    ContinuationKind continuation_kind() const { return m_kind; }

private:
    NODE(ContinuationControl);

    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;

    ContinuationKind m_kind { ContinuationKind::Break };
};

class DynamicEvaluate final : public Node {
public:
    DynamicEvaluate(Position, NonnullRefPtr<Node>);
    virtual ~DynamicEvaluate();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& inner() const { return m_inner; }

private:
    NODE(DynamicEvaluate);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

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

    RefPtr<Node> const& inner() const { return m_inner; }

private:
    NODE(DoubleQuotedString);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    RefPtr<Node> m_inner;
};

class Fd2FdRedirection final : public Node {
public:
    Fd2FdRedirection(Position, int, int);
    virtual ~Fd2FdRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    int source_fd() const { return m_old_fd; }
    int dest_fd() const { return m_new_fd; }

private:
    NODE(Fd2FdRedirection);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_command() const override { return true; }

    int m_old_fd { -1 };
    int m_new_fd { -1 };
};

class FunctionDeclaration final : public Node {
public:
    FunctionDeclaration(Position, NameWithPosition name, Vector<NameWithPosition> argument_names, RefPtr<AST::Node> body);
    virtual ~FunctionDeclaration();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NameWithPosition const& name() const { return m_name; }
    Vector<NameWithPosition> const arguments() const { return m_arguments; }
    RefPtr<Node> const& block() const { return m_block; }

private:
    NODE(FunctionDeclaration);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual bool would_execute() const override { return true; }
    virtual bool should_override_execution_in_current_process() const override { return true; }

    NameWithPosition m_name;
    Vector<NameWithPosition> m_arguments;
    RefPtr<AST::Node> m_block;
};

class ForLoop final : public Node {
public:
    ForLoop(Position, Optional<NameWithPosition> variable, Optional<NameWithPosition> index_variable, RefPtr<AST::Node> iterated_expr, RefPtr<AST::Node> block, Optional<Position> in_kw_position = {}, Optional<Position> index_kw_position = {});
    virtual ~ForLoop();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    Optional<NameWithPosition> const& variable() const { return m_variable; }
    Optional<NameWithPosition> const& index_variable() const { return m_index_variable; }
    RefPtr<Node> const& iterated_expression() const { return m_iterated_expression; }
    RefPtr<Node> const& block() const { return m_block; }
    Optional<Position> const index_keyword_position() const { return m_index_kw_position; }
    Optional<Position> const in_keyword_position() const { return m_in_kw_position; }

private:
    NODE(ForLoop);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool would_execute() const override { return true; }
    virtual bool should_override_execution_in_current_process() const override { return true; }

    Optional<NameWithPosition> m_variable;
    Optional<NameWithPosition> m_index_variable;
    RefPtr<AST::Node> m_iterated_expression;
    RefPtr<AST::Node> m_block;
    Optional<Position> m_in_kw_position;
    Optional<Position> m_index_kw_position;
};

class Glob final : public Node {
public:
    Glob(Position, String);
    virtual ~Glob();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String const& text() const { return m_text; }

private:
    NODE(Glob);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual bool is_glob() const override { return true; }
    virtual bool is_list() const override { return true; }

    String m_text;
};

struct HistorySelector {
    enum EventKind {
        IndexFromStart,
        IndexFromEnd,
        StartingStringLookup,
        ContainingStringLookup,
    };
    enum WordSelectorKind {
        Index,
        Last,
    };

    struct {
        EventKind kind { IndexFromStart };
        size_t index { 0 };
        Position text_position;
        String text;
    } event;

    struct WordSelector {
        WordSelectorKind kind { Index };
        size_t selector { 0 };
        Position position;
        RefPtr<AST::SyntaxError> syntax_error_node;

        size_t resolve(size_t size) const
        {
            if (kind == Index)
                return selector;
            if (kind == Last)
                return size - selector - 1;
            VERIFY_NOT_REACHED();
        }
    };
    struct {
        WordSelector start;
        Optional<WordSelector> end;
    } word_selector_range;
};

class HistoryEvent final : public Node {
public:
    HistoryEvent(Position, HistorySelector);
    virtual ~HistoryEvent();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    HistorySelector const& selector() const { return m_selector; }

private:
    NODE(HistoryEvent);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;

    HistorySelector m_selector;
};

class Execute final : public Node {
public:
    Execute(Position, NonnullRefPtr<Node>, bool capture_stdout = false);
    virtual ~Execute();
    void capture_stdout() { m_capture_stdout = true; }
    NonnullRefPtr<Node>& command() { return m_command; }
    virtual ErrorOr<void> for_each_entry(RefPtr<Shell> shell, Function<ErrorOr<IterationDecision>(NonnullRefPtr<Value>)> callback) override;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& command() const { return m_command; }
    bool does_capture_stdout() const { return m_capture_stdout; }

private:
    NODE(Execute);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
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

    NonnullRefPtr<Node> const& condition() const { return m_condition; }
    RefPtr<Node> const& true_branch() const { return m_true_branch; }
    RefPtr<Node> const& false_branch() const { return m_false_branch; }
    RefPtr<Node>& false_branch() { return m_false_branch; }
    Optional<Position> const else_position() const { return m_else_position; }

private:
    NODE(IfCond);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool should_override_execution_in_current_process() const override { return true; }

    NonnullRefPtr<AST::Node> m_condition;
    RefPtr<AST::Node> m_true_branch;
    RefPtr<AST::Node> m_false_branch;

    Optional<Position> m_else_position;
};

class ImmediateExpression final : public Node {
public:
    ImmediateExpression(Position, NameWithPosition function, Vector<NonnullRefPtr<AST::Node>> arguments, Optional<Position> closing_brace_position);
    virtual ~ImmediateExpression();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    Vector<NonnullRefPtr<Node>> const& arguments() const { return m_arguments; }
    auto const& function() const { return m_function; }
    String const& function_name() const { return m_function.name; }
    Position const& function_position() const { return m_function.position; }
    bool has_closing_brace() const { return m_closing_brace_position.has_value(); }

private:
    NODE(ImmediateExpression);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual HitTestResult hit_test_position(size_t) const override;

    Vector<NonnullRefPtr<AST::Node>> m_arguments;
    NameWithPosition m_function;
    Optional<Position> m_closing_brace_position;
};

class Join final : public Node {
public:
    Join(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~Join();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& left() const { return m_left; }
    NonnullRefPtr<Node> const& right() const { return m_right; }

private:
    NODE(Join);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }
    virtual RefPtr<Node const> leftmost_trivial_literal() const override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

struct MatchEntry {
    Variant<Vector<NonnullRefPtr<Node>>, Vector<Regex<ECMA262>>> options;
    Optional<Vector<String>> match_names;
    Optional<Position> match_as_position;
    Vector<Position> pipe_positions;
    RefPtr<Node> body;
};

class MatchExpr final : public Node {
public:
    MatchExpr(Position, NonnullRefPtr<Node> expr, String name, Optional<Position> as_position, Vector<MatchEntry> entries);
    virtual ~MatchExpr();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& matched_expr() const { return m_matched_expr; }
    String const& expr_name() const { return m_expr_name; }
    Vector<MatchEntry> const& entries() const { return m_entries; }
    Optional<Position> const& as_position() const { return m_as_position; }

private:
    NODE(MatchExpr);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool would_execute() const override { return true; }
    virtual bool should_override_execution_in_current_process() const override { return true; }

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

    NonnullRefPtr<Node> const& left() const { return m_left; }
    NonnullRefPtr<Node> const& right() const { return m_right; }
    Position const& or_position() const { return m_or_position; }

private:
    NODE(Or);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
    Position m_or_position;
};

class Pipe final : public Node {
public:
    Pipe(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~Pipe();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& left() const { return m_left; }
    NonnullRefPtr<Node> const& right() const { return m_right; }

private:
    NODE(Pipe);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool is_command() const override { return true; }

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

class Range final : public Node {
public:
    Range(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~Range();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& start() const { return m_start; }
    NonnullRefPtr<Node> const& end() const { return m_end; }

private:
    NODE(Range);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    NonnullRefPtr<Node> m_start;
    NonnullRefPtr<Node> m_end;
};

class ReadRedirection final : public PathRedirectionNode {
public:
    ReadRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~ReadRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(ReadRedirection);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
};

class ReadWriteRedirection final : public PathRedirectionNode {
public:
    ReadWriteRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~ReadWriteRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(ReadWriteRedirection);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
};

class Sequence final : public Node {
public:
    Sequence(Position, Vector<NonnullRefPtr<Node>>, Vector<Position> separator_positions);
    virtual ~Sequence();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    Vector<NonnullRefPtr<Node>> const& entries() const { return m_entries; }

    Vector<Position> const& separator_positions() const { return m_separator_positions; }

private:
    NODE(Sequence);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool is_list() const override { return true; }
    virtual bool should_override_execution_in_current_process() const override { return true; }
    virtual RefPtr<Node const> leftmost_trivial_literal() const override;

    Vector<NonnullRefPtr<Node>> m_entries;
    Vector<Position> m_separator_positions;
};

class Subshell final : public Node {
public:
    Subshell(Position, RefPtr<Node> block);
    virtual ~Subshell();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    RefPtr<Node> const& block() const { return m_block; }

private:
    NODE(Subshell);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool would_execute() const override { return false; }
    virtual bool should_override_execution_in_current_process() const override { return true; }

    RefPtr<AST::Node> m_block;
};

class Slice final : public Node {
public:
    Slice(Position, NonnullRefPtr<AST::Node>);
    virtual ~Slice() override;

    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<AST::Node> selector() const { return m_selector; }

    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual HitTestResult hit_test_position(size_t) const override;

protected:
    NODE(Slice);
    NonnullRefPtr<AST::Node> m_selector;
};

class VariableNode : public Node {
public:
    VariableNode(Position position)
        : Node(move(position))
    {
    }

    void set_slice(NonnullRefPtr<Slice>&& slice)
    {
        VERIFY(!m_slice);
        m_slice = move(slice);
        if (m_slice->is_syntax_error())
            set_is_syntax_error(m_slice->syntax_error_node());
    }

    Slice const* slice() const { return m_slice.ptr(); }

protected:
    RefPtr<Slice> m_slice;
};

class SimpleVariable final : public VariableNode {
public:
    SimpleVariable(Position, String);
    virtual ~SimpleVariable();

    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }
    String const& name() const { return m_name; }

private:
    NODE(SimpleVariable);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual bool is_simple_variable() const override { return true; }

    String m_name;
};

class SpecialVariable final : public VariableNode {
public:
    SpecialVariable(Position, char);
    virtual ~SpecialVariable();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    char name() const { return m_name; }

private:
    NODE(SpecialVariable);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual HitTestResult hit_test_position(size_t) const override;

    char m_name { 0 };
};

class Juxtaposition final : public Node {
public:
    enum class Mode {
        ListExpand,
        StringExpand,
    };
    Juxtaposition(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>, Mode = Mode::ListExpand);
    virtual ~Juxtaposition();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& left() const { return m_left; }
    NonnullRefPtr<Node> const& right() const { return m_right; }

private:
    NODE(Juxtaposition);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
    Mode m_mode { Mode::ListExpand };
};

class Heredoc final : public Node {
public:
    Heredoc(Position, String end, bool allow_interpolation, bool deindent, Optional<int> target_fd = {});
    virtual ~Heredoc();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String const& end() const { return m_end; }
    bool allow_interpolation() const { return m_allows_interpolation; }
    bool deindent() const { return m_deindent; }
    Optional<int> target_fd() const { return m_target_fd; }
    bool evaluates_to_string() const { return !m_target_fd.has_value(); }
    RefPtr<AST::Node> const& contents() const { return m_contents; }
    void set_contents(RefPtr<AST::Node> contents)
    {
        m_contents = move(contents);
        if (m_contents->is_syntax_error())
            set_is_syntax_error(m_contents->syntax_error_node());
        else if (is_syntax_error())
            clear_syntax_error();
    }

private:
    NODE(Heredoc);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
    virtual RefPtr<Node const> leftmost_trivial_literal() const override { return this; }

    String m_end;
    bool m_allows_interpolation { false };
    bool m_deindent { false };
    Optional<int> m_target_fd;
    RefPtr<AST::Node> m_contents;
};

class StringLiteral final : public Node {
public:
    enum class EnclosureType {
        None,
        SingleQuotes,
        DoubleQuotes,
    };

    StringLiteral(Position, String, EnclosureType);
    virtual ~StringLiteral();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String const& text() const { return m_text; }
    EnclosureType enclosure_type() const { return m_enclosure_type; }

private:
    NODE(StringLiteral);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual RefPtr<Node const> leftmost_trivial_literal() const override { return this; }

    String m_text;
    EnclosureType m_enclosure_type;
};

class StringPartCompose final : public Node {
public:
    StringPartCompose(Position, NonnullRefPtr<Node>, NonnullRefPtr<Node>);
    virtual ~StringPartCompose();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    NonnullRefPtr<Node> const& left() const { return m_left; }
    NonnullRefPtr<Node> const& right() const { return m_right; }

private:
    NODE(StringPartCompose);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;

    NonnullRefPtr<Node> m_left;
    NonnullRefPtr<Node> m_right;
};

class SyntaxError final : public Node {
public:
    SyntaxError(Position, String, bool is_continuable = false);
    virtual ~SyntaxError();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String const& error_text() const { return m_syntax_error_text; }
    bool is_continuable() const { return m_is_continuable; }

    virtual void clear_syntax_error() override
    {
        m_is_cleared = true;
    }
    virtual void set_is_syntax_error(SyntaxError& error) override
    {
        m_position = error.position();
        m_is_cleared = error.m_is_cleared;
        m_is_continuable = error.m_is_continuable;
        m_syntax_error_text = error.error_text();
    }

    virtual bool is_syntax_error() const override { return !m_is_cleared; }

private:
    NODE(SyntaxError);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override { return { nullptr, nullptr, nullptr }; }
    virtual SyntaxError& syntax_error_node() override;

    String m_syntax_error_text;
    bool m_is_continuable { false };
    bool m_is_cleared { false };
};

class SyntheticNode final : public Node {
public:
    SyntheticNode(Position, NonnullRefPtr<Value>);
    virtual ~SyntheticNode() = default;
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    Value const& value() const { return m_value; }

private:
    NODE(SyntheticValue);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;

    NonnullRefPtr<Value> m_value;
};

class Tilde final : public Node {
public:
    Tilde(Position, String);
    virtual ~Tilde();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

    String text() const;

private:
    NODE(Tilde);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual ErrorOr<Vector<Line::CompletionSuggestion>> complete_for_editor(Shell&, size_t, HitTestResult const&) const override;
    virtual HitTestResult hit_test_position(size_t) const override;
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

    Vector<Variable> const& variables() const { return m_variables; }

private:
    NODE(VariableDeclarations);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
    virtual ErrorOr<void> highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) const override;
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
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
};

class WriteRedirection final : public PathRedirectionNode {
public:
    WriteRedirection(Position, int, NonnullRefPtr<Node>);
    virtual ~WriteRedirection();
    virtual void visit(NodeVisitor& visitor) override { visitor.visit(this); }

private:
    NODE(WriteRedirection);
    virtual ErrorOr<void> dump(int level) const override;
    virtual ErrorOr<RefPtr<Value>> run(RefPtr<Shell>) override;
};

}

namespace AK {

template<>
struct Formatter<Shell::AST::Command> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(FormatBuilder&, Shell::AST::Command const& value);
};

}
