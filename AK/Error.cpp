/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/String.h>

namespace AK {

Error::Error(Error&& other)
{
    swap(this->m_syscall, other.m_syscall);
    swap(this->m_code, other.m_code);
#if defined(KERNEL)
    swap(this->m_string_literal, other.m_string_literal);
#else
    if (other.m_string_literal.has<StringView>())
        this->m_string_literal = other.m_string_literal.get<StringView>();
    else if (other.m_string_literal.get<String const*>() != nullptr) {
        this->m_string_literal = other.m_string_literal.get<String const*>();
        // Make sure the other Error doesn't destruct the string!
        other.m_string_literal.get<String const*>() = nullptr;
    }
#endif
}

Error::Error(Error const& other)
    : m_code(other.m_code)
    , m_syscall(other.m_syscall)
{
#if defined(KERNEL)
    this->m_string_literal = other.m_string_literal;
#else
    if (other.m_string_literal.has<StringView>())
        this->m_string_literal = other.m_string_literal.get<StringView>();
    else if (other.m_string_literal.get<String const*>() != nullptr)
        this->m_string_literal = new (nothrow) String { *other.m_string_literal.get<String const*>() };
#endif
}

Error::Error(Error& other)
    : Error(static_cast<Error const&>(other))
{
}

Error::~Error()
{
#if !defined(KERNEL)
    if (m_string_literal.has<String const*>() && m_string_literal.get<String const*>() != nullptr)
        delete m_string_literal.get<String const*>();
#endif
}

#if !defined(KERNEL)
Error::Error(String const* owned_string_literal)
    : m_string_literal(owned_string_literal)
{
}

Error::Error(String const* owned_string_literal, int code)
    : m_string_literal(owned_string_literal)
    , m_code(code)
{
    VERIFY(m_string_literal.has<String const*>());
}

Error Error::from_string(ErrorOr<String>&& string_or_error, Optional<int> code)
{
    // We intentionally do not return the String creation error here, since that is probably unrelated to the actual error.
    if (string_or_error.is_error()) {
        if (code.has_value())
            return { code.value() };
        return { ""sv };
    }

    // Note that we can safely put the nullptr into the new Error, since string_literal handles that case.
    auto const* heap_string = new (nothrow) String { string_or_error.release_value() };

    if (code.has_value())
        return { heap_string, code.value() };

    return Error { heap_string };
}
#endif

StringView Error::string_literal() const
{
#if defined(KERNEL)
    return m_string_literal;
#else
    return m_string_literal.visit(
        [](StringView const& view) { return view; },
        [](String const* owned_string) { return owned_string != nullptr ? owned_string->bytes_as_string_view() : StringView {}; });
#endif
}

}
