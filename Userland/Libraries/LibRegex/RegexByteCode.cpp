/*
 * Copyright (c) 2020, Emanuel Sprung <emanuel.sprung@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "RegexByteCode.h"
#include "AK/StringBuilder.h"
#include "RegexDebug.h"
#include <AK/BinarySearch.h>
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

char const* opcode_id_name(OpCodeId opcode)
{
    switch (opcode) {
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

static void advance_string_position(MatchState& state, RegexStringView view, Optional<u32> code_point = {})
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

static void advance_string_position(MatchState& state, RegexStringView, RegexStringView advance_by)
{
    state.string_position += advance_by.length();
    state.string_position_in_code_units += advance_by.length_in_code_units();
}

static void reverse_string_position(MatchState& state, RegexStringView view, size_t amount)
{
    VERIFY(state.string_position >= amount);
    state.string_position -= amount;

    if (view.unicode())
        state.string_position_in_code_units = view.code_unit_offset_of(state.string_position);
    else
        state.string_position_in_code_units -= amount;
}

static void save_string_position(MatchInput const& input, MatchState const& state)
{
    input.saved_positions.append(state.string_position);
    input.saved_forks_since_last_save.append(state.forks_since_last_save);
    input.saved_code_unit_positions.append(state.string_position_in_code_units);
}

static bool restore_string_position(MatchInput const& input, MatchState& state)
{
    if (input.saved_positions.is_empty())
        return false;

    state.string_position = input.saved_positions.take_last();
    state.string_position_in_code_units = input.saved_code_unit_positions.take_last();
    state.forks_since_last_save = input.saved_forks_since_last_save.take_last();
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
#define __ENUMERATE_OPCODE(OpCode)              \
    case OpCodeId::OpCode:                      \
        s_opcodes[i] = make<OpCode_##OpCode>(); \
        break;

            ENUMERATE_OPCODES

#undef __ENUMERATE_OPCODE
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
    if (auto opcode_ptr = static_cast<DisjointChunks<ByteCodeValueType> const&>(*this).find(state.instruction_position))
        opcode_id = (OpCodeId)*opcode_ptr;
    else
        opcode_id = OpCodeId::Exit;

    auto& opcode = get_opcode_by_id(opcode_id);
    opcode.set_state(state);
    return opcode;
}

ALWAYS_INLINE ExecutionResult OpCode_Exit::execute(MatchInput const& input, MatchState& state) const
{
    if (state.string_position > input.view.length() || state.instruction_position >= m_bytecode->size())
        return ExecutionResult::Succeeded;

    return ExecutionResult::Failed;
}

ALWAYS_INLINE ExecutionResult OpCode_Save::execute(MatchInput const& input, MatchState& state) const
{
    save_string_position(input, state);
    state.forks_since_last_save = 0;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Restore::execute(MatchInput const& input, MatchState& state) const
{
    if (!restore_string_position(input, state))
        return ExecutionResult::Failed;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_GoBack::execute(MatchInput const& input, MatchState& state) const
{
    if (count() > state.string_position)
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    reverse_string_position(state, input.view, count());
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_FailForks::execute(MatchInput const& input, MatchState& state) const
{
    input.fail_counter += state.forks_since_last_save;
    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_Jump::execute(MatchInput const&, MatchState& state) const
{
    state.instruction_position += offset();
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkJump::execute(MatchInput const&, MatchState& state) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    state.forks_since_last_save++;
    return ExecutionResult::Fork_PrioHigh;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkReplaceJump::execute(MatchInput const& input, MatchState& state) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    input.fork_to_replace = state.instruction_position;
    state.forks_since_last_save++;
    return ExecutionResult::Fork_PrioHigh;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkStay::execute(MatchInput const&, MatchState& state) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    state.forks_since_last_save++;
    return ExecutionResult::Fork_PrioLow;
}

ALWAYS_INLINE ExecutionResult OpCode_ForkReplaceStay::execute(MatchInput const& input, MatchState& state) const
{
    state.fork_at_position = state.instruction_position + size() + offset();
    input.fork_to_replace = state.instruction_position;
    state.forks_since_last_save++;
    return ExecutionResult::Fork_PrioLow;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckBegin::execute(MatchInput const& input, MatchState& state) const
{
    auto is_at_line_boundary = [&] {
        if (state.string_position == 0)
            return true;

        if (input.regex_options.has_flag_set(AllFlags::Multiline) && input.regex_options.has_flag_set(AllFlags::Internal_ConsiderNewline)) {
            auto input_view = input.view.substring_view(state.string_position - 1, 1)[0];
            return input_view == '\n';
        }

        return false;
    }();
    if (is_at_line_boundary && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    if ((is_at_line_boundary && !(input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (!is_at_line_boundary && (input.regex_options & AllFlags::MatchNotBeginOfLine))
        || (is_at_line_boundary && (input.regex_options & AllFlags::Global)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_CheckBoundary::execute(MatchInput const& input, MatchState& state) const
{
    auto isword = [](auto ch) { return is_ascii_alphanumeric(ch) || ch == '_'; };
    auto is_word_boundary = [&] {
        if (state.string_position == input.view.length()) {
            return (state.string_position > 0 && isword(input.view[state.string_position_in_code_units - 1]));
        }

        if (state.string_position == 0) {
            return (isword(input.view[0]));
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

ALWAYS_INLINE ExecutionResult OpCode_CheckEnd::execute(MatchInput const& input, MatchState& state) const
{
    auto is_at_line_boundary = [&] {
        if (state.string_position == input.view.length())
            return true;

        if (input.regex_options.has_flag_set(AllFlags::Multiline) && input.regex_options.has_flag_set(AllFlags::Internal_ConsiderNewline)) {
            auto input_view = input.view.substring_view(state.string_position, 1)[0];
            return input_view == '\n';
        }

        return false;
    }();
    if (is_at_line_boundary && (input.regex_options & AllFlags::MatchNotEndOfLine))
        return ExecutionResult::Failed_ExecuteLowPrioForks;

    if ((is_at_line_boundary && !(input.regex_options & AllFlags::MatchNotEndOfLine))
        || (!is_at_line_boundary && (input.regex_options & AllFlags::MatchNotEndOfLine || input.regex_options & AllFlags::MatchNotBeginOfLine)))
        return ExecutionResult::Continue;

    return ExecutionResult::Failed_ExecuteLowPrioForks;
}

ALWAYS_INLINE ExecutionResult OpCode_ClearCaptureGroup::execute(MatchInput const& input, MatchState& state) const
{
    if (input.match_index < state.capture_group_matches.size()) {
        auto& group = state.capture_group_matches[input.match_index];
        auto group_id = id();
        if (group_id >= group.size())
            group.resize(group_id + 1);

        group[group_id].reset();
    }
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_SaveLeftCaptureGroup::execute(MatchInput const& input, MatchState& state) const
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

ALWAYS_INLINE ExecutionResult OpCode_SaveRightCaptureGroup::execute(MatchInput const& input, MatchState& state) const
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

ALWAYS_INLINE ExecutionResult OpCode_SaveRightNamedCaptureGroup::execute(MatchInput const& input, MatchState& state) const
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
        match = { view.to_string(), name(), input.line, start_position, input.global_offset + start_position }; // create a copy of the original string
    } else {
        match = { view, name(), input.line, start_position, input.global_offset + start_position }; // take view to original string
    }

    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Compare::execute(MatchInput const& input, MatchState& state) const
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

            auto input_view = input.view.substring_view(state.string_position, 1)[0];
            if (input_view != '\n' || (input.regex_options.has_flag_set(AllFlags::SingleLine) && input.regex_options.has_flag_set(AllFlags::Internal_ConsiderNewline)))
                advance_string_position(state, input.view, input_view);

        } else if (compare_type == CharacterCompareType::String) {
            VERIFY(!current_inversion_state());

            auto const& length = m_bytecode->at(offset++);

            // We want to compare a string that is definitely longer than the available string
            if (input.view.length() < state.string_position + length)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            Optional<String> str;
            Vector<u16, 1> utf16;
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

        } else if (compare_type == CharacterCompareType::LookupTable) {
            if (input.view.length() <= state.string_position)
                return ExecutionResult::Failed_ExecuteLowPrioForks;

            auto count = m_bytecode->at(offset++);
            auto range_data = m_bytecode->spans().slice(offset, count);
            offset += count;

            auto ch = input.view.substring_view(state.string_position, 1)[0];

            auto const* matching_range = binary_search(range_data, ch, nullptr, [insensitive = input.regex_options & AllFlags::Insensitive](auto needle, CharRange range) {
                auto from = range.from;
                auto to = range.to;
                if (insensitive) {
                    from = to_ascii_lowercase(from);
                    to = to_ascii_lowercase(to);
                    needle = to_ascii_lowercase(needle);
                }
                if (needle > range.to)
                    return 1;
                if (needle < range.from)
                    return -1;
                return 0;
            });

            if (matching_range) {
                if (current_inversion_state())
                    inverse_matched = true;
                else
                    advance_string_position(state, input.view, ch);
            }

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

        } else if (compare_type == CharacterCompareType::Property) {
            auto property = static_cast<Unicode::Property>(m_bytecode->at(offset++));
            compare_property(input, state, property, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::GeneralCategory) {
            auto general_category = static_cast<Unicode::GeneralCategory>(m_bytecode->at(offset++));
            compare_general_category(input, state, general_category, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::Script) {
            auto script = static_cast<Unicode::Script>(m_bytecode->at(offset++));
            compare_script(input, state, script, current_inversion_state(), inverse_matched);

        } else if (compare_type == CharacterCompareType::ScriptExtension) {
            auto script = static_cast<Unicode::Script>(m_bytecode->at(offset++));
            compare_script_extension(input, state, script, current_inversion_state(), inverse_matched);

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

    auto input_view = input.view.substring_view(state.string_position, 1)[0];
    bool equal;
    if (input.regex_options & AllFlags::Insensitive)
        equal = to_ascii_lowercase(input_view) == to_ascii_lowercase(ch1); // FIXME: Implement case-insensitive matching for non-ascii characters
    else
        equal = input_view == ch1;

    if (equal) {
        if (inverse)
            inverse_matched = true;
        else
            advance_string_position(state, input.view, ch1);
    }
}

ALWAYS_INLINE bool OpCode_Compare::compare_string(MatchInput const& input, MatchState& state, RegexStringView str, bool& had_zero_length_match)
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
        advance_string_position(state, input.view, str);

    return equals;
}

ALWAYS_INLINE void OpCode_Compare::compare_character_class(MatchInput const& input, MatchState& state, CharClass character_class, u32 ch, bool inverse, bool& inverse_matched)
{
    auto is_space_or_line_terminator = [](u32 code_point) {
        static auto space_separator = Unicode::general_category_from_string("Space_Separator"sv);
        if (!space_separator.has_value())
            return is_ascii_space(code_point);

        if ((code_point == 0x0a) || (code_point == 0x0d) || (code_point == 0x2028) || (code_point == 0x2029))
            return true;
        if ((code_point == 0x09) || (code_point == 0x0b) || (code_point == 0x0c) || (code_point == 0xfeff))
            return true;
        return Unicode::code_point_has_general_category(code_point, *space_separator);
    };

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
        if (is_space_or_line_terminator(ch)) {
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

ALWAYS_INLINE void OpCode_Compare::compare_script_extension(MatchInput const& input, MatchState& state, Unicode::Script script, bool inverse, bool& inverse_matched)
{
    if (state.string_position == input.view.length())
        return;

    u32 code_point = input.view[state.string_position_in_code_units];
    bool equal = Unicode::code_point_has_script_extension(code_point, script);

    if (equal) {
        if (inverse)
            inverse_matched = true;
        else
            advance_string_position(state, input.view, code_point);
    }
}

String OpCode_Compare::arguments_string() const
{
    return String::formatted("argc={}, args={} ", arguments_count(), arguments_size());
}

Vector<CompareTypeAndValuePair> OpCode_Compare::flat_compares() const
{
    Vector<CompareTypeAndValuePair> result;

    size_t offset { state().instruction_position + 3 };

    for (size_t i = 0; i < arguments_count(); ++i) {
        auto compare_type = (CharacterCompareType)m_bytecode->at(offset++);

        if (compare_type == CharacterCompareType::Char) {
            auto ch = m_bytecode->at(offset++);
            result.append({ compare_type, ch });
        } else if (compare_type == CharacterCompareType::Reference) {
            auto ref = m_bytecode->at(offset++);
            result.append({ compare_type, ref });
        } else if (compare_type == CharacterCompareType::String) {
            auto& length = m_bytecode->at(offset++);
            if (length > 0)
                result.append({ compare_type, m_bytecode->at(offset) });
            StringBuilder str_builder;
            offset += length;
        } else if (compare_type == CharacterCompareType::CharClass) {
            auto character_class = m_bytecode->at(offset++);
            result.append({ compare_type, character_class });
        } else if (compare_type == CharacterCompareType::CharRange) {
            auto value = m_bytecode->at(offset++);
            result.append({ compare_type, value });
        } else if (compare_type == CharacterCompareType::LookupTable) {
            auto count = m_bytecode->at(offset++);
            for (size_t i = 0; i < count; ++i)
                result.append({ CharacterCompareType::CharRange, m_bytecode->at(offset++) });
        } else {
            result.append({ compare_type, 0 });
        }
    }
    return result;
}

Vector<String> OpCode_Compare::variable_arguments_to_string(Optional<MatchInput> input) const
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
        } else if (compare_type == CharacterCompareType::Reference) {
            auto ref = m_bytecode->at(offset++);
            result.empend(String::formatted("number={}", ref));
            if (input.has_value()) {
                if (state().capture_group_matches.size() > input->match_index) {
                    auto& match = state().capture_group_matches[input->match_index];
                    if (match.size() > ref) {
                        auto& group = match[ref];
                        result.empend(String::formatted("left={}", group.left_column));
                        result.empend(String::formatted("right={}", group.left_column + group.view.length_in_code_units()));
                        result.empend(String::formatted("contents='{}'", group.view));
                    } else {
                        result.empend(String::formatted("(invalid ref, max={})", match.size() - 1));
                    }
                } else {
                    result.empend(String::formatted("(invalid index {}, max={})", input->match_index, state().capture_group_matches.size() - 1));
                }
            }
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
            result.empend(String::formatted("ch_range={:x}-{:x}", value.from, value.to));
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: '{}'",
                    input.value().view.substring_view(string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
        } else if (compare_type == CharacterCompareType::LookupTable) {
            auto count = m_bytecode->at(offset++);
            for (size_t j = 0; j < count; ++j) {
                auto range = (CharRange)m_bytecode->at(offset++);
                result.append(String::formatted("{:x}-{:x}", range.from, range.to));
            }
            if (!view.is_null() && view.length() > state().string_position)
                result.empend(String::formatted(
                    "compare against: '{}'",
                    input.value().view.substring_view(string_start_offset, state().string_position > view.length() ? 0 : 1).to_string()));
        }
    }
    return result;
}

ALWAYS_INLINE ExecutionResult OpCode_Repeat::execute(MatchInput const&, MatchState& state) const
{
    VERIFY(count() > 0);

    if (id() >= state.repetition_marks.size())
        state.repetition_marks.resize(id() + 1);
    auto& repetition_mark = state.repetition_marks.at(id());

    if (repetition_mark == count() - 1) {
        repetition_mark = 0;
    } else {
        state.instruction_position -= offset() + size();
        ++repetition_mark;
    }

    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_ResetRepeat::execute(MatchInput const&, MatchState& state) const
{
    if (id() >= state.repetition_marks.size())
        state.repetition_marks.resize(id() + 1);

    state.repetition_marks.at(id()) = 0;
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_Checkpoint::execute(MatchInput const& input, MatchState& state) const
{
    input.checkpoints.set(state.instruction_position, state.string_position);
    return ExecutionResult::Continue;
}

ALWAYS_INLINE ExecutionResult OpCode_JumpNonEmpty::execute(MatchInput const& input, MatchState& state) const
{
    auto current_position = state.string_position;
    auto checkpoint_ip = state.instruction_position + size() + checkpoint();
    if (input.checkpoints.get(checkpoint_ip).value_or(current_position) != current_position) {
        auto form = this->form();

        if (form == OpCodeId::Jump) {
            state.instruction_position += offset();
            return ExecutionResult::Continue;
        }

        state.fork_at_position = state.instruction_position + size() + offset();

        if (form == OpCodeId::ForkJump)
            return ExecutionResult::Fork_PrioHigh;

        if (form == OpCodeId::ForkStay)
            return ExecutionResult::Fork_PrioLow;

        if (form == OpCodeId::ForkReplaceStay) {
            input.fork_to_replace = state.instruction_position;
            return ExecutionResult::Fork_PrioLow;
        }

        if (form == OpCodeId::ForkReplaceJump) {
            input.fork_to_replace = state.instruction_position;
            return ExecutionResult::Fork_PrioHigh;
        }
    }

    return ExecutionResult::Continue;
}

}
