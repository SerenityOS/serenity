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

template<class T, class... Args>
static inline NonnullRefPtr<T>
create_ast_node(Args&&... args)
{
    return adopt(*new T(forward<Args>(args)...));
}

class ASTNode : public RefCounted<ASTNode> {
public:
    virtual ~ASTNode() {}
    virtual const char* class_name() const = 0;
    virtual Value execute(Interpreter&) const = 0;
    virtual void dump(int indent) const;
    virtual bool is_identifier() const { return false; }
    virtual bool is_spread_expression() const { return false; }
    virtual bool is_member_expression() const { return false; }
    virtual bool is_scope_node() const { return false; }
    virtual bool is_program() const { return false; }
    virtual bool is_variable_declaration() const { return false; }
    virtual bool is_new_expression() const { return false; }

protected:
    ASTNode() {}

private:
};

class Statement : public ASTNode {
};

class ErrorStatement final : public Statement {
public:
    Value execute(Interpreter&) const override { return js_undefined(); }
    const char* class_name() const override { return "ErrorStatement"; }
};

class ExpressionStatement final : public Statement {
public:
    ExpressionStatement(NonnullRefPtr<Expression> expression)
        : m_expression(move(expression))
    {
    }

    Value execute(Interpreter&) const override;
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
    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    void add_variables(NonnullRefPtrVector<VariableDeclaration>);
    const NonnullRefPtrVector<VariableDeclaration>& variables() const { return m_variables; }

protected:
    ScopeNode() {}

private:
    virtual bool is_scope_node() const final { return true; }
    NonnullRefPtrVector<Statement> m_children;
    NonnullRefPtrVector<VariableDeclaration> m_variables;
};

class Program : public ScopeNode {
public:
    Program() {}

private:
    virtual bool is_program() const override { return true; }
    virtual const char* class_name() const override { return "Program"; }
};

class BlockStatement : public ScopeNode {
public:
    BlockStatement() {}

private:
    virtual const char* class_name() const override { return "BlockStatement"; }
};

class Expression : public ASTNode {
public:
    virtual Reference to_reference(Interpreter&) const;
};

class Declaration : public Statement {
};

class FunctionNode {
public:
    struct Parameter {
        FlyString name;
        RefPtr<Expression> default_value;
    };

    const FlyString& name() const { return m_name; }
    const Statement& body() const { return *m_body; }
    const Vector<Parameter>& parameters() const { return m_parameters; };

protected:
    FunctionNode(const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, NonnullRefPtrVector<VariableDeclaration> variables)
        : m_name(name)
        , m_body(move(body))
        , m_parameters(move(parameters))
        , m_variables(move(variables))
    {
    }

    void dump(int indent, const char* class_name) const;

    const NonnullRefPtrVector<VariableDeclaration>& variables() const { return m_variables; }

private:
    FlyString m_name;
    NonnullRefPtr<Statement> m_body;
    const Vector<Parameter> m_parameters;
    NonnullRefPtrVector<VariableDeclaration> m_variables;
};

class FunctionDeclaration final
    : public Declaration
    , public FunctionNode {
public:
    static bool must_have_name() { return true; }

    FunctionDeclaration(const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, NonnullRefPtrVector<VariableDeclaration> variables)
        : FunctionNode(name, move(body), move(parameters), move(variables))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "FunctionDeclaration"; }
};

class FunctionExpression final : public Expression
    , public FunctionNode {
public:
    static bool must_have_name() { return false; }

    FunctionExpression(const FlyString& name, NonnullRefPtr<Statement> body, Vector<Parameter> parameters, NonnullRefPtrVector<VariableDeclaration> variables)
        : FunctionNode(name, move(body), move(parameters), move(variables))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "FunctionExpression"; }
};

class ErrorExpression final : public Expression {
public:
    Value execute(Interpreter&) const override { return js_undefined(); }
    const char* class_name() const override { return "ErrorExpression"; }
};

class ReturnStatement : public Statement {
public:
    explicit ReturnStatement(RefPtr<Expression> argument)
        : m_argument(move(argument))
    {
    }

    const Expression* argument() const { return m_argument; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ReturnStatement"; }

    RefPtr<Expression> m_argument;
};

class IfStatement : public Statement {
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

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "IfStatement"; }

    NonnullRefPtr<Expression> m_predicate;
    NonnullRefPtr<Statement> m_consequent;
    RefPtr<Statement> m_alternate;
};

class WhileStatement : public Statement {
public:
    WhileStatement(NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : m_test(move(test))
        , m_body(move(body))
    {
    }

    const Expression& test() const { return *m_test; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "WhileStatement"; }

    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class DoWhileStatement : public Statement {
public:
    DoWhileStatement(NonnullRefPtr<Expression> test, NonnullRefPtr<Statement> body)
        : m_test(move(test))
        , m_body(move(body))
    {
    }

    const Expression& test() const { return *m_test; }
    const Statement& body() const { return *m_body; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "DoWhileStatement"; }

    NonnullRefPtr<Expression> m_test;
    NonnullRefPtr<Statement> m_body;
};

class ForStatement : public Statement {
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

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ForStatement"; }

    RefPtr<ASTNode> m_init;
    RefPtr<Expression> m_test;
    RefPtr<Expression> m_update;
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

class BinaryExpression : public Expression {
public:
    BinaryExpression(BinaryOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
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
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class LogicalOp {
    And,
    Or,
    NullishCoalescing,
};

class LogicalExpression : public Expression {
public:
    LogicalExpression(LogicalOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
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

class UnaryExpression : public Expression {
public:
    UnaryExpression(UnaryOp op, NonnullRefPtr<Expression> lhs)
        : m_op(op)
        , m_lhs(move(lhs))
    {
    }

    virtual Value execute(Interpreter&) const override;
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
    virtual Value execute(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "SequenceExpression"; }

    NonnullRefPtrVector<Expression> m_expressions;
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

class NullLiteral final : public Literal {
public:
    explicit NullLiteral() {}

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "NullLiteral"; }
};

class Identifier final : public Expression {
public:
    explicit Identifier(const FlyString& string)
        : m_string(string)
    {
    }

    const FlyString& string() const { return m_string; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual bool is_identifier() const override { return true; }
    virtual Reference to_reference(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "Identifier"; }

    FlyString m_string;
};

class SpreadExpression final : public Expression {
public:
    explicit SpreadExpression(NonnullRefPtr<Expression> target)
        : m_target(target)
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual bool is_spread_expression() const override { return true; }

private:
    virtual const char* class_name() const override { return "SpreadExpression"; }

    NonnullRefPtr<Expression> m_target;
};

class ThisExpression final : public Expression {
public:
    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ThisExpression"; }
};

class CallExpression : public Expression {
public:
    CallExpression(NonnullRefPtr<Expression> callee, NonnullRefPtrVector<Expression> arguments = {})
        : m_callee(move(callee))
        , m_arguments(move(arguments))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "CallExpression"; }

    struct ThisAndCallee {
        Value this_value;
        Value callee;
    };
    ThisAndCallee compute_this_and_callee(Interpreter&) const;

    NonnullRefPtr<Expression> m_callee;
    const NonnullRefPtrVector<Expression> m_arguments;
};

class NewExpression final : public CallExpression {
public:
    NewExpression(NonnullRefPtr<Expression> callee, NonnullRefPtrVector<Expression> arguments = {})
        : CallExpression(move(callee), move(arguments))
    {
    }

private:
    virtual const char* class_name() const override { return "NewExpression"; }
    virtual bool is_new_expression() const override { return true; }
};

enum class AssignmentOp {
    Assignment,
    AdditionAssignment,
    SubtractionAssignment,
    MultiplicationAssignment,
    DivisionAssignment,
    LeftShiftAssignment,
    RightShiftAssignment,
    UnsignedRightShiftAssignment,
};

class AssignmentExpression : public Expression {
public:
    AssignmentExpression(AssignmentOp op, NonnullRefPtr<Expression> lhs, NonnullRefPtr<Expression> rhs)
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
    NonnullRefPtr<Expression> m_lhs;
    NonnullRefPtr<Expression> m_rhs;
};

enum class UpdateOp {
    Increment,
    Decrement,
};

class UpdateExpression : public Expression {
public:
    UpdateExpression(UpdateOp op, NonnullRefPtr<Expression> argument, bool prefixed = false)
        : m_op(op)
        , m_argument(move(argument))
        , m_prefixed(prefixed)
    {
    }

    virtual Value execute(Interpreter&) const override;
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
    VariableDeclarator(NonnullRefPtr<Identifier> id, RefPtr<Expression> init)
        : m_id(move(id))
        , m_init(move(init))
    {
    }

    const Identifier& id() const { return m_id; }
    const Expression* init() const { return m_init; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "VariableDeclarator"; }

    NonnullRefPtr<Identifier> m_id;
    RefPtr<Expression> m_init;
};

class VariableDeclaration : public Declaration {
public:
    VariableDeclaration(DeclarationKind declaration_kind, NonnullRefPtrVector<VariableDeclarator> declarations)
        : m_declaration_kind(declaration_kind)
        , m_declarations(move(declarations))
    {
    }

    virtual bool is_variable_declaration() const override { return true; }
    DeclarationKind declaration_kind() const { return m_declaration_kind; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

    const NonnullRefPtrVector<VariableDeclarator>& declarations() const { return m_declarations; }

private:
    virtual const char* class_name() const override { return "VariableDeclaration"; }

    DeclarationKind m_declaration_kind;
    NonnullRefPtrVector<VariableDeclarator> m_declarations;
};

class ObjectProperty final : public ASTNode {
public:
    ObjectProperty(NonnullRefPtr<Expression> key, NonnullRefPtr<Expression> value)
        : m_key(move(key))
        , m_value(move(value))
    {
    }

    const Expression& key() const { return m_key; }
    const Expression& value() const { return m_value; }

    bool is_spread() const { return m_is_spread; }
    void set_is_spread() { m_is_spread = true; }

    virtual void dump(int indent) const override;
    virtual Value execute(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "ObjectProperty"; }

    NonnullRefPtr<Expression> m_key;
    NonnullRefPtr<Expression> m_value;
    bool m_is_spread { false };
};

class ObjectExpression : public Expression {
public:
    ObjectExpression(NonnullRefPtrVector<ObjectProperty> properties = {})
        : m_properties(move(properties))
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ObjectExpression"; }

    NonnullRefPtrVector<ObjectProperty> m_properties;
};

class ArrayExpression : public Expression {
public:
    ArrayExpression(Vector<RefPtr<Expression>> elements)
        : m_elements(move(elements))
    {
    }

    const Vector<RefPtr<Expression>>& elements() const { return m_elements; }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;

private:
    virtual const char* class_name() const override { return "ArrayExpression"; }

    Vector<RefPtr<Expression>> m_elements;
};

class MemberExpression final : public Expression {
public:
    MemberExpression(NonnullRefPtr<Expression> object, NonnullRefPtr<Expression> property, bool computed = false)
        : m_object(move(object))
        , m_property(move(property))
        , m_computed(computed)
    {
    }

    virtual Value execute(Interpreter&) const override;
    virtual void dump(int indent) const override;
    virtual Reference to_reference(Interpreter&) const override;

    bool is_computed() const { return m_computed; }
    const Expression& object() const { return *m_object; }
    const Expression& property() const { return *m_property; }

    PropertyName computed_property_name(Interpreter&) const;

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
    virtual Value execute(Interpreter&) const override;

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
    virtual Value execute(Interpreter&) const override;

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
    virtual Value execute(Interpreter&) const override;

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
    virtual Value execute(Interpreter&) const override;

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
    virtual Value execute(Interpreter&) const override;

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
    virtual Value execute(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "SwitchStatement"; }

    NonnullRefPtr<Expression> m_discriminant;
    NonnullRefPtrVector<SwitchCase> m_cases;
};

class BreakStatement final : public Statement {
public:
    BreakStatement() {}

    virtual Value execute(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "BreakStatement"; }
};

class ContinueStatement final : public Statement {
public:
    ContinueStatement() {}

    virtual Value execute(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "ContinueStatement"; }
};

class DebuggerStatement final : public Statement {
public:
    DebuggerStatement() {}

    virtual Value execute(Interpreter&) const override;

private:
    virtual const char* class_name() const override { return "DebuggerStatement"; }
};

}
