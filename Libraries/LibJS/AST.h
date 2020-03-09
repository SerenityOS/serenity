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
#include <LibJS/Forward.h>
#include <LibJS/Value.h>

namespace JS {

class ASTNode {
public:
    virtual ~ASTNode() {}
    virtual const char* class_name() const = 0;
    virtual Value execute(Interpreter&) const = 0;
    virtual void dump(int indent) const;

protected:
    ASTNode() {}

private:
};

class ScopeNode : public ASTNode {
public:
    template<typename T, typename... Args>
    T& append(Args&&... args)
    {
        auto child = make<T>(forward<Args>(args)...);
        m_children.append(move(child));
        return static_cast<T&>(m_children.last());
    }

    const NonnullOwnPtrVector<ASTNode>& children() const { return m_children; }
    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

protected:
    ScopeNode() {}

private:
    NonnullOwnPtrVector<ASTNode> m_children;
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

class FunctionDeclaration : public ASTNode {
public:
    FunctionDeclaration(String name, NonnullOwnPtr<ScopeNode> body)
        : m_name(move(name))
        , m_body(move(body))
    {
    }

    String name() const { return m_name; }
    const ScopeNode& body() const { return *m_body; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "FunctionDeclaration"; }

    String m_name;
    NonnullOwnPtr<ScopeNode> m_body;
};

class Expression : public ASTNode {
public:
};

class ReturnStatement : public ASTNode {
public:
    explicit ReturnStatement(NonnullOwnPtr<Expression> argument)
        : m_argument(move(argument))
    {
    }

    const Expression& argument() const { return *m_argument; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ReturnStatement"; }

    NonnullOwnPtr<Expression> m_argument;
};

class IfStatement : public ASTNode {
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

class WhileStatement : public ASTNode {
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

enum class BinaryOp {
    Plus,
    Minus,
    TypedEquals,
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
    Not
};

class LogicalExpression : public Expression {
public:
    LogicalExpression(LogicalOp op, NonnullOwnPtr<Expression> lhs, NonnullOwnPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    LogicalExpression(LogicalOp op, NonnullOwnPtr<Expression> lhs)
        : m_op(op)
        , m_lhs(move(lhs))
    {
        ASSERT(op == LogicalOp::Not);
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "LogicalExpression"; }

    LogicalOp m_op;
    NonnullOwnPtr<Expression> m_lhs;
    OwnPtr<Expression> m_rhs;
};

class Literal : public Expression {
public:
    explicit Literal(Value value)
        : m_value(move(value))
    {
    }

    virtual Value execute(Interpreter&) const override { return m_value; }
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "Literal"; }

    Value m_value;
};

class CallExpression : public Expression {
public:
    explicit CallExpression(String name)
        : m_name(move(name))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    const String& name() const { return m_name; }

private:
    virtual const char* class_name() const override { return "CallExpression"; }

    String m_name;
};

}
