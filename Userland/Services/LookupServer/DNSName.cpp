/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "DNSName.h"
#include <AK/Random.h>
#include <AK/Vector.h>
#include <ctype.h>

namespace LookupServer {

DNSName::DNSName(const String& name)
{
    if (name.ends_with('.'))
        m_name = name.substring(0, name.length() - 1);
    else
        m_name = name;
}

DNSName DNSName::parse(const u8* data, size_t& offset, size_t max_offset, size_t recursion_level)
{
    if (recursion_level > 4)
        return DNSName({});

    StringBuilder builder;
    while (true) {
        if (offset >= max_offset)
            return DNSName({});
        u8 b = data[offset++];
        if (b == '\0') {
            // This terminates the name.
            return builder.to_string();
        } else if ((b & 0xc0) == 0xc0) {
            // The two bytes tell us the offset when to continue from.
            if (offset >= max_offset)
                return DNSName({});
            size_t dummy = (b & 0x3f) << 8 | data[offset++];
            auto rest_of_name = parse(data, dummy, max_offset, recursion_level + 1);
            builder.append(rest_of_name.as_string());
            return builder.to_string();
        } else {
            // This is the length of a part.
            if (offset + b >= max_offset)
                return DNSName({});
            builder.append((const char*)&data[offset], (size_t)b);
            builder.append('.');
            offset += b;
        }
    }
}

size_t DNSName::serialized_size() const
{
    if (m_name.is_empty())
        return 1;
    return m_name.length() + 2;
}

void DNSName::randomize_case()
{
    StringBuilder builder;
    for (char c : m_name) {
        // Randomize the 0x20 bit in every ASCII character.
        if (isalpha(c)) {
            if (get_random_uniform(2))
                c |= 0x20;
            else
                c &= ~0x20;
        }
        builder.append(c);
    }
    m_name = builder.to_string();
}

OutputStream& operator<<(OutputStream& stream, const DNSName& name)
{
    auto parts = name.as_string().split_view('.');
    for (auto& part : parts) {
        stream << (u8)part.length();
        stream << part.bytes();
    }
    stream << '\0';
    return stream;
}

unsigned DNSName::Traits::hash(const DNSName& name)
{
    return CaseInsensitiveStringTraits::hash(name.as_string());
}

bool DNSName::Traits::equals(const DNSName& a, const DNSName& b)
{
    return CaseInsensitiveStringTraits::equals(a.as_string(), b.as_string());
}

}
