/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Formatter.h"
#include "Shell.h"
#include <LibRegex/Regex.h>

namespace Shell {

RefPtr<AST::Node> Shell::immediate_length_impl(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments, bool across)
{
    auto name = across ? "length_across" : "length";
    if (arguments.size() < 1 || arguments.size() > 2) {
        raise_error(ShellError::EvaluatedSyntaxError, String::formatted("Expected one or two arguments to `{}'", name), invoking_node.position());
        return nullptr;
    }

    enum {
        Infer,
        String,
        List,
    } mode { Infer };

    bool is_inferred = false;

    const AST::Node* expr_node;
    if (arguments.size() == 2) {
        // length string <expr>
        // length list <expr>

        auto& mode_arg = arguments.first();
        if (!mode_arg.is_bareword()) {
            raise_error(ShellError::EvaluatedSyntaxError, String::formatted("Expected a bareword (either 'string' or 'list') in the two-argument form of the `{}' immediate", name), mode_arg.position());
            return nullptr;
        }

        const auto& mode_name = static_cast<const AST::BarewordLiteral&>(mode_arg).text();
        if (mode_name == "list") {
            mode = List;
        } else if (mode_name == "string") {
            mode = String;
        } else if (mode_name == "infer") {
            mode = Infer;
        } else {
            raise_error(ShellError::EvaluatedSyntaxError, String::formatted("Expected either 'string' or 'list' (and not {}) in the two-argument form of the `{}' immediate", mode_name, name), mode_arg.position());
            return nullptr;
        }

        expr_node = &arguments[1];
    } else {
        expr_node = &arguments[0];
    }

    if (mode == Infer) {
        is_inferred = true;
        if (expr_node->is_list())
            mode = List;
        else if (expr_node->is_simple_variable()) // "Look inside" variables
            mode = const_cast<AST::Node*>(expr_node)->run(this)->resolve_without_cast(this)->is_list_without_resolution() ? List : String;
        else if (is<AST::ImmediateExpression>(expr_node))
            mode = List;
        else
            mode = String;
    }

    auto value_with_number = [&](auto number) -> NonnullRefPtr<AST::Node> {
        return AST::make_ref_counted<AST::BarewordLiteral>(invoking_node.position(), String::number(number));
    };

    auto do_across = [&](StringView mode_name, auto& values) {
        if (is_inferred)
            mode_name = "infer";
        // Translate to a list of applications of `length <mode_name>`
        Vector<NonnullRefPtr<AST::Node>> resulting_nodes;
        resulting_nodes.ensure_capacity(values.size());
        for (auto& entry : values) {
            // ImmediateExpression(length <mode_name> <entry>)
            resulting_nodes.unchecked_append(AST::make_ref_counted<AST::ImmediateExpression>(
                expr_node->position(),
                AST::NameWithPosition { "length", invoking_node.function_position() },
                NonnullRefPtrVector<AST::Node> { Vector<NonnullRefPtr<AST::Node>> {
                    static_cast<NonnullRefPtr<AST::Node>>(AST::make_ref_counted<AST::BarewordLiteral>(expr_node->position(), mode_name)),
                    AST::make_ref_counted<AST::SyntheticNode>(expr_node->position(), NonnullRefPtr<AST::Value>(entry)),
                } },
                expr_node->position()));
        }

        return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(resulting_nodes));
    };

    switch (mode) {
    default:
    case Infer:
        VERIFY_NOT_REACHED();
    case List: {
        auto value = (const_cast<AST::Node*>(expr_node))->run(this);
        if (!value)
            return value_with_number(0);

        value = value->resolve_without_cast(this);

        if (auto list = dynamic_cast<AST::ListValue*>(value.ptr())) {
            if (across)
                return do_across("list", list->values());

            return value_with_number(list->values().size());
        }

        auto list = value->resolve_as_list(this);
        if (!across)
            return value_with_number(list.size());

        dbgln("List has {} entries", list.size());
        auto values = AST::make_ref_counted<AST::ListValue>(move(list));
        return do_across("list", values->values());
    }
    case String: {
        // 'across' will only accept lists, and '!across' will only accept non-lists here.
        if (expr_node->is_list()) {
            if (!across) {
            raise_no_list_allowed:;
                Formatter formatter { *expr_node };

                if (is_inferred) {
                    raise_error(ShellError::EvaluatedSyntaxError,
                        String::formatted("Could not infer expression type, please explicitly use `{0} string' or `{0} list'", name),
                        invoking_node.position());
                    return nullptr;
                }

                auto source = formatter.format();
                raise_error(ShellError::EvaluatedSyntaxError,
                    source.is_empty()
                        ? "Invalid application of `length' to a list"
                        : String::formatted("Invalid application of `length' to a list\nperhaps you meant `{1}length \"{0}\"{2}' or `{1}length_across {0}{2}'?", source, "\x1b[32m", "\x1b[0m"),
                    expr_node->position());
                return nullptr;
            }
        }

        auto value = (const_cast<AST::Node*>(expr_node))->run(this);
        if (!value)
            return value_with_number(0);

        value = value->resolve_without_cast(*this);

        if (auto list = dynamic_cast<AST::ListValue*>(value.ptr())) {
            if (!across)
                goto raise_no_list_allowed;

            return do_across("string", list->values());
        }

        if (across && !value->is_list()) {
            Formatter formatter { *expr_node };

            auto source = formatter.format();
            raise_error(ShellError::EvaluatedSyntaxError,
                String::formatted("Invalid application of `length_across' to a non-list\nperhaps you meant `{1}length {0}{2}'?", source, "\x1b[32m", "\x1b[0m"),
                expr_node->position());
            return nullptr;
        }

        // Evaluate the nodes and substitute with the lengths.
        auto list = value->resolve_as_list(this);

        if (!expr_node->is_list()) {
            if (list.size() == 1) {
                if (across)
                    goto raise_no_list_allowed;

                // This is the normal case, the expression is a normal non-list expression.
                return value_with_number(list.first().length());
            }

            // This can be hit by asking for the length of a command list (e.g. `(>/dev/null)`)
            // raise an error about misuse of command lists for now.
            // FIXME: What's the length of `(>/dev/null)` supposed to be?
            raise_error(ShellError::EvaluatedSyntaxError, "Length of meta value (or command list) requested, this is currently not supported.", expr_node->position());
            return nullptr;
        }

        auto values = AST::make_ref_counted<AST::ListValue>(move(list));
        return do_across("string", values->values());
    }
    }
}

RefPtr<AST::Node> Shell::immediate_length(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    return immediate_length_impl(invoking_node, arguments, false);
}

RefPtr<AST::Node> Shell::immediate_length_across(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    return immediate_length_impl(invoking_node, arguments, true);
}

RefPtr<AST::Node> Shell::immediate_regex_replace(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    if (arguments.size() != 3) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 3 arguments to regex_replace", invoking_node.position());
        return nullptr;
    }

    auto pattern = const_cast<AST::Node&>(arguments[0]).run(this);
    auto replacement = const_cast<AST::Node&>(arguments[1]).run(this);
    auto value = const_cast<AST::Node&>(arguments[2]).run(this)->resolve_without_cast(this);

    if (!pattern->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the regex_replace pattern to be a string", arguments[0].position());
        return nullptr;
    }

    if (!replacement->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the regex_replace replacement string to be a string", arguments[1].position());
        return nullptr;
    }

    if (!value->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the regex_replace target value to be a string", arguments[2].position());
        return nullptr;
    }

    Regex<PosixExtendedParser> re { pattern->resolve_as_list(this).first() };
    auto result = re.replace(value->resolve_as_list(this)[0], replacement->resolve_as_list(this)[0], PosixFlags::Global | PosixFlags::Multiline | PosixFlags::Unicode);

    return AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), move(result));
}

RefPtr<AST::Node> Shell::immediate_remove_suffix(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to remove_suffix", invoking_node.position());
        return nullptr;
    }

    auto suffix = const_cast<AST::Node&>(arguments[0]).run(this);
    auto value = const_cast<AST::Node&>(arguments[1]).run(this)->resolve_without_cast(this);

    if (!suffix->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the remove_suffix suffix string to be a string", arguments[0].position());
        return nullptr;
    }

    auto suffix_str = suffix->resolve_as_list(this)[0];
    auto values = value->resolve_as_list(this);

    Vector<NonnullRefPtr<AST::Node>> nodes;

    for (auto& value_str : values) {
        StringView removed { value_str };

        if (value_str.ends_with(suffix_str))
            removed = removed.substring_view(0, value_str.length() - suffix_str.length());
        nodes.append(AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), removed));
    }

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(nodes));
}

RefPtr<AST::Node> Shell::immediate_remove_prefix(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to remove_prefix", invoking_node.position());
        return nullptr;
    }

    auto prefix = const_cast<AST::Node&>(arguments[0]).run(this);
    auto value = const_cast<AST::Node&>(arguments[1]).run(this)->resolve_without_cast(this);

    if (!prefix->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the remove_prefix prefix string to be a string", arguments[0].position());
        return nullptr;
    }

    auto prefix_str = prefix->resolve_as_list(this)[0];
    auto values = value->resolve_as_list(this);

    Vector<NonnullRefPtr<AST::Node>> nodes;

    for (auto& value_str : values) {
        StringView removed { value_str };

        if (value_str.starts_with(prefix_str))
            removed = removed.substring_view(prefix_str.length());
        nodes.append(AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), removed));
    }

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(nodes));
}

RefPtr<AST::Node> Shell::immediate_split(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to split", invoking_node.position());
        return nullptr;
    }

    auto delimiter = const_cast<AST::Node&>(arguments[0]).run(this);
    auto value = const_cast<AST::Node&>(arguments[1]).run(this)->resolve_without_cast(this);

    if (!delimiter->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the split delimiter string to be a string", arguments[0].position());
        return nullptr;
    }

    auto delimiter_str = delimiter->resolve_as_list(this)[0];

    auto transform = [&](const auto& values) {
        // Translate to a list of applications of `split <delimiter>`
        Vector<NonnullRefPtr<AST::Node>> resulting_nodes;
        resulting_nodes.ensure_capacity(values.size());
        for (auto& entry : values) {
            // ImmediateExpression(split <delimiter> <entry>)
            resulting_nodes.unchecked_append(AST::make_ref_counted<AST::ImmediateExpression>(
                arguments[1].position(),
                invoking_node.function(),
                NonnullRefPtrVector<AST::Node> { Vector<NonnullRefPtr<AST::Node>> {
                    arguments[0],
                    AST::make_ref_counted<AST::SyntheticNode>(arguments[1].position(), NonnullRefPtr<AST::Value>(entry)),
                } },
                arguments[1].position()));
        }

        return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(resulting_nodes));
    };

    if (auto list = dynamic_cast<AST::ListValue*>(value.ptr())) {
        return transform(list->values());
    }

    // Otherwise, just resolve to a list and transform that.
    auto list = value->resolve_as_list(this);
    if (!value->is_list()) {
        if (list.is_empty())
            return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), NonnullRefPtrVector<AST::Node> {});

        auto& value = list.first();
        Vector<String> split_strings;
        if (delimiter_str.is_empty()) {
            StringBuilder builder;
            for (auto code_point : Utf8View { value }) {
                builder.append_code_point(code_point);
                split_strings.append(builder.build());
                builder.clear();
            }
        } else {
            auto split = StringView { value }.split_view(delimiter_str, options.inline_exec_keep_empty_segments);
            split_strings.ensure_capacity(split.size());
            for (auto& entry : split)
                split_strings.append(entry);
        }
        return AST::make_ref_counted<AST::SyntheticNode>(invoking_node.position(), AST::make_ref_counted<AST::ListValue>(move(split_strings)));
    }

    return transform(AST::make_ref_counted<AST::ListValue>(list)->values());
}

RefPtr<AST::Node> Shell::immediate_concat_lists(AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
    NonnullRefPtrVector<AST::Node> result;

    for (auto& argument : arguments) {
        if (auto* list = dynamic_cast<const AST::ListConcatenate*>(&argument)) {
            result.extend(list->list());
        } else {
            auto list_of_values = const_cast<AST::Node&>(argument).run(this)->resolve_without_cast(this);
            if (auto* list = dynamic_cast<AST::ListValue*>(list_of_values.ptr())) {
                for (auto& entry : static_cast<Vector<NonnullRefPtr<AST::Value>>&>(list->values()))
                    result.append(AST::make_ref_counted<AST::SyntheticNode>(argument.position(), entry));
            } else {
                auto values = list_of_values->resolve_as_list(this);
                for (auto& entry : values)
                    result.append(AST::make_ref_counted<AST::StringLiteral>(argument.position(), entry));
            }
        }
    }

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(result));
}

RefPtr<AST::Node> Shell::run_immediate_function(StringView str, AST::ImmediateExpression& invoking_node, const NonnullRefPtrVector<AST::Node>& arguments)
{
#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(name) \
    if (str == #name)                              \
        return immediate_##name(invoking_node, arguments);

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION
    raise_error(ShellError::EvaluatedSyntaxError, String::formatted("Unknown immediate function {}", str), invoking_node.position());
    return nullptr;
}

bool Shell::has_immediate_function(StringView str)
{
#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(name) \
    if (str == #name)                              \
        return true;

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION

    return false;
}
}
