/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
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

#include <AK/FlyString.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibCpp/Lexer.h>

namespace Cpp {

class ASTNode;
class TranslationUnit;
class Declaration;
class FunctionDefinition;
class Type;
class Parameter;
class Statement;

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() = default;
    virtual const char* class_name() const = 0;
    virtual void dump(size_t indent) const;

    ASTNode* parent() const { return m_parent; }
    Position start() const
    {
        VERIFY(m_start.has_value());
        return m_start.value();
    }
    Position end() const
    {
        VERIFY(m_end.has_value());
        return m_end.value();
    }
    const FlyString& filename() const
    {
        return m_filename;
    }
    void set_end(const Position& end) { m_end = end; }
    void set_parent(ASTNode& parent) { m_parent = &parent; }

    virtual NonnullRefPtrVector<Declaration> declarations() const { return {}; }

    virtual bool is_identifier() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_variable_or_parameter_declaration() const { return false; }
    virtual bool is_function_call() const { return false; }
    virtual bool is_type() const { return false; }
    virtual bool is_declaration() const { return false; }

protected:
    ASTNode(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : m_parent(parent)
        , m_start(start)
        , m_end(end)
        , m_filename(filename)
    {
    }

private:
    ASTNode* m_parent { nullptr };
    Optional<Position> m_start;
    Optional<Position> m_end;
    FlyString m_filename;
};

class TranslationUnit : public ASTNode {

public:
    virtual ~TranslationUnit() override = default;
    virtual const char* class_name() const override { return "TranslationUnit"; }
    virtual void dump(size_t indent) const override;
    virtual NonnullRefPtrVector<Declaration> declarations() const override { return m_declarations; }

    TranslationUnit(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

    NonnullRefPtrVector<Declaration> m_declarations;
};

class Statement : public ASTNode {
public:
    virtual ~Statement() override = default;
    virtual const char* class_name() const override { return "Statement"; }

    virtual NonnullRefPtrVector<Declaration> declarations() const override;

protected:
    Statement(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : ASTNode(parent, start, end, filename)
    {
    }
};

class Declaration : public Statement {

public:
    virtual bool is_declaration() const override { return true; }
    virtual bool is_variable_declaration() const { return false; }
    virtual bool is_parameter() const { return false; }
    virtual bool is_struct_or_class() const { return false; }
    virtual bool is_struct() const { return false; }
    virtual bool is_class() const { return false; }
    virtual bool is_function() const { return false; }
    virtual bool is_namespace() const { return false; }
    const StringView& name() const { return m_name; }

    StringView m_name;

protected:
    Declaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class InvalidDeclaration : public Declaration {

public:
    virtual ~InvalidDeclaration() override = default;
    virtual const char* class_name() const override { return "InvalidDeclaration"; }
    InvalidDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Declaration(parent, start, end, filename)
    {
    }
};

class FunctionDeclaration : public Declaration {
public:
    virtual ~FunctionDeclaration() override = default;
    virtual const char* class_name() const override { return "FunctionDeclaration"; }
    virtual void dump(size_t indent) const override;
    virtual bool is_function() const override { return true; }
    RefPtr<FunctionDefinition> definition() { return m_definition; }

    FunctionDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    virtual NonnullRefPtrVector<Declaration> declarations() const override;

    Vector<StringView> m_qualifiers;
    RefPtr<Type> m_return_type;
    NonnullRefPtrVector<Parameter> m_parameters;
    RefPtr<FunctionDefinition> m_definition;
};

class VariableOrParameterDeclaration : public Declaration {
public:
    virtual ~VariableOrParameterDeclaration() override = default;
    virtual bool is_variable_or_parameter_declaration() const override { return true; }

    RefPtr<Type> m_type;

protected:
    VariableOrParameterDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Declaration(parent, start, end, filename)
    {
    }
};

class Parameter : public VariableOrParameterDeclaration {
public:
    virtual ~Parameter() override = default;
    virtual const char* class_name() const override { return "Parameter"; }
    virtual void dump(size_t indent) const override;

    Parameter(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename, StringView name)
        : VariableOrParameterDeclaration(parent, start, end, filename)
    {
        m_name = name;
    }

    virtual bool is_parameter() const override { return true; }

    bool m_is_ellipsis { false };
};

class Type : public ASTNode {
public:
    virtual ~Type() override = default;
    virtual const char* class_name() const override { return "Type"; }
    const StringView& name() const { return m_name; }
    virtual void dump(size_t indent) const override;
    virtual bool is_type() const override { return true; }

    Type(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename, StringView name)
        : ASTNode(parent, start, end, filename)
        , m_name(name)
    {
    }

    StringView m_name;
    Vector<StringView> m_qualifiers;
};

class Pointer : public Type {
public:
    virtual ~Pointer() override = default;
    virtual const char* class_name() const override { return "Pointer"; }
    virtual void dump(size_t indent) const override;

    Pointer(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Type(parent, start, end, filename, {})
    {
    }

    RefPtr<Type> m_pointee;
};

class FunctionDefinition : public ASTNode {
public:
    virtual ~FunctionDefinition() override = default;
    virtual const char* class_name() const override { return "FunctionDefinition"; }
    NonnullRefPtrVector<Statement>& statements() { return m_statements; }
    virtual void dump(size_t indent) const override;

    FunctionDefinition(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

    virtual NonnullRefPtrVector<Declaration> declarations() const override;

    NonnullRefPtrVector<Statement> m_statements;
};

class InvalidStatement : public Statement {
public:
    virtual ~InvalidStatement() override = default;
    virtual const char* class_name() const override { return "InvalidStatement"; }
    InvalidStatement(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class Expression : public Statement {
public:
    virtual ~Expression() override = default;
    virtual const char* class_name() const override { return "Expression"; }

protected:
    Expression(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class InvalidExpression : public Expression {
public:
    virtual ~InvalidExpression() override = default;
    virtual const char* class_name() const override { return "InvalidExpression"; }
    InvalidExpression(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }
};

class VariableDeclaration : public VariableOrParameterDeclaration {
public:
    virtual ~VariableDeclaration() override = default;
    virtual const char* class_name() const override { return "VariableDeclaration"; }
    virtual void dump(size_t indent) const override;

    VariableDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : VariableOrParameterDeclaration(parent, start, end, filename)
    {
    }

    virtual bool is_variable_declaration() const override { return true; }

    RefPtr<Expression> m_initial_value;
};

class Identifier : public Expression {
public:
    virtual ~Identifier() override = default;
    virtual const char* class_name() const override { return "Identifier"; }
    virtual void dump(size_t indent) const override;

    Identifier(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename, StringView name)
        : Expression(parent, start, end, filename)
        , m_name(name)
    {
    }
    Identifier(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Identifier(parent, start, end, filename, {})
    {
    }

    virtual bool is_identifier() const override { return true; }

    StringView m_name;
};

class NumericLiteral : public Expression {
public:
    virtual ~NumericLiteral() override = default;
    virtual const char* class_name() const override { return "NumricLiteral"; }
    virtual void dump(size_t indent) const override;

    NumericLiteral(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename, StringView value)
        : Expression(parent, start, end, filename)
        , m_value(value)
    {
    }

    StringView m_value;
};

class BooleanLiteral : public Expression {
public:
    virtual ~BooleanLiteral() override = default;
    virtual const char* class_name() const override { return "BooleanLiteral"; }
    virtual void dump(size_t indent) const override;

    BooleanLiteral(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename, bool value)
        : Expression(parent, start, end, filename)
        , m_value(value)
    {
    }

    bool m_value;
};

enum class BinaryOp {
    Addition,
    Subtraction,
    Multiplication,
    Division,
    Modulo,
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
    BinaryExpression(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~BinaryExpression() override = default;
    virtual const char* class_name() const override { return "BinaryExpression"; }
    virtual void dump(size_t indent) const override;

    BinaryOp m_op;
    RefPtr<Expression> m_lhs;
    RefPtr<Expression> m_rhs;
};

enum class AssignmentOp {
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
};

class AssignmentExpression : public Expression {
public:
    AssignmentExpression(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~AssignmentExpression() override = default;
    virtual const char* class_name() const override { return "AssignmentExpression"; }
    virtual void dump(size_t indent) const override;

    AssignmentOp m_op;
    RefPtr<Expression> m_lhs;
    RefPtr<Expression> m_rhs;
};

class FunctionCall final : public Expression {
public:
    FunctionCall(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }

    ~FunctionCall() override = default;
    virtual const char* class_name() const override { return "FunctionCall"; }
    virtual void dump(size_t indent) const override;
    virtual bool is_function_call() const override { return true; }

    StringView m_name;
    NonnullRefPtrVector<Expression> m_arguments;
};

class StringLiteral final : public Expression {
public:
    StringLiteral(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }

    ~StringLiteral() override = default;
    virtual const char* class_name() const override { return "StringLiteral"; }
    virtual void dump(size_t indent) const override;

    String m_value;
};

class ReturnStatement : public Statement {
public:
    virtual ~ReturnStatement() override = default;
    virtual const char* class_name() const override { return "ReturnStatement"; }

    ReturnStatement(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }
    virtual void dump(size_t indent) const override;

    RefPtr<Expression> m_value;
};

class EnumDeclaration : public Declaration {
public:
    virtual ~EnumDeclaration() override = default;
    virtual const char* class_name() const override { return "EnumDeclaration"; }
    virtual void dump(size_t indent) const override;

    EnumDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    StringView m_name;
    Vector<StringView> m_entries;
};

class MemberDeclaration : public Declaration {
public:
    virtual ~MemberDeclaration() override = default;
    virtual const char* class_name() const override { return "MemberDeclaration"; }
    virtual void dump(size_t indent) const override;

    MemberDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    RefPtr<Type> m_type;
    StringView m_name;
    RefPtr<Expression> m_initial_value;
};

class StructOrClassDeclaration : public Declaration {
public:
    virtual ~StructOrClassDeclaration() override = default;
    virtual const char* class_name() const override { return "StructOrClassDeclaration"; }
    virtual void dump(size_t indent) const override;
    virtual bool is_struct_or_class() const override { return true; }
    virtual bool is_struct() const override { return m_type == Type::Struct; }
    virtual bool is_class() const override { return m_type == Type::Class; }

    enum class Type {
        Struct,
        Class
    };

    StructOrClassDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename, StructOrClassDeclaration::Type type)
        : Declaration(parent, start, end, filename)
        , m_type(type)
    {
    }

    StructOrClassDeclaration::Type m_type;
    NonnullRefPtrVector<MemberDeclaration> m_members;
};

enum class UnaryOp {
    Invalid,
    BitwiseNot,
    Not,
    Plus,
    Minus,
    PlusPlus,
};

class UnaryExpression : public Expression {
public:
    UnaryExpression(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~UnaryExpression() override = default;
    virtual const char* class_name() const override { return "UnaryExpression"; }
    virtual void dump(size_t indent) const override;

    UnaryOp m_op;
    RefPtr<Expression> m_lhs;
};

class MemberExpression : public Expression {
public:
    MemberExpression(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~MemberExpression() override = default;
    virtual const char* class_name() const override { return "MemberExpression"; }
    virtual void dump(size_t indent) const override;
    virtual bool is_member_expression() const override { return true; }

    RefPtr<Expression> m_object;
    RefPtr<Identifier> m_property;
};

class ForStatement : public Statement {
public:
    ForStatement(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~ForStatement() override = default;
    virtual const char* class_name() const override { return "ForStatement"; }
    virtual void dump(size_t indent) const override;

    virtual NonnullRefPtrVector<Declaration> declarations() const override;

    RefPtr<VariableDeclaration> m_init;
    RefPtr<Expression> m_test;
    RefPtr<Expression> m_update;
    RefPtr<Statement> m_body;
};

class BlockStatement final : public Statement {
public:
    BlockStatement(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~BlockStatement() override = default;
    virtual const char* class_name() const override { return "BlockStatement"; }
    virtual void dump(size_t indent) const override;

    virtual NonnullRefPtrVector<Declaration> declarations() const override;

    NonnullRefPtrVector<Statement> m_statements;
};

class Comment final : public Statement {
public:
    Comment(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~Comment() override = default;
    virtual const char* class_name() const override { return "Comment"; }
};

class IfStatement : public Statement {
public:
    IfStatement(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~IfStatement() override = default;
    virtual const char* class_name() const override { return "IfStatement"; }
    virtual void dump(size_t indent) const override;
    virtual NonnullRefPtrVector<Declaration> declarations() const override;

    RefPtr<Expression> m_predicate;
    RefPtr<Statement> m_then;
    RefPtr<Statement> m_else;
};

class NamespaceDeclaration : public Declaration {
public:
    virtual ~NamespaceDeclaration() override = default;
    virtual const char* class_name() const override { return "NamespaceDeclaration"; }
    virtual void dump(size_t indent) const override;
    virtual bool is_namespace() const override { return true; }

    NamespaceDeclaration(ASTNode* parent, Optional<Position> start, Optional<Position> end, const String& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    virtual NonnullRefPtrVector<Declaration> declarations() const override { return m_declarations; }

    StringView m_name;
    NonnullRefPtrVector<Declaration> m_declarations;
};

}
