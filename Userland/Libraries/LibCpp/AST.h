/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/DeprecatedFlyString.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <AK/TypeCasts.h>
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
class Name;

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() = default;
    virtual StringView class_name() const = 0;
    virtual void dump(FILE* = stdout, size_t indent = 0) const;

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
    DeprecatedFlyString const& filename() const
    {
        return m_filename;
    }
    void set_end(Position const& end) { m_end = end; }
    void set_parent(ASTNode const& parent) { m_parent = &parent; }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const { return {}; }

    virtual bool is_identifier() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_variable_or_parameter_declaration() const { return false; }
    virtual bool is_function_call() const { return false; }
    virtual bool is_type() const { return false; }
    virtual bool is_declaration() const { return false; }
    virtual bool is_name() const { return false; }
    virtual bool is_dummy_node() const { return false; }

protected:
    ASTNode(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
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
    DeprecatedFlyString m_filename;
};

class TranslationUnit : public ASTNode {

public:
    virtual ~TranslationUnit() override = default;
    virtual StringView class_name() const override { return "TranslationUnit"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override { return m_declarations; }

    TranslationUnit(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
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
    Statement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
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
    virtual bool is_enum() const { return false; }
    bool is_member() const { return parent() != nullptr && parent()->is_declaration() && verify_cast<Declaration>(parent())->is_struct_or_class(); }
    Name const* name() const { return m_name; }
    StringView full_name() const;
    void set_name(RefPtr<Name const> name) { m_name = move(name); }

protected:
    Declaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    RefPtr<Name const> m_name;
    mutable Optional<ByteString> m_full_name;
};

class InvalidDeclaration : public Declaration {

public:
    virtual ~InvalidDeclaration() override = default;
    virtual StringView class_name() const override { return "InvalidDeclaration"sv; }
    InvalidDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }
};

class FunctionDeclaration : public Declaration {
public:
    virtual ~FunctionDeclaration() override = default;
    virtual StringView class_name() const override { return "FunctionDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_function() const override { return true; }
    virtual bool is_constructor() const { return false; }
    virtual bool is_destructor() const { return false; }
    RefPtr<FunctionDefinition const> definition() { return m_definition; }

    FunctionDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;
    Vector<StringView> const& qualifiers() const { return m_qualifiers; }
    void set_qualifiers(Vector<StringView> const& qualifiers) { m_qualifiers = qualifiers; }
    Type const* return_type() const { return m_return_type.ptr(); }
    void set_return_type(RefPtr<Type const> const& return_type) { m_return_type = return_type; }
    Vector<NonnullRefPtr<Parameter const>> const& parameters() const { return m_parameters; }
    void set_parameters(Vector<NonnullRefPtr<Parameter const>> const& parameters) { m_parameters = parameters; }
    FunctionDefinition const* definition() const { return m_definition.ptr(); }
    void set_definition(RefPtr<FunctionDefinition const>&& definition) { m_definition = move(definition); }

private:
    Vector<StringView> m_qualifiers;
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
    VariableOrParameterDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    RefPtr<Type const> m_type;
};

class Parameter : public VariableOrParameterDeclaration {
public:
    virtual ~Parameter() override = default;
    virtual StringView class_name() const override { return "Parameter"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_parameter() const override { return true; }

    Parameter(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename, RefPtr<Name const> name)
        : VariableOrParameterDeclaration(parent, start, end, filename)
    {
        m_name = name;
    }

    bool is_ellipsis() const { return m_is_ellipsis; }
    void set_ellipsis(bool is_ellipsis) { m_is_ellipsis = is_ellipsis; }

private:
    bool m_is_ellipsis { false };
};

class Type : public ASTNode {
public:
    virtual ~Type() override = default;
    virtual StringView class_name() const override { return "Type"sv; }
    virtual bool is_type() const override { return true; }
    virtual bool is_templatized() const { return false; }
    virtual bool is_named_type() const { return false; }
    virtual ByteString to_byte_string() const = 0;
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    bool is_auto() const { return m_is_auto; }
    void set_auto(bool is_auto) { m_is_auto = is_auto; }
    Vector<StringView> const& qualifiers() const { return m_qualifiers; }
    void set_qualifiers(Vector<StringView>&& qualifiers) { m_qualifiers = move(qualifiers); }

protected:
    Type(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

private:
    bool m_is_auto { false };
    Vector<StringView> m_qualifiers;
};

class NamedType : public Type {
public:
    virtual ~NamedType() override = default;
    virtual StringView class_name() const override { return "NamedType"sv; }
    virtual ByteString to_byte_string() const override;
    virtual bool is_named_type() const override { return true; }

    NamedType(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Type(parent, start, end, filename)
    {
    }

    Name const* name() const { return m_name.ptr(); }
    void set_name(RefPtr<Name const>&& name) { m_name = move(name); }

private:
    RefPtr<Name const> m_name;
};

class Pointer : public Type {
public:
    virtual ~Pointer() override = default;
    virtual StringView class_name() const override { return "Pointer"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual ByteString to_byte_string() const override;

    Pointer(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Type(parent, start, end, filename)
    {
    }

    Type const* pointee() const { return m_pointee.ptr(); }
    void set_pointee(RefPtr<Type const>&& pointee) { m_pointee = move(pointee); }

private:
    RefPtr<Type const> m_pointee;
};

class Reference : public Type {
public:
    virtual ~Reference() override = default;
    virtual StringView class_name() const override { return "Reference"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual ByteString to_byte_string() const override;

    enum class Kind {
        Lvalue,
        Rvalue,
    };

    Reference(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename, Kind kind)
        : Type(parent, start, end, filename)
        , m_kind(kind)
    {
    }

    Type const* referenced_type() const { return m_referenced_type.ptr(); }
    void set_referenced_type(RefPtr<Type const>&& pointee) { m_referenced_type = move(pointee); }
    Kind kind() const { return m_kind; }

private:
    RefPtr<Type const> m_referenced_type;
    Kind m_kind;
};

class FunctionType : public Type {
public:
    virtual ~FunctionType() override = default;
    virtual StringView class_name() const override { return "FunctionType"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual ByteString to_byte_string() const override;

    FunctionType(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Type(parent, start, end, filename)
    {
    }

    void set_return_type(Type& type) { m_return_type = type; }
    void set_parameters(Vector<NonnullRefPtr<Parameter const>> parameters) { m_parameters = move(parameters); }

private:
    RefPtr<Type const> m_return_type;
    Vector<NonnullRefPtr<Parameter const>> m_parameters;
};

class FunctionDefinition : public ASTNode {
public:
    virtual ~FunctionDefinition() override = default;
    virtual StringView class_name() const override { return "FunctionDefinition"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    FunctionDefinition(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;
    Vector<NonnullRefPtr<Statement const>> const& statements() const { return m_statements; }
    void add_statement(NonnullRefPtr<Statement const>&& statement) { m_statements.append(move(statement)); }

private:
    Vector<NonnullRefPtr<Statement const>> m_statements;
};

class InvalidStatement : public Statement {
public:
    virtual ~InvalidStatement() override = default;
    virtual StringView class_name() const override { return "InvalidStatement"sv; }
    InvalidStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class Expression : public Statement {
public:
    virtual ~Expression() override = default;
    virtual StringView class_name() const override { return "Expression"sv; }

protected:
    Expression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }
};

class InvalidExpression : public Expression {
public:
    virtual ~InvalidExpression() override = default;
    virtual StringView class_name() const override { return "InvalidExpression"sv; }
    InvalidExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }
};

class VariableDeclaration : public VariableOrParameterDeclaration {
public:
    virtual ~VariableDeclaration() override = default;
    virtual StringView class_name() const override { return "VariableDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    VariableDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : VariableOrParameterDeclaration(parent, start, end, filename)
    {
    }

    virtual bool is_variable_declaration() const override { return true; }

    Expression const* initial_value() const { return m_initial_value; }
    void set_initial_value(RefPtr<Expression const>&& initial_value) { m_initial_value = move(initial_value); }

private:
    RefPtr<Expression const> m_initial_value;
};

class Identifier : public Expression {
public:
    virtual ~Identifier() override = default;
    virtual StringView class_name() const override { return "Identifier"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    Identifier(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename, StringView name)
        : Expression(parent, start, end, filename)
        , m_name(name)
    {
    }
    Identifier(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Identifier(parent, start, end, filename, {})
    {
    }

    virtual bool is_identifier() const override { return true; }

    StringView name() const { return m_name; }
    void set_name(StringView&& name) { m_name = move(name); }

private:
    StringView m_name;
};

class Name : public Expression {
public:
    virtual ~Name() override = default;
    virtual StringView class_name() const override { return "Name"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_name() const override { return true; }
    virtual bool is_templatized() const { return false; }
    virtual bool is_sized() const { return false; }

    Name(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }
    virtual StringView full_name() const;

    Identifier const* name() const { return m_name.ptr(); }
    void set_name(RefPtr<Identifier const>&& name) { m_name = move(name); }
    Vector<NonnullRefPtr<Identifier const>> const& scope() const { return m_scope; }
    void set_scope(Vector<NonnullRefPtr<Identifier const>> scope) { m_scope = move(scope); }
    void add_to_scope(NonnullRefPtr<Identifier const>&& part) { m_scope.append(move(part)); }

private:
    RefPtr<Identifier const> m_name;
    Vector<NonnullRefPtr<Identifier const>> m_scope;
    mutable Optional<ByteString> m_full_name;
};

class SizedName : public Name {
public:
    virtual ~SizedName() override = default;
    virtual StringView class_name() const override { return "SizedName"sv; }
    virtual bool is_sized() const override { return true; }
    void dump(FILE* output, size_t indent) const override;

    SizedName(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Name(parent, start, end, filename)
    {
    }

    void append_dimension(StringView dim) { m_dimensions.append(dim); }

private:
    Vector<StringView> m_dimensions;
    mutable Optional<ByteString> m_full_name;
};

class TemplatizedName : public Name {
public:
    virtual ~TemplatizedName() override = default;
    virtual StringView class_name() const override { return "TemplatizedName"sv; }
    virtual bool is_templatized() const override { return true; }
    virtual StringView full_name() const override;

    TemplatizedName(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Name(parent, start, end, filename)
    {
    }

    void add_template_argument(NonnullRefPtr<Type const>&& type) { m_template_arguments.append(move(type)); }

private:
    Vector<NonnullRefPtr<Type const>> m_template_arguments;
    mutable Optional<ByteString> m_full_name;
};

class NumericLiteral : public Expression {
public:
    virtual ~NumericLiteral() override = default;
    virtual StringView class_name() const override { return "NumericLiteral"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    NumericLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename, StringView value)
        : Expression(parent, start, end, filename)
        , m_value(value)
    {
    }

    StringView value() const { return m_value; }

private:
    StringView m_value;
};

class NullPointerLiteral : public Expression {
public:
    virtual ~NullPointerLiteral() override = default;
    virtual StringView class_name() const override { return "NullPointerLiteral"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    NullPointerLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }
};

class BooleanLiteral : public Expression {
public:
    virtual ~BooleanLiteral() override = default;
    virtual StringView class_name() const override { return "BooleanLiteral"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    BooleanLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename, bool value)
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
    LogicalAnd,
    Arrow,
};

class BinaryExpression : public Expression {
public:
    BinaryExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~BinaryExpression() override = default;
    virtual StringView class_name() const override { return "BinaryExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

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

enum class AssignmentOp {
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
};

class AssignmentExpression : public Expression {
public:
    AssignmentExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~AssignmentExpression() override = default;
    virtual StringView class_name() const override { return "AssignmentExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    AssignmentOp op() const { return m_op; }
    void set_op(AssignmentOp op) { m_op = op; }
    Expression const* lhs() const { return m_lhs; }
    void set_lhs(RefPtr<Expression const>&& e) { m_lhs = move(e); }
    Expression const* rhs() const { return m_rhs; }
    void set_rhs(RefPtr<Expression const>&& e) { m_rhs = move(e); }

private:
    AssignmentOp m_op {};
    RefPtr<Expression const> m_lhs;
    RefPtr<Expression const> m_rhs;
};

class FunctionCall : public Expression {
public:
    FunctionCall(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~FunctionCall() override = default;
    virtual StringView class_name() const override { return "FunctionCall"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_function_call() const override { return true; }

    Expression const* callee() const { return m_callee.ptr(); }
    void set_callee(RefPtr<Expression const>&& callee) { m_callee = move(callee); }

    void add_argument(NonnullRefPtr<Expression const>&& arg) { m_arguments.append(move(arg)); }
    Vector<NonnullRefPtr<Expression const>> const& arguments() const { return m_arguments; }

private:
    RefPtr<Expression const> m_callee;
    Vector<NonnullRefPtr<Expression const>> m_arguments;
};

class StringLiteral final : public Expression {
public:
    StringLiteral(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    ~StringLiteral() override = default;
    virtual StringView class_name() const override { return "StringLiteral"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    ByteString const& value() const { return m_value; }
    void set_value(ByteString value) { m_value = move(value); }

private:
    ByteString m_value;
};

class ReturnStatement : public Statement {
public:
    virtual ~ReturnStatement() override = default;
    virtual StringView class_name() const override { return "ReturnStatement"sv; }

    ReturnStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    Expression const* value() const { return m_value.ptr(); }
    void set_value(RefPtr<Expression const>&& value) { m_value = move(value); }

private:
    RefPtr<Expression const> m_value;
};

class EnumDeclaration : public Declaration {
public:
    virtual ~EnumDeclaration() override = default;
    virtual StringView class_name() const override { return "EnumDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_enum() const override { return true; }

    EnumDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    enum class Type {
        RegularEnum,
        EnumClass
    };

    void set_type(Type type) { m_type = type; }
    void add_entry(StringView entry, RefPtr<Expression const> value = nullptr) { m_entries.append({ entry, move(value) }); }

private:
    Type m_type { Type::RegularEnum };
    struct EnumerationEntry {
        StringView name;
        RefPtr<Expression const> value;
    };
    Vector<EnumerationEntry> m_entries;
};

class StructOrClassDeclaration : public Declaration {
public:
    virtual ~StructOrClassDeclaration() override = default;
    virtual StringView class_name() const override { return "StructOrClassDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_struct_or_class() const override { return true; }
    virtual bool is_struct() const override { return m_type == Type::Struct; }
    virtual bool is_class() const override { return m_type == Type::Class; }
    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    enum class Type {
        Struct,
        Class
    };

    StructOrClassDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename, StructOrClassDeclaration::Type type)
        : Declaration(parent, start, end, filename)
        , m_type(type)
    {
    }

    Vector<NonnullRefPtr<Declaration const>> const& members() const { return m_members; }
    void set_members(Vector<NonnullRefPtr<Declaration const>>&& members) { m_members = move(members); }

    Vector<NonnullRefPtr<Name const>> const& baseclasses() const { return m_baseclasses; }
    void set_baseclasses(Vector<NonnullRefPtr<Name const>>&& baseclasses) { m_baseclasses = move(baseclasses); }

private:
    StructOrClassDeclaration::Type m_type;
    Vector<NonnullRefPtr<Declaration const>> m_members;
    Vector<NonnullRefPtr<Name const>> m_baseclasses;
};

enum class UnaryOp {
    Invalid,
    BitwiseNot,
    Not,
    Plus,
    Minus,
    PlusPlus,
    Address,
};

class UnaryExpression : public Expression {
public:
    UnaryExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~UnaryExpression() override = default;
    virtual StringView class_name() const override { return "UnaryExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    void set_op(UnaryOp op) { m_op = op; }
    void set_lhs(RefPtr<Expression const>&& e) { m_lhs = move(e); }

private:
    UnaryOp m_op;
    RefPtr<Expression const> m_lhs;
};

class MemberExpression : public Expression {
public:
    MemberExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~MemberExpression() override = default;
    virtual StringView class_name() const override { return "MemberExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_member_expression() const override { return true; }

    Expression const* object() const { return m_object.ptr(); }
    void set_object(RefPtr<Expression const>&& object) { m_object = move(object); }
    Expression const* property() const { return m_property.ptr(); }
    void set_property(RefPtr<Expression const>&& property) { m_property = move(property); }

private:
    RefPtr<Expression const> m_object;
    RefPtr<Expression const> m_property;
};

class ForStatement : public Statement {
public:
    ForStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~ForStatement() override = default;
    virtual StringView class_name() const override { return "ForStatement"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

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
    BlockStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~BlockStatement() override = default;
    virtual StringView class_name() const override { return "BlockStatement"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    void add_statement(NonnullRefPtr<Statement const>&& statement) { m_statements.append(move(statement)); }

    Vector<NonnullRefPtr<Statement const>> const& statements() const { return m_statements; }

private:
    Vector<NonnullRefPtr<Statement const>> m_statements;
};

class Comment final : public Statement {
public:
    Comment(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~Comment() override = default;
    virtual StringView class_name() const override { return "Comment"sv; }
};

class IfStatement : public Statement {
public:
    IfStatement(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Statement(parent, start, end, filename)
    {
    }

    virtual ~IfStatement() override = default;
    virtual StringView class_name() const override { return "IfStatement"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override;

    void set_predicate(RefPtr<Expression const>&& predicate) { m_predicate = move(predicate); }
    void set_then_statement(RefPtr<Statement const>&& then) { m_then = move(then); }
    void set_else_statement(RefPtr<Statement const>&& _else) { m_else = move(_else); }

    Expression const* predicate() const { return m_predicate.ptr(); }
    Statement const* then_statement() const { return m_then.ptr(); }
    Statement const* else_statement() const { return m_else.ptr(); }

private:
    RefPtr<Expression const> m_predicate;
    RefPtr<Statement const> m_then;
    RefPtr<Statement const> m_else;
};

class NamespaceDeclaration : public Declaration {
public:
    virtual ~NamespaceDeclaration() override = default;
    virtual StringView class_name() const override { return "NamespaceDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_namespace() const override { return true; }

    NamespaceDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    virtual Vector<NonnullRefPtr<Declaration const>> declarations() const override { return m_declarations; }
    void add_declaration(NonnullRefPtr<Declaration const>&& declaration) { m_declarations.append(move(declaration)); }

private:
    Vector<NonnullRefPtr<Declaration const>> m_declarations;
};

class CppCastExpression : public Expression {
public:
    CppCastExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~CppCastExpression() override = default;
    virtual StringView class_name() const override { return "CppCastExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    void set_cast_type(StringView cast_type) { m_cast_type = move(cast_type); }
    void set_type(NonnullRefPtr<Type const>&& type) { m_type = move(type); }
    void set_expression(NonnullRefPtr<Expression const>&& e) { m_expression = move(e); }

private:
    StringView m_cast_type;
    RefPtr<Type const> m_type;
    RefPtr<Expression const> m_expression;
};

class CStyleCastExpression : public Expression {
public:
    CStyleCastExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~CStyleCastExpression() override = default;
    virtual StringView class_name() const override { return "CStyleCastExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    void set_type(NonnullRefPtr<Type const>&& type) { m_type = move(type); }
    void set_expression(NonnullRefPtr<Expression const>&& e) { m_expression = move(e); }

private:
    RefPtr<Type const> m_type;
    RefPtr<Expression const> m_expression;
};

class SizeofExpression : public Expression {
public:
    SizeofExpression(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~SizeofExpression() override = default;
    virtual StringView class_name() const override { return "SizeofExpression"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    void set_type(RefPtr<Type const>&& type) { m_type = move(type); }

private:
    RefPtr<Type const> m_type;
};

class BracedInitList : public Expression {
public:
    BracedInitList(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Expression(parent, start, end, filename)
    {
    }

    virtual ~BracedInitList() override = default;
    virtual StringView class_name() const override { return "BracedInitList"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    void add_expression(NonnullRefPtr<Expression const>&& exp) { m_expressions.append(move(exp)); }

private:
    Vector<NonnullRefPtr<Expression const>> m_expressions;
};

class DummyAstNode : public ASTNode {
public:
    DummyAstNode(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : ASTNode(parent, start, end, filename)
    {
    }
    virtual bool is_dummy_node() const override { return true; }
    virtual StringView class_name() const override { return "DummyAstNode"sv; }
    virtual void dump(FILE* = stdout, size_t = 0) const override { }
};

class Constructor : public FunctionDeclaration {
public:
    virtual ~Constructor() override = default;
    virtual StringView class_name() const override { return "Constructor"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_constructor() const override { return true; }

    Constructor(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : FunctionDeclaration(parent, start, end, filename)
    {
    }
};

class Destructor : public FunctionDeclaration {
public:
    virtual ~Destructor() override = default;
    virtual StringView class_name() const override { return "Destructor"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;
    virtual bool is_destructor() const override { return true; }

    Destructor(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : FunctionDeclaration(parent, start, end, filename)
    {
    }
};

class UsingNamespaceDeclaration : public Declaration {
public:
    virtual ~UsingNamespaceDeclaration() override = default;
    virtual StringView class_name() const override { return "UsingNamespaceDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    UsingNamespaceDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }
};

class TypedefDeclaration : public Declaration {
public:
    virtual ~TypedefDeclaration() override = default;
    virtual StringView class_name() const override { return "TypedefDeclaration"sv; }
    virtual void dump(FILE* = stdout, size_t indent = 0) const override;

    TypedefDeclaration(ASTNode const* parent, Optional<Position> start, Optional<Position> end, ByteString const& filename)
        : Declaration(parent, start, end, filename)
    {
    }

    void set_alias(Type const& alias) { m_alias = alias; }
    Type const* alias() const { return m_alias.ptr(); }

private:
    RefPtr<Type const> m_alias;
};
template<>
inline bool ASTNode::fast_is<Identifier>() const { return is_identifier(); }
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
inline bool ASTNode::fast_is<StructOrClassDeclaration>() const { return is_declaration() && verify_cast<Declaration>(*this).is_struct_or_class(); }
template<>
inline bool ASTNode::fast_is<FunctionDeclaration>() const { return is_declaration() && verify_cast<Declaration>(*this).is_function(); }
template<>
inline bool ASTNode::fast_is<NamespaceDeclaration>() const { return is_declaration() && verify_cast<Declaration>(*this).is_namespace(); }
template<>
inline bool ASTNode::fast_is<Constructor>() const { return is_declaration() && verify_cast<Declaration>(*this).is_function() && verify_cast<FunctionDeclaration>(*this).is_constructor(); }
template<>
inline bool ASTNode::fast_is<Destructor>() const { return is_declaration() && verify_cast<Declaration>(*this).is_function() && verify_cast<FunctionDeclaration>(*this).is_destructor(); }
template<>
inline bool ASTNode::fast_is<NamedType>() const { return is_type() && verify_cast<Type>(*this).is_named_type(); }
template<>
inline bool ASTNode::fast_is<TemplatizedName>() const { return is_name() && verify_cast<Name>(*this).is_templatized(); }
template<>
inline bool ASTNode::fast_is<SizedName>() const { return is_name() && verify_cast<Name>(*this).is_sized(); }
}
