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
    [[nodiscard]] static ErrorOr<NonnullOwnPtr<KString>> try_create_uninitialized(size_t, char*&);
    [[nodiscard]] static NonnullOwnPtr<KString> must_create_uninitialized(size_t, char*&);
    [[nodiscard]] static ErrorOr<NonnullOwnPtr<KString>> try_create(StringView);
    [[nodiscard]] static NonnullOwnPtr<KString> must_create(StringView);

    [[nodiscard]] static ErrorOr<NonnullOwnPtr<KString>> vformatted(StringView fmtstr, AK::TypeErasedFormatParams&);

    template<typename... Parameters>
    [[nodiscard]] static ErrorOr<NonnullOwnPtr<KString>> formatted(CheckedFormatString<Parameters...>&& fmtstr, Parameters const&... parameters)
    {
        AK::VariadicFormatParams<AK::AllowDebugOnlyFormatters::No, Parameters...> variadic_format_parameters { parameters... };
        return vformatted(fmtstr.view(), variadic_format_parameters);
    }

    [[nodiscard]] static ErrorOr<NonnullOwnPtr<KString>> number(Arithmetic auto value)
    {
        return formatted("{}", value);
    }

    void operator delete(void*);

    ErrorOr<NonnullOwnPtr<KString>> try_clone() const;

    [[nodiscard]] bool is_empty() const { return m_length == 0; }
    [[nodiscard]] size_t length() const { return m_length; }
    [[nodiscard]] char const* characters() const { return m_characters; }
    [[nodiscard]] StringView view() const { return { characters(), length() }; }
    [[nodiscard]] ReadonlyBytes bytes() const { return { characters(), length() }; }

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
    ErrorOr<void> format(FormatBuilder& builder, Kernel::KString const& value)
    {
        return Formatter<StringView>::format(builder, value.view());
    }
};

template<>
struct Formatter<OwnPtr<Kernel::KString>> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, OwnPtr<Kernel::KString> const& value)
    {
        if (value)
            return Formatter<StringView>::format(builder, value->view());
        return Formatter<StringView>::format(builder, "[out of memory]"sv);
    }
};

template<>
struct Formatter<NonnullOwnPtr<Kernel::KString>> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, NonnullOwnPtr<Kernel::KString> const& value)
    {
        return Formatter<StringView>::format(builder, value->view());
    }
};

template<>
struct Traits<NonnullOwnPtr<Kernel::KString>> : public DefaultTraits<NonnullOwnPtr<Kernel::KString>> {
    using PeekType = Kernel::KString*;
    using ConstPeekType = Kernel::KString const*;
    static unsigned hash(NonnullOwnPtr<Kernel::KString> const& p) { return string_hash(p->characters(), p->length()); }
    static bool equals(NonnullOwnPtr<Kernel::KString> const& a, NonnullOwnPtr<Kernel::KString> const& b) { return a->view() == b->view(); }
    static bool equals(NonnullOwnPtr<Kernel::KString> const& a, StringView b) { return a->view() == b; }
};

template<>
struct Traits<OwnPtr<Kernel::KString>> : public DefaultTraits<OwnPtr<Kernel::KString>> {
    using PeekType = Kernel::KString*;
    using ConstPeekType = Kernel::KString const*;
    static unsigned hash(OwnPtr<Kernel::KString> const& p)
    {
        if (!p)
            return ptr_hash(nullptr);
        return string_hash(p->characters(), p->length());
    }
    static bool equals(OwnPtr<Kernel::KString> const& a, OwnPtr<Kernel::KString> const& b)
    {
        if (!a || !b)
            return a.ptr() == b.ptr();
        if (a == b)
            return true;

        return a->view() == b->view();
    }
    static bool equals(OwnPtr<Kernel::KString> const& a, StringView b)
    {
        if (!a)
            return b.is_null();
        return a->view() == b;
    }
};

namespace Detail {
template<>
inline constexpr bool IsHashCompatible<StringView, NonnullOwnPtr<Kernel::KString>> = true;
template<>
inline constexpr bool IsHashCompatible<StringView, OwnPtr<Kernel::KString>> = true;
}

}
