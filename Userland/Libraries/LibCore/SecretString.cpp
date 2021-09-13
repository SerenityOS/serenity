/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <LibCore/SecretString.h>

namespace Core {

SecretString SecretString::take_ownership(char*& cstring, size_t length)
{
    auto buffer = ByteBuffer::copy(cstring, length);
    VERIFY(buffer.has_value());

    secure_zero(cstring, length);
    free(cstring);
    cstring = nullptr;

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
    // Note: We use secure_zero to avoid the zeroing from being optimized out by the compiler,
    // which is possible if memset was to be used here.
    if (!m_secure_buffer.is_empty()) {
        secure_zero(m_secure_buffer.data(), m_secure_buffer.capacity());
    }
}

}
