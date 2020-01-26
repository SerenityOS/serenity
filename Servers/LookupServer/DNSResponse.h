#pragma once

#include "DNSAnswer.h"
#include "DNSQuestion.h"
#include <AK/Types.h>
#include <AK/Vector.h>
#include <AK/Optional.h>

class DNSResponse {
public:
    static Optional<DNSResponse> from_raw_response(const u8*, size_t);

    u16 id() const { return m_id; }
    const Vector<DNSQuestion>& questions() const { return m_questions; }
    const Vector<DNSAnswer>& answers() const { return m_answers; }

    u16 question_count() const
    {
        ASSERT(m_questions.size() <= UINT16_MAX);
        return m_questions.size();
    }

    u16 answer_count() const
    {
        ASSERT(m_answers.size() <= UINT16_MAX);
        return m_answers.size();
    }

private:
    DNSResponse() {}

    u16 m_id { 0 };
    Vector<DNSQuestion> m_questions;
    Vector<DNSAnswer> m_answers;
};
