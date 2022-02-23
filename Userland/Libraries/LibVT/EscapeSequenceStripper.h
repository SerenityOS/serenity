/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/UnicodeUtils.h>
#include <LibVT/EscapeSequenceParser.h>

namespace VT {
struct EscapeSequenceStripper : public EscapeSequenceExecutor {
    inline static Span<u8> strip_inplace(Span<u8> text_buffer)
    {
        EscapeSequenceStripper stripper(text_buffer);
        return stripper.m_raw_buffer.trim(stripper.m_write_pos);
    }

private:
    inline void emit_code_point(u32 code_point) override
    {
        (void)AK::UnicodeUtils::code_point_to_utf8(code_point, [&](char utf8) {
            rewrite_byte(utf8);
        });
    }
    inline void execute_control_code(u8 code) override
    {
        if (code == '\t' || code == '\n')
            rewrite_byte(code);
    }
    inline void execute_escape_sequence([[maybe_unused]] Intermediates intermediates, [[maybe_unused]] bool ignore, [[maybe_unused]] u8 last_byte) override
    {
    }
    inline void execute_csi_sequence([[maybe_unused]] Parameters parameters, [[maybe_unused]] Intermediates intermediates, [[maybe_unused]] bool ignore, [[maybe_unused]] u8 last_byte) override
    {
    }
    inline void execute_osc_sequence([[maybe_unused]] OscParameters parameters, [[maybe_unused]] u8 last_byte) override
    {
    }
    inline void dcs_hook([[maybe_unused]] Parameters parameters, [[maybe_unused]] Intermediates intermediates, [[maybe_unused]] bool ignore, [[maybe_unused]] u8 last_byte) override
    {
    }
    inline void receive_dcs_char([[maybe_unused]] u8 byte) override
    {
    }
    inline void execute_dcs_sequence() override {};

    EscapeSequenceStripper(Span<u8> raw_buffer)
        : m_raw_buffer { raw_buffer }
    {
        for (u8 c : m_raw_buffer) {
            m_parser.on_input(c);
            m_read_pos++;
        }
    }

    inline void rewrite_byte(u8 byte)
    {
        VERIFY(m_read_pos >= m_write_pos);
        m_raw_buffer[m_write_pos++] = byte;
    }

    Span<u8> m_raw_buffer;
    size_t m_read_pos { 0 };
    size_t m_write_pos { 0 };
    EscapeSequenceParser m_parser { *this };
};
}
