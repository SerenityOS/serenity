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
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibLine/Editor.h>

using TheExecutionInputType = RefPtr<Shell>;

namespace AST {

struct HighlightMetadata {
    bool is_first_in_list { true };
};

struct Position {
    size_t start_offset { 0 };
    size_t end_offset { 0 };
    bool contains(size_t offset) const { return start_offset <= offset && offset <= end_offset; }
};

struct Rewiring {
    int source_fd { -1 };
    int dest_fd { -1 };
    enum class Close {
        None,
        Source,
        Destination,
    } must_be_closed { Close::None };
};

struct Redirection : public RefCounted<Redirection> {
    virtual Result<Rewiring, String> apply() const = 0;
    virtual ~Redirection();
    virtual bool is_path_redirection() const { return false; }
    virtual bool is_fd_redirection() const { return false; }
    virtual bool is_close_redirection() const { return false; }
};

struct CloseRedirection : public Redirection {
    int fd { -1 };

    virtual Result<Rewiring, String> apply() const override;
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

    virtual Result<Rewiring, String> apply() const override;
    virtual ~PathRedirection();
    PathRedirection(String path, int fd, decltype(direction) direction)
        : path(move(path))
        , fd(fd)
        , direction(direction)
    {
    }

private:
    virtual bool is_path_redirection() const override { return true; }
};

struct FdRedirection : public Redirection
    , public Rewiring {

    virtual Result<Rewiring, String> apply() const override { return *this; }
    virtual ~FdRedirection();
    FdRedirection(int source, int dest, Rewiring::Close close)
        : Rewiring({ source, dest, close })
    {
    }

private:
    virtual bool is_fd_redirection() const override { return true; }
};

struct Command {
    Vector<String> argv;
    Vector<NonnullRefPtr<Redirection>> redirections;
    bool should_wait { true };
    bool is_pipe_source { false };
};

struct HitTestResult {
    RefPtr<Node> matching_node;
    RefPtr<Node> closest_node_with_semantic_meaning; // This is used if matching_node is a bareword
};

class Value : public RefCounted<Value> {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) = 0;
    virtual Vector<Command> resolve_as_commands(TheExecutionInputType);
    virtual ~Value();
    virtual bool is_command() const { return false; }
    virtual bool is_glob() const { return false; }
    virtual bool is_job() const { return false; }
    virtual bool is_list() const { return false; }
    virtual bool is_string() const { return false; }
};

class CommandValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual Vector<Command> resolve_as_commands(TheExecutionInputType) override;
    virtual ~CommandValue();
    virtual bool is_command() const override { return true; }
    CommandValue(Command command)
        : m_command(move(command))
    {
    }

    CommandValue(Vector<String> argv)
        : m_command({ move(argv), {}, true, false })
    {
    }

private:
    Command m_command;
};

class CommandSequenceValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual Vector<Command> resolve_as_commands(TheExecutionInputType) override;
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
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override { ASSERT_NOT_REACHED(); }
    virtual Vector<Command> resolve_as_commands(TheExecutionInputType) override { ASSERT_NOT_REACHED(); }
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
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual ~ListValue();
    virtual bool is_list() const override { return true; }
    ListValue(Vector<String> values);
    ListValue(Vector<RefPtr<Value>> values)
        : m_contained_values(move(values))
    {
    }

private:
    Vector<RefPtr<Value>> m_contained_values;
};

class StringValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual ~StringValue();
    virtual bool is_string() const override { return m_split.is_null(); }
    virtual bool is_list() const override { return !m_split.is_null(); }
    StringValue(String string, String split_by = {})
        : m_string(string)
        , m_split(move(split_by))
    {
    }

private:
    String m_string;
    String m_split;
};

class GlobValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual ~GlobValue();
    virtual bool is_glob() const override { return true; }
    GlobValue(String glob)
        : m_glob(glob)
    {
    }

private:
    String m_glob;
};

class SimpleVariableValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual ~SimpleVariableValue();
    // FIXME: Should override is_list and is_string,
    //        as it might have different types of values.
    SimpleVariableValue(String name)
        : m_name(name)
    {
    }

private:
    String m_name;
};

class SpecialVariableValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual ~SpecialVariableValue();
    // FIXME: Should override is_list and is_string,
    //        as it might have different types of values.
    SpecialVariableValue(char name)
        : m_name(name)
    {
    }

private:
    char m_name { -1 };
};

class TildeValue final : public Value {
public:
    virtual Vector<String> resolve_as_list(TheExecutionInputType) override;
    virtual ~TildeValue();
    virtual bool is_string() const override { return true; }
    TildeValue(String name)
        : m_username(name)
    {
    }

private:
    String m_username;
};

class Node : public RefCounted<Node> {
public:
    virtual void dump(int level) = const 0;
    virtual RefPtr<Value> run(TheExecutionInputType) = 0;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) = 0;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node);
    Vector<Line::CompletionSuggestion> complete_for_editor(Shell& shell, size_t offset);
    virtual HitTestResult hit_test_position(size_t offset)
    {
        if (m_position.contains(offset))
            return { this, nullptr };
        return { nullptr, nullptr };
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

    virtual bool is_list() const { return false; }

    const Position& position() const { return m_position; }

protected:
    Position m_position;
};

class PathRedirectionNode : public Node {
public:
    PathRedirectionNode(Position, int, RefPtr<Node>);
    virtual ~PathRedirectionNode();
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node) override;
    virtual HitTestResult hit_test_position(size_t offset) override;
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }

protected:
    int m_fd { -1 };
    RefPtr<Node> m_path;
};

class And final : public Node {
public:
    And(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~And();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "And"; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class ListConcatenate final : public Node {
public:
    ListConcatenate(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~ListConcatenate();

private:
    virtual void dump(int level) const override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "ListConcatenate"; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_element;
    RefPtr<Node> m_list;
};

class Background final : public Node {
public:
    Background(Position, RefPtr<Node>);
    virtual ~Background();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Background"; }

    RefPtr<Node> m_command;
};

class BarewordLiteral final : public Node {
public:
    BarewordLiteral(Position, String);
    virtual ~BarewordLiteral();
    const String& text() const { return m_text; }

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual String class_name() const override { return "BarewordLiteral"; }
    virtual bool is_bareword() const override { return true; }

    String m_text;
};

class CastToCommand final : public Node {
public:
    CastToCommand(Position, RefPtr<Node>);
    virtual ~CastToCommand();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node) override;
    virtual String class_name() const override { return "CastToCommand"; }
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_inner;
};

class CastToList final : public Node {
public:
    CastToList(Position, RefPtr<Node>);
    virtual ~CastToList();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "CastToList"; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_inner;
};

class CloseFdRedirection final : public Node {
public:
    CloseFdRedirection(Position, int);
    virtual ~CloseFdRedirection();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual String class_name() const override { return "CloseFdRedirection"; }
    virtual bool is_command() const override { return true; }

    int m_fd { -1 };
};

class CommandLiteral final : public Node {
public:
    CommandLiteral(Position, Command);
    virtual ~CommandLiteral();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override { ASSERT_NOT_REACHED(); }
    virtual String class_name() const override { return "CommandLiteral"; }
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }

    Command m_command;
};

class Comment : public Node {
public:
    Comment(Position, String);
    virtual ~Comment();
    const String& text() const { return m_text; }

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual String class_name() const override { return "Comment"; }

    String m_text;
};

class DynamicEvaluate final : public Node {
public:
    DynamicEvaluate(Position, RefPtr<Node>);
    virtual ~DynamicEvaluate();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "DynamicEvaluate"; }

    virtual bool is_bareword() const override { return m_inner->is_bareword(); }
    virtual bool is_command() const override { return is_list(); }
    virtual bool is_execute() const override { return true; }
    virtual bool is_glob() const override { return m_inner->is_glob(); }
    virtual bool is_list() const override
    {
        return m_inner->is_list() || m_inner->is_command() || m_inner->is_glob(); // Anything that generates a list.
    }

    RefPtr<Node> m_inner;
};

class DoubleQuotedString final : public Node {
public:
    DoubleQuotedString(Position, RefPtr<Node>);
    virtual ~DoubleQuotedString();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "DoubleQuotedString"; }

    RefPtr<Node> m_inner;
};

class Fd2FdRedirection final : public Node {
public:
    Fd2FdRedirection(Position, int, int);
    virtual ~Fd2FdRedirection();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual String class_name() const override { return "Fd2FdRedirection"; }
    virtual bool is_command() const override { return true; }

    int source_fd { -1 };
    int dest_fd { -1 };
};

class Glob final : public Node {
public:
    Glob(Position, String);
    virtual ~Glob();
    const String& text() const { return m_text; }

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual String class_name() const override { return "Glob"; }
    virtual bool is_glob() const override { return true; }
    virtual bool is_list() const override { return true; }

    String m_text;
};

class Execute final : public Node {
public:
    Execute(Position, RefPtr<Node>, bool capture_stdout = false);
    virtual ~Execute();
    void capture_stdout() { m_capture_stdout = true; }
    RefPtr<Node> command() { return m_command; }

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node) override;
    virtual String class_name() const override { return "Execute"; }
    virtual bool is_execute() const override { return true; }

    RefPtr<Node> m_command;
    bool m_capture_stdout { false };
};

class Join final : public Node {
public:
    Join(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~Join();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Join"; }
    virtual bool is_command() const override { return true; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class Or final : public Node {
public:
    Or(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~Or();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Or"; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class Pipe final : public Node {
public:
    Pipe(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~Pipe();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Pipe"; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class ReadRedirection final : public PathRedirectionNode {
public:
    ReadRedirection(Position, int, RefPtr<Node>);
    virtual ~ReadRedirection();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual String class_name() const override { return "ReadRedirection"; }
};

class ReadWriteRedirection final : public PathRedirectionNode {
public:
    ReadWriteRedirection(Position, int, RefPtr<Node>);
    virtual ~ReadWriteRedirection();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual String class_name() const override { return "ReadWriteRedirection"; }
};

class Sequence final : public Node {
public:
    Sequence(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~Sequence();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Sequence"; }
    virtual bool is_list() const override { return true; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class SimpleVariable final : public Node {
public:
    SimpleVariable(Position, String);
    virtual ~SimpleVariable();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "SimpleVariable"; }

    String m_name;
};

class SpecialVariable final : public Node {
public:
    SpecialVariable(Position, char);
    virtual ~SpecialVariable();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "SpecialVariable"; }

    char m_name { -1 };
};

class Juxtaposition final : public Node {
public:
    Juxtaposition(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~Juxtaposition();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Juxtaposition"; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class StringLiteral final : public Node {
public:
    StringLiteral(Position, String);
    virtual ~StringLiteral();
    const String& text() const { return m_text; }

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual String class_name() const override { return "StringLiteral"; }

    String m_text;
};

class StringPartCompose final : public Node {
public:
    StringPartCompose(Position, RefPtr<Node>, RefPtr<Node>);
    virtual ~StringPartCompose();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "StringPartCompose"; }

    RefPtr<Node> m_left;
    RefPtr<Node> m_right;
};

class SyntaxError final : public Node {
public:
    SyntaxError(Position);
    virtual ~SyntaxError();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override { return { nullptr, nullptr }; }
    virtual String class_name() const override { return "SyntaxError"; }
};

class Tilde final : public Node {
public:
    Tilde(Position, String);
    virtual ~Tilde();
    String text() const;

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual Vector<Line::CompletionSuggestion> complete_for_editor(Shell&, size_t, RefPtr<Node> matching_node) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "Tilde"; }
    virtual bool is_tilde() const override { return true; }

    String m_username;
};

class VariableDeclarations final : public Node {
public:
    struct Variable {
        RefPtr<Node> name;
        RefPtr<Node> value;
    };
    VariableDeclarations(Position, Vector<Variable> variables);
    virtual ~VariableDeclarations();

    const Vector<Variable>& variables() const { return m_variables; }

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual void highlight_in_editor(Line::Editor&, Shell&, HighlightMetadata = {}) override;
    virtual HitTestResult hit_test_position(size_t) override;
    virtual String class_name() const override { return "VariableDeclarations"; }
    virtual bool is_variable_decls() const override { return true; }

    Vector<Variable> m_variables;
};

class WriteAppendRedirection final : public PathRedirectionNode {
public:
    WriteAppendRedirection(Position, int, RefPtr<Node>);
    virtual ~WriteAppendRedirection();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual String class_name() const override { return "WriteAppendRedirection"; }
};

class WriteRedirection final : public PathRedirectionNode {
public:
    WriteRedirection(Position, int, RefPtr<Node>);
    virtual ~WriteRedirection();

private:
    virtual void dump(int level) const override;
    virtual RefPtr<Value> run(TheExecutionInputType) override;
    virtual String class_name() const override { return "WriteRedirection"; }
};

}
