#include "DNSResponse.h"
#include "DNSPacket.h"
#include "DNSRequest.h"
#include <AK/IPv4Address.h>
#include <AK/StringBuilder.h>

static String parse_dns_name(const u8* data, size_t& offset, size_t max_offset, size_t recursion_level = 0);

class [[gnu::packed]] DNSRecordWithoutName
{
public:
    DNSRecordWithoutName() {}

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


Optional<DNSResponse> DNSResponse::from_raw_response(const u8* raw_data, size_t raw_size)
{
    if (raw_size < sizeof(DNSPacket)) {
        dbg() << "DNS response not large enough (" << raw_size << " out of " << sizeof(DNSPacket) << ") to be a DNS packet.";
        return {};
    }

    auto& response_header = *(const DNSPacket*)(raw_data);
    dbgprintf("Got response (ID: %u)\n", response_header.id());
    dbgprintf("  Question count: %u\n", response_header.question_count());
    dbgprintf("  Answer count: %u\n", response_header.answer_count());
    dbgprintf(" Authority count: %u\n", response_header.authority_count());
    dbgprintf("Additional count: %u\n", response_header.additional_count());

    DNSResponse response;
    response.m_id = response_header.id();

    size_t offset = sizeof(DNSPacket);

    for (u16 i = 0; i < response_header.question_count(); ++i) {
        auto name = parse_dns_name(raw_data, offset, raw_size);
        struct RawDNSAnswerQuestion {
            NetworkOrdered<u16> record_type;
            NetworkOrdered<u16> class_code;
        };
        auto& record_and_class = *(const RawDNSAnswerQuestion*)&raw_data[offset];
        response.m_questions.empend(name, record_and_class.record_type, record_and_class.class_code);
        auto& question = response.m_questions.last();
        offset += 4;
        dbg() << "Question #" << i << ": _" << question.name() << "_ type: " << question.record_type() << ", class: " << question.class_code();
    }

    for (u16 i = 0; i < response_header.answer_count(); ++i) {
        auto name = parse_dns_name(raw_data, offset, raw_size);

        auto& record = *(const DNSRecordWithoutName*)(&raw_data[offset]);

        String data;

        offset += sizeof(DNSRecordWithoutName);
        if (record.type() == T_PTR) {
            size_t dummy_offset = offset;
            data = parse_dns_name(raw_data, dummy_offset, raw_size);
        } else if (record.type() == T_A) {
            auto ipv4_address = IPv4Address((const u8*)record.data());
            data = ipv4_address.to_string();
        } else {
            // FIXME: Parse some other record types perhaps?
            dbg() << "  data=(unimplemented record type " << record.type() << ")";
        }
        dbg() << "Answer   #" << i << ": type=" << record.type() << ", ttl=" << record.ttl() << ", length=" << record.data_length() << ", data=_" << data << "_";
        response.m_answers.empend(name, record.type(), record.record_class(), record.ttl(), data);
        offset += record.data_length();
    }

    return response;
}

String parse_dns_name(const u8* data, size_t& offset, size_t max_offset, size_t recursion_level)
{
    if (recursion_level > 4)
        return {};
    Vector<char, 128> buf;
    while (offset < max_offset) {
        u8 ch = data[offset];
        if (ch == '\0') {
            ++offset;
            break;
        }
        if ((ch & 0xc0) == 0xc0) {
            if ((offset + 1) >= max_offset)
                return {};
            size_t dummy = (ch & 0x3f) << 8 | data[offset + 1];
            offset += 2;
            StringBuilder builder;
            builder.append(buf.data(), buf.size());
            auto okay = parse_dns_name(data, dummy, max_offset, recursion_level + 1);
            builder.append(okay);
            return builder.to_string();
        }
        for (size_t i = 0; i < ch; ++i)
            buf.append(data[offset + i + 1]);
        buf.append('.');
        offset += ch + 1;
    }
    return String::copy(buf);
}
