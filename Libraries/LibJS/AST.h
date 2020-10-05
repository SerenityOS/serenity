/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Linus Groh <mail@linusgroh.de>
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
#include <AK/HashMap.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibJS/Forward.h>
#include <LibJS/Runtime/PropertyName.h>
#include <LibJS/Runtime/Value.h>

namespace JS {

class VariableDeclaration;
class FunctionDeclaration;

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(Args&&... args)
{
    return adopt(*new T(forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() { }
    virtual const char* class_name() const = 0;
    virtual Value execute(Interpreter&, GlobalObject&) const = 0;
    virtual void dump(int indent) const;
    virtual bool is_identifier() const { return false; }
    virtual bool is_spread_expression() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_scope_node() const { return false; }
    virtual bool is_program() const { return false; }
    virtual bool is_variable_declaration() const { return false; }
    virtual bool is_call_expression() const { return false; }
    virtual bool is_new_expression() const { return false; }
    virtual bool is_super_expression() const { return false; }

protected:
    ASTNode() { }

private:
};

class Statement : public ASTNode {
public:
    const FlyString& label() const { return m_label; }
    void set_label(FlyString string) { m_label = string; }

protected:
    FlyString m_label;
};

class EmptyStatement final : public Statement {
public:
    Value execute(Interpreter&, GlobalObject&) const override { return js_undefined(); }
    const char* class_name() const override { return "EmptyStatement"; }
};

class ErrorStatement final : public Statement {
public:
    Value execute(Interpreter&, GlobalObject&) const override { return js_undefined(); }
    const char* class_name() const override { return "ErrorStatement"; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(NonnullRefPtr<Expression> expression)
        : m_expression(move(expression))
    {
    }

    Value execute(Interpreter&, GlobalObject&) const override;
    const char* class_name() const override { return "ExpressionStatement"; }
    virtual void dump(int indent) const override;

private:
    NonnullRefPtr<Expression> m_expression;
};

class ScopeNode : public Statement {
public:
    template<typename T, typename... Args>
    T& append(Args&&... args)
    {
        auto child = create_ast_node<T>(forward<Args>(args)...);
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
    ScopeNode() { }

private:
    virtual bool is_scope_node() const final { return true; }
    NonnullRefPtrVector<Statement> m_children;
    NonnullRefPtrVector<VariableDeclaration> m_variables;
    NonnullRefPtrVector<FunctionDeclaration> m_functions;
};

class Program final : public ScopeNode {
public:
    Program() { }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    bool is_strict_mode() const { return m_is_strict_mode; }
    void set_strict_mode() { m_is_strict_mode = true; }

private:
    bool m_is_strict_mode { false };

    virtual bool is_program() const override { return true; }
    virtual const char* class_name() const override { return "Program"; }
};

class BlockStatement final : public ScopeNode {
public:
    BlockStatement() { }

private:
    virtual const char* class_name() const override { return "BlockStatement"; }
};

class Expression : public ASTNode {
public:
    virtual Reference to_reference(Interpreter&, GlobalObject&) const;
};

class Declaration : public Statement {
};

class FunctionNode {
public:
    struct Parameter {
        FlyString name;
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

    void dump(int indent, const char* class_name) const;

    const NonnullRefPtrVector<VariableDeclaration>& variables() const { return m_variables; }

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

    FunctionDeclaration(const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, NonnullRefPtrVector<VariableDeclaration> variables, bool is_strict_mode = false)
        : FunctionNode(name, move(body), move(parameters), function_length, move(variables), is_strict_mode)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "FunctionDeclaration"; }
};

class FunctionExpression final
    : public Expression
    , public FunctionNode {
public:
    static bool must_have_name() { return false; }

    FunctionExpression(const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, i32 function_length, NonnullRefPtrVector<VariableDeclaration> variables, bool is_strict_mode, bool is_arrow_function = false)
        : FunctionNode(name, move(body), move(parameters), function_length, move(variables), is_strict_mode)
        , m_is_arrow_function(is_arrow_function)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "FunctionExpression"; }

    bool m_is_arrow_function;
};

class ErrorExpression final : public Expression {
public:
    Value execute(Interpreter&, GlobalObject&) const override { return js_undefined(); }
    const char* class_name() const override { return "ErrorExpression"; }
};

class ReturnStatement final : public Statement {
public:
    explicit ReturnStatement(RefPtr<Expression> argument)
        : m_argument(move(argument))
    {
    }

    const Expression* argument() const { return m_argument; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ReturnStatement"; }

    RefPtr<Expression> m_argument;
};

class IfStatement final : public Statement {
public:
    IfStatement(NonnullRefPtr<Expression> predicate, NonnullRefPtr<Statement> consequent, RefPtr<Statement> alternate)
        : m_predicate(move(predicate))
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
    virtual const char* class_name() const override { return "IfStatement"; }

    NonnullRefPtr<Expression> m_predicate;
    NonnullRefPtr<Statement> m_consequent;
    RefPtr<Statement> m_alternate;
};

class WhileStatement final : public Statement {
public:
    WhileStatement(NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : m_test(move(test))
        , m_body(move(body))
    {
    }

    const Expression& test() const { return *m_test; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "WhileStatement"; }

    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class DoWhileStatement final : public Statement {
public:
    DoWhileStatement(NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : m_test(move(test))
        , m_body(move(body))
    {
    }

    const Expression& test() const { return *m_test; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "DoWhileStatement"; }

    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class ForStatement final : public Statement {
public:
    ForStatement(RefPtr<ASTNode> init, RefPtr<Expression> test, RefPtr<Expression> update, NonnullRefPtr<Statement> body)
        : m_init(move(init))
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
    virtual const char* class_name() const override { return "ForStatement"; }

    RefPtr<ASTNode> m_init;
    RefPtr<Expression> m_test;
    RefPtr<Expression> m_update;
    NonnullRefPtr<Statement> m_body;
};

class ForInStatement final : public Statement {
public:
    ForInStatement(NonnullRefPtr<ASTNode> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : m_lhs(move(lhs))
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
    virtual const char* class_name() const override { return "ForInStatement"; }

    NonnullRefPtr<ASTNode> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
    NonnullRefPtr<Statement> m_body;
};

class ForOfStatement final : public Statement {
public:
    ForOfStatement(NonnullRefPtr<ASTNode> lhs, NonnullRefPtr<Expression> rhs, NonnullRefPtr<Statement> body)
        : m_lhs(move(lhs))
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
    virtual const char* class_name() const override { return "ForOfStatement"; }

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
    BinaryExpression(BinaryOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "BinaryExpression"; }

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
    LogicalExpression(LogicalOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "LogicalExpression"; }

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
    UnaryExpression(UnaryOp op, NonnullRefPtr<Expression> lhs)
        : m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "UnaryExpression"; }

    UnaryOp m_op;
    NonnullRefPtr<Expression> m_lhs;
};

class SequenceExpression final : public Expression {
public:
    SequenceExpression(NonnullRefPtrVector<Expression> expressions)
        : m_expressions(move(expressions))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "SequenceExpression"; }

    NonnullRefPtrVector<Expression> m_expressions;
};

class Literal : public Expression {
protected:
    explicit Literal() { }
};

class BooleanLiteral final : public Literal {
public:
    explicit BooleanLiteral(bool value)
        : m_value(value)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
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

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "NumericLiteral"; }

    double m_value { 0 };
};

class BigIntLiteral final : public Literal {
public:
    explicit BigIntLiteral(String value)
        : m_value(move(value))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "BigIntLiteral"; }

    String m_value;
};

class StringLiteral final : public Literal {
public:
    explicit StringLiteral(String value)
        : m_value(move(value))
    {
    }

    StringView value() const { return m_value; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "StringLiteral"; }

    String m_value;
};

class NullLiteral final : public Literal {
public:
    explicit NullLiteral() { }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "NullLiteral"; }
};

class RegExpLiteral final : public Literal {
public:
    explicit RegExpLiteral(String content, String flags)
        : m_content(content)
        , m_flags(flags)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const String& content() const { return m_content; }
    const String& flags() const { return m_flags; }

private:
    virtual const char* class_name() const override { return "RegexLiteral"; }

    String m_content;
    String m_flags;
};

class Identifier final : public Expression {
public:
    explicit Identifier(const FlyString& string)
        : m_string(string)
    {
    }

    const FlyString& string() const { return m_string; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual bool is_identifier() const override { return true; }
    virtual Reference to_reference(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "Identifier"; }

    FlyString m_string;
};

class ClassMethod final : public ASTNode {
public:
    enum class Kind {
        Method,
        Getter,
        Setter,
    };

    ClassMethod(NonnullRefPtr<Expression> key, NonnullRefPtr<FunctionExpression> function, Kind kind, bool is_static)
        : m_key(move(key))
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
    virtual const char* class_name() const override { return "ClassMethod"; }

    NonnullRefPtr<Expression> m_key;
    NonnullRefPtr<FunctionExpression> m_function;
    Kind m_kind;
    bool m_is_static;
};

class SuperExpression final : public Expression {
public:
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual bool is_super_expression() const override { return true; }
    virtual const char* class_name() const override { return "SuperExpression"; }
};

class ClassExpression final : public Expression {
public:
    ClassExpression(String name, RefPtr<FunctionExpression> constructor, RefPtr<Expression> super_class, NonnullRefPtrVector<ClassMethod> methods)
        : m_name(move(name))
        , m_constructor(move(constructor))
        , m_super_class(move(super_class))
        , m_methods(move(methods))
    {
    }

    StringView name() const { return m_name; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ClassExpression"; }

    String m_name;
    RefPtr<FunctionExpression> m_constructor;
    RefPtr<Expression> m_super_class;
    NonnullRefPtrVector<ClassMethod> m_methods;
};

class ClassDeclaration final : public Declaration {
public:
    ClassDeclaration(NonnullRefPtr<ClassExpression> class_expression)
        : m_class_expression(move(class_expression))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ClassDeclaration"; }
    NonnullRefPtr<ClassExpression> m_class_expression;
};

class SpreadExpression final : public Expression {
public:
    explicit SpreadExpression(NonnullRefPtr<Expression> target)
        : m_target(target)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;
    virtual bool is_spread_expression() const override { return true; }

private:
    virtual const char* class_name() const override { return "SpreadExpression"; }

    NonnullRefPtr<Expression> m_target;
};

class ThisExpression final : public Expression {
public:
    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ThisExpression"; }
};

class CallExpression : public Expression {
public:
    struct Argument {
        NonnullRefPtr<Expression> value;
        bool is_spread;
    };

    CallExpression(NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : m_callee(move(callee))
        , m_arguments(move(arguments))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "CallExpression"; }
    virtual bool is_call_expression() const override { return true; }

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
    NewExpression(NonnullRefPtr<Expression> callee, Vector<Argument> arguments = {})
        : CallExpression(move(callee), move(arguments))
    {
    }

private:
    virtual const char* class_name() const override { return "NewExpression"; }
    virtual bool is_call_expression() const override { return false; }
    virtual bool is_new_expression() const override { return true; }
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
    AssignmentExpression(AssignmentOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
        : m_op(op)
        , m_lhs(move(lhs))
        , m_rhs(move(rhs))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "AssignmentExpression"; }

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
    UpdateExpression(UpdateOp op, NonnullRefPtr<Expression> argument, bool prefixed = false)
        : m_op(op)
        , m_argument(move(argument))
        , m_prefixed(prefixed)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "UpdateExpression"; }

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
    VariableDeclarator(NonnullRefPtr<Identifier> id)
        : m_id(move(id))
    {
    }

    VariableDeclarator(NonnullRefPtr<Identifier> id, RefPtr<Expression> init)
        : m_id(move(id))
        , m_init(move(init))
    {
    }

    const Identifier& id() const { return m_id; }
    const Expression* init() const { return m_init; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "VariableDeclarator"; }

    NonnullRefPtr<Identifier> m_id;
    RefPtr<Expression> m_init;
};

class VariableDeclaration final : public Declaration {
public:
    VariableDeclaration(DeclarationKind declaration_kind, NonnullRefPtrVector<VariableDeclarator> declarations)
        : m_declaration_kind(declaration_kind)
        , m_declarations(move(declarations))
    {
    }

    virtual bool is_variable_declaration() const override { return true; }
    DeclarationKind declaration_kind() const { return m_declaration_kind; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const NonnullRefPtrVector<VariableDeclarator>& declarations() const { return m_declarations; }

private:
    virtual const char* class_name() const override { return "VariableDeclaration"; }

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

    ObjectProperty(NonnullRefPtr<Expression> key, RefPtr<Expression> value, Type property_type, bool is_method)
        : m_key(move(key))
        , m_value(move(value))
        , m_property_type(property_type)
        , m_is_method(is_method)
    {
    }

    const Expression& key() const { return m_key; }
    const Expression& value() const
    {
        ASSERT(m_value);
        return *m_value;
    }

    Type type() const { return m_property_type; }
    bool is_method() const { return m_is_method; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "ObjectProperty"; }

    NonnullRefPtr<Expression> m_key;
    RefPtr<Expression> m_value;
    Type m_property_type;
    bool m_is_method { false };
};

class ObjectExpression final : public Expression {
public:
    ObjectExpression(NonnullRefPtrVector<ObjectProperty> properties = {})
        : m_properties(move(properties))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ObjectExpression"; }

    NonnullRefPtrVector<ObjectProperty> m_properties;
};

class ArrayExpression final : public Expression {
public:
    ArrayExpression(Vector<RefPtr<Expression>> elements)
        : m_elements(move(elements))
    {
    }

    const Vector<RefPtr<Expression>>& elements() const { return m_elements; }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ArrayExpression"; }

    Vector<RefPtr<Expression>> m_elements;
};

class TemplateLiteral final : public Expression {
public:
    TemplateLiteral(NonnullRefPtrVector<Expression> expressions)
        : m_expressions(move(expressions))
    {
    }

    TemplateLiteral(NonnullRefPtrVector<Expression> expressions, NonnullRefPtrVector<Expression> raw_strings)
        : m_expressions(move(expressions))
        , m_raw_strings(move(raw_strings))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

    const NonnullRefPtrVector<Expression>& expressions() const { return m_expressions; }
    const NonnullRefPtrVector<Expression>& raw_strings() const { return m_raw_strings; }

private:
    virtual const char* class_name() const override { return "TemplateLiteral"; }

    const NonnullRefPtrVector<Expression> m_expressions;
    const NonnullRefPtrVector<Expression> m_raw_strings;
};

class TaggedTemplateLiteral final : public Expression {
public:
    TaggedTemplateLiteral(NonnullRefPtr<Expression> tag, NonnullRefPtr<TemplateLiteral> template_literal)
        : m_tag(move(tag))
        , m_template_literal(move(template_literal))
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "TaggedTemplateLiteral"; }

    const NonnullRefPtr<Expression> m_tag;
    const NonnullRefPtr<TemplateLiteral> m_template_literal;
};

class MemberExpression final : public Expression {
public:
    MemberExpression(NonnullRefPtr<Expression> object, NonnullRefPtr<Expression> property, bool computed = false)
        : m_object(move(object))
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
    virtual bool is_member_expression() const override { return true; }
    virtual const char* class_name() const override { return "MemberExpression"; }

    NonnullRefPtr<Expression> m_object;
    NonnullRefPtr<Expression> m_property;
    bool m_computed { false };
};

class ConditionalExpression final : public Expression {
public:
    ConditionalExpression(NonnullRefPtr<Expression> test, NonnullRefPtr<Expression> consequent, NonnullRefPtr<Expression> alternate)
        : m_test(move(test))
        , m_consequent(move(consequent))
        , m_alternate(move(alternate))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "ConditionalExpression"; }

    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Expression> m_consequent;
    NonnullRefPtr<Expression> m_alternate;
};

class CatchClause final : public ASTNode {
public:
    CatchClause(const FlyString& parameter, NonnullRefPtr<BlockStatement> body)
        : m_parameter(parameter)
        , m_body(move(body))
    {
    }

    const FlyString& parameter() const { return m_parameter; }
    const BlockStatement& body() const { return m_body; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "CatchClause"; }

    FlyString m_parameter;
    NonnullRefPtr<BlockStatement> m_body;
};

class TryStatement final : public Statement {
public:
    TryStatement(NonnullRefPtr<BlockStatement> block, RefPtr<CatchClause> handler, RefPtr<BlockStatement> finalizer)
        : m_block(move(block))
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
    virtual const char* class_name() const override { return "TryStatement"; }

    NonnullRefPtr<BlockStatement> m_block;
    RefPtr<CatchClause> m_handler;
    RefPtr<BlockStatement> m_finalizer;
};

class ThrowStatement final : public Statement {
public:
    explicit ThrowStatement(NonnullRefPtr<Expression> argument)
        : m_argument(move(argument))
    {
    }

    const Expression& argument() const { return m_argument; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "ThrowStatement"; }

    NonnullRefPtr<Expression> m_argument;
};

class SwitchCase final : public ASTNode {
public:
    SwitchCase(RefPtr<Expression> test, NonnullRefPtrVector<Statement> consequent)
        : m_test(move(test))
        , m_consequent(move(consequent))
    {
    }

    const Expression* test() const { return m_test; }
    const NonnullRefPtrVector<Statement>& consequent() const { return m_consequent; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "SwitchCase"; }

    RefPtr<Expression> m_test;
    NonnullRefPtrVector<Statement> m_consequent;
};

class SwitchStatement final : public Statement {
public:
    SwitchStatement(NonnullRefPtr<Expression> discriminant, NonnullRefPtrVector<SwitchCase> cases)
        : m_discriminant(move(discriminant))
        , m_cases(move(cases))
    {
    }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "SwitchStatement"; }

    NonnullRefPtr<Expression> m_discriminant;
    NonnullRefPtrVector<SwitchCase> m_cases;
};

class BreakStatement final : public Statement {
public:
    BreakStatement(FlyString target_label)
        : m_target_label(target_label)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    const FlyString& target_label() const { return m_target_label; }

private:
    virtual const char* class_name() const override { return "BreakStatement"; }

    FlyString m_target_label;
};

class ContinueStatement final : public Statement {
public:
    ContinueStatement(FlyString target_label)
        : m_target_label(target_label)
    {
    }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

    const FlyString& target_label() const { return m_target_label; }

private:
    virtual const char* class_name() const override { return "ContinueStatement"; }

    FlyString m_target_label;
};

class DebuggerStatement final : public Statement {
public:
    DebuggerStatement() { }

    virtual Value execute(Interpreter&, GlobalObject&) const override;

private:
    virtual const char* class_name() const override { return "DebuggerStatement"; }
};

}
