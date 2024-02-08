/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Vector.h>

#include "Forward.h"

namespace JSSpecCompiler {

template<typename T>
RefPtr<T> as(NullableTree const& tree)
{
    return dynamic_cast<T*>(tree.ptr());
}

class NodeSubtreePointer {
public:
    NodeSubtreePointer(Tree* tree_ptr)
        : m_tree_ptr(tree_ptr)
    {
    }

    NodeSubtreePointer(NullableTree* tree_ptr)
        : m_tree_ptr(tree_ptr)
    {
    }

    NodeSubtreePointer(VariableRef* tree_ptr)
        : m_tree_ptr(tree_ptr)
    {
    }

    Tree get(Badge<RecursiveASTVisitor>);
    void replace_subtree(Badge<RecursiveASTVisitor>, NullableTree replacement);

private:
    Variant<Tree*, NullableTree*, VariableRef*> m_tree_ptr;
};

class VariableDeclaration : public RefCounted<VariableDeclaration> {
public:
    virtual ~VariableDeclaration() = default;
};

class NamedVariableDeclaration : public VariableDeclaration {
public:
    NamedVariableDeclaration(StringView name)
        : m_name(name)
    {
    }

    StringView m_name;
};

class SSAVariableDeclaration : public VariableDeclaration {
public:
    SSAVariableDeclaration(u64 version)
        : m_version(version)
    {
    }

    size_t m_index = 0;
    u64 m_version;
};

class Node : public RefCounted<Node> {
public:
    virtual ~Node() = default;

    void format_tree(StringBuilder& builder);

    // For expressions, order must be the same as the evaluation order.
    virtual Vector<NodeSubtreePointer> subtrees() { return {}; }

    virtual bool is_list() const { return false; }
    virtual bool is_statement() { VERIFY_NOT_REACHED(); }

protected:
    template<typename... Parameters>
    void dump_node(StringBuilder& builder, AK::CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters);

    virtual void dump_tree(StringBuilder& builder) = 0;
};

// Although both statements and expressions are allowed to return value, CFG building differentiates
// between them. Expressions are not allowed to change control flow, while statements are. Special
// handling required if a statement turns out to be a descendant of an expression. Roughly speaking,
// from the CFG standpoint, something like `a = ({ b + ({ c }) }) + ({ d })` will look like
// ```
//   auto tmp1 = c;
//   auto tmp2 = b + tmp1;
//   auto tmp3 = d;
//   a = tmp1 + tmp2;
// ```.
class Statement : public Node {
public:
    bool is_statement() override { return true; }
};

class Expression : public Node {
public:
    bool is_statement() override { return false; }
};

class ControlFlowOperator : public Statement {
public:
    bool is_statement() override { return false; }

    virtual Vector<BasicBlockRef*> references() = 0;
};

class ErrorNode : public Expression {
public:
    ErrorNode(StringView error = ""sv)
        : m_error(error)
    {
    }

    StringView m_error;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class WellKnownNode : public Expression {
public:
    enum Type {
        ZeroArgumentFunctionCall,
        // Update WellKnownNode::dump_tree after adding an entry here
    };

    WellKnownNode(Type type)
        : m_type(type)
    {
    }

protected:
    void dump_tree(StringBuilder& builder) override;

private:
    Type m_type;
};

inline Tree const error_tree = make_ref_counted<ErrorNode>();
inline Tree const zero_argument_function_call = make_ref_counted<WellKnownNode>(WellKnownNode::ZeroArgumentFunctionCall);

class ControlFlowFunctionReturn : public ControlFlowOperator {
public:
    ControlFlowFunctionReturn(VariableRef return_value)
        : m_return_value(move(return_value))
    {
    }

    VariableRef m_return_value;

    Vector<NodeSubtreePointer> subtrees() override { return { { &m_return_value } }; }
    Vector<BasicBlockRef*> references() override { return {}; }

protected:
    void dump_tree(StringBuilder& builder) override;
};

class ControlFlowJump : public ControlFlowOperator {
public:
    ControlFlowJump(BasicBlockRef block)
        : m_block(block)
    {
    }

    Vector<BasicBlockRef*> references() override;

    BasicBlockRef m_block;

protected:
    void dump_tree(StringBuilder& builder) override;
};

// This should be invalid enough to crash program on use.
inline NonnullRefPtr<ControlFlowOperator> const invalid_continuation = make_ref_counted<ControlFlowJump>(nullptr);

class ControlFlowBranch : public ControlFlowOperator {
public:
    ControlFlowBranch(Tree condition, BasicBlockRef then, BasicBlockRef else_)
        : m_condition(move(condition))
        , m_then(then)
        , m_else(else_)
    {
    }

    Vector<NodeSubtreePointer> subtrees() override { return { { &m_condition } }; }
    Vector<BasicBlockRef*> references() override;

    Tree m_condition;
    BasicBlockRef m_then;
    BasicBlockRef m_else;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class MathematicalConstant : public Expression {
public:
    MathematicalConstant(i64 number)
        : m_number(number)
    {
    }

    // TODO: This should be able to hold arbitrary number
    i64 m_number;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class StringLiteral : public Expression {
public:
    StringLiteral(StringView literal)
        : m_literal(literal)
    {
    }

    StringView m_literal;

protected:
    void dump_tree(StringBuilder& builder) override;
};

#define ENUMERATE_UNARY_OPERATORS(F) \
    F(Invalid)                       \
    F(AssertCompletion)              \
    F(Minus)

#define ENUMERATE_BINARY_OPERATORS(F) \
    F(Invalid)                        \
    F(ArraySubscript)                 \
    F(Assignment)                     \
    F(Comma)                          \
    F(CompareEqual)                   \
    F(CompareGreater)                 \
    F(CompareLess)                    \
    F(CompareNotEqual)                \
    F(Declaration)                    \
    F(Division)                       \
    F(FunctionCall)                   \
    F(MemberAccess)                   \
    F(Minus)                          \
    F(Multiplication)                 \
    F(Plus)

#define NAME(name) name,
#define STRINGIFY(name) #name##sv,

enum class UnaryOperator {
    ENUMERATE_UNARY_OPERATORS(NAME)
};

inline constexpr StringView unary_operator_names[] = {
    ENUMERATE_UNARY_OPERATORS(STRINGIFY)
};

enum class BinaryOperator {
#define NAME(name) name,
    ENUMERATE_BINARY_OPERATORS(NAME)
};

inline constexpr StringView binary_operator_names[] = {
    ENUMERATE_BINARY_OPERATORS(STRINGIFY)
};

#undef NAME
#undef STRINGIFY

class BinaryOperation : public Expression {
public:
    BinaryOperation(BinaryOperator operation, Tree left, Tree right)
        : m_operation(operation)
        , m_left(move(left))
        , m_right(move(right))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    BinaryOperator m_operation;
    Tree m_left;
    Tree m_right;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class UnaryOperation : public Expression {
public:
    UnaryOperation(UnaryOperator operation, Tree operand)
        : m_operation(operation)
        , m_operand(move(operand))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    UnaryOperator m_operation;
    Tree m_operand;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class IsOneOfOperation : public Expression {
public:
    IsOneOfOperation(Tree operand, Vector<Tree>&& compare_values)
        : m_operand(move(operand))
        , m_compare_values(move(compare_values))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_operand;
    Vector<Tree> m_compare_values;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class UnresolvedReference : public Expression {
public:
    UnresolvedReference(StringView name)
        : m_name(name)
    {
    }

    StringView m_name;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class ReturnNode : public Node {
public:
    ReturnNode(Tree return_value)
        : m_return_value(move(return_value))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_return_value;

protected:
    void dump_tree(StringBuilder& builder) override;
};

// Although assert might seems a good candidate for ControlFlowOperator, we are not interested in
// tracking control flow after a failed assertion.
class AssertExpression : public Expression {
public:
    AssertExpression(Tree condition)
        : m_condition(move(condition))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_condition;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class IfBranch : public Node {
public:
    IfBranch(Tree condition, Tree branch)
        : m_condition(move(condition))
        , m_branch(move(branch))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_condition;
    Tree m_branch;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class ElseIfBranch : public Node {
public:
    ElseIfBranch(NullableTree condition, Tree branch)
        : m_condition(move(condition))
        , m_branch(move(branch))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    NullableTree m_condition;
    Tree m_branch;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class IfElseIfChain : public Statement {
public:
    IfElseIfChain(Vector<Tree>&& conditions, Vector<Tree>&& branches, NullableTree else_branch)
        : m_conditions(move(conditions))
        , m_branches(move(branches))
        , m_else_branch(move(else_branch))
    {
        VERIFY(m_branches.size() == m_conditions.size());
    }

    Vector<NodeSubtreePointer> subtrees() override;

    // Excluding else branch, if one is present
    size_t branches_count() const { return m_branches.size(); }

    Vector<Tree> m_conditions;
    Vector<Tree> m_branches;
    NullableTree m_else_branch;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class TreeList : public Statement {
public:
    TreeList(Vector<Tree>&& trees);

    Vector<NodeSubtreePointer> subtrees() override;

    bool is_list() const override { return true; }

    Vector<Tree> m_trees;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class RecordDirectListInitialization : public Expression {
public:
    struct Argument {
        Tree name;
        Tree value;
    };

    RecordDirectListInitialization(Tree type_reference, Vector<Argument>&& arguments)
        : m_type_reference(move(type_reference))
        , m_arguments(move(arguments))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_type_reference;
    Vector<Argument> m_arguments;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class FunctionCall : public Expression {
public:
    FunctionCall(Tree name, Vector<Tree>&& arguments)
        : m_name(move(name))
        , m_arguments(move(arguments))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_name;
    Vector<Tree> m_arguments;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class SlotName : public Expression {
public:
    SlotName(StringView member_name)
        : m_member_name(member_name)
    {
    }

    StringView m_member_name;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class Variable : public Expression {
public:
    Variable(NamedVariableDeclarationRef name)
        : m_name(move(name))
    {
    }

    NamedVariableDeclarationRef m_name;
    SSAVariableDeclarationRef m_ssa;

    String name() const;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class FunctionPointer : public Expression {
public:
    FunctionPointer(FunctionDeclarationRef declaration)
        : m_declaration(declaration)
    {
    }

    FunctionDeclarationRef m_declaration;

protected:
    void dump_tree(StringBuilder& builder) override;
};

}

namespace AK {

template<>
struct Formatter<JSSpecCompiler::Tree> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, JSSpecCompiler::Tree const& tree)
    {
        tree->format_tree(builder.builder());
        return {};
    }
};

}
