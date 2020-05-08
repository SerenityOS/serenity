/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#pragma once

#include <AK/NetworkOrdered.h>
#include <AK/Types.h>

class [[gnu::packed]] DNSPacket
{
public:
    DNSPacket()
        : m_recursion_desired(false)
        , m_truncated(false)
        , m_authoritative_answer(false)
        , m_opcode(0)
        , m_query_or_response(false)
        , m_response_code(0)
        , m_checking_disabled(false)
        , m_authenticated_data(false)
        , m_zero(false)
        , m_recursion_available(false)
    {
    }

    u16 id() const { return m_id; }
    void set_id(u16 w) { m_id = w; }

    bool recursion_desired() const { return m_recursion_desired; }
    void set_recursion_desired(bool b) { m_recursion_desired = b; }

    bool is_truncated() const { return m_truncated; }
    void set_truncated(bool b) { m_truncated = b; }

    bool is_authoritative_answer() const { return m_authoritative_answer; }
    void set_authoritative_answer(bool b) { m_authoritative_answer = b; }

    u8 opcode() const { return m_opcode; }
    void set_opcode(u8 b) { m_opcode = b; }

    bool is_query() const { return !m_query_or_response; }
    bool is_response() const { return m_query_or_response; }
    void set_is_query() { m_query_or_response = false; }
    void set_is_response() { m_query_or_response = true; }

    u8 response_code() const { return m_response_code; }
    void set_response_code(u8 b) { m_response_code = b; }

    bool checking_disabled() const { return m_checking_disabled; }
    void set_checking_disabled(bool b) { m_checking_disabled = b; }

    bool is_authenticated_data() const { return m_authenticated_data; }
    void set_authenticated_data(bool b) { m_authenticated_data = b; }

    bool is_recursion_available() const { return m_recursion_available; }
    void set_recursion_available(bool b) { m_recursion_available = b; }

    u16 question_count() const { return m_question_count; }
    void set_question_count(u16 w) { m_question_count = w; }

    u16 answer_count() const { return m_answer_count; }
    void set_answer_count(u16 w) { m_answer_count = w; }

    u16 authority_count() const { return m_authority_count; }
    void set_authority_count(u16 w) { m_authority_count = w; }

    u16 additional_count() const { return m_additional_count; }
    void set_additional_count(u16 w) { m_additional_count = w; }

    void* payload() { return this + 1; }
    const void* payload() const { return this + 1; }

private:
    NetworkOrdered<u16> m_id;

    bool m_recursion_desired : 1;
    bool m_truncated : 1;
    bool m_authoritative_answer : 1;
    u8 m_opcode : 4;
    bool m_query_or_response : 1;
    u8 m_response_code : 4;
    bool m_checking_disabled : 1;
    bool m_authenticated_data : 1;
    bool m_zero : 1;
    bool m_recursion_available : 1;

    NetworkOrdered<u16> m_question_count;
    NetworkOrdered<u16> m_answer_count;
    NetworkOrdered<u16> m_authority_count;
    NetworkOrdered<u16> m_additional_count;
};

static_assert(sizeof(DNSPacket) == 12);
