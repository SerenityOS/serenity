/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DNSAnswer.h"
#include <AK/Stream.h>
#include <time.h>

namespace LookupServer {

DNSAnswer::DNSAnswer(const DNSName& name, DNSRecordType type, DNSRecordClass class_code, u32 ttl, const String& record_data, bool mdns_cache_flush)
    : m_name(name)
    , m_type(type)
    , m_class_code(class_code)
    , m_ttl(ttl)
    , m_record_data(record_data)
    , m_mdns_cache_flush(mdns_cache_flush)
{
    time(&m_received_time);
}

bool DNSAnswer::has_expired() const
{
    return time(nullptr) >= m_received_time + m_ttl;
}

}

ErrorOr<void> AK::Formatter<LookupServer::DNSRecordType>::format(AK::FormatBuilder& builder, LookupServer::DNSRecordType value)
{
    switch (value) {
    case LookupServer::DNSRecordType::A:
        return builder.put_string("A");
    case LookupServer::DNSRecordType::NS:
        return builder.put_string("NS");
    case LookupServer::DNSRecordType::CNAME:
        return builder.put_string("CNAME");
    case LookupServer::DNSRecordType::SOA:
        return builder.put_string("SOA");
    case LookupServer::DNSRecordType::PTR:
        return builder.put_string("PTR");
    case LookupServer::DNSRecordType::MX:
        return builder.put_string("MX");
    case LookupServer::DNSRecordType::TXT:
        return builder.put_string("TXT");
    case LookupServer::DNSRecordType::AAAA:
        return builder.put_string("AAAA");
    case LookupServer::DNSRecordType::SRV:
        return builder.put_string("SRV");
    }

    TRY(builder.put_string("DNS record type "));
    TRY(builder.put_u64((u16)value));
    return {};
}

ErrorOr<void> AK::Formatter<LookupServer::DNSRecordClass>::format(AK::FormatBuilder& builder, LookupServer::DNSRecordClass value)
{
    switch (value) {
    case LookupServer::DNSRecordClass::IN:
        return builder.put_string("IN");
    }

    TRY(builder.put_string("DNS record class "));
    TRY(builder.put_u64((u16)value));
    return {};
}
