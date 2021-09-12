/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Platform.h>
#include <LibCore/SecretString.h>
#if defined(AK_OS_MACOS)
#    define __STDC_WANT_LIB_EXT1__ 1
#endif
#include <string.h>

namespace Core {

SecretString SecretString::take_ownership(char*& cstring, size_t length)
{
    auto buffer = ByteBuffer::copy(cstring, length);
    VERIFY(buffer.has_value());

#if defined(AK_OS_MACOS)
    memset_s(cstring, length, 0, length);
#else
    explicit_bzero(cstring, length);
#endif
    free(cstring);

    return SecretString(buffer.release_value());
}

SecretString SecretString::take_ownership(ByteBuffer&& buffer)
{
    return SecretString(move(buffer));
}

SecretString::SecretString(ByteBuffer&& buffer)
    : m_secure_buffer(move(buffer))
{
}

SecretString::~SecretString()
{
    // Note: We use explicit_bzero to avoid the zeroing from being optimized out by the compiler,
    // which is possible if memset was to be used here.
    if (!m_secure_buffer.is_empty()) {
#if defined(AK_OS_MACOS)
        memset_s(m_secure_buffer.data(), m_secure_buffer.size(), 0, m_secure_buffer.size());
#else
        explicit_bzero(m_secure_buffer.data(), m_secure_buffer.capacity());
#endif
    }
}

}
