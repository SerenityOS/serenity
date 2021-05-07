/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSName.h"
#include <AK/String.h>
#include <AK/Types.h>

namespace LookupServer {

#define MDNS_CACHE_FLUSH 0x8000

class DNSAnswer {
public:
    DNSAnswer(const DNSName& name, u16 type, u16 class_code, u32 ttl, const String& record_data, bool mdns_cache_flush);

    const DNSName& name() const { return m_name; }
    u16 type() const { return m_type; }
    u16 class_code() const { return m_class_code; }
    u16 raw_class_code() const { return m_class_code | (m_mdns_cache_flush ? MDNS_CACHE_FLUSH : 0); }
    u32 ttl() const { return m_ttl; }
    const String& record_data() const { return m_record_data; }
    bool mdns_cache_flush() const { return m_mdns_cache_flush; }

    bool has_expired() const;

private:
    DNSName m_name;
    u16 m_type { 0 };
    u16 m_class_code { 0 };
    u32 m_ttl { 0 };
    time_t m_expiration_time { 0 };
    String m_record_data;
    bool m_mdns_cache_flush { false };
};

}
