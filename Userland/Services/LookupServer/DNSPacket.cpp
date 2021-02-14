/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
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

#include "DNSPacket.h"
#include "DNSName.h"
#include "DNSPacketHeader.h"
#include <AK/IPv4Address.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <arpa/inet.h>
#include <ctype.h>
#include <stdlib.h>

namespace LookupServer {

void DNSPacket::add_question(const String& name, u16 record_type, ShouldRandomizeCase should_randomize_case)
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

    m_questions.empend(builder.to_string(), record_type, (u16)C_IN);
}

ByteBuffer DNSPacket::to_byte_buffer() const
{
    DNSPacketHeader header;
    header.set_id(m_id);
    if (is_query())
        header.set_is_query();
    else
        header.set_is_response();
    // FIXME: What should this be?
    header.set_opcode(0);
    header.set_truncated(false); // hopefully...
    header.set_recursion_desired(true);
    // FIXME: what should the be for requests?
    header.set_recursion_available(true);
    header.set_question_count(m_questions.size());
    header.set_answer_count(m_answers.size());

    DuplexMemoryStream stream;

    stream << ReadonlyBytes { &header, sizeof(header) };
    for (auto& question : m_questions) {
        stream << question.name();
        stream << htons(question.record_type());
        stream << htons(question.class_code());
    }
    for (auto& answer : m_answers) {
        stream << answer.name();
        stream << htons(answer.type());
        stream << htons(answer.class_code());
        stream << htonl(answer.ttl());
        if (answer.type() == T_PTR) {
            DNSName name { answer.record_data() };
            stream << htons(name.serialized_size());
            stream << name;
        } else {
            stream << htons(answer.record_data().length());
            stream << answer.record_data().bytes();
        }
    }

    return stream.copy_into_contiguous_buffer();
}

class [[gnu::packed]] DNSRecordWithoutName {
public:
    DNSRecordWithoutName() { }

    u16 type() const { return m_type; }
    u16 record_class() const { return m_class; }
    u32 ttl() const { return m_ttl; }
    u16 data_length() const { return m_data_length; }

    void* data() { return this + 1; }
    const void* data() const { return this + 1; }

private:
    NetworkOrdered<u16> m_type;
    NetworkOrdered<u16> m_class;
    NetworkOrdered<u32> m_ttl;
    NetworkOrdered<u16> m_data_length;
};

static_assert(sizeof(DNSRecordWithoutName) == 10);

Optional<DNSPacket> DNSPacket::from_raw_packet(const u8* raw_data, size_t raw_size)
{
    if (raw_size < sizeof(DNSPacketHeader)) {
        dbgln("DNS response not large enough ({} out of {}) to be a DNS packet.", raw_size, sizeof(DNSPacketHeader));
        return {};
    }

    auto& header = *(const DNSPacketHeader*)(raw_data);
#ifdef LOOKUPSERVER_DEBUG
    dbgln("Got packet (ID: {})", header.id());
    dbgln("  Question count: {}", header.question_count());
    dbgln("    Answer count: {}", header.answer_count());
    dbgln(" Authority count: {}", header.authority_count());
    dbgln("Additional count: {}", header.additional_count());
#endif

    DNSPacket packet;
    packet.m_id = header.id();
    packet.m_query_or_response = header.is_response();
    packet.m_code = header.response_code();

    // FIXME: Should we parse further in this case?
    if (packet.code() != Code::NOERROR)
        return packet;

    size_t offset = sizeof(DNSPacketHeader);

    for (u16 i = 0; i < header.question_count(); i++) {
        auto name = DNSName::parse(raw_data, offset, raw_size);
        struct RawDNSAnswerQuestion {
            NetworkOrdered<u16> record_type;
            NetworkOrdered<u16> class_code;
        };
        auto& record_and_class = *(const RawDNSAnswerQuestion*)&raw_data[offset];
        packet.m_questions.empend(name, record_and_class.record_type, record_and_class.class_code);
        offset += 4;
#ifdef LOOKUPSERVER_DEBUG
        auto& question = packet.m_questions.last();
        dbgln("Question #{}: name=_{}_, type={}, class={}", i, question.name(), question.record_type(), question.class_code());
#endif
    }

    for (u16 i = 0; i < header.answer_count(); ++i) {
        auto name = DNSName::parse(raw_data, offset, raw_size);

        auto& record = *(const DNSRecordWithoutName*)(&raw_data[offset]);

        String data;

        offset += sizeof(DNSRecordWithoutName);

        if (record.type() == T_PTR) {
            size_t dummy_offset = offset;
            data = DNSName::parse(raw_data, dummy_offset, raw_size).as_string();
        } else if (record.type() == T_A) {
            data = { record.data(), record.data_length() };
        } else {
            // FIXME: Parse some other record types perhaps?
            dbgln("data=(unimplemented record type {})", record.type());
        }
#ifdef LOOKUPSERVER_DEBUG
        dbgln("Answer   #{}: name=_{}_, type={}, ttl={}, length={}, data=_{}_", i, name, record.type(), record.ttl(), record.data_length(), data);
#endif
        packet.m_answers.empend(name, record.type(), record.record_class(), record.ttl(), data);
        offset += record.data_length();
    }

    return packet;
}

}
