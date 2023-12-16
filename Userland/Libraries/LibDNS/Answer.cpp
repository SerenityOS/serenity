/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Answer.h"
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>
#include <time.h>

namespace DNS {

Answer::Answer(Name const& name, RecordType type, RecordClass class_code, u32 ttl, ByteString const& record_data, bool mdns_cache_flush)
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
    return time(nullptr) >= static_cast<time_t>(m_received_time + m_ttl);
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
        return builder.put_string("A"sv);
    case DNS::RecordType::NS:
        return builder.put_string("NS"sv);
    case DNS::RecordType::CNAME:
        return builder.put_string("CNAME"sv);
    case DNS::RecordType::SOA:
        return builder.put_string("SOA"sv);
    case DNS::RecordType::PTR:
        return builder.put_string("PTR"sv);
    case DNS::RecordType::MX:
        return builder.put_string("MX"sv);
    case DNS::RecordType::TXT:
        return builder.put_string("TXT"sv);
    case DNS::RecordType::AAAA:
        return builder.put_string("AAAA"sv);
    case DNS::RecordType::SRV:
        return builder.put_string("SRV"sv);
    }

    TRY(builder.put_string("DNS record type "sv));
    TRY(builder.put_u64((u16)value));
    return {};
}

ErrorOr<void> AK::Formatter<DNS::RecordClass>::format(AK::FormatBuilder& builder, DNS::RecordClass value)
{
    switch (value) {
    case DNS::RecordClass::IN:
        return builder.put_string("IN"sv);
    }

    TRY(builder.put_string("DNS record class "sv));
    TRY(builder.put_u64((u16)value));
    return {};
}

namespace IPC {

template<>
ErrorOr<void> encode(Encoder& encoder, DNS::Answer const& answer)
{
    TRY(encoder.encode(answer.name().as_string()));
    TRY(encoder.encode(static_cast<u16>(answer.type())));
    TRY(encoder.encode(static_cast<u16>(answer.class_code())));
    TRY(encoder.encode(answer.ttl()));
    TRY(encoder.encode(answer.record_data()));
    TRY(encoder.encode(answer.mdns_cache_flush()));

    return {};
}

template<>
ErrorOr<DNS::Answer> decode(Decoder& decoder)
{
    auto name = TRY(decoder.decode<ByteString>());
    auto record_type = TRY(decoder.decode<DNS::RecordType>());
    auto class_code = TRY(decoder.decode<DNS::RecordClass>());
    auto ttl = TRY(decoder.decode<u32>());
    auto record_data = TRY(decoder.decode<ByteString>());
    auto cache_flush = TRY(decoder.decode<bool>());

    return DNS::Answer { name, record_type, class_code, ttl, record_data, cache_flush };
}

}
