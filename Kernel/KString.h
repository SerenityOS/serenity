/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <AK/OwnPtr.h>

namespace Kernel {

class KString {
    AK_MAKE_NONCOPYABLE(KString);
    AK_MAKE_NONMOVABLE(KString);

public:
    static OwnPtr<KString> try_create_uninitialized(size_t, char*&);
    static NonnullOwnPtr<KString> must_create_uninitialized(size_t, char*&);
    static OwnPtr<KString> try_create(StringView const&);
    static NonnullOwnPtr<KString> must_create(StringView const&);

    void operator delete(void*);

    OwnPtr<KString> try_clone() const;

    bool is_empty() const { return m_length == 0; }
    size_t length() const { return m_length; }
    char const* characters() const { return m_characters; }
    StringView view() const { return { characters(), length() }; }

private:
    explicit KString(size_t length)
        : m_length(length)
    {
    }

    size_t m_length { 0 };
    char m_characters[0];
};

}

namespace AK {

template<>
struct Formatter<Kernel::KString> : Formatter<StringView> {
    void format(FormatBuilder& builder, Kernel::KString const& value)
    {
        Formatter<StringView>::format(builder, value.characters());
    }
};

}
