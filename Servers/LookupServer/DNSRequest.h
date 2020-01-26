#pragma once

#include "DNSQuestion.h"
#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <AK/Types.h>

#define T_A 1
#define T_NS 2
#define T_CNAME 5
#define T_SOA 6
#define T_PTR 12
#define T_MX 15

enum class ShouldRandomizeCase {
    No = 0,
    Yes
};

class DNSRequest {
public:
    DNSRequest();

    void add_question(const String& name, u16 record_type, ShouldRandomizeCase);

    const Vector<DNSQuestion>& questions() const { return m_questions; }

    u16 question_count() const
    {
        ASSERT(m_questions.size() < UINT16_MAX);
        return m_questions.size();
    }

    u16 id() const { return m_id; }
    ByteBuffer to_byte_buffer() const;

private:
    u16 m_id { 0 };
    Vector<DNSQuestion> m_questions;
};
