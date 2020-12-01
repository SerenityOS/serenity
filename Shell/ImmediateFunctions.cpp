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

#include "ImmediateFunctions.h"
#include "Shell.h"
#include <LibRegex/Regex.h>

namespace Shell {

Array<ImmediateFunctionType, ImmediateFunction::_Count> s_immediate_functions;

void ensure_immediate_functions()
{
    static bool initialized = false;
    if (initialized)
        return;

    initialized = true;

#define IMMEDIATE_FUNCTION(fn, defn) \
    s_immediate_functions[ImmediateFunction::fn] = [](Shell & shell, NonnullRefPtrVector<AST::Node> arguments, Function<IterationDecision(NonnullRefPtr<AST::Node>)> callback) -> void defn;

    IMMEDIATE_FUNCTION(count, {
        for (auto& argument : arguments) {
            size_t count = 0;
            argument.for_each_entry(shell, [&](auto) {
                ++count;
                return IterationDecision::Continue;
            });
            if (callback(AST::create<AST::StringLiteral>(argument.position(), String::number(count))) == IterationDecision::Break)
                break;
        }
    });

    IMMEDIATE_FUNCTION(length, {
        for (auto& argument : arguments) {
            size_t length = 0;
            auto list = argument.run(shell)->resolve_as_list(shell);
            bool first = true;
            for (auto& entry : list) {
                if (!first)
                    ++length; // space.
                first = false;
                length += entry.length();
            }
            if (callback(AST::create<AST::StringLiteral>(argument.position(), String::number(length))) == IterationDecision::Break)
                break;
        }
    });

    IMMEDIATE_FUNCTION(nth, {
        if (arguments.size() < 2)
            return;

        auto index_string = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        auto index = index_string.to_uint().value_or(0);
        for (auto& argument : arguments) {
            size_t current_index = 0;
            argument.for_each_entry(shell, [&](auto value) {
                if (current_index++ == index) {
                    callback(AST::create<AST::SyntheticNode>(argument.position(), move(value)));
                    return IterationDecision::Break;
                }
                return IterationDecision::Continue;
            });
        }
    });

    IMMEDIATE_FUNCTION(substring, {
        if (arguments.size() < 3)
            return;

        auto start_index_string = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        auto start_index = start_index_string.to_uint().value_or(0);
        auto length_string = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        auto length = length_string.to_uint().value_or(0);
        for (auto& argument : arguments) {
            auto list = argument.run(shell)->resolve_as_list(shell);
            if (list.is_empty())
                continue;
            if (list.size() == 1) {
                auto corrected_length = (length + start_index <= list.first().length()) ? length : list.first().length() - start_index;
                if (start_index < list.first().length())
                    callback(AST::create<AST::StringLiteral>(argument.position(), list.first().substring_view(start_index, corrected_length)));
                continue;
            }
            StringBuilder data;
            data.join(' ', list);

            auto str = data.string_view();
            auto corrected_length = (length + start_index <= str.length()) ? length : str.length() - start_index;
            if (start_index < str.length())
                callback(AST::create<AST::StringLiteral>(argument.position(), str.substring_view(start_index, corrected_length)));
        }
    });

    IMMEDIATE_FUNCTION(slice, {
        if (arguments.size() < 3)
            return;

        auto start_index_string = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        auto start_index = start_index_string.to_uint().value_or(0);
        auto length_string = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        auto length = length_string.to_uint().value_or(0);
        auto end_index = start_index + length;
        for (auto& argument : arguments) {
            size_t current_index = 0;
            argument.for_each_entry(shell, [&](auto value) {
                if (current_index >= start_index && current_index < end_index)
                    callback(AST::create<AST::SyntheticNode>(argument.position(), move(value)));
                if (current_index >= end_index)
                    return IterationDecision::Break;
                ++current_index;
                return IterationDecision::Continue;
            });
        }
    });

    IMMEDIATE_FUNCTION(remove_prefix, {
        if (arguments.size() < 2)
            return;

        auto prefix = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        for (auto& argument : arguments) {
            argument.for_each_entry(shell, [&](auto value) {
                auto list = value->resolve_as_list(shell);
                StringBuilder data;
                data.join(' ', list);
                auto view = data.string_view();
                if (view.starts_with(prefix))
                    view = view.substring_view(prefix.length());
                callback(AST::create<AST::StringLiteral>(argument.position(), view));
                return IterationDecision::Continue;
            });
        }
    });

    IMMEDIATE_FUNCTION(remove_suffix, {
        if (arguments.size() < 2)
            return;

        auto prefix = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        for (auto& argument : arguments) {
            argument.for_each_entry(shell, [&](auto value) {
                auto list = value->resolve_as_list(shell);
                StringBuilder data;
                data.join(' ', list);
                auto view = data.string_view();
                if (view.ends_with(prefix))
                    view = view.substring_view(0, view.length() - prefix.length());
                callback(AST::create<AST::StringLiteral>(argument.position(), view));
                return IterationDecision::Continue;
            });
        }
    });

    IMMEDIATE_FUNCTION(regex_replace, {
        if (arguments.size() < 2)
            return;

        auto pattern_node = arguments.take_first();
        auto pattern = pattern_node->run(shell)->resolve_as_list(shell).first();
        auto re = Regex<ECMA262>(pattern);
        if (re.parser_result.error != regex::Error::NoError) {
            callback(AST::create<AST::SyntaxError>(pattern_node->position(), re.error_string()));
            return;
        }
        auto replacement = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        for (auto& argument : arguments) {
            argument.for_each_entry(shell, [&](auto value) {
                auto list = value->resolve_as_list(shell);
                StringBuilder data;
                data.join(' ', list);
                auto string = re.replace(data.string_view(), replacement, ECMAScriptFlags::Multiline);
                callback(AST::create<AST::StringLiteral>(argument.position(), move(string)));
                return IterationDecision::Continue;
            });
        }
    });

    IMMEDIATE_FUNCTION(filter_glob, {
        if (arguments.size() < 2)
            return;

        auto glob = arguments.take_first()->run(shell)->resolve_as_list(shell).first();
        for (auto& argument : arguments) {
            argument.for_each_entry(shell, [&](auto value) {
                auto list = value->resolve_as_list(shell);
                StringBuilder data;
                data.join(' ', list);
                auto view = data.string_view();
                if (view.matches(glob))
                    callback(AST::create<AST::StringLiteral>(argument.position(), view));
                return IterationDecision::Continue;
            });
        }
    });
}

}
