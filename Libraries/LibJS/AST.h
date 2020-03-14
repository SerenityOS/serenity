/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/NonnullOwnPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Value.h>

namespace JS {

class ASTNode {
public:
    virtual ~ASTNode() {}
    virtual const char* class_name() const = 0;
    virtual Value execute(Interpreter&) const = 0;
    virtual void dump(int indent) const;
    virtual bool is_identifier() const { return false; }

protected:
    ASTNode() {}

private:
};

class Statement : public ASTNode {
public:
    virtual bool is_variable_declaration() const { return false; }
};

class ErrorStatement final : public Statement {
public:
    Value execute(Interpreter&) const { return js_undefined(); }
    const char* class_name() const override { return "ErrorStatement"; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(NonnullOwnPtr<Expression> expression)
        : m_expression(move(expression))
    {
    }

    Value execute(Interpreter&) const override;
    const char* class_name() const override { return "ExpressionStatement"; }
    virtual void dump(int indent) const override;

private:
    NonnullOwnPtr<Expression> m_expression;
};

class ScopeNode : public Statement {
public:
    template<typename T, typename... Args>
    T& append(Args&&... args)
    {
        auto child = make<T>(forward<Args>(args)...);
        m_children.append(move(child));
        return static_cast<T&>(m_children.last());
    }
    void append(NonnullOwnPtr<Statement> child)
    {
        m_children.append(move(child));
    }

    const NonnullOwnPtrVector<Statement>& children() const { return m_children; }
    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

protected:
    ScopeNode() {}

private:
    NonnullOwnPtrVector<Statement> m_children;
};

class Program : public ScopeNode {
public:
    Program() {}

private:
    virtual const char* class_name() const override { return "Program"; }
};

class BlockStatement : public ScopeNode {
public:
    BlockStatement() {}

private:
    virtual const char* class_name() const override { return "BlockStatement"; }
};

class FunctionDeclaration : public Statement {
public:
    FunctionDeclaration(String name, NonnullOwnPtr<ScopeNode> body, Vector<String> parameters = {})
        : m_name(move(name))
        , m_body(move(body))
        , m_parameters(move(parameters))
    {
    }

    String name() const { return m_name; }
    const ScopeNode& body() const { return *m_body; }
    const Vector<String>& parameters() const { return m_parameters; };

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "FunctionDeclaration"; }

    String m_name;
    NonnullOwnPtr<ScopeNode> m_body;
    const Vector<String> m_parameters;
};

class Expression : public ASTNode {
public:
};

class ErrorExpression final : public Expression {
public:
    Value execute(Interpreter&) const { return js_undefined(); }
    const char* class_name() const override { return "ErrorExpression"; }
};

class ReturnStatement : public Statement {
public:
    explicit ReturnStatement(OwnPtr<Expression> argument)
        : m_argument(move(argument))
    {
    }

    const Expression* argument() const { return m_argument; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ReturnStatement"; }

    OwnPtr<Expression> m_argument;
};

class IfStatement : public Statement {
public:
    IfStatement(NonnullOwnPtr<Expression> predicate, NonnullOwnPtr<ScopeNode> consequent, NonnullOwnPtr<ScopeNode> alternate)
        : m_predicate(move(predicate))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    const Expression& predicate() const { return *m_predicate; }
    const ScopeNode& consequent() const { return *m_consequent; }
    const ScopeNode& alternate() const { return *m_alternate; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "IfStatement"; }

    NonnullOwnPtr<Expression> m_predicate;
    NonnullOwnPtr<ScopeNode> m_consequent;
    NonnullOwnPtr<ScopeNode> m_alternate;
};

class WhileStatement : public Statement {
public:
    WhileStatement(NonnullOwnPtr<Expression> predicate, NonnullOwnPtr<ScopeNode> body)
        : m_predicate(move(predicate))
        , m_body(move(body))
    {
    }

    const Expression& predicate() const { return *m_predicate; }
    const ScopeNode& body() const { return *m_body; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "WhileStatement"; }

    NonnullOwnPtr<Expression> m_predicate;
    NonnullOwnPtr<ScopeNode> m_body;
};

class ForStatement : public Statement {
public:
    ForStatement(OwnPtr<Statement> init, OwnPtr<Expression> test, OwnPtr<Expression> update, NonnullOwnPtr<ScopeNode> body)
        : m_init(move(init))
        , m_test(move(test))
        , m_update(move(update))
        , m_body(move(body))
    {
    }

    const Statement* init() const { return m_init; }
    const Expression* test() const { return m_test; }
    const Expression* update() const { return m_update; }
    const ScopeNode& body() const { return *m_body; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ForStatement"; }

    OwnPtr<Statement> m_init;
    OwnPtr<Expression> m_test;
    OwnPtr<Expression> m_update;
    NonnullOwnPtr<ScopeNode> m_body;
};

enum class BinaryOp {
    Plus,
    Minus,
    Asterisk,
    Slash,
    TypedEquals,
    TypedInequals,
    GreaterThan,
    GreaterThanEquals,
    LessThan,
    LessThanEquals,
    BitwiseAnd,
    BitwiseOr,
    BitwiseXor,
    LeftShift,
    RightShift,
};

class BinaryExpression : public Expression {
public:
    BinaryExpression(BinaryOp op, NonnullOwnPtr<Expression> lhs, NonnullOwnPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "BinaryExpression"; }

    BinaryOp m_op;
    NonnullOwnPtr<Expression> m_lhs;
    NonnullOwnPtr<Expression> m_rhs;
};

enum class LogicalOp {
    And,
    Or,
};

class LogicalExpression : public Expression {
public:
    LogicalExpression(LogicalOp op, NonnullOwnPtr<Expression> lhs, NonnullOwnPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "LogicalExpression"; }

    LogicalOp m_op;
    NonnullOwnPtr<Expression> m_lhs;
    NonnullOwnPtr<Expression> m_rhs;
};

enum class UnaryOp {
    BitNot,
    Not,
};

class UnaryExpression : public Expression {
public:
    UnaryExpression(UnaryOp op, NonnullOwnPtr<Expression> lhs)
        : m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "UnaryExpression"; }

    UnaryOp m_op;
    NonnullOwnPtr<Expression> m_lhs;
};

class Literal : public Expression {
protected:
    explicit Literal() {}
};

class BooleanLiteral final : public Literal {
public:
    explicit BooleanLiteral(bool value)
        : m_value(value)
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "BooleanLiteral"; }

    bool m_value { false };
};

class NumericLiteral final : public Literal {
public:
    explicit NumericLiteral(double value)
        : m_value(value)
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "NumericLiteral"; }

    double m_value { 0 };
};

class StringLiteral final : public Literal {
public:
    explicit StringLiteral(String value)
        : m_value(move(value))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "StringLiteral"; }

    String m_value;
};

class Identifier final : public Expression {
public:
    explicit Identifier(String string)
        : m_string(move(string))
    {
    }

    const String& string() const { return m_string; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual bool is_identifier() const override { return true; }

private:
    virtual const char* class_name() const override { return "Identifier"; }

    String m_string;
};

class CallExpression : public Expression {
public:
    explicit CallExpression(String name, NonnullOwnPtrVector<Expression> arguments = {})
        : m_name(move(name))
        , m_arguments(move(arguments))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    const String& name() const { return m_name; }

private:
    virtual const char* class_name() const override { return "CallExpression"; }

    String m_name;
    const NonnullOwnPtrVector<Expression> m_arguments;
};

enum class AssignmentOp {
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
    MultiplicationAssignment,
    DivisionAssignment,
};

class AssignmentExpression : public Expression {
public:
    AssignmentExpression(AssignmentOp op, NonnullOwnPtr<ASTNode> lhs, NonnullOwnPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "AssignmentExpression"; }

    AssignmentOp m_op;
    NonnullOwnPtr<ASTNode> m_lhs;
    NonnullOwnPtr<Expression> m_rhs;
};

enum class UpdateOp {
    Increment,
    Decrement,
};

class UpdateExpression : public Expression {
public:
    UpdateExpression(UpdateOp op, NonnullOwnPtr<Expression> argument)
        : m_op(op)
        , m_argument(move(argument))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "UpdateExpression"; }

    UpdateOp m_op;
    NonnullOwnPtr<Identifier> m_argument;
};

enum class DeclarationType {
    Var,
    Let,
    Const,
};

class VariableDeclaration : public Statement {
public:
    VariableDeclaration(NonnullOwnPtr<Identifier> name, OwnPtr<Expression> initializer, DeclarationType declaration_type)
        : m_declaration_type(declaration_type)
        , m_name(move(name))
        , m_initializer(move(initializer))
    {
    }

    virtual bool is_variable_declaration() const override { return true; }
    const Identifier& name() const { return *m_name; }
    DeclarationType declaration_type() const { return m_declaration_type; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "VariableDeclaration"; }

    DeclarationType m_declaration_type;
    NonnullOwnPtr<Identifier> m_name;
    OwnPtr<Expression> m_initializer;
};

class ObjectExpression : public Expression {
public:
    ObjectExpression() {}

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ObjectExpression"; }
};

class MemberExpression final : public Expression {
public:
    MemberExpression(NonnullOwnPtr<Expression> object, NonnullOwnPtr<Expression> property)
        : m_object(move(object))
        , m_property(move(property))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "MemberExpression"; }

    NonnullOwnPtr<Expression> m_object;
    NonnullOwnPtr<Expression> m_property;
};

}
