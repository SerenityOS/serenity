/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DNSName.h"
#include <AK/Types.h>

namespace LookupServer {

class DNSQuestion {
public:
    DNSQuestion(const DNSName& name, u16 record_type, u16 class_code)
        : m_name(name)
        , m_record_type(record_type)
        , m_class_code(class_code)
    {
    }

    u16 record_type() const { return m_record_type; }
    u16 class_code() const { return m_class_code; }
    const DNSName& name() const { return m_name; }

private:
    DNSName m_name;
    u16 m_record_type { 0 };
    u16 m_class_code { 0 };
};

}
