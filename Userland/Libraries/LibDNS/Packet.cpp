/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Packet.h"
#include "Name.h"
#include "PacketHeader.h"
#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <AK/StringBuilder.h>
#include <arpa/inet.h>
#include <stdlib.h>

namespace DNS {

void Packet::add_question(Question const& question)
{
    m_questions.empend(question);

    VERIFY(m_questions.size() <= UINT16_MAX);
}

void Packet::add_answer(Answer const& answer)
{
    m_answers.empend(answer);

    VERIFY(m_answers.size() <= UINT16_MAX);
}

ByteBuffer Packet::to_byte_buffer() const
{
    PacketHeader header;
    header.set_id(m_id);
    if (is_query())
        header.set_is_query();
    else
        header.set_is_response();
    header.set_authoritative_answer(m_authoritative_answer);
    // FIXME: What should this be?
    header.set_opcode(0);
    header.set_response_code(m_code);
    header.set_truncated(false); // hopefully...
    header.set_recursion_desired(m_recursion_desired);
    // FIXME: what should the be for requests?
    header.set_recursion_available(m_recursion_available);
    header.set_question_count(m_questions.size());
    header.set_answer_count(m_answers.size());

    DuplexMemoryStream stream;

    stream << ReadonlyBytes { &header, sizeof(header) };
    for (auto& question : m_questions) {
        stream << question.name();
        stream << htons((u16)question.record_type());
        stream << htons(question.raw_class_code());
    }
    for (auto& answer : m_answers) {
        stream << answer.name();
        stream << htons((u16)answer.type());
        stream << htons(answer.raw_class_code());
        stream << htonl(answer.ttl());
        if (answer.type() == RecordType::PTR) {
            Name name { answer.record_data() };
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
    DNSRecordWithoutName() = default;

    u16 type() const { return m_type; }
    u16 record_class() const { return m_class; }
    u32 ttl() const { return m_ttl; }
    u16 data_length() const { return m_data_length; }

    void* data() { return this + 1; }
    void const* data() const { return this + 1; }

private:
    NetworkOrdered<u16> m_type;
    NetworkOrdered<u16> m_class;
    NetworkOrdered<u32> m_ttl;
    NetworkOrdered<u16> m_data_length;
};

static_assert(sizeof(DNSRecordWithoutName) == 10);

Optional<Packet> Packet::from_raw_packet(u8 const* raw_data, size_t raw_size)
{
    if (raw_size < sizeof(PacketHeader)) {
        dbgln("DNS response not large enough ({} out of {}) to be a DNS packet.", raw_size, sizeof(PacketHeader));
        return {};
    }

    auto& header = *(PacketHeader const*)(raw_data);
    dbgln_if(LOOKUPSERVER_DEBUG, "Got packet (ID: {})", header.id());
    dbgln_if(LOOKUPSERVER_DEBUG, "  Question count: {}", header.question_count());
    dbgln_if(LOOKUPSERVER_DEBUG, "    Answer count: {}", header.answer_count());
    dbgln_if(LOOKUPSERVER_DEBUG, " Authority count: {}", header.authority_count());
    dbgln_if(LOOKUPSERVER_DEBUG, "Additional count: {}", header.additional_count());

    Packet packet;
    packet.m_id = header.id();
    packet.m_query_or_response = header.is_response();
    packet.m_code = header.response_code();

    // FIXME: Should we parse further in this case?
    if (packet.code() != Code::NOERROR)
        return packet;

    size_t offset = sizeof(PacketHeader);

    for (u16 i = 0; i < header.question_count(); i++) {
        auto name = Name::parse(raw_data, offset, raw_size);
        struct RawDNSAnswerQuestion {
            NetworkOrdered<u16> record_type;
            NetworkOrdered<u16> class_code;
        };
        auto& record_and_class = *(RawDNSAnswerQuestion const*)&raw_data[offset];
        u16 class_code = record_and_class.class_code & ~MDNS_WANTS_UNICAST_RESPONSE;
        bool mdns_wants_unicast_response = record_and_class.class_code & MDNS_WANTS_UNICAST_RESPONSE;
        packet.m_questions.empend(name, (RecordType)(u16)record_and_class.record_type, (RecordClass)class_code, mdns_wants_unicast_response);
        offset += 4;
        auto& question = packet.m_questions.last();
        dbgln_if(LOOKUPSERVER_DEBUG, "Question #{}: name=_{}_, type={}, class={}", i, question.name(), question.record_type(), question.class_code());
    }

    for (u16 i = 0; i < header.answer_count(); ++i) {
        auto name = Name::parse(raw_data, offset, raw_size);

        auto& record = *(DNSRecordWithoutName const*)(&raw_data[offset]);

        String data;

        offset += sizeof(DNSRecordWithoutName);

        switch ((RecordType)record.type()) {
        case RecordType::PTR: {
            size_t dummy_offset = offset;
            data = Name::parse(raw_data, dummy_offset, raw_size).as_string();
            break;
        }
        case RecordType::CNAME:
            // Fall through
        case RecordType::A:
            // Fall through
        case RecordType::TXT:
            // Fall through
        case RecordType::AAAA:
            // Fall through
        case RecordType::SRV:
            data = { record.data(), record.data_length() };
            break;
        default:
            // FIXME: Parse some other record types perhaps?
            dbgln("data=(unimplemented record type {})", (u16)record.type());
        }

        dbgln_if(LOOKUPSERVER_DEBUG, "Answer   #{}: name=_{}_, type={}, ttl={}, length={}, data=_{}_", i, name, record.type(), record.ttl(), record.data_length(), data);
        u16 class_code = record.record_class() & ~MDNS_CACHE_FLUSH;
        bool mdns_cache_flush = record.record_class() & MDNS_CACHE_FLUSH;
        packet.m_answers.empend(name, (RecordType)record.type(), (RecordClass)class_code, record.ttl(), data, mdns_cache_flush);
        offset += record.data_length();
    }

    return packet;
}

}
