/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
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

#include "RegexMatcher.h"
#include "RegexDebug.h"
#include "RegexParser.h"
#include <AK/ScopedValueRollback.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace regex {

#ifdef REGEX_DEBUG
static RegexDebug s_regex_dbg(stderr);
#endif

template<class Parser>
Regex<Parser>::Regex(StringView pattern, typename ParserTraits<Parser>::OptionsType regex_options)
{
    pattern_value = pattern.to_string();
    regex::Lexer lexer(pattern);

    Parser parser(lexer, regex_options);
    parser_result = parser.parse();

    if (parser_result.error == regex::Error::NoError)
        matcher = make<Matcher<Parser>>(*this, regex_options);
}

template<class Parser>
typename ParserTraits<Parser>::OptionsType Regex<Parser>::options() const
{
    if (parser_result.error != Error::NoError)
        return {};

    return matcher->options();
}

template<class Parser>
String Regex<Parser>::error_string(Optional<String> message) const
{
    StringBuilder eb;
    eb.appendf("Error during parsing of regular expression:\n");
    eb.appendf("    %s\n    ", pattern_value.characters());
    for (size_t i = 0; i < parser_result.error_token.position(); ++i)
        eb.append(" ");

    eb.appendf("^---- %s", message.value_or(get_error_string(parser_result.error)).characters());
    return eb.build();
}

template<typename Parser>
RegexResult Matcher<Parser>::match(const RegexStringView& view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options) const
{
    AllOptions options = m_regex_options | regex_options.value_or({}).value();

    if (options.has_flag_set(AllFlags::Multiline))
        return match(view.lines(), regex_options); // FIXME: how do we know, which line ending a line has (1char or 2char)? This is needed to get the correct match offsets from start of string...

    Vector<RegexStringView> views;
    views.append(view);
    return match(views, regex_options);
}

template<typename Parser>
RegexResult Matcher<Parser>::match(const Vector<RegexStringView> views, Optional<typename ParserTraits<Parser>::OptionsType> regex_options) const
{
    // If the pattern *itself* isn't stateful, reset any changes to start_offset.
    if (!((AllFlags)m_regex_options.value() & AllFlags::Internal_Stateful))
        m_pattern.start_offset = 0;

    size_t match_count { 0 };

    MatchInput input;
    MatchState state;
    MatchOutput output;

    input.regex_options = m_regex_options | regex_options.value_or({}).value();
    input.start_offset = m_pattern.start_offset;
    output.operations = 0;

    if (input.regex_options.has_flag_set(AllFlags::Internal_Stateful))
        ASSERT(views.size() == 1);

    if (c_match_preallocation_count) {
        output.matches.ensure_capacity(c_match_preallocation_count);
        output.capture_group_matches.ensure_capacity(c_match_preallocation_count);
        output.named_capture_group_matches.ensure_capacity(c_match_preallocation_count);

        auto& capture_groups_count = m_pattern.parser_result.capture_groups_count;
        auto& named_capture_groups_count = m_pattern.parser_result.named_capture_groups_count;

        for (size_t j = 0; j < c_match_preallocation_count; ++j) {
            output.matches.empend();
            output.capture_group_matches.unchecked_append({});
            output.capture_group_matches.at(j).ensure_capacity(capture_groups_count);
            for (size_t k = 0; k < capture_groups_count; ++k)
                output.capture_group_matches.at(j).unchecked_append({});

            output.named_capture_group_matches.unchecked_append({});
            output.named_capture_group_matches.at(j).ensure_capacity(named_capture_groups_count);
        }
    }

    auto append_match = [](auto& input, auto& state, auto& output, auto& start_position) {
        if (output.matches.size() == input.match_index)
            output.matches.empend();

        ASSERT(start_position + state.string_position - start_position <= input.view.length());
        if (input.regex_options.has_flag_set(AllFlags::StringCopyMatches)) {
            output.matches.at(input.match_index) = { input.view.substring_view(start_position, state.string_position - start_position).to_string(), input.line, start_position, input.global_offset + start_position };
        } else { // let the view point to the original string ...
            output.matches.at(input.match_index) = { input.view.substring_view(start_position, state.string_position - start_position), input.line, start_position, input.global_offset + start_position };
        }
    };

#ifdef REGEX_DEBUG
    s_regex_dbg.print_header();
#endif

    bool continue_search = input.regex_options.has_flag_set(AllFlags::Global) || input.regex_options.has_flag_set(AllFlags::Multiline);
    if (input.regex_options.has_flag_set(AllFlags::Internal_Stateful))
        continue_search = false;

    for (auto& view : views) {
        input.view = view;
#ifdef REGEX_DEBUG
        dbg() << "[match] Starting match with view (" << view.length() << "): _" << view.to_string() << "_";
#endif

        auto view_length = view.length();
        size_t view_index = m_pattern.start_offset;
        state.string_position = view_index;

        if (view_index == view_length && m_pattern.parser_result.match_length_minimum == 0) {
            // Run the code until it tries to consume something.
            // This allows non-consuming code to run on empty strings, for instance
            // e.g. "Exit"
            MatchOutput temp_output { output };

            input.column = match_count;
            input.match_index = match_count;

            state.string_position = view_index;
            state.instruction_position = 0;

            auto success = execute(input, state, temp_output, 0);
            // This success is acceptable only if it doesn't read anything from the input (input length is 0).
            if (state.string_position <= view_index) {
                if (success.value()) {
                    output = move(temp_output);
                    if (!match_count) {
                        // Nothing was *actually* matched, so append an empty match.
                        append_match(input, state, output, view_index);
                        ++match_count;
                    }
                }
            }
        }

        for (; view_index < view_length; ++view_index) {
            auto& match_length_minimum = m_pattern.parser_result.match_length_minimum;
            // FIXME: More performant would be to know the remaining minimum string
            //        length needed to match from the current position onwards within
            //        the vm. Add new OpCode for MinMatchLengthFromSp with the value of
            //        the remaining string length from the current path. The value though
            //        has to be filled in reverse. That implies a second run over bytecode
            //        after generation has finished.
            if (match_length_minimum && match_length_minimum > view_length - view_index)
                break;

            input.column = match_count;
            input.match_index = match_count;

            state.string_position = view_index;
            state.instruction_position = 0;

            auto success = execute(input, state, output, 0);
            if (!success.has_value())
                return { false, 0, {}, {}, {}, output.operations };

            if (success.value()) {

                if (input.regex_options.has_flag_set(AllFlags::MatchNotEndOfLine) && state.string_position == input.view.length()) {
                    if (!continue_search)
                        break;
                    continue;
                }
                if (input.regex_options.has_flag_set(AllFlags::MatchNotBeginOfLine) && view_index == 0) {
                    if (!continue_search)
                        break;
                    continue;
                }

#ifdef REGEX_DEBUG
                dbg() << "state.string_position: " << state.string_position << " view_index: " << view_index;
                dbg() << "[match] Found a match (length = " << state.string_position - view_index << "): " << input.view.substring_view(view_index, state.string_position - view_index).to_string();
#endif
                ++match_count;

                if (continue_search) {
                    append_match(input, state, output, view_index);

                    bool has_zero_length = state.string_position == view_index;
                    view_index = state.string_position - (has_zero_length ? 0 : 1);
                    continue;

                } else if (input.regex_options.has_flag_set(AllFlags::Internal_Stateful)) {
                    append_match(input, state, output, view_index);
                    break;

                } else if (state.string_position < view_length) {
                    return { false, 0, {}, {}, {}, output.operations };
                }

                append_match(input, state, output, view_index);
                break;
            }

            if (!continue_search && !input.regex_options.has_flag_set(AllFlags::Internal_Stateful))
                break;
        }

        ++input.line;
        input.global_offset += view.length() + 1; // +1 includes the line break character

        if (input.regex_options.has_flag_set(AllFlags::Internal_Stateful))
            m_pattern.start_offset = state.string_position;
    }

    MatchOutput output_copy;
    if (match_count) {
        auto capture_groups_count = min(output.capture_group_matches.size(), output.matches.size());
        for (size_t i = 0; i < capture_groups_count; ++i) {
            if (input.regex_options.has_flag_set(AllFlags::SkipTrimEmptyMatches)) {
                output_copy.capture_group_matches.append(output.capture_group_matches.at(i));
            } else {
                Vector<Match> capture_group_matches;
                for (size_t j = 0; j < output.capture_group_matches.at(i).size(); ++j) {
                    if (!output.capture_group_matches.at(i).at(j).view.is_null())
                        capture_group_matches.append(output.capture_group_matches.at(i).at(j));
                }
                output_copy.capture_group_matches.append(capture_group_matches);
            }
        }

        auto named_capture_groups_count = min(output.named_capture_group_matches.size(), output.matches.size());
        for (size_t i = 0; i < named_capture_groups_count; ++i) {
            if (output.matches.at(i).view.length())
                output_copy.named_capture_group_matches.append(output.named_capture_group_matches.at(i));
        }

        for (size_t i = 0; i < match_count; ++i)
            output_copy.matches.append(output.matches.at(i));

    } else {
        output_copy.capture_group_matches.clear_with_capacity();
        output_copy.named_capture_group_matches.clear_with_capacity();
    }

    return {
        match_count ? true : false,
        match_count,
        move(output_copy.matches),
        move(output_copy.capture_group_matches),
        move(output_copy.named_capture_group_matches),
        output.operations,
        m_pattern.parser_result.capture_groups_count,
        m_pattern.parser_result.named_capture_groups_count,
    };
}

template<class Parser>
Optional<bool> Matcher<Parser>::execute(const MatchInput& input, MatchState& state, MatchOutput& output, size_t recursion_level) const
{
    if (recursion_level > c_max_recursion)
        return false;

    Vector<MatchState> fork_low_prio_states;
    MatchState fork_high_prio_state;
    Optional<bool> success;

    auto& bytecode = m_pattern.parser_result.bytecode;

    for (;;) {
        ++output.operations;
        auto* opcode = bytecode.get_opcode(state);

        if (!opcode) {
            dbgln("Wrong opcode... failed!");
            return {};
        }

#ifdef REGEX_DEBUG
        s_regex_dbg.print_opcode("VM", *opcode, state, recursion_level, false);
#endif

        ExecutionResult result;
        if (input.fail_counter > 0) {
            --input.fail_counter;
            result = ExecutionResult::Failed_ExecuteLowPrioForks;
        } else {
            result = opcode->execute(input, state, output);
        }

#ifdef REGEX_DEBUG
        s_regex_dbg.print_result(*opcode, bytecode, input, state, result);
#endif

        state.instruction_position += opcode->size();

        switch (result) {
        case ExecutionResult::Fork_PrioLow:
            fork_low_prio_states.prepend(state);
            continue;
        case ExecutionResult::Fork_PrioHigh:
            fork_high_prio_state = state;
            fork_high_prio_state.instruction_position = fork_high_prio_state.fork_at_position;
            success = execute(input, fork_high_prio_state, output, ++recursion_level);
            if (!success.has_value())
                return {};

            if (success.value()) {
                state = fork_high_prio_state;
                return true;
            }

            continue;
        case ExecutionResult::Continue:
            continue;
        case ExecutionResult::Succeeded:
            return true;
        case ExecutionResult::Failed:
            return false;
        case ExecutionResult::Failed_ExecuteLowPrioForks:
            return execute_low_prio_forks(input, state, output, fork_low_prio_states, recursion_level + 1);
        }
    }

    ASSERT_NOT_REACHED();
}

template<class Parser>
ALWAYS_INLINE Optional<bool> Matcher<Parser>::execute_low_prio_forks(const MatchInput& input, MatchState& original_state, MatchOutput& output, Vector<MatchState> states, size_t recursion_level) const
{
    for (auto& state : states) {

        state.instruction_position = state.fork_at_position;
#ifdef REGEX_DEBUG
        fprintf(stderr, "Forkstay... ip = %lu, sp = %lu\n", state.instruction_position, state.string_position);
#endif
        auto success = execute(input, state, output, recursion_level);
        if (!success.has_value())
            return {};
        if (success.value()) {
#ifdef REGEX_DEBUG
            fprintf(stderr, "Forkstay succeeded... ip = %lu, sp = %lu\n", state.instruction_position, state.string_position);
#endif
            original_state = state;
            return true;
        }
    }

    original_state.string_position = 0;
    return false;
}

template class Matcher<PosixExtendedParser>;
template class Regex<PosixExtendedParser>;

template class Matcher<ECMA262Parser>;
template class Regex<ECMA262Parser>;
}
