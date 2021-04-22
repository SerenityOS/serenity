/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSAnswer.h"
#include "DNSQuestion.h"
#include <AK/Optional.h>
#include <AK/Types.h>
#include <AK/Vector.h>

#define T_A 1
#define T_NS 2
#define T_CNAME 5
#define T_SOA 6
#define T_PTR 12
#define T_MX 15

#define C_IN 1

namespace LookupServer {

enum class ShouldRandomizeCase {
    No = 0,
    Yes
};

class DNSPacket {
public:
    DNSPacket() { }

    static Optional<DNSPacket> from_raw_packet(const u8*, size_t);
    ByteBuffer to_byte_buffer() const;

    bool is_query() const { return !m_query_or_response; }
    bool is_response() const { return m_query_or_response; }
    void set_is_query() { m_query_or_response = false; }
    void set_is_response() { m_query_or_response = true; }

    u16 id() const { return m_id; }
    void set_id(u16 id) { m_id = id; }

    const Vector<DNSQuestion>& questions() const { return m_questions; }
    const Vector<DNSAnswer>& answers() const { return m_answers; }

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

    void add_question(const DNSQuestion&);
    void add_answer(const DNSAnswer&);

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
    bool m_query_or_response { false };
    Vector<DNSQuestion> m_questions;
    Vector<DNSAnswer> m_answers;
};

}
