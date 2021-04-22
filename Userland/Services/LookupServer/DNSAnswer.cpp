/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DNSAnswer.h"
#include <time.h>

namespace LookupServer {

DNSAnswer::DNSAnswer(const DNSName& name, u16 type, u16 class_code, u32 ttl, const String& record_data)
    : m_name(name)
    , m_type(type)
    , m_class_code(class_code)
    , m_ttl(ttl)
    , m_record_data(record_data)
{
    auto now = time(nullptr);
    m_expiration_time = now + m_ttl;
    if (m_expiration_time < now)
        m_expiration_time = 0;
}

bool DNSAnswer::has_expired() const
{
    return time(nullptr) >= m_expiration_time;
}

}
