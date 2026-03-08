/*
 * Copyright (c) 2026, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteBuffer.h>

namespace SSH {

class Cipher {
    // FIXME: Erase the secrets on destruction.
public:
    Cipher(u8 block_size);
    virtual ~Cipher() = default;

    ErrorOr<void> decrypt(Bytes);
    ErrorOr<void> encrypt(Bytes);

    u8 block_size() const { return m_block_size; }

private:
    virtual void decrypt_impl(Bytes) = 0;
    virtual void encrypt_impl(Bytes) = 0;

    u8 m_block_size {};
};

class IdentityCipher : public Cipher {
public:
    IdentityCipher()
        : Cipher(1)
    {
    }
    virtual ~IdentityCipher() = default;

private:
    void decrypt_impl(Bytes) override { }
    void encrypt_impl(Bytes) override { }
};

} // SSH
