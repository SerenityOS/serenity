/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSName.h"
#include <AK/Types.h>

namespace LookupServer {

#define MDNS_WANTS_UNICAST_RESPONSE 0x8000

class DNSQuestion {
public:
    DNSQuestion(const DNSName& name, DNSRecordType record_type, DNSRecordClass class_code, bool mdns_wants_unicast_response)
        : m_name(name)
        , m_record_type(record_type)
        , m_class_code(class_code)
        , m_mdns_wants_unicast_response(mdns_wants_unicast_response)
    {
    }

    DNSRecordType record_type() const { return m_record_type; }
    DNSRecordClass class_code() const { return m_class_code; }
    u16 raw_class_code() const { return (u16)m_class_code | (m_mdns_wants_unicast_response ? MDNS_WANTS_UNICAST_RESPONSE : 0); }
    const DNSName& name() const { return m_name; }
    bool mdns_wants_unicast_response() const { return m_mdns_wants_unicast_response; }

private:
    DNSName m_name;
    DNSRecordType m_record_type { 0 };
    DNSRecordClass m_class_code { 0 };
    bool m_mdns_wants_unicast_response { false };
};

}
