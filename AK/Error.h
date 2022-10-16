/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Try.h>
#include <AK/Variant.h>

#if defined(AK_OS_SERENITY) && defined(KERNEL)
#    include <LibC/errno_codes.h>
#else
#    include <errno.h>
#    include <string.h>
#endif

namespace AK {

class Error {
public:
    static Error from_errno(int code) { return Error(code); }
    static Error from_syscall(StringView syscall_name, int rc) { return Error(syscall_name, rc); }
    static Error from_string_view(StringView string_literal) { return Error(string_literal); }

    // NOTE: Prefer `from_string_literal` when directly typing out an error message:
    //
    //     return Error::from_string_literal("Class: Some failure");
    //
    // If you need to return a static string based on a dynamic condition (like
    // picking an error from an array), then prefer `from_string_view` instead.
    template<size_t N>
    ALWAYS_INLINE static Error from_string_literal(char const (&string_literal)[N])
    {
        return from_string_view(StringView { string_literal, N - 1 });
    }

    bool operator==(Error const& other) const
    {
        return m_code == other.m_code && m_string_literal == other.m_string_literal && m_syscall == other.m_syscall;
    }

    bool is_errno() const { return m_code != 0; }
    bool is_syscall() const { return m_syscall; }

    int code() const { return m_code; }
    StringView string_literal() const { return m_string_literal; }

protected:
    Error(int code)
        : m_code(code)
    {
    }

private:
    Error(StringView string_literal)
        : m_string_literal(string_literal)
    {
    }

    Error(StringView syscall_name, int rc)
        : m_code(-rc)
        , m_string_literal(syscall_name)
        , m_syscall(true)
    {
    }

    int m_code { 0 };
    StringView m_string_literal;
    bool m_syscall { false };
};

template<typename T, typename ErrorType>
class [[nodiscard]] ErrorOr {
public:
    ErrorOr()
    requires(IsSame<T, Empty>)
        : m_value_or_error(Empty {})
    {
    }

    template<typename U>
    ALWAYS_INLINE ErrorOr(U&& value)
    requires(!IsSame<RemoveCVReference<U>, ErrorOr<T, ErrorType>>)
        : m_value_or_error(forward<U>(value))
    {
    }

#ifdef AK_OS_SERENITY
    ErrorOr(ErrnoCode code)
        : m_value_or_error(Error::from_errno(code))
    {
    }
#endif

    T& value()
    {
        return m_value_or_error.template get<T>();
    }
    T const& value() const { return m_value_or_error.template get<T>(); }

    ErrorType& error() { return m_value_or_error.template get<ErrorType>(); }
    ErrorType const& error() const { return m_value_or_error.template get<ErrorType>(); }

    bool is_error() const { return m_value_or_error.template has<ErrorType>(); }

    T release_value() { return move(value()); }
    ErrorType release_error() { return move(error()); }

    T release_value_but_fixme_should_propagate_errors()
    {
        VERIFY(!is_error());
        return release_value();
    }

private:
    Variant<T, ErrorType> m_value_or_error;
};

template<typename ErrorType>
class [[nodiscard]] ErrorOr<void, ErrorType> : public ErrorOr<Empty, ErrorType> {
public:
    using ErrorOr<Empty, ErrorType>::ErrorOr;
};

}

#if USING_AK_GLOBALLY
using AK::Error;
using AK::ErrorOr;
#endif
