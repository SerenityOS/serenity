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
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <cstdio>

namespace AK {
namespace regex {

#ifdef REGEX_DEBUG
static RegexDebug s_regex_dbg(stderr);
#endif

template<class Parser>
Regex<Parser>::Regex(StringView pattern, typename ParserTraits<Parser>::OptionsType regex_options)
{
    pattern_value = pattern.to_string();
    AK::regex::Lexer lexer(pattern);

    Parser parser(lexer);
    parser_result = parser.parse();

    if (parser_result.error == AK::regex::Error::NoError) {
        matcher = make<Matcher<Parser>>(*this, regex_options);
    } else {
        fprintf(stderr, "%s\n", error_string().characters());
    }
}

template<class Parser>
String Regex<Parser>::error_string() const
{
    StringBuilder eb;
    eb.appendf("Error during parsing of regular expression:\n");
    eb.appendf("    %s\n    ", pattern_value.characters());
    for (size_t i = 0; i < parser_result.error_token.position(); ++i)
        eb.append(" ");
    eb.appendf("^---- %s\n", get_error_string(parser_result.error).characters());
    return eb.build();
}

template<typename Parser>
RegexResult Matcher<Parser>::match(const StringView& view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options)
{
    size_t match_count { 0 };

    MatchInput input;
    MatchState state;
    MatchOutput output;

    input.regex_options = m_regex_options | regex_options.value_or({}).value();

    Vector<StringView>
        views { view };

    if (input.regex_options & AllFlags::Multiline)
        views = view.lines();

    output.matches.ensure_capacity(c_match_preallocation_count);
    output.capture_group_matches.ensure_capacity(c_match_preallocation_count);
    output.named_capture_group_matches.ensure_capacity(c_match_preallocation_count);

    auto& capture_groups_count = m_pattern.parser_result.capture_groups_count;
    auto& named_capture_groups_count = m_pattern.parser_result.named_capture_groups_count;

    for (size_t j = 0; j < c_match_preallocation_count; ++j) {
        output.matches.empend();
        output.capture_group_matches.empend();
        output.capture_group_matches.at(j).ensure_capacity(capture_groups_count);
        for (size_t k = 0; k < capture_groups_count; ++k)
            output.capture_group_matches.at(j).empend();

        output.named_capture_group_matches.empend();
        output.named_capture_group_matches.at(j).ensure_capacity(named_capture_groups_count);
    }

    auto append_match = [](auto& input, auto& state, auto& output, auto& start_position) {
        if (output.matches.size() == input.match_index)
            output.matches.empend();

        if (input.regex_options & AllFlags::StringCopyMatches) {
            output.matches.at(input.match_index) = { input.view.substring_view(start_position, state.string_position - start_position).to_string(), input.line, start_position };
        } else { // let the view point to the original string ...
            output.matches.at(input.match_index) = { input.view.substring_view(start_position, state.string_position - start_position), input.line, start_position };
        }
    };

#ifdef REGEX_DEBUG
    s_regex_dbg.print_header();
#endif

    for (auto& view : views) {
        input.view = view;
#ifdef REGEX_DEBUG
        dbg() << "[match] Starting match with view (" << view.length() << "): _" << view << "_";
#endif

        auto view_length = view.length();
        for (size_t view_index = 0; view_index < view_length; ++view_index) {
            auto& match_length_minimum = m_pattern.parser_result.match_length_minimum;
            // FIXME: More performantly would be to know the remaining minimum string
            //        lenght needed to match from the current position onwards within
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

            if (execute(input, state, output, 0)) {
#ifdef REGEX_DEBUG
                dbg() << "state.string_position: " << state.string_position << " view_index: " << view_index;
                dbg() << "[match] Found a match (length = " << state.string_position - view_index << "): " << input.view.substring_view(view_index, state.string_position - view_index);
#endif
                ++match_count;

                if (input.regex_options & AllFlags::Global) {
                    append_match(input, state, output, view_index);

                    bool has_zero_length = state.string_position == view_index;
                    view_index = state.string_position - (has_zero_length ? 0 : 1);
                    continue;

                } else if (!(input.regex_options & AllFlags::Global) && state.string_position < view_length)
                    return { false, 0, {}, {}, {}, output.operations };

                append_match(input, state, output, view_index);
                break;
            }

            if (!(input.regex_options & AllFlags::Global))
                break;
        }

        ++input.line;
    }

    if (match_count < c_match_preallocation_count) {
        output.matches.shrink(match_count);
        output.capture_group_matches.shrink(match_count);
        output.named_capture_group_matches.shrink(match_count);
    }

    return {
        match_count ? true : false,
        match_count,
        move(output.matches),
        move(output.capture_group_matches),
        move(output.named_capture_group_matches),
        output.operations,
    };
}

template<class Parser>
bool Matcher<Parser>::execute(const MatchInput& input, MatchState& state, MatchOutput& output, size_t recursion_level)
{
    if (recursion_level > c_max_recursion)
        return false;

    Vector<MatchState> fork_low_prio_states;
    MatchState fork_high_prio_state;

    auto& bytecode = m_pattern.parser_result.bytecode;

    for (;;) {
        ++output.operations;
        auto* opcode = bytecode.get_opcode(state);

        if (!opcode) {
            //FIXME: How to bring this bad situation to a good end?
            return false;
            //fprintf(stderr, "\n[VM] Invalid opcode: %lu, stack index: %lu\n", at(state.instruction_position), state.instruction_position);
            //exit(1);
        }

#ifdef REGEX_DEBUG
        s_regex_dbg.print_opcode("VM", *opcode, state, recursion_level, false);
#endif

        auto result = opcode->execute(input, state, output);

#ifdef REGEX_DEBUG
        s_regex_dbg.print_result(*opcode, bytecode, input, state, result);
#endif

        state.instruction_position += opcode->size();

        switch (result) {
        case ExecutionResult::Fork_PrioLow:
            fork_low_prio_states.prepend(MatchState { state });
            continue;
        case ExecutionResult::Fork_PrioHigh:
            fork_high_prio_state = state;
            fork_high_prio_state.instruction_position = fork_high_prio_state.fork_at_position;
            if (execute(input, fork_high_prio_state, output, ++recursion_level)) {
                state = fork_high_prio_state;
                return true;
            }

            continue;
        case ExecutionResult::Done:
            if (state.string_position > input.view.length() - 1 || state.instruction_position >= m_pattern.parser_result.bytecode.size())
                return true;
            return false;

        case ExecutionResult::Continue:
            continue;
        case ExecutionResult::Exit:
            return false;
        case ExecutionResult::ExitWithFork:
            return execute_low_prio_forks(input, state, output, fork_low_prio_states, recursion_level + 1);
        }
    }

    ASSERT_NOT_REACHED();
}

template<class Parser>
inline bool Matcher<Parser>::execute_low_prio_forks(const MatchInput& input, MatchState& original_state, MatchOutput& output, Vector<MatchState> states, size_t recursion_level)
{
    for (auto& state : states) {

        state.instruction_position = state.fork_at_position;
#ifdef REGEX_DEBUG
        fprintf(stderr, "Forkstay... ip = %lu, sp = %lu\n", state.instruction_position, state.string_position);
#endif

        if (execute(input, state, output, recursion_level)) {
#ifdef REGEX_DEBUG
            fprintf(stderr, "Forkstay succeeded... ip = %lu, sp = %lu\n", state.instruction_position, state.string_position);
#endif
            original_state = MatchState { state };
            return true;
        }
    }

    original_state.string_position = 0;
    return false;
};

template class Matcher<PosixExtendedParser>;
template class Regex<PosixExtendedParser>;
}
}
