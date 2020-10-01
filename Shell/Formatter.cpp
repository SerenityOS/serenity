/*
 * Copyright (c) 2020, the SerenityOS developers.
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

#include "Formatter.h"
#include "AST.h"
#include "Parser.h"
#include <AK/TemporaryChange.h>

namespace Shell {

String Formatter::format()
{
    auto node = Parser(m_source).parse();
    if (m_cursor >= 0)
        m_output_cursor = m_cursor;

    if (!node)
        return String();

    if (node->is_syntax_error())
        return m_source;

    if (m_cursor >= 0) {
        auto hit_test = node->hit_test_position(m_cursor);
        if (hit_test.matching_node)
            m_hit_node = hit_test.matching_node.ptr();
        else
            m_hit_node = nullptr;
    }

    m_parent_node = nullptr;

    node->visit(*this);

    auto string = m_builder.string_view();

    if (!string.ends_with(" "))
        m_builder.append(m_trivia);

    return m_builder.to_string();
}

void Formatter::with_added_indent(int indent, Function<void()> callback)
{
    TemporaryChange indent_change { m_current_indent, m_current_indent + indent };
    callback();
}

void Formatter::in_new_block(Function<void()> callback)
{
    current_builder().append('{');

    with_added_indent(1, [&] {
        insert_separator();
        callback();
    });

    insert_separator();
    current_builder().append('}');
}

void Formatter::test_and_update_output_cursor(const AST::Node* node)
{
    if (!node)
        return;

    if (node != m_hit_node)
        return;

    m_output_cursor = current_builder().length() + m_cursor - node->position().start_offset;
}

void Formatter::insert_separator()
{
    current_builder().append('\n');
    insert_indent();
}

void Formatter::insert_indent()
{
    for (size_t i = 0; i < m_current_indent; ++i)
        current_builder().append("  ");
}

void Formatter::visit(const AST::PathRedirectionNode* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::And* node)
{
    test_and_update_output_cursor(node);
    auto should_indent = m_parent_node && m_parent_node->class_name() != "And";
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    with_added_indent(should_indent ? 1 : 0, [&] {
        node->left()->visit(*this);

        current_builder().append(" \\");
        insert_separator();
        current_builder().append("&& ");

        node->right()->visit(*this);
    });
}

void Formatter::visit(const AST::ListConcatenate* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    auto first = true;
    for (auto& subnode : node->list()) {
        if (!first)
            current_builder().append(' ');
        first = false;
        subnode->visit(*this);
    }
}

void Formatter::visit(const AST::Background* node)
{
    test_and_update_output_cursor(node);

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
    current_builder().append(" &");
}

void Formatter::visit(const AST::BarewordLiteral* node)
{
    test_and_update_output_cursor(node);
    current_builder().append(node->text());
}

void Formatter::visit(const AST::CastToCommand* node)
{
    test_and_update_output_cursor(node);
    if (m_options.explicit_parentheses)
        current_builder().append('(');

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);

    if (m_options.explicit_parentheses)
        current_builder().append(')');
}

void Formatter::visit(const AST::CastToList* node)
{
    test_and_update_output_cursor(node);
    current_builder().append('(');

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);

    current_builder().append(')');
}

void Formatter::visit(const AST::CloseFdRedirection* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    current_builder().appendf(" %d>&-", node->fd());
}

void Formatter::visit(const AST::CommandLiteral*)
{
    ASSERT_NOT_REACHED();
}

void Formatter::visit(const AST::Comment* node)
{
    test_and_update_output_cursor(node);
    current_builder().append("#");
    current_builder().append(node->text());
}

void Formatter::visit(const AST::DynamicEvaluate* node)
{
    test_and_update_output_cursor(node);
    current_builder().append('$');
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::DoubleQuotedString* node)
{
    test_and_update_output_cursor(node);
    current_builder().append("\"");

    TemporaryChange quotes { m_options.in_double_quotes, true };
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    NodeVisitor::visit(node);

    current_builder().append("\"");
}

void Formatter::visit(const AST::Fd2FdRedirection* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    current_builder().appendf(" %d>&%d", node->source_fd(), node->dest_fd());
    if (m_hit_node == node)
        ++m_output_cursor;
}

void Formatter::visit(const AST::FunctionDeclaration* node)
{
    test_and_update_output_cursor(node);
    current_builder().append(node->name().name);
    current_builder().append('(');
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    auto first = true;
    for (auto& arg : node->arguments()) {
        if (!first)
            current_builder().append(' ');
        first = false;
        current_builder().append(arg.name);
    }

    current_builder().append(") ");

    in_new_block([&] {
        if (node->block())
            node->block()->visit(*this);
    });
}

void Formatter::visit(const AST::ForLoop* node)
{
    test_and_update_output_cursor(node);
    current_builder().append("for ");
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->variable_name() != "it") {
        current_builder().append(node->variable_name());
        current_builder().append(" in ");
    }

    node->iterated_expression()->visit(*this);

    current_builder().append(' ');
    in_new_block([&] {
        if (node->block())
            node->block()->visit(*this);
    });
}

void Formatter::visit(const AST::Glob* node)
{
    test_and_update_output_cursor(node);
    current_builder().append(node->text());
}

void Formatter::visit(const AST::Execute* node)
{
    test_and_update_output_cursor(node);
    auto& builder = current_builder();
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    ScopedValueRollback options_rollback { m_options };

    if (node->does_capture_stdout()) {
        builder.append("$");
        m_options.explicit_parentheses = true;
    }

    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::IfCond* node)
{
    test_and_update_output_cursor(node);

    current_builder().append("if ");
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    node->condition()->visit(*this);

    current_builder().append(' ');

    in_new_block([&] {
        if (node->true_branch())
            node->true_branch()->visit(*this);
    });

    if (node->false_branch()) {
        current_builder().append(" else ");
        if (node->false_branch()->class_name() != "IfCond") {
            in_new_block([&] {
                node->false_branch()->visit(*this);
            });
        } else {
            node->false_branch()->visit(*this);
        }
    } else if (node->else_position().has_value()) {
        current_builder().append(" else ");
    }
}

void Formatter::visit(const AST::Join* node)
{
    test_and_update_output_cursor(node);
    auto should_parenthesise = m_options.explicit_parentheses;

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    TemporaryChange parens { m_options.explicit_parentheses, false };

    if (should_parenthesise)
        current_builder().append('(');

    NodeVisitor::visit(node);

    if (should_parenthesise)
        current_builder().append(')');
}

void Formatter::visit(const AST::MatchExpr* node)
{
    test_and_update_output_cursor(node);
    current_builder().append("match ");

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    node->matched_expr()->visit(*this);

    if (!node->expr_name().is_empty()) {
        current_builder().append(" as ");
        current_builder().append(node->expr_name());
    }

    current_builder().append(' ');
    in_new_block([&] {
        auto first_entry = true;
        for (auto& entry : node->entries()) {
            if (!first_entry)
                insert_separator();
            first_entry = false;
            auto first = true;
            for (auto& option : entry.options) {
                if (!first)
                    current_builder().append(" | ");
                first = false;
                option.visit(*this);
            }

            in_new_block([&] {
                if (entry.body)
                    entry.body->visit(*this);
            });
        }
    });
}

void Formatter::visit(const AST::Or* node)
{
    test_and_update_output_cursor(node);
    auto should_indent = m_parent_node && m_parent_node->class_name() != "Or";
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    with_added_indent(should_indent ? 1 : 0, [&] {
        node->left()->visit(*this);

        current_builder().append(" \\");
        insert_separator();
        current_builder().append("|| ");

        node->right()->visit(*this);
    });
}

void Formatter::visit(const AST::Pipe* node)
{
    test_and_update_output_cursor(node);
    auto should_indent = m_parent_node && m_parent_node->class_name() != "Pipe";
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    node->left()->visit(*this);
    current_builder().append(" \\");

    with_added_indent(should_indent ? 1 : 0, [&] {
        insert_separator();
        current_builder().append("| ");

        node->right()->visit(*this);
    });
}

void Formatter::visit(const AST::ReadRedirection* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 0)
        current_builder().appendf(" %d<", node->fd());
    else
        current_builder().append(" <");
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::ReadWriteRedirection* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 0)
        current_builder().appendf(" %d<>", node->fd());
    else
        current_builder().append(" <>");
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::Sequence* node)
{
    test_and_update_output_cursor(node);

    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    node->left()->visit(*this);
    insert_separator();

    node->right()->visit(*this);
}

void Formatter::visit(const AST::Subshell* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    in_new_block([&] {
        insert_separator();
        NodeVisitor::visit(node);
        insert_separator();
    });
}

void Formatter::visit(const AST::SimpleVariable* node)
{
    test_and_update_output_cursor(node);
    current_builder().append('$');
    current_builder().append(node->name());
}

void Formatter::visit(const AST::SpecialVariable* node)
{
    test_and_update_output_cursor(node);
    current_builder().append('$');
    current_builder().append(node->name());
}

void Formatter::visit(const AST::Juxtaposition* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::StringLiteral* node)
{
    test_and_update_output_cursor(node);
    if (!m_options.in_double_quotes)
        current_builder().append("'");

    if (m_options.in_double_quotes) {
        for (auto ch : node->text()) {
            switch (ch) {
            case '"':
            case '\\':
            case '$':
                current_builder().append('\\');
                break;
            case '\n':
                current_builder().append("\\n");
                continue;
            case '\r':
                current_builder().append("\\r");
                continue;
            case '\t':
                current_builder().append("\\t");
                continue;
            case '\v':
                current_builder().append("\\v");
                continue;
            case '\f':
                current_builder().append("\\f");
                continue;
            case '\a':
                current_builder().append("\\a");
                continue;
            case '\e':
                current_builder().append("\\e");
                continue;
            default:
                break;
            }
            current_builder().append(ch);
        }
    } else {
        current_builder().append(node->text());
    }

    if (!m_options.in_double_quotes)
        current_builder().append("'");
}

void Formatter::visit(const AST::StringPartCompose* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::SyntaxError* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::Tilde* node)
{
    test_and_update_output_cursor(node);
    current_builder().append(node->text());
}

void Formatter::visit(const AST::VariableDeclarations* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    auto first = true;
    for (auto& entry : node->variables()) {
        if (!first)
            current_builder().append(' ');
        first = false;
        entry.name->visit(*this);
        current_builder().append('=');

        if (entry.value->is_command())
            current_builder().append('(');

        entry.value->visit(*this);

        if (entry.value->is_command())
            current_builder().append(')');
    }
}

void Formatter::visit(const AST::WriteAppendRedirection* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 1)
        current_builder().appendf(" %d>>", node->fd());
    else
        current_builder().append(" >>");
    NodeVisitor::visit(node);
}

void Formatter::visit(const AST::WriteRedirection* node)
{
    test_and_update_output_cursor(node);
    TemporaryChange<const AST::Node*> parent { m_parent_node, node };

    if (node->fd() != 1)
        current_builder().appendf(" %d>", node->fd());
    else
        current_builder().append(" >");
    NodeVisitor::visit(node);
}

}
