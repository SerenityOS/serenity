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

#include "DNSRequest.h"
#include "DNSPacket.h"
#include <AK/BufferStream.h>
#include <AK/StringBuilder.h>
#include <arpa/inet.h>
#include <ctype.h>
#include <stdlib.h>

#define C_IN 1

DNSRequest::DNSRequest()
    : m_id(arc4random_uniform(UINT16_MAX))
{
}

void DNSRequest::add_question(const String& name, u16 record_type, ShouldRandomizeCase should_randomize_case)
{
    ASSERT(m_questions.size() <= UINT16_MAX);

    if (name.is_empty())
        return;

    StringBuilder builder;
    for (size_t i = 0; i < name.length(); ++i) {
        u8 ch = name[i];
        if (should_randomize_case == ShouldRandomizeCase::Yes) {
            // Randomize the 0x20 bit in every ASCII character.
            if (isalpha(ch)) {
                if (arc4random_uniform(2))
                    ch |= 0x20;
                else
                    ch &= ~0x20;
            }
        }
        builder.append(ch);
    }

    if (name[name.length() - 1] != '.')
        builder.append('.');

    m_questions.empend(builder.to_string(), record_type, C_IN);
}

ByteBuffer DNSRequest::to_byte_buffer() const
{
    DNSPacket request_header;
    request_header.set_id(m_id);
    request_header.set_is_query();
    request_header.set_opcode(0);
    request_header.set_truncated(false);
    request_header.set_recursion_desired(true);
    request_header.set_question_count(m_questions.size());

    auto buffer = ByteBuffer::create_uninitialized(m_questions.size() * 4096);
    BufferStream stream(buffer);

    stream << ByteBuffer::wrap(&request_header, sizeof(request_header));

    for (auto& question : m_questions) {
        auto parts = question.name().split('.');
        for (auto& part : parts) {
            stream << (u8)part.length();
            stream << part;
        }
        stream << '\0';
        stream << htons(question.record_type());
        stream << htons(question.class_code());
    }
    stream.snip();

    return buffer;
}
