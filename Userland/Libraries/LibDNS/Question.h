/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Answer.h"
#include "Name.h"
#include <AK/Types.h>

namespace DNS {

#define MDNS_WANTS_UNICAST_RESPONSE 0x8000

class Question {
public:
    Question(Name const& name, RecordType record_type, RecordClass class_code, bool mdns_wants_unicast_response)
        : m_name(name)
        , m_record_type(record_type)
        , m_class_code(class_code)
        , m_mdns_wants_unicast_response(mdns_wants_unicast_response)
    {
    }

    RecordType record_type() const { return m_record_type; }
    RecordClass class_code() const { return m_class_code; }
    u16 raw_class_code() const { return (u16)m_class_code | (m_mdns_wants_unicast_response ? MDNS_WANTS_UNICAST_RESPONSE : 0); }
    Name const& name() const { return m_name; }
    bool mdns_wants_unicast_response() const { return m_mdns_wants_unicast_response; }

private:
    Name m_name;
    RecordType m_record_type { 0 };
    RecordClass m_class_code { 0 };
    bool m_mdns_wants_unicast_response { false };
};

}
