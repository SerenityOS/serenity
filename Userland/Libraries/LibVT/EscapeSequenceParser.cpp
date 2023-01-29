/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Types.h>
#include <LibVT/EscapeSequenceParser.h>
#include <LibVT/EscapeSequenceStateMachine.h>

namespace VT {
EscapeSequenceParser::EscapeSequenceParser(EscapeSequenceExecutor& executor)
    : m_executor(executor)
    , m_state_machine([this](auto action, auto byte) { perform_action(action, byte); })
{
}

Vector<EscapeSequenceParser::OscParameter> EscapeSequenceParser::osc_parameters() const
{
    VERIFY(m_osc_raw.size() >= m_osc_parameter_indexes.last());
    Vector<EscapeSequenceParser::OscParameter> params;
    size_t prev_idx = 0;
    for (auto end_idx : m_osc_parameter_indexes) {
        // If the parameter is empty, we take an out of bounds index as the beginning of the Span.
        // This should not be a problem as we won't dereference the 0-length Span that's created.
        // Using &m_osc_raw[prev_idx] to get the start pointer checks whether we're out of bounds,
        // so we would crash.
        params.append({ m_osc_raw.data() + prev_idx, end_idx - prev_idx });
        prev_idx = end_idx;
    }
    return params;
}

void EscapeSequenceParser::perform_action(EscapeSequenceStateMachine::Action action, u8 byte)
{
    auto advance_utf8 = [&](u8 byte) {
        u32 new_code_point = m_code_point;
        new_code_point <<= 6;
        new_code_point |= byte & 0x3f;
        return new_code_point;
    };

    switch (action) {
    case EscapeSequenceStateMachine::Action::_Ignore:
        break;
    case EscapeSequenceStateMachine::Action::Print:
        m_executor.emit_code_point((u32)byte);
        break;
    case EscapeSequenceStateMachine::Action::PrintUTF8:
        m_executor.emit_code_point(advance_utf8(byte));
        break;
    case EscapeSequenceStateMachine::Action::Execute:
        m_executor.execute_control_code(byte);
        break;
    case EscapeSequenceStateMachine::Action::Hook:
        if (m_param_vector.size() == MAX_PARAMETERS)
            m_ignoring = true;
        else
            m_param_vector.append(m_param);
        m_executor.dcs_hook(m_param_vector, intermediates(), m_ignoring, byte);
        break;
    case EscapeSequenceStateMachine::Action::Put:
        m_executor.receive_dcs_char(byte);
        break;
    case EscapeSequenceStateMachine::Action::BeginUTF8:
        if ((byte & 0xe0) == 0xc0) {
            m_code_point = byte & 0x1f;
        } else if ((byte & 0xf0) == 0xe0) {
            m_code_point = byte & 0x0f;
        } else if ((byte & 0xf8) == 0xf0) {
            m_code_point = byte & 0x07;
        } else {
            dbgln("Invalid character was parsed as UTF-8 initial byte {:02x}", byte);
            VERIFY_NOT_REACHED();
        }
        break;
    case EscapeSequenceStateMachine::Action::AdvanceUTF8:
        VERIFY((byte & 0xc0) == 0x80);
        m_code_point = advance_utf8(byte);
        break;
    case EscapeSequenceStateMachine::Action::FailUTF8:
        m_executor.emit_code_point(U'�');
        break;
    case EscapeSequenceStateMachine::Action::OscStart:
        m_osc_raw.clear();
        m_osc_parameter_indexes.clear();
        break;
    case EscapeSequenceStateMachine::Action::OscPut:
        if (byte == ';') {
            if (m_osc_parameter_indexes.size() == MAX_OSC_PARAMETERS) {
                dbgln("EscapeSequenceParser::perform_action: shenanigans! OSC sequence has too many parameters");
            } else {
                m_osc_parameter_indexes.append(m_osc_raw.size());
            }
        } else {
            m_osc_raw.append(byte);
        }
        break;
    case EscapeSequenceStateMachine::Action::OscEnd:
        if (m_osc_parameter_indexes.size() == MAX_OSC_PARAMETERS) {
            dbgln("EscapeSequenceParser::perform_action: shenanigans! OSC sequence has too many parameters");
        } else {
            m_osc_parameter_indexes.append(m_osc_raw.size());
        }
        m_executor.execute_osc_sequence(osc_parameters(), byte);
        break;
    case EscapeSequenceStateMachine::Action::Unhook:
        m_executor.execute_dcs_sequence();
        break;
    case EscapeSequenceStateMachine::Action::CsiDispatch:
        if (m_param_vector.size() > MAX_PARAMETERS) {
            dbgln("EscapeSequenceParser::perform_action: shenanigans! CSI sequence has too many parameters");
            m_ignoring = true;
        } else {
            m_param_vector.append(m_param);
        }

        m_executor.execute_csi_sequence(m_param_vector, intermediates(), m_ignoring, byte);
        break;

    case EscapeSequenceStateMachine::Action::EscDispatch:
        m_executor.execute_escape_sequence(intermediates(), m_ignoring, byte);
        break;
    case EscapeSequenceStateMachine::Action::Collect:
        if (m_intermediate_idx == MAX_INTERMEDIATES) {
            dbgln("EscapeSequenceParser::perform_action: shenanigans! escape sequence has too many intermediates");
            m_ignoring = true;
        } else {
            m_intermediates[m_intermediate_idx++] = byte;
        }
        break;
    case EscapeSequenceStateMachine::Action::Param:
        if (m_param_vector.size() == MAX_PARAMETERS) {
            dbgln("EscapeSequenceParser::perform_action: shenanigans! escape sequence has too many parameters");
            m_ignoring = true;
        } else {
            if (byte == ';') {
                m_param_vector.append(m_param);
                m_param = 0;
            } else if (byte == ':') {
                dbgln("EscapeSequenceParser::perform_action: subparameters are not yet implemented");
            } else {
                m_param *= 10;
                m_param += (byte - '0');
            }
        }
        break;
    case EscapeSequenceStateMachine::Action::Clear:
        m_intermediate_idx = 0;
        m_ignoring = false;

        m_param = 0;
        m_param_vector.clear_with_capacity();
        break;
    }
}
}
