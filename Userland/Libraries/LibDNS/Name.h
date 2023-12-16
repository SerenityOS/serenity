/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Forward.h>

namespace DNS {

class Name {
public:
    Name() = default;
    Name(ByteString const&);

    static ErrorOr<Name> parse(ReadonlyBytes data, size_t& offset, size_t recursion_level = 0);

    size_t serialized_size() const;
    ByteString const& as_string() const { return m_name; }
    ErrorOr<void> write_to_stream(Stream&) const;

    void randomize_case();

    bool operator==(Name const& other) const { return Traits::equals(*this, other); }

    class Traits : public AK::Traits<Name> {
    public:
        static unsigned hash(Name const& name);
        static bool equals(Name const&, Name const&);
    };

private:
    ByteString m_name;
};

}

template<>
struct AK::Formatter<DNS::Name> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, DNS::Name const& value)
    {
        return Formatter<StringView>::format(builder, value.as_string());
    }
};
