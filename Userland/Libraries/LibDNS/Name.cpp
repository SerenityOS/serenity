/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Name.h"
#include <AK/Random.h>
#include <AK/Stream.h>
#include <AK/Vector.h>
#include <ctype.h>

namespace DNS {

Name::Name(DeprecatedString const& name)
{
    if (name.ends_with('.'))
        m_name = name.substring(0, name.length() - 1);
    else
        m_name = name;
}

Name Name::parse(u8 const* data, size_t& offset, size_t max_offset, size_t recursion_level)
{
    if (recursion_level > 4)
        return {};

    StringBuilder builder;
    while (true) {
        if (offset >= max_offset)
            return {};
        u8 b = data[offset++];
        if (b == '\0') {
            // This terminates the name.
            return builder.to_deprecated_string();
        } else if ((b & 0xc0) == 0xc0) {
            // The two bytes tell us the offset when to continue from.
            if (offset >= max_offset)
                return {};
            size_t dummy = (b & 0x3f) << 8 | data[offset++];
            auto rest_of_name = parse(data, dummy, max_offset, recursion_level + 1);
            builder.append(rest_of_name.as_string());
            return builder.to_deprecated_string();
        } else {
            // This is the length of a part.
            if (offset + b >= max_offset)
                return {};
            builder.append((char const*)&data[offset], (size_t)b);
            builder.append('.');
            offset += b;
        }
    }
}

size_t Name::serialized_size() const
{
    if (m_name.is_empty())
        return 1;
    return m_name.length() + 2;
}

void Name::randomize_case()
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
    m_name = builder.to_deprecated_string();
}

ErrorOr<void> Name::write_to_stream(Stream& stream) const
{
    auto parts = as_string().split_view('.');
    for (auto& part : parts) {
        TRY(stream.write_value<u8>(part.length()));
        TRY(stream.write_entire_buffer(part.bytes()));
    }
    TRY(stream.write_value('\0'));
    return {};
}

unsigned Name::Traits::hash(Name const& name)
{
    return CaseInsensitiveStringTraits::hash(name.as_string());
}

bool Name::Traits::equals(Name const& a, Name const& b)
{
    return CaseInsensitiveStringTraits::equals(a.as_string(), b.as_string());
}

}
