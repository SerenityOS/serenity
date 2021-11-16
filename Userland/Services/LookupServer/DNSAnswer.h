/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSName.h"
#include <AK/Format.h>
#include <AK/String.h>
#include <AK/Types.h>

namespace LookupServer {

enum class DNSRecordType : u16 {
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

enum class DNSRecordClass : u16 {
    IN = 1
};

#define MDNS_CACHE_FLUSH 0x8000

class DNSAnswer {
public:
    DNSAnswer(const DNSName& name, DNSRecordType type, DNSRecordClass class_code, u32 ttl, const String& record_data, bool mdns_cache_flush);

    const DNSName& name() const { return m_name; }
    DNSRecordType type() const { return m_type; }
    DNSRecordClass class_code() const { return m_class_code; }
    u16 raw_class_code() const { return (u16)m_class_code | (m_mdns_cache_flush ? MDNS_CACHE_FLUSH : 0); }
    u32 ttl() const { return m_ttl; }
    time_t received_time() const { return m_received_time; }
    const String& record_data() const { return m_record_data; }
    bool mdns_cache_flush() const { return m_mdns_cache_flush; }

    bool has_expired() const;

private:
    DNSName m_name;
    DNSRecordType m_type { 0 };
    DNSRecordClass m_class_code { 0 };
    u32 m_ttl { 0 };
    time_t m_received_time { 0 };
    String m_record_data;
    bool m_mdns_cache_flush { false };
};

}
template<>
struct AK::Formatter<LookupServer::DNSRecordType> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(AK::FormatBuilder&, LookupServer::DNSRecordType);
};

template<>
struct AK::Formatter<LookupServer::DNSRecordClass> : StandardFormatter {
    Formatter() = default;
    explicit Formatter(StandardFormatter formatter)
        : StandardFormatter(formatter)
    {
    }

    ErrorOr<void> format(AK::FormatBuilder&, LookupServer::DNSRecordClass);
};
