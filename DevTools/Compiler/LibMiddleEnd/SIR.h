/*
 * Copyright (c) 2020, Denis Campredon <deni_@hotmail.fr>
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

#include <AK/NonnullRefPtrVector.h>
#include <AK/String.h>

namespace Cpp {
struct Option;
}

namespace SIR {
class ASTNode : public RefCounted<ASTNode> {

public:
    ASTNode() = default;
    virtual ~ASTNode() = default;
    virtual bool is_expression() const { return false; }
    virtual bool is_statement() const { return false; }
    virtual bool is_variable() const { return false; }
    virtual bool is_return_statement() const { return false; }
    virtual bool is_primary_expression() const { return false; }

    virtual bool is_identifier_expression() const { return false; }
    virtual bool is_binary_expression() const { return false; }
};

class Type : public ASTNode {
public:
    enum class Kind {
        Integer,
        Void,
    };

    Type(Kind kind, size_t size_in_bits, size_t size_in_bytes)
        : m_kind(kind)
        , m_size_in_bits(size_in_bits)
        , m_size_in_bytes(size_in_bytes)
    {
    }

    Kind kind() const { return m_kind; }
    size_t size_in_bits() const { return m_size_in_bits; }
    size_t size_in_bytes() const { return m_size_in_bytes; }

private:
    const Kind m_kind;
    const size_t m_size_in_bits;
    const size_t m_size_in_bytes;
};

class VoidType : public Type {
public:
    VoidType()
        : Type(Kind::Void, 0, 0)
    {
    }
};

class IntegerType : public Type {
public:
    IntegerType(size_t size_in_bits, size_t size_in_bytes, bool is_signed)
        : Type(Kind::Integer, size_in_bits, size_in_bytes)
        , m_is_signed(is_signed)
    {
    }

private:
    bool m_is_signed;
};

class Variable : public ASTNode {
public:
    Variable(NonnullRefPtr<Type>& node_type, String& name)
        : m_node_type(node_type)
        , m_name(name)
    {
    }

    bool is_variable() const override { return true; }
    const NonnullRefPtr<Type>& node_type() const { return m_node_type; }
    NonnullRefPtr<Type>& node_type() { return m_node_type; }
    const String& name() const { return m_name; }
    String& name() { return m_name; }

private:
    NonnullRefPtr<Type> m_node_type;
    String m_name;
};

class Statement : public ASTNode {
public:
    bool is_statement() const override { return true; }
};

class Expression : public ASTNode {
public:
    explicit Expression(RefPtr<Variable>& result)
        : m_result(result)
    {
    }
    bool is_expression() const override { return true; }

    const RefPtr<Variable>& result() const { return m_result; }

private:
    RefPtr<Variable> m_result;
};

class BinaryExpression : public Expression {
public:
    enum class Kind {
        Addition,
        Multiplication,
        Subtraction
    };
    BinaryExpression(Kind kind, NonnullRefPtr<ASTNode>& left, NonnullRefPtr<ASTNode>& right, RefPtr<Variable> result)
        : Expression(result)
        , m_binary_operation(kind)
        , m_left(left)
        , m_right(right)
    {
    }

    bool is_binary_expression() const override { return true; }

    const NonnullRefPtr<ASTNode>& left() const { return m_left; }
    NonnullRefPtr<ASTNode>& left() { return m_left; }
    const NonnullRefPtr<ASTNode>& right() const { return m_right; }
    NonnullRefPtr<ASTNode>& right() { return m_right; }
    Kind binary_operation() const { return m_binary_operation; }

private:
    Kind m_binary_operation;
    NonnullRefPtr<ASTNode> m_left;
    NonnullRefPtr<ASTNode> m_right;
};

class PrimaryExpression : public Expression {
public:
    explicit PrimaryExpression(RefPtr<Variable> result)
        : Expression(result)
    {
    }
    bool is_primary_expression() const override { return false; }
};

class IdentifierExpression : public PrimaryExpression {
public:
    //TODO: m_identifier should disapear to replace result
    explicit IdentifierExpression(String& identifier, RefPtr<Variable>& result)
        : PrimaryExpression(result)
        , m_identifier(identifier)
    {
    }
    bool is_identifier_expression() const override { return true; }

private:
    String m_identifier;
};

class ReturnStatement : public Statement {
public:
    explicit ReturnStatement()
    {
    }
    explicit ReturnStatement(RefPtr<Expression>& expression)
        : m_expression(expression)
    {
    }
    bool is_return_statement() const override { return true; }

    const RefPtr<ASTNode>& expression() const { return m_expression; }
    RefPtr<ASTNode>& expression() { return m_expression; }
    void set_expression(NonnullRefPtr<ASTNode>& expression) { m_expression = expression; }

private:
    RefPtr<ASTNode> m_expression;
};

class Function : public RefCounted<Function> {
public:
    Function(NonnullRefPtr<Type>& return_type, String& name, NonnullRefPtrVector<Variable>& parameters, NonnullRefPtrVector<ASTNode>& body)
        : m_return_type(return_type)
        , m_name(name)
        , m_parameters(parameters)
        , m_body(body)
    {
    }

    const String& name() const { return m_name; }
    void set_name(const String& name) { this->m_name = name; }
    const NonnullRefPtr<Type>& return_type() const { return m_return_type; }
    const NonnullRefPtrVector<Variable>& parameters() const { return m_parameters; }
    NonnullRefPtrVector<Variable>& parameters() { return m_parameters; }
    NonnullRefPtrVector<ASTNode>& body() { return m_body; }
    const NonnullRefPtrVector<ASTNode>& body() const { return m_body; }

private:
    NonnullRefPtr<Type> m_return_type;
    String m_name;
    NonnullRefPtrVector<Variable> m_parameters;
    NonnullRefPtrVector<ASTNode> m_body;
};

class TranslationUnit {
public:
    TranslationUnit(NonnullRefPtrVector<Function> functions = NonnullRefPtrVector<Function>())
        : m_functions(move(functions))
    {
    }

    const NonnullRefPtrVector<Function>& functions() const { return m_functions; }
    NonnullRefPtrVector<Function>& functions() { return m_functions; }

private:
    NonnullRefPtrVector<Function> m_functions;
};

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(Args&&... args)
{
    return adopt(*new T(forward<Args>(args)...));
}

void run_intermediate_representation_passes(SIR::TranslationUnit&);

}
using SIR::create_ast_node;
