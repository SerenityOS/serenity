/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Volodymyr V. <vvmposeydon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/TypeCasts.h>
#include <AK/Vector.h>
#include <LibCore/File.h>
#include <LibGLSL/Lexer.h>

namespace GLSL {

class ASTNode;
class TranslationUnit;
class Declaration;
class FunctionDefinition;
class Type;
class Parameter;
class Statement;
class Name;

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() = default;
    virtual StringView class_name() const = 0;
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const;

    template<typename T>
    bool fast_is() const = delete;

    ASTNode const* parent() const { return m_parent; }
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
    FlyString const& filename() const
    {
        return m_filename;
    }
    void set_end(Position const& end) { m_end = end; }
    void set_parent(ASTNode const& parent) { m_parent = &parent; }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const { return {}; }

    virtual bool is_variable_or_parameter_declaration() const { return false; }
    virtual bool is_function_call() const { return false; }
    virtual bool is_type() const { return false; }
    virtual bool is_declaration() const { return false; }
    virtual bool is_name() const { return false; }
    virtual bool is_member_expression() const { return true; }
    virtual bool is_dummy_node() const { return false; }

protected:
    ASTNode(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : m_parent(parent)
        , m_start(start)
        , m_end(end)
        , m_filename(filename)
    {
    }

private:
    ASTNode const* m_parent { nullptr };
    Optional<Position> m_start;
    Optional<Position> m_end;
    FlyString m_filename;
};

class TranslationUnit : public ASTNode {

public:
    virtual ~TranslationUnit() override = default;
    virtual StringView class_name() const override { return "TranslationUnit"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override { return m_declarations; }

    TranslationUnit(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

    void set_declarations(Vector<NonnullRefPtr<Declaration const>>&& declarations) { m_declarations = move(declarations); }

private:
    Vector<NonnullRefPtr<Declaration const>> m_declarations;
};

class Statement : public ASTNode {
public:
    virtual ~Statement() override = default;
    virtual StringView class_name() const override { return "Statement"sv; }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

protected:
    Statement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }
};

class Declaration : public Statement {

public:
    virtual bool is_declaration() const override { return true; }
    virtual bool is_variable_declaration() const { return false; }
    virtual bool is_parameter() const { return false; }
    virtual bool is_struct() const { return false; }
    virtual bool is_function() const { return false; }
    Name const* name() const { return m_name; }
    void set_name(RefPtr<Name const> name) { m_name = move(name); }

protected:
    Declaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    RefPtr<Name const> m_name;
};

class InvalidDeclaration : public Declaration {

public:
    virtual ~InvalidDeclaration() override = default;
    virtual StringView class_name() const override { return "InvalidDeclaration"sv; }
    InvalidDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Declaration(parent, start, end, filename)
    {
    }
};

class FunctionDeclaration : public Declaration {
public:
    virtual ~FunctionDeclaration() override = default;
    virtual StringView class_name() const override { return "FunctionDeclaration"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual bool is_function() const override { return true; }
    RefPtr<FunctionDefinition const> definition() { return m_definition; }

    FunctionDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;
    Type const* return_type() const { return m_return_type.ptr(); }
    void set_return_type(RefPtr<Type const> const& return_type) { m_return_type = return_type; }
    Vector<NonnullRefPtr<Parameter const>> const& parameters() const { return m_parameters; }
    void set_parameters(Vector<NonnullRefPtr<Parameter const>> const& parameters) { m_parameters = parameters; }
    FunctionDefinition const* definition() const { return m_definition.ptr(); }
    void set_definition(RefPtr<FunctionDefinition const>&& definition) { m_definition = move(definition); }

private:
    RefPtr<Type const> m_return_type;
    Vector<NonnullRefPtr<Parameter const>> m_parameters;
    RefPtr<FunctionDefinition const> m_definition;
};

class VariableOrParameterDeclaration : public Declaration {
public:
    virtual ~VariableOrParameterDeclaration() override = default;
    virtual bool is_variable_or_parameter_declaration() const override { return true; }

    void set_type(RefPtr<Type const>&& type) { m_type = move(type); }
    Type const* type() const { return m_type.ptr(); }

protected:
    VariableOrParameterDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    RefPtr<Type const> m_type;
};

class Parameter : public VariableOrParameterDeclaration {
public:
    virtual ~Parameter() override = default;
    virtual StringView class_name() const override { return "Parameter"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual bool is_parameter() const override { return true; }

    Parameter(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename, RefPtr<Name const> name)
        : VariableOrParameterDeclaration(parent, start, end, filename)
    {
        m_name = name;
    }
};

enum class StorageTypeQualifier {
    Const,
    In,
    Out,
    Inout,
    Centroid,
    Patch,
    Sample,
    Uniform,
    Buffer,
    Shared,
    Coherent,
    Volatile,
    Restrict,
    Readonly,
    Writeonly,
    Subroutine,
};

class Type : public ASTNode {
public:
    virtual ~Type() override = default;
    virtual StringView class_name() const override { return "Type"sv; }
    virtual bool is_type() const override { return true; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    Type(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

    Name const* name() const { return m_name.ptr(); }
    void set_name(RefPtr<Name const>&& name) { m_name = move(name); }

    Vector<StorageTypeQualifier> const& storage_qualifiers() const { return m_storage_qualifiers; }
    void set_storage_qualifiers(Vector<StorageTypeQualifier>&& storage_qualifiers) { m_storage_qualifiers = move(storage_qualifiers); }

private:
    RefPtr<Name const> m_name;
    Vector<StorageTypeQualifier> m_storage_qualifiers;
};

class FunctionDefinition : public ASTNode {
public:
    virtual ~FunctionDefinition() override = default;
    virtual StringView class_name() const override { return "FunctionDefinition"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    FunctionDefinition(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;
    Vector<NonnullRefPtr<Statement const>> const& statements() { return m_statements; }
    void add_statement(NonnullRefPtr<Statement const>&& statement) { m_statements.append(move(statement)); }

private:
    Vector<NonnullRefPtr<Statement const>> m_statements;
};

class InvalidStatement : public Statement {
public:
    virtual ~InvalidStatement() override = default;
    virtual StringView class_name() const override { return "InvalidStatement"sv; }
    InvalidStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class Expression : public Statement {
public:
    virtual ~Expression() override = default;
    virtual StringView class_name() const override { return "Expression"sv; }

protected:
    Expression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class InvalidExpression : public Expression {
public:
    virtual ~InvalidExpression() override = default;
    virtual StringView class_name() const override { return "InvalidExpression"sv; }
    InvalidExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }
};

class VariableDeclaration : public VariableOrParameterDeclaration {
public:
    virtual ~VariableDeclaration() override = default;
    virtual StringView class_name() const override { return "VariableDeclaration"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    VariableDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : VariableOrParameterDeclaration(parent, start, end, filename)
    {
    }

    virtual bool is_variable_declaration() const override { return true; }

    Expression const* initial_value() const { return m_initial_value; }
    void set_initial_value(RefPtr<Expression const>&& initial_value) { m_initial_value = move(initial_value); }

private:
    RefPtr<Expression const> m_initial_value;
};

class Name : public Expression {
public:
    virtual ~Name() override = default;
    virtual StringView class_name() const override { return "Name"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual bool is_name() const override { return true; }
    virtual bool is_sized() const { return false; }

    Name(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    StringView name() const { return m_name; }
    void set_name(StringView name) { m_name = name; }

private:
    StringView m_name;
};

class SizedName : public Name {
public:
    virtual ~SizedName() override = default;
    virtual StringView class_name() const override { return "SizedName"sv; }
    virtual bool is_sized() const override { return true; }
    ErrorOr<void> dump(AK::Stream&, size_t indent) const override;

    SizedName(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Name(parent, start, end, filename)
    {
    }

    void append_dimension(StringView dim) { m_dimensions.append(dim); }

private:
    Vector<StringView> m_dimensions;
};

class NumericLiteral : public Expression {
public:
    virtual ~NumericLiteral() override = default;
    virtual StringView class_name() const override { return "NumericLiteral"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    NumericLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename, StringView value)
        : Expression(parent, start, end, filename)
        , m_value(value)
    {
    }

private:
    StringView m_value;
};

class BooleanLiteral : public Expression {
public:
    virtual ~BooleanLiteral() override = default;
    virtual StringView class_name() const override { return "BooleanLiteral"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    BooleanLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename, bool value)
        : Expression(parent, start, end, filename)
        , m_value(value)
    {
    }

private:
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
    EqualsEquals,
    NotEqual,
    LogicalOr,
    LogicalXor,
    LogicalAnd,
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
    MultiplicationAssignment,
    DivisionAssignment,
    ModuloAssignment,
    AndAssignment,
    OrAssignment,
    XorAssignment,
    LeftShiftAssignment,
    RightShiftAssignment,
};

class BinaryExpression : public Expression {
public:
    BinaryExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~BinaryExpression() override = default;
    virtual StringView class_name() const override { return "BinaryExpression"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    BinaryOp op() const { return m_op; }
    void set_op(BinaryOp op) { m_op = op; }
    Expression const* lhs() const { return m_lhs.ptr(); }
    void set_lhs(RefPtr<Expression const>&& e) { m_lhs = move(e); }
    Expression const* rhs() const { return m_rhs.ptr(); }
    void set_rhs(RefPtr<Expression const>&& e) { m_rhs = move(e); }

private:
    BinaryOp m_op;
    RefPtr<Expression const> m_lhs;
    RefPtr<Expression const> m_rhs;
};

class FunctionCall : public Expression {
public:
    FunctionCall(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~FunctionCall() override = default;
    virtual StringView class_name() const override { return "FunctionCall"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual bool is_function_call() const override { return true; }

    Expression const* callee() const { return m_callee; }
    void set_callee(RefPtr<Expression const>&& callee) { m_callee = move(callee); }

    void set_arguments(Vector<NonnullRefPtr<Expression const>>&& args) { m_arguments = move(args); }
    Vector<NonnullRefPtr<Expression const>> const& arguments() const { return m_arguments; }

private:
    RefPtr<Expression const> m_callee;
    Vector<NonnullRefPtr<Expression const>> m_arguments;
};

class StringLiteral final : public Expression {
public:
    StringLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    ~StringLiteral() override = default;
    virtual StringView class_name() const override { return "StringLiteral"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    String const& value() const { return m_value; }
    void set_value(String value) { m_value = move(value); }

private:
    String m_value;
};

class ReturnStatement : public Statement {
public:
    virtual ~ReturnStatement() override = default;
    virtual StringView class_name() const override { return "ReturnStatement"sv; }

    ReturnStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    Expression const* value() const { return m_value.ptr(); }
    void set_value(RefPtr<Expression const>&& value) { m_value = move(value); }

private:
    RefPtr<Expression const> m_value;
};

class DiscardStatement : public Statement {
public:
    virtual ~DiscardStatement() override = default;
    virtual StringView class_name() const override { return "DiscardStatement"sv; }

    DiscardStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class StructDeclaration : public Declaration {
public:
    virtual ~StructDeclaration() override = default;
    virtual StringView class_name() const override { return "StructDeclaration"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual bool is_struct() const override { return true; }
    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    StructDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    Vector<NonnullRefPtr<Declaration const>> const& members() const { return m_members; }
    void set_members(Vector<NonnullRefPtr<Declaration const>>&& members) { m_members = move(members); }

private:
    Vector<NonnullRefPtr<Declaration const>> m_members;
};

enum class UnaryOp {
    BitwiseNot,
    Not,
    Plus,
    Minus,
    PlusPlus,
    MinusMinus,
};

class UnaryExpression : public Expression {
public:
    UnaryExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~UnaryExpression() override = default;
    virtual StringView class_name() const override { return "UnaryExpression"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    UnaryOp op() const { return m_op; }
    void set_op(UnaryOp op) { m_op = op; }
    Expression const* lhs() const { return m_lhs.ptr(); }
    void set_lhs(RefPtr<Expression const>&& e) { m_lhs = move(e); }
    bool is_postfix() const { return m_is_postfix; }
    void set_is_postfix(bool is_postfix) { m_is_postfix = is_postfix; }

private:
    UnaryOp m_op;
    RefPtr<Expression const> m_lhs;
    bool m_is_postfix = false;
};

class MemberExpression : public Expression {
public:
    MemberExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~MemberExpression() override = default;
    virtual StringView class_name() const override { return "MemberExpression"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual bool is_member_expression() const override { return true; }

    Expression const* object() const { return m_object.ptr(); }
    void set_object(RefPtr<Expression const>&& object) { m_object = move(object); }
    Expression const* property() const { return m_property.ptr(); }
    void set_property(RefPtr<Expression const>&& property) { m_property = move(property); }

private:
    RefPtr<Expression const> m_object;
    RefPtr<Expression const> m_property;
};

class ArrayElementExpression : public Expression {
public:
    ArrayElementExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~ArrayElementExpression() override = default;
    virtual StringView class_name() const override { return "ArrayElementExpression"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    Expression const* array() const { return m_array.ptr(); }
    void set_array(RefPtr<Expression const>&& array) { m_array = move(array); }
    Expression const* index() const { return m_index.ptr(); }
    void set_index(RefPtr<Expression const>&& index) { m_index = move(index); }

private:
    RefPtr<Expression const> m_array;
    RefPtr<Expression const> m_index;
};

class ForStatement : public Statement {
public:
    ForStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~ForStatement() override = default;
    virtual StringView class_name() const override { return "ForStatement"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    void set_init(RefPtr<VariableDeclaration const>&& init) { m_init = move(init); }
    void set_test(RefPtr<Expression const>&& test) { m_test = move(test); }
    void set_update(RefPtr<Expression const>&& update) { m_update = move(update); }
    void set_body(RefPtr<Statement const>&& body) { m_body = move(body); }
    Statement const* body() const { return m_body.ptr(); }

private:
    RefPtr<VariableDeclaration const> m_init;
    RefPtr<Expression const> m_test;
    RefPtr<Expression const> m_update;
    RefPtr<Statement const> m_body;
};

class BlockStatement final : public Statement {
public:
    BlockStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~BlockStatement() override = default;
    virtual StringView class_name() const override { return "BlockStatement"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    void add_statement(NonnullRefPtr<Statement const>&& statement) { m_statements.append(move(statement)); }

private:
    Vector<NonnullRefPtr<Statement const>> m_statements;
};

class IfStatement : public Statement {
public:
    IfStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~IfStatement() override = default;
    virtual StringView class_name() const override { return "IfStatement"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t indent = 0) const override;
    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    void set_predicate(RefPtr<Expression const>&& predicate) { m_predicate = move(predicate); }
    void set_then_statement(RefPtr<Statement const>&& then) { m_then = move(then); }
    void set_else_statement(RefPtr<Statement const>&& _else) { m_else = move(_else); }

    Statement const* then_statement() const { return m_then.ptr(); }
    Statement const* else_statement() const { return m_else.ptr(); }

private:
    RefPtr<Expression const> m_predicate;
    RefPtr<Statement const> m_then;
    RefPtr<Statement const> m_else;
};

class DummyAstNode : public ASTNode {
public:
    DummyAstNode(ASTNode const* parent, Optional<Position> start, Optional<Position> end, String const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }
    virtual bool is_dummy_node() const override { return true; }
    virtual StringView class_name() const override { return "DummyAstNode"sv; }
    virtual ErrorOr<void> dump(AK::Stream&, size_t = 0) const override { return {}; }
};

template<>
inline bool ASTNode::fast_is<MemberExpression>() const { return is_member_expression(); }
template<>
inline bool ASTNode::fast_is<VariableOrParameterDeclaration>() const { return is_variable_or_parameter_declaration(); }
template<>
inline bool ASTNode::fast_is<FunctionCall>() const { return is_function_call(); }
template<>
inline bool ASTNode::fast_is<Type>() const { return is_type(); }
template<>
inline bool ASTNode::fast_is<Declaration>() const { return is_declaration(); }
template<>
inline bool ASTNode::fast_is<Name>() const { return is_name(); }
template<>
inline bool ASTNode::fast_is<DummyAstNode>() const { return is_dummy_node(); }

template<>
inline bool ASTNode::fast_is<VariableDeclaration>() const { return is_declaration() && verify_cast<Declaration>(*this).is_variable_declaration(); }
template<>
inline bool ASTNode::fast_is<FunctionDeclaration>() const { return is_declaration() && verify_cast<Declaration>(*this).is_function(); }

template<>
inline bool ASTNode::fast_is<SizedName>() const { return is_name() && verify_cast<Name>(*this).is_sized(); }
}
