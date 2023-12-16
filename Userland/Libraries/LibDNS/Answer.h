/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Name.h"
#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/Traits.h>
#include <AK/Types.h>
#include <LibIPC/Forward.h>

namespace DNS {

enum class RecordType : u16 {
    A = 1,
    NS = 2,
    CNAME = 5,
    SOA = 6,
    PTR = 12,
    MX = 15,
    TXT = 16,
    AAAA = 28,
    SRV = 33,
};

enum class RecordClass : u16 {
    IN = 1
};

#define MDNS_CACHE_FLUSH 0x8000

class Answer {
public:
    Answer() = default;
    Answer(Name const& name, RecordType type, RecordClass class_code, u32 ttl, ByteString const& record_data, bool mdns_cache_flush);

    Name const& name() const { return m_name; }
    RecordType type() const { return m_type; }
    RecordClass class_code() const { return m_class_code; }
    u16 raw_class_code() const { return (u16)m_class_code | (m_mdns_cache_flush ? MDNS_CACHE_FLUSH : 0); }
    u32 ttl() const { return m_ttl; }
    time_t received_time() const { return m_received_time; }
    ByteString const& record_data() const { return m_record_data; }
    bool mdns_cache_flush() const { return m_mdns_cache_flush; }

    bool has_expired() const;

    unsigned hash() const;
    bool operator==(Answer const&) const;

private:
    Name m_name;
    RecordType m_type { 0 };
    RecordClass m_class_code { 0 };
    u32 m_ttl { 0 };
    time_t m_received_time { 0 };
    ByteString m_record_data;
    bool m_mdns_cache_flush { false };
};

}

template<>
struct AK::Traits<DNS::Answer> : public DefaultTraits<DNS::Answer> {
    static constexpr bool is_trivial() { return false; }
    static unsigned hash(DNS::Answer a) { return a.hash(); }
};

template<>
struct AK::Formatter<DNS::RecordType> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(AK::FormatBuilder&, DNS::RecordType);
};

template<>
struct AK::Formatter<DNS::RecordClass> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(AK::FormatBuilder&, DNS::RecordClass);
};

namespace IPC {

template<>
ErrorOr<void> encode(Encoder&, DNS::Answer const&);

template<>
ErrorOr<DNS::Answer> decode(Decoder&);

}
