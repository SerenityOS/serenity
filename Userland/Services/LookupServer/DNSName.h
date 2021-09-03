/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Forward.h>
#include <YAK/String.h>

namespace LookupServer {

class DNSName {
public:
    DNSName(const String&);

    static DNSName parse(const u8* data, size_t& offset, size_t max_offset, size_t recursion_level = 0);

    size_t serialized_size() const;
    const String& as_string() const { return m_name; }

    void randomize_case();

    bool operator==(const DNSName& other) const { return Traits::equals(*this, other); }

    class Traits : public YAK::Traits<DNSName> {
    public:
        static unsigned hash(const DNSName& name);
        static bool equals(const DNSName&, const DNSName&);
    };

private:
    String m_name;
};

OutputStream& operator<<(OutputStream& stream, const DNSName&);

}

template<>
struct YAK::Formatter<LookupServer::DNSName> : Formatter<StringView> {
    void format(FormatBuilder& builder, const LookupServer::DNSName& value)
    {
        return Formatter<StringView>::format(builder, value.as_string());
    }
};
