/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Formatter.h"
#include "Shell.h"
#include <LibRegex/Regex.h>
#include <math.h>

namespace Shell {

ErrorOr<RefPtr<AST::Node>> Shell::immediate_length_impl(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments, bool across)
{
    auto name = across ? "length_across" : "length";
    if (arguments.size() < 1 || arguments.size() > 2) {
        raise_error(ShellError::EvaluatedSyntaxError, ByteString::formatted("Expected one or two arguments to `{}'", name), invoking_node.position());
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
        if (!mode_arg->is_bareword()) {
            raise_error(ShellError::EvaluatedSyntaxError, ByteString::formatted("Expected a bareword (either 'string' or 'list') in the two-argument form of the `{}' immediate", name), mode_arg->position());
            return nullptr;
        }

        auto const& mode_name = static_cast<const AST::BarewordLiteral&>(*mode_arg).text();
        if (mode_name == "list") {
            mode = List;
        } else if (mode_name == "string") {
            mode = String;
        } else if (mode_name == "infer") {
            mode = Infer;
        } else {
            raise_error(ShellError::EvaluatedSyntaxError, ByteString::formatted("Expected either 'string' or 'list' (and not {}) in the two-argument form of the `{}' immediate", mode_name, name), mode_arg->position());
            return nullptr;
        }

        expr_node = arguments[1];
    } else {
        expr_node = arguments[0];
    }

    if (mode == Infer) {
        is_inferred = true;
        if (expr_node->is_list())
            mode = List;
        else if (expr_node->is_simple_variable()) // "Look inside" variables
            mode = TRY(TRY(const_cast<AST::Node*>(expr_node)->run(this))->resolve_without_cast(this))->is_list_without_resolution() ? List : String;
        else if (is<AST::ImmediateExpression>(expr_node))
            mode = List;
        else
            mode = String;
    }

    auto value_with_number = [&](auto number) -> ErrorOr<NonnullRefPtr<AST::Node>> {
        return AST::make_ref_counted<AST::BarewordLiteral>(invoking_node.position(), String::number(number));
    };

    auto do_across = [&](StringView mode_name, auto& values) -> ErrorOr<RefPtr<AST::Node>> {
        if (is_inferred)
            mode_name = "infer"sv;
        // Translate to a list of applications of `length <mode_name>`
        Vector<NonnullRefPtr<AST::Node>> resulting_nodes;
        resulting_nodes.ensure_capacity(values.size());
        for (auto& entry : values) {
            // ImmediateExpression(length <mode_name> <entry>)
            resulting_nodes.unchecked_append(AST::make_ref_counted<AST::ImmediateExpression>(
                expr_node->position(),
                AST::NameWithPosition { "length"_string, invoking_node.function_position() },
                Vector<NonnullRefPtr<AST::Node>> { Vector<NonnullRefPtr<AST::Node>> {
                    static_cast<NonnullRefPtr<AST::Node>>(AST::make_ref_counted<AST::BarewordLiteral>(expr_node->position(), TRY(String::from_utf8(mode_name)))),
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
        auto value = TRY(const_cast<AST::Node*>(expr_node)->run(this));
        if (!value)
            return value_with_number(0);

        value = TRY(value->resolve_without_cast(this));

        if (auto list = dynamic_cast<AST::ListValue*>(value.ptr())) {
            if (across)
                return do_across("list"sv, list->values());

            return value_with_number(list->values().size());
        }

        auto list = TRY(value->resolve_as_list(this));
        if (!across)
            return value_with_number(list.size());

        dbgln("List has {} entries", list.size());
        auto values = AST::make_ref_counted<AST::ListValue>(move(list));
        return do_across("list"sv, values->values());
    }
    case String: {
        // 'across' will only accept lists, and '!across' will only accept non-lists here.
        if (expr_node->is_list()) {
            if (!across) {
            raise_no_list_allowed:;
                Formatter formatter { *expr_node };

                if (is_inferred) {
                    raise_error(ShellError::EvaluatedSyntaxError,
                        ByteString::formatted("Could not infer expression type, please explicitly use `{0} string' or `{0} list'", name),
                        invoking_node.position());
                    return nullptr;
                }

                auto source = formatter.format();
                raise_error(ShellError::EvaluatedSyntaxError,
                    source.is_empty()
                        ? "Invalid application of `length' to a list"
                        : ByteString::formatted("Invalid application of `length' to a list\nperhaps you meant `{1}length \"{0}\"{2}' or `{1}length_across {0}{2}'?", source, "\x1b[32m", "\x1b[0m"),
                    expr_node->position());
                return nullptr;
            }
        }

        auto value = TRY(const_cast<AST::Node*>(expr_node)->run(this));
        if (!value)
            return value_with_number(0);

        value = TRY(value->resolve_without_cast(*this));

        if (auto list = dynamic_cast<AST::ListValue*>(value.ptr())) {
            if (!across)
                goto raise_no_list_allowed;

            return do_across("string"sv, list->values());
        }

        if (across && !value->is_list()) {
            Formatter formatter { *expr_node };

            auto source = formatter.format();
            raise_error(ShellError::EvaluatedSyntaxError,
                ByteString::formatted("Invalid application of `length_across' to a non-list\nperhaps you meant `{1}length {0}{2}'?", source, "\x1b[32m", "\x1b[0m"),
                expr_node->position());
            return nullptr;
        }

        // Evaluate the nodes and substitute with the lengths.
        auto list = TRY(value->resolve_as_list(this));

        if (!expr_node->is_list()) {
            if (list.size() == 1) {
                if (across)
                    goto raise_no_list_allowed;

                // This is the normal case, the expression is a normal non-list expression.
                return value_with_number(list.first().bytes_as_string_view().length());
            }

            // This can be hit by asking for the length of a command list (e.g. `(>/dev/null)`)
            // raise an error about misuse of command lists for now.
            // FIXME: What's the length of `(>/dev/null)` supposed to be?
            raise_error(ShellError::EvaluatedSyntaxError, "Length of meta value (or command list) requested, this is currently not supported.", expr_node->position());
            return nullptr;
        }

        auto values = AST::make_ref_counted<AST::ListValue>(move(list));
        return do_across("string"sv, values->values());
    }
    }
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_length(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    return immediate_length_impl(invoking_node, arguments, false);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_length_across(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    return immediate_length_impl(invoking_node, arguments, true);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_regex_replace(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 3) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 3 arguments to regex_replace", invoking_node.position());
        return nullptr;
    }

    auto pattern = TRY(const_cast<AST::Node&>(*arguments[0]).run(this));
    auto replacement = TRY(const_cast<AST::Node&>(*arguments[1]).run(this));
    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments[2]).run(this))->resolve_without_cast(this));

    if (!pattern->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the regex_replace pattern to be a string", arguments[0]->position());
        return nullptr;
    }

    if (!replacement->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the regex_replace replacement string to be a string", arguments[1]->position());
        return nullptr;
    }

    if (!value->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the regex_replace target value to be a string", arguments[2]->position());
        return nullptr;
    }

    Regex<PosixExtendedParser> re { TRY(pattern->resolve_as_list(this)).first().to_byte_string() };
    auto result = re.replace(
        TRY(value->resolve_as_list(this))[0],
        TRY(replacement->resolve_as_list(this))[0],
        PosixFlags::Global | PosixFlags::Multiline | PosixFlags::Unicode);

    return AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), TRY(String::from_byte_string(result)), AST::StringLiteral::EnclosureType::None);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_remove_suffix(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to remove_suffix", invoking_node.position());
        return nullptr;
    }

    auto suffix = TRY(const_cast<AST::Node&>(*arguments[0]).run(this));
    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments[1]).run(this))->resolve_without_cast(this));

    if (!suffix->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the remove_suffix suffix string to be a string", arguments[0]->position());
        return nullptr;
    }

    auto suffix_str = TRY(suffix->resolve_as_list(this))[0];
    auto values = TRY(value->resolve_as_list(this));

    Vector<NonnullRefPtr<AST::Node>> nodes;

    for (auto& value_str : values) {
        String removed = value_str;

        if (value_str.bytes_as_string_view().ends_with(suffix_str))
            removed = TRY(removed.substring_from_byte_offset(0, value_str.bytes_as_string_view().length() - suffix_str.bytes_as_string_view().length()));

        nodes.append(AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), move(removed), AST::StringLiteral::EnclosureType::None));
    }

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(nodes));
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_remove_prefix(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to remove_prefix", invoking_node.position());
        return nullptr;
    }

    auto prefix = TRY(const_cast<AST::Node&>(*arguments[0]).run(this));
    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments[1]).run(this))->resolve_without_cast(this));

    if (!prefix->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the remove_prefix prefix string to be a string", arguments[0]->position());
        return nullptr;
    }

    auto prefix_str = TRY(prefix->resolve_as_list(this))[0];
    auto values = TRY(value->resolve_as_list(this));

    Vector<NonnullRefPtr<AST::Node>> nodes;

    for (auto& value_str : values) {
        String removed = value_str;

        if (value_str.bytes_as_string_view().starts_with(prefix_str))
            removed = TRY(removed.substring_from_byte_offset(prefix_str.bytes_as_string_view().length()));
        nodes.append(AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), move(removed), AST::StringLiteral::EnclosureType::None));
    }

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(nodes));
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_split(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to split", invoking_node.position());
        return nullptr;
    }

    auto delimiter = TRY(const_cast<AST::Node&>(*arguments[0]).run(this));
    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments[1]).run(this))->resolve_without_cast(this));

    if (!delimiter->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the split delimiter string to be a string", arguments[0]->position());
        return nullptr;
    }

    auto delimiter_str = TRY(delimiter->resolve_as_list(this))[0];

    auto transform = [&](auto const& values) {
        // Translate to a list of applications of `split <delimiter>`
        Vector<NonnullRefPtr<AST::Node>> resulting_nodes;
        resulting_nodes.ensure_capacity(values.size());
        for (auto& entry : values) {
            // ImmediateExpression(split <delimiter> <entry>)
            resulting_nodes.unchecked_append(AST::make_ref_counted<AST::ImmediateExpression>(
                arguments[1]->position(),
                invoking_node.function(),
                Vector<NonnullRefPtr<AST::Node>> { Vector<NonnullRefPtr<AST::Node>> {
                    arguments[0],
                    AST::make_ref_counted<AST::SyntheticNode>(arguments[1]->position(), NonnullRefPtr<AST::Value>(entry)),
                } },
                arguments[1]->position()));
        }

        return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(resulting_nodes));
    };

    if (auto list = dynamic_cast<AST::ListValue*>(value.ptr())) {
        return transform(list->values());
    }

    // Otherwise, just resolve to a list and transform that.
    auto list = TRY(value->resolve_as_list(this));
    if (!value->is_list()) {
        if (list.is_empty())
            return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), Vector<NonnullRefPtr<AST::Node>> {});

        auto& value = list.first();
        Vector<String> split_strings;
        if (delimiter_str.is_empty()) {
            StringBuilder builder;
            for (auto code_point : Utf8View { value }) {
                builder.append_code_point(code_point);
                split_strings.append(TRY(builder.to_string()));
                builder.clear();
            }
        } else {
            auto split = StringView { value }.split_view(delimiter_str, options.inline_exec_keep_empty_segments ? SplitBehavior::KeepEmpty : SplitBehavior::Nothing);
            split_strings.ensure_capacity(split.size());
            for (auto& entry : split)
                split_strings.append(TRY(String::from_utf8(entry)));
        }
        return AST::make_ref_counted<AST::SyntheticNode>(invoking_node.position(), AST::make_ref_counted<AST::ListValue>(move(split_strings)));
    }

    return transform(AST::make_ref_counted<AST::ListValue>(list)->values());
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_concat_lists(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    Vector<NonnullRefPtr<AST::Node>> result;

    for (auto& argument : arguments) {
        if (auto* list = dynamic_cast<AST::ListConcatenate const*>(argument.ptr())) {
            result.extend(list->list());
        } else {
            auto list_of_values = TRY(TRY(const_cast<AST::Node&>(*argument).run(this))->resolve_without_cast(this));
            if (auto* list = dynamic_cast<AST::ListValue*>(list_of_values.ptr())) {
                for (auto& entry : static_cast<Vector<NonnullRefPtr<AST::Value>>&>(list->values()))
                    result.append(AST::make_ref_counted<AST::SyntheticNode>(argument->position(), entry));
            } else {
                auto values = TRY(list_of_values->resolve_as_list(this));
                for (auto& entry : values)
                    result.append(AST::make_ref_counted<AST::StringLiteral>(argument->position(), entry, AST::StringLiteral::EnclosureType::None));
            }
        }
    }

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(result));
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_filter_glob(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    // filter_glob string list
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly two arguments to filter_glob (<glob> <list>)", invoking_node.position());
        return nullptr;
    }

    auto glob_list = TRY(TRY(const_cast<AST::Node&>(*arguments[0]).run(*this))->resolve_as_list(*this));
    if (glob_list.size() != 1) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the <glob> argument to filter_glob to be a single string", arguments[0]->position());
        return nullptr;
    }
    auto& glob = glob_list.first();
    auto& list_node = arguments[1];

    Vector<NonnullRefPtr<AST::Node>> result;

    TRY(const_cast<AST::Node&>(*list_node).for_each_entry(*this, [&](NonnullRefPtr<AST::Value> entry) -> ErrorOr<IterationDecision> {
        auto value = TRY(entry->resolve_as_list(*this));
        if (value.size() == 0)
            return IterationDecision::Continue;
        if (value.size() == 1) {
            if (!value.first().bytes_as_string_view().matches(glob))
                return IterationDecision::Continue;
            result.append(AST::make_ref_counted<AST::StringLiteral>(arguments[1]->position(), value.first(), AST::StringLiteral::EnclosureType::None));
            return IterationDecision::Continue;
        }

        for (auto& entry : value) {
            if (entry.bytes_as_string_view().matches(glob)) {
                Vector<NonnullRefPtr<AST::Node>> nodes;
                for (auto& string : value)
                    nodes.append(AST::make_ref_counted<AST::StringLiteral>(arguments[1]->position(), string, AST::StringLiteral::EnclosureType::None));
                result.append(AST::make_ref_counted<AST::ListConcatenate>(arguments[1]->position(), move(nodes)));
                return IterationDecision::Continue;
            }
        }
        return IterationDecision::Continue;
    }));

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(result));
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_join(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to join", invoking_node.position());
        return nullptr;
    }

    auto delimiter = TRY(const_cast<AST::Node&>(*arguments[0]).run(this));
    if (!delimiter->is_string()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the join delimiter string to be a string", arguments[0]->position());
        return nullptr;
    }

    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments[1]).run(this))->resolve_without_cast(this));
    if (!value->is_list()) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected the joined list to be a list", arguments[1]->position());
        return nullptr;
    }

    auto delimiter_str = TRY(delimiter->resolve_as_list(this))[0];
    StringBuilder builder;
    builder.join(delimiter_str, TRY(value->resolve_as_list(*this)));

    return AST::make_ref_counted<AST::StringLiteral>(invoking_node.position(), TRY(builder.to_string()), AST::StringLiteral::EnclosureType::None);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_value_or_default(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to value_or_default", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (!TRY(local_variable_or(name, ""sv)).is_empty())
        return make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);

    return arguments.last();
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_assign_default(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to assign_default", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (!TRY(local_variable_or(name, ""sv)).is_empty())
        return make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);

    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments.last()).run(*this))->resolve_without_cast(*this));
    set_local_variable(name.to_byte_string(), value);

    return make_ref_counted<AST::SyntheticNode>(invoking_node.position(), value);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_error_if_empty(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to error_if_empty", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (!TRY(local_variable_or(name, ""sv)).is_empty())
        return make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);

    auto error_value = TRY(TRY(const_cast<AST::Node&>(*arguments.last()).run(*this))->resolve_as_string(*this));
    if (error_value.is_empty())
        error_value = TRY(String::formatted("Expected {} to be non-empty", name));

    raise_error(ShellError::EvaluatedSyntaxError, error_value.bytes_as_string_view(), invoking_node.position());
    return nullptr;
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_null_or_alternative(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to null_or_alternative", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    auto frame = find_frame_containing_local_variable(name);
    if (!frame)
        return make_ref_counted<AST::StringLiteral>(invoking_node.position(), ""_string, AST::StringLiteral::EnclosureType::None);

    auto value = frame->local_variables.get(name.bytes_as_string_view()).value();
    if ((value->is_string() && TRY(value->resolve_as_string(*this)).is_empty()) || (value->is_list() && TRY(value->resolve_as_list(*this)).is_empty()))
        return make_ref_counted<AST::SyntheticNode>(invoking_node.position(), *value);

    return arguments.last();
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_defined_value_or_default(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to defined_value_or_default", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (!find_frame_containing_local_variable(name))
        return arguments.last();

    return make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_assign_defined_default(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to assign_defined_default", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (find_frame_containing_local_variable(name))
        return make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);

    auto value = TRY(TRY(const_cast<AST::Node&>(*arguments.last()).run(*this))->resolve_without_cast(*this));
    set_local_variable(name.to_byte_string(), value);

    return make_ref_counted<AST::SyntheticNode>(invoking_node.position(), value);
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_error_if_unset(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to error_if_unset", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (find_frame_containing_local_variable(name))
        return make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);

    auto error_value = TRY(TRY(const_cast<AST::Node&>(*arguments.last()).run(*this))->resolve_as_string(*this));
    if (error_value.is_empty())
        error_value = TRY(String::formatted("Expected {} to be set", name));

    raise_error(ShellError::EvaluatedSyntaxError, error_value.bytes_as_string_view(), invoking_node.position());
    return nullptr;
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_null_if_unset_or_alternative(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 2) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 2 arguments to null_if_unset_or_alternative", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    if (find_frame_containing_local_variable(name))
        return arguments.last();

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), Vector<NonnullRefPtr<AST::Node>>());
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_reexpand(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 1) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 1 argument to reexpand", invoking_node.position());
        return nullptr;
    }

    auto values = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_list(*this));
    auto result = Vector<NonnullRefPtr<AST::Node>> {};
    for (auto& value : values) {
        if (auto node = parse(value, m_is_interactive, false))
            result.append(node.release_nonnull());
    }

    if (values.size() == 1)
        return result.take_first();

    return AST::make_ref_counted<AST::ListConcatenate>(invoking_node.position(), move(result));
}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_length_of_variable(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 1) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 1 argument to length_of_variable", invoking_node.position());
        return nullptr;
    }

    auto name = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_string(*this));
    auto variable = make_ref_counted<AST::SimpleVariable>(invoking_node.position(), name);

    return immediate_length_impl(
        invoking_node,
        { move(variable) },
        false);
}

namespace Arithmetic {
struct BinaryOperationNode;
struct UnaryOperationNode;
struct TernaryOperationNode;
struct ErrorNode;

struct Node {
    Variant<String, i64, NonnullOwnPtr<BinaryOperationNode>, NonnullOwnPtr<UnaryOperationNode>, NonnullOwnPtr<TernaryOperationNode>, NonnullOwnPtr<ErrorNode>> value;
};

struct ErrorNode {
    String error;
};

enum class Operator {
    Add,                  // +
    Subtract,             // -
    Multiply,             // *
    Quotient,             // /
    Remainder,            // %
    Power,                // **
    Equal,                // ==
    GreaterThan,          // >
    LessThan,             // <
    NotEqual,             // !=
    GreaterThanOrEqual,   // >=
    LessThanOrEqual,      // <=
    BitwiseAnd,           // &
    BitwiseOr,            // |
    BitwiseXor,           // ^
    ShiftLeft,            // <<
    ShiftRight,           // >>
    ArithmeticAnd,        // &&
    ArithmeticOr,         // ||
    Comma,                // ,
    Negate,               // !
    BitwiseNegate,        // ~
    TernaryQuestion,      // ?
    TernaryColon,         // :
    Assignment,           // =
    PlusAssignment,       // +=
    MinusAssignment,      // -=
    MultiplyAssignment,   // *=
    DivideAssignment,     // /=
    ModuloAssignment,     // %=
    AndAssignment,        // &=
    OrAssignment,         // |=
    XorAssignment,        // ^=
    LeftShiftAssignment,  // <<=
    RightShiftAssignment, // >>=

    OpenParen,  // (
    CloseParen, // )
};

static Operator assignment_operation_of(Operator op)
{
    switch (op) {
    case Operator::PlusAssignment:
        return Operator::Add;
    case Operator::MinusAssignment:
        return Operator::Subtract;
    case Operator::MultiplyAssignment:
        return Operator::Multiply;
    case Operator::DivideAssignment:
        return Operator::Quotient;
    case Operator::ModuloAssignment:
        return Operator::Remainder;
    case Operator::AndAssignment:
        return Operator::BitwiseAnd;
    case Operator::OrAssignment:
        return Operator::BitwiseOr;
    case Operator::XorAssignment:
        return Operator::BitwiseXor;
    case Operator::LeftShiftAssignment:
        return Operator::ShiftLeft;
    case Operator::RightShiftAssignment:
        return Operator::ShiftRight;
    default:
        VERIFY_NOT_REACHED();
    }
}

static bool is_assignment_operator(Operator op)
{
    switch (op) {
    case Operator::Assignment:
    case Operator::PlusAssignment:
    case Operator::MinusAssignment:
    case Operator::MultiplyAssignment:
    case Operator::DivideAssignment:
    case Operator::ModuloAssignment:
    case Operator::AndAssignment:
    case Operator::OrAssignment:
    case Operator::XorAssignment:
    case Operator::LeftShiftAssignment:
    case Operator::RightShiftAssignment:
        return true;
    default:
        return false;
    }
}

using Token = Variant<String, i64, Operator>;

struct BinaryOperationNode {
    BinaryOperationNode(Operator op, Node lhs, Node rhs)
        : op(op)
        , lhs(move(lhs))
        , rhs(move(rhs))
    {
    }

    Operator op;
    Node lhs;
    Node rhs;
};

struct UnaryOperationNode {
    UnaryOperationNode(Operator op, Node rhs)
        : op(op)
        , rhs(move(rhs))
    {
    }

    Operator op;
    Node rhs;
};

struct TernaryOperationNode {
    TernaryOperationNode(Node condition, Node true_value, Node false_value)
        : condition(move(condition))
        , true_value(move(true_value))
        , false_value(move(false_value))
    {
    }

    Node condition;
    Node true_value;
    Node false_value;
};

static ErrorOr<Node> parse_expression(Span<Token>);
static ErrorOr<Node> parse_assignment_expression(Span<Token>&);
static ErrorOr<Node> parse_comma_expression(Span<Token>&);
static ErrorOr<Node> parse_ternary_expression(Span<Token>&);
static ErrorOr<Node> parse_logical_or_expression(Span<Token>&);
static ErrorOr<Node> parse_logical_and_expression(Span<Token>&);
static ErrorOr<Node> parse_bitwise_or_expression(Span<Token>&);
static ErrorOr<Node> parse_bitwise_xor_expression(Span<Token>&);
static ErrorOr<Node> parse_bitwise_and_expression(Span<Token>&);
static ErrorOr<Node> parse_equality_expression(Span<Token>&);
static ErrorOr<Node> parse_comparison_expression(Span<Token>&);
static ErrorOr<Node> parse_shift_expression(Span<Token>&);
static ErrorOr<Node> parse_additive_expression(Span<Token>&);
static ErrorOr<Node> parse_multiplicative_expression(Span<Token>&);
static ErrorOr<Node> parse_exponential_expression(Span<Token>&);
static ErrorOr<Node> parse_unary_expression(Span<Token>&);
static ErrorOr<Node> parse_primary_expression(Span<Token>&);
template<size_t N>
static ErrorOr<Node> parse_binary_expression_using_operators(Span<Token>&, Array<Operator, N>, Function<ErrorOr<Node>(Span<Token>&)> const& parse_rhs);
static ErrorOr<Node> parse_binary_expression_using_operator(Span<Token>& tokens, Operator op, Function<ErrorOr<Node>(Span<Token>&)> const& parse_rhs)
{
    return parse_binary_expression_using_operators(tokens, Array { op }, parse_rhs);
}

static bool next_token_is_operator(Span<Token>& tokens, Operator op)
{
    if (tokens.is_empty())
        return false;
    return tokens.first().has<Operator>() && tokens.first().get<Operator>() == op;
}

ErrorOr<Node> parse_expression(Span<Token> tokens)
{
    return parse_comma_expression(tokens);
}

ErrorOr<Node> parse_comma_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operator(tokens, Operator::Comma, &parse_assignment_expression);
}

ErrorOr<Node> parse_assignment_expression(Span<Token>& tokens)
{
    auto lhs = TRY(parse_ternary_expression(tokens));
    if (tokens.is_empty())
        return lhs;

    auto is_assignment_operator = [](Operator op) {
        return op == Operator::Assignment
            || op == Operator::PlusAssignment
            || op == Operator::MinusAssignment
            || op == Operator::MultiplyAssignment
            || op == Operator::DivideAssignment
            || op == Operator::ModuloAssignment
            || op == Operator::AndAssignment
            || op == Operator::OrAssignment
            || op == Operator::XorAssignment
            || op == Operator::LeftShiftAssignment
            || op == Operator::RightShiftAssignment;
    };

    auto& token = tokens.first();
    if (auto op = token.get_pointer<Operator>(); op && is_assignment_operator(*op)) {
        if (!lhs.value.has<String>()) {
            return Node {
                make<ErrorNode>("Left-hand side of assignment must be a variable"_string)
            };
        }

        tokens = tokens.slice(1);
        auto rhs = TRY(parse_assignment_expression(tokens));
        return Node {
            make<BinaryOperationNode>(*op, move(lhs), move(rhs))
        };
    }

    return lhs;
}

ErrorOr<Node> parse_ternary_expression(Span<Token>& tokens)
{
    auto condition = TRY(parse_logical_or_expression(tokens));
    if (!next_token_is_operator(tokens, Operator::TernaryQuestion))
        return condition;

    tokens = tokens.slice(1);

    auto true_value = TRY(parse_comma_expression(tokens));

    if (!next_token_is_operator(tokens, Operator::TernaryColon)) {
        return Node {
            make<ErrorNode>("Expected ':' after true value in ternary expression"_string)
        };
    }

    tokens = tokens.slice(1);

    auto false_value = TRY(parse_ternary_expression(tokens));

    return Node {
        make<TernaryOperationNode>(move(condition), move(true_value), move(false_value))
    };
}

ErrorOr<Node> parse_logical_or_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operator(tokens, Operator::ArithmeticOr, &parse_logical_and_expression);
}

ErrorOr<Node> parse_logical_and_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operator(tokens, Operator::ArithmeticAnd, &parse_bitwise_or_expression);
}

ErrorOr<Node> parse_bitwise_or_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operator(tokens, Operator::BitwiseOr, &parse_bitwise_xor_expression);
}

ErrorOr<Node> parse_bitwise_xor_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operator(tokens, Operator::BitwiseXor, &parse_bitwise_and_expression);
}

ErrorOr<Node> parse_bitwise_and_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operator(tokens, Operator::BitwiseAnd, &parse_equality_expression);
}

ErrorOr<Node> parse_equality_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operators(tokens, Array { Operator::Equal, Operator::NotEqual }, &parse_comparison_expression);
}

ErrorOr<Node> parse_comparison_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operators(tokens, Array { Operator::LessThan, Operator::GreaterThan, Operator::LessThanOrEqual, Operator::GreaterThanOrEqual }, &parse_shift_expression);
}

ErrorOr<Node> parse_shift_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operators(tokens, Array { Operator::ShiftLeft, Operator::ShiftRight }, &parse_additive_expression);
}

ErrorOr<Node> parse_additive_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operators(tokens, Array { Operator::Add, Operator::Subtract }, &parse_multiplicative_expression);
}

ErrorOr<Node> parse_multiplicative_expression(Span<Token>& tokens)
{
    return parse_binary_expression_using_operators(tokens, Array { Operator::Multiply, Operator::Quotient, Operator::Remainder }, &parse_exponential_expression);
}

ErrorOr<Node> parse_exponential_expression(Span<Token>& tokens)
{
    auto lhs = TRY(parse_unary_expression(tokens));
    if (!next_token_is_operator(tokens, Operator::Power))
        return lhs;

    tokens = tokens.slice(1);
    auto rhs = TRY(parse_exponential_expression(tokens));

    return Node {
        make<BinaryOperationNode>(Operator::Power, move(lhs), move(rhs))
    };
}

ErrorOr<Node> parse_unary_expression(Span<Token>& tokens)
{
    if (tokens.is_empty()) {
        return Node {
            make<ErrorNode>("Expected expression, got end of input"_string)
        };
    }

    auto& token = tokens.first();
    if (auto op = token.get_pointer<Operator>()) {
        if (*op == Operator::Add || *op == Operator::Subtract || *op == Operator::Negate || *op == Operator::BitwiseNegate) {
            tokens = tokens.slice(1);
            auto rhs = TRY(parse_unary_expression(tokens));
            return Node {
                make<UnaryOperationNode>(*op, move(rhs))
            };
        }
    }

    return parse_primary_expression(tokens);
}

ErrorOr<Node> parse_primary_expression(Span<Token>& tokens)
{
    if (tokens.is_empty())
        return Node { make<ErrorNode>("Expected expression, got end of input"_string) };

    auto& token = tokens.first();
    return token.visit(
        [&](String const& var) -> ErrorOr<Node> {
            tokens = tokens.slice(1);
            return Node { var };
        },
        [&](i64 value) -> ErrorOr<Node> {
            tokens = tokens.slice(1);
            return Node { value };
        },
        [&](Operator op) -> ErrorOr<Node> {
            switch (op) {
            case Operator::OpenParen: {
                tokens = tokens.slice(1);
                auto value = TRY(parse_expression(tokens));
                if (!next_token_is_operator(tokens, Operator::CloseParen)) {
                    return Node {
                        make<ErrorNode>("Expected ')' after expression in parentheses"_string)
                    };
                }
                tokens = tokens.slice(1);
                return value;
            }
            default:
                return Node {
                    make<ErrorNode>("Expected expression, got operator"_string)
                };
            }
        });
}

template<size_t N>
ErrorOr<Node> parse_binary_expression_using_operators(Span<Token>& tokens, Array<Operator, N> operators, Function<ErrorOr<Node>(Span<Token>&)> const& parse_rhs)
{
    auto lhs = TRY(parse_rhs(tokens));
    for (;;) {
        Optional<Operator> op;
        for (auto candidate : operators) {
            if (next_token_is_operator(tokens, candidate)) {
                op = candidate;
                break;
            }
        }

        if (!op.has_value())
            return lhs;

        tokens = tokens.slice(1);
        auto rhs = TRY(parse_rhs(tokens));
        lhs = Node {
            make<BinaryOperationNode>(*op, move(lhs), move(rhs))
        };
    }
}

}

ErrorOr<RefPtr<AST::Node>> Shell::immediate_math(AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
    if (arguments.size() != 1) {
        raise_error(ShellError::EvaluatedSyntaxError, "Expected exactly 1 argument to math", invoking_node.position());
        return nullptr;
    }

    auto expression_parts = TRY(TRY(const_cast<AST::Node&>(*arguments.first()).run(*this))->resolve_as_list(*this));
    auto expression = TRY(String::join(' ', expression_parts));

    using Arithmetic::Operator;
    using Arithmetic::Token;

    Vector<Token> tokens;

    auto view = expression.code_points();
    Optional<size_t> integer_or_word_start_offset;
    for (auto it = view.begin(); it != view.end(); ++it) {
        auto code_point = *it;
        if (is_ascii_alphanumeric(code_point) || code_point == U'_') {
            if (!integer_or_word_start_offset.has_value())
                integer_or_word_start_offset = view.byte_offset_of(it);
            continue;
        }

        if (integer_or_word_start_offset.has_value()) {
            auto integer_or_word = view.substring_view(
                *integer_or_word_start_offset,
                view.byte_offset_of(it) - *integer_or_word_start_offset);

            if (all_of(integer_or_word, is_ascii_digit))
                tokens.append(*integer_or_word.as_string().to_number<int>());
            else
                tokens.append(TRY(expression.substring_from_byte_offset_with_shared_superstring(*integer_or_word_start_offset, integer_or_word.length())));

            integer_or_word_start_offset.clear();
        }

        switch (code_point) {
        case U'!':
            if (it.peek(1) == U'=') {
                ++it;
                tokens.append(Operator::NotEqual);
            } else {
                tokens.append(Operator::Negate);
            }
            break;
        case U'=':
            if (it.peek(1) == U'=') {
                ++it;
                tokens.append(Operator::Equal);
            } else {
                tokens.append(Operator::Assignment);
            }
            break;
        case U'~':
            tokens.append(Operator::BitwiseNegate);
            break;
        case U'(':
            tokens.append(Operator::OpenParen);
            break;
        case U')':
            tokens.append(Operator::CloseParen);
            break;
        case U'&':
            switch (it.peek(1).value_or(0)) {
            case U'&':
                ++it;
                tokens.append(Operator::ArithmeticAnd);
                break;
            case U'=':
                ++it;
                tokens.append(Operator::AndAssignment);
                break;
            default:
                tokens.append(Operator::BitwiseAnd);
                break;
            }
            break;
        case U'|':
            switch (it.peek(1).value_or(0)) {
            case U'|':
                ++it;
                tokens.append(Operator::ArithmeticOr);
                break;
            case U'=':
                ++it;
                tokens.append(Operator::OrAssignment);
                break;
            default:
                tokens.append(Operator::BitwiseOr);
                break;
            }
            break;
        case U'^':
            if (it.peek(1) == U'=') {
                ++it;
                tokens.append(Operator::XorAssignment);
            } else {
                tokens.append(Operator::BitwiseXor);
            }
            break;
        case U',':
            tokens.append(Operator::Comma);
            break;
        case U'?':
            tokens.append(Operator::TernaryQuestion);
            break;
        case U':':
            tokens.append(Operator::TernaryColon);
            break;
        case U'+':
            switch (it.peek(1).value_or(0)) {
            case U'=':
                ++it;
                tokens.append(Operator::PlusAssignment);
                break;
            default:
                tokens.append(Operator::Add);
                break;
            }
            break;
        case U'-':
            switch (it.peek(1).value_or(0)) {
            case U'=':
                ++it;
                tokens.append(Operator::MinusAssignment);
                break;
            default:
                tokens.append(Operator::Subtract);
                break;
            }
            break;
        case U'*':
            switch (it.peek(1).value_or(0)) {
            case U'=':
                ++it;
                tokens.append(Operator::MultiplyAssignment);
                break;
            case U'*':
                ++it;
                tokens.append(Operator::Power);
                break;
            default:
                tokens.append(Operator::Multiply);
                break;
            }
            break;
        case U'/':
            if (it.peek(1) == U'=') {
                ++it;
                tokens.append(Operator::DivideAssignment);
            } else {
                tokens.append(Operator::Quotient);
            }
            break;
        case U'%':
            if (it.peek(1) == U'=') {
                ++it;
                tokens.append(Operator::ModuloAssignment);
            } else {
                tokens.append(Operator::Remainder);
            }
            break;
        case U'<':
            switch (it.peek(1).value_or(0)) {
            case U'<':
                ++it;
                if (it.peek(1) == U'=') {
                    ++it;
                    tokens.append(Operator::LeftShiftAssignment);
                } else {
                    tokens.append(Operator::ShiftLeft);
                }
                break;
            case U'=':
                ++it;
                tokens.append(Operator::LessThanOrEqual);
                break;
            default:
                tokens.append(Operator::LessThan);
                break;
            }
            break;
        case U'>':
            switch (it.peek(1).value_or(0)) {
            case U'>':
                ++it;
                if (it.peek(1) == U'=') {
                    ++it;
                    tokens.append(Operator::RightShiftAssignment);
                } else {
                    tokens.append(Operator::ShiftRight);
                }
                break;
            case U'=':
                ++it;
                tokens.append(Operator::GreaterThanOrEqual);
                break;
            default:
                tokens.append(Operator::GreaterThan);
                break;
            }
            break;
        case U' ':
        case U'\t':
        case U'\n':
        case U'\r':
            break;
        default:
            raise_error(ShellError::EvaluatedSyntaxError, ByteString::formatted("Unexpected character '{:c}' in math expression", code_point), arguments.first()->position());
            return nullptr;
        }
    }
    if (integer_or_word_start_offset.has_value()) {
        auto integer_or_word = view.substring_view(*integer_or_word_start_offset);

        if (all_of(integer_or_word, is_ascii_digit))
            tokens.append(*integer_or_word.as_string().to_number<int>());
        else
            tokens.append(TRY(expression.substring_from_byte_offset_with_shared_superstring(*integer_or_word_start_offset, integer_or_word.length())));

        integer_or_word_start_offset.clear();
    }

    auto ast = TRY(Arithmetic::parse_expression(tokens));

    // Now interpret that.
    Function<ErrorOr<i64>(Arithmetic::Node const&)> interpret = [&](Arithmetic::Node const& node) -> ErrorOr<i64> {
        return node.value.visit(
            [&](String const& name) -> ErrorOr<i64> {
                size_t resolution_attempts_remaining = 100;
                for (auto resolved_name = name; resolution_attempts_remaining > 0; --resolution_attempts_remaining) {
                    auto value = TRY(look_up_local_variable(resolved_name.bytes_as_string_view()));
                    if (!value)
                        break;

                    StringBuilder builder;
                    builder.join(' ', TRY(const_cast<AST::Value&>(*value).resolve_as_list(const_cast<Shell&>(*this))));
                    resolved_name = TRY(builder.to_string());

                    auto integer = resolved_name.to_number<i64>();
                    if (integer.has_value())
                        return *integer;
                }

                if (resolution_attempts_remaining == 0)
                    raise_error(ShellError::EvaluatedSyntaxError, ByteString::formatted("Too many indirections when resolving variable '{}'", name), arguments.first()->position());

                return 0;
            },
            [&](i64 value) -> ErrorOr<i64> {
                return value;
            },
            [&](NonnullOwnPtr<Arithmetic::BinaryOperationNode> const& node) -> ErrorOr<i64> {
                if (Arithmetic::is_assignment_operator(node->op)) {
                    // lhs must be a variable name.
                    auto name = node->lhs.value.get_pointer<String>();
                    if (!name) {
                        raise_error(ShellError::EvaluatedSyntaxError, "Invalid left-hand side of assignment", arguments.first()->position());
                        return 0;
                    }

                    auto rhs = TRY(interpret(node->rhs));

                    if (node->op != Arithmetic::Operator::Assignment) {
                        // Evaluate the new value
                        rhs = TRY(interpret(Arithmetic::Node {
                            .value = make<Arithmetic::BinaryOperationNode>(
                                Arithmetic::assignment_operation_of(node->op),
                                Arithmetic::Node { *name },
                                Arithmetic::Node { rhs }),
                        }));
                    }

                    set_local_variable(name->to_byte_string(), make_ref_counted<AST::StringValue>(String::number(rhs)));
                    return rhs;
                }

                auto lhs = TRY(interpret(node->lhs));
                auto rhs = TRY(interpret(node->rhs));

                using Arithmetic::Operator;
                switch (node->op) {
                case Operator::Add:
                    return lhs + rhs;
                case Operator::Subtract:
                    return lhs - rhs;
                case Operator::Multiply:
                    return lhs * rhs;
                case Operator::Quotient:
                    return lhs / rhs;
                case Operator::Remainder:
                    return lhs % rhs;
                case Operator::ShiftLeft:
                    return lhs << rhs;
                case Operator::ShiftRight:
                    return lhs >> rhs;
                case Operator::BitwiseAnd:
                    return lhs & rhs;
                case Operator::BitwiseOr:
                    return lhs | rhs;
                case Operator::BitwiseXor:
                    return lhs ^ rhs;
                case Operator::ArithmeticAnd:
                    return lhs != 0 && rhs != 0;
                case Operator::ArithmeticOr:
                    return lhs != 0 || rhs != 0;
                case Operator::LessThan:
                    return lhs < rhs;
                case Operator::LessThanOrEqual:
                    return lhs <= rhs;
                case Operator::GreaterThan:
                    return lhs > rhs;
                case Operator::GreaterThanOrEqual:
                    return lhs >= rhs;
                case Operator::Equal:
                    return lhs == rhs;
                case Operator::NotEqual:
                    return lhs != rhs;
                case Operator::Power:
                    return trunc(pow(static_cast<double>(lhs), static_cast<double>(rhs)));
                case Operator::Comma:
                    return rhs;
                default:
                    VERIFY_NOT_REACHED();
                }
            },
            [&](NonnullOwnPtr<Arithmetic::UnaryOperationNode> const& node) -> ErrorOr<i64> {
                auto value = TRY(interpret(node->rhs));

                switch (node->op) {
                case Arithmetic::Operator::Negate:
                    return value == 0;
                case Arithmetic::Operator::BitwiseNegate:
                    return ~value;
                case Arithmetic::Operator::Add:
                    return value;
                case Arithmetic::Operator::Subtract:
                    return -value;
                default:
                    VERIFY_NOT_REACHED();
                }
            },
            [&](NonnullOwnPtr<Arithmetic::TernaryOperationNode> const& node) -> ErrorOr<i64> {
                auto condition = TRY(interpret(node->condition));
                if (condition != 0)
                    return TRY(interpret(node->true_value));
                return TRY(interpret(node->false_value));
            },
            [&](NonnullOwnPtr<Arithmetic::ErrorNode> const& node) -> ErrorOr<i64> {
                raise_error(ShellError::EvaluatedSyntaxError, node->error.to_byte_string(), arguments.first()->position());
                return 0;
            });
    };

    auto result = TRY(interpret(ast));

    return make_ref_counted<AST::StringLiteral>(arguments.first()->position(), String::number(result), AST::StringLiteral::EnclosureType::None);
}

ErrorOr<RefPtr<AST::Node>> Shell::run_immediate_function(StringView str, AST::ImmediateExpression& invoking_node, Vector<NonnullRefPtr<AST::Node>> const& arguments)
{
#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(name) \
    if (str == #name)                              \
        return immediate_##name(invoking_node, arguments);

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION
    raise_error(ShellError::EvaluatedSyntaxError, ByteString::formatted("Unknown immediate function {}", str), invoking_node.position());
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
