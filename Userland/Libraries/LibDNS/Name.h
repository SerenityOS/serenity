/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/String.h>

namespace DNS {

class Name {
public:
    Name() = default;
    Name(String const&);

    static Name parse(u8 const* data, size_t& offset, size_t max_offset, size_t recursion_level = 0);

    size_t serialized_size() const;
    String const& as_string() const { return m_name; }

    void randomize_case();

    bool operator==(Name const& other) const { return Traits::equals(*this, other); }

    class Traits : public AK::Traits<Name> {
    public:
        static unsigned hash(Name const& name);
        static bool equals(Name const&, Name const&);
    };

private:
    String m_name;
};

OutputStream& operator<<(OutputStream& stream, Name const&);

}

template<>
struct AK::Formatter<DNS::Name> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, DNS::Name const& value)
    {
        return Formatter<StringView>::format(builder, value.as_string());
    }
};
