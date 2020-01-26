/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

private:
    DNSResponse() {}

    u16 m_id { 0 };
    u8 m_code { 0 };
    Vector<DNSQuestion> m_questions;
    Vector<DNSAnswer> m_answers;
};
