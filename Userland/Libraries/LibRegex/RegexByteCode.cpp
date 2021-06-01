/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RegexByteCode.h"
#include "AK/StringBuilder.h"
#include "RegexDebug.h"
#include <AK/CharacterTypes.h>
#include <AK/Debug.h>

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
        VERIFY_NOT_REACHED();
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
        VERIFY_NOT_REACHED();
        return "<Unknown>";
    }
}

const char* boundary_check_type_name(BoundaryCheckType ty)
{
    switch (ty) {
#define __ENUMERATE_BOUNDARY_CHECK_TYPE(x) \
    case BoundaryCheckType::x:             \
        return #x;
        ENUMERATE_BOUNDARY_CHECK_TYPES
#undef __ENUMERATE_BOUNDARY_CHECK_TYPE
    default:
        VERIFY_NOT_REACHED();
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
        VERIFY_NOT_REACHED();
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
        VERIFY_NOT_REACHED();
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
            case OpCodeId::CheckBoundary:
                s_opcodes.set(i, make<OpCode_CheckBoundary>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::ForkJump:
                s_opcodes.set(i, make<OpCode_ForkJump>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::ForkStay:
                s_opcodes.set(i, make<OpCode_ForkStay>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::FailForks:
                s_opcodes.set(i, make<OpCode_FailForks>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::Save:
                s_opcodes.set(i, make<OpCode_Save>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::Restore:
                s_opcodes.set(i, make<OpCode_Restore>(*const_cast<ByteCode*>(this)));
                break;
            case OpCodeId::GoBack:
                s_opcodes.set(i, make<OpCode_GoBack>(*const_cast<ByteCode*>(this)));
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

ALWAYS_INLINE ExecutionResult OpCode_Save::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    input.saved_positions.append(state.string_position);
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Restore::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    if (input.saved_positions.is_empty())
        return ExecutionResult::Failed;

    state.string_position = input.saved_positions.take_last();
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_GoBack::execute(const MatchInput&, MatchState& state, MatchOutput&) const
{
    if (count() > state.string_position)
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    state.string_position -= count();
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_FailForks::execute(const MatchInput& input, MatchState&, MatchOutput&) const
{
    VERIFY(count() > 0);

    input.fail_counter += count() - 1;
    return ExecutionResult::Failed_ExecuteLowPrioForks;
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
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    if ((0 == state.string_position && !(input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (0 != state.string_position && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (0 == state.string_position && (input.regex_options & AllFlags::Global)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckBoundary::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    auto isword = [](auto ch) { return is_ascii_alphanumeric(ch) || ch == '_'; };
    auto is_word_boundary = [&] {
        if (state.string_position == input.view.length()) {
            if (state.string_position > 0 && isword(input.view[state.string_position - 1]))
                return true;
            return false;
        }

        if (state.string_position == 0) {
            if (isword(input.view[0]))
                return true;

            return false;
        }

        return !!(isword(input.view[state.string_position]) ^ isword(input.view[state.string_position - 1]));
    };
    switch (type()) {
    case BoundaryCheckType::Word: {
        if (is_word_boundary())
            return ExecutionResult::Continue;
        return ExecutionResult::Failed_ExecuteLowPrioForks;
    }
    case BoundaryCheckType::NonWord: {
        if (!is_word_boundary())
            return ExecutionResult::Continue;
        return ExecutionResult::Failed_ExecuteLowPrioForks;
    }
    }
    VERIFY_NOT_REACHED();
}

ALWAYS_INLINE ExecutionResult OpCode_CheckEnd::execute(const MatchInput& input, MatchState& state, MatchOutput&) const
{
    if (state.string_position == input.view.length() && (input.regex_options & AllFlags::MatchNotEndOfLine))
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    if ((state.string_position == input.view.length() && !(input.regex_options & AllFlags::MatchNotEndOfLine))
        || (state.string_position != input.view.length() && (input.regex_options & AllFlags::MatchNotEndOfLine || input.regex_options & AllFlags::MatchNotBeginOfLine)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed_ExecuteLowPrioForks;
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

    VERIFY(start_position + length <= input.view.length());

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

        if constexpr (REGEX_DEBUG) {
            VERIFY(start_position + length <= input.view.length());
            dbgln("Save named capture group with name={} and content='{}'", capture_group_name, input.view.substring_view(start_position, length));
        }

        VERIFY(start_position + length <= input.view.length());
        auto view = input.view.substring_view(start_position, length);
        if (input.regex_options & AllFlags::StringCopyMatches) {
            map.set(capture_group_name, { view.to_string(), input.line, start_position, input.global_offset + start_position }); // create a copy of the original string
        } else {
            map.set(capture_group_name, { view, input.line, start_position, input.global_offset + start_position }); // take view to original string
        }
    } else {
        warnln("Didn't find corresponding capture group match for name={}, match_index={}", capture_group_name.to_string(), input.match_index);
    }

    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Compare::execute(const MatchInput& input, MatchState& state, MatchOutput& output) const
{
    bool inverse { false };
    bool temporary_inverse { false };
    bool reset_temp_inverse { false };

    auto current_inversion_state = [&]() -> bool { return temporary_inverse ^ inverse; };

    size_t string_position = state.string_position;
    bool inverse_matched { false };
    bool had_zero_length_match { false };

    size_t offset { state.instruction_position + 3 };
    for (size_t i = 0; i < arguments_count(); ++i) {
        if (state.string_position > string_position)
            break;

        if (reset_temp_inverse) {
            reset_temp_inverse = false;
            temporary_inverse = false;
        } else {
            reset_temp_inverse = true;
        }

        auto compare_type = (CharacterCompareType)m_bytecode->at(offset++);

        if (compare_type == CharacterCompareType::Inverse)
            inverse = true;

        else if (compare_type == CharacterCompareType::TemporaryInverse) {
            // If "TemporaryInverse" is given, negate the current inversion state only for the next opcode.
            // it follows that this cannot be the last compare element.
            VERIFY(i != arguments_count() - 1);

            temporary_inverse = true;
            reset_temp_inverse = false;

        } else if (compare_type == CharacterCompareType::Char) {
            u32 ch = m_bytecode->at(offset++);

            // We want to compare a string that is longer or equal in length to the available string
            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            compare_char(input, state, ch, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::AnyChar) {
            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            VERIFY(!current_inversion_state());
            ++state.string_position;

        } else if (compare_type == CharacterCompareType::String) {
            VERIFY(!current_inversion_state());

            const auto& length = m_bytecode->at(offset++);
            StringBuilder str_builder;
            for (size_t i = 0; i < length; ++i)
                str_builder.append(m_bytecode->at(offset++));

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < length)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            if (!compare_string(input, state, str_builder.string_view().characters_without_null_termination(), length, had_zero_length_match))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else if (compare_type == CharacterCompareType::CharClass) {

            if (input.view.length() - state.string_position < 1)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto character_class = (CharClass)m_bytecode->at(offset++);
            auto ch = input.view[state.string_position];

            compare_character_class(input, state, character_class, ch, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode->at(offset++);

            auto from = value.from;
            auto to = value.to;
            auto ch = input.view[state.string_position];

            compare_character_range(input, state, from, to, ch, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::Reference) {
            auto reference_number = (size_t)m_bytecode->at(offset++);
            auto& groups = output.capture_group_matches.at(input.match_index);
            if (groups.size() <= reference_number)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto str = groups.at(reference_number).view;

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < str.length())
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            if (!compare_string(input, state, str.characters_without_null_termination(), str.length(), had_zero_length_match))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else if (compare_type == CharacterCompareType::NamedReference) {
            auto ptr = (const char*)m_bytecode->at(offset++);
            auto length = (size_t)m_bytecode->at(offset++);
            StringView name { ptr, length };

            auto group = output.named_capture_group_matches.at(input.match_index).get(name);
            if (!group.has_value())
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto str = group.value().view;

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() - state.string_position < str.length())
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            if (!compare_string(input, state, str.characters_without_null_termination(), str.length(), had_zero_length_match))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else {
            warnln("Undefined comparison: {}", (int)compare_type);
            VERIFY_NOT_REACHED();
            break;
        }
    }

    if (current_inversion_state() && !inverse_matched)
        ++state.string_position;

    if ((!had_zero_length_match && string_position == state.string_position) || state.string_position > input.view.length())
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    return ExecutionResult::Continue;
}

ALWAYS_INLINE void OpCode_Compare::compare_char(const MatchInput& input, MatchState& state, u32 ch1, bool inverse, bool& inverse_matched)
{
    u32 ch2 = input.view[state.string_position];

    if (input.regex_options & AllFlags::Insensitive) {
        ch1 = to_ascii_lowercase(ch1);
        ch2 = to_ascii_uppercase(ch2);
    }

    if (ch1 == ch2) {
        if (inverse)
            inverse_matched = true;
        else
            ++state.string_position;
    }
}

ALWAYS_INLINE bool OpCode_Compare::compare_string(const MatchInput& input, MatchState& state, const char* str, size_t length, bool& had_zero_length_match)
{
    if (input.view.is_u8_view()) {
        auto str_view1 = StringView(str, length);
        auto str_view2 = StringView(&input.view.u8view()[state.string_position], length);

        String str1, str2;
        if (input.regex_options & AllFlags::Insensitive) {
            str1 = str_view1.to_string().to_lowercase();
            str2 = str_view2.to_string().to_lowercase();
            str_view1 = str1.view();
            str_view2 = str2.view();
        }

        if (str_view1 == str_view2) {
            state.string_position += length;
            if (length == 0)
                had_zero_length_match = true;
            return true;
        }
    }

    return false;
}

ALWAYS_INLINE void OpCode_Compare::compare_character_class(const MatchInput& input, MatchState& state, CharClass character_class, u32 ch, bool inverse, bool& inverse_matched)
{
    switch (character_class) {
    case CharClass::Alnum:
        if (is_ascii_alphanumeric(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Alpha:
        if (is_ascii_alpha(ch))
            ++state.string_position;
        break;
    case CharClass::Blank:
        if (is_ascii_blank(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Cntrl:
        if (is_ascii_control(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Digit:
        if (is_ascii_digit(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Graph:
        if (is_ascii_graphical(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Lower:
        if (is_ascii_lower_alpha(ch) || ((input.regex_options & AllFlags::Insensitive) && is_ascii_upper_alpha(ch))) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Print:
        if (is_ascii_printable(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Punct:
        if (is_ascii_punctuation(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Space:
        if (is_ascii_space(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Upper:
        if (is_ascii_upper_alpha(ch) || ((input.regex_options & AllFlags::Insensitive) && is_ascii_lower_alpha(ch))) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Word:
        if (is_ascii_alphanumeric(ch) || ch == '_') {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    case CharClass::Xdigit:
        if (is_ascii_hex_digit(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                ++state.string_position;
        }
        break;
    }
}

ALWAYS_INLINE void OpCode_Compare::compare_character_range(const MatchInput& input, MatchState& state, u32 from, u32 to, u32 ch, bool inverse, bool& inverse_matched)
{
    if (input.regex_options & AllFlags::Insensitive) {
        from = to_ascii_lowercase(from);
        to = to_ascii_lowercase(to);
        ch = to_ascii_lowercase(ch);
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
    return String::formatted("argc={}, args={} ", arguments_count(), arguments_size());
}

const Vector<String> OpCode_Compare::variable_arguments_to_string(Optional<MatchInput> input) const
{
    Vector<String> result;

    size_t offset { state().instruction_position + 3 };
    RegexStringView view = ((input.has_value()) ? input.value().view : nullptr);

    for (size_t i = 0; i < arguments_count(); ++i) {
        auto compare_type = (CharacterCompareType)m_bytecode->at(offset++);
        result.empend(String::formatted("type={} [{}]", (size_t)compare_type, character_compare_type_name(compare_type)));

        auto compared_against_string_start_offset = state().string_position > 0 ? state().string_position - 1 : state().string_position;

        if (compare_type == CharacterCompareType::Char) {
            auto ch = m_bytecode->at(offset++);
            auto is_ascii = is_ascii_printable(ch);
            if (is_ascii)
                result.empend(String::formatted("value='{:c}'", static_cast<char>(ch)));
            else
                result.empend(String::formatted("value={:x}", ch));

            if (!view.is_null() && view.length() > state().string_position) {
                if (is_ascii) {
                    result.empend(String::formatted(
                        "compare against: '{}'",
                        view.substring_view(compared_against_string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
                } else {
                    auto str = view.substring_view(compared_against_string_start_offset, state().string_position > view.length() ? 0 : 1).to_string();
                    u8 buf[8] { 0 };
                    __builtin_memcpy(buf, str.characters(), min(str.length(), sizeof(buf)));
                    result.empend(String::formatted("compare against: {:x},{:x},{:x},{:x},{:x},{:x},{:x},{:x}",
                        buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6], buf[7]));
                }
            }
        } else if (compare_type == CharacterCompareType::NamedReference) {
            auto ptr = (const char*)m_bytecode->at(offset++);
            auto length = m_bytecode->at(offset++);
            result.empend(String::formatted("name='{}'", StringView { ptr, (size_t)length }));
        } else if (compare_type == CharacterCompareType::Reference) {
            auto ref = m_bytecode->at(offset++);
            result.empend(String::formatted("number={}", ref));
        } else if (compare_type == CharacterCompareType::String) {
            auto& length = m_bytecode->at(offset++);
            StringBuilder str_builder;
            for (size_t i = 0; i < length; ++i)
                str_builder.append(m_bytecode->at(offset++));
            result.empend(String::formatted("value=\"{}\"", str_builder.string_view().substring_view(0, length)));
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: \"{}\"",
                    input.value().view.substring_view(compared_against_string_start_offset, compared_against_string_start_offset + length > view.length() ? 0 : length).to_string()));
        } else if (compare_type == CharacterCompareType::CharClass) {
            auto character_class = (CharClass)m_bytecode->at(offset++);
            result.empend(String::formatted("ch_class={} [{}]", (size_t)character_class, character_class_name(character_class)));
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: '{}'",
                    input.value().view.substring_view(compared_against_string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode->at(offset++);
            result.empend(String::formatted("ch_range='{:c}'-'{:c}'", value.from, value.to));
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: '{}'",
                    input.value().view.substring_view(compared_against_string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
        }
    }
    return result;
}
}
