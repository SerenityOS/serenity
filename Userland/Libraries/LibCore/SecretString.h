/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>
#include <AK/Noncopyable.h>
#include <AK/StringView.h>

namespace Core {

class SecretString {
    AK_MAKE_NONCOPYABLE(SecretString);
    AK_MAKE_DEFAULT_MOVABLE(SecretString);

public:
    [[nodiscard]] static ErrorOr<SecretString> take_ownership(char*&, size_t);
    [[nodiscard]] static SecretString take_ownership(ByteBuffer&&);

    [[nodiscard]] bool is_empty() const { return m_secure_buffer.is_empty(); }
    [[nodiscard]] size_t length() const { return m_secure_buffer.size(); }
    [[nodiscard]] char const* characters() const { return reinterpret_cast<char const*>(m_secure_buffer.data()); }
    [[nodiscard]] StringView view() const { return { characters(), length() }; }

    SecretString() = default;
    ~SecretString();

private:
    explicit SecretString(ByteBuffer&&);

    ByteBuffer m_secure_buffer;
};

}
