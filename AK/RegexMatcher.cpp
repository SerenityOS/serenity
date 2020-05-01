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
#include "RegexParser.h"
#include <AK/String.h>
#include <AK/StringBuilder.h>
#include <cstdio>
#include <ctype.h>

namespace AK {
namespace regex {

//#define REGEX_DEBUG

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

template<class Parser>
void Regex<Parser>::print_bytecode() const
{
    size_t ip = 0;

    auto pop = [&](size_t value = 1) -> ByteCodeValue {
        if (ip >= parser_result.bytecode.size())
            return ByteCodeValue(OpCode::Exit);

        auto& current = parser_result.bytecode.at(ip);
        ip += value;
        return current;
    };

    for (;;) {
        auto stack_item = pop();
        printf("OpCode[%3lu] 0x%02X: %s ", ip - 1, (u32)stack_item.op_code, stack_item.name());

        switch (stack_item.op_code) {
        case OpCode::Compare: {
            auto& arguments = pop().positive_number;
            size_t fetched_arguments = 0;
            printf("- Number of arguments: %lu, ", arguments);
            for (; fetched_arguments < arguments; ++fetched_arguments) {
                auto& compare_type = pop().compare_type;

                printf("Compare type: 0x%02X = ", (u32)compare_type);
                if (compare_type == CharacterCompareType::Inverse)
                    printf("inverse\n");
                else if (compare_type == CharacterCompareType::AnySingleCharacter)
                    printf("matchall\n");
                else if (compare_type == CharacterCompareType::OrdinaryCharacter) {
                    auto ch = pop().ch;
                    printf("single character '%c'\n", ch);
                } else if (compare_type == CharacterCompareType::OrdinaryCharacters) {
                    auto* str = pop().string;
                    auto& length = pop().positive_number;
                    printf("multiple characters \"%s\"\n", String { str, length }.characters());
                } else if (compare_type == CharacterCompareType::CharacterClass) {
                    auto& character_class = pop().compare_value.character_class;
                    String ch_class;
                    switch (character_class) {
                    case CharacterClass::Alnum:
                        ch_class = "Alnum";
                        break;
                    case CharacterClass::Alpha:
                        ch_class = "Alpha";
                        break;
                    case CharacterClass::Blank:
                        ch_class = "Blank";
                        break;
                    case CharacterClass::Cntrl:
                        ch_class = "Cntrl";
                        break;
                    case CharacterClass::Digit:
                        ch_class = "Digit";
                        break;
                    case CharacterClass::Graph:
                        ch_class = "Graph";
                        break;
                    case CharacterClass::Lower:
                        ch_class = "Lower";
                        break;
                    case CharacterClass::Print:
                        ch_class = "Print";
                        break;
                    case CharacterClass::Punct:
                        ch_class = "Punct";
                        break;
                    case CharacterClass::Space:
                        ch_class = "Space";
                        break;
                    case CharacterClass::Upper:
                        ch_class = "Upper";
                        break;
                    case CharacterClass::Xdigit:
                        ch_class = "xdigit";
                        break;
                    default:
                        StringBuilder b;
                        b.appendf("unknown (%lu)", (size_t)character_class);
                        ch_class = b.to_string();
                    }
                    printf("character class '%s'\n", ch_class.characters());
                } else if (compare_type == CharacterCompareType::RangeExpression) {
                    auto& value = pop().compare_value;
                    printf("range expression '%c'-'%c'\n", value.range_values.from, value.range_values.to);
                } else {
                    printf("undefined!\n");
                }
            }
        } break;

        case OpCode::ForkJump:
        case OpCode::ForkStay:
        case OpCode::Jump: {
            auto v = pop();
            printf("- To: %i\n", (int)ip + v.number);
        } break;

        case OpCode::SaveLeftNamedCaptureGroup:
        case OpCode::SaveRightNamedCaptureGroup: {
            printf("\n");
            pop();
            pop();
        } break;
        case OpCode::SaveLeftCaptureGroup:
        case OpCode::SaveRightCaptureGroup: {
            printf("\n");
            pop();
        } break;

            // no extra arguments
        case OpCode::CheckBegin:
        case OpCode::CheckEnd:
            printf("\n");
            break;

        case OpCode::Exit: {
            printf("\n");
            fflush(stdout);
            return;
        } break;

        default:
            printf("- Invalid opcode: %lu, stackpointer: %lu\n", (size_t)stack_item.op_code, ip);
        }
    }

    fflush(stdout);
}

template<typename Parser>
RegexResult Matcher<Parser>::match(const StringView& view, Optional<typename ParserTraits<Parser>::OptionsType> regex_options)
{
    size_t match_count { 0 };
    MatchState state { m_pattern.parser_result.bytecode };
    state.regex_options = m_regex_options | regex_options.value_or({}).value();

    Vector<StringView> views { view };

    if (state.regex_options & AllFlags::Multiline)
        views = view.lines();

    state.matches.ensure_capacity(c_match_preallocation_count);
    state.capture_group_matches.ensure_capacity(c_match_preallocation_count);
    state.named_capture_group_matches.ensure_capacity(c_match_preallocation_count);

    auto& capture_groups_count = m_pattern.parser_result.capture_groups_count;
    auto& named_capture_groups_count = m_pattern.parser_result.named_capture_groups_count;

    for (size_t j = 0; j < c_match_preallocation_count; ++j) {
        state.matches.empend();
        state.capture_group_matches.empend();
        state.capture_group_matches.at(j).ensure_capacity(capture_groups_count);
        for (size_t k = 0; k < capture_groups_count; ++k)
            state.capture_group_matches.at(j).empend();

        state.named_capture_group_matches.empend();
        state.named_capture_group_matches.at(j).ensure_capacity(named_capture_groups_count);
    }

    auto append_match = [](auto& state, auto& view, auto& start_position) {
        if (state.matches.size() == state.match_index)
            state.matches.empend();

        if (state.regex_options & AllFlags::StringCopyMatches) {
            state.matches.at(state.match_index) = { view.substring_view(start_position, state.sp - start_position).to_string(), state.line, state.column };
        } else { // let the view point to the original string...
            state.matches.at(state.match_index) = { view.substring_view(start_position, state.sp - start_position), state.line, state.column };
        }
    };

    for (auto& view : views) {
        state.view = view;

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

            state.reset(view_index, match_count);

            if (execute(state)) {
                ++match_count;

                if (state.regex_options & AllFlags::Global) {
                    append_match(state, view, view_index);
                    bool has_zero_length = state.sp == view_index;
                    view_index = state.sp - (has_zero_length ? 0 : 1);
                    continue;

                } else if (!(state.regex_options & AllFlags::Global) && state.sp < view_length)
                    return { false, 0, {}, {}, {}, state.operations };

                append_match(state, view, view_index);
                break;
            }

            if (!(state.regex_options & AllFlags::Global))
                break;
        }

        ++state.line;
    }

    if (match_count < c_match_preallocation_count) {
        state.matches.shrink(match_count);
        state.capture_group_matches.shrink(match_count);
        state.named_capture_group_matches.shrink(match_count);
    }

    return {
        match_count ? true : false,
        match_count,
        move(state.matches),
        move(state.capture_group_matches),
        move(state.named_capture_group_matches),
        state.operations,
    };
}

template<class T>
bool Matcher<T>::execute(MatchState& state, size_t recursion_level) const
{
    if (recursion_level > c_max_recursion)
        return false;

    Vector<IpSpTuple> ip_sp_tuples;

    // FIXME: Refactor as inline methods?
    auto append_capture_group_match = [](auto& id, auto& state, auto& start_position, auto& length) {
        if (state.regex_options & AllFlags::StringCopyMatches) {
            state.capture_group_matches.at(state.match_index).at(id) = { state.view.substring_view(start_position, length).to_string(), state.line, start_position };
        } else { // let the view point to the original string ...
            state.capture_group_matches.at(state.match_index).at(id) = { state.view.substring_view(start_position, length), state.line, start_position };
        }
    };

    // FIXME: Refactor as inline methods?
    auto append_named_capture_group_match = [](auto& name, auto& state, auto& start_position, auto& length) {
        if (state.regex_options & AllFlags::StringCopyMatches) {
            state.named_capture_group_matches.at(state.match_index).set(name, { state.view.substring_view(start_position, length).to_string(), state.line, start_position });
        } else { // let the view point to the original string ...
            state.named_capture_group_matches.at(state.match_index).set(name, { { state.view.substring_view(start_position, length) }, state.line, start_position });
        }
    };

    // FIXME: Refactor as ...?
    auto run_forkstay = [&]() -> bool {
        auto tuples_size = ip_sp_tuples.size();
        for (size_t i = 0; i < tuples_size; ++i) {
            auto& ip_sp_tuple = ip_sp_tuples.at(tuples_size - i - 1);
            auto ip = state.ip;
            auto sp = state.sp;

            state.ip = ip_sp_tuple.ip;
            state.sp = ip_sp_tuple.sp;

#ifdef REGEX_DEBUG
            printf("[VM][r=%lu] Execute ForkStay - instructionp: %2lu, stringp: %2lu - ", recursion_level, state.ip, state.sp);
            printf("[%20s]\n", String(&state.view[state.sp], state.view.length() - state.sp).characters());
#endif

            if (execute(state, recursion_level + 1))
                return true;

            state.ip = ip;
            state.sp = sp;
        }
        return false;
    };

    // FIXME: Refactor as ...?
    auto run_forkstay_or_false = [&]() -> bool {
        if (run_forkstay())
            return true;

        state.sp = 0;
        return false;
    };

    // FIXME: Refactor as ...? Reduce debugging
    auto check_exit_conditions = [&]() -> bool {
#ifdef REGEX_DEBUG
        bool verbose = true;
        if (verbose) {
            printf("[VM][r=%lu] Checking exit conditions: ", recursion_level);
            printf("String: stringp: %lu, length: %lu; ", state.sp, state.view.length());
            printf("Instruction: instructionp: %lu, size: %lu; ", state.ip, m_pattern.parser_result.bytecode.size());
            printf("Condition: %s\n", state.sp > state.view.length() || state.ip >= m_pattern.parser_result.bytecode.size() ? "true" : "false");
        } else {
            if (state.ip >= m_pattern.parser_result.bytecode.size())
                printf("[VM][r=%lu] Reached end of OpCodes with stringp = %lu!\n", recursion_level, state.sp);

            if (state.sp > state.view.length())
                printf("[VM][r=%lu] Reached end of string with instructionp = %lu!\n", recursion_level, state.ip);
        }
#endif

        if (state.sp > state.view.length() || state.ip >= m_pattern.parser_result.bytecode.size())
            return true;

        return false;
    };

    // Refactor state functions, only state and ip as arguments?
    for (;;) {
        ++state.operations;
        auto current_ip = state.ip;
        auto stack_item = state.pop();

#ifdef REGEX_DEBUG
        printf("[VM][r=%lu]  OpCode: 0x%i (%14s) - instructionp: %2lu, stringp: %2lu - ", recursion_level, stack_item.number, stack_item.name(), current_ip, state.sp);
        printf("[%20s]\n", String(&state.view[state.sp], state.view.length() - state.sp).characters());
#endif

        if (stack_item.op_code == OpCode::Compare) {
            bool inverse { false };
            auto& arguments = state.pop().positive_number;
            size_t fetched_arguments = 0;
            size_t sp = state.sp;
            bool inverse_matched { false };

            for (; fetched_arguments < arguments; ++fetched_arguments) {
                if (state.sp > sp)
                    break;

                auto& compare_type = state.pop().compare_type;

                // FIXME: refactor the comparisons out... e.g. as callables
                if (compare_type == CharacterCompareType::Inverse)
                    inverse = true;

                else if (compare_type == CharacterCompareType::OrdinaryCharacter) {
                    auto ch = state.pop().ch;

                    // We want to compare a string that is definitely longer than the available string
                    if (state.view.length() - state.sp < 1)
                        return run_forkstay_or_false();

                    auto ch1 = ch;
                    auto ch2 = state.view[state.sp];

                    if (state.regex_options & AllFlags::Insensitive) {
                        ch1 = tolower(ch1);
                        ch2 = tolower(ch2);
                    }

                    if (ch1 == ch2) {
                        if (inverse)
                            inverse_matched = true;
                        else
                            ++state.sp;
                    }

                } else if (compare_type == CharacterCompareType::AnySingleCharacter) {
                    // We want to compare a string that is definitely longer than the available string
                    if (state.view.length() - state.sp < 1)
                        return run_forkstay_or_false();

                    ASSERT(!inverse);
                    ++state.sp;

                } else if (compare_type == CharacterCompareType::OrdinaryCharacters) {
                    // We want to compare a string that is definitely longer than the available string
                    ASSERT(!inverse);

                    auto* str = state.pop().string;
                    auto& length = state.pop().positive_number;

                    // We want to compare a string that is definitely longer than the available string
                    if (state.view.length() - state.sp < length)
                        return run_forkstay_or_false();

                    auto str_view1 = StringView(str, length);
                    auto str_view2 = StringView(&state.view[state.sp], length);
                    String str1, str2;
                    if (state.regex_options & AllFlags::Insensitive) {
                        str1 = str_view1.to_string().to_lowercase();
                        str2 = str_view2.to_string().to_lowercase();
                        str_view1 = str1.view();
                        str_view2 = str2.view();
                    }

                    if (!strncmp(str_view1.characters_without_null_termination(),
                            str_view2.characters_without_null_termination(), length))
                        state.sp += length;
                    else
                        return run_forkstay_or_false();
                } else if (compare_type == CharacterCompareType::CharacterClass) {

                    if (state.view.length() - state.sp < 1)
                        return run_forkstay_or_false();

                    auto& character_class = state.pop().compare_value.character_class;
                    auto& ch = state.view[state.sp];

                    switch (character_class) {
                    case CharacterClass::Alnum:
                        if (isalnum(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Alpha:
                        if (isalpha(ch))
                            ++state.sp;
                        break;
                    case CharacterClass::Blank:
                        if (ch == ' ' || ch == '\t') {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Cntrl:
                        if (iscntrl(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Digit:
                        if (isdigit(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Graph:
                        if (isgraph(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Lower:
                        if (islower(ch) || ((state.regex_options & AllFlags::Insensitive) && isupper(ch))) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Print:
                        if (isprint(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Punct:
                        if (ispunct(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Space:
                        if (isspace(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Upper:
                        if (isupper(ch) || ((state.regex_options & AllFlags::Insensitive) && islower(ch))) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    case CharacterClass::Xdigit:
                        if (isxdigit(ch)) {
                            if (inverse)
                                inverse_matched = true;
                            else
                                ++state.sp;
                        }
                        break;
                    }

                } else if (compare_type == CharacterCompareType::RangeExpression) {
                    auto& value = state.pop().compare_value;
                    auto from = value.range_values.from;
                    auto to = value.range_values.to;
                    auto ch = state.view[state.sp];

                    if (state.regex_options & AllFlags::Insensitive) {
                        from = tolower(from);
                        to = tolower(to);
                        ch = tolower(ch);
                    }

                    if (ch >= from && ch <= to) {
                        if (inverse)
                            inverse_matched = true;
                        else
                            ++state.sp;
                    }

                } else {
                    fprintf(stderr, "Undefined comparison: %i\n", (int)compare_type);
                    ASSERT_NOT_REACHED();
                    break;
                }
            }

            if (inverse && !inverse_matched)
                ++state.sp;

            // FIXME: Improve by providing length of arguments in OpCode, reset ip = ip + length_of_arguments - fetched_arguments;

            // fetch missing arguments from stack
            for (; fetched_arguments < arguments; ++fetched_arguments) {
                auto& compare_type = state.pop().compare_type;
                switch (compare_type) {
                case CharacterCompareType::OrdinaryCharacter:
                    state.pop();
                    break;
                case CharacterCompareType::OrdinaryCharacters:
                    state.pop();
                    state.pop();
                    break;
                case CharacterCompareType::CharacterClass:
                    state.pop();
                    break;
                case CharacterCompareType::RangeExpression:
                    state.pop();
                    break;
                default: {
                }
                }
            }

            if (sp == state.sp)
                return run_forkstay_or_false();

            if (state.sp > state.view.length())
                return run_forkstay_or_false();

        } else if (stack_item.op_code == OpCode::ForkJump) {
            auto& offset = state.pop().number;
#ifdef REGEX_DEBUG
            printf(" > ForkJump to offset: %i, instructionp: %lu, stringp: %lu\n", offset, state.ip + offset, state.sp);
#endif
            //save instructionp and stringp
            auto ip = state.ip;
            auto sp = state.sp;
            state.ip = state.ip + offset;

            if (!execute(state, recursion_level + 1)) {
                // no valid solution via forkjump found... continue here at old string position and instruction
                state.ip = ip;
                state.sp = sp;
            }
        } else if (stack_item.op_code == OpCode::ForkStay) {
            auto& offset = state.pop().number;
            ip_sp_tuples.append({ state.ip + offset, state.sp });

#ifdef REGEX_DEBUG
            printf(" > ForkStay to offset: %i, instructionp: %lu, stringp: %lu\n", offset, ip_sp_tuples.last().ip, ip_sp_tuples.last().sp);
#endif
        } else if (stack_item.op_code == OpCode::Jump) {
            auto& offset = state.pop().number;
            state.ip += offset;
#ifdef REGEX_DEBUG
            printf(" > Jump to offset: %i: new instructionp: %lu\n", offset, state.ip);
#endif
            continue; // directly jump to next instruction!
        } else if (stack_item.op_code == OpCode::SaveLeftCaptureGroup) {
            auto& id = state.pop().positive_number;
#ifdef REGEX_DEBUG
            printf(" > Left parens for capture group %lu at stringp = %lu\n", id, state.sp);
#endif
            state.capture_group_matches.at(state.match_index).at(id).column = state.sp;

        } else if (stack_item.op_code == OpCode::SaveLeftNamedCaptureGroup) {
            StringView name { state.pop().string, state.pop().positive_number };

#ifdef REGEX_DEBUG
            printf(" > Left parens for named capture group '%s' at stringp = %lu\n", name.to_string().characters(), state.sp);
#endif
            state.named_capture_group_matches.at(state.match_index).ensure(name).column = state.sp;

        } else if (stack_item.op_code == OpCode::SaveRightCaptureGroup) {
            auto& id = state.pop().positive_number;

#ifdef REGEX_DEBUG
            printf(" > Right parens for capture group %lu at stringp = %lu\n", id, state.sp);
#endif
            //if (state.capture_group_matches.at(state.match_index).at(id).column) {
            auto start_position = state.capture_group_matches.at(state.match_index).at(id).column;
            auto length = state.sp - start_position;
            append_capture_group_match(id, state, start_position, length);

#ifdef REGEX_DEBUG
            printf("Match capture group id %lu: from %lu to %lu\n", id, start_position, state.sp);
#endif
            //}

        } else if (stack_item.op_code == OpCode::SaveRightNamedCaptureGroup) {
            StringView name { state.pop().string, state.pop().positive_number };

#ifdef REGEX_DEBUG
            printf(" > Right parens for named capture group '%s' at stringp = %lu\n", name.to_string().characters(), state.sp);
#endif
            if (state.named_capture_group_matches.at(state.match_index).contains(name)) {
                auto start_position = state.named_capture_group_matches.at(state.match_index).ensure(name).column;
                auto length = state.sp - start_position;
                append_named_capture_group_match(name, state, start_position, length);

#ifdef REGEX_DEBUG
                fprintf(stderr, "Match named capture group '%s' from %lu to %lu\n", name.to_string().characters(), start_position, state.sp);
#endif
            }

        } else if (stack_item.op_code == OpCode::CheckBegin) {
#ifdef REGEX_DEBUG
            printf("\n");
#endif

            if (state.sp != 0 || ((state.regex_options & AllFlags::Global) && !((state.regex_options & AllFlags::Anchored) || (state.regex_options & AllFlags::Global))))
                return false;
        } else if (stack_item.op_code == OpCode::CheckEnd) {
#ifdef REGEX_DEBUG
            printf(" > Check end: %lu == %lu\n", state.sp, state.view.length());
#endif
            if (state.sp != state.view.length()) // || (!state.m_match_flags & AllFlags::DollarEndOnly) && !((state.m_match_flags & AllFlags::Anchored) || (state.m_match_flags & AllFlags::Global))))
                return false;
        } else if (stack_item.op_code == OpCode::Exit) {
            bool cond = check_exit_conditions();
#ifdef REGEX_DEBUG
            printf(" > Condition %s\n", cond ? "true" : "false");
#endif
            if (cond)
                return true;
            return false;
        } else {
            printf("\n[VM][r=%lu] Invalid opcode: %lu, stackpointer: %lu\n", recursion_level, (size_t)stack_item.op_code, current_ip);
            exit(1);
        }

        if (check_exit_conditions())
            return true;
    }

    ASSERT_NOT_REACHED();
}

template class Matcher<PosixExtendedParser>;
template class Regex<PosixExtendedParser>;
}
}
