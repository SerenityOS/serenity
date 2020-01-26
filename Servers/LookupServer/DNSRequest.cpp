#include "DNSRequest.h"
#include "DNSPacket.h"
#include <AK/BufferStream.h>
#include <arpa/inet.h>
#include <stdlib.h>

#define C_IN 1

DNSRequest::DNSRequest()
    : m_id(arc4random_uniform(UINT16_MAX))
{
}

void DNSRequest::add_question(const String& name, u16 record_type)
{
    ASSERT(m_questions.size() <= UINT16_MAX);
    m_questions.empend(name, record_type, C_IN);
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
