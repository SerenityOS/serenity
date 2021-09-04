/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DNSAnswer.h"
#include <AK/Stream.h>
#include <time.h>

namespace LookupServer {

DNSAnswer::DNSAnswer(DNSName const& name, DNSRecordType type, DNSRecordClass class_code, u32 ttl, String const& record_data, bool mdns_cache_flush)
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

void AK::Formatter<LookupServer::DNSRecordType>::format(AK::FormatBuilder& builder, LookupServer::DNSRecordType value)
{
    switch (value) {
    case LookupServer::DNSRecordType::A:
        builder.put_string("A");
        return;
    case LookupServer::DNSRecordType::NS:
        builder.put_string("NS");
        return;
    case LookupServer::DNSRecordType::CNAME:
        builder.put_string("CNAME");
        return;
    case LookupServer::DNSRecordType::SOA:
        builder.put_string("SOA");
        return;
    case LookupServer::DNSRecordType::PTR:
        builder.put_string("PTR");
        return;
    case LookupServer::DNSRecordType::MX:
        builder.put_string("MX");
        return;
    case LookupServer::DNSRecordType::TXT:
        builder.put_string("TXT");
        return;
    case LookupServer::DNSRecordType::AAAA:
        builder.put_string("AAAA");
        return;
    case LookupServer::DNSRecordType::SRV:
        builder.put_string("SRV");
        return;
    }

    builder.put_string("DNS record type ");
    builder.put_u64((u16)value);
}

void AK::Formatter<LookupServer::DNSRecordClass>::format(AK::FormatBuilder& builder, LookupServer::DNSRecordClass value)
{
    switch (value) {
    case LookupServer::DNSRecordClass::IN:
        builder.put_string("IN");
        return;
    }

    builder.put_string("DNS record class ");
    builder.put_u64((u16)value);
}
