/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Debug.h>
#include <AK/Format.h>
#include <AK/Platform.h>
#include <AK/Span.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibVT/EscapeSequenceStateMachine.h>

namespace VT {
class EscapeSequenceExecutor {
public:
    virtual ~EscapeSequenceExecutor() = default;

    using Parameters = ReadonlySpan<unsigned>;
    using Intermediates = ReadonlyBytes;
    using OscParameter = ReadonlyBytes;
    using OscParameters = ReadonlySpan<OscParameter>;

    virtual void emit_code_point(u32) = 0;
    virtual void execute_control_code(u8) = 0;
    virtual void execute_escape_sequence(Intermediates intermediates, bool ignore, u8 last_byte) = 0;
    virtual void execute_csi_sequence(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte) = 0;
    virtual void execute_osc_sequence(OscParameters parameters, u8 last_byte) = 0;
    virtual void dcs_hook(Parameters parameters, Intermediates intermediates, bool ignore, u8 last_byte) = 0;
    virtual void receive_dcs_char(u8 byte) = 0;
    virtual void execute_dcs_sequence() = 0;
};

class EscapeSequenceParser {
public:
    explicit EscapeSequenceParser(EscapeSequenceExecutor&);
    ~EscapeSequenceParser() = default;

    ALWAYS_INLINE void on_input(u8 byte)
    {
        dbgln_if(ESCAPE_SEQUENCE_DEBUG, "on_input {:02x}", byte);
        m_state_machine.advance(byte);
    }

private:
    static constexpr size_t MAX_INTERMEDIATES = 2;
    static constexpr size_t MAX_PARAMETERS = 16;
    static constexpr size_t MAX_OSC_PARAMETERS = 16;

    using Intermediates = EscapeSequenceExecutor::Intermediates;
    using OscParameter = EscapeSequenceExecutor::OscParameter;

    void perform_action(EscapeSequenceStateMachine::Action, u8);

    EscapeSequenceExecutor& m_executor;
    EscapeSequenceStateMachine m_state_machine;

    u32 m_code_point { 0 };

    u8 m_intermediates[MAX_INTERMEDIATES];
    u8 m_intermediate_idx { 0 };

    Intermediates intermediates() const { return { m_intermediates, m_intermediate_idx }; }
    Vector<OscParameter> osc_parameters() const;

    Vector<unsigned, 4> m_param_vector;
    unsigned m_param { 0 };

    Vector<size_t> m_osc_parameter_indexes;
    Vector<u8, 16> m_osc_raw;

    bool m_ignoring { false };
};

}
