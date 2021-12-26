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
#include <LibUnicode/CharacterTypes.h>

namespace regex {

char const* OpCode::name(OpCodeId opcode_id)
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

char const* OpCode::name() const
{
    return name(opcode_id());
}

char const* execution_result_name(ExecutionResult result)
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

char const* boundary_check_type_name(BoundaryCheckType ty)
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

char const* character_compare_type_name(CharacterCompareType ch_compare_type)
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

static char const* character_class_name(CharClass ch_class)
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

static void advance_string_position(MatchState& state, RegexStringView const& view, Optional<u32> code_point = {})
{
    ++state.string_position;

    if (view.unicode()) {
        if (!code_point.has_value() && (state.string_position_in_code_units < view.length_in_code_units()))
            code_point = view[state.string_position_in_code_units];
        if (code_point.has_value())
            state.string_position_in_code_units += view.length_of_code_point(*code_point);
    } else {
        ++state.string_position_in_code_units;
    }
}

static void save_string_position(MatchInput const& input, MatchState const& state)
{
    input.saved_positions.append(state.string_position);
    input.saved_code_unit_positions.append(state.string_position_in_code_units);
}

static bool restore_string_position(MatchInput const& input, MatchState& state)
{
    if (input.saved_positions.is_empty())
        return false;

    state.string_position = input.saved_positions.take_last();
    state.string_position_in_code_units = input.saved_code_unit_positions.take_last();
    return true;
}

OwnPtr<OpCode> ByteCode::s_opcodes[(size_t)OpCodeId::Last + 1];
bool ByteCode::s_opcodes_initialized { false };

void ByteCode::ensure_opcodes_initialized()
{
    if (s_opcodes_initialized)
        return;
    for (u32 i = (u32)OpCodeId::First; i <= (u32)OpCodeId::Last; ++i) {
        switch ((OpCodeId)i) {
        case OpCodeId::Exit:
            s_opcodes[i] = make<OpCode_Exit>();
            break;
        case OpCodeId::Jump:
            s_opcodes[i] = make<OpCode_Jump>();
            break;
        case OpCodeId::Compare:
            s_opcodes[i] = make<OpCode_Compare>();
            break;
        case OpCodeId::CheckEnd:
            s_opcodes[i] = make<OpCode_CheckEnd>();
            break;
        case OpCodeId::CheckBoundary:
            s_opcodes[i] = make<OpCode_CheckBoundary>();
            break;
        case OpCodeId::ForkJump:
            s_opcodes[i] = make<OpCode_ForkJump>();
            break;
        case OpCodeId::ForkStay:
            s_opcodes[i] = make<OpCode_ForkStay>();
            break;
        case OpCodeId::FailForks:
            s_opcodes[i] = make<OpCode_FailForks>();
            break;
        case OpCodeId::Save:
            s_opcodes[i] = make<OpCode_Save>();
            break;
        case OpCodeId::Restore:
            s_opcodes[i] = make<OpCode_Restore>();
            break;
        case OpCodeId::GoBack:
            s_opcodes[i] = make<OpCode_GoBack>();
            break;
        case OpCodeId::CheckBegin:
            s_opcodes[i] = make<OpCode_CheckBegin>();
            break;
        case OpCodeId::ClearCaptureGroup:
            s_opcodes[i] = make<OpCode_ClearCaptureGroup>();
            break;
        case OpCodeId::ClearNamedCaptureGroup:
            s_opcodes[i] = make<OpCode_ClearNamedCaptureGroup>();
            break;
        case OpCodeId::SaveLeftCaptureGroup:
            s_opcodes[i] = make<OpCode_SaveLeftCaptureGroup>();
            break;
        case OpCodeId::SaveRightCaptureGroup:
            s_opcodes[i] = make<OpCode_SaveRightCaptureGroup>();
            break;
        case OpCodeId::SaveLeftNamedCaptureGroup:
            s_opcodes[i] = make<OpCode_SaveLeftNamedCaptureGroup>();
            break;
        case OpCodeId::SaveRightNamedCaptureGroup:
            s_opcodes[i] = make<OpCode_SaveRightNamedCaptureGroup>();
            break;
        }
    }
    s_opcodes_initialized = true;
}

ALWAYS_INLINE OpCode& ByteCode::get_opcode_by_id(OpCodeId id) const
{
    VERIFY(id >= OpCodeId::First && id <= OpCodeId::Last);

    auto& opcode = s_opcodes[(u32)id];
    opcode->set_bytecode(*const_cast<ByteCode*>(this));
    return *opcode;
}

OpCode& ByteCode::get_opcode(MatchState& state) const
{
    OpCodeId opcode_id;
    if (state.instruction_position >= size())
        opcode_id = OpCodeId::Exit;
    else
        opcode_id = (OpCodeId)at(state.instruction_position);

    auto& opcode = get_opcode_by_id(opcode_id);
    opcode.set_state(state);
    return opcode;
}

ALWAYS_INLINE ExecutionResult OpCode_Exit::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (state.string_position > input.view.length() || state.instruction_position >= m_bytecode->size())
        return ExecutionResult::Succeeded;

    return ExecutionResult::Failed;
}

ALWAYS_INLINE ExecutionResult OpCode_Save::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    save_string_position(input, state);
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Restore::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (!restore_string_position(input, state))
        return ExecutionResult::Failed;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_GoBack::execute(MatchInput const&, MatchState& state, MatchOutput&) const
{
    if (count() > state.string_position)
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    state.string_position -= count();
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_FailForks::execute(MatchInput const& input, MatchState&, MatchOutput&) const
{
    VERIFY(count() > 0);

    input.fail_counter += count() - 1;
    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_Jump::execute(MatchInput const&, MatchState& state, MatchOutput&) const
{
    state.instruction_position += offset();
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkJump::execute(MatchInput const&, MatchState& state, MatchOutput&) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    return ExecutionResult::Fork_PrioHigh;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkStay::execute(MatchInput const&, MatchState& state, MatchOutput&) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    return ExecutionResult::Fork_PrioLow;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckBegin::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (0 == state.string_position && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    if ((0 == state.string_position && !(input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (0 != state.string_position && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (0 == state.string_position && (input.regex_options & AllFlags::Global)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckBoundary::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    auto isword = [](auto ch) { return is_ascii_alphanumeric(ch) || ch == '_'; };
    auto is_word_boundary = [&] {
        if (state.string_position == input.view.length()) {
            if (state.string_position > 0 && isword(input.view[state.string_position_in_code_units - 1]))
                return true;
            return false;
        }

        if (state.string_position == 0) {
            if (isword(input.view[0]))
                return true;

            return false;
        }

        return !!(isword(input.view[state.string_position_in_code_units]) ^ isword(input.view[state.string_position_in_code_units - 1]));
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

ALWAYS_INLINE ExecutionResult OpCode_CheckEnd::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (state.string_position == input.view.length() && (input.regex_options & AllFlags::MatchNotEndOfLine))
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    if ((state.string_position == input.view.length() && !(input.regex_options & AllFlags::MatchNotEndOfLine))
        || (state.string_position != input.view.length() && (input.regex_options & AllFlags::MatchNotEndOfLine || input.regex_options & AllFlags::MatchNotBeginOfLine)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_ClearCaptureGroup::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (input.match_index < state.capture_group_matches.size()) {
        auto& group = state.capture_group_matches[input.match_index];
        if (id() < group.size())
            group[id()].reset();
    }
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveLeftCaptureGroup::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (input.match_index >= state.capture_group_matches.size()) {
        state.capture_group_matches.ensure_capacity(input.match_index);
        auto capacity = state.capture_group_matches.capacity();
        for (size_t i = state.capture_group_matches.size(); i <= capacity; ++i)
            state.capture_group_matches.empend();
    }

    if (id() >= state.capture_group_matches.at(input.match_index).size()) {
        state.capture_group_matches.at(input.match_index).ensure_capacity(id());
        auto capacity = state.capture_group_matches.at(input.match_index).capacity();
        for (size_t i = state.capture_group_matches.at(input.match_index).size(); i <= capacity; ++i)
            state.capture_group_matches.at(input.match_index).empend();
    }

    state.capture_group_matches.at(input.match_index).at(id()).left_column = state.string_position;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveRightCaptureGroup::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    auto& match = state.capture_group_matches.at(input.match_index).at(id());
    auto start_position = match.left_column;
    if (state.string_position < start_position)
        return ExecutionResult::Failed_ExecuteLowPrioForks;

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

ALWAYS_INLINE ExecutionResult OpCode_ClearNamedCaptureGroup::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (input.match_index < state.capture_group_matches.size()) {
        auto& group = state.named_capture_group_matches[input.match_index];
        if (auto it = group.find(name()); it != group.end())
            it->value.reset();
    }
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveLeftNamedCaptureGroup::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    if (input.match_index >= state.named_capture_group_matches.size()) {
        state.named_capture_group_matches.ensure_capacity(input.match_index);
        auto capacity = state.named_capture_group_matches.capacity();
        for (size_t i = state.named_capture_group_matches.size(); i <= capacity; ++i)
            state.named_capture_group_matches.empend();
    }
    state.named_capture_group_matches.at(input.match_index).ensure(name()).column = state.string_position;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveRightNamedCaptureGroup::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    StringView capture_group_name = name();

    if (state.named_capture_group_matches.at(input.match_index).contains(capture_group_name)) {
        auto start_position = state.named_capture_group_matches.at(input.match_index).ensure(capture_group_name).column;
        auto length = state.string_position - start_position;

        auto& map = state.named_capture_group_matches.at(input.match_index);

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

ALWAYS_INLINE ExecutionResult OpCode_Compare::execute(MatchInput const& input, MatchState& state, MatchOutput&) const
{
    bool inverse { false };
    bool temporary_inverse { false };
    bool reset_temp_inverse { false };

    auto current_inversion_state = [&]() -> bool { return temporary_inverse ^ inverse; };

    size_t string_position = state.string_position;
    bool inverse_matched { false };
    bool had_zero_length_match { false };

    state.string_position_before_match = state.string_position;

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
            if (input.view.length() <= state.string_position)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            compare_char(input, state, ch, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::AnyChar) {
            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() <= state.string_position)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            VERIFY(!current_inversion_state());
            advance_string_position(state, input.view);

        } else if (compare_type == CharacterCompareType::String) {
            VERIFY(!current_inversion_state());

            auto const& length = m_bytecode->at(offset++);

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() < state.string_position + length)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            Optional<String> str;
            Vector<u16> utf16;
            Vector<u32> data;
            data.ensure_capacity(length);
            for (size_t i = offset; i < offset + length; ++i)
                data.unchecked_append(m_bytecode->at(i));

            auto view = input.view.construct_as_same(data, str, utf16);
            offset += length;
            if (!compare_string(input, state, view, had_zero_length_match))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else if (compare_type == CharacterCompareType::CharClass) {

            if (input.view.length() <= state.string_position)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto character_class = (CharClass)m_bytecode->at(offset++);
            auto ch = input.view[state.string_position_in_code_units];

            compare_character_class(input, state, character_class, ch, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::CharRange) {
            if (input.view.length() <= state.string_position)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto value = (CharRange)m_bytecode->at(offset++);

            auto from = value.from;
            auto to = value.to;
            auto ch = input.view[state.string_position_in_code_units];

            compare_character_range(input, state, from, to, ch, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::Reference) {
            auto reference_number = (size_t)m_bytecode->at(offset++);
            auto& groups = state.capture_group_matches.at(input.match_index);
            if (groups.size() <= reference_number)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto str = groups.at(reference_number).view;

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() < state.string_position + str.length())
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            if (!compare_string(input, state, str, had_zero_length_match))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else if (compare_type == CharacterCompareType::NamedReference) {
            auto ptr = (char const*)m_bytecode->at(offset++);
            auto length = (size_t)m_bytecode->at(offset++);
            StringView name { ptr, length };

            auto group = state.named_capture_group_matches.at(input.match_index).get(name);
            if (!group.has_value())
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto str = group.value().view;

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() < state.string_position + str.length())
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            if (!compare_string(input, state, str, had_zero_length_match))
                return ExecutionResult::Failed_ExecuteLowPrioForks;

        } else if (compare_type == CharacterCompareType::Property) {
            auto property = static_cast<Unicode::Property>(m_bytecode->at(offset++));
            compare_property(input, state, property, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::GeneralCategory) {
            auto general_category = static_cast<Unicode::GeneralCategory>(m_bytecode->at(offset++));
            compare_general_category(input, state, general_category, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::Script) {
            auto script = static_cast<Unicode::Script>(m_bytecode->at(offset++));
            compare_script(input, state, script, current_inversion_state(), inverse_matched);

        } else {
            warnln("Undefined comparison: {}", (int)compare_type);
            VERIFY_NOT_REACHED();
            break;
        }
    }

    if (current_inversion_state() && !inverse_matched)
        advance_string_position(state, input.view);

    if ((!had_zero_length_match && string_position == state.string_position) || state.string_position > input.view.length())
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    return ExecutionResult::Continue;
}

ALWAYS_INLINE void OpCode_Compare::compare_char(MatchInput const& input, MatchState& state, u32 ch1, bool inverse, bool& inverse_matched)
{
    if (state.string_position == input.view.length())
        return;

    auto input_view = input.view.substring_view(state.string_position, 1);
    Optional<String> str;
    Vector<u16> utf16;
    auto compare_view = input_view.construct_as_same({ &ch1, 1 }, str, utf16);
    bool equal;
    if (input.regex_options & AllFlags::Insensitive)
        equal = input_view.equals_ignoring_case(compare_view);
    else
        equal = input_view.equals(compare_view);

    if (equal) {
        if (inverse)
            inverse_matched = true;
        else
            advance_string_position(state, input.view, ch1);
    }
}

ALWAYS_INLINE bool OpCode_Compare::compare_string(MatchInput const& input, MatchState& state, RegexStringView const& str, bool& had_zero_length_match)
{
    if (state.string_position + str.length() > input.view.length()) {
        if (str.is_empty()) {
            had_zero_length_match = true;
            return true;
        }
        return false;
    }

    if (str.length() == 0) {
        had_zero_length_match = true;
        return true;
    }

    auto subject = input.view.substring_view(state.string_position, str.length());
    bool equals;
    if (input.regex_options & AllFlags::Insensitive)
        equals = subject.equals_ignoring_case(str);
    else
        equals = subject.equals(str);

    if (equals)
        state.string_position += str.length();

    return equals;
}

ALWAYS_INLINE void OpCode_Compare::compare_character_class(MatchInput const& input, MatchState& state, CharClass character_class, u32 ch, bool inverse, bool& inverse_matched)
{
    switch (character_class) {
    case CharClass::Alnum:
        if (is_ascii_alphanumeric(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Alpha:
        if (is_ascii_alpha(ch))
            advance_string_position(state, input.view, ch);
        break;
    case CharClass::Blank:
        if (is_ascii_blank(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Cntrl:
        if (is_ascii_control(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Digit:
        if (is_ascii_digit(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Graph:
        if (is_ascii_graphical(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Lower:
        if (is_ascii_lower_alpha(ch) || ((input.regex_options & AllFlags::Insensitive) && is_ascii_upper_alpha(ch))) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Print:
        if (is_ascii_printable(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Punct:
        if (is_ascii_punctuation(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Space:
        if (is_ascii_space(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Upper:
        if (is_ascii_upper_alpha(ch) || ((input.regex_options & AllFlags::Insensitive) && is_ascii_lower_alpha(ch))) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Word:
        if (is_ascii_alphanumeric(ch) || ch == '_') {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    case CharClass::Xdigit:
        if (is_ascii_hex_digit(ch)) {
            if (inverse)
                inverse_matched = true;
            else
                advance_string_position(state, input.view, ch);
        }
        break;
    }
}

ALWAYS_INLINE void OpCode_Compare::compare_character_range(MatchInput const& input, MatchState& state, u32 from, u32 to, u32 ch, bool inverse, bool& inverse_matched)
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
            advance_string_position(state, input.view, ch);
    }
}

ALWAYS_INLINE void OpCode_Compare::compare_property(MatchInput const& input, MatchState& state, Unicode::Property property, bool inverse, bool& inverse_matched)
{
    if (state.string_position == input.view.length())
        return;

    u32 code_point = input.view[state.string_position_in_code_units];
    bool equal = Unicode::code_point_has_property(code_point, property);

    if (equal) {
        if (inverse)
            inverse_matched = true;
        else
            advance_string_position(state, input.view, code_point);
    }
}

ALWAYS_INLINE void OpCode_Compare::compare_general_category(MatchInput const& input, MatchState& state, Unicode::GeneralCategory general_category, bool inverse, bool& inverse_matched)
{
    if (state.string_position == input.view.length())
        return;

    u32 code_point = input.view[state.string_position_in_code_units];
    bool equal = Unicode::code_point_has_general_category(code_point, general_category);

    if (equal) {
        if (inverse)
            inverse_matched = true;
        else
            advance_string_position(state, input.view, code_point);
    }
}

ALWAYS_INLINE void OpCode_Compare::compare_script(MatchInput const& input, MatchState& state, Unicode::Script script, bool inverse, bool& inverse_matched)
{
    if (state.string_position == input.view.length())
        return;

    u32 code_point = input.view[state.string_position_in_code_units];
    bool equal = Unicode::code_point_has_script(code_point, script);

    if (equal) {
        if (inverse)
            inverse_matched = true;
        else
            advance_string_position(state, input.view, code_point);
    }
}

String const OpCode_Compare::arguments_string() const
{
    return String::formatted("argc={}, args={} ", arguments_count(), arguments_size());
}

Vector<String> const OpCode_Compare::variable_arguments_to_string(Optional<MatchInput> input) const
{
    Vector<String> result;

    size_t offset { state().instruction_position + 3 };
    RegexStringView view = ((input.has_value()) ? input.value().view : nullptr);

    for (size_t i = 0; i < arguments_count(); ++i) {
        auto compare_type = (CharacterCompareType)m_bytecode->at(offset++);
        result.empend(String::formatted("type={} [{}]", (size_t)compare_type, character_compare_type_name(compare_type)));

        auto string_start_offset = state().string_position_before_match;

        if (compare_type == CharacterCompareType::Char) {
            auto ch = m_bytecode->at(offset++);
            auto is_ascii = is_ascii_printable(ch);
            if (is_ascii)
                result.empend(String::formatted("value='{:c}'", static_cast<char>(ch)));
            else
                result.empend(String::formatted("value={:x}", ch));

            if (!view.is_null() && view.length() > string_start_offset) {
                if (is_ascii) {
                    result.empend(String::formatted(
                        "compare against: '{}'",
                        view.substring_view(string_start_offset, string_start_offset > view.length() ? 0 : 1).to_string()));
                } else {
                    auto str = view.substring_view(string_start_offset, string_start_offset > view.length() ? 0 : 1).to_string();
                    u8 buf[8] { 0 };
                    __builtin_memcpy(buf, str.characters(), min(str.length(), sizeof(buf)));
                    result.empend(String::formatted("compare against: {:x},{:x},{:x},{:x},{:x},{:x},{:x},{:x}",
                        buf[0], buf[1], buf[2], buf[3], buf[4], buf[5], buf[6], buf[7]));
                }
            }
        } else if (compare_type == CharacterCompareType::NamedReference) {
            auto ptr = (char const*)m_bytecode->at(offset++);
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
                    input.value().view.substring_view(string_start_offset, string_start_offset + length > view.length() ? 0 : length).to_string()));
        } else if (compare_type == CharacterCompareType::CharClass) {
            auto character_class = (CharClass)m_bytecode->at(offset++);
            result.empend(String::formatted("ch_class={} [{}]", (size_t)character_class, character_class_name(character_class)));
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: '{}'",
                    input.value().view.substring_view(string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = (CharRange)m_bytecode->at(offset++);
            result.empend(String::formatted("ch_range='{:c}'-'{:c}'", value.from, value.to));
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: '{}'",
                    input.value().view.substring_view(string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
        }
    }
    return result;
}
}
