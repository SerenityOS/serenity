/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

namespace JS {

class VariableDeclaration;
class FunctionDeclaration;
class Identifier;

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
    virtual void dump(int indent) const;

    const SourceRange& source_range() const { return m_source_range; }
    SourceRange& source_range() { return m_source_range; }

    String class_name() const;

protected:
    ASTNode(SourceRange source_range)
        : m_source_range(move(source_range))
    {
    }

private:
    SourceRange m_source_range;
};

class Statement : public ASTNode {
public:
    Statement(SourceRange source_range)
        : ASTNode(move(source_range))
    {
    }

    const FlyString& label() const { return m_label; }
    void set_label(FlyString string) { m_label = string; }

protected:
    FlyString m_label;
};

class EmptyStatement final : public Statement {
public:
    EmptyStatement(SourceRange source_range)
        : Statement(move(source_range))
    {
    }
    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class ErrorStatement final : public Statement {
public:
    ErrorStatement(SourceRange source_range)
        : Statement(move(source_range))
    {
    }
    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(SourceRange source_range, NonnullRefPtr<Expression> expression)
        : Statement(move(source_range))
        , m_expression(move(expression))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const Expression& expression() const { return m_expression; };

private:
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

    const NonnullRefPtrVector<Statement>& children() const { return m_children; }
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    void add_variables(NonnullRefPtrVector<VariableDeclaration>);
    void add_functions(NonnullRefPtrVector<FunctionDeclaration>);
    const NonnullRefPtrVector<VariableDeclaration>& variables() const { return m_variables; }
    const NonnullRefPtrVector<FunctionDeclaration>& functions() const { return m_functions; }

protected:
    ScopeNode(SourceRange source_range)
        : Statement(move(source_range))
    {
    }

private:
    NonnullRefPtrVector<Statement> m_children;
    NonnullRefPtrVector<VariableDeclaration> m_variables;
    NonnullRefPtrVector<FunctionDeclaration> m_functions;
};

class Program final : public ScopeNode {
public:
    Program(SourceRange source_range)
        : ScopeNode(move(source_range))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    bool is_strict_mode() const { return m_is_strict_mode; }
    void set_strict_mode() { m_is_strict_mode = true; }

private:
    bool m_is_strict_mode { false };
};

class BlockStatement final : public ScopeNode {
public:
    BlockStatement(SourceRange source_range)
        : ScopeNode(move(source_range))
    {
    }
};

class Expression : public ASTNode {
public:
    Expression(SourceRange source_range)
        : ASTNode(move(source_range))
    {
    }
    virtual Reference to_reference(Interpreter&, GlobalObject&) const;
};

class Declaration : public Statement {
public:
    Declaration(SourceRange source_range)
        : Statement(move(source_range))
    {
    }
};

class ErrorDeclaration final : public Declaration {
public:
    ErrorDeclaration(SourceRange source_range)
        : Declaration(move(source_range))
    {
    }
    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

struct BindingPattern : RefCounted<BindingPattern> {
    struct BindingProperty {
        RefPtr<Identifier> name;
        RefPtr<Identifier> alias;
        RefPtr<BindingPattern> pattern;
        RefPtr<Expression> initializer;
        bool is_rest { false };
    };

    enum class Kind {
        Array,
        Object,
    };

    void dump(int indent) const;
    template<typename C>
    void for_each_assigned_name(C&& callback) const;

    Vector<BindingProperty> properties;
    Kind kind { Kind::Object };
};

class FunctionNode {
public:
    struct Parameter {
        Variant<FlyString, NonnullRefPtr<BindingPattern>> binding;
        RefPtr<Expression> default_value;
        bool is_rest { false };
    };

    const FlyString& name() const { return m_name; }
    const Statement& body() const { return *m_body; }
    const Vector<Parameter>& parameters() const { return m_parameters; };
    i32 function_length() const { return m_function_length; }
    bool is_strict_mode() const { return m_is_strict_mode; }

protected:
    FunctionNode(const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, NonnullRefPtrVector<VariableDeclaration> variables, bool is_strict_mode)
        : m_name(name)
        , m_body(move(body))
        , m_parameters(move(parameters))
        , m_variables(move(variables))
        , m_function_length(function_length)
        , m_is_strict_mode(is_strict_mode)
    {
    }

    void dump(int indent, const String& class_name) const;

    const NonnullRefPtrVector<VariableDeclaration>& variables() const { return m_variables; }

protected:
    void set_name(FlyString name)
    {
        VERIFY(m_name.is_empty());
        m_name = move(name);
    }

private:
    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    const Vector<Parameter> m_parameters;
    NonnullRefPtrVector<VariableDeclaration> m_variables;
    const i32 m_function_length;
    bool m_is_strict_mode;
};

class FunctionDeclaration final
    : public Declaration
    , public FunctionNode {
public:
    static bool must_have_name() { return true; }

    FunctionDeclaration(SourceRange source_range, const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, NonnullRefPtrVector<VariableDeclaration> variables, bool is_strict_mode = false)
        : Declaration(move(source_range))
        , FunctionNode(name, move(body), move(parameters), function_length, move(variables), is_strict_mode)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
};

class FunctionExpression final
    : public Expression
    , public FunctionNode {
public:
    static bool must_have_name() { return false; }

    FunctionExpression(SourceRange source_range, const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, NonnullRefPtrVector<VariableDeclaration> variables, bool is_strict_mode, bool is_arrow_function = false)
        : Expression(source_range)
        , FunctionNode(name, move(body), move(parameters), function_length, move(variables), is_strict_mode)
        , m_is_arrow_function(is_arrow_function)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    void set_name_if_possible(FlyString new_name)
    {
        if (m_cannot_auto_rename)
            return;
        m_cannot_auto_rename = true;
        if (name().is_empty())
            set_name(move(new_name));
    }
    bool cannot_auto_rename() const { return m_cannot_auto_rename; }
    void set_cannot_auto_rename() { m_cannot_auto_rename = true; }

private:
    bool m_cannot_auto_rename { false };
    bool m_is_arrow_function { false };
};

class ErrorExpression final : public Expression {
public:
    explicit ErrorExpression(SourceRange source_range)
        : Expression(move(source_range))
    {
    }

    Value execute(Interpreter&, GlobalObject&) const override { return {}; }
};

class ReturnStatement final : public Statement {
public:
    explicit ReturnStatement(SourceRange source_range, RefPtr<Expression> argument)
        : Statement(move(source_range))
        , m_argument(move(argument))
    {
    }

    const Expression* argument() const { return m_argument; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    RefPtr<Expression> m_argument;
};

class IfStatement final : public Statement {
public:
    IfStatement(SourceRange source_range, NonnullRefPtr<Expression> predicate, NonnullRefPtr<Statement> consequent, RefPtr<Statement> alternate)
        : Statement(move(source_range))
        , m_predicate(move(predicate))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    const Expression& predicate() const { return *m_predicate; }
    const Statement& consequent() const { return *m_consequent; }
    const Statement* alternate() const { return m_alternate; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_predicate;
    NonnullRefPtr<Statement> m_consequent;
    RefPtr<Statement> m_alternate;
};

class WhileStatement final : public Statement {
public:
    WhileStatement(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : Statement(move(source_range))
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    const Expression& test() const { return *m_test; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class DoWhileStatement final : public Statement {
public:
    DoWhileStatement(SourceRange source_range, NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : Statement(move(source_range))
        , m_test(move(test))
        , m_body(move(body))
    {
    }

    const Expression& test() const { return *m_test; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class WithStatement final : public Statement {
public:
    WithStatement(SourceRange source_range, NonnullRefPtr<Expression> object, NonnullRefPtr<Statement> body)
        : Statement(move(source_range))
        , m_object(move(object))
        , m_body(move(body))
    {
    }

    const Expression& object() const { return *m_object; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_object;
    NonnullRefPtr<Statement> m_body;
};

class ForStatement final : public Statement {
public:
    ForStatement(SourceRange source_range, RefPtr<ASTNode> init, RefPtr<Expression> test, RefPtr<Expression> update, NonnullRefPtr<Statement> body)
        : Statement(move(source_range))
        , m_init(move(init))
        , m_test(move(test))
        , m_update(move(update))
        , m_body(move(body))
    {
    }

    const ASTNode* init() const { return m_init; }
    const Expression* test() const { return m_test; }
    const Expression* update() const { return m_update; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    RefPtr<ASTNode> m_init;
    RefPtr<Expression> m_test;
    RefPtr<Expression> m_update;
    NonnullRefPtr<Statement> m_body;
};

class ForInStatement final : public Statement {
public:
    ForInStatement(SourceRange source_range, NonnullRefPtr<ASTNode> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : Statement(move(source_range))
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    const ASTNode& lhs() const { return *m_lhs; }
    const Expression& rhs() const { return *m_rhs; }
    const Statement& body() const { return *m_body; }

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
        : Statement(move(source_range))
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
        , m_body(move(body))
    {
    }

    const ASTNode& lhs() const { return *m_lhs; }
    const Expression& rhs() const { return *m_rhs; }
    const Statement& body() const { return *m_body; }

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
        : Expression(move(source_range))
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

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
        : Expression(move(source_range))
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

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
        : Expression(move(source_range))
        , m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    UnaryOp m_op;
    NonnullRefPtr<Expression> m_lhs;
};

class SequenceExpression final : public Expression {
public:
    SequenceExpression(SourceRange source_range, NonnullRefPtrVector<Expression> expressions)
        : Expression(move(source_range))
        , m_expressions(move(expressions))
    {
        VERIFY(m_expressions.size() >= 2);
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtrVector<Expression> m_expressions;
};

class Literal : public Expression {
protected:
    explicit Literal(SourceRange source_range)
        : Expression(move(source_range))
    {
    }
};

class BooleanLiteral final : public Literal {
public:
    explicit BooleanLiteral(SourceRange source_range, bool value)
        : Literal(move(source_range))
        , m_value(value)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

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

private:
    Value m_value;
};

class BigIntLiteral final : public Literal {
public:
    explicit BigIntLiteral(SourceRange source_range, String value)
        : Literal(move(source_range))
        , m_value(move(value))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    String m_value;
};

class StringLiteral final : public Literal {
public:
    explicit StringLiteral(SourceRange source_range, String value, bool is_use_strict_directive = false)
        : Literal(move(source_range))
        , m_value(move(value))
        , m_is_use_strict_directive(is_use_strict_directive)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    StringView value() const { return m_value; }
    bool is_use_strict_directive() const { return m_is_use_strict_directive; };

private:
    String m_value;
    bool m_is_use_strict_directive;
};

class NullLiteral final : public Literal {
public:
    explicit NullLiteral(SourceRange source_range)
        : Literal(move(source_range))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
};

class RegExpLiteral final : public Literal {
public:
    explicit RegExpLiteral(SourceRange source_range, String pattern, String flags)
        : Literal(move(source_range))
        , m_pattern(move(pattern))
        , m_flags(move(flags))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const String& pattern() const { return m_pattern; }
    const String& flags() const { return m_flags; }

private:
    String m_pattern;
    String m_flags;
};

class Identifier final : public Expression {
public:
    explicit Identifier(SourceRange source_range, const FlyString& string)
        : Expression(move(source_range))
        , m_string(string)
    {
    }

    const FlyString& string() const { return m_string; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Reference to_reference(Interpreter&, GlobalObject&) const override;

private:
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
        : ASTNode(move(source_range))
        , m_key(move(key))
        , m_function(move(function))
        , m_kind(kind)
        , m_is_static(is_static)
    {
    }

    const Expression& key() const { return *m_key; }
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

class SuperExpression final : public Expression {
public:
    SuperExpression(SourceRange source_range)
        : Expression(move(source_range))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
};

class ClassExpression final : public Expression {
public:
    ClassExpression(SourceRange source_range, String name, RefPtr<FunctionExpression> constructor, RefPtr<Expression> super_class, NonnullRefPtrVector<ClassMethod> methods)
        : Expression(move(source_range))
        , m_name(move(name))
        , m_constructor(move(constructor))
        , m_super_class(move(super_class))
        , m_methods(move(methods))
    {
    }

    StringView name() const { return m_name; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    String m_name;
    RefPtr<FunctionExpression> m_constructor;
    RefPtr<Expression> m_super_class;
    NonnullRefPtrVector<ClassMethod> m_methods;
};

class ClassDeclaration final : public Declaration {
public:
    ClassDeclaration(SourceRange source_range, NonnullRefPtr<ClassExpression> class_expression)
        : Declaration(move(source_range))
        , m_class_expression(move(class_expression))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<ClassExpression> m_class_expression;
};

class SpreadExpression final : public Expression {
public:
    explicit SpreadExpression(SourceRange source_range, NonnullRefPtr<Expression> target)
        : Expression(move(source_range))
        , m_target(target)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_target;
};

class ThisExpression final : public Expression {
public:
    ThisExpression(SourceRange source_range)
        : Expression(move(source_range))
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
        : Expression(move(source_range))
        , m_callee(move(callee))
        , m_arguments(move(arguments))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    struct ThisAndCallee {
        Value this_value;
        Value callee;
    };
    ThisAndCallee compute_this_and_callee(Interpreter&, GlobalObject&) const;

    NonnullRefPtr<Expression> m_callee;
    const Vector<Argument> m_arguments;
};

class NewExpression final : public CallExpression {
public:
    NewExpression(SourceRange source_range, NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : CallExpression(move(source_range), move(callee), move(arguments))
    {
    }
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
        : Expression(move(source_range))
        , m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    AssignmentOp m_op;
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class UpdateOp {
    Increment,
    Decrement,
};

class UpdateExpression final : public Expression {
public:
    UpdateExpression(SourceRange source_range, UpdateOp op, NonnullRefPtr<Expression> argument, bool prefixed = false)
        : Expression(move(source_range))
        , m_op(op)
        , m_argument(move(argument))
        , m_prefixed(prefixed)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

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
        : ASTNode(move(source_range))
        , m_target(move(id))
    {
    }

    VariableDeclarator(SourceRange source_range, NonnullRefPtr<Identifier> target, RefPtr<Expression> init)
        : ASTNode(move(source_range))
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    VariableDeclarator(SourceRange source_range, Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> target, RefPtr<Expression> init)
        : ASTNode(move(source_range))
        , m_target(move(target))
        , m_init(move(init))
    {
    }

    auto& target() const { return m_target; }
    const Expression* init() const { return m_init; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Variant<NonnullRefPtr<Identifier>, NonnullRefPtr<BindingPattern>> m_target;
    RefPtr<Expression> m_init;
};

class VariableDeclaration final : public Declaration {
public:
    VariableDeclaration(SourceRange source_range, DeclarationKind declaration_kind, NonnullRefPtrVector<VariableDeclarator> declarations)
        : Declaration(move(source_range))
        , m_declaration_kind(declaration_kind)
        , m_declarations(move(declarations))
    {
    }

    DeclarationKind declaration_kind() const { return m_declaration_kind; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const NonnullRefPtrVector<VariableDeclarator>& declarations() const { return m_declarations; }

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
        : ASTNode(move(source_range))
        , m_key(move(key))
        , m_value(move(value))
        , m_property_type(property_type)
        , m_is_method(is_method)
    {
    }

    const Expression& key() const { return m_key; }
    const Expression& value() const
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
    ObjectExpression(SourceRange source_range, NonnullRefPtrVector<ObjectProperty> properties = {})
        : Expression(move(source_range))
        , m_properties(move(properties))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    NonnullRefPtrVector<ObjectProperty> m_properties;
};

class ArrayExpression final : public Expression {
public:
    ArrayExpression(SourceRange source_range, Vector<RefPtr<Expression>> elements)
        : Expression(move(source_range))
        , m_elements(move(elements))
    {
    }

    const Vector<RefPtr<Expression>>& elements() const { return m_elements; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    Vector<RefPtr<Expression>> m_elements;
};

class TemplateLiteral final : public Expression {
public:
    TemplateLiteral(SourceRange source_range, NonnullRefPtrVector<Expression> expressions)
        : Expression(move(source_range))
        , m_expressions(move(expressions))
    {
    }

    TemplateLiteral(SourceRange source_range, NonnullRefPtrVector<Expression> expressions, NonnullRefPtrVector<Expression> raw_strings)
        : Expression(move(source_range))
        , m_expressions(move(expressions))
        , m_raw_strings(move(raw_strings))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const NonnullRefPtrVector<Expression>& expressions() const { return m_expressions; }
    const NonnullRefPtrVector<Expression>& raw_strings() const { return m_raw_strings; }

private:
    const NonnullRefPtrVector<Expression> m_expressions;
    const NonnullRefPtrVector<Expression> m_raw_strings;
};

class TaggedTemplateLiteral final : public Expression {
public:
    TaggedTemplateLiteral(SourceRange source_range, NonnullRefPtr<Expression> tag, NonnullRefPtr<TemplateLiteral> template_literal)
        : Expression(move(source_range))
        , m_tag(move(tag))
        , m_template_literal(move(template_literal))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    const NonnullRefPtr<Expression> m_tag;
    const NonnullRefPtr<TemplateLiteral> m_template_literal;
};

class MemberExpression final : public Expression {
public:
    MemberExpression(SourceRange source_range, NonnullRefPtr<Expression> object, NonnullRefPtr<Expression> property, bool computed = false)
        : Expression(move(source_range))
        , m_object(move(object))
        , m_property(move(property))
        , m_computed(computed)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual Reference to_reference(Interpreter&, GlobalObject&) const override;

    bool is_computed() const { return m_computed; }
    const Expression& object() const { return *m_object; }
    const Expression& property() const { return *m_property; }

    PropertyName computed_property_name(Interpreter&, GlobalObject&) const;

    String to_string_approximation() const;

private:
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
        : Expression(move(source_range))
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
        : Expression(move(source_range))
        , m_test(move(test))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Expression> m_consequent;
    NonnullRefPtr<Expression> m_alternate;
};

class CatchClause final : public ASTNode {
public:
    CatchClause(SourceRange source_range, const FlyString& parameter, NonnullRefPtr<BlockStatement> body)
        : ASTNode(move(source_range))
        , m_parameter(parameter)
        , m_body(move(body))
    {
    }

    const FlyString& parameter() const { return m_parameter; }
    const BlockStatement& body() const { return m_body; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    FlyString m_parameter;
    NonnullRefPtr<BlockStatement> m_body;
};

class TryStatement final : public Statement {
public:
    TryStatement(SourceRange source_range, NonnullRefPtr<BlockStatement> block, RefPtr<CatchClause> handler, RefPtr<BlockStatement> finalizer)
        : Statement(move(source_range))
        , m_block(move(block))
        , m_handler(move(handler))
        , m_finalizer(move(finalizer))
    {
    }

    const BlockStatement& block() const { return m_block; }
    const CatchClause* handler() const { return m_handler; }
    const BlockStatement* finalizer() const { return m_finalizer; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtr<BlockStatement> m_block;
    RefPtr<CatchClause> m_handler;
    RefPtr<BlockStatement> m_finalizer;
};

class ThrowStatement final : public Statement {
public:
    explicit ThrowStatement(SourceRange source_range, NonnullRefPtr<Expression> argument)
        : Statement(move(source_range))
        , m_argument(move(argument))
    {
    }

    const Expression& argument() const { return m_argument; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtr<Expression> m_argument;
};

class SwitchCase final : public ASTNode {
public:
    SwitchCase(SourceRange source_range, RefPtr<Expression> test, NonnullRefPtrVector<Statement> consequent)
        : ASTNode(move(source_range))
        , m_test(move(test))
        , m_consequent(move(consequent))
    {
    }

    const Expression* test() const { return m_test; }
    const NonnullRefPtrVector<Statement>& consequent() const { return m_consequent; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    RefPtr<Expression> m_test;
    NonnullRefPtrVector<Statement> m_consequent;
};

class SwitchStatement final : public Statement {
public:
    SwitchStatement(SourceRange source_range, NonnullRefPtr<Expression> discriminant, NonnullRefPtrVector<SwitchCase> cases)
        : Statement(move(source_range))
        , m_discriminant(move(discriminant))
        , m_cases(move(cases))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    NonnullRefPtr<Expression> m_discriminant;
    NonnullRefPtrVector<SwitchCase> m_cases;
};

class BreakStatement final : public Statement {
public:
    BreakStatement(SourceRange source_range, FlyString target_label)
        : Statement(move(source_range))
        , m_target_label(target_label)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    const FlyString& target_label() const { return m_target_label; }

private:
    FlyString m_target_label;
};

class ContinueStatement final : public Statement {
public:
    ContinueStatement(SourceRange source_range, FlyString target_label)
        : Statement(move(source_range))
        , m_target_label(target_label)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    const FlyString& target_label() const { return m_target_label; }

private:
    FlyString m_target_label;
};

class DebuggerStatement final : public Statement {
public:
    DebuggerStatement(SourceRange source_range)
        : Statement(move(source_range))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
};

template<typename C>
void BindingPattern::for_each_assigned_name(C&& callback) const
{
    for (auto& property : properties) {
        if (property.name) {
            callback(property.name->string());
            continue;
        }
        property.pattern->template for_each_assigned_name(forward<C>(callback));
    }
}

}
