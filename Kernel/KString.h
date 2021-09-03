/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Format.h>
#include <YAK/OwnPtr.h>

namespace Kernel {

class KString {
    YAK_MAKE_NONCOPYABLE(KString);
    YAK_MAKE_NONMOVABLE(KString);

public:
    [[nodiscard]] static OwnPtr<KString> try_create_uninitialized(size_t, char*&);
    [[nodiscard]] static NonnullOwnPtr<KString> must_create_uninitialized(size_t, char*&);
    [[nodiscard]] static OwnPtr<KString> try_create(StringView const&);
    [[nodiscard]] static NonnullOwnPtr<KString> must_create(StringView const&);

    void operator delete(void*);

    [[nodiscard]] OwnPtr<KString> try_clone() const;

    [[nodiscard]] bool is_empty() const { return m_length == 0; }
    [[nodiscard]] size_t length() const { return m_length; }
    [[nodiscard]] char const* characters() const { return m_characters; }
    [[nodiscard]] StringView view() const { return { characters(), length() }; }

private:
    explicit KString(size_t length)
        : m_length(length)
    {
    }

    size_t m_length { 0 };
    char m_characters[0];
};

}

namespace YAK {

template<>
struct Formatter<Kernel::KString> : Formatter<StringView> {
    void format(FormatBuilder& builder, Kernel::KString const& value)
    {
        Formatter<StringView>::format(builder, value.view());
    }
};

template<>
struct Formatter<OwnPtr<Kernel::KString>> : Formatter<StringView> {
    void format(FormatBuilder& builder, OwnPtr<Kernel::KString> const& value)
    {
        if (value)
            Formatter<StringView>::format(builder, value->view());
        else
            Formatter<StringView>::format(builder, "[out of memory]"sv);
    }
};

}
