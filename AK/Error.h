/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <AK/Try.h>

#if defined(__serenity__) && defined(KERNEL)
#    include <LibC/errno_numbers.h>
#else
#    include <errno.h>
#endif

namespace AK {

class Error {
public:
    static Error from_errno(int code) { return Error(code); }
    static Error from_syscall(StringView syscall_name, int rc) { return Error(syscall_name, rc); }
    static Error from_string_literal(StringView string_literal) { return Error(string_literal); }

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
    ErrorOr(T const& value)
        : m_value(value)
    {
    }

    ErrorOr(T&& value)
        : m_value(move(value))
    {
    }

    template<typename U>
    ALWAYS_INLINE ErrorOr(U&& value) requires(!IsSame<RemoveCVReference<U>, ErrorOr<T>>)
        : m_value(forward<U>(value))
    {
    }

#ifdef __serenity__
    ErrorOr(ErrnoCode code)
        : m_error(Error::from_errno(code))
    {
    }
#endif

    ErrorOr(ErrorType&& error)
        : m_error(move(error))
    {
    }

    ErrorOr(ErrorOr&& other) = default;
    ErrorOr(ErrorOr const& other) = default;
    ~ErrorOr() = default;

    ErrorOr& operator=(ErrorOr&& other) = default;
    ErrorOr& operator=(ErrorOr const& other) = default;

    T& value() { return m_value.value(); }
    T const& value() const { return m_value.value(); }
    ErrorType& error() { return m_error.value(); }
    ErrorType const& error() const { return m_error.value(); }

    bool is_error() const { return m_error.has_value(); }

    T release_value() { return m_value.release_value(); }
    ErrorType release_error() { return m_error.release_value(); }

    T release_value_but_fixme_should_propagate_errors() { return release_value(); }

private:
    Optional<T> m_value;
    Optional<ErrorType> m_error;
};

// Partial specialization for void value type
template<typename ErrorType>
class [[nodiscard]] ErrorOr<void, ErrorType> {
public:
    ErrorOr(ErrorType error)
        : m_error(move(error))
    {
    }

#ifdef __serenity__
    ErrorOr(ErrnoCode code)
        : m_error(Error::from_errno(code))
    {
    }
#endif

    ErrorOr() = default;
    ErrorOr(ErrorOr&& other) = default;
    ErrorOr(ErrorOr const& other) = default;
    ~ErrorOr() = default;

    ErrorOr& operator=(ErrorOr&& other) = default;
    ErrorOr& operator=(ErrorOr const& other) = default;

    ErrorType& error() { return m_error.value(); }
    bool is_error() const { return m_error.has_value(); }
    ErrorType release_error() { return m_error.release_value(); }
    void release_value() { }

private:
    Optional<ErrorType> m_error;
};

}

using AK::Error;
using AK::ErrorOr;
