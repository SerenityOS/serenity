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

static const char* character_class_name(CharClass ch_class)
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

HashMap<u32, OwnPtr<OpCode>> ByteCode::s_opcodes {};

ALWAYS_INLINE OpCode* ByteCode::get_opcode_by_id(OpCodeId id) const
{
    if (!s_opcodes.size()) {
        for (u32 i = (u32)OpCodeId::First; i <= (u32)OpCodeId::Last; ++i) {
            switch ((OpCodeId)i) {
            case OpCodeId::Exit:
                s_opcodes.set(i, make<OpCode_Exit>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::Jump:
                s_opcodes.set(i, make<OpCode_Jump>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::Compare:
                s_opcodes.set(i, make<OpCode_Compare>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::CheckEnd:
                s_opcodes.set(i, make<OpCode_CheckEnd>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::ForkJump:
                s_opcodes.set(i, make<OpCode_ForkJump>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::ForkStay:
                s_opcodes.set(i, make<OpCode_ForkStay>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::CheckBegin:
                s_opcodes.set(i, make<OpCode_CheckBegin>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::SaveLeftCaptureGroup:
                s_opcodes.set(i, make<OpCode_SaveLeftCaptureGroup>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::SaveRightCaptureGroup:
                s_opcodes.set(i, make<OpCode_SaveRightCaptureGroup>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::SaveLeftNamedCaptureGroup:
                s_opcodes.set(i, make<OpCode_SaveLeftNamedCaptureGroup>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::SaveRightNamedCaptureGroup:
                s_opcodes.set(i, make<OpCode_SaveRightNamedCaptureGroup>(*const_cast<ByteCode*>(this)));
                break;
            }
        }
    }

    if (id > OpCodeId::Last)
        return nullptr;

    return const_cast<OpCode*>(s_opcodes.get((u32)id).value())->set_bytecode(*const_cast<ByteCode*>(this));
}

OpCode* ByteCode::get_opcode(MatchState& state) const
{
    OpCode* op_code;

    if (state.instruction_position >= size()) {
        op_code = get_opcode_by_id(OpCodeId::Exit);
    } else
        op_code = get_opcode_by_id((OpCodeId)at(state.instruction_position));

    if (op_code)
        op_code->set_state(state);

    return op_code;
}

ALWAYS_INLINE ExecutionResult OpCode_Exit::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    if (state.string_position > input.view.length() || state.instruction_position >= m_bytecode->size())
        return ExecutionResult::Succeeded;

    return ExecutionResult::Failed;
}

ALWAYS_INLINE ExecutionResult OpCode_Jump::execute(const MatchInput&, MatchState& state, MatchOutput&) const
{

    state.instruction_position += offset();
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkJump::execute(const MatchInput&, MatchState& state, MatchOutput&) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    return ExecutionResult::Fork_PrioHigh;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkStay::execute(const MatchInput&, MatchState& state, MatchOutput&) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    return ExecutionResult::Fork_PrioLow;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckBegin::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    if (0 == state.string_position && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        return ExecutionResult::Failed;

    if ((0 == state.string_position && !(input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (0 != state.string_position && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (0 == state.string_position && (input.regex_options & AllFlags::Global)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckEnd::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    if (state.string_position == input.view.length() && (input.regex_options & AllFlags::MatchNotEndOfLine))
        return ExecutionResult::Failed;

    if ((state.string_position == input.view.length() && !(input.regex_options & AllFlags::MatchNotEndOfLine))
        || (state.string_position != input.view.length() && (input.regex_options & AllFlags::MatchNotEndOfLine || input.regex_options & AllFlags::MatchNotBeginOfLine)))
        return ExecutionResult::Succeeded;

    return ExecutionResult::Failed;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveLeftCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output) const
{
    if (input.match_index >= output.capture_group_matches.size()) {
        output.capture_group_matches.ensure_capacity(input.match_index);
        auto capacity = output.capture_group_matches.capacity();
        for (size_t i = output.capture_group_matches.size(); i <= capacity; ++i)
            output.capture_group_matches.empend();
    }

    if (id() >= output.capture_group_matches.at(input.match_index).size()) {
        output.capture_group_matches.at(input.match_index).ensure_capacity(id());
        auto capacity = output.capture_group_matches.at(input.match_index).capacity();
        for (size_t i = output.capture_group_matches.at(input.match_index).size(); i <= capacity; ++i)
            output.capture_group_matches.at(input.match_index).empend();
    }

    output.capture_group_matches.at(input.match_index).at(id()).left_column = state.string_position;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveRightCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output) const
{
    auto& match = output.capture_group_matches.at(input.match_index).at(id());
    auto start_position = match.left_column;
    auto length = state.string_position - start_position;

    if (start_position < match.column)
        return ExecutionResult::Continue;

    ASSERT(start_position + length <= input.view.length());

    auto view = input.view.substring_view(start_position, length);

    if (input.regex_options & AllFlags::StringCopyMatches) {
        match = { view.to_string(), input.line, start_position, input.global_offset + start_position }; // create a copy of the original string
    } else {
        match = { view, input.line, start_position, input.global_offset + start_position }; // take view to original string
    }

    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveLeftNamedCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output) const
{
    if (input.match_index >= output.named_capture_group_matches.size()) {
        output.named_capture_group_matches.ensure_capacity(input.match_index);
        auto capacity = output.named_capture_group_matches.capacity();
        for (size_t i = output.named_capture_group_matches.size(); i <= capacity; ++i)
            output.named_capture_group_matches.empend();
    }
    output.named_capture_group_matches.at(input.match_index).ensure(name()).column = state.string_position;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveRightNamedCaptureGroup::execute(const MatchInput& input, MatchState& state, MatchOutput& output) const
{
    StringView capture_group_name = name();

    if (output.named_capture_group_matches.at(input.match_index).contains(capture_group_name)) {
        auto start_position = output.named_capture_group_matches.at(input.match_index).ensure(capture_group_name).column;
        auto length = state.string_position - start_position;

        auto& map = output.named_capture_group_matches.at(input.match_index);

#ifdef REGEX_DEBUG
        ASSERT(start_position + length < input.view.length());
        dbg() << "Save named capture group with name=" << capture_group_name << " and content: " << input.view.substring_view(start_position, length).to_string();
#endif

        ASSERT(start_position + length <= input.view.length());
        auto view = input.view.substring_view(start_position, length);
        if (input.regex_options & AllFlags::StringCopyMatches) {
            map.set(capture_group_name, { view.to_string(), input.line, start_position, input.global_offset + start_position }); // create a copy of the original string
        } else {
            map.set(capture_group_name, { view, input.line, start_position, input.global_offset + start_position }); // take view to original string
        }
    } else {
        fprintf(stderr, "Didn't find corresponding capture group match for name=%s, match_index=%lu\n", capture_group_name.to_string().characters(), input.match_index);
    }

    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Compare::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    bool inverse { false };

    size_t string_position = state.string_position;
    bool inverse_matched { false };

    size_t offset { state.instruction_position + 3 };
    for (size_t i = 0; i < arguments_count(); ++i) {
        if (state.string_position > string_position)
            break;

        auto compare_type = (CharacterCompareType)m_bytecode->at(offset++);

        if (compare_type == CharacterCompareType::Inverse)
            inverse = true;

        else if (compare_type == CharacterCompareType::Char) {
            char ch = m_bytecode->at(offset++);

            // We want to compare a string that is longer or equal in length to the available string
            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            compare_char(input, state, ch, inverse, inverse_matched);

        } else if (compare_type == CharacterCompareType::AnyChar) {
            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            ASSERT(!inverse);
            ++state.string_position;

        } else if (compare_type == CharacterCompareType::String) {
            ASSERT(!inverse);

            char* str = reinterpret_cast<char*>(m_bytecode->at(offset++));
            auto& length = m_bytecode->at(offset++);

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < length)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            if (!compare_string(input, state, str, length))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else if (compare_type == CharacterCompareType::CharClass) {

            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto character_class = (CharClass)m_bytecode->at(offset++);
            auto& ch = input.view[state.string_position];

            compare_character_class(input, state, character_class, ch, inverse, inverse_matched);

        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode->at(offset++);

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
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    return ExecutionResult::Continue;
}

ALWAYS_INLINE void OpCode_Compare::compare_char(const MatchInput& input, MatchState& state, char& ch, bool inverse, bool& inverse_matched) const
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

ALWAYS_INLINE bool OpCode_Compare::compare_string(const MatchInput& input, MatchState& state, const char* str, size_t length) const
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

    if (str_view1 == str_view2) {
        state.string_position += length;
        return true;
    } else
        return false;
}

ALWAYS_INLINE void OpCode_Compare::compare_character_class(const MatchInput& input, MatchState& state, CharClass character_class, char ch, bool inverse, bool& inverse_matched) const
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

ALWAYS_INLINE void OpCode_Compare::compare_character_range(const MatchInput& input, MatchState& state, char from, char to, char ch, bool inverse, bool& inverse_matched) const
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

const String OpCode_Compare::arguments_string() const
{
    return String::format("argc=%lu, args=%lu ", arguments_count(), arguments_size());
}

const Vector<String> OpCode_Compare::variable_arguments_to_string(Optional<MatchInput> input) const
{
    Vector<String> result;

    size_t offset { state().instruction_position + 3 };
    StringView view;
    if (input.has_value())
        view = input.value().view;

    for (size_t i = 0; i < arguments_count(); ++i) {
        auto compare_type = (CharacterCompareType)m_bytecode->at(offset++);
        result.empend(String::format("type=%lu [%s]", (size_t)compare_type, character_compare_type_name(compare_type)));

        if (compare_type == CharacterCompareType::Char) {
            char ch = m_bytecode->at(offset++);
            result.empend(String::format("value='%c'", ch));
            if (!view.is_null())
                result.empend(String::format("compare against: '%s'", String { view.substring_view(state().string_position, state().string_position + 1 > view.length() ? 0 : 1) }.characters()));
        } else if (compare_type == CharacterCompareType::String) {
            char* str = reinterpret_cast<char*>(m_bytecode->at(offset++));
            auto& length = m_bytecode->at(offset++);
            result.empend(String::format("value=\"%s\"", String { str, length }.characters()));
            if (!view.is_null())
                result.empend(String::format("compare against: \"%s\"", String { input.value().view.substring_view(state().string_position, state().string_position + length > view.length() ? 0 : length) }.characters()));
        } else if (compare_type == CharacterCompareType::CharClass) {
            auto character_class = (CharClass)m_bytecode->at(offset++);
            result.empend(String::format("ch_class=%lu [%s]", (size_t)character_class, character_class_name(character_class)));
            if (!view.is_null())
                result.empend(String::format("compare against: '%s'", String { input.value().view.substring_view(state().string_position, state().string_position + 1 > view.length() ? 0 : 1) }.characters()));
        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode->at(offset++);
            result.empend(String::format("ch_range='%c'-'%c'", value.from, value.to));
            if (!view.is_null())
                result.empend(String::format("compare against: '%s'", String { input.value().view.substring_view(state().string_position, state().string_position + 1 > view.length() ? 0 : 1) }.characters()));
        }
    }
    return result;
}
}
