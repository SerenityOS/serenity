/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Answer.h"
#include "Question.h"
#include <AK/Optional.h>
#include <AK/Types.h>
#include <AK/Vector.h>

namespace DNS {

enum class ShouldRandomizeCase {
    No = 0,
    Yes
};

class Packet {
public:
    Packet() = default;

    static ErrorOr<Packet> from_raw_packet(ReadonlyBytes bytes);
    ErrorOr<ByteBuffer> to_byte_buffer() const;

    bool is_query() const { return !m_query_or_response; }
    bool is_response() const { return m_query_or_response; }
    bool is_authoritative_answer() const { return m_authoritative_answer; }
    bool recursion_desired() const { return m_recursion_desired; }
    bool recursion_available() const { return m_recursion_available; }
    void set_is_query() { m_query_or_response = false; }
    void set_is_response() { m_query_or_response = true; }
    void set_authoritative_answer(bool authoritative_answer) { m_authoritative_answer = authoritative_answer; }
    void set_recursion_desired(bool recursion_desired) { m_recursion_desired = recursion_desired; }
    void set_recursion_available(bool recursion_available) { m_recursion_available = recursion_available; }

    u16 id() const { return m_id; }
    void set_id(u16 id) { m_id = id; }

    Vector<Question> const& questions() const { return m_questions; }
    Vector<Answer> const& answers() const { return m_answers; }

    u16 question_count() const
    {
        VERIFY(m_questions.size() <= UINT16_MAX);
        return m_questions.size();
    }

    u16 answer_count() const
    {
        VERIFY(m_answers.size() <= UINT16_MAX);
        return m_answers.size();
    }

    void add_question(Question const&);
    void add_answer(Answer const&);

    enum class Code : u8 {
        NOERROR = 0,
        FORMERR = 1,
        SERVFAIL = 2,
        NXDOMAIN = 3,
        NOTIMP = 4,
        REFUSED = 5,
        YXDOMAIN = 6,
        XRRSET = 7,
        NOTAUTH = 8,
        NOTZONE = 9,
    };

    Code code() const { return (Code)m_code; }
    void set_code(Code code) { m_code = (u8)code; }

private:
    u16 m_id { 0 };
    u8 m_code { 0 };
    bool m_authoritative_answer { false };
    bool m_query_or_response { false };
    bool m_recursion_desired { true };
    bool m_recursion_available { true };
    Vector<Question> m_questions;
    Vector<Answer> m_answers;
};

}
