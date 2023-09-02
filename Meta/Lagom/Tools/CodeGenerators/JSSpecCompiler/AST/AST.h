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

    Tree& get(Badge<RecursiveASTVisitor>) { return *m_tree_ptr; }

    void replace_subtree(Badge<RecursiveASTVisitor>, Tree tree) { *m_tree_ptr = move(tree); }

private:
    Tree* m_tree_ptr;
};

// ===== Generic nodes =====
class Node : public RefCounted<Node> {
public:
    virtual ~Node() = default;

    void format_tree(StringBuilder& builder);

    // For expressions, order must be the same as the evaluation order.
    virtual Vector<NodeSubtreePointer> subtrees() { return {}; }

    virtual bool is_type() { return false; }

protected:
    template<typename... Parameters>
    void dump_node(StringBuilder& builder, AK::CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters);

    virtual void dump_tree(StringBuilder& builder) = 0;
};

class ErrorNode : public Node {
public:
    ErrorNode() { }

protected:
    void dump_tree(StringBuilder& builder) override;
};

inline Tree const error_tree = make_ref_counted<ErrorNode>();

// ===== Concrete evaluatable nodes =====
class MathematicalConstant : public Node {
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

class StringLiteral : public Node {
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
    F(Minus)                         \
    F(AssertCompletion)

#define ENUMERATE_BINARY_OPERATORS(F) \
    F(Invalid)                        \
    F(CompareLess)                    \
    F(CompareGreater)                 \
    F(CompareNotEqual)                \
    F(CompareEqual)                   \
    F(Assignment)                     \
    F(Declaration)                    \
    F(Plus)                           \
    F(Minus)                          \
    F(Multiplication)                 \
    F(Division)                       \
    F(Comma)                          \
    F(MemberAccess)                   \
    F(FunctionCall)                   \
    F(ArraySubscript)

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

class BinaryOperation : public Node {
public:
    BinaryOperation(BinaryOperator operation, Tree left, Tree right)
        : m_operation(operation)
        , m_left(left)
        , m_right(right)
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    BinaryOperator m_operation;
    Tree m_left;
    Tree m_right;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class UnaryOperation : public Node {
public:
    UnaryOperation(UnaryOperator operation, Tree operand)
        : m_operation(operation)
        , m_operand(operand)
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    UnaryOperator m_operation;
    Tree m_operand;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class IsOneOfOperation : public Node {
public:
    IsOneOfOperation(Tree operand, Vector<Tree>&& compare_values)
        : m_operand(operand)
        , m_compare_values(move(compare_values))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_operand;
    Vector<Tree> m_compare_values;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class UnresolvedReference : public Node {
public:
    UnresolvedReference(StringView name)
        : m_name(name)
    {
    }

    StringView m_name;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class ReturnExpression : public Node {
public:
    ReturnExpression(Tree return_value)
        : m_return_value(return_value)
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_return_value;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class AssertExpression : public Node {
public:
    AssertExpression(Tree condition)
        : m_condition(condition)
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
        : m_condition(condition)
        , m_branch(branch)
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
    ElseIfBranch(Optional<Tree> condition, Tree branch)
        : m_condition(condition)
        , m_branch(branch)
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Optional<Tree> m_condition;
    Tree m_branch;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class TreeList : public Node {
public:
    TreeList(Vector<Tree>&& expressions_)
        : m_expressions(move(expressions_))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Vector<Tree> m_expressions;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class RecordDirectListInitialization : public Node {
public:
    struct Argument {
        Tree name;
        Tree value;
    };

    RecordDirectListInitialization(Tree type_reference, Vector<Argument>&& arguments)
        : m_type_reference(type_reference)
        , m_arguments(move(arguments))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_type_reference;
    Vector<Argument> m_arguments;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class FunctionCall : public Node {
public:
    FunctionCall(Tree name, Vector<Tree>&& arguments)
        : m_name(name)
        , m_arguments(move(arguments))
    {
    }

    Vector<NodeSubtreePointer> subtrees() override;

    Tree m_name;
    Vector<Tree> m_arguments;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class SlotName : public Node {
public:
    SlotName(StringView member_name)
        : m_member_name(member_name)
    {
    }

    StringView m_member_name;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class Variable : public Node {
public:
    Variable(StringView variable_name)
        : m_name(variable_name)
    {
    }

    StringView m_name;

protected:
    void dump_tree(StringBuilder& builder) override;
};

class FunctionPointer : public Node {
public:
    FunctionPointer(StringView function_name)
        : m_function(function_name)
    {
    }

    Variant<StringView, FunctionRef> m_function;

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
