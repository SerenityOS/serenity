/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Value.h>
#include <LibJS/SourceRange.h>
#include <LibRegex/Regex.h>

namespace JS {

class VariableDeclaration;
class FunctionDeclaration;
class Identifier;

enum class FunctionKind {
    Generator,
    Regular,
};

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(SourceRange range, Args&&... args)
{
    return adopt_ref(*new T(range, forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() { }
    virtual Value execute(Interpreter&, GlobalObject&) const = 0;
    virtual void generate_bytecode(Bytecode::Generator&) const;
    virtual void dump(int indent) const;

    SourceRange const& source_range() const { return m_source_range; }
    SourceRange& source_range() { return m_source_range; }

    String class_name() const;

    template<typename T>
    bool fast_is() const = delete;

    virtual bool is_new_expression() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_super_expression() const { return false; }
    virtual bool is_expression_statement() const { return false; }
    virtual bool is_identifier() const { return false; }
    virtual bool is_scope_node() const { return false; }
    virtual bool is_program() const { return false; }

protected:
    explicit ASTNode(SourceRange source_range)
        : m_source_range(source_range)
    {
    }

private:
    SourceRange m_source_range;
};

class Statement : public ASTNode {
public:
    explicit Statement(SourceRange source_range)
        : ASTNode(source_range)
    {
    }

    FlyString const& label() const { return m_label; }
    void set_label(FlyString string) { m_label = move(string); }

protected:
    FlyString m_label;
};

class EmptyStatement final : public Statement {
public:
    explicit EmptyStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }
    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
    virtual void generate_bytecode(Bytecode::Generator&) const override;
};

class ErrorStatement final : public Statement {
public:
    explicit ErrorStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }
    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(SourceRange source_range, NonnullRefPtr<Expression> expression)
        : Statement(source_range)
        , m_expression(move(expression))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    Expression const& expression() const { return m_expression; };

private:
    virtual bool is_expression_statement() const override { return true; }

    NonnullRefPtr<Expression> m_expression;
};

class ScopeNode : public Statement {
public:
    template<typename T, typename... Args>
    T& append(SourceRange range, Args&&... args)
    {
        auto child = create_ast_node<T>(range, forward<Args>(args)...);
        m_children.append(move(child));
        return static_cast<T&>(m_children.last());
    }
    void append(NonnullRefPtr<Statement> child)
    {
        m_children.append(move(child));
    }

    NonnullRefPtrVector<Statement> const& children() const { return m_children; }
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    void add_variables(NonnullRefPtrVector<VariableDeclaration>);
    void add_functions(NonnullRefPtrVector<FunctionDeclaration>);
    void add_hoisted_function(NonnullRefPtr<FunctionDeclaration>);
    NonnullRefPtrVector<VariableDeclaration> const& variables() const { return m_variables; }
    NonnullRefPtrVector<FunctionDeclaration> const& functions() const { return m_functions; }
    NonnullRefPtrVector<FunctionDeclaration> const& hoisted_functions() const { return m_hoisted_functions; }

protected:
    explicit ScopeNode(SourceRange source_range)
        : Statement(source_range)
    {
    }

private:
    virtual bool is_scope_node() const final { return true; }

    NonnullRefPtrVector<Statement> m_children;
    NonnullRefPtrVector<VariableDeclaration> m_variables;
    NonnullRefPtrVector<FunctionDeclaration> m_functions;
    NonnullRefPtrVector<FunctionDeclaration> m_hoisted_functions;
};

class ImportStatement final : public Statement {
public:
    struct ImportEntry {
        String import_name;
        String local_name;
    };

    explicit ImportStatement(SourceRange source_range, StringView from_module, Vector<ImportEntry> entries = {})
        : Statement(source_range)
        , m_module_request(from_module)
        , m_entries(move(entries))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    virtual void dump(int indent) const override;

    bool has_bound_name(StringView name) const;

private:
    String m_module_request;
    Vector<ImportEntry> m_entries;
};

class ExportStatement final : public Statement {
public:
    struct ExportEntry {
        enum Kind {
            ModuleRequest,
            LocalExport
        } kind;
        // Can always have
        String export_name;

        // Only if module request
        String module_request;

        // Has just one of ones below
        String local_or_import_name;

        ExportEntry(String export_name, String local_name)
            : kind(LocalExport)
            , export_name(export_name)
            , local_or_import_name(local_name)
        {
        }
    };

    explicit ExportStatement(SourceRange source_range, RefPtr<ASTNode> statement, Vector<ExportEntry> entries)
        : Statement(source_range)
        , m_statement(move(statement))
        , m_entries(move(entries))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    virtual void dump(int indent) const override;

    bool has_export(StringView export_name) const;

private:
    RefPtr<ASTNode> m_statement;
    Vector<ExportEntry> m_entries;
};

class Program final : public ScopeNode {
public:
    enum class Type {
        Script,
        Module
    };

    explicit Program(SourceRange source_range, Type program_type)
        : ScopeNode(source_range)
        , m_type(program_type)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    bool is_strict_mode() const { return m_is_strict_mode; }
    void set_strict_mode() { m_is_strict_mode = true; }

    Type type() const { return m_type; }

    void append_import(NonnullRefPtr<ImportStatement> import_statement)
    {
        m_imports.append(import_statement);
        append(import_statement);
    }

    void append_export(NonnullRefPtr<ExportStatement> export_statement)
    {
        m_exports.append(export_statement);
        append(export_statement);
    }

    NonnullRefPtrVector<ImportStatement> const& imports() const { return m_imports; }
    NonnullRefPtrVector<ExportStatement> const& exports() const { return m_exports; }

private:
    virtual bool is_program() const override { return true; }

    bool m_is_strict_mode { false };
    Type m_type { Type::Script };

    NonnullRefPtrVector<ImportStatement> m_imports;
    NonnullRefPtrVector<ExportStatement> m_exports;
};

class BlockStatement final : public ScopeNode {
public:
    explicit BlockStatement(SourceRange source_range)
        : ScopeNode(source_range)
    {
    }
};

class Expression : public ASTNode {
public:
    explicit Expression(SourceRange source_range)
        : ASTNode(source_range)
    {
    }
    virtual Reference to_reference(Interpreter&, GlobalObject&) const;
};

class Declaration : public Statement {
public:
    explicit Declaration(SourceRange source_range)
        : Statement(source_range)
    {
    }
};

class ErrorDeclaration final : public Declaration {
public:
    explicit ErrorDeclaration(SourceRange source_range)
        : Declaration(source_range)
    {
    }
    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

struct BindingPattern : RefCounted<BindingPattern> {
    // This covers both BindingProperty and BindingElement, hence the more generic name
    struct BindingEntry {
        // If this entry represents a BindingElement, then name will be Empty
        Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<Expression>, Empty> name { Empty {} };
        Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>, Empty> alias { Empty {} };
        RefPtr<Expression> initializer {};
        bool is_rest { false };

        bool is_elision() const { return name.has<Empty>() && alias.has<Empty>(); }
    };

    enum class Kind {
        Array,
        Object,
    };

    void dump(int indent) const;

    template<typename C>
    void for_each_bound_name(C&& callback) const;

    Vector<BindingEntry> entries;
    Kind kind { Kind::Object };
};

class FunctionNode {
public:
    struct Parameter {
        Variant<FlyString, NonnullRefPtr<BindingPattern>> binding;
        RefPtr<Expression> default_value;
        bool is_rest { false };
    };

    FlyString const& name() const { return m_name; }
    Statement const& body() const { return *m_body; }
    Vector<Parameter> const& parameters() const { return m_parameters; };
    i32 function_length() const { return m_function_length; }
    bool is_strict_mode() const { return m_is_strict_mode; }
    bool is_arrow_function() const { return m_is_arrow_function; }
    FunctionKind kind() const { return m_kind; }

protected:
    FunctionNode(FlyString name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool is_arrow_function)
        : m_name(move(name))
        , m_body(move(body))
        , m_parameters(move(parameters))
        , m_function_length(function_length)
        , m_kind(kind)
        , m_is_strict_mode(is_strict_mode)
        , m_is_arrow_function(is_arrow_function)
    {
    }

    void dump(int indent, String const& class_name) const;

protected:
    void set_name(FlyString name)
    {
        VERIFY(m_name.is_empty());
        m_name = move(name);
    }

private:
    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    Vector<Parameter> const m_parameters;
    const i32 m_function_length;
    FunctionKind m_kind;
    bool m_is_strict_mode;
    bool m_is_arrow_function { false };
};

class FunctionDeclaration final
    : public Declaration
    , public FunctionNode {
public:
    static bool must_have_name() { return true; }

    FunctionDeclaration(SourceRange source_range, FlyString const& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode = false)
        : Declaration(source_range)
        , FunctionNode(name, move(body), move(parameters), function_length, kind, is_strict_mode, false)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;
};

class FunctionExpression final
    : public Expression
    , public FunctionNode {
public:
    static bool must_have_name() { return false; }

    FunctionExpression(SourceRange source_range, FlyString const& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, FunctionKind kind, bool is_strict_mode, bool is_arrow_function = false)
        : Expression(source_range)
        , FunctionNode(name, move(body), move(parameters), function_length, kind, is_strict_mode, is_arrow_function)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    void set_name_if_possible(FlyString new_name)
    {
        if (m_cannot_auto_rename)
            return;
        m_cannot_auto_rename = true;
        if (name().is_empty()) {
            set_name(move(new_name));
            m_is_auto_renamed = true;
        }
    }
    bool cannot_auto_rename() const { return m_cannot_auto_rename; }
    bool is_auto_renamed() const { return m_is_auto_renamed; }
    void set_cannot_auto_rename() { m_cannot_auto_rename = true; }

    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    bool m_cannot_auto_rename { false };
    bool m_is_auto_renamed { false };
};

class ErrorExpression final : public Expression {
public:
    explicit ErrorExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }

    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class YieldExpression final : public Expression {
public:
    explicit YieldExpression(SourceRange source_range, RefPtr<Expression> argument, bool is_yield_from)
        : Expression(source_range)
        , m_argument(move(argument))
        , m_is_yield_from(is_yield_from)
    {
    }

    Expression const* argument() const { return m_argument; }
    bool is_yield_from() const { return m_is_yield_from; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<Expression> m_argument;
    bool m_is_yield_from { false };
};

class ReturnStatement final : public Statement {
public:
    explicit ReturnStatement(SourceRange source_range, RefPtr<Expression> argument)
        : Statement(source_range)
        , m_argument(move(argument))
    {
    }

    Expression const* argument() const { return m_argument; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<Expression> m_argument;
};

class IfStatement final : public Statement {
public:
    IfStatement(SourceRange source_range, NonnullRefPtr<Expression> predicate, NonnullRefPtr<Statement> consequent, RefPtr<Statement> alternate)
        : Statement(source_range)
        , m_predicate(move(predicate))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    Expression const& predicate() const { return *m_predicate; }
    Statement const& consequent() const { return *m_consequent; }
    Statement const* alternate() const { return m_alternate; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_predicate;
    NonnullRefPtr<Statement> m_consequent;
    RefPtr<Statement> m_alternate;
};

class WhileStatement final : public Statement {
public:
    WhileStatement(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    Expression const& test() const { return *m_test; }
    Statement const& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class DoWhileStatement final : public Statement {
public:
    DoWhileStatement(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    Expression const& test() const { return *m_test; }
    Statement const& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class WithStatement final : public Statement {
public:
    WithStatement(SourceRange source_range, NonnullRefPtr<Expression> object, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_object(move(object))
        , m_body(move(body))
    {
    }

    Expression const& object() const { return *m_object; }
    Statement const& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_object;
    NonnullRefPtr<Statement> m_body;
};

class ForStatement final : public Statement {
public:
    ForStatement(SourceRange source_range, RefPtr<ASTNode> init, RefPtr<Expression> test, RefPtr<Expression> update, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_init(move(init))
        , m_test(move(test))
        , m_update(move(update))
        , m_body(move(body))
    {
    }

    ASTNode const* init() const { return m_init; }
    Expression const* test() const { return m_test; }
    Expression const* update() const { return m_update; }
    Statement const& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    RefPtr<ASTNode> m_init;
    RefPtr<Expression> m_test;
    RefPtr<Expression> m_update;
    NonnullRefPtr<Statement> m_body;
};

class ForInStatement final : public Statement {
public:
    ForInStatement(SourceRange source_range, NonnullRefPtr<ASTNode> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    ASTNode const& lhs() const { return *m_lhs; }
    Expression const& rhs() const { return *m_rhs; }
    Statement const& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<ASTNode> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
    NonnullRefPtr<Statement> m_body;
};

class ForOfStatement final : public Statement {
public:
    ForOfStatement(SourceRange source_range, NonnullRefPtr<ASTNode> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : Statement(source_range)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    ASTNode const& lhs() const { return *m_lhs; }
    Expression const& rhs() const { return *m_rhs; }
    Statement const& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<ASTNode> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
    NonnullRefPtr<Statement> m_body;
};

enum class BinaryOp {
    Addition,
    Subtraction,
    Multiplication,
    Division,
    Modulo,
    Exponentiation,
    TypedEquals,
    TypedInequals,
    AbstractEquals,
    AbstractInequals,
    GreaterThan,
    GreaterThanEquals,
    LessThan,
    LessThanEquals,
    BitwiseAnd,
    BitwiseOr,
    BitwiseXor,
    LeftShift,
    RightShift,
    UnsignedRightShift,
    In,
    InstanceOf,
};

class BinaryExpression final : public Expression {
public:
    BinaryExpression(SourceRange source_range, BinaryOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    BinaryOp m_op;
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class LogicalOp {
    And,
    Or,
    NullishCoalescing,
};

class LogicalExpression final : public Expression {
public:
    LogicalExpression(SourceRange source_range, LogicalOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    LogicalOp m_op;
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class UnaryOp {
    BitwiseNot,
    Not,
    Plus,
    Minus,
    Typeof,
    Void,
    Delete,
};

class UnaryExpression final : public Expression {
public:
    UnaryExpression(SourceRange source_range, UnaryOp op, NonnullRefPtr<Expression> lhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    UnaryOp m_op;
    NonnullRefPtr<Expression> m_lhs;
};

class SequenceExpression final : public Expression {
public:
    SequenceExpression(SourceRange source_range, NonnullRefPtrVector<Expression> expressions)
        : Expression(source_range)
        , m_expressions(move(expressions))
    {
        VERIFY(m_expressions.size() >= 2);
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtrVector<Expression> m_expressions;
};

class Literal : public Expression {
protected:
    explicit Literal(SourceRange source_range)
        : Expression(source_range)
    {
    }
};

class BooleanLiteral final : public Literal {
public:
    explicit BooleanLiteral(SourceRange source_range, bool value)
        : Literal(source_range)
        , m_value(value)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    bool m_value { false };
};

class NumericLiteral final : public Literal {
public:
    explicit NumericLiteral(SourceRange source_range, double value)
        : Literal(source_range)
        , m_value(value)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    Value m_value;
};

class BigIntLiteral final : public Literal {
public:
    explicit BigIntLiteral(SourceRange source_range, String value)
        : Literal(source_range)
        , m_value(move(value))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    String m_value;
};

class StringLiteral final : public Literal {
public:
    explicit StringLiteral(SourceRange source_range, String value, bool is_use_strict_directive = false)
        : Literal(source_range)
        , m_value(move(value))
        , m_is_use_strict_directive(is_use_strict_directive)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    StringView value() const { return m_value; }
    bool is_use_strict_directive() const { return m_is_use_strict_directive; };

private:
    String m_value;
    bool m_is_use_strict_directive;
};

class NullLiteral final : public Literal {
public:
    explicit NullLiteral(SourceRange source_range)
        : Literal(source_range)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;
};

class RegExpLiteral final : public Literal {
public:
    RegExpLiteral(SourceRange source_range, regex::Parser::Result parsed_regex, String parsed_pattern, regex::RegexOptions<ECMAScriptFlags> parsed_flags, String pattern, String flags)
        : Literal(source_range)
        , m_parsed_regex(move(parsed_regex))
        , m_parsed_pattern(move(parsed_pattern))
        , m_parsed_flags(move(parsed_flags))
        , m_pattern(move(pattern))
        , m_flags(move(flags))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    regex::Parser::Result const& parsed_regex() const { return m_parsed_regex; }
    String const& parsed_pattern() const { return m_parsed_pattern; }
    regex::RegexOptions<ECMAScriptFlags> const& parsed_flags() const { return m_parsed_flags; }
    String const& pattern() const { return m_pattern; }
    String const& flags() const { return m_flags; }

private:
    regex::Parser::Result m_parsed_regex;
    String m_parsed_pattern;
    regex::RegexOptions<ECMAScriptFlags> m_parsed_flags;
    String m_pattern;
    String m_flags;
};

class Identifier final : public Expression {
public:
    explicit Identifier(SourceRange source_range, FlyString string)
        : Expression(source_range)
        , m_string(move(string))
    {
    }

    FlyString const& string() const { return m_string; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Reference to_reference(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    virtual bool is_identifier() const override { return true; }

    FlyString m_string;
};

class ClassMethod final : public ASTNode {
public:
    enum class Kind {
        Method,
        Getter,
        Setter,
    };

    ClassMethod(SourceRange source_range, NonnullRefPtr<Expression> key, NonnullRefPtr<FunctionExpression> function, Kind kind, bool is_static)
        : ASTNode(source_range)
        , m_key(move(key))
        , m_function(move(function))
        , m_kind(kind)
        , m_is_static(is_static)
    {
    }

    Expression const& key() const { return *m_key; }
    Kind kind() const { return m_kind; }
    bool is_static() const { return m_is_static; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_key;
    NonnullRefPtr<FunctionExpression> m_function;
    Kind m_kind;
    bool m_is_static;
};

class ClassField final : public ASTNode {
public:
    ClassField(SourceRange source_range, NonnullRefPtr<Expression> key, RefPtr<Expression> init, bool is_static)
        : ASTNode(source_range)
        , m_key(move(key))
        , m_initializer(move(init))
        , m_is_static(is_static)
    {
    }

    Expression const& key() const { return *m_key; }
    bool is_static() const { return m_is_static; }
    RefPtr<Expression> const& initializer() const { return m_initializer; }
    RefPtr<Expression>& initializer() { return m_initializer; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_key;
    RefPtr<Expression> m_initializer;
    bool m_is_static;
};

class SuperExpression final : public Expression {
public:
    explicit SuperExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    virtual bool is_super_expression() const override { return true; }
};

class ClassExpression final : public Expression {
public:
    ClassExpression(SourceRange source_range, String name, RefPtr<FunctionExpression> constructor, RefPtr<Expression> super_class, NonnullRefPtrVector<ClassMethod> methods, NonnullRefPtrVector<ClassField> fields)
        : Expression(source_range)
        , m_name(move(name))
        , m_constructor(move(constructor))
        , m_super_class(move(super_class))
        , m_methods(move(methods))
        , m_fields(move(fields))
    {
    }

    StringView name() const { return m_name; }
    RefPtr<FunctionExpression> constructor() const { return m_constructor; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    String m_name;
    RefPtr<FunctionExpression> m_constructor;
    RefPtr<Expression> m_super_class;
    NonnullRefPtrVector<ClassMethod> m_methods;
    NonnullRefPtrVector<ClassField> m_fields;
};

class ClassDeclaration final : public Declaration {
public:
    ClassDeclaration(SourceRange source_range, NonnullRefPtr<ClassExpression> class_expression)
        : Declaration(source_range)
        , m_class_expression(move(class_expression))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<ClassExpression> m_class_expression;
};

class SpreadExpression final : public Expression {
public:
    explicit SpreadExpression(SourceRange source_range, NonnullRefPtr<Expression> target)
        : Expression(source_range)
        , m_target(move(target))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_target;
};

class ThisExpression final : public Expression {
public:
    explicit ThisExpression(SourceRange source_range)
        : Expression(source_range)
    {
    }
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
};

class CallExpression : public Expression {
public:
    struct Argument {
        NonnullRefPtr<Expression> value;
        bool is_spread;
    };

    CallExpression(SourceRange source_range, NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : Expression(source_range)
        , m_callee(move(callee))
        , m_arguments(move(arguments))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    Expression const& callee() const { return m_callee; }

protected:
    void throw_type_error_for_callee(Interpreter&, GlobalObject&, Value callee_value, StringView call_type) const;

    NonnullRefPtr<Expression> m_callee;
    Vector<Argument> const m_arguments;

private:
    struct ThisAndCallee {
        Value this_value;
        Value callee;
    };

    ThisAndCallee compute_this_and_callee(Interpreter&, GlobalObject&) const;
};

class NewExpression final : public CallExpression {
public:
    NewExpression(SourceRange source_range, NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : CallExpression(source_range, move(callee), move(arguments))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    virtual bool is_new_expression() const override { return true; }
};

class SuperCall final : public Expression {
public:
    SuperCall(SourceRange source_range, Vector<CallExpression::Argument> arguments)
        : Expression(source_range)
        , m_arguments(move(arguments))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Vector<CallExpression::Argument> const m_arguments;
};

enum class AssignmentOp {
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
    MultiplicationAssignment,
    DivisionAssignment,
    ModuloAssignment,
    ExponentiationAssignment,
    BitwiseAndAssignment,
    BitwiseOrAssignment,
    BitwiseXorAssignment,
    LeftShiftAssignment,
    RightShiftAssignment,
    UnsignedRightShiftAssignment,
    AndAssignment,
    OrAssignment,
    NullishAssignment,
};

class AssignmentExpression final : public Expression {
public:
    AssignmentExpression(SourceRange source_range, AssignmentOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    AssignmentExpression(SourceRange source_range, AssignmentOp op, NonnullRefPtr<BindingPattern> lhs, NonnullRefPtr<Expression> rhs)
        : Expression(source_range)
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    AssignmentOp m_op;
    Variant<NonnullRefPtr<Expression>, NonnullRefPtr<BindingPattern>> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class UpdateOp {
    Increment,
    Decrement,
};

class UpdateExpression final : public Expression {
public:
    UpdateExpression(SourceRange source_range, UpdateOp op, NonnullRefPtr<Expression> argument, bool prefixed = false)
        : Expression(source_range)
        , m_op(op)
        , m_argument(move(argument))
        , m_prefixed(prefixed)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    UpdateOp m_op;
    NonnullRefPtr<Expression> m_argument;
    bool m_prefixed;
};

enum class DeclarationKind {
    Var,
    Let,
    Const,
};

class VariableDeclarator final : public ASTNode {
public:
    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier> id)
        : ASTNode(source_range)
        , m_target(move(id))
    {
    }

    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier> target, RefPtr<Expression> init)
        : ASTNode(source_range)
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    VariableDeclarator(SourceRange source_range, Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> target, RefPtr<Expression> init)
        : ASTNode(source_range)
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    auto& target() const { return m_target; }
    Expression const* init() const { return m_init; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> m_target;
    RefPtr<Expression> m_init;
};

class VariableDeclaration final : public Declaration {
public:
    VariableDeclaration(SourceRange source_range, DeclarationKind declaration_kind, NonnullRefPtrVector<VariableDeclarator> declarations)
        : Declaration(source_range)
        , m_declaration_kind(declaration_kind)
        , m_declarations(move(declarations))
    {
    }

    DeclarationKind declaration_kind() const { return m_declaration_kind; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    NonnullRefPtrVector<VariableDeclarator> const& declarations() const { return m_declarations; }

private:
    DeclarationKind m_declaration_kind;
    NonnullRefPtrVector<VariableDeclarator> m_declarations;
};

class ObjectProperty final : public ASTNode {
public:
    enum class Type {
        KeyValue,
        Getter,
        Setter,
        Spread,
    };

    ObjectProperty(SourceRange source_range, NonnullRefPtr<Expression> key, RefPtr<Expression> value, Type property_type, bool is_method)
        : ASTNode(source_range)
        , m_key(move(key))
        , m_value(move(value))
        , m_property_type(property_type)
        , m_is_method(is_method)
    {
    }

    Expression const& key() const { return m_key; }
    Expression const& value() const
    {
        VERIFY(m_value);
        return *m_value;
    }

    Type type() const { return m_property_type; }
    bool is_method() const { return m_is_method; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtr<Expression> m_key;
    RefPtr<Expression> m_value;
    Type m_property_type;
    bool m_is_method { false };
};

class ObjectExpression final : public Expression {
public:
    explicit ObjectExpression(SourceRange source_range, NonnullRefPtrVector<ObjectProperty> properties = {}, Optional<SourceRange> first_invalid_property_range = {})
        : Expression(source_range)
        , m_properties(move(properties))
        , m_first_invalid_property_range(move(first_invalid_property_range))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    Optional<SourceRange> const& invalid_property_range() const { return m_first_invalid_property_range; }

private:
    NonnullRefPtrVector<ObjectProperty> m_properties;
    Optional<SourceRange> m_first_invalid_property_range;
};

class ArrayExpression final : public Expression {
public:
    ArrayExpression(SourceRange source_range, Vector<RefPtr<Expression>> elements)
        : Expression(source_range)
        , m_elements(move(elements))
    {
    }

    Vector<RefPtr<Expression>> const& elements() const { return m_elements; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    Vector<RefPtr<Expression>> m_elements;
};

class TemplateLiteral final : public Expression {
public:
    TemplateLiteral(SourceRange source_range, NonnullRefPtrVector<Expression> expressions)
        : Expression(source_range)
        , m_expressions(move(expressions))
    {
    }

    TemplateLiteral(SourceRange source_range, NonnullRefPtrVector<Expression> expressions, NonnullRefPtrVector<Expression> raw_strings)
        : Expression(source_range)
        , m_expressions(move(expressions))
        , m_raw_strings(move(raw_strings))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    NonnullRefPtrVector<Expression> const& expressions() const { return m_expressions; }
    NonnullRefPtrVector<Expression> const& raw_strings() const { return m_raw_strings; }

private:
    NonnullRefPtrVector<Expression> const m_expressions;
    NonnullRefPtrVector<Expression> const m_raw_strings;
};

class TaggedTemplateLiteral final : public Expression {
public:
    TaggedTemplateLiteral(SourceRange source_range, NonnullRefPtr<Expression> tag, NonnullRefPtr<TemplateLiteral> template_literal)
        : Expression(source_range)
        , m_tag(move(tag))
        , m_template_literal(move(template_literal))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> const m_tag;
    NonnullRefPtr<TemplateLiteral> const m_template_literal;
};

class MemberExpression final : public Expression {
public:
    MemberExpression(SourceRange source_range, NonnullRefPtr<Expression> object, NonnullRefPtr<Expression> property, bool computed = false)
        : Expression(source_range)
        , m_object(move(object))
        , m_property(move(property))
        , m_computed(computed)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Reference to_reference(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    bool is_computed() const { return m_computed; }
    Expression const& object() const { return *m_object; }
    Expression const& property() const { return *m_property; }

    PropertyName computed_property_name(Interpreter&, GlobalObject&) const;

    String to_string_approximation() const;

private:
    virtual bool is_member_expression() const override { return true; }

    NonnullRefPtr<Expression> m_object;
    NonnullRefPtr<Expression> m_property;
    bool m_computed { false };
};

class MetaProperty final : public Expression {
public:
    enum class Type {
        NewTarget,
        ImportMeta,
    };

    MetaProperty(SourceRange source_range, Type type)
        : Expression(source_range)
        , m_type(type)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Type m_type;
};

class ConditionalExpression final : public Expression {
public:
    ConditionalExpression(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Expression> consequent, NonnullRefPtr<Expression> alternate)
        : Expression(source_range)
        , m_test(move(test))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Expression> m_consequent;
    NonnullRefPtr<Expression> m_alternate;
};

class CatchClause final : public ASTNode {
public:
    CatchClause(SourceRange source_range, FlyString parameter, NonnullRefPtr<BlockStatement> body)
        : ASTNode(source_range)
        , m_parameter(move(parameter))
        , m_body(move(body))
    {
    }

    CatchClause(SourceRange source_range, NonnullRefPtr<BindingPattern> parameter, NonnullRefPtr<BlockStatement> body)
        : ASTNode(source_range)
        , m_parameter(move(parameter))
        , m_body(move(body))
    {
    }

    auto& parameter() const { return m_parameter; }
    BlockStatement const& body() const { return m_body; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    Variant<FlyString, NonnullRefPtr<BindingPattern>> m_parameter;
    NonnullRefPtr<BlockStatement> m_body;
};

class TryStatement final : public Statement {
public:
    TryStatement(SourceRange source_range, NonnullRefPtr<BlockStatement> block, RefPtr<CatchClause> handler, RefPtr<BlockStatement> finalizer)
        : Statement(source_range)
        , m_block(move(block))
        , m_handler(move(handler))
        , m_finalizer(move(finalizer))
    {
    }

    BlockStatement const& block() const { return m_block; }
    CatchClause const* handler() const { return m_handler; }
    BlockStatement const* finalizer() const { return m_finalizer; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<BlockStatement> m_block;
    RefPtr<CatchClause> m_handler;
    RefPtr<BlockStatement> m_finalizer;
};

class ThrowStatement final : public Statement {
public:
    explicit ThrowStatement(SourceRange source_range, NonnullRefPtr<Expression> argument)
        : Statement(source_range)
        , m_argument(move(argument))
    {
    }

    Expression const& argument() const { return m_argument; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_argument;
};

class SwitchCase final : public ASTNode {
public:
    SwitchCase(SourceRange source_range, RefPtr<Expression> test, NonnullRefPtrVector<Statement> consequent)
        : ASTNode(source_range)
        , m_test(move(test))
        , m_consequent(move(consequent))
    {
    }

    Expression const* test() const { return m_test; }
    NonnullRefPtrVector<Statement> const& consequent() const { return m_consequent; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    RefPtr<Expression> m_test;
    NonnullRefPtrVector<Statement> m_consequent;
};

class SwitchStatement final : public Statement {
public:
    SwitchStatement(SourceRange source_range, NonnullRefPtr<Expression> discriminant, NonnullRefPtrVector<SwitchCase> cases)
        : Statement(source_range)
        , m_discriminant(move(discriminant))
        , m_cases(move(cases))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    NonnullRefPtr<Expression> m_discriminant;
    NonnullRefPtrVector<SwitchCase> m_cases;
};

class BreakStatement final : public Statement {
public:
    BreakStatement(SourceRange source_range, FlyString target_label)
        : Statement(source_range)
        , m_target_label(move(target_label))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    FlyString const& target_label() const { return m_target_label; }
    virtual void generate_bytecode(Bytecode::Generator&) const override;

private:
    FlyString m_target_label;
};

class ContinueStatement final : public Statement {
public:
    ContinueStatement(SourceRange source_range, FlyString target_label)
        : Statement(source_range)
        , m_target_label(move(target_label))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;

    FlyString const& target_label() const { return m_target_label; }

private:
    FlyString m_target_label;
};

class DebuggerStatement final : public Statement {
public:
    explicit DebuggerStatement(SourceRange source_range)
        : Statement(source_range)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void generate_bytecode(Bytecode::Generator&) const override;
};

template<typename C>
void BindingPattern::for_each_bound_name(C&& callback) const
{
    for (auto& entry : entries) {
        auto& alias = entry.alias;
        if (alias.has<NonnullRefPtr<Identifier>>()) {
            callback(alias.get<NonnullRefPtr<Identifier>>()->string());
        } else if (alias.has<NonnullRefPtr<BindingPattern>>()) {
            alias.get<NonnullRefPtr<BindingPattern>>()->for_each_bound_name(forward<C>(callback));
        } else {
            auto& name = entry.name;
            if (name.has<NonnullRefPtr<Identifier>>())
                callback(name.get<NonnullRefPtr<Identifier>>()->string());
        }
    }
}

template<>
inline bool ASTNode::fast_is<NewExpression>() const { return is_new_expression(); }

template<>
inline bool ASTNode::fast_is<MemberExpression>() const { return is_member_expression(); }

template<>
inline bool ASTNode::fast_is<SuperExpression>() const { return is_super_expression(); }

template<>
inline bool ASTNode::fast_is<Identifier>() const { return is_identifier(); }

template<>
inline bool ASTNode::fast_is<ExpressionStatement>() const { return is_expression_statement(); }

template<>
inline bool ASTNode::fast_is<ScopeNode>() const { return is_scope_node(); }

template<>
inline bool ASTNode::fast_is<Program>() const { return is_program(); }

}
