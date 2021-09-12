/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/SecretString.h>
#include <string.h>

namespace Core {

SecretString SecretString::take_ownership(char*& cstring, size_t length)
{
    auto buffer = ByteBuffer::copy(cstring, length);
    VERIFY(buffer.has_value());

    explicit_bzero(cstring, length);
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
    if (!m_secure_buffer.is_empty()) {
        // Note: We use explicit_bzero to avoid the zeroing from being optimized out by the compiler,
        // which is possible if memset was to be used here.
        explicit_bzero(m_secure_buffer.data(), m_secure_buffer.capacity());
    }
}

}
