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

#include "RegexByteCode.h"
#include "AK/StringBuilder.h"
#include "RegexDebug.h"

#include <ctype.h>

namespace AK {
namespace regex {

const char* OpCode::name(OpCodeId opcode_id)
{
    switch (opcode_id) {
#define __ENUMERATE_OPCODE(x) \
    case OpCodeId::x:         \
        return #x;
        ENUMERATE_OPCODES
#undef __ENUMERATE_OPCODE
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* OpCode::name() const
{
    return name(opcode_id());
}

const char* execution_result_name(ExecutionResult result)
{
    switch (result) {
#define __ENUMERATE_EXECUTION_RESULT(x) \
    case ExecutionResult::x:            \
        return #x;
        ENUMERATE_EXECUTION_RESULTS
#undef __ENUMERATE_EXECUTION_RESULT
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* character_compare_type_name(CharacterCompareType ch_compare_type)
{
    switch (ch_compare_type) {
#define __ENUMERATE_CHARACTER_COMPARE_TYPE(x) \
    case CharacterCompareType::x:             \
        return #x;
        ENUMERATE_CHARACTER_COMPARE_TYPES
#undef __ENUMERATE_CHARACTER_COMPARE_TYPE
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* character_class_name(CharClass ch_class)
{
    switch (ch_class) {
#define __ENUMERATE_CHARACTER_CLASS(x) \
    case CharClass::x:                 \
        return #x;
        ENUMERATE_CHARACTER_CLASSES
#undef __ENUMERATE_CHARACTER_CLASS
    default:
        ASSERT_NOT_REACHED();
        return "<Unknown>";
    }
}

NonnullOwnPtr<OpCode> ByteCode::next(MatchState& state) const
{
    OwnPtr<OpCode> ret;
    if (state.instruction_position >= size()) {
        ret = make<OpCode_Exit>(*this, state);
        return ret.release_nonnull();
    }

    switch ((OpCodeId)at(state.instruction_position)) {
    case OpCodeId::Exit:
        ret = make<OpCode_Exit>(*this, state);
        break;
    case OpCodeId::Jump:
        ret = make<OpCode_Jump>(*this, state);
        break;
    case OpCodeId::Compare:
        ret = make<OpCode_Compare>(*this, state);
        break;
    case OpCodeId::CheckEnd:
        ret = make<OpCode_CheckEnd>(*this, state);
        break;
    case OpCodeId::ForkJump:
        ret = make<OpCode_ForkJump>(*this, state);
        break;
    case OpCodeId::ForkStay:
        ret = make<OpCode_ForkStay>(*this, state);
        break;
    case OpCodeId::CheckBegin:
        ret = make<OpCode_CheckBegin>(*this, state);
        break;
    case OpCodeId::SaveLeftCaptureGroup:
        ret = make<OpCode_SaveLeftCaptureGroup>(*this, state);
        break;
    case OpCodeId::SaveRightCaptureGroup:
        ret = make<OpCode_SaveRightCaptureGroup>(*this, state);
        break;
    case OpCodeId::SaveLeftNamedCaptureGroup:
        ret = make<OpCode_SaveLeftNamedCaptureGroup>(*this, state);
        break;
    case OpCodeId::SaveRightNamedCaptureGroup:
        ret = make<OpCode_SaveRightNamedCaptureGroup>(*this, state);
        break;
    default:
        fprintf(stderr, "\n[VM] Invalid opcode: %lu, stack index: %lu\n", at(state.instruction_position), state.instruction_position);
        exit(1);
    }

    state.instruction_position += ret->size();

    return ret.release_nonnull();
}

ExecutionResult OpCode_Exit::execute(const MatchInput& input, MatchState& state, MatchOutput&)
{
    if (state.string_position > input.view.length() || state.instruction_position >= m_bytecode.size())
        return ExecutionResult::Done;

    return ExecutionResult::Exit;
}

ExecutionResult OpCode_Jump::execute(const MatchInput&, MatchState& state, MatchOutput&)
{

    state.instruction_position += offset();
    return ExecutionResult::Continue;
}

ExecutionResult OpCode_ForkJump::execute(const MatchInput&, MatchState&, MatchOutput&)
{
    m_state.fork_at_position = m_state.instruction_position + size() + offset();
    return ExecutionResult::Fork_PrioHigh;
}

ExecutionResult OpCode_ForkStay::execute(const MatchInput&, MatchState&, MatchOutput&)
{
    m_state.fork_at_position = m_state.instruction_position + size() + offset();
    return ExecutionResult::Fork_PrioLow;
}

ExecutionResult OpCode_CheckBegin::execute(const MatchInput& input, MatchState& state, MatchOutput&)
{
    if (state.string_position != 0 || ((input.regex_options & AllFlags::Global) && !((input.regex_options & AllFlags::Anchored) || (input.regex_options & AllFlags::Global))))
        return ExecutionResult::Exit;

    return ExecutionResult::Continue;
}

ExecutionResult OpCode_CheckEnd::execute(const MatchInput& input, MatchState& state, MatchOutput&)
{
    if (state.string_position != input.view.length())
        return ExecutionResult::Exit;

    return ExecutionResult::Continue;
}

ExecutionResult OpCode_SaveLeftCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output)
{
    output.capture_group_matches.at(input.match_index).at(id()).column = state.string_position;
    return ExecutionResult::Continue;
}

ExecutionResult OpCode_SaveRightCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output)
{
    auto& match = output.capture_group_matches.at(input.match_index).at(id());
    auto start_position = match.column;
    auto length = state.string_position - start_position;

    if (input.regex_options & AllFlags::StringCopyMatches) {
        match = { input.view.substring_view(start_position, length).to_string(), input.line, start_position };
    } else { // let the view point to the original string ...
        match = { input.view.substring_view(start_position, length), input.line, start_position };
    }

    return ExecutionResult::Continue;
}

ExecutionResult OpCode_SaveLeftNamedCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output)
{
    output.named_capture_group_matches.at(input.match_index).ensure(name()).column = state.string_position;
    return ExecutionResult::Continue;
}

ExecutionResult OpCode_SaveRightNamedCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output)
{
    StringView capture_group_name = name();

    if (output.named_capture_group_matches.at(input.match_index).contains(capture_group_name)) {
        auto start_position = output.named_capture_group_matches.at(input.match_index).ensure(capture_group_name).column;
        auto length = state.string_position - start_position;

        auto& map = output.named_capture_group_matches.at(input.match_index);

#ifdef REGEX_DEBUG
        dbg() << "Save named capture group with name=" << capture_group_name << " and content: " << input.view.substring_view(start_position, length).to_string();
#endif

        if (input.regex_options & AllFlags::StringCopyMatches) {
            map.set(capture_group_name, { input.view.substring_view(start_position, length).to_string(), input.line, start_position });
        } else { // let the view point to the original string ...
            map.set(capture_group_name, { input.view.substring_view(start_position, length), input.line, start_position });
        }
    } else {
        fprintf(stderr, "Didn't find corresponding capture group match for name=%s, match_index=%lu\n", capture_group_name.to_string().characters(), input.match_index);
    }

    return ExecutionResult::Continue;
}

ExecutionResult OpCode_Compare::execute(const MatchInput& input, MatchState& state, MatchOutput&)
{
    bool inverse { false };

    size_t string_position = state.string_position;
    bool inverse_matched { false };

    size_t offset { m_state.instruction_position + 3 };
    for (size_t i = 0; i < arguments_count(); ++i) {
        if (state.string_position > string_position)
            break;

        auto compare_type = (CharacterCompareType)m_bytecode.at(offset++);

        // FIXME: refactor the comparisons out... e.g. as callables
        if (compare_type == CharacterCompareType::Inverse)
            inverse = true;

        else if (compare_type == CharacterCompareType::Char) {
            char ch = m_bytecode.at(offset++);

            // We want to compare a string that is longer or equal in length to the available string
            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::ExitWithFork;

            compare_char(input, state, ch, inverse, inverse_matched);

        } else if (compare_type == CharacterCompareType::AnyChar) {
            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::ExitWithFork;

            ASSERT(!inverse);
            ++state.string_position;

        } else if (compare_type == CharacterCompareType::String) {
            ASSERT(!inverse);

            char* str = reinterpret_cast<char*>(m_bytecode.at(offset++));
            auto& length = m_bytecode.at(offset++);

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < length)
                return ExecutionResult::ExitWithFork;

            if (!compare_string(input, state, str, length))
                return ExecutionResult::ExitWithFork;

        } else if (compare_type == CharacterCompareType::CharClass) {

            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::ExitWithFork;

            auto character_class = (CharClass)m_bytecode.at(offset++);
            auto& ch = input.view[state.string_position];

            compare_character_class(input, state, character_class, ch, inverse, inverse_matched);

        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode.at(offset++);

            auto from = value.from;
            auto to = value.to;
            auto ch = input.view[state.string_position];

            compare_character_range(input, state, from, to, ch, inverse, inverse_matched);

        } else {
            fprintf(stderr, "Undefined comparison: %i\n", (int)compare_type);
            ASSERT_NOT_REACHED();
            break;
        }
    }

    if (inverse && !inverse_matched)
        ++state.string_position;

    if (string_position == state.string_position || state.string_position > input.view.length())
        return ExecutionResult::ExitWithFork;

    return ExecutionResult::Continue;
}

const String OpCode_Compare::arguments_string() const
{
    return String::format("argc=%lu, args=%lu ", arguments_count(), arguments_size());
}

const Vector<String> OpCode_Compare::variable_arguments_to_string(Optional<const MatchInput> input) const
{
    Vector<String> result;

    size_t offset { m_state.instruction_position + 3 };
    StringView view;
    if (input.has_value())
        view = input.value().view;

    for (size_t i = 0; i < arguments_count(); ++i) {
        auto compare_type = (CharacterCompareType)m_bytecode.at(offset++);
        result.empend(String::format("type=%lu [%s]", (size_t)compare_type, character_compare_type_name(compare_type)));

        if (compare_type == CharacterCompareType::Char) {
            char ch = m_bytecode.at(offset++);
            result.empend(String::format("value='%c'", ch));
            if (!view.is_null())
                result.empend(String::format("compare against: '%s'", String { view.substring_view(m_state.string_position, m_state.string_position + 1 > view.length() ? 0 : 1) }.characters()));
        } else if (compare_type == CharacterCompareType::String) {
            char* str = reinterpret_cast<char*>(m_bytecode.at(offset++));
            auto& length = m_bytecode.at(offset++);
            result.empend(String::format("value=\"%s\"", String { str, length }.characters()));
            if (!view.is_null())
                result.empend(String::format("compare against: \"%s\"", String { input.value().view.substring_view(m_state.string_position, m_state.string_position + length > view.length() ? 0 : length) }.characters()));
        } else if (compare_type == CharacterCompareType::CharClass) {
            auto character_class = (CharClass)m_bytecode.at(offset++);
            result.empend(String::format("ch_class=%lu [%s]", (size_t)character_class, character_class_name(character_class)));
            if (!view.is_null())
                result.empend(String::format("compare against: '%s'", String { input.value().view.substring_view(m_state.string_position, m_state.string_position + 1 > view.length() ? 0 : 1) }.characters()));
        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode.at(offset++);
            result.empend(String::format("ch_range='%c'-'%c'", value.from, value.to));
            if (!view.is_null())
                result.empend(String::format("compare against: '%s'", String { input.value().view.substring_view(m_state.string_position, m_state.string_position + 1 > view.length() ? 0 : 1) }.characters()));
        }
    }
    return result;
}

inline void OpCode_Compare::compare_char(const MatchInput& input, MatchState& state, char& ch, bool inverse, bool& inverse_matched) const
{
    auto ch1 = ch;
    auto ch2 = input.view[state.string_position];

    if (input.regex_options & AllFlags::Insensitive) {
        ch1 = tolower(ch1);
        ch2 = tolower(ch2);
    }

    if (ch1 == ch2) {
        if (inverse)
            inverse_matched = true;
        else
            ++state.string_position;
    }
}

inline bool OpCode_Compare::compare_string(const MatchInput& input, MatchState& state, const char* str, size_t length) const
{
    auto str_view1 = StringView(str, length);
    auto str_view2 = StringView(&input.view[state.string_position], length);
    String str1, str2;
    if (input.regex_options & AllFlags::Insensitive) {
        str1 = str_view1.to_string().to_lowercase();
        str2 = str_view2.to_string().to_lowercase();
        str_view1 = str1.view();
        str_view2 = str2.view();
    }

    if (!strncmp(str_view1.characters_without_null_termination(),
            str_view2.characters_without_null_termination(), length)) {
        state.string_position += length;
        return true;
    } else
        return false;
}

inline void OpCode_Compare::compare_character_class(const MatchInput& input, MatchState& state, CharClass character_class, char ch, bool inverse, bool& inverse_matched) const
{
    switch (character_class) {
    case CharClass::Alnum:
        if (isalnum(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Alpha:
        if (isalpha(ch))
            ++state.string_position;
        break;
    case CharClass::Blank:
        if (ch == ' ' || ch == '\t') {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Cntrl:
        if (iscntrl(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Digit:
        if (isdigit(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Graph:
        if (isgraph(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Lower:
        if (islower(ch) || ((input.regex_options & AllFlags::Insensitive) && isupper(ch))) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Print:
        if (isprint(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Punct:
        if (ispunct(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Space:
        if (isspace(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Upper:
        if (isupper(ch) || ((input.regex_options & AllFlags::Insensitive) && islower(ch))) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Xdigit:
        if (isxdigit(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    }
}

inline void OpCode_Compare::compare_character_range(const MatchInput& input, MatchState& state, char from, char to, char ch, bool inverse, bool& inverse_matched) const
{
    if (input.regex_options & AllFlags::Insensitive) {
        from = tolower(from);
        to = tolower(to);
        ch = tolower(ch);
    }

    if (ch >= from && ch <= to) {
        if (inverse)
            inverse_matched = true;
        else
            ++state.string_position;
    }
}

}
}
