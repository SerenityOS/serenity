/*
 * Copyright (c) 2020-2021, Denis Campredon <deni_@hotmail.fr>
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
#include <LibCpp/Lexer.h>

namespace Cpp {
struct Option;
}

namespace SIR {

using Position = Cpp::Position;

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(Args&&... args)
{
    return adopt(*new T(forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {

public:
    ASTNode(Position start, Position end)
        : m_start(start)
        , m_end(end)
    {
    }
    virtual ~ASTNode() = default;
    virtual bool is_expression() const { return false; }
    virtual bool is_statement() const { return false; }
    virtual bool is_variable() const { return false; }
    virtual bool is_return_statement() const { return false; }
    virtual bool is_jump_statement() const { return false; }
    virtual bool is_primary_expression() const { return false; }

    virtual bool is_identifier_expression() const { return false; }
    virtual bool is_label_expression() const { return false; }
    virtual bool is_binary_expression() const { return false; }
    virtual bool is_constant_expression() const { return false; }

    const Position& start() { return m_start; }
    const Position& end() { return m_end; }

private:
    Position m_start;
    Position m_end;
};

class Type : public ASTNode {
public:
    enum class Kind {
        Boolean,
        Integer,
        Void,
    };

    Type(Position start, Position end, Kind kind, size_t size_in_bits, size_t size_in_bytes)
        : ASTNode(start, end)
        , m_kind(kind)
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
    VoidType(Position start, Position end)
        : Type(start, end, Kind::Void, 0, 0)
    {
    }
};

class IntegerType : public Type {
public:
    IntegerType(Position start, Position end, size_t size_in_bits, size_t size_in_bytes, bool is_signed)
        : Type(start, end, Kind::Integer, size_in_bits, size_in_bytes)
        , m_is_signed(is_signed)
    {
    }

private:
    bool m_is_signed;
};

class BooleanType : public Type {
public:
    BooleanType(Position start, Position end)
        : Type(start, end, Kind::Boolean, 1, 1)
    {
    }
};

class Variable : public ASTNode {
public:
    Variable(Position start, Position end, NonnullRefPtr<Type> node_type, String name)
        : ASTNode(start, end)
        , m_node_type(move(node_type))
        , m_name(move(name))
    {
    }
    Variable(Position start, Position end, NonnullRefPtr<Type> node_type)
        : Variable(start, end, move(node_type), String::format(".D%zu", number_unnamed_variable_created++))
    {
    }
    explicit Variable(NonnullRefPtr<Variable>& other)
        : Variable(other->start(), other->end(), other->node_type())
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
    static inline size_t number_unnamed_variable_created { 0 };
};

class Statement : public ASTNode {
public:
    Statement(Position start, Position end)
        : ASTNode(start, end)
    {
    }

    bool is_statement() const override { return true; }
};

class Expression : public ASTNode {
public:
    Expression(Position start, Position end, NonnullRefPtr<Variable> result)
        : ASTNode(start, end)
        , m_result(move(result))
    {
    }
    bool is_expression() const override { return true; }

    const NonnullRefPtr<Variable>& result() const { return m_result; }
    NonnullRefPtr<Variable>& result() { return m_result; }

private:
    NonnullRefPtr<Variable> m_result;
};

class BinaryExpression : public Expression {
public:
    enum class Kind {
        Addition,
        And,
        Division,
        LeftShift,
        Modulo,
        Multiplication,
        NotEqual,
        Or,
        RightShift,
        Subtraction,
        Xor
    };
    BinaryExpression(Position start, Position end, Kind kind, NonnullRefPtr<ASTNode> left, NonnullRefPtr<ASTNode> right, NonnullRefPtr<Variable> result)
        : Expression(start, end, move(result))
        , m_binary_operation(kind)
        , m_left(move(left))
        , m_right(move(right))
    {
    }

    bool is_binary_expression() const override { return true; }

    const NonnullRefPtr<Expression>& left() const { return m_left; }
    NonnullRefPtr<Expression>& left() { return m_left; }
    const NonnullRefPtr<Expression>& right() const { return m_right; }
    NonnullRefPtr<Expression>& right() { return m_right; }
    void set_right(NonnullRefPtr<Expression> right) { m_right = move(right); }
    void set_left(NonnullRefPtr<Expression> left) { m_left = move(left); }
    Kind binary_operation() const { return m_binary_operation; }

private:
    Kind m_binary_operation;
    NonnullRefPtr<Expression> m_left;
    NonnullRefPtr<Expression> m_right;
};

class PrimaryExpression : public Expression {
public:
    explicit PrimaryExpression(Position start, Position end, NonnullRefPtr<Variable> result)
        : Expression(start, end, move(result))
    {
    }
    bool is_primary_expression() const override { return false; }
};

class IdentifierExpression : public PrimaryExpression {
public:
    explicit IdentifierExpression(Position start, Position end, NonnullRefPtr<Variable> result)
        : PrimaryExpression(start, end, move(result))

    {
    }
    bool is_identifier_expression() const override { return true; }
};

class ConstantExpression : public PrimaryExpression {
public:
    explicit ConstantExpression(Position start, Position end, int value)
        : PrimaryExpression(start, end, create_ast_node<Variable>(start, end, create_ast_node<IntegerType>(start, end, 32, 32, true)))
        , m_value(value)
    {
    }

    bool is_constant_expression() const override { return true; }
    int value() const { return m_value; }

private:
    int m_value;
};

class LabelExpression : public PrimaryExpression {
public:
    explicit LabelExpression(Position start, Position end)
        : PrimaryExpression(start, end, create_ast_node<Variable>(start, end, create_ast_node<VoidType>(start, end), String::format(".L%zu", number_unnamed_label_created++)))
    {
    }
    bool is_label_expression() const override { return true; }
    const String& identifier() const { return result()->name(); }

private:
    static inline size_t number_unnamed_label_created { 0 };
};

class JumpStatement : public Statement {
public:
    JumpStatement(Position start, Position end, NonnullRefPtr<Expression> condition, NonnullRefPtrVector<ASTNode> if_true, Optional<NonnullRefPtrVector<ASTNode>> if_false)
        : Statement(start, end)
        , m_condition(move(condition))
        , m_if_true(move(if_true))
        , m_if_false(move(if_false))
    {
    }

    bool is_jump_statement() const override { return true; }

    NonnullRefPtr<Expression> condition() const { return m_condition; }
    NonnullRefPtrVector<ASTNode>& if_true() { return m_if_true; }
    const NonnullRefPtrVector<ASTNode>& if_true() const { return m_if_true; }
    Optional<NonnullRefPtrVector<ASTNode>>& if_false() { return m_if_false; }
    const Optional<NonnullRefPtrVector<ASTNode>>& if_false() const { return m_if_false; }
    void set_condition(NonnullRefPtr<Expression> condition) { m_condition = move(condition); }
    void set_if_true(NonnullRefPtrVector<ASTNode> if_true) { m_if_true = move(if_true); }
    void set_if_false(Optional<NonnullRefPtrVector<ASTNode>> if_false) { m_if_false = move(if_false); }

private:
    NonnullRefPtr<Expression> m_condition;
    NonnullRefPtrVector<ASTNode> m_if_true;
    Optional<NonnullRefPtrVector<ASTNode>> m_if_false;
};

class ReturnStatement : public Statement {
public:
    ReturnStatement(Position start, Position end, RefPtr<Expression> expression = nullptr)
        : Statement(start, end)
        , m_expression(move(expression))
    {
    }
    bool is_return_statement() const override { return true; }

    const RefPtr<ASTNode>& expression() const { return m_expression; }
    RefPtr<ASTNode>& expression() { return m_expression; }
    void set_expression(NonnullRefPtr<ASTNode> expression) { m_expression = move(expression); }

private:
    RefPtr<ASTNode> m_expression;
};

class Function : public RefCounted<Function> {
public:
    Function(Position start, Position end, NonnullRefPtr<Type> return_type, String& name, NonnullRefPtrVector<Variable> parameters, NonnullRefPtrVector<ASTNode> body)
        : m_return_type(move(return_type))
        , m_name(name)
        , m_parameters(move(parameters))
        , m_body(move(body))
        , m_start(start)
        , m_end(end)
    {
    }

    const String& name() const { return m_name; }
    void set_name(const String& name) { this->m_name = name; }
    const NonnullRefPtr<Type>& return_type() const { return m_return_type; }
    const NonnullRefPtrVector<Variable>& parameters() const { return m_parameters; }
    NonnullRefPtrVector<Variable>& parameters() { return m_parameters; }
    NonnullRefPtrVector<ASTNode>& body() { return m_body; }
    const NonnullRefPtrVector<ASTNode>& body() const { return m_body; }
    const Position& start() const { return m_start; }
    const Position& end() const { return m_end; }

private:
    NonnullRefPtr<Type> m_return_type;
    String m_name;
    NonnullRefPtrVector<Variable> m_parameters;
    NonnullRefPtrVector<ASTNode> m_body;
    Position m_start;
    Position m_end;
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

void run_intermediate_representation_passes(SIR::TranslationUnit&);

}
using SIR::create_ast_node;
