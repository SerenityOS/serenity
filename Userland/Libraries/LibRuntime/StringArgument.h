/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Noncopyable.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Types.h>
#ifdef AK_OS_SERENITY
#    include <Kernel/API/Syscall.h>
#endif

namespace Runtime {

class StringArgument {
    AK_MAKE_NONCOPYABLE(StringArgument);
    AK_MAKE_NONMOVABLE(StringArgument);

public:
    StringArgument(char const* string)
        : m_data(string)
        , m_length(__builtin_strlen(string))
    {
    }

#ifdef AK_OS_SERENITY
    StringArgument(StringView view)
        : m_data(view.characters_without_null_termination())
        , m_length(view.length())
    {
    }
#else
    StringArgument(StringView view)
    {
        char* data = reinterpret_cast<char*>(kmalloc(view.length() + 1));
        if (data == nullptr) {
            m_allocation_failure = true;
            return;
        }
        m_must_deallocate = true;
        AK::TypedTransfer<char>::copy(data, view.characters_without_null_termination(), view.length());
        m_data = data;
        m_length = view.length();
        m_must_deallocate = true;
    }

    ~StringArgument()
    {
        if (m_must_deallocate) {
            kfree_sized(const_cast<char*>(m_data), m_length);
            m_data = bit_cast<char const*>(explode_byte(0xf2));
        }
    }
#endif

    StringArgument(ByteString const& string)
        : m_data(string.characters())
        , m_length(string.length())
    {
    }

    // FIXME: Add zero terminator to underlying storage on non-Serenity instead of reallocating.
    StringArgument(String const& string)
        : StringArgument(string.bytes_as_string_view())
    {
    }

#ifdef AK_OS_SERENITY
    Kernel::Syscall::StringArgument get() const
    {
        return Kernel::Syscall::StringArgument { m_data, m_length };
    }
#else
    ErrorOr<char const*> get() const
    {
        // FIXME: Should we assert there is no '\0' inside?
        if (m_allocation_failure)
            return Error::from_errno(ENOMEM);
        return m_data;
    }
#endif

private:
    char const* m_data;
    size_t m_length;
#ifndef AK_OS_SERENITY
    bool m_must_deallocate = false;
    bool m_allocation_failure = false;
#endif
};

}
