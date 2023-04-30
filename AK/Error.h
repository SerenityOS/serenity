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
#    include <errno_codes.h>
#else
#    include <errno.h>
#    include <string.h>
#endif

// TODO: We don't have a way to use something like dynamic_cast with -fno-rtti.
//       This limits the ability of the Kernel and the DynamicLoader to check whether a payload is of a specific type.
#if !defined(KERNEL) && !defined(_DYNAMIC_LOADER)
#    define AK_ERROR_SUPPORTS_DYNAMIC
#endif

namespace AK {

class ErrorPayload {
public:
    virtual ErrorOr<void> format(Formatter<FormatString>& formatter, FormatBuilder& builder) const = 0;

#ifdef AK_ERROR_SUPPORTS_DYNAMIC
    virtual bool operator==(ErrorPayload const&) const = 0;
#endif

    virtual ~ErrorPayload() = default;
};

class [[nodiscard]] Error {
public:
    // Arbitrarily chosen size, which should fit a vtable pointer and a standard uintptr_t.
    // Bump as necessary if required for larger error payloads, but double-check the performance impact.
    static constexpr size_t MAXIMUM_ERROR_PAYLOAD_SIZE = sizeof(void*) + sizeof(uintptr_t);

    using RawErrorPayload = Array<u8, MAXIMUM_ERROR_PAYLOAD_SIZE>;

    ALWAYS_INLINE Error(Error&&) = default;
    ALWAYS_INLINE Error& operator=(Error&&) = default;

    static Error from_errno(int code)
    {
        VERIFY(code != 0);
        return Error(code);
    }

    template<DerivedFrom<ErrorPayload> T>
    [[nodiscard]] static Error from_error_payload(T error_payload)
    {
        return Error(move(error_payload));
    }

    // NOTE: For calling this method from within kernel code, we will simply print
    // the error message and return the errno code.
    // For calling this method from userspace programs, we will simply return from
    // the Error::from_string_view method!
    static Error from_string_view_or_print_error_and_return_errno(StringView string_literal, int code);

#ifndef KERNEL
    static Error from_syscall(StringView syscall_name, int rc)
    {
        return Error(syscall_name, rc);
    }
    static Error from_string_view(StringView string_literal) { return Error(string_literal); }

    template<OneOf<DeprecatedString, DeprecatedFlyString, String, FlyString> T>
    static Error from_string_view(T)
    {
        // `Error::from_string_view(DeprecatedString::formatted(...))` is a somewhat common mistake, which leads to a UAF situation.
        // If your string outlives this error and _isn't_ a temporary being passed to this function, explicitly call .view() on it to resolve to the StringView overload.
        static_assert(DependentFalse<T>, "Error::from_string_view(String) is almost always a use-after-free");
        VERIFY_NOT_REACHED();
    }

#endif

    static Error copy(Error const& error)
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
    ALWAYS_INLINE static Error from_string_literal(char const (&string_literal)[N])
    {
        return from_string_view(StringView { string_literal, N - 1 });
    }

    // Note: Don't call this from C++; it's here for Jakt interop (as the name suggests).
    template<SameAs<StringView> T>
    ALWAYS_INLINE static Error __jakt_from_string_literal(T string)
    {
        return from_string_view(string);
    }
#endif

    bool operator==(Error const& other) const
    {
#ifdef KERNEL
        return m_payload == other.m_payload;
#else
        return m_payload == other.m_payload && m_string_literal == other.m_string_literal && m_syscall == other.m_syscall;
#endif
    }

    int code() const { return m_payload.get<int>(); }
    bool is_errno() const
    {
        return m_payload.has<int>();
    }

    Optional<ErrorPayload const&> generic_error_payload() const
    {
        if (!m_payload.has<RawErrorPayload>())
            return {};

        return *bit_cast<ErrorPayload const*>(m_payload.get<RawErrorPayload>().data());
    }

#ifdef AK_ERROR_SUPPORTS_DYNAMIC
    template<typename T>
    Optional<T const&> error_payload() const
    {
        if (!m_payload.has<RawErrorPayload>())
            return {};

        auto const* error_payload = bit_cast<ErrorPayload const*>(m_payload.get<RawErrorPayload>().data());
        auto const* typed_error_payload = dynamic_cast<T const*>(error_payload);

        if (!typed_error_payload)
            return {};

        return *typed_error_payload;
    }
#endif

#ifndef KERNEL
    bool is_syscall() const
    {
        return m_syscall;
    }
    StringView string_literal() const
    {
        return m_string_literal;
    }
#endif

protected:
    Error(int code)
        : m_payload(code)
    {
    }

    template<DerivedFrom<ErrorPayload> T>
    Error(T error_payload)
        : m_payload(RawErrorPayload {})
    {
        static_assert(sizeof(T) <= MAXIMUM_ERROR_PAYLOAD_SIZE);

        // We made sure that the type we are given inherits from ErrorPayload and that it doesn't exceed the size of our storage.
        // Therefore, we should be able to store it in the Variant without risking slicing off bits.
        new (m_payload.get<RawErrorPayload>().data()) T(move(error_payload));
    }

private:
#ifndef KERNEL
    Error(StringView string_literal)
        : m_string_literal(string_literal)
    {
    }

    Error(StringView syscall_name, int rc)
        : m_string_literal(syscall_name)
        , m_payload(-rc)
        , m_syscall(true)
    {
    }
#endif

    Error(Error const&) = default;
    Error& operator=(Error const&) = default;

#ifndef KERNEL
    StringView m_string_literal;
#endif

    Variant<Empty, int, RawErrorPayload> m_payload { Empty {} };

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

    ErrorOr()
    requires(IsSame<T, Empty>)
        : m_value_or_error(Empty {})
    {
    }

    ALWAYS_INLINE ErrorOr(ErrorOr&&) = default;
    ALWAYS_INLINE ErrorOr& operator=(ErrorOr&&) = default;

    ErrorOr(ErrorOr const&) = delete;
    ErrorOr& operator=(ErrorOr const&) = delete;

    template<typename U>
    ALWAYS_INLINE ErrorOr(ErrorOr<U, ErrorType>&& value)
    requires(IsConvertible<U, T>)
        : m_value_or_error(value.m_value_or_error.visit([](U& v) { return Variant<T, ErrorType>(move(v)); }, [](ErrorType& error) { return Variant<T, ErrorType>(move(error)); }))
    {
    }

    template<typename U>
    ALWAYS_INLINE ErrorOr(U&& value)
    requires(
        requires { T(declval<U>()); } || requires { ErrorType(declval<RemoveCVReference<U>>()); })
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
    using ResultType = void;
    using ErrorOr<Empty, ErrorType>::ErrorOr;
};

}

#if USING_AK_GLOBALLY
using AK::Error;
using AK::ErrorOr;
using AK::ErrorPayload;
#endif
