/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Try.h>

#if defined(AK_OS_SERENITY) && defined(KERNEL)
#    include <errno_codes.h>
#else
#    include <errno.h>
#    include <string.h>
#endif

namespace AK {

class [[nodiscard]] Error {
public:
    ALWAYS_INLINE constexpr Error(Error&&) = default;
    ALWAYS_INLINE constexpr Error& operator=(Error&&) = default;

    constexpr static Error from_errno(int code)
    {
        VERIFY(code != 0);
        return Error(code);
    }

    // NOTE: For calling this method from within kernel code, we will simply print
    // the error message and return the errno code.
    // For calling this method from userspace programs, we will simply return from
    // the Error::from_string_view method!
    static Error from_string_view_or_print_error_and_return_errno(StringView string_literal, int code);

#ifndef KERNEL
    constexpr static Error from_syscall(StringView syscall_name, int rc)
    {
        return Error(syscall_name, rc);
    }
    constexpr static Error from_string_view(StringView string_literal) { return Error(string_literal); }

    // `Error::from_string_view(ByteString::formatted(...))` is a somewhat common mistake, which leads to a UAF situation.
    // If your string outlives this error and _isn't_ a temporary being passed to this function, explicitly call .view() on it to resolve to the StringView overload.
    template<OneOf<ByteString, DeprecatedFlyString, String, FlyString> T>
    constexpr static Error from_string_view(T) = delete;

#endif

    constexpr static Error copy(Error const& error)
    {
        return Error(error);
    }

#ifndef KERNEL
    // NOTE: Prefer `from_string_literal` when directly typing out an error message:
    //
    //     return Error::from_string_literal("Class: Some failure");
    //
    // If you need to return a static string based on a dynamic condition (like
    // picking an error from an array), then prefer `from_string_view` instead.
    template<size_t N>
    constexpr static Error from_string_literal(char const (&string_literal)[N])
    {
        return from_string_view(StringView { string_literal, N - 1 });
    }

    // Note: Don't call this from C++; it's here for Jakt interop (as the name suggests).
    template<SameAs<StringView> T>
    ALWAYS_INLINE constexpr static Error __jakt_from_string_literal(T string)
    {
        return from_string_view(string);
    }
#endif

    constexpr bool operator==(Error const& other) const
    {
#ifdef KERNEL
        return m_code == other.m_code;
#else
        return m_code == other.m_code && m_string_literal == other.m_string_literal && m_syscall == other.m_syscall;
#endif
    }

    constexpr int code() const { return m_code; }
    constexpr bool is_errno() const
    {
        return m_code != 0;
    }
#ifndef KERNEL
    constexpr bool is_syscall() const
    {
        return m_syscall;
    }
    constexpr StringView string_literal() const
    {
        return m_string_literal;
    }
#endif

protected:
    constexpr Error(int code)
        : m_code(code)
    {
    }

private:
#ifndef KERNEL
    constexpr Error(StringView string_literal)
        : m_string_literal(string_literal)
    {
    }

    constexpr Error(StringView syscall_name, int rc)
        : m_string_literal(syscall_name)
        , m_code(-rc)
        , m_syscall(true)
    {
    }
#endif

    constexpr Error(Error const&) = default;
    constexpr Error& operator=(Error const&) = default;

#ifndef KERNEL
    StringView m_string_literal;
#endif

    int m_code { 0 };

#ifndef KERNEL
    bool m_syscall { false };
#endif
};

template<typename T, typename E>
class [[nodiscard]] ErrorOr {
    template<typename U, typename F>
    friend class ErrorOr;

public:
    using ResultType = T;
    using ErrorType = E;

    ALWAYS_INLINE constexpr ErrorOr()
    requires(IsSame<T, Empty>)
        : m_value {}
        , m_is_error(false)
    {
    }

    ALWAYS_INLINE constexpr ErrorOr(ErrorOr&& other)
    requires(!Detail::IsTriviallyMoveConstructible<T> || !Detail::IsTriviallyMoveConstructible<E>)
        : m_is_error(other.m_is_error)
    {
        if (other.is_error())
            construct_at<E>(&m_error, other.release_error());
        else
            construct_at<T>(&m_value, other.release_value());
    }
    ALWAYS_INLINE constexpr ErrorOr(ErrorOr&& other) = default;

    ALWAYS_INLINE constexpr ErrorOr& operator=(ErrorOr&& other)
    requires(!Detail::IsTriviallyMoveConstructible<T> || !Detail::IsTriviallyMoveConstructible<E>)
    {
        if (this == &other)
            return *this;

        if (m_is_error)
            m_error.~ErrorType();
        else
            m_value.~T();

        if (other.is_error())
            construct_at<E>(&m_error, other.release_error());
        else
            construct_at<T>(&m_value, other.release_value());

        m_is_error = other.m_is_error;
        return *this;
    }
    ALWAYS_INLINE constexpr ErrorOr& operator=(ErrorOr&& other) = default;

    ErrorOr(ErrorOr const&) = delete;
    ErrorOr& operator=(ErrorOr const&) = delete;

    template<typename U>
    ALWAYS_INLINE constexpr ErrorOr(ErrorOr<U, ErrorType>&& value)
    requires(IsConvertible<U, T>)
        : m_is_error(value.is_error())
    {
        if (value.is_error())
            construct_at<E>(&m_error, value.release_error());
        else
            construct_at<T>(&m_value, value.release_value());
    }

    template<typename U>
    ALWAYS_INLINE constexpr ErrorOr(U&& value)
    requires(requires { T(declval<U>()); } && !IsSame<U, ErrorType>)
        : m_value(forward<U>(value))
        , m_is_error(false)
    {
    }
    template<typename U>
    ALWAYS_INLINE constexpr ErrorOr(U&& error)
    requires(requires { ErrorType(declval<RemoveCVReference<U>>()); } && !IsSame<U, T>)
        : m_error(forward<U>(error))
        , m_is_error(true)
    {
    }

#ifdef AK_OS_SERENITY
    ALWAYS_INLINE constexpr ErrorOr(ErrnoCode code)
        : m_error(Error::from_errno(code))
        , m_is_error(true)
    {
    }
#endif

    ALWAYS_INLINE constexpr T& value()
    {
        VERIFY(!is_error());
        return m_value;
    }
    ALWAYS_INLINE constexpr T const& value() const
    {
        VERIFY(!is_error());
        return m_value;
    }

    ALWAYS_INLINE constexpr ErrorType& error()
    {
        VERIFY(is_error());
        return m_error;
    }
    ALWAYS_INLINE constexpr ErrorType const& error() const
    {
        VERIFY(is_error());
        return m_error;
    }

    ALWAYS_INLINE constexpr bool is_error() const { return m_is_error; }

    ALWAYS_INLINE constexpr T&& release_value() { return move(value()); }
    ALWAYS_INLINE constexpr ErrorType&& release_error() { return move(error()); }

    ALWAYS_INLINE constexpr T release_value_but_fixme_should_propagate_errors()
    {
        VERIFY(!is_error());
        return release_value();
    }

    ~ErrorOr()
    requires(!Detail::IsDestructible<T> || !Detail::IsDestructible<ErrorType>)
    = delete;

    ALWAYS_INLINE constexpr ~ErrorOr()
    requires(IsTriviallyDestructible<T> && IsTriviallyDestructible<ErrorType>)
    = default;
    ALWAYS_INLINE constexpr ~ErrorOr()
    {
        if (m_is_error)
            m_error.~ErrorType();
        else
            m_value.~T();
    }

private:
    union {
        T m_value;
        ErrorType m_error;
    };
    bool m_is_error;
};

template<typename ErrorType>
class [[nodiscard]] ErrorOr<void, ErrorType> : public ErrorOr<Empty, ErrorType> {
public:
    using ResultType = void;
    using ErrorOr<Empty, ErrorType>::ErrorOr;
};

}

#if USING_AK_GLOBALLY
using AK::Error;
using AK::ErrorOr;
#endif
