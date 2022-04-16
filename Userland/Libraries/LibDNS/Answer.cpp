/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Answer.h"
#include <AK/Stream.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <time.h>

namespace DNS {

Answer::Answer(Name const& name, RecordType type, RecordClass class_code, u32 ttl, String const& record_data, bool mdns_cache_flush)
    : m_name(name)
    , m_type(type)
    , m_class_code(class_code)
    , m_ttl(ttl)
    , m_record_data(record_data)
    , m_mdns_cache_flush(mdns_cache_flush)
{
    time(&m_received_time);
}

bool Answer::has_expired() const
{
    return time(nullptr) >= m_received_time + m_ttl;
}

unsigned Answer::hash() const
{
    auto hash = pair_int_hash(CaseInsensitiveStringTraits::hash(name().as_string()), (u32)type());
    hash = pair_int_hash(hash, pair_int_hash((u32)class_code(), ttl()));
    hash = pair_int_hash(hash, record_data().hash());
    hash = pair_int_hash(hash, (u32)mdns_cache_flush());
    return hash;
}

bool Answer::operator==(Answer const& other) const
{
    if (&other == this)
        return true;
    if (!Name::Traits::equals(name(), other.name()))
        return false;
    if (type() != other.type())
        return false;
    if (class_code() != other.class_code())
        return false;
    if (ttl() != other.ttl())
        return false;
    if (record_data() != other.record_data())
        return false;
    if (mdns_cache_flush() != other.mdns_cache_flush())
        return false;
    return true;
}

}

ErrorOr<void> AK::Formatter<DNS::RecordType>::format(AK::FormatBuilder& builder, DNS::RecordType value)
{
    switch (value) {
    case DNS::RecordType::A:
        return builder.put_string("A");
    case DNS::RecordType::NS:
        return builder.put_string("NS");
    case DNS::RecordType::CNAME:
        return builder.put_string("CNAME");
    case DNS::RecordType::SOA:
        return builder.put_string("SOA");
    case DNS::RecordType::PTR:
        return builder.put_string("PTR");
    case DNS::RecordType::MX:
        return builder.put_string("MX");
    case DNS::RecordType::TXT:
        return builder.put_string("TXT");
    case DNS::RecordType::AAAA:
        return builder.put_string("AAAA");
    case DNS::RecordType::SRV:
        return builder.put_string("SRV");
    }

    TRY(builder.put_string("DNS record type "));
    TRY(builder.put_u64((u16)value));
    return {};
}

ErrorOr<void> AK::Formatter<DNS::RecordClass>::format(AK::FormatBuilder& builder, DNS::RecordClass value)
{
    switch (value) {
    case DNS::RecordClass::IN:
        return builder.put_string("IN");
    }

    TRY(builder.put_string("DNS record class "));
    TRY(builder.put_u64((u16)value));
    return {};
}

namespace IPC {

bool encode(Encoder& encoder, DNS::Answer const& answer)
{
    encoder << answer.name().as_string() << (u16)answer.type() << (u16)answer.class_code() << answer.ttl() << answer.record_data() << answer.mdns_cache_flush();
    return true;
}

ErrorOr<void> decode(Decoder& decoder, DNS::Answer& answer)
{
    String name;
    TRY(decoder.decode(name));
    u16 record_type, class_code;
    TRY(decoder.decode(record_type));
    TRY(decoder.decode(class_code));
    u32 ttl;
    TRY(decoder.decode(ttl));
    String record_data;
    TRY(decoder.decode(record_data));
    bool cache_flush;
    TRY(decoder.decode(cache_flush));
    answer = { { name }, (DNS::RecordType)record_type, (DNS::RecordClass)class_code, ttl, record_data, cache_flush };
    return {};
}

}
