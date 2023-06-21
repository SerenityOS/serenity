/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Memory.h>
#include <LibCore/SecretString.h>

namespace Core {

ErrorOr<SecretString> SecretString::take_ownership(char*& cstring, size_t length)
{
    auto buffer = TRY(ByteBuffer::copy(cstring, length));

    secure_zero(cstring, length);
    free(cstring);
    cstring = nullptr;

    return SecretString(move(buffer));
}

SecretString SecretString::take_ownership(ByteBuffer&& buffer)
{
    return SecretString(move(buffer));
}

SecretString::SecretString(ByteBuffer&& buffer)
    : m_secure_buffer(move(buffer))
{
    // SecretString is currently only used to provide the character data to invocations to crypt(),
    // which requires a NUL-terminated string. To ensure this operation avoids a buffer overrun,
    // append a NUL terminator here if there isn't already one.
    if (m_secure_buffer.is_empty() || (m_secure_buffer[m_secure_buffer.size() - 1] != 0)) {
        u8 nul = '\0';
        m_secure_buffer.append(&nul, 1);
    }
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
